# -*- coding: utf-8 -*-
"""AI 面试 E2E：创建会话 -> start 第 1 题 -> 逐题作答(对话式出题) -> 第 10 题统一评分 -> 结果"""
import json
import time
import urllib.error
import urllib.request

BASE = "http://localhost:8080/api"


def call(m, p, b=None, t=None, to=360):
    d = json.dumps(b).encode() if b is not None else None
    req = urllib.request.Request(BASE + p, data=d, method=m)
    req.add_header("Content-Type", "application/json")
    if t:
        req.add_header("Authorization", "Bearer " + t)
    try:
        with urllib.request.urlopen(req, timeout=to) as x:
            return x.status, json.loads(x.read().decode())
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode())
        except Exception:
            return e.code, str(e)
    except Exception as e:
        return -1, str(e)


st, r = call("POST", "/auth/login", {"username": "admin", "password": "admin123"})
tok = r["data"]["token"]
print("login ok")

resume = (
    "技术栈：Java、Spring Boot、Spring MVC、MyBatis-Plus、MySQL、Redis、Vue3、JavaScript、Git、Linux、JWT、Element Plus。\n"
    "项目1：AI 智能校园平台助手，Spring Boot + Vue3，负责后端接口与 JWT 鉴权、Redis 缓存热点数据。\n"
    "项目2：二手交易平台，商品发布浏览搜索，Redis 搜索缓存，MySQL 订单表设计。\n"
    "项目3：Web 智能交易平台，商品管理、购物车、下单支付流程模拟。"
)
st, r = call("POST", "/interview/sessions", {"resumeText": resume}, t=tok)
d = r.get("data") or {}
sid = d.get("sessionId")
print("session:", sid, "| techStack:", d.get("techStack"))
print("create time:", round(r.get("costMs") or 0, 0), "ms")

t0 = time.time()
st, r = call("POST", f"/interview/sessions/{sid}/start", None, t=tok, to=150)
if isinstance(r, dict) and r.get("code") == 200:
    q = r.get("data") or {}
    print("start OK | Q1:", (q.get("question") or "")[:70])
else:
    print("start FAIL:", r)
    raise SystemExit(1)
print("start time:", round(time.time() - t0, 1), "s")

answered = []
for no in range(1, 11):
    ans = f"这是第 {no} 题的模拟回答：结合 Spring Boot 和 MySQL 实践，说明原理并给出实现思路，尽量完整覆盖要点，长度适中便于评分。"
    t0 = time.time()
    st, r = call("POST", f"/interview/sessions/{sid}/answer", {"no": no, "answer": ans}, t=tok, to=330)
    cost = round(time.time() - t0, 1)
    if not isinstance(r, dict):
        print(f"answer{no} FAIL:", st, r)
        raise SystemExit(1)
    body = r.get("data") or {}
    if r.get("code") != 200:
        print(f"answer{no} code:", r.get("code"), r.get("message"))
        raise SystemExit(1)
    if body.get("finished"):
        res = body.get("result") or {}
        print(f"answer{no} -> FINISHED ({cost}s) totalScore:", res.get("totalScore"))
        print("  summary:", (res.get("summary") or "")[:60])
        print("  suggestion:", (res.get("suggestion") or "")[:60])
        print("  questions:", len(res.get("questions") or []))
        for q in res.get("questions") or []:
            print(f"   Q{q.get('no')}: score={q.get('aiScore')} comment={(q.get('aiComment') or '')[:40]}")
    else:
        nq = body.get("currentQuestion") or {}
        print(f"answer{no} -> next Q{no+1} ({cost}s):", (nq.get("question") or "")[:60])
    answered.append(no)

print("ALL DONE")
