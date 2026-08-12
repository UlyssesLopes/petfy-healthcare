package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.AnimalSightingRequestDTO;
import br.com.petfy.healthcare.domain.dto.AnimalSightingResponseDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.AnimalSighting;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.AnimalSightingRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.AnimalSightingService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * "Vi o gato hoje" (Tela 43).
 *
 * <b>O gesto mais frequente do produto inteiro, e o mais barato de propósito.</b> Um toque, sem
 * formulário: ele acontece todo dia, por seis pessoas, em catorze gatos. Qualquer campo
 * obrigatório aqui reduziria o registro — e sem registro a tela não sabe dizer quem sumiu, que é
 * a única coisa que ela existe para dizer.
 *
 * <b>Escrita, e não custódia.</b> "O que qualquer um pode fazer: marcar que viu o gato, registrar
 * ferida, foto, comportamento." Quem alcança o animal para escrever pode marcar que o viu — e
 * essa é a metade da regra que faz a colônia funcionar. A outra metade, o que exige duas pessoas,
 * mora no {@link GroupApprovalServiceImpl}.
 */
@Service
@RequiredArgsConstructor
public class AnimalSightingServiceImpl implements AnimalSightingService {

    private final AnimalAccessGuard animalAccessGuard;
    private final CurrentPersonProvider currentPersonProvider;
    private final CurrentProfessionalProvider currentProfessionalProvider;
    private final AnimalSightingRepository animalSightingRepository;

    /**
     * <b>Idempotente por pessoa, animal e dia — e isso é o gesto, não higiene de dados.</b>
     *
     * Sandra passa na praça de manhã e à noite, e toca duas vezes no mesmo botão: a segunda
     * devolve o mesmo registro em vez de criar linha nova ou responder erro. Ela já fez o que
     * queria fazer, e um 409 aqui a ensinaria a não tocar.
     *
     * <b>Marta vendo o mesmo gato no mesmo dia É registro novo</b>, e por isso a pessoa entra na
     * chave: duas pessoas confirmando é informação, e não repetição.
     */
    @Override
    @Transactional
    public AnimalSightingResponseDTO registrar(UUID animalId, AnimalSightingRequestDTO dto) {
        Animal animal = animalAccessGuard.requireEscrita(animalId);
        Person eu = currentPersonProvider.require();

        LocalDate quando = dto.getSeenOn() != null ? dto.getSeenOn() : LocalDate.now();
        exigirDataPossivel(quando);

        AnimalSighting avistamento = animalSightingRepository
                .findByAnimalAnimalIdAndRecordedByPersonIdAndSeenOn(animalId, eu.getPersonId(), quando)
                .orElseGet(() -> animalSightingRepository.save(AnimalSighting.builder()
                        .animal(animal)
                        .seenOn(quando)
                        .recordedBy(eu)
                        .organization(organizacaoDeclarada(eu))
                        .recordedAt(LocalDateTime.now())
                        .build()));

        return toResponse(avistamento);
    }

    /**
     * De qual colônia este avistamento fala.
     *
     * Nulo sem reclamar: quem não declarou organização está agindo por si, e ver um gato por si
     * mesmo continua sendo ver o gato. Exigir contexto aqui barraria o vizinho que ainda não
     * entrou no grupo — e ele é justamente quem descobre o gato novo.
     */
    private Organization organizacaoDeclarada(Person eu) {
        return currentProfessionalProvider.organizacaoDeclarada(eu).orElse(null);
    }

    /** Ver um animal amanhã não é possível, e a recusa não cabe num CHECK do Postgres. */
    private void exigirDataPossivel(LocalDate quando) {
        if (quando.isAfter(LocalDate.now())) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.SIGHTING_DATE_IN_FUTURE.getMessage(),
                    ErrorMessageEnum.SIGHTING_DATE_IN_FUTURE.getCode(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    private AnimalSightingResponseDTO toResponse(AnimalSighting avistamento) {
        return AnimalSightingResponseDTO.builder()
                .animalSightingId(avistamento.getAnimalSightingId())
                .animalId(avistamento.getAnimal().getAnimalId())
                .seenOn(avistamento.getSeenOn())
                .recordedByName(avistamento.getRecordedBy().getName())
                .daysSince(ChronoUnit.DAYS.between(avistamento.getSeenOn(), LocalDate.now()))
                .build();
    }

}
