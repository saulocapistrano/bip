package br.com.bip.api.rest.user;

import br.com.bip.api.handler.exception.GlobalExceptionHandler;
import br.com.bip.application.user.dto.WalletDepositRequest;
import br.com.bip.application.user.service.ClientWalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ClientWalletController.class)
@Import(GlobalExceptionHandler.class)
class ClientWalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClientWalletService clientWalletService;

    @Test
    void deposit_deveRetornar204() throws Exception {
        doNothing().when(clientWalletService).deposit(any(WalletDepositRequest.class));

        WalletDepositRequest request = new WalletDepositRequest(UUID.randomUUID(), BigDecimal.valueOf(10.00));

        mockMvc.perform(post("/client/wallet/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(clientWalletService).deposit(any(WalletDepositRequest.class));
    }

    @Test
    void deposit_quandoPayloadInvalido_deveRetornar400ComApiError() throws Exception {
        mockMvc.perform(post("/client/wallet/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/client/wallet/deposit"));
    }
}
