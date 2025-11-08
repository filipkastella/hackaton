package com.backend.hackaton.dto;
import java.util.UUID;

import com.backend.hackaton.models.Position;
public class GroupPostDTO {

    private UUID hostId;
    private Position destination;
    private Position hostPos;
    private String username;

    public String getUsername() {
        return username;
    }

    public Position getDestination() {
        return destination;
    }

    public UUID getHostId() {
        return hostId;
    }

    public Position getHostPos() {
        return hostPos;
    }

    public void setHostId(UUID hostId) {
        this.hostId = hostId;
    }

    public void setDestination(Position destination) {
        this.destination = destination;
    }

    public void setHostPos(Position hostPos) {
        this.hostPos = hostPos;
    }

    public void setUsername(String name) {
        this.username = name;
    }
}
