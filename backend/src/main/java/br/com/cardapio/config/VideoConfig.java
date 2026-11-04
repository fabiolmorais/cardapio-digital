package br.com.cardapio.config;

import br.com.cardapio.adapter.out.video.VideoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * O serviço de vídeo (Guia 22): o Bunny Stream de verdade ou o falso, conforme
 * app.video.provider. Cada adaptador se liga sozinho pela propriedade (@ConditionalOnProperty).
 */
@Configuration
@EnableConfigurationProperties(VideoProperties.class)
class VideoConfig {
}
