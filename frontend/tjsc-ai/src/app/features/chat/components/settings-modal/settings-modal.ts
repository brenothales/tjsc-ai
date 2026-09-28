import { Component, inject, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { SettingsService, ChatSettings } from '../../shared/services/settings.service';

@Component({
  selector: 'app-settings-modal',
  imports: [FormsModule, TranslatePipe],
  templateUrl: './settings-modal.html',
})
export class SettingsModalComponent {
  protected readonly svc = inject(SettingsService);
  private readonly translate = inject(TranslateService);

  readonly close = output<void>();
  readonly draft = signal<ChatSettings>({ ...this.svc.settings() });

  get models() {
    return SettingsService.MODELS.map(m => ({
      id: m.id,
      label: this.translate.instant(`modelos.${m.labelKey}`),
    }));
  }

  setModel(id: string): void {
    this.draft.update(s => ({ ...s, model: id }));
  }

  setTemperature(value: string): void {
    this.draft.update(s => ({ ...s, temperature: parseFloat(value) }));
  }

  setSystemExtra(value: string): void {
    this.draft.update(s => ({ ...s, systemExtra: value }));
  }

  save(): void {
    this.svc.save(this.draft());
    this.close.emit();
  }

  cancel(): void {
    this.close.emit();
  }
}
