package com.agent.server.repository;

import com.agent.server.model.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {
    List<KnowledgeDocument> findByKnowledgeBaseId(Long knowledgeBaseId);
    int countByKnowledgeBaseId(Long knowledgeBaseId);
}
