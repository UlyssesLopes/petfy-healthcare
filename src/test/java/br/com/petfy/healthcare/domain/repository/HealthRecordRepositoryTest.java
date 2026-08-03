package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.HealthRecord;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre o modo de falha que os testes com mock nao alcancam: a query derivada
 * findByPetPetIdOrderByEventDateDesc so e traduzida quando o Spring Data cria o
 * repositorio, ou seja, na subida do contexto. Um nome de propriedade errado
 * derrubaria a aplicacao no boot, e nao no teste. Se este contexto sobe e a
 * query executa, a derivacao esta correta.
 *
 * O que este teste NAO faz e afirmar quais linhas voltam. O Hibernate 5.6 gera
 * BINARY(255) para UUID no H2, e o H2 2.x trata BINARY como tamanho fixo: o
 * valor gravado e preenchido ate 255 bytes, entao a comparacao com um parametro
 * de 16 bytes nunca casa e qualquer consulta por UUID volta vazia. Comparacao
 * entre colunas funciona (os dois lados tem o mesmo padding), o que confirma que
 * os dados estao la. E artefato do H2 e nao afeta o Postgres, onde o UUID e tipo
 * nativo - mas impede este teste de validar o filtro de verdade.
 *
 * Filtro e ordenacao continuam sem verificacao contra banco real. Para fechar
 * isso, o caminho e um teste de integracao com Postgres (Testcontainers).
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        // o schema.sql do projeto e escrito para Postgres e ja esta defasado em
        // relacao as entidades; aqui quem cria as tabelas e o proprio Hibernate
        "spring.sql.init.mode=never"
})
class HealthRecordRepositoryTest {

    @Autowired
    private HealthRecordRepository healthRecordRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @Test
    @DisplayName("a query derivada por pet deve ser traduzida pelo Spring Data e executar sem erro")
    void queryDerivadaDeveSerTraduzidaEExecutar() {
        Owner owner = ownerRepository.save(Owner.builder()
                .name("Ulysses")
                .email("ulysses@petfy.com.br")
                .password("hash")
                .build());

        Pet rex = petRepository.save(Pet.builder().name("Rex").owner(owner).build());

        healthRecordRepository.save(HealthRecord.builder()
                .pet(rex)
                .eventType("Consulta")
                .eventDate(LocalDate.of(2025, 1, 10))
                .build());

        assertThat(healthRecordRepository.findByPetPetIdOrderByEventDateDesc(rex.getPetId())).isNotNull();
        assertThat(healthRecordRepository.findByPetPetIdOrderByEventDateDesc(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("o mapeamento das entidades deve gerar um schema valido")
    void mapeamentoDeveGerarSchemaValido() {
        Owner owner = ownerRepository.save(Owner.builder()
                .name("Ulysses")
                .email("outro@petfy.com.br")
                .password("hash")
                .build());

        Pet pet = petRepository.save(Pet.builder().name("Mia").owner(owner).build());

        HealthRecord salvo = healthRecordRepository.save(HealthRecord.builder()
                .pet(pet)
                .eventType("Cirurgia")
                .eventDate(LocalDate.of(2025, 8, 20))
                .description("Castracao")
                .build());

        assertThat(salvo.getHealthRecordId()).isNotNull();
        assertThat(healthRecordRepository.findById(salvo.getHealthRecordId()))
                .get()
                .satisfies(r -> {
                    assertThat(r.getEventType()).isEqualTo("Cirurgia");
                    assertThat(r.getDescription()).isEqualTo("Castracao");
                    assertThat(r.getPet().getPetId()).isEqualTo(pet.getPetId());
                });
    }
}
