ALTER TABLE usuario
    ADD COLUMN troca_senha_obrigatoria BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN unidade VARCHAR(80) NULL;

ALTER TABLE vacina
    ADD COLUMN idade_minima_meses INT NOT NULL DEFAULT 0,
    ADD COLUMN idade_maxima_meses INT NULL,
    ADD COLUMN numero_doses INT NOT NULL DEFAULT 1,
    ADD COLUMN intervalo_dias INT NULL,
    ADD COLUMN reforco_meses INT NULL,
    ADD CONSTRAINT ck_vacina_idades CHECK (idade_minima_meses >= 0 AND (idade_maxima_meses IS NULL OR idade_maxima_meses >= idade_minima_meses)),
    ADD CONSTRAINT ck_vacina_doses CHECK (numero_doses > 0);

CREATE TABLE configuracao_agenda (
    configuracao_agenda_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    unidade VARCHAR(80) NOT NULL,
    dia_semana TINYINT NOT NULL,
    hora_abertura TIME NOT NULL,
    hora_fechamento TIME NOT NULL,
    intervalo_minutos INT NOT NULL DEFAULT 30,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uk_agenda_unidade_dia (unidade, dia_semana),
    CONSTRAINT ck_agenda_dia CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT ck_agenda_horas CHECK (hora_fechamento > hora_abertura),
    CONSTRAINT ck_agenda_intervalo CHECK (intervalo_minutos BETWEEN 10 AND 240)
);

CREATE TABLE bloqueio_agenda (
    bloqueio_agenda_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    unidade VARCHAR(80) NOT NULL,
    data_bloqueio DATE NOT NULL,
    hora_inicio TIME NULL,
    hora_fim TIME NULL,
    motivo VARCHAR(160) NOT NULL,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_bloqueio_horas CHECK ((hora_inicio IS NULL AND hora_fim IS NULL) OR (hora_inicio IS NOT NULL AND hora_fim > hora_inicio))
);

INSERT INTO configuracao_agenda (unidade,dia_semana,hora_abertura,hora_fechamento,intervalo_minutos,ativo) VALUES
('Orbe Centro',1,'08:00','17:00',30,TRUE),('Orbe Centro',2,'08:00','17:00',30,TRUE),
('Orbe Centro',3,'08:00','17:00',30,TRUE),('Orbe Centro',4,'08:00','17:00',30,TRUE),
('Orbe Centro',5,'08:00','17:00',30,TRUE);
