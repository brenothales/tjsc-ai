import { Component, inject } from '@angular/core';
import { VoiceService, VoiceState } from '../../shared/services/voice.service';

@Component({
  selector: 'app-voice-button',
  template: `
    <div class="flex items-center gap-2">
      <button
        (click)="voice.toggle()"
        [title]="voice.buttonLabel()"
        [attr.aria-label]="voice.buttonLabel()"
        [class]="buttonClass()"
        [disabled]="voice.state() === 'closing'"
        type="button"
      >
        @if (voice.state() === 'closing') {
          <span class="w-3.5 h-3.5 rounded-full border-2 border-current border-t-transparent animate-spin" aria-hidden="true"></span>
          <span>Encerrando</span>
        } @else if (voice.isActive) {
          <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
            <rect x="6" y="6" width="12" height="12" rx="2"/>
          </svg>
          <span>Parar</span>
        } @else {
          @switch (voice.state()) {
            @case ('connecting') {
              <span class="w-4 h-4 rounded-full border-2 border-current border-t-transparent animate-spin block"></span>
            }
            @case ('tool_executing') {
              <span class="w-4 h-4 rounded-full border-2 border-amber-400 border-t-transparent animate-spin block"></span>
            }
            @default {
            <!-- mic idle -->
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M19 11a7 7 0 0 1-7 7m0 0a7 7 0 0 1-7-7m7 7v4m0 0H8m4 0h4M12 3a4 4 0 0 1 4 4v4a4 4 0 0 1-8 0V7a4 4 0 0 1 4-4z"/>
            </svg>
            }
          }
        }
      </button>

      @if (voice.state() === 'error' && voice.errorMessage()) {
        <span class="text-xs text-red-400 truncate max-w-[180px]">{{ voice.errorMessage() }}</span>
      }
    </div>
  `,
})
export class VoiceButtonComponent {
  protected readonly voice = inject(VoiceService);

  buttonClass(): string {
    const base = 'inline-flex items-center justify-center gap-2 px-2.5 py-2 rounded-lg text-xs font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-indigo-500 shrink-0';
    const map: Record<VoiceState, string> = {
      idle:           `${base} text-gray-400 hover:text-indigo-400 hover:bg-gray-800`,
      connecting:     `${base} text-red-300 bg-red-950/70 border border-red-800 hover:bg-red-900`,
      listening:      `${base} text-red-300 bg-red-950/70 border border-red-800 hover:bg-red-900`,
      user_speaking:  `${base} text-red-300 bg-red-950/70 border border-red-800 hover:bg-red-900`,
      processing:     `${base} text-red-300 bg-red-950/70 border border-red-800 hover:bg-red-900`,
      agent_speaking: `${base} text-red-300 bg-red-950/70 border border-red-800 hover:bg-red-900`,
      tool_executing: `${base} text-red-300 bg-red-950/70 border border-red-800 hover:bg-red-900`,
      closing:        `${base} text-gray-400 bg-gray-900 border border-gray-700 cursor-wait`,
      error:          `${base} text-red-500 hover:bg-gray-800`,
    };
    return map[this.voice.state()];
  }
}
