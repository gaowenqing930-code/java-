package com.xbmd.module.practice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xbmd.module.practice.entity.WrongQuestion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 错题 Mapper（Task 7 复用）
 */
@Mapper
public interface WrongQuestionMapper extends BaseMapper<WrongQuestion> {
}
