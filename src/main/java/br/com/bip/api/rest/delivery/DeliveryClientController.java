package br.com.bip.api.rest.delivery;

import br.com.bip.application.delivery.dto.DeliveryCreateRequest;
import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.application.delivery.service.DeliveryCreationService;
import br.com.bip.domain.delivery.service.ClientDeliveryQueryService;
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

    public DeliveryClientController(DeliveryCreationService deliveryCreationService,
                                    ClientDeliveryQueryService clientDeliveryQueryService) {
        this.deliveryCreationService = deliveryCreationService;
        this.clientDeliveryQueryService = clientDeliveryQueryService;
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
}
