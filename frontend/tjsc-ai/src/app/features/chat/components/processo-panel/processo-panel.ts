import { Component, computed, effect, inject, signal, OnDestroy } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import {
  ContextoResponse,
  DocumentoTextoDto,
  MovimentacaoDto,
  ProcessoApiService,
} from '../../shared/services/processo.api.service';
import { ProcessoPanelService, PanelAba } from '../../shared/services/processo-panel.service';
import { ChatService } from '../../shared/services/chat.service';
import { MinutaService } from '../../shared/services/minuta.service';

const INSIGHT_KEYS = ['resumo', 'risco', 'timeline'] as const;
type InsightKey = typeof INSIGHT_KEYS[number];

const MIN_WIDTH = 320;
const MAX_WIDTH = 900;
const DEFAULT_WIDTH = 420;

@Component({
  selector: 'app-processo-panel',
  imports: [TranslatePipe],
  templateUrl: './processo-panel.html',
  styleUrl: './processo-panel.css',
})
export class ProcessoPanelComponent implements OnDestroy {
  protected readonly panel  = inject(ProcessoPanelService);
  private  readonly api    = inject(ProcessoApiService);
  protected readonly chat   = inject(ChatService);
  protected readonly minuta = inject(MinutaService);
  private  readonly translate = inject(TranslateService);

  readonly isEmAndamento = computed(() =>
    this.ctx()?.processo?.situacao?.toLowerCase().includes('andamento') ?? false
  );

  readonly partesAtivas  = computed(() => this.ctx()?.partes.filter(p => p.polo === 'ativo')   ?? []);
  readonly partesPassivas = computed(() => this.ctx()?.partes.filter(p => p.polo === 'passivo') ?? []);

  readonly panelWidth    = signal(DEFAULT_WIDTH);
  readonly resizing      = signal(false);
  readonly dropdownOpen  = signal(false);

  get insightActions() {
    return INSIGHT_KEYS.map(key => ({
      key,
      label: this.translate.instant(`insights.${key}`),
      prompt: (n: string) => this.translate.instant(`insights.prompt_${key}`, { numero: n }),
    }));
  }

  readonly infoRows = computed(() => {
    const c = this.ctx();
    if (!c) return [];
    const p = c.processo;
    const t = (k: string) => this.translate.instant(k);
    return [
      { label: t('painel.campos.classe'),   value: p.classe },
      { label: t('painel.campos.assunto'),  value: p.assunto },
      { label: t('painel.campos.comarca'),  value: p.comarca },
      { label: t('painel.campos.autuacao'), value: this.formatDate(p.dataAutuacao) },
      { label: t('painel.campos.sentenca'), value: p.dataSentenca ? this.formatDate(p.dataSentenca) : null },
      { label: t('painel.campos.valor'),    value: this.formatCurrency(p.valorCausa) },
    ].filter(r => r.value);
  });

  toggleDropdown(): void { this.dropdownOpen.update(v => !v); }
  closeDropdown():  void { this.dropdownOpen.set(false); }

  readonly ctx = signal<ContextoResponse | null>(null);
  readonly loadingCtx = signal(false);

  readonly movs = signal<MovimentacaoDto[]>([]);
  readonly currentPage = signal(0);
  readonly totalPages = signal(1);
  readonly totalMovs = signal<number | null>(null);
  readonly loadingMovs = signal(false);

  readonly docs = signal<DocumentoTextoDto[]>([]);
  readonly totalDocs = signal<number | null>(null);
  readonly loadingDocs = signal(false);
  readonly selectedDoc = signal<DocumentoTextoDto | null>(null);

  private trackNumero: string | null = null;
  private onMouseMove = (e: MouseEvent) => this.handleResize(e);
  private onMouseUp = () => this.stopResize();

  constructor() {
    effect(() => {
      const s = this.panel.state();
      if (!s) return;
      if (s.numero !== this.trackNumero) {
        this.trackNumero = s.numero;
        this.ctx.set(s.contexto);
        this.movs.set([]);
        this.currentPage.set(0);
        this.totalPages.set(1);
        this.totalMovs.set(null);
        this.docs.set([]);
        this.totalDocs.set(null);
        this.selectedDoc.set(null);
        if (!s.contexto) this.loadCtx(s.numero);
        this.loadMovs(s.numero, 0);
        this.loadDocs(s.numero);
        this.minuta.carregarHistorico(s.numero);
      }
    });
  }

  startResize(e: MouseEvent): void {
    e.preventDefault();
    this.resizing.set(true);
    document.addEventListener('mousemove', this.onMouseMove);
    document.addEventListener('mouseup', this.onMouseUp);
    document.body.style.userSelect = 'none';
    document.body.style.cursor = 'col-resize';
  }

  private handleResize(e: MouseEvent): void {
    const width = window.innerWidth - e.clientX;
    this.panelWidth.set(Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, width)));
  }

  private stopResize(): void {
    this.resizing.set(false);
    document.removeEventListener('mousemove', this.onMouseMove);
    document.removeEventListener('mouseup', this.onMouseUp);
    document.body.style.userSelect = '';
    document.body.style.cursor = '';
  }

  ngOnDestroy(): void {
    this.stopResize();
  }

  loadMore(): void {
    const s = this.panel.state();
    if (s) this.loadMovs(s.numero, this.currentPage() + 1);
  }

  selectDoc(doc: DocumentoTextoDto): void {
    this.selectedDoc.set(doc);
  }

  backToDocs(): void {
    this.selectedDoc.set(null);
  }

  setAba(aba: PanelAba): void {
    this.panel.setAba(aba);
    this.selectedDoc.set(null);
  }

  close(): void {
    this.panel.close();
  }

  resumirDoc(doc: DocumentoTextoDto, event: Event): void {
    event.stopPropagation();
    const numero = this.panel.state()?.numero;
    if (!numero) return;
    this.chat.setInput(this.docPrompt(doc.tipo, numero));
  }

  private docPrompt(tipo: string, numero: string): string {
    const t = tipo.toLowerCase();
    if (t.includes('sentença') || t.includes('sentenca'))
      return `Resuma a sentença do processo ${numero}: principais fundamentos e dispositivo`;
    if (t.includes('parecer'))
      return `Resuma o parecer do Ministério Público no processo ${numero}`;
    if (t.includes('contestação') || t.includes('contestacao'))
      return `Resuma os principais argumentos da contestação no processo ${numero}`;
    if (t.includes('petição inicial') || t.includes('peticao inicial'))
      return `Resuma a petição inicial do processo ${numero}: pedido e causa de pedir`;
    if (t.includes('ata'))
      return `Resuma a ata de audiência do processo ${numero}`;
    if (t.includes('decisão') || t.includes('decisao'))
      return `Resuma a decisão interlocutória do processo ${numero}`;
    if (t.includes('certidão') || t.includes('certidao'))
      return `Resuma a certidão do processo ${numero}`;
    return `Resuma o documento "${tipo}" do processo ${numero}`;
  }

  sendInsight(action: { key: InsightKey; prompt: (n: string) => string }): void {
    const numero = this.panel.state()?.numero;
    if (!numero) return;
    this.panel.close();
    this.chat.sendMessage(action.prompt(numero), { insightKey: `${action.key}:${numero}` });
  }

  hasMore(): boolean {
    return this.currentPage() + 1 < this.totalPages();
  }

  private loadMovs(numero: string, page: number): void {
    this.loadingMovs.set(true);
    this.api.movimentacoes(numero, page).subscribe({
      next: (res) => {
        this.movs.update((m) => (page === 0 ? res.data : [...m, ...res.data]));
        this.currentPage.set(page);
        this.totalPages.set(res.totalPages);
        this.totalMovs.set(res.totalElements);
        this.loadingMovs.set(false);
      },
      error: () => this.loadingMovs.set(false),
    });
  }

  private loadCtx(numero: string): void {
    this.loadingCtx.set(true);
    this.api.buscarContexto(numero).subscribe({
      next: (c) => { this.ctx.set(c); this.loadingCtx.set(false); },
      error: () => this.loadingCtx.set(false),
    });
  }

  private loadDocs(numero: string): void {
    this.loadingDocs.set(true);
    this.api.documentos(numero).subscribe({
      next: (res) => {
        this.docs.set(res.data);
        this.totalDocs.set(res.totalElements);
        this.loadingDocs.set(false);
      },
      error: () => this.loadingDocs.set(false),
    });
  }

  formatDate(value: string | null): string {
    if (!value) return '—';
    const [y, m, d] = value.split('-');
    return `${d}/${m}/${y}`;
  }

  formatCurrency(value: number | null): string {
    if (value == null) return '—';
    return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  situacaoClass(situacao: string): string {
    const s = situacao.toLowerCase();
    if (s.includes('arquivado') || s.includes('extinto'))
      return 'bg-gray-700 text-gray-300';
    if (s.includes('julgado') || s.includes('procedente'))
      return 'bg-emerald-900/60 text-emerald-300';
    if (s.includes('andamento') || s.includes('ativo'))
      return 'bg-blue-900/60 text-blue-300';
    if (s.includes('suspenso'))
      return 'bg-amber-900/60 text-amber-300';
    return 'bg-gray-700 text-gray-300';
  }
}
