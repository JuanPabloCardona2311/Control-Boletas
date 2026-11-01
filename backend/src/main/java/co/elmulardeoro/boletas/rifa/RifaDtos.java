package co.elmulardeoro.boletas.rifa;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public final class RifaDtos {

    private RifaDtos() {}

    public record PremioDto(
            Long id,
            @NotBlank @Size(max = 200) String descripcion,
            @NotNull PosicionPremio posicion,
            @Min(1) @Max(6) int cifras,
            @Positive Long valorEfectivo) {

        static PremioDto de(Premio p) {
            return new PremioDto(p.getId(), p.getDescripcion(), p.getPosicion(), p.getCifras(), p.getValorEfectivo());
        }
    }

    public record RifaDto(
            Long id,
            String nombre,
            int cantidadNumeros,
            int cifras,
            long precio,
            String loteria,
            LocalDate fechaSorteo,
            int diasRecordatorio,
            int diasLiberacion,
            LocalDate fechaRecordatorio,
            LocalDate fechaLiberacion,
            String responsables,
            boolean activa,
            String resultado,
            List<PremioDto> premios) {

        public static RifaDto de(Rifa r) {
            return new RifaDto(r.getId(), r.getNombre(), r.getCantidadNumeros(), r.cifras(), r.getPrecio(),
                    r.getLoteria(), r.getFechaSorteo(), r.getDiasRecordatorio(), r.getDiasLiberacion(),
                    r.fechaRecordatorio(), r.fechaLiberacion(), r.getResponsables(), r.isActiva(), r.getResultado(),
                    r.getPremios().stream().map(PremioDto::de).toList());
        }
    }

    /** Datos de una rifa nueva. La cantidad de números no se puede cambiar después de crearla. */
    public record CrearRifa(
            @NotBlank @Size(max = 150) String nombre,
            @NotNull Integer cantidadNumeros,
            @Positive long precio,
            @NotBlank @Size(max = 100) String loteria,
            @NotNull LocalDate fechaSorteo,
            @Min(0) @Max(90) int diasRecordatorio,
            @Min(0) @Max(90) int diasLiberacion,
            @Size(max = 300) String responsables,
            @NotEmpty List<@Valid PremioDto> premios) {}

    public record EditarRifa(
            @NotBlank @Size(max = 150) String nombre,
            @Positive long precio,
            @NotBlank @Size(max = 100) String loteria,
            @NotNull LocalDate fechaSorteo,
            @Min(0) @Max(90) int diasRecordatorio,
            @Min(0) @Max(90) int diasLiberacion,
            @Size(max = 300) String responsables,
            @NotEmpty List<@Valid PremioDto> premios) {}

    public record Resultado(@NotBlank @Pattern(regexp = "\\d{3,6}", message = "debe ser el número de la lotería, solo cifras") String resultado) {}
}
