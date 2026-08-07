package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Observation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ObservationRepository extends JpaRepository<Observation, UUID> {

    /**
     * As observacoes de um animal, do mais recente para o mais antigo.
     *
     * Ordena por {@code observedAt}, e nao por {@code recordedAt}: a pergunta e o que
     * aconteceu com o animal, e nao em que ordem alguem digitou (3.9).
     *
     * <b>Lista, e nao Page:</b> e leitura escopada por animal, como as vizinhas de vacina
     * e condicao. A leitura que cresce para sempre e a linha do tempo, e essa e paginada.
     */
    List<Observation> findByAnimalAnimalIdOrderByObservedAtDesc(UUID animalId);

    /**
     * Para o purger.
     *
     * Delete em massa serve aqui porque nada aponta para {@code observations} - ela e
     * filha direta do animal e nao tem neta. Onde ha neta (grants e grant_scopes) o
     * purger apaga por entidade, e o comentario dele explica por que.
     */
    void deleteByAnimalAnimalIdIn(List<UUID> animalIds);

}
