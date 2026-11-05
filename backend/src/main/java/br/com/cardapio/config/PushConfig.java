package br.com.cardapio.config;

import br.com.cardapio.adapter.out.push.PushProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** As notificações no celular (Guia 29): as chaves VAPID e os serviços de push aceitos. */
@Configuration
@EnableConfigurationProperties(PushProperties.class)
class PushConfig {
}
