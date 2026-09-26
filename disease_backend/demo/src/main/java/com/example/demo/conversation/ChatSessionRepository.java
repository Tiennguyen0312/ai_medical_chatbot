package com.example.demo.conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, Long> {
    Optional<ChatSessionEntity> findByClientSessionKey(String clientSessionKey);
    List<ChatSessionEntity> findAllByOrderByCreatedAtDesc();
    List<ChatSessionEntity> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}