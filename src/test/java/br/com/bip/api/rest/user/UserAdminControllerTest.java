package br.com.bip.api.rest.user;

import br.com.bip.api.handler.exception.GlobalExceptionHandler;
import br.com.bip.application.user.dto.UserResponse;
import br.com.bip.application.user.dto.UserUpdateRequest;
import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.model.UserStatus;
import br.com.bip.domain.user.service.UserApprovalService;
import br.com.bip.domain.user.service.UserManagementService;
import br.com.bip.shared.exception.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserAdminController.class)
@Import(GlobalExceptionHandler.class)
class UserAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserApprovalService userApprovalService;

    @MockitoBean
    private UserManagementService userManagementService;

    @Test
    void getById_deveRetornar200() throws Exception {
        UUID id = UUID.randomUUID();
        UserResponse response = new UserResponse(
                id,
                "Nome",
                "user@teste.com",
                "85999990000",
                UserRole.BIP_CLIENTE,
                UserStatus.APPROVED,
                OffsetDateTime.now()
        );

        when(userManagementService.getById(id)).thenReturn(response);

        mockMvc.perform(get("/admin/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("user@teste.com"));
    }

    @Test
    void getById_quandoNaoEncontrado_deveRetornar404ComApiError() throws Exception {
        UUID id = UUID.randomUUID();
        when(userManagementService.getById(id)).thenThrow(new NotFoundException("Usuário não encontrado: " + id));

        mockMvc.perform(get("/admin/users/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/admin/users/" + id));
    }

    @Test
    void listPending_deveRetornar200() throws Exception {
        UserResponse u1 = new UserResponse(UUID.randomUUID(), "A", "a@t.com", null, UserRole.BIP_ENTREGADOR, UserStatus.PENDING_APPROVAL, OffsetDateTime.now());
        when(userApprovalService.listPendingByRole(UserRole.BIP_ENTREGADOR)).thenReturn(List.of(u1));

        mockMvc.perform(get("/admin/users/pending")
                        .param("role", "BIP_ENTREGADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("a@t.com"));
    }

    @Test
    void update_deveRetornar200() throws Exception {
        UUID id = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest("Novo Nome", "85999990000");
        UserResponse response = new UserResponse(id, "Novo Nome", "user@teste.com", "85999990000", UserRole.BIP_CLIENTE, UserStatus.APPROVED, OffsetDateTime.now());

        when(userManagementService.update(any(UUID.class), any(UserUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/admin/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Novo Nome"));
    }

    @Test
    void reject_deveRetornar200() throws Exception {
        UUID id = UUID.randomUUID();
        UserResponse response = new UserResponse(id, "Nome", "user@teste.com", null, UserRole.BIP_ENTREGADOR, UserStatus.REJECTED, OffsetDateTime.now());
        when(userApprovalService.reject(id)).thenReturn(response);

        mockMvc.perform(post("/admin/users/{id}/reject", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }
}
