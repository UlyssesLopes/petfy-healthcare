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

    /**
     * "Buscar outro profissional" — quem pode receber um encaminhamento (Tela 45).
     *
     * <b>Busca por NOME e por ESPECIALIDADE, e a parcial existe aqui</b> — ao contrario da busca por
     * microchip da Tela 34, onde "9810" acharia todo animal de uma fabricante de chip. A diferenca
     * nao e de gosto: la a parcial servia varredura e nao servia a ninguem, porque quem tem o animal
     * na mao le o numero inteiro. Aqui a parcial E o gesto — digita-se "orto" para achar o
     * ortopedista, e exigir o nome exato faria a busca so funcionar para quem ja sabe a resposta.
     *
     * <b>Quem esta suspenso nao aparece.</b> Nao e punicao: encaminhar e indicar a quem o tutor vai
     * conceder acesso ao prontuario, e oferecer nessa lista um registro suspenso faria o produto
     * sugerir o que o conselho proibiu.
     *
     * <b>O join fetch da pessoa nao e otimizacao prematura</b>, e este projeto ja pagou por
     * descobrir isso cinco vezes: sem ele, montar o DTO com o nome de cada resultado faz uma
     * consulta por linha e estoura {@code LazyInitializationException} fora da transacao.
     */
    @Query("select c from ProfessionalCredential c join fetch c.person p "
            + "where c.status <> :suspenso "
            + "and (lower(p.name) like :busca or lower(coalesce(c.specialty, '')) like :busca) "
            + "order by p.name asc")
    List<ProfessionalCredential> buscarAtivas(@Param("busca") String busca,
                                              @Param("suspenso") CredentialStatus suspenso,
                                              org.springframework.data.domain.Pageable pageable);

    void deleteByPersonPersonId(UUID personId);

}
