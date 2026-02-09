package br.com.bip.infrastructure.cache.redis;

import br.com.bip.domain.delivery.model.DeliveryRequest;
import br.com.bip.domain.delivery.model.DeliveryStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DeliveryInRouteRedisAdapterTest {

    private StringRedisTemplate redisTemplate;
    private ObjectMapper objectMapper;

    private HashOperations<String, Object, Object> hashOps;
    private SetOperations<String, String> setOps;

    private DeliveryInRouteRedisAdapter adapter;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        objectMapper = mock(ObjectMapper.class);

        hashOps = mock(HashOperations.class);
        setOps = mock(SetOperations.class);

        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);

        adapter = new DeliveryInRouteRedisAdapter(redisTemplate, objectMapper);
    }

    @Test
    void save_quandoSemId_deveLancarIllegalArgument() {
        DeliveryRequest delivery = new DeliveryRequest();

        assertThatThrownBy(() -> adapter.save(delivery))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("precisa ter ID");

        verifyNoInteractions(hashOps, setOps);
    }

    @Test
    void save_quandoComDriver_devePersistirNaHashEAdicionarNoSetDoDriver() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        DeliveryRequest delivery = DeliveryRequest.builder()
                .id(deliveryId)
                .clientId(UUID.randomUUID())
                .driverId(driverId)
                .pickupAddress("A")
                .deliveryAddress("B")
                .description("desc")
                .weightKg(BigDecimal.ONE)
                .offeredPrice(BigDecimal.TEN)
                .status(DeliveryStatus.IN_ROUTE)
                .createdAt(OffsetDateTime.now())
                .build();

        when(objectMapper.writeValueAsString(delivery)).thenReturn("{\"id\":\"" + deliveryId + "\"}");

        adapter.save(delivery);

        verify(hashOps).put(eq("delivery:in_route"), eq(deliveryId.toString()), anyString());
        verify(setOps).add(eq("driver:" + driverId + ":deliveries"), eq(deliveryId.toString()));
    }

    @Test
    void findById_quandoNaoExiste_deveRetornarEmpty() {
        UUID deliveryId = UUID.randomUUID();
        when(hashOps.get("delivery:in_route", deliveryId.toString())).thenReturn(null);

        Optional<DeliveryRequest> result = adapter.findById(deliveryId);

        assertThat(result).isEmpty();
    }

    @Test
    void findById_quandoExiste_deveDesserializar() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        when(hashOps.get("delivery:in_route", deliveryId.toString())).thenReturn("{\"id\":\"" + deliveryId + "\"}");

        DeliveryRequest expected = DeliveryRequest.builder().id(deliveryId).build();
        when(objectMapper.readValue(anyString(), eq(DeliveryRequest.class))).thenReturn(expected);

        Optional<DeliveryRequest> result = adapter.findById(deliveryId);

        assertThat(result).containsSame(expected);
    }

    @Test
    void findByDriverId_quandoSemIds_deveRetornarListaVazia() {
        UUID driverId = UUID.randomUUID();
        when(setOps.members("driver:" + driverId + ":deliveries")).thenReturn(Set.of());

        var result = adapter.findByDriverId(driverId);

        assertThat(result).isEmpty();
    }

    @Test
    void deleteById_deveRemoverDaHashESetQuandoExistir() throws Exception {
        UUID deliveryId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();

        when(hashOps.get("delivery:in_route", deliveryId.toString())).thenReturn("{\"id\":\"" + deliveryId + "\",\"driverId\":\"" + driverId + "\"}");

        DeliveryRequest parsed = DeliveryRequest.builder().id(deliveryId).driverId(driverId).build();
        when(objectMapper.readValue(anyString(), eq(DeliveryRequest.class))).thenReturn(parsed);

        adapter.deleteById(deliveryId);

        verify(hashOps).delete("delivery:in_route", deliveryId.toString());
        verify(setOps).remove("driver:" + driverId + ":deliveries", deliveryId.toString());
    }

    @Test
    void save_quandoFalhaSerializacao_deveLancarIllegalState() throws Exception {
        UUID deliveryId = UUID.randomUUID();

        DeliveryRequest delivery = DeliveryRequest.builder().id(deliveryId).build();
        when(objectMapper.writeValueAsString(delivery)).thenThrow(new JsonProcessingException("boom") {});

        assertThatThrownBy(() -> adapter.save(delivery))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("serializar");
    }
}
