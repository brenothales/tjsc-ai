import { inject, Injectable, signal } from '@angular/core';
import { API } from '../constants/api.constants';
import { ChatService } from './chat.service';

export type VoiceState =
  | 'idle'
  | 'connecting'
  | 'listening'
  | 'user_speaking'
  | 'processing'
  | 'agent_speaking'
  | 'tool_executing'
  | 'closing'
  | 'error';

interface RealtimeEvent {
  type: string;
  [key: string]: unknown;
}

@Injectable({ providedIn: 'root' })
export class VoiceService {
  private readonly chat = inject(ChatService);

  readonly state                = signal<VoiceState>('idle');
  readonly transcript           = signal<string>('');
  readonly agentText            = signal<string>('');
  readonly errorMessage         = signal<string>('');
  readonly voiceConversationId  = signal<string | null>(null);
  readonly messageAdded         = signal(0);

  private pc: RTCPeerConnection | null = null;
  private dc: RTCDataChannel | null = null;
  private audioEl: HTMLAudioElement | null = null;
  private closeTimeout: ReturnType<typeof setTimeout> | null = null;

  get isActive(): boolean {
    return this.state() !== 'idle' && this.state() !== 'error';
  }

  toggle(): void {
    if (this.isActive) this.stop();
    else void this.start();
  }

  buttonLabel(): string {
    switch (this.state()) {
      case 'idle': return 'Iniciar conversa por voz';
      case 'connecting': return 'Cancelar conexão de voz';
      case 'closing': return 'Encerrando sessão de voz…';
      case 'error': return 'Erro — clique para tentar novamente';
      default: return 'Parar conversa por voz';
    }
  }

  statusLabel(): string {
    switch (this.state()) {
      case 'connecting': return 'Conectando ao assistente…';
      case 'listening': return 'Estou ouvindo. Pode continuar falando.';
      case 'user_speaking': return 'Ouvindo você';
      case 'processing': return 'Preparando uma resposta…';
      case 'tool_executing': return 'Consultando informações…';
      case 'agent_speaking': return 'O assistente está respondendo';
      case 'closing': return 'Encerrando a sessão de voz…';
      default: return 'Conversa por voz ativa';
    }
  }

  caption(): string {
    if (this.state() === 'user_speaking') return this.transcript();
    if (this.state() === 'agent_speaking') return this.agentText();
    return '';
  }

  async start(): Promise<void> {
    if (this.isActive) return;

    if (!this.chat.conversationId()) {
      this.chat.startNewConversation();
    }
    const conversationId = this.chat.conversationId()!;
    this.voiceConversationId.set(conversationId);

    this.state.set('connecting');
    this.transcript.set('');
    this.agentText.set('');
    this.errorMessage.set('');

    try {
      this.pc = new RTCPeerConnection();
      this.audioEl = document.createElement('audio');
      this.audioEl.autoplay = true;

      this.pc.ontrack = (e) => {
        this.audioEl!.srcObject = e.streams[0];
      };

      this.pc.onconnectionstatechange = () => {
        const s = this.pc?.connectionState;
        if (s === 'disconnected' || s === 'failed') {
          this.stop();
        }
      };

      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      stream.getTracks().forEach((t) => this.pc!.addTrack(t, stream));

      this.dc = this.pc.createDataChannel('oai-events');
      this.dc.onmessage = (e) => this.handleEvent(JSON.parse(e.data) as RealtimeEvent);

      const offer = await this.pc.createOffer();
      await this.pc.setLocalDescription(offer);

      const res = await fetch(API.voiceSession, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sdp: offer.sdp }),
      });

      if (!res.ok) throw new Error(`Session error: ${res.status}`);
      const data = await res.json() as { sessionId: string; sdp: string };

      await this.pc.setRemoteDescription({ type: 'answer', sdp: data.sdp });
    } catch (err) {
      this.errorMessage.set(err instanceof Error ? err.message : 'Erro ao conectar');
      this.state.set('error');
      this.cleanup();
    }
  }

  stop(): void {
    if (this.state() === 'idle' || this.state() === 'closing') return;

    if (this.dc?.readyState === 'open') {
      this.state.set('closing');
      this.closeTimeout = setTimeout(() => {
        this.finishStop(false);
      }, 15_000);
      this.sendEvent({ type: 'session.close' });
      return;
    }

    this.finishStop();
  }

  private finishStop(confirmed = true): void {
    if (this.closeTimeout) {
      clearTimeout(this.closeTimeout);
      this.closeTimeout = null;
    }
    this.cleanup();
    this.state.set(confirmed ? 'idle' : 'error');
    if (!confirmed) {
      this.errorMessage.set('Não foi possível confirmar o encerramento da sessão no servidor.');
    }
    this.voiceConversationId.set(null);
  }

  interrupt(): void {
    if (this.state() === 'agent_speaking') {
      this.sendEvent({ type: 'response.cancel' });
    }
  }

  private handleEvent(event: RealtimeEvent): void {
    switch (event['type']) {
      case 'session.started':
        this.state.set('listening');
        break;

      case 'session.closed':
        this.finishStop();
        break;

      case 'input_audio_buffer.speech_started':
        this.transcript.set('');
        if (this.state() === 'agent_speaking') {
          this.interrupt();
        }
        this.state.set('user_speaking');
        break;

      case 'input_audio_buffer.speech_stopped':
        this.state.set('processing');
        break;

      case 'conversation.item.input_audio_transcription.completed':
        this.transcript.set((event['transcript'] as string) ?? '');
        break;

      case 'session.input_transcript.delta':
        this.state.set('user_speaking');
        this.transcript.update((t) => t + ((event['delta'] as string) ?? ''));
        break;

      case 'response.audio.delta':
      case 'session.output_audio.delta':
        this.state.set('agent_speaking');
        break;

      case 'response.audio_transcript.delta':
      case 'session.output_transcript.delta':
        this.state.set('agent_speaking');
        this.agentText.update((t) => t + ((event['delta'] as string) ?? ''));
        break;

      case 'response.audio_transcript.done':
      case 'session.output_transcript.done':
        this.agentText.set('');
        break;

      case 'session.delegation.created': {
        const delegation = event['delegation'] as { id?: string } | undefined;
        if (delegation?.id) this.handleDelegation(delegation.id);
        break;
      }

      case 'response.done':
        if (this.state() === 'agent_speaking' || this.state() === 'processing') {
          this.state.set('listening');
        }
        break;

      case 'error':
        this.errorMessage.set((event['error'] as { message?: string })?.message ?? 'Erro Realtime');
        if (this.closeTimeout) {
          clearTimeout(this.closeTimeout);
          this.closeTimeout = null;
        }
        this.state.set('error');
        this.cleanup();
        this.voiceConversationId.set(null);
        break;

      default: break;
    }
  }

  private async handleDelegation(delegationId: string): Promise<void> {
    this.state.set('tool_executing');
    const query = this.transcript().trim();
    this.transcript.set('');

    if (!query) {
      this.sendEvent({
        type: 'session.commentary.append',
        delegation_id: delegationId,
        content: 'Não consegui identificar a pergunta. Pode repeti-la, por favor?',
      });
      this.state.set('listening');
      return;
    }

    try {
      const res = await fetch(API.voiceAgent, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: query, conversationId: this.voiceConversationId() }),
      });

      if (!res.ok) throw new Error(`Agent error: ${res.status}`);
      const data = await res.json() as { content: string };

      this.sendEvent({
        type: 'session.commentary.append',
        delegation_id: delegationId,
        content: data.content,
      });

      this.messageAdded.update((n) => n + 1);
      this.state.set('listening');
    } catch {
      this.sendEvent({
        type: 'session.commentary.append',
        delegation_id: delegationId,
        content: 'Não foi possível obter a resposta do agente.',
      });
      this.state.set('listening');
    }
  }

  private sendEvent(event: RealtimeEvent): void {
    if (this.dc?.readyState === 'open') {
      this.dc.send(JSON.stringify(event));
    }
  }

  private cleanup(): void {
    this.dc?.close();
    this.pc?.close();
    if (this.audioEl) {
      this.audioEl.srcObject = null;
      this.audioEl = null;
    }
    this.pc = null;
    this.dc = null;
  }
}
