# -*- coding: utf-8 -*-
"""直接调用方舟接口，复现面试题生成失败原因"""
import json
import urllib.error
import urllib.request
import os

KEY = os.environ.get("VOLC_API_KEY", "")
EP = "ep-20260925174043-zpql5"

prompt = """你是资深技术面试官。请生成 3 道由浅入深的 Java 面试题。
要求：每题满分 10 分，评分点 2-4 个，分数之和必须等于 10。
只输出 JSON 数组：[{"question":"...","scorePoints":[{"point":"...","score":3}],"referenceAnswer":"..."}]
"""

for mt in (2048, 4096):
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
    try:
        with urllib.request.urlopen(req, timeout=180) as x:
            d = json.loads(x.read().decode())
            c = d["choices"][0]["message"]["content"]
            print("max_tokens", mt, "-> OK, len:", len(c))
            print("  head:", c[:120].replace("\n", " "))
    except urllib.error.HTTPError as e:
        print("max_tokens", mt, "-> HTTP", e.code, e.read().decode()[:300])
    except Exception as e:
        print("max_tokens", mt, "-> ERR", str(e)[:200])
