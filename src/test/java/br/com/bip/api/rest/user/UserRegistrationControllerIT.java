package br.com.bip.api.rest.user;

import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.model.UserStatus;
import br.com.bip.infrastructure.persistence.jpa.user.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserRegistrationControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Test
    void deveCadastrarClienteComSucesso() throws Exception {
        String body = """
                {
                  "name": "Cliente IT",
                  "email": "cliente.it@teste.com",
                  "phone": "85999990000",
                  "role": "BIP_CLIENTE"
                }
                """;

        mockMvc.perform(post("/api/users/registration/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("cliente.it@teste.com"))
                .andExpect(jsonPath("$.role").value("BIP_CLIENTE"))
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));

        var user = userJpaRepository.findByEmail("cliente.it@teste.com").orElseThrow();
        assertThat(user.getRole()).isEqualTo(UserRole.BIP_CLIENTE);
        assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING_APPROVAL);
    }
}
