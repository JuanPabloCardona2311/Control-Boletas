package co.elmulardeoro.boletas.config;

import co.elmulardeoro.boletas.usuario.Usuario;
import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/** Autenticación cuyo principal es el {@link Usuario}, para recibirlo con @AuthenticationPrincipal. */
public class SesionToken extends AbstractAuthenticationToken {

    private final Usuario usuario;
    private final Jwt jwt;

    public SesionToken(Usuario usuario, Jwt jwt, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.usuario = usuario;
        this.jwt = jwt;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return jwt;
    }

    @Override
    public Usuario getPrincipal() {
        return usuario;
    }

    @Override
    public String getName() {
        return usuario.getUsuario();
    }
}
