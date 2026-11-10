import { CircleAlert, Info, TriangleAlert } from "lucide-react";
import type { ReactNode } from "react";
import { cn } from "cn";

/** As cores dos avisos, como no desenho: azul = informativo, amarelo = atenção, vermelho = importante. */
const tones = {
  info: { box: "border-info/30 bg-info/10", icon: "text-info", Icon: Info },
  warning: { box: "border-warning/30 bg-warning/10", icon: "text-warning", Icon: TriangleAlert },
  danger: { box: "border-destructive/30 bg-destructive/10", icon: "text-destructive", Icon: CircleAlert },
};

export type NoticeTone = keyof typeof tones;

/** Um aviso em caixa no painel: o ícone e a cor dizem o peso; o texto fica na cor normal, para ler bem. */
export function Notice({
  tone,
  role,
  className,
  children,
}: {
  tone: NoticeTone;
  role?: "status" | "alert";
  className?: string;
  children: ReactNode;
}) {
  const { box, icon, Icon } = tones[tone];
  return (
    <p role={role} className={cn("flex items-start gap-2.5 rounded-xl border px-4 py-3 text-sm", box, className)}>
      <Icon aria-hidden className={cn("mt-0.5 size-4 shrink-0", icon)} />
      <span>{children}</span>
    </p>
  );
}
