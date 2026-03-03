package com.example.backend.controller;

import com.example.backend.entity.Workspace;
import com.example.backend.entity.WorkspaceMember;
import com.example.backend.service.WorkspaceService;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

  private final WorkspaceService workspaceService;

  @PostMapping
  public ResponseEntity<Long> createWorkspace(@RequestParam String name, Principal principal) {
    return ResponseEntity.ok(workspaceService.createWorkspace(name, principal.getName()));
  }

  @PostMapping("/{workspaceId}/join")
  public ResponseEntity<WorkspaceMember> requestJoin(
      @PathVariable Long workspaceId, Principal principal) {
    return ResponseEntity.ok(workspaceService.requestJoin(workspaceId, principal.getName()));
  }

  @GetMapping("/{workspaceId}/pending")
  public ResponseEntity<List<WorkspaceMember>> getPendingMembers(
      @PathVariable Long workspaceId, Principal principal) {
    return ResponseEntity.ok(workspaceService.getPendingMembers(workspaceId, principal.getName()));
  }

  @PostMapping("/members/{membershipId}/approve")
  public ResponseEntity<Void> approve(@PathVariable Long membershipId, Principal principal) {
    workspaceService.approveMembership(membershipId, principal.getName());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/members/{membershipId}/reject")
  public ResponseEntity<Void> reject(@PathVariable Long membershipId, Principal principal) {
    workspaceService.rejectMembership(membershipId, principal.getName());
    return ResponseEntity.ok().build();
  }

  @GetMapping("/search")
  public ResponseEntity<List<Workspace>> search(@RequestParam String name) {
    return ResponseEntity.ok(workspaceService.searchWorkspaces(name));
  }

  @GetMapping("/my")
  public ResponseEntity<List<WorkspaceMember>> getMyWorkspaces(Principal principal) {
    return ResponseEntity.ok(workspaceService.getMyWorkspaces(principal.getName()));
  }
}
