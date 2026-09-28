import { Component, effect, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ChatService } from '../../shared/services/chat.service';
import { VoiceService } from '../../shared/services/voice.service';
import { ProcessoPanelService } from '../../shared/services/processo-panel.service';
import { MinutaService } from '../../shared/services/minuta.service';
import { ThemeService } from '../../../../shared/services/theme.service';
import { ConversationSidebarComponent } from '../conversation-sidebar/conversation-sidebar';
import { MessageListComponent } from '../message-list/message-list';
import { ChatInputComponent } from '../chat-input/chat-input';
import { ProcessoPanelComponent } from '../processo-panel/processo-panel';
import { MinutaModalComponent } from '../minuta-modal/minuta-modal';

@Component({
  selector: 'app-chat-page',
  imports: [ConversationSidebarComponent, MessageListComponent, ChatInputComponent, ProcessoPanelComponent, MinutaModalComponent, TranslatePipe],
  templateUrl: './chat-page.html',
  styleUrl: './chat-page.css',
})
export class ChatPageComponent implements OnInit {
  protected readonly chat   = inject(ChatService);
  protected readonly voice  = inject(VoiceService);
  protected readonly panel  = inject(ProcessoPanelService);
  protected readonly minuta = inject(MinutaService);
  protected readonly theme  = inject(ThemeService);
  private readonly route = inject(ActivatedRoute);

  readonly sidebarOpen = signal(true);

  constructor() {
    effect(() => {
      const n = this.voice.messageAdded();
      if (n === 0) return;
      const voiceId = this.voice.voiceConversationId();
      if (voiceId && voiceId === this.chat.conversationId()) {
        this.chat.loadConversation(voiceId);
        this.chat.loadConversations();
      }
    });
  }

  ngOnInit(): void {
    this.chat.loadConversations();

    this.route.paramMap.subscribe((params) => {
      const id = params.get('id');
      if (id) {
        if (id !== this.chat.conversationId()) {
          if (id === this.voice.voiceConversationId()) {
            this.chat.setVoiceConversation(id);
          } else {
            this.chat.loadConversation(id);
          }
        }
      } else {
        this.chat.startNewConversation();
      }
    });
  }

  toggleSidebar(): void {
    this.sidebarOpen.update((v) => !v);
  }

  onSend(message: string, opts?: { insightKey?: string; bypassCache?: boolean }): void {
    this.chat.sendMessage(message, opts);
  }
}
