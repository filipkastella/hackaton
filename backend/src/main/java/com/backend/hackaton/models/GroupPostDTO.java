package com.backend.hackaton.models;
import java.util.UUID;
public class GroupPostDTO {

    private UUID hostId;
    private String hostUsername;
    private Position destination;
    private Position hostPos;
    private String name;

    public String getName() {
        return name;
    }

    public Position getDestination() {
        return destination;
    }

    public UUID getHostId() {
        return hostId;
    }

    public String getHostUsername() {
        return hostUsername;
    }

    public Position getHostPos() {
        return hostPos;
    }

    public void setHostId(UUID hostId) {
        this.hostId = hostId;
    }

    public void setHostUsername(String hostUsername) {
        this.hostUsername = hostUsername;
    }

    public void setDestination(Position destination) {
        this.destination = destination;
    }

    public void setHostPos(Position hostPos) {
        this.hostPos = hostPos;
    }
}
