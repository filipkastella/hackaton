package com.backend.hackaton.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.hackaton.models.GroupDTO;
import com.backend.hackaton.models.GroupPostDTO;
import com.backend.hackaton.models.Member;
import com.backend.hackaton.services.GroupService;

@RestController
@RequestMapping("/api/trip")
public class GroupController {
    
    private final GroupService groupService;

    @Autowired
    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping("/groupMake")
    public ResponseEntity<GroupDTO> groupMake(@RequestBody GroupPostDTO data) {
        GroupDTO group = new GroupDTO();
        Member host = new Member(data.getHostId(), true);
        host.setPos(data.getHostPos());
        group.addMember(host);
        group.setDestination(data.getDestination());
        group = groupService.createSession(group);
        return ResponseEntity.ok(group);
        
    }

}
