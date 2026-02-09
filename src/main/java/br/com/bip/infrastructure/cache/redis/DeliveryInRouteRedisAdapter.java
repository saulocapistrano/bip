package br.com.bip.infrastructure.cache.redis;

import br.com.bip.domain.delivery.model.DeliveryRequest;
import br.com.bip.domain.delivery.repository.DeliveryInRouteCachePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class DeliveryInRouteRedisAdapter implements DeliveryInRouteCachePort {

    private static final String IN_ROUTE_HASH_KEY = "delivery:in_route";
    private static final String DRIVER_DELIVERIES_KEY_PREFIX = "driver:";
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public DeliveryInRouteRedisAdapter(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    private String driverDeliveriesKey(UUID driverId){
        return DRIVER_DELIVERIES_KEY_PREFIX + driverId + ":deliveries";
    }

    @Override
    public void save(DeliveryRequest delivery) {
        if (delivery.getId() == null) {
            throw new IllegalArgumentException("Delivery precisa ter ID para ser salva no Redis.");
        }

        try {
            String json = objectMapper.writeValueAsString(delivery);

            redisTemplate.opsForHash()
                    .put(IN_ROUTE_HASH_KEY, delivery.getId().toString(), json);

            if (delivery.getDriverId() != null) {
                redisTemplate.opsForSet()
                        .add(driverDeliveriesKey(delivery.getDriverId()), delivery.getId().toString());
            }

        } catch (JsonProcessingException exeption) {
            throw new IllegalStateException("Erro ao serializar entrega para Redis", exeption);
        }
    }

    @Override
    public Optional<DeliveryRequest> findById(UUID deliveryId) {
        Object raw = redisTemplate.opsForHash()
                .get(IN_ROUTE_HASH_KEY, deliveryId.toString());

        if (raw == null) {
            return Optional.empty();
        }

        try {
            DeliveryRequest delivery =
                    objectMapper.readValue(raw.toString(), DeliveryRequest.class);
            return Optional.of(delivery);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Erro ao desserializar entrega do Redis", e);
        }
    }

    @Override
    public List<DeliveryRequest> findByDriverId(UUID driverId) {
        String key = driverDeliveriesKey(driverId);
        Set<String> ids = redisTemplate.opsForSet().members(key);

        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<DeliveryRequest> result = new ArrayList<>();

        for (String idStr : ids) {
            UUID id = UUID.fromString(idStr);
            findById(id).ifPresent(result::add);
        }

        return result;
    }

    @Override
    public void deleteById(UUID deliveryId) {
        Object raw = redisTemplate.opsForHash()
                .get(IN_ROUTE_HASH_KEY, deliveryId.toString());

        redisTemplate.opsForHash()
                .delete(IN_ROUTE_HASH_KEY, deliveryId.toString());

        if (raw != null) {
            try {
                DeliveryRequest delivery =
                        objectMapper.readValue(raw.toString(), DeliveryRequest.class);

                if (delivery.getDriverId() != null) {
                    redisTemplate.opsForSet()
                            .remove(driverDeliveriesKey(delivery.getDriverId()),
                                    deliveryId.toString());
                }
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Erro ao desserializar entrega ao remover do Redis", e);
            }
        }
    }
}
