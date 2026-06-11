package com.agent.server.controller;

import com.agent.server.model.dto.ModelConfigDTO;
import com.agent.server.security.SecurityUtil;
import com.agent.server.service.ModelConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class ModelConfigController {

    private final ModelConfigService service;

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(service.listByUser(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/presets")
    public ResponseEntity<?> presets() {
        return ResponseEntity.ok(service.getPresets());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ModelConfigDTO dto) {
        return ResponseEntity.ok(service.create(dto, SecurityUtil.getCurrentUserId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody ModelConfigDTO dto) {
        return ResponseEntity.ok(service.update(id, dto, SecurityUtil.getCurrentUserId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}
