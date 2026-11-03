package com.teraha.sales.configs;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import com.teraha.commons.dtos.ClientDTO;
import com.teraha.commons.dtos.SaleInvoiceDTO;

import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper,
            @Value("${app.cache.ttl.default-seconds}") long defaultTtl,
            @Value("${app.cache.ttl.clients-seconds}") long clientsTtl,
            @Value("${app.cache.ttl.sale-invoices-seconds}") long invoicesTtl) {

        RedisCacheConfiguration defaults =
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofSeconds(defaultTtl))
                        .disableCachingNullValues();

        RedisCacheConfiguration clientsCache =
                defaults
                        .entryTtl(Duration.ofSeconds(clientsTtl))
                        .serializeValuesWith(
                                SerializationPair.fromSerializer(
                                        new JacksonJsonRedisSerializer<>(
                                                objectMapper, ClientDTO.class)));

        RedisCacheConfiguration invoicesCache =
                defaults
                        .entryTtl(Duration.ofSeconds(invoicesTtl))
                        .serializeValuesWith(
                                SerializationPair.fromSerializer(
                                        new JacksonJsonRedisSerializer<>(
                                                objectMapper, SaleInvoiceDTO.class)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(Map.of(
                        "sales:clients", clientsCache,
                        "sales:invoices", invoicesCache))
                .transactionAware()
                .build();
    }
}
