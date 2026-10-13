package com.xbmd.module.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbmd.common.BusinessException;
import com.xbmd.common.ResultCode;
import com.xbmd.module.ai.config.AiProperties;
import com.xbmd.module.ai.util.AiScoreCoordinator;
import com.xbmd.module.interview.dto.InterviewAnswerRequest;
import com.xbmd.module.interview.dto.InterviewAnswerVO;
import com.xbmd.module.interview.dto.InterviewCreateRequest;
import com.xbmd.module.interview.dto.InterviewGeneratedQuestion;
import com.xbmd.module.interview.dto.InterviewQuestionVO;
import com.xbmd.module.interview.dto.InterviewResultVO;
import com.xbmd.module.interview.dto.InterviewSessionVO;
import com.xbmd.module.interview.dto.ProjectBriefVO;
import com.xbmd.module.interview.dto.ResumeParseVO;
import com.xbmd.module.interview.dto.ScoreSummary;
import com.xbmd.module.interview.entity.InterviewQuestion;
import com.xbmd.module.interview.entity.InterviewSession;
import com.xbmd.module.interview.mapper.InterviewQuestionMapper;
import com.xbmd.module.interview.mapper.InterviewSessionMapper;
import com.xbmd.module.interview.service.InterviewService;
import com.xbmd.module.question.dto.ScorePointDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 智能面试服务实现：简历解析 - 对话式动态出题（AI 引导 + 用户可主动结束） - 异步逐题评分 - 总结评价
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private static final String STATUS_CREATED = "CREATED";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_FINISHED = "FINISHED";
    private static final String Q_PENDING = "PENDING";
    private static final String Q_SCORED = "SCORED";
    /** 面试题兜底上限：题数动态由 AI 判断，达上限自动结束，防止无限追问 */
    private static final int MAX_QUESTIONS = 15;

    private final ObjectProvider<ChatModel> chatModelProvider;

    @Value("${spring.ai.openai.chat.options.model:}")
    private String aiModel;
    private final InterviewSessionMapper sessionMapper;
    private final InterviewQuestionMapper questionMapper;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final InterviewScoreScorer scoreScorer;
    private final AiScoreCoordinator coordinator;

    // ---------- 创建会话：解析简历 ----------

    @Override
    public ResumeParseVO create(InterviewCreateRequest request, Long userId) {
        String resume = request.getResumeText().trim();
        if (resume.length() > 20000) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "简历文本过长，请精简后重试");
        }
        ResumeParseVO parsed = parseResume(resume);
        InterviewSession session = new InterviewSession();
        session.setUserId(userId);
        session.setResumeText(resume);
        session.setTechStack(String.join(",", parsed.getTechStack()));
        session.setProjects(toJson(parsed.getProjects()));
        session.setStatus(STATUS_CREATED);
        sessionMapper.insert(session);
        parsed.setSessionId(session.getId());
        return parsed;
    }

    private ResumeParseVO parseResume(String resume) {
        ChatModel model = chatModelProvider.getIfAvailable();
        if (model == null) {
            return ruleParseResume(resume);
        }
        for (int attempt = 1; attempt <= aiProperties.getMaxRetries(); attempt++) {
            try {
                String prompt = loadTemplate("INTERVIEW_PARSE.txt")
                        .replace("${resume}", truncate(resume, 6000));
                String content = callModel(model, prompt, 1024);
                return objectMapper.readValue(extractJsonObject(content), ResumeParseVO.class);
            } catch (BusinessException e) {
                log.warn("简历解析第 {} 次尝试失败：{}", attempt, e.getMessage());
            } catch (Exception e) {
                log.warn("简历解析第 {} 次尝试异常：{}", attempt, e.getMessage());
            }
        }
        return ruleParseResume(resume);
    }

    /** 规则兜底：扫描常见技术关键词 */
    private ResumeParseVO ruleParseResume(String resume) {
        String lower = resume.toLowerCase();
        String[] candidates = {"Java", "Spring Boot", "Spring", "MySQL", "Redis", "MyBatis",
                "Vue", "React", "JavaScript", "TypeScript", "HTML", "CSS", "Python",
                "Docker", "Linux", "Git", "RabbitMQ", "Kafka", "Elasticsearch", "Nginx"};
        List<String> hit = new ArrayList<>();
        for (String kw : candidates) {
            if (lower.contains(kw.toLowerCase())) {
                hit.add(kw);
            }
        }
        if (hit.isEmpty()) {
            hit.add("Java");
        }
        ResumeParseVO vo = new ResumeParseVO();
        vo.setTechStack(hit);
        vo.setProjects(new ArrayList<>());
        return vo;
    }

    // ---------- 开始面试：生成第 1 题 ----------

    @Override
    public InterviewQuestionVO start(Long sessionId, Long userId) {
        InterviewSession session = getOwnedSession(sessionId, userId);
        if (STATUS_FINISHED.equals(session.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "本次面试已结束，请重新创建");
        }
        List<InterviewQuestion> existing = listQuestions(sessionId);
        if (!existing.isEmpty()) {
            session.setStatus(STATUS_IN_PROGRESS);
            sessionMapper.updateById(session);
            return toVO(existing.get(0));
        }
        InterviewGeneratedQuestion g = generateOne(session, new ArrayList<>(), 1);
        InterviewQuestion q = buildQuestion(sessionId, 1, g);
        questionMapper.insert(q);
        session.setStatus(STATUS_IN_PROGRESS);
        sessionMapper.updateById(session);
        return toVO(q);
    }

    /** 调用模型生成单道面试题（失败自动重试，重试耗尽抛异常） */
    private InterviewGeneratedQuestion generateOne(InterviewSession session,
                                                   List<InterviewQuestion> answered,
                                                   int nextNo) {
        ChatModel model = chatModelProvider.getIfAvailable();
        if (model == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "大模型不可用，无法生成面试题");
        }
        String history = buildHistory(answered);
        for (int attempt = 1; attempt <= aiProperties.getMaxRetries(); attempt++) {
            try {
                String prompt = loadTemplate("INTERVIEW_GENERATE.txt")
                        .replace("${techStack}", nvl(session.getTechStack()))
                        .replace("${projects}", nvl(session.getProjects()))
                        .replace("${history}", history)
                        .replace("${nextNo}", String.valueOf(nextNo));
                String content = callModel(model, prompt, 2048);
                InterviewGeneratedQuestion g = objectMapper.readValue(
                        extractJsonObject(content), InterviewGeneratedQuestion.class);
                validateGenerated(g);
                return g;
            } catch (BusinessException e) {
                log.warn("生成第 {} 道面试题第 {} 次尝试失败：{}", nextNo, attempt, e.getMessage());
            } catch (Exception e) {
                log.warn("生成第 {} 道面试题第 {} 次尝试异常：{}", nextNo, attempt, e.getMessage(), e);
            }
        }
        throw new BusinessException(ResultCode.INTERNAL_ERROR, "AI 生成面试题多次失败，请重试");
    }

    /** 组装题目实体 */
    private InterviewQuestion buildQuestion(Long sessionId, int no, InterviewGeneratedQuestion g) {
        InterviewQuestion q = new InterviewQuestion();
        q.setSessionId(sessionId);
        q.setQuestionNo(no);
        q.setQuestion(g.getQuestion().trim());
        q.setScorePointsJson(toJson(g.getScorePoints()));
        q.setReferenceAnswer(g.getReferenceAnswer());
        q.setStatus(Q_PENDING);
        return q;
    }

    /** 拼接已答问答摘要，供下一题生成参考 */
    private String buildHistory(List<InterviewQuestion> answered) {
        if (answered == null || answered.isEmpty()) {
            return "（暂无，这是第一题）";
        }
        StringBuilder sb = new StringBuilder();
        for (InterviewQuestion q : answered) {
            sb.append("第 ").append(q.getQuestionNo()).append(" 题：").append(nvl(q.getQuestion())).append("\n");
            sb.append("面试者回答：").append(truncate(nvl(q.getUserAnswer()), 300)).append("\n");
        }
        return sb.toString();
    }

    private void validateGenerated(InterviewGeneratedQuestion g) {
        if (g == null || !StringUtils.hasText(g.getQuestion())) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "面试题内容为空");
        }
        if (g.getScorePoints() == null || g.getScorePoints().isEmpty()) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "面试题缺少评分点");
        }
        int sum = g.getScorePoints().stream().mapToInt(sp -> sp.getScore() == null ? 0 : sp.getScore()).sum();
        if (sum != 10) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "面试题评分点之和必须为 10，实际 " + sum);
        }
    }

    // ---------- 作答与评分 ----------

    @Override
    public InterviewAnswerVO answer(Long sessionId, InterviewAnswerRequest request, Long userId) {
        InterviewSession session = getOwnedSession(sessionId, userId);
        if (!STATUS_IN_PROGRESS.equals(session.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "当前状态不能作答");
        }
        List<InterviewQuestion> all = listQuestions(sessionId);
        long answeredCount = all.stream().filter(q -> StringUtils.hasText(q.getUserAnswer())).count();
        int expectNo = (int) answeredCount + 1;
        if (request.getNo() == null || request.getNo() != expectNo) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "请按顺序作答，当前应回答第 " + expectNo + " 题");
        }
        InterviewQuestion cur = all.stream()
                .filter(q -> q.getQuestionNo() == expectNo)
                .findFirst().orElse(null);
        if (cur == null || StringUtils.hasText(cur.getUserAnswer())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "题目不存在或已作答");
        }
        cur.setUserAnswer(request.getAnswer().trim());
        questionMapper.updateById(cur);
        // 异步一题一评：后台评分不阻塞作答；结束面试时由 doFinish 统一等待补齐
        scoreScorer.scoreAsync(cur.getId());

        InterviewAnswerVO vo = new InterviewAnswerVO();
        if (expectNo >= MAX_QUESTIONS) {
            // 已达兜底上限：本题答完自动评分结束
            vo.setFinished(true);
            vo.setResult(doFinish(session, listQuestions(sessionId)));
            return vo;
        }
        // 对话式出题：AI 结合上一题回答引导式生成下一题，并判断是否建议结束
        // 出题失败不回滚答案、不抛异常：返回 needRetry=true，由前端引导用户调 next 接口重试出题
        try {
            InterviewGeneratedQuestion g = generateOne(session, listQuestions(sessionId), expectNo + 1);
            InterviewQuestion next = buildQuestion(sessionId, expectNo + 1, g);
            questionMapper.insert(next);
            vo.setFinished(false);
            vo.setCurrentQuestion(withSuggest(toVO(next), g));
        } catch (BusinessException e) {
            log.warn("回答第 {} 题后 AI 出下一题失败：{}", expectNo, e.getMessage());
            vo.setFinished(false);
            vo.setNeedRetry(true);
        }
        return vo;
    }

    /** 出题失败后重试生成下一题；幂等：下一题已存在则直接返回（防重复出题） */
    @Override
    public InterviewQuestionVO nextQuestion(Long sessionId, Long userId) {
        InterviewSession session = getOwnedSession(sessionId, userId);
        if (!STATUS_IN_PROGRESS.equals(session.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "当前状态不能出题");
        }
        List<InterviewQuestion> all = listQuestions(sessionId);
        long answeredCount = all.stream().filter(q -> StringUtils.hasText(q.getUserAnswer())).count();
        int nextNo = (int) answeredCount + 1;
        if (nextNo > MAX_QUESTIONS) {
            throw new BusinessException(ResultCode.CONFLICT, "本轮面试题目已出完，请结束面试");
        }
        InterviewQuestion existing = all.stream()
                .filter(q -> q.getQuestionNo() == nextNo)
                .findFirst().orElse(null);
        if (existing != null) {
            return toVO(existing);
        }
        InterviewGeneratedQuestion g = generateOne(session, all, nextNo);
        InterviewQuestion next = buildQuestion(sessionId, nextNo, g);
        questionMapper.insert(next);
        return withSuggest(toVO(next), g);
    }

    /** 用户主动结束面试：等待已答题目异步评分补齐后统一汇总并生成总结 */
    private InterviewResultVO doFinish(InterviewSession session, List<InterviewQuestion> all) {
        // 只保留已答的题，未答题删除（不进入结果明细）
        List<InterviewQuestion> answered = new ArrayList<>();
        for (InterviewQuestion q : all) {
            if (StringUtils.hasText(q.getUserAnswer())) {
                answered.add(q);
            } else {
                questionMapper.deleteById(q.getId());
            }
        }
        if (answered.isEmpty()) {
            session.setStatus(STATUS_FINISHED);
            session.setTotalScore(0);
            session.setSummary("尚未回答任何题目，暂时无法生成测评结论。");
            session.setSuggestion("重新开始面试并认真作答，AI 会根据你的回答逐步测评技术栈并给出建议。");
            session.setFinishTime(LocalDateTime.now());
            sessionMapper.updateById(session);
            return toResultVO(session, new ArrayList<>());
        }
        // 等待所有已答题的异步评分补齐（已评完的立即返回，带超时兜底）
        coordinator.awaitAll(
                answered.stream().map(q -> "interview:" + q.getId()).toList(), 10000L);
        // 重新读取已答题（异步评分线程已将分数/评语/状态回写库）
        List<InterviewQuestion> scored = listQuestions(session.getId());
        int sum = scored.stream()
                .filter(q -> StringUtils.hasText(q.getUserAnswer()))
                .mapToInt(q -> q.getAiScore() == null ? 0 : q.getAiScore()).sum();
        // 总分按百分比折算：sum / (已答题数 * 10) * 100
        int total = Math.round(sum * 10.0f / answered.size());
        ScoreSummary summary = summarize(scored);
        session.setStatus(STATUS_FINISHED);
        session.setTotalScore(total);
        session.setSummary(summary.getSummary());
        session.setSuggestion(summary.getSuggestion());
        session.setFinishTime(LocalDateTime.now());
        sessionMapper.updateById(session);
        return toResultVO(session, scored);
    }

    /** 总结评价：AI 生成，失败按分数段模板降级 */
    private ScoreSummary summarize(List<InterviewQuestion> all) {
        ChatModel model = chatModelProvider.getIfAvailable();
        if (model != null) {
            StringBuilder details = new StringBuilder();
            for (InterviewQuestion q : all) {
                details.append("第 ").append(q.getQuestionNo()).append(" 题（")
                        .append(q.getAiScore()).append("/10）：")
                        .append(nvl(q.getAiComment())).append("\n");
            }
            for (int attempt = 1; attempt <= aiProperties.getMaxRetries(); attempt++) {
                try {
                    String prompt = loadTemplate("INTERVIEW_SUMMARY.txt")
                            .replace("${scoreDetails}", details.toString());
                    String content = callModel(model, prompt, 1024);
                    ScoreSummary s = objectMapper.readValue(extractJsonObject(content), ScoreSummary.class);
                    if (StringUtils.hasText(s.getSummary()) && StringUtils.hasText(s.getSuggestion())) {
                        return s;
                    }
                    throw new BusinessException(ResultCode.INTERNAL_ERROR, "总结内容为空");
                } catch (BusinessException e) {
                    log.warn("面试总结第 {} 次尝试失败：{}", attempt, e.getMessage());
                } catch (Exception e) {
                    log.warn("面试总结第 {} 次尝试异常：{}", attempt, e.getMessage());
                }
            }
        }
        return ruleSummary(all);
    }

    private ScoreSummary ruleSummary(List<InterviewQuestion> all) {
        int total = all.stream().mapToInt(q -> q.getAiScore() == null ? 0 : q.getAiScore()).sum();
        String summary;
        String suggestion;
        if (total >= 90) {
            summary = "整体表现非常出色，技术基础扎实、表达清晰，多数问题能答到要点，具备较强的岗位竞争力。";
            suggestion = "继续保持，并针对高频追问场景补充项目数据细节；尝试主动引出项目难点与优化方案；学习系统设计类题目。";
        } else if (total >= 75) {
            summary = "整体表现良好，技术基础较扎实，但对部分深入问题回答不够完整，项目细节需要进一步打磨。";
            suggestion = "系统复盘简历中每个项目的技术难点与踩坑点；加强原理层问题（如框架底层、数据库调优）的练习；答题时先给结论再展开。";
        } else if (total >= 60) {
            summary = "整体表现中等，能回答基础问题，但对原理与场景类问题掌握不足，回答深度有限。";
            suggestion = "按简历技术栈逐点补基础知识；用 STAR 法则组织项目描述；每周做 2-3 次模拟面试并复盘评语。";
        } else {
            summary = "整体表现偏弱，基础概念掌握不牢，项目经历描述不清晰，需要系统性地查漏补缺。";
            suggestion = "先巩固语言与框架核心知识；重写简历项目，突出职责与技术难点；跟随题库专项练习并利用错题本反复巩固。";
        }
        return new ScoreSummary(summary, suggestion);
    }

    // ---------- 结束 / 结果 ----------

    @Override
    public InterviewResultVO finish(Long sessionId, Long userId) {
        InterviewSession session = getOwnedSession(sessionId, userId);
        if (STATUS_FINISHED.equals(session.getStatus())) {
            // 幂等：已结束直接返回结果
            return toResultVO(session, listQuestions(sessionId));
        }
        if (!STATUS_IN_PROGRESS.equals(session.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "当前状态不能结束面试");
        }
        return doFinish(session, listQuestions(sessionId));
    }

    @Override
    public InterviewResultVO result(Long sessionId, Long userId) {
        InterviewSession session = getOwnedSession(sessionId, userId);
        return toResultVO(session, listQuestions(sessionId));
    }

    /** 当前用户历史面试列表（倒序） */
    @Override
    public List<InterviewSessionVO> mySessions(Long userId) {
        List<InterviewSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<InterviewSession>()
                        .eq(InterviewSession::getUserId, userId)
                        .orderByDesc(InterviewSession::getCreateTime));
        List<InterviewSessionVO> result = new ArrayList<>();
        for (InterviewSession s : sessions) {
            InterviewSessionVO vo = new InterviewSessionVO();
            vo.setSessionId(s.getId());
            vo.setTotalScore(s.getTotalScore());
            vo.setStatus(s.getStatus());
            vo.setTechStack(s.getTechStack());
            vo.setCreateTime(s.getCreateTime());
            vo.setFinishTime(s.getFinishTime());
            Long answered = questionMapper.selectCount(new LambdaQueryWrapper<InterviewQuestion>()
                    .eq(InterviewQuestion::getSessionId, s.getId())
                    .isNotNull(InterviewQuestion::getUserAnswer));
            vo.setAnsweredCount(answered == null ? 0 : answered.intValue());
            result.add(vo);
        }
        return result;
    }

    // ---------- 工具 ----------

    private InterviewSession getOwnedSession(Long sessionId, Long userId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "面试会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问他人的面试会话");
        }
        return session;
    }

    private List<InterviewQuestion> listQuestions(Long sessionId) {
        return questionMapper.selectList(new LambdaQueryWrapper<InterviewQuestion>()
                .eq(InterviewQuestion::getSessionId, sessionId)
                .orderByAsc(InterviewQuestion::getQuestionNo));
    }

    private InterviewQuestionVO withSuggest(InterviewQuestionVO vo, InterviewGeneratedQuestion g) {
        if (g != null) {
            vo.setSuggestFinish(Boolean.TRUE.equals(g.getSuggestFinish()));
            vo.setFinishReason(g.getFinishReason());
        }
        return vo;
    }

    private InterviewQuestionVO toVO(InterviewQuestion q) {
        InterviewQuestionVO vo = new InterviewQuestionVO();
        vo.setNo(q.getQuestionNo());
        vo.setQuestion(q.getQuestion());
        vo.setStatus(q.getStatus());
        vo.setUserAnswer(q.getUserAnswer());
        vo.setAiScore(q.getAiScore());
        vo.setAiComment(q.getAiComment());
        if (Q_SCORED.equals(q.getStatus())) {
            vo.setScorePoints(fromJsonPoints(q.getScorePointsJson()));
            vo.setReferenceAnswer(q.getReferenceAnswer());
        }
        return vo;
    }

    private InterviewResultVO toResultVO(InterviewSession session, List<InterviewQuestion> questions) {
        InterviewResultVO vo = new InterviewResultVO();
        vo.setSessionId(session.getId());
        vo.setTotalScore(session.getTotalScore());
        vo.setSummary(session.getSummary());
        vo.setSuggestion(session.getSuggestion());
        vo.setFinishTime(session.getFinishTime());
        List<InterviewQuestionVO> list = new ArrayList<>();
        for (InterviewQuestion q : questions) {
            list.add(toVO(q));
        }
        vo.setQuestions(list);
        return vo;
    }

    private String callModel(ChatModel model, String prompt, int maxTokens) {
        OpenAiChatOptions.Builder optBuilder = OpenAiChatOptions.builder().maxTokens(maxTokens);
        if (StringUtils.hasText(aiModel)) {
            optBuilder.model(aiModel);
        }
        ChatClient client = ChatClient.builder(model)
                .defaultOptions(optBuilder)
                .build();
        String content = client.prompt().user(prompt).call().content();
        if (content == null || content.isBlank()) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "模型返回内容为空");
        }
        return content;
    }

    private String loadTemplate(String name) {
        Resource resource = resourceLoader.getResource(
                "classpath:" + aiProperties.getPromptDir() + "/" + name);
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "Prompt 模板加载失败：" + name);
        }
    }

    private String extractJsonObject(String content) {
        String text = content.trim();
        int fence = text.indexOf("```");
        if (fence >= 0) {
            int newline = text.indexOf('\n', fence);
            int fenceEnd = text.lastIndexOf("```");
            if (newline >= 0 && fenceEnd > newline) {
                text = text.substring(newline + 1, fenceEnd).trim();
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        throw new BusinessException(ResultCode.INTERNAL_ERROR, "模型输出中未找到 JSON 对象");
    }

    private String extractJsonArray(String content) {
        String text = content.trim();
        int fence = text.indexOf("```");
        if (fence >= 0) {
            int newline = text.indexOf('\n', fence);
            int fenceEnd = text.lastIndexOf("```");
            if (newline >= 0 && fenceEnd > newline) {
                text = text.substring(newline + 1, fenceEnd).trim();
            }
        }
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        throw new BusinessException(ResultCode.INTERNAL_ERROR, "模型输出中未找到 JSON 数组");
    }

    private List<ScorePointDTO> fromJsonPoints(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, ScorePointDTO.class));
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "数据序列化失败");
        }
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max);
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }
}
