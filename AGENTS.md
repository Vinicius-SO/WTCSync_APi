# AGENTS.md

## Estrutura do Projeto

O WtcSync segue a **Clean Architecture** com 4 camadas principais:

```
src/
├── main/
│   ├── java/com/fiap/WtcSync/
│   │   ├── domain/           # Entidades e interfaces (regras de negócio)
│   │   │   ├── entities/   # Domain entities
│   │   │   └── interfaces/ # Repository interfaces (abstractions)
│   │   ├── application/    # Casos de uso e serviços
│   │   │   ├── usecases/   # Use cases
│   │   │   ├── dtos/       # Data Transfer Objects
│   │   │   └── services/   # Application services
│   │   ├── infrastructure/ # Implementações externas
│   │   │   ├── repositories/ # Repository implementations
│   │   │   └── configs/   # Configurações
│   │   └── presentation/  # Adaptadores de entrada
│   │       └── controllers/ # REST controllers
│   └── resources/       # Recursos (configs, migrations)
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

## Convenções

### Nomenclatura
- **Entities**: `NomeEntidade.java` (ex: `Usuario.java`)
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
- JPA / Hibernate
- Flyway (migrations)
- Spring Security
- REST API (Spring Web MVC)

## TODOs para implementação futura

### Autenticação JWT
- [ ] Substituir credenciais hardcoded em `AuthController.java` (`admin`/`admin123`) por autenticação com banco de dados
- [ ] Mover `jwt.secret` para variável de ambiente (`JWT_SECRET`)
- [ ] Implementar Entity `Usuario` e Repository para persistência
- [ ] Adicionar endpoint de registro de usuários (`/api/auth/register`)
- [ ] Implementar logout ( blacklist de tokens ou shorter expiration)
- [ ] Adicionar refresh token
- [ ] Implementar testes unitários para `TokenService` e `AuthController`