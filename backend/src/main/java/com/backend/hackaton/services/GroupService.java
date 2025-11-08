package com.backend.hackaton.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.UUID;
import com.backend.hackaton.models.GroupDTO;
import com.backend.hackaton.repositories.GroupRepository;

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

    public boolean joinGroup(UUID userId, String groupCode) {

        GroupDTO group = groupRepository.findByCode(groupCode);
        group.addMember(new Member(userId, false));
        groupRepository.saveRecord(group);
        return true;
    }
}
