// O service worker do app da equipe (Guia 29): só recebe os avisos do web push e abre o painel
// no toque. Não guarda páginas nem dados (sem cache): a equipe sempre vê o que está no servidor.

self.addEventListener("install", () => self.skipWaiting());
self.addEventListener("activate", (event) => event.waitUntil(self.clients.claim()));

self.addEventListener("push", (event) => {
  let message = {};
  try {
    message = event.data ? event.data.json() : {};
  } catch {
    // Mensagem fora do formato: ainda avisa, com o texto padrão.
  }
  const title = message.title || "Cardápio Digital";
  event.waitUntil(
    self.registration.showNotification(title, {
      body: message.body || "",
      // A mesma tag troca o aviso antigo pelo novo, em vez de empilhar.
      tag: message.tag,
      renotify: Boolean(message.tag),
      icon: "/icons/equipe-192.png",
      badge: "/icons/equipe-badge-96.png",
      data: { url: sameOrigin(message.url) },
    }),
  );
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const url = new URL(event.notification.data?.url || "/painel", self.location.origin);
  event.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((windows) => {
      // Já tem o painel aberto: traz para a frente. Senão, abre.
      const open = windows.find((w) => new URL(w.url).pathname === url.pathname);
      if (open) return open.focus();
      return self.clients.openWindow(url.href);
    }),
  );
});

/** Só abre páginas deste site: o endereço do aviso nunca leva para fora. */
function sameOrigin(url) {
  try {
    const parsed = new URL(url || "/painel", self.location.origin);
    return parsed.origin === self.location.origin ? parsed.pathname + parsed.search : "/painel";
  } catch {
    return "/painel";
  }
}
