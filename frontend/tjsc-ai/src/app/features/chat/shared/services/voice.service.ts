import { inject, Injectable, signal } from '@angular/core';
import { API } from '../constants/api.constants';

export type VoiceState = 'idle' | 'connecting' | 'listening' | 'speaking' | 'error';

@Injectable({ providedIn: 'root' })
export class VoiceService {
  readonly state = signal<VoiceState>('idle');
  readonly transcript = signal<string>('');

  private pc: RTCPeerConnection | null = null;
  private dc: RTCDataChannel | null = null;
  private audioEl: HTMLAudioElement | null = null;

  get isActive(): boolean {
    return this.state() !== 'idle' && this.state() !== 'error';
  }

  async start(): Promise<void> {
    if (this.isActive) return;
    this.state.set('connecting');
    this.transcript.set('');

    try {
      const { clientSecret } = await this.fetchSession();

      this.pc = new RTCPeerConnection();
      this.audioEl = document.createElement('audio');
      this.audioEl.autoplay = true;

      this.pc.ontrack = (e) => {
        this.audioEl!.srcObject = e.streams[0];
        this.state.set('listening');
      };

      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      stream.getTracks().forEach((t) => this.pc!.addTrack(t, stream));

      this.dc = this.pc.createDataChannel('oai-events');
      this.dc.onmessage = (e) => this.handleEvent(JSON.parse(e.data));

      const offer = await this.pc.createOffer();
      await this.pc.setLocalDescription(offer);

      const sdpResponse = await fetch(
        `https://api.openai.com/v1/realtime?model=gpt-4o-realtime-preview`,
        {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${clientSecret}`,
            'Content-Type': 'application/sdp',
          },
          body: offer.sdp,
        }
      );

      const answer: RTCSessionDescriptionInit = {
        type: 'answer',
        sdp: await sdpResponse.text(),
      };
      await this.pc.setRemoteDescription(answer);
    } catch (err) {
      console.error('[VoiceService] start error:', err);
      this.state.set('error');
      this.stop();
    }
  }

  stop(): void {
    this.dc?.close();
    this.pc?.close();
    if (this.audioEl) {
      this.audioEl.srcObject = null;
      this.audioEl = null;
    }
    this.pc = null;
    this.dc = null;
    this.state.set('idle');
  }

  private handleEvent(event: Record<string, unknown>): void {
    switch (event['type']) {
      case 'response.audio_transcript.delta':
        this.transcript.update((t) => t + (event['delta'] as string ?? ''));
        break;
      case 'response.audio_transcript.done':
        this.transcript.set('');
        break;
      case 'response.audio.delta':
        this.state.set('speaking');
        break;
      case 'input_audio_buffer.speech_started':
        this.state.set('listening');
        break;
      case 'error':
        console.error('[VoiceService] realtime error:', event);
        this.state.set('error');
        this.stop();
        break;
    }
  }

  private async fetchSession(): Promise<{ clientSecret: string }> {
    const res = await fetch(API.voiceSession, { method: 'POST' });
    if (!res.ok) throw new Error(`Session error: ${res.status}`);
    const data = await res.json();
    return { clientSecret: data.clientSecret };
  }
}
