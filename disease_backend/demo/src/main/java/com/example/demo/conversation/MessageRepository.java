package com.example.demo.conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    List<MessageEntity> findAllByChatSessionIdOrderByCreatedAtAsc(Long chatSessionId);
}