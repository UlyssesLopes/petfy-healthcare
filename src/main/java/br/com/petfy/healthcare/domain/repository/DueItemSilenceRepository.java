package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.DueItemKind;
import br.com.petfy.healthcare.domain.entity.DueItemSilence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DueItemSilenceRepository extends JpaRepository<DueItemSilence, UUID> {

    /**
     * Tudo que esta pessoa silenciou.
     *
     * <b>Uma consulta para o feed inteiro.</b> Perguntar por item seria uma consulta por
     * pendencia, e o feed e a leitura que a area do tutor faz todo dia.
     */
    List<DueItemSilence> findByPersonPersonId(UUID personId);

    Optional<DueItemSilence> findByPersonPersonIdAndKindAndSourceId(UUID personId,
                                                                   DueItemKind kind,
                                                                   UUID sourceId);

    /** Para a exclusao de conta: o silencio e da pessoa, e sai com ela. */
    void deleteByPersonPersonId(UUID personId);

}
