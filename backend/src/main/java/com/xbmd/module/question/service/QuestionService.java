package com.xbmd.module.question.service;

import com.xbmd.common.PageResult;
import com.xbmd.module.question.dto.QuestionQuery;
import com.xbmd.module.question.dto.QuestionRequest;
import com.xbmd.module.question.dto.QuestionVO;
import com.xbmd.module.question.entity.Question;

import java.util.List;

/**
 * 题目服务
 */
public interface QuestionService {

    /** 新增题目（管理员） */
    QuestionVO create(QuestionRequest request);

    /** 修改题目（管理员） */
    QuestionVO update(Long id, QuestionRequest request);

    /** 删除题目：逻辑删除（status=0） */
    void delete(Long id);

    /** 上下架 */
    void updateStatus(Long id, Integer status);

    /** 五维组合分页筛选 */
    PageResult<QuestionVO> pageQuery(QuestionQuery query);

    /** 题目详情 */
    QuestionVO getDetail(Long id);

    /** 保存 AI 生成的题目（含知识点关联，指纹判重） */
    Question saveGenerated(Question question, List<Long> categoryIds);

    /** 判断指纹是否已存在 */
    boolean fingerprintExists(String fingerprint);

    /** 根据指纹查询已有题目 */
    Question getByFingerprint(String fingerprint);

    /** 兜底抽题：按条件随机抽取 count 道上架题目（AI 出题降级用） */
    List<QuestionVO> randomPick(String type, Integer difficulty, List<Long> pointIds, int count);
}
