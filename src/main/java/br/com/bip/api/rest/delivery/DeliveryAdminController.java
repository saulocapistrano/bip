package br.com.bip.api.rest.delivery;

import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.domain.delivery.model.DeliveryStatus;
import br.com.bip.domain.delivery.service.AdminDeliveryQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/deliveries")
public class DeliveryAdminController {

    private final AdminDeliveryQueryService adminDeliveryQueryService;

    public DeliveryAdminController(AdminDeliveryQueryService adminDeliveryQueryService) {
        this.adminDeliveryQueryService = adminDeliveryQueryService;
    }

    @GetMapping
    public ResponseEntity<List<DeliveryResponse>> listAll(@RequestParam(required = false) DeliveryStatus status) {
        List<DeliveryResponse> deliveries = adminDeliveryQueryService.listAll(status);
        return ResponseEntity.ok(deliveries);
    }
}
