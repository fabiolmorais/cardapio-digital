import { api } from "@/lib/api/client";
import { messages } from "@/messages/pt-BR";

const t = messages.passwordReset;

/** O erro com o status da resposta (o 422 é o link vencido ou já usado). */
export class PasswordResetError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
  }
}

/** O 400 (campos, senha fraca), o 422 (link vencido) e o 429 (limite da rede) trazem a frase pronta. */
function failure(response: Response, error: unknown): PasswordResetError {
  const problem = error as { detail?: string } | undefined;
  const ready = [400, 422, 429].includes(response.status) && problem?.detail;
  return new PasswordResetError(ready ? problem.detail! : t.errors.generic, response.status);
}

export async function requestReset(email: string): Promise<void> {
  const { error, response } = await api.POST("/api/v1/auth/password-reset", { body: { email } });
  if (!response.ok) throw failure(response, error);
}

export async function confirmReset(token: string, newPassword: string): Promise<void> {
  const { error, response } = await api.POST("/api/v1/auth/password-reset/confirm", { body: { token, newPassword } });
  if (!response.ok) throw failure(response, error);
}
