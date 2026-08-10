package br.com.petfy.healthcare.domain.entity;

/**
 * O estado da matricula.
 *
 * <b>PENDENTE nao e recusa</b>, e essa distincao e a tela inteira: a Tela 10 escreve "a matricula
 * fica guardada e se completa sozinha assim que a dose for registrada". Recusar faria a creche
 * refazer o cadastro depois; guardar faz o produto trabalhar para o tutor.
 */
public enum EnrollmentStatus {

    /** Existe, esta guardada, e falta comprovacao de saude para ativar. */
    PENDENTE,

    /** Vale: o animal entra. */
    ATIVA,

    /** Terminou — saida, transferencia de titularidade ou fim do vinculo com a creche. */
    ENCERRADA
}
