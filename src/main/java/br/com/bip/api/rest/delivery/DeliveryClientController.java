package br.com.bip.api.rest.delivery;

import br.com.bip.application.delivery.dto.DeliveryCreateMineRequest;
import br.com.bip.application.delivery.dto.DeliveryCreateRequest;
import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.application.security.AuthenticatedUserResolver;
import br.com.bip.application.delivery.service.DeliveryCreationService;
import br.com.bip.domain.delivery.service.ClientDeliveryQueryService;
import br.com.bip.domain.delivery.service.DeliveryCancellationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/client/deliveries")
public class DeliveryClientController {

    private final DeliveryCreationService deliveryCreationService;
    private final ClientDeliveryQueryService clientDeliveryQueryService;

    private final DeliveryCancellationService deliveryCancellationService;

    private final AuthenticatedUserResolver authenticatedUserResolver;

    public DeliveryClientController(DeliveryCreationService deliveryCreationService,
                                    ClientDeliveryQueryService clientDeliveryQueryService,
                                    DeliveryCancellationService deliveryCancellationService,
                                    AuthenticatedUserResolver authenticatedUserResolver) {
        this.deliveryCreationService = deliveryCreationService;
        this.clientDeliveryQueryService = clientDeliveryQueryService;
        this.deliveryCancellationService = deliveryCancellationService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping
    public ResponseEntity<DeliveryResponse> create(@RequestBody @Valid DeliveryCreateRequest request) {
        DeliveryResponse response = deliveryCreationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/mine")
    public ResponseEntity<DeliveryResponse> createMine(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid DeliveryCreateMineRequest request
    ) {
        var user = authenticatedUserResolver.resolveCurrentUser(jwt);
        var fullRequest = new DeliveryCreateRequest(
                user.getId(),
                request.pickupAddress(),
                request.deliveryAddress(),
                request.description(),
                request.weightKg(),
                request.offeredPrice()
        );
        DeliveryResponse response = deliveryCreationService.create(fullRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DeliveryResponse>> listByClient(@RequestParam UUID clientId) {
        List<DeliveryResponse> deliveries = clientDeliveryQueryService.listDeliveriesOfClient(clientId);
        return ResponseEntity.ok(deliveries);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<DeliveryResponse>> listMine(@AuthenticationPrincipal Jwt jwt) {
        var user = authenticatedUserResolver.resolveCurrentUser(jwt);
        List<DeliveryResponse> deliveries = clientDeliveryQueryService.listDeliveriesOfClient(user.getId());
        return ResponseEntity.ok(deliveries);
    }
    @PostMapping("/{id}/cancel")
    public ResponseEntity<DeliveryResponse> cancel(
            @PathVariable("id") UUID deliveryId,
            @RequestParam("clientId") UUID clientId,
            @RequestParam(value = "reason", required = false) String reason
    ) {
        DeliveryResponse response = deliveryCancellationService.cancelByClient(deliveryId, clientId, reason);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel/mine")
    public ResponseEntity<DeliveryResponse> cancelMine(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID deliveryId,
            @RequestParam(value = "reason", required = false) String reason
    ) {
        var user = authenticatedUserResolver.resolveCurrentUser(jwt);
        DeliveryResponse response = deliveryCancellationService.cancelByClient(deliveryId, user.getId(), reason);
        return ResponseEntity.ok(response);
    }
}
