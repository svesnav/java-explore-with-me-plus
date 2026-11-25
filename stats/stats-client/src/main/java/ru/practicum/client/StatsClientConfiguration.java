package ru.practicum.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration(proxyBeanMethods = false)
@PropertySource("classpath:stats-client.properties")
public class StatsClientConfiguration {

    @Bean
    StatsClient statsClient(@Value("${stats.server.url}") String serverUrl) {
        return new StatsClient(serverUrl);
    }
}
