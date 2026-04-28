# Spec: JWT Authentication

## Resumo
Sistema de autenticação JWT para a API WtcSync. Usuários autenticados recebem um token JWT que deve ser enviado em todas as requisições protegidas via header `Authorization: Bearer <token>`.

## Pesquisa realizada

### Bibliotecas avaliadas

| Biblioteca | Versão | Prós | Contras | Veredicto |
|------------|--------|------|---------|-----------|
| JJWT (Java JWT) | 0.12.x | Mantida, compatível Java 21, documentação completa | Pouca curva de inicial | **Selecionada** |
| Auth0 JWT Java | 3.x | Popular,widely used | Pouco mantida recentemente | Descartada |
| Nimbus+JOSE | 7.x | Completa, flexível | Complexa para use case simples | Descartada |

### Abordagem de armazenamento

| Abordagem | Prós | Contras | Veredicto |
|-----------|------|---------|-----------|
| Spring Security + JWT only | Sem estado, escalável | Não é possível revogar tokens individuais | **Selecionada** |
| JWT + Redis | Revogação possível | Complexidade adicional | Descartada para MVP |

## Decisão

Arquitetura JWT com:

1. **Login endpoint** (`POST /api/auth/login`) → retorna token JWT
2. **JWT Filter** → intercepta requisições, valida token
3. **Security Config** → configura endpoints públicos/protegidos
4. **Token Service** → gera e valida tokens

```
┌─────────┐     ┌──────────┐     ┌─────────────┐
│ Client  │────▶│ Controller│────▶│TokenService │
└─────────┘     └──────────┘     └──────┬──────┘
         ┌──────────┐                  │
         │JWT Filter │◀─────────────────┘
         └────┬─────┘
              ▼
         ┌────────────┐
         │SecurityCtx │
         └────────────┘
```

## Especificação de implementação

### Dependencies (adicionar ao pom.xml)

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

### Novos arquivos

| Arquivo | Camada | Responsabilidade |
|---------|--------|------------------|
| `TokenService.java` | application/services | Gera e valida tokens JWT |
| `AuthController.java` | presentation/controllers | Endpoint de login |
| `AuthRequestDTO.java` | application/dtos | DTO para credenciais |
| `AuthResponseDTO.java` | application/dtos | DTO com token |
| `JwtAuthenticationFilter.java` | infrastructure/configs | Filtro de autenticação |
| `SecurityConfig.java` | infrastructure/configs | Configuração segurança |
| `application-jwt.properties` | resources | configs JWT |

### APIs

```java
// TokenService
public String generateToken(String username);
public String validateToken(String token);
public String getUsernameFromToken(String token);

// AuthController
@PostMapping("/api/auth/login")
public ResponseEntity<AuthResponseDTO> login(@RequestBody AuthRequestDTO request);

// AuthRequestDTO
record AuthRequestDTO(String username, String password) {}

// AuthResponseDTO  
record AuthResponseDTO(String token, String username, long expiresIn) {}
```

### Configurações (application.properties)

```properties
# JWT
jwt.secret=chave-secreta-minima-256-bits-para-hs256
jwt.expiration=86400000  # 24 horas em ms
```

### Endpoints

| Método | Path | Protegido | Descrição |
|--------|------|-----------|------------|
| POST | /api/auth/login | Não | Login, retorna token |
| GET | /api/auth/me | Sim | Retorna usuário atual |

## Dependências novas

| Pacote | Motivo | Tamanho estimado |
|--------|--------|------------------|
| jjwt-api 0.12.6 | Geração/validação JWT | ~100KB |
| jjwt-impl 0.12.6 | Implementação runtime | ~200KB |
| jjwt-jackson 0.12.6 | Serialização JWT | ~50KB |

## Riscos e considerações

1. **Sem revogação de tokens**: tokens expiram naturalmente (~24h). Para revogação antecipada, seria necessário Redis (considerar para v2).
2. **Segredo hardcoded**: em produção, usar variável de ambiente `JWT_SECRET`.
3. **Passwords**: implementação assume BCrypt para hash de senhas (já suportado pelo Spring Security).

## TODOs de implementação

- [ ] Adicionar dependências JJWT ao pom.xml
- [ ] Criar arquivo application.properties com configs JWT
- [ ] Criar AuthRequestDTO e AuthResponseDTO
- [ ] Criar TokenService com generateToken e validateToken
- [ ] Criar AuthController com endpoint /api/auth/login
- [ ] Criar JwtAuthenticationFilter
- [ ] Criar SecurityConfig