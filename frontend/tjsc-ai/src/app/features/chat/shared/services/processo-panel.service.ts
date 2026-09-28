import { Injectable, signal } from '@angular/core';
import { ContextoResponse } from './processo.api.service';

export type PanelAba = 'informacoes' | 'movimentacoes' | 'documentos';

export interface PanelState {
  numero: string;
  aba: PanelAba;
  contexto: ContextoResponse | null;
}

@Injectable({ providedIn: 'root' })
export class ProcessoPanelService {
  readonly state = signal<PanelState | null>(null);

  open(numero: string, contexto: ContextoResponse | null = null, aba: PanelAba = 'informacoes'): void {
    this.state.set({ numero, aba, contexto });
  }

  setAba(aba: PanelAba): void {
    this.state.update((s) => (s ? { ...s, aba } : null));
  }

  close(): void {
    this.state.set(null);
  }
}
