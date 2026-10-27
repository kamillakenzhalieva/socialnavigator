package com.social_navigator_ar.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "import_runs")
public class ImportRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime startedAt = LocalDateTime.now();
    private LocalDateTime finishedAt;

    @Column(name = "trigger_type", length = 20)
    private String triggerType;

    @Column(length = 20)
    private String status = "RUNNING";

    private int fetched;
    private int added;
    private int duplicates;
    private int rejected;

    @Column(length = 1000)
    private String message;

    public Long getId() { return id; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getFetched() { return fetched; }
    public void setFetched(int fetched) { this.fetched = fetched; }
    public int getAdded() { return added; }
    public void setAdded(int added) { this.added = added; }
    public int getDuplicates() { return duplicates; }
    public void setDuplicates(int duplicates) { this.duplicates = duplicates; }
    public int getRejected() { return rejected; }
    public void setRejected(int rejected) { this.rejected = rejected; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
