import type { Metadata, Viewport } from "next";

/**
 * O que faz as telas da equipe (painel e gestão) virarem app: o manifesto com início no /painel,
 * o ícone do iPhone e a cor da barra. Só os layouts da equipe usam: a página da mesa não tem
 * manifesto e nunca vira app.
 */
export const staffAppMetadata: Metadata = {
  manifest: "/equipe.webmanifest",
  appleWebApp: { capable: true, title: "Equipe", statusBarStyle: "black-translucent" },
  icons: { apple: "/icons/equipe-apple-180.png" },
};

export const staffAppViewport: Viewport = { themeColor: "#0e0c0b" };
