# CLIENT-SERVER-TTP
Trzy aplikacje Spring Boot: TTP, Server, Client. TTP wystawia certyfikaty i pośredniczy w uwierzytelnianiu, Server udostępnia usługę (odwracanie tekstu), Client to backend dla Angulara.

## Co gdzie chodzi

| Aplikacja | Port | Gdzie |
|---|---|---|
| TTP | 5000 | Docker (docelowo) |
| Server | 5001 | Docker (docelowo) |
| Client (backend) | 5002 | maszyna fizyczna |
| Angular | 4200 | maszyna fizyczna |

## Stack

- Java 21
- Spring Boot 3.3.4
- Lombok
- Bouncy Castle (do certyfikatów X.509)
- Maven (multi-module)

## Jak odpalić

W tej kolejności (Server i Client czekają na TTP):

```
ttp/    -> TtpApplication
server/ -> ServerApplication
client/ -> ClientApplication
```


## Endpointy (Client backend, port 5002)

Wszystkie pod `/api`.

### `GET /api/ping`

Health check. Sprawdzić czy backend chodzi.

Odpowiedź:
```json
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
```json
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
```json
{
  "text": "pies"
}
```

Odpowiedź:
```json
{
  "reversed": "seip"
}
```

Bez aktywnej sesji wyjątek "Brak klucza sesyjnego ; najpierw start-service".

### `POST /api/end-session`

Zakończenie sesji. Czyści klucz po obu stronach (Client i Server).

Bez ciała.

Odpowiedź:
```json
{
  "status": "session_ended"
}
```

Po tym `/reverse` znowu rzuci "Brak klucza sesyjnego". Trzeba `/start-service` od nowa.

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
1. Angular        -> Client backend  : POST /api/reverse {"text": "kajak"}
2. Client backend szyfruje tekst AES-256-GCM (z losowym nonce)
3. Client backend -> Server          : POST /api/reverse (zaszyfrowany tekst)
4. Server deszyfruje, odwraca, szyfruje wynik
5. Server         -> Client backend  : zaszyfrowana odpowiedź
6. Client backend deszyfruje
7. Client backend -> Angular         : {"reversed": "kajak"}
```

## Struktura projektu

```
.
├── pom.xml                 (parent POM)
├── shared/                 (CryptoUtils ; generowanie kluczy, szyfrowanie, certyfikaty)
├── ttp/                    (TTP, port 5000)
├── server/                 (Server, port 5001)
└── client/                 (Client backend, port 5002)
```
