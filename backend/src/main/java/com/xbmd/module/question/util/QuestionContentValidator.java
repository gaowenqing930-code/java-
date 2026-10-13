package com.xbmd.module.question.util;

import com.xbmd.common.BusinessException;
import com.xbmd.common.ResultCode;
import com.xbmd.module.question.dto.OptionDTO;
import com.xbmd.module.question.dto.ScorePointDTO;
import com.xbmd.module.question.enums.QuestionType;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 题目内容校验器：按题型校验题干/选项/答案/评分点，并返回规范化答案
 */
public final class QuestionContentValidator {

    private QuestionContentValidator() {
    }

    /**
     * @return 规范化后的答案（JUDGE 统一为 true/false；MULTIPLE 统一为 "A,B"）
     */
    public static String validateAndNormalize(String type, String stem,
                                              List<OptionDTO> options, String answer,
                                              List<ScorePointDTO> scorePoints) {
        QuestionType questionType = QuestionType.of(type);
        if (questionType == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的题型：" + type);
        }
        if (stem == null || stem.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "题干不能为空");
        }
        return switch (questionType) {
            case SINGLE -> validateSingle(options, answer);
            case MULTIPLE -> validateMultiple(options, answer);
            case JUDGE -> validateJudge(answer);
            case SHORT -> validateShort(answer, scorePoints);
        };
    }

    private static String validateSingle(List<OptionDTO> options, String answer) {
        if (options == null || options.size() != 4) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "单选题必须提供恰好 4 个选项");
        }
        Set<String> keys = checkKeys(options);
        if (answer == null || !keys.contains(answer.trim())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "单选题答案必须是某个选项的 key");
        }
        return answer.trim();
    }

    private static String validateMultiple(List<OptionDTO> options, String answer) {
        if (options == null || options.size() < 2) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "多选题至少需要 2 个选项");
        }
        Set<String> keys = checkKeys(options);
        if (answer == null || answer.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "多选题答案不能为空");
        }
        // 支持 "A,B"、"A、B"、"AB" 三种写法，统一为 "A,B"
        String normalized = Arrays.stream(answer.split("[,、]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.length() == 1 ? s : "")
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.joining(","));
        if (normalized.split(",").length < 2) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "多选题答案至少包含 2 个选项 key");
        }
        for (String k : normalized.split(",")) {
            if (!keys.contains(k)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "多选题答案包含不存在的选项 key：" + k);
            }
        }
        return normalized;
    }

    private static String validateJudge(String answer) {
        if (answer == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "判断题答案不能为空");
        }
        return switch (answer.trim().toLowerCase()) {
            case "true", "对", "正确", "√" -> "true";
            case "false", "错", "错误", "×" -> "false";
            default -> throw new BusinessException(ResultCode.BAD_REQUEST, "判断题答案仅支持 对/错");
        };
    }

    private static String validateShort(String answer, List<ScorePointDTO> scorePoints) {
        if (answer == null || answer.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "简答题参考答案不能为空");
        }
        if (scorePoints == null || scorePoints.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "简答题必须提供至少 1 个评分点");
        }
        for (ScorePointDTO sp : scorePoints) {
            if (sp.getPoint() == null || sp.getPoint().isBlank()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "评分点描述不能为空");
            }
            if (sp.getScore() == null || sp.getScore() <= 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "评分点分值必须为正数");
            }
        }
        return answer.trim();
    }

    /** 校验选项 key 连续且文本非空，返回 key 集合 */
    private static Set<String> checkKeys(List<OptionDTO> options) {
        Set<String> keys = options.stream().map(OptionDTO::getKey).collect(Collectors.toSet());
        for (int i = 0; i < options.size(); i++) {
            String expect = String.valueOf((char) ('A' + i));
            OptionDTO opt = options.get(i);
            if (opt.getKey() == null || !expect.equals(opt.getKey().trim())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "选项 key 必须从 A 开始连续编号，第 " + (i + 1) + " 个应为 " + expect);
            }
            if (opt.getText() == null || opt.getText().isBlank()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "选项 " + expect + " 的内容不能为空");
            }
        }
        return keys;
    }
}
