package br.com.bip.api.rest.user;

import br.com.bip.api.handler.exception.GlobalExceptionHandler;
import br.com.bip.application.user.dto.UserRegistrationRequest;
import br.com.bip.application.user.dto.UserResponse;
import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.model.UserStatus;
import br.com.bip.domain.user.service.UserRegistrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserRegistrationController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class UserRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRegistrationService userRegistrationService;

    @Test
    void registerClient_deveRetornar201EBody() throws Exception {
        UserResponse response = new UserResponse(
                UUID.randomUUID(),
                "Cliente",
                "cliente@teste.com",
                "85999990000",
                UserRole.BIP_CLIENTE,
                UserStatus.PENDING_APPROVAL,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                OffsetDateTime.now()
        );

        when(userRegistrationService.registerClient(any(UserRegistrationRequest.class)))
                .thenReturn(response);

        String body = objectMapper.writeValueAsString(new UserRegistrationRequest(
                "Cliente",
                "cliente@teste.com",
                "85999990000",
                UserRole.BIP_CLIENTE,
                null
        ));

        mockMvc.perform(post("/users/registration/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("cliente@teste.com"))
                .andExpect(jsonPath("$.role").value("BIP_CLIENTE"));

        verify(userRegistrationService).registerClient(any(UserRegistrationRequest.class));
    }

    @Test
    void registerClient_quandoPayloadInvalido_deveRetornar400ComApiError() throws Exception {
        String body = "{}";

        mockMvc.perform(post("/users/registration/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.path").value("/users/registration/client"));
    }
}
