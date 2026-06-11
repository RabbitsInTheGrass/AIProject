package com.agent.server.controller;

import com.agent.server.security.SecurityUtil;
import com.agent.server.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService service;

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(service.listByUser(SecurityUtil.getCurrentUserId()));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(service.create(body.get("name"), body.get("description"), userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/documents")
    public ResponseEntity<?> documents(@PathVariable Long id) {
        return ResponseEntity.ok(service.listDocuments(id));
    }

    @PostMapping("/{id}/documents")
    public ResponseEntity<?> uploadDocument(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(service.uploadDocument(id, file));
    }

    @DeleteMapping("/{kbId}/documents/{docId}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long kbId, @PathVariable Long docId) {
        service.deleteDocument(kbId, docId);
        return ResponseEntity.ok().build();
    }
}
