# Orbe Vacinas

O Orbe é um sistema web para gestão de vacinação em clínicas, laboratórios e hospitais. O projeto reúne, em uma única aplicação, o cadastro de pacientes, o agendamento de atendimentos, o registro de aplicações, a carteira vacinal e o controle de estoque.

O sistema possui três perfis de acesso:

- **Paciente:** consulta agendamentos, carteira vacinal, dependentes e convênios;
- **Funcionário:** acompanha a agenda diária, realiza check-in, gerencia pacientes e registra aplicações;
- **Administrador:** gerencia usuários, vacinas, lotes, convênios, funcionamento da clínica, relatórios e auditoria.

## Principais funcionalidades

- Cadastro e autenticação de usuários;
- Gestão de pacientes e dependentes;
- Agendamento, reagendamento e cancelamento de vacinas;
- Controle dos estados do atendimento;
- Registro de aplicações por profissional e lote;
- Atualização automática do estoque após a aplicação;
- Carteira e histórico vacinal;
- Cadastro de vacinas, lotes e convênios;
- Configuração dos dias e horários de funcionamento;
- Relatórios administrativos e registros de auditoria;
- Temas claro e escuro.

## Tecnologias

### Front-end

- Svelte 5;
- TypeScript;
- Vite;
- Vitest;
- CSS com design system próprio.

### Back-end

- Java 21;
- Jakarta Servlet 6.1;
- JDBC e SQL manual;
- Jackson;
- Maven;
- Tomcat 11;
- MySQL.

O projeto não utiliza Spring, JPA ou Hibernate.

## Arquitetura

O front-end e o back-end são aplicações separadas. A comunicação acontece por HTTP, utilizando JSON.

```text
Svelte
  ↓ HTTP/JSON
Filtro de segurança
  ↓
Servlet
  ↓
Service
  ↓
DAO JDBC
  ↓
MySQL
```

No back-end, os Servlets recebem as requisições, os Services executam as regras de negócio e os DAOs concentram os comandos SQL e o acesso ao banco.

## Estrutura do projeto

```text
orbe/
├── back_end/
│   ├── src/main/java/br/com/orbe/
│   │   ├── config/
│   │   ├── dao/
│   │   ├── dto/
│   │   ├── exception/
│   │   ├── model/
│   │   ├── service/
│   │   ├── servlet/
│   │   └── util/
│   └── src/main/resources/db/migration/
├── front_end/
│   └── src/
│       ├── design-system/
│       ├── features/
│       ├── layout/
│       ├── lib/
│       └── mocks/
└── docs/
```

## Modelo do banco de dados

O banco é criado e evoluído pelos arquivos SQL presentes em `back_end/src/main/resources/db/migration`. Eles devem ser executados na ordem numérica, de `V1` até `V4`.

![Diagrama de Entidade e Relacionamento do Orbe](docs/der-orbe.png)

## Como executar

### Pré-requisitos

- Java 21;
- Maven;
- Tomcat 11;
- MySQL;
- Node.js e npm.

### 1. Banco de dados

Crie um banco chamado `orbe` e execute os scripts da pasta de migrações na sequência:

```text
V1__create_orbe_schema.sql
V2__add_convenio_automatic_analysis.sql
V3__separate_insurance_catalog_and_cards.sql
V4__administrative_rules.sql
```

Antes de iniciar o Tomcat, configure as variáveis do banco. Exemplo no PowerShell:

```powershell
$env:ORBE_DB_URL="jdbc:mysql://localhost:3306/orbe?useSSL=false&serverTimezone=America/Sao_Paulo"
$env:ORBE_DB_USER="root"
$env:ORBE_DB_PASSWORD="sua_senha"
$env:ORBE_ALLOWED_ORIGIN="http://localhost:5173"
```

### 2. Back-end

Na pasta `back_end`, gere o arquivo WAR:

```bash
mvn clean package
```

Copie `target/orbe-backend-1.0.0-SNAPSHOT.war` para a pasta `webapps` do Tomcat com o nome `orbe-backend.war`. Em seguida, inicie o servidor pelo `startup.bat` no Windows ou `startup.sh` no Linux.

A API ficará disponível em:

```text
http://localhost:8080/orbe-backend/api
```

### 3. Front-end

Na pasta `front_end`, instale as dependências e inicie o Vite:

```bash
npm install
npm run dev
```

A aplicação ficará disponível em:

```text
http://localhost:5173
```

Durante o desenvolvimento, o Vite encaminha as chamadas iniciadas por `/orbe-backend` para o Tomcat local.

## Verificações do projeto

Front-end:

```bash
npm run check
npm test
npm run build
```

Back-end:

```bash
mvn test package
```

## Segurança

As senhas são armazenadas com hash PBKDF2 e salt aleatório. A autenticação utiliza sessão HTTP com cookie `HttpOnly`, e as operações de escrita exigem um token CSRF. O acesso às rotas também é verificado de acordo com o perfil do usuário.

Este projeto foi desenvolvido como trabalho acadêmico de Engenharia de Software.
