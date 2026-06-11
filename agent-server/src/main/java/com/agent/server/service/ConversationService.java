package com.agent.server.service;

import com.agent.server.model.entity.Conversation;
import com.agent.server.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository repository;

    public List<Conversation> listByUser(Long userId) {
        if (userId == null) return repository.findAllByOrderByUpdatedAtDesc();
        return repository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public Conversation getById(String id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Conversation not found: " + id));
    }

    public Conversation getOrCreate(String conversationId, Long userId, Long modelConfigId) {
        if (conversationId != null) {
            return repository.findById(conversationId)
                    .orElseGet(() -> create(userId, modelConfigId));
        }
        return create(userId, modelConfigId);
    }

    public Conversation create(Long userId, Long modelConfigId) {
        Conversation conv = new Conversation();
        conv.setId(UUID.randomUUID().toString());
        conv.setUserId(userId);
        conv.setModelConfigId(modelConfigId);
        return repository.save(conv);
    }

    public Conversation updateTitle(String id, String title) {
        Conversation conv = getById(id);
        conv.setTitle(title);
        return repository.save(conv);
    }

    public void delete(String id) {
        repository.deleteById(id);
    }
}
