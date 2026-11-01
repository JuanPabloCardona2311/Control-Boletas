package co.elmulardeoro.boletas.usuario;

import co.elmulardeoro.boletas.comun.ApiException;
import co.elmulardeoro.boletas.usuario.UsuarioDtos.CrearUsuario;
import co.elmulardeoro.boletas.usuario.UsuarioDtos.EditarUsuario;
import co.elmulardeoro.boletas.usuario.UsuarioDtos.LoginResponse;
import co.elmulardeoro.boletas.usuario.UsuarioDtos.UsuarioDto;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final TokenService tokens;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder encoder, TokenService tokens) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.tokens = tokens;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(String usuario, String clave) {
        Usuario u = usuarios.findByUsuarioIgnoreCase(usuario.trim())
                .filter(Usuario::isActivo)
                .filter(x -> encoder.matches(clave, x.getClaveHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario o clave incorrectos"));
        return new LoginResponse(tokens.emitir(u), UsuarioDto.de(u));
    }

    @Transactional(readOnly = true)
    public List<UsuarioDto> listar() {
        return usuarios.findAllByOrderByNombreAsc().stream().map(UsuarioDto::de).toList();
    }

    @Transactional
    public UsuarioDto crear(CrearUsuario req) {
        if (usuarios.existsByUsuarioIgnoreCase(req.usuario())) {
            throw ApiException.conflicto("Ya existe el usuario " + req.usuario());
        }
        Usuario u = new Usuario(req.nombre().trim(), req.usuario().trim().toLowerCase(),
                encoder.encode(req.clave()), req.rol(), req.celular());
        return UsuarioDto.de(usuarios.save(u));
    }

    @Transactional
    public UsuarioDto editar(Long id, EditarUsuario req, Usuario actual) {
        Usuario u = buscar(id);
        if (u.getId().equals(actual.getId()) && (!req.activo() || req.rol() != Rol.ADMIN)) {
            throw ApiException.invalido("No se puede quitar a sí mismo el rol de administrador ni desactivarse");
        }
        u.setNombre(req.nombre().trim());
        u.setRol(req.rol());
        u.setCelular(req.celular());
        u.setActivo(req.activo());
        return UsuarioDto.de(u);
    }

    @Transactional
    public void cambiarClave(Long id, String clave) {
        buscar(id).setClaveHash(encoder.encode(clave));
    }

    @Transactional
    public void cambiarMiClave(Usuario actual, String claveActual, String claveNueva) {
        Usuario u = buscar(actual.getId());
        if (!encoder.matches(claveActual, u.getClaveHash())) {
            throw ApiException.invalido("La clave actual no es correcta");
        }
        u.setClaveHash(encoder.encode(claveNueva));
    }

    private Usuario buscar(Long id) {
        return usuarios.findById(id).orElseThrow(() -> ApiException.noEncontrado("Usuario no encontrado"));
    }
}
