package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.ObservationRequestDTO;
import br.com.petfy.healthcare.domain.dto.ServiceAppointmentCloseRequestDTO;
import br.com.petfy.healthcare.domain.dto.ServiceAppointmentRequestDTO;
import br.com.petfy.healthcare.domain.dto.ServiceAppointmentResponseDTO;
import br.com.petfy.healthcare.domain.dto.ServiceSafetyNoteDTO;
import br.com.petfy.healthcare.domain.entity.*;
import br.com.petfy.healthcare.domain.repository.*;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.ObservationService;
import br.com.petfy.healthcare.service.ServiceAppointmentService;
import br.com.petfy.healthcare.service.VaccineStatusCalculator;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A agenda de banho e tosa (Tela 18).
 *
 * <b>"Se o sistema fica bom com um tosador vendo quatro linhas, o escopo funciona. E o teste mais duro
 * da tela 09."</b> Este servico e esse teste, e ele passa por uma razao que nao esta neste arquivo: o
 * escopo de concessao existe desde o P2a, e o petshop e simplesmente uma organizacao cuja concessao e
 * pequena. <b>Nao ha nenhuma regra especial de petshop na guarda</b> — o que ha e um tutor que
 * concedeu {@code CONDICOES} e {@code OBSERVACOES} e nao concedeu {@code PRONTUARIO}.
 *
 * <b>E o estado "no escuro" e a parte mais importante desta tela.</b> Um cartao sem as quatro linhas
 * parece um animal sem restricao nenhuma; dizer que o acesso venceu e a diferenca entre tosar com
 * cuidado e tosar as cegas. A maioria dos produtos esconderia isso.
 */
@Service
@RequiredArgsConstructor
public class ServiceAppointmentServiceImpl implements ServiceAppointmentService {

    private final ServiceAppointmentRepository appointmentRepository;
    private final AnimalRepository animalRepository;
    private final GrantRepository grantRepository;
    private final AnimalHealthConditionRepository conditionRepository;
    private final ObservationRepository observationRepository;
    private final VaccineRepository vaccineRepository;
    private final VaccineStatusCalculator vaccineStatusCalculator;
    private final ObservationService observationService;
    private final CurrentPersonProvider currentPersonProvider;
    private final CurrentProfessionalProvider currentProfessionalProvider;

    @Override
    @Transactional(readOnly = true)
    public List<ServiceAppointmentResponseDTO> doDia(LocalDate dia) {
        Organization petshop = petshopAtivo();

        LocalDate quando = dia == null ? LocalDate.now() : dia;

        return appointmentRepository.findDoDia(petshop.getOrganizationId(),
                        quando.atStartOfDay(), quando.atTime(23, 59, 59)).stream()
                .map(agendamento -> toResponse(agendamento, petshop))
                .collect(Collectors.toList());
    }

    /**
     * "Agendar banho."
     *
     * <b>Exige que o petshop ALCANCE o animal.</b> Sem isso, qualquer organizacao poria qualquer animal
     * na propria agenda — e a agenda e justamente onde aparece o que o tutor compartilhou. Um cartao
     * com o nome de um animal que ninguem autorizou ja seria vazamento, mesmo sem nenhuma linha de
     * saude junto.
     */
    @Override
    @Transactional
    public ServiceAppointmentResponseDTO agendar(ServiceAppointmentRequestDTO dto) {
        Organization petshop = petshopAtivo();

        Animal animal = animalRepository.findById(dto.getAnimalId())
                .orElseThrow(() -> erro(ErrorMessageEnum.ANIMAL_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (concessaoDoPetshop(animal.getAnimalId(), petshop).isEmpty()) {
            throw erro(ErrorMessageEnum.ANIMAL_NOT_REACHED_BY_ORGANIZATION, HttpStatus.NOT_FOUND);
        }

        appointmentRepository
                .findNoHorario(petshop.getOrganizationId(), animal.getAnimalId(), dto.getScheduledAt())
                .ifPresent(existente -> {
                    throw erro(ErrorMessageEnum.APPOINTMENT_ALREADY_SCHEDULED, HttpStatus.CONFLICT);
                });

        ServiceAppointment agendamento = appointmentRepository.save(ServiceAppointment.builder()
                .organization(petshop)
                .animal(animal)
                .scheduledAt(dto.getScheduledAt())
                .service(dto.getService().trim())
                .status(ServiceAppointmentStatus.AGENDADO)
                .createdBy(currentPersonProvider.require())
                .creationDate(LocalDateTime.now())
                .build());

        return toResponse(agendamento, petshop);
    }

    @Override
    @Transactional
    public ServiceAppointmentResponseDTO marcarEntrada(UUID appointmentId) {
        Organization petshop = petshopAtivo();
        ServiceAppointment agendamento = meuAgendamento(appointmentId, petshop);

        exigirEstado(agendamento, ServiceAppointmentStatus.AGENDADO);

        agendamento.setStatus(ServiceAppointmentStatus.EM_ATENDIMENTO);
        agendamento.setCheckedInAt(LocalDateTime.now());

        return toResponse(appointmentRepository.save(agendamento), petshop);
    }

    /**
     * "Entregar e avisar Marcelo."
     *
     * <b>O texto vira uma OBSERVACAO, e nao um atendimento.</b> "Descreva o que viu, nao o que acha que
     * e. Isso entra na linha do tempo do Code como observacao sua, e o veterinario decide o resto." O
     * petshop nao tem autoridade clinica, e observacao nunca vira ato clinico sozinha — e a distincao
     * que o DESIGN chama de "a mais importante do produto".
     *
     * <b>E o aviso ao tutor vem de graca</b>: registrar observacao ja avisa quem cuida do animal, pelo
     * caminho que existe desde o P4. Nao ha aviso proprio de petshop, e nao deveria haver — seria um
     * segundo canal dizendo a mesma coisa.
     *
     * <b>Sem texto, nada entra na linha do tempo</b>, e isso e deliberado: um banho que aconteceu sem
     * novidade nao e fato de saude, e enche-la de "deu banho" enterraria a vermelhidao na barriga que
     * alguem vir no banho seguinte.
     */
    @Override
    @Transactional
    public ServiceAppointmentResponseDTO entregar(UUID appointmentId,
                                                  ServiceAppointmentCloseRequestDTO dto) {
        Organization petshop = petshopAtivo();
        ServiceAppointment agendamento = meuAgendamento(appointmentId, petshop);

        exigirEstado(agendamento, ServiceAppointmentStatus.EM_ATENDIMENTO);

        LocalDateTime agora = LocalDateTime.now();

        agendamento.setStatus(ServiceAppointmentStatus.CONCLUIDO);
        agendamento.setCompletedAt(agora);

        ServiceAppointment salvo = appointmentRepository.save(agendamento);

        String texto = dto == null || dto.getNote() == null ? "" : dto.getNote().trim();

        if (!texto.isEmpty()) {
            observationService.create(agendamento.getAnimal().getAnimalId(),
                    ObservationRequestDTO.builder()
                            .description(texto)
                            .observedAt(agora)
                            .build());
        }

        return toResponse(salvo, petshop);
    }

    @Override
    @Transactional
    public ServiceAppointmentResponseDTO faltou(UUID appointmentId) {
        Organization petshop = petshopAtivo();
        ServiceAppointment agendamento = meuAgendamento(appointmentId, petshop);

        exigirEstado(agendamento, ServiceAppointmentStatus.AGENDADO);

        agendamento.setStatus(ServiceAppointmentStatus.FALTOU);

        return toResponse(appointmentRepository.save(agendamento), petshop);
    }

    /**
     * <b>"O que voce precisa saber antes de encostar nele."</b>
     *
     * As quatro linhas, e o que decide cada uma e O ESCOPO DA CONCESSAO — nao uma regra de petshop.
     * Quem recebeu {@code CONDICOES} ve alergia e restricao de manejo; quem recebeu
     * {@code OBSERVACOES} ve o que outros notaram; quem recebeu {@code CARTEIRA} ve se a vacinacao
     * esta em dia. Um petshop com escopo menor ve menos, e a tela diz o que ele nao ve.
     *
     * <b>Nada de prontuario atravessa</b>, mesmo que a concessao o inclua por acaso: esta lista e "o
     * que o banho exige", e nao um resumo clinico. "O historico clinico do Code existe e nao esta
     * aqui."
     */
    private List<ServiceSafetyNoteDTO> linhasDeSeguranca(UUID animalId, Grant concessao) {
        List<ServiceSafetyNoteDTO> linhas = new ArrayList<>();

        Set<GrantScope> escopo = concessao.getScopes();

        if (escopo.contains(GrantScope.CONDICOES)) {
            conditionRepository.findByAnimalOrdenadasPorRelevancia(animalId).stream()
                    .filter(c -> c.getResolvedAt() == null)
                    .forEach(c -> linhas.add(ServiceSafetyNoteDTO.builder()
                            /*
                             * O LOSANGO VERMELHO VEM DO TIPO, e nao da gravidade — e foi o schema que
                             * ensinou isso. O CHECK `gravidade_so_em_alergia` existe desde o P3: uma
                             * condicao cronica NAO TEM gravidade, porque "leve/moderada/grave" e
                             * vocabulario de reacao alergica.
                             *
                             * E o desenho concorda: o losango dele e "displasia no quadril — nao
                             * erguer pelas patas traseiras", uma condicao cronica. O que faz aquela
                             * linha ser a mais forte da lista nao e uma escala; e ela restringir o
                             * MANEJO do animal, que e exatamente o que o tosador precisa saber antes
                             * de encostar nele.
                             */
                            .severity(c.getKind() == AnimalHealthConditionKind.CONDICAO_CRONICA
                                    ? "MANEJO" : "ATENCAO")
                            .text(c.getDescription())
                            .build()));
        }

        if (escopo.contains(GrantScope.OBSERVACOES)) {
            observationRepository.findByAnimalAnimalIdOrderByObservedAtDesc(animalId).stream()
                    // as tres mais recentes: "medo de secador quente. No ultimo banho, tremeu." A
                    // lista inteira viraria um historico, e o desenho pede quatro linhas
                    .limit(3)
                    .forEach(o -> linhas.add(ServiceSafetyNoteDTO.builder()
                            .severity("ATENCAO")
                            .text(o.getDescription())
                            .build()));
        }

        if (escopo.contains(GrantScope.CARTEIRA)) {
            boolean emDia = vaccineRepository
                    .findByAnimalAnimalIdOrderByApplicationDateDesc(animalId).stream()
                    .noneMatch(v -> v.getNextDoseDate() != null
                            && v.getNextDoseDate().isBefore(LocalDate.now()));

            linhas.add(ServiceSafetyNoteDTO.builder()
                    .severity(emDia ? "OK" : "ATENCAO")
                    .text(emDia ? "VACINACAO_EM_DIA" : "VACINACAO_ATRASADA")
                    .build());
        }

        return linhas;
    }

    /**
     * A organizacao em nome de que a pessoa esta agindo.
     *
     * <b>Declarada no cabecalho, e nao inferida</b> — a mesma regra do {@code requireCustodia}: quem
     * trabalha em dois petshops tem de dizer em qual esta, senao o banho apareceria na agenda errada.
     */
    private Organization petshopAtivo() {
        return currentProfessionalProvider
                .organizacaoDeclarada(currentPersonProvider.require())
                .orElseThrow(() -> erro(ErrorMessageEnum.AMBIGUOUS_CONTEXT, HttpStatus.CONFLICT));
    }

    private Optional<Grant> concessaoDoPetshop(UUID animalId, Organization petshop) {
        return grantRepository.findVigenteDaClinicaNoAnimal(
                animalId, petshop.getOrganizationId(), LocalDateTime.now());
    }

    /**
     * O agendamento desta organizacao.
     *
     * <b>404 e nao 403 para o de outro petshop</b>, pela mesma razao do {@code AnimalAccessGuard}: um
     * 403 confirmaria que aquele id existe, e permitiria varrer ids para descobrir quem atende quem.
     */
    private ServiceAppointment meuAgendamento(UUID appointmentId, Organization petshop) {
        return appointmentRepository.findById(appointmentId)
                .filter(a -> a.getOrganization().getOrganizationId()
                        .equals(petshop.getOrganizationId()))
                .orElseThrow(() -> erro(ErrorMessageEnum.APPOINTMENT_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    private void exigirEstado(ServiceAppointment agendamento, ServiceAppointmentStatus esperado) {
        if (agendamento.getStatus() != esperado) {
            throw erro(ErrorMessageEnum.APPOINTMENT_WRONG_STATE, HttpStatus.CONFLICT);
        }
    }

    private ServiceAppointmentResponseDTO toResponse(ServiceAppointment agendamento,
                                                     Organization petshop) {
        UUID animalId = agendamento.getAnimal().getAnimalId();

        Optional<Grant> concessao = concessaoDoPetshop(animalId, petshop);

        return ServiceAppointmentResponseDTO.builder()
                .serviceAppointmentId(agendamento.getServiceAppointmentId())
                .animalId(animalId)
                .animalName(agendamento.getAnimal().getName())
                .service(agendamento.getService())
                .scheduledAt(agendamento.getScheduledAt())
                .status(agendamento.getStatus())
                .checkedInAt(agendamento.getCheckedInAt())
                .completedAt(agendamento.getCompletedAt())
                .safetyNotes(concessao
                        .map(c -> linhasDeSeguranca(animalId, c))
                        .orElseGet(List::of))
                // "Acesso ao Petfy venceu em 31/07 — voce esta no escuro"
                .inTheDark(concessao.isEmpty())
                .build();
    }

    private PetfyHealthcareException erro(ErrorMessageEnum mensagem, HttpStatus status) {
        return new PetfyHealthcareException(mensagem.getMessage(), mensagem.getCode(), status);
    }

}
