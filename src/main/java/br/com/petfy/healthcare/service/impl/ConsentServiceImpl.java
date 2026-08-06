package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.ConsentStatusResponseDTO;
import br.com.petfy.healthcare.domain.entity.ConsentDocument;
import br.com.petfy.healthcare.domain.entity.ConsentRecord;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.ConsentRecordRepository;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.RequestEvidenceProvider;
import br.com.petfy.healthcare.service.ConsentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsentServiceImpl implements ConsentService {

    private final ConsentRecordRepository consentRecordRepository;
    private final CurrentPersonProvider currentPersonProvider;
    private final RequestEvidenceProvider requestEvidenceProvider;

    /**
     * Versao vigente de cada documento, por configuracao e nao por constante.
     *
     * Mudar a politica de privacidade nao pode exigir deploy de codigo: e mudanca de
     * texto juridico, e quem a faz nao e quem compila. Trocar o valor faz todos os
     * aceites anteriores virarem pendentes na proxima leitura, que e exatamente o
     * comportamento desejado.
     */
    @Value("${petfy.consent.terms-version:2026-08-05}")
    private String termsVersion;

    @Value("${petfy.consent.privacy-version:2026-08-05}")
    private String privacyVersion;

    @Override
    @Transactional
    public void registrarAceiteNoCadastro(Person person) {
        gravarVigentes(person);
    }

    @Override
    @Transactional(readOnly = true)
    public ConsentStatusResponseDTO statusDoAutenticado() {
        Person person = currentPersonProvider.require();

        return montarStatus(consentRecordRepository
                .findByPersonPersonIdOrderByAcceptedAtDesc(person.getPersonId()));
    }

    @Override
    @Transactional
    public ConsentStatusResponseDTO aceitarVigentes() {
        Person person = currentPersonProvider.require();

        gravarVigentes(person);

        return montarStatus(consentRecordRepository
                .findByPersonPersonIdOrderByAcceptedAtDesc(person.getPersonId()));
    }

    /**
     * Grava o aceite de cada documento vigente que ainda nao tenha sido aceito
     * naquela versao.
     *
     * Repetir o aceite da mesma versao e no-op, e nao erro: o cliente pode reenviar
     * por perda de resposta, e a chave unica (person, documento, versao) recusaria a
     * segunda linha com 500. O primeiro aceite e o que vale - e a data dele que
     * interessa a uma auditoria.
     */
    private void gravarVigentes(Person person) {
        String ip = requestEvidenceProvider.ip();
        String userAgent = requestEvidenceProvider.userAgent();

        vigentes().forEach((documento, versao) -> {
            boolean jaAceito = consentRecordRepository
                    .existsByPersonPersonIdAndDocumentAndDocumentVersion(person.getPersonId(), documento, versao);

            if (jaAceito) {
                return;
            }

            consentRecordRepository.save(ConsentRecord.builder()
                    .person(person)
                    .document(documento)
                    .documentVersion(versao)
                    .acceptedAt(LocalDateTime.now())
                    .ipAddress(ip)
                    .userAgent(userAgent)
                    .build());
        });
    }

    private Map<ConsentDocument, String> vigentes() {
        Map<ConsentDocument, String> mapa = new EnumMap<>(ConsentDocument.class);
        mapa.put(ConsentDocument.TERMS_OF_SERVICE, termsVersion);
        mapa.put(ConsentDocument.PRIVACY_POLICY, privacyVersion);
        return mapa;
    }

    /**
     * Pendente e documento vigente sem aceite <b>naquela versao</b>, e nao documento
     * nunca aceito. Quem aceitou a versao de janeiro e viu a politica mudar em marco
     * esta pendente, mesmo tendo aceitado algo um dia - e a resposta diz qual versao
     * a pessoa aceitou antes, para o cliente poder mostrar o que mudou.
     */
    private ConsentStatusResponseDTO montarStatus(List<ConsentRecord> registros) {
        List<ConsentStatusResponseDTO.AceiteDTO> aceites = registros.stream()
                .map(r -> ConsentStatusResponseDTO.AceiteDTO.builder()
                        .document(r.getDocument())
                        .documentVersion(r.getDocumentVersion())
                        .acceptedAt(r.getAcceptedAt())
                        .build())
                .toList();

        List<ConsentStatusResponseDTO.PendenteDTO> pendentes = new ArrayList<>();

        vigentes().forEach((documento, versaoVigente) -> {
            boolean aceitouAVigente = registros.stream()
                    .anyMatch(r -> r.getDocument() == documento
                            && r.getDocumentVersion().equals(versaoVigente));

            if (aceitouAVigente) {
                return;
            }

            Optional<String> anterior = registros.stream()
                    .filter(r -> r.getDocument() == documento)
                    .max(Comparator.comparing(ConsentRecord::getAcceptedAt))
                    .map(ConsentRecord::getDocumentVersion);

            pendentes.add(ConsentStatusResponseDTO.PendenteDTO.builder()
                    .document(documento)
                    .documentVersion(versaoVigente)
                    .versaoAceitaAnteriormente(anterior.orElse(null))
                    .build());
        });

        return ConsentStatusResponseDTO.builder()
                .aceites(aceites)
                .pendentes(pendentes)
                .tudoAceito(pendentes.isEmpty())
                .build();
    }

}
