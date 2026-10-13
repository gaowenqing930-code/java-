# -*- coding: utf-8 -*-
import io

p = r"D:\Codes\ai-interview-coach\backend\src\main\java\com\xbmd\module\interview\service\impl\InterviewServiceImpl.java"
s = io.open(p, encoding="utf-8").read()
old = '                log.warn("生成面试题第 {} 次尝试异常：{}", attempt, e.getMessage());'
new = '                log.warn("生成面试题第 {} 次尝试异常：{}", attempt, e.getMessage(), e);'
assert old in s, "pattern not found"
s = s.replace(old, new)
io.open(p, "w", encoding="utf-8", newline="").write(s)
print("patched")
