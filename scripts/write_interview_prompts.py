# -*- coding: utf-8 -*-
"""写入 AI 面试的 4 个 prompt 模板（utf-8）"""
import io

base = r"D:\Codes\ai-interview-coach\backend\src\main\resources\ai\prompts"

parse = """你是资深技术面试官。请从下面的简历文本中提取求职者的技术栈与项目经历。

要求：
1. techStack：数组，列出简历中出现的所有技术关键词（如 Java、Spring Boot、MySQL、Redis、Vue3 等），最多 12 个。
2. projects：数组，每项 {name: 项目名, description: 一句话说明项目做什么、用到什么技术}，最多 5 个。
3. 简历中没有的信息不要编造。

只输出 JSON 对象，不要输出其他文字，格式：
{"techStack":["Java","Spring Boot"],"projects":[{"name":"项目名","description":"项目说明"}]}

简历文本：
${resume}
"""

generate = """你是资深技术面试官，正在一对一面试一位求职者。请结合他的简历（技术栈与项目经历），生成 10 道由浅入深的面试题，覆盖：技术栈核心概念、项目细节追问、场景设计题。

要求：
1. 题目必须紧扣简历中的技术栈和项目，不要问简历之外的技术。
2. 每题满分 10 分，评分点 2-4 个，所有评分点分数之和必须等于 10。
3. referenceAnswer 给出面试官期望的回答要点，80-200 字。
4. question 要像真实面试提问，具体、可回答，不要泛泛而谈。
5. 只输出 JSON 数组，不要输出其他文字。

格式：
[{"question":"面试题","scorePoints":[{"point":"评分点描述","score":3}],"referenceAnswer":"参考回答要点"}]

简历技术栈：${techStack}
简历项目：${projects}
"""

score = """你是严格的面试评分官。根据面试题、评分点和面试者的回答，给出得分与简短评语。

要求：
1. score 为 0-10 的整数，按评分点达成度给分，答得越好分数越高。
2. comment 30-80 字，指出回答的优点和不足。
3. 回答为空或完全不相关时给 0-2 分。
4. 只输出 JSON 对象，不要输出其他文字。

格式：{"score":7,"comment":"评语"}

面试题：${question}
评分点：${scorePoints}
参考答案要点：${referenceAnswer}
面试者回答：${userAnswer}
"""

summary = """你是资深技术面试官。根据求职者 10 道面试题的得分与评语，给出总体评价和改进建议。

要求：
1. summary：80-150 字，概括求职者的整体表现、突出优势与明显短板。
2. suggestion：80-150 字，给出 3 条具体可执行的提升建议，用分号分隔。
3. 只输出 JSON 对象，不要输出其他文字。

格式：{"summary":"总体评价","suggestion":"建议1；建议2；建议3"}

面试得分明细：
${scoreDetails}
"""

files = {
    "INTERVIEW_PARSE.txt": parse,
    "INTERVIEW_GENERATE.txt": generate,
    "INTERVIEW_SCORE.txt": score,
    "INTERVIEW_SUMMARY.txt": summary,
}
for name, content in files.items():
    with io.open(base + "\\" + name, "w", encoding="utf-8") as f:
        f.write(content)
    print("written:", name)
