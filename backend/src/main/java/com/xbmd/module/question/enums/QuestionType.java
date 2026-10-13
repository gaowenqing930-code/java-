package com.xbmd.module.question.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 题型枚举
 */
@Getter
public enum QuestionType {

    SINGLE("单选题"),
    MULTIPLE("多选题"),
    JUDGE("判断题"),
    SHORT("简答题");

    private final String label;

    QuestionType(String label) {
        this.label = label;
    }

    public static boolean isValid(String type) {
        return Arrays.stream(values()).anyMatch(t -> t.name().equals(type));
    }

    public static QuestionType of(String type) {
        return Arrays.stream(values())
                .filter(t -> t.name().equals(type))
                .findFirst()
                .orElse(null);
    }
}
