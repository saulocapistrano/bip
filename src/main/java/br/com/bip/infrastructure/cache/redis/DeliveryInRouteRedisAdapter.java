package br.com.bip.infrastructure.cache.redis;

import br.com.bip.domain.delivery.model.DeliveryRequest;
import br.com.bip.domain.delivery.repository.DeliveryInRouteCachePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class DeliveryInRouteRedisAdapter implements DeliveryInRouteCachePort {

    private static final Logger log = LoggerFactory.getLogger(DeliveryInRouteRedisAdapter.class);

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

            log.info("[REDIS] save in-route. hashKey={}, deliveryId={}, driverId={}",
                    IN_ROUTE_HASH_KEY,
                    delivery.getId(),
                    delivery.getDriverId());

            if (delivery.getDriverId() != null) {
                redisTemplate.opsForSet()
                        .add(driverDeliveriesKey(delivery.getDriverId()), delivery.getId().toString());

                log.info("[REDIS] link delivery to driver set. setKey={}, deliveryId={}",
                        driverDeliveriesKey(delivery.getDriverId()),
                        delivery.getId());
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
            log.debug("[REDIS] miss in-route. hashKey={}, deliveryId={}", IN_ROUTE_HASH_KEY, deliveryId);
            return Optional.empty();
        }

        try {
            DeliveryRequest delivery =
                    objectMapper.readValue(raw.toString(), DeliveryRequest.class);

            log.debug("[REDIS] hit in-route. hashKey={}, deliveryId={}, driverId={}",
                    IN_ROUTE_HASH_KEY,
                    delivery.getId(),
                    delivery.getDriverId());

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
            log.debug("[REDIS] empty driver deliveries set. setKey={}, driverId={}", key, driverId);
            return List.of();
        }

        log.debug("[REDIS] driver deliveries set found. setKey={}, driverId={}, count={}", key, driverId, ids.size());

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

        log.info("[REDIS] delete in-route. hashKey={}, deliveryId={}", IN_ROUTE_HASH_KEY, deliveryId);

        if (raw != null) {
            try {
                DeliveryRequest delivery =
                        objectMapper.readValue(raw.toString(), DeliveryRequest.class);

                if (delivery.getDriverId() != null) {
                    redisTemplate.opsForSet()
                            .remove(driverDeliveriesKey(delivery.getDriverId()),
                                    deliveryId.toString());

                    log.info("[REDIS] unlink delivery from driver set. setKey={}, deliveryId={}",
                            driverDeliveriesKey(delivery.getDriverId()),
                            deliveryId);
                }
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Erro ao desserializar entrega ao remover do Redis", e);
            }
        }
    }
}
