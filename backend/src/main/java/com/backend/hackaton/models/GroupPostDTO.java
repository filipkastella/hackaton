package com.backend.hackaton.models;
import java.util.UUID;
public class GroupPostDTO {

    private UUID hostId;
    private Position destination;
    private Position hostPos;

    public Position getDestination() {
        return destination;
    }

    public UUID getHostId() {
        return hostId;
    }

    public Position getHostPos() {
        return hostPos;
    }

}
