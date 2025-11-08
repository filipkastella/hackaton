package com.backend.hackaton.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.backend.hackaton.repositories.UserRepository;

@Service
public class UserService {

    private final GroupRepository groupRepository;
    private UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean joinGroup(UUID userId, String groupCode){

        GroupDTO group = groupRepository.findByCode(groupCode);
        group.addMember(new Member(userId, false));
        groupRepository.saveRecord(group);
        return true;
    }
}
