package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.PersonNotificationResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * O aviso que fica dentro do produto.
 *
 * <b>Todo metodo parte do autenticado</b>, e nao ha rota para os avisos de outra pessoa: o que
 * alguem foi avisado e dado dela, e nao do animal.
 */
public interface PersonNotificationService {

    Page<PersonNotificationResponseDTO> listMine(Pageable pageable);

    long countMineUnread();

    PersonNotificationResponseDTO markAsRead(UUID personNotificationId);

    /** Devolve quantos deixaram de estar por ler. */
    int markAllAsRead();

}
