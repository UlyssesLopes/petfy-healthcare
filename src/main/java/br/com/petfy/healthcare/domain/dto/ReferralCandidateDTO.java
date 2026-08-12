package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Um profissional que pode receber o encaminhamento — o "Buscar outro profissional" da Tela 45.
 *
 * <b>A busca vive DENTRO do animal</b>, e não numa rota de gente: {@code
 * GET /animals/{id}/referral-candidates}. São duas razões, e a segunda é a que decidiu.
 *
 * A primeira é que ela responde melhor: "Ele já registrou o raio-X do Code em 2023" só existe se a
 * consulta souber de que animal se fala.
 *
 * A segunda é privacidade. Uma rota que devolve pessoas por nome parcial é varredura de dado pessoal
 * — é a lição do {@code POST /found} do bloco 5, onde a busca por microchip ganhou limite por IP
 * justamente por isso. Debaixo do animal, quem varre precisa antes ter acesso de escrita a um animal;
 * a trava não é um filtro de taxa, é a mesma guarda que já protege tudo.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralCandidateDTO {

    private UUID personId;

    private String name;

    /** Nula em quem não declarou. A busca não pode exigir este campo — ver a V42. */
    private String specialty;

    /** "CRMV-SP 12345" já montado: o cliente não deveria saber como se compõe um registro. */
    private String credential;

    /**
     * As organizações de que a pessoa participa. "Clínica Anhangabaú", no desenho.
     *
     * Plural porque o veterinário que atende em duas clínicas é o caso comum, e escolher uma para
     * mostrar esconderia metade de onde encontrá-lo.
     */
    private List<String> organizations;

    /**
     * Quando esta pessoa registrou algo neste animal por último. Nulo se nunca registrou.
     *
     * <b>É o que o desenho põe embaixo do nome:</b> "Ele já registrou o raio-X do Code em 2023." Sai
     * do {@code ultimaContribuicaoPorPessoa}, que a rede de quem cuida já usava — e é a diferença
     * entre encaminhar para um nome numa lista e encaminhar para quem já conhece o caso.
     */
    private LocalDateTime lastContributionAt;

}
