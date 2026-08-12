package br.com.petfy.healthcare.security;

import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.Grant;
import br.com.petfy.healthcare.domain.entity.GrantLevel;
import br.com.petfy.healthcare.domain.entity.GrantScope;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.GrantRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Decide se a pessoa autenticada alcanca um animal, e em que nivel.
 *
 * Existe como peca unica porque antes da V15 essa decisao estava repetida em duas
 * dezenas de lugares. Enquanto a regra era "dono unico", repetir era so feio; com
 * varias origens de alcance, cada copia vira uma chance de alguem esquecer um caso
 * e transformar leitura em escrita - ou pior, deixar passar animal de terceiro.
 *
 * <b>Duas origens de alcance desde o P2b, e elas nao sao equivalentes:</b>
 *
 * <ul>
 *   <li><b>Custodia</b> - a pessoa responde pelo animal. Alcance total, sem prazo e
 *       sem concessao de ninguem. E o que era o papel HOLDER.</li>
 *   <li><b>Concessao</b> - alguem que responde pelo animal deu acesso a ela, com
 *       nivel e prazo. E o que eram os papeis EDITOR e VIEWER.</li>
 * </ul>
 *
 * A ordem da consulta importa: custodia primeiro. Quem responde pelo animal nao
 * precisa de concessao, e checar concessao antes faria o titular depender de uma
 * linha que nao existe para ele.
 *
 * <b>Animal inalcancavel responde 404, nunca 403.</b> Um 403 confirmaria que aquele
 * id existe, permitindo varrer ids para descobrir o que ha na base. A distincao so
 * aparece quando a pessoa ja alcanca o animal e falta nivel: ai o 403 e correto,
 * porque nao revela nada que ela ja nao saiba.
 */
@Component
@RequiredArgsConstructor
public class AnimalAccessGuard {

    private final AnimalRepository animalRepository;
    private final CustodyRepository custodyRepository;
    private final GrantRepository grantRepository;
    private final CurrentPersonProvider currentPersonProvider;
    private final CurrentProfessionalProvider currentProfessionalProvider;

    /** Le a carteira: custodia ou qualquer concessao vigente serve. */
    public Animal requireLeitura(UUID animalId) {
        return require(animalId, GrantLevel.VIEWER);
    }

    /**
     * Quem respondia pelo animal ate ele morrer continua lendo a vida dele. <b>Para sempre, e so
     * leitura.</b>
     *
     * A Tela 33 promete isso com todas as letras — "os sete anos de vida dele continuam aqui,
     * inteiros, para voce abrir quando quiser" — e sem esta excecao a promessa se quebraria no
     * dia seguinte ao encerramento: a custodia terminou, obito nao tem sucessor que conceda nada,
     * e o {@code require} responderia 404 a quem passou sete anos registrando aquilo.
     *
     * <b>Nao virou uma concessao gravada no encerramento, e a escolha e deliberada.</b> Toda
     * concessao tem quem a concedeu e quem pode revoga-la; aqui nao ha nem um nem outro. Uma
     * linha em {@code grants} "concedida por ninguem, revogavel por ninguem" seria uma mentira
     * sobre a origem daquele acesso, e apareceria na tela de quem cuida do animal como se alguem
     * tivesse autorizado.
     *
     * <b>Leitura e o teto, e nao um detalhe:</b> a excecao vive so no {@code VIEWER} porque
     * escrever exige responder pelo animal, e ninguem responde por um animal morto. Quem lia por
     * concessao antes do obito continua com a concessao que tinha — este caminho nao lhe tira
     * nem lhe da nada.
     */
    private boolean respondiaAteOFim(UUID animalId, UUID personId) {
        return custodyRepository.findEncerradaPorObitoDaPessoa(animalId, personId).isPresent();
    }

    /** Registra vacina, corrige peso, edita o cadastro: custodia ou concessao EDITOR. */
    public Animal requireEscrita(UUID animalId) {
        return require(animalId, GrantLevel.EDITOR);
    }

    /**
     * Concede acesso, transfere custodia, apaga o animal: <b>so quem responde por
     * ele</b>.
     *
     * Nenhum nivel de concessao chega aqui, e nao por rigor: quem tem acesso
     * concedido poderia conceder acesso a um comparsa e, no limite, tomar o animal
     * de quem responde por ele. Era a mesma razao pela qual o EDITOR nunca pode
     * convidar.
     */
    public Animal requireCustodia(UUID animalId) {
        UUID personId = currentPersonProvider.require().getPersonId();

        if (custodyRepository.findEmCursoDaPessoa(animalId, personId).isPresent()) {
            return carregar(animalId);
        }

        /*
         * A CUSTODIA DA ORGANIZACAO conta aqui, e nao contava antes.
         *
         * O `Custody` aceita `holderOrganization` desde o P2 — o abrigo que resgatou um animal
         * responde por ele, e nao ha tutor humano nenhum. Mas esta guarda so sabia perguntar
         * por pessoa, e o efeito era que NENHUM membro do abrigo conseguia agir sobre o animal
         * do proprio abrigo: nao dava para conceder acesso a uma clinica, nao dava para
         * convidar o adotante, nao dava para nada. A adocao inteira estava trancada por esta
         * linha que faltava.
         *
         * <b>Quem responde e a organizacao DECLARADA no cabecalho</b>, e nao qualquer uma de
         * que a pessoa participe. E a mesma regra do `organizacaoAssinante` das observacoes:
         * agir em nome de uma organizacao e uma escolha explicita de quem age, nunca uma
         * inferencia do servidor — senao um voluntario de dois abrigos agiria pelo abrigo
         * errado sem saber.
         */
        Optional<UUID> organizacaoDeclarada = organizacaoAtiva();

        if (organizacaoDeclarada.isPresent()
                && custodyRepository.findEmCursoDaOrganizacao(animalId, organizacaoDeclarada.get()).isPresent()) {
            return carregar(animalId);
        }

        // se a pessoa alcanca o animal por concessao, ela sabe que ele existe: negar com 404
        // seria mentir para quem ja tem a informacao
        if (concessaoVigente(animalId, personId).isPresent()) {
            throw nivelInsuficiente();
        }

        throw animalNaoEncontrado();
    }

    /**
     * A organizacao em nome de que a pessoa declarou estar agindo, se declarou.
     *
     * Devolve vazio sem reclamar: quem nao declarou nada esta agindo como pessoa, e isso e o
     * caso comum. O erro de contexto ambiguo pertence a quem PRECISA da organizacao para agir,
     * e nao a esta guarda.
     */
    private Optional<UUID> organizacaoAtiva() {
        return currentProfessionalProvider
                .organizacaoDeclarada(currentPersonProvider.require())
                .map(organizacao -> organizacao.getOrganizationId());
    }

    /**
     * A custodia em curso da pessoa autenticada neste animal.
     *
     * Substituiu {@code vinculoDoAutenticado}, que devolvia o PetTutor e servia para
     * qualquer um dos tres papeis. Agora quem quer saber sobre concessao pergunta
     * sobre concessao - misturar as duas coisas num retorno so foi o que este passo
     * desfez.
     */
    public Optional<Custody> custodiaDoAutenticado(UUID animalId) {
        UUID personId = currentPersonProvider.require().getPersonId();
        return custodyRepository.findEmCursoDaPessoa(animalId, personId);
    }

    /**
     * O escopo com que a pessoa autenticada alcanca o animal.
     *
     * <b>Devolve nulo para quem responde pelo animal</b>, e nulo aqui significa "sem
     * limite" - nao "sem acesso". Quem responde nao alcanca por concessao, entao nao ha
     * escopo a consultar: inventar um conjunto completo devolveria a mesma coisa com
     * mais linhas, e esconderia a diferenca entre as duas origens de alcance.
     *
     * <b>Conjunto vazio, por outro lado, e negativa.</b> Ele so aparece se alguem
     * gravar concessao sem escopo, o que a entidade e o servico tratam como acesso que
     * nao alcanca nada.
     */
    public Set<GrantScope> escopoDoAutenticado(UUID animalId) {
        UUID personId = currentPersonProvider.require().getPersonId();

        if (custodyRepository.findEmCursoDaPessoa(animalId, personId).isPresent()) {
            return null;
        }

        /*
         * Quem respondia ate o obito tambem le sem limite, e por uma razao pratica: um escopo
         * qualquer aqui faria a linha do tempo que ele leu inteira ontem voltar hoje com metade
         * dos eventos opacos. O animal morreu; o que ele podia ver nao encolheu por causa disso.
         */
        if (respondiaAteOFim(animalId, personId)) {
            return null;
        }

        return concessaoVigente(animalId, personId)
                .map(Grant::getScopes)
                .orElseGet(java.util.Set::of);
    }

    /**
     * Responde pelo animal? — a pergunta do {@link #requireCustodia}, sem lancar.
     *
     * <b>Existe para o dinheiro, e so para ele.</b> O combinado da creche (Tela 41) viaja no mesmo
     * DTO da matricula, que quem tem concessao le: sem esta pergunta, o petshop com acesso a
     * carteira do animal leria a mensalidade que o tutor paga a creche. "Nenhum escopo de acesso
     * concede preco junto com saude."
     *
     * <b>Nao serve para decidir o que uma rota deixa fazer</b> — para isso e {@code
     * requireCustodia}, que responde 404 ou 403 e nao deixa a decisao para quem chamou. Aqui a
     * resposta e "false" e o efeito e um campo que vem nulo, que e outra coisa.
     */
    public boolean respondePor(UUID animalId) {
        UUID personId = currentPersonProvider.require().getPersonId();

        if (custodyRepository.findEmCursoDaPessoa(animalId, personId).isPresent()) {
            return true;
        }

        // a custodia da ORGANIZACAO declarada conta, pela mesma razao do requireCustodia: o abrigo
        // que resgatou o animal responde por ele, e nao ha tutor humano nenhum
        return organizacaoAtiva()
                .flatMap(organizationId -> custodyRepository.findEmCursoDaOrganizacao(animalId, organizationId))
                .isPresent();
    }

    /** Alcanca de qualquer forma - por responder pelo animal ou por concessao. */
    public boolean alcanca(UUID animalId) {
        UUID personId = currentPersonProvider.require().getPersonId();

        return custodyRepository.findEmCursoDaPessoa(animalId, personId).isPresent()
                || concessaoVigente(animalId, personId).isPresent();
    }

    private Animal require(UUID animalId, GrantLevel nivelExigido) {
        UUID personId = currentPersonProvider.require().getPersonId();

        // custodia primeiro: quem responde pelo animal nao depende de concessao
        if (custodyRepository.findEmCursoDaPessoa(animalId, personId).isPresent()) {
            return carregar(animalId);
        }

        /*
         * A CUSTODIA DA ORGANIZACAO CONTA AQUI TAMBEM, e nao contava — <b>e este era o mesmo
         * defeito que a Tela 13 encontrou no `requireCustodia`, na outra metade da guarda.</b>
         *
         * La a linha que faltava trancava a adocao inteira: nenhum membro do abrigo conseguia
         * agir sobre o animal do proprio abrigo. Aqui ela trancava algo maior e menos visivel —
         * ler e ESCREVER. Um animal cuja custodia e de uma organizacao, sem tutor humano, so
         * podia receber registro de quem tivesse concessao: o abrigo nao conseguia lancar peso no
         * animal que ele resgatou, e a colonia inteira (Tela 43) seria inconstruivel, porque
         * "marcar que viu o gato, registrar ferida, foto" e escrita.
         *
         * <b>Quem responde e a organizacao DECLARADA no cabecalho</b>, pela mesma razao de sempre:
         * agir em nome de uma organizacao e escolha explicita de quem age, nunca inferencia do
         * servidor. Um voluntario de duas colonias registraria na colonia errada sem saber.
         *
         * O nivel exigido nao filtra aqui: quem RESPONDE pelo animal escreve, e nao ha grau de
         * custodia. E a mesma decisao que o ramo de cima ja tomava para a pessoa.
         */
        Optional<UUID> organizacaoDeclarada = organizacaoAtiva();

        if (organizacaoDeclarada.isPresent()
                && custodyRepository.findEmCursoDaOrganizacao(animalId, organizacaoDeclarada.get()).isPresent()) {
            return carregar(animalId);
        }

        // o animal morreu e quem responde por ele era esta pessoa: le, e nao escreve. A checagem
        // vem antes da concessao porque quem respondia nao tem concessao nenhuma para consultar
        if (nivelExigido == GrantLevel.VIEWER && respondiaAteOFim(animalId, personId)) {
            return carregar(animalId);
        }

        Grant concessao = concessaoVigente(animalId, personId)
                .orElseThrow(AnimalAccessGuard::animalNaoEncontrado);

        if (!concessao.getLevel().permite(nivelExigido)) {
            // aqui o 403 e seguro: quem chegou ate este ponto ja alcanca o animal,
            // entao a resposta nao revela existencia de nada novo
            throw nivelInsuficiente();
        }

        return carregar(animalId);
    }

    private Optional<Grant> concessaoVigente(UUID animalId, UUID personId) {
        return grantRepository.findVigenteDaPessoaNoAnimal(animalId, personId, LocalDateTime.now());
    }

    /**
     * Carrega o animal so depois de o alcance estar decidido.
     *
     * A ordem cobre o animal apagado entre o vinculo e a leitura: ele responde igual
     * a quem nunca alcancou, em vez de 500.
     */
    private Animal carregar(UUID animalId) {
        return animalRepository.findById(animalId)
                .orElseThrow(AnimalAccessGuard::animalNaoEncontrado);
    }

    private static PetfyHealthcareException nivelInsuficiente() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.INSUFFICIENT_ANIMAL_ROLE.getMessage(),
                ErrorMessageEnum.INSUFFICIENT_ANIMAL_ROLE.getCode(),
                HttpStatus.FORBIDDEN);
    }

    private static PetfyHealthcareException animalNaoEncontrado() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.ANIMAL_NOT_FOUND.getMessage(),
                ErrorMessageEnum.ANIMAL_NOT_FOUND.getCode(),
                HttpStatus.NOT_FOUND);
    }

}
