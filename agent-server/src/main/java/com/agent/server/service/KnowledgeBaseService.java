package com.agent.server.service;

import com.agent.server.model.entity.KnowledgeBase;
import com.agent.server.model.entity.KnowledgeDocument;
import com.agent.server.repository.KnowledgeBaseRepository;
import com.agent.server.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository kbRepository;
    private final KnowledgeDocumentRepository docRepository;

    public List<KnowledgeBase> listByUser(Long userId) {
        if (userId == null) return kbRepository.findAll();
        return kbRepository.findByUserId(userId);
    }

    public KnowledgeBase getById(Long id) {
        return kbRepository.findById(id).orElseThrow(() -> new RuntimeException("Knowledge base not found: " + id));
    }

    @Transactional
    public KnowledgeBase create(String name, String description, Long userId) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(name);
        kb.setDescription(description);
        kb.setUserId(userId);
        kb.setCollectionName("kb_" + UUID.randomUUID().toString().replace("-", ""));
        return kbRepository.save(kb);
    }

    @Transactional
    public void delete(Long id) {
        // Delete all documents first
        List<KnowledgeDocument> docs = docRepository.findByKnowledgeBaseId(id);
        docRepository.deleteAll(docs);
        kbRepository.deleteById(id);
    }

    public List<KnowledgeDocument> listDocuments(Long knowledgeBaseId) {
        return docRepository.findByKnowledgeBaseId(knowledgeBaseId);
    }

    @Transactional
    public KnowledgeDocument uploadDocument(Long knowledgeBaseId, MultipartFile file) {
        KnowledgeBase kb = getById(knowledgeBaseId);

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setKnowledgeBaseId(knowledgeBaseId);
        doc.setFileName(file.getOriginalFilename());
        doc.setFileType(getFileType(file.getOriginalFilename()));
        doc.setFileSize(file.getSize());
        doc.setStatus("PROCESSING");
        doc = docRepository.save(doc);

        // Async process document
        processDocumentAsync(doc, file);

        // Update knowledge base stats
        kb.setDocumentCount(docRepository.countByKnowledgeBaseId(knowledgeBaseId));
        kbRepository.save(kb);

        return doc;
    }

    @Transactional
    public void deleteDocument(Long knowledgeBaseId, Long documentId) {
        docRepository.deleteById(documentId);
        KnowledgeBase kb = getById(knowledgeBaseId);
        kb.setDocumentCount(docRepository.countByKnowledgeBaseId(knowledgeBaseId));
        kbRepository.save(kb);
    }

    private void processDocumentAsync(KnowledgeDocument doc, MultipartFile file) {
        try {
            // Read file content using Tika
            var reader = new org.springframework.ai.reader.tika.TikaDocumentReader(
                    new org.springframework.core.io.InputStreamResource(file.getInputStream()));
            var documents = reader.get();

            int chunkCount = documents.size();
            doc.setChunkCount(chunkCount);
            doc.setStatus("COMPLETED");
            docRepository.save(doc);

            log.info("Processed document {} with {} chunks", doc.getFileName(), chunkCount);

            // TODO: Store embeddings in Milvus when available
            // For now, documents are stored in MySQL only

        } catch (Exception e) {
            log.error("Failed to process document: {}", doc.getFileName(), e);
            doc.setStatus("FAILED");
            doc.setErrorMessage(e.getMessage());
            docRepository.save(doc);
        }
    }

    private String getFileType(String fileName) {
        if (fileName == null) return "UNKNOWN";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0) {
            return fileName.substring(dotIndex + 1).toUpperCase();
        }
        return "UNKNOWN";
    }
}
