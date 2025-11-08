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
}
