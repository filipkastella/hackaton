package com.backend.hackaton.models;

import java.util.ArrayList;

public class GroupDTO {

    private String code;
    private Position destination;
    private ArrayList<Member> members = new ArrayList<>();

    public GroupDTO() {

    }

    public void addMember(Member member) {
        members.add(member);
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

    public void setDestination(Position destination) {
        this.destination = destination;
    }

    public void setCode(String code) {
        this.code = code;
    }

}
