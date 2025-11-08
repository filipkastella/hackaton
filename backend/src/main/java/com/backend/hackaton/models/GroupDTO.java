package com.backend.hackaton.models;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class GroupDTO {

    private String code;
    private Position destination;
    private ArrayList<Member> members = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime lastActivity;
    private boolean isActive;

    public GroupDTO() {
        this.createdAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
        this.isActive = true;
    }

    public void addMember(Member member) {
        members.add(member);
        updateLastActivity();
    }

    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    public Member getMember(int index) {
        return members.get(index);
    }

    public ArrayList<Member> getMembers() {
        return members;
    }

    public Position getDestination() {
        return destination;
    }

    public String getCode() {
        return code;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setDestination(Position destination) {
        this.destination = destination;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    /**
     * Check if any member is currently on route (has updated position recently)
     */
    public boolean hasActiveMembers() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30); // 30 minutes threshold
        return members.stream().anyMatch(member -> 
            member.getLastPositionUpdate() != null && 
            member.getLastPositionUpdate().isAfter(threshold)
        );
    }

    /**
     * Check if group should be deleted (no activity for 24 hours)
     */
    public boolean shouldBeDeleted() {
        LocalDateTime deletionThreshold = LocalDateTime.now().minusHours(24);
        return lastActivity.isBefore(deletionThreshold) && !hasActiveMembers();
    }
}
