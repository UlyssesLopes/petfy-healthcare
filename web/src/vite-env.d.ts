/// <reference types="vite/client" />

interface ImportMetaEnv {
  /**
   * A base da API. Sem ela, vale `http://localhost:8080`, que e a origem que o perfil
   * `local` do backend libera no CORS.
   *
   * O contrato versionado nao tem no `servers`: ele descreve a forma da API, e nao onde
   * ela esta. Cada ambiente informa a sua aqui.
   */
  readonly VITE_API_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
