import tailwindcss from "@tailwindcss/vite";
import { tanstackRouter } from "@tanstack/router-plugin/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

// A porta é a mesma que o perfil `local` do backend libera no CORS. Trocar aqui
// sem trocar lá faz a primeira requisição da primeira tela falhar no navegador.
export default defineConfig({
  plugins: [
    // Antes do react(), como o plugin exige: ele reescreve os arquivos de rota, e
    // precisa fazê-lo antes de o JSX ser transformado.
    tanstackRouter({
      target: "react",
      routesDirectory: "src/rotas",
      // Fora de `routesDirectory` de proposito: dentro dela o plugin examina o proprio
      // arquivo gerado como se fosse uma rota, e avisa a cada build.
      generatedRouteTree: "src/arvore-de-rotas.gen.ts",
      autoCodeSplitting: true,
    }),
    react(),
    tailwindcss(),
  ],
  server: {
    port: 5173,
    strictPort: true,
  },
  test: {
    environment: "node",
    include: ["src/**/*.test.ts", "src/**/*.test.tsx"],
  },
});
