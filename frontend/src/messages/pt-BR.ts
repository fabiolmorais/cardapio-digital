/** Textos da interface em pt-BR, centralizados. */
export const messages = {
  app: {
    name: "Cardápio Digital",
    description: "Cardápio digital e atendimento por mesa para bares e restaurantes.",
  },
  home: {
    headline: "Cardápio digital e atendimento por mesa",
    subheadline:
      "O cliente escaneia o QR Code, chama o garçom, vê o cardápio e faz o pedido pelo celular. A equipe recebe tudo na hora.",
    plansTitle: "Planos",
    plansLoading: "Carregando planos…",
    plansError: "Não foi possível carregar os planos agora. Tente de novo em instantes.",
  },
  plans: {
    perMonth: "/mês",
    perYear: (value: string) => `ou ${value} por ano`,
    customPricing: "Sob consulta",
    tables: (n: number | null) => (n === null ? "Mesas ilimitadas" : `Até ${n} mesas`),
    users: (n: number | null) =>
      n === null ? "Usuários ilimitados" : `Até ${n} usuários da equipe`,
    sectors: (n: number | null) =>
      n === null ? "Setores ilimitados" : n === 1 ? "1 setor do salão" : `${n} setores do salão`,
    history: (days: number | null) =>
      days === null ? "Histórico ilimitado" : days >= 365 ? `Histórico de ${Math.round(days / 30)} meses` : `Histórico de ${days} dias`,
    included: "Chamar garçom, pedido pelo celular e comanda",
    features: {
      PRODUCT_VIDEOS: "Vídeos nos pratos",
      PREP_STATIONS: "Filas de cozinha e bar",
      ROLES_AND_PERMISSIONS: "Perfis e permissões",
      ADVANCED_REPORTS: "Relatórios avançados",
      REPORT_EXPORT: "Exportação de relatórios",
      MULTI_UNIT: "Várias unidades",
    },
    support: {
      EMAIL: "Suporte por e-mail",
      WHATSAPP: "Suporte por WhatsApp",
      PRIORITY_WHATSAPP: "WhatsApp prioritário",
      DEDICATED_MANAGER: "Gerente dedicado",
    },
  },
} as const;
