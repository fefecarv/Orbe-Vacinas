CREATE DATABASE IF NOT EXISTS orbe CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE orbe;

CREATE TABLE usuario (
    usuario_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    cpf CHAR(11) NOT NULL UNIQUE,
    email VARCHAR(120) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    data_nascimento DATE NOT NULL,
    cep CHAR(8), logradouro VARCHAR(120), numero VARCHAR(20), complemento VARCHAR(80),
    bairro VARCHAR(80), cidade VARCHAR(80), estado CHAR(2),
    status ENUM('ATIVO','INATIVO','BLOQUEADO','PENDENTE') NOT NULL DEFAULT 'PENDENTE',
    verificacao_duas_etapas BOOLEAN NOT NULL DEFAULT FALSE,
    ultimo_acesso_em DATETIME NULL, ultimo_ip VARCHAR(45) NULL,
    token_recuperacao_hash VARCHAR(255) NULL,
    token_recuperacao_expira_em DATETIME NULL,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE usuario_perfil (
    usuario_perfil_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    perfil ENUM('PACIENTE','FUNCIONARIO','ADMINISTRADOR') NOT NULL,
    matricula VARCHAR(30) NULL, cargo VARCHAR(60) NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_perfil_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(usuario_id),
    CONSTRAINT uk_usuario_perfil UNIQUE (usuario_id, perfil)
);

CREATE TABLE dependente (
    dependente_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(120) NOT NULL, cpf CHAR(11) NULL UNIQUE,
    data_nascimento DATE NOT NULL, sexo VARCHAR(30) NULL,
    observacoes VARCHAR(500) NULL,
    status ENUM('ATIVO','INATIVO') NOT NULL DEFAULT 'ATIVO',
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE usuario_dependente (
    usuario_dependente_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL, dependente_id BIGINT NOT NULL,
    parentesco VARCHAR(40) NOT NULL, responsavel_legal BOOLEAN NOT NULL DEFAULT FALSE,
    pode_agendar BOOLEAN NOT NULL DEFAULT TRUE,
    pode_visualizar_carteira BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vinculo_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(usuario_id),
    CONSTRAINT fk_vinculo_dependente FOREIGN KEY (dependente_id) REFERENCES dependente(dependente_id),
    CONSTRAINT uk_usuario_dependente UNIQUE (usuario_id, dependente_id)
);

CREATE TABLE vacina (
    vacina_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(120) NOT NULL, fabricante VARCHAR(120) NOT NULL,
    descricao VARCHAR(500) NOT NULL, categoria VARCHAR(80) NOT NULL,
    indicacao VARCHAR(255) NOT NULL, esquema_doses VARCHAR(120) NOT NULL,
    valor_base DECIMAL(10,2) NOT NULL DEFAULT 0,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT ck_vacina_valor CHECK (valor_base >= 0)
);

CREATE TABLE lote (
    lote_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vacina_id BIGINT NOT NULL, numero_lote VARCHAR(45) NOT NULL,
    data_validade DATE NOT NULL, quantidade_inicial INT NOT NULL,
    quantidade_atual INT NOT NULL, fornecedor VARCHAR(120) NULL,
    status ENUM('ATIVO','ESGOTADO','BLOQUEADO','VENCIDO') NOT NULL DEFAULT 'ATIVO',
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lote_vacina FOREIGN KEY (vacina_id) REFERENCES vacina(vacina_id),
    CONSTRAINT uk_vacina_lote UNIQUE (vacina_id, numero_lote),
    CONSTRAINT ck_lote_quantidades CHECK (quantidade_inicial >= 0 AND quantidade_atual >= 0 AND quantidade_atual <= quantidade_inicial)
);

CREATE TABLE convenio (
    convenio_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL, plano VARCHAR(100) NOT NULL,
    codigo_operacional VARCHAR(45) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    tipo_cobertura ENUM('INTEGRAL','PERCENTUAL','COPARTICIPACAO','SEM_COBERTURA','ANALISE_MANUAL') NOT NULL DEFAULT 'ANALISE_MANUAL',
    percentual_desconto DECIMAL(5,2) NULL,
    valor_coparticipacao DECIMAL(10,2) NULL,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT ck_convenio_percentual CHECK (percentual_desconto IS NULL OR percentual_desconto BETWEEN 0 AND 100),
    CONSTRAINT ck_convenio_coparticipacao CHECK (valor_coparticipacao IS NULL OR valor_coparticipacao >= 0),
    CONSTRAINT uk_convenio_plano UNIQUE (nome, plano)
);

CREATE TABLE usuario_convenio (
    usuario_convenio_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL, convenio_id BIGINT NOT NULL,
    numero_carteirinha VARCHAR(60) NOT NULL, titular VARCHAR(120) NOT NULL,
    data_validade DATE NOT NULL,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuario_convenio_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(usuario_id),
    CONSTRAINT fk_usuario_convenio_catalogo FOREIGN KEY (convenio_id) REFERENCES convenio(convenio_id),
    CONSTRAINT uk_usuario_convenio_carteira UNIQUE (convenio_id, numero_carteirinha)
);

CREATE TABLE agendamento (
    agendamento_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    protocolo VARCHAR(40) NOT NULL UNIQUE,
    usuario_id BIGINT NULL, dependente_id BIGINT NULL,
    vacina_id BIGINT NOT NULL, convenio_id BIGINT NULL,
    data_agendamento DATETIME NOT NULL, unidade VARCHAR(100) NOT NULL, sala VARCHAR(40) NULL,
    dose_prevista VARCHAR(40) NOT NULL,
    tipo_atendimento ENUM('PARTICULAR','CONVENIO','CAMPANHA') NOT NULL,
    valor_estimado DECIMAL(10,2) NULL,
    status ENUM('PENDENTE','CONFIRMADO','ESPERA','EM_ATENDIMENTO','CONCLUIDO','CANCELADO','FALTOU') NOT NULL DEFAULT 'PENDENTE',
    motivo_cancelamento VARCHAR(255) NULL, cancelado_em DATETIME NULL,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_agendamento_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(usuario_id),
    CONSTRAINT fk_agendamento_dependente FOREIGN KEY (dependente_id) REFERENCES dependente(dependente_id),
    CONSTRAINT fk_agendamento_vacina FOREIGN KEY (vacina_id) REFERENCES vacina(vacina_id),
    CONSTRAINT fk_agendamento_usuario_convenio FOREIGN KEY (convenio_id) REFERENCES usuario_convenio(usuario_convenio_id),
    CONSTRAINT ck_agendamento_paciente CHECK ((usuario_id IS NOT NULL AND dependente_id IS NULL) OR (usuario_id IS NULL AND dependente_id IS NOT NULL)),
    CONSTRAINT ck_agendamento_valor CHECK (valor_estimado IS NULL OR valor_estimado >= 0),
    INDEX idx_agendamento_data_status (data_agendamento, status)
);

CREATE TABLE aplicacao (
    aplicacao_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    protocolo VARCHAR(40) NOT NULL UNIQUE, agendamento_id BIGINT NULL,
    usuario_id BIGINT NULL, dependente_id BIGINT NULL,
    funcionario_id BIGINT NOT NULL, lote_id BIGINT NOT NULL,
    dose VARCHAR(40) NOT NULL, data_aplicacao DATETIME NOT NULL,
    tipo_atendimento ENUM('PARTICULAR','CONVENIO','CAMPANHA') NOT NULL,
    via_administracao VARCHAR(40) NOT NULL, local_aplicacao VARCHAR(80) NOT NULL,
    valor_pago DECIMAL(10,2) NULL, observacoes VARCHAR(500) NULL,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_aplicacao_agendamento FOREIGN KEY (agendamento_id) REFERENCES agendamento(agendamento_id),
    CONSTRAINT fk_aplicacao_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(usuario_id),
    CONSTRAINT fk_aplicacao_dependente FOREIGN KEY (dependente_id) REFERENCES dependente(dependente_id),
    CONSTRAINT fk_aplicacao_funcionario FOREIGN KEY (funcionario_id) REFERENCES usuario(usuario_id),
    CONSTRAINT fk_aplicacao_lote FOREIGN KEY (lote_id) REFERENCES lote(lote_id),
    CONSTRAINT ck_aplicacao_paciente CHECK ((usuario_id IS NOT NULL AND dependente_id IS NULL) OR (usuario_id IS NULL AND dependente_id IS NOT NULL)),
    CONSTRAINT ck_aplicacao_valor CHECK (valor_pago IS NULL OR valor_pago >= 0),
    INDEX idx_aplicacao_data (data_aplicacao)
);

CREATE TABLE recomendacao_vacinal (
    recomendacao_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NULL, dependente_id BIGINT NULL, vacina_id BIGINT NOT NULL,
    dose_recomendada VARCHAR(40) NOT NULL, data_prevista DATE NULL,
    motivo VARCHAR(255) NOT NULL,
    status ENUM('RECOMENDADA','AGENDADA','REVISAR','CONCLUIDA','DESCARTADA') NOT NULL DEFAULT 'RECOMENDADA',
    agendamento_id BIGINT NULL,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_recomendacao_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(usuario_id),
    CONSTRAINT fk_recomendacao_dependente FOREIGN KEY (dependente_id) REFERENCES dependente(dependente_id),
    CONSTRAINT fk_recomendacao_vacina FOREIGN KEY (vacina_id) REFERENCES vacina(vacina_id),
    CONSTRAINT fk_recomendacao_agendamento FOREIGN KEY (agendamento_id) REFERENCES agendamento(agendamento_id),
    CONSTRAINT ck_recomendacao_paciente CHECK ((usuario_id IS NOT NULL AND dependente_id IS NULL) OR (usuario_id IS NULL AND dependente_id IS NOT NULL))
);

CREATE TABLE movimentacao_estoque (
    movimentacao_estoque_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lote_id BIGINT NOT NULL, usuario_id BIGINT NOT NULL, aplicacao_id BIGINT NULL,
    tipo ENUM('ENTRADA','APLICACAO','AJUSTE_ENTRADA','AJUSTE_SAIDA','PERDA','VENCIMENTO') NOT NULL,
    quantidade INT NOT NULL, saldo_anterior INT NOT NULL, saldo_posterior INT NOT NULL,
    motivo VARCHAR(255) NULL, criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_movimentacao_lote FOREIGN KEY (lote_id) REFERENCES lote(lote_id),
    CONSTRAINT fk_movimentacao_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(usuario_id),
    CONSTRAINT fk_movimentacao_aplicacao FOREIGN KEY (aplicacao_id) REFERENCES aplicacao(aplicacao_id),
    CONSTRAINT ck_movimentacao_valores CHECK (quantidade > 0 AND saldo_anterior >= 0 AND saldo_posterior >= 0),
    INDEX idx_movimentacao_lote_data (lote_id, criado_em)
);

CREATE TABLE auditoria (
    auditoria_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NULL, acao VARCHAR(80) NOT NULL, entidade VARCHAR(80) NOT NULL,
    entidade_id VARCHAR(60) NULL, descricao VARCHAR(500) NOT NULL,
    ip VARCHAR(45) NULL, criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(usuario_id),
    INDEX idx_auditoria_data (criado_em), INDEX idx_auditoria_usuario (usuario_id)
);
