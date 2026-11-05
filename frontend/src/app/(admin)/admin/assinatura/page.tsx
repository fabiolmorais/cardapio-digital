import type { Metadata } from "next";
import { SubscriptionPage } from "@/features/subscription/subscription-page";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.subscription.title} · ${messages.app.name}`,
};

export default function SubscriptionRoute() {
  return <SubscriptionPage />;
}
