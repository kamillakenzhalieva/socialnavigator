package com.social_navigator_ar.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "imported_vacancies",
        uniqueConstraints = @UniqueConstraint(columnNames = {"source", "external_id"}))
public class ImportedVacancy {

    public enum Status { NEW, VIEWED, CONTACTED, DISMISSED, AUTO_REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String source;

    @Column(name = "external_id", nullable = false, length = 200)
    private String externalId;

    @Column(length = 300)
    private String title;

    @Column(length = 300)
    private String personName;

    @Column(length = 30)
    private String category;

    @Column(length = 200)
    private String city;

    @Column(length = 1500)
    private String snippet;

    @Column(length = 1000)
    private String aiSummary;

    private int relevance;

    @Column(length = 1000)
    private String sourceUrl;

    @Column(length = 60)
    private String section;

    @Column(name = "reject_reason", length = 200)
    private String rejectReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.NEW;

    @Column(nullable = false)
    private LocalDateTime foundAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }
    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }
    public int getRelevance() { return relevance; }
    public void setRelevance(int relevance) { this.relevance = relevance; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getFoundAt() { return foundAt; }
    public void setFoundAt(LocalDateTime foundAt) { this.foundAt = foundAt; }
}
