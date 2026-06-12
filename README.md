# CLIENT-SERVER-TTP

[![en](https://img.shields.io/badge/lang-en-blue?style=for-the-badge)](README.md)
[![pl](https://img.shields.io/badge/lang-pl-lightgrey?style=for-the-badge)](README.pl.md)

Four applications. TTP issues X.509 certificates and mediates authentication, Server provides a service (text reversing), Client is a backend for Angular, and Angular is the user interface. Before the client and server start exchanging anything, each registers with the TTP and receives a certificate. Each session begins with the authentication of both parties by the TTP, which only then issues a shared AES-256 session key.

## What runs where

| Application      | Port | Where            |
| ---------------- | ---- | ---------------- |
| TTP              | 5000 | Docker           |
| Server           | 5001 | Docker           |
| Client (backend) | 5002 | physical machine |
| Angular (UI)     | 4200 | physical machine |

The division results from the project assumptions: TTP and Server emulate two machines (containers), and the user application (Client backend + Angular) runs on a physical machine.

## Stack

- Java 21
- Spring Boot 3.3.4
- Lombok
- Bouncy Castle (for X.509 certificates)
- Maven (multi-module)
- Angular 21
- Docker

## How to run

### Variant with Docker (TTP + Server in containers)

From the root directory of the project:

```
docker compose up --build
```
TTP (5000) and Server (5001) start up. Then on the physical machine, we run the Client backend and UI:

```
client/    -> ClientApplication          (port 5002)
client-ui/ -> npm install && npm start    (port 4200)
```

### Local variant

In this order (Server and Client wait for TTP):

```
ttp/    -> TtpApplication
server/ -> ServerApplication
client/ -> ClientApplication
```

And then the UI:
```
client-ui/ -> npm install && npm start
```

UI opens at http://localhost:4200.

## Interface (Angular)

Simple single-page. 

Components:

- `StatusComponent` ; shows the backend state (from `/api/ping`) and whether the session is active. Receives `sessionActive` via `@Input`.
- `ServicePanelComponent` ; "Start session" / "End session" buttons and a field for reversing text (visible only when the session is active). Emits `sessionStarted` / `sessionEnded` via `@Output`.

## Endpoints (Client backend, port 5002)

All under `/api`.

### `GET /api/ping`

Health check. Check if the backend is running.

Response:

```
{
  "service": "Client",
  "timestamp": "2026-05-09T15:23:01.123",
  "status": "ok"
}
```
### `POST /api/start-service`

Establishing a session. Internally: client sends a service request to the Server, Server asks TTP for authentication, TTP validates, Client authenticates with TTP, receives an AES-256 session key.

No body.

Success:

```
{
  "status": "client_auth_ok",
  "message": "Client uwierzytelniony",
  "sessionKey": "..."
}
```

`sessionKey` in the response is an encrypted key, Angular does nothing with it (decryption happens on the Client backend side).

### `POST /api/reverse`

Sending text to be reversed. Requires an active session (call start-service first).

Body:

```
{
  "text": "pies"
}
```

Response:

```
{
  "reversed": "seip"
}
```

### `POST /api/end-session`

Ending the session. Clears the key on both sides (Client and Server).

No body.

Response:

```
{
  "status": "session_ended"
}
```

## How it works

Full flow for `/start-service`:
```
1. Angular        -> Client backend  : POST /api/start-service
2. Client backend -> Server          : POST /api/request-service (ID + client cert)
3. Server         -> TTP             : POST /api/auth-server     (both IDs + both certificates)
4. TTP verifies the Server's certificate (checks TTP signature)
5. TTP            -> Server          : "server_auth_ok"
6. Server         -> Client backend  : "server_auth_ok"
7. Client backend -> TTP             : POST /api/auth-client (encrypted ID + cert)
8. TTP verifies the Client and the certificate
9. TTP generates a random AES-256 session key
10. TTP          -> Server           : POST /api/notify-session-key (key for Server)
11. TTP          -> Client backend   : response to (7) with key for Client
12. Server and Client have the same AES-256 key, they can talk encrypted
```

All session keys are encrypted with RSA using the recipient's public key (so only the recipient can decrypt them).

For `/reverse`:

```
1. Angular        -> Client backend  : POST /api/reverse {"text": "pies"}
2. Client backend encrypts the text with AES-256-GCM (with a random nonce)
3. Client backend -> Server          : POST /api/reverse (encrypted text)
4. Server decrypts, reverses, encrypts the result
5. Server         -> Client backend  : encrypted response
6. Client backend decrypts
7. Client backend -> Angular         : {"reversed": "seip"}
```

## Cryptography

- RSA 4096, padding OAEP ; registration, certificates
- AES-256-GCM ; session data
- SHA-256 ; public IDs of the parties
- X.509 via Bouncy Castle ; SHA256withRSA signature
- session keys from a pseudorandom generator

## Project structure

```
.
├── pom.xml                 (parent POM)
├── docker-compose.yml      (TTP + Server)
├── shared/                 (CryptoUtils ; key generation, encryption, certificates)
├── ttp/                    (TTP, port 5000)
├── server/                 (Server, port 5001)
├── client/                 (Client backend, port 5002)
└── client-ui/              (Angular UI, port 4200)
```