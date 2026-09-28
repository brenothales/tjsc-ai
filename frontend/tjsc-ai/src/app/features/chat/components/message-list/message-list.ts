import { AfterViewChecked, Component, ElementRef, inject, ViewChild } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ChatService } from '../../shared/services/chat.service';
import { MessageItemComponent } from '../message-item/message-item';
import { SpinnerComponent } from '../../../../shared/components/spinner/spinner';

@Component({
  selector: 'app-message-list',
  imports: [MessageItemComponent, SpinnerComponent, TranslatePipe],
  templateUrl: './message-list.html',
  styleUrl: './message-list.css',
})
export class MessageListComponent implements AfterViewChecked {
  protected readonly chat = inject(ChatService);

  @ViewChild('bottom') private bottom!: ElementRef<HTMLDivElement>;

  private lastMessageCount = 0;
  private lastStreaming = false;

  ngAfterViewChecked(): void {
    const count = this.chat.messages().length;
    const streaming = this.chat.isStreaming();

    if (count !== this.chat.messages().length || streaming !== this.lastStreaming) {
      this.lastMessageCount = count;
      this.lastStreaming = streaming;
      this.bottom?.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'end' });
    }
  }
}
