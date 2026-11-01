package co.elmulardeoro.boletas.boleta;

public enum EstadoPago {
    CONFIRMADO,
    /** Transferencia registrada por un vendedor que Julián o Adriana todavía no han visto en la cuenta. */
    POR_CONFIRMAR,
    RECHAZADO
}
