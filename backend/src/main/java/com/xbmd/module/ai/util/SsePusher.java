package com.xbmd.module.ai.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbmd.module.question.dto.QuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 推送器：管理每个出题任务的 SseEmitter 连接，向前端实时推送进度/完成/失败事件。
 * 断线与超时自动清理，防止内存泄漏。
 */
@Component
@RequiredArgsConstructor
public class SsePusher {

    /** 连接空闲 60s 自动关闭（生成期间会持续推送 progress，不会触发） */
    private static final long IDLE_TIMEOUT = 60_000L;

    private final ObjectMapper objectMapper;
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /** 订阅某任务的 SSE 连接（Controller 返回给前端） */
    public SseEmitter subscribe(Long taskId) {
        SseEmitter emitter = new SseEmitter(IDLE_TIMEOUT);
        emitters.put(taskId, emitter);
        emitter.onCompletion(() -> emitters.remove(taskId));
        emitter.onTimeout(() -> emitters.remove(taskId));
        emitter.onError(e -> emitters.remove(taskId));
        return emitter;
    }

    /** 推送进度：event=progress */
    public void pushProgress(Long taskId, int done, int total) {
        int percent = total <= 0 ? 0 : (int) (done * 100.0 / total);
        push(taskId, "progress",
                "{\"done\":" + done + ",\"total\":" + total + ",\"percent\":" + percent + "}");
    }

    /** 推送完成：event=done，含题目列表，随后关闭连接 */
    public void pushDone(Long taskId, List<QuestionVO> questions, String message) {
        push(taskId, "done",
                "{\"questions\":" + toJson(questions) + ",\"fromFallback\":false,\"message\":\"" + safe(message) + "\"}");
        complete(taskId);
    }

    /** 推送失败：event=failed，随后关闭连接 */
    public void pushError(Long taskId, String message) {
        push(taskId, "failed", "{\"message\":\"" + safe(message) + "\"}");
        complete(taskId);
    }

    private void push(Long taskId, String event, String data) {
        SseEmitter emitter = emitters.get(taskId);
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException | IllegalStateException e) {
            emitters.remove(taskId);
        }
    }

    private void complete(Long taskId) {
        SseEmitter emitter = emitters.remove(taskId);
        if (emitter != null) {
            emitter.complete();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String safe(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}
