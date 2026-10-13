# -*- coding: utf-8 -*-
"""直测 AI 简历解析接口耗时/失败"""
import json
import time
import urllib.error
import urllib.request

BASE = "http://localhost:8080/api"


def call(m, p, b=None, t=None, to=240):
    d = json.dumps(b).encode() if b is not None else None
    req = urllib.request.Request(BASE + p, data=d, method=m)
    req.add_header("Content-Type", "application/json")
    if t:
        req.add_header("Authorization", "Bearer " + t)
    try:
        t0 = time.time()
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
print("login:", st, "time:", t, "s")
tok = r["data"]["token"]

resume = "技术栈：Java、Spring Boot、Spring MVC、MyBatis-Plus、MySQL、Redis、Vue3。项目：AI智能校园平台助手，负责JWT鉴权与Redis缓存。"
st, r, t = call("POST", "/interview/sessions", {"resumeText": resume}, t=tok)
print("parse:", st, "time:", t, "s")
if isinstance(r, dict):
    print("  code:", r.get("code"), "msg:", (r.get("message") or "")[:120])
    d = r.get("data") or {}
    print("  techStack:", d.get("techStack"))
else:
    print("  raw:", str(r)[:200])
