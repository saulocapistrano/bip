package br.com.bip.api.rest.delivery;

import br.com.bip.api.handler.exception.GlobalExceptionHandler;
import br.com.bip.application.delivery.dto.DeliveryResponse;
import br.com.bip.domain.delivery.model.DeliveryStatus;
import br.com.bip.domain.delivery.service.AdminDeliveryQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DeliveryAdminController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class DeliveryAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDeliveryQueryService adminDeliveryQueryService;

    @Test
    void listAll_semFiltro_deveRetornar200() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        DeliveryResponse response = new DeliveryResponse(
                deliveryId,
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

        when(adminDeliveryQueryService.listAll(null)).thenReturn(List.of(response));

        mockMvc.perform(get("/admin/deliveries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(deliveryId.toString()));

        verify(adminDeliveryQueryService).listAll(null);
    }

    @Test
    void listAll_comStatus_deveRetornar200() throws Exception {
        DeliveryResponse response = new DeliveryResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
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

        when(adminDeliveryQueryService.listAll(DeliveryStatus.COMPLETED)).thenReturn(List.of(response));

        mockMvc.perform(get("/admin/deliveries")
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));

        verify(adminDeliveryQueryService).listAll(DeliveryStatus.COMPLETED);
    }
}
