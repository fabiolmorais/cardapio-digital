import { cn } from "cn";

/** Uma opção liga/desliga com título e explicação; a linha inteira é o interruptor. */
export function ToggleRow({
  title,
  text,
  checked,
  onChange,
}: {
  title: string;
  text: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
}) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      onClick={() => onChange(!checked)}
      className="flex min-h-14 items-center justify-between gap-4 rounded-xl px-1 text-left focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:outline-none"
    >
      <span className="flex flex-col gap-0.5">
        <span className="font-medium">{title}</span>
        <span className="text-sm text-muted-foreground">{text}</span>
      </span>
      <span
        aria-hidden
        className={cn(
          "relative h-7 w-12 shrink-0 rounded-full border border-input bg-muted transition-colors",
          checked && "border-transparent bg-success",
        )}
      >
        <span
          className={cn(
            "absolute top-0.5 left-0.5 size-5.5 rounded-full bg-muted-foreground transition-transform",
            checked && "translate-x-5 bg-white",
          )}
        />
      </span>
    </button>
  );
}
