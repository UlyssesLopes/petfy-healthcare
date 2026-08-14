package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.PersonSession;

/**
 * Trocar um token vencido por um novo, sem pedir a senha.
 *
 * <b>E o que faz a sessao sobreviver a recarga da pagina.</b> O JWT vive em memoria no cliente, por
 * decisao contra XSS registrada no `ROADMAP.md` — recarregar sempre o perdeu. O refresh mora num
 * cookie httpOnly, que JavaScript nenhum le e que o navegador guarda: e o unico jeito de a sessao
 * durar sem afrouxar aquela regra.
 */
public interface SessionRenewal {

    /** O que a renovacao devolve: o JWT novo, e o refresh rotacionado que vai no cookie. */
    record Renovada(String token, String refreshToken, long expiresInMinutes, Person pessoa,
                    PersonSession sessao) { }

    /**
     * Renova a partir do refresh apresentado.
     *
     * <b>ROTACIONA SEMPRE.</b> Cada renovacao emite um refresh novo e apaga o anterior — um cookie
     * copiado deixa de valer no primeiro refresh legitimo que o dono fizer, e o intruso passa a ter
     * um papel sem valor em vez de acesso permanente.
     *
     * <b>Recusa com 401 e um estado so</b>, como toda leitura de token deste produto: refresh
     * inexistente, ja rotacionado, expirado e de sessao encerrada respondem igual. Distinguir diria
     * a quem tenta adivinhar qual parte errou — e aqui a diferenca entre "expirado" e "encerrado" e
     * justamente a informacao que interessa a quem roubou o cookie.
     */
    Renovada renovar(String refreshToken);

    /**
     * Encerra a sessao de quem esta saindo, e o cookie sai junto.
     *
     * <b>Sair passou a ter efeito no servidor.</b> Antes o cliente jogava o token fora e pronto: o
     * JWT continuava valido ate expirar, e nada mais existia para invalidar. Com a sessao gravada,
     * sair encerra a entrada — e o refresh que estava no navegador deixa de renovar.
     */
    void sair(String refreshToken);

}
