package br.com.cardapio.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Liga as tarefas agendadas (@Scheduled), como a limpeza das sessões de mesa (Guia 17). */
@Configuration
@EnableScheduling
class SchedulingConfig {
}
