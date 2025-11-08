package com.backend.hackaton.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
