import { CircleAlert, Info, TriangleAlert } from "lucide-react";
import type { ReactNode } from "react";
import { cn } from "cn";

/** As cores dos avisos da mesa, como no desenho: azul = informativo, amarelo = atenção, vermelho = importante. */
const tones = {
  info: { box: "border-(--mesa-info)/30 bg-(--mesa-info-soft)", icon: "text-(--mesa-info)", Icon: Info },
  warning: { box: "border-(--mesa-warn)/30 bg-(--mesa-warn-soft)", icon: "text-(--mesa-warn)", Icon: TriangleAlert },
  danger: { box: "border-(--mesa-danger)/30 bg-(--mesa-danger-soft)", icon: "text-(--mesa-danger)", Icon: CircleAlert },
};

/** Um aviso em caixa na página da mesa (as cores seguem o tema escuro ou claro do restaurante). */
export function MesaNotice({
  tone,
  role,
  children,
}: {
  tone: keyof typeof tones;
  role?: "status" | "alert";
  children: ReactNode;
}) {
  const { box, icon, Icon } = tones[tone];
  return (
    <p role={role} className={cn("flex items-start gap-3 rounded-2xl border p-4 text-sm", box)}>
      <Icon aria-hidden className={cn("mt-0.5 size-4 shrink-0", icon)} />
      <span>{children}</span>
    </p>
  );
}
