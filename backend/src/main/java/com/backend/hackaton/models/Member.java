package com.backend.hackaton.models;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a member of a group trip session.
 * 
 * <p>Tracks member information including their current position, host status,
 * and activity timestamps for session management.</p>
 * 
 * @author Hackaton Team
 * @version 1.0
 * @since 2025-11-08
 */
public class Member {
    private UUID id;
    private String name;
    private Position pos;
    private boolean host;
    private LocalDateTime lastPositionUpdate;
    private boolean isOnRoute;

    /**
     * Constructs a new Member with specified details.
     * 
     * @param id the unique identifier of the user
     * @param username the display name of the member
     * @param host whether this member is the host of the group
     */
    public Member(UUID id, String username, boolean host) {
        this.id = id;
        this.host = host;
        this.name = username;
        this.lastPositionUpdate = LocalDateTime.now();
        this.isOnRoute = false;
    }

    /**
     * Default constructor for deserialization.
     */
    public Member() {
        this.lastPositionUpdate = LocalDateTime.now();
        this.isOnRoute = false;
    }

    /**
     * Sets the member's position and updates activity timestamps.
     * 
     * @param pos the new geographic position
     */
    public void setPos(Position pos) {
        this.pos = pos;
        this.lastPositionUpdate = LocalDateTime.now(); // Update timestamp when position changes
        this.isOnRoute = true; // User is active if updating position
    }

    /**
     * Updates whether the member is currently on the planned route.
     * 
     * @param onRoute true if member is on route, false otherwise
     */
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
