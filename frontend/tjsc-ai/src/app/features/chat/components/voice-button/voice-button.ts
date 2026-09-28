import { Component, inject } from '@angular/core';
import { VoiceService, VoiceState } from '../../shared/services/voice.service';

const LABELS: Record<VoiceState, string> = {
  idle:           'Iniciar conversa por voz',
  connecting:     'Conectando...',
  listening:      'Aguardando — clique para encerrar',
  user_speaking:  'Ouvindo você...',
  processing:     'Processando...',
  agent_speaking: 'Respondendo — clique para interromper',
  tool_executing: 'Consultando agente...',
  error:          'Erro — clique para tentar novamente',
};

@Component({
  selector: 'app-voice-button',
  template: `
    <div class="flex items-center gap-2">
      <button
        (click)="onClick()"
        [title]="label()"
        [attr.aria-label]="label()"
        [class]="buttonClass()"
        type="button"
      >
        @switch (voice.state()) {
          @case ('connecting') {
            <span class="w-4 h-4 rounded-full border-2 border-current border-t-transparent animate-spin block"></span>
          }
          @case ('tool_executing') {
            <span class="w-4 h-4 rounded-full border-2 border-amber-400 border-t-transparent animate-spin block"></span>
          }
          @case ('user_speaking') {
            <!-- mic animado -->
            <svg class="w-4 h-4 animate-pulse" fill="currentColor" viewBox="0 0 24 24">
              <path d="M12 1a4 4 0 0 1 4 4v6a4 4 0 0 1-8 0V5a4 4 0 0 1 4-4zm6.364 10.636a.75.75 0 0 1 .736.912A7.003 7.003 0 0 1 12.75 18.93V21h2.25a.75.75 0 0 1 0 1.5h-6a.75.75 0 0 1 0-1.5H11.25v-2.07a7.003 7.003 0 0 1-6.35-6.382.75.75 0 0 1 1.488-.176A5.5 5.5 0 0 0 17.5 14a5.472 5.472 0 0 0-.046-.655.75.75 0 0 1 .91-.709z"/>
            </svg>
          }
          @case ('agent_speaking') {
            <!-- waveform -->
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M15.536 8.464a5 5 0 0 1 0 7.072M12 6v12M9 8.464a5 5 0 0 0 0 7.072M18.364 5.636a9 9 0 0 1 0 12.728M5.636 5.636a9 9 0 0 0 0 12.728"/>
            </svg>
          }
          @default {
            <!-- mic idle -->
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M19 11a7 7 0 0 1-7 7m0 0a7 7 0 0 1-7-7m7 7v4m0 0H8m4 0h4M12 3a4 4 0 0 1 4 4v4a4 4 0 0 1-8 0V7a4 4 0 0 1 4-4z"/>
            </svg>
          }
        }
      </button>

      @if (voice.isActive) {
        <span class="text-xs truncate max-w-[180px]" [class]="subtitleClass()">
          {{ subtitle() }}
        </span>
      }

      @if (voice.state() === 'error' && voice.errorMessage()) {
        <span class="text-xs text-red-400 truncate max-w-[180px]">{{ voice.errorMessage() }}</span>
      }
    </div>
  `,
})
export class VoiceButtonComponent {
  protected readonly voice = inject(VoiceService);

  onClick(): void {
    if (this.voice.state() === 'agent_speaking') {
      this.voice.interrupt();
    } else if (this.voice.isActive) {
      this.voice.stop();
    } else {
      this.voice.start();
    }
  }

  label(): string {
    return LABELS[this.voice.state()];
  }

  subtitle(): string {
    const s = this.voice.state();
    if (s === 'user_speaking' && this.voice.transcript()) return this.voice.transcript();
    if (s === 'agent_speaking' && this.voice.agentText())   return this.voice.agentText();
    if (s === 'tool_executing') return 'Consultando agente...';
    return '';
  }

  subtitleClass(): string {
    const s = this.voice.state();
    if (s === 'agent_speaking') return 'text-indigo-300 italic';
    return 'text-gray-400 italic';
  }

  buttonClass(): string {
    const base = 'p-2 rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-indigo-500 shrink-0';
    const map: Record<VoiceState, string> = {
      idle:           `${base} text-gray-400 hover:text-indigo-400 hover:bg-gray-800`,
      connecting:     `${base} text-indigo-400 bg-gray-800 cursor-wait`,
      listening:      `${base} text-green-400 bg-green-900/20 hover:bg-green-900/40`,
      user_speaking:  `${base} text-red-400 bg-red-900/20 hover:bg-red-900/40`,
      processing:     `${base} text-amber-400 bg-amber-900/20 cursor-wait`,
      agent_speaking: `${base} text-indigo-400 bg-indigo-900/20 hover:bg-indigo-900/40`,
      tool_executing: `${base} text-amber-400 bg-amber-900/20 cursor-wait`,
      error:          `${base} text-red-500 hover:bg-gray-800`,
    };
    return map[this.voice.state()];
  }
}
