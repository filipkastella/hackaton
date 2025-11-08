package com.backend.hackaton.services;

import com.backend.hackaton.dto.GroupDTO;
import com.backend.hackaton.repositories.GroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupCleanupService {

    private final GroupRepository groupRepository;

    /**
     * Scheduled task that runs every hour to automatically clean up inactive groups
     * Deletes groups that have been inactive for 24 hours with no users on route
     */
    @Scheduled(fixedRate = 3600000) // Run every hour (3600000 ms)
    public void cleanupInactiveGroups() {
        log.info("Starting automatic group cleanup...");
        
        try {
            List<GroupDTO> allGroups = groupRepository.getAllGroups();
            int deletedCount = 0;
            int checkedCount = allGroups.size();
            
            for (GroupDTO group : allGroups) {
                if (group.shouldBeDeleted()) {
                    deleteInactiveGroup(group);
                    deletedCount++;
                    log.info("Auto-deleted inactive group: {} (Code: {}, Created: {}, Last Activity: {})", 
                            group.getCode(), 
                            group.getCode(),
                            group.getCreatedAt(),
                            group.getLastActivity());
                }
            }
            
            log.info("Automatic cleanup completed. Checked: {}, Deleted: {}", 
                    checkedCount, deletedCount);
                    
        } catch (Exception e) {
            log.error("Error during automatic group cleanup", e);
        }
    }

    /**
     * Delete group from Redis database
     */
    private void deleteInactiveGroup(GroupDTO group) {
        try {
            groupRepository.deleteGroup(group.getCode());
            log.info("Successfully deleted group from database: {} with {} members", 
                    group.getCode(), group.getMembers().size());
        } catch (Exception e) {
            log.error("Failed to delete group from database: {}", group.getCode(), e);
        }
    }
}