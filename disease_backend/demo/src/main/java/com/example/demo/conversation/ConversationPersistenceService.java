package com.example.demo.conversation;
import com.example.demo.dto.PredictResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ConversationPersistenceService {
    private final ChatSessionRepository chatSessionRepository;
    private final CaseRepository caseRepository;
    private final ObjectMapper objectMapper;
    public ConversationPersistenceService(ChatSessionRepository chatSessionRepository,
                                          CaseRepository caseRepository,
                                          ObjectMapper objectMapper) {
        this.chatSessionRepository = chatSessionRepository;
        this.caseRepository = caseRepository;
        this.objectMapper = objectMapper;}
    
    public ChatSessionEntity getOrCreateChatSession(Long userId, String clientSessionKey) {
        return chatSessionRepository.findByClientSessionKey(clientSessionKey)
                .map(s -> {
                    if (s.getUserId() == null && userId != null) {
                        s.setUserId(userId);
                    }
                    if (userId != null && s.getUserId() != null && !s.getUserId().equals(userId)) {
                        throw new IllegalStateException("Session does not belong to this user");
                    }
                    if (s.getStatus() == ChatSessionStatus.CLOSED) {
                        s.setStatus(ChatSessionStatus.ACTIVE);
                    }
                    return chatSessionRepository.save(s);
                })
                .orElseGet(() -> {
                    ChatSessionEntity s = new ChatSessionEntity();
                    s.setClientSessionKey(clientSessionKey);
                    s.setUserId(userId);
                    s.setStatus(ChatSessionStatus.ACTIVE);
                    return chatSessionRepository.save(s);});
    }
    public Optional<ChatSessionEntity> findOwnedChatSession(Long userId, String clientSessionKey) {
        Optional<ChatSessionEntity> chatSessionOpt = chatSessionRepository.findByClientSessionKey(clientSessionKey);
        if (chatSessionOpt.isEmpty()) return Optional.empty();
        ChatSessionEntity s = chatSessionOpt.get();
        if (userId != null && s.getUserId() != null && !s.getUserId().equals(userId)) {
            return Optional.empty();
        }
        return Optional.of(s);
    }
    public Optional<CaseEntity> findOpenCaseByClientSessionKey(Long userId, String clientSessionKey) {
        Optional<ChatSessionEntity> chatSessionOpt = chatSessionRepository.findByClientSessionKey(clientSessionKey);
        if (chatSessionOpt.isEmpty()) return Optional.empty();

        ChatSessionEntity s = chatSessionOpt.get();
        if (userId != null && s.getUserId() != null && !s.getUserId().equals(userId)) {
            return Optional.empty(); 
        }
        return caseRepository.findFirstByChatSessionIdAndStatusOrderByUpdatedAtDesc(s.getId(), CaseStatus.OPEN);
    }

    public CaseEntity createOrUpdateOpenCase(Long userId,
            String clientSessionKey,
            Set<String> allSymptoms,
            PredictResponse mlRes,
            boolean adviceShown) {
    	ChatSessionEntity chatSession = getOrCreateChatSession(userId, clientSessionKey);

        CaseEntity entity = caseRepository
                .findFirstByChatSessionIdAndStatusOrderByUpdatedAtDesc(chatSession.getId(), CaseStatus.OPEN)
                .orElseGet(() -> {
                    CaseEntity c = new CaseEntity();
                    c.setChatSession(chatSession);
                    c.setStatus(CaseStatus.OPEN);
                    return c;
                });
        entity.setSymptomsJson(toJsonSafe(allSymptoms));
        entity.setTop1Disease(extractTop1Disease(mlRes));
        entity.setTop3Json(toJsonSafe(mlRes != null ? mlRes.getTop3() : null));
        if (adviceShown) {
            entity.setAdviceShown(true);
        }
        return caseRepository.save(entity);
    }

    public void markAdviceShown(Long userId, String clientSessionKey) {
        findOpenCaseByClientSessionKey(userId, clientSessionKey).ifPresent(c -> {
            c.setAdviceShown(true);
            caseRepository.save(c);
        });
    }

    public void closeOpenCase(Long userId, String clientSessionKey) {
        findOpenCaseByClientSessionKey(userId, clientSessionKey).ifPresent(c -> {
            c.setStatus(CaseStatus.CLOSED);
            caseRepository.save(c);
        });
    }

    public void closeChatSession(Long userId, String clientSessionKey) {
        chatSessionRepository.findByClientSessionKey(clientSessionKey).ifPresent(s -> {
            if (userId != null && s.getUserId() != null && !s.getUserId().equals(userId)) return;
            s.setStatus(ChatSessionStatus.CLOSED);
            chatSessionRepository.save(s);
        });
    }

    private String extractTop1Disease(PredictResponse mlRes) {
        if (mlRes == null || mlRes.getTop3() == null || mlRes.getTop3().isEmpty()) return null;
        Object disease = mlRes.getTop3().get(0).get("disease");
        return disease == null ? null : String.valueOf(disease);
    }

    private String toJsonSafe(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            if (value instanceof Set<?> s) return s.toString();
            if (value instanceof List<?> l) return l.toString();
            if (value instanceof Map<?, ?> m) return new LinkedHashMap<>(m).toString();
            return String.valueOf(value);
        }
    }
}