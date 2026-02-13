package br.com.bip.api.rest.delivery;

import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.application.security.AuthenticatedUserResolver;
import br.com.bip.domain.delivery.service.DeliveryAssignmentService;
import br.com.bip.domain.delivery.service.DeliveryCompletionService;
import br.com.bip.domain.delivery.service.DriverDeliveryQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/driver/deliveries")
public class DeliveryDriverController {

    private final DriverDeliveryQueryService driverDeliveryQueryService;
    private final DeliveryAssignmentService deliveryAssignmentService;

    private final DeliveryCompletionService deliveryCompletionService;

    private final AuthenticatedUserResolver authenticatedUserResolver;

    public DeliveryDriverController(DriverDeliveryQueryService driverDeliveryQueryService,
                                    DeliveryAssignmentService deliveryAssignmentService,
                                    DeliveryCompletionService deliveryCompletionService,
                                    AuthenticatedUserResolver authenticatedUserResolver) {
        this.driverDeliveryQueryService = driverDeliveryQueryService;
        this.deliveryAssignmentService = deliveryAssignmentService;
        this.deliveryCompletionService = deliveryCompletionService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping("/available")
    public ResponseEntity<List<DeliveryResponse>> listAvailable() {
        List<DeliveryResponse> deliveries = driverDeliveryQueryService.listAvailableForDriver();
        return ResponseEntity.ok(deliveries);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<DeliveryResponse>> listByDriver(@RequestParam UUID driverId) {
        List<DeliveryResponse> deliveries = driverDeliveryQueryService.listDeliveriesOfDriver(driverId);
        return ResponseEntity.ok(deliveries);
    }

    @GetMapping("/mine/auth")
    public ResponseEntity<List<DeliveryResponse>> listMine(@AuthenticationPrincipal Jwt jwt) {
        var user = authenticatedUserResolver.resolveCurrentUser(jwt);
        List<DeliveryResponse> deliveries = driverDeliveryQueryService.listDeliveriesOfDriver(user.getId());
        return ResponseEntity.ok(deliveries);
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<DeliveryResponse> accept(
            @PathVariable("id") UUID deliveryId,
            @RequestParam("driverId") UUID driverId
    ) {
        DeliveryResponse response = deliveryAssignmentService.acceptDelivery(deliveryId, driverId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/accept/mine")
    public ResponseEntity<DeliveryResponse> acceptMine(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID deliveryId
    ) {
        var user = authenticatedUserResolver.resolveCurrentUser(jwt);
        DeliveryResponse response = deliveryAssignmentService.acceptDelivery(deliveryId, user.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<DeliveryResponse> complete(
            @PathVariable("id") UUID deliveryId,
            @RequestParam("driverId") UUID driverId
    ) {
        DeliveryResponse response = deliveryCompletionService.completeDelivery(deliveryId, driverId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/complete/mine")
    public ResponseEntity<DeliveryResponse> completeMine(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID deliveryId
    ) {
        var user = authenticatedUserResolver.resolveCurrentUser(jwt);
        DeliveryResponse response = deliveryCompletionService.completeDelivery(deliveryId, user.getId());
        return ResponseEntity.ok(response);
    }

}
