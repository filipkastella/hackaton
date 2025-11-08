package com.backend.hackaton.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.backend.hackaton.dto.updatePosDTO;
import com.backend.hackaton.models.GroupDTO;
import com.backend.hackaton.repositories.GroupRepository;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public GroupService(GroupRepository groupRepository, SimpMessagingTemplate messagingTemplate) {
        this.groupRepository = groupRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public GroupDTO createSession(GroupDTO groupDTO) {
        return groupRepository.saveRecord(groupDTO);
    }

    public GroupDTO updatePos(updatePosDTO data) {
        return null;
    }
}
