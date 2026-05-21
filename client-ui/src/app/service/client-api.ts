import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PingResponse {
  service: string;
  timestamp: string;
  status: string;
}
export interface StartServiceResponse {
  status: string;
  message: string;
  sessionKey?: string;
}
export interface ReverseResponse {
  reversed: string;
}
export interface EndSessionResponse {
  status: string;
}
@Injectable({
  providedIn: 'root',
})
export class ClientApi {
  constructor(private http: HttpClient) { }

    ping(): Observable<PingResponse> {
      return this.http.get<PingResponse>('api/ping');
    }
    startService(): Observable<StartServiceResponse> {
      return this.http.post<StartServiceResponse>('api/start-service', {});
    }
    reverse(text: string): Observable<ReverseResponse> {
      return this.http.post<ReverseResponse>('api/reverse', { text });
    }
    endSession(): Observable<EndSessionResponse> {
      return this.http.post<EndSessionResponse>('api/end-session', {});
    }

}
