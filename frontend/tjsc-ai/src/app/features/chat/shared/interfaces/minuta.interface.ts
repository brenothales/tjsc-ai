export interface MinutaStep {
  acao: string;
  descricao: string;
}

export interface MinutaVersao {
  tipo: string;
  relatorio: string;
  fundamentacao: string;
  dispositivo: string;
  steps: MinutaStep[];
}

export type MinutaSecao = 'relatorio' | 'fundamentacao' | 'dispositivo';

export interface MinutaHistoricoItem {
  id: string;
  numero: string;
  versaoMinuta: number;
  criadaEm: string;
  qtdVersoes: number;
}

export type MinutaEvento =
  | { evento: 'step'; acao: string; mensagem: string }
  | { evento: 'versao'; dados: MinutaVersao }
  | { evento: 'done'; versaoMinuta: number }
  | { evento: 'erro'; mensagem: string };
