package com.carthage.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class ReclamationResponse {

    private UUID id;
    private String message;
    private LocalDateTime createdAt;
    private boolean isAdminResponse;
    private Reclamation reclamation;
    private User author;

    public ReclamationResponse() {}

    public ReclamationResponse(UUID id, String message, LocalDateTime createdAt, boolean isAdminResponse, Reclamation reclamation, User author) {
        this.id = id;
        this.message = message;
        this.createdAt = createdAt;
        this.isAdminResponse = isAdminResponse;
        this.reclamation = reclamation;
        this.author = author;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isIsAdminResponse() {
        return isAdminResponse;
    }

    public void setIsAdminResponse(boolean isAdminResponse) {
        this.isAdminResponse = isAdminResponse;
    }

    public Reclamation getReclamation() {
        return reclamation;
    }

    public void setReclamation(Reclamation reclamation) {
        this.reclamation = reclamation;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReclamationResponse that = (ReclamationResponse) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(message, that.message) &&
                Objects.equals(createdAt, that.createdAt) &&
                isAdminResponse == that.isAdminResponse &&
                Objects.equals(reclamation, that.reclamation) &&
                Objects.equals(author, that.author);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, message, createdAt, isAdminResponse, reclamation, author);
    }
}
