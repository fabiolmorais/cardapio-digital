"use client";

import { Eye, EyeOff } from "lucide-react";
import { useState, type ComponentProps } from "react";
import { Input } from "@/components/ui/input";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";

/**
 * O campo de senha com o botão de olho: mostra o que foi digitado, para conferir antes de enviar.
 * O nome do botão não muda ("Mostrar senha"); o estado vai no aria-pressed, como pede um botão
 * de liga e desliga.
 */
export function PasswordInput({ className, ...props }: Omit<ComponentProps<typeof Input>, "type">) {
  const [visible, setVisible] = useState(false);
  return (
    <div className="relative">
      <Input {...props} type={visible ? "text" : "password"} className={cn(className, "pr-12")} />
      <button
        type="button"
        aria-label={messages.common.showPassword}
        aria-pressed={visible}
        onClick={() => setVisible((v) => !v)}
        className="absolute inset-y-0 right-0 flex w-12 items-center justify-center rounded-r-xl text-muted-foreground transition-colors hover:text-foreground focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
      >
        {visible ? <EyeOff aria-hidden className="size-5" /> : <Eye aria-hidden className="size-5" />}
      </button>
    </div>
  );
}
