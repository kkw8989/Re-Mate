package com.example.backend.dto;

import com.example.backend.entity.WorkspaceRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class WorkspaceMemberResponseDto {
    private Long userId;
    private String name;
    private String email;
    private String picture;
    private WorkspaceRole role;
}