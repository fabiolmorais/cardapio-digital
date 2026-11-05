import Link from "next/link";
import { messages } from "@/messages/pt-BR";

/**
 * As frases de limite do plano vêm do back e terminam pedindo para mudar de plano
 * (PlanLimitReachedException e FeatureNotInPlanException). Nelas, o erro ganha o atalho.
 */
export function isPlanLimit(message: string) {
  return message.includes("mude de plano") || message.includes("Mude de plano");
}

/** O texto do erro, com o link para a assinatura quando o erro é o limite do plano. */
export function ErrorText({ message }: { message: string }) {
  if (!isPlanLimit(message)) return message;
  return (
    <>
      {message}{" "}
      <Link href="/admin/assinatura" className="font-semibold underline underline-offset-4">
        {messages.subscription.see}
      </Link>
    </>
  );
}
