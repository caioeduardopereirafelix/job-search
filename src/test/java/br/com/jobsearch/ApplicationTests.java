package br.com.jobsearch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Sobe o contexto do Spring por completo (todos os beans, Flyway rodando as
 * migrations de verdade contra o banco) e falha se algo nao fechar - foi
 * assim que um bug real (dois @ExceptionHandler ambiguos) derrubou a
 * aplicacao inteira sem que nenhum teste unitario, so com mocks, pegasse.
 */
@SpringBootTest
@TestPropertySource(properties = "scheduler.enabled=false")
class ApplicationTests {

    @Test
    void contextLoads() {
    }

}
