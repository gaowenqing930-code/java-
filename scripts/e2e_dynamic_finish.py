# -*- coding: utf-8 -*-
"""E2E：动态题数面试 —— start -> 答 1 题 -> 答 2 题 -> 主动 finish -> 验证百分比总分与明细"""
import json
import time
import urllib.error
import urllib.request

BASE = "http://localhost:8080/api"


def call(m, p, b=None, t=None, to=320):
    d = json.dumps(b).encode() if b is not None else None
    req = urllib.request.Request(BASE + p, data=d, method=m)
    req.add_header("Content-Type", "application/json")
    if t:
        req.add_header("Authorization", "Bearer " + t)
    t0 = time.time()
    try:
        with urllib.request.urlopen(req, timeout=to) as x:
            return x.status, json.loads(x.read().decode()), round(time.time() - t0, 1)
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode()), round(time.time() - t0, 1)
        except Exception:
            return e.code, str(e), round(time.time() - t0, 1)
    except Exception as e:
        return -1, str(e), round(time.time() - t0, 1)


st, r, t = call("POST", "/auth/login", {"username": "admin", "password": "admin123"})
tok = r["data"]["token"]
print("login ok")

resume = "技术栈：Java、Spring Boot、MyBatis-Plus、MySQL、Redis、Vue3。项目：AI智能校园平台助手，负责JWT鉴权与Redis缓存。"
st, r, t = call("POST", "/interview/sessions", {"resumeText": resume}, t=tok)
sid = r["data"]["sessionId"]
print("session", sid, "parse", t, "s")

st, r, t = call("POST", f"/interview/sessions/{sid}/start", {}, t=tok)
q1 = r["data"]
print("start -> Q", q1["no"], "suggestFinish=", q1.get("suggestFinish"), "|", q1["question"][:40], "...", t, "s")

# 答第 1 题
st, r, t = call("POST", f"/interview/sessions/{sid}/answer", {"no": 1, "answer": "我会用 Spring Boot 搭建 RESTful 接口，JWT 做登录鉴权，Redis 缓存热点数据，MyBatis-Plus 做数据访问。"}, t=tok)
q2 = r["data"].get("currentQuestion")
print("answer1 -> Q", q2["no"] if q2 else None, "finished=", r["data"]["finished"], "| suggestFinish=", (q2 or {}).get("suggestFinish"), t, "s")

# 答第 2 题
st, r, t = call("POST", f"/interview/sessions/{sid}/answer", {"no": 2, "answer": "Redis 缓存穿透我会用布隆过滤器，缓存击穿用互斥锁和热点预加载，缓存雪崩通过过期时间随机化解决。"}, t=tok)
q3 = r["data"].get("currentQuestion")
print("answer2 -> Q", q3["no"] if q3 else None, "finished=", r["data"]["finished"], "| suggestFinish=", (q3 or {}).get("suggestFinish"), "reason=", (q3 or {}).get("finishReason"), t, "s")

# 主动结束
st, r, t = call("POST", f"/interview/sessions/{sid}/finish", {}, t=tok)
res = r["data"]
print("finish -> code", st, "totalScore=", res.get("totalScore"), "t=", t, "s")
print("  summary:", (res.get("summary") or "")[:80])
print("  questions:", len(res.get("questions") or []), "个")
for q in res.get("questions") or []:
    print("   Q", q["no"], "score", q["aiScore"], "status", q["status"])
