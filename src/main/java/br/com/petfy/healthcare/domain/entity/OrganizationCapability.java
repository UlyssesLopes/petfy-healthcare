package br.com.petfy.healthcare.domain.entity;

/**
 * O que uma organizacao <b>pode fazer</b> sobre animais.
 *
 * <b>Por que existe, e por que nao ha tipo de organizacao.</b> Clinica, creche e
 * abrigo nao sao tres entidades: sao a mesma exercendo conjuntos diferentes destas
 * capacidades. Modelar como tipo obrigaria a escolher um e mentir sobre o resto - e
 * ONG com clinica propria faz as tres, creche que hospeda faz duas.
 *
 * <table>
 *   <tr><th>Capacidade</th><th>Clinica</th><th>Creche</th><th>Abrigo</th></tr>
 *   <tr><td>REGISTRAR_ATO_CLINICO</td><td>sim</td><td>nao</td><td>com vet membro</td></tr>
 *   <tr><td>REGISTRAR_OBSERVACAO</td><td>sim</td><td>sim</td><td>sim</td></tr>
 *   <tr><td>COMUNICAR_COM_TUTOR</td><td>sim</td><td>sim</td><td>sim</td></tr>
 *   <tr><td>DETER_CUSTODIA</td><td>nao</td><td>nao</td><td>sim</td></tr>
 *   <tr><td>GERIR_TURMA_E_VAGA</td><td>nao</td><td>sim</td><td>nao</td></tr>
 *   <tr><td>MANTER_REDE_DE_LARES</td><td>nao</td><td>nao</td><td>sim</td></tr>
 * </table>
 *
 * <b>Capacidade nao e o mesmo que lotacao.</b> Aqui esta o que a organizacao pode
 * fazer; quantos animais ela comporta e outra coisa, e chega no Horizonte 3.
 */
public enum OrganizationCapability {

    /**
     * Diagnostico, prescricao, procedimento.
     *
     * <b>A organizacao ter a capacidade nao basta:</b> quem pratica precisa de
     * credencial profissional. E por isso que o abrigo pode ter esta capacidade e
     * exerce-la apenas atraves do vet membro.
     */
    REGISTRAR_ATO_CLINICO,

    /**
     * O que alguem viu: nao comeu, mancou, vomitou, brigou.
     *
     * Aberta a qualquer membro. <b>Observacao nunca vira ato clinico sozinha</b> - a
     * creche registra o que viu, e o diagnostico vem de quem pode dar. Sem essa
     * fronteira, em seis meses ha monitor de creche lancando suspeita de displasia no
     * historico medico do animal.
     */
    REGISTRAR_OBSERVACAO,

    /** Recado, foto, avaliacao do dia. Nao entra no prontuario. */
    COMUNICAR_COM_TUTOR,

    /**
     * Responder pelo animal, e nao apenas alcanca-lo por concessao.
     *
     * So o abrigo. E o que destrava o animal sem tutor humano - resgate, animal de
     * rua, ninhada -, e e a unica capacidade que aparece na custodia em vez de numa
     * concessao.
     */
    DETER_CUSTODIA,

    /** Turma, matricula, vaga e janela de check-in. Da creche. */
    GERIR_TURMA_E_VAGA,

    /** A rede de lares transitorios. Do abrigo. */
    MANTER_REDE_DE_LARES

}
