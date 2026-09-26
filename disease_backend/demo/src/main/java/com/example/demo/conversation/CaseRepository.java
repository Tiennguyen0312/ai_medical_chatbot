package com.example.demo.conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface CaseRepository extends JpaRepository<CaseEntity, Long> {
    Optional<CaseEntity> findFirstByChatSessionIdAndStatusOrderByUpdatedAtDesc(
            Long chatSessionId,            
            CaseStatus status);
    long countByChatSessionId(Long chatSessionId);
    List<CaseEntity> findAllByChatSessionIdOrderByUpdatedAtDesc(Long chatSessionId);
}