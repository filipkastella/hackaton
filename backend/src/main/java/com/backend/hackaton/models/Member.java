package com.backend.hackaton.models;

import java.time.LocalDateTime;
import java.util.UUID;

public class Member {
    private UUID id;
    private String name;
    private Position pos;
    private boolean host;
    private LocalDateTime lastPositionUpdate;
    private boolean isOnRoute;

    public Member(UUID id, String username, boolean host) {
        this.id = id;
        this.host = host;
        this.name = username;
        this.lastPositionUpdate = LocalDateTime.now();
        this.isOnRoute = false;
    }

    public Member() {
        this.lastPositionUpdate = LocalDateTime.now();
        this.isOnRoute = false;
    }

    public void setPos(Position pos) {
        this.pos = pos;
        this.lastPositionUpdate = LocalDateTime.now(); // Update timestamp when position changes
        this.isOnRoute = true; // User is active if updating position
    }

    public void updateRouteStatus(boolean onRoute) {
        this.isOnRoute = onRoute;
        if (onRoute) {
            this.lastPositionUpdate = LocalDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Position getPos() {
        return pos;
    }

    public boolean isHost() {
        return host;
    }

    public LocalDateTime getLastPositionUpdate() {
        return lastPositionUpdate;
    }

    public boolean isOnRoute() {
        return isOnRoute;
    }

    public void setLastPositionUpdate(LocalDateTime lastPositionUpdate) {
        this.lastPositionUpdate = lastPositionUpdate;
    }

    public void setOnRoute(boolean onRoute) {
        this.isOnRoute = onRoute;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setHost(boolean host) {
        this.host = host;
    }
}
