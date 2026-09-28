import { Component, computed, inject, input, output, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ChatService } from '../../shared/services/chat.service';
import { ConversationSummary } from '../../shared/interfaces/conversation.interface';
import { SettingsModalComponent } from '../settings-modal/settings-modal';

export interface ConversationGroup {
  key: string;
  labelKey: string;
  items: ConversationSummary[];
}

const PERIOD_KEYS = {
  hoje:      { key: 'hoje',       labelKey: 'sidebar.grupos.hoje' },
  ontem:     { key: 'ontem',      labelKey: 'sidebar.grupos.ontem' },
  semana:    { key: 'semana',     labelKey: 'sidebar.grupos.ultimos_7' },
  mes:       { key: 'mes',        labelKey: 'sidebar.grupos.ultimos_30' },
  antigos:   { key: 'antigos',    labelKey: 'sidebar.grupos.mais_antigos' },
  favoritos: { key: 'favoritos',  labelKey: 'sidebar.grupos.favoritos' },
  arquivadas:{ key: 'arquivadas', labelKey: 'sidebar.grupos.arquivadas' },
} as const;

function groupByPeriod(convs: ConversationSummary[]): ConversationGroup[] {
  const now = new Date();
  const startOf = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate());
  const today     = startOf(now);
  const yesterday = new Date(today); yesterday.setDate(today.getDate() - 1);
  const week      = new Date(today); week.setDate(today.getDate() - 7);
  const month     = new Date(today); month.setDate(today.getDate() - 30);

  const starred: ConversationSummary[] = [];
  const archived: ConversationSummary[] = [];
  const periodGroups: Record<string, ConversationSummary[]> = {
    [PERIOD_KEYS.hoje.key]:    [],
    [PERIOD_KEYS.ontem.key]:   [],
    [PERIOD_KEYS.semana.key]:  [],
    [PERIOD_KEYS.mes.key]:     [],
    [PERIOD_KEYS.antigos.key]: [],
  };

  for (const c of convs) {
    if (c.archived) { archived.push(c); continue; }
    if (c.starred)  { starred.push(c);  continue; }
    const d = startOf(new Date(c.lastMessageAt));
    if (d >= today)          periodGroups[PERIOD_KEYS.hoje.key].push(c);
    else if (d >= yesterday) periodGroups[PERIOD_KEYS.ontem.key].push(c);
    else if (d >= week)      periodGroups[PERIOD_KEYS.semana.key].push(c);
    else if (d >= month)     periodGroups[PERIOD_KEYS.mes.key].push(c);
    else                     periodGroups[PERIOD_KEYS.antigos.key].push(c);
  }

  const result: ConversationGroup[] = [];

  if (starred.length > 0)
    result.push({ ...PERIOD_KEYS.favoritos, items: starred });

  for (const pk of [PERIOD_KEYS.hoje, PERIOD_KEYS.ontem, PERIOD_KEYS.semana, PERIOD_KEYS.mes, PERIOD_KEYS.antigos]) {
    const items = periodGroups[pk.key];
    if (items.length > 0) result.push({ ...pk, items });
  }

  if (archived.length > 0)
    result.push({ ...PERIOD_KEYS.arquivadas, items: archived });

  return result;
}

@Component({
  selector: 'app-conversation-sidebar',
  imports: [SettingsModalComponent, TranslatePipe],
  templateUrl: './conversation-sidebar.html',
  styleUrl: './conversation-sidebar.css',
})
export class ConversationSidebarComponent {
  protected readonly chat = inject(ChatService);
  private readonly router = inject(Router);

  readonly open = input(true);
  readonly toggleSidebar = output<void>();

  readonly editingId = signal<string | null>(null);
  readonly editTitle = signal('');
  readonly searchQuery = signal('');

  readonly filteredConversations = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    if (!q) return this.chat.conversations();
    return this.chat.conversations().filter(c => c.title.toLowerCase().includes(q));
  });

  readonly groupedConversations = computed(() =>
    groupByPeriod(this.filteredConversations())
  );

  readonly settingsOpen = signal(false);

  readonly collapsedGroups = signal<Set<string>>(new Set([
    PERIOD_KEYS.favoritos.key,
    PERIOD_KEYS.ontem.key,
    PERIOD_KEYS.semana.key,
    PERIOD_KEYS.mes.key,
    PERIOD_KEYS.antigos.key,
    PERIOD_KEYS.arquivadas.key,
  ]));

  toggleGroup(key: string): void {
    this.collapsedGroups.update(s => {
      const next = new Set(s);
      next.has(key) ? next.delete(key) : next.add(key);
      return next;
    });
  }

  isCollapsed(key: string): boolean {
    return this.collapsedGroups().has(key);
  }

  onNew(): void {
    this.router.navigate(['/chat']);
  }

  onSelect(id: string): void {
    this.router.navigate(['/chat', id]);
  }

  onDelete(event: Event, id: string): void {
    event.preventDefault();
    event.stopPropagation();
    this.chat.deleteConversation(id);
  }

  startRename(event: Event, conv: ConversationSummary): void {
    event.preventDefault();
    event.stopPropagation();
    this.editingId.set(conv.id);
    this.editTitle.set(conv.title);
  }

  confirmRename(id: string): void {
    const title = this.editTitle().trim();
    if (title) this.chat.renameConversation(id, title);
    this.editingId.set(null);
  }

  cancelRename(): void {
    this.editingId.set(null);
  }

  toggleStar(event: Event, conv: ConversationSummary): void {
    event.preventDefault();
    event.stopPropagation();
    this.chat.setStarred(conv.id, !conv.starred);
  }

  toggleArchive(event: Event, conv: ConversationSummary): void {
    event.preventDefault();
    event.stopPropagation();
    this.chat.setArchived(conv.id, !conv.archived);
  }
}
