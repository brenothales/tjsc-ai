import { computed, inject, Injectable, NgZone, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MessageRole } from '../enums/message-role.enum';
import { Message } from '../interfaces/message.interface';
import { ChatApiService } from './chat.api.service';
import { ConversationApiService } from './conversation.api.service';
import { ConversationSummary } from '../interfaces/conversation.interface';
import { SettingsService } from './settings.service';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly chatApi = inject(ChatApiService);
  private readonly conversationApi = inject(ConversationApiService);
  private readonly router = inject(Router);
  private readonly ngZone = inject(NgZone);
  private readonly settingsSvc = inject(SettingsService);

  readonly conversationId = signal<string | null>(null);
  readonly messages = signal<Message[]>([]);
  readonly conversations = signal<ConversationSummary[]>([]);
  readonly isStreaming = signal(false);
  readonly isLoadingHistory = signal(false);

  readonly hasMessages = computed(() => this.messages().length > 0);

  private abortController: AbortController | null = null;

  private pendingId = 0;
  readonly pendingInput = signal<{ value: string; id: number } | null>(null);
  setInput(value: string): void {
    this.pendingInput.set({ value, id: ++this.pendingId });
  }

  readonly conversationTitle = computed(() => {
    const id = this.conversationId();
    if (!id) return null;
    return this.conversations().find((c) => c.id === id)?.title ?? null;
  });

  loadConversations(): void {
    this.conversationApi.list(0, 200).subscribe((page) => {
      this.conversations.set(page.content);
    });
  }

  loadConversation(id: string): void {
    this.isLoadingHistory.set(true);
    this.conversationId.set(id);
    this.conversationApi.get(id).subscribe({
      next: (detail) => {
        const messages: Message[] = detail.messages
          .filter((m) => (m.role === 'USER' || m.role === 'ASSISTANT') && m.content)
          .map((m, i) => ({
            id: `${id}-${i}`,
            role: m.role === 'USER' ? MessageRole.User : MessageRole.Assistant,
            content: m.content,
            timestamp: new Date(m.timestamp),
          }));
        this.messages.set(messages);
        this.isLoadingHistory.set(false);
      },
      error: () => {
        this.isLoadingHistory.set(false);
        this.router.navigate(['/']);
      },
    });
  }

  stopStreaming(): void {
    this.abortController?.abort();
  }

  startNewConversation(): void {
    this.conversationId.set(crypto.randomUUID());
    this.messages.set([]);
  }

  async sendMessage(
    content: string,
    opts?: { insightKey?: string; bypassCache?: boolean }
  ): Promise<void> {
    if (!content.trim() || this.isStreaming()) return;

    const assistantId = crypto.randomUUID();

    const userMessage: Message = {
      id: crypto.randomUUID(),
      role: MessageRole.User,
      content,
      timestamp: new Date(),
    };

    const assistantMessage: Message = {
      id: assistantId,
      role: MessageRole.Assistant,
      content: '',
      timestamp: new Date(),
      streaming: true,
      insightKey: opts?.insightKey,
    };

    this.messages.update((msgs) => [...msgs, userMessage, assistantMessage]);
    this.isStreaming.set(true);
    this.abortController = new AbortController();

    const s = this.settingsSvc.settings();
    const stream = this.chatApi.streamMessage(
      {
        conversationId: this.conversationId()!,
        message: content,
        insightKey: opts?.insightKey,
        bypassCache: opts?.bypassCache,
        model: s.model || undefined,
        temperature: s.temperature,
        systemExtra: s.systemExtra || undefined,
      },
      this.abortController.signal,
      (meta) => {
        this.messages.update((msgs) =>
          msgs.map((m) =>
            m.id === assistantId ? { ...m, fromCache: meta.fromCache } : m
          )
        );
      }
    );

    const reader = stream.getReader();
    try {
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        this.ngZone.run(() => {
          this.messages.update((msgs) => {
            const last = msgs[msgs.length - 1];
            return [...msgs.slice(0, -1), { ...last, content: last.content + value }];
          });
        });
      }
    } finally {
      this.ngZone.run(() => {
        this.messages.update((msgs) => {
          const last = msgs[msgs.length - 1];
          return [...msgs.slice(0, -1), { ...last, streaming: false }];
        });
        this.isStreaming.set(false);
      });
      this.abortController = null;
      this.loadConversations();
      this.router.navigate(['/chat', this.conversationId()!], { replaceUrl: true });
    }
  }

  async refreshInsight(messageId: string): Promise<void> {
    const msg = this.messages().find((m) => m.id === messageId);
    if (!msg?.insightKey || this.isStreaming()) return;

    const userMsg = this.messages()[this.messages().indexOf(msg) - 1];
    if (!userMsg) return;

    this.messages.update((msgs) => msgs.filter((m) => m.id !== messageId && m.id !== userMsg.id));
    await this.sendMessage(userMsg.content, { insightKey: msg.insightKey, bypassCache: true });
  }

  deleteConversation(id: string): void {
    this.conversationApi.delete(id).subscribe(() => {
      this.conversations.update((list) => list.filter((c) => c.id !== id));
      if (this.conversationId() === id) {
        this.startNewConversation();
        this.router.navigate(['/chat']);
      }
    });
  }

  renameConversation(id: string, title: string): void {
    this.conversationApi.updateTitle(id, title).subscribe(() => {
      this.conversations.update((list) =>
        list.map((c) => (c.id === id ? { ...c, title } : c))
      );
    });
  }

  setStarred(id: string, starred: boolean): void {
    this.conversations.update((list) => list.map((c) => c.id === id ? { ...c, starred } : c));
    this.conversationApi.setStarred(id, starred).subscribe();
  }

  setArchived(id: string, archived: boolean): void {
    this.conversations.update((list) => list.map((c) => c.id === id ? { ...c, archived } : c));
    this.conversationApi.setArchived(id, archived).subscribe();
  }
}
