# Spec: MongoDB Database (WTC CRM)

## Resumo
Configurar MongoDB como banco de dados do WTC CRM usando Spring Data MongoDB, criando as collections `users`, `customers`, `segments`, `messages`, `campaigns` e `audit_logs` com seus respectivos índices e regras de negócio.

## Pesquisa realizada

### Bancos de dados avaliados

| Banco | Tipo | Prós | Contras | Veredicto |
|-------|------|------|---------|-----------|
| MongoDB | NoSQL Document | Schema flexível, Spring Data integrado, ideal para dados de CRM | Sem joins nativos | **Selecionado** |
| PostgreSQL | Relacional | ACID forte, relacionamentos | Overhead para dados do CRM | Descartado |
| H2 (atual) | Relacional em memória | Simples para dev | Dados perdidos ao reiniciar | Descartado |

### Spring Data MongoDB vs JPA

| Abordagem | Prós | Contras | Veredicto |
|-----------|------|---------|-----------|
| Spring Data MongoDB | Anotações similares ao JPA, MongoTemplate | Sem @ManyToMany nativo | **Selecionado** |
| JPA + Hibernate OG | Familiar, relacionamentos | Não é o padrão definido (MongoDB) | Descartado |

## Decisão

Arquitetura MongoDB com Spring Data MongoDB:

1. **Remover** H2, JPA e Flyway do `pom.xml`
2. **Adicionar** `spring-boot-starter-data-mongodb`
3. **Criar entities** como documentos `@Document` para todas as collections
4. **Criar repositories** (interface domain + implementação infrastructure)
5. **Atualizar** `AuthController` para autenticar via MongoDB
6. **Configurar** auditoria automática com `@CreatedDate` e `@LastModifiedDate`

```
users (1) ────────────── (0..1) customers
  │                               │
  │ senderId                      │ customerId
  ▼                               ▼
messages (N) ◄───── segmentId ──── segments (1)
  │                               │
  └──── campaignId ──────────────► campaigns (1)
                                  │
audit_logs (N) ◄── userId ────── users (1)
```

## Especificação de implementação

### Dependencies (pom.xml)

**Remover:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-flyway</artifactId>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

**Adicionar:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```

### Configuração (application.properties)

```properties
spring.data.mongodb.uri=mongodb://localhost:27017/wtc_crm
spring.data.mongodb.auto-index-creation=true
```

### Entities (domain/entities)

#### User.java
```java
@Document(collection = "users")
public class User {
    @Id
    private String id;
    
    private String name;
    private String email;
    private String password; // BCrypt hash
    private String role; // OPERATOR, CLIENT
    private String fcmToken;
    private Boolean active;
    
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

#### Customer.java
```java
@Document(collection = "customers")
public class Customer {
    @Id
    private String id;
    
    private String name;
    private String email;
    private String phone;
    private String userId; // ref User
    private List<String> tags;
    private Integer score;
    private String status; // ACTIVE, INACTIVE, PROSPECT
    private List<String> segmentIds;
    private String notes;
    private Boolean active;
    
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

#### Segment.java
```java
@Document(collection = "segments")
public class Segment {
    @Id
    private String id;
    
    private String name;
    private String description;
    private List<String> customerIds;
    private Map<String, Object> filters;
    private Boolean active;
    
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

#### Message.java
```java
@Document(collection = "messages")
public class Message {
    @Id
    private String id;
    
    private String senderId; // ref User
    private String recipientId; // ref Customer (1:1)
    private String segmentId; // ref Segment (broadcast)
    private String title;
    private String body;
    private String mediaUrl;
    private String url;
    private String type; // CHAT, PROMO, CAMPAIGN, EVENT, BANNER
    private List<Map<String, String>> actions;
    private Map<String, String> actionUrls;
    private String status; // SENT, DELIVERED, READ, FAILED
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
}
```

#### Campaign.java
```java
@Document(collection = "campaigns")
public class Campaign {
    @Id
    private String id;
    
    private String title;
    private String body;
    private String segmentId;
    private String mediaUrl;
    private String deeplink;
    private List<Map<String, String>> actions;
    private Map<String, String> actionUrls;
    private String status; // DRAFT, SCHEDULED, SENT, FAILED
    private String createdBy;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private Map<String, Integer> stats;
    
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

#### AuditLog.java
```java
@Document(collection = "audit_logs")
public class AuditLog {
    @Id
    private String id;
    
    private String userId;
    private String action; // LOGIN, CREATE_CUSTOMER, SEND_CAMPAIGN, etc.
    private String entityType;
    private String entityId;
    private String description;
    private String ipAddress;
    private LocalDateTime timestamp;
}
```

### Repositories

| Interface (domain/interfaces) | Implementação (infrastructure/repositories) |
|-------------------------------|---------------------------------------------|
| `IUserRepository` | `UserRepository` |
| `ICustomerRepository` | `CustomerRepository` |
| `ISegmentRepository` | `SegmentRepository` |
| `IMessageRepository` | `MessageRepository` |
| `ICampaignRepository` | `CampaignRepository` |
| `IAuditLogRepository` | `AuditLogRepository` |

### Índices a criar

```javascript
// users
db.users.createIndex({ email: 1 }, { unique: true })
db.users.createIndex({ role: 1 })
db.users.createIndex({ active: 1 })

// customers
db.customers.createIndex({ email: 1 }, { unique: true })
db.customers.createIndex({ tags: 1 })
db.customers.createIndex({ status: 1 })
db.customers.createIndex({ score: 1 })
db.customers.createIndex({ segmentIds: 1 })
db.customers.createIndex({ userId: 1 })
db.customers.createIndex({ active: 1 })
db.customers.createIndex({ status: 1, score: -1, tags: 1 })
db.customers.createIndex({ name: "text", email: "text" })

// segments
db.segments.createIndex({ name: 1 }, { unique: true })
db.segments.createIndex({ customerIds: 1 })
db.segments.createIndex({ active: 1 })

// messages
db.messages.createIndex({ recipientId: 1, sentAt: -1 })
db.messages.createIndex({ segmentId: 1, sentAt: -1 })
db.messages.createIndex({ senderId: 1, sentAt: -1 })
db.messages.createIndex({ status: 1 })
db.messages.createIndex({ type: 1, status: 1, sentAt: -1 })

// campaigns
db.campaigns.createIndex({ segmentId: 1 })
db.campaigns.createIndex({ status: 1 })
db.campaigns.createIndex({ createdBy: 1 })
db.campaigns.createIndex({ scheduledAt: 1 }, { sparse: true })
db.campaigns.createIndex({ sentAt: -1 })
db.campaigns.createIndex({ createdBy: 1, status: 1, createdAt: -1 })

// audit_logs
db.audit_logs.createIndex({ userId: 1, timestamp: -1 })
db.audit_logs.createIndex({ entityType: 1, entityId: 1, timestamp: -1 })
db.audit_logs.createIndex({ action: 1, timestamp: -1 })
db.audit_logs.createIndex({ timestamp: 1 }, { expireAfterSeconds: 31536000 })
```

### Seed Script (Mongo Shell)

```javascript
db.users.insertOne({
  name: "Admin WTC",
  email: "admin@wtc.com",
  password: "$2a$10$...",
  role: "OPERATOR",
  active: true,
  createdAt: new Date(),
  updatedAt: new Date()
});

db.segments.insertMany([
  {
    name: "Finance Innovation Talks",
    description: "CEOs e Diretores do mercado financeiro",
    customerIds: [],
    filters: { tags: ["Finance"], status: "ACTIVE", minScore: 50 },
    active: true,
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    name: "ESG Innovation Talks",
    description: "VPs e Diretores de ESG",
    customerIds: [],
    filters: { tags: ["ESG"], status: "ACTIVE" },
    active: true,
    createdAt: new Date(),
    updatedAt: new Date()
  },
  {
    name: "CX Innovation Talks",
    description: "Executivos de Marketing e Vendas",
    customerIds: [],
    filters: { tags: ["CX", "Marketing"], status: "ACTIVE" },
    active: true,
    createdAt: new Date(),
    updatedAt: new Date()
  }
]);
```

## Dependências novas

| Pacote | Motivo | Tamanho estimado |
|--------|--------|------------------|
| spring-boot-starter-data-mongodb | Integração Spring + MongoDB | ~800KB (core) |

## Riscos e considerações

1. **Integridade referencial**: MongoDB não aplica FKs — validação deve ser feita na camada de serviço (Java).
2. **Transações**: MongoDB tem transações desde v4.0, mas para o CRM simples não é crítico.
3. **BCrypt**: passwords hasheadas armazenadas como String no documento.
4. **Connection string**: em produção usar variável de ambiente `MONGODB_URI`.
5. **TTL audit_logs**: logs expiram automaticamente após 1 ano (31536000s).
6. **Regra messages**: `recipientId` e `segmentId` são mutuamente exclusivos.

## TODOs de implementação

- [ ] Remover dependências JPA, Flyway e H2 do pom.xml
- [ ] Adicionar spring-boot-starter-data-mongodb ao pom.xml
- [ ] Criar `MongoConfig.java` com `@EnableMongoAuditing` e conversores
- [ ] Criar `User.java` em domain/entities
- [ ] Criar `Customer.java` em domain/entities
- [ ] Criar `Segment.java` em domain/entities
- [ ] Criar `Message.java` em domain/entities
- [ ] Criar `Campaign.java` em domain/entities
- [ ] Criar `AuditLog.java` em domain/entities
- [ ] Criar interfaces em domain/interfaces para cada repository
- [ ] Criar implementações em infrastructure/repositories
- [ ] Atualizar `AuthController` para injetar `IUserRepository` e validar via MongoDB
- [ ] Atualizar `application.properties` com URI do MongoDB
- [ ] Criar script de seed para usuário admin e segmentos padrão
- [ ] Criar DTOs para registro de usuário (`UsuarioRequestDTO`, `UsuarioResponseDTO`)
- [ ] Adicionar endpoint `POST /api/auth/register`
