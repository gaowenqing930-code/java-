# -*- coding: utf-8 -*-
"""生成 AI 面试模块后端 Java 文件（utf-8）"""
import io
import os

BASE = r"D:\Codes\ai-interview-coach\backend\src\main\java\com\xbmd\module\interview"

files = {}

files["entity/InterviewSession.java"] = '''package com.xbmd.module.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 面试会话表
 */
@Data
@TableName("interview_session")
public class InterviewSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 简历文本（原始） */
    private String resumeText;

    /** 技术栈（逗号分隔） */
    private String techStack;

    /** 项目列表 JSON */
    private String projects;

    /** CREATED / IN_PROGRESS / FINISHED */
    private String status;

    /** 总分（每题满分 10，共 100） */
    private Integer totalScore;

    /** 总体评价 */
    private String summary;

    /** 改进建议 */
    private String suggestion;

    private LocalDateTime createTime;

    private LocalDateTime finishTime;
}
'''

files["entity/InterviewQuestion.java"] = '''package com.xbmd.module.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * AI 面试题目与评分
 */
@Data
@TableName("interview_question")
public class InterviewQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    /** 题号 1-10 */
    private Integer questionNo;

    private String question;

    /** 评分点 JSON [{point,score}] */
    private String scorePointsJson;

    private String referenceAnswer;

    private String userAnswer;

    /** AI 评分 0-10 */
    private Integer aiScore;

    private String aiComment;

    /** PENDING / SCORED */
    private String status;
}
'''

files["mapper/InterviewSessionMapper.java"] = '''package com.xbmd.module.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbmd.module.interview.entity.InterviewSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InterviewSessionMapper extends BaseMapper<InterviewSession> {
}
'''

files["mapper/InterviewQuestionMapper.java"] = '''package com.xbmd.module.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbmd.module.interview.entity.InterviewQuestion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InterviewQuestionMapper extends BaseMapper<InterviewQuestion> {
}
'''

files["dto/InterviewCreateRequest.java"] = '''package com.xbmd.module.interview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建 AI 面试请求：简历文本
 */
@Data
public class InterviewCreateRequest {

    @NotBlank(message = "简历内容不能为空")
    private String resumeText;
}
'''

files["dto/InterviewAnswerRequest.java"] = '''package com.xbmd.module.interview.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交面试作答请求
 */
@Data
public class InterviewAnswerRequest {

    /** 题号 1-10 */
    @NotNull(message = "题号不能为空")
    @Min(value = 1, message = "题号范围 1-10")
    private Integer no;

    @NotBlank(message = "回答内容不能为空")
    private String answer;
}
'''

files["dto/ProjectBriefVO.java"] = '''package com.xbmd.module.interview.dto;

import lombok.Data;

/**
 * 简历项目简介
 */
@Data
public class ProjectBriefVO {

    private String name;

    private String description;
}
'''

files["dto/ResumeParseVO.java"] = '''package com.xbmd.module.interview.dto;

import lombok.Data;

import java.util.List;

/**
 * 简历解析结果：技术栈 + 项目
 */
@Data
public class ResumeParseVO {

    private Long sessionId;

    private List<String> techStack;

    private List<ProjectBriefVO> projects;
}
'''

files["dto/InterviewGeneratedQuestion.java"] = '''package com.xbmd.module.interview.dto;

import com.xbmd.module.question.dto.ScorePointDTO;
import lombok.Data;

import java.util.List;

/**
 * AI 生成的面试题（模型输出）
 */
@Data
public class InterviewGeneratedQuestion {

    private String question;

    private List<ScorePointDTO> scorePoints;

    private String referenceAnswer;
}
'''

files["dto/ScoreDTO.java"] = '''package com.xbmd.module.interview.dto;

import lombok.Data;

/**
 * AI 单题评分结果（模型输出）
 */
@Data
public class ScoreDTO {

    private Integer score;

    private String comment;
}
'''

files["dto/ScoreSummary.java"] = '''package com.xbmd.module.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 面试总结（模型输出）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreSummary {

    private String summary;

    private String suggestion;
}
'''

files["dto/InterviewQuestionVO.java"] = '''package com.xbmd.module.interview.dto;

import com.xbmd.module.question.dto.ScorePointDTO;
import lombok.Data;

import java.util.List;

/**
 * 面试题视图：未答时只返回题干，答完返回评分与参考回答
 */
@Data
public class InterviewQuestionVO {

    private Integer no;

    private String question;

    /** PENDING / SCORED */
    private String status;

    private String userAnswer;

    private Integer aiScore;

    private String aiComment;

    private List<ScorePointDTO> scorePoints;

    private String referenceAnswer;
}
'''

files["dto/InterviewAnswerVO.java"] = '''package com.xbmd.module.interview.dto;

import lombok.Data;

/**
 * 作答响应：未结束返回下一题，结束返回完整结果
 */
@Data
public class InterviewAnswerVO {

    private boolean finished;

    private InterviewQuestionVO currentQuestion;

    private InterviewResultVO result;
}
'''

files["dto/InterviewResultVO.java"] = '''package com.xbmd.module.interview.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 面试结果：总分 + 评价 + 建议 + 逐题明细
 */
@Data
public class InterviewResultVO {

    private Long sessionId;

    private Integer totalScore;

    private String summary;

    private String suggestion;

    private LocalDateTime finishTime;

    private List<InterviewQuestionVO> questions;
}
'''

files["service/InterviewService.java"] = '''package com.xbmd.module.interview.service;

import com.xbmd.module.interview.dto.InterviewAnswerRequest;
import com.xbmd.module.interview.dto.InterviewAnswerVO;
import com.xbmd.module.interview.dto.InterviewCreateRequest;
import com.xbmd.module.interview.dto.InterviewQuestionVO;
import com.xbmd.module.interview.dto.InterviewResultVO;
import com.xbmd.module.interview.dto.ResumeParseVO;

/**
 * AI 智能面试服务
 */
public interface InterviewService {

    /** 解析简历并创建面试会话，返回技术栈与项目概览 */
    ResumeParseVO create(InterviewCreateRequest request, Long userId);

    /** 开始面试：按简历生成 10 道面试题，返回第一题 */
    InterviewQuestionVO start(Long sessionId, Long userId);

    /** 提交当前题作答，AI 评分；最后一题时生成总结并结束 */
    InterviewAnswerVO answer(Long sessionId, InterviewAnswerRequest request, Long userId);

    /** 获取面试结果 */
    InterviewResultVO result(Long sessionId, Long userId);
}
'''

files["service/impl/InterviewServiceImpl.java"] = '''package com.xbmd.module.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbmd.common.BusinessException;
import com.xbmd.common.ResultCode;
import com.xbmd.module.ai.config.AiProperties;
import com.xbmd.module.interview.dto.InterviewAnswerRequest;
import com.xbmd.module.interview.dto.InterviewAnswerVO;
import com.xbmd.module.interview.dto.InterviewCreateRequest;
import com.xbmd.module.interview.dto.InterviewGeneratedQuestion;
import com.xbmd.module.interview.dto.InterviewQuestionVO;
import com.xbmd.module.interview.dto.InterviewResultVO;
import com.xbmd.module.interview.dto.ProjectBriefVO;
import com.xbmd.module.interview.dto.ResumeParseVO;
import com.xbmd.module.interview.dto.ScoreDTO;
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
 * AI 智能面试服务实现：简历解析 - 生成 10 题 - 逐题评分 - 总结评价
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
    private static final int QUESTION_COUNT = 10;

    private final ObjectProvider<ChatModel> chatModelProvider;
    private final InterviewSessionMapper sessionMapper;
    private final InterviewQuestionMapper questionMapper;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

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

    // ---------- 开始面试：生成 10 题 ----------

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
        ChatModel model = chatModelProvider.getIfAvailable();
        if (model == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "大模型不可用，无法生成面试题");
        }
        List<InterviewGeneratedQuestion> parsed = null;
        for (int attempt = 1; attempt <= aiProperties.getMaxRetries(); attempt++) {
            try {
                String prompt = loadTemplate("INTERVIEW_GENERATE.txt")
                        .replace("${techStack}", nvl(session.getTechStack()))
                        .replace("${projects}", nvl(session.getProjects()));
                String content = callModel(model, prompt, 4096);
                String json = extractJsonArray(content);
                parsed = objectMapper.readValue(json, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, InterviewGeneratedQuestion.class));
                if (parsed == null || parsed.size() != QUESTION_COUNT) {
                    throw new BusinessException(ResultCode.INTERNAL_ERROR,
                            "AI 生成的面试题数量不足 " + QUESTION_COUNT + " 道");
                }
                for (InterviewGeneratedQuestion g : parsed) {
                    validateGenerated(g);
                }
                break;
            } catch (BusinessException e) {
                log.warn("生成面试题第 {} 次尝试失败：{}", attempt, e.getMessage());
            } catch (Exception e) {
                log.warn("生成面试题第 {} 次尝试异常：{}", attempt, e.getMessage());
            }
        }
        if (parsed == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "AI 生成面试题多次失败，请重试");
        }
        int no = 1;
        for (InterviewGeneratedQuestion g : parsed) {
            InterviewQuestion q = new InterviewQuestion();
            q.setSessionId(sessionId);
            q.setQuestionNo(no++);
            q.setQuestion(g.getQuestion().trim());
            q.setScorePointsJson(toJson(g.getScorePoints()));
            q.setReferenceAnswer(g.getReferenceAnswer());
            q.setStatus(Q_PENDING);
            questionMapper.insert(q);
        }
        session.setStatus(STATUS_IN_PROGRESS);
        sessionMapper.updateById(session);
        return toVO(listQuestions(sessionId).get(0));
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
        InterviewQuestion cur = questionMapper.selectOne(new LambdaQueryWrapper<InterviewQuestion>()
                .eq(InterviewQuestion::getSessionId, sessionId)
                .eq(InterviewQuestion::getQuestionNo, request.getNo()));
        if (cur == null || !Q_PENDING.equals(cur.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "题目不存在或已作答");
        }
        cur.setUserAnswer(request.getAnswer().trim());
        scoreOne(cur);
        questionMapper.updateById(cur);

        List<InterviewQuestion> all = listQuestions(sessionId);
        boolean allScored = all.stream().allMatch(q -> Q_SCORED.equals(q.getStatus()));
        InterviewAnswerVO vo = new InterviewAnswerVO();
        if (allScored) {
            int total = all.stream().mapToInt(q -> q.getAiScore() == null ? 0 : q.getAiScore()).sum();
            ScoreSummary summary = summarize(all);
            session.setStatus(STATUS_FINISHED);
            session.setTotalScore(total);
            session.setSummary(summary.getSummary());
            session.setSuggestion(summary.getSuggestion());
            session.setFinishTime(LocalDateTime.now());
            sessionMapper.updateById(session);
            vo.setFinished(true);
            vo.setResult(toResultVO(session, all));
            return vo;
        }
        vo.setFinished(false);
        InterviewQuestion next = all.stream().filter(q -> Q_PENDING.equals(q.getStatus())).findFirst().orElse(null);
        if (next != null) {
            vo.setCurrentQuestion(toVO(next));
        }
        return vo;
    }

    /** 单题 AI 评分，多次失败按回答长度降级 */
    private void scoreOne(InterviewQuestion q) {
        ChatModel model = chatModelProvider.getIfAvailable();
        if (model == null) {
            q.setAiScore(ruleScore(q.getUserAnswer()));
            q.setAiComment("大模型不可用，已按回答长度降级评分");
            q.setStatus(Q_SCORED);
            return;
        }
        String pointsText = formatPoints(fromJsonPoints(q.getScorePointsJson()));
        for (int attempt = 1; attempt <= aiProperties.getMaxRetries(); attempt++) {
            try {
                String prompt = loadTemplate("INTERVIEW_SCORE.txt")
                        .replace("${question}", nvl(q.getQuestion()))
                        .replace("${scorePoints}", pointsText)
                        .replace("${referenceAnswer}", nvl(q.getReferenceAnswer()))
                        .replace("${userAnswer}", nvl(q.getUserAnswer()));
                String content = callModel(model, prompt, 512);
                ScoreDTO dto = objectMapper.readValue(extractJsonObject(content), ScoreDTO.class);
                if (dto == null || dto.getScore() == null || dto.getScore() < 0 || dto.getScore() > 10) {
                    throw new BusinessException(ResultCode.INTERNAL_ERROR, "评分超出范围");
                }
                if (!StringUtils.hasText(dto.getComment())) {
                    throw new BusinessException(ResultCode.INTERNAL_ERROR, "评语为空");
                }
                q.setAiScore(dto.getScore());
                q.setAiComment(dto.getComment());
                q.setStatus(Q_SCORED);
                return;
            } catch (BusinessException e) {
                log.warn("面试评分第 {} 次尝试失败：{}", attempt, e.getMessage());
            } catch (Exception e) {
                log.warn("面试评分第 {} 次尝试异常：{}", attempt, e.getMessage());
            }
        }
        q.setAiScore(ruleScore(q.getUserAnswer()));
        q.setAiComment("AI 评分多次失败，已按回答长度降级评分");
        q.setStatus(Q_SCORED);
    }

    /** 总结评价：AI 生成，失败按分数段模板降级 */
    private ScoreSummary summarize(List<InterviewQuestion> all) {
        ChatModel model = chatModelProvider.getIfAvailable();
        if (model != null) {
            StringBuilder details = new StringBuilder();
            for (InterviewQuestion q : all) {
                details.append("第 ").append(q.getQuestionNo()).append(" 题（")
                        .append(q.getAiScore()).append("/10）：")
                        .append(nvl(q.getAiComment())).append("\\n");
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

    /** 规则降级评分：按回答长度 */
    private int ruleScore(String answer) {
        if (!StringUtils.hasText(answer)) {
            return 0;
        }
        int len = answer.length();
        if (len >= 150) {
            return 8;
        }
        if (len >= 80) {
            return 6;
        }
        if (len >= 30) {
            return 4;
        }
        return 2;
    }

    // ---------- 结果 ----------

    @Override
    public InterviewResultVO result(Long sessionId, Long userId) {
        InterviewSession session = getOwnedSession(sessionId, userId);
        return toResultVO(session, listQuestions(sessionId));
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
        ChatClient client = ChatClient.builder(model)
                .defaultOptions(OpenAiChatOptions.builder().maxTokens(maxTokens).build())
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
            int newline = text.indexOf('\\n', fence);
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
            int newline = text.indexOf('\\n', fence);
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

    private String formatPoints(List<ScorePointDTO> points) {
        if (points == null || points.isEmpty()) {
            return "（无评分点，满分 10 分）";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < points.size(); i++) {
            sb.append(i + 1).append(". ").append(nvl(points.get(i).getPoint()))
                    .append("（").append(points.get(i).getScore()).append("分）\\n");
        }
        return sb.toString();
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
'''

files["controller/InterviewController.java"] = '''package com.xbmd.module.interview.controller;

import com.xbmd.common.Result;
import com.xbmd.module.interview.dto.InterviewAnswerRequest;
import com.xbmd.module.interview.dto.InterviewAnswerVO;
import com.xbmd.module.interview.dto.InterviewCreateRequest;
import com.xbmd.module.interview.dto.InterviewQuestionVO;
import com.xbmd.module.interview.dto.InterviewResultVO;
import com.xbmd.module.interview.dto.ResumeParseVO;
import com.xbmd.module.interview.service.InterviewService;
import com.xbmd.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 智能面试接口（登录用户）
 */
@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    /** 解析简历并创建面试会话 */
    @PostMapping("/sessions")
    public Result<ResumeParseVO> create(@Valid @RequestBody InterviewCreateRequest request) {
        return Result.success(interviewService.create(request, UserContext.getUserId()));
    }

    /** 开始面试：按简历生成 10 道面试题，返回第一题 */
    @PostMapping("/sessions/{id}/start")
    public Result<InterviewQuestionVO> start(@PathVariable Long id) {
        return Result.success(interviewService.start(id, UserContext.getUserId()));
    }

    /** 提交当前题作答，AI 评分；最后一题返回完整结果 */
    @PostMapping("/sessions/{id}/answer")
    public Result<InterviewAnswerVO> answer(@PathVariable Long id,
                                            @Valid @RequestBody InterviewAnswerRequest request) {
        return Result.success(interviewService.answer(id, request, UserContext.getUserId()));
    }

    /** 获取面试结果 */
    @GetMapping("/sessions/{id}/result")
    public Result<InterviewResultVO> result(@PathVariable Long id) {
        return Result.success(interviewService.result(id, UserContext.getUserId()));
    }
}
'''

for rel, content in files.items():
    path = os.path.join(BASE, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with io.open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print("written:", rel)
print("DONE")
