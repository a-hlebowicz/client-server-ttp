import { Component, OnInit, signal, Input } from '@angular/core';
import { ClientApi, PingResponse } from '../../service/client-api';

@Component({
  selector: 'app-status',
  imports: [],
  templateUrl: './status.html',
  styleUrl: './status.css',
})
export class Status implements OnInit {
  ping = signal<PingResponse | null>(null);
  @Input() sessionActive: boolean = false;

  constructor(private api: ClientApi) {}

  ngOnInit(): void {
    this.api.ping().subscribe({
      next: (response) => {
        this.ping.set(response);
      },
      error: (err) => {
        console.error('Ping nie wyszedł', err);
      }
    });
  }
}
