"use client";

import "leaflet/dist/leaflet.css";
import L from "leaflet";
import { useEffect, useRef } from "react";
import { messages } from "@/messages/pt-BR";

const t = messages.presence.location;

type Props = {
  latitude: number;
  longitude: number;
  radiusMeters: number;
  onMove: (latitude: number, longitude: number) => void;
};

/**
 * O mapa da tela "Presença": o pino do restaurante (arrastável) e o círculo do raio. Leaflet com o
 * mapa do OpenStreetMap: sem chave e sem custo, com o crédito que o OSM exige. Carregado só nesta
 * tela (next/dynamic), para não pesar no resto do sistema.
 *
 * O pino é um "divIcon" (HTML) e não a imagem padrão do Leaflet, que se perde com os bundlers.
 */
export default function LocationMap({ latitude, longitude, radiusMeters, onMove }: Props) {
  const container = useRef<HTMLDivElement>(null);
  const map = useRef<L.Map | null>(null);
  const marker = useRef<L.Marker | null>(null);
  const circle = useRef<L.Circle | null>(null);
  const move = useRef(onMove);

  useEffect(() => {
    move.current = onMove;
  }, [onMove]);

  // Cria o mapa uma vez; as mudanças de ponto e raio vêm pelo efeito de baixo.
  useEffect(() => {
    if (!container.current) return;
    const created = L.map(container.current, { scrollWheelZoom: false }).setView([latitude, longitude], 17);
    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).addTo(created);
    circle.current = L.circle([latitude, longitude], {
      radius: radiusMeters,
      color: "#FF8466",
      weight: 2,
      fillColor: "#CC3F1C",
      fillOpacity: 0.16,
    }).addTo(created);
    marker.current = L.marker([latitude, longitude], {
      draggable: true,
      title: t.pin,
      alt: t.pin,
      icon: L.divIcon({
        className: "",
        html: '<span class="block size-5 rounded-full border-[3px] border-white bg-[#CC3F1C] shadow-md"></span>',
        iconSize: [20, 20],
        iconAnchor: [10, 10],
      }),
    }).addTo(created);
    marker.current.on("dragend", () => {
      const { lat, lng } = marker.current!.getLatLng();
      move.current(lat, lng);
    });
    created.on("click", (event: L.LeafletMouseEvent) => move.current(event.latlng.lat, event.latlng.lng));
    map.current = created;
    return () => {
      created.remove();
      map.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps -- criado uma vez; o efeito de baixo acompanha as mudanças
  }, []);

  useEffect(() => {
    if (!map.current) return;
    marker.current?.setLatLng([latitude, longitude]);
    circle.current?.setLatLng([latitude, longitude]).setRadius(radiusMeters);
    map.current.fitBounds(circle.current!.getBounds(), { padding: [24, 24], maxZoom: 18 });
  }, [latitude, longitude, radiusMeters]);

  return (
    <div
      ref={container}
      role="group"
      aria-label={t.map(radiusMeters)}
      className="z-0 h-72 w-full overflow-hidden rounded-xl border border-border"
    />
  );
}
