package br.com.petfy.healthcare.storage;

import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilesystemAttachmentStorageTest {

    @TempDir
    Path raiz;

    private static final UUID ANIMAL_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private FilesystemAttachmentStorage storage() {
        return new FilesystemAttachmentStorage(raiz.toString());
    }

    private InputStream conteudo(String texto) {
        return new ByteArrayInputStream(texto.getBytes(StandardCharsets.UTF_8));
    }

    @Nested
    @DisplayName("store")
    class Store {

        @Test
        @DisplayName("guarda o conteudo e devolve tamanho e checksum")
        void guardaConteudo() throws IOException {
            var armazenado = storage().store(ANIMAL_ID, conteudo("laudo"));

            assertThat(armazenado.sizeBytes()).isEqualTo(5);
            // SHA-256 em hexadecimal minusculo: 64 caracteres. O valor exato nao e
            // fixado aqui de proposito - o que importa e o formato e o determinismo, e o
            // determinismo esta em doisUploadsNaoColidem
            assertThat(armazenado.checksumSha256()).matches("[0-9a-f]{64}");
            assertThat(Files.readString(raiz.resolve(armazenado.storageKey()))).isEqualTo("laudo");
        }

        /**
         * O tamanho vem do que foi escrito, e nao de um cabecalho: {@code Content-Length}
         * e informado pelo cliente e nao precisa corresponder ao corpo.
         */
        @Test
        @DisplayName("o tamanho e o do que foi realmente escrito")
        void tamanhoEODoQueFoiEscrito() {
            var armazenado = storage().store(ANIMAL_ID, conteudo("1234567890"));

            assertThat(armazenado.sizeBytes()).isEqualTo(10);
        }

        /**
         * A chave e gerada pelo servidor. Nada do nome que o cliente enviou entra nela -
         * e por isso que o {@code store} nem recebe o nome do arquivo.
         */
        @Test
        @DisplayName("a chave vive sob o animal e nao carrega nome de cliente")
        void chaveEGeradaPeloServidor() {
            var armazenado = storage().store(ANIMAL_ID, conteudo("x"));

            assertThat(armazenado.storageKey()).startsWith("animals/" + ANIMAL_ID + "/");
            assertThat(armazenado.storageKey()).doesNotContain("..");
        }

        /**
         * Dois uploads do mesmo {@code exame.pdf} nao podem se sobrescrever - o segundo
         * apagaria o laudo do primeiro sem ninguem notar.
         */
        @Test
        @DisplayName("dois uploads iguais nao colidem")
        void doisUploadsNaoColidem() {
            var primeiro = storage().store(ANIMAL_ID, conteudo("mesmo conteudo"));
            var segundo = storage().store(ANIMAL_ID, conteudo("mesmo conteudo"));

            assertThat(primeiro.storageKey()).isNotEqualTo(segundo.storageKey());
            // conteudo igual, checksum igual: e o que permite reconhecer reenvio
            assertThat(primeiro.checksumSha256()).isEqualTo(segundo.checksumSha256());
        }

        /**
         * Grava em temporario e move no fim: escrita direta no destino deixaria arquivo
         * pela metade se a conexao caisse, e o banco - gravado depois - nao saberia.
         */
        @Test
        @DisplayName("nao deixa arquivo temporario para tras")
        void naoDeixaTemporario() throws IOException {
            var armazenado = storage().store(ANIMAL_ID, conteudo("laudo"));

            try (var arquivos = Files.list(raiz.resolve(armazenado.storageKey()).getParent())) {
                assertThat(arquivos).allSatisfy(caminho ->
                        assertThat(caminho.getFileName().toString()).doesNotContain(".part"));
            }
        }
    }

    @Nested
    @DisplayName("read")
    class Read {

        @Test
        @DisplayName("devolve o mesmo byte que entrou")
        void devolveOMesmoConteudo() throws IOException {
            var storage = storage();
            var armazenado = storage.store(ANIMAL_ID, conteudo("resultado do exame"));

            try (InputStream lido = storage.read(armazenado.storageKey())) {
                assertThat(new String(lido.readAllBytes(), StandardCharsets.UTF_8))
                        .isEqualTo("resultado do exame");
            }
        }

        @Test
        @DisplayName("chave inexistente responde erro de storage, e nao NullPointer")
        void chaveInexistenteFalhaLimpo() {
            assertThatThrownBy(() -> storage().read("animals/" + ANIMAL_ID + "/" + UUID.randomUUID()))
                    .isInstanceOf(PetfyHealthcareException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("apaga o arquivo")
        void apagaOArquivo() {
            var storage = storage();
            var armazenado = storage.store(ANIMAL_ID, conteudo("laudo"));

            storage.delete(List.of(armazenado.storageKey()));

            assertThat(raiz.resolve(armazenado.storageKey())).doesNotExist();
        }

        /**
         * Idempotente por necessidade: sem isso, uma exclusao de conta que falhasse no
         * meio nao poderia ser reanimalida - a segunda tentativa quebraria no arquivo que a
         * primeira ja tinha apagado.
         */
        @Test
        @DisplayName("apagar duas vezes nao e erro")
        void apagarDuasVezesNaoEErro() {
            var storage = storage();
            var armazenado = storage.store(ANIMAL_ID, conteudo("laudo"));

            storage.delete(List.of(armazenado.storageKey()));
            storage.delete(List.of(armazenado.storageKey()));

            assertThat(raiz.resolve(armazenado.storageKey())).doesNotExist();
        }

        @Test
        @DisplayName("lista vazia nao faz nada")
        void listaVaziaNaoFazNada() {
            storage().delete(List.of());
        }

        @Test
        @DisplayName("apaga varias chaves de uma vez")
        void apagaVariasChaves() {
            var storage = storage();
            var a = storage.store(ANIMAL_ID, conteudo("a"));
            var b = storage.store(ANIMAL_ID, conteudo("b"));

            storage.delete(List.of(a.storageKey(), b.storageKey()));

            assertThat(raiz.resolve(a.storageKey())).doesNotExist();
            assertThat(raiz.resolve(b.storageKey())).doesNotExist();
        }
    }

    /**
     * As chaves sao geradas pelo storage, entao hoje nenhuma consegue escapar da raiz. A
     * checagem existe porque isso e propriedade do codigo atual e nao da interface: no
     * download a chave vem do banco, e o que a torna segura passa a ser esta validacao.
     */
    @Nested
    @DisplayName("travessia de diretorio")
    class Travessia {

        @Test
        @DisplayName("chave com .. nao le fora da raiz")
        void naoLeForaDaRaiz() throws IOException {
            Path fora = raiz.getParent().resolve("segredo.txt");
            Files.writeString(fora, "conteudo de fora");

            assertThatThrownBy(() -> storage().read("../segredo.txt"))
                    .isInstanceOf(PetfyHealthcareException.class);

            // o arquivo de fora continua intacto
            assertThat(Files.readString(fora)).isEqualTo("conteudo de fora");
        }

        @Test
        @DisplayName("chave com .. nao apaga fora da raiz")
        void naoApagaForaDaRaiz() throws IOException {
            Path fora = raiz.getParent().resolve("nao-apague.txt");
            Files.writeString(fora, "importante");

            assertThatThrownBy(() -> storage().delete(List.of("../nao-apague.txt")))
                    .isInstanceOf(PetfyHealthcareException.class);

            assertThat(fora).exists();
        }

        @Test
        @DisplayName("caminho absoluto tambem e recusado")
        void caminhoAbsolutoERecusado() {
            assertThatThrownBy(() -> storage().read("/etc/passwd"))
                    .isInstanceOf(PetfyHealthcareException.class);
        }
    }

}
