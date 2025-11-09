package com.backend.hackaton.services;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.backend.hackaton.dto.GroupDTO;
import com.backend.hackaton.dto.updatePosDTO;
import com.backend.hackaton.models.Member;
import com.backend.hackaton.repositories.GroupRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Service layer for managing group trip sessions.
 * 
 * <p>This service handles business logic for creating groups, managing memberships,
 * and updating member positions in real-time collaborative trip sessions.</p>
 * 
 * @author Hackaton Team
 * @version 1.0
 * @since 2025-11-08
 */
@Service
@Slf4j
public class GroupService {

    private final GroupRepository groupRepository;

    /**
     * Constructs a GroupService with the required repository.
     * 
     * @param groupRepository the repository for group data persistence
     */
    @Autowired
    public GroupService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    /**
     * Creates a new group session.
     * 
     * <p>Generates a unique group code and persists the group data to Redis.</p>
     * 
     * @param groupDTO the group data transfer object containing initial group information
     * @return the created GroupDTO with generated group code
     */
    public GroupDTO createSession(GroupDTO groupDTO) {
        return groupRepository.saveNewRecord(groupDTO);
    }

    /**
     * Adds a user to an existing group.
     * 
     * <p>Creates a new non-host member and adds them to the group. Updates the
     * group record in Redis with the new member.</p>
     * 
     * @param userId the unique identifier of the user joining
     * @param groupCode the code of the group to join
     * @param username the display name of the user
     * @return the updated GroupDTO with the new member, or null if group not found
     */
    public GroupDTO joinGroup(UUID userId, String groupCode, String username) {
        GroupDTO group = groupRepository.getGroupByCode(groupCode);
        if (group == null) return null;

        group.addMember(new Member(userId, username, false));
        groupRepository.updateRecord(group);
        return group;
    }

    /**
     * Updates a member's GPS coordinates within a group.
     * 
     * <p>Locates the member by user ID and updates their position. Also updates
     * the last activity timestamp for the group.</p>
     * 
     * @param code the group code
     * @param data the position update data containing user ID and new coordinates
     * @return true if update was successful, false if group or member not found
     */
    public boolean updateUserCoordinates(String code, updatePosDTO data) {
        GroupDTO group = groupRepository.getGroupByCode(code);
        if (group == null || group.getMembers() == null) {
            return false;
        }

        Optional<Member> existingUserOpt = group.getMembers().stream()
                .filter(u -> u.getId().equals(data.getUserID()))
                .findFirst();

        if (existingUserOpt.isEmpty()) {
            return false;
        }

        Member user = existingUserOpt.get();
        user.setPos(data.getNewPos());

        // Save updated record
        groupRepository.updateRecord(group);
        return true;
    }
}
