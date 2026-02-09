package br.com.bip.api.rest.delivery;

import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.domain.delivery.service.DeliveryInRouteQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/driver/deliveries")
public class DriverInRouteController {

    private final DeliveryInRouteQueryService inRouteQueryService;

    public DriverInRouteController(DeliveryInRouteQueryService inRouteQueryService) {
        this.inRouteQueryService = inRouteQueryService;
    }

    @GetMapping("/in-route")
    public ResponseEntity<List<DeliveryResponse>> listInRoute(@RequestParam UUID driverId) {
        List<DeliveryResponse> deliveries = inRouteQueryService.listByDriver(driverId);
        return ResponseEntity.ok(deliveries);
    }
}
