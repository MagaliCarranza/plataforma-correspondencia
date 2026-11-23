package com.magali.correspondencia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.magali.correspondencia.dto.LoginRequest;
import com.magali.correspondencia.exception.CredencialesInvalidasException;
import com.magali.correspondencia.model.Area;
import com.magali.correspondencia.model.Rol;
import com.magali.correspondencia.model.Usuario;
import com.magali.correspondencia.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AuthServiceDemoTest {

    private static final int MAX_INTENTOS = 5;

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtEncoder jwtEncoder;

    private AuthService authService;
    private Area area;

    @BeforeEach
    void setUp() {
        area = Area.builder().id(1L).nombre("Sistemas").activo(true).build();
        authService = new AuthService(usuarioRepository, passwordEncoder, jwtEncoder, MAX_INTENTOS, 120L);
    }

    @Test
    void cuentaDemoNoAcumulaIntentosFallidosNiSeGuarda() {
        Usuario demoAdmin = usuario("demo.admin", true, 0);
        given(usuarioRepository.findByUsername("demo.admin")).willReturn(Optional.of(demoAdmin));
        given(passwordEncoder.matches("mal", "hash")).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("demo.admin", "mal")))
                .isInstanceOf(CredencialesInvalidasException.class);

        assertThat(demoAdmin.getIntentosFallidos()).isZero();
        assertThat(demoAdmin.isBloqueada()).isFalse();
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void cuentaDemoNoSeBloqueaAunqueTengaIntentosAcumuladosPrevios() {
        Usuario demoAdmin = usuario("demo.admin", true, MAX_INTENTOS - 1);
        given(usuarioRepository.findByUsername("demo.admin")).willReturn(Optional.of(demoAdmin));
        given(passwordEncoder.matches("mal", "hash")).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("demo.admin", "mal")))
                .isInstanceOf(CredencialesInvalidasException.class);

        assertThat(demoAdmin.getIntentosFallidos()).isEqualTo(MAX_INTENTOS - 1);
        assertThat(demoAdmin.isBloqueada()).isFalse();
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void cuentaNormalSiAcumulaIntentosFallidos() {
        Usuario normal = usuario("recep", false, 0);
        given(usuarioRepository.findByUsername("recep")).willReturn(Optional.of(normal));
        given(passwordEncoder.matches("mal", "hash")).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("recep", "mal")))
                .isInstanceOf(CredencialesInvalidasException.class);

        assertThat(normal.getIntentosFallidos()).isEqualTo(1);
        assertThat(normal.isBloqueada()).isFalse();
        verify(usuarioRepository).save(normal);
    }

    private Usuario usuario(String username, boolean esDemo, int intentosFallidos) {
        return Usuario.builder()
                .id(1L)
                .username(username)
                .nombreCompleto("Nombre " + username)
                .email(username + "@local")
                .passwordHash("hash")
                .rol(Rol.ADMIN)
                .area(area)
                .esDemo(esDemo)
                .intentosFallidos(intentosFallidos)
                .bloqueada(false)
                .debeCambiarPassword(false)
                .build();
    }
}
