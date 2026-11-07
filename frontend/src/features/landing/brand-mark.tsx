import Image from "next/image";
import Link from "next/link";
import { messages } from "@/messages/pt-BR";
import { SectionLink } from "./section-link";
import icon from "../../../public/brand/ohchefe-icone.png";

/**
 * O logo do OhChefe: a mão levantada chamando e o nome, com o "Oh" na cor da marca. Leva à página
 * inicial; na própria página inicial (onLanding), rola até o topo.
 */
export function BrandMark({ size = 36, onLanding = false }: { size?: number; onLanding?: boolean }) {
  const mark = (
    <>
      <Image src={icon} alt="" width={size} height={size} priority />
      <span className="text-[22px] font-extrabold tracking-[-0.03em]">
        <span className="text-brand-text">Oh</span>Chefe
      </span>
    </>
  );
  const props = { "aria-label": messages.home.brandHome, className: "flex items-center gap-2.5 text-foreground" };
  return onLanding ? (
    <SectionLink section="topo" {...props}>
      {mark}
    </SectionLink>
  ) : (
    <Link href="/" {...props}>
      {mark}
    </Link>
  );
}
