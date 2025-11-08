package com.backend.hackaton.repositories;

import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import com.backend.hackaton.models.GroupDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class GroupRepository {

    private final String PREFIX = "session:id:";
    private final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final int CODE_LENGTH = 7;
    private final int MAX_ATTEMPTS = 20;

    private final SecureRandom random = new SecureRandom();

    @Autowired
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public GroupRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public GroupDTO saveRecord(GroupDTO group) {
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

}
