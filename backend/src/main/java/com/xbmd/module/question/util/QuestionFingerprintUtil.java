package com.xbmd.module.question.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 题目指纹工具：指纹 = SHA-256(题干规范化 + 知识点集合排序 + 难度)
 */
public final class QuestionFingerprintUtil {

    private QuestionFingerprintUtil() {
    }

    public static String compute(String stem, List<Long> categoryIds, Integer difficulty) {
        String normalizedStem = normalize(stem);
        String categoryPart = categoryIds == null
                ? ""
                : categoryIds.stream()
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .sorted(Comparator.naturalOrder())
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
        String seed = normalizedStem + "|" + categoryPart + "|" + difficulty;
        return sha256(seed);
    }

    /** 题干规范化：去除首尾空白、压缩内部连续空白、统一小写 */
    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
