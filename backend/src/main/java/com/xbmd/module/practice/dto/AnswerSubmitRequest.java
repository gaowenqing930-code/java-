package com.xbmd.module.practice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 单题答案提交请求
 */
@Data
public class AnswerSubmitRequest {

    @NotNull(message = "题目 ID 不能为空")
    private Long questionId;

    /** 用户答案：单选/判断为 key 或 true/false，多选为逗号分隔的 key 集合，简答为文本 */
    @NotBlank(message = "答案不能为空")
    private String userAnswer;
}
