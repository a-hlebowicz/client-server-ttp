# CLIENT-SERVER-TTP

[![en](https://img.shields.io/badge/lang-en-lightgrey?style=for-the-badge)](README.md)
[![pl](https://img.shields.io/badge/lang-pl-blue?style=for-the-badge)](README.pl.md)

Cztery aplikacje. TTP wystawia certyfikaty X.509 i pośredniczy w uwierzytelnianiu, Server udostępnia usługę (odwracanie tekstu), Client to backend dla Angulara, a Angular to interfejs użytkownika. Zanim klient i serwer zaczną cokolwiek wymieniać, każdy rejestruje się w TTP i dostaje certyfikat. Każda sesja zaczyna się od uwierzytelnienia obu stron przez TTP, który dopiero wtedy wydaje wspólny klucz sesyjny AES-256.

## Co gdzie chodzi

| Aplikacja        | Port | Gdzie            |
| ---------------- | ---- | ---------------- |
| TTP              | 5000 | Docker           |
| Server           | 5001 | Docker           |
| Client (backend) | 5002 | maszyna fizyczna |
| Angular (UI)     | 4200 | maszyna fizyczna |

Podział wynika z założeń projektu: TTP i Server emulują dwie maszyny (kontenery), a aplikacja użytkownika (Client backend + Angular) chodzi na maszynie fizycznej.

## Stack

- Java 21
- Spring Boot 3.3.4
- Lombok
- Bouncy Castle (do certyfikatów X.509)
- Maven (multi-module)
- Angular 21
- Docker

## Jak odpalić

### Wariant z Dockerem (TTP + Server w kontenerach)

Z katalogu głównego projektu:

```
docker compose up --build
```

Wstaje TTP (5000) i Server (5001). Potem na maszynie fizycznej odpalamy backend Clienta i UI:

```
client/    -> ClientApplication          (port 5002)
client-ui/ -> npm install && npm start    (port 4200)
```

### Wariant lokalny

W tej kolejności (Server i Client czekają na TTP):

```
ttp/    -> TtpApplication
server/ -> ServerApplication
client/ -> ClientApplication
```

A na koniec UI:

```
client-ui/ -> npm install && npm start
```

UI otwiera się na http://localhost:4200.

## Interfejs (Angular)

Prosty single-page. 

Komponenty:

- `StatusComponent` ; pokazuje stan backendu (z `/api/ping`) oraz czy sesja jest aktywna. Dostaje `sessionActive` przez `@Input`.
- `ServicePanelComponent` ; przyciski "Rozpocznij sesję" / "Zakończ sesję" oraz pole do odwracania tekstu (widoczne tylko przy aktywnej sesji). Emituje `sessionStarted` / `sessionEnded` przez `@Output`.

## Endpointy (Client backend, port 5002)

Wszystkie pod `/api`.

### `GET /api/ping`

Health check. Sprawdzić czy backend chodzi.

Odpowiedź:

```
{
  "service": "Client",
  "timestamp": "2026-05-09T15:23:01.123",
  "status": "ok"
}
```

### `POST /api/start-service`

Nawiązanie sesji. Wewnętrznie: client wysyła service request do Servera, Server prosi TTP o uwierzytelnienie, TTP waliduje, Client uwierzytelnia się w TTP, dostaje klucz sesyjny AES-256.

Bez ciała.

Sukces:

```
{
  "status": "client_auth_ok",
  "message": "Client uwierzytelniony",
  "sessionKey": "..."
}
```

`sessionKey` w odpowiedzi to zaszyfrowany klucz, Angular nic z nim nie robi (deszyfrowanie idzie po stronie backendu Clienta).

### `POST /api/reverse`

Wysłanie tekstu do odwrócenia. Wymaga aktywnej sesji (najpierw `start-service`).

Ciało:

```
{
  "text": "pies"
}
```

Odpowiedź:

```
{
  "reversed": "seip"
}
```



### `POST /api/end-session`

Zakończenie sesji. Czyści klucz po obu stronach (Client i Server).

Bez ciała.

Odpowiedź:

```
{
  "status": "session_ended"
}
```

## Jak działa

Pełen przepływ przy `/start-service`:

```
1. Angular        -> Client backend  : POST /api/start-service
2. Client backend -> Server          : POST /api/request-service (ID + cert clienta)
3. Server         -> TTP             : POST /api/auth-server     (oba ID + oba certyfikaty)
4. TTP weryfikuje certyfikat Servera (sprawdza podpis TTP)
5. TTP            -> Server          : "server_auth_ok"
6. Server         -> Client backend  : "server_auth_ok"
7. Client backend -> TTP             : POST /api/auth-client (zaszyfrowane ID + cert)
8. TTP weryfikuje Clienta i certyfikat
9. TTP generuje losowy klucz sesyjny AES-256
10. TTP          -> Server           : POST /api/notify-session-key (klucz dla Servera)
11. TTP          -> Client backend   : odpowiedź na (7) z kluczem dla Clienta
12. Server i Client mają ten sam klucz AES-256, mogą gadać szyfrowane
```

Wszystkie klucze sesyjne są szyfrowane RSA kluczem publicznym odbiorcy (więc tylko on je odszyfruje).

Przy `/reverse`:

```
1. Angular        -> Client backend  : POST /api/reverse {"text": "pies"}
2. Client backend szyfruje tekst AES-256-GCM (z losowym nonce)
3. Client backend -> Server          : POST /api/reverse (zaszyfrowany tekst)
4. Server deszyfruje, odwraca, szyfruje wynik
5. Server         -> Client backend  : zaszyfrowana odpowiedź
6. Client backend deszyfruje
7. Client backend -> Angular         : {"reversed": "seip"}
```

## Kryptografia

- RSA 4096, padding OAEP ; rejestracja, certyfikaty
- AES-256-GCM ; dane sesyjne
- SHA-256 ; publiczne ID stron
- X.509 przez Bouncy Castle ; podpis SHA256withRSA
- klucze sesyjne z generatora pseudolosowego

## Struktura projektu

```
.
├── pom.xml                 (parent POM)
├── docker-compose.yml      (TTP + Server)
├── shared/                 (CryptoUtils ; generowanie kluczy, szyfrowanie, certyfikaty)
├── ttp/                    (TTP, port 5000)
├── server/                 (Server, port 5001)
├── client/                 (Client backend, port 5002)
└── client-ui/              (Angular UI, port 4200)
```