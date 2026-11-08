"use client";

import { Select as SelectPrimitive } from "@base-ui/react/select";
import { Check, ChevronDown } from "lucide-react";
import { cn } from "cn";

export type SelectOption = { value: string; label: string };

/**
 * Campo de escolha desenhado no padrão do sistema. Troca o <select> nativo porque a lista que ele
 * abre é do sistema operacional: no Chrome do Linux e do Windows ela vem clara mesmo no tema escuro.
 * O <Label htmlFor={id}> dá o nome ao botão; com `name`, o valor vai junto no envio do formulário.
 */
export function Select({
  id,
  options,
  value,
  defaultValue,
  onValueChange,
  name,
  required,
  className,
}: {
  id: string;
  options: SelectOption[];
  value?: string;
  defaultValue?: string;
  onValueChange?: (value: string) => void;
  name?: string;
  required?: boolean;
  className?: string;
}) {
  return (
    <SelectPrimitive.Root
      items={options}
      value={value}
      defaultValue={defaultValue}
      onValueChange={(next) => {
        if (next !== null) onValueChange?.(next as string);
      }}
      name={name}
      required={required}
    >
      <SelectPrimitive.Trigger
        id={id}
        data-slot="select"
        className={cn(
          "flex h-12 w-full items-center justify-between gap-2 rounded-xl border border-input bg-transparent px-3 text-left text-base outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 data-popup-open:border-ring dark:bg-input/30",
          className,
        )}
      >
        <SelectPrimitive.Value className="truncate" />
        <SelectPrimitive.Icon>
          <ChevronDown aria-hidden className="size-4 text-muted-foreground" />
        </SelectPrimitive.Icon>
      </SelectPrimitive.Trigger>
      <SelectPrimitive.Portal>
        <SelectPrimitive.Positioner sideOffset={6} alignItemWithTrigger={false} className="z-50 outline-none">
          <SelectPrimitive.Popup className="max-h-[min(var(--available-height),20rem)] min-w-(--anchor-width) overflow-y-auto rounded-xl border border-border bg-popover p-1 text-popover-foreground shadow-[0_24px_50px_rgba(0,0,0,.5)] outline-none">
            <SelectPrimitive.List>
              {options.map((option) => (
                <SelectPrimitive.Item
                  key={option.value}
                  value={option.value}
                  className="flex min-h-11 cursor-default items-center gap-3 rounded-lg px-3 text-base outline-none select-none data-highlighted:bg-accent data-selected:font-semibold"
                >
                  <SelectPrimitive.ItemText className="flex-1">{option.label}</SelectPrimitive.ItemText>
                  <SelectPrimitive.ItemIndicator>
                    <Check aria-hidden className="size-4 text-brand-text" />
                  </SelectPrimitive.ItemIndicator>
                </SelectPrimitive.Item>
              ))}
            </SelectPrimitive.List>
          </SelectPrimitive.Popup>
        </SelectPrimitive.Positioner>
      </SelectPrimitive.Portal>
    </SelectPrimitive.Root>
  );
}
