package com.agent.server.repository;

import com.agent.server.model.entity.UserLongTermMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserLongTermMemoryRepository extends JpaRepository<UserLongTermMemory, Long> {
    List<UserLongTermMemory> findByUserIdAndIsActiveTrue(Long userId);
    List<UserLongTermMemory> findByUserIdAndMemoryTypeAndIsActiveTrue(Long userId, String memoryType);
}
