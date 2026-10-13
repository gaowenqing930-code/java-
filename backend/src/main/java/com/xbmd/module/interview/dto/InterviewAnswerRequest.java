package com.xbmd.module.interview.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交面试作答请求
 */
@Data
public class InterviewAnswerRequest {

    /** 题号 1-10 */
    @NotNull(message = "题号不能为空")
    @Min(value = 1, message = "题号范围 1-10")
    private Integer no;

    @NotBlank(message = "回答内容不能为空")
    private String answer;
}
