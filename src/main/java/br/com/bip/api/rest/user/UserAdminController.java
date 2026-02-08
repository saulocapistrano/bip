package br.com.bip.api.rest.user;

import br.com.bip.application.user.dto.UserResponse;
import br.com.bip.application.user.dto.UserUpdateRequest;
import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.model.UserStatus;
import br.com.bip.domain.user.service.UserApprovalService;
import br.com.bip.domain.user.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
public class UserAdminController {

    private final UserApprovalService userApprovalService;
    private final UserManagementService userManagementService;

    public UserAdminController(UserApprovalService userApprovalService, UserManagementService userManagementService) {
        this.userApprovalService = userApprovalService;
        this.userManagementService = userManagementService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable UUID id) {
        UserResponse response = userManagementService.getById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> list(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status
    ) {
        List<UserResponse> users = userManagementService.list(role, status);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid UserUpdateRequest request
    ) {
        UserResponse response = userManagementService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userManagementService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pending")
    public ResponseEntity<List<UserResponse>> listPending(@RequestParam UserRole role){
        List<UserResponse> users = userApprovalService.listPendingByRole(role);
        return ResponseEntity.ok(users);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<UserResponse> approve(@PathVariable UUID id){
        UserResponse response = userApprovalService.approve(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<UserResponse> reject(@PathVariable UUID id){
        UserResponse response = userApprovalService.reject(id);
        return ResponseEntity.ok(response);
    }

}
