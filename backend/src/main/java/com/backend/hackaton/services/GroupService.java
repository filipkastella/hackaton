package com.backend.hackaton.services;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.backend.hackaton.dto.GroupDTO;
import com.backend.hackaton.models.Member;
import com.backend.hackaton.repositories.GroupRepository;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
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
        if (group == null) return null;

        group.addMember(new Member(userId, username, false));
        groupRepository.updateRecord(group);
        return group;
    }

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
