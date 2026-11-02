import { initials } from "@/lib/initials";
import { cn } from "cn";

/**
 * A logo do restaurante, ou as iniciais num círculo na cor da marca quando não há logo.
 * alt vazio: quando o nome já está escrito ao lado (o leitor de tela não precisa ouvir duas vezes).
 */
export function BrandLogo({
  logo,
  name,
  color,
  alt,
  className,
}: {
  logo?: string;
  name: string;
  color: string;
  alt: string;
  className: string;
}) {
  if (logo) {
    return (
      // eslint-disable-next-line @next/next/no-img-element -- logo já no tamanho certo, vinda do R2
      <img src={logo} alt={alt} className={cn("shrink-0 rounded-full object-contain", className)} />
    );
  }
  return (
    <span
      aria-hidden
      className={cn("flex shrink-0 items-center justify-center rounded-full font-bold text-white", className)}
      style={{ backgroundColor: color }}
    >
      {initials(name)}
    </span>
  );
}
