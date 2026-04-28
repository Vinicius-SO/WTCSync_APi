# WtcSync API

API para gestão de chat e mensagens com módulo CRM, desenvolvida em parceria com o **WTC** dentro do curso de Análise e Desenvolvimento de Sistemas da **FIAP**.

## Stack

- **Java** 21
- **Spring Boot** 4.0.6
- **JPA / Hibernate**
- **Flyway** (migrations)
- **Spring Security** + JWT
- **Swagger** (OpenAPI)
- **H2** (banco em memória para desenvolvimento)

## Funcionalidades

### Autenticação JWT
- `POST /api/auth/login` - Login e geração de token JWT
- `GET /api/auth/me` - Retorna dados do usuário autenticado

## Como Rodar

### Pré-requisitos
- Java 21
- Maven

### Comandos

```bash
# Compilar
./mvnw compile

# Rodar aplicação
./mvnw spring-boot:run
```

### URLs
- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:wtcsync`)

## Testando a API

### Via Swagger
1. Acesse `http://localhost:8080/swagger-ui.html`
2. Execute `POST /api/auth/login` com:
   ```json
   {
     "username": "admin",
     "password": "admin123"
   }
   ```
3. Copie o token retornado
4. Clique em **Authorize** e cole o token
5. Teste os endpoints protegidos (ex: `GET /api/auth/me`)

### Via curl
```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Usar o token retornado para chamadas autenticadas
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <SEU_TOKEN>"
```

## Variáveis de Ambiente (application.properties)

```properties
# JWT
jwt.secret=sua_chave_secreta_aqui
jwt.expiration=86400000  # 24 horas em ms

# Banco de dados
spring.datasource.url=jdbc:h2:mem:wtcsync
```

## Estrutura do Projeto

```
src/
├── main/
│   ├── java/com/fiap/WtcSync/
│   │   ├── domain/           # Entidades e interfaces
│   │   ├── application/    # Casos de uso, DTOs, serviços
│   │   ├── infrastructure/ # Repositórios, configurações
│   │   └── presentation/  # Controllers REST
│   └── resources/       # Configurações, migrations
└── test/
    └── java/             # Testes
```

## Repositório

GitHub: https://github.com/Vinicius-SO/WTCSync_APi

## Licença

FIAP - Análise e Desenvolvimento de Sistemas