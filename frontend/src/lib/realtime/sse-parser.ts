/** Um aviso do servidor: o nome (event:) e o conteúdo (data:), já como objeto. */
export type ServerEvent = { name: string; data: Record<string, string> };

/**
 * Lê o formato SSE aos pedaços, do jeito que chega pela rede: um aviso termina numa linha em
 * branco, e um pedaço pode cortar um aviso no meio (o resto fica guardado para o próximo).
 * Linhas que começam com ":" são comentários (o "ping" do servidor) e são ignoradas.
 */
export function createSseParser(onEvent: (event: ServerEvent) => void) {
  let buffer = "";
  return (chunk: string) => {
    buffer += chunk.replace(/\r\n?/g, "\n");
    let end: number;
    while ((end = buffer.indexOf("\n\n")) >= 0) {
      const block = buffer.slice(0, end);
      buffer = buffer.slice(end + 2);
      let name = "message";
      const data: string[] = [];
      for (const line of block.split("\n")) {
        if (line.startsWith(":")) continue;
        const colon = line.indexOf(":");
        const field = colon < 0 ? line : line.slice(0, colon);
        const value = colon < 0 ? "" : line.slice(colon + 1).replace(/^ /, "");
        if (field === "event") name = value;
        else if (field === "data") data.push(value);
      }
      if (data.length === 0) continue;
      try {
        onEvent({ name, data: JSON.parse(data.join("\n")) });
      } catch {
        // Aviso fora do formato esperado: ignora (a tela busca o estado de novo no próximo).
      }
    }
  };
}
