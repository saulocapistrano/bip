package br.com.bip.api.rest.delivery;

import br.com.bip.domain.delivery.model.DeliveryRequest;
import br.com.bip.domain.delivery.model.DeliveryStatus;
import br.com.bip.domain.delivery.repository.DeliveryRequestRepositoryPort;
import br.com.bip.domain.user.model.User;
import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.repository.UserRepositoryPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DeliveryFlowIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepositoryPort userRepositoryPort;

    @Autowired
    private DeliveryRequestRepositoryPort deliveryRepository;

    @Test
    void fluxoCompleto_deveConcluirEntregaComSucesso() throws Exception {
        String clienteJson = """
                {
                  "name": "Cliente IT",
                  "email": "cliente.it@teste.com",
                  "phone": "85999990001",
                  "role": "BIP_CLIENTE"
                }
                """;

        String clienteResponse = mockMvc.perform(post("/api/users/registration/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clienteJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        record UserResponse(UUID id, String name, String email, String phone, UserRole role) {}
        UserResponse cliente = objectMapper.readValue(clienteResponse, UserResponse.class);

        String driverJson = """
                {
                  "name": "Entregador IT",
                  "email": "entregador.it@teste.com",
                  "phone": "85999990002",
                  "role": "BIP_ENTREGADOR"
                }
                """;

        String driverResponse = mockMvc.perform(post("/api/users/registration/driver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(driverJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserResponse driver = objectMapper.readValue(driverResponse, UserResponse.class);

        mockMvc.perform(post("/api/admin/users/{id}/approve", driver.id()))
                .andExpect(status().isOk());

        String depositoJson = """
                {
                  "clientId": "%s",
                  "amount": 200.00
                }
                """.formatted(cliente.id());

        mockMvc.perform(post("/api/client/wallet/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(depositoJson))
                .andExpect(status().is2xxSuccessful());

        String deliveryJson = """
                {
                  "clientId": "%s",
                  "pickupAddress": "Rua Teste, 123",
                  "deliveryAddress": "Av Teste, 456",
                  "description": "Entrega IT",
                  "weightKg": 2.5,
                  "offeredPrice": 80.00
                }
                """.formatted(cliente.id());

        String deliveryResponse = mockMvc.perform(post("/api/client/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deliveryJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        record DeliveryResponse(UUID id, UUID clientId, UUID driverId, DeliveryStatus status) {}
        DeliveryResponse delivery = objectMapper.readValue(deliveryResponse, DeliveryResponse.class);

        mockMvc.perform(post("/api/driver/deliveries/{id}/accept", delivery.id())
                        .param("driverId", driver.id().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_ROUTE"));

        mockMvc.perform(post("/api/driver/deliveries/{id}/complete", delivery.id())
                        .param("driverId", driver.id().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        DeliveryRequest savedDelivery = deliveryRepository.findById(delivery.id()).orElseThrow();
        assertThat(savedDelivery.getStatus()).isEqualTo(DeliveryStatus.COMPLETED);

        User clienteReloaded = userRepositoryPort.findById(cliente.id()).orElseThrow();
        User driverReloaded = userRepositoryPort.findById(driver.id()).orElseThrow();

        // cliente teve 80 debitado, drver recebeu 80
        assertThat(clienteReloaded.getClientBalance()).isEqualByComparingTo(BigDecimal.valueOf(120.00));
        assertThat(driverReloaded.getDriverBalance()).isEqualByComparingTo(BigDecimal.valueOf(80.00));
    }
}
