package com.backend.hackaton.models;

public class Member {
    private int id;
    private String name;
    private Position pos;
    private boolean host;

    public Member(int id, boolean host) {
        this.id = id;
        this.host = host;
    }

    public Member() {
    }

    public void setPos(Position pos) {
        this.pos = pos;
    }

    public int getId() {
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
