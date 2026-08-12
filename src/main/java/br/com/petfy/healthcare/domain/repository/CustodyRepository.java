package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Custody;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustodyRepository extends JpaRepository<Custody, UUID> {

    /**
     * A custodia em curso do animal.
     *
     * Devolve Optional e nao a entidade porque o animal pode estar entre custodias
     * num instante - obito e perda encerram sem sucessor. Quem chama trata o vazio
     * como "ninguem responde por este animal agora", que e um estado legitimo.
     *
     * O indice unico parcial no banco garante que ha no maximo uma.
     */
    @Query("select c from Custody c where c.animal.animalId = :animalId and c.endedAt is null")
    Optional<Custody> findEmCurso(@Param("animalId") UUID animalId);

    @Query("select c from Custody c where c.animal.animalId = :animalId "
            + "and c.holderPerson.personId = :personId and c.endedAt is null")
    Optional<Custody> findEmCursoDaPessoa(@Param("animalId") UUID animalId,
                                          @Param("personId") UUID personId);

    /** Os animais por que a pessoa responde agora. */
    @Query("select c from Custody c where c.holderPerson.personId = :personId and c.endedAt is null")
    List<Custody> findEmCursoDaPessoa(@Param("personId") UUID personId);

    /**
     * A custodia em curso do animal quando quem responde e uma ORGANIZACAO.
     *
     * <b>Ela existe desde o P2 e nunca teve caminho HTTP.</b> O {@code holderOrganization} e
     * exclusivo com o {@code holderPerson}: um abrigo que responde por um animal resgatado nao
     * tem tutor humano, e e por isso que o modelo previu os dois. Sem esta consulta, a guarda
     * so sabia perguntar por pessoa — e nenhum membro do abrigo conseguia agir sobre o animal
     * do proprio abrigo.
     */
    @Query("select c from Custody c where c.animal.animalId = :animalId "
            + "and c.holderOrganization.organizationId = :organizationId and c.endedAt is null")
    Optional<Custody> findEmCursoDaOrganizacao(@Param("animalId") UUID animalId,
                                               @Param("organizationId") UUID organizationId);

    /**
     * Os animais sob custodia da organizacao agora — a lista do abrigo (Tela 12).
     *
     * Paginada e com busca por nome ou microchip, como a lista da clinica: um abrigo com 84
     * animais nao cabe numa tela, e o desenho ja pede o campo de busca.
     */
    /*
     * O `join fetch` do animal nao e otimizacao prematura: sem ele, uma pagina de 20 custodias
     * faz 21 consultas para montar o DTO — e foi o proprio teste desta consulta que denunciou o
     * problema, quebrando com LazyInitializationException ao ler o nome do animal. O animal e
     * ManyToOne, entao paginar com fetch dele nao traz o aviso de colecao em memoria.
     */
    @Query("select c from Custody c join fetch c.animal a "
            + "where c.holderOrganization.organizationId = :organizationId "
            + "and c.endedAt is null "
            + "and (lower(a.name) like :busca "
            + "  or lower(coalesce(a.microchipNumber, '')) like :busca)")
    Page<Custody> buscarEmCursoDaOrganizacao(@Param("organizationId") UUID organizationId,
                                             @Param("busca") String busca,
                                             Pageable pageable);

    /** A historia inteira do animal, da custodia mais antiga para a mais recente. */
    List<Custody> findByAnimalAnimalIdOrderByStartedAtAsc(UUID animalId);

    /**
     * Toda custodia da pessoa, encerrada ou nao.
     *
     * Usada na exclusao de conta: uma custodia encerrada e historia do animal, e
     * apagar a pessoa nao pode deixar a linha com um buraco sem nome - mas a linha
     * tambem nao pode segurar a exclusao por chave estrangeira.
     */
    List<Custody> findByHolderPersonPersonId(UUID personId);

    List<Custody> findByAnimalAnimalIdIn(List<UUID> animalIds);

    /**
     * A pessoa respondeu por este animal ate ele morrer?
     *
     * <b>E a consulta que mantem a promessa central da Tela 33:</b> "os sete anos de vida dele
     * continuam aqui, inteiros, para voce abrir quando quiser". Sem ela, encerrar por obito
     * arrancaria do tutor o acesso a vida que ele passou sete anos registrando — a custodia
     * acabou, nao ha concessao nenhuma, e o guard responderia 404 no dia seguinte ao enterro.
     *
     * <b>So OBITO, e nao qualquer custodia encerrada.</b> Quem transferiu o animal recebe
     * concessao de leitura no ato da transferencia, e essa concessao e revogavel por quem passou
     * a responder — como deve ser. Aqui nao ha quem revogue nem quem conceda, e e por isso que a
     * leitura nasce do fato e nao de uma concessao inventada em nome de ninguem.
     */
    @Query("select c from Custody c where c.animal.animalId = :animalId "
            + "and c.holderPerson.personId = :personId "
            + "and c.endReason = br.com.petfy.healthcare.domain.entity.CustodyEndReason.OBITO")
    Optional<Custody> findEncerradaPorObitoDaPessoa(@Param("animalId") UUID animalId,
                                                    @Param("personId") UUID personId);

    /**
     * "Quem ja esteve com voce" — os animais de quem a pessoa cuidou e nao cuida mais.
     *
     * <b>O filtro de baixo e o que impede a lista de duplicar a outra.</b> Quem transfere um
     * animal recebe concessao de leitura no ato, e continua vendo o bicho na lista principal;
     * mostra-lo tambem aqui diria que ele saiu quando ele esta na tela ao lado. Entao esta lista
     * e "teve custodia encerrada E nao alcanca mais de nenhum jeito" — que na pratica e o animal
     * que morreu, e o que foi transferido sem que sobrasse acesso.
     */
    @Query("select c.animal from Custody c "
            + "where c.holderPerson.personId = :personId and c.endedAt is not null "
            + "and not exists ("
            + "  select 1 from Custody atual where atual.animal = c.animal "
            + "    and atual.holderPerson.personId = :personId and atual.endedAt is null) "
            + "and not exists ("
            + "  select 1 from Grant g where g.animal = c.animal "
            + "    and g.granteePerson.personId = :personId and g.revokedAt is null "
            + "    and (g.expiresAt is null or g.expiresAt > :agora))")
    Page<Animal> findQueJaEstiveramComAPessoa(@Param("personId") UUID personId,
                                              @Param("agora") java.time.LocalDateTime agora,
                                              Pageable pageable);

}
