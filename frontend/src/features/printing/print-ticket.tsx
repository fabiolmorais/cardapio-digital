import { messages } from "@/messages/pt-BR";
import type { PrintJob } from "./print-api";

const t = messages.panel.printing.ticket;

const time = new Intl.DateTimeFormat("pt-BR", {
  hour: "2-digit",
  minute: "2-digit",
});

/** O que a estação imprime: um pedido, a via "CANCELADO", a reimpressão ou o teste da impressora. */
export type Ticket = {
  job: PrintJob;
  variant: "new" | "reprint" | "test";
  printedAt: Date;
};

/**
 * A comanda no papel térmico (80 ou 58 mm): preto no branco, letras grandes, a mesa em destaque
 * e um item por linha, com a observação logo abaixo. Só aparece na impressão.
 */
export function PrintTicket({ ticket, restaurant }: { ticket: Ticket; restaurant: string }) {
  const { job, variant, printedAt } = ticket;
  const canceled = job.kind === "CANCELLATION";
  const orderedAt = job.orderedAt ? new Date(job.orderedAt) : printedAt;
  return (
    <article data-print-ticket className="flex flex-col gap-1 bg-white font-mono text-[13px] leading-tight text-black">
      <p className="text-center text-[12px]">{restaurant}</p>
      {variant === "test" && <p className="text-center text-[15px] font-bold">{t.test}</p>}
      {variant === "reprint" && <p className="text-center text-[15px] font-bold">{t.reprint}</p>}
      {canceled && <p className="text-center text-[17px] font-bold">{t.canceled}</p>}
      <p className="text-center text-[26px] font-bold">{t.table(job.tableLabel)}</p>
      <p className="text-center">{job.sectorName}</p>
      <p className="text-center">{t.order(job.orderNumber, time.format(orderedAt))}</p>
      <hr className="my-1 border-t border-dashed border-black" />
      <ul className="flex flex-col gap-1.5">
        {job.items.map((item) => (
          <li key={item.id} className="flex flex-col">
            <span className={canceled ? "text-[17px] font-bold line-through" : "text-[17px] font-bold"}>
              {item.quantity}x {item.productName}
              {item.rodizio && ` (${t.rodizio})`}
            </span>
            {item.note && <span className="pl-3">{t.note(item.note)}</span>}
            {canceled && item.cancelReason && <span className="pl-3">{t.reason(item.cancelReason)}</span>}
          </li>
        ))}
      </ul>
      {variant === "test" && <p className="pt-1">{t.testLine}</p>}
      <hr className="my-1 border-t border-dashed border-black" />
      <p className="text-center text-[11px]">{t.printedAt(time.format(printedAt))}</p>
    </article>
  );
}
