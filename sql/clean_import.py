# -*- coding: utf-8 -*-
"""清洗牛客采集数据并生成入库 SQL（question + question_knowledge）"""
import io, json, hashlib, re, sys

RAW = r"D:\Codes\ai-interview-coach\sql\raw_nowcoder.jsonl"
OUT_Q = r"D:\Codes\ai-interview-coach\sql\import_questions.sql"
OUT_L = r"D:\Codes\ai-interview-coach\sql\import_links.sql"
OUT_STAT = r"D:\Codes\ai-interview-coach\sql\import_stats.txt"

# point 词条 -> category_id（按长度降序匹配）
POINT_MAP = [
    ("概率论与数理统计", 451), ("编译和体系结构", 361), ("大模型概念", 481), ("提示词工程", 411),
    ("机器学习", 431), ("深度学习", 441), ("数据挖掘", 461), ("计算机视觉", 471),
    ("数理统计", 451), ("操作系统", 313), ("网络基础", 323), ("数据库", 334),
    ("设计模式", 351), ("软件工程", 371), ("ZooKeeper", 551), ("Javascript", 141),
    ("动态规划", 222), ("复杂度", 225), ("递归", 221), ("排序", 224), ("贪心", 223),
    ("数组", 211), ("字符串", 212), ("链表", 213), ("栈", 214), ("队列", 215),
    ("树", 216), ("哈希", 217), ("查找", 2110), ("图", 218), ("堆", 219),
    ("Redis", 511), ("Kafka", 521), ("Hadoop", 531), ("Hive", 541),
    ("Kotlin", 171), ("Swift", 181), ("HTML", 191), ("CSS", 191), ("C语言", 161),
    ("Go", 151), ("C++", 131), ("Python", 121), ("Java", 116), ("SQL", 331),
    ("Linux", 341), ("React", 611), ("Spring", 621), ("Vue", 631),
    ("Agent", 4101), ("RAG", 421), ("微调", 491),
    # 行测等非计算机内容，映射为 None 丢弃
    ("判断推理", None), ("常识判断", None), ("数学运算", None), ("资料分析", None),
]
POINT_MAP.sort(key=lambda x: len(x[0]), reverse=True)


def split_points(s):
    """把合并标签拆成已知 point 列表"""
    found = []
    rest = s or ""
    i = 0
    while i < len(rest):
        matched = False
        for word, cid in POINT_MAP:
            if rest.startswith(word, i):
                if cid is not None:
                    found.append(cid)
                i += len(word)
                matched = True
                break
        if not matched:
            i += 1
    return list(dict.fromkeys(found))  # 去重保序


def fallback_cat(stem, point):
    """point 为空时的题干关键词兜底"""
    s = (point or "") + " " + (stem or "")
    s = s.lower()
    if "swift" in s or "xcode" in s or "playground" in s:
        return [171]
    if "countuniques" in s:
        return [141]
    if "static int" in s or "int main" in s or "c语言" in s:
        return [161]
    return []


def clean_text(t):
    if not t:
        return ""
    t = t.replace("\xa0", " ").replace("\u3000", " ")
    return re.sub(r"[ \t]+", " ", t).strip()


def fingerprint(stem, category_ids, difficulty=2):
    norm = clean_text(stem).lower()
    cats = ",".join(str(x) for x in sorted(set(category_ids)))
    seed = norm + "|" + cats + "|" + str(difficulty)
    return hashlib.sha256(seed.encode("utf-8")).hexdigest()


def esc(s):
    if s is None:
        return "NULL"
    return "'" + str(s).replace("\\", "\\\\").replace("'", "''") + "'"


def main():
    lines = [json.loads(x) for x in io.open(RAW, encoding="utf-8").read().strip().split("\n")]
    # 去重：按清洗后 stem
    seen = {}
    for q in lines:
        stem = clean_text(q.get("stem", ""))
        if not stem:
            continue
        key = re.sub(r"\s+", "", stem)
        if key not in seen:
            seen[key] = q
    items = list(seen.values())
    print("unique after dedupe:", len(items), file=sys.stderr)

    q_rows, link_rows, skipped = [], [], []
    stats = {}
    for q in items:
        stem = clean_text(q.get("stem"))
        opts = [{"key": o.get("key", ""), "text": clean_text(o.get("text"))} for o in (q.get("options") or [])]
        answer = clean_text(q.get("answer", "")).replace("：", "")
        analysis = clean_text(q.get("analysis"))
        raw_type = (q.get("type") or "").strip()
        # 题型推断：答案多字母 -> 多选
        if raw_type == "多选题" or (raw_type == "" and len(re.sub(r"[^A-Za-z]", "", answer)) > 1):
            qtype = "MULTIPLE"
        else:
            qtype = "SINGLE"
        cat_ids = split_points(q.get("point", ""))
        if not cat_ids:
            cat_ids = fallback_cat(stem, q.get("point", ""))
        if not cat_ids:
            skipped.append(stem[:50])
            continue
        fp = fingerprint(stem, cat_ids, 2)
        stats[qtype] = stats.get(qtype, 0) + 1
        q_rows.append(
            "(%s,%s,%s,%s,%s,NULL,%s,'MANUAL',%s,1)"
            % (esc(qtype), 2, esc(stem), esc(json.dumps(opts, ensure_ascii=False)),
               esc(answer), esc(analysis), esc(fp))
        )
        for cid in cat_ids:
            link_rows.append((fp, cid))

    # 生成 question 插入 SQL
    with io.open(OUT_Q, "w", encoding="utf-8", newline="\n") as f:
        f.write("USE ai_interview_coach;\n")
        f.write("INSERT IGNORE INTO question (type,difficulty,stem,options_json,answer,score_points_json,analysis,source,fingerprint,status) VALUES\n")
        f.write(",\n".join(q_rows))
        f.write(";\n")

    # 生成 question_knowledge 插入 SQL（用子查询按 fingerprint 找 id）
    with io.open(OUT_L, "w", encoding="utf-8", newline="\n") as f:
        f.write("USE ai_interview_coach;\n")
        f.write("INSERT IGNORE INTO question_knowledge (question_id, category_id)\n")
        f.write("SELECT q.id, v.cid FROM question q JOIN (\n")
        vals = []
        for fp, cid in link_rows:
            vals.append("SELECT %s AS fp, %s AS cid" % (esc(fp), cid))
        f.write(" UNION ALL ".join(vals))
        f.write("\n) v ON v.fp = q.fingerprint;\n")

    with io.open(OUT_STAT, "w", encoding="utf-8", newline="\n") as f:
        f.write("types: %s\n" % json.dumps(stats, ensure_ascii=False))
        f.write("skipped (no category): %d\n" % len(skipped))
        for s in skipped:
            f.write("  - %s\n" % s)

    print("questions:", len(q_rows), "links:", len(link_rows), "skipped:", len(skipped))
    print("types:", stats)
    print("output:", OUT_Q, OUT_L)


if __name__ == "__main__":
    main()
