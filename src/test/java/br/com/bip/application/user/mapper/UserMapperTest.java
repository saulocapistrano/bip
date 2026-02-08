package br.com.bip.application.user.mapper;

import br.com.bip.application.user.dto.UserRegistrationRequest;
import br.com.bip.domain.user.model.User;
import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.model.UserStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserMapperTest {
    @Test
    void toNewUser_deveMapearCorretamenteOsCamposDoRequest(){
        UserRegistrationRequest request = new UserRegistrationRequest(
                "Cliente Teste",
                "cliente@teste.com",
                "85999990000",
                UserRole.BIP_CLIENTE,
                null
        );

        User user = UserMapper.toNewUser(request);

        assertThat(user.getName()).isEqualTo("Cliente Teste");
        assertThat(user.getEmail()).isEqualTo("cliente@teste.com");
        assertThat(user.getPhone()).isEqualTo("85999990000");
        assertThat(user.getRole()).isEqualTo(UserRole.BIP_CLIENTE);
        assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING_APPROVAL);
    }
}
