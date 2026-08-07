package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.ObservationRequestDTO;
import br.com.petfy.healthcare.domain.dto.ObservationResponseDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Observation;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.ObservationRepository;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.ObservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ObservationServiceImpl implements ObservationService {

    private final ObservationRepository observationRepository;
    private final AnimalAccessGuard animalAccessGuard;
    private final CurrentPersonProvider currentPersonProvider;
    private final CurrentProfessionalProvider currentProfessionalProvider;

    /**
     * <b>Exige escrita, e nada alem disso.</b> O 3.11 e literal: "observacao, qualquer um
     * com acesso - tutor, monitor, lar transitorio, voluntario". Nao ha checagem de
     * credencial nem de capacidade, e a ausencia e a regra: exigir qualquer das duas
     * transformaria observacao em ato clinico por via de autorizacao, que e exatamente a
     * distincao que este conceito existe para manter.
     */
    @Override
    @Transactional
    public ObservationResponseDTO create(UUID animalId, ObservationRequestDTO request) {
        Animal animal = animalAccessGuard.requireEscrita(animalId);
        Person person = currentPersonProvider.require();
        LocalDateTime agora = LocalDateTime.now();

        return toResponse(observationRepository.save(Observation.builder()
                .animal(animal)
                .description(request.getDescription())
                // quem registra na hora nao precisa preencher; quem registra as 18h o que
                // viu as 9h preenche, e a linha do tempo o coloca as 9h
                .observedAt(request.getObservedAt() == null ? agora : request.getObservedAt())
                .urgent(request.isUrgent())
                .recordedBy(person)
                .organization(organizacaoAssinante(person))
                .recordedAt(agora)
                .creationDate(agora)
                .build()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObservationResponseDTO> listByAnimal(UUID animalId) {
        animalAccessGuard.requireLeitura(animalId);

        return observationRepository.findByAnimalAnimalIdOrderByObservedAtDesc(animalId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Em nome de quem o registro sai, quando alguem declarou.
     *
     * <b>So o header decide, e nunca se escolhe sozinho.</b> "A creche Pata Legal observou"
     * e "a Maria observou" nao sao o mesmo fato - o primeiro carrega responsabilidade
     * institucional (3.2), e contexto nao e editavel depois (5.7). Sem header o registro
     * sai da pessoa, o que e o certo para o tutor observando em casa.
     *
     * Nao exige credencial: quem mais registra observacao e o monitor da creche, que e
     * membro e nunca vai ter CRMV.
     */
    private Organization organizacaoAssinante(Person person) {
        return currentProfessionalProvider.organizacaoDeclarada(person).orElse(null);
    }

    private ObservationResponseDTO toResponse(Observation observacao) {
        return ObservationResponseDTO.builder()
                .observationId(observacao.getObservationId())
                .animalId(observacao.getAnimal().getAnimalId())
                .description(observacao.getDescription())
                .observedAt(observacao.getObservedAt())
                .recordedAt(observacao.getRecordedAt())
                .urgent(observacao.isUrgent())
                .recordedByName(observacao.getRecordedBy() == null ? null : observacao.getRecordedBy().getName())
                .organizationName(observacao.getOrganization() == null ? null : observacao.getOrganization().getName())
                .build();
    }

}
