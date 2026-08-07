package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.AttachmentResponseDTO;
import br.com.petfy.healthcare.service.AttachmentContent;
import br.com.petfy.healthcare.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Anexos do animal.
 *
 * O upload e a listagem ficam sob {@code /animals/{animalId}}, porque o animal e a ancora de
 * autorizacao. O download e a remocao ficam em {@code /attachments/{id}} <b>sem
 * animalId</b>: o animal sai do proprio anexo, e pedi-lo abriria a possibilidade de autorizar
 * contra um animal e servir o arquivo de outro.
 */
@RestController
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @Operation(summary = "Anexa um arquivo ao animal",
               description = "Multipart. Aceita JPEG, PNG, WEBP e PDF, reconhecidos pelo CONTEUDO - o "
                             + "Content-Type declarado nao e consultado. vaccineId e healthRecordId sao "
                             + "opcionais e mutuamente exclusivos: dizem o que o arquivo documenta. Sem "
                             + "nenhum dos dois, o anexo e do animal em si.")
    @PostMapping(value = "/animals/{animalId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponseDTO> upload(
            @PathVariable UUID animalId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) UUID vaccineId,
            @RequestParam(required = false) UUID healthRecordId,
            @RequestParam(required = false) String description) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attachmentService.upload(animalId, file, vaccineId, healthRecordId, description));
    }

    @Operation(summary = "Anexos do animal",
               description = "Sem filtro, devolve todos os anexos do animal. Com vaccineId ou "
                             + "healthRecordId, devolve os daquele registro.")
    @GetMapping("/animals/{animalId}/attachments")
    public ResponseEntity<List<AttachmentResponseDTO>> listByAnimal(
            @PathVariable UUID animalId,
            @RequestParam(required = false) UUID vaccineId,
            @RequestParam(required = false) UUID healthRecordId) {
        return ResponseEntity.ok(attachmentService.listByAnimal(animalId, vaccineId, healthRecordId));
    }

    /**
     * O conteudo, em stream.
     *
     * {@code attachment} no Content-Disposition, e nunca {@code inline}: PDF e SVG
     * renderizados no dominio da API viram vetor de XSS, e o navegador do tutor passaria
     * a executar conteudo que outra pessoa subiu. Forcar download custa um clique e
     * fecha a porta.
     */
    @Operation(summary = "Baixa o conteudo do anexo",
               description = "Autorizado a cada chamada pelo animal do anexo. Nao ha URL assinada: link "
                             + "encaminhado por engano daria acesso a dado de saude ate expirar.")
    @GetMapping("/attachments/{attachmentId}/content")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID attachmentId) {
        AttachmentContent conteudo = attachmentService.download(attachmentId);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(conteudo.filename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                // impede o navegador de adivinhar outro tipo a partir do conteudo, que
                // e como um arquivo servido como imagem acaba executado como script
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(conteudo.contentType()))
                .contentLength(conteudo.sizeBytes())
                .body(new InputStreamResource(conteudo.content()));
    }

    @Operation(summary = "Apaga o anexo", description = "Apaga a linha e os BYTES no disco. Arquivo orfao com laudo dentro e dado pessoal nao apagado, que e o oposto do que um pedido de exclusao pede.")
    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID attachmentId) {
        attachmentService.delete(attachmentId);
        return ResponseEntity.noContent().build();
    }

}
