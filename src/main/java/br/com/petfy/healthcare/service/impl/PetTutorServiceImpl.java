package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.PetTutorInviteRequestDTO;
import br.com.petfy.healthcare.domain.dto.PetTutorInviteResponseDTO;
import br.com.petfy.healthcare.domain.dto.PetTutorResponseDTO;
import br.com.petfy.healthcare.domain.dto.PetTutorRoleUpdateRequestDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.CustodyEndReason;
import br.com.petfy.healthcare.domain.entity.CustodyNature;
import br.com.petfy.healthcare.domain.entity.Grant;
import br.com.petfy.healthcare.domain.entity.GrantLevel;
import br.com.petfy.healthcare.domain.entity.GrantScope;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.PetTutorInvite;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.GrantRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.PetTutorInviteRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.notification.PetTutorActivityNotifier;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.OpaqueTokenService;
import br.com.petfy.healthcare.service.PetTutorService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Quem cuida do animal junto: convite, acesso concedido e troca de quem responde.
 *
 * <b>O que mudou no P2b.</b> Este servico mexia numa tabela so, o {@code pet_tutors},
 * onde HOLDER e os outros dois papeis moravam juntos. Agora mexe em duas coisas
 * diferentes, e a diferenca e o ponto: <b>custodia</b> para quem responde pelo
 * animal, <b>concessao</b> para quem recebeu acesso. Convidar alguem como EDITOR e
 * conceder acesso; convidar como HOLDER e transferir a responsabilidade - eram o
 * mesmo insert antes, e nao deveriam ser.
 */
@Service
@RequiredArgsConstructor
public class PetTutorServiceImpl implements PetTutorService {

    /**
     * O escopo de uma concessao a pessoa.
     *
     * Total, e nao minimo, porque e o que um co-tutor sempre alcancou. Escopo
     * estreito para pessoa nao e caso de v1 - os dois que o produto nomeia, a creche
     * e o cartao de emergencia, sao concessao a organizacao e por link, e ja tem
     * escopo desde o P2a.
     */
    private static final Set<GrantScope> ESCOPO_DE_PESSOA = Set.of(
            GrantScope.CARTEIRA, GrantScope.CONDICOES, GrantScope.PRONTUARIO,
            GrantScope.PESO, GrantScope.ANEXOS, GrantScope.CONTATO);

    private final CustodyRepository custodyRepository;
    private final GrantRepository grantRepository;
    private final PetTutorInviteRepository petTutorInviteRepository;
    private final PersonRepository personRepository;
    private final CurrentPersonProvider currentPersonProvider;
    private final AnimalAccessGuard animalAccessGuard;
    private final OpaqueTokenService opaqueTokenService;
    private final PetTutorActivityNotifier petTutorActivityNotifier;

    @Value("${petfy.pet-tutor-invite.default-expiration-days:7}")
    private int defaultExpirationDays;

    /**
     * Convidar e de quem responde pelo animal. Quem tem acesso concedido nao convida:
     * bastaria convidar um comparsa como HOLDER para tomar o animal de quem o
     * cadastrou.
     *
     * O convite existe em vez de vinculo direto porque o caso comum e o conjuge que
     * <b>ainda nao tem conta</b> - exigir cadastro previo mataria o fluxo onde ele
     * comeca. E vincular alguem sem que aceite faria a pessoa passar a receber
     * lembrete que nao pediu.
     */
    @Override
    @Transactional
    public PetTutorInviteResponseDTO invite(UUID animalId, PetTutorInviteRequestDTO request) {
        Animal animal = animalAccessGuard.requireCustodia(animalId);
        Person emissor = currentPersonProvider.require();

        recusarSeJaAlcanca(animalId, request.getEmail());

        int validade = request.getExpiresInDays() != null
                ? request.getExpiresInDays()
                : defaultExpirationDays;

        String token = opaqueTokenService.generate();

        PetTutorInvite invite = petTutorInviteRepository.save(PetTutorInvite.builder()
                .animal(animal)
                .createdBy(emissor)
                .tokenHash(opaqueTokenService.hash(token))
                .email(request.getEmail().trim())
                .role(request.getRole())
                .expiresAt(LocalDateTime.now().plusDays(validade))
                .creationDate(LocalDateTime.now())
                .build());

        // unico momento em que o token existe fora do cliente
        return toResponse(invite, token);
    }

    /**
     * Aceitar exige estar autenticado, e o e-mail da conta tem de ser o do convite.
     * Sem isso o link viraria portador: quem o recebesse encaminhado entraria no
     * historico de saude de um animal que nao e dele.
     *
     * <b>Token invalido, expirado, revogado, ja usado e destinado a outra pessoa
     * respondem igual.</b> Distinguir diria a quem tenta adivinhar qual parte errou -
     * e um convite de animal e o que separa um estranho da carteira inteira.
     */
    /**
     * O convite antes de aceitar (Telas 19, 20 e 21).
     *
     * <b>Nao consome, e nao entrega saude.</b> Quem ainda nao aceitou nao alcanca o animal: viaja
     * daqui o nome dele, quem convidou, o que esta sendo oferecido e ate quando vale. Mandar condicao,
     * vacina ou peso entregaria o prontuario a quem tem um link — o oposto do que o convite existe
     * para proteger.
     *
     * <b>E confere o e-mail como o {@code accept} confere</b>, com a mesma resposta unica para todos
     * os motivos. Uma previa mais permissiva que o aceite transformaria a leitura num oraculo: quem
     * tivesse um link saberia o nome do animal e de quem cuida dele sem nunca poder aceitar.
     */
    @Override
    @Transactional(readOnly = true)
    public br.com.petfy.healthcare.domain.dto.PetTutorInvitePreviewResponseDTO preview(String token) {
        Person quemLe = currentPersonProvider.require();

        PetTutorInvite invite = petTutorInviteRepository.findByTokenHash(opaqueTokenService.hash(token))
                .filter(i -> i.isUsable(LocalDateTime.now()))
                .filter(i -> i.getEmail().equalsIgnoreCase(quemLe.getEmail()))
                .orElseThrow(PetTutorServiceImpl::conviteInvalido);

        Custody atual = custodyRepository.findEmCurso(invite.getAnimal().getAnimalId()).orElse(null);

        Organization abrigo = atual == null ? null : atual.getHolderOrganization();

        return br.com.petfy.healthcare.domain.dto.PetTutorInvitePreviewResponseDTO.builder()
                .animalName(invite.getAnimal().getName())
                .invitedByName(invite.getCreatedBy().getName())
                .role(invite.getRole())
                // e o que separa a Tela 20 da 21: receber de um amigo e adotar de um abrigo sao a
                // mesma mecanica e nao sao a mesma frase
                .fromOrganization(abrigo != null)
                .currentHolderName(abrigo != null
                        ? abrigo.getName()
                        : (atual == null || atual.getHolderPerson() == null
                                ? null : atual.getHolderPerson().getName()))
                .expiresAt(invite.getExpiresAt())
                .build();
    }

    /**
     * Quem recebeu diz nao.
     *
     * <b>Consome o convite e avisa quem convidou</b> — "se recusar, Marcelo e avisado e nada muda para
     * o Code". O aviso e metade da promessa: sem ele, quem convidou ficaria esperando indefinidamente
     * uma resposta que ja veio.
     *
     * <b>Grava em coluna propria, e nao no {@code revokedAt}</b>: revogar e o que quem convidou faz, e
     * a lista dele diria "voce revogou" sobre uma decisao que nao foi dele.
     */
    @Override
    @Transactional
    public void reject(String token) {
        Person quemRecusa = currentPersonProvider.require();

        PetTutorInvite invite = petTutorInviteRepository.findByTokenHash(opaqueTokenService.hash(token))
                .filter(i -> i.isUsable(LocalDateTime.now()))
                .filter(i -> i.getEmail().equalsIgnoreCase(quemRecusa.getEmail()))
                .orElseThrow(PetTutorServiceImpl::conviteInvalido);

        invite.setRejectedAt(LocalDateTime.now());
        petTutorInviteRepository.save(invite);

        petTutorActivityNotifier.conviteRecusado(invite.getAnimal(), invite.getCreatedBy(),
                quemRecusa, invite.transfereTitularidade());
    }

    @Override
    @Transactional
    public PetTutorResponseDTO accept(String token) {
        Person aceitante = currentPersonProvider.require();

        PetTutorInvite invite = petTutorInviteRepository.findByTokenHash(opaqueTokenService.hash(token))
                .filter(i -> i.isUsable(LocalDateTime.now()))
                .filter(i -> i.getEmail().equalsIgnoreCase(aceitante.getEmail()))
                .orElseThrow(PetTutorServiceImpl::conviteInvalido);

        Animal animal = invite.getAnimal();
        UUID animalId = animal.getAnimalId();

        if (alcanca(animalId, aceitante.getPersonId())) {
            throw jaETutor();
        }

        PetTutorResponseDTO resposta;
        Person titularAnterior = null;

        if (invite.transfereTitularidade()) {
            Custody atual = custodiaEmCurso(animalId);
            titularAnterior = atual.getHolderPerson();
            Organization abrigoAnterior = atual.getHolderOrganization();

            /*
             * ADOCAO E TRANSFERENCIA SAO A MESMA MECANICA COM DOIS NOMES, e o nome importa.
             *
             * Quem respondia era uma organizacao, o `CustodyEndReason.ADOCAO` existe desde o
             * P2 e nunca foi usado — o enum ate documenta este caso: "o abrigo cadastra o
             * adotante, e e isso que encerra a custodia dele". Gravar TRANSFERENCIA aqui
             * apagaria da historia do animal a unica palavra que conta o que aconteceu com
             * ele.
             */
            boolean ehAdocao = abrigoAnterior != null;

            Custody nova = transferir(atual, aceitante,
                    ehAdocao ? CustodyEndReason.ADOCAO : CustodyEndReason.TRANSFERENCIA);

            revogarAcessosHerdados(animal);

            // quem respondia ate ontem continua enxergando a carteira, agora por concessao de
            // LEITURA - decidir sobre o animal e de quem responde por ele
            if (titularAnterior != null) {
                conceder(animal, titularAnterior, GrantLevel.VIEWER, aceitante);
            }

            /*
             * O ABRIGO FICA COM LEITURA, e nao com EDITOR como uma pessoa ficaria.
             *
             * A Tela 13 escreve a regra: "a partir do aceite, o Abrigo Lar dos Focinhos passa
             * a LER o que registrou, e nao decide mais nada sobre o Teco". Nao e delicadeza —
             * onze anos de resgate, castracao e cirurgias continuam sendo trabalho do abrigo, e
             * ele tem direito de ver o que produziu. Mandar no animal, nao.
             */
            if (abrigoAnterior != null) {
                concederA(animal, abrigoAnterior, aceitante);
            }

            resposta = toResponse(nova);
        } else {
            Grant concessao = conceder(animal, aceitante, nivelDo(invite.getRole()), invite.getCreatedBy());
            resposta = toResponse(concessao);
        }

        invite.setAcceptedAt(LocalDateTime.now());
        invite.setAcceptedBy(aceitante);
        petTutorInviteRepository.save(invite);

        // Um aviso so, e nao dois: aceitar convite de HOLDER e entrar no animal e
        // passar a responder por ele ao mesmo tempo, e a segunda e a informacao que
        // importa - ela ja diz que ha gente nova cuidando do animal.
        if (titularAnterior != null) {
            petTutorActivityNotifier.titularidadeMudou(animal, quemAlcanca(animalId),
                    titularAnterior, aceitante, aceitante);
        } else {
            petTutorActivityNotifier.tutorEntrou(animal, quemAlcanca(animalId),
                    aceitante, invite.getRole());
        }

        return resposta;
    }

    /**
     * Quem alcanca o animal ve a lista. Saber quem chega ao historico de saude do seu
     * animal e parte da privacidade, e nao o contrario - inclusive para quem so
     * acompanha.
     */
    @Override
    @Transactional(readOnly = true)
    public List<PetTutorResponseDTO> listTutors(UUID animalId) {
        animalAccessGuard.requireLeitura(animalId);

        List<PetTutorResponseDTO> lista = new ArrayList<>();

        // quem responde vem primeiro, sempre: era o que a ordenacao por papel fazia
        custodyRepository.findEmCurso(animalId)
                .filter(c -> c.getHolderPerson() != null)
                .ifPresent(c -> lista.add(toResponse(c)));

        grantRepository.findVigentesDePessoasNoAnimal(animalId, LocalDateTime.now())
                .forEach(g -> lista.add(toResponse(g)));

        return lista;
    }

    /** Convite pendente e informacao de gestao: so quem responde pelo animal. */
    @Override
    @Transactional(readOnly = true)
    public List<PetTutorInviteResponseDTO> listInvites(UUID animalId) {
        animalAccessGuard.requireCustodia(animalId);

        return petTutorInviteRepository.findByAnimalAnimalIdOrderByCreationDateDesc(animalId)
                .stream()
                .map(invite -> toResponse(invite, null))
                .toList();
    }

    @Override
    @Transactional
    public void revokeInvite(UUID animalId, UUID petTutorInviteId) {
        animalAccessGuard.requireCustodia(animalId);

        PetTutorInvite invite = petTutorInviteRepository.findById(petTutorInviteId)
                .filter(i -> i.getAnimal().getAnimalId().equals(animalId))
                .orElseThrow(PetTutorServiceImpl::conviteInvalido);

        // revogar duas vezes nao e erro, mas a primeira data e que vale
        if (invite.getRevokedAt() == null) {
            invite.setRevokedAt(LocalDateTime.now());
            petTutorInviteRepository.save(invite);
        }
    }

    /**
     * Duas saidas pela mesma porta, com regras diferentes:
     *
     * <ul>
     *   <li><b>Quem responde pelo animal revoga uma concessao</b> - tirar acesso e
     *       decisao de quem responde.</li>
     *   <li><b>Quem tem acesso revoga o proprio</b> - quem nao quer mais acompanhar
     *       sai sozinho. Exigir autorizacao para sair prenderia a pessoa a
     *       notificacoes de um animal que nao e dela.</li>
     * </ul>
     *
     * Quem responde pelo animal nao sai por aqui, e agora por uma razao mais forte
     * que o indice do banco: sair sem sucessor deixaria o animal sem ninguem que
     * responda por ele, que e o que o quarto invariante proibe. Transfere primeiro,
     * ou encerra a custodia com motivo.
     */
    @Override
    @Transactional
    public void removeTutor(UUID animalId, UUID personId) {
        Person quemAgiu = currentPersonProvider.require();
        boolean saindoSozinho = quemAgiu.getPersonId().equals(personId);

        Animal animal = saindoSozinho
                // provar que alcanca basta: sair nao exige nivel nenhum
                ? animalAccessGuard.requireLeitura(animalId)
                : animalAccessGuard.requireCustodia(animalId);

        if (custodyRepository.findEmCursoDaPessoa(animalId, personId).isPresent()) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.CANNOT_REMOVE_HOLDER.getMessage(),
                    ErrorMessageEnum.CANNOT_REMOVE_HOLDER.getCode(),
                    HttpStatus.CONFLICT);
        }

        Grant concessao = buscarConcessao(animalId, personId);
        Person queSaiu = concessao.getGranteePerson();

        concessao.setRevokedAt(LocalDateTime.now());
        grantRepository.save(concessao);
        // o flush e o que faz quemAlcanca devolver a lista sem quem saiu: sem ele a
        // revogacao fica pendente e o aviso iria tambem para quem acabou de perder o
        // acesso, contando-lhe que ele mesmo saiu
        grantRepository.flush();

        petTutorActivityNotifier.tutorSaiu(animal, quemAlcanca(animalId), queSaiu, quemAgiu);
    }

    /**
     * Troca entre EDITOR e VIEWER. HOLDER nao entra: passar a responder pelo animal
     * encerra a custodia de quem responde hoje, entao nao e mudar o nivel de um
     * acesso - e a transferencia, que tem endpoint proprio.
     */
    @Override
    @Transactional
    public PetTutorResponseDTO changeRole(UUID animalId, UUID personId, PetTutorRoleUpdateRequestDTO request) {
        animalAccessGuard.requireCustodia(animalId);

        if (request.getRole() == PetTutorRole.HOLDER) {
            throw transferenciaExigida();
        }

        // quem responde pelo animal nao tem nivel a trocar: ele nao alcanca por
        // concessao. A resposta certa nao e 404 - e apontar a porta
        if (custodyRepository.findEmCursoDaPessoa(animalId, personId).isPresent()) {
            throw transferenciaExigida();
        }

        Grant concessao = buscarConcessao(animalId, personId);
        concessao.setLevel(nivelDo(request.getRole()));

        return toResponse(grantRepository.save(concessao));
    }

    /**
     * Adocao, venda, separacao: o animal troca de responsavel sem trocar de
     * historico.
     *
     * Quem recebe precisa <b>ja alcancar o animal</b>. Transferir para quem esta fora
     * dele e o convite com papel HOLDER - la a pessoa aceita, e aqui nao haveria como
     * pedir consentimento de quem passaria a responder pelo animal.
     *
     * <b>Acessos nao sao herdados:</b> toda concessao vigente do animal cai no ato. Quem respondia
     * continua enxergando a carteira, agora por concessao de LEITURA — decidir sobre o animal e de
     * quem responde por ele, e quem passou a responder decide se revoga ate isso.
     */
    @Override
    @Transactional
    public PetTutorResponseDTO transferHolder(UUID animalId, UUID toPersonId) {
        Animal animal = animalAccessGuard.requireCustodia(animalId);
        Person quemAgiu = currentPersonProvider.require();

        // transferir para quem ja responde e no-op, e nao erro: o estado final pedido
        // e o estado atual. Nao avisa ninguem, porque nada mudou
        var jaResponde = custodyRepository.findEmCursoDaPessoa(animalId, toPersonId);
        if (jaResponde.isPresent()) {
            return toResponse(jaResponde.get());
        }

        Grant concessaoDoDestino = buscarConcessao(animalId, toPersonId);
        Person destino = concessaoDoDestino.getGranteePerson();

        Custody atual = custodiaEmCurso(animalId);
        Person titularAnterior = atual.getHolderPerson();

        Custody nova = transferir(atual, destino, CustodyEndReason.TRANSFERENCIA);

        // quem passa a responder nao precisa mais de concessao, e manter as duas
        // deixaria a mesma pessoa alcancando o animal por dois caminhos - o dia em que
        // divergissem seria um vazamento
        concessaoDoDestino.setRevokedAt(LocalDateTime.now());
        grantRepository.save(concessaoDoDestino);

        revogarAcessosHerdados(animal);

        if (titularAnterior != null) {
            conceder(animal, titularAnterior, GrantLevel.VIEWER, destino);
        }

        grantRepository.flush();

        petTutorActivityNotifier.titularidadeMudou(animal, quemAlcanca(animalId),
                titularAnterior, destino, quemAgiu);

        return toResponse(nova);
    }

    /**
     * Encerra a custodia atual e abre a do sucessor, nesta ordem.
     *
     * <b>A ordem e obrigatoria e as duas escritas do meio precisam de flush.</b> O
     * indice unico parcial admite no maximo uma custodia em curso por animal: abrir a
     * nova antes de fechar a antiga deixa duas, e o Postgres recusa o insert. Foi
     * assim que a troca de titularidade quebrou no 8b, com outra tabela e o mesmo
     * tipo de indice - e mock nao tem indice, entao isso nao aparece em teste de
     * servico.
     *
     * O sucessor e apontado depois de a nova existir, porque a chave estrangeira
     * exige a linha gravada. E por isso que o invariante nao virou CHECK: ele
     * precisaria valer no meio desta sequencia, onde nao pode valer. Ver a V24.
     */
    private Custody transferir(Custody atual, Person novoResponsavel, CustodyEndReason motivo) {
        atual.setEndedAt(LocalDateTime.now());
        atual.setEndReason(motivo);
        custodyRepository.saveAndFlush(atual);

        Custody nova = custodyRepository.saveAndFlush(Custody.builder()
                .animal(atual.getAnimal())
                .holderPerson(novoResponsavel)
                .nature(CustodyNature.DEFINITIVA)
                .startedAt(LocalDateTime.now())
                .build());

        atual.setSuccessor(nova);
        custodyRepository.save(atual);

        return nova;
    }

    /**
     * <b>ACESSOS NAO SAO HERDADOS.</b> No aceite da transferencia, toda concessao vigente do
     * animal cai — co-tutora, clinica, creche, abrigo e link compartilhado.
     *
     * A tese e do desenho da Tela 11, e ela e sobre consentimento: quem autorizou a clinica a ver
     * o prontuario foi o titular ANTERIOR, e quem passa a responder pelo animal nao herda as
     * decisoes dele. Manter os acessos faria o produto tratar autorizacao como propriedade do
     * animal, quando ela e de quem responde por ele.
     *
     * <b>O link compartilhado cai junto, e ele e o mais importante da lista</b>: uma URL que
     * continua abrindo a carteira e um acesso que ninguem ve na tela de quem agora responde.
     *
     * O que o titular anterior e o abrigo recebem DEPOIS disto e concessao nova, de leitura, e
     * nao sobra da anterior — por isso a revogacao vem primeiro.
     */
    private void revogarAcessosHerdados(Animal animal) {
        LocalDateTime agora = LocalDateTime.now();

        List<Grant> vigentes = grantRepository
                .findTodasVigentesNoAnimal(animal.getAnimalId(), agora);

        vigentes.forEach(concessao -> concessao.setRevokedAt(agora));

        grantRepository.saveAll(vigentes);
        grantRepository.flush();
    }

    /**
     * A concessao que sobra para o abrigo depois da adocao: <b>ler, e nada alem</b>.
     *
     * `VIEWER` e o nivel, e o escopo e o clinico sem `CONTATO`: o abrigo continua vendo a vida
     * que ajudou a registrar, e o telefone de quem adotou nao e parte disso. Quem adotou pode
     * revogar quando quiser, como qualquer concessao.
     */
    private Grant concederA(Animal animal, Organization abrigo, Person concedente) {
        return grantRepository.save(Grant.builder()
                .animal(animal)
                .granteeOrganization(abrigo)
                .level(GrantLevel.VIEWER)
                .scopes(new LinkedHashSet<>(Set.of(
                        GrantScope.CARTEIRA, GrantScope.CONDICOES, GrantScope.PRONTUARIO,
                        GrantScope.PESO, GrantScope.ANEXOS)))
                .grantedBy(concedente)
                .grantedAt(LocalDateTime.now())
                .build());
    }

    private Grant conceder(Animal animal, Person beneficiario, GrantLevel nivel, Person concedente) {
        return grantRepository.save(Grant.builder()
                .animal(animal)
                .granteePerson(beneficiario)
                .level(nivel)
                .scopes(new LinkedHashSet<>(ESCOPO_DE_PESSOA))
                .grantedBy(concedente)
                .grantedAt(LocalDateTime.now())
                .build());
    }

    /**
     * Quem alcanca o animal agora, por consulta e nao pela colecao de {@code Animal}.
     *
     * A colecao e lazy e acabou de ser mexida - custodia encerrada, concessao criada
     * ou revogada -, entao ler dela daria uma lista que pode nao refletir o que foi
     * gravado. O aviso iria para o conjunto errado de pessoas, que e o unico jeito de
     * este recurso piorar a privacidade em vez de melhorar.
     */
    private List<Person> quemAlcanca(UUID animalId) {
        List<Person> pessoas = new ArrayList<>();

        custodyRepository.findEmCurso(animalId)
                .map(Custody::getHolderPerson)
                .ifPresent(pessoas::add);

        grantRepository.findVigentesDePessoasNoAnimal(animalId, LocalDateTime.now())
                .forEach(g -> pessoas.add(g.getGranteePerson()));

        return pessoas;
    }

    private boolean alcanca(UUID animalId, UUID personId) {
        return custodyRepository.findEmCursoDaPessoa(animalId, personId).isPresent()
                || grantRepository.findVigenteDaPessoaNoAnimal(animalId, personId, LocalDateTime.now()).isPresent();
    }

    /**
     * Convidar quem ja alcanca o animal nao e engano de digitacao a ser silenciado: a
     * pessoa levaria o erro no aceite, no lugar de quem convidou.
     */
    private void recusarSeJaAlcanca(UUID animalId, String email) {
        personRepository.findByEmail(email.trim())
                .filter(person -> alcanca(animalId, person.getPersonId()))
                .ifPresent(person -> {
                    throw jaETutor();
                });
    }

    private Custody custodiaEmCurso(UUID animalId) {
        return custodyRepository.findEmCurso(animalId)
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.ANIMAL_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.ANIMAL_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    private Grant buscarConcessao(UUID animalId, UUID personId) {
        return grantRepository.findVigenteDaPessoaNoAnimal(animalId, personId, LocalDateTime.now())
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.TUTOR_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.TUTOR_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    /** O papel do convite diz a que a pessoa esta sendo convidada, e nao o que ela e. */
    private static GrantLevel nivelDo(PetTutorRole role) {
        return role == PetTutorRole.VIEWER ? GrantLevel.VIEWER : GrantLevel.EDITOR;
    }

    private static PetfyHealthcareException conviteInvalido() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.PET_TUTOR_INVITE_NOT_FOUND.getMessage(),
                ErrorMessageEnum.PET_TUTOR_INVITE_NOT_FOUND.getCode(),
                HttpStatus.NOT_FOUND);
    }

    private static PetfyHealthcareException jaETutor() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.ALREADY_A_TUTOR.getMessage(),
                ErrorMessageEnum.ALREADY_A_TUTOR.getCode(),
                HttpStatus.CONFLICT);
    }

    private static PetfyHealthcareException transferenciaExigida() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.TRANSFER_REQUIRED_FOR_HOLDER.getMessage(),
                ErrorMessageEnum.TRANSFER_REQUIRED_FOR_HOLDER.getCode(),
                HttpStatus.CONFLICT);
    }

    private PetTutorResponseDTO toResponse(Custody custodia) {
        return PetTutorResponseDTO.builder()
                .vinculoId(custodia.getCustodyId())
                .animalId(custodia.getAnimal().getAnimalId())
                .personId(custodia.getHolderPerson().getPersonId())
                .personName(custodia.getHolderPerson().getName())
                .personEmail(custodia.getHolderPerson().getEmail())
                .relacao("CUSTODIA")
                .holder(true)
                .creationDate(custodia.getStartedAt())
                .build();
    }

    private PetTutorResponseDTO toResponse(Grant concessao) {
        return PetTutorResponseDTO.builder()
                .vinculoId(concessao.getGrantId())
                .animalId(concessao.getAnimal().getAnimalId())
                .personId(concessao.getGranteePerson().getPersonId())
                .personName(concessao.getGranteePerson().getName())
                .personEmail(concessao.getGranteePerson().getEmail())
                .relacao(concessao.getLevel().name())
                .holder(false)
                .invitedByPersonName(concessao.getGrantedBy() != null
                        ? concessao.getGrantedBy().getName()
                        : null)
                .creationDate(concessao.getGrantedAt())
                .build();
    }

    private PetTutorInviteResponseDTO toResponse(PetTutorInvite invite, String token) {
        return PetTutorInviteResponseDTO.builder()
                .petTutorInviteId(invite.getPetTutorInviteId())
                .animalId(invite.getAnimal().getAnimalId())
                .animalName(invite.getAnimal().getName())
                .token(token)
                .email(invite.getEmail())
                .role(invite.getRole())
                .createdByPersonName(invite.getCreatedBy().getName())
                .expiresAt(invite.getExpiresAt())
                .acceptedAt(invite.getAcceptedAt())
                .revokedAt(invite.getRevokedAt())
                .usable(invite.isUsable(LocalDateTime.now()))
                .transfersHolder(invite.transfereTitularidade())
                .build();
    }

}
