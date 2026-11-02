import { messages } from "@/messages/pt-BR";

/** Como o back mandou enviar o arquivo: a URL assinada, o método e os cabeçalhos. */
export type SignedUpload = { url: string; method: string; headers: Record<string, string> };

/**
 * Envia o arquivo direto ao armazenamento (R2), pela URL assinada, sem passar pelo back.
 * XMLHttpRequest, e não fetch, porque só ele informa o progresso do envio (a barra que a
 * pessoa vê no 4G). Serve para as fotos do cardápio e para a logo.
 */
export function sendToStorage(
  upload: SignedUpload,
  file: File,
  onProgress: (fraction: number) => void = () => {},
): Promise<void> {
  return new Promise((resolve, reject) => {
    const request = new XMLHttpRequest();
    request.open(upload.method, upload.url);
    for (const [name, value] of Object.entries(upload.headers)) request.setRequestHeader(name, value);
    request.upload.onprogress = (event) => {
      if (event.lengthComputable) onProgress(event.loaded / event.total);
    };
    request.onload = () =>
      request.status >= 200 && request.status < 300 ? resolve() : reject(new Error(messages.common.sendFailed));
    request.onerror = () => reject(new Error(messages.common.sendFailed));
    request.send(file);
  });
}
