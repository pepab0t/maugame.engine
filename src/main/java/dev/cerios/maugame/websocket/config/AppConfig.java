package dev.cerios.maugame.websocket.config;

import dev.cerios.maugame.mauengine.game.GameFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public GameFactory gameFactory() {
        return new GameFactory();
    }
}
