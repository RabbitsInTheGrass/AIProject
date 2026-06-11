package com.agent.server.controller;

import com.agent.server.model.entity.ChatMessageEntity;
import com.agent.server.repository.ChatMessageRepository;
import com.agent.server.security.SecurityUtil;
import com.agent.server.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService service;
    private final ChatMessageRepository messageRepository;

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(service.listByUser(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<?> messages(@PathVariable String id) {
        return ResponseEntity.ok(messageRepository.findByConversationIdOrderBySortOrder(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTitle(@PathVariable String id, @RequestBody Map<String, String> body) {
        String title = body.get("title");
        return ResponseEntity.ok(service.updateTitle(id, title));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}
