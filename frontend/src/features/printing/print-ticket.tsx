import { messages } from "@/messages/pt-BR";
import { choiceLines } from "@/features/table/choices";
import type { MovePrintJob, PrintJob } from "./print-api";

const t = messages.panel.printing.ticket;

const time = new Intl.DateTimeFormat("pt-BR", {
  hour: "2-digit",
  minute: "2-digit",
});

/**
 * O que a estação imprime: um pedido, a via "CANCELADO", a reimpressão, o teste da impressora ou a
 * via "MUDOU DE MESA" (ADR 0012).
 */
export type Ticket =
  | { job: PrintJob; variant: "new" | "reprint" | "test"; printedAt: Date }
  | { move: MovePrintJob; variant: "move"; printedAt: Date };

/**
 * A comanda no papel térmico (80 ou 58 mm): preto no branco, letras grandes, a mesa em destaque
 * e um item por linha, com a observação logo abaixo. Só aparece na impressão.
 */
export function PrintTicket({ ticket, restaurant }: { ticket: Ticket; restaurant: string }) {
  if (ticket.variant === "move")
    return <MoveTicket move={ticket.move} printedAt={ticket.printedAt} restaurant={restaurant} />;
  const { job, variant, printedAt } = ticket;
  const canceled = job.kind === "CANCELLATION";
  const orderedAt = job.orderedAt ? new Date(job.orderedAt) : printedAt;
  return (
    <article data-print-ticket className="flex flex-col gap-1 bg-white font-mono text-[13px] leading-tight text-black">
      <p className="text-center text-[12px]">{restaurant}</p>
      {variant === "test" && <p className="text-center text-[15px] font-bold">{t.test}</p>}
      {variant === "reprint" && <p className="text-center text-[15px] font-bold">{t.reprint}</p>}
      {canceled && <p className="text-center text-[17px] font-bold">{t.canceled}</p>}
      {job.counterName ? (
        <>
          <p className="text-center text-[26px] font-bold">{t.counter}</p>
          <p className="text-center text-[17px] font-bold">{t.counterGuest(job.counterName, job.counterCard)}</p>
          <p className="text-center">{job.tableLabel}</p>
        </>
      ) : (
        <>
          <p className="text-center text-[26px] font-bold">{t.table(job.tableLabel)}</p>
          <p className="text-center">{job.sectorName}</p>
        </>
      )}
      <p className="text-center">{t.order(job.orderNumber, time.format(orderedAt))}</p>
      <hr className="my-1 border-t border-dashed border-black" />
      <ul className="flex flex-col gap-1.5">
        {job.items.map((item) => (
          <li key={item.id} className="flex flex-col">
            <span className={canceled ? "text-[17px] font-bold line-through" : "text-[17px] font-bold"}>
              {item.quantity}x {item.productName}
              {item.rodizio && ` (${t.rodizio})`}
            </span>
            {/* O tamanho e as escolhas, uma por linha e em negrito: é o que a cozinha lê (ADR 0010). */}
            {choiceLines(item.choices).map((line) => (
              <span key={line} className="pl-3 text-[15px] font-bold">
                {line}
              </span>
            ))}
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

/**
 * A via "MUDOU DE MESA" (ou "JUNTOU MESAS"): a mesa antiga e a nova em destaque, e os itens que já
 * tinham saído no papel com a mesa antiga, para quem leva o prato ir à mesa certa.
 */
function MoveTicket({ move, printedAt, restaurant }: { move: MovePrintJob; printedAt: Date; restaurant: string }) {
  return (
    <article data-print-ticket className="flex flex-col gap-1 bg-white font-mono text-[13px] leading-tight text-black">
      <p className="text-center text-[12px]">{restaurant}</p>
      <p className="text-center text-[17px] font-bold">{move.merged ? t.merged : t.moved}</p>
      <p className="text-center text-[22px] font-bold">{t.fromTo(move.fromTableLabel, move.toTableLabel)}</p>
      <p className="text-center">{move.sectorName}</p>
      <p className="text-center">{t.movedAt(time.format(new Date(move.movedAt)))}</p>
      <hr className="my-1 border-t border-dashed border-black" />
      <p>{t.movedHint}</p>
      <ul className="flex flex-col gap-1.5">
        {move.items.map((item) => (
          <li key={item.id} className="flex flex-col">
            <span className="text-[17px] font-bold">
              {item.quantity}x {item.productName}
              {item.rodizio && ` (${t.rodizio})`}
            </span>
            {choiceLines(item.choices).map((line) => (
              <span key={line} className="pl-3 text-[15px] font-bold">
                {line}
              </span>
            ))}
          </li>
        ))}
      </ul>
      <hr className="my-1 border-t border-dashed border-black" />
      <p className="text-center text-[11px]">{t.printedAt(time.format(printedAt))}</p>
    </article>
  );
}
