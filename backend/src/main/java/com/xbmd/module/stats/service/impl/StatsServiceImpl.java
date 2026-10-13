package com.xbmd.module.stats.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xbmd.module.knowledge.entity.KnowledgeCategory;
import com.xbmd.module.knowledge.mapper.KnowledgeCategoryMapper;
import com.xbmd.module.knowledge.service.KnowledgeCategoryService;
import com.xbmd.module.practice.entity.PracticeRecord;
import com.xbmd.module.practice.mapper.PracticeRecordMapper;
import com.xbmd.module.question.dto.QuestionVO;
import com.xbmd.module.question.service.QuestionService;
import com.xbmd.module.stats.dto.StudyReportVO;
import com.xbmd.module.stats.entity.KnowledgeStat;
import com.xbmd.module.stats.mapper.KnowledgeStatMapper;
import com.xbmd.module.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学习能力分析与报告服务实现
 */
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("MM-dd");
    private static final int WEAK_TOP_N = 5;
    private static final int MIN_SAMPLE = 3;

    private final KnowledgeStatMapper knowledgeStatMapper;
    private final KnowledgeCategoryMapper categoryMapper;
    private final KnowledgeCategoryService categoryService;
    private final QuestionService questionService;
    private final PracticeRecordMapper practiceRecordMapper;

    @Override
    @Transactional
    public void updateStats(Long userId, Long questionId, boolean correct) {
        QuestionVO question = questionService.getDetail(questionId);
        List<Long> pointIds = collectPoints(question.getCategoryIds());
        if (pointIds.isEmpty()) {
            return;
        }
        for (Long pointId : pointIds) {
            KnowledgeStat exist = knowledgeStatMapper.selectOne(
                    new LambdaQueryWrapper<KnowledgeStat>()
                            .eq(KnowledgeStat::getUserId, userId)
                            .eq(KnowledgeStat::getCategoryId, pointId));
            int total = 0;
            int hit = 0;
            if (exist == null) {
                exist = new KnowledgeStat();
                exist.setUserId(userId);
                exist.setCategoryId(pointId);
            } else {
                total = exist.getTotalCount() == null ? 0 : exist.getTotalCount();
                hit = exist.getCorrectCount() == null ? 0 : exist.getCorrectCount();
            }
            exist.setTotalCount(total + 1);
            exist.setCorrectCount(hit + (correct ? 1 : 0));
            exist.setMastery(rate(exist.getCorrectCount(), exist.getTotalCount()));
            if (exist.getId() == null) {
                knowledgeStatMapper.insert(exist);
            } else {
                knowledgeStatMapper.updateById(exist);
            }
        }
    }

    @Override
    public List<Long> listWeakPointIds(Long userId) {
        List<KnowledgeStat> stats = knowledgeStatMapper.selectList(
                new LambdaQueryWrapper<KnowledgeStat>().eq(KnowledgeStat::getUserId, userId));
        return stats.stream()
                .filter(s -> s.getTotalCount() != null && s.getTotalCount() >= MIN_SAMPLE)
                .filter(s -> s.getCorrectCount() == null
                        || (s.getCorrectCount() * 1.0 / s.getTotalCount()) < 0.6)
                .map(KnowledgeStat::getCategoryId)
                .toList();
    }

    @Override
    public StudyReportVO report(Long userId, int days) {
        int d = days <= 0 ? 30 : Math.min(days, 365);
        LocalDateTime from = LocalDateTime.now().minusDays(d);
        StudyReportVO vo = new StudyReportVO();
        vo.setDays(d);

        // 练习场次与趋势
        List<PracticeRecord> finished = practiceRecordMapper.selectList(
                new LambdaQueryWrapper<PracticeRecord>()
                        .eq(PracticeRecord::getUserId, userId)
                        .eq(PracticeRecord::getStatus, 1)
                        .ge(PracticeRecord::getFinishTime, from));
        vo.setPracticeCount(finished.size());

        // 知识点统计
        List<KnowledgeStat> stats = knowledgeStatMapper.selectList(
                new LambdaQueryWrapper<KnowledgeStat>().eq(KnowledgeStat::getUserId, userId));
        long totalAnswered = stats.stream().mapToLong(s -> s.getTotalCount() == null ? 0 : s.getTotalCount()).sum();
        long totalCorrect = stats.stream().mapToLong(s -> s.getCorrectCount() == null ? 0 : s.getCorrectCount()).sum();
        vo.setTotalAnswered(totalAnswered);
        vo.setOverallRate(rateOf(totalCorrect, totalAnswered));

        // 领域聚合
        Map<String, long[]> domainAgg = new LinkedHashMap<>();
        for (KnowledgeStat s : stats) {
            KnowledgeCategory domain = domainOf(s.getCategoryId());
            if (domain == null) {
                continue;
            }
            long[] acc = domainAgg.computeIfAbsent(domain.getName(), k -> new long[]{0, 0});
            acc[0] += s.getTotalCount() == null ? 0 : s.getTotalCount();
            acc[1] += s.getCorrectCount() == null ? 0 : s.getCorrectCount();
        }
        List<StudyReportVO.DomainStat> domainStats = domainAgg.entrySet().stream().map(e -> {
            StudyReportVO.DomainStat ds = new StudyReportVO.DomainStat();
            ds.setName(e.getKey());
            ds.setTotal(e.getValue()[0]);
            ds.setCorrect(e.getValue()[1]);
            ds.setRate(rateOf(e.getValue()[1], e.getValue()[0]));
            return ds;
        }).sorted(Comparator.comparingDouble(StudyReportVO.DomainStat::getRate).reversed()).toList();
        vo.setDomainStats(domainStats);

        // 知识点掌握度（按掌握度排序 TOP 10）
        List<StudyReportVO.PointStat> pointStats = new ArrayList<>();
        for (KnowledgeStat s : stats) {
            KnowledgeCategory point = categoryMapper.selectById(s.getCategoryId());
            KnowledgeCategory domain = domainOf(s.getCategoryId());
            if (point == null) {
                continue;
            }
            StudyReportVO.PointStat ps = new StudyReportVO.PointStat();
            ps.setName(point.getName());
            ps.setDomain(domain == null ? "" : domain.getName());
            ps.setTotal(s.getTotalCount() == null ? 0 : s.getTotalCount());
            ps.setCorrect(s.getCorrectCount() == null ? 0 : s.getCorrectCount());
            ps.setRate(rateOf(ps.getCorrect(), ps.getTotal()));
            ps.setMastery(s.getMastery());
            pointStats.add(ps);
        }
        pointStats.sort(Comparator.comparing(StudyReportVO.PointStat::getMastery,
                        Comparator.nullsLast(BigDecimal::compareTo))
                .reversed());
        vo.setPointStats(pointStats.stream().limit(10).toList());

        // 薄弱知识点（样本≥3 且正确率<60%）
        List<StudyReportVO.WeakPoint> weak = new ArrayList<>();
        for (KnowledgeStat s : stats) {
            int total = s.getTotalCount() == null ? 0 : s.getTotalCount();
            int correct = s.getCorrectCount() == null ? 0 : s.getCorrectCount();
            if (total < MIN_SAMPLE || (correct * 1.0 / total) >= 0.6) {
                continue;
            }
            KnowledgeCategory point = categoryMapper.selectById(s.getCategoryId());
            KnowledgeCategory domain = domainOf(s.getCategoryId());
            if (point == null) {
                continue;
            }
            StudyReportVO.WeakPoint wp = new StudyReportVO.WeakPoint();
            wp.setName(point.getName());
            wp.setDomain(domain == null ? "" : domain.getName());
            wp.setErrorRate(round1((1 - correct * 1.0 / total) * 100));
            weak.add(wp);
        }
        weak.sort(Comparator.comparingDouble(StudyReportVO.WeakPoint::getErrorRate).reversed());
        vo.setWeakPoints(weak.stream().limit(WEAK_TOP_N).toList());

        // 近 N 天趋势（逐天聚合）
        LocalDate today = LocalDate.now();
        LocalDate startDay = today.minusDays(d - 1L);
        Map<String, long[]> dayPractice = new LinkedHashMap<>();
        for (PracticeRecord pr : finished) {
            if (pr.getFinishTime() == null || pr.getTotalCount() == null) {
                continue;
            }
            LocalDate day = pr.getFinishTime().toLocalDate();
            if (day.isBefore(startDay)) {
                continue;
            }
            long[] acc = dayPractice.computeIfAbsent(day.format(DAY_FMT), k -> new long[]{0, 0, 0});
            acc[0]++;                       // 场次
            acc[1] += pr.getTotalCount();   // 总题
            acc[2] += pr.getCorrectCount() == null ? 0 : pr.getCorrectCount(); // 答对
        }
        List<StudyReportVO.TrendPoint> trend = new ArrayList<>();
        for (int i = 0; i < d; i++) {
            String key = startDay.plusDays(i).format(DAY_FMT);
            StudyReportVO.TrendPoint tp = new StudyReportVO.TrendPoint();
            tp.setDate(key);
            long[] acc = dayPractice.get(key);
            if (acc == null) {
                tp.setPracticeCount(0);
                tp.setCorrectRate(0);
            } else {
                tp.setPracticeCount(acc[0]);
                tp.setCorrectRate(acc[1] == 0 ? 0 : round1(acc[2] * 100.0 / acc[1]));
            }
            trend.add(tp);
        }
        vo.setTrend(trend);

        vo.setSuggestion(buildSuggestion(vo));
        return vo;
    }

    // ---------- 内部工具 ----------

    /** 收集题目挂载的 level=3 知识点（非 level3 时展开其子级） */
    private List<Long> collectPoints(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> points = new ArrayList<>();
        for (Long cid : categoryIds) {
            KnowledgeCategory cat = categoryMapper.selectById(cid);
            if (cat == null) {
                continue;
            }
            if (cat.getLevel() != null && cat.getLevel() == 3) {
                points.add(cid);
            } else {
                points.addAll(categoryService.collectKnowledgePointIds(cid));
            }
        }
        return points.stream().distinct().toList();
    }

    /** 找到知识点所属领域（level=1 祖先） */
    private KnowledgeCategory domainOf(Long categoryId) {
        KnowledgeCategory cur = categoryMapper.selectById(categoryId);
        int guard = 0;
        while (cur != null && cur.getLevel() != null && cur.getLevel() > 1 && guard++ < 10) {
            if (cur.getParentId() == null) {
                break;
            }
            cur = categoryMapper.selectById(cur.getParentId());
        }
        return (cur != null && cur.getLevel() != null && cur.getLevel() == 1) ? cur : null;
    }

    private BigDecimal rate(int correct, int total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(correct * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
    }

    private double rateOf(long correct, long total) {
        if (total <= 0) {
            return 0;
        }
        return round1(correct * 100.0 / total);
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private String buildSuggestion(StudyReportVO vo) {
        StringBuilder sb = new StringBuilder();
        if (vo.getPracticeCount() == 0) {
            return "暂无练习数据。建议先完成一组专项练习，平台将自动生成你的学习画像。";
        }
        sb.append("近 ").append(vo.getDays()).append(" 天共完成 ").append(vo.getPracticeCount())
                .append(" 场练习，整体正确率 ").append(round1(vo.getOverallRate())).append("%。");
        if (!vo.getWeakPoints().isEmpty()) {
            sb.append(" 薄弱环节集中在：");
            List<String> names = vo.getWeakPoints().stream().limit(3)
                    .map(w -> w.getDomain() + "·" + w.getName()).toList();
            sb.append(String.join("、", names));
            sb.append("。建议优先通过错题重练与对应专项卡片加强练习，并回顾答案解析。");
        } else {
            sb.append(" 当前无明显薄弱知识点，可尝试更高难度或 AI 出题保持挑战。");
        }
        if (vo.getOverallRate() < 60) {
            sb.append(" 整体正确率偏低，建议放慢节奏、逐题吃透解析后再进行新一组练习。");
        } else if (vo.getOverallRate() >= 85) {
            sb.append(" 状态不错，建议保持节奏并挑战更高难度题目。");
        }
        return sb.toString();
    }
}
