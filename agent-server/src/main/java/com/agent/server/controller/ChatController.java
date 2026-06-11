package com.agent.server.controller;

import com.agent.server.model.dto.ChatRequest;
import com.agent.server.security.SecurityUtil;
import com.agent.server.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/stream")
    public SseEmitter streamChat(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 min timeout
        Long userId = SecurityUtil.getCurrentUserId();

        // Run in separate thread to not block the request
        Thread.startVirtualThread(() -> chatService.streamChat(request, emitter, userId));

        return emitter;
    }
}
