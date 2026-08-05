package br.com.petfy.healthcare.storage;

/**
 * O que o storage devolve depois de guardar.
 *
 * O tamanho e o checksum sao calculados <b>durante</b> a gravacao, e nao lidos do
 * request: {@code Content-Length} vem do cliente, e o que interessa e o tamanho do que
 * de fato foi escrito. E o checksum e a unica forma de conferir depois que o byte que
 * voltou do storage e o mesmo que entrou.
 */
public record StoredFile(String storageKey, long sizeBytes, String checksumSha256) {
}
