package com.example.demo.conversation;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cases")
public class CaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_session_id", nullable = false)
    private ChatSessionEntity chatSession;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CaseStatus status = CaseStatus.OPEN;
    @Lob
    @Column(name = "symptoms_json")
    private String symptomsJson;
    @Column(name = "top1_disease", length = 120)
    private String top1Disease;
    @Lob
    @Column(name = "top3_json")
    private String top3Json;
    @Column(name = "advice_shown", nullable = false)
    private boolean adviceShown = false;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) this.status = CaseStatus.OPEN;
    }
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    public Long getId() { return id; }
    
    public ChatSessionEntity getChatSession() { return chatSession; }
    public void setChatSession(ChatSessionEntity chatSession) { this.chatSession = chatSession; }

    public CaseStatus getStatus() { return status; }
    public void setStatus(CaseStatus status) { this.status = status; }

    public String getSymptomsJson() { return symptomsJson; }
    public void setSymptomsJson(String symptomsJson) { this.symptomsJson = symptomsJson; }

    public String getTop1Disease() { return top1Disease; }
    public void setTop1Disease(String top1Disease) { this.top1Disease = top1Disease; }

    public String getTop3Json() { return top3Json; }
    public void setTop3Json(String top3Json) { this.top3Json = top3Json; }

    public boolean isAdviceShown() { return adviceShown; }
    public void setAdviceShown(boolean adviceShown) { this.adviceShown = adviceShown; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}