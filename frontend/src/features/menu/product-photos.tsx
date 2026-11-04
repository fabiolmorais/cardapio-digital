"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Clapperboard, GripVertical, ImagePlus, LoaderCircle, TriangleAlert, X } from "lucide-react";
import { useId, useState, type ChangeEvent, type DragEvent } from "react";
import { ConfirmDialog } from "@/components/confirm-dialog";
import { messages } from "@/messages/pt-BR";
import { cn } from "cn";
import {
  fetchProductMedia,
  removeProductMedia,
  reorderProductMedia,
  uploadProductPhoto,
  uploadProductVideo,
  type ProductMedia,
} from "./menu-api";
import { useReorder } from "./use-reorder";

const t = messages.menu.photos;
const tv = t.video;
/**
 * As mesmas regras do back (PhotoFile, VideoFile e os limites do ProductMedia), só para avisar
 * antes de enviar: o back confere de novo.
 */
const TYPES = ["image/jpeg", "image/png"];
const MAX_BYTES = 8 * 1024 * 1024;
const MAX_PHOTOS = 8;
const VIDEO_TYPES = ["video/mp4", "video/quicktime", "video/webm"];
const MAX_VIDEO_BYTES = 200 * 1024 * 1024;
const MAX_VIDEOS = 3;
/** Enquanto um vídeo converte, a galeria pergunta de novo a cada 3 segundos. */
const PROCESSING_REFRESH_MS = 3000;

type Sending = { key: string; name: string; percent: number | null };

/**
 * Painel "Fotos e vídeos" da tela do produto (desenho 12.3): a galeria com a capa marcada,
 * arrastar para ordenar, remover e enviar com barra de progresso. O vídeo vai em pedaços (TUS)
 * direto para o serviço de vídeo, e aparece "convertendo" até ficar pronto.
 */
export function ProductPhotos({ productId, productName }: { productId: string; productName: string }) {
  const queryClient = useQueryClient();
  const media = useQuery({
    queryKey: ["product-media", productId],
    queryFn: () => fetchProductMedia(productId),
    // Um vídeo em conversão? A galeria pergunta de novo até ele ficar pronto (o back confere no serviço).
    refetchInterval: (query) =>
      query.state.data?.some((m) => m.status === "PROCESSING") ? PROCESSING_REFRESH_MS : false,
  });
  const [sending, setSending] = useState<Sending[]>([]);
  const [notice, setNotice] = useState<string | null>(null);
  const [errors, setErrors] = useState<string[]>([]);
  const [dragOver, setDragOver] = useState(false);
  const [removing, setRemoving] = useState<{ photo: ProductMedia; position: number } | null>(null);
  const [videoSending, setVideoSending] = useState<{ name: string; percent: number } | null>(null);
  const inputId = useId();
  const videoInputId = useId();
  const hintId = useId();

  const photos = media.data ?? [];
  const refresh = () => queryClient.invalidateQueries({ queryKey: ["product-media", productId] });

  const reorder = useReorder(
    photos,
    async (ids) => {
      setErrors([]);
      try {
        await reorderProductMedia(productId, ids);
      } catch (error) {
        setErrors([(error as Error).message]);
      }
      await refresh();
    },
    (_photo, position, total) => t.moved(position, total),
    "x",
  );

  /** Envia uma por vez: no 4G, várias ao mesmo tempo dividiriam a pouca banda e demorariam mais. */
  async function send(files: File[]) {
    setNotice(null);
    const problems: string[] = [];
    const accepted = files.filter((file) => {
      if (!TYPES.includes(file.type)) problems.push(t.wrongType(file.name));
      else if (file.size > MAX_BYTES) problems.push(t.tooBig(file.name));
      else return true;
      return false;
    });
    const room = MAX_PHOTOS - photos.length;
    if (accepted.length > room) problems.push(t.full(MAX_PHOTOS));
    const queue = accepted.slice(0, Math.max(0, room));
    setErrors(problems);
    const items = queue.map((file, i) => ({ key: `${Date.now()}-${i}`, name: file.name, percent: 0 as number | null }));
    setSending(items);

    let sent = 0;
    for (const [i, file] of queue.entries()) {
      const { key } = items[i];
      const update = (percent: number | null) =>
        setSending((list) => list.map((item) => (item.key === key ? { ...item, percent } : item)));
      try {
        // Enviado o arquivo (100%), o back ainda confere e gera os tamanhos: "preparando…".
        await uploadProductPhoto(productId, file, (fraction) => update(fraction < 1 ? Math.round(fraction * 100) : null));
        sent++;
        await refresh();
      } catch (error) {
        problems.push(`${file.name}: ${(error as Error).message}`);
        setErrors([...problems]);
      }
      setSending((list) => list.filter((item) => item.key !== key));
    }
    setSending([]);
    if (sent > 0) setNotice(t.sent(sent));
  }

  async function sendVideo(file: File) {
    setNotice(null);
    if (!VIDEO_TYPES.includes(file.type)) return setErrors([tv.wrongType(file.name)]);
    if (file.size > MAX_VIDEO_BYTES) return setErrors([tv.tooBig(file.name)]);
    setErrors([]);
    setVideoSending({ name: file.name, percent: 0 });
    try {
      await uploadProductVideo(productId, file, (fraction) =>
        setVideoSending({ name: file.name, percent: Math.round(fraction * 100) }),
      );
      setNotice(tv.sent);
    } catch (error) {
      setErrors([`${file.name}: ${(error as Error).message}`]);
    }
    setVideoSending(null);
    await refresh();
  }

  function onPickVideo(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (file) void sendVideo(file);
    event.target.value = "";
  }

  function onPick(event: ChangeEvent<HTMLInputElement>) {
    void send(Array.from(event.target.files ?? []));
    event.target.value = ""; // a mesma foto pode ser escolhida de novo
  }

  function onDrop(event: DragEvent<HTMLLabelElement>) {
    event.preventDefault();
    setDragOver(false);
    void send(Array.from(event.dataTransfer.files));
  }

  const busy = sending.length > 0 || videoSending !== null;
  const full = photos.length >= MAX_PHOTOS;
  // O vídeo que falhou não conta no limite (como no back): dá para enviar outro no lugar.
  const videosFull = photos.filter((m) => m.type === "VIDEO" && m.status !== "FAILED").length >= MAX_VIDEOS;
  const cover = photos.findIndex((m) => m.type === "IMAGE");
  const failed = photos
    .map((m, index) => ({ m, position: index + 1 }))
    .filter(({ m }) => m.status === "FAILED");

  return (
    <section
      aria-labelledby={`${inputId}title`}
      className="flex flex-col gap-4 rounded-2xl border border-border bg-card p-5 sm:p-6"
    >
      <div className="flex flex-col gap-1">
        <h2 id={`${inputId}title`} className="font-semibold">
          {t.title}
        </h2>
        <p id={hintId} className="text-sm text-muted-foreground">
          {t.hint}
        </p>
      </div>

      {media.isPending ? (
        <p className="text-sm text-muted-foreground">{t.loading}</p>
      ) : media.isError ? (
        <p role="alert" className="text-sm text-destructive">
          {t.loadError}
        </p>
      ) : photos.length === 0 ? (
        <p className="text-sm text-muted-foreground">{t.empty}</p>
      ) : (
        <>
          <p aria-live="polite" className="sr-only">
            {reorder.announcement}
          </p>
          <ul
            aria-busy={reorder.saving}
            className={cn("flex gap-2.5 overflow-x-auto pb-1", reorder.saving && "opacity-60")}
          >
            {reorder.ordered.map((photo, index) => (
              <li
                key={photo.id}
                ref={reorder.rowRef(photo.id)}
                style={reorder.rowStyle(photo.id)}
                className={cn(
                  "relative size-24 shrink-0 overflow-hidden rounded-xl bg-muted",
                  reorder.draggingId === photo.id && "shadow-lg ring-2 ring-ring",
                )}
              >
                {photo.type === "IMAGE" ? (
                  // eslint-disable-next-line @next/next/no-img-element -- miniatura já no tamanho certo, vinda do R2
                  <img
                    src={photo.urls?.thumb}
                    alt={t.alt(productName, index + 1)}
                    width={96}
                    height={96}
                    draggable={false}
                    className="size-full object-cover"
                  />
                ) : (
                  <VideoTile media={photo} position={index + 1} />
                )}
                {index === cover && (
                  <span className="absolute bottom-1 left-1 rounded-md bg-black/70 px-1.5 py-0.5 text-[11px] font-semibold text-white">
                    {t.cover}
                  </span>
                )}
                <button
                  type="button"
                  aria-label={photo.type === "VIDEO" ? tv.handle(index + 1) : t.handle(index + 1)}
                  aria-describedby={hintId}
                  className="absolute top-1 left-1 flex size-8 cursor-grab touch-none items-center justify-center rounded-lg bg-black/60 text-white focus-visible:ring-3 focus-visible:ring-ring focus-visible:outline-none active:cursor-grabbing"
                  {...reorder.handleProps(photo)}
                >
                  <GripVertical aria-hidden className="size-4" />
                </button>
                <button
                  type="button"
                  aria-label={photo.type === "VIDEO" ? tv.remove(index + 1) : t.remove(index + 1)}
                  onClick={() => setRemoving({ photo, position: index + 1 })}
                  className="absolute top-1 right-1 flex size-8 items-center justify-center rounded-lg bg-black/60 text-white hover:bg-black/80 focus-visible:ring-3 focus-visible:ring-ring focus-visible:outline-none"
                >
                  <X aria-hidden className="size-4" />
                </button>
              </li>
            ))}
          </ul>
        </>
      )}

      {busy && (
        <ul role="status" className="flex flex-col gap-2">
          {sending.map((item) => (
            <li key={item.key} className="flex flex-col gap-1 text-sm">
              <span className="truncate">
                {item.percent === null ? t.processing(item.name) : t.sending(item.name, item.percent)}
              </span>
              <span aria-hidden className="h-1.5 overflow-hidden rounded-full bg-muted">
                <span
                  className={cn("block h-full bg-primary transition-[width]", item.percent === null && "animate-pulse")}
                  style={{ width: `${item.percent ?? 100}%` }}
                />
              </span>
            </li>
          ))}
        </ul>
      )}

      {!full && (
        <>
          <input
            id={inputId}
            type="file"
            accept={TYPES.join(",")}
            multiple
            disabled={busy}
            onChange={onPick}
            className="sr-only"
          />
          <label
            htmlFor={inputId}
            onDragOver={(event) => {
              event.preventDefault();
              setDragOver(true);
            }}
            onDragLeave={() => setDragOver(false)}
            onDrop={onDrop}
            className={cn(
              "flex min-h-28 cursor-pointer flex-col items-center justify-center gap-1.5 rounded-xl border-2 border-dashed border-border px-4 py-5 text-center transition-colors hover:bg-muted has-[:focus-visible]:ring-3 has-[:focus-visible]:ring-ring/50",
              dragOver && "border-primary bg-muted",
              busy && "pointer-events-none opacity-60",
            )}
          >
            <ImagePlus aria-hidden className="size-6 text-muted-foreground" />
            <span className="font-medium">{dragOver ? t.drop : t.send}</span>
            <span className="text-sm text-muted-foreground">{t.sendHint}</span>
          </label>
        </>
      )}
      {full && <p className="text-sm text-muted-foreground">{t.full(MAX_PHOTOS)}</p>}

      {videoSending && (
        <div role="status" className="flex flex-col gap-1 text-sm">
          <span className="truncate">{tv.sending(videoSending.name, videoSending.percent)}</span>
          <span aria-hidden className="h-1.5 overflow-hidden rounded-full bg-muted">
            <span className="block h-full bg-primary transition-[width]" style={{ width: `${videoSending.percent}%` }} />
          </span>
        </div>
      )}

      {!full && !videosFull && (
        <div className="flex flex-wrap items-center gap-3">
          <input
            id={videoInputId}
            type="file"
            accept={VIDEO_TYPES.join(",")}
            disabled={busy}
            onChange={onPickVideo}
            className="sr-only"
          />
          <label
            htmlFor={videoInputId}
            className={cn(
              "flex h-11 cursor-pointer items-center gap-2 rounded-xl border border-border px-4 font-medium hover:bg-muted has-[:focus-visible]:ring-3 has-[:focus-visible]:ring-ring/50",
              busy && "pointer-events-none opacity-60",
            )}
          >
            <Clapperboard aria-hidden className="size-5" />
            {tv.send}
          </label>
          <span className="text-sm text-muted-foreground">{tv.sendHint}</span>
        </div>
      )}
      {!full && videosFull && <p className="text-sm text-muted-foreground">{tv.full}</p>}

      {failed.length > 0 && (
        <ul className="flex flex-col gap-1 text-sm text-destructive">
          {failed.map(({ m, position }) => (
            <li key={m.id}>{tv.failedDetail(position, m.failure ?? "")}</li>
          ))}
        </ul>
      )}

      {notice && (
        <p role="status" className="text-sm text-success">
          {notice}
        </p>
      )}
      {errors.length > 0 && (
        <ul role="alert" className="flex flex-col gap-1 text-sm text-destructive">
          {errors.map((error) => (
            <li key={error}>{error}</li>
          ))}
        </ul>
      )}

      <ConfirmDialog
        open={removing !== null}
        onOpenChange={(open) => !open && setRemoving(null)}
        title={
          removing
            ? removing.photo.type === "VIDEO"
              ? tv.confirmTitle(removing.position)
              : t.confirmTitle(removing.position)
            : ""
        }
        description={removing?.photo.type === "VIDEO" ? tv.confirmText : t.confirmText}
        confirmLabel={t.confirm}
        onConfirm={async () => {
          if (!removing) return;
          await removeProductMedia(productId, removing.photo.id);
          await refresh();
        }}
      />
    </section>
  );
}

/**
 * O vídeo na galeria: pronto, a capa (ou o primeiro quadro) com a duração; convertendo, a roda;
 * falhou, o aviso (o motivo vem embaixo da galeria).
 */
function VideoTile({ media, position }: { media: ProductMedia; position: number }) {
  const label = tv.label(position);
  if (media.status === "PROCESSING") {
    return (
      <span className="flex size-full flex-col items-center justify-center gap-1 text-xs text-muted-foreground">
        <LoaderCircle aria-hidden className="size-5 animate-spin" />
        <span>{tv.processing}</span>
        <span className="sr-only">{label}</span>
      </span>
    );
  }
  if (media.status === "FAILED" || !media.video) {
    return (
      <span className="flex size-full flex-col items-center justify-center gap-1 bg-destructive/15 text-xs text-destructive">
        <TriangleAlert aria-hidden className="size-5" />
        <span>{tv.failed}</span>
        <span className="sr-only">{label}</span>
      </span>
    );
  }
  return (
    <span className="relative block size-full">
      {/* Sem capa (o serviço falso não gera), o vídeo mostra o primeiro quadro. Mudo e parado. */}
      <video
        src={media.video.url}
        poster={media.video.poster ?? undefined}
        preload="metadata"
        muted
        playsInline
        aria-label={label}
        className="size-full object-cover"
      />
      <span className="absolute right-1 bottom-1 flex items-center gap-1 rounded-md bg-black/70 px-1.5 py-0.5 text-[11px] font-semibold text-white">
        <Clapperboard aria-hidden className="size-3" />
        {tv.duration(media.video.durationSeconds)}
      </span>
    </span>
  );
}
