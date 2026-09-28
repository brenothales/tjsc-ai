import { Injectable, signal } from '@angular/core';

export interface ChatSettings {
  model: string;
  temperature: number;
  systemExtra: string;
}

const MODELS = [
  { id: 'gpt-4o-mini', labelKey: 'gpt4o_mini' },
  { id: 'gpt-4o',      labelKey: 'gpt4o' },
  { id: 'gpt-4-turbo', labelKey: 'gpt4_turbo' },
] as const;

const STORAGE_KEY = 'tjsc.ai.settings';

function load(): ChatSettings {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) return { ...defaults(), ...JSON.parse(raw) };
  } catch {}
  return defaults();
}

function defaults(): ChatSettings {
  return { model: 'gpt-4o-mini', temperature: 0, systemExtra: '' };
}

@Injectable({ providedIn: 'root' })
export class SettingsService {
  static readonly MODELS = MODELS;

  readonly settings = signal<ChatSettings>(load());

  save(s: ChatSettings): void {
    this.settings.set(s);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(s));
  }
}
