import type { Metadata } from "next";
import { AuthFrame } from "@/features/account/auth-frame";
import { LoginForm } from "@/features/account/login-form";
import { messages } from "@/messages/pt-BR";

const t = messages.login;

export const metadata: Metadata = {
  title: `${t.title} · ${messages.app.name}`,
};

export default function LoginPage() {
  return (
    <AuthFrame title={t.title} subtitle={t.subtitle}>
      <LoginForm />
    </AuthFrame>
  );
}
