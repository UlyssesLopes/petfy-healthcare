package br.com.petfy.healthcare.domain.entity;

/**
 * Documento que o titular aceita.
 *
 * Sao dois e nao um porque tratam de coisas diferentes: os termos regem o uso do
 * servico, a politica de privacidade rege o tratamento de dado pessoal. Uma pode
 * mudar sem a outra, e a versao aceita e por documento - juntar os dois num aceite
 * unico faria a mudanca de qualquer um invalidar o consentimento do outro.
 */
public enum ConsentDocument {

    TERMS_OF_SERVICE,

    /**
     * A que importa para a LGPD: e ela que descreve qual dado e tratado, para que, e
     * por quanto tempo. Dado de saude de animal ligado a nome e e-mail de pessoa
     * fisica e dado pessoal do tutor, nao do animal.
     */
    PRIVACY_POLICY

}
