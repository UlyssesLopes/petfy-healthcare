package br.com.petfy.healthcare.service;

import java.io.InputStream;

/**
 * O conteudo de um anexo, pronto para ser escrito na resposta.
 *
 * Carrega {@code InputStream} e nao {@code byte[]} de proposito: um laudo de 10 MB em
 * array vai inteiro para a heap, e dez downloads simultaneos fazem 100 MB. Em stream, o
 * arquivo passa do storage para a resposta sem nunca existir todo na memoria.
 *
 * <b>Quem consome fecha o stream.</b> O controller o entrega ao Spring, que fecha ao
 * terminar de escrever.
 */
public record AttachmentContent(String filename, String contentType, long sizeBytes, InputStream content) {
}
