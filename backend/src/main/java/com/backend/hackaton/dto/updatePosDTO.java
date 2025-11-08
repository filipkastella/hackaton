package com.backend.hackaton.dto;

import java.util.UUID;

import com.backend.hackaton.models.Position;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class updatePosDTO {

    private UUID userID;
    private Position newPos;

}