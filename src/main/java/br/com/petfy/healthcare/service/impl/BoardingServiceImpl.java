package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.BoardingRequestDTO;
import br.com.petfy.healthcare.domain.dto.BoardingResponseDTO;
import br.com.petfy.healthcare.domain.entity.*;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.GrantRepository;
import br.com.petfy.healthcare.domain.repository.OrganizationRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.BoardingService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * O animal fora de casa por uma semana (Tela 47).
 *
 * <b>NAO HA MODELO NOVO AQUI, e essa e a noticia.</b> <i>"Hospedagem e custodia temporaria. E a mesma
 * mecanica do lar transitorio do abrigo, aplicada a quem viaja. Por isso o produto ja sabia fazer
 * isto — a arquitetura de custodia que passa de mao em mao foi desenhada na primeira tela."</i> Uma
 * estadia e uma {@link Custody} com {@code nature = TRANSITORIA}, {@code holderOrganization} e
 * {@code expectedEndAt} — os tres campos existem desde o P2 e nunca tiveram caminho HTTP, como o
 * {@code holderOrganization} antes da Tela 13.
 *
 * <b>O QUE ESTE SERVICO PRECISOU RESOLVER E OUTRA COISA, e ela nao aparece no desenho:</b> a custodia
 * e unica, entao quando ela passa para a creche o tutor <i>deixa de responder pelo animal</i> — e o
 * {@code AnimalAccessGuard} passaria a responder 404 para ele. O tutor perderia o proprio cachorro de
 * vista exatamente na semana em que a tela existe para ele acompanhar.
 *
 * A saida e uma concessao comum, criada na entrega e revogada na volta, com {@code grantedBy} sendo
 * <b>ele mesmo</b> — e essa e a diferenca em relacao ao que a Tela 33 recusou: la nao havia quem
 * concedesse nem quem revogasse, e aqui ha os dois, com nome e data. Ela aparece na lista de acessos
 * do animal dizendo a verdade: durante a estadia, quem entregou le por concessao, e nao por responder.
 */
@Service
@RequiredArgsConstructor
public class BoardingServiceImpl implements BoardingService {

    /** O que quem entrega continua alcancando enquanto o animal esta fora. Tudo, porque era tudo. */
    private static final Set<GrantScope> ESCOPO_DE_QUEM_ENTREGOU = Set.of(
            GrantScope.CARTEIRA, GrantScope.CONDICOES, GrantScope.PRONTUARIO,
            GrantScope.OBSERVACOES, GrantScope.PESO, GrantScope.ANEXOS, GrantScope.CONTATO);

    private final AnimalAccessGuard animalAccessGuard;
    private final CurrentPersonProvider currentPersonProvider;
    private final CurrentProfessionalProvider currentProfessionalProvider;
    private final CustodyRepository custodyRepository;
    private final OrganizationRepository organizationRepository;
    private final GrantRepository grantRepository;

    /**
     * A entrega.
     *
     * <b>Quem entrega e quem RESPONDE, e nao quem tem escrita:</b> passar a custodia adiante e o
     * mesmo ato que transferir titularidade, e nenhum nivel de concessao chega la. A creche que ja
     * tem acesso ao animal nao pode se auto-hospedar.
     */
    @Override
    @Transactional
    public BoardingResponseDTO hospedar(UUID animalId, BoardingRequestDTO dto) {
        Animal animal = animalAccessGuard.requireCustodia(animalId);

        Custody atual = custodyRepository.findEmCurso(animalId)
                .orElseThrow(() -> erro(ErrorMessageEnum.NO_ONE_CAN_AUTHORIZE, HttpStatus.CONFLICT));

        if (atual.getHolderOrganization() != null
                && atual.getNature() == CustodyNature.TRANSITORIA) {
            throw erro(ErrorMessageEnum.ALREADY_BOARDED, HttpStatus.CONFLICT);
        }

        if (!dto.getExpectedReturnOn().isAfter(LocalDate.now())) {
            throw erro(ErrorMessageEnum.INVALID_RETURN_DATE, HttpStatus.BAD_REQUEST);
        }

        Organization creche = organizationRepository.findById(dto.getOrganizationId())
                .orElseThrow(() -> erro(ErrorMessageEnum.CLINIC_NOT_FOUND, HttpStatus.NOT_FOUND));

        exigirQuePossaDeterCustodia(creche);

        LocalDateTime agora = LocalDateTime.now();

        /*
         * A NOVA CUSTODIA E CRIADA ANTES DE A ANTIGA SER ENCERRADA, e o flush no meio nao e
         * decoracao: o indice unico parcial admite no maximo UMA custodia em curso por animal, e as
         * duas abertas ao mesmo tempo derrubariam a operacao inteira. E a mesma armadilha que o 8b
         * encontrou na troca de titularidade, e que a exclusao de conta repete no comentario dela.
         *
         * Entao: encerra primeiro, com flush, e so depois abre.
         */
        atual.setEndedAt(agora);
        atual.setEndReason(CustodyEndReason.TRANSFERENCIA);
        custodyRepository.saveAndFlush(atual);

        Custody estadia = custodyRepository.saveAndFlush(Custody.builder()
                .animal(animal)
                .holderOrganization(creche)
                .nature(CustodyNature.TRANSITORIA)
                .startedAt(agora)
                .expectedEndAt(dto.getExpectedReturnOn().atStartOfDay())
                .build());

        atual.setSuccessor(estadia);
        custodyRepository.saveAndFlush(atual);

        manterQuemEntregouLendo(animal, atual, agora);

        return toResponse(estadia, atual, agora);
    }

    @Override
    @Transactional(readOnly = true)
    public BoardingResponseDTO emCurso(UUID animalId) {
        animalAccessGuard.requireLeitura(animalId);

        Custody estadia = custodyRepository.findEmCurso(animalId)
                .filter(c -> c.getNature() == CustodyNature.TRANSITORIA)
                .filter(c -> c.getHolderOrganization() != null)
                .orElseThrow(() -> erro(ErrorMessageEnum.BOARDING_NOT_FOUND, HttpStatus.NOT_FOUND));

        return toResponse(estadia, anteriorDe(estadia), LocalDateTime.now());
    }

    /**
     * A volta.
     *
     * <b>Encerra quem esta com o animal OU quem o entregou</b>, e os dois importam: a creche registra
     * a saida no balcao, e o tutor que buscou o cachorro num sabado tambem precisa poder fechar. Sem
     * o segundo, uma creche que esquecesse de registrar deixaria o animal fora de casa para sempre —
     * e o tutor sem poder fazer nada sobre o proprio animal.
     */
    @Override
    @Transactional
    public BoardingResponseDTO devolver(UUID animalId) {
        Custody estadia = custodyRepository.findEmCurso(animalId)
                .filter(c -> c.getNature() == CustodyNature.TRANSITORIA)
                .filter(c -> c.getHolderOrganization() != null)
                .orElseThrow(() -> erro(ErrorMessageEnum.BOARDING_NOT_FOUND, HttpStatus.NOT_FOUND));

        Custody anterior = anteriorDe(estadia);

        if (!podeEncerrar(estadia, anterior)) {
            throw erro(ErrorMessageEnum.CANNOT_END_BOARDING, HttpStatus.FORBIDDEN);
        }

        LocalDateTime agora = LocalDateTime.now();

        estadia.setEndedAt(agora);
        estadia.setEndReason(CustodyEndReason.DEVOLUCAO);
        custodyRepository.saveAndFlush(estadia);

        /*
         * A CUSTODIA VOLTA A QUEM ENTREGOU, e nao "a quem estava antes" em geral: o {@code
         * CustodyNature.TRANSITORIA} ja dizia que "quem recebeu devolve para alguem, nunca para lugar
         * nenhum". Sem o sucessor apontando de volta, o quarto invariante do produto — nenhuma
         * custodia termina sem sucessor — seria quebrado justamente pelo fluxo que mais parece
         * inofensivo.
         */
        Custody devolvida = custodyRepository.saveAndFlush(Custody.builder()
                .animal(estadia.getAnimal())
                .holderPerson(anterior == null ? null : anterior.getHolderPerson())
                .holderOrganization(anterior == null ? null : anterior.getHolderOrganization())
                .nature(anterior == null ? CustodyNature.DEFINITIVA : anterior.getNature())
                .startedAt(agora)
                .build());

        estadia.setSuccessor(devolvida);
        custodyRepository.saveAndFlush(estadia);

        revogarAConcessaoDaEstadia(estadia, anterior, agora);

        return toResponse(estadia, anterior, agora);
    }

    /**
     * Quem entregou continua lendo a vida do animal enquanto ele esta fora.
     *
     * <b>Sem isto, a tela nao existiria:</b> a custodia passou para a creche, e o {@code
     * AnimalAccessGuard} responderia 404 ao tutor que abriu o Petfy em viagem — no unico momento em
     * que ele mais precisa olhar.
     *
     * <b>EDITOR e nao VIEWER</b>, porque quem entregou nao virou espectador: ele pode corrigir um
     * peso, anotar que o animal tomou remedio antes de sair. Perder a escrita seria uma punicao por
     * ter viajado.
     *
     * <b>Reusa a concessao vigente, se houver</b>, pela mesma razao do encaminhamento (Tela 45):
     * gravar por cima encolheria um acesso que ja existia por outro motivo.
     */
    private void manterQuemEntregouLendo(Animal animal, Custody anterior, LocalDateTime agora) {
        Person quemEntregou = anterior.getHolderPerson();

        // quem entregou era uma ORGANIZACAO — o abrigo mandando um animal para a creche. Nao ha
        // pessoa a quem conceder, e a organizacao continua alcancando o animal pelos caminhos dela
        if (quemEntregou == null) {
            return;
        }

        Grant concessao = grantRepository
                .findVigenteDaPessoaNoAnimal(animal.getAnimalId(), quemEntregou.getPersonId(), agora)
                .orElseGet(() -> Grant.builder()
                        .animal(animal)
                        .granteePerson(quemEntregou)
                        .grantedAt(agora)
                        .build());

        Set<GrantScope> escopos = new LinkedHashSet<>(concessao.getScopes());
        escopos.addAll(ESCOPO_DE_QUEM_ENTREGOU);

        concessao.setScopes(escopos);
        concessao.setLevel(GrantLevel.EDITOR);
        // GRANTED BY ELE MESMO, e e a verdade: foi ele quem entregou o animal, e e ele quem retoma a
        // custodia na volta. E o que faz esta linha ser uma concessao de verdade, e nao a excecao
        // "concedida por ninguem" que a Tela 33 recusou.
        concessao.setGrantedBy(quemEntregou);
        concessao.setRevokedAt(null);
        // SEM PRAZO: a volta e que encerra, e ela nao acontece no relogio. Uma data aqui faria o
        // acesso fechar sozinho numa estadia que se estendeu, e o tutor perderia a tela no dia em que
        // a viagem atrasou
        concessao.setExpiresAt(null);

        grantRepository.save(concessao);
    }

    /**
     * Na volta, a concessao da estadia deixa de existir.
     *
     * <b>So se ela nasceu com a estadia.</b> Quem ja tinha concessao antes — a co-tutora, a clinica
     * de sempre — continua com a dela: revogar aqui tiraria um acesso que ninguem pediu para tirar, e
     * o sintoma apareceria semanas depois, longe daqui.
     *
     * O criterio e simples e verdadeiro: quem RESPONDE pelo animal nao precisa de concessao nenhuma.
     * Se quem retomou a custodia tem uma, ela e a que foi criada na entrega.
     */
    private void revogarAConcessaoDaEstadia(Custody estadia, Custody anterior, LocalDateTime agora) {
        if (anterior == null || anterior.getHolderPerson() == null) {
            return;
        }

        grantRepository
                .findVigenteDaPessoaNoAnimal(estadia.getAnimal().getAnimalId(),
                        anterior.getHolderPerson().getPersonId(), agora)
                .filter(concessao -> concessao.getGrantedBy() != null
                        && concessao.getGrantedBy().getPersonId()
                                .equals(anterior.getHolderPerson().getPersonId()))
                .ifPresent(concessao -> {
                    concessao.setRevokedAt(agora);
                    grantRepository.save(concessao);
                });
    }

    /**
     * Quem entregou o animal.
     *
     * <b>Pela corrente de sucessao, e nao por "a ultima que terminou".</b> Um animal com historia
     * longa tem varias custodias encerradas, e a que aponta para esta e a unica que significa "foi
     * daqui que ele veio".
     */
    private Custody anteriorDe(Custody estadia) {
        return custodyRepository
                .findByAnimalAnimalIdOrderByStartedAtAsc(estadia.getAnimal().getAnimalId()).stream()
                .filter(c -> c.getSuccessor() != null
                        && c.getSuccessor().getCustodyId().equals(estadia.getCustodyId()))
                .findFirst()
                .orElse(null);
    }

    private boolean podeEncerrar(Custody estadia, Custody anterior) {
        Person eu = currentPersonProvider.require();

        boolean souACreche = currentProfessionalProvider.organizacaoDeclarada(eu)
                .map(declarada -> declarada.getOrganizationId()
                        .equals(estadia.getHolderOrganization().getOrganizationId()))
                .orElse(false);

        boolean euEntreguei = anterior != null
                && anterior.getHolderPerson() != null
                && anterior.getHolderPerson().getPersonId().equals(eu.getPersonId());

        return souACreche || euEntreguei;
    }

    /**
     * Hospedar exige poder DETER CUSTODIA.
     *
     * Uma clinica que so registra ato clinico nao passa a responder pelo animal por uma semana — e a
     * capacidade existe no modelo desde a Tela 15 justamente para separar essas coisas.
     */
    private void exigirQuePossaDeterCustodia(Organization creche) {
        boolean pode = creche.getCapabilities() != null
                && creche.getCapabilities().contains(OrganizationCapability.DETER_CUSTODIA);

        if (!pode) {
            throw erro(ErrorMessageEnum.ORGANIZATION_CANNOT_BOARD, HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private BoardingResponseDTO toResponse(Custody estadia, Custody anterior, LocalDateTime agora) {
        LocalDateTime fim = estadia.getExpectedEndAt();

        /*
         * CONTA POR DATA, E NAO POR INSTANTE — e o teste desta tela e que encontrou a diferenca.
         *
         * "De 08/08 a 15/08" sao sete dias para quem viaja, e a duracao entre os dois INSTANTES
         * (09h15 do dia 8 e 00h00 do dia 15) e de seis dias e pouco, que trunca para seis. A tela
         * diria "dia 3 de 6" numa semana que a propria tela chama de sete.
         *
         * A pergunta e de calendario, entao a conta e de calendario.
         */
        LocalDate entrada = estadia.getStartedAt().toLocalDate();

        long totalDeDias = fim == null ? 0
                : Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(entrada, fim.toLocalDate()));

        LocalDateTime referencia = estadia.getEndedAt() == null ? agora : estadia.getEndedAt();

        // "Dia 3 de 7": o dia da entrada e o dia 1, e nao o dia 0 — e como quem viaja conta
        long diaAtual = java.time.temporal.ChronoUnit.DAYS
                .between(entrada, referencia.toLocalDate()) + 1;

        return BoardingResponseDTO.builder()
                .boardingId(estadia.getCustodyId())
                .animalId(estadia.getAnimal().getAnimalId())
                .animalName(estadia.getAnimal().getName())
                .organizationId(estadia.getHolderOrganization().getOrganizationId())
                .organizationName(estadia.getHolderOrganization().getName())
                .startedAt(estadia.getStartedAt())
                .expectedReturnOn(fim == null ? null : fim.toLocalDate())
                .endedAt(estadia.getEndedAt())
                .dayOfStay((int) Math.max(1, diaAtual))
                .totalDays((int) totalDeDias)
                .returnsToName(nomeDe(anterior))
                .canEnd(estadia.getEndedAt() == null && podeEncerrar(estadia, anterior))
                .build();
    }

    private String nomeDe(Custody custodia) {
        if (custodia == null) {
            return null;
        }

        return Optional.ofNullable(custodia.getHolderPerson())
                .map(Person::getName)
                .orElseGet(() -> custodia.getHolderOrganization() == null
                        ? null : custodia.getHolderOrganization().getName());
    }

    private PetfyHealthcareException erro(ErrorMessageEnum mensagem, HttpStatus status) {
        return new PetfyHealthcareException(mensagem.getMessage(), mensagem.getCode(), status);
    }

}
