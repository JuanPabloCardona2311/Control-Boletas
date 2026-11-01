package co.elmulardeoro.boletas.reporte;

import co.elmulardeoro.boletas.boleta.EstadoBoleta;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ReporteDtos {

    private ReporteDtos() {}

    public record Resumen(
            int vendidas,
            int libres,
            long recaudado,
            long porConfirmar,
            long porCobrar,
            long abonosPerdidos,
            List<EfectivoVendedor> efectivoPorVendedor) {}

    /** Efectivo que cada vendedor ha recibido, para cuadrar cuentas con Julián y Adriana. */
    public record EfectivoVendedor(Long vendedorId, String vendedor, long total) {}

    public record Deudor(int numero, String numeroTexto, EstadoBoleta estado, Long compradorId, String comprador,
                         String celular, String ubicacion, String vendedor, long pagado, long porConfirmar, long saldo) {}

    /**
     * Quienes deben, con las fechas de la regla de la rifa: recordar desde {@code fechaRecordatorio};
     * las apartadas sin ningún abono se liberan desde {@code fechaLiberacion}; las abonadas se esperan hasta el sorteo.
     */
    public record Deudores(LocalDate fechaRecordatorio, LocalDate fechaLiberacion, LocalDate fechaSorteo,
                           boolean tocaRecordar, boolean tocaLiberar, List<Deudor> deudores) {}

    public record TransferenciaPendiente(Long pagoId, int numero, String numeroTexto, String comprador, long valor,
                                         String nota, String registradoPor, Instant registradoEn) {}

    public record Ganador(String premio, String cifras, Integer numero, String numeroTexto, String comprador,
                          String celular, EstadoBoleta estado, boolean juega) {}

    public record ResultadoSorteo(String resultado, List<Ganador> ganadores) {}
}
