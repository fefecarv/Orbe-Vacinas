ALTER TABLE convenio
    ADD COLUMN tipo_cobertura ENUM(
        'INTEGRAL',
        'PERCENTUAL',
        'COPARTICIPACAO',
        'SEM_COBERTURA',
        'ANALISE_MANUAL'
    ) NOT NULL DEFAULT 'ANALISE_MANUAL' AFTER ativo,
    ADD COLUMN percentual_desconto DECIMAL(5,2) NULL AFTER tipo_cobertura,
    ADD COLUMN valor_coparticipacao DECIMAL(10,2) NULL AFTER percentual_desconto,
    ADD CONSTRAINT ck_convenio_percentual
        CHECK (percentual_desconto IS NULL OR percentual_desconto BETWEEN 0 AND 100),
    ADD CONSTRAINT ck_convenio_coparticipacao
        CHECK (valor_coparticipacao IS NULL OR valor_coparticipacao >= 0);
