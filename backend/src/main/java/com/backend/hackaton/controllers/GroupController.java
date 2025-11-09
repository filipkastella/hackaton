package com.backend.hackaton.controllers;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backend.hackaton.dto.GroupDTO;
import com.backend.hackaton.dto.GroupPostDTO;
import com.backend.hackaton.dto.WebSocketResponse;
import com.backend.hackaton.dto.updatePosDTO;
import com.backend.hackaton.models.Member;
import com.backend.hackaton.services.GroupService;

/**
 * REST controller for managing group trip sessions.
 * 
 * <p>This controller handles HTTP endpoints for creating and joining group trips,
 * as well as WebSocket endpoints for real-time position updates.</p>
 * 
 * @author Hackaton Team
 * @version 1.0
 * @since 2025-11-08
 */
@RestController
@RequestMapping("/api/trip")
public class GroupController {
    
    private final GroupService groupService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Constructs a GroupController with required dependencies.
     * 
     * @param groupService the service for group operations
     * @param messagingTemplate the template for sending WebSocket messages
     */
    @Autowired
    public GroupController(GroupService groupService, SimpMessagingTemplate messagingTemplate) {
        this.groupService = groupService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Creates a new group trip session.
     * 
     * <p>This endpoint initializes a new group with the host member and generates
     * a unique group code for others to join.</p>
     * 
     * @param data the group creation request containing host information and destination
     * @return ResponseEntity with the created GroupDTO containing the group code
     */
    @PostMapping("/groupMake")
    public ResponseEntity<GroupDTO> groupMake(@RequestBody GroupPostDTO data) {
        GroupDTO group = new GroupDTO();
        Member host = new Member(data.getHostId(), data.getUsername(), true);
        host.setPos(data.getHostPos());
        group.addMember(host);
        group.setDestination(data.getDestination());
        group = groupService.createSession(group);
        return ResponseEntity.ok(group);
        
    }

    /**
     * Allows a user to join an existing group trip.
     * 
     * <p>Users provide their ID, username, and the group code to join.
     * They will be added as a non-host member of the group.</p>
     * 
     * @param userId the unique identifier of the user joining
     * @param groupCode the unique code of the group to join
     * @param username the display name of the user
     * @return ResponseEntity with the updated GroupDTO or 400 if join fails
     */
    @PostMapping("/joinGroup")
    public ResponseEntity<?> joinGroup(@RequestParam UUID userId, @RequestParam String groupCode, @RequestParam String username) {
        if(groupService.joinGroup(userId, groupCode, username) == null){
            return ResponseEntity.status(400).body("Failed to join group");
        } else {
            return ResponseEntity.ok(groupService.joinGroup(userId, groupCode, username));
        }
    }

/*     @PostMapping("/updatePosition")
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
    } */

    /**
     * WebSocket endpoint for real-time position updates.
     * 
     * <p>Receives position updates from group members via WebSocket and broadcasts
     * the update to all subscribers of the session.</p>
     * 
     * @param sessionCode the unique code of the group session
     * @param data the position update data containing user ID and new coordinates
     */
    @MessageMapping("/{sessionCode}")
    @SendTo("/receive/{sessionCode}")
    public void processMessage(@DestinationVariable String sessionCode, @RequestBody updatePosDTO data) {
        boolean success = groupService.updateUserCoordinates(sessionCode, data);

        WebSocketResponse response = new WebSocketResponse();
        response.setCode(success ? 200 : 400);
        response.setMessage(success ? "Coordinates updated" : "Failed to update user");
        response.setSessionCode(sessionCode);
        response.setUserId(data.getUserID());
        response.setNewPos(data.getNewPos());

        // Send broadcast to all subscribers of that session
        messagingTemplate.convertAndSend("/receive/" + sessionCode, response);
    }

}
