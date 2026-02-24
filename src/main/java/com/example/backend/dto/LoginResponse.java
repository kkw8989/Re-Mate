package com.example.backend.dto;

public record LoginResponse(
        String accessToken,
        String email,
        String name,
        int workspaceCount,    // 0이면 /workspaces/join으로 리다이렉트
        Long lastWorkspaceId
) {}