package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Referral;
import br.com.petfy.healthcare.domain.entity.ReferralStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferralRepository extends JpaRepository<Referral, UUID> {

    /**
     * O mesmo animal, para o mesmo especialista, ja encaminhado e esperando decisao.
     *
     * <b>Quem encaminhou nao entra na pergunta</b>, e e deliberado: se a Ana ja encaminhou o Code
     * para o Roberto e o pedido esta parado, um colega da mesma clinica encaminhando de novo nao
     * adiciona informacao — adiciona uma segunda pergunta sobre o mesmo caso ao mesmo tutor.
     */
    @Query("select r from Referral r where r.animal.animalId = :animalId "
            + "and r.toPerson.personId = :toPersonId "
            + "and r.status = br.com.petfy.healthcare.domain.entity.ReferralStatus.PENDENTE")
    Optional<Referral> findPendenteDoAnimalPara(@Param("animalId") UUID animalId,
                                                @Param("toPersonId") UUID toPersonId);

    /**
     * O que espera decisao de quem responde pelos animais desta pessoa — a tela do tutor.
     *
     * <b>A pergunta parte da CUSTODIA, e nao de uma coluna no encaminhamento.</b> Guardar aqui para
     * quem o pedido foi enderecado seria mais rapido de consultar e estaria errado no dia seguinte:
     * a custodia passa de mao em mao — e a Tela 44 acabou de construir um fluxo que a passa —, e um
     * pedido enderecado a quem respondia ontem ficaria parado para sempre, esperando decisao de
     * alguem que nao decide mais nada sobre aquele animal.
     *
     * <b>A custodia de ORGANIZACAO conta, e so a DECLARADA.</b> E a mesma regra do
     * {@code requireCustodia}: agir em nome de uma organizacao e escolha explicita de quem age,
     * nunca inferencia do servidor. Quem nao declarou nada passa {@code null} e ve so os animais
     * que responde como pessoa.
     */
    @Query("select r from Referral r "
            + "where r.status = br.com.petfy.healthcare.domain.entity.ReferralStatus.PENDENTE "
            + "and exists (select 1 from Custody c where c.animal = r.animal and c.endedAt is null "
            + "  and (c.holderPerson.personId = :personId "
            + "    or (:organizationId is not null "
            + "        and c.holderOrganization.organizationId = :organizationId))) "
            + "order by r.requestedAt desc")
    List<Referral> findPendentesParaDecisaoDe(@Param("personId") UUID personId,
                                              @Param("organizationId") UUID organizationId);

    /**
     * O que chegou para o especialista — a caixa de entrada do lado que recebe.
     *
     * <b>Traz o pendente tambem, e essa e a decisao que mais pesa nesta consulta.</b> A tentacao e
     * mostrar so o autorizado, porque e o unico que ele pode abrir; e o efeito seria pior que a
     * inconsistencia que evita. O especialista que sabe que ha um caso esperando autorizacao pode
     * ligar para a clinica que encaminhou — e um pedido parado por tres semanas e informacao, nao
     * ruido. O que ele NAO ve do pendente e o animal: nome, prontuario, nada. Ver
     * {@code ReferralServiceImpl}.
     */
    @Query("select r from Referral r where r.toPerson.personId = :personId "
            + "order by r.requestedAt desc")
    List<Referral> findRecebidosPor(@Param("personId") UUID personId);

    /** O que esta clinica encaminhou, para ela acompanhar o que foi autorizado e o que nao foi. */
    @Query("select r from Referral r where r.animal.animalId = :animalId "
            + "order by r.requestedAt desc")
    List<Referral> findDoAnimal(@Param("animalId") UUID animalId);

    List<Referral> findByStatusAndAnimalAnimalId(ReferralStatus status, UUID animalId);

    /**
     * Todo encaminhamento em que esta pessoa aparece — para a exclusao de conta.
     *
     * <b>Os tres papeis, e nao so quem encaminhou.</b> A tabela aponta para {@code persons} em tres
     * colunas, e duas delas sao NOT NULL: esquecer qualquer uma faria o {@code DELETE /persons/me}
     * responder 500 para quem tivesse encaminhado ou recebido um caso. E a mesma familia de defeito
     * que a credencial profissional e o silencio de pendencia ja produziram — tabela nova que aponta
     * para a conta e nao entra na lista de exclusao.
     *
     * <b>Apaga em vez de desassociar</b>, ao contrario do anexo, que perde so o autor. O anexo
     * pertence ao ANIMAL: o laudo continua valendo sem quem o subiu. Um encaminhamento e uma conversa
     * entre duas pessoas nomeadas — "Ana encaminhou para o Roberto" —, e sem uma das duas a linha nao
     * diz mais nada. A concessao que ele produziu sai junto pelo caminho dela.
     */
    @Query("select r from Referral r where r.referredBy.personId = :personId "
            + "or r.toPerson.personId = :personId or r.decidedBy.personId = :personId")
    List<Referral> findEnvolvendoPessoa(@Param("personId") UUID personId);

    List<Referral> findByAnimalAnimalIdIn(List<UUID> animalIds);

    void deleteByAnimalAnimalIdIn(List<UUID> animalIds);

}
