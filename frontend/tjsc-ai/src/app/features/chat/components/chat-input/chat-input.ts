import { Component, computed, effect, ElementRef, inject, input, output, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ChatService } from '../../shared/services/chat.service';
import { MentionService, MentionResult } from '../../shared/services/mention.service';
import { VoiceButtonComponent } from '../voice-button/voice-button';
import { VoiceService } from '../../shared/services/voice.service';
import { Subject } from 'rxjs';
import { debounceTime, switchMap } from 'rxjs/operators';

export interface PromptTemplate {
  label: string;
  text: string;
}

export interface TemplateCategory {
  category: string;
  templates: PromptTemplate[];
}

const MENTION_RE = /@([^@:\n]*)$/;

@Component({
  selector: 'app-chat-input',
  imports: [FormsModule, TranslatePipe, VoiceButtonComponent],
  templateUrl: './chat-input.html',
  styleUrl: './chat-input.css',
})
export class ChatInputComponent {
  private readonly chat = inject(ChatService);
  protected readonly voice = inject(VoiceService);
  private readonly mentionSvc = inject(MentionService);
  private readonly translate = inject(TranslateService);

  readonly disabled = input(false);
  readonly send = output<string>();
  readonly stop = output<void>();

  @ViewChild('textarea') private textarea!: ElementRef<HTMLTextAreaElement>;

  readonly value = signal('');
  readonly templatesOpen = signal(false);
  protected readonly waveformBars = Array.from({ length: 17 }, (_, index) => index);

  readonly showSuggestions = computed(
    () => !this.voice.isActive && !this.chat.hasMessages() && !this.value() && !this.disabled()
  );

  readonly suggestions = computed<string[]>(() => {
    const result = this.translate.instant('sugestoes');
    return Array.isArray(result) ? result : [];
  });

  readonly templateCategories = computed<TemplateCategory[]>(() => {
    const t = (k: string) => this.translate.instant(k);
    const it = (k: string) => this.translate.instant(`templates.itens.${k}`);
    return [
      {
        category: t('templates.categorias.processos'),
        templates: [
          it('maior_valor'),
          it('mais_movimentacoes'),
          it('por_classe'),
          it('arquivados'),
          it('sentenca_homologatoria'),
        ].map(i => ({ label: i.label, text: i.texto })),
      },
      {
        category: t('templates.categorias.magistrados'),
        templates: [
          it('mais_produtivos'),
          it('por_comarca_mag'),
          it('distribuicao_mag'),
        ].map(i => ({ label: i.label, text: i.texto })),
      },
      {
        category: t('templates.categorias.comarcas'),
        templates: [
          it('em_andamento_com'),
          it('ranking_comarcas'),
          it('comparativo'),
        ].map(i => ({ label: i.label, text: i.texto })),
      },
      {
        category: t('templates.categorias.estatisticas'),
        templates: [
          it('visao_geral'),
          it('distribuicao_classe'),
          it('evolucao'),
        ].map(i => ({ label: i.label, text: i.texto })),
      },
    ];
  });

  readonly mentionResults = signal<MentionResult[]>([]);
  readonly mentionLoading = signal(false);
  readonly mentionActiveIndex = signal(0);
  readonly mentionOpen = signal(false);

  private mentionStart = -1;
  private mentionSearch$ = new Subject<string>();

  private history: string[] = [];
  private historyIndex = -1;
  private draft = '';

  constructor() {
    effect(() => {
      const pending = this.chat.pendingInput();
      if (!pending) return;
      this.setValueAndResize(pending.value);
      setTimeout(() => this.textarea?.nativeElement.focus());
    });

    this.mentionSearch$
      .pipe(
        debounceTime(180),
        switchMap((q) => {
          this.mentionLoading.set(true);
          return this.mentionSvc.search(q);
        })
      )
      .subscribe((results) => {
        this.mentionResults.set(results);
        this.mentionActiveIndex.set(0);
        this.mentionLoading.set(false);
      });
  }

  toggleTemplates(): void {
    this.templatesOpen.update((v) => !v);
    this.closeMention();
  }

  selectTemplate(template: PromptTemplate): void {
    this.setValueAndResize(template.text);
    this.templatesOpen.set(false);
    setTimeout(() => this.textarea?.nativeElement.focus());
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape' && this.templatesOpen()) {
      this.templatesOpen.set(false);
      return;
    }

    if (this.mentionOpen()) {
      if (event.key === 'ArrowDown') {
        event.preventDefault();
        this.mentionActiveIndex.update((i) =>
          Math.min(i + 1, this.mentionResults().length - 1)
        );
        return;
      }
      if (event.key === 'ArrowUp') {
        event.preventDefault();
        this.mentionActiveIndex.update((i) => Math.max(i - 1, 0));
        return;
      }
      if (event.key === 'Enter' || event.key === 'Tab') {
        event.preventDefault();
        const r = this.mentionResults()[this.mentionActiveIndex()];
        if (r) this.insertMention(r);
        return;
      }
      if (event.key === 'Escape') {
        this.closeMention();
        return;
      }
    }

    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.submit();
      return;
    }

    if (event.key === 'ArrowUp') {
      const el = this.textarea.nativeElement;
      const onFirstLine = el.value.lastIndexOf('\n', el.selectionStart - 1) === -1;
      if (onFirstLine && this.history.length > 0) {
        event.preventDefault();
        if (this.historyIndex === -1) this.draft = this.value();
        this.historyIndex = Math.min(this.historyIndex + 1, this.history.length - 1);
        this.setValueAndResize(this.history[this.historyIndex]);
      }
      return;
    }

    if (event.key === 'ArrowDown') {
      if (this.historyIndex === -1) return;
      event.preventDefault();
      this.historyIndex--;
      const next = this.historyIndex === -1 ? this.draft : this.history[this.historyIndex];
      this.setValueAndResize(next);
    }
  }

  submit(): void {
    const text = this.value().trim();
    if (!text || this.disabled()) return;
    this.history.unshift(text);
    this.historyIndex = -1;
    this.draft = '';
    this.send.emit(text);
    this.value.set('');
    this.resetHeight();
    this.closeMention();
    this.templatesOpen.set(false);
    setTimeout(() => this.textarea?.nativeElement.focus());
  }

  selectSuggestion(text: string): void {
    this.value.set(text);
    setTimeout(() => {
      this.textarea?.nativeElement.focus();
      this.submit();
    });
  }

  onInput(event: Event): void {
    const el = event.target as HTMLTextAreaElement;
    this.historyIndex = -1;
    this.value.set(el.value);
    el.style.height = 'auto';
    el.style.height = Math.min(el.scrollHeight, 200) + 'px';
    this.checkMention(el);
  }

  selectMention(result: MentionResult): void {
    this.insertMention(result);
  }

  private checkMention(el: HTMLTextAreaElement): void {
    const before = el.value.slice(0, el.selectionStart);
    const match = MENTION_RE.exec(before);
    if (match) {
      this.mentionStart = match.index;
      this.mentionOpen.set(true);
      this.mentionSearch$.next(match[1]);
    } else {
      this.closeMention();
    }
  }

  private insertMention(result: MentionResult): void {
    const el = this.textarea.nativeElement;
    const before = el.value.slice(0, this.mentionStart);
    const after = el.value.slice(el.selectionStart);
    const inserted = result.value + ' ';
    const newVal = before + inserted + after;
    this.value.set(newVal);
    el.value = newVal;
    el.style.height = 'auto';
    el.style.height = Math.min(el.scrollHeight, 200) + 'px';
    const pos = before.length + inserted.length;
    setTimeout(() => {
      el.focus();
      el.setSelectionRange(pos, pos);
    });
    this.closeMention();
  }

  private closeMention(): void {
    this.mentionOpen.set(false);
    this.mentionResults.set([]);
    this.mentionStart = -1;
  }

  private setValueAndResize(text: string): void {
    this.value.set(text);
    const el = this.textarea?.nativeElement;
    if (!el) return;
    el.value = text;
    el.style.height = 'auto';
    el.style.height = Math.min(el.scrollHeight, 200) + 'px';
    setTimeout(() => el.setSelectionRange(text.length, text.length));
  }

  private resetHeight(): void {
    const el = this.textarea?.nativeElement;
    if (el) el.style.height = 'auto';
  }
}
