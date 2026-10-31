package co.elmulardeoro.boletas.boleta;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class BoletaDtos {

    private BoletaDtos() {}

    /** Lo que pinta la cuadrícula. Solo trae los números ocupados; los que no aparecen están libres. */
    public record TableroDto(Long rifaId, int cantidadNumeros, int cifras, long precio, Conteo conteo, List<Ocupada> ocupadas) {}

    public record Ocupada(int numero, EstadoBoleta estado, boolean porConfirmar) {}

    public record Conteo(int libres, int apartadas, int abonadas, int pagadas) {}

    public record CompradorDto(Long id, String nombre, String celular, String ubicacion) {
        public static CompradorDto de(Comprador c) {
            return new CompradorDto(c.getId(), c.getNombre(), c.getCelular(), c.getUbicacion());
        }
    }

    public record PagoDto(Long id, long valor, MetodoPago metodo, EstadoPago estado, String nota,
                          String registradoPor, Instant registradoEn, String revisadoPor, Instant revisadoEn) {
        public static PagoDto de(Pago p) {
            return new PagoDto(p.getId(), p.getValor(), p.getMetodo(), p.getEstado(), p.getNota(),
                    p.getRegistradoPor().getNombre(), p.getRegistradoEn(),
                    p.getRevisadoPor() == null ? null : p.getRevisadoPor().getNombre(), p.getRevisadoEn());
        }
    }

    public record VentaDto(Long id, CompradorDto comprador, Long vendedorId, String vendedor, Instant creadaEn,
                           long pagado, long porConfirmar, long saldo, List<PagoDto> pagos) {}

    public record LiberadaDto(String comprador, String celular, long abonado, Instant liberadaEn, String liberadaPor, String motivo) {}

    public record BoletaDto(int numero, String numeroTexto, EstadoBoleta estado, long precio, VentaDto venta, List<LiberadaDto> historial) {}

    public record PagoRequest(@Positive long valor, @NotNull MetodoPago metodo, @Size(max = 200) String nota) {}

    /** Apartar con un comprador ya registrado (compradorId) o con uno nuevo (nombre y celular). */
    public record ApartarRequest(
            Long compradorId,
            @Size(max = 100) String nombre,
            @Size(max = 20) String celular,
            @Size(max = 150) String ubicacion,
            @Valid PagoRequest abono) {}

    public record LiberarRequest(@Size(max = 200) String motivo) {}

    public record EditarComprador(@NotBlank @Size(max = 100) String nombre, @NotBlank @Size(max = 20) String celular, @Size(max = 150) String ubicacion) {}

    public record BoletaDeComprador(int numero, String numeroTexto, EstadoBoleta estado, long saldo) {}

    public record CompradorConBoletas(CompradorDto comprador, List<BoletaDeComprador> boletas) {}
}
