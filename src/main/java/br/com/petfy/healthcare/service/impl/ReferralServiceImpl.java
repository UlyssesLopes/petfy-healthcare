package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.ReferralCandidateDTO;
import br.com.petfy.healthcare.domain.dto.ReferralOptionsDTO;
import br.com.petfy.healthcare.domain.dto.ReferralRequestDTO;
import br.com.petfy.healthcare.domain.dto.ReferralResponseDTO;
import br.com.petfy.healthcare.domain.dto.ReferralScopeOptionDTO;
import br.com.petfy.healthcare.domain.entity.*;
import br.com.petfy.healthcare.domain.repository.*;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.notification.ReferralNotifier;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.ReferralService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Encaminhar o caso ao especialista, e o tutor decidir (Tela 45).
 *
 * <b>"Encaminhar e voce indicando o caminho; conceder acesso continua sendo dele, como sempre
 * foi."</b> E a frase que governa este servico inteiro, e ela se traduz em duas guardas diferentes:
 * quem encaminha precisa de {@code requireEscrita} — e o profissional que atende o animal —, e quem
 * autoriza precisa de {@code requireCustodia}, que nenhum nivel de concessao alcanca.
 *
 * <b>O aceite cria um {@link Grant} comum</b>, com {@code grantedBy} sendo o tutor e prazo. Nao ha
 * caminho de acesso novo: o {@link AnimalAccessGuard} nao aprendeu nada sobre encaminhamento, e nem
 * precisa. Isso e deliberado — um segundo caminho de alcance seria consultado em toda leitura para
 * sempre, e a Tela 33 ja registrou por que uma linha de concessao "concedida por ninguem" e pior que
 * uma excecao na guarda. Aqui ha quem concedeu e quem revoga, com nome e data.
 */
@Service
@RequiredArgsConstructor
public class ReferralServiceImpl implements ReferralService {

    /** Os 90 dias que a tela promete, quando quem encaminha nao diz outro numero. */
    private static final int DIAS_PADRAO = 90;

    /**
     * As caixas que vem marcadas: o recorte clinico de um encaminhamento.
     *
     * <b>Sao as tres que o desenho mostra marcadas</b> — o diagnostico e o raio-X ({@code PRONTUARIO}
     * e {@code ANEXOS}), o historico de peso, e as observacoes da creche. Carteira, condicoes e
     * contato ficam de fora da sugestao, e nao da lista: sao uteis e nao sao o caso.
     */
    private static final Set<GrantScope> SUGERIDOS = Set.of(
            GrantScope.PRONTUARIO, GrantScope.ANEXOS, GrantScope.PESO, GrantScope.OBSERVACOES);

    /** Quantos profissionais a busca devolve. Vinte cabe numa lista e nao serve varredura. */
    private static final int LIMITE_DA_BUSCA = 20;

    /**
     * Quantas letras a busca exige.
     *
     * <b>Uma letra devolveria meio cadastro de profissionais</b>, e e isso que separa "buscar" de
     * "listar todo mundo". Nao e a mesma trava do microchip da Tela 34, onde a busca parcial nao
     * existe: la a parcial nao servia a ninguem, aqui ela e o gesto — digita-se "orto" para achar o
     * ortopedista.
     */
    private static final int MINIMO_DA_BUSCA = 3;

    private final AnimalAccessGuard animalAccessGuard;
    private final CurrentPersonProvider currentPersonProvider;
    private final CurrentProfessionalProvider currentProfessionalProvider;
    private final ReferralRepository referralRepository;
    private final PersonRepository personRepository;
    private final ProfessionalCredentialRepository credentialRepository;
    private final MembershipRepository membershipRepository;
    private final CustodyRepository custodyRepository;
    private final GrantRepository grantRepository;
    private final TimelineRepository timelineRepository;
    private final ReferralNotifier referralNotifier;

    /**
     * O que pode ir junto, com o numero de cada caixa.
     *
     * <b>Uma consulta agregada, e nao uma por escopo.</b> A contagem sai da linha do tempo e usa o
     * mapeamento do {@link TimelineEventType} — o mesmo que a guarda aplica ao mascarar —, entao o
     * numero que a tela promete e o que o especialista abre depois vem da mesma fonte.
     */
    @Override
    @Transactional(readOnly = true)
    public ReferralOptionsDTO opcoes(UUID animalId) {
        Animal animal = animalAccessGuard.requireEscrita(animalId);

        Map<GrantScope, Long> eventos = new EnumMap<>(GrantScope.class);
        Map<GrantScope, LocalDateTime> desde = new EnumMap<>(GrantScope.class);

        for (TimelineRepository.PorTipo linha : timelineRepository.contagemPorTipo(animalId)) {
            GrantScope escopo = linha.getTipo().escopoExigido();

            eventos.merge(escopo, linha.getEventos(), Long::sum);

            // dois tipos caem no mesmo escopo (vacina e antiparasitario, na carteira), e o "desde"
            // e o mais antigo dos dois
            if (linha.getPrimeiro() != null) {
                desde.merge(escopo, linha.getPrimeiro(),
                        (atual, novo) -> novo.isBefore(atual) ? novo : atual);
            }
        }

        TimelineRepository.Tamanho tamanho = timelineRepository.tamanhoDe(animalId);

        List<ReferralScopeOptionDTO> caixas = Arrays.stream(GrantScope.values())
                .map(escopo -> ReferralScopeOptionDTO.builder()
                        .scope(escopo)
                        .events(eventos.getOrDefault(escopo, 0L))
                        .since(desde.get(escopo))
                        .suggested(SUGERIDOS.contains(escopo))
                        .personalData(escopo == GrantScope.CONTATO)
                        .build())
                .collect(Collectors.toList());

        return ReferralOptionsDTO.builder()
                .animalId(animalId)
                .animalName(animal.getName())
                .totalEvents(tamanho.getEventos())
                .firstEventAt(tamanho.getPrimeiro())
                .defaultAccessDays(DIAS_PADRAO)
                .scopes(caixas)
                .build();
    }

    /**
     * "Buscar outro profissional."
     *
     * <b>A guarda e a do ANIMAL, e e ela que impede a varredura.</b> Uma rota de gente que devolve
     * nome por busca parcial precisaria de limite por IP, como o {@code POST /found} do bloco 5 —
     * aqui quem varre precisa antes ter escrita em um animal, e a trava passa a ser a mesma que
     * protege todo o resto.
     *
     * <b>Quem esta lendo nao aparece na propria busca.</b> Nao e cosmetica: encaminhar para si mesmo
     * e recusado no {@code encaminhar}, e oferecer o proprio nome na lista faria a tela apresentar uma
     * opcao que o servidor rejeita.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ReferralCandidateDTO> candidatos(UUID animalId, String busca) {
        animalAccessGuard.requireEscrita(animalId);

        String termo = busca == null ? "" : busca.trim();

        if (termo.length() < MINIMO_DA_BUSCA) {
            return List.of();
        }

        UUID euMesmo = currentPersonProvider.require().getPersonId();

        List<ProfessionalCredential> encontradas = credentialRepository.buscarAtivas(
                "%" + termo.toLowerCase() + "%",
                CredentialStatus.SUSPENSO,
                PageRequest.of(0, LIMITE_DA_BUSCA));

        /*
         * UMA LINHA POR PESSOA, e nao por credencial: quem tem registro em dois estados apareceria
         * duas vezes na busca, e quem encaminha escolheria entre duas linhas com o mesmo nome sem
         * saber qual e qual. A primeira credencial vence, que e a de nome alfabetico da consulta.
         */
        Map<UUID, ProfessionalCredential> porPessoa = new LinkedHashMap<>();

        encontradas.stream()
                .filter(credencial -> !credencial.getPerson().getPersonId().equals(euMesmo))
                .forEach(credencial ->
                        porPessoa.putIfAbsent(credencial.getPerson().getPersonId(), credencial));

        if (porPessoa.isEmpty()) {
            return List.of();
        }

        List<UUID> pessoas = new ArrayList<>(porPessoa.keySet());

        Map<UUID, List<String>> organizacoes = new HashMap<>();

        membershipRepository.findAtivosDasPessoas(pessoas).forEach(vinculo ->
                organizacoes.computeIfAbsent(vinculo.getPerson().getPersonId(), k -> new ArrayList<>())
                        .add(vinculo.getOrganization().getName()));

        // "Ele ja registrou o raio-X do Code em 2023" — uma consulta agregada para a lista inteira,
        // e nao uma por resultado
        Map<UUID, LocalDateTime> contribuicoes = timelineRepository
                .ultimaContribuicaoPorPessoa(animalId).stream()
                .collect(Collectors.toMap(
                        TimelineRepository.UltimaContribuicao::getPessoaId,
                        TimelineRepository.UltimaContribuicao::getEm,
                        (a, b) -> a.isAfter(b) ? a : b));

        return porPessoa.values().stream()
                .map(credencial -> ReferralCandidateDTO.builder()
                        .personId(credencial.getPerson().getPersonId())
                        .name(credencial.getPerson().getName())
                        .specialty(credencial.getSpecialty())
                        .credential(credencial.getCouncil() + "-" + credencial.getUf() + " "
                                + credencial.getNumber())
                        .organizations(organizacoes.getOrDefault(
                                credencial.getPerson().getPersonId(), List.of()))
                        .lastContributionAt(contribuicoes.get(credencial.getPerson().getPersonId()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * O pedido, e o unico caso em que ele nasce ja decidido.
     *
     * <b>Quem RESPONDE pelo animal encaminhando produz um encaminhamento autorizado na hora</b>, com
     * a concessao criada. O tutor que e tambem veterinario, e a clinica que detem a custodia do
     * animal resgatado, sao esse caso — e pedir a eles que autorizem o proprio pedido seria teatro:
     * eles apertariam dois botoes seguidos para o mesmo efeito. O registro nao mente sobre isso, e
     * {@code decidedBy} fica sendo quem pediu, o que o CHECK de {@code group_approvals} proibia e
     * este nao proibe justamente por essa razao.
     */
    @Override
    @Transactional
    public ReferralResponseDTO encaminhar(UUID animalId, ReferralRequestDTO dto) {
        Animal animal = animalAccessGuard.requireEscrita(animalId);
        Person quemEncaminha = currentPersonProvider.require();

        if (quemEncaminha.getPersonId().equals(dto.getToPersonId())) {
            throw erro(ErrorMessageEnum.CANNOT_REFER_TO_SELF, HttpStatus.BAD_REQUEST);
        }

        Person especialista = personRepository.findById(dto.getToPersonId())
                .orElseThrow(() -> erro(ErrorMessageEnum.PERSON_NOT_FOUND, HttpStatus.NOT_FOUND));

        exigirProfissional(especialista);
        exigirQueNaoRespondePeloAnimal(animalId, especialista);

        referralRepository.findPendenteDoAnimalPara(animalId, especialista.getPersonId())
                .ifPresent(existente -> {
                    throw erro(ErrorMessageEnum.REFERRAL_ALREADY_PENDING, HttpStatus.CONFLICT);
                });

        boolean euMesmoAutorizo = animalAccessGuard.respondePor(animalId);

        if (!euMesmoAutorizo) {
            exigirQueAlguemPossaAutorizar(animalId);
        }

        LocalDateTime agora = LocalDateTime.now();

        Referral referral = Referral.builder()
                .animal(animal)
                .referredBy(quemEncaminha)
                .fromOrganization(currentProfessionalProvider
                        .organizacaoDeclarada(quemEncaminha).orElse(null))
                .toPerson(especialista)
                .reason(dto.getReason().trim())
                .scopes(new LinkedHashSet<>(dto.getScopes()))
                .accessDays(dto.getAccessDays() == null ? DIAS_PADRAO : dto.getAccessDays())
                .status(ReferralStatus.PENDENTE)
                .requestedAt(agora)
                .build();

        if (euMesmoAutorizo) {
            conceder(referral, quemEncaminha, agora);
            referral = referralRepository.save(referral);
            referralNotifier.encaminhamentoAutorizado(referral);
            return toResponse(referral, true);
        }

        referral = referralRepository.save(referral);
        referralNotifier.encaminhamentoPedido(referral);

        return toResponse(referral, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferralResponseDTO> doAnimal(UUID animalId) {
        animalAccessGuard.requireEscrita(animalId);

        boolean podeDecidir = animalAccessGuard.respondePor(animalId);

        return referralRepository.findDoAnimal(animalId).stream()
                .map(referral -> toResponse(referral, podeDecidir && referral.estaPendente()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferralResponseDTO> pendentesParaDecisao() {
        Person eu = currentPersonProvider.require();

        UUID organizacao = currentProfessionalProvider.organizacaoDeclarada(eu)
                .map(Organization::getOrganizationId)
                .orElse(null);

        return referralRepository.findPendentesParaDecisaoDe(eu.getPersonId(), organizacao).stream()
                // canDecide verdadeiro sem excecao: a consulta parte da custodia, entao tudo que ela
                // devolve e decidivel por quem perguntou
                .map(referral -> toResponse(referral, true))
                .collect(Collectors.toList());
    }

    /**
     * A caixa do especialista.
     *
     * <b>O pendente aparece SEM o nome do animal</b>, e e a decisao mais delicada desta classe. Ele
     * precisa saber que ha um caso esperando autorizacao — pode ligar para a clinica que encaminhou,
     * e um pedido parado por tres semanas e informacao. Mas o tutor nao autorizou nada ainda, e o
     * nome do animal ja e informacao sobre um animal que nao e dele.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ReferralResponseDTO> recebidos() {
        Person eu = currentPersonProvider.require();

        return referralRepository.findRecebidosPor(eu.getPersonId()).stream()
                .map(referral -> {
                    ReferralResponseDTO resposta = toResponse(referral, false);

                    if (referral.estaPendente()) {
                        resposta.setAnimalName(null);
                        resposta.setAnimalId(null);
                    }

                    return resposta;
                })
                .collect(Collectors.toList());
    }

    /**
     * O tutor autoriza, e a concessao nasce.
     *
     * <b>Autorizar e conceder na mesma transacao</b>, como o acordo de duas pessoas fez com concordar
     * e executar: um estado "autorizado, falta conceder" faria a tela dizer ao tutor que o
     * especialista alcanca o animal enquanto o especialista recebe 404.
     */
    @Override
    @Transactional
    public ReferralResponseDTO autorizar(UUID referralId) {
        Referral referral = paraDecisao(referralId);
        Person tutor = currentPersonProvider.require();

        conceder(referral, tutor, LocalDateTime.now());

        Referral salvo = referralRepository.save(referral);
        referralNotifier.encaminhamentoAutorizado(salvo);

        return toResponse(salvo, false);
    }

    @Override
    @Transactional
    public ReferralResponseDTO recusar(UUID referralId) {
        Referral referral = paraDecisao(referralId);

        referral.setStatus(ReferralStatus.RECUSADO);
        referral.setDecidedBy(currentPersonProvider.require());
        referral.setDecidedAt(LocalDateTime.now());

        Referral salvo = referralRepository.save(referral);
        referralNotifier.encaminhamentoRecusado(salvo);

        return toResponse(salvo, false);
    }

    /**
     * A concessao que o aceite produz.
     *
     * <b>Reusa a concessao vigente da pessoa, e UNE os escopos em vez de substitui-los.</b> Este e o
     * ponto em que a implementacao obvia estaria errada de um jeito silencioso e grave: se o
     * especialista ja e co-tutor com acesso permanente — o que acontece —, gravar prazo e escopos do
     * encaminhamento por cima ENCOLHERIA o acesso dele. O tutor autorizaria a ver mais e o efeito
     * seria ver menos, e ninguem descobriria antes de o co-tutor abrir a carteira e nao achar a
     * vacina.
     *
     * <b>Por isso o prazo mais generoso vence, e nulo vence de tudo:</b> um acesso sem prazo nao
     * ganha data de fim porque alguem encaminhou um caso.
     *
     * <b>E o nivel e EDITOR, e nao VIEWER.</b> "O especialista que registra ali passa a devolver o
     * retorno para a clinica que encaminhou. O registro do animal fica mais completo a cada volta."
     * Um encaminhamento so de leitura entregaria o caso e nao deixaria o especialista escrever o que
     * concluiu — e o retorno voltaria a ser telefone.
     */
    private void conceder(Referral referral, Person quemAutoriza, LocalDateTime agora) {
        UUID animalId = referral.getAnimal().getAnimalId();
        LocalDateTime fim = agora.plusDays(referral.getAccessDays());

        Grant concessao = grantRepository
                .findVigenteDaPessoaNoAnimal(animalId, referral.getToPerson().getPersonId(), agora)
                .orElseGet(() -> Grant.builder()
                        .animal(referral.getAnimal())
                        .granteePerson(referral.getToPerson())
                        .grantedAt(agora)
                        .build());

        Set<GrantScope> escopos = new LinkedHashSet<>(concessao.getScopes());
        escopos.addAll(referral.getScopes());

        concessao.setScopes(escopos);
        concessao.setLevel(GrantLevel.EDITOR);
        concessao.setGrantedBy(quemAutoriza);
        concessao.setRevokedAt(null);

        if (concessao.getGrantId() == null || venceAntes(concessao.getExpiresAt(), fim)) {
            concessao.setExpiresAt(fim);
        }

        referral.setStatus(ReferralStatus.AUTORIZADO);
        referral.setDecidedBy(quemAutoriza);
        referral.setDecidedAt(agora);
        referral.setGrantAcesso(grantRepository.save(concessao));
    }

    /** Nulo nao vence antes de nada: e o acesso sem prazo, e ele e o mais generoso possivel. */
    private boolean venceAntes(LocalDateTime atual, LocalDateTime candidato) {
        return atual != null && atual.isBefore(candidato);
    }

    /**
     * O encaminhamento que esta pessoa pode decidir.
     *
     * <b>A guarda e {@code requireCustodia}</b>, e nenhum nivel de concessao chega la — "conceder
     * acesso continua sendo dele". Sem isso, a clinica com acesso de escrita autorizaria o proprio
     * encaminhamento, o que e o mesmo que dar a ela o poder de conceder acesso a terceiros: o cenario
     * que o {@code requireCustodia} existe para impedir desde o P2b.
     */
    private Referral paraDecisao(UUID referralId) {
        Referral referral = referralRepository.findById(referralId)
                .orElseThrow(() -> erro(ErrorMessageEnum.REFERRAL_NOT_FOUND, HttpStatus.NOT_FOUND));

        animalAccessGuard.requireCustodia(referral.getAnimal().getAnimalId());

        if (!referral.estaPendente()) {
            throw erro(ErrorMessageEnum.REFERRAL_ALREADY_DECIDED, HttpStatus.CONFLICT);
        }

        return referral;
    }

    /**
     * So se encaminha a quem pode praticar ato clinico.
     *
     * <b>A tela e "entre profissionais", e o que o aceite produz e acesso de ESCRITA.</b> Sem esta
     * recusa, encaminhar viraria o caminho mais curto para dar acesso a qualquer pessoa sem passar
     * pela tela de conceder — e com o tutor autorizando na crenca de que aquilo era um especialista.
     */
    private void exigirProfissional(Person especialista) {
        if (!credentialRepository.existsAtivaPorEmail(
                especialista.getEmail(), CredentialStatus.SUSPENSO)) {
            throw erro(ErrorMessageEnum.NOT_A_PROFESSIONAL, HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    /**
     * Encaminhar a quem ja responde pelo animal nao tem efeito nenhum.
     *
     * Quem responde alcanca tudo sem concessao, entao o aceite criaria uma linha que nao muda nada.
     * Aceitar em silencio faria a clinica esperar uma autorizacao de alguem que ja podia abrir o caso
     * desde sempre.
     */
    private void exigirQueNaoRespondePeloAnimal(UUID animalId, Person especialista) {
        if (custodyRepository.findEmCursoDaPessoa(animalId, especialista.getPersonId()).isPresent()) {
            throw erro(ErrorMessageEnum.CANNOT_REFER_TO_HOLDER, HttpStatus.CONFLICT);
        }
    }

    /**
     * Ha quem autorize?
     *
     * <b>Recusa antes de gravar, e nao depois.</b> Um animal sem custodia em curso — perdido, ou com
     * a linha do tempo encerrada pela Tela 33 — deixaria o pedido parado para sempre: a tela diria
     * "o Marcelo recebe e decide" sobre um Marcelo que nao existe mais, e a Ana esperaria uma
     * resposta que nunca vem.
     */
    private void exigirQueAlguemPossaAutorizar(UUID animalId) {
        if (custodyRepository.findEmCurso(animalId).isEmpty()) {
            throw erro(ErrorMessageEnum.NO_ONE_CAN_AUTHORIZE, HttpStatus.CONFLICT);
        }
    }

    /**
     * <b>Copia dos escopos, e nao a colecao do Hibernate.</b> O {@code scopes} e uma
     * {@code @ElementCollection} preguicosa: entregar a referencia faz o DTO carregar um proxy que so
     * e tocado quando o Jackson serializa — ja fora da transacao. E o sintoma engana, porque a
     * leitura funciona e a falha aparece como {@code HttpMessageNotWritableException} na escrita da
     * resposta. Era o sexto caso do mesmo defeito no projeto.
     */
    private ReferralResponseDTO toResponse(Referral referral, boolean canDecide) {
        return ReferralResponseDTO.builder()
                .referralId(referral.getReferralId())
                .animalId(referral.getAnimal().getAnimalId())
                .animalName(referral.getAnimal().getName())
                .status(referral.getStatus())
                .reason(referral.getReason())
                .scopes(new LinkedHashSet<>(referral.getScopes()))
                .accessDays(referral.getAccessDays())
                .referredByPersonId(referral.getReferredBy().getPersonId())
                .referredByName(referral.getReferredBy().getName())
                .fromOrganizationName(referral.getFromOrganization() == null
                        ? null : referral.getFromOrganization().getName())
                .toPersonId(referral.getToPerson().getPersonId())
                .toPersonName(referral.getToPerson().getName())
                .toPersonSpecialty(especialidadeDe(referral.getToPerson()))
                .requestedAt(referral.getRequestedAt())
                .decidedByName(referral.getDecidedBy() == null
                        ? null : referral.getDecidedBy().getName())
                .decidedAt(referral.getDecidedAt())
                .accessExpiresAt(referral.getGrantAcesso() == null
                        ? null : referral.getGrantAcesso().getExpiresAt())
                .canDecide(canDecide)
                .build();
    }

    /**
     * A especialidade declarada, se houver.
     *
     * <b>A primeira que existir vence</b>, e nao ha ordem prometida: quem tem registro em dois
     * conselhos e declarou coisas diferentes em cada um e um caso raro o suficiente para nao valer
     * uma regra de desempate — e qualquer escolha aqui seria arbitraria do mesmo jeito.
     */
    private String especialidadeDe(Person pessoa) {
        return credentialRepository.findByPersonPersonId(pessoa.getPersonId()).stream()
                .map(ProfessionalCredential::getSpecialty)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private PetfyHealthcareException erro(ErrorMessageEnum mensagem, HttpStatus status) {
        return new PetfyHealthcareException(mensagem.getMessage(), mensagem.getCode(), status);
    }

}
