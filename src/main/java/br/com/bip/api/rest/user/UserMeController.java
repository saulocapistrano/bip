package br.com.bip.api.rest.user;

import br.com.bip.application.security.AuthenticatedUserResolver;
import br.com.bip.application.user.dto.UserMeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserMeController {

    private final AuthenticatedUserResolver authenticatedUserResolver;

    public UserMeController(AuthenticatedUserResolver authenticatedUserResolver) {
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> me(@AuthenticationPrincipal Jwt jwt) {
        var user = authenticatedUserResolver.resolveCurrentUser(jwt);
        var response = new UserMeResponse(user.getId(), user.getRole(), user.getStatus(), user.getKeycloakId());
        return ResponseEntity.ok(response);
    }
}
