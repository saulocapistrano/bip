package br.com.bip.api.rest.user;

import br.com.bip.application.user.dto.WalletDepositRequest;
import br.com.bip.application.user.service.ClientWalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/client/wallet")
public class ClientWalletController {

    private final ClientWalletService clientWalletService;

    public ClientWalletController(ClientWalletService clientWalletService) {
        this.clientWalletService = clientWalletService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<Void> deposit(@RequestBody @Valid WalletDepositRequest request) {
        clientWalletService.deposit(request);
        return ResponseEntity.noContent().build();
    }
}
