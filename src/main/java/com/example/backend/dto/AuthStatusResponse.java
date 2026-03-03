package com.example.backend.dto;

import com.example.backend.entity.WorkspaceRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthStatusResponse {
  private boolean authenticated;
  private String userEmail;
  private String message;
  private Long workspaceId;
  private String userName;
  private WorkspaceRole role;
  private Long userId;
}
