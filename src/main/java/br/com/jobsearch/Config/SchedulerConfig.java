package br.com.jobsearch.Config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * scheduler.enabled=false desliga o @Scheduled da aplicacao inteira (ninguem
 * mais habilita @EnableScheduling). Usado nos testes: sem isto, o scheduler
 * dispara assim que o contexto sobe e chama a API real da Adzuna.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerConfig {
}
