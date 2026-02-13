# BIP – Backend

Backend do teste prático de entregas (tipo "marketplace de corridas"), implementado em Java 17 / Spring Boot 3, com arquitetura em camadas DDD-friendly, eventos em Kafka, cache em Redis, notificações em tempo real via WebSocket e testes automatizados com boa cobertura (Jacoco).

---

## 1. Visão geral

Este backend expõe uma API para:

- cadastro e aprovação de usuários (admin / cliente / entregador);
- gestão de carteira (saldo de clientes e entregadores);
- fluxo completo de entregas:
  - cliente solicita entrega;
  - entregador visualiza "vitrine" de entregas disponíveis e aceita;
  - entrega fica "em rota" (cache em Redis);
  - cliente pode cancelar (com ou sem multa);
  - entregador conclui a entrega (transferência de saldo cliente → entregador);
- publicação de eventos de domínio em Kafka;
- consumo desses eventos para trilha de auditoria;
- notificações em tempo real via WebSocket para entregadores.

Tudo foi pensado para o avaliador conseguir subir o ambiente com `docker compose up --build`, já com dados de exemplo e usuários prontos para uso no frontend.

---

## 2. Como executar

### 2.1. Pré-requisitos

- Docker Desktop instalado e em execução.
- Git instalado.
- Opcional: Postman para rodar os fluxos automatizados.

### 2.2. Clonar repositórios

Backend:

```bash
git clone https://github.com/saulocapistrano/bip.git bip-backend
cd bip-backend
```

### 2.3. Subir o backend

Na raiz do backend:

```bash
docker compose up
```

Isso sobe:

- `bip-api` (Spring Boot na porta 8087)
- `bip-postgres`
- `bip-pgadmin`
- `bip-redis`
- `bip-zookeeper`
- `bip-kafka`
- `bip-kafka-ui`
- `bip-keycloak` (porta 8080, com realm importado)

O perfil ativo é `dev` (`SPRING_PROFILES_ACTIVE=dev`), o que ativa:

- migrações Liquibase;
- seeder `DevDataInitializer`:
  - cria usuários padrão (admin, clientes, entregador) no Postgres;
  - cria entregas `AVAILABLE` para os clientes.

### 2.4. Subir o frontend (opcional)

Na raiz do frontend:

```bash
git clone <url-do-repo-frontend> bip-frontend
cd bip-frontend
docker compose up --build
```

Ou conforme instruções específicas do projeto frontend (por exemplo, `npm install && npm start`).

Por padrão, o frontend costuma rodar em:

- `http://localhost:4200`

Ajuste se o docker-compose do frontend estiver usando outra porta.

### 2.5. URLs úteis

- API backend: `http://localhost:8087`
- Keycloak (admin console e realm bip): `http://localhost:8080`
- pgAdmin: `http://localhost:5050`
- Kafka UI: `http://localhost:8081`

---

## 3. Arquitetura

### 3.1. Organização de pacotes

Arquitetura em camadas, DDD-friendly:

- `br.com.bip.api`
  - controllers REST (`/api/...`);
  - handler global de exceções.
- `br.com.bip.application`
  - DTOs de entrada/saída;
  - mappers (entity ↔ DTO ↔ eventos);
  - services orientados a caso de uso mais próximos da API (`DeliveryCreationService`, `ClientWalletService`, etc.).
- `br.com.bip.domain`
  - modelos de domínio (`User`, `DeliveryRequest`, enums, etc.);
  - regras de negócio (services de domínio: `DeliveryAssignmentService`, `DeliveryCancellationService`, `DeliveryCompletionService`, queries);
  - ports (interfaces) de repositório e cache:
    - `UserRepositoryPort`
    - `DeliveryRequestRepositoryPort`
    - `DeliveryInRouteCachePort`
- `br.com.bip.infrastructure`
  - adapters JPA (`UserRepositoryAdapter`, `DeliveryRequestRepositoryAdapter`);
  - adapter Redis (`DeliveryInRouteRedisAdapter`);
  - Kafka producers/consumers;
  - WebSocket notifier (`DeliveryRealtimeNotifier`).
- `br.com.bip.shared`
  - exceções de negócio (`BusinessException`, `NotFoundException`);
  - estrutura de erro da API.

Não é um "DDD puro" isolando totalmente JPA do domínio, mas o desenho segue conceitos de Ports & Adapters/Hexagonal de forma pragmática: controllers chamam services, que dependem de ports, implementados na camada `infrastructure`.

---

## 4. Regras de negócio implementadas

### 4.1. Perfis de usuário

Perfis técnicos no backend:

- `BIP_ADMIN`
- `BIP_CLIENTE`
- `BIP_ENTREGADOR`

No Keycloak, os papéis equivalentes são:

- `bip-admin`
- `bip-cliente`
- `bip-entregador`

Regras:

- apenas usuários com `BIP_ADMIN` podem aprovar cadastros;
- apenas `BIP_CLIENTE` podem:
  - adicionar saldo na carteira de cliente;
  - solicitar/cancelar entregas;
- apenas `BIP_ENTREGADOR` podem:
  - aceitar entregas;
  - concluir entregas.

### 4.2. Carteira e saldo

Campos adicionados em `User`:

- `clientBalance` – saldo do cliente;
- `driverBalance` – saldo do entregador;
- `driverScore` – score do entregador (inicial 1000 para entregadores).

Regras principais:

- Depósito:
  - endpoint de depósito para clientes (ex.: `POST /api/client/wallet/deposit`);
  - apenas `BIP_CLIENTE` com status `APPROVED` podem depositar;
  - valor deve ser maior que zero;
  - gera `FinancialTransactionEvent` do tipo `CLIENT_DEPOSIT` em Kafka.
- Score do entregador:
  - todo entregador começa com `driverScore = 1000`;
  - campo e regra de bloqueio estão modelados para uso futuro (redução de 0.05 e bloqueio abaixo de 499 ainda não implementados na lógica de devolução).

### 4.3. Solicitação de entrega

Quem pode solicitar:

- apenas usuários com role `BIP_CLIENTE` e status `APPROVED`.

Campos no pedido:

- endereço de retirada (`pickupAddress`);
- endereço de destino (`deliveryAddress`);
- descrição (`description`);
- peso aproximado em kg (`weightKg`);
- valor ofertado (`offeredPrice`).

Regra de saldo mínimo para solicitar:

- cliente deve ter pelo menos **2x o valor da entrega** em `clientBalance`;
- se `clientBalance < 2 * offeredPrice`, uma `BusinessException` é lançada.

Comportamento ao criar:

- `DeliveryStatus.AVAILABLE`;
- persistido em Postgres via `DeliveryRequestRepositoryPort`;
- evento `DeliveryRequestedEvent` publicado em Kafka no tópico `bip.delivery.requested`;
- entrega aparece na "vitrine" (`/api/driver/deliveries/available`).

### 4.4. Lista de entregas (vitrine x visão do cliente)

- Cliente:
  - só enxerga as próprias entregas, em qualquer status (`AVAILABLE`, `IN_ROUTE`, `COMPLETED`, `CANCELED`);
  - serviço: `ClientDeliveryQueryService`.
- Entregador:
  - pode listar todas as entregas disponíveis (`AVAILABLE`) para aceitar;
  - pode listar apenas as entregas atribuídas a ele;
  - serviço: `DriverDeliveryQueryService`.

### 4.5. Aceite da entrega pelo entregador

Serviço: `DeliveryAssignmentService`.

Regras:

- entregador deve:
  - existir;
  - ter role `BIP_ENTREGADOR`;
  - estar com status `APPROVED`.
- entrega deve:
  - existir;
  - estar com status `AVAILABLE`;
  - não ter `driverId` ainda associado.

Ao aceitar:

- seta `driverId` com o ID do entregador;
- muda `status` para `IN_ROUTE`;
- grava em Postgres;
- salva cópia no Redis via `DeliveryInRouteCachePort` (entrega em rota);
- no frontend, isso corresponde a mudar visualmente para "em rota" (tag amarela).

### 4.6. Cancelamento pelo cliente

Serviço: `DeliveryCancellationService`.

Regras de quem pode cancelar:

- apenas o próprio cliente dono da entrega;
- only role `BIP_CLIENTE`, status `APPROVED`;
- entrega deve pertencer ao `clientId` informado.

Cenários:

1. Entrega `AVAILABLE`:

  - cliente pode cancelar sem multa;
  - status muda para `CANCELED`;
  - `cancellationReason` recebe a razão informada (ou padrão).

2. Entrega `IN_ROUTE`:

  - cliente pode solicitar cancelamento, com multa de 30%;
  - se não houver `driverId`, é erro;
  - calcula `penalty = 0.30 * offeredPrice`;
  - se `clientBalance < penalty`, lança erro de saldo insuficiente;
  - debita `penalty` do cliente;
  - credita `penalty` no entregador;
  - status muda para `CANCELED`;
  - `cancellationReason` recebe a razão + "(multa de 30% aplicada)";
  - entrega removida do Redis (`DeliveryInRouteCachePort.deleteById`).

Eventos:

- cenário `IN_ROUTE`:
  - dispara `DeliveryCanceledEvent` em `bip.delivery.canceled`;
  - dispara `FinancialTransactionEvent` com tipo `CANCELLATION_PENALTY` (ou equivalente, conforme modelagem).

### 4.7. Conclusão da entrega

Serviço: `DeliveryCompletionService`.

Regras:

- apenas o entregador dono da entrega pode concluir:
  - role `BIP_ENTREGADOR`;
  - entrega deve estar `IN_ROUTE`;
  - `delivery.driverId` deve ser igual ao `driverId` do request.
- valor da entrega (`offeredPrice`) deve estar setado;
- cliente deve possuir saldo suficiente (pelo menos `offeredPrice`).

Ao concluir:

- debita `offeredPrice` de `client.clientBalance`;
- credita `offeredPrice` em `driver.driverBalance`;
- muda status para `COMPLETED`;
- remove entrega do Redis;
- publica:
  - `DeliveryCompletedEvent` em `bip.delivery.completed`;
  - `FinancialTransactionEvent` do tipo `DELIVERY_PAYMENT`.

---

## 5. Stack técnica

- Java 17
- Spring Boot 3.5.10
  - Spring Web (REST)
  - Spring Data JPA (Postgres)
  - Spring Data Redis
  - Spring for Apache Kafka
  - Spring WebSocket / STOMP
  - Spring Boot Actuator
- Banco de dados:
  - PostgreSQL 16
  - Migrações: Liquibase
- Cache:
  - Redis 7
- Mensageria:
  - Kafka (Confluent 7.6.1)
  - Zookeeper
  - Kafka UI (Provectus)
- Autenticação/Autorização:
  - Keycloak 24 (realm `bip` pré-configurado com usuários e roles)
- Ferramentas:
  - Docker e Docker Compose
  - pgAdmin 4
  - Postman
  - JUnit 5, Mockito
  - Jacoco (relatório em `target/site/jacoco/index.html`)

---

## 6. Usuários e dados de exemplo

### 6.1. Usuários seedados

Realm `bip` (Keycloak) e seed `DevDataInitializer` criam os seguintes usuários:

| Usuário    | Papel funcional | Role backend   | Senha           |
| ---------- | --------------- | -------------- | --------------- |
| tortorelli | Administrador   | BIP_ADMIN      | `admin123`      |
| clicia     | Cliente         | BIP_CLIENTE    | `cliente123`    |
| elias      | Cliente         | BIP_CLIENTE    | `cliente123`    |
| saulo      | Entregador      | BIP_ENTREGADOR | `entregador123` |

Comportamento esperado no frontend:

- `tortorelli`:
  - acessa funcionalidades de admin (aprovar usuários, listar todos, etc.);
- `clicia` / `elias`:
  - podem cadastrar entregas, listar e cancelar as suas;
  - possuem saldo inicial (500 e 150, respectivamente);
- `saulo`:
  - pode ver a vitrine de entregas disponíveis;
  - aceitar entregas;
  - concluir entregas;
  - possui score inicial 1000 e carteira de entregador.

### 6.2. Entregas seedadas

No startup (primeira vez, banco vazio), o seeder cria:

- Para `Clicia` (saldo 500):
  - 3 entregas `AVAILABLE` com diferentes endereços e valores.
- Para `Elias` (saldo 150):
  - 3 entregas `AVAILABLE`.

Todas ficam imediatamente visíveis:

- na vitrine do entregador (`/api/driver/deliveries/available`);
- na listagem de entregas do cliente (`/api/client/deliveries?clientId=...`).

---

## 7. Integração com Postgres e pgAdmin

Banco de dados:

- Host (Docker): `bip-postgres`
- Porta exposta: `5432`
- Nome do banco: `bip_db`
- Usuário: `bip-user`
- Senha: `strong_bip_pass1524`
  (uso exclusivo para ambiente dev/teste)

Acessar via pgAdmin em `http://localhost:5050`:

- Email: `admin@bip.com`
- Senha: `admin`

Ao criar o servidor no pgAdmin:

- Host name/address: `bip-postgres`
- Maintenance database: `bip_db`
- Username: `bip-user`
- Password: `strong_bip_pass1524`

---

## 8. Kafka e eventos

### 8.1. Tópicos utilizados

- `bip.delivery.requested`
- `bip.delivery.completed`
- `bip.delivery.canceled`
- `bip.financial.transaction`

### 8.2. Produção de eventos

Principais producers:

- `DeliveryEventProducer`
  - envia:
    - `DeliveryRequestedEvent` em `bip.delivery.requested` quando uma entrega é criada;
    - `DeliveryCompletedEvent` em `bip.delivery.completed` quando é concluída;
    - `DeliveryCanceledEvent` em `bip.delivery.canceled` quando é cancelada.
- `FinancialEventProducer`
  - envia `FinancialTransactionEvent` em `bip.financial.transaction` para:
    - depósito de cliente;
    - pagamento de entrega ao entregador;
    - multa de cancelamento.

### 8.3. Consumers

- `DeliveryRequestedConsumer`
  - `@KafkaListener` em `bip.delivery.requested`;
  - desserializa payload em `DeliveryRequestedEvent`;
  - registra logs estruturados (ponto de extensão para auditoria/integradores).
- `DeliveryLifecycleConsumer`
  - listeners em `bip.delivery.completed` e `bip.delivery.canceled`;
  - trata `DeliveryCompletedEvent` e `DeliveryCanceledEvent`;
  - registra logs e pode ser estendido para métricas.
- `FinancialTransactionConsumer`
  - listener em `bip.financial.transaction`;
  - trata `FinancialTransactionEvent`;
  - registra logs (ponto de extensão para antifraude, bilhetagem, etc.).

### 8.4. Semântica de "fila"

Do ponto de vista de negócio:

- **Fila de entregas disponíveis** = todas as entregas com `DeliveryStatus.AVAILABLE` salvas em Postgres;
- Entram na fila:
  - ao criar uma entrega (status `AVAILABLE` + evento `bip.delivery.requested`);
- Saem da fila:
  - quando são aceitas (`status` muda para `IN_ROUTE`);
  - quando são canceladas (`status = CANCELED`);
  - quando são concluídas (`status = COMPLETED`).

Kafka é usado como:

- trilha de eventos de domínio;
- possível integração assíncrona com outros serviços;
- e não como fonte única do estado da fila (que é mantido em Postgres/Redis).

---

## 9. Redis e entregas em rota

Porta:

- `DeliveryInRouteCachePort` – abstração de cache para entregas `IN_ROUTE`.

Adapter:

- `DeliveryInRouteRedisAdapter` – implementação usando Redis.

Uso:

- Ao aceitar entrega:
  - `DeliveryAssignmentService` salva a entrega `IN_ROUTE` no Redis;
- Ao cancelar entrega em rota:
  - `DeliveryCancellationService` remove do Redis;
- Ao concluir entrega:
  - `DeliveryCompletionService` remove do Redis;
- Endpoint dedicado:
  - `DeliveryInRouteQueryService` expõe listagem das entregas em rota por entregador;
  - controlador para `/api/driver/deliveries/in-route?driverId=...` (consulta diretamente o Redis).

Objetivo:

- deixar Redis como "fonte de verdade rápida" para corridas em andamento, enquanto o histórico completo fica no Postgres.

---

## 10. WebSocket / notificações em tempo real

Serviço:

- `DeliveryRealtimeNotifier` com `SimpMessagingTemplate`.

Destinos definidos:

- `/topic/deliveries/available`
  - broadcast de novas entregas disponíveis para todos os entregadores conectados.
- `/topic/deliveries/updates`
  - atualização de status de entregas (em rota, concluída, cancelada).
- `/topic/drivers/{driverId}/deliveries`
  - canal específico por entregador, para notificações direcionadas.

Eventos disparados para WebSocket (pontos principais):

- nova entrega criada (`AVAILABLE`);
- entrega aceita (`IN_ROUTE`);
- entrega concluída;
- entrega cancelada.

O frontend pode se conectar via STOMP sobre WebSocket, assinar esses tópicos e atualizar a interface em tempo real.

---

## 11. Endpoints principais (resumo)

Base URL do backend:

- `http://localhost:8087/api`

Alguns endpoints relevantes (nomes de paths exemplificados conforme a organização do projeto):

- Usuários:
  - `POST /api/users/registration` – cadastro de usuário (cliente/entregador);
  - `GET /api/admin/users` – listagem com filtros;
  - `POST /api/admin/users/{id}/approve` – aprovar usuário;
  - `POST /api/admin/users/{id}/reject` – rejeitar usuário;
  - `PUT /api/admin/users/{id}` – atualização de dados básicos;
  - `DELETE /api/admin/users/{id}` – exclusão.
- Carteira:
  - `POST /api/client/wallet/deposit` – depósito em saldo do cliente.
- Entregas – cliente:
  - `POST /api/client/deliveries` – cria uma entrega;
  - `GET /api/client/deliveries?clientId=...` – lista entregas do cliente;
  - `POST /api/client/deliveries/{id}/cancel?clientId=...&reason=...` – cancelamento (AVAILABLE ou IN_ROUTE).
- Entregas – entregador:
  - `GET /api/driver/deliveries/available` – vitrine de entregas disponíveis;
  - `GET /api/driver/deliveries/mine?driverId=...` – entregas do entregador;
  - `GET /api/driver/deliveries/in-route?driverId=...` – entregas em rota (Redis);
  - `POST /api/driver/deliveries/{id}/accept?driverId=...` – aceitar entrega;
  - `POST /api/driver/deliveries/{id}/complete?driverId=...` – concluir entrega.

---

## 12. Testes automatizados

- Framework: JUnit 5 + Mockito.
- Testes unitários.

Execução:

```bash
mvn clean test
```

Relatório de cobertura (Jacoco):

```bash
mvn jacoco:report
```

Arquivo:

- `target/site/jacoco/index.html`

Indicadores (aproximados):

- Cobertura global de instruções: ~76%;
- Serviços centrais de domínio/aplicação: ~80%+;
- DTOs, mappers e exceções com cobertura alta;
- Producers/consumers Kafka e WebSocket têm cobertura mínima por serem cascas finas.

---

## 13. Coleção Postman

Há uma collection de apoio (por exemplo: `BIP - Backend Flows`) no repositório, com um environment de exemplo (`bip-dev`) que encapsula:

- `baseUrl` do backend (`http://localhost:8087/api`);
- IDs dinâmicos capturados (clientes, entregador, entregas).

Fluxo coberto pela collection:

1. Cadastro dos usuários (quando não se usa o seeder).
2. Aprovação de entregador.
3. Depósito de saldo para clientes.
4. Criação de múltiplas entregas.
5. Aceite pelo entregador.
6. Cancelamento de entrega:
  - ainda `AVAILABLE` (sem multa);
  - já `IN_ROUTE` (com multa).
7. Conclusão de entrega:
  - débito do cliente;
  - crédito no entregador;
  - evento Kafka correspondente.

Execução:

1. Abrir o Postman.
2. Importar a collection do repositório.
3. Configurar environment (`bip-dev`) com `baseUrl` e atributos necessários.
4. Usar o Collection Runner para executar o fluxo end-to-end.

---
```
