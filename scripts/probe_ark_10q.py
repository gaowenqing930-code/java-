# -*- coding: utf-8 -*-
"""直测方舟：生成 10 道面试题的真实耗时"""
import json
import time
import urllib.error
import urllib.request
import os

KEY = os.environ.get("VOLC_API_KEY", "")
EP = "ep-20260925174043-zpql5"

prompt = """你是资深技术面试官，正在一对一面试一位求职者。请结合他的简历（技术栈与项目经历），生成 10 道由浅入深的面试题，覆盖：技术栈核心概念、项目细节追问、场景设计题。

要求：
1. 题目必须紧扣简历中的技术栈和项目，不要问简历之外的技术。
2. 每题满分 10 分，评分点 2-4 个，所有评分点分数之和必须等于 10。
3. referenceAnswer 给出面试官期望的回答要点，80-200 字。
4. question 要像真实面试提问，具体、可回答，不要泛泛而谈。
5. 只输出 JSON 数组，不要输出其他文字。

格式：
[{"question":"面试题","scorePoints":[{"point":"评分点描述","score":3}],"referenceAnswer":"参考回答要点"}]

简历技术栈：Java,Spring Boot,Spring MVC,MyBatis-Plus,MySQL,Redis,Vue3,JavaScript,Git,Linux,JWT,Element Plus
简历项目：[{"name":"AI 智能校园平台助手","description":"Spring Boot + Vue3，AI 问答助手、JWT 鉴权、Redis 缓存"},{"name":"二手交易平台","description":"商品发布浏览搜索，Redis 搜索缓存，MySQL 订单分表"},{"name":"Web 智能交易平台","description":"商品管理、购物车、下单支付流程模拟"}]
"""

for mt in (4096,):
    body = {
        "model": EP,
        "messages": [{"role": "user", "content": prompt}],
        "max_tokens": mt,
        "temperature": 0.7,
    }
    req = urllib.request.Request(
        "https://ark.cn-beijing.volces.com/api/v3/chat/completions",
        data=json.dumps(body).encode(),
        headers={"Content-Type": "application/json", "Authorization": "Bearer " + KEY},
    )
    t0 = time.time()
    try:
        with urllib.request.urlopen(req, timeout=600) as x:
            d = json.loads(x.read().decode())
            c = d["choices"][0]["message"]["content"]
            print("OK elapsed:", round(time.time() - t0, 1), "s len:", len(c))
            print("finish_reason:", d["choices"][0].get("finish_reason"))
            print("usage:", d.get("usage"))
    except urllib.error.HTTPError as e:
        print("HTTP", e.code, e.read().decode()[:300], "elapsed:", round(time.time() - t0, 1), "s")
    except Exception as e:
        print("ERR", str(e)[:200], "elapsed:", round(time.time() - t0, 1), "s")
