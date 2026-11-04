package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Attachment;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.CustodyNature;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.AttachmentRepository;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.service.AnimalService;
import br.com.petfy.healthcare.service.AttachmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A V53 contra Postgres de verdade: a foto aparece na ficha, e apagar o anexo a tira. */
@SpringBootTest
@DisplayName("a foto do animal, contra Postgres real")
class FotoDoAnimalContainerTest extends PostgresContainerTest {

    @Autowired private AttachmentService attachmentService;
    @Autowired private AnimalService animalService;
    @Autowired private PersonRepository personRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private CustodyRepository custodyRepository;
    @Autowired private AttachmentRepository attachmentRepository;

    private Animal pipoca;
    private Person tutora;

    @BeforeEach
    void montar() {
        tutora = personRepository.saveAndFlush(Person.builder()
                .name("Ana Ribeiro")
                .email("foto-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .phone("11999998888")
                .build());

        pipoca = animalRepository.saveAndFlush(Animal.builder()
                .name("Pipoca").species(Species.CANINA).creationDate(LocalDateTime.now()).build());

        custodyRepository.saveAndFlush(Custody.builder()
                .animal(pipoca).holderPerson(tutora).nature(CustodyNature.DEFINITIVA)
                .startedAt(LocalDateTime.now()).build());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(tutora.getEmail(), "n/a", List.of()));
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private Attachment anexo(String contentType) {
        return attachmentRepository.saveAndFlush(Attachment.builder()
                .animal(pipoca)
                .recordedBy(tutora)
                .originalFilename("pipoca")
                .contentType(contentType)
                .sizeBytes(26L)
                .checksumSha256("a".repeat(64))
                .storageKey("animals/" + pipoca.getAnimalId() + "/" + UUID.randomUUID())
                .creationDate(LocalDateTime.now())
                .build());
    }

    @Test
    @DisplayName("a imagem marcada aparece na ficha do animal")
    void aFotoApareceNaFicha() {
        Attachment foto = anexo("image/jpeg");

        attachmentService.setPhoto(pipoca.getAnimalId(), foto.getAttachmentId());

        assertThat(animalService.getAnimalById(pipoca.getAnimalId()).getPhotoAttachmentId())
                .isEqualTo(foto.getAttachmentId());
    }

    @Test
    @DisplayName("apagar o anexo tira a foto, e a ficha continua de pe")
    void apagarOAnexoTiraAFoto() {
        Attachment foto = anexo("image/png");
        attachmentService.setPhoto(pipoca.getAnimalId(), foto.getAttachmentId());

        attachmentRepository.deleteById(foto.getAttachmentId());
        attachmentRepository.flush();

        assertThat(animalRepository.findById(pipoca.getAnimalId()).orElseThrow().getPhotoAttachmentId())
                .isNull();
        assertThat(animalService.getAnimalById(pipoca.getAnimalId()).getName()).isEqualTo("Pipoca");
    }

    @Test
    @DisplayName("um PDF nao vira foto")
    void pdfNaoViraFoto() {
        Attachment laudo = anexo("application/pdf");

        assertThatThrownBy(() -> attachmentService.setPhoto(pipoca.getAnimalId(), laudo.getAttachmentId()))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting("httpStatus")
                .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);

        assertThat(animalRepository.findById(pipoca.getAnimalId()).orElseThrow().getPhotoAttachmentId())
                .isNull();
    }
}
