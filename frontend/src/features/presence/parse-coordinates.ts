/**
 * Lê um ponto colado pelo dono: as coordenadas ("-23.5577, -46.6358") ou o endereço do Google
 * Maps aberto no navegador. No link, vale primeiro o pino do lugar (!3d…!4d…), depois o ponto da
 * busca (?q= ou ll=) e por último o centro da tela (@lat,lng). O link curto (maps.app.goo.gl) não
 * traz o ponto: precisa abrir e copiar o endereço completo.
 */
export type ParsedPoint = { latitude: number; longitude: number } | "short-link" | null;

const NUMBER = String.raw`(-?\d{1,3}(?:\.\d+)?)`;

export function parseCoordinates(text: string): ParsedPoint {
  const value = text.trim();
  if (!value) return null;
  if (/^https?:\/\/(maps\.app\.goo\.gl|goo\.gl\/maps)\//i.test(value)) return "short-link";
  const decoded = safeDecode(value);
  const patterns = [
    new RegExp(String.raw`!3d${NUMBER}!4d${NUMBER}`),
    new RegExp(String.raw`[?&](?:q|ll|query|destination)=${NUMBER},\s*${NUMBER}`),
    new RegExp(String.raw`@${NUMBER},${NUMBER}`),
    new RegExp(String.raw`^${NUMBER}\s*[,;\s]\s*${NUMBER}$`),
  ];
  for (const pattern of patterns) {
    const match = pattern.exec(decoded);
    if (!match) continue;
    const latitude = Number(match[1]);
    const longitude = Number(match[2]);
    if (Math.abs(latitude) <= 90 && Math.abs(longitude) <= 180) return { latitude, longitude };
  }
  return null;
}

function safeDecode(value: string) {
  try {
    return decodeURIComponent(value);
  } catch {
    return value;
  }
}
