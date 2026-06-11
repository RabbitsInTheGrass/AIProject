package com.agent.server.repository;

import com.agent.server.model.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {
    Optional<VerificationCode> findTopByTargetAndTargetTypeAndIsUsedFalseAndExpireAtAfter(
            String target, String targetType, LocalDateTime now);
}
