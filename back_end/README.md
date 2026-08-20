# Orbe Back-end

Backend Java Web tradicional, sem Spring, JPA ou Hibernate. A aplicação usa Java 21,
Servlets Jakarta, JDBC, SQL manual, MySQL e empacotamento WAR para Tomcat 11.

## Estrutura

- `model`: as 12 entidades persistidas como classes Java tradicionais;
- `dao`: `GenericDao<T, ID>` e contratos especializados;
- `dao/jdbc`: implementações com SQL explícito, `PreparedStatement` e `ResultSet`;
- `service`: regras de negócio e transações;
- `servlet`: endpoints HTTP executados pelo Tomcat;
- `dto`: objetos de entrada e saída;
- `config`: conexão e propriedades;
- `exception`: erros de domínio e persistência;
- `resources/db/migration`: schema SQL versionado.

## Configuração

Variáveis de ambiente:

```text
ORBE_DB_URL=jdbc:mysql://localhost:3306/orbe?useSSL=false&serverTimezone=America/Sao_Paulo
ORBE_DB_USER=root
ORBE_DB_PASSWORD=sua_senha
ORBE_ALLOWED_ORIGIN=http://localhost:5173
```

### Primeiro administrador

Para criar o primeiro administrador, configure temporariamente as variaveis abaixo
antes de iniciar o Tomcat:

```text
ORBE_BOOTSTRAP_ADMIN_ENABLED=true
ORBE_BOOTSTRAP_ADMIN_NAME=Administrador Orbe
ORBE_BOOTSTRAP_ADMIN_CPF=00000000000
ORBE_BOOTSTRAP_ADMIN_EMAIL=admin@orbe.local
ORBE_BOOTSTRAP_ADMIN_PASSWORD=defina-uma-senha-forte
ORBE_BOOTSTRAP_ADMIN_PHONE=00000000000
ORBE_BOOTSTRAP_ADMIN_BIRTH_DATE=2000-01-01
```

O bootstrap somente cria a conta quando ainda nao existe nenhum perfil
`ADMINISTRADOR`. Depois do primeiro acesso, defina
`ORBE_BOOTSTRAP_ADMIN_ENABLED=false` e remova a senha do ambiente. Nunca versione
uma senha real em arquivos `.env`, SQL ou no repositorio.

Os models seguem o padrão JavaBeans, com atributos privados, construtor vazio, getters e setters. Os `record` são usados somente nos DTOs imutáveis.

O fluxo da aplicação é:

```text
Svelte -> Servlet -> Service -> DAO JDBC -> MySQL
```

## Autenticacao e endpoints

A API usa sessao HTTP com cookie `HttpOnly`. Depois do login, o cliente envia
`credentials: "include"` em todas as requisicoes. Operacoes de escrita tambem
enviam no header `X-CSRF-Token` o valor `csrfToken` devolvido pelo login.

```text
POST   /api/auth/login
GET    /api/auth/me
POST   /api/auth/logout
POST   /api/usuarios
GET    /api/usuarios
GET    /api/usuarios/{id}
PUT    /api/usuarios/{id}
GET    /api/vacinas
POST   /api/vacinas
PUT    /api/vacinas
GET    /api/agendamentos?data=AAAA-MM-DD
GET    /api/agendamentos?usuarioId={id}
GET    /api/agendamentos?dependenteId={id}
PUT    /api/agendamentos/{id}/status
POST   /api/agendamentos
PUT    /api/agendamentos/{id}/reagendar
DELETE /api/agendamentos/{id}
GET    /api/aplicacoes?usuarioId={id}
GET    /api/aplicacoes?dependenteId={id}
POST   /api/aplicacoes
GET    /api/dependentes
GET    /api/convenios
GET    /api/recomendacoes?usuarioId={id}
GET    /api/recomendacoes?dependenteId={id}
GET    /api/lotes?vacinaId={id}
GET    /api/pacientes
POST   /api/pacientes
PUT    /api/pacientes/{U:id|D:id}
GET    /api/admin/usuarios
GET    /api/admin/lotes
POST   /api/admin/lotes
PUT    /api/admin/lotes/{id}
GET    /api/admin/convenios
POST   /api/admin/convenios
PUT    /api/admin/convenios/{id}
GET    /api/admin/movimentacoes
GET    /api/admin/auditoria
GET    /api/admin/relatorio?inicio=AAAA-MM-DD&fim=AAAA-MM-DD
```

O cadastro anonimo sempre cria um `PACIENTE`. Somente um administrador autenticado
pode cadastrar funcionarios ou administradores. O registro de aplicacao exige
perfil interno e o `funcionarioId` do proprio usuario autenticado.

Os verbos HTTP não fazem consultas. Eles somente definem a operação da API. Todo
`SELECT`, `INSERT`, `UPDATE` e desativação lógica fica escrito manualmente em `dao/jdbc`.

## Tomcat

O projeto gera `orbe-backend.war`. Ele deve ser implantado no Tomcat 11, compatível
com Jakarta Servlet 6.1. O driver MySQL e o Jackson ficam dentro do WAR; a API de
Servlet é fornecida pelo Tomcat.
