package com.agent.server.controller;

import com.agent.server.model.dto.HttpToolConfigDTO;
import com.agent.server.security.SecurityUtil;
import com.agent.server.service.HttpToolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/http-tools")
@RequiredArgsConstructor
public class HttpToolController {

    private final HttpToolService service;

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(service.listByUser(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody HttpToolConfigDTO dto) {
        return ResponseEntity.ok(service.create(dto, SecurityUtil.getCurrentUserId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody HttpToolConfigDTO dto) {
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
