package com.xbmd.module.interview.dto;

import lombok.Data;

import java.util.List;

/**
 * 简历项目简介
 */
@Data
public class ProjectBriefVO {

    private String name;

    private String description;

    /** 项目亮点：职责 / 技术难点 / 可量化成果，最多 3 条 */
    private List<String> highlights;
}
