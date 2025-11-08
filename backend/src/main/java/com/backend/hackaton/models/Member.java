package com.backend.hackaton.models;

import java.util.UUID;

public class Member {
    private UUID id;
    private String name;
    private Position pos;
    private boolean host;

    public Member(UUID id, String username, boolean host) {
        this.id = id;
        this.host = host;
        this.name = username;
    }

    public Member() {
    }

    public void setPos(Position pos) {
        this.pos = pos;
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

}
