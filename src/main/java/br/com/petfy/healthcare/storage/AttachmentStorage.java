package br.com.petfy.healthcare.storage;

import java.io.InputStream;
import java.util.Collection;
import java.util.UUID;

/**
 * Onde os bytes do anexo ficam.
 *
 * <b>Nao ha metodo que devolva URL.</b> A escolha e deliberada: URL assinada e o
 * padrao para arquivo publico, mas para dado de saude ela troca "checar autorizacao a
 * cada download" por "quem tiver o link entra ate expirar" - e link encaminhado por
 * engano e exatamente o caso que este produto passou o passo 8 inteiro tentando
 * evitar. O download passa pela API, pelo {@code PetAccessGuard} e pelo log de acesso.
 *
 * O efeito colateral e bom: sem URL assinada, a interface e um armazem de bytes puro,
 * e trocar filesystem por S3 ou R2 nao encosta em regra de negocio nenhuma.
 */
public interface AttachmentStorage {

    /**
     * Guarda o conteudo e devolve onde ficou.
     *
     * A chave e gerada aqui, a partir do petId e de um UUID novo - nada do nome que o
     * cliente enviou entra nela.
     */
    StoredFile store(UUID petId, InputStream content);

    /** Abre o conteudo para leitura. Lanca se a chave nao existir. */
    InputStream read(String storageKey);

    /**
     * Apaga os arquivos das chaves informadas.
     *
     * <b>Idempotente:</b> chave que nao existe mais nao e erro. Sem isso, uma exclusao
     * de conta que falhasse no meio nao poderia ser repetida - a segunda tentativa
     * quebraria no arquivo que a primeira ja tinha apagado.
     */
    void delete(Collection<String> storageKeys);

}
