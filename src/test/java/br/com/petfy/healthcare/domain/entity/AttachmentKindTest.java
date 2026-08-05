package br.com.petfy.healthcare.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O reconhecimento de tipo por conteudo.
 *
 * E a peca de seguranca do anexo: o {@code Content-Type} do upload vem do cliente, e um
 * executavel renomeado para {@code .pdf} chega anunciado como PDF. Se a validacao
 * confiasse no que foi declarado, o storage guardaria qualquer coisa e o download a
 * devolveria com o cabecalho que o atacante escolheu.
 */
class AttachmentKindTest {

    /** Concatena assinatura e recheio, para simular o comeco de um arquivo real. */
    private byte[] arquivo(int... bytes) {
        byte[] conteudo = new byte[Math.max(bytes.length, AttachmentKind.bytesNecessarios())];

        for (int i = 0; i < bytes.length; i++) {
            conteudo[i] = (byte) bytes[i];
        }

        return conteudo;
    }

    @Nested
    @DisplayName("formatos aceitos")
    class FormatosAceitos {

        @Test
        @DisplayName("reconhece JPEG")
        void reconheceJpeg() {
            assertThat(AttachmentKind.detectar(arquivo(0xFF, 0xD8, 0xFF, 0xE0)))
                    .contains(AttachmentKind.JPEG);
        }

        @Test
        @DisplayName("reconhece PNG")
        void reconhecePng() {
            assertThat(AttachmentKind.detectar(arquivo(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)))
                    .contains(AttachmentKind.PNG);
        }

        @Test
        @DisplayName("reconhece PDF")
        void reconhecePdf() {
            assertThat(AttachmentKind.detectar("%PDF-1.7 resto".getBytes(StandardCharsets.US_ASCII)))
                    .contains(AttachmentKind.PDF);
        }

        @Test
        @DisplayName("reconhece WEBP, que tem assinatura em duas partes")
        void reconheceWebp() {
            // RIFF nos bytes 0-3, tamanho nos 4-7, WEBP nos 8-11
            byte[] webp = arquivo(0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50);

            assertThat(AttachmentKind.detectar(webp)).contains(AttachmentKind.WEBP);
        }

        @Test
        @DisplayName("o content type gravado e o do formato, nao o declarado no upload")
        void contentTypeVemDoFormato() {
            assertThat(AttachmentKind.JPEG.getContentType()).isEqualTo("image/jpeg");
            assertThat(AttachmentKind.PDF.getContentType()).isEqualTo("application/pdf");
        }
    }

    @Nested
    @DisplayName("o que tem de ser recusado")
    class Recusados {

        /**
         * O caso que justifica esta classe existir: o arquivo se anuncia como PDF pela
         * extensao e pelo Content-Type, e os bytes dizem outra coisa.
         */
        @Test
        @DisplayName("executavel Windows nao passa por PDF")
        void executavelNaoPassa() {
            // MZ, o cabecalho de PE/EXE
            assertThat(AttachmentKind.detectar(arquivo(0x4D, 0x5A, 0x90, 0x00))).isEmpty();
        }

        @Test
        @DisplayName("script em texto puro nao passa")
        void scriptNaoPassa() {
            assertThat(AttachmentKind.detectar("#!/bin/sh\nrm -rf /".getBytes(StandardCharsets.UTF_8)))
                    .isEmpty();
        }

        /**
         * SVG e imagem, mas e XML com script dentro: servido no dominio da API viraria
         * XSS. Fica fora da lista de proposito, e nao por esquecimento.
         */
        @Test
        @DisplayName("SVG nao entra, apesar de ser imagem")
        void svgNaoEntra() {
            assertThat(AttachmentKind.detectar("<svg xmlns=\"http://www.w3.org/2000/svg\">"
                    .getBytes(StandardCharsets.UTF_8))).isEmpty();
        }

        /**
         * RIFF sozinho tambem e WAV e AVI. Sem conferir o WEBP no byte 8, um audio
         * entraria como imagem.
         */
        @Test
        @DisplayName("RIFF que nao e WEBP nao passa por imagem")
        void riffQueNaoEWebpNaoPassa() {
            byte[] wav = arquivo(0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x41, 0x56, 0x45);

            assertThat(AttachmentKind.detectar(wav)).isEmpty();
        }

        @Test
        @DisplayName("ZIP nao passa, nem disfarcado de documento")
        void zipNaoPassa() {
            assertThat(AttachmentKind.detectar(arquivo(0x50, 0x4B, 0x03, 0x04))).isEmpty();
        }
    }

    @Nested
    @DisplayName("entradas degeneradas")
    class Degeneradas {

        @Test
        @DisplayName("nulo devolve vazio em vez de estourar")
        void nuloDevolveVazio() {
            assertThat(AttachmentKind.detectar(null)).isEmpty();
        }

        @Test
        @DisplayName("vazio devolve vazio")
        void vazioDevolveVazio() {
            assertThat(AttachmentKind.detectar(new byte[0])).isEmpty();
        }

        /**
         * Arquivo menor que a assinatura nao pode estourar indice: um upload de 2 bytes
         * derrubaria o endpoint com ArrayIndexOutOfBounds em vez de responder 415.
         */
        @Test
        @DisplayName("arquivo menor que a assinatura devolve vazio")
        void menorQueAssinaturaDevolveVazio() {
            assertThat(AttachmentKind.detectar(new byte[]{(byte) 0xFF})).isEmpty();
            assertThat(AttachmentKind.detectar(new byte[]{0x52, 0x49, 0x46, 0x46})).isEmpty();
        }

        /**
         * O PNG tem a assinatura mais longa (8 bytes) e o WEBP precisa chegar ao byte 11:
         * ler menos que isso faria o WEBP nunca ser reconhecido.
         */
        @Test
        @DisplayName("bastam 12 bytes para decidir qualquer formato aceito")
        void dozeBytesBastam() {
            assertThat(AttachmentKind.bytesNecessarios()).isEqualTo(12);
        }
    }

}
