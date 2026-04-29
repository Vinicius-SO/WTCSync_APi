# AGENTS.md

## Sobre o Projeto

O **WTC Sync** nasceu de um problema real: o World Trade Center Business Club São Paulo precisava de uma forma melhor de falar com seus clientes. E-mail marketing genérico não funciona para uma base de CEOs, VPs e diretores que esperam um nível de personalização acima da média. Ferramentas avulsas de WhatsApp ou push notification não se integram ao histórico do cliente. O resultado era um relacionamento fragmentado, sem rastreabilidade e difícil de escalar.

A solução é uma plataforma que une CRM e mensageria num sistema só. O operador vê o perfil completo do cliente, segmenta audiências por critérios precisos e dispara comunicações direto para o app mobile. O cliente recebe tudo organizado, com botões de ação e links que levam exatamente para onde a empresa quer.

## Estrutura do Projeto

O WtcSync segue a **Clean Architecture** com 4 camadas principais:

```
src/
├── main/
│   ├── java/com/fiap/WtcSync/
│   │   ├── domain/           # Entidades e interfaces (regras de negócio)
│   │   │   ├── entities/   # User, Client, Segment, Campaign, Message
│   │   │   └── interfaces/ # Repository interfaces (abstractions)
│   │   ├── application/    # Casos de uso e serviços
│   │   │   ├── usecases/   # Use cases
│   │   │   ├── dtos/       # Data Transfer Objects
│   │   │   └── services/   # Application services (Token, FCM, WebSocket)
│   │   ├── infrastructure/ # Implementações externas
│   │   │   ├── repositories/ # MongoDB repository implementations
│   │   │   └── configs/   # Configurações (Security, Mongo, WebSocket)
│   │   └── presentation/  # Adaptadores de entrada
│   │       └── controllers/ # REST controllers
│   └── resources/       # Recursos (configs)
└── test/
    └── java/             # Testes
```

### Camadas

| Camada | Responsabilidade |
|-------|------------------|
| **domain** | Entidades, regras de negócio-core, interfaces abstratas |
| **application** | Casos de uso, DTOs, serviços de aplicação |
| **infrastructure** | Implementações de banco, frameworks, serviços externos |
| **presentation** | Controllers REST, adapters de entrada |

## Funcionalidades

### Visão do Operador (Backend)

#### CRM com busca e filtros
- Lista de clientes com filtros por tag, score, status e segmento
- Perfil 360° com histórico de mensagens, campanhas recebidas e tarefas abertas
- Anotações rápidas no perfil do cliente

#### Segmentação
- Agrupamento de clientes por critérios combináveis (tags como "Finance" ou "ESG", score mínimo, status ativo)
- Segmentos reutilizáveis para campanhas futuras

#### Chat 1:1
- Mensagens diretas com push notification via Firebase FCM
- Histórico persistente para ambos os lados
- Acompanhamento de status: enviado, entregue, lido

#### Campanhas Express
- Disparo imediato para um segmento inteiro
- Título, texto, imagem opcional e até dois botões de ação
- URLs de destino configuráveis
- Envio via Firebase FCM
- Estatísticas consolidadas por campanha

#### Comandos rápidos e gestos
- Templates via comandos (ex: `/promo`, `/agradecer`)
- Gestos para marcar como importante ou criar tarefa

### Visão do Cliente (App Mobile)

- Histórico de chat organizado com todas as comunicações
- Campanhas com botões de ação configurados pelo operador
- Deep links que abrem telas específicas do app diretamente na conversa
- Recebimento de push notifications

### Modelo de Mensagem

```json
{
  "title": "Financial Shift 2025",
  "body": "Não perca o maior evento de finanças do ano.",
  "url": "https://wtc.com/evento",
  "mediaUrl": "https://cdn.wtc.com/banners/financial-shift.png",
  "actions": [
    { "action": "btn1", "title": "Garantir Vaga" },
    { "action": "btn2", "title": "Ver Programação" }
  ],
  "actionUrls": {
    "btn1": "https://wtc.com/evento/inscricao",
    "btn2": "https://wtc.com/evento/programacao"
  }
}
```

## Convenções

### Nomenclatura
- **Entities**: `NomeEntidade.java` (ex: `User.java`, `Client.java`, `Campaign.java`)
- **Use Cases**: `NomeEntidadeUseCase.java` ou `CriarNomeEntidadeUseCase.java`
- **DTOs**: `NomeEntidadeRequestDTO.java`, `NomeEntidadeResponseDTO.java`
- **Repositories**: `INomeEntidadeRepository.java` (interface), `NomeEntidadeRepository.java` (implementação)
- **Controllers**: `NomeEntidadeController.java`

### Pacotes
- `com.fiap.WtcSync.domain.entities`
- `com.fiap.WtcSync.domain.interfaces`
- `com.fiap.WtcSync.application.usecases`
- `com.fiap.WtcSync.application.dtos`
- `com.fiap.WtcSync.application.services`
- `com.fiap.WtcSync.infrastructure.repositories`
- `com.fiap.WtcSync.infrastructure.configs`
- `com.fiap.WtcSync.presentation.controllers`

## Comandos

```bash
# Executar aplicação
./mvnw spring-boot:run

# Compilar
./mvnw compile

# Testar
./mvnw test

# Build
./mvnw package
```

## Stack

- Java 21
- Spring Boot 4.0.6
- **MongoDB** (spring-boot-starter-data-mongodb)
- Spring Security + JWT
- REST API (Spring Web MVC)
- WebSocket (atualizações em tempo real)
- Firebase FCM (push notifications)
- Swagger (OpenAPI)

## TODOs para implementação futura

### Autenticação JWT (✅ Implementado)
- [x] Substituir credenciais hardcoded por autenticação com banco de dados
- [x] Implementar Entity `User` e Repository para persistência
- [x] Adicionar endpoint de registro de usuários (`/api/auth/register`)
- [ ] Mover `jwt.secret` para variável de ambiente (`JWT_SECRET`)
- [ ] Implementar logout (blacklist de tokens ou shorter expiration)
- [ ] Adicionar refresh token
- [ ] Implementar testes unitários para `TokenService` e `AuthController`

### CRM e Clientes
- [ ] Implementar entidade `Client` com tags, score, status e segmento
- [ ] Criar endpoints REST para CRUD de clientes
- [ ] Implementar filtros de busca (tag, score, status, segmento)
- [ ] Criar perfil 360° com histórico de mensagens e campanhas
- [ ] Adicionar anotações rápidas no perfil do cliente

### Segmentação
- [ ] Implementar entidade `Segment` com critérios combináveis
- [ ] Criar endpoints para CRUD de segmentos
- [ ] Implementar lógica de filtragem de clientes por critérios
- [ ] Reutilização de segmentos em campanhas

### Chat 1:1
- [ ] Implementar entidade `Message` com status (enviado, entregue, lido)
- [ ] Criar endpoints para envio e histórico de mensagens
- [ ] Integrar com Firebase FCM para push notifications
- [ ] Implementar WebSocket para atualizações em tempo real

### Campanhas Express
- [ ] Implementar entidade `Campaign` com título, texto, imagem e botões
- [ ] Criar endpoint para disparo de campanha para segmentos
- [ ] Integrar com Firebase FCM para envio em massa
- [ ] Implementar coleta e exibição de estatísticas de campanha

### Comandos e Gestos
- [ ] Implementar sistema de templates de mensagens
- [ ] Criar comandos rápidos (ex: `/promo`, `/agradecer`)
- [ ] Implementar gestos para marcar como importante ou criar tarefa

### App Mobile (Cliente)
- [ ] Implementar recebimento de push notifications
- [ ] Criar histórico de chat organizado no app
- [ ] Implementar exibição de campanhas com botões de ação
- [ ] Configurar deep links para telas específicas do app

### Auditoria e Logs
- [ ] Implementar logs de auditoria para ações do operador
- [ ] Criar rastreamento de entrega e leitura de mensagens
- [ ] Adicionar endpoints para consulta de logs
