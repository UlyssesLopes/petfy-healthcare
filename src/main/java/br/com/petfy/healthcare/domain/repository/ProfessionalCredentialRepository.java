package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.CredentialStatus;
import br.com.petfy.healthcare.domain.entity.ProfessionalCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProfessionalCredentialRepository extends JpaRepository<ProfessionalCredential, UUID> {

    List<ProfessionalCredential> findByPersonPersonId(UUID personId);

    /**
     * Responde "esta pessoa pode praticar ato clinico?" pelo e-mail, sem carregar
     * a pessoa.
     *
     * Existe com o e-mail e nao com o id porque quem pergunta primeiro e a cadeia
     * de filtros, que so tem o que o token carrega. E devolve boolean em vez da
     * lista porque essa consulta roda em toda requisicao de /vet/**.
     */
    @Query("select count(c) > 0 from ProfessionalCredential c "
            + "where c.person.email = :email and c.status <> :suspenso")
    boolean existsAtivaPorEmail(@Param("email") String email,
                                @Param("suspenso") CredentialStatus suspenso);

    boolean existsByCouncilAndUfAndNumber(String council, String uf, String number);

    void deleteByPersonPersonId(UUID personId);

}
