package com.example.demo.service;

import com.example.demo.dto.PredictResponse;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatSessionStore {
    private final Map<String, Set<String>> sessionSymptoms = new ConcurrentHashMap<>();
    private final Map<String, Boolean> awaitingAdvice = new ConcurrentHashMap<>();
    private final Map<String, Boolean> awaitingNextAction = new ConcurrentHashMap<>();
    private final Map<String, PredictResponse> lastPredict = new ConcurrentHashMap<>();
    private final Map<String, Boolean> awaitingWorseningAskedBefore = new ConcurrentHashMap<>();
    private final Map<String, Boolean> awaitingWorseningDuration = new ConcurrentHashMap<>();
    public Set<String> addSymptoms(String sessionId, Set<String> newOnes) {
        sessionSymptoms.putIfAbsent(sessionId, ConcurrentHashMap.newKeySet());
        if (newOnes != null) sessionSymptoms.get(sessionId).addAll(newOnes);
        return sessionSymptoms.get(sessionId);
    }
    public Set<String> getSymptoms(String sessionId) {
        return sessionSymptoms.getOrDefault(sessionId, Set.of());
    }
    public void setAwaitingAdvice(String sessionId, boolean value) {
        awaitingAdvice.put(sessionId, value);
    }

    public boolean isAwaitingAdvice(String sessionId) {
        return awaitingAdvice.getOrDefault(sessionId, false);
    }

    public void setAwaitingNextAction(String sessionId, boolean value) {
        awaitingNextAction.put(sessionId, value);
    }

    public boolean isAwaitingNextAction(String sessionId) {
        return awaitingNextAction.getOrDefault(sessionId, false);
    }
    public void setLastPredict(String sessionId, PredictResponse res) {
        lastPredict.put(sessionId, res);
    }

    public PredictResponse getLastPredict(String sessionId) {
        return lastPredict.get(sessionId);
    }

    public void clearSymptoms(String sessionId) {
        sessionSymptoms.remove(sessionId);
    }
    public void clear(String sessionId) {
        sessionSymptoms.remove(sessionId);
        awaitingAdvice.remove(sessionId);
        awaitingNextAction.remove(sessionId);
        lastPredict.remove(sessionId);
        awaitingWorseningAskedBefore.remove(sessionId);
        awaitingWorseningDuration.remove(sessionId);
    }
    public void setAwaitingWorseningAskedBefore(String sessionId, boolean v) {
        awaitingWorseningAskedBefore.put(sessionId, v);
    }
    public boolean isAwaitingWorseningAskedBefore(String sessionId) {
        return awaitingWorseningAskedBefore.getOrDefault(sessionId, false);
    }
    public void setAwaitingWorseningDuration(String sessionId, boolean v) {
        awaitingWorseningDuration.put(sessionId, v);
    }
    public boolean isAwaitingWorseningDuration(String sessionId) {
        return awaitingWorseningDuration.getOrDefault(sessionId, false);
    }
}