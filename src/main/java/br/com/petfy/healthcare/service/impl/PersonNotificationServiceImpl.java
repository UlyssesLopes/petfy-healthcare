package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.PersonNotificationResponseDTO;
import br.com.petfy.healthcare.domain.entity.PersonNotification;
import br.com.petfy.healthcare.domain.repository.PersonNotificationRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.service.PersonNotificationService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Os avisos de quem esta lendo, e de mais ninguem.
 *
 * <b>Nao existe rota para "os avisos de fulano".</b> Todo metodo aqui parte do autenticado, e o id
 * do aviso so e aceito depois de conferido que ele e dessa pessoa — um aviso alheio responde 404, e
 * nao 403, porque confirmar que o aviso existe ja diria algo sobre a vida de outra pessoa.
 */
@Service
@RequiredArgsConstructor
public class PersonNotificationServiceImpl implements PersonNotificationService {

    private final PersonNotificationRepository personNotificationRepository;
    private final CurrentPersonProvider currentPersonProvider;

    @Override
    @Transactional(readOnly = true)
    public Page<PersonNotificationResponseDTO> listMine(Pageable pageable) {
        UUID eu = currentPersonProvider.require().getPersonId();

        return personNotificationRepository
                .findByPersonPersonIdOrderByCreatedAtDesc(eu, pageable)
                .map(this::toResponse);
    }

    /**
     * Quantos ainda nao foram lidos.
     *
     * <b>Rota propria, e nao um campo do feed:</b> a marca no sino e perguntada com frequencia e de
     * qualquer tela, e carregar uma pagina de avisos para contar seria pagar a leitura inteira para
     * mostrar um numero.
     */
    @Override
    @Transactional(readOnly = true)
    public long countMineUnread() {
        return personNotificationRepository
                .countByPersonPersonIdAndReadAtIsNull(currentPersonProvider.require().getPersonId());
    }

    @Override
    @Transactional
    public PersonNotificationResponseDTO markAsRead(UUID personNotificationId) {
        UUID eu = currentPersonProvider.require().getPersonId();

        PersonNotification aviso = personNotificationRepository.findById(personNotificationId)
                .filter(candidato -> candidato.getPerson().getPersonId().equals(eu))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.NOTIFICATION_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.NOTIFICATION_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));

        /*
         * Ler duas vezes nao reescreve a data. "Quando ela viu" e a PRIMEIRA vez que viu — e uma
         * tela que marca ao rolar chamaria isto a cada passagem, empurrando a data para frente e
         * apagando a informacao que o campo existe para guardar.
         */
        if (aviso.getReadAt() == null) {
            aviso.setReadAt(LocalDateTime.now());
            personNotificationRepository.save(aviso);
        }

        return toResponse(aviso);
    }

    @Override
    @Transactional
    public int markAllAsRead() {
        return personNotificationRepository.marcarTodosComoLidos(
                currentPersonProvider.require().getPersonId(), LocalDateTime.now());
    }

    private PersonNotificationResponseDTO toResponse(PersonNotification aviso) {
        return PersonNotificationResponseDTO.builder()
                .personNotificationId(aviso.getPersonNotificationId())
                .subject(aviso.getSubject())
                .body(aviso.getBody())
                .event(aviso.getEvent())
                .createdAt(aviso.getCreatedAt())
                .readAt(aviso.getReadAt())
                .build();
    }

}
