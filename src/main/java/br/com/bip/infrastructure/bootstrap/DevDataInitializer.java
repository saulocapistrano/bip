package br.com.bip.infrastructure.bootstrap;


import br.com.bip.domain.delivery.model.DeliveryRequest;
import br.com.bip.domain.delivery.model.DeliveryStatus;
import br.com.bip.domain.delivery.repository.DeliveryRequestRepositoryPort;
import br.com.bip.domain.user.model.User;
import br.com.bip.domain.user.model.UserRole;
import br.com.bip.domain.user.model.UserStatus;
import br.com.bip.domain.user.repository.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.CommandLineRunner;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);

    private final UserRepositoryPort userRepositoryPort;
    private final DeliveryRequestRepositoryPort deliveryRepository;

    public DevDataInitializer(UserRepositoryPort userRepositoryPort,
                              DeliveryRequestRepositoryPort deliveryRepository) {
        this.userRepositoryPort = userRepositoryPort;
        this.deliveryRepository = deliveryRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepositoryPort.count() > 0) {
            log.info("[DEV-BOOTSTRAP] Já existem usuários no banco, seed será ignorado.");
            return;
        }

        log.info("[DEV-BOOTSTRAP] Iniciando seed de dados de exemplo...");

        User admin = User.builder()
                .name("Tortorelli")
                .email("tortorelli@bip.local")
                .phone("85999990001")
                .role(UserRole.BIP_ADMIN)
                .status(UserStatus.APPROVED)
                .clientBalance(BigDecimal.ZERO)
                .driverBalance(BigDecimal.ZERO)
                .driverScore(BigDecimal.ZERO)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        admin = userRepositoryPort.save(admin);

        User clicia = User.builder()
                .name("Clicia")
                .email("clicia@bip.local")
                .phone("85999990002")
                .role(UserRole.BIP_CLIENTE)
                .status(UserStatus.APPROVED)
                .clientBalance(new BigDecimal("500.00"))
                .driverBalance(BigDecimal.ZERO)
                .driverScore(BigDecimal.ZERO)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        clicia = userRepositoryPort.save(clicia);

        User elias = User.builder()
                .name("Elias")
                .email("elias@bip.local")
                .phone("85999990003")
                .role(UserRole.BIP_CLIENTE)
                .status(UserStatus.APPROVED)
                .clientBalance(new BigDecimal("150.00"))
                .driverBalance(BigDecimal.ZERO)
                .driverScore(BigDecimal.ZERO)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        elias = userRepositoryPort.save(elias);

        User saulo = User.builder()
                .name("Saulo")
                .email("saulo@bip.local")
                .phone("85999990004")
                .role(UserRole.BIP_ENTREGADOR)
                .status(UserStatus.APPROVED)
                .clientBalance(BigDecimal.ZERO)
                .driverBalance(BigDecimal.ZERO)
                .driverScore(new BigDecimal("1000.00"))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        saulo = userRepositoryPort.save(saulo);

        createDelivery(
                clicia.getId(),
                "Rua das Flores, 100 - Fortaleza",
                "Av Beira Mar, 2000 - Fortaleza",
                "Documentos de contrato",
                new BigDecimal("2.5"),
                new BigDecimal("120.00")
        );

        createDelivery(
                clicia.getId(),
                "Rua João Cordeiro, 50 - Fortaleza",
                "Av Dom Luís, 900 - Fortaleza",
                "Pequeno pacote de brindes",
                new BigDecimal("1.2"),
                new BigDecimal("80.00")
        );

        createDelivery(
                clicia.getId(),
                "Rua Ana Bilhar, 300 - Fortaleza",
                "Av Santos Dumont, 1500 - Fortaleza",
                "Caixa de livros",
                new BigDecimal("5.0"),
                new BigDecimal("60.00")
        );

        createDelivery(
                elias.getId(),
                "Rua Bárbara de Alencar, 45 - Fortaleza",
                "Av Carlos Pereira, 300 - Fortaleza",
                "Material de escritório",
                new BigDecimal("3.0"),
                new BigDecimal("30.00")
        );

        createDelivery(
                elias.getId(),
                "Rua Pinto Madeira, 88 - Fortaleza",
                "Av Pontes Vieira, 950 - Fortaleza",
                "Caixa de eletrônicos",
                new BigDecimal("4.5"),
                new BigDecimal("40.00")
        );

        createDelivery(
                elias.getId(),
                "Rua Senador Pompeu, 10 - Fortaleza",
                "Av Aguanambi, 150 - Fortaleza",
                "Pequeno pacote de amostras",
                new BigDecimal("1.0"),
                new BigDecimal("50.00")
        );

        log.info("[DEV-BOOTSTRAP] Seed de dados concluído com sucesso.");
    }

    private void createDelivery(
            java.util.UUID clientId,
            String pickup,
            String deliveryAddress,
            String description,
            BigDecimal weightKg,
            BigDecimal offeredPrice
    ) {
        DeliveryRequest dr = new DeliveryRequest();
        dr.setClientId(clientId);
        dr.setPickupAddress(pickup);
        dr.setDeliveryAddress(deliveryAddress);
        dr.setDescription(description);
        dr.setWeightKg(weightKg);
        dr.setOfferedPrice(offeredPrice);
        dr.setStatus(DeliveryStatus.AVAILABLE);
        dr.setCreatedAt(OffsetDateTime.now());

        deliveryRepository.save(dr);
    }
}
