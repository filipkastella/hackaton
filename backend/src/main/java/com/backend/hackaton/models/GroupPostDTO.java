package com.backend.hackaton.models;

public class GroupPostDTO {

    private int hostId;
    private Position destination;
    private Position hostPos;

    public Position getDestination() {
        return destination;
    }

    public int getHostId() {
        return hostId;
    }

    public Position getHostPos() {
        return hostPos;
    }

}
