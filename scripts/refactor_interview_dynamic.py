# -*- coding: utf-8 -*-
"""AI 面试改版：动态题数（AI 建议结束 + 用户主动结束 + 兜底 15 题）+ 百分比总分"""
import pathlib

BASE = pathlib.Path(r"D:\Codes\ai-interview-coach\backend\src\main\java\com\xbmd\module\interview")


def patch(path, pairs):
    p = BASE / path
    s = p.read_text(encoding="utf-8")
    for old, new in pairs:
        assert old in s, f"[{path}] NOT FOUND: {old[:60]!r}"
        s = s.replace(old, new)
    p.write_text(s, encoding="utf-8", newline="")
    print("patched", path)


# ---------- 1. InterviewGeneratedQuestion：加 suggestFinish/finishReason ----------
patch("dto/InterviewGeneratedQuestion.java", [(
    """    private String referenceAnswer;
}""",
    """    private String referenceAnswer;

    /** AI 判断：当前已充分测评简历技术栈，建议结束面试 */
    private Boolean suggestFinish;

    /** 建议结束的原因（suggestFinish=true 时给出） */
    private String finishReason;
}""",
)])

# ---------- 2. InterviewQuestionVO：加 suggestFinish/finishReason ----------
patch("dto/InterviewQuestionVO.java", [(
    """    private String referenceAnswer;
}""",
    """    private String referenceAnswer;

    /** AI 建议是否可结束面试（仅当前题返回时有效） */
    private Boolean suggestFinish;

    /** 建议结束的原因 */
    private String finishReason;
}""",
)])

# ---------- 3. InterviewService 接口：加 finish ----------
patch("service/InterviewService.java", [(
    """    /** 提交当前题作答，AI 评分；最后一题时生成总结并结束 */
    InterviewAnswerVO answer(Long sessionId, InterviewAnswerRequest request, Long userId);""",
    """    /** 提交当前题作答，保存答案后 AI 引导式生成下一题；达到兜底上限时自动评分结束 */
    InterviewAnswerVO answer(Long sessionId, InterviewAnswerRequest request, Long userId);

    /** 用户主动结束面试：统一评分已答题目并生成总结（已结束会话幂等返回结果） */
    InterviewResultVO finish(Long sessionId, Long userId);""",
)], )

# ---------- 4. InterviewController：加 /finish ----------
patch("controller/InterviewController.java", [(
    """    /** 获取面试结果 */
    @GetMapping("/sessions/{id}/result")""",
    """    /** 用户主动结束面试：统一评分已答题目并生成总结 */
    @PostMapping("/sessions/{id}/finish")
    public Result<InterviewResultVO> finish(@PathVariable Long id) {
        return Result.success(interviewService.finish(id, UserContext.getUserId()));
    }

    /** 获取面试结果 */
    @GetMapping("/sessions/{id}/result")""",
)])

# ---------- 5. InterviewServiceImpl：核心重构 ----------
impl = BASE / "service/impl/InterviewServiceImpl.java"
s = impl.read_text(encoding="utf-8")

# 5.1 常量：固定 10 题 -> 兜底上限 15 题
old_const = """    private static final String Q_PENDING = "PENDING";
    private static final String Q_SCORED = "SCORED";
    private static final int QUESTION_COUNT = 10;"""
new_const = """    private static final String Q_PENDING = "PENDING";
    private static final String Q_SCORED = "SCORED";
    /** 面试题兜底上限：题数动态由 AI 判断，达上限自动结束，防止无限追问 */
    private static final int MAX_QUESTIONS = 15;"""
assert old_const in s
s = s.replace(old_const, new_const)

# 5.2 类注释
old_doc = "/**\n * AI 智能面试服务实现：简历解析 - 生成 10 题 - 逐题评分 - 总结评价\n */"
new_doc = "/**\n * AI 智能面试服务实现：简历解析 - 对话式动态出题（AI 引导 + 用户可主动结束） - 逐题评分 - 总结评价\n */"
assert old_doc in s
s = s.replace(old_doc, new_doc)

# 5.3 generateOne：去掉固定 questionCount 替换
old_gen = """                String prompt = loadTemplate("INTERVIEW_GENERATE.txt")
                        .replace("${techStack}", nvl(session.getTechStack()))
                        .replace("${projects}", nvl(session.getProjects()))
                        .replace("${history}", history)
                        .replace("${nextNo}", String.valueOf(nextNo))
                        .replace("${questionCount}", String.valueOf(QUESTION_COUNT));"""
new_gen = """                String prompt = loadTemplate("INTERVIEW_GENERATE.txt")
                        .replace("${techStack}", nvl(session.getTechStack()))
                        .replace("${projects}", nvl(session.getProjects()))
                        .replace("${history}", history)
                        .replace("${nextNo}", String.valueOf(nextNo));"""
assert old_gen in s
s = s.replace(old_gen, new_gen)

# 5.4 answer：动态出题 + 兜底自动结束
old_answer_tail = """        cur.setUserAnswer(request.getAnswer().trim());
        questionMapper.updateById(cur);

        InterviewAnswerVO vo = new InterviewAnswerVO();
        if (expectNo < QUESTION_COUNT) {
            // 尚未答完：保存答案后生成下一题（对话式出题）
            InterviewGeneratedQuestion g = generateOne(session, listQuestions(sessionId), expectNo + 1);
            InterviewQuestion next = buildQuestion(sessionId, expectNo + 1, g);
            questionMapper.insert(next);
            vo.setFinished(false);
            vo.setCurrentQuestion(toVO(next));
            return vo;
        }

        // 10 题全部答完：统一逐题评分（并发加速）+ 总结评价
        List<InterviewQuestion> pending = listQuestions(sessionId);
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(3, pending.size()));
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (InterviewQuestion q : pending) {
                futures.add(pool.submit(() -> {
                    scoreOne(q);
                    questionMapper.updateById(q);
                    return null;
                }));
            }
            for (Future<?> f : futures) {
                f.get();
            }
        } catch (Exception e) {
            log.warn("并发评分异常：{}", e.getMessage(), e);
        } finally {
            pool.shutdown();
        }
        List<InterviewQuestion> scored = listQuestions(sessionId);
        int total = scored.stream().mapToInt(q -> q.getAiScore() == null ? 0 : q.getAiScore()).sum();
        ScoreSummary summary = summarize(scored);
        session.setStatus(STATUS_FINISHED);
        session.setTotalScore(total);
        session.setSummary(summary.getSummary());
        session.setSuggestion(summary.getSuggestion());
        session.setFinishTime(LocalDateTime.now());
        sessionMapper.updateById(session);
        vo.setFinished(true);
        vo.setResult(toResultVO(session, scored));
        return vo;
    }"""
new_answer_tail = """        cur.setUserAnswer(request.getAnswer().trim());
        questionMapper.updateById(cur);

        InterviewAnswerVO vo = new InterviewAnswerVO();
        if (expectNo >= MAX_QUESTIONS) {
            // 已达兜底上限：本题答完自动评分结束
            vo.setFinished(true);
            vo.setResult(doFinish(session, listQuestions(sessionId)));
            return vo;
        }
        // 对话式出题：AI 结合上一题回答引导式生成下一题，并判断是否建议结束
        InterviewGeneratedQuestion g = generateOne(session, listQuestions(sessionId), expectNo + 1);
        InterviewQuestion next = buildQuestion(sessionId, expectNo + 1, g);
        questionMapper.insert(next);
        vo.setFinished(false);
        vo.setCurrentQuestion(withSuggest(toVO(next), g));
        return vo;
    }

    /** 用户主动结束面试：统一评分已答题目并生成总结 */
    private InterviewResultVO doFinish(InterviewSession session, List<InterviewQuestion> all) {
        // 只评已答的题，未答题删除（不进入结果明细）
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
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(3, answered.size()));
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (InterviewQuestion q : answered) {
                futures.add(pool.submit(() -> {
                    scoreOne(q);
                    questionMapper.updateById(q);
                    return null;
                }));
            }
            for (Future<?> f : futures) {
                f.get();
            }
        } catch (Exception e) {
            log.warn("并发评分异常：{}", e.getMessage(), e);
        } finally {
            pool.shutdown();
        }
        List<InterviewQuestion> scored = listQuestions(sessionId);
        int sum = scored.stream().mapToInt(q -> q.getAiScore() == null ? 0 : q.getAiScore()).sum();
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
    }"""
assert old_answer_tail in s
s = s.replace(old_answer_tail, new_answer_tail)

# 5.5 新增 finish 方法（插在 result 方法前）
old_result = """    // ---------- 结果 ----------

    @Override
    public InterviewResultVO result(Long sessionId, Long userId) {"""
new_result = """    // ---------- 结束 / 结果 ----------

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
    public InterviewResultVO result(Long sessionId, Long userId) {"""
assert old_result in s
s = s.replace(old_result, new_result)

# 5.6 toVO 旁新增 withSuggest 辅助
old_tovo = """    private InterviewQuestionVO toVO(InterviewQuestion q) {
        InterviewQuestionVO vo = new InterviewQuestionVO();"""
new_tovo = """    private InterviewQuestionVO withSuggest(InterviewQuestionVO vo, InterviewGeneratedQuestion g) {
        if (g != null) {
            vo.setSuggestFinish(Boolean.TRUE.equals(g.getSuggestFinish()));
            vo.setFinishReason(g.getFinishReason());
        }
        return vo;
    }

    private InterviewQuestionVO toVO(InterviewQuestion q) {
        InterviewQuestionVO vo = new InterviewQuestionVO();"""
assert old_tovo in s
s = s.replace(old_tovo, new_tovo)

impl.write_text(s, encoding="utf-8", newline="")
print("patched InterviewServiceImpl.java")
