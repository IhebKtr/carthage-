package com.carthage.entity;

import com.carthage.entity.enums.ReclamationCategory;
import com.carthage.entity.enums.ReclamationPriority;
import com.carthage.entity.enums.ReclamationStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Reclamation {

    private UUID id;
    private String subject;
    private String message;
    private ReclamationCategory category;
    private ReclamationPriority priority;
    private ReclamationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private User author;
    private List<ReclamationResponse> responses;

    public Reclamation() {}

    public Reclamation(UUID id, String subject, String message, ReclamationCategory category, ReclamationPriority priority, ReclamationStatus status, LocalDateTime createdAt, LocalDateTime updatedAt, User author, List<ReclamationResponse> responses) {
        this.id = id;
        this.subject = subject;
        this.message = message;
        this.category = category;
        this.priority = priority;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.author = author;
        this.responses = responses;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ReclamationCategory getCategory() {
        return category;
    }

    public void setCategory(ReclamationCategory category) {
        this.category = category;
    }

    public ReclamationPriority getPriority() {
        return priority;
    }

    public void setPriority(ReclamationPriority priority) {
        this.priority = priority;
    }

    public ReclamationStatus getStatus() {
        return status;
    }

    public void setStatus(ReclamationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public List<ReclamationResponse> getResponses() {
        return responses;
    }

    public void setResponses(List<ReclamationResponse> responses) {
        this.responses = responses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reclamation that = (Reclamation) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(subject, that.subject) &&
                Objects.equals(message, that.message) &&
                Objects.equals(category, that.category) &&
                Objects.equals(priority, that.priority) &&
                Objects.equals(status, that.status) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(updatedAt, that.updatedAt) &&
                Objects.equals(author, that.author) &&
                Objects.equals(responses, that.responses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, subject, message, category, priority, status, createdAt, updatedAt, author, responses);
    }
}
