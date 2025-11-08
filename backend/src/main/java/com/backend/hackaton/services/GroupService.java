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
        return groupRepository.saveRecord(groupDTO);
    }

    public boolean joinGroup(UUID userId, String groupCode, String username) {
        try {
            GroupDTO group = groupRepository.getGroupByCode(groupCode);
            if (group == null) {
                return false;
            }
            
            group.addMember(new Member(userId, username, false));
            groupRepository.updateGroup(group);
            return true;
        } catch (Exception e) {
            System.err.println("Error joining group: " + e.getMessage());
            return false;
        }
    }

    public boolean updateMemberPosition(String groupCode, UUID userId, double latitude, double longitude) {
        try {
            GroupDTO group = groupRepository.getGroupByCode(groupCode);
            if (group == null) {
                return false;
            }

            // Find the member and update their position
            for (Member member : group.getMembers()) {
                if (member.getId().equals(userId)) {
                    Position newPosition = new Position();
                    newPosition.setLatitude(latitude);
                    newPosition.setLongitude(longitude);
                    
                    member.setPos(newPosition); // This also updates lastPositionUpdate
                    member.setOnRoute(true);
                    
                    group.updateLastActivity(); // Update group activity
                    groupRepository.updateGroup(group);
                    return true;
                }
            }
            
            return false; // Member not found
        } catch (Exception e) {
            System.err.println("Error updating member position: " + e.getMessage());
            return false;
        }
    }

    public GroupDTO getGroupByCode(String groupCode) {
        return groupRepository.getGroupByCode(groupCode);
    }

    public boolean updateMemberRouteStatus(String groupCode, UUID userId, boolean isOnRoute) {
        try {
            GroupDTO group = groupRepository.getGroupByCode(groupCode);
            if (group == null) {
                return false;
            }

            for (Member member : group.getMembers()) {
                if (member.getId().equals(userId)) {
                    member.updateRouteStatus(isOnRoute);
                    
                    if (isOnRoute) {
                        group.updateLastActivity();
                    }
                    
                    groupRepository.updateGroup(group);
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("Error updating member route status: " + e.getMessage());
            return false;
        }
    }
}
