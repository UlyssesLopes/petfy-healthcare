package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.entity.AccessActorType;
import br.com.petfy.healthcare.domain.entity.AccessedResource;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.SensitiveAccessLog;
import br.com.petfy.healthcare.domain.repository.SensitiveAccessLogRepository;
import br.com.petfy.healthcare.security.RequestEvidenceProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Registra que um terceiro leu dado de saude de um animal.
 *
 * <b>Falha aqui derruba a leitura, de proposito.</b> E o oposto da politica do
 * {@link br.com.petfy.healthcare.notification.ClinicActivityNotifier}, e a diferenca
 * nao e descuido: la o efeito principal era o registro no historico e o aviso era
 * acessorio, entao perder o efeito por causa do acessorio seria trocar um problema
 * pequeno por um grande. Aqui o log <b>e</b> a garantia. Servir historico de saude
 * sem conseguir registrar quem o leu entrega o dado e perde a unica prova de que
 * alguem o viu - e um log de auditoria que falha em silencio e pior que log nenhum,
 * porque cria a impressao de cobertura.
 *
 * A consequencia esta assumida: se esta tabela ficar indisponivel, a leitura do
 * veterinario e o link publico param. E o comportamento correto para dado de saude, e
 * inverter a escolha e trocar a propagacao por um try - decisao de uma linha, tomada
 * com o motivo a vista.
 *
 * Nao ha metodo para leitura de tutor nem de co-tutor: ver a V17 para por que.
 */
@Service
@RequiredArgsConstructor
public class SensitiveAccessLogger {

    private final SensitiveAccessLogRepository sensitiveAccessLogRepository;
    private final RequestEvidenceProvider requestEvidenceProvider;

    /**
     * Veterinario leu um recurso do animal.
     *
     * Guarda o nome do veterinario e o da clinica no momento do acesso. A clinica
     * importa porque foi ela que o tutor autorizou - a pessoa que abriu e uma
     * consequencia dessa autorizacao, nao o objeto dela.
     */
    public void vetLeu(Person vet, Animal animal, AccessedResource recurso) {
        registrar(SensitiveAccessLog.builder()
                .animal(animal)
                .actorType(AccessActorType.VET)
                .actorId(vet.getPersonId())
                .actorName(vet.getName())
                .clinicName(vet.getClinic() != null ? vet.getClinic().getName() : null)
                .resource(recurso));
    }

    /**
     * Alguem abriu a carteira pelo link publico.
     *
     * Sem ator identificado: quem abre o link nao tem conta. O que sobra e a
     * evidencia de rede, e e justamente por isso que ela volta na resposta ao tutor -
     * sem o IP, dois acessos pelo mesmo link sao indistinguiveis, e o tutor nao tem
     * como decidir se revoga.
     */
    public void linkPublicoAberto(Animal animal) {
        registrar(SensitiveAccessLog.builder()
                .animal(animal)
                .actorType(AccessActorType.SHARE_LINK)
                .resource(AccessedResource.SHARED_CARD));
    }

    private void registrar(SensitiveAccessLog.SensitiveAccessLogBuilder builder) {
        sensitiveAccessLogRepository.save(builder
                .accessedAt(LocalDateTime.now())
                .ipAddress(requestEvidenceProvider.ip())
                .userAgent(requestEvidenceProvider.userAgent())
                .build());
    }

}
