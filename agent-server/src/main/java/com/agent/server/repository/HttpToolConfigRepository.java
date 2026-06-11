package com.agent.server.repository;

import com.agent.server.model.entity.HttpToolConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HttpToolConfigRepository extends JpaRepository<HttpToolConfig, Long> {
    List<HttpToolConfig> findByUserId(Long userId);
    List<HttpToolConfig> findByUserIdAndIsEnabledTrue(Long userId);
}
