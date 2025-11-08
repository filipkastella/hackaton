package com.backend.hackaton.services;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.backend.hackaton.dto.updatePosDTO;
import com.backend.hackaton.models.GroupDTO;
import com.backend.hackaton.models.Member;
import com.backend.hackaton.repositories.GroupRepository;

import lombok.extern.slf4j.Slf4j;

import com.backend.hackaton.models.Member;

@Service
@Slf4j
public class GroupService {

    private final GroupRepository groupRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public GroupService(GroupRepository groupRepository, SimpMessagingTemplate messagingTemplate) {
        this.groupRepository = groupRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public GroupDTO createSession(GroupDTO groupDTO) {
        return groupRepository.saveNewRecord(groupDTO);
    }

    public GroupDTO joinGroup(UUID userId, String groupCode, String username) {

        GroupDTO group = groupRepository.getGroupByCode(groupCode);
        if (!group.getMembers().stream().anyMatch(member -> member.getId().equals(userId))) {
            group.addMember(new Member(userId, username, false));
            groupRepository.updateRecord(group);
            return group;
        } else{
            log.info("User with ID {} is already a member of the group with code {}", userId, groupCode);
            return null;
        }
    }

    /**
     * Update member position in a group
     */
    public boolean updateMemberPosition(String groupCode, UUID userId, double latitude, double longitude) {
        try {
            GroupDTO group = groupRepository.getGroupByCode(groupCode);
            if (group == null) {
                return false;
            }

            // Find the member and update their position
            for (Member member : group.getMembers()) {
                if (member.getId().equals(userId)) {
                    Position newPosition = new Position((float) longitude, (float) latitude);
                    member.setPos(newPosition);
                    group.updateLastActivity(); // Update group's last activity
                    groupRepository.updateRecord(group);
                    return true;
                }
            }
            
            return false; // Member not found
        } catch (Exception e) {
            throw new RuntimeException("Error updating member position", e);
        }
    }
}
