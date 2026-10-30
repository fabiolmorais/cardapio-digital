"use client";

import { useState } from "react";
import { cn } from "cn";

/**
 * Interruptor liga/desliga (role="switch"). Fica desabilitado enquanto a mudança é salva,
 * para um segundo toque não sair antes da resposta do primeiro.
 */
export function ToggleSwitch({
  checked,
  label,
  onToggle,
}: {
  checked: boolean;
  label: string;
  onToggle: () => Promise<void>;
}) {
  const [busy, setBusy] = useState(false);
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      disabled={busy}
      onClick={async () => {
        setBusy(true);
        try {
          await onToggle();
        } finally {
          setBusy(false);
        }
      }}
      className={cn(
        "relative h-7 w-12 shrink-0 rounded-full border border-input bg-muted transition-colors focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none disabled:opacity-60",
        checked && "border-transparent bg-success",
      )}
    >
      <span
        aria-hidden
        className={cn(
          "absolute top-0.5 left-0.5 size-5.5 rounded-full bg-muted-foreground transition-transform",
          checked && "translate-x-5 bg-white",
        )}
      />
    </button>
  );
}
