package com.agent.server.repository;

import com.agent.server.model.entity.PluginTool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PluginToolRepository extends JpaRepository<PluginTool, Long> {
    List<PluginTool> findByUserId(Long userId);
    List<PluginTool> findByUserIdAndIsEnabledTrue(Long userId);
}
