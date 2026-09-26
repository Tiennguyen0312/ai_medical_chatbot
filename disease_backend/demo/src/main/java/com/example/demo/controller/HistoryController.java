package com.example.demo.controller;

import com.example.demo.auth.UserRepository;
import com.example.demo.conversation.*;
import com.example.demo.dto.*;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class HistoryController {
    private final ChatSessionRepository chatSessionRepository;
    private final CaseRepository caseRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    public HistoryController(ChatSessionRepository chatSessionRepository,
                             CaseRepository caseRepository,
                             MessageRepository messageRepository,
                             UserRepository userRepository) {
        this.chatSessionRepository = chatSessionRepository;
        this.caseRepository = caseRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;}
    private Long requireUserId(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing username");
        }
        return userRepository.findByUsername(username.trim())
                .map(u -> u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
    }
    private ChatSessionEntity requireOwnedSession(Long sessionId, Long userId) {
        ChatSessionEntity s = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat session not found"));

        if (s.getUserId() == null || !s.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your chat session");
        }
        return s;
    }
    @GetMapping("/history")
    public List<ChatSessionSummary> history(@RequestParam String username) {
        Long userId = requireUserId(username);
        List<ChatSessionEntity> sessions = chatSessionRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        List<ChatSessionSummary> out = new ArrayList<>();
        List<ChatSessionEntity> oldestFirst = new ArrayList<>(sessions);
        java.util.Collections.reverse(oldestFirst);
        java.util.Map<Long, String> titleMap = new java.util.HashMap<>();
        for (int i = 0; i < oldestFirst.size(); i++) {
            titleMap.put(oldestFirst.get(i).getId(), "Chat " + (i + 1));
        }
        for (ChatSessionEntity s : sessions) {
            long count = caseRepository.countByChatSessionId(s.getId());
            out.add(new ChatSessionSummary(
                    s.getId(),
                    titleMap.get(s.getId()),
                    s.getStatus().name(),
                    count
            ));
        }
        return out;
    }
    @GetMapping("/history/{sessionId}/cases")
    public List<CaseSummary> cases(@PathVariable Long sessionId, @RequestParam String username) {
        Long userId = requireUserId(username);
        requireOwnedSession(sessionId, userId);

        List<CaseEntity> cases = caseRepository.findAllByChatSessionIdOrderByUpdatedAtDesc(sessionId);
        return cases.stream()
                .map(c -> new CaseSummary(
                        c.getId(),
                        c.getStatus().name(),
                        c.getTop1Disease(),
                        c.isAdviceShown(),
                        String.valueOf(c.getUpdatedAt())
                ))
                .collect(Collectors.toList());
    }
    @GetMapping("/history/{sessionId}/messages")
    public List<MessageDto> messages(@PathVariable Long sessionId, @RequestParam String username) {
        Long userId = requireUserId(username);
        requireOwnedSession(sessionId, userId);

        List<MessageEntity> msgs = messageRepository.findAllByChatSessionIdOrderByCreatedAtAsc(sessionId);
        return msgs.stream()
                .map(m -> new MessageDto(
                        m.getId(),
                        m.getRole().name(),
                        m.getContent(),
                        String.valueOf(m.getCreatedAt())
                ))
                .collect(Collectors.toList());
    }

    @GetMapping("/history/{id}")
    public ChatSessionKeyResponse getSessionKey(@PathVariable Long id, @RequestParam String username) {
        Long userId = requireUserId(username);
        ChatSessionEntity s = requireOwnedSession(id, userId);

        return new ChatSessionKeyResponse(
                s.getId(),
                s.getClientSessionKey(),
                s.getStatus().name());
    }
}