package com.backend.hackaton.dto;

import java.util.UUID;

import com.backend.hackaton.models.Position;

import lombok.Data;

@Data
public class WebSocketResponse {
    private int code;
    private String message;
    private String sessionCode;
    private UUID userId;
    private Position newPos;
}