package es.bytescolab.msdashboard.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_DASHBOARD_SUMMARY = "dashboard-summary";
    public static final String CACHE_DASHBOARD_TIMESERIES = "dashboard-timeseries";
    public static final String CACHE_DASHBOARD_RANKING = "dashboard-ranking";

    private static final List<String> CACHES_REQUERIDAS = List.of(
            CACHE_DASHBOARD_SUMMARY,
            CACHE_DASHBOARD_TIMESERIES,
            CACHE_DASHBOARD_RANKING
    );

    @Bean
    public CacheManager cacheManager(@Value("${dashboard.cache.ttl-seconds}") long ttlSegundos) {
        CaffeineCacheManager gestor = new CaffeineCacheManager();
        gestor.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(ttlSegundos, TimeUnit.SECONDS)
                .maximumSize(512));
        gestor.setCacheNames(CACHES_REQUERIDAS);
        return gestor;
    }
}
