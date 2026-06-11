package com.agent.server.controller;

import com.agent.server.model.dto.PluginToolDTO;
import com.agent.server.security.SecurityUtil;
import com.agent.server.service.PluginToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/plugins")
@RequiredArgsConstructor
public class PluginToolController {

    private final PluginToolService service;

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(service.listByUser(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestPart("config") PluginToolDTO dto,
            @RequestPart("jar") MultipartFile jarFile) {
        try {
            return ResponseEntity.ok(service.create(dto, jarFile, SecurityUtil.getCurrentUserId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody PluginToolDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<?> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(service.toggle(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}
