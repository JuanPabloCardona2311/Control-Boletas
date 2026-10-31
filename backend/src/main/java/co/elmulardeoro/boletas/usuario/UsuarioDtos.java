package co.elmulardeoro.boletas.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UsuarioDtos {

    private UsuarioDtos() {}

    public record UsuarioDto(Long id, String nombre, String usuario, Rol rol, String celular, boolean activo) {
        public static UsuarioDto de(Usuario u) {
            return new UsuarioDto(u.getId(), u.getNombre(), u.getUsuario(), u.getRol(), u.getCelular(), u.isActivo());
        }
    }

    public record LoginRequest(@NotBlank String usuario, @NotBlank String clave) {}

    public record LoginResponse(String token, UsuarioDto usuario) {}

    public record CrearUsuario(
            @NotBlank @Size(max = 100) String nombre,
            @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "[a-zA-Z0-9._-]+", message = "solo letras, números, punto, guion") String usuario,
            @NotBlank @Size(min = 6, max = 72) String clave,
            @NotNull Rol rol,
            @Size(max = 20) String celular) {}

    public record EditarUsuario(
            @NotBlank @Size(max = 100) String nombre,
            @NotNull Rol rol,
            @Size(max = 20) String celular,
            boolean activo) {}

    public record CambiarClave(@NotBlank @Size(min = 6, max = 72) String clave) {}

    public record CambiarMiClave(@NotBlank String claveActual, @NotBlank @Size(min = 6, max = 72) String claveNueva) {}
}
