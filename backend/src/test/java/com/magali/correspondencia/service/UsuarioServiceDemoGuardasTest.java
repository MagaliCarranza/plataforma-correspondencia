package com.magali.correspondencia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.magali.correspondencia.dto.ActualizarUsuarioRequest;
import com.magali.correspondencia.dto.CambiarPasswordRequest;
import com.magali.correspondencia.dto.CrearUsuarioRequest;
import com.magali.correspondencia.dto.CrearUsuarioResponse;
import com.magali.correspondencia.exception.ReglaNegocioException;
import com.magali.correspondencia.model.Area;
import com.magali.correspondencia.model.Rol;
import com.magali.correspondencia.model.Usuario;
import com.magali.correspondencia.repository.AreaRepository;
import com.magali.correspondencia.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceDemoGuardasTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AreaRepository areaRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UsuarioService usuarioService;

    private Area area;
    private Usuario demoAdmin;
    private Usuario demoRecep;
    private Usuario adminReal;
    private Usuario recepReal;

    @BeforeEach
    void setUp() {
        area = Area.builder().id(1L).nombre("Sistemas").activo(true).build();
        demoAdmin = usuario(10L, "demo.admin", Rol.ADMIN, true);
        demoRecep = usuario(11L, "demo.recep", Rol.RECEPCIONISTA, true);
        adminReal = usuario(20L, "admin", Rol.ADMIN, false);
        recepReal = usuario(21L, "recep", Rol.RECEPCIONISTA, false);
    }

    @Test
    void demoNoPuedeCrearUsuariosAdmin() {
        given(usuarioRepository.findByUsername("demo.admin")).willReturn(Optional.of(demoAdmin));
        CrearUsuarioRequest request = new CrearUsuarioRequest(
                "Nuevo Admin", "nuevo@x.com", "nuevoadm", 1L, Rol.ADMIN);

        assertThatThrownBy(() -> usuarioService.crear(request, "demo.admin"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("ADMIN");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void demoPuedeCrearUsuariosNoAdminYNuncaQuedanComoDemo() {
        given(usuarioRepository.findByUsername("demo.admin")).willReturn(Optional.of(demoAdmin));
        given(usuarioRepository.existsByUsername("nuevorec")).willReturn(false);
        given(usuarioRepository.existsByEmail("nuevo@x.com")).willReturn(false);
        given(areaRepository.findById(1L)).willReturn(Optional.of(area));
        given(passwordEncoder.encode(any(String.class))).willReturn("hashed");
        given(usuarioRepository.save(any(Usuario.class))).willAnswer(inv -> inv.getArgument(0));

        CrearUsuarioRequest request = new CrearUsuarioRequest(
                "Nuevo Recep", "nuevo@x.com", "nuevorec", 1L, Rol.RECEPCIONISTA);

        CrearUsuarioResponse response = usuarioService.crear(request, "demo.admin");

        assertThat(response.usuario().esDemo()).isFalse();
    }

    @Test
    void demoNoPuedeActualizarUsuariosAdmin() {
        given(usuarioRepository.findByUsername("demo.admin")).willReturn(Optional.of(demoAdmin));
        given(usuarioRepository.findById(adminReal.getId())).willReturn(Optional.of(adminReal));

        ActualizarUsuarioRequest request = new ActualizarUsuarioRequest(
                "Nuevo Nombre", "admin@x.com", 1L, Rol.ADMIN);

        assertThatThrownBy(() -> usuarioService.actualizar(adminReal.getId(), request, "demo.admin"))
                .isInstanceOf(ReglaNegocioException.class);

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void demoNoPuedeActualizarOtrasCuentasDemo() {
        given(usuarioRepository.findByUsername("demo.admin")).willReturn(Optional.of(demoAdmin));
        given(usuarioRepository.findById(demoRecep.getId())).willReturn(Optional.of(demoRecep));

        ActualizarUsuarioRequest request = new ActualizarUsuarioRequest(
                "Nuevo Nombre", "demo.recep@x.com", 1L, Rol.RECEPCIONISTA);

        assertThatThrownBy(() -> usuarioService.actualizar(demoRecep.getId(), request, "demo.admin"))
                .isInstanceOf(ReglaNegocioException.class);

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void demoNoPuedeEscalarUsuariosNoAdminARolAdmin() {
        given(usuarioRepository.findByUsername("demo.admin")).willReturn(Optional.of(demoAdmin));
        given(usuarioRepository.findById(recepReal.getId())).willReturn(Optional.of(recepReal));

        ActualizarUsuarioRequest request = new ActualizarUsuarioRequest(
                "Recep Real", "recep@x.com", 1L, Rol.ADMIN);

        assertThatThrownBy(() -> usuarioService.actualizar(recepReal.getId(), request, "demo.admin"))
                .isInstanceOf(ReglaNegocioException.class);

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void demoNoPuedeResetearPasswordDeAdmin() {
        given(usuarioRepository.findByUsername("demo.admin")).willReturn(Optional.of(demoAdmin));
        given(usuarioRepository.findById(adminReal.getId())).willReturn(Optional.of(adminReal));

        assertThatThrownBy(() -> usuarioService.resetearPassword(adminReal.getId(), "demo.admin"))
                .isInstanceOf(ReglaNegocioException.class);

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void demoNoPuedeResetearPasswordDeOtroDemo() {
        given(usuarioRepository.findByUsername("demo.admin")).willReturn(Optional.of(demoAdmin));
        given(usuarioRepository.findById(demoRecep.getId())).willReturn(Optional.of(demoRecep));

        assertThatThrownBy(() -> usuarioService.resetearPassword(demoRecep.getId(), "demo.admin"))
                .isInstanceOf(ReglaNegocioException.class);

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void demoNoPuedeDesbloquearAdmin() {
        given(usuarioRepository.findByUsername("demo.admin")).willReturn(Optional.of(demoAdmin));
        given(usuarioRepository.findById(adminReal.getId())).willReturn(Optional.of(adminReal));

        assertThatThrownBy(() -> usuarioService.desbloquear(adminReal.getId(), "demo.admin"))
                .isInstanceOf(ReglaNegocioException.class);

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void demoNoPuedeCambiarSuPropiaPassword() {
        given(usuarioRepository.findByUsername("demo.admin")).willReturn(Optional.of(demoAdmin));

        CambiarPasswordRequest request = new CambiarPasswordRequest("demo1234", "Otra-123#");

        assertThatThrownBy(() -> usuarioService.cambiarPassword("demo.admin", request))
                .isInstanceOf(ReglaNegocioException.class);

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void adminRealPuedeCrearUsuariosAdmin() {
        given(usuarioRepository.findByUsername("admin")).willReturn(Optional.of(adminReal));
        given(usuarioRepository.existsByUsername("nuevoadm")).willReturn(false);
        given(usuarioRepository.existsByEmail("nuevo@x.com")).willReturn(false);
        given(areaRepository.findById(1L)).willReturn(Optional.of(area));
        given(passwordEncoder.encode(any(String.class))).willReturn("hashed");
        given(usuarioRepository.save(any(Usuario.class))).willAnswer(inv -> inv.getArgument(0));

        CrearUsuarioRequest request = new CrearUsuarioRequest(
                "Nuevo Admin", "nuevo@x.com", "nuevoadm", 1L, Rol.ADMIN);

        CrearUsuarioResponse response = usuarioService.crear(request, "admin");

        assertThat(response.usuario().rol()).isEqualTo(Rol.ADMIN.name());
        assertThat(response.usuario().esDemo()).isFalse();
    }

    private Usuario usuario(long id, String username, Rol rol, boolean esDemo) {
        return Usuario.builder()
                .id(id)
                .username(username)
                .nombreCompleto("Nombre " + username)
                .email(username + "@local")
                .passwordHash("hash")
                .rol(rol)
                .area(area)
                .esDemo(esDemo)
                .debeCambiarPassword(false)
                .intentosFallidos(0)
                .bloqueada(false)
                .build();
    }
}
