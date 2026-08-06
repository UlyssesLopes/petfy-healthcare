package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.AccessActorType;
import br.com.petfy.healthcare.domain.entity.AccessedResource;
import br.com.petfy.healthcare.domain.entity.AntiparasiticKind;
import br.com.petfy.healthcare.domain.entity.ConsentDocument;
import br.com.petfy.healthcare.domain.entity.HealthEventCategory;
import br.com.petfy.healthcare.domain.entity.AnimalHealthConditionKind;
import br.com.petfy.healthcare.domain.entity.AnimalHealthConditionSeverity;
import br.com.petfy.healthcare.domain.entity.GrantScope;
import br.com.petfy.healthcare.domain.entity.Species;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Tudo que o Petfy guarda sobre um tutor, num documento.
 *
 * E o exercicio do direito de <b>portabilidade</b> pela LGPD, irmao da exclusao que o
 * passo 10 entregou: sair do sistema sem poder levar o historico de saude do proprio
 * animal deixa o tutor preso ao produto por refem, e nao por escolha.
 *
 * <b>Dado de terceiro entra reduzido, e nao inteiro.</b> A portabilidade e dos dados
 * <i>do titular</i>, e um arquivo de export circula e fica guardado - manda-lo por
 * e-mail ou joga-lo numa pasta compartilhada e o destino normal dele. Entao:
 *
 * <ul>
 *   <li><b>Co-tutor</b> aparece por nome e papel, sem e-mail. Saber com quem se divide o
 *       animal e informacao do titular; o endereco de contato da outra pessoa nao e.</li>
 *   <li><b>Veterinario e clinica</b> aparecem por nome. E capacidade profissional, e faz
 *       parte da integridade do registro saber quem escreveu no prontuario.</li>
 *   <li><b>Quem abriu o link publico</b> aparece so pelo IP, porque e tudo que existe -
 *       ver o log de acesso.</li>
 * </ul>
 *
 * <b>Animal compartilhado entra</b>, com o papel do titular indicado. Ele tem acesso
 * legitimo aquele animal, e omiti-lo daria um export que contradiz o que o app mostra.
 */
@Builder
public record PersonExportDTO(

        /** Quando o documento foi gerado. E o que datar uma copia guardada. */
        LocalDateTime generatedAt,

        /**
         * Versao do formato. Existe para o dia em que o documento mudar de forma: um
         * arquivo guardado em 2026 precisa dizer por qual leitor foi escrito.
         */
        String formatVersion,

        TutorDTO tutor,

        List<ConsentimentoDTO> consentimentos,

        List<AnimalExportDTO> animals,

        /** O que este documento deliberadamente nao carrega. */
        List<String> limitacoes

) {

    @Builder
    public record TutorDTO(
            UUID personId,
            String name,
            String email,
            String phone,
            String address,
            LocalDateTime emailVerifiedAt,
            LocalDateTime creationDate,
            LocalDateTime updateDate
            // a senha nao aparece, nem como hash: hash de senha nao e dado que o
            // titular precise portar, e num arquivo que circula e material para
            // ataque offline
    ) {
    }

    @Builder
    public record ConsentimentoDTO(
            ConsentDocument document,
            String documentVersion,
            LocalDateTime acceptedAt
    ) {
    }

    @Builder
    public record AnimalExportDTO(
            UUID animalId,
            String name,
            String type,
            String breed,
            Species species,
            LocalDate bornDate,
            String gender,
            String color,
            Boolean microchip,
            /** O numero, que e o identificador legal do animal. */
            String microchipNumber,
            Boolean castrated,
            LocalDate castratedAt,
            String generalRegistry,
            Double weight,
            LocalDateTime creationDate,

            /**
             * Como o titular alcanca <b>este</b> animal: CUSTODIA quando ele responde
             * pelo animal, ou o nivel da concessao quando alguem lhe deu acesso.
             *
             * Substituiu meuPapel, que era um PetTutorRole. O papel juntava as duas
             * coisas num campo so, e era exatamente essa mistura que o P2 desfez.
             */
            String minhaRelacao,

            /**
             * Alergias e condicoes cronicas. Vem logo depois dos tutores de proposito: e a
             * informacao que precisa aparecer antes do historico, nao depois dele.
             */
            List<CondicaoDTO> condicoes,

            List<CoTutorDTO> coTutores,
            List<VacinaDTO> vacinas,
            List<AntiparasiticoDTO> antiparasitarios,
            List<PesagemDTO> pesagens,
            List<AtendimentoDTO> atendimentos,
            List<AnexoDTO> anexos,
            List<LinkCompartilhadoDTO> linksCompartilhados,
            List<AcessoDeClinicaDTO> acessosDeClinica,
            List<AcessoRegistradoDTO> acessosDeTerceiros
    ) {
    }

    /**
     * Alergia ou condicao cronica.
     *
     * Entra no export porque e a informacao que outro sistema precisa ler primeiro: quem
     * importa o prontuario deste animal tem de saber a que ele e alergico antes de ler o
     * que ja aconteceu com ele.
     */
    @Builder
    public record CondicaoDTO(
            AnimalHealthConditionKind kind,
            String description,
            AnimalHealthConditionSeverity severity,
            String notes,
            LocalDate since,
            LocalDate resolvedAt,
            boolean ativa
    ) {
    }

    @Builder
    public record CoTutorDTO(
            String name,
            String relacao,
            LocalDateTime desde
            // sem personId e sem e-mail: identificam outra pessoa fora deste documento
    ) {
    }

    @Builder
    public record VacinaDTO(
            UUID vaccineId,
            String vaccineName,
            LocalDate applicationDate,
            LocalDate nextDoseDate,
            String description,
            String clinicName,
            LocalDateTime creationDate,
            List<CorrecaoDTO> correcoes
    ) {
    }

    @Builder
    public record AntiparasiticoDTO(
            UUID antiparasiticId,
            String name,
            AntiparasiticKind kind,
            LocalDate applicationDate,
            LocalDate nextDoseDate,
            String description
    ) {
    }

    @Builder
    public record PesagemDTO(
            Double weight,
            LocalDate measuredAt,
            String note
    ) {
    }

    @Builder
    public record AtendimentoDTO(
            UUID healthRecordId,
            /** Classificacao. O rotulo livre continua em {@code eventType}, ao lado. */
            HealthEventCategory category,
            String eventType,
            String diagnosis,
            LocalDate eventDate,
            String description,
            String clinicName,
            LocalDateTime creationDate,
            List<CorrecaoDTO> correcoes
    ) {
    }

    /**
     * Uma correcao no registro. Serve a portabilidade e a confianca: o historico de
     * saude que o tutor leva embora inclui o que foi alterado e por quem.
     */
    @Builder
    public record CorrecaoDTO(
            String corrigidoPor,
            String valorAnterior,
            LocalDateTime corrigidoEm
    ) {
    }

    /**
     * O anexo aparece como metadado e caminho de download, e nao como bytes.
     *
     * JSON carrega binario so em base64, o que infla o arquivo em um terco e transforma
     * um export com tres laudos em algo que nenhum editor de texto abre. O caminho e
     * autenticado - ver a limitacao registrada no documento.
     */
    @Builder
    public record AnexoDTO(
            UUID attachmentId,
            String originalFilename,
            String contentType,
            Long sizeBytes,
            String checksumSha256,
            String description,
            String downloadPath,
            LocalDateTime creationDate
    ) {
    }

    @Builder
    public record LinkCompartilhadoDTO(
            UUID grantId,
            // o quanto o link mostra. Sem isto o documento diria que houve acesso sem
            // dizer a quanto, que e justamente o que o P2 passou a permitir limitar
            Set<GrantScope> scopes,
            LocalDateTime expiresAt,
            LocalDateTime revokedAt,
            boolean active,
            LocalDateTime creationDate
            // o hash do token nao entra: nao serve ao titular e e material para tentar
            // reverter o link
    ) {
    }

    @Builder
    public record AcessoDeClinicaDTO(
            String clinicName,
            Set<GrantScope> scopes,
            LocalDateTime grantedAt,
            LocalDateTime revokedAt,
            boolean active
    ) {
    }

    @Builder
    public record AcessoRegistradoDTO(
            AccessActorType actorType,
            String actorName,
            String clinicName,
            AccessedResource resource,
            LocalDateTime accessedAt,
            String ipAddress
    ) {
    }

}
