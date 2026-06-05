import { Component, signal, EventEmitter, Input, Output } from '@angular/core';
import { ClientApi } from '../../service/client-api';

@Component({
  selector: 'app-service-panel',
  imports: [],
  templateUrl: './service-panel.html',
  styleUrl: './service-panel.css',
})
export class ServicePanel {
  @Input() sessionActive: boolean = false;
  lastResult = signal<string | null>(null);
  errorMessage = signal<string | null>(null);
  @Output() sessionStarted = new EventEmitter<void>();
  @Output() sessionEnded = new EventEmitter<void>();

  constructor(private api: ClientApi) {}

  onStart(): void {
    this.errorMessage.set(null);
    this.api.startService().subscribe({
      next: (response) => {
        if (response.status === 'client_auth_ok') {
          this.sessionStarted.emit();
        } else {
          this.errorMessage.set(response.message || 'Nie udało się rozpocząć sesji');
        }
      },
      error: (err) => {
        this.errorMessage.set('Nie udało się rozpocząć sesji');
        console.error(err);
      }
    });
  }
  onReverse(text: string): void {
    if (!text) return;
    this.errorMessage.set(null);
    this.api.reverse(text).subscribe({
      next: (response) => {
        this.lastResult.set(response.reversed);
      },
      error: (err) => {
        this.errorMessage.set('Błąd przy odwracaniu');
        console.error(err);
      }
    });
  }

  onEnd(): void {
    this.errorMessage.set(null);
    this.api.endSession().subscribe({
      next: (response) => {
        if (response.status === 'session_ended') {
          this.sessionEnded.emit();
          this.lastResult.set(null);
        } else {
          this.errorMessage.set('Nie udało się zakończyć sesji');
        }
      },
      error: (err) => {
        this.errorMessage.set('Błąd przy kończeniu sesji');
        console.error(err);
      }
    });
  }
}
