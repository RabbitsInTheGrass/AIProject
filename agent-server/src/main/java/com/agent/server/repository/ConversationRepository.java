package com.agent.server.repository;

import com.agent.server.model.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {
    List<Conversation> findByUserIdOrderByUpdatedAtDesc(Long userId);
    List<Conversation> findAllByOrderByUpdatedAtDesc();
}
