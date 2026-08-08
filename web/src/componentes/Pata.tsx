/**
 * A marca neutra do animal, no lugar onde um dia entra a foto dele.
 *
 * <b>Nao e mascote e nao e emoji</b> — a secao 1 proibe os dois. E uma silhueta que ocupa
 * a posicao ate existir foto, e a foto e sempre de um animal real do usuario: cachorro
 * sorridente de banco de imagem e a mentira visual mais barata que existe.
 *
 * `aria-hidden` porque o nome do animal ja esta no texto ao lado. Silhueta anunciada por
 * leitor de tela e ruido, nao informacao.
 */
export function Pata({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 48 48" fill="currentColor" aria-hidden="true" className={className}>
      <ellipse cx="13" cy="19" rx="4.6" ry="6" />
      <ellipse cx="21" cy="14" rx="4.8" ry="6.4" />
      <ellipse cx="29.5" cy="14" rx="4.8" ry="6.4" />
      <ellipse cx="37.5" cy="19" rx="4.6" ry="6" />
      <path d="M24 24c6.5 0 12 5 12 10.5C36 39.5 31 42 24 42s-12-2.5-12-7.5C12 29 17.5 24 24 24z" />
    </svg>
  );
}
