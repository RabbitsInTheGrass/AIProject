package com.agent.server.controller;

import com.agent.server.model.entity.SkillConfig;
import com.agent.server.repository.SkillConfigRepository;
import com.agent.server.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillConfigRepository repository;

    @GetMapping
    public ResponseEntity<?> list() {
        Long userId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(repository.findByUserId(userId));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<?> toggle(@PathVariable Long id) {
        SkillConfig config = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + id));
        config.setIsEnabled(!config.getIsEnabled());
        return ResponseEntity.ok(repository.save(config));
    }
}
