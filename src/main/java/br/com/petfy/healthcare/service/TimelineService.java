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
     */
    Page<TimelineEntryResponseDTO> doAnimal(UUID animalId, boolean apenasDaMinhaOrganizacao,
                                            boolean apenasMeus, Pageable pageable);

}
