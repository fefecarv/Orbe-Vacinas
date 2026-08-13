CREATE TABLE usuario_convenio (
    usuario_convenio_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    convenio_id BIGINT NOT NULL,
    numero_carteirinha VARCHAR(60) NOT NULL,
    titular VARCHAR(120) NOT NULL,
    data_validade DATE NOT NULL,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuario_convenio_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(usuario_id),
    CONSTRAINT fk_usuario_convenio_catalogo FOREIGN KEY (convenio_id) REFERENCES convenio(convenio_id),
    CONSTRAINT uk_usuario_convenio_carteira UNIQUE (convenio_id, numero_carteirinha)
);

INSERT INTO usuario_convenio
    (usuario_id, convenio_id, numero_carteirinha, titular, data_validade)
SELECT usuario_id, convenio_id, numero_carteirinha, titular, data_validade
  FROM convenio
 WHERE usuario_id IS NOT NULL
   AND numero_carteirinha IS NOT NULL
   AND data_validade IS NOT NULL;

ALTER TABLE agendamento ADD COLUMN usuario_convenio_id BIGINT NULL AFTER convenio_id;
UPDATE agendamento a
JOIN usuario_convenio uc ON uc.convenio_id = a.convenio_id AND uc.usuario_id = a.usuario_id
SET a.usuario_convenio_id = uc.usuario_convenio_id;
ALTER TABLE agendamento DROP FOREIGN KEY fk_agendamento_convenio;
ALTER TABLE agendamento DROP COLUMN convenio_id;
ALTER TABLE agendamento CHANGE usuario_convenio_id convenio_id BIGINT NULL;
ALTER TABLE agendamento ADD CONSTRAINT fk_agendamento_usuario_convenio
    FOREIGN KEY (convenio_id) REFERENCES usuario_convenio(usuario_convenio_id);

ALTER TABLE convenio DROP FOREIGN KEY fk_convenio_usuario;
ALTER TABLE convenio
    DROP COLUMN usuario_id,
    DROP COLUMN numero_carteirinha,
    DROP COLUMN titular,
    DROP COLUMN data_validade;
