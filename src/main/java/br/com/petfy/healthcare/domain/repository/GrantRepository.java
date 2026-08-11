package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Grant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * Substitui {@code findByOrganizationOrganizationIdAndRevokedAtIsNull} do PetOrganizationAccess, e
     * acrescenta a expiracao: acesso de clinica passou a poder ter prazo, e um
     * acesso vencido nao pode continuar aparecendo em "meus pacientes".
     */
    @Query("select g from Grant g where g.granteeOrganization.organizationId = :organizationId "
            + "and g.revokedAt is null and (g.expiresAt is null or g.expiresAt > :agora)")
    List<Grant> findVigentesDaClinica(@Param("organizationId") UUID organizationId,
                                      @Param("agora") LocalDateTime agora);

    @Query("select g from Grant g where g.animal.animalId = :animalId "
            + "and g.granteeOrganization.organizationId = :organizationId "
            + "and g.revokedAt is null and (g.expiresAt is null or g.expiresAt > :agora)")
    Optional<Grant> findVigenteDaClinicaNoAnimal(@Param("animalId") UUID animalId,
                                                 @Param("organizationId") UUID organizationId,
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
    Optional<Grant> findFirstByAnimalAnimalIdAndGranteeOrganizationOrganizationIdOrderByGrantedAtDesc(
            UUID animalId, UUID organizationId);

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

    /**
     * TODAS as concessoes vigentes do animal — pessoa, organizacao e link.
     *
     * Existe para a transferencia de titularidade, e o "todas" e a regra: <b>acessos nao sao
     * herdados</b>. Quem recebe a responsabilidade recebe um animal cujos acessos ele proprio
     * concedera, e nao a lista que o titular anterior montou — inclusive o link compartilhado,
     * que continua valendo na mao de quem tiver a URL.
     */
    @Query("select g from Grant g where g.animal.animalId = :animalId "
            + "and g.revokedAt is null and (g.expiresAt is null or g.expiresAt > :agora)")
    List<Grant> findTodasVigentesNoAnimal(@Param("animalId") UUID animalId,
                                          @Param("agora") LocalDateTime agora);

    /**
     * Toda concessao que toca esta pessoa - recebida ou concedida por ela.
     *
     * Usada na exclusao de conta. As duas pontas entram: a concessao que ela recebeu
     * deixa de ter beneficiario, e a que ela concedeu deixa de ter autor - e concessao
     * sem autor nao diz mais de onde veio a autorizacao.
     *
     * Por entidade, e nao delete em massa: grant_scopes aponta para grants.
     */
    @Query("select g from Grant g where g.granteePerson.personId = :personId "
            + "or g.grantedBy.personId = :personId")
    List<Grant> findDaPessoa(@Param("personId") UUID personId);

    /** Os animais que esta pessoa alcanca por concessao agora. */
    @Query("select g from Grant g where g.granteePerson.personId = :personId "
            + "and g.revokedAt is null and (g.expiresAt is null or g.expiresAt > :agora)")
    List<Grant> findVigentesDaPessoa(@Param("personId") UUID personId,
                                     @Param("agora") LocalDateTime agora);

    /**
     * As duas consultas abaixo sao as de cima com recorte, e existem por causa da
     * area de organizacao: ela le centenas de animais, e sem pagina, busca e ordem
     * e inutilizavel para o ator que a justifica (PRODUTO 9.3 e 9.5).
     *
     * <b>Por que sao metodos novos e nao a mesma consulta com {@code Pageable}:</b>
     * as versoes {@code List} continuam servindo a exportacao LGPD, que precisa de
     * <i>tudo</i> e nao de uma pagina - paginar ali seria exportar dado incompleto.
     *
     * <b>Por que o termo chega pronto como {@code %texto%} e nao como {@code :q is
     * null}:</b> parametro nulo em comparacao de String faz o Postgres reclamar que
     * nao consegue inferir o tipo. Busca vazia vira {@code %}, que casa com tudo, e
     * o caminho e um so - sem ramo condicional dentro do SQL.
     *
     * A ordem nao esta na consulta de proposito: quem a decide e o {@code Pageable},
     * porque a tela ordena por nome, por especie e por quando o acesso foi
     * concedido, e nenhuma dessas e mais legitima que as outras.
     */
    @Query("select g from Grant g where g.granteeOrganization.organizationId = :organizationId "
            + "and g.revokedAt is null and (g.expiresAt is null or g.expiresAt > :agora) "
            + "and (lower(g.animal.name) like :termo "
            + "or lower(coalesce(g.animal.generalRegistry, '')) like :termo "
            + "or lower(coalesce(g.animal.microchipNumber, '')) like :termo)")
    Page<Grant> buscarVigentesDaClinica(@Param("organizationId") UUID organizationId,
                                        @Param("agora") LocalDateTime agora,
                                        @Param("termo") String termo,
                                        Pageable pageable);

    /**
     * Os ids de TODOS os animais que a organizacao alcanca agora — sem busca e sem pagina.
     *
     * O cabecalho da Tela 03 conta sobre a organizacao inteira ("vencendo em 30 dias · 12"), e
     * nao sobre a pagina aberta: um numero que muda ao virar a pagina nao e um resumo, e a
     * decisao que ele apoia — a quem ligar hoje — e sobre todo mundo.
     *
     * {@code distinct} porque a mesma organizacao pode ter mais de uma concessao vigente para o
     * mesmo animal (escopos concedidos em momentos diferentes), e o animal continua sendo um.
     */
    @Query("select distinct g.animal.animalId from Grant g "
            + "where g.granteeOrganization.organizationId = :organizationId "
            + "and g.revokedAt is null and (g.expiresAt is null or g.expiresAt > :agora)")
    List<UUID> idsDosAnimaisDaClinica(@Param("organizationId") UUID organizationId,
                                      @Param("agora") LocalDateTime agora);

    /** O mesmo para quem atua por si: o autonomo tambem tem area de organizacao (PRODUTO 9.3). */
    @Query("select distinct g.animal.animalId from Grant g "
            + "where g.granteePerson.personId = :personId "
            + "and g.revokedAt is null and (g.expiresAt is null or g.expiresAt > :agora)")
    List<UUID> idsDosAnimaisDaPessoa(@Param("personId") UUID personId,
                                     @Param("agora") LocalDateTime agora);

    @Query("select g from Grant g where g.granteePerson.personId = :personId "
            + "and g.revokedAt is null and (g.expiresAt is null or g.expiresAt > :agora) "
            + "and (lower(g.animal.name) like :termo "
            + "or lower(coalesce(g.animal.generalRegistry, '')) like :termo "
            + "or lower(coalesce(g.animal.microchipNumber, '')) like :termo)")
    Page<Grant> buscarVigentesDaPessoa(@Param("personId") UUID personId,
                                       @Param("agora") LocalDateTime agora,
                                       @Param("termo") String termo,
                                       Pageable pageable);

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
