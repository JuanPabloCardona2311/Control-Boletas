package co.elmulardeoro.boletas.config;

import co.elmulardeoro.boletas.usuario.Rol;
import co.elmulardeoro.boletas.usuario.Usuario;
import co.elmulardeoro.boletas.usuario.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Crea el primer administrador con ADMIN_USUARIO y ADMIN_CLAVE cuando la base está vacía. */
@Component
public class AdminInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    public AdminInicial(UsuarioRepository usuarios, PasswordEncoder encoder, AppProperties props) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarios.count() > 0) {
            return;
        }
        AppProperties.Admin admin = props.admin();
        if (admin == null || !StringUtils.hasText(admin.usuario()) || !StringUtils.hasText(admin.clave())) {
            log.warn("No hay usuarios. Defina ADMIN_USUARIO y ADMIN_CLAVE para crear el primer administrador.");
            return;
        }
        usuarios.save(new Usuario("Administrador", admin.usuario(), encoder.encode(admin.clave()), Rol.ADMIN, null));
        log.info("Administrador inicial '{}' creado", admin.usuario());
    }
}
