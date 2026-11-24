import { Star } from "lucide-react";
import { messages } from "@/messages/pt-BR";

/** As estrelas de uma nota, só de enfeite: o leitor de tela ouve "Nota 4 de 5". */
export function RatingStars({ stars, size = "size-4" }: { stars: number; size?: string }) {
  return (
    <span role="img" aria-label={messages.panel.ratings.stars(stars)} className="flex gap-0.5 text-amber-400">
      {[1, 2, 3, 4, 5].map((n) => (
        <Star key={n} aria-hidden className={size} fill={n <= stars ? "currentColor" : "none"} />
      ))}
    </span>
  );
}
