/**
 * Por onde a pessoa entrou nesta aba: e-mail e senha (/entrar) ou PIN no aparelho (/aparelho).
 * Ao sair ou quando a sessão acaba, ela volta para a mesma porta: o garçom volta para o
 * teclado do PIN, e não para a tela de e-mail. Fica no sessionStorage (só esta aba) e não
 * guarda nada sensível; sem ele, o padrão é /entrar.
 */
const KEY = "cardapio:login-origin";

export type LoginOrigin = "password" | "device";

export function setLoginOrigin(origin: LoginOrigin) {
  try {
    sessionStorage.setItem(KEY, origin);
  } catch {
    // Navegação privada ou armazenamento bloqueado: fica o padrão.
  }
}

export function loginPath(): "/aparelho" | "/entrar" {
  try {
    return sessionStorage.getItem(KEY) === "device" ? "/aparelho" : "/entrar";
  } catch {
    return "/entrar";
  }
}
