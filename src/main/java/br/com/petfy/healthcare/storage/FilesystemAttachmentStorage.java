package br.com.petfy.healthcare.storage;

import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Guarda os anexos no disco da propria maquina.
 *
 * <b>Serve para {@code local} e {@code dev}, nao para producao com mais de uma
 * instancia:</b> disco local nao e compartilhado, entao duas instancias nao veem os
 * arquivos uma da outra. O ROADMAP registra "uma instancia, escala vertical" como
 * decisao vigente, o que torna isto suficiente por ora - e o dia em que deixar de ser,
 * a substituicao e uma classe nova implementando {@link AttachmentStorage}, sem
 * encostar em regra de negocio.
 *
 * A implementacao de S3/R2 nao entrou porque depende de bucket e credencial que ainda
 * nao existem. Deixar a interface pronta e a alternativa a esperar por eles.
 */
@Slf4j
@Component
public class FilesystemAttachmentStorage implements AttachmentStorage {

    private final Path raiz;

    public FilesystemAttachmentStorage(@Value("${petfy.attachments.filesystem.root}") String root) {
        this.raiz = Path.of(root).toAbsolutePath().normalize();
    }

    /**
     * A chave e {@code animals/{animalId}/{uuid}} - so valor gerado pelo servidor.
     *
     * O UUID novo, e nao o nome do arquivo, e o que impede duas coisas: travessia de
     * diretorio, porque nao ha string de terceiro no caminho; e colisao, porque dois
     * uploads do mesmo {@code exame.pdf} nao se sobrescrevem.
     *
     * Grava em arquivo temporario e move no fim. Escrita direta no destino final
     * deixaria arquivo pela metade no disco se a conexao caisse no meio do upload, e o
     * banco - que so e gravado depois - nao saberia que aquilo existe.
     */
    @Override
    public StoredFile store(UUID animalId, InputStream content) {
        String storageKey = "animals/" + animalId + "/" + UUID.randomUUID();
        Path destino = resolver(storageKey);

        try {
            Files.createDirectories(destino.getParent());

            Path temporario = Files.createTempFile(destino.getParent(), "upload-", ".part");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long tamanho;

            try (DigestInputStream comDigest = new DigestInputStream(content, digest)) {
                tamanho = Files.copy(comDigest, temporario, StandardCopyOption.REPLACE_EXISTING);
            }

            Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);

            return new StoredFile(storageKey, tamanho, HexFormat.of().formatHex(digest.digest()));

        } catch (IOException | NoSuchAlgorithmException e) {
            throw falhaDeStorage("gravar", storageKey, e);
        }
    }

    @Override
    public InputStream read(String storageKey) {
        Path caminho = resolver(storageKey);

        try {
            return Files.newInputStream(caminho);
        } catch (IOException e) {
            throw falhaDeStorage("ler", storageKey, e);
        }
    }

    @Override
    public void delete(Collection<String> storageKeys) {
        for (String storageKey : storageKeys) {
            try {
                // deleteIfExists, e nao delete: chave ausente nao e erro, senao uma
                // exclusao que falhou no meio nao poderia ser repetida
                Files.deleteIfExists(resolver(storageKey));
            } catch (IOException e) {
                throw falhaDeStorage("apagar", storageKey, e);
            }
        }
    }

    /**
     * Resolve a chave dentro da raiz e <b>confere que nao escapou dela</b>.
     *
     * As chaves sao geradas aqui, entao hoje nenhuma consegue escapar. A checagem
     * existe porque isso e uma propriedade do codigo atual, nao da interface: no dia em
     * que uma chave vier do banco - e ela vem, no download - o que garante que ela e
     * segura passa a ser esta linha, e nao a memoria de quem a escreveu.
     */
    private Path resolver(String storageKey) {
        Path resolvido = raiz.resolve(storageKey).normalize();

        if (!resolvido.startsWith(raiz)) {
            log.error("Chave de storage tentou escapar da raiz: {}", storageKey);
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.ATTACHMENT_NOT_FOUND.getMessage(),
                    ErrorMessageEnum.ATTACHMENT_NOT_FOUND.getCode(),
                    HttpStatus.NOT_FOUND);
        }

        return resolvido;
    }

    private PetfyHealthcareException falhaDeStorage(String operacao, String storageKey, Exception causa) {
        log.error("Falha ao {} o anexo {}", operacao, storageKey, causa);

        return new PetfyHealthcareException(
                ErrorMessageEnum.ATTACHMENT_STORAGE_FAILURE.getMessage(),
                ErrorMessageEnum.ATTACHMENT_STORAGE_FAILURE.getCode(),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
