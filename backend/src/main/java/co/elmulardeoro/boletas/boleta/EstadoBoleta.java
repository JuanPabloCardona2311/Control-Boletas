package co.elmulardeoro.boletas.boleta;

/** El estado no se guarda: sale de lo que se ha pagado y confirmado. */
public enum EstadoBoleta {
    LIBRE,
    APARTADA,
    ABONADA,
    PAGADA;

    public static EstadoBoleta segun(long pagado, long precio) {
        if (pagado >= precio) {
            return PAGADA;
        }
        return pagado > 0 ? ABONADA : APARTADA;
    }
}
