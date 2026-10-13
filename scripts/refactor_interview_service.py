# -*- coding: utf-8 -*-
"""重构 InterviewServiceImpl：逐题生成 + 全部答完统一评分"""
import io

p = r"D:\Codes\ai-interview-coach\backend\src\main\java\com\xbmd\module\interview\service\impl\InterviewServiceImpl.java"
s = io.open(p, encoding="utf-8").read()

# ---------- 1) 替换 start 段（含旧批量生成逻辑） ----------
old_start = """    // ---------- 开始面试：生成 10 题 ----------

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
                log.warn("生成面试题第 {} 次尝试异常：{}", attempt, e.getMessage(), e);
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
"""

new_start = """    // ---------- 开始面试：生成第 1 题 ----------

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
                        .replace("${nextNo}", String.valueOf(nextNo))
                        .replace("${questionCount}", String.valueOf(QUESTION_COUNT));
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
            sb.append("第 ").append(q.getQuestionNo()).append(" 题：").append(nvl(q.getQuestion())).append("\\n");
            sb.append("面试者回答：").append(truncate(nvl(q.getUserAnswer()), 300)).append("\\n");
        }
        return sb.toString();
    }
"""

assert old_start in s, "start block not found"
s = s.replace(old_start, new_start)

# ---------- 2) 替换 answer：保存答案 -> 生成下一题 / 全部答完统一评分 ----------
old_answer = """    @Override
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
"""

new_answer = """    @Override
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

        // 10 题全部答完：统一逐题评分 + 总结评价
        for (InterviewQuestion q : listQuestions(sessionId)) {
            if (!Q_SCORED.equals(q.getStatus())) {
                scoreOne(q);
                questionMapper.updateById(q);
            }
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
    }
"""

assert old_answer in s, "answer block not found"
s = s.replace(old_answer, new_answer)

io.open(p, "w", encoding="utf-8", newline="").write(s)
print("InterviewServiceImpl refactored")
