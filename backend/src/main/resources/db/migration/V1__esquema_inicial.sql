CREATE TABLE usuario (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    usuario     VARCHAR(50)  NOT NULL UNIQUE,
    clave_hash  VARCHAR(100) NOT NULL,
    rol         VARCHAR(20)  NOT NULL CHECK (rol IN ('ADMIN', 'VENDEDOR')),
    celular     VARCHAR(20),
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE rifa (
    id                 BIGSERIAL PRIMARY KEY,
    nombre             VARCHAR(150) NOT NULL,
    cantidad_numeros   INTEGER      NOT NULL CHECK (cantidad_numeros IN (100, 1000, 10000)),
    precio             BIGINT       NOT NULL CHECK (precio > 0),
    loteria            VARCHAR(100) NOT NULL,
    fecha_sorteo       DATE         NOT NULL,
    dias_recordatorio  INTEGER      NOT NULL DEFAULT 14 CHECK (dias_recordatorio >= 0),
    dias_liberacion    INTEGER      NOT NULL DEFAULT 7  CHECK (dias_liberacion >= 0),
    responsables       VARCHAR(300),
    activa             BOOLEAN      NOT NULL DEFAULT TRUE,
    resultado          VARCHAR(10),
    creada_en          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Solo puede haber una rifa activa a la vez
CREATE UNIQUE INDEX rifa_una_activa ON rifa (activa) WHERE activa;

CREATE TABLE premio (
    id              BIGSERIAL PRIMARY KEY,
    rifa_id         BIGINT       NOT NULL REFERENCES rifa (id) ON DELETE CASCADE,
    descripcion     VARCHAR(200) NOT NULL,
    posicion        VARCHAR(20)  NOT NULL CHECK (posicion IN ('PRIMERAS', 'ULTIMAS')),
    cifras          INTEGER      NOT NULL CHECK (cifras BETWEEN 1 AND 6),
    valor_efectivo  BIGINT,
    orden           INTEGER      NOT NULL DEFAULT 0
);

CREATE TABLE comprador (
    id         BIGSERIAL PRIMARY KEY,
    nombre     VARCHAR(100) NOT NULL,
    celular    VARCHAR(20)  NOT NULL,
    ubicacion  VARCHAR(150),
    creado_en  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX comprador_celular ON comprador (celular);

CREATE TABLE boleta (
    id              BIGSERIAL PRIMARY KEY,
    rifa_id         BIGINT      NOT NULL REFERENCES rifa (id) ON DELETE CASCADE,
    numero          INTEGER     NOT NULL,
    venta_id        BIGINT UNIQUE,
    actualizada_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (rifa_id, numero)
);

-- Una venta es el paso de una boleta por un comprador. Si se libera, queda como historial.
CREATE TABLE venta (
    id                 BIGSERIAL PRIMARY KEY,
    boleta_id          BIGINT      NOT NULL REFERENCES boleta (id) ON DELETE CASCADE,
    comprador_id       BIGINT      NOT NULL REFERENCES comprador (id),
    vendedor_id        BIGINT      NOT NULL REFERENCES usuario (id),
    creada_en          TIMESTAMPTZ NOT NULL DEFAULT now(),
    liberada_en        TIMESTAMPTZ,
    liberada_por       BIGINT REFERENCES usuario (id),
    motivo_liberacion  VARCHAR(200)
);

CREATE INDEX venta_boleta ON venta (boleta_id);
CREATE INDEX venta_comprador ON venta (comprador_id);

ALTER TABLE boleta ADD CONSTRAINT boleta_venta_fk FOREIGN KEY (venta_id) REFERENCES venta (id);

CREATE TABLE pago (
    id              BIGSERIAL PRIMARY KEY,
    venta_id        BIGINT      NOT NULL REFERENCES venta (id) ON DELETE CASCADE,
    valor           BIGINT      NOT NULL CHECK (valor > 0),
    metodo          VARCHAR(20) NOT NULL CHECK (metodo IN ('EFECTIVO', 'TRANSFERENCIA')),
    estado          VARCHAR(20) NOT NULL CHECK (estado IN ('CONFIRMADO', 'POR_CONFIRMAR', 'RECHAZADO')),
    nota            VARCHAR(200),
    registrado_por  BIGINT      NOT NULL REFERENCES usuario (id),
    registrado_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
    revisado_por    BIGINT REFERENCES usuario (id),
    revisado_en     TIMESTAMPTZ
);

CREATE INDEX pago_venta ON pago (venta_id);
CREATE INDEX pago_estado ON pago (estado);
