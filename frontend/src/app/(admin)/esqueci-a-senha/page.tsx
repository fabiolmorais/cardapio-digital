import type { Metadata } from "next";
import { AuthFrame } from "@/features/account/auth-frame";
import { ForgotPasswordForm } from "@/features/account/forgot-password-form";
import { messages } from "@/messages/pt-BR";

const t = messages.passwordReset;

export const metadata: Metadata = {
  title: `${t.requestTitle} · ${messages.app.name}`,
};

export default function ForgotPasswordPage() {
  return (
    <AuthFrame title={t.requestTitle} subtitle={t.requestSubtitle}>
      <ForgotPasswordForm />
    </AuthFrame>
  );
}
