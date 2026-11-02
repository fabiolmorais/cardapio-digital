import type { Metadata } from "next";
import { BrandEditor } from "@/features/brand/brand-editor";
import { messages } from "@/messages/pt-BR";

export const metadata: Metadata = {
  title: `${messages.brand.title} · ${messages.app.name}`,
};

export default function BrandPage() {
  return <BrandEditor />;
}
