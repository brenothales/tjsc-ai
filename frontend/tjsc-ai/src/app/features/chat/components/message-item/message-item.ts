import { computed, Component, inject, input } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { TranslatePipe } from '@ngx-translate/core';
import { marked } from 'marked';
import { Message } from '../../shared/interfaces/message.interface';
import { MessageRole } from '../../shared/enums/message-role.enum';
import { ProcessoTooltipDirective } from '../../../../shared/directives/processo-tooltip.directive';
import { ChatService } from '../../shared/services/chat.service';

const CNJ_REGEX = /(\d{7}-\d{2}\.\d{4}\.\d\.\d{2}\.\d{4})/g;

@Component({
  selector: 'app-message-item',
  imports: [ProcessoTooltipDirective, TranslatePipe],
  templateUrl: './message-item.html',
  styleUrl: './message-item.css',
})
export class MessageItemComponent {
  readonly message = input.required<Message>();
  protected readonly chat = inject(ChatService);
  protected readonly MessageRole = MessageRole;

  private readonly sanitizer = inject(DomSanitizer);

  protected readonly html = computed<SafeHtml>(() => {
    const msg = this.message();
    if (!msg.content) return '';

    if (msg.streaming) {
      const escaped = msg.content
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
      return this.sanitizer.bypassSecurityTrustHtml(
        `<span style="white-space:pre-wrap">${escaped}</span>`
      );
    }

    const html = (marked.parse(msg.content) as string)
      .replace(CNJ_REGEX, '<span class="processo-ref" data-numero="$1">$1</span>');
    return this.sanitizer.bypassSecurityTrustHtml(html);
  });

  refresh(): void {
    this.chat.refreshInsight(this.message().id);
  }
}
