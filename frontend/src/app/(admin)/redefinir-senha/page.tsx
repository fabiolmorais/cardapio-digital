import type { Metadata } from "next";
import { Suspense } from "react";
import { AuthFrame } from "@/features/account/auth-frame";
import { ResetPasswordForm } from "@/features/account/reset-password-form";
import { messages } from "@/messages/pt-BR";

const t = messages.passwordReset;

export const metadata: Metadata = {
  title: `${t.resetTitle} · ${messages.app.name}`,
  // O endereço leva o token do link: não é para aparecer em busca nenhuma.
  robots: { index: false, follow: false },
  // E não vai junto quando a pessoa clica num link para fora.
  referrer: "no-referrer",
};

export default function ResetPasswordPage() {
  return (
    <AuthFrame title={t.resetTitle} subtitle={t.resetSubtitle}>
      {/* O token vem do endereço (?token=): o formulário só existe no navegador. */}
      <Suspense>
        <ResetPasswordForm />
      </Suspense>
    </AuthFrame>
  );
}
