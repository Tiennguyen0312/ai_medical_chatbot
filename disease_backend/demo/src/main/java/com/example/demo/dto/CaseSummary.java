package com.example.demo.dto;

public class CaseSummary {
    private Long id;
    private String status;
    private String top1Disease;
    private boolean adviceShown;
    private String updatedAt;   
    public CaseSummary() {}
    public CaseSummary(Long id, String status, String top1Disease, boolean adviceShown, String updatedAt) {
        this.id = id;
        this.status = status;
        this.top1Disease = top1Disease;
        this.adviceShown = adviceShown;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTop1Disease() { return top1Disease; }
    public void setTop1Disease(String top1Disease) { this.top1Disease = top1Disease; }
    public boolean isAdviceShown() { return adviceShown; }
    public void setAdviceShown(boolean adviceShown) { this.adviceShown = adviceShown; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}