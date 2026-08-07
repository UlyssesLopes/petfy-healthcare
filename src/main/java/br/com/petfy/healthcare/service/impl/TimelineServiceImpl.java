package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.TimelineEntryResponseDTO;
import br.com.petfy.healthcare.domain.entity.GrantScope;
import br.com.petfy.healthcare.domain.entity.TimelineEntry;
import br.com.petfy.healthcare.domain.repository.TimelineRepository;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.service.TimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * A leitura ordenada dos eventos de um animal, atravessando tudo que aconteceu com ele.
 *
 * <b>E isto o produto.</b> Nenhum ator do ecossistema tem esse insumo: a clinica ve os
 * atendimentos dela, a creche ve a turma dela, o tutor ve o que lembra. Só o Petfy ve o
 * animal inteiro - e e dai que sai a percepcao do Horizonte 4.
 *
 * <b>A linha do tempo nao recomeca na transferencia.</b> Ela e do animal, e nao da
 * conta: o adotante recebe a vida inteira, e nao uma ficha em branco com a data de
 * hoje. Isso sai de graca aqui porque a view filtra por {@code animal_id} e nao sabe o
 * que e custodia.
 */
@Service
@RequiredArgsConstructor
public class TimelineServiceImpl implements TimelineService {

    private final TimelineRepository timelineRepository;
    private final AnimalAccessGuard animalAccessGuard;

    /**
     * <b>O escopo filtra o que aparece, e e aqui que ele finalmente vale para leitura.</b>
     *
     * Quem responde pelo animal ve tudo. Quem alcanca por concessao ve so os tipos que
     * o escopo dela permite - a creche com CARTEIRA e CONDICOES ve vacina, antiparasitario
     * e alergia, e nao ve atendimento. Sem esse filtro, a linha do tempo seria a porta
     * dos fundos do escopo: entregaria num endpoint o prontuario que o P2a passou a
     * proteger nos outros.
     *
     * <b>Filtra depois de paginar, e isso e uma limitacao assumida.</b> Uma pagina pode
     * voltar com menos itens que o tamanho pedido quando o escopo corta parte dela. O
     * alternativo - empurrar o escopo para dentro da consulta - exigiria montar o
     * {@code where} dinamicamente com os tipos permitidos, e vale fazer quando houver
     * cliente reclamando de pagina curta. O que nao pode acontecer e vazar evento fora
     * do escopo, e isso nao acontece.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<TimelineEntryResponseDTO> doAnimal(UUID animalId, Pageable pageable) {
        animalAccessGuard.requireLeitura(animalId);

        Set<GrantScope> escopo = animalAccessGuard.escopoDoAutenticado(animalId);

        return timelineRepository.findDoAnimal(animalId, pageable)
                .map(entrada -> toResponse(entrada, escopo));
    }

    /**
     * <b>Tudo que identifica autoria e mascarado fora do escopo, e isso e regra e nao
     * zelo.</b> Dizer "Clinica Bicho Feliz" ou "CRMV-SP 12345" num evento que quem le
     * nao pode abrir entregaria exatamente o que o escopo esconde - que o animal foi
     * atendido, e por quem. O que atravessa e so o que a entrada opaca precisa dizer:
     * que houve um evento daquele tipo, naquela data.
     */
    private TimelineEntryResponseDTO toResponse(TimelineEntry entrada, Set<GrantScope> escopo) {
        boolean alcanca = escopo == null
                || escopo.contains(entrada.getEventType().escopoExigido());

        return TimelineEntryResponseDTO.builder()
                .eventId(alcanca ? entrada.getEventId() : null)
                .eventType(entrada.getEventType())
                .occurredAt(entrada.getOccurredAt())
                .recordedAt(entrada.getRecordedAt())
                // Fora do escopo, o evento aparece SEM conteudo em vez de desaparecer.
                //
                // Some seria pior: quem le acharia que o animal nao tem prontuario, e a
                // creche concluiria que o animal nunca foi ao veterinario. Dizer "houve
                // um atendimento em marco, e voce nao tem acesso a ele" e honesto sobre
                // o que existe e sobre o que foi concedido.
                .summary(alcanca ? entrada.getSummary() : null)
                .visivel(alcanca)
                // vem da view desde a V29: era uma consulta por entrada, e uma pagina de
                // vinte eventos custava vinte
                .recordedByName(alcanca ? entrada.getRecordedByName() : null)
                .organizationName(alcanca ? entrada.getOrganizationName() : null)
                .credentialLabel(alcanca ? entrada.getCredentialLabel() : null)
                .credentialStatus(alcanca ? entrada.getCredentialStatus() : null)
                // zero fora do escopo: a contagem de correcoes diria que houve retificacao
                // num evento cujo conteudo quem le nao alcanca
                .correctionCount(alcanca ? entrada.getCorrectionCount() : 0)
                // o peso anterior tambem some fora do escopo: quem nao alcanca PESO nao
                // recebe o valor atual, e entregar o anterior daria a curva pela porta
                // dos fundos
                .previousWeight(alcanca ? entrada.getPreviousWeight() : null)
                .healthData(entrada.isHealthData())
                .build();
    }

}
