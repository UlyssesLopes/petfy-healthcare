package br.com.petfy.healthcare.domain.entity;

import java.util.Arrays;
import java.util.Optional;

/**
 * Os tipos de arquivo aceitos, reconhecidos <b>pelo conteudo</b>.
 *
 * O {@code Content-Type} do upload vem do cliente e nao vale como verdade: um
 * executavel renomeado para {@code .pdf} chega anunciado como PDF e passaria por
 * qualquer checagem de extensao ou de cabecalho. Entao a deteccao le os primeiros
 * bytes, que sao o que o formato de fato tem.
 *
 * A lista e curta de proposito. Anexo de saude e imagem ou PDF - carteirinha, laudo,
 * exame. Aceitar mais formatos aumenta a superficie de ataque do storage e do
 * visualizador do cliente sem atender caso de uso nenhum que exista hoje.
 */
public enum AttachmentKind {

    JPEG("image/jpeg", new int[]{0xFF, 0xD8, 0xFF}),

    PNG("image/png", new int[]{0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}),

    PDF("application/pdf", new int[]{0x25, 0x50, 0x44, 0x46, 0x2D}),

    /**
     * WEBP nao tem assinatura contigua: e {@code RIFF} nos bytes 0-3 e {@code WEBP}
     * nos bytes 8-11, com o tamanho do arquivo entre eles. Por isso o reconhecimento
     * dele tem tratamento proprio em {@link #detectar(byte[])}.
     */
    WEBP("image/webp", new int[]{0x52, 0x49, 0x46, 0x46});

    private final String contentType;
    private final int[] assinatura;

    AttachmentKind(String contentType, int[] assinatura) {
        this.contentType = contentType;
        this.assinatura = assinatura;
    }

    public String getContentType() {
        return contentType;
    }

    /**
     * Quantos bytes bastam para decidir. O WEBP e o maior caso: precisa chegar ao
     * byte 11.
     */
    public static int bytesNecessarios() {
        return 12;
    }

    /**
     * O tipo do conteudo, ou vazio se nao for nenhum dos aceitos.
     *
     * Vazio significa recusar o upload. Nao ha tentativa de adivinhar por extensao
     * como desempate: se os bytes nao dizem o que o arquivo e, aceita-lo seria
     * guardar conteudo desconhecido apostando na boa vontade de quem enviou.
     */
    public static Optional<AttachmentKind> detectar(byte[] inicio) {
        if (inicio == null) {
            return Optional.empty();
        }

        return Arrays.stream(values())
                .filter(kind -> kind.combina(inicio))
                .findFirst();
    }

    private boolean combina(byte[] inicio) {
        if (!prefixoCombina(inicio, assinatura, 0)) {
            return false;
        }

        // RIFF sozinho tambem e WAV e AVI: sem conferir o WEBP no byte 8 um audio
        // entraria como imagem
        return this != WEBP || prefixoCombina(inicio, new int[]{0x57, 0x45, 0x42, 0x50}, 8);
    }

    private boolean prefixoCombina(byte[] inicio, int[] esperado, int offset) {
        if (inicio.length < offset + esperado.length) {
            return false;
        }

        for (int i = 0; i < esperado.length; i++) {
            if ((inicio[offset + i] & 0xFF) != esperado[i]) {
                return false;
            }
        }

        return true;
    }

}
