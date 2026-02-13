package br.com.bip.api.rest.delivery;

import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.application.security.AuthenticatedUserResolver;
import br.com.bip.domain.delivery.service.DeliveryInRouteQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/driver/deliveries")
public class DriverInRouteController {

    private final DeliveryInRouteQueryService inRouteQueryService;

    private final AuthenticatedUserResolver authenticatedUserResolver;

    public DriverInRouteController(DeliveryInRouteQueryService inRouteQueryService,
                                   AuthenticatedUserResolver authenticatedUserResolver) {
        this.inRouteQueryService = inRouteQueryService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping("/in-route")
    public ResponseEntity<List<DeliveryResponse>> listInRoute(@RequestParam UUID driverId) {
        List<DeliveryResponse> deliveries = inRouteQueryService.listByDriver(driverId);
        return ResponseEntity.ok(deliveries);
    }

    @GetMapping("/in-route/mine")
    public ResponseEntity<List<DeliveryResponse>> listInRouteMine(@AuthenticationPrincipal Jwt jwt) {
        var user = authenticatedUserResolver.resolveCurrentUser(jwt);
        List<DeliveryResponse> deliveries = inRouteQueryService.listByDriver(user.getId());
        return ResponseEntity.ok(deliveries);
    }
}
