export const REALTIME_CONFIG = {
  model: 'gpt-4o-realtime-preview-2024-12-17',
  voice: 'verse',
  sdpEndpoint: 'https://api.openai.com/v1/realtime',
} as const;

export const CONSULTAR_AGENTE_TOOL = {
  type: 'function' as const,
  name: 'consultar_agente',
  description:
    'Consulta o agente institucional do TJSC para responder perguntas sobre processos, partes, magistrados e movimentações.',
  parameters: {
    type: 'object',
    properties: {
      pergunta: { type: 'string', description: 'Pergunta a ser respondida pelo agente' },
    },
    required: ['pergunta'],
  },
};
