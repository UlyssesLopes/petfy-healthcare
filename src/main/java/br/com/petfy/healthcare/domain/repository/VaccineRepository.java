package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Vaccine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface VaccineRepository extends JpaRepository<Vaccine, UUID> {

    /** Usado pela agenda e lembretes, onde paginacao nao se aplica. */
    List<Vaccine> findByPetOwnerOwnerId(UUID ownerId);

    /** Usado pela listagem paginada do controller. */
    Page<Vaccine> findByPetOwnerOwnerId(UUID ownerId, Pageable pageable);

    /**
     * Usado pela rotina de lembretes, que varre a base inteira e nao um tutor.
     * O filtro por ultimo envio fica no service: aqui a data da proxima dose ja
     * corta a maior parte, e deixar o resto em memoria mantem a regra de
     * reenvio testavel sem banco.
     */
    List<Vaccine> findByNextDoseDateLessThanEqual(LocalDate limite);

    List<Vaccine> findByPetPetIdOrderByApplicationDateDesc(UUID petId);

}
