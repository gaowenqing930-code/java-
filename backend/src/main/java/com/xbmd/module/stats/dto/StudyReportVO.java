package com.xbmd.module.stats.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 学习报告返回体：四维统计 + 训练建议
 */
@Data
public class StudyReportVO {

    /** 统计时间段（近 N 天） */
    private int days;

    /** 练习场次 */
    private long practiceCount;

    /** 累计答题数 */
    private long totalAnswered;

    /** 整体正确率（%） */
    private double overallRate;

    /** 各知识领域正确率 */
    private List<DomainStat> domainStats;

    /** 知识点掌握度（TOP 展示） */
    private List<PointStat> pointStats;

    /** 薄弱知识点 Top N */
    private List<WeakPoint> weakPoints;

    /** 近 30 天练习量与正确率趋势 */
    private List<TrendPoint> trend;

    /** 文字版训练建议 */
    private String suggestion;

    @Data
    public static class DomainStat {
        private String name;
        private long total;
        private long correct;
        private double rate;
    }

    @Data
    public static class PointStat {
        private String name;
        private String domain;
        private long total;
        private long correct;
        private double rate;
        private BigDecimal mastery;
    }

    @Data
    public static class WeakPoint {
        private String name;
        private String domain;
        private double errorRate;
    }

    @Data
    public static class TrendPoint {
        private String date;
        private long practiceCount;
        private double correctRate;
    }
}
