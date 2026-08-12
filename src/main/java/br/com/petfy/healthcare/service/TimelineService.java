package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.TimelineEntryResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TimelineService {

    Page<TimelineEntryResponseDTO> doAnimal(UUID animalId, Pageable pageable);

    /**
     * A mesma linha do tempo, com os dois recortes da Tela 30.
     *
     * {@code apenasDaMinhaOrganizacao} restringe ao que foi registrado em nome da organizacao em
     * que a pessoa esta agindo agora; {@code apenasMeus}, ao que ela mesma registrou. Os dois
     * juntos sao validos e e o que a tela permite marcar.
     *
     * <b>Sao booleanos e nao ids</b>: a pergunta da tela e "desta clinica" e "meu", e nao "da
     * clinica X". Aceitar um id deixaria o cliente recortar a linha por uma organizacao qualquer,
     * que e uma API mais larga do que qualquer tela pede.
     *
     * <b>{@code desde} e o terceiro recorte, e chegou com a hospedagem (Tela 47):</b> "isto e o que
     * aconteceu com ele desde que saiu de casa". Este SIM e uma data e nao um booleano, porque a
     * janela nao e uma das duas ou tres que a tela conhece — ela e a data em que aquela estadia
     * comecou, e cada animal tem a sua.
     */
    Page<TimelineEntryResponseDTO> doAnimal(UUID animalId, boolean apenasDaMinhaOrganizacao,
                                            boolean apenasMeus,
                                            java.time.LocalDateTime desde,
                                            Pageable pageable);

}
