package com.example.demo.dto;
public class ChatSessionKeyResponse {
    private Long id;
    private String clientSessionKey;
    private String status;
    public ChatSessionKeyResponse() {}
    public ChatSessionKeyResponse(Long id, String clientSessionKey, String status) {
        this.id = id;
        this.clientSessionKey = clientSessionKey;
        this.status = status;}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getClientSessionKey() { return clientSessionKey; }
    public void setClientSessionKey(String clientSessionKey) { this.clientSessionKey = clientSessionKey; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}