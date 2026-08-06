package br.com.petfy.healthcare.domain.entity;

/**
 * O estado de uma credencial profissional.
 *
 * Credencial tem estado, e e por isso que ela nao serve como chave de
 * identidade: um profissional suspenso perderia a conta, e a linha do tempo
 * perderia o autor de atos que continuam validos.
 */
public enum CredentialStatus {

    /**
     * A pessoa informou o registro e ninguem conferiu. E o unico estado possivel
     * hoje: nao ha integracao com conselho. O registro carrega essa informacao em
     * vez de fingir garantia que o produto nao tem.
     */
    INFORMADO,

    /** Conferido junto ao conselho. Depende de integracao que ainda nao existe. */
    VERIFICADO,

    /**
     * Suspenso ou cassado. Deixa de autorizar ato clinico novo, e nao apaga
     * nem invalida o que ja foi assinado.
     */
    SUSPENSO;

    /**
     * Credencial informada ja autoriza ato clinico. Exigir VERIFICADO travaria o
     * produto inteiro numa integracao que nao existe - o preco de aceitar
     * INFORMADO e dito em voz alta no registro, que e o que 5.10 pede.
     */
    public boolean autorizaAtoClinico() {
        return this != SUSPENSO;
    }

}
