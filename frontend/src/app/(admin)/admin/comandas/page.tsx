import type { Metadata } from "next";
import { TabHistory } from "@/features/tab-history/tab-history";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.tabHistory.title} · ${messages.app.name}`,
};

export default function TabHistoryPage() {
  return <TabHistory />;
}
