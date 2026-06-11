package com.agent.server.repository;

import com.agent.server.model.entity.SkillConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillConfigRepository extends JpaRepository<SkillConfig, Long> {
    List<SkillConfig> findByIsEnabledTrue();
    Optional<SkillConfig> findByName(String name);
    List<SkillConfig> findByUserId(Long userId);
}
