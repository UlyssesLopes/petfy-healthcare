package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.AnimalDeath;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnimalDeathRepository extends JpaRepository<AnimalDeath, UUID> {

    /**
     * Os obitos de um lote de animais, para a lista de "quem ja esteve com voce".
     *
     * <b>Em lote, e nao um por animal.</b> A lista mostra data de fim em cada cartao, e uma
     * consulta por linha faria a tela de quem teve seis animais custar seis idas ao banco — o
     * mesmo N+1 que a V29 tirou da linha do tempo trazendo autoria para dentro da view.
     */
    List<AnimalDeath> findByAnimalIdIn(List<UUID> animalIds);

    void deleteByAnimalIdIn(List<UUID> animalIds);

}
