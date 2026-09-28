import { inject, Injectable, NgZone, signal } from '@angular/core';
import { MinutaEvento, MinutaHistoricoItem, MinutaSecao, MinutaStep, MinutaVersao } from '../interfaces/minuta.interface';
import { API } from '../constants/api.constants';

export interface ProgressoStep {
  acao: string;
  mensagem: string;
  concluido: boolean;
}

@Injectable({ providedIn: 'root' })
export class MinutaService {
  private readonly ngZone = inject(NgZone);

  readonly isOpen        = signal(false);
  readonly isGenerating  = signal(false);
  readonly numero        = signal('');
  readonly progresso     = signal<ProgressoStep[]>([]);
  readonly versoes       = signal<MinutaVersao[]>([]);
  readonly versaoAtiva   = signal(0);
  readonly secaoAtiva    = signal<MinutaSecao>('relatorio');
  readonly erro          = signal<string | null>(null);
  readonly historico     = signal<MinutaHistoricoItem[]>([]);
  readonly versaoAtualNumero = signal(0);

  hasMinuta(numero: string): boolean {
    return (this.numero() === numero && this.versoes().length > 0)
        || this.historico().length > 0;
  }

  reabrir(): void {
    this.isOpen.set(true);
  }

  open(numero: string): void {
    this.carregarHistorico(numero);

    // geração em andamento para este processo → só reabre o modal
    if (this.isGenerating() && this.numero() === numero) {
      this.isOpen.set(true);
      return;
    }

    // versões já carregadas em memória → só reabre
    if (this.numero() === numero && this.versoes().length > 0) {
      this.isOpen.set(true);
      return;
    }

    // existe histórico no MongoDB → carrega a versão mais recente
    if (this.historico().length > 0) {
      this.carregarVersaoDoHistorico(numero, this.historico()[0].versaoMinuta);
      return;
    }

    // nenhuma minuta ainda → gera
    this.numero.set(numero);
    this.isOpen.set(true);
    this.progresso.set([]);
    this.versoes.set([]);
    this.versaoAtiva.set(0);
    this.secaoAtiva.set('relatorio');
    this.erro.set(null);
    this.gerar(numero);
  }

  regenerar(numero: string): void {
    this.numero.set(numero);
    this.progresso.set([]);
    this.versoes.set([]);
    this.versaoAtiva.set(0);
    this.secaoAtiva.set('relatorio');
    this.erro.set(null);
    this.gerar(numero);
  }

  close(): void {
    this.isOpen.set(false);
  }

  carregarHistorico(numero: string): void {
    fetch(API.minutaHistorico(numero))
      .then(res => res.ok ? res.json() : [])
      .then((items: MinutaHistoricoItem[]) => {
        this.ngZone.run(() => this.historico.set(items));
      })
      .catch(() => {/* silently ignore */});
  }

  carregarVersaoDoHistorico(numero: string, versaoMinuta: number): void {
    fetch(API.minutaVersao(numero, versaoMinuta))
      .then(res => res.ok ? res.json() : null)
      .then((doc: { versoes: MinutaVersao[] } | null) => {
        if (!doc) return;
        this.ngZone.run(() => {
          this.numero.set(numero);
          this.versoes.set(doc.versoes ?? []);
          this.versaoAtiva.set(0);
          this.secaoAtiva.set('relatorio');
          this.erro.set(null);
          this.versaoAtualNumero.set(versaoMinuta);
          this.isOpen.set(true);
        });
      })
      .catch(() => {/* silently ignore */});
  }

  private gerar(numero: string): void {
    this.isGenerating.set(true);

    fetch(API.minutaGerar, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
      body: JSON.stringify({ numero, versoes: [] }),
    }).then(async (res) => {
      if (!res.ok || !res.body) {
        this.ngZone.run(() => {
          this.erro.set('Erro ao iniciar geração da minuta.');
          this.isGenerating.set(false);
        });
        return;
      }

      const reader = res.body.pipeThrough(new TextDecoderStream()).getReader();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += value;
        const events = buffer.split('\n\n');
        buffer = events.pop() ?? '';

        for (const event of events) {
          const dataLine = event
            .split('\n')
            .filter(l => l.startsWith('data:'))
            .map(l => l.slice(5))
            .join('');

          if (!dataLine.trim()) continue;

          try {
            const evt: MinutaEvento = JSON.parse(dataLine);
            this.ngZone.run(() => this.handleEvento(evt));
          } catch {}
        }
      }

      this.ngZone.run(() => this.isGenerating.set(false));

    }).catch(err => {
      this.ngZone.run(() => {
        this.erro.set(err?.message ?? 'Erro desconhecido.');
        this.isGenerating.set(false);
      });
    });
  }

  private handleEvento(evt: MinutaEvento): void {
    switch (evt.evento) {
      case 'step': {
        this.progresso.update(steps => {
          const ultimo = steps[steps.length - 1];
          if (ultimo && !ultimo.concluido && ultimo.acao === evt.acao) {
            return [...steps.slice(0, -1), { ...ultimo, concluido: true }];
          }
          return [...steps, { acao: evt.acao, mensagem: evt.mensagem, concluido: false }];
        });
        break;
      }
      case 'versao': {
        this.progresso.update(steps =>
          steps.map(s => ({ ...s, concluido: true }))
        );
        this.versoes.update(v => [...v, evt.dados]);
        break;
      }
      case 'done': {
        this.progresso.update(steps => steps.map(s => ({ ...s, concluido: true })));
        this.versaoAtualNumero.set(evt.versaoMinuta);
        this.carregarHistorico(this.numero());
        break;
      }
      case 'erro': {
        this.erro.set(evt.mensagem);
        break;
      }
    }
  }
}
