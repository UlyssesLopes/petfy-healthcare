package br.com.petfy.healthcare;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base dos testes que precisam de Postgres de verdade.
 *
 * Existe porque o H2 nao consegue validar duas coisas centrais deste projeto:
 *
 * 1. As migrations. Elas sao escritas para Postgres, entao no H2 quem cria as
 *    tabelas e o Hibernate - o que valida o mapeamento contra si mesmo, e nao
 *    contra o schema que vai para producao.
 *
 * 2. As consultas por UUID. O Hibernate 5.6 gera BINARY(255) para UUID no H2, e
 *    o H2 2.x trata BINARY como tamanho fixo: o valor gravado e preenchido ate
 *    255 bytes e nunca casa com um parametro de 16 bytes, entao toda consulta
 *    por UUID volta vazia.
 *
 * disabledWithoutDocker faz estes testes serem PULADOS onde nao ha Docker, em
 * vez de falharem. Assim quem roda a suite na maquina sem Docker nao fica
 * bloqueado, e o pipeline - onde ha Docker - executa de verdade.
 *
 * O container e estatico: sobe uma vez e e reaproveitado por todas as classes
 * que estendem esta, em vez de um Postgres por classe.
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class PostgresContainerTest {

    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:14")
                    .withDatabaseName("petfy")
                    .withUsername("petfy")
                    .withPassword("petfy");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");

        // exatamente como em producao: Flyway cria o schema, Hibernate confere
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        registry.add("petfy.jwt.secret", () -> "segredo-de-teste-com-mais-de-32-caracteres");
    }

}
