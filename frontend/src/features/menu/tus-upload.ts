/**
 * O envio de vídeo pelo protocolo TUS (https://tus.io), escrito à mão: é o mínimo que o serviço de
 * vídeo pede, e não vale uma biblioteca. O arquivo vai em pedaços; se a rede cair no meio, o
 * envio pergunta ao servidor até onde chegou (HEAD) e continua dali, em vez de recomeçar.
 */

const VERSION = "1.0.0";
/** 5 MB por pedaço: grande o bastante para não perder tempo em idas e voltas, pequeno para retomar. */
const CHUNK = 5 * 1024 * 1024;
/** Quantas vezes seguidas tenta de novo um pedaço que falhou (com espera crescente). */
const RETRIES = 4;

export type TusTicket = { endpoint: string; headers: Record<string, string> };

function metadata(entries: Record<string, string>) {
  // TUS: "chave base64, chave base64". O btoa só aceita latim-1: o texto passa por UTF-8 antes.
  const b64 = (text: string) => btoa(String.fromCharCode(...new TextEncoder().encode(text)));
  return Object.entries(entries)
    .map(([key, value]) => `${key} ${b64(value)}`)
    .join(",");
}

/** O serviço de vídeo recusou um passo do envio: o código HTTP e o motivo que ele mandou. */
export class TusError extends Error {
  constructor(
    readonly status: number,
    readonly detail: string,
  ) {
    super(detail ? `${status}: ${detail}` : String(status));
  }
}

async function refused(response: Response): Promise<TusError> {
  const text = await response.text().catch(() => "");
  return new TusError(response.status, text.replace(/\s+/g, " ").trim().slice(0, 200));
}

const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

/** Cria o envio (POST) com a autorização que o back deu, e devolve o endereço dele. */
async function create(ticket: TusTicket, file: File): Promise<string> {
  const response = await fetch(ticket.endpoint, {
    method: "POST",
    headers: {
      ...ticket.headers,
      "Tus-Resumable": VERSION,
      "Upload-Length": String(file.size),
      "Upload-Metadata": metadata({ filetype: file.type, title: file.name }),
    },
  });
  const location = response.headers.get("Location");
  if (response.status !== 201 || !location) throw await refused(response);
  // O Location pode vir relativo ("/tus/…"): vale a partir do endpoint.
  return new URL(location, ticket.endpoint).toString();
}

/** Até onde o servidor já recebeu (para retomar). */
async function offsetOf(url: string): Promise<number> {
  const response = await fetch(url, { method: "HEAD", headers: { "Tus-Resumable": VERSION } });
  const offset = Number(response.headers.get("Upload-Offset"));
  if (!response.ok || Number.isNaN(offset)) throw new Error(String(response.status));
  return offset;
}

/**
 * Envia o arquivo inteiro. `onProgress` recebe a fração já recebida pelo servidor (0 a 1), a
 * cada pedaço.
 */
export async function tusUpload(ticket: TusTicket, file: File, onProgress: (fraction: number) => void) {
  const url = await create(ticket, file);
  let offset = 0;
  let failures = 0;
  onProgress(0);
  while (offset < file.size) {
    try {
      const response = await fetch(url, {
        method: "PATCH",
        headers: {
          "Tus-Resumable": VERSION,
          "Upload-Offset": String(offset),
          "Content-Type": "application/offset+octet-stream",
        },
        body: file.slice(offset, offset + CHUNK),
      });
      if (response.status === 409) {
        // Fora de ordem (um pedaço chegou e a resposta se perdeu): pergunta e segue de lá.
        offset = await offsetOf(url);
        continue;
      }
      const next = Number(response.headers.get("Upload-Offset"));
      if (!response.ok || Number.isNaN(next)) throw await refused(response);
      offset = next;
      failures = 0;
      onProgress(offset / file.size);
    } catch (error) {
      // Recusa do serviço (4xx): tentar de novo dá o mesmo resultado. Só rede e 5xx repetem.
      if (error instanceof TusError && error.status < 500) throw error;
      failures++;
      if (failures > RETRIES) throw error;
      await wait(1000 * 2 ** failures);
      offset = await offsetOf(url).catch(() => offset);
    }
  }
}
