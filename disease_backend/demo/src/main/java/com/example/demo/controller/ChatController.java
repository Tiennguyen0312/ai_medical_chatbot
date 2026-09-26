package com.example.demo.controller;
import com.example.demo.auth.UserRepository;
import com.example.demo.conversation.ChatSessionEntity;
import com.example.demo.conversation.ConversationPersistenceService;
import com.example.demo.conversation.MessageEntity;
import com.example.demo.conversation.MessageRepository;
import com.example.demo.conversation.MessageRole;
import com.example.demo.dto.ChatRequest;
import com.example.demo.dto.ChatResponse;
import com.example.demo.dto.PredictRequest;
import com.example.demo.dto.PredictResponse;
import com.example.demo.service.ChatPersonalityService;
import com.example.demo.service.ChatSessionStore;
import com.example.demo.service.SymptomExtractor;
import com.example.demo.dto.MessageDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class ChatController {
    private final RestClient mlRestClient;
    private final Set<String> seenSessions = ConcurrentHashMap.newKeySet();
    private final SymptomExtractor symptomExtractor;
    private final ConversationPersistenceService conversationPersistenceService;
    private final ChatSessionStore chatSessionStore;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatPersonalityService personality;
    public ChatController(RestClient mlRestClient,
                          SymptomExtractor symptomExtractor,
                          ChatSessionStore chatSessionStore,
                          ConversationPersistenceService conversationPersistenceService,
                          MessageRepository messageRepository,
                          UserRepository userRepository,
                          ChatPersonalityService personality) {
        this.mlRestClient = mlRestClient;
        this.symptomExtractor = symptomExtractor;
        this.chatSessionStore = chatSessionStore;
        this.conversationPersistenceService = conversationPersistenceService;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.personality = personality;}
    @PostMapping("/predict")
    public PredictResponse predict(@RequestBody PredictRequest request) {
        try {
            return mlRestClient.post()
                    .uri("/predict")
                    .body(request)
                    .retrieve()
                    .body(PredictResponse.class);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "ML service is unavailable or an error",
                    ex
            );
        }
    }
    @GetMapping("/chat/messages")
    public List<MessageDto> getCurrentChatMessages(
            @RequestParam("session_id") String sessionId,
            @RequestParam String username
    ) {
        if (sessionId == null || sessionId.isBlank()) {
            return List.of();
        }

        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing username");
        }

        Long userId = userRepository.findByUsername(username.trim())
                .map(u -> u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));

        ChatSessionEntity session = conversationPersistenceService
                .findOwnedChatSession(userId, sessionId)
                .orElse(null);

        if (session == null) {
            return List.of();
        }
        List<MessageEntity> msgs = messageRepository.findAllByChatSessionIdOrderByCreatedAtAsc(session.getId());

        return msgs.stream()
                .map(m -> new MessageDto(
                        m.getId(),
                        m.getRole().name(),
                        m.getContent(),
                        String.valueOf(m.getCreatedAt())
                ))
                .toList();
    }
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest req) {

        String sessionId = req.getSession_id();
        String msg = (req.getMessage() == null) ? "" : req.getMessage().trim();
        String lower = msg.toLowerCase(Locale.ROOT);
        String username = (req.getUsername() == null) ? null : req.getUsername().trim();
        Long userId = null;
        if (username != null && !username.isBlank()) {
            userId = userRepository.findByUsername(username).map(u -> u.getId()).orElse(null);}
        if (sessionId == null || sessionId.isBlank()) {
            return new ChatResponse("Missing session_id.");}
        seenSessions.add(sessionId);
        ChatSessionEntity dbSession = conversationPersistenceService.getOrCreateChatSession(userId, sessionId);
        
        String intent = personality.detectIntent(msg);
        boolean isEndCmd = lower.equals("end") || lower.equals("3") || lower.contains("end chat");
        if (!isEndCmd && !msg.isBlank()) {
            saveMessage(dbSession, MessageRole.USER, msg);
        }
        if (isEndCmd) {
            String reply = personality.pick("goodbye", username);
            conversationPersistenceService.closeOpenCase(userId, sessionId);
            conversationPersistenceService.closeChatSession(userId, sessionId);
            chatSessionStore.clear(sessionId);
            saveMessage(dbSession, MessageRole.BOT, reply);
            return new ChatResponse(reply, null, "ENDED");
            
        }
        if ("goodbye".equals(intent)) {
            String reply = personality.pick("goodbye", username);
            conversationPersistenceService.closeOpenCase(userId, sessionId);
            conversationPersistenceService.closeChatSession(userId, sessionId);
            chatSessionStore.clear(sessionId);
            saveMessage(dbSession, MessageRole.BOT, reply);
            return new ChatResponse(reply, null, "ENDED_DELAYED");
        }
        ChatResponse sideRes = handleSideIntent(sessionId, dbSession, intent, username);
        if (sideRes != null) return sideRes;
        if (chatSessionStore.isAwaitingWorseningAskedBefore(sessionId)) {
            if (lower.equals("1") || isYes(lower)) {
                chatSessionStore.setAwaitingWorseningAskedBefore(sessionId, false);
                chatSessionStore.setAwaitingWorseningDuration(sessionId, true);
                String reply = personality.pick("worsening_ask_duration", username);
                saveMessage(dbSession, MessageRole.BOT, reply);
                return new ChatResponse(
                        reply,
                        List.of("1) less than 1 week.", "2) more than 1 week"),
                        "AWAITING_WORSENING_DURATION"
                );
            }
            if (lower.equals("2") || isNo(lower)) {
                chatSessionStore.setAwaitingWorseningAskedBefore(sessionId, false);
                String reply = personality.pick("ask_symptoms", username);
                saveMessage(dbSession, MessageRole.BOT, reply);
                return new ChatResponse(reply, null, "COLLECTING_SYMPTOMS");
            }
            return redirectToHelp(sessionId, userId, dbSession, username);
        }
        if (chatSessionStore.isAwaitingWorseningDuration(sessionId)) {
            if (isOneWeekOrLess(lower)) {
                chatSessionStore.setAwaitingWorseningDuration(sessionId, false);

                String reply = personality.pick("ask_symptoms", username);
                saveMessage(dbSession, MessageRole.BOT, reply);
                return new ChatResponse(reply, null, "COLLECTING_SYMPTOMS");
            }
            if (isMoreThanAWeek(lower) || looksLikeLongDuration(lower)) {
                chatSessionStore.setAwaitingWorseningDuration(sessionId, false);
                return redirectToHelp(sessionId, userId, dbSession, username);
            }
            return redirectToHelp(sessionId, userId, dbSession, username);
        }
        if (chatSessionStore.isAwaitingNextAction(sessionId)) {
            int action = parseNextAction(lower);
            PredictResponse last = chatSessionStore.getLastPredict(sessionId);
            switch (action) {
                case 1 -> {
                    chatSessionStore.setAwaitingNextAction(sessionId, false);
                    String reply = "Okay. " + personality.pick("ask_symptoms", username);
                    saveMessage(dbSession, MessageRole.BOT, reply);
                    return new ChatResponse(reply, null, "COLLECTING_SYMPTOMS");
                }
                
                case 2 -> {
                    conversationPersistenceService.closeOpenCase(userId, sessionId);
                    chatSessionStore.clear(sessionId);

                    String reply = "Started a new case.\n" + personality.pick("ask_symptoms", username);
                    saveMessage(dbSession, MessageRole.BOT, reply);
                    return new ChatResponse(reply, null, "COLLECTING_SYMPTOMS");
                }
                case 3 -> {
                    conversationPersistenceService.closeOpenCase(userId, sessionId);
                    conversationPersistenceService.closeChatSession(userId, sessionId);
                    chatSessionStore.clear(sessionId);

                    String reply = personality.pick("goodbye", username);
                    saveMessage(dbSession, MessageRole.BOT, reply);
                    return new ChatResponse(reply, null, "ENDED");
                }
                default -> {      
                    String reply = personality.pick("after_advice_next", username);
                    saveMessage(dbSession, MessageRole.BOT, reply);
                    return new ChatResponse(reply, nextActionOptions(), "AWAITING_NEXT_ACTION");
                }
            }
        }
        if (chatSessionStore.isAwaitingAdvice(sessionId)) {
            if (isYes(lower)) {
                chatSessionStore.setAwaitingAdvice(sessionId, false);
                PredictResponse last = chatSessionStore.getLastPredict(sessionId);
                if (last == null || last.getAdvice() == null) {
                    String reply = personality.pick("fallback_general", username);
                    saveMessage(dbSession, MessageRole.BOT, reply);
                    return new ChatResponse(reply);
                }
                conversationPersistenceService.markAdviceShown(userId, sessionId);
                chatSessionStore.setAwaitingNextAction(sessionId, true);
                String adviceText = formatAdvice(last) + "\n\n" + personality.pick("after_advice_next", username);
                saveMessage(dbSession, MessageRole.BOT, adviceText);
                return new ChatResponse(adviceText, nextActionOptions(), "AWAITING_NEXT_ACTION");
            }
            if (isNo(lower)) {
                chatSessionStore.setAwaitingAdvice(sessionId, false);
                chatSessionStore.setAwaitingNextAction(sessionId, true);

                String reply = "Okay — no treatment advice for now.\n\n" + personality.pick("after_advice_next", username);
                saveMessage(dbSession, MessageRole.BOT, reply);
                return new ChatResponse(reply, nextActionOptions(), "AWAITING_NEXT_ACTION");
            }
            String reply = personality.pick("after_prediction_yesno", username);
            saveMessage(dbSession, MessageRole.BOT, reply);
            return new ChatResponse(reply);
        }
        if (chatSessionStore.getSymptoms(sessionId).size() < 4) {
        	if ("worsening".equals(intent)
        	        && !chatSessionStore.isAwaitingWorseningAskedBefore(sessionId)
        	        && !chatSessionStore.isAwaitingWorseningDuration(sessionId)) {

        	    chatSessionStore.setAwaitingWorseningAskedBefore(sessionId, true);
        	    String reply = personality.pick("worsening_ask_before", username);
        	    saveMessage(dbSession, MessageRole.BOT, reply);
        	    return new ChatResponse(
        	            reply,
        	            List.of("Yes", "No"),
        	            "AWAITING_WORSENING_ASKED_BEFORE"
        	    );
        	}
            if (intent.equals("greeting")) {
                String reply = personality.pick("greeting", username);
                saveMessage(dbSession, MessageRole.BOT, reply);
                return new ChatResponse(reply);
            }
            if (intent.equals("feeling_ok")) {
                String reply = personality.pick("followup_feeling_ok", username);
                saveMessage(dbSession, MessageRole.BOT, reply);
                return new ChatResponse(reply);
            }
            if (intent.equals("feeling_bad")) {
                String reply = personality.pick("followup_feeling_bad", username) + "\n" + personality.pick("ask_symptoms", username);
                saveMessage(dbSession, MessageRole.BOT, reply);
                return new ChatResponse(reply);
            }
        }
        Set<String> extracted = symptomExtractor.extract(msg);

        if (extracted == null || extracted.isEmpty()) {
            String reply = personality.pick("unknown_symptom", username);
            saveMessage(dbSession, MessageRole.BOT, reply);
            return new ChatResponse(reply);
        }
        Set<String> allSymptoms = chatSessionStore.addSymptoms(sessionId, extracted);

        if (allSymptoms.size() < 4) {
            String reply = personality.pick("ask_more_symptoms", username) + "\n\nCurrent symptoms: " + allSymptoms;
            saveMessage(dbSession, MessageRole.BOT, reply);
            return new ChatResponse(reply);
        }
        PredictRequest pr = new PredictRequest();
        pr.setText(msg);
        pr.setSymptoms(new ArrayList<>(allSymptoms));
        PredictResponse mlRes;
        try {
            mlRes = mlRestClient.post()
                    .uri("/predict")
                    .body(pr)
                    .retrieve()
                    .body(PredictResponse.class);
        } catch (HttpStatusCodeException ex) {
            String body = ex.getResponseBodyAsString();
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "ML service error: " + body,
                    ex
            );
        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "ML service is unavailable or returned an error",
                    ex
            );
        }
        String topText = formatTop3(mlRes);
        chatSessionStore.setLastPredict(sessionId, mlRes);
        chatSessionStore.setAwaitingAdvice(sessionId, true);
        chatSessionStore.setAwaitingNextAction(sessionId, false);
        conversationPersistenceService.createOrUpdateOpenCase(
                userId,
                sessionId,
                allSymptoms,
                mlRes,
                false
        );
        String reply =
                "Based on your symptoms " + allSymptoms +
                        ", top suggested conditions: " + topText +
                        ".\n\n" + personality.pick("after_prediction_yesno", username);

        saveMessage(dbSession, MessageRole.BOT, reply);
        return new ChatResponse(reply, null, "AWAITING_TREATMENT_CONFIRMATION");
    }
    private boolean isYes(String lower) {
        return lower.equals("yes") || lower.equals("y") || lower.equals("ok")
                || lower.equals("okay") || lower.equals("sure");
    }
    private boolean isNo(String lower) {
        return lower.equals("no") || lower.equals("n") || lower.equals("nope")
                || lower.equals("nah");
    }
    private ChatResponse handleSideIntent(String sessionId,
                                         ChatSessionEntity dbSession,
                                         String intent,
                                         String username) {
        if (!personality.isSideIntent(intent)) return null;

        String side = personality.pickForSideIntent(intent, username);
        if (side == null || side.isBlank()) return null;

        if (chatSessionStore.isAwaitingAdvice(sessionId)) {
            String reply = side + "\n\n" + personality.pick("after_prediction_yesno", username);
            saveMessage(dbSession, MessageRole.BOT, reply);
            return new ChatResponse(reply, null, "AWAITING_TREATMENT_CONFIRMATION");
        }

        if (chatSessionStore.isAwaitingNextAction(sessionId)) {
            String reply = side + "\n\n" + personality.pick("after_advice_next", username);
            saveMessage(dbSession, MessageRole.BOT, reply);
            return new ChatResponse(reply, nextActionOptions(), "AWAITING_NEXT_ACTION");
        }

        int currentCount = chatSessionStore.getSymptoms(sessionId).size();
        String prompt = (currentCount > 0)
                ? personality.pick("ask_more_symptoms", username) + "\n\nCurrent symptoms: " + chatSessionStore.getSymptoms(sessionId)
                : personality.pick("ask_symptoms", username);

        String reply = side + "\n\n" + prompt;
        saveMessage(dbSession, MessageRole.BOT, reply);
        return new ChatResponse(reply, null, "COLLECTING_SYMPTOMS");
    }
    private List<String> nextActionOptions() {
        return List.of(
                "1.Add more symptoms",        
                "2.Start a new case",
                "3.End chat"
        );
    }
    private int parseNextAction(String lower) {
        String s = (lower == null) ? "" : lower.trim().toLowerCase(Locale.ROOT);

        if (s.equals("1") || s.contains("add more symptom") || s.contains("more symptom") || s.contains("refine")) return 1;     
        if (s.equals("2") || s.contains("new case") || s.contains("start new")) return 2;
        if (s.equals("3") || s.contains("end chat") || s.equals("end") || s.equals("quit") || s.equals("stop")) return 3;

        return 0;
    }
    private String formatTop3(PredictResponse mlRes) {
        List<Map<String, Object>> top3 = mlRes.getTop3();
        if (top3 == null || top3.isEmpty()) return "N/A";

        return top3.stream()
                .map(m -> {
                    Object d = m.get("disease");
                    Object p = m.get("confidence");
                    double prob = (p instanceof Number) ? ((Number) p).doubleValue() : 0.0;
                    return String.format("%s (%.0f%%)", String.valueOf(d), prob * 100);
                })
                .reduce((a, b) -> a + ", " + b)
                .orElse("N/A");
    }
    @SuppressWarnings("unchecked")
    private String formatAdvice(PredictResponse mlRes) {
        Map<String, Object> advice = mlRes.getAdvice();
        if (advice == null) return "No advice available.";

        StringBuilder sb = new StringBuilder();
        sb.append("Treatment advice (general):\n");
        appendList(sb, advice, "self_care", "Self-care");
        appendList(sb, advice, "otc", "OTC options");
        appendList(sb, advice, "avoid", "Avoid");
        appendList(sb, advice, "go_hospital_if", "Go to hospital if");
        Object notes = advice.get("notes");
        if (notes != null) sb.append("\nNotes: ").append(notes);
        Object src = advice.get("source");
        if (src != null) sb.append("\nSource: ").append(src);
        sb.append("\n\nDisclaimer: Academic demo only. Not a substitute for professional medical advice.");
        return sb.toString();
    }
    @SuppressWarnings("unchecked")
    private void appendList(StringBuilder sb, Map<String, Object> advice, String key, String title) {
        Object val = advice.get(key);
        if (!(val instanceof List)) return;
        List<Object> items = (List<Object>) val;
        if (items.isEmpty()) return;
        sb.append("\n").append(title).append(":\n");
        for (Object it : items) sb.append("- ").append(String.valueOf(it)).append("\n");
    }
    private void saveMessage(ChatSessionEntity session, MessageRole role, String content) {
        if (session == null || content == null) return;
        MessageEntity m = new MessageEntity();
        m.setChatSession(session);
        m.setRole(role);
        m.setContent(content);
        messageRepository.save(m);
    }
    private boolean looksLikeLongDuration(String lower) {
        if (lower == null) return false;
        String s = lower.trim().toLowerCase(Locale.ROOT);
        if (s.contains("month") || s.contains("months")) return true;
        if (s.contains("2 week") || s.contains("3 week") || s.contains("4 week")
                || s.contains("two week") || s.contains("three week") || s.contains("four week")
                || s.contains("for weeks") || s.contains("weeks ago")) {
            return true;
        }
        return false;
    }
    private boolean isOneWeekOrLess(String lower) {
        if (lower == null) return false;
        String s = lower.trim().toLowerCase(Locale.ROOT);
        return s.equals("1")
                || s.startsWith("1)")
                || s.startsWith("1.")
                || s.contains("≤ 1 week")
                || s.contains("<= 1 week")
                || s.contains("less than or equal to 1 week")
                || s.contains("less")
                || s.contains("under a week")
                || s.contains("within a week")
                || s.contains("one week");
    }
    private boolean isMoreThanAWeek(String lower) {
        if (lower == null) return false;
        String s = lower.trim().toLowerCase(Locale.ROOT);
        return s.equals("2")
                || s.startsWith("2)")
                || s.startsWith("2.")
                || s.contains("> 1 week")
                || s.contains("more than 1 week")
                || s.contains("more than a week")
                || s.contains("over a week")
                || s.contains("more");
    }
    private ChatResponse redirectToHelp(
            String sessionId,
            Long userId,
            ChatSessionEntity dbSession,
            String username
    ) {
        String reply = personality.pick("worsening_redirect_help", username);
        saveMessage(dbSession, MessageRole.BOT, reply);
        conversationPersistenceService.closeOpenCase(userId, sessionId);
        conversationPersistenceService.closeChatSession(userId, sessionId);
        chatSessionStore.clear(sessionId);
        return new ChatResponse(reply, null, "REDIRECT_HELP", "/helpcenter");
    }   
}