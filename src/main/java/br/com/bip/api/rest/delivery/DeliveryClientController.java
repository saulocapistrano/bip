package br.com.bip.api.rest.delivery;

import br.com.bip.application.delivery.dto.DeliveryCreateRequest;
import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.application.delivery.service.DeliveryCreationService;
import br.com.bip.domain.delivery.service.ClientDeliveryQueryService;
import br.com.bip.domain.delivery.service.DeliveryCancellationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/client/deliveries")
public class DeliveryClientController {

    private final DeliveryCreationService deliveryCreationService;
    private final ClientDeliveryQueryService clientDeliveryQueryService;

    private final DeliveryCancellationService deliveryCancellationService;

    public DeliveryClientController(DeliveryCreationService deliveryCreationService,
                                    ClientDeliveryQueryService clientDeliveryQueryService,
                                    DeliveryCancellationService deliveryCancellationService) {
        this.deliveryCreationService = deliveryCreationService;
        this.clientDeliveryQueryService = clientDeliveryQueryService;
        this.deliveryCancellationService = deliveryCancellationService;
    }

    @PostMapping
    public ResponseEntity<DeliveryResponse> create(@RequestBody @Valid DeliveryCreateRequest request) {
        DeliveryResponse response = deliveryCreationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DeliveryResponse>> listByClient(@RequestParam UUID clientId) {
        List<DeliveryResponse> deliveries = clientDeliveryQueryService.listDeliveriesOfClient(clientId);
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
}
