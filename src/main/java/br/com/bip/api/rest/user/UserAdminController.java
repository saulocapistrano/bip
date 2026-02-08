package br.com.bip.api.rest.user;

import br.com.bip.application.user.dto.UserResponse;
import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.service.UserApprovalService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/users")
public class UserAdminController {

    private final UserApprovalService userApprovalService;

    public UserAdminController(UserApprovalService userApprovalService) {
        this.userApprovalService = userApprovalService;
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
        UserResponse response = userApprovalService.approve(id);
        return ResponseEntity.ok(response);
    }
}
