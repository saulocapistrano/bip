package br.com.bip.api.rest.delivery;

import br.com.bip.api.handler.exception.GlobalExceptionHandler;
import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.application.security.AuthenticatedUserResolver;
import br.com.bip.domain.delivery.model.DeliveryStatus;
import br.com.bip.domain.delivery.service.DeliveryInRouteQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DriverInRouteController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class DriverInRouteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeliveryInRouteQueryService deliveryInRouteQueryService;

    @MockitoBean
    private AuthenticatedUserResolver authenticatedUserResolver;

    @Test
    void listInRoute_deveRetornar200() throws Exception {
        UUID driverId = UUID.randomUUID();

        DeliveryResponse response = new DeliveryResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                driverId,
                "Rua A",
                "Rua B",
                "desc",
                BigDecimal.ONE,
                BigDecimal.TEN,
                DeliveryStatus.IN_ROUTE,
                null,
                null,
                OffsetDateTime.now()
        );

        when(deliveryInRouteQueryService.listByDriver(driverId)).thenReturn(List.of(response));

        mockMvc.perform(get("/driver/deliveries/in-route")
                        .param("driverId", driverId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("IN_ROUTE"));
    }
}
