export const API_BASE = '/api/v1';

export const API = {
  conversations:      `${API_BASE}/conversations`,
  conversationById:   (id: string) => `${API_BASE}/conversations/${id}`,
  conversationTitle:  (id: string) => `${API_BASE}/conversations/${id}/title`,
  chatStream:         `${API_BASE}/chat/stream`,
  minutaGerar:        `${API_BASE}/minuta/gerar`,
  minutaHistorico:    (numero: string) => `${API_BASE}/minuta/${encodeURIComponent(numero)}/historico`,
  minutaVersao:       (numero: string, versao: number) => `${API_BASE}/minuta/${encodeURIComponent(numero)}/${versao}`,
  voiceSession:       `${API_BASE}/voice/session`,
  voiceAgent:         `${API_BASE}/voice/agent`,
} as const;
