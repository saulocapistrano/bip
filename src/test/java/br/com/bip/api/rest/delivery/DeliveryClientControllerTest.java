package br.com.bip.api.rest.delivery;

import br.com.bip.api.handler.exception.GlobalExceptionHandler;
import br.com.bip.application.delivery.dto.DeliveryCreateRequest;
import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.application.delivery.service.DeliveryCreationService;
import br.com.bip.application.security.AuthenticatedUserResolver;
import br.com.bip.domain.delivery.model.DeliveryStatus;
import br.com.bip.domain.delivery.service.ClientDeliveryQueryService;
import br.com.bip.domain.delivery.service.DeliveryCancellationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DeliveryClientController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class DeliveryClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeliveryCreationService deliveryCreationService;

    @MockBean
    private ClientDeliveryQueryService clientDeliveryQueryService;

    @MockBean
    private DeliveryCancellationService deliveryCancellationService;

    @MockBean
    private AuthenticatedUserResolver authenticatedUserResolver;

    @Test
    void create_deveRetornar201() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        DeliveryResponse response = new DeliveryResponse(
                deliveryId,
                clientId,
                null,
                "Rua A",
                "Rua B",
                "desc",
                BigDecimal.valueOf(1.2),
                BigDecimal.valueOf(50.0),
                DeliveryStatus.AVAILABLE,
                null,
                null,
                OffsetDateTime.now()
        );

        when(deliveryCreationService.create(any(DeliveryCreateRequest.class))).thenReturn(response);

        DeliveryCreateRequest request = new DeliveryCreateRequest(
                clientId,
                "Rua A",
                "Rua B",
                "desc",
                BigDecimal.valueOf(1.2),
                BigDecimal.valueOf(50.0)
        );

        mockMvc.perform(post("/client/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(deliveryId.toString()))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void listByClient_deveRetornar200() throws Exception {
        UUID clientId = UUID.randomUUID();
        DeliveryResponse response = new DeliveryResponse(
                UUID.randomUUID(),
                clientId,
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

        when(clientDeliveryQueryService.listDeliveriesOfClient(clientId)).thenReturn(List.of(response));

        mockMvc.perform(get("/client/deliveries")
                        .param("clientId", clientId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clientId").value(clientId.toString()));
    }
}
