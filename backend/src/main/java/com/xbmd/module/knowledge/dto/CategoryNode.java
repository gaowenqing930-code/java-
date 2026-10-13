package com.xbmd.module.knowledge.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类树节点
 */
@Data
public class CategoryNode {

    private Long id;
    private Long parentId;
    private String name;
    private Integer level;
    private Integer sort;
    private List<CategoryNode> children = new ArrayList<>();
}
