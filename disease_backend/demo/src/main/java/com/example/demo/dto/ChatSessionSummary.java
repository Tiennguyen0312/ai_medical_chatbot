package com.example.demo.dto;

public class ChatSessionSummary {
    private Long id;
    private String title;   
    private String status;  
    private long caseCount;
    public ChatSessionSummary() {}
    public ChatSessionSummary(Long id, String title, String status, long caseCount) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.caseCount = caseCount;}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getCaseCount() { return caseCount; }
    public void setCaseCount(long caseCount) { this.caseCount = caseCount; }
}