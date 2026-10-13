package com.xbmd.module.practice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 交卷批量提交请求
 */
@Data
public class SubmitAllRequest {

    @NotEmpty(message = "答案列表不能为空")
    @Valid
    private List<AnswerSubmitRequest> answers;
}
