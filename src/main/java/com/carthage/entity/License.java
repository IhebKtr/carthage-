package com.carthage.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class License {

    private UUID id;
    private String licenseCode;
    private boolean isUsed;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;
    private User assignedTo;

    public License() {}

    public License(UUID id, String licenseCode, boolean isUsed, LocalDateTime usedAt, LocalDateTime createdAt, User assignedTo) {
        this.id = id;
        this.licenseCode = licenseCode;
        this.isUsed = isUsed;
        this.usedAt = usedAt;
        this.createdAt = createdAt;
        this.assignedTo = assignedTo;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getLicenseCode() {
        return licenseCode;
    }

    public void setLicenseCode(String licenseCode) {
        this.licenseCode = licenseCode;
    }

    public boolean isIsUsed() {
        return isUsed;
    }

    public void setIsUsed(boolean isUsed) {
        this.isUsed = isUsed;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(User assignedTo) {
        this.assignedTo = assignedTo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        License that = (License) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(licenseCode, that.licenseCode) &&
                isUsed == that.isUsed &&
                Objects.equals(usedAt, that.usedAt) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(assignedTo, that.assignedTo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, licenseCode, isUsed, usedAt, createdAt, assignedTo);
    }
}
