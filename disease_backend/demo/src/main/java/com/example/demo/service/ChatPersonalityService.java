package com.example.demo.service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ChatPersonalityService {
    private final Map<String, List<String>> templates;
    private final Map<String, List<String>> intents;
    private static final Map<String, String> SIDE_INTENT_TO_TEMPLATE = Map.of(
            "thanks", "thanks_reply",
            "i_dont_understand", "confused_reply",
            "offtopic", "offtopic_reply",
            "offgeneral", "offgeneral_reply");
    public ChatPersonalityService() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            try (InputStream is = new ClassPathResource("chat_templates.json").getInputStream()) {
                templates = mapper.readValue(is, new TypeReference<>() {});
            }
            try (InputStream is = new ClassPathResource("intent_phrases.json").getInputStream()) {
                intents = mapper.readValue(is, new TypeReference<>() {});
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load personality JSON files", e);
        }
    }
    public String detectIntent(String text) {
        String t = norm(text);
        String[] order = new String[] {
                "goodbye",
                "worsening",
                "thanks",
                "i_dont_understand",
                "greeting",
                "feeling_bad",
                "feeling_ok",
                "offtopic",
                "offgeneral",
                "ask_advice",
                "ask_symptoms"
        };
        for (String k : order) {
            for (String p : intents.getOrDefault(k, List.of())) {
                if (!p.isBlank() && t.contains(norm(p))) return k;}}
        return "unknown";
    }
    public boolean isSideIntent(String intent) {
        return SIDE_INTENT_TO_TEMPLATE.containsKey(intent);
    }
    public String pickForSideIntent(String intent, String username) {
        String key = SIDE_INTENT_TO_TEMPLATE.get(intent);
        if (key == null) return "";
        return pick(key, username);
    }
    public String pick(String templateKey, String username) {
        List<String> list = templates.getOrDefault(templateKey, List.of());
        if (list.isEmpty()) return "";

        int idx = ThreadLocalRandom.current().nextInt(list.size());
        String raw = list.get(idx);

        String name = (username == null || username.isBlank()) ? "friend" : username.trim();
        return raw.replace("{username}", name);
    }
    private String norm(String s) {
        if (s == null) return "";
        String t = s.toLowerCase(Locale.ROOT).trim();
        t = t.replaceAll("[^a-z0-9\\s]", " ");
        t = t.replaceAll("\\s+", " ");
        return t;}
}