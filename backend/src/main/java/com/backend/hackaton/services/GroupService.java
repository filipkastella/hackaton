package com.backend.hackaton.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.UUID;
import com.backend.hackaton.models.GroupDTO;
import com.backend.hackaton.repositories.GroupRepository;

import lombok.extern.slf4j.Slf4j;

import com.backend.hackaton.models.Member;

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
        if (!group.getMembers().stream().anyMatch(member -> member.getId().equals(userId))) {
            group.addMember(new Member(userId, username, false));
            groupRepository.updateRecord(group);
            return group;
        } else{
            log.info("User with ID {} is already a member of the group with code {}", userId, groupCode);
            return null;
        }
    }
}
