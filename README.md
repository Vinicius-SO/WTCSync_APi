# WTC Sync

Plataforma integrada de CRM e mensageria desenvolvida para o **World Trade Center Business Club São Paulo** em parceria com a **FIAP**. O sistema une gestão de relacionamento com disparo de comunicações personalizadas via app mobile.

## Sobre o Projeto

O WTC CRM nasceu de um problema real: o World Trade Center Business Club São Paulo precisava de uma forma melhor de falar com seus clientes. E-mail marketing genérico não funciona para uma base de CEOs, VPs e diretores que esperam um nível de personalização acima da média. Ferramentas avulsas de WhatsApp ou push notification não se integram ao histórico do cliente. O resultado era um relacionamento fragmentado, sem rastreabilidade e difícil de escalar.

A solução é uma plataforma que une CRM e mensageria num sistema só. O operador vê o perfil completo do cliente, segmenta audiências por critérios precisos e dispara comunicações direto para o app mobile. O cliente recebe tudo organizado, com botões de ação e links que levam exatamente para onde a empresa quer.

## Stack

- **Java** 21
- **Spring Boot** 4.0.6
- **MongoDB** (spring-boot-starter-data-mongodb)
- **Spring Security** + JWT
- **Swagger** (OpenAPI)
- **WebSocket** (atualizações em tempo real)
- **Firebase FCM** (push notifications)

## Arquitetura

```
┌─────────────────────────────────────────────────────┐
│                   App Mobile (Cliente)              │
│  Chat, Histórico, Campanhas, Botões de Ação        │
└──────────────────────┬──────────────────────────────┘
                       │ REST API + WebSocket
┌──────────────────────┴──────────────────────────────┐
│              Backend Java (Operador)                │
│  CRM, Segmentação, Campanhas, Chat, Auditoria     │
└─────────────────────────────────────────────────────┘
```

## Funcionalidades

### Visão do Operador (Backend)

#### CRM com busca e filtros
- Lista de clientes com filtros por tag, score, status e segmento
- Perfil 360° com histórico de mensagens, campanhas e tarefas
- Anotações rápidas no perfil do cliente

#### Segmentação
- Agrupamento de clientes por critérios combináveis (tags, score, status)
- Segmentos reutilizáveis para campanhas futuras

#### Chat 1:1
- Mensagens diretas com push notification no app
- Histórico persistente para ambos os lados
- Status de mensagem: enviado, entregue, lido

#### Campanhas Express
- Disparo imediato para segmentos inteiros
- Título, texto, imagem opcional e até dois botões de ação
- URLs de destino configuráveis
- Envio via Firebase FCM
- Estatísticas consolidadas por campanha

#### Comandos rápidos e gestos
- Templates via comandos (ex: `/promo`, `/agradecer`)
- Gestos para marcar como importante ou criar tarefa

### Visão do Cliente (App Mobile)

- Histórico de chat organizado
- Campanhas com botões de ação configuráveis
- Deep links que abrem telas específicas do app diretamente na conversa
- Recebimento de push notifications

## Modelo de Mensagem

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

## Endpoints da API

### Autenticação
- `POST /api/auth/login` - Login e geração de token JWT
- `POST /api/auth/register` - Registro de novo usuário
- `GET /api/auth/me` - Retorna dados do usuário autenticado

### Clientes (CRM)
- `GET /api/clients` - Lista clientes com filtros (tag, score, status, segmento)
- `GET /api/clients/{id}` - Detalhes do cliente
- `POST /api/clients` - Criar cliente
- `PUT /api/clients/{id}` - Atualizar cliente
- `GET /api/clients/{id}/history` - Histórico 360° do cliente

### Segmentos
- `GET /api/segments` - Lista segmentos
- `POST /api/segments` - Criar segmento
- `GET /api/segments/{id}/clients` - Clientes no segmento

### Chat
- `GET /api/chat/{clientId}` - Histórico de chat
- `POST /api/chat/{clientId}/message` - Enviar mensagem
- `WebSocket /ws/chat` - Atualizações em tempo real

### Campanhas
- `GET /api/campaigns` - Lista campanhas
- `POST /api/campaigns` - Criar e disparar campanha
- `GET /api/campaigns/{id}/stats` - Estatísticas da campanha

## Como Rodar

### Pré-requisitos
- Java 21
- Maven
- MongoDB Atlas (ou local)

### Comandos

```bash
# Compilar
./mvnw compile

# Rodar aplicação
./mvnw spring-boot:run
```

### Configuração

Crie um arquivo `application.properties` com:

```properties
# MongoDB Atlas
spring.data.mongodb.uri=mongodb+srv://user:password@cluster.mongodb.net/wtc_crm
spring.data.mongodb.auto-index-creation=true

# JWT
jwt.secret=seu_segredo_jwt_min_256_bits
jwt.expiration=86400000

# Firebase FCM
firebase.credentials.path=classpath:firebase-service-account.json
```

### URLs
- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Testando a API

### Via Swagger
1. Acesse `http://localhost:8080/swagger-ui.html`
2. Execute `POST /api/auth/register` para criar um usuário
3. Execute `POST /api/auth/login` com as credenciais
4. Copie o token retornado
5. Clique em **Authorize** e cole o token
6. Teste os endpoints protegidos

### Via curl
```bash
# Registrar
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@wtc.com","password":"admin123"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@wtc.com","password":"admin123"}'

# Usar o token para chamadas autenticadas
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <SEU_TOKEN>"
```

## Estrutura do Projeto

```
src/
├── main/
│   ├── java/com/fiap/WtcSync/
│   │   ├── domain/           # Entidades e interfaces (regras de negócio)
│   │   │   ├── entities/   # User, Client, Segment, Campaign, Message
│   │   │   └── interfaces/ # Repository interfaces
│   │   ├── application/    # Casos de uso e serviços
│   │   │   ├── usecases/   # Casos de uso
│   │   │   ├── dtos/       # Data Transfer Objects
│   │   │   └── services/   # Application services (Token, FCM, WebSocket)
│   │   ├── infrastructure/ # Implementações externas
│   │   │   ├── repositories/ # MongoDB repositories
│   │   │   └── configs/   # Configurações (Security, Mongo, WebSocket)
│   │   └── presentation/  # Adaptadores de entrada
│   │       └── controllers/ # REST controllers
│   └── resources/       # Configurações
└── test/
    └── java/             # Testes
```

## Repositório

GitHub: https://github.com/Vinicius-SO/WTCSync_APi

## Licença

FIAP - Análise e Desenvolvimento de Sistemas
