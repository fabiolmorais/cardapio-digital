import { api } from "@/lib/api/client";

/**
 * Por onde a pessoa entrou nesta aba: e-mail e senha (/entrar) ou PIN no aparelho (/aparelho).
 * Ao sair ou quando a sessão acaba, ela volta para a mesma porta: o garçom volta para o
 * teclado do PIN, e não para a tela de e-mail. Fica no sessionStorage (só esta aba) e não
 * guarda nada sensível.
 */
const KEY = "cardapio:login-origin";

export type LoginOrigin = "password" | "device";

export function setLoginOrigin(origin: LoginOrigin) {
  try {
    sessionStorage.setItem(KEY, origin);
  } catch {
    // Navegação privada ou armazenamento bloqueado: fica a pergunta ao back.
  }
}

function storedOrigin(): LoginOrigin | null {
  try {
    const origin = sessionStorage.getItem(KEY);
    return origin === "device" || origin === "password" ? origin : null;
  } catch {
    return null;
  }
}

/** O aparelho foi vinculado ao restaurante (o cookie HttpOnly do aparelho, que a página não lê). */
async function isLinkedDevice(): Promise<boolean> {
  try {
    const { response } = await api.GET("/api/v1/auth/device");
    return response.ok;
  } catch {
    return false;
  }
}

/**
 * A tela de entrar desta pessoa. Sem a marca da aba (o app da equipe aberto pelo ícone, uma aba
 * nova), pergunta ao back: aparelho vinculado vai para o PIN; os outros, para o e-mail e a senha.
 */
export async function loginPath(): Promise<"/aparelho" | "/entrar"> {
  const origin = storedOrigin() ?? ((await isLinkedDevice()) ? "device" : "password");
  return origin === "device" ? "/aparelho" : "/entrar";
}
