import Image from "next/image";
import Link from "next/link";
import { messages } from "@/messages/pt-BR";
import icon from "../../../public/brand/ohchefe-icone.png";

/** O logo do OhChefe: a mão levantada chamando e o nome, com o "Oh" na cor da marca. */
export function BrandMark({ size = 36 }: { size?: number }) {
  return (
    <Link href="/" aria-label={messages.home.brandHome} className="flex items-center gap-2.5 text-foreground">
      <Image src={icon} alt="" width={size} height={size} priority />
      <span className="text-[22px] font-extrabold tracking-[-0.03em]">
        <span className="text-brand-text">Oh</span>Chefe
      </span>
    </Link>
  );
}
