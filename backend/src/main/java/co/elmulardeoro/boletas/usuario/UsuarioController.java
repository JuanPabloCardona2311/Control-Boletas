package co.elmulardeoro.boletas.usuario;

import co.elmulardeoro.boletas.usuario.UsuarioDtos.CambiarClave;
import co.elmulardeoro.boletas.usuario.UsuarioDtos.CambiarMiClave;
import co.elmulardeoro.boletas.usuario.UsuarioDtos.CrearUsuario;
import co.elmulardeoro.boletas.usuario.UsuarioDtos.EditarUsuario;
import co.elmulardeoro.boletas.usuario.UsuarioDtos.LoginRequest;
import co.elmulardeoro.boletas.usuario.UsuarioDtos.LoginResponse;
import co.elmulardeoro.boletas.usuario.UsuarioDtos.UsuarioDto;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsuarioController {

    private final UsuarioService servicio;

    public UsuarioController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/api/salud")
    Map<String, String> salud() {
        return Map.of("estado", "ok");
    }

    @PostMapping("/api/auth/login")
    LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return servicio.login(req.usuario(), req.clave());
    }

    @GetMapping("/api/auth/yo")
    UsuarioDto yo(@AuthenticationPrincipal Usuario actual) {
        return UsuarioDto.de(actual);
    }

    @PutMapping("/api/auth/yo/clave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cambiarMiClave(@AuthenticationPrincipal Usuario actual, @Valid @RequestBody CambiarMiClave req) {
        servicio.cambiarMiClave(actual, req.claveActual(), req.claveNueva());
    }

    @GetMapping("/api/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    List<UsuarioDto> listar() {
        return servicio.listar();
    }

    @PostMapping("/api/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    UsuarioDto crear(@Valid @RequestBody CrearUsuario req) {
        return servicio.crear(req);
    }

    @PutMapping("/api/usuarios/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    UsuarioDto editar(@PathVariable Long id, @Valid @RequestBody EditarUsuario req, @AuthenticationPrincipal Usuario actual) {
        return servicio.editar(id, req, actual);
    }

    @PutMapping("/api/usuarios/{id}/clave")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cambiarClave(@PathVariable Long id, @Valid @RequestBody CambiarClave req) {
        servicio.cambiarClave(id, req.clave());
    }
}
