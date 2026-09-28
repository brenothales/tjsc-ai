import { Injectable } from '@angular/core';
import { API } from '../constants/api.constants';
import { ChatRequest } from '../interfaces/message.interface';

export interface InsightMeta {
  fromCache: boolean;
  insightKey: string;
}

const INSIGHT_PREFIX = '[INSIGHT:';

@Injectable({ providedIn: 'root' })
export class ChatApiService {
  streamMessage(
    request: ChatRequest,
    signal?: AbortSignal,
    onInsightMeta?: (meta: InsightMeta) => void
  ): ReadableStream<string> {
    const { readable, writable } = new TransformStream<string, string>();
    const writer = writable.getWriter();

    fetch(API.chatStream, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
      body: JSON.stringify(request),
      signal,
    }).then(async (response) => {
      if (!response.ok || !response.body) {
        await writer.write(`[ERRO] ${response.statusText}`);
        await writer.close();
        return;
      }

      const reader = response.body.pipeThrough(new TextDecoderStream()).getReader();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += value;
        const events = buffer.split('\n\n');
        buffer = events.pop() ?? '';

        for (const event of events) {
          const data = event
            .split('\n')
            .filter((line) => line.startsWith('data:'))
            .map((line) => line.slice(5))
            .join('\n');

          const trimmed = data.trim();
          if (data === '' || trimmed === '[DONE]') continue;

          if (trimmed.startsWith(INSIGHT_PREFIX)) {
            try {
              const json = trimmed.slice(INSIGHT_PREFIX.length, -1);
              onInsightMeta?.(JSON.parse(json));
            } catch {}
            continue;
          }

          await writer.write(data);
        }
      }

      await writer.close();
    }).catch(async (err) => {
      if (err?.name !== 'AbortError') {
        await writer.write(`[ERRO] ${err.message}`);
      }
      await writer.close();
    });

    return readable;
  }
}
