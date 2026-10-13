package com.xbmd.module.interview.dto;

import lombok.Data;

import java.util.List;

/**
 * 简历解析结果：技术栈 + 项目
 */
@Data
public class ResumeParseVO {

    private Long sessionId;

    private List<String> techStack;

    private List<ProjectBriefVO> projects;
}
