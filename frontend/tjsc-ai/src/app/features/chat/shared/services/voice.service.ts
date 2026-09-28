import { Injectable, signal } from '@angular/core';
import { API } from '../constants/api.constants';

export type VoiceState =
  | 'idle'
  | 'connecting'
  | 'listening'
  | 'user_speaking'
  | 'processing'
  | 'agent_speaking'
  | 'tool_executing'
  | 'error';

interface RealtimeEvent {
  type: string;
  [key: string]: unknown;
}

@Injectable({ providedIn: 'root' })
export class VoiceService {
  readonly state         = signal<VoiceState>('idle');
  readonly transcript    = signal<string>('');
  readonly agentText     = signal<string>('');
  readonly errorMessage  = signal<string>('');

  private pc: RTCPeerConnection | null = null;
  private dc: RTCDataChannel | null = null;
  private audioEl: HTMLAudioElement | null = null;
  private sessionId: string | null = null;
  private voiceConversationId: string | null = null;

  get isActive(): boolean {
    return this.state() !== 'idle' && this.state() !== 'error';
  }

  async start(): Promise<void> {
    if (this.isActive) return;

    this.state.set('connecting');
    this.transcript.set('');
    this.agentText.set('');
    this.errorMessage.set('');
    this.voiceConversationId = crypto.randomUUID();

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
          console.warn('[Voice] conexão WebRTC perdida:', s);
          this.stop();
        }
      };

      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      stream.getTracks().forEach((t) => this.pc!.addTrack(t, stream));

      this.dc = this.pc.createDataChannel('oai-events');
      this.dc.onopen  = () => console.log('[Voice] DataChannel aberto');
      this.dc.onclose = () => console.log('[Voice] DataChannel fechado');
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

      this.sessionId = data.sessionId;
      await this.pc.setRemoteDescription({ type: 'answer', sdp: data.sdp });
      console.log('[Voice] WebRTC estabelecido — sessionId:', this.sessionId);
    } catch (err) {
      console.error('[Voice] erro ao iniciar:', err);
      this.errorMessage.set(err instanceof Error ? err.message : 'Erro ao conectar');
      this.state.set('error');
      this.cleanup();
    }
  }

  stop(): void {
    console.log('[Voice] encerrando sessão');
    this.cleanup();
    this.state.set('idle');
  }

  interrupt(): void {
    if (this.state() === 'agent_speaking') {
      this.sendEvent({ type: 'response.cancel' });
      console.log('[Voice] resposta interrompida (barge-in)');
    }
  }

  private handleEvent(event: RealtimeEvent): void {
    console.log('[Voice] <<', event['type'], event);
    switch (event['type']) {
      case 'session.created':
        this.state.set('listening');
        console.log('[Voice] sessão Realtime criada');
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
        console.log('[Voice] transcript do usuário:', this.transcript());
        break;

      case 'response.audio.delta':
        this.state.set('agent_speaking');
        break;

      case 'response.audio_transcript.delta':
      case 'session.output_transcript.delta':
        this.agentText.update((t) => t + ((event['delta'] as string) ?? ''));
        break;

      case 'response.audio_transcript.done':
      case 'session.output_transcript.done':
        console.log('[Voice] transcript do agente:', this.agentText());
        this.agentText.set('');
        break;

      case 'delegation.request':
        this.handleDelegation(
          event['id'] as string,
          event['query'] as string
        );
        break;

      case 'response.done':
        if (this.state() === 'agent_speaking' || this.state() === 'processing') {
          this.state.set('listening');
        }
        break;

      case 'error':
        console.error('[Voice] erro Realtime:', event);
        this.errorMessage.set((event['error'] as { message?: string })?.message ?? 'Erro Realtime');
        this.state.set('error');
        this.cleanup();
        break;

      default:
        console.log('[Voice] evento desconhecido:', event['type'], event);
        break;
    }
  }

  private async handleDelegation(delegationId: string, query: string): Promise<void> {
    this.state.set('tool_executing');
    const startTime = Date.now();
    console.log('[Voice] delegation.request — query:', query);

    try {
      const res = await fetch(API.voiceAgent, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: query, conversationId: this.voiceConversationId }),
      });

      if (!res.ok) throw new Error(`Agent error: ${res.status}`);
      const data = await res.json() as { content: string };

      console.log('[Voice] delegation respondida em', Date.now() - startTime, 'ms');

      this.sendEvent({ type: 'delegation.response', id: delegationId, response: data.content });
      this.state.set('listening');
    } catch (err) {
      console.error('[Voice] erro na delegation:', err);
      this.sendEvent({
        type: 'delegation.response',
        id: delegationId,
        response: 'Não foi possível obter a resposta do agente.',
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
