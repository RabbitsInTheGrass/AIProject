package com.agent.server.repository;

import com.agent.server.model.entity.ModelConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModelConfigRepository extends JpaRepository<ModelConfig, Long> {
    Optional<ModelConfig> findByIsDefaultTrue();
    List<ModelConfig> findByUserId(Long userId);
}
