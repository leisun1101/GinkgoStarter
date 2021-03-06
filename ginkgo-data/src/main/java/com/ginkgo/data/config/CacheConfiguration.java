package com.ginkgo.data.config;

import com.ginkgo.data.exception.DataException;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@ConditionalOnProperty(prefix = "ginkgo.data.cache", name = "type")
@EnableConfigurationProperties(DataProperties.class)
@Configuration
@EnableCaching
public class CacheConfiguration {

    @Autowired
    private DataProperties dataProperties;

    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;

    @Bean
    public CacheManager cacheManager() throws DataException {
        if (this.dataProperties.getCache().getType().equals(CacheType.REDIS)) {

            if (Objects.isNull(this.dataProperties.getRedis().getHostName())) {
                throw new DataException("Redis Hostname is not set for the Redis Cache");
            }
            RedisCacheManager redisCacheManager = RedisCacheManager.builder(this.redisConnectionFactory)
                    .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                            .entryTtl(Duration.ofSeconds(this.dataProperties.getCache().getSeconds()))).build();
            return redisCacheManager;
        } else {
            CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
            Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                    .expireAfterWrite(this.dataProperties.getCache().getSeconds(), TimeUnit.SECONDS)
                    .initialCapacity(100)
                    .maximumSize(150);
            caffeineCacheManager.setCaffeine(caffeine);
            caffeineCacheManager.setAllowNullValues(false);
            return caffeineCacheManager;
        }
    }
}
