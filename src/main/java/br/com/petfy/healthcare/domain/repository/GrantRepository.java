package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Grant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GrantRepository extends JpaRepository<Grant, UUID> {

    List<Grant> findByAnimalAnimalIdOrderByGrantedAtDesc(UUID animalId);

    Optional<Grant> findByTokenHash(String tokenHash);

    /**
     * Os animais que uma clinica alcanca agora.
     *
     * Substitui {@code findByClinicClinicIdAndRevokedAtIsNull} do PetClinicAccess, e
     * acrescenta a expiracao: acesso de clinica passou a poder ter prazo, e um
     * acesso vencido nao pode continuar aparecendo em "meus pacientes".
     */
    @Query("select g from Grant g where g.granteeClinic.clinicId = :clinicId "
            + "and g.revokedAt is null and (g.expiresAt is null or g.expiresAt > :agora)")
    List<Grant> findVigentesDaClinica(@Param("clinicId") UUID clinicId,
                                      @Param("agora") LocalDateTime agora);

    @Query("select g from Grant g where g.animal.animalId = :animalId "
            + "and g.granteeClinic.clinicId = :clinicId "
            + "and g.revokedAt is null and (g.expiresAt is null or g.expiresAt > :agora)")
    Optional<Grant> findVigenteDaClinicaNoAnimal(@Param("animalId") UUID animalId,
                                                 @Param("clinicId") UUID clinicId,
                                                 @Param("agora") LocalDateTime agora);

    @Query("select g from Grant g where g.animal.animalId = :animalId "
            + "and g.granteePerson.personId = :personId "
            + "and g.revokedAt is null and (g.expiresAt is null or g.expiresAt > :agora)")
    Optional<Grant> findVigenteDaPessoaNoAnimal(@Param("animalId") UUID animalId,
                                                @Param("personId") UUID personId,
                                                @Param("agora") LocalDateTime agora);

    /**
     * A concessao mais recente da clinica no animal, vigente ou nao.
     *
     * Existe para a revogacao continuar <b>idempotente</b>: revogar duas vezes nao e
     * erro, e a primeira data e que vale. Com a consulta que filtra vigencia, a
     * segunda chamada nao encontraria nada e responderia 404 - trocando um no-op por
     * um erro, o que quebraria um cliente que reenvia a requisicao.
     */
    Optional<Grant> findFirstByAnimalAnimalIdAndGranteeClinicClinicIdOrderByGrantedAtDesc(
            UUID animalId, UUID clinicId);

    /**
     * As pessoas que alcancam o animal por concessao agora.
     *
     * Só pessoas: clinica e link ficam de fora porque a pergunta que isto responde e
     * "quem mais poderia responder por este animal se quem responde sair" - e uma
     * clinica nunca vai responder por um animal.
     */
    @Query("select g from Grant g where g.animal.animalId = :animalId "
            + "and g.granteePerson is not null "
            + "and g.revokedAt is null and (g.expiresAt is null or g.expiresAt > :agora)")
    List<Grant> findVigentesDePessoasNoAnimal(@Param("animalId") UUID animalId,
                                              @Param("agora") LocalDateTime agora);

    /** Os animais que esta pessoa alcanca por concessao agora. */
    @Query("select g from Grant g where g.granteePerson.personId = :personId "
            + "and g.revokedAt is null and (g.expiresAt is null or g.expiresAt > :agora)")
    List<Grant> findVigentesDaPessoa(@Param("personId") UUID personId,
                                     @Param("agora") LocalDateTime agora);

    /**
     * Devolve as concessoes para o purger apagar por entidade, e nao um delete em
     * massa.
     *
     * <b>A diferenca nao e estilo.</b> {@code grant_scopes} aponta para
     * {@code grants} por chave estrangeira, e um {@code delete ... where animal_id
     * in (...)} em JPQL nao passa pela colecao: o Postgres recusaria por FK, e o
     * mock nao recusaria nada. E exatamente a familia dos seis bugs da Fase 4.
     * Carregando as entidades, o Hibernate apaga os escopos junto.
     */
    List<Grant> findByAnimalAnimalIdIn(List<UUID> animalIds);

}
