package com.xbmd.module.practice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbmd.common.BusinessException;
import com.xbmd.common.ResultCode;
import com.xbmd.module.ai.dto.AiGenerateRequest;
import com.xbmd.module.ai.dto.AiGenerateResult;
import com.xbmd.module.ai.service.AiQuestionService;
import com.xbmd.module.ai.util.AiScoreCoordinator;
import com.xbmd.module.evaluation.dto.EvaluationVO;
import com.xbmd.module.evaluation.entity.EvaluationResult;
import com.xbmd.module.evaluation.mapper.EvaluationResultMapper;
import com.xbmd.module.practice.dto.AnswerSubmitRequest;
import com.xbmd.module.practice.dto.AnswerSubmitResult;
import com.xbmd.module.practice.dto.CreatePracticeRequest;
import com.xbmd.module.practice.dto.PracticeDetailVO;
import com.xbmd.module.practice.dto.PracticeHistoryVO;
import com.xbmd.module.practice.dto.PracticeItemVO;
import com.xbmd.module.practice.entity.AnswerRecord;
import com.xbmd.module.practice.entity.PracticeRecord;
import com.xbmd.module.practice.entity.WrongQuestion;
import com.xbmd.module.practice.mapper.AnswerRecordMapper;
import com.xbmd.module.practice.mapper.PracticeRecordMapper;
import com.xbmd.module.practice.mapper.WrongQuestionMapper;
import com.xbmd.module.practice.service.AiAsyncScoreService;
import com.xbmd.module.practice.service.PracticeService;
import com.xbmd.module.question.dto.QuestionVO;
import com.xbmd.module.question.entity.Question;
import com.xbmd.module.question.enums.QuestionType;
import com.xbmd.module.question.mapper.QuestionMapper;
import com.xbmd.module.question.service.QuestionService;
import com.xbmd.module.stats.service.StatsService;
import com.xbmd.module.wrong.service.WrongQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 在线练习服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PracticeServiceImpl implements PracticeService {

    private static final int STATUS_PROGRESS = 0;
    private static final int STATUS_FINISHED = 1;
    private static final int SCORE_PER_QUESTION = 10;

    private final PracticeRecordMapper practiceRecordMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final QuestionMapper questionMapper;
    private final QuestionService questionService;
    private final AiQuestionService aiQuestionService;
    private final AiAsyncScoreService aiAsyncScoreService;
    private final AiScoreCoordinator aiScoreCoordinator;
    private final EvaluationResultMapper evaluationResultMapper;
    private final ObjectMapper objectMapper;
    private final com.xbmd.module.knowledge.mapper.KnowledgeCategoryMapper categoryMapper;
    private final com.xbmd.module.knowledge.service.KnowledgeCategoryService categoryService;
    private final com.xbmd.module.exam.mapper.ExamPaperMapper examPaperMapper;
    private final com.xbmd.module.exam.mapper.ExamPaperQuestionMapper examPaperQuestionMapper;
    private final WrongQuestionService wrongQuestionService;
    private final StatsService statsService;

    // ---------- 创建练习与组题 ----------

    @Override
    @Transactional
    public PracticeDetailVO create(CreatePracticeRequest request, Long userId) {
        List<QuestionVO> questions = buildQuestions(request, userId);
        if (questions.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前条件下没有可练习的题目，请调整条件");
        }

        PracticeRecord record = new PracticeRecord();
        record.setUserId(userId);
        record.setMode(request.getMode());
        record.setConditionJson(toJson(buildCondition(request, questions)));
        record.setTotalCount(questions.size());
        record.setCorrectCount(0);
        record.setScore(0);
        record.setStatus(STATUS_PROGRESS);
        practiceRecordMapper.insert(record);

        PracticeDetailVO vo = new PracticeDetailVO();
        vo.setId(record.getId());
        vo.setMode(record.getMode());
        vo.setConditionJson(record.getConditionJson());
        vo.setTotalCount(record.getTotalCount());
        vo.setCorrectCount(0);
        vo.setScore(0);
        vo.setStatus(STATUS_PROGRESS);
        vo.setCreateTime(record.getCreateTime());
        vo.setItems(questions.stream().map(q -> {
            PracticeItemVO item = new PracticeItemVO();
            item.setQuestion(q);
            return item;
        }).toList());
        return vo;
    }

    /** 按五种模式组题 */
    private List<QuestionVO> buildQuestions(CreatePracticeRequest request, Long userId) {
        List<Question> picked = switch (request.getMode()) {
            case "PAPER_BANK" -> pickFromBank(request, userId);
            case "EXAM" -> pickPaper(request);
            case "AI" -> pickFromAi(request, userId);
            case "ADAPTIVE" -> pickAdaptive(request, userId);
            case "WRONG" -> pickFromWrong(userId, request.getCount());
            default -> throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的练习方式");
        };
        return picked.stream().map(q -> questionService.getDetail(q.getId())).toList();
    }

    /**
     * 真题模式：按预置卷子的固定题目整套作答。
     * 卷子题目提前配置在 exam_paper / exam_paper_question，不做动态组卷、不调 AI 补题。
     */
    private List<Question> pickPaper(CreatePracticeRequest request) {
        if (request.getPaperId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "真题模式必须指定卷子");
        }
        com.xbmd.module.exam.entity.ExamPaper paper = examPaperMapper.selectById(request.getPaperId());
        if (paper == null || !Integer.valueOf(1).equals(paper.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "卷子不存在或已下架");
        }
        List<com.xbmd.module.exam.entity.ExamPaperQuestion> rels = examPaperQuestionMapper.selectList(
                new LambdaQueryWrapper<com.xbmd.module.exam.entity.ExamPaperQuestion>()
                        .eq(com.xbmd.module.exam.entity.ExamPaperQuestion::getPaperId, request.getPaperId())
                        .orderByAsc(com.xbmd.module.exam.entity.ExamPaperQuestion::getSort));
        List<Question> questions = new ArrayList<>();
        for (com.xbmd.module.exam.entity.ExamPaperQuestion rel : rels) {
            Question q = questionMapper.selectById(rel.getQuestionId());
            if (q != null && Integer.valueOf(1).equals(q.getStatus())) {
                questions.add(q);
            }
        }
        if (questions.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该卷子暂无可用题目");
        }
        return questions;
    }

    /**
     * 题库模式：关键词抽题，不足 count 时用 AI 按命中的分类知识点补足，
     * 保证用户设置 5/10/15 道就出 5/10/15 道（AI 补题会自动入库挂知识点）。
     */
    private List<Question> pickFromBank(CreatePracticeRequest request, Long userId) {
        Long categoryId = null;
        if (StringUtils.hasText(request.getKeyword())) {
            List<com.xbmd.module.knowledge.entity.KnowledgeCategory> cats =
                    findCategoriesByKeyword(request.getKeyword().trim());
            if (!cats.isEmpty()) {
                categoryId = cats.get(0).getId();
            }
        }
        return pickWithAiTopup(request.getKeyword(), request.getType(), request.getDifficulty(),
                request.getCount(), categoryId, userId);
    }

    /**
     * 从题库抽 need 道指定题型，不足部分用 AI 按知识点补足到 need。
     * 题库和 AI 都拿不到时接受已有数量（不会报错）。
     */
    private List<Question> pickWithAiTopup(String keyword, String type, Integer difficulty,
                                           int need, Long categoryId, Long userId) {
        List<Question> result = new ArrayList<>(pickByKeyword(keyword, type, difficulty, need));
        int miss = need - result.size();
        if (miss <= 0 || categoryId == null || !StringUtils.hasText(type)) {
            return result;
        }
        // 分批补足（AI 单次最多 10 道；失败即停，接受已有题目）
        while (miss > 0) {
            int batch = Math.min(miss, 10);
            try {
                com.xbmd.module.ai.dto.AiGenerateRequest ai = new com.xbmd.module.ai.dto.AiGenerateRequest();
                ai.setCategoryId(categoryId);
                ai.setType(type);
                ai.setDifficulty(difficulty == null ? 3 : difficulty);
                ai.setCount(batch);
                com.xbmd.module.ai.dto.AiGenerateResult gen = aiQuestionService.generate(ai, userId);
                List<Long> ids = gen.getQuestions().stream().map(QuestionVO::getId).toList();
                if (ids.isEmpty()) {
                    break;
                }
                result.addAll(questionMapper.selectBatchIds(ids));
                miss = need - result.size();
            } catch (BusinessException ex) {
                log.warn("AI 补题失败({}×{}): {}", type, batch, ex.getMessage());
                break;
            }
        }
        return result;
    }

    /** 按关键词抽指定题型指定数量（分类精确→模糊→题干模糊三级回退） */
    private List<Question> pickByKeyword(String keyword, String type, Integer difficulty, int count) {
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            // 1) 精确匹配分类名（如 Java 不会误命中 JavaScript）
            List<Question> exact = pickByCategoryName(kw, type, difficulty, count, false);
            if (exact != null && !exact.isEmpty()) {
                return exact;
            }
            // 2) 模糊匹配分类名（如"二叉树"命中"树"、"网络"命中"网络基础"）
            List<Question> like = pickByCategoryName(kw, type, difficulty, count, true);
            if (like != null && !like.isEmpty()) {
                return like;
            }
        }
        // 3) 回退：原逻辑按题干模糊匹配
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Question::getStatus, 1);
        if (StringUtils.hasText(type) && QuestionType.isValid(type)) {
            wrapper.eq(Question::getType, type);
        }
        if (difficulty != null) {
            wrapper.eq(Question::getDifficulty, difficulty);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Question::getStem, keyword.trim());
        }
        List<Question> candidates = questionMapper.selectList(wrapper);
        Collections.shuffle(candidates);
        return candidates.stream().limit(count).toList();
    }

    /** 按关键词精确/模糊匹配分类节点 */
    private List<com.xbmd.module.knowledge.entity.KnowledgeCategory> findCategoriesByKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return new ArrayList<>();
        }
        String kw = keyword.trim();
        List<com.xbmd.module.knowledge.entity.KnowledgeCategory> cats = categoryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                        com.xbmd.module.knowledge.entity.KnowledgeCategory>()
                        .eq(com.xbmd.module.knowledge.entity.KnowledgeCategory::getName, kw));
        if (cats.isEmpty()) {
            cats = categoryMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                            com.xbmd.module.knowledge.entity.KnowledgeCategory>()
                            .like(com.xbmd.module.knowledge.entity.KnowledgeCategory::getName, kw));
        }
        return cats;
    }

    /** 按分类名匹配收集知识点并抽题；无分类命中返回 null */
    private List<Question> pickByCategoryName(String kw, String type, Integer difficulty, int count, boolean fuzzy) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.xbmd.module.knowledge.entity.KnowledgeCategory> cw =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (fuzzy) {
            cw.like(com.xbmd.module.knowledge.entity.KnowledgeCategory::getName, kw);
        } else {
            cw.eq(com.xbmd.module.knowledge.entity.KnowledgeCategory::getName, kw);
        }
        List<com.xbmd.module.knowledge.entity.KnowledgeCategory> cats = categoryMapper.selectList(cw);
        if (cats.isEmpty()) {
            return null;
        }
        Set<Long> pointIds = new LinkedHashSet<>();
        for (com.xbmd.module.knowledge.entity.KnowledgeCategory c : cats) {
            if (c.getLevel() != null && c.getLevel() == 3) {
                pointIds.add(c.getId());
            } else {
                pointIds.addAll(categoryService.collectKnowledgePointIds(c.getId()));
            }
        }
        if (pointIds.isEmpty()) {
            return null;
        }
        List<QuestionVO> picked = questionService.randomPick(
                type, difficulty, new ArrayList<>(pointIds), count);
        if (picked.isEmpty()) {
            return null;
        }
        return picked.stream().map(vo -> questionMapper.selectById(vo.getId())).toList();
    }

    /** AI 模式：调用 AI 出题（失败自动兜底题库） */
    private List<Question> pickFromAi(CreatePracticeRequest request, Long userId) {
        if (request.getCategoryId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "AI 模式必须选择目标知识点");
        }
        if (!StringUtils.hasText(request.getType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "AI 模式必须指定题型");
        }
        AiGenerateRequest ai = new AiGenerateRequest();
        ai.setCategoryId(request.getCategoryId());
        ai.setType(request.getType());
        ai.setDifficulty(request.getDifficulty() == null ? 3 : request.getDifficulty());
        ai.setCount(Math.min(request.getCount(), 10));
        AiGenerateResult result = aiQuestionService.generate(ai, userId);
        if (result.getQuestions().isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = result.getQuestions().stream().map(QuestionVO::getId).toList();
        return questionMapper.selectBatchIds(ids);
    }

    /** 自适应模式：规则引擎——滑动窗口正确率 + 连对连错 + 知识点错误率 → 难度调整，冷启动默认中等难度 */
    private List<Question> pickAdaptive(CreatePracticeRequest request, Long userId) {
        int difficulty = computeAdaptiveDifficulty(userId);
        // 优先推送薄弱知识点题目（样本≥3 且正确率<60%）
        List<Long> weakPointIds = statsService.listWeakPointIds(userId);
        if (!weakPointIds.isEmpty()) {
            List<QuestionVO> picked = questionService.randomPick(
                    request.getType(), null, weakPointIds, request.getCount());
            if (picked.size() >= request.getCount()) {
                return picked.stream().map(vo -> questionMapper.selectById(vo.getId())).toList();
            }
        }
        // 回退：按规则难度抽题
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Question::getStatus, 1);
        wrapper.eq(Question::getDifficulty, difficulty);
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.like(Question::getStem, request.getKeyword().trim());
        }
        List<Question> candidates = questionMapper.selectList(wrapper);
        Collections.shuffle(candidates);
        return candidates.stream().limit(request.getCount()).toList();
    }

    /**
     * 自适应难度计算：近 20 题滑动窗口正确率 + 连对/连错计数。
     * 冷启动（无历史）默认中等难度 3。
     */
    private int computeAdaptiveDifficulty(Long userId) {
        List<AnswerRecord> recent = answerRecordMapper.selectList(
                new LambdaQueryWrapper<AnswerRecord>()
                        .eq(AnswerRecord::getUserId, userId)
                        .orderByDesc(AnswerRecord::getCreateTime)
                        .last("LIMIT 20"));
        if (recent.isEmpty()) {
            return 3;
        }
        int correctCount = (int) recent.stream()
                .filter(r -> r.getIsCorrect() != null && r.getIsCorrect() == 1).count();
        double rate = correctCount * 1.0 / recent.size();
        int difficulty = 3;
        // 连对/连错（从最新一条往前数）
        boolean lastCorrect = recent.get(0).getIsCorrect() != null && recent.get(0).getIsCorrect() == 1;
        int streak = 0;
        for (AnswerRecord r : recent) {
            boolean c = r.getIsCorrect() != null && r.getIsCorrect() == 1;
            if (c == lastCorrect) {
                streak++;
            } else {
                break;
            }
        }
        if (lastCorrect && streak >= 3) {
            difficulty += 1;
        }
        if (!lastCorrect && streak >= 2) {
            difficulty -= 1;
        }
        if (rate > 0.8) {
            difficulty += 1;
        }
        if (rate < 0.4) {
            difficulty -= 1;
        }
        return Math.max(1, Math.min(5, difficulty));
    }

    /** 错题重练模式：从错题本取题 */
    private List<Question> pickFromWrong(Long userId, int count) {
        List<WrongQuestion> wrongList = wrongQuestionMapper.selectList(
                new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId)
                        .eq(WrongQuestion::getStatus, 1));
        if (wrongList.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> questionIds = wrongList.stream().map(WrongQuestion::getQuestionId).toList();
        List<Question> questions = questionMapper.selectBatchIds(questionIds);
        Collections.shuffle(questions);
        return questions.stream().limit(count).toList();
    }

    /** 组题条件快照：保留原条件 + 题目 ID 列表 */
    private Map<String, Object> buildCondition(CreatePracticeRequest request, List<QuestionVO> questions) {
        Map<String, Object> condition = new LinkedHashMap<>();
        condition.put("mode", request.getMode());
        condition.put("keyword", request.getKeyword());
        condition.put("type", request.getType());
        condition.put("difficulty", request.getDifficulty());
        condition.put("categoryId", request.getCategoryId());
        condition.put("count", request.getCount());
        condition.put("questionIds", questions.stream().map(QuestionVO::getId).toList());
        return condition;
    }

    // ---------- 提交答案与判分 ----------

    @Override
    @Transactional
    public AnswerSubmitResult submitAnswer(Long practiceId, AnswerSubmitRequest request, Long userId) {
        PracticeRecord practice = requireOwnedPractice(practiceId, userId);
        if (practice.getStatus() != null && practice.getStatus() == STATUS_FINISHED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "练习已交卷，不能继续作答");
        }
        Question question = questionMapper.selectById(request.getQuestionId());
        if (question == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "题目不存在");
        }
        // 幂等：(practiceId, questionId) 已存在 → 直接返回已有结果
        AnswerRecord exist = answerRecordMapper.selectOne(
                new LambdaQueryWrapper<AnswerRecord>()
                        .eq(AnswerRecord::getPracticeId, practiceId)
                        .eq(AnswerRecord::getQuestionId, request.getQuestionId()));
        if (exist != null) {
            return toSubmitResult(exist, question);
        }

        AnswerRecord record = new AnswerRecord();
        record.setPracticeId(practiceId);
        record.setUserId(userId);
        record.setQuestionId(request.getQuestionId());
        record.setUserAnswer(request.getUserAnswer().trim());

        boolean subjective = QuestionType.SHORT.name().equals(question.getType());
        if (subjective) {
            // 主观题：先落库（isCorrect/score=null 表示待评测），后台异步 AI 评测回填，不阻塞作答
            answerRecordMapper.insert(record);
            aiAsyncScoreService.scoreSubjective(record.getId(), question, record.getUserAnswer(), userId);
        } else {
            boolean correct = judge(question, record.getUserAnswer());
            record.setIsCorrect(correct ? 1 : 0);
            record.setScore(correct ? SCORE_PER_QUESTION : 0);
            answerRecordMapper.insert(record);
        }
        // 答错自动收录错题本（去重由 wrong_question 唯一约束 + recordWrong 处理）
        // 说明：主观题此时 isCorrect 仍为 null（待评测），错题本收录由异步评分完成后统一处理
        if (record.getIsCorrect() != null && record.getIsCorrect() == 0) {
            wrongQuestionService.recordWrong(userId, question.getId(), record.getId());
        }
        // 更新知识点统计（学习画像）
        if (record.getIsCorrect() != null) {
            statsService.updateStats(userId, question.getId(), record.getIsCorrect() == 1);
        }
        return toSubmitResult(record, question);
    }

    @Override
    public PracticeDetailVO submitAll(Long practiceId, List<AnswerSubmitRequest> answers, Long userId) {
        // 注意：此处不能加 @Transactional —— 交卷会触发异步 AI 评分（@Async 线程回填 answer_record），
        // 若本方法持有事务，会对刚 insert 的 answer_record 行加锁，导致评分线程 UPDATE 被行锁阻塞、
        // awaitAll 等不到评分完成，结算读到未回填的 NULL（前端得分/对错/正确率与数据库不一致）。
        // 去掉事务后，每个 submitAnswer 各自短事务立即提交，评分线程可及时回填，awaitAll 能等到结果。
        PracticeRecord practice = requireOwnedPractice(practiceId, userId);
        if (practice.getStatus() != null && practice.getStatus() == STATUS_FINISHED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "练习已交卷");
        }
        // 逐题提交（幂等），未答的题不补
        for (AnswerSubmitRequest answer : answers) {
            submitAnswer(practiceId, answer, userId);
        }
        // 异步评分：等待该练习所有未评完的主观题补齐（带超时），保证成绩准确
        List<AnswerRecord> pendingRecords = answerRecordMapper.selectList(
                new LambdaQueryWrapper<AnswerRecord>()
                        .eq(AnswerRecord::getPracticeId, practiceId)
                        .isNull(AnswerRecord::getIsCorrect));
        aiScoreCoordinator.awaitAll(
                pendingRecords.stream().map(r -> "practice:" + r.getId()).toList(), 60000L);
        // 结算
        List<AnswerRecord> records = answerRecordMapper.selectList(
                new LambdaQueryWrapper<AnswerRecord>().eq(AnswerRecord::getPracticeId, practiceId));
        int correctCount = (int) records.stream()
                .filter(r -> r.getIsCorrect() != null && r.getIsCorrect() == 1).count();
        int score = records.stream().mapToInt(r -> r.getScore() == null ? 0 : r.getScore()).sum();

        practice.setStatus(STATUS_FINISHED);
        practice.setCorrectCount(correctCount);
        practice.setScore(score);
        practice.setFinishTime(LocalDateTime.now());
        practiceRecordMapper.updateById(practice);
        return detail(practiceId, userId);
    }

    // ---------- 详情 ----------

    @Override
    public PracticeDetailVO detail(Long practiceId, Long userId) {
        PracticeRecord practice = requireOwnedPractice(practiceId, userId);
        // 从条件快照还原题目列表
        List<Long> questionIds = parseQuestionIds(practice.getConditionJson());
        Map<Long, Question> questionMap = questionIds.isEmpty() ? Map.of()
                : questionMapper.selectBatchIds(questionIds).stream()
                        .collect(Collectors.toMap(Question::getId, q -> q));

        List<AnswerRecord> records = answerRecordMapper.selectList(
                new LambdaQueryWrapper<AnswerRecord>().eq(AnswerRecord::getPracticeId, practiceId));
        Map<Long, AnswerRecord> answerMap = records.stream()
                .collect(Collectors.toMap(AnswerRecord::getQuestionId, r -> r));
        // 评测结果按 answerId 批量加载
        Map<Long, EvaluationVO> evaluationMap = loadEvaluations(records);

        List<PracticeItemVO> items = new ArrayList<>();
        for (Long qid : questionIds) {
            Question question = questionMap.get(qid);
            if (question == null) {
                continue;
            }
            PracticeItemVO item = new PracticeItemVO();
            item.setQuestion(questionService.getDetail(qid));
            AnswerRecord record = answerMap.get(qid);
            if (record != null) {
                item.setAnswerRecordId(record.getId());
                item.setUserAnswer(record.getUserAnswer());
                item.setCorrect(record.getIsCorrect() == null ? null : record.getIsCorrect() == 1);
                item.setScore(record.getScore());
                item.setEvaluation(evaluationMap.get(record.getId()));
            }
            items.add(item);
        }

        PracticeDetailVO vo = new PracticeDetailVO();
        vo.setId(practice.getId());
        vo.setMode(practice.getMode());
        vo.setConditionJson(practice.getConditionJson());
        vo.setTotalCount(practice.getTotalCount());
        vo.setCorrectCount(practice.getCorrectCount());
        vo.setScore(practice.getScore());
        vo.setStatus(practice.getStatus());
        vo.setCreateTime(practice.getCreateTime());
        vo.setFinishTime(practice.getFinishTime());
        if (practice.getFinishTime() != null && practice.getCreateTime() != null) {
            vo.setDurationSeconds(Duration.between(practice.getCreateTime(), practice.getFinishTime()).getSeconds());
        }
        vo.setItems(items);
        return vo;
    }

    // ---------- 内部工具 ----------

    @Override
    public List<PracticeHistoryVO> history(Long userId) {
        List<PracticeRecord> records = practiceRecordMapper.selectList(
                new LambdaQueryWrapper<PracticeRecord>()
                        .eq(PracticeRecord::getUserId, userId)
                        .eq(PracticeRecord::getStatus, STATUS_FINISHED)
                        .orderByDesc(PracticeRecord::getCreateTime)
                        .last("LIMIT 100"));
        if (records.isEmpty()) {
            return new ArrayList<>();
        }
        // 批量聚合已答数
        List<Long> practiceIds = records.stream().map(PracticeRecord::getId).toList();
        List<AnswerRecord> allAnswers = answerRecordMapper.selectList(
                new LambdaQueryWrapper<AnswerRecord>().in(AnswerRecord::getPracticeId, practiceIds));
        Map<Long, Long> answeredMap = allAnswers.stream()
                .collect(Collectors.groupingBy(AnswerRecord::getPracticeId, Collectors.counting()));
        return records.stream().map(r -> {
            PracticeHistoryVO vo = new PracticeHistoryVO();
            vo.setId(r.getId());
            vo.setMode(r.getMode());
            vo.setTotalCount(r.getTotalCount());
            vo.setCorrectCount(r.getCorrectCount());
            vo.setScore(r.getScore());
            vo.setAnsweredCount((int) (long) answeredMap.getOrDefault(r.getId(), 0L));
            vo.setCreateTime(r.getCreateTime());
            vo.setFinishTime(r.getFinishTime());
            if (r.getFinishTime() != null && r.getCreateTime() != null) {
                vo.setDurationSeconds(Duration.between(r.getCreateTime(), r.getFinishTime()).getSeconds());
            }
            return vo;
        }).toList();
    }

    private PracticeRecord requireOwnedPractice(Long practiceId, Long userId) {
        PracticeRecord practice = practiceRecordMapper.selectById(practiceId);
        if (practice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "练习不存在");
        }
        if (!practice.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问他人的练习");
        }
        return practice;
    }

    /** 客观题判分：单选/判断直接比对；多选集合比对 */
    private boolean judge(Question question, String userAnswer) {
        String ua = userAnswer == null ? "" : userAnswer.trim();
        String answer = question.getAnswer() == null ? "" : question.getAnswer().trim();
        if (QuestionType.MULTIPLE.name().equals(question.getType())) {
            return splitSet(ua).equals(splitSet(answer));
        }
        return ua.equalsIgnoreCase(answer);
    }

    private Set<String> splitSet(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private AnswerSubmitResult toSubmitResult(AnswerRecord record, Question question) {
        AnswerSubmitResult result = new AnswerSubmitResult();
        result.setAnswerId(record.getId());
        result.setQuestionId(record.getQuestionId());
        result.setCorrect(record.getIsCorrect() == null ? null : record.getIsCorrect() == 1);
        result.setScore(record.getScore());
        if (QuestionType.SHORT.name().equals(question.getType())) {
            Map<Long, EvaluationVO> map = loadEvaluations(List.of(record));
            result.setEvaluation(map.get(record.getId()));
        }
        return result;
    }

    private Map<Long, EvaluationVO> loadEvaluations(List<AnswerRecord> records) {
        if (records.isEmpty()) {
            return Map.of();
        }
        List<Long> answerIds = records.stream().map(AnswerRecord::getId).toList();
        List<EvaluationResult> evaluations = evaluationResultMapper.selectList(
                new LambdaQueryWrapper<EvaluationResult>().in(EvaluationResult::getAnswerId, answerIds));
        Map<Long, EvaluationVO> map = new HashMap<>();
        for (EvaluationResult ev : evaluations) {
            EvaluationVO vo = new EvaluationVO();
            vo.setId(ev.getId());
            vo.setEvalType(ev.getEvalType());
            vo.setScore(ev.getScore());
            vo.setComment(ev.getComment());
            vo.setStatus(ev.getStatus());
            vo.setPointResults(fromPointResults(ev.getPointResultsJson()));
            map.put(ev.getAnswerId(), vo);
        }
        return map;
    }

    private List<com.xbmd.module.evaluation.dto.PointResultDTO> fromPointResults(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, com.xbmd.module.evaluation.dto.PointResultDTO.class));
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Long> parseQuestionIds(String conditionJson) {
        if (!StringUtils.hasText(conditionJson)) {
            return new ArrayList<>();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(conditionJson, Map.class);
            Object ids = map.get("questionIds");
            if (ids instanceof List<?> list) {
                return list.stream().map(o -> Long.valueOf(o.toString())).toList();
            }
        } catch (JsonProcessingException | NumberFormatException e) {
            log.warn("练习条件快照解析失败：{}", e.getMessage());
        }
        return new ArrayList<>();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "练习条件序列化失败");
        }
    }
}
