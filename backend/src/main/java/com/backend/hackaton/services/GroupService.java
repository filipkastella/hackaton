package com.backend.hackaton.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.UUID;
import com.backend.hackaton.models.GroupDTO;
import com.backend.hackaton.repositories.GroupRepository;
import com.backend.hackaton.models.Member;
import com.backend.hackaton.models.Position;

@Service
public class GroupService {

    private final GroupRepository groupRepository;

    @Autowired
    public GroupService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    public GroupDTO createSession(GroupDTO groupDTO) {
        return groupRepository.saveNewRecord(groupDTO);
    }

    public GroupDTO joinGroup(UUID userId, String groupCode, String username) {

        GroupDTO group = groupRepository.getGroupByCode(groupCode);
        group.addMember(new Member(userId, username, false));
        groupRepository.updateRecord(group);
        return group;
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
