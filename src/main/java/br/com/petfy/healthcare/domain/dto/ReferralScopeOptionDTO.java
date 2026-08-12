package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.GrantScope;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Uma caixa do "o que vai junto", com o que existe de verdade dentro dela (Tela 45).
 *
 * <b>O desenho mostra "12 atendimentos desde 2019", e o número tem de ser o número.</b> Uma caixa
 * rotulada só "Prontuário" faria quem encaminha marcar às cegas — e é justamente contra o marcar às
 * cegas que o desenho escreve, na quarta caixa, "147 eventos. Provavelmente mais do que ele precisa
 * para este caso."
 *
 * <b>Os números vêm da linha do tempo</b>, e não de contar cada tabela de origem: assim a promessa da
 * caixa e o que o especialista abre depois são a mesma consulta, com o mesmo mapeamento de tipo para
 * escopo. Ver {@code TimelineRepository.contagemPorTipo}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralScopeOptionDTO {

    private GrantScope scope;

    /**
     * Quantos eventos deste escopo existem no animal.
     *
     * <b>Zero é resposta, e não ausência.</b> Um escopo sem nada dentro continua na lista, marcável:
     * conceder {@code OBSERVACOES} num animal que ainda não tem nenhuma vale para o que a creche
     * escrever durante o tratamento — e esconder a caixa faria o especialista perder exatamente as
     * observações que o caso dele vai gerar.
     */
    private long events;

    /** O evento mais antigo deste escopo. Nulo quando não há nenhum. */
    private LocalDateTime since;

    /**
     * Se esta caixa vem marcada.
     *
     * <b>O servidor sugere; a tela não decide sozinha.</b> As sugeridas são as três que o desenho
     * mostra marcadas — prontuário, anexos, peso e observações —, que é o recorte clínico de um
     * encaminhamento. Carteira, condições e contato ficam desmarcadas: são úteis e não são o caso.
     */
    private boolean suggested;

    /**
     * Se o escopo carrega dado de contato de quem responde pelo animal.
     *
     * Existe para a tela poder avisar antes do gesto: {@code CONTATO} não é um pedaço do prontuário,
     * é o telefone do tutor — e marcá-lo junto com os outros, sem que a diferença apareça, entregaria
     * dado pessoal escondido dentro de um recorte clínico.
     */
    private boolean personalData;

}
