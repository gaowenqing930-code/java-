# -*- coding: utf-8 -*-
import pathlib

p = pathlib.Path(r"D:\Codes\ai-interview-coach\backend\src\main\java\com\xbmd\module\interview\service\impl\InterviewServiceImpl.java")
s = p.read_text(encoding="utf-8")

old = "        List<InterviewQuestion> scored = listQuestions(sessionId);\n        int sum = scored.stream().mapToInt(q -> q.getAiScore() == null ? 0 : q.getAiScore()).sum();"
new = "        // scoreOne 已就地更新 q 对象（含失败降级），直接使用 answered\n        List<InterviewQuestion> scored = answered;\n        int sum = scored.stream().mapToInt(q -> q.getAiScore() == null ? 0 : q.getAiScore()).sum();"
assert old in s, "not found"
s = s.replace(old, new)
p.write_text(s, encoding="utf-8", newline="")
print("OK")
