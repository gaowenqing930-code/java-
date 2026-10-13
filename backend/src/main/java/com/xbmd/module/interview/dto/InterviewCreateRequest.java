package com.xbmd.module.interview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建 AI 面试请求：简历文本
 */
@Data
public class InterviewCreateRequest {

    @NotBlank(message = "简历内容不能为空")
    private String resumeText;
}
