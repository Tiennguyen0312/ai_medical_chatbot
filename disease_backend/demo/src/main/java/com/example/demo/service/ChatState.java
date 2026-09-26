package com.example.demo.service;
import com.example.demo.dto.PredictResponse;

public class ChatState {
    private boolean awaitingAdviceConsent = false;
    private PredictResponse lastPredict = null;
    private String lastBestDisease = null;
    public boolean isAwaitingAdviceConsent() { return awaitingAdviceConsent; }
    public void setAwaitingAdviceConsent(boolean v) { this.awaitingAdviceConsent = v; }
    public PredictResponse getLastPredict() { return lastPredict; }
    public void setLastPredict(PredictResponse lastPredict) { this.lastPredict = lastPredict; }
    public String getLastBestDisease() { return lastBestDisease; }
    public void setLastBestDisease(String lastBestDisease) { this.lastBestDisease = lastBestDisease; }
}
