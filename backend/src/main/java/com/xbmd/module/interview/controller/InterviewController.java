package com.xbmd.module.interview.controller;

import com.xbmd.common.Result;
import com.xbmd.module.interview.dto.InterviewAnswerRequest;
import com.xbmd.module.interview.dto.InterviewAnswerVO;
import com.xbmd.module.interview.dto.InterviewCreateRequest;
import com.xbmd.module.interview.dto.InterviewQuestionVO;
import com.xbmd.module.interview.dto.InterviewResultVO;
import com.xbmd.module.interview.dto.InterviewSessionVO;
import com.xbmd.module.interview.dto.ResumeExtractVO;
import com.xbmd.module.interview.dto.ResumeParseVO;
import com.xbmd.module.interview.service.InterviewService;
import com.xbmd.module.interview.support.ResumeTextExtractor;
import com.xbmd.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * AI 智能面试接口（登录用户）
 */
@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;
    private final ResumeTextExtractor resumeTextExtractor;

    /** 上传简历文件（.docx/.doc/.pdf），提取文本供用户确认 */
    @PostMapping("/resume/extract")
    public Result<ResumeExtractVO> extract(@RequestParam("file") MultipartFile file) {
        ResumeExtractVO vo = new ResumeExtractVO();
        vo.setFileName(file.getOriginalFilename());
        vo.setText(resumeTextExtractor.extract(file));
        return Result.success(vo);
    }

    /** 解析简历并创建面试会话 */
    @PostMapping("/sessions")
    public Result<ResumeParseVO> create(@Valid @RequestBody InterviewCreateRequest request) {
        return Result.success(interviewService.create(request, UserContext.getUserId()));
    }

    /** 开始面试：按简历生成 10 道面试题，返回第一题 */
    @PostMapping("/sessions/{id}/start")
    public Result<InterviewQuestionVO> start(@PathVariable Long id) {
        return Result.success(interviewService.start(id, UserContext.getUserId()));
    }

    /** 提交当前题作答，AI 评分；最后一题返回完整结果 */
    @PostMapping("/sessions/{id}/answer")
    public Result<InterviewAnswerVO> answer(@PathVariable Long id,
                                            @Valid @RequestBody InterviewAnswerRequest request) {
        return Result.success(interviewService.answer(id, request, UserContext.getUserId()));
    }

    /** 出题失败后重试生成下一题（answer 返回 needRetry=true 时调用） */
    @PostMapping("/sessions/{id}/next")
    public Result<InterviewQuestionVO> next(@PathVariable Long id) {
        return Result.success(interviewService.nextQuestion(id, UserContext.getUserId()));
    }

    /** 用户主动结束面试：统一评分已答题目并生成总结 */
    @PostMapping("/sessions/{id}/finish")
    public Result<InterviewResultVO> finish(@PathVariable Long id) {
        return Result.success(interviewService.finish(id, UserContext.getUserId()));
    }

    /** 获取面试结果 */
    @GetMapping("/sessions/{id}/result")
    public Result<InterviewResultVO> result(@PathVariable Long id) {
        return Result.success(interviewService.result(id, UserContext.getUserId()));
    }

    /** 我的面试历史列表（按创建时间倒序） */
    @GetMapping("/sessions/my")
    public Result<List<InterviewSessionVO>> mySessions() {
        return Result.success(interviewService.mySessions(UserContext.getUserId()));
    }
}
