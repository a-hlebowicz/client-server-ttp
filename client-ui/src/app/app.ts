import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Status } from './component/status/status';
import { ServicePanel } from './component/service-panel/service-panel';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Status, ServicePanel],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  sessionActive = signal(false);

  onSessionStarted(): void {
    this.sessionActive.set(true);
  }

  onSessionEnded(): void {
    this.sessionActive.set(false);
  }
}
