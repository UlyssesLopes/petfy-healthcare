package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.HealthEventCategory;
import br.com.petfy.healthcare.domain.entity.HealthRecord;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Species;
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
 * findByAnimalAnimalIdOrderByEventDateDesc so e traduzida quando o Spring Data cria o
 * repositorio, ou seja, na subida do contexto. Um nome de propriedade errado
 * derrubaria a aplicacao no boot, e nao no teste.
 *
 * Roda em H2 e por isso nao afirma quais linhas voltam: o Hibernate 5.6 gera
 * BINARY(255) para UUID nele, e o H2 2.x trata BINARY como tamanho fixo, entao
 * consulta por UUID volta sempre vazia. Quem verifica o filtro e a ordenacao de
 * verdade e o UuidQueriesContainerTest, contra Postgres.
 *
 * Vale manter os dois: este e rapido e roda em qualquer maquina; o de container
 * precisa de Docker e so executa onde ha.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        // a migration do Flyway e escrita para Postgres; neste teste quem cria
        // as tabelas no H2 e o proprio Hibernate
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class HealthRecordRepositoryTest {

    @Autowired
    private HealthRecordRepository healthRecordRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @Test
    @DisplayName("a query derivada por animal deve ser traduzida pelo Spring Data e executar sem erro")
    void queryDerivadaDeveSerTraduzidaEExecutar() {
        Owner owner = ownerRepository.save(Owner.builder()
                .name("Ulysses")
                .email("ulysses@petfy.com.br")
                .password("hash")
                .build());

        Animal rex = animalRepository.save(Animal.builder().name("Rex").tutors(br.com.petfy.healthcare.PetTutores.titular(owner)).species(Species.CANINA).build());

        healthRecordRepository.save(HealthRecord.builder()
                .category(HealthEventCategory.CONSULTA)
                    .animal(rex)
                .eventType("Consulta")
                .eventDate(LocalDate.of(2025, 1, 10))
                .build());

        assertThat(healthRecordRepository.findByAnimalAnimalIdOrderByEventDateDesc(rex.getAnimalId())).isNotNull();
        assertThat(healthRecordRepository.findByAnimalAnimalIdOrderByEventDateDesc(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("o mapeamento das entidades deve gerar um schema valido")
    void mapeamentoDeveGerarSchemaValido() {
        Owner owner = ownerRepository.save(Owner.builder()
                .name("Ulysses")
                .email("outro@petfy.com.br")
                .password("hash")
                .build());

        Animal animal = animalRepository.save(Animal.builder().name("Mia").tutors(br.com.petfy.healthcare.PetTutores.titular(owner)).species(Species.CANINA).build());

        HealthRecord salvo = healthRecordRepository.save(HealthRecord.builder()
                .category(HealthEventCategory.CONSULTA)
                    .animal(animal)
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
                    assertThat(r.getAnimal().getAnimalId()).isEqualTo(animal.getAnimalId());
                });
    }
}
