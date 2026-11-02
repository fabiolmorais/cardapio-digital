/** As iniciais de um nome, para o lugar da logo: "Sakura Sushi" → "SS", "Bar" → "B". */
export function initials(name: string) {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");
}
