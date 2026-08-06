package br.com.petfy.healthcare.security;

import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.OrganizationCapability;
import br.com.petfy.healthcare.domain.entity.Person;

import java.util.Optional;

/**
 * O ponto de vista ativo de quem esta atuando: a pessoa, e em nome de quem ela age.
 *
 * <b>E a secao 3.2 do PRODUTO virando codigo.</b> Uma pessoa com tres vinculos tem
 * tres contextos, e o que ela ve, pode e registra muda conforme o contexto - "a Ana
 * registrou" e "a Ana, pela Clinica Norte, registrou" sao fatos diferentes, e so o
 * segundo carrega responsabilidade institucional.
 *
 * <b>Substituiu {@code person.getOrganization()}.</b> Aquele campo dizia que a pessoa
 * <i>pertence</i> a uma organizacao, no singular e para sempre. Este diz em nome de
 * quem ela esta agindo <i>nesta requisicao</i> - e admite que a resposta seja
 * "ninguem", que e o veterinario autonomo.
 *
 * @param person       quem esta atuando. Nunca nulo.
 * @param organization em nome de quem, quando ha. Nulo no autonomo.
 */
public record ProfessionalContext(Person person, Organization organization) {

    public Optional<Organization> organizacao() {
        return Optional.ofNullable(organization);
    }

    public boolean atuaPorOrganizacao() {
        return organization != null;
    }

    /**
     * Se este contexto permite a capacidade.
     *
     * <b>Sem organizacao devolve verdadeiro, e isso nao e falha de validacao.</b>
     * Capacidade limita o que uma <i>organizacao</i> faz; quem atua por si e limitado
     * pela propria credencial, que o ProfessionalAccessManager ja conferiu. O
     * autonomo nao tem organizacao para restringi-lo, e inventar uma restricao aqui
     * seria reintroduzir a exigencia de clinica que este passo acabou de remover.
     */
    public boolean permite(OrganizationCapability capacidade) {
        return organization == null || organization.pode(capacidade);
    }

}
