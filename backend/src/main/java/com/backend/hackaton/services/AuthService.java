package com.backend.hackaton.services;

import java.util.Random;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend.hackaton.dto.AuthResponse;
import com.backend.hackaton.dto.RegisterRequest;
import com.backend.hackaton.models.User;
import com.backend.hackaton.repositories.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final Random random = new Random();

    // Arrays of words to generate random nicknames
    private final String[] adjectives = {
        "Happy", "Brave", "Swift", "Clever", "Mighty", "Golden", "Silver", "Lightning", 
        "Shadow", "Bright", "Cool", "Epic", "Super", "Ninja", "Cosmic", "Royal"
    };
    
    private final String[] nouns = {
        "Warrior", "Explorer", "Hunter", "Wizard", "Knight", "Ranger", "Pilot", 
        "Guardian", "Champion", "Hero", "Master", "Legend", "Phoenix", "Dragon", "Tiger", "Eagle"
    };

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String nickname;
        User user;
        
        // Keep generating nicknames until we find a unique one
        do {
            nickname = generateRandomNickname();
        } while (userRepository.findByNickname(nickname).isPresent());
        
        // Create and save new user to PostgreSQL database
        user = new User();
        user.setNickname(nickname);
        user = userRepository.save(user);
        
        return new AuthResponse(user.getId(), user.getNickname());
    }

    private String generateRandomNickname() {
        String adjective = adjectives[random.nextInt(adjectives.length)];
        String noun = nouns[random.nextInt(nouns.length)];
        int number = random.nextInt(1000); // Random number 0-999
        
        return adjective + noun + number;
    }
}
