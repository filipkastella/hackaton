package com.backend.hackaton.controllers;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

import com.backend.hackaton.dto.GroupDTO;
import com.backend.hackaton.dto.GroupPostDTO;
import com.backend.hackaton.dto.updatePosDTO;
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
        Member host = new Member(data.getHostId(), data.getName(), true);
        host.setPos(data.getHostPos());
        group.addMember(host);
        group.setDestination(data.getDestination());
        group = groupService.createSession(group);
        return ResponseEntity.ok(group);
        
    }

    @PostMapping("/joinGroup")
    public ResponseEntity<?> joinGroup(UUID userId, String groupCode, String username) {
        if(groupService.joinGroup(userId, groupCode, username) == null){
            return ResponseEntity.status(400).body("Failed to join group");
        } else {
            return ResponseEntity.ok(groupService.joinGroup(userId, groupCode, username));
        }
    }

    @PostMapping("/updatePosition")
    public ResponseEntity<?> updateMemberPosition(@RequestParam String groupCode,
                                                @RequestParam UUID userId,
                                                @RequestParam double latitude,
                                                @RequestParam double longitude) {
        try {
            boolean updated = groupService.updateMemberPosition(groupCode, userId, latitude, longitude);
            if (updated) {
                return ResponseEntity.ok().body("Position updated successfully");
            } else {
                return ResponseEntity.status(404).body("Group or member not found");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error updating position: " + e.getMessage());
        }
    }

    @MessageMapping("/{sessionCode}")
    @SendTo("/receive/{sessionCode}")
    public void processMessage(@DestinationVariable String sessionCode, @RequestBody updatePosDTO data) {
        
    }

}
