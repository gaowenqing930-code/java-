package com.xbmd.module.exam.dto;

import com.xbmd.module.question.dto.QuestionVO;
import lombok.Data;

import java.util.List;

/**
 * 真题卷子返回体
 */
@Data
public class ExamPaperVO {

    private Long id;
    private String company;
    private Integer year;
    private String season;
    private String round;
    private String position;

    /** 笔试 / 面试 */
    private String type;

    /** 热度 */
    private Integer heat;

    /** 是否热门：1是 0否 */
    private Integer hot;

    /** 卷子题目总数 */
    private Integer questionCount;

    /** 卷子固定题目（按题号顺序）；列表接口为空 */
    private List<QuestionVO> questions;
}
