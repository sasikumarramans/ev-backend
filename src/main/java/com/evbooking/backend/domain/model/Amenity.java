package com.evbooking.backend.domain.model;

import java.time.LocalDateTime;

public class Amenity {
    private Long id;
    private String name;
    private String description;
    private String icon;
    private AmenityType type;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Amenity() {}

    public Amenity(String name, String description, String icon, AmenityType type) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.type = type;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public AmenityType getType() { return type; }
    public void setType(AmenityType type) { this.type = type; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public enum AmenityType {
        COMFORT,
        CONVENIENCE,
        SAFETY,
        ENTERTAINMENT,
        FOOD_BEVERAGE
    }
}