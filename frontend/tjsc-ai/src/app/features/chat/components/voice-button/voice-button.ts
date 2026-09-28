import { Component, inject } from '@angular/core';
import { VoiceService, VoiceState } from '../../shared/services/voice.service';

@Component({
  selector: 'app-voice-button',
  template: `
    <button
      (click)="toggle()"
      [class]="buttonClass()"
      [title]="label()"
      [attr.aria-label]="label()"
      type="button"
    >
      @switch (voice.state()) {
        @case ('connecting') {
          <span class="w-4 h-4 rounded-full border-2 border-current border-t-transparent animate-spin block"></span>
        }
        @case ('listening') {
          <svg class="w-4 h-4 animate-pulse" fill="currentColor" viewBox="0 0 24 24">
            <path d="M12 1a4 4 0 0 1 4 4v6a4 4 0 0 1-8 0V5a4 4 0 0 1 4-4zm0 2a2 2 0 0 0-2 2v6a2 2 0 0 0 4 0V5a2 2 0 0 0-2-2zm6.364 5.636a.75.75 0 0 1 .736.912A7.003 7.003 0 0 1 12.75 15.93V18h2.25a.75.75 0 0 1 0 1.5h-6a.75.75 0 0 1 0-1.5H11.25v-2.07a7.003 7.003 0 0 1-6.35-6.382.75.75 0 0 1 1.488-.176A5.5 5.5 0 0 0 17.5 11a5.472 5.472 0 0 0-.046-.655.75.75 0 0 1 .91-.709z"/>
          </svg>
        }
        @case ('speaking') {
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M15.536 8.464a5 5 0 0 1 0 7.072M12 6v12M9 8.464a5 5 0 0 0 0 7.072M18.364 5.636a9 9 0 0 1 0 12.728M5.636 5.636a9 9 0 0 0 0 12.728"/>
          </svg>
        }
        @default {
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M19 11a7 7 0 0 1-7 7m0 0a7 7 0 0 1-7-7m7 7v4m0 0H8m4 0h4M12 3a4 4 0 0 1 4 4v4a4 4 0 0 1-8 0V7a4 4 0 0 1 4-4z"/>
          </svg>
        }
      }
    </button>

    @if (voice.transcript()) {
      <span class="text-xs text-gray-400 italic truncate max-w-[160px]">{{ voice.transcript() }}</span>
    }
  `,
  host: { class: 'flex items-center gap-2' },
})
export class VoiceButtonComponent {
  protected readonly voice = inject(VoiceService);

  toggle(): void {
    this.voice.isActive ? this.voice.stop() : this.voice.start();
  }

  label(): string {
    const labels: Record<VoiceState, string> = {
      idle:       'Iniciar conversa por voz',
      connecting: 'Conectando...',
      listening:  'Ouvindo — clique para encerrar',
      speaking:   'Respondendo — clique para encerrar',
      error:      'Erro — clique para tentar novamente',
    };
    return labels[this.voice.state()];
  }

  buttonClass(): string {
    const base = 'p-2 rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-indigo-500';
    const map: Record<VoiceState, string> = {
      idle:       `${base} text-gray-400 hover:text-indigo-400 hover:bg-gray-800`,
      connecting: `${base} text-indigo-400 bg-gray-800 cursor-wait`,
      listening:  `${base} text-red-400 bg-red-900/20 hover:bg-red-900/40`,
      speaking:   `${base} text-indigo-400 bg-indigo-900/20 hover:bg-indigo-900/40`,
      error:      `${base} text-red-500 hover:bg-gray-800`,
    };
    return map[this.voice.state()];
  }
}
