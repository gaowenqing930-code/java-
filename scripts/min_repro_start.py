# -*- coding: utf-8 -*-
"""最小复现：创建会话 + start，看堆栈"""
import json
import time
import urllib.error
import urllib.request

BASE = "http://localhost:8080/api"


def call(m, p, b=None, t=None, to=600):
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
resume = "技术栈：Java、Spring Boot、MySQL、Redis、Vue3。项目：AI 智能校园平台（Spring Boot + Vue3）。"
st, r = call("POST", "/interview/sessions", {"resumeText": resume}, t=tok)
sid = (r.get("data") or {}).get("sessionId")
print("session:", sid)
t0 = time.time()
st, r = call("POST", f"/interview/sessions/{sid}/start", None, t=tok, to=600)
print("start:", st, r if not isinstance(r, dict) else (r.get("code"), (r.get("message") or "")[:100]))
print("elapsed:", round(time.time() - t0, 1), "s")
