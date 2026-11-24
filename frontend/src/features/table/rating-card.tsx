"use client";

import { Check, ExternalLink, MessageCircle, Star } from "lucide-react";
import { useEffect, useState } from "react";
import { tablePage } from "@/messages/table.pt-BR";
import { cn } from "cn";
import { clickGoogle, fetchRatingOffer, rateService, type RatingOffer, type RatingTag } from "./table-rating";

const t = tablePage.rating;
const TAGS: RatingTag[] = ["SPEED", "FRIENDLINESS", "FOOD", "AMBIENCE"];
const HIGH = 4;
const LOW = 2;

/**
 * "Como foi o atendimento?" (RF40, desenho 5.3): aparece depois de a mesa pedir a conta e logo
 * depois de a comanda fechar. De 1 a 5 estrelas, os atalhos e um comentário curto; pular não muda
 * nada. Depois de enviar: nota 4 ou 5 convida a avaliar no Google (se o restaurante tem o link);
 * nota 1 ou 2 avisa que o gerente já recebeu.
 */
export function RatingCard({
  token,
  reviewUrl,
  place,
}: {
  token: string;
  reviewUrl: string | null;
  place: string | null;
}) {
  const [offer, setOffer] = useState<RatingOffer | null>(null);
  const [skipped, setSkipped] = useState(false);
  const [stars, setStars] = useState(0);
  const [tags, setTags] = useState<RatingTag[]>([]);
  const [comment, setComment] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    void fetchRatingOffer(token).then((found) => {
      if (!cancelled) setOffer(found);
    });
    return () => {
      cancelled = true;
    };
  }, [token]);

  if (!offer || skipped) return null;

  // Já avaliou: o agradecimento (com o convite ao Google, se for o caso).
  if (offer.stars != null) {
    const given = offer.stars;
    const high = given >= HIGH;
    const low = given <= LOW;
    return (
      <section
        role="status"
        className="flex flex-col items-center gap-3.5 rounded-3xl border border-(--mesa-stroke) bg-(--mesa-glass) px-4.5 py-6 text-center"
      >
        <span
          className={cn(
            "flex size-16 items-center justify-center rounded-full",
            high ? "bg-(--mesa-ok-soft) text-(--mesa-ok)" : "bg-(--brand)/15 text-(--brand-text)",
          )}
        >
          {high ? (
            <Check aria-hidden className="size-8" strokeWidth={2.6} />
          ) : (
            <MessageCircle aria-hidden className="size-7" />
          )}
        </span>
        <h2 className="text-2xl font-bold tracking-tight">{high ? t.highTitle : low ? t.lowTitle : t.midTitle}</h2>
        <p className="text-(--mesa-muted)">
          {high ? (reviewUrl ? t.highText(place ?? "") : t.highTextNoLink) : low ? t.lowText : t.midText}
        </p>
        {high && reviewUrl && offer.googleInvite && (
          <a
            href={reviewUrl}
            target="_blank"
            rel="noopener noreferrer"
            onClick={() => clickGoogle(token)}
            className="flex h-13 w-full items-center justify-center gap-2 rounded-2xl bg-(--brand) font-semibold text-white focus-visible:ring-3 focus-visible:ring-(--brand-text) focus-visible:outline-none"
          >
            <ExternalLink aria-hidden className="size-4.5" />
            {t.google}
            <span className="sr-only">{tablePage.newTab}</span>
          </a>
        )}
      </section>
    );
  }

  if (!offer.available) return null;

  const toggle = (tag: RatingTag) =>
    setTags((current) => (current.includes(tag) ? current.filter((x) => x !== tag) : [...current, tag]));

  async function submit() {
    if (stars === 0) return;
    setBusy(true);
    setError(null);
    try {
      setOffer(await rateService(token, { stars, tags, comment: comment.trim() || undefined }));
    } catch (e) {
      setError((e as Error).message);
    }
    setBusy(false);
  }

  return (
    <section
      aria-labelledby="rating-title"
      className="flex flex-col gap-4.5 rounded-3xl border border-(--mesa-stroke) bg-(--mesa-glass) px-4.5 py-5"
    >
      <div className="flex flex-col gap-0.5">
        <h2 id="rating-title" className="text-xl font-bold tracking-tight">
          {t.title}
        </h2>
        <span className="text-sm text-(--mesa-muted)">
          {offer.waiterName ? t.waiter(offer.waiterName) : t.noWaiter}
        </span>
      </div>
      <div role="group" aria-label={t.stars} className="flex justify-between gap-1.5">
        {[1, 2, 3, 4, 5].map((n) => (
          <button
            key={n}
            type="button"
            aria-label={t.star(n)}
            aria-pressed={n === stars}
            onClick={() => setStars(n)}
            className={cn(
              "flex size-13 items-center justify-center rounded-2xl border border-(--mesa-stroke) bg-(--mesa-glass) text-(--mesa-muted) focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none",
              n <= stars && "border-amber-400/45 bg-amber-400/12 text-amber-400",
            )}
          >
            <Star aria-hidden className="size-6.5" fill={n <= stars ? "currentColor" : "none"} />
          </button>
        ))}
      </div>
      <span aria-live="polite" className="-mt-2 text-center text-sm text-(--mesa-muted)">
        {t.captions[stars]}
      </span>
      <div className="flex flex-col gap-2.5">
        <span className="text-sm text-(--mesa-muted)">{stars > 0 && stars <= 3 ? t.whatToImprove : t.whatMarked}</span>
        <div role="group" aria-label={t.tagsLabel} className="flex flex-wrap gap-2">
          {TAGS.map((tag) => (
            <button
              key={tag}
              type="button"
              aria-pressed={tags.includes(tag)}
              onClick={() => toggle(tag)}
              className={cn(
                "h-10 rounded-full border border-(--mesa-stroke) bg-(--mesa-glass) px-3.5 text-sm font-semibold focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none",
                tags.includes(tag) && "border-transparent bg-(--mesa-fg) text-(--mesa-bg)",
              )}
            >
              {t.tags[tag]}
            </button>
          ))}
        </div>
      </div>
      <label className="flex flex-col gap-2">
        <span className="text-sm text-(--mesa-muted)">{t.comment}</span>
        <textarea
          value={comment}
          maxLength={280}
          rows={3}
          placeholder={t.commentPlaceholder}
          onChange={(e) => setComment(e.target.value)}
          className="resize-none rounded-2xl border border-(--mesa-stroke) bg-(--mesa-glass) px-3.5 py-3 text-base focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none"
        />
      </label>
      {error && (
        <p role="alert" className="text-sm font-medium text-(--mesa-danger)">
          {error}
        </p>
      )}
      <div className="flex flex-col gap-2">
        <button
          type="button"
          disabled={stars === 0 || busy}
          onClick={() => void submit()}
          className="h-13 rounded-2xl bg-(--brand) font-semibold text-white focus-visible:ring-3 focus-visible:ring-(--brand-text) focus-visible:outline-none disabled:opacity-50"
        >
          {busy ? t.sending : t.send}
        </button>
        <button
          type="button"
          onClick={() => setSkipped(true)}
          className="h-11 rounded-2xl text-sm font-semibold text-(--mesa-muted) focus-visible:ring-3 focus-visible:ring-(--brand) focus-visible:outline-none"
        >
          {t.skip}
        </button>
      </div>
    </section>
  );
}
