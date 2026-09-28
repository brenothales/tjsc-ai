import { Component, computed, inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { marked } from 'marked';
import { MinutaService } from '../../shared/services/minuta.service';
import { MinutaSecao, MinutaVersao } from '../../shared/interfaces/minuta.interface';

interface SecaoDef { id: MinutaSecao; label: string }
interface StepDef   { acao: string; label: string }

@Component({
  selector: 'app-minuta-modal',
  imports: [TranslatePipe],
  templateUrl: './minuta-modal.html',
})
export class MinutaModalComponent {
  protected readonly svc   = inject(MinutaService);
  private  readonly san    = inject(DomSanitizer);
  private  readonly translate = inject(TranslateService);

  get secoes(): SecaoDef[] {
    const t = (k: string) => this.translate.instant(k);
    return [
      { id: 'relatorio',     label: t('minuta.secoes.relatorio')     },
      { id: 'fundamentacao', label: t('minuta.secoes.fundamentacao') },
      { id: 'dispositivo',   label: t('minuta.secoes.dispositivo')   },
    ];
  }

  private get stepsEsperados(): StepDef[] {
    const t = (k: string) => this.translate.instant(k);
    return [
      { acao: 'BUSCAR_DADOS',          label: t('minuta.steps.buscar_dados')               },
      { acao: 'REDIGIR_RELATORIO',     label: t('minuta.steps.relatorio')                  },
      { acao: 'REDIGIR_FUNDAMENTACAO', label: t('minuta.steps.fundamentacao_procedente')   },
      { acao: 'REDIGIR_DISPOSITIVO',   label: t('minuta.steps.dispositivo_procedente')     },
      { acao: 'REDIGIR_FUNDAMENTACAO', label: t('minuta.steps.fundamentacao_improcedente') },
      { acao: 'REDIGIR_DISPOSITIVO',   label: t('minuta.steps.dispositivo_improcedente')   },
      { acao: 'REDIGIR_FUNDAMENTACAO', label: t('minuta.steps.fundamentacao_parcial')      },
      { acao: 'REDIGIR_DISPOSITIVO',   label: t('minuta.steps.dispositivo_parcial')        },
    ];
  }

  readonly versaoAtual = computed<MinutaVersao | null>(() =>
    this.svc.versoes()[this.svc.versaoAtiva()] ?? null
  );

  readonly stepsView = computed(() => {
    const progresso = this.svc.progresso();
    return this.stepsEsperados.map((def, i) => {
      const passo = progresso[i];
      const status = !passo ? 'pending' : passo.concluido ? 'done' : 'active';
      return { acao: def.acao, label: def.label, status };
    });
  });

  readonly progressoPct = computed(() => {
    const done = this.svc.progresso().filter(s => s.concluido).length;
    return Math.min(Math.round((done / this.stepsEsperados.length) * 100), 95);
  });

  readonly versaoEmGeracao = computed(() => {
    const ativo = this.svc.progresso().find(s => !s.concluido);
    if (!ativo) return null;
    const m = ativo.mensagem.match(/\(([^)]+)\)/);
    return m ? m[1] : null;
  });

  iconePath(acao: string): string {
    switch (acao) {
      case 'BUSCAR_DADOS':
        return 'M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z';
      case 'REDIGIR_RELATORIO':
        return 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z';
      case 'REDIGIR_FUNDAMENTACAO':
        return 'M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4';
      case 'REDIGIR_DISPOSITIVO':
        return 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4';
      default:
        return 'M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z';
    }
  }

  readonly conteudoHtml = computed<SafeHtml>(() => {
    const v = this.versaoAtual();
    if (!v) return '';
    const texto = v[this.svc.secaoAtiva()];
    const html  = marked.parse(texto ?? '') as string;
    return this.san.bypassSecurityTrustHtml(html);
  });

  tipoLabel(tipo: string): string {
    return tipo.charAt(0).toUpperCase() + tipo.slice(1);
  }

  copiarSecao(): void {
    const v = this.versaoAtual();
    if (!v) return;
    navigator.clipboard.writeText(v[this.svc.secaoAtiva()]);
  }

  copiarCompleta(): void {
    const v = this.versaoAtual();
    if (!v) return;
    const txt = [v.relatorio, v.fundamentacao, v.dispositivo].join('\n\n---\n\n');
    navigator.clipboard.writeText(txt);
  }
}
