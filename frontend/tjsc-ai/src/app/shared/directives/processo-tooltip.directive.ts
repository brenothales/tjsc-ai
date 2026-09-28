import { Directive, HostListener, inject, OnDestroy } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { ChatService } from '../../features/chat/shared/services/chat.service';
import { ProcessoPanelService } from '../../features/chat/shared/services/processo-panel.service';
import {
  ContextoResponse,
  MovimentacaoDto,
  ProcessoApiService,
} from '../../features/chat/shared/services/processo.api.service';

const INSIGHT_KEYS = ['resumo', 'risco', 'timeline'] as const;
type InsightKey = typeof INSIGHT_KEYS[number];

const TOOLTIP_ID = 'processo-tooltip';

@Directive({ selector: '[processoTooltip]' })
export class ProcessoTooltipDirective implements OnDestroy {
  private readonly api = inject(ProcessoApiService);
  private readonly panelService = inject(ProcessoPanelService);
  private readonly chatService = inject(ChatService);
  private readonly translate = inject(TranslateService);

  private cache = new Map<string, ContextoResponse | null>();
  private activeNumero: string | null = null;
  private activeAnchor: HTMLElement | null = null;
  private showTimeout: ReturnType<typeof setTimeout> | null = null;
  private hideTimeout: ReturnType<typeof setTimeout> | null = null;

  @HostListener('click', ['$event'])
  onClick(event: MouseEvent): void {
    const span = (event.target as HTMLElement).closest('.processo-ref') as HTMLElement | null;
    if (!span) return;
    const numero = span.dataset['numero'];
    if (!numero) return;
    this.clearAll();
    const cached = this.cache.get(numero) ?? null;
    this.panelService.open(numero, cached);
  }

  @HostListener('mouseover', ['$event'])
  onMouseOver(event: MouseEvent): void {
    const span = (event.target as HTMLElement).closest('.processo-ref') as HTMLElement | null;
    if (!span) return;
    const numero = span.dataset['numero'];
    if (!numero) return;
    if (numero === this.activeNumero) { this.clearHideTimeout(); return; }
    this.clearAll();
    this.activeNumero = numero;
    this.activeAnchor = span;
    this.showTimeout = setTimeout(() => {
      this.showLoading(span);
      this.fetchAndUpdate(numero, span);
    }, 80);
  }

  @HostListener('mouseout', ['$event'])
  onMouseOut(event: MouseEvent): void {
    const to = event.relatedTarget as HTMLElement | null;
    const tooltip = document.getElementById(TOOLTIP_ID);
    if (tooltip?.contains(to)) return;
    if (this.activeAnchor && (this.activeAnchor === to || this.activeAnchor.contains(to))) return;
    if ((to as HTMLElement | null)?.closest?.('.processo-ref')) return;
    this.clearShowTimeout();
    this.scheduleHide();
  }

  ngOnDestroy(): void {
    this.clearAll();
  }

  private t(key: string, params?: Record<string, unknown>): string {
    return this.translate.instant(key, params);
  }

  private fetchAndUpdate(numero: string, anchor: HTMLElement): void {
    if (this.cache.has(numero)) {
      this.render(anchor, this.cache.get(numero)!);
      return;
    }
    this.api.buscarContexto(numero).subscribe({
      next: (ctx) => {
        this.cache.set(numero, ctx);
        if (this.activeNumero === numero) this.render(anchor, ctx);
      },
      error: () => {
        this.cache.set(numero, null);
        if (this.activeNumero === numero) this.renderError();
      },
    });
  }

  private showLoading(anchor: HTMLElement): void {
    const tooltip = this.getOrCreateTooltip();
    tooltip.innerHTML = `<div style="${styles.loading}">${this.t('tooltip.consultando')}</div>`;
    this.position(tooltip, anchor);
  }

  private renderError(): void {
    const tooltip = document.getElementById(TOOLTIP_ID);
    if (tooltip)
      tooltip.innerHTML = `<div style="${styles.error}">${this.t('tooltip.nao_encontrado')}</div>`;
  }

  private render(anchor: HTMLElement, ctx: ContextoResponse): void {
    const tooltip = this.getOrCreateTooltip();
    const p = ctx.processo;
    const situacao = ctx.situacaoNormalizada ?? p.situacao;
    const light = this.isLight();

    const ativos = ctx.partes.filter((pt) => pt.polo === 'ativo');
    const passivos = ctx.partes.filter((pt) => pt.polo === 'passivo');
    const magistrado = ctx.magistrados?.[0] ?? null;
    const movs = ctx.movimentacoes ?? [];
    const ultimaMov: MovimentacaoDto | null = movs[0] ?? null;

    const t = (k: string) => this.t(k);

    tooltip.innerHTML = `
      ${header(p.numero, situacao, light)}
      ${divider(light)}

      ${section(t('tooltip.secoes.processo'), light)}
      ${row(t('painel.campos.classe'),   p.classe, light)}
      ${row(t('painel.campos.assunto'),  p.assunto, light)}
      ${row(t('painel.campos.comarca'),  p.comarca, light)}
      ${row(t('painel.campos.autuacao'), formatDate(p.dataAutuacao), light)}
      ${p.dataSentenca ? row(t('painel.campos.sentenca'), formatDate(p.dataSentenca), light) : ''}
      ${p.valorCausa != null ? row(t('painel.campos.valor'), formatCurrency(p.valorCausa), light) : ''}

      ${ativos.length || passivos.length ? divider(light) : ''}
      ${ativos.length || passivos.length ? section(t('tooltip.secoes.partes'), light) : ''}
      ${ativos.map((pt) => parteRow(t('tooltip.polo_ativo'), pt.nome)).join('')}
      ${passivos.map((pt) => parteRow(t('tooltip.polo_passivo'), pt.nome)).join('')}

      ${magistrado ? divider(light) : ''}
      ${magistrado ? section(t('tooltip.secoes.magistrado'), light) : ''}
      ${magistrado ? magistradoRow(magistrado.nome, magistrado.situacao) : ''}

      ${ultimaMov ? divider(light) : ''}
      ${ultimaMov ? section(`${t('tooltip.secoes.movimentacoes')} <span style="color:${light ? '#71717a' : '#6b7280'};font-weight:400">(${movs.length} no total)</span>`, light) : ''}
      ${ultimaMov ? movimentacaoRow(ultimaMov, light) : ''}

      ${divider(light)}
      ${this.insightActionsHtml(p.numero, light)}
    `;

    tooltip.querySelectorAll<HTMLButtonElement>('[data-insight]').forEach((btn) => {
      btn.addEventListener('mousedown', (e) => {
        e.preventDefault();
        this.sendInsight(btn.dataset['numero']!, btn.dataset['insight']! as InsightKey);
      });
    });

    this.position(tooltip, anchor);
  }

  private insightActionsHtml(numero: string, light: boolean): string {
    const bg = light ? '#f4f4f5' : '#1f2937';
    const border = light ? '#e4e4e7' : '#374151';
    const color = light ? '#52525b' : '#9ca3af';
    const hoverBg = light ? '#d1fae5' : '#064e3b';
    const hoverColor = light ? '#047857' : '#6ee7b7';
    const btns = INSIGHT_KEYS.map(key => `
      <button
        data-insight="${key}"
        data-numero="${numero}"
        style="flex:1;padding:5px 4px;border-radius:6px;font-size:10px;font-weight:600;background:${bg};border:1px solid ${border};color:${color};cursor:pointer;transition:background .15s,color .15s;white-space:nowrap;"
        onmouseover="this.style.background='${hoverBg}';this.style.color='${hoverColor}'"
        onmouseout="this.style.background='${bg}';this.style.color='${color}'"
      >${this.t(`insights.${key}`)}</button>
    `).join('');
    return `<div style="display:flex;gap:4px;margin-top:2px">${btns}</div>`;
  }

  private isLight(): boolean {
    return document.documentElement.classList.contains('light');
  }

  private getOrCreateTooltip(): HTMLElement {
    let el = document.getElementById(TOOLTIP_ID);
    if (!el) {
      el = document.createElement('div');
      el.id = TOOLTIP_ID;
      const light = this.isLight();
      Object.assign(el.style, {
        position: 'fixed',
        zIndex: '9999',
        background: light ? '#ffffff' : '#0e0e12',
        border: `1px solid ${light ? '#e4e4e7' : '#3f3f46'}`,
        borderRadius: '10px',
        padding: '14px 16px',
        boxShadow: light
          ? '0 8px 32px rgba(0,0,0,.12), 0 2px 8px rgba(0,0,0,.08)'
          : '0 8px 32px rgba(0,0,0,.9), 0 0 0 1px rgba(255,255,255,.04)',
        maxWidth: '300px',
        minWidth: '220px',
        pointerEvents: 'auto',
        fontFamily: 'inherit',
        color: light ? '#18181b' : '#d4d4d8',
      });
      el.addEventListener('mouseenter', () => this.clearHideTimeout());
      el.addEventListener('mouseleave', () => this.scheduleHide());
      document.body.appendChild(el);
    }
    return el;
  }

  private position(tooltip: HTMLElement, anchor: HTMLElement): void {
    const rect = anchor.getBoundingClientRect();
    const gap = 2;
    tooltip.style.visibility = 'hidden';
    tooltip.style.display = 'block';
    const tw = tooltip.offsetWidth;
    const th = tooltip.offsetHeight;
    let top = rect.bottom + gap;
    let left = rect.left;
    if (left + tw > window.innerWidth - 8) left = window.innerWidth - tw - 8;
    if (top + th > window.innerHeight - 8) top = rect.top - th - gap;
    if (left < 8) left = 8;
    tooltip.style.top = `${top}px`;
    tooltip.style.left = `${left}px`;
    tooltip.style.visibility = 'visible';
  }

  private scheduleHide(): void {
    this.hideTimeout = setTimeout(() => {
      this.activeNumero = null;
      this.activeAnchor = null;
      document.getElementById(TOOLTIP_ID)?.remove();
    }, 500);
  }

  private clearHideTimeout(): void {
    if (this.hideTimeout) { clearTimeout(this.hideTimeout); this.hideTimeout = null; }
  }

  private clearShowTimeout(): void {
    if (this.showTimeout) { clearTimeout(this.showTimeout); this.showTimeout = null; }
  }

  private sendInsight(numero: string, key: InsightKey): void {
    this.clearAll();
    const prompt = this.translate.instant(`insights.prompt_${key}`, { numero });
    this.chatService.sendMessage(prompt, { insightKey: `${key}:${numero}` });
  }

  private clearAll(): void {
    this.clearShowTimeout();
    this.clearHideTimeout();
    this.activeNumero = null;
    this.activeAnchor = null;
    document.getElementById(TOOLTIP_ID)?.remove();
  }
}

// ── template helpers ──────────────────────────────────────────────────────────

const styles = {
  loading: 'padding:10px;color:#6b7280;font-size:12px',
  error:   'padding:10px;color:#ef4444;font-size:12px',
};

function header(numero: string, situacao: string, light: boolean): string {
  return `
    <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;margin-bottom:10px">
      <span style="font-size:11px;font-weight:700;color:${light ? '#047857' : '#34d399'};letter-spacing:.3px;word-break:break-all">${numero}</span>
      <span style="flex-shrink:0;padding:2px 7px;border-radius:4px;font-size:10px;font-weight:700;background:${situacaoBg(situacao)};color:#fff;white-space:nowrap">${situacao}</span>
    </div>`;
}

function section(label: string, light: boolean): string {
  return `<div style="font-size:10px;font-weight:700;color:${light ? '#a1a1aa' : '#4b5563'};text-transform:uppercase;letter-spacing:.8px;margin:6px 0 4px">${label}</div>`;
}

function divider(light: boolean): string {
  return `<div style="border-top:1px solid ${light ? '#e4e4e7' : '#27272a'};margin:8px 0"></div>`;
}

function row(label: string, value: string, light: boolean): string {
  return `
    <div style="display:flex;gap:6px;margin-bottom:3px;font-size:12px;line-height:1.4">
      <span style="color:${light ? '#71717a' : '#4b5563'};flex-shrink:0;min-width:72px">${label}</span>
      <span style="color:${light ? '#18181b' : '#d1d5db'}">${value}</span>
    </div>`;
}

function parteRow(polo: string, nome: string): string {
  const color = polo === 'Ativo' ? '#10b981' : '#f87171';
  const light = document.documentElement.classList.contains('light');
  return `
    <div style="display:flex;gap:6px;margin-bottom:3px;font-size:12px;align-items:baseline">
      <span style="color:${color};flex-shrink:0;font-size:10px;font-weight:600;min-width:48px">${polo}</span>
      <span style="color:${light ? '#18181b' : '#d1d5db'}">${nome}</span>
    </div>`;
}

function magistradoRow(nome: string, situacao: string): string {
  const cor = situacao?.toLowerCase() === 'ativo' ? '#10b981' : '#9ca3af';
  const light = document.documentElement.classList.contains('light');
  return `
    <div style="display:flex;align-items:center;gap:6px;font-size:12px">
      <span style="color:${light ? '#18181b' : '#d1d5db'}">${nome}</span>
      <span style="color:${cor};font-size:10px">(${situacao})</span>
    </div>`;
}

function movimentacaoRow(mov: MovimentacaoDto, light: boolean): string {
  return `
    <div style="font-size:12px;line-height:1.4">
      <span style="color:${light ? '#a1a1aa' : '#6b7280'}">${formatDate(mov.dataMov)}</span>
      <span style="color:${light ? '#18181b' : '#d1d5db'};margin-left:6px">${mov.descricao}</span>
    </div>`;
}

function formatDate(value: string | null): string {
  if (!value) return '—';
  const [y, m, d] = value.split('-');
  return `${d}/${m}/${y}`;
}

function formatCurrency(value: number): string {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function situacaoBg(situacao: string): string {
  const s = situacao.toLowerCase();
  if (s.includes('arquivado') || s.includes('extinto')) return '#374151';
  if (s.includes('julgado') || s.includes('procedente')) return '#059669';
  if (s.includes('andamento') || s.includes('ativo')) return '#2563eb';
  if (s.includes('suspenso')) return '#d97706';
  return '#7c3aed';
}
