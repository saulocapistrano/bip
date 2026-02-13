package br.com.bip.api.rest.delivery;

import br.com.bip.api.handler.exception.GlobalExceptionHandler;
import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.application.security.AuthenticatedUserResolver;
import br.com.bip.domain.delivery.model.DeliveryStatus;
import br.com.bip.domain.delivery.service.DeliveryAssignmentService;
import br.com.bip.domain.delivery.service.DeliveryCompletionService;
import br.com.bip.domain.delivery.service.DriverDeliveryQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DeliveryDriverController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class DeliveryDriverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DriverDeliveryQueryService driverDeliveryQueryService;

    @MockBean
    private DeliveryAssignmentService deliveryAssignmentService;

    @MockBean
    private DeliveryCompletionService deliveryCompletionService;

    @MockBean
    private AuthenticatedUserResolver authenticatedUserResolver;

    @Test
    void listAvailable_deveRetornar200() throws Exception {
        DeliveryResponse response = new DeliveryResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                "Rua A",
                "Rua B",
                "desc",
                BigDecimal.valueOf(1.0),
                BigDecimal.valueOf(10.0),
                DeliveryStatus.AVAILABLE,
                null,
                null,
                OffsetDateTime.now()
        );

        when(driverDeliveryQueryService.listAvailableForDriver()).thenReturn(List.of(response));

        mockMvc.perform(get("/driver/deliveries/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    void accept_deveRetornar200() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        DeliveryResponse response = new DeliveryResponse(
                deliveryId,
                UUID.randomUUID(),
                driverId,
                "Rua A",
                "Rua B",
                "desc",
                BigDecimal.valueOf(1.0),
                BigDecimal.valueOf(10.0),
                DeliveryStatus.IN_ROUTE,
                null,
                null,
                OffsetDateTime.now()
        );

        when(deliveryAssignmentService.acceptDelivery(deliveryId, driverId)).thenReturn(response);

        mockMvc.perform(post("/driver/deliveries/{id}/accept", deliveryId)
                        .param("driverId", driverId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_ROUTE"));
    }

    @Test
    void complete_deveRetornar200() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        DeliveryResponse response = new DeliveryResponse(
                deliveryId,
                UUID.randomUUID(),
                driverId,
                "Rua A",
                "Rua B",
                "desc",
                BigDecimal.valueOf(1.0),
                BigDecimal.valueOf(10.0),
                DeliveryStatus.COMPLETED,
                null,
                null,
                OffsetDateTime.now()
        );

        when(deliveryCompletionService.completeDelivery(deliveryId, driverId)).thenReturn(response);

        mockMvc.perform(post("/driver/deliveries/{id}/complete", deliveryId)
                        .param("driverId", driverId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}
