package com.xbmd.module.stats.controller;

import com.xbmd.common.Result;
import com.xbmd.module.stats.dto.StudyReportVO;
import com.xbmd.module.stats.service.StatsService;
import com.xbmd.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学习能力分析与报告接口（登录用户，仅本人）
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /** 学习报告（默认近 30 天） */
    @GetMapping("/report")
    public Result<StudyReportVO> report(@RequestParam(defaultValue = "30") int days) {
        return Result.success(statsService.report(UserContext.getUserId(), days));
    }
}
