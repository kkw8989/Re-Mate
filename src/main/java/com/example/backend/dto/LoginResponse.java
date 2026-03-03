package com.example.backend.dto;

public record LoginResponse(
    String accessToken, String email, String name, int workspaceCount, Long lastWorkspaceId) {}
