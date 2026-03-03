package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WorkspaceMember {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long workspaceId;
  private Long userId;

  @Enumerated(EnumType.STRING)
  private WorkspaceRole role;

  @Enumerated(EnumType.STRING)
  private MembershipStatus status;

  public void updateStatus(MembershipStatus status) {
    this.status = status;
  }
}
