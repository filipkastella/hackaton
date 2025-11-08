package com.backend.hackaton.repositories;

import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

import com.backend.hackaton.dto.GroupDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class GroupRepository {

    private final String PREFIX = "session:id:";
    private final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final int CODE_LENGTH = 7;
    private final int MAX_ATTEMPTS = 20;

    private final SecureRandom random = new SecureRandom();
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public GroupRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public GroupDTO saveNewRecord(GroupDTO group) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        String jsonValue;

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String id = randomCode();
            String key = PREFIX + id;

            // Attempt to set the key only if it does not exist
            String success = ops.get(key);

            if (success==null) {
                group.setCode(id);
                try {
                    jsonValue = objectMapper.writeValueAsString(group);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to serialize DTO to JSON", e);
                }

                ops.set(key, jsonValue); // update DTO with generated ID
                return group;     // return the generated code
            }
        }

        throw new IllegalStateException("Failed to generate unique ID after " + MAX_ATTEMPTS + " attempts");
    }

    public String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARSET.charAt(random.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }

    public GroupDTO getGroupByCode(String code) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        String key = PREFIX + code;
        String jsonValue = ops.get(key);

        if (jsonValue == null) {
            return null; // or throw an exception if preferred
        }

        try {
            return objectMapper.readValue(jsonValue, GroupDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize JSON to DTO", e);
        }
    }

    public GroupDTO updateRecord(GroupDTO group){
            if (group.getCode() == null || group.getCode().isEmpty()) {
            throw new IllegalArgumentException("Group code cannot be null or empty");
        }

        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        String key = PREFIX + group.getCode();

        // Check if the record exists
        String existingValue = ops.get(key);
        if (existingValue == null) {
            throw new IllegalStateException("Group with code " + group.getCode() + " does not exist");
        }

        // Serialize and update the record
        try {
            String jsonValue = objectMapper.writeValueAsString(group);
            ops.set(key, jsonValue);
            return group;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize DTO to JSON", e);
        }
    }

    /**
     * Get all groups from Redis for cleanup operations
     */
    public java.util.List<GroupDTO> getAllGroups() {
        java.util.Set<String> keys = redisTemplate.keys(PREFIX + "*");
        java.util.List<GroupDTO> groups = new java.util.ArrayList<>();
        
        if (keys != null) {
            ValueOperations<String, String> ops = redisTemplate.opsForValue();
            for (String key : keys) {
                String jsonValue = ops.get(key);
                if (jsonValue != null) {
                    try {
                        GroupDTO group = objectMapper.readValue(jsonValue, GroupDTO.class);
                        groups.add(group);
                    } catch (JsonProcessingException e) {
                        System.err.println("Failed to deserialize group from key: " + key);
                    }
                }
            }
        }
        
        return groups;
    }

    /**
     * Delete a group from Redis by group code
     */
    public Boolean deleteGroup(String groupCode) {
        String key = PREFIX + groupCode;
        Boolean deleted = redisTemplate.delete(key);
        System.out.println("Deleted group " + groupCode + " from Redis: " + deleted);
        return deleted;
    }

    /**
     * Update group data in Redis
     */
    public void updateGroup(GroupDTO group) {
        updateRecord(group);
    }

}
