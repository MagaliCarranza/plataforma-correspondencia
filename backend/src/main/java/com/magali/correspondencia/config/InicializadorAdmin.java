package com.magali.correspondencia.config;

import com.magali.correspondencia.model.Area;
import com.magali.correspondencia.model.Rol;
import com.magali.correspondencia.model.Usuario;
import com.magali.correspondencia.repository.AreaRepository;
import com.magali.correspondencia.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(InicializadorAdmin.ORDEN)
public class InicializadorAdmin implements CommandLineRunner {

    public static final int ORDEN = 0;

    private static final Logger log = LoggerFactory.getLogger(InicializadorAdmin.class);

    private static final String ADMIN_USERNAME_DEV = "admin";
    private static final String ADMIN_PASSWORD_DEV = "admin1234";
    private static final String ADMIN_EMAIL = "admin@correspondencia.local";
    private static final String AREA_SISTEMAS = "Sistemas";
    private static final String[] AREAS_BASE = {"Sistemas", "Recursos Humanos", "Direccion"};
    private static final int LONGITUD_MINIMA_PASSWORD_PROD = 12;

    private final AreaRepository areaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;
    private final String adminInicialUsername;
    private final String adminInicialPassword;

    public InicializadorAdmin(AreaRepository areaRepository,
                              UsuarioRepository usuarioRepository,
                              PasswordEncoder passwordEncoder,
                              Environment environment,
                              @Value("${correspondencia.admin-inicial.username:}") String adminInicialUsername,
                              @Value("${correspondencia.admin-inicial.password:}") String adminInicialPassword) {
        this.areaRepository = areaRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
        this.adminInicialUsername = adminInicialUsername;
        this.adminInicialPassword = adminInicialPassword;
    }

    @Override
    public void run(String... args) {
        sembrarAreasBase();

        // Solo un ADMIN real (es_demo = false) cuenta. Si existen cuentas demo con rol ADMIN
        // (p. ej. demo.admin tras el primer seed), eso no bloquea la creacion del admin real.
        if (usuarioRepository.existsByRolAndEsDemoFalse(Rol.ADMIN)) {
            return;
        }

        boolean esProd = environment.acceptsProfiles(Profiles.of("prod"));
        if (esProd) {
            crearAdminProd();
        } else {
            crearAdminDev();
        }
    }

    private void sembrarAreasBase() {
        for (String nombre : AREAS_BASE) {
            areaRepository.findByNombreIgnoreCase(nombre)
                    .orElseGet(() -> areaRepository.save(
                            Area.builder().nombre(nombre).activo(true).build()));
        }
    }

    private void crearAdminDev() {
        Area sistemas = areaRepository.findByNombreIgnoreCase(AREA_SISTEMAS)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontro el area base '" + AREA_SISTEMAS + "' al sembrar el admin dev"));
        Usuario admin = Usuario.builder()
                .nombreCompleto("Administrador del sistema")
                .email(ADMIN_EMAIL)
                .username(ADMIN_USERNAME_DEV)
                .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD_DEV))
                .rol(Rol.ADMIN)
                .area(sistemas)
                .debeCambiarPassword(false)
                .esDemo(false)
                .build();
        usuarioRepository.save(admin);
        log.info("Admin dev creado - username='{}' password='{}' (solo dev)",
                ADMIN_USERNAME_DEV, ADMIN_PASSWORD_DEV);
    }

    private void crearAdminProd() {
        if (adminInicialUsername.isBlank() || adminInicialPassword.isBlank()) {
            log.warn("No existe usuario ADMIN real y las variables ADMIN_INICIAL_USERNAME / ADMIN_INICIAL_PASSWORD "
                    + "no estan definidas. Configuralas o crea el admin manualmente en la base de datos.");
            return;
        }
        if (adminInicialUsername.length() > 12) {
            log.warn("ADMIN_INICIAL_USERNAME tiene mas de 12 caracteres; no se crea el admin inicial.");
            return;
        }
        if (adminInicialPassword.length() < LONGITUD_MINIMA_PASSWORD_PROD) {
            log.warn("ADMIN_INICIAL_PASSWORD debe tener al menos {} caracteres; no se crea el admin inicial.",
                    LONGITUD_MINIMA_PASSWORD_PROD);
            return;
        }
        Area sistemas = areaRepository.findByNombreIgnoreCase(AREA_SISTEMAS)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontro el area base '" + AREA_SISTEMAS + "' al sembrar el admin inicial"));
        Usuario admin = Usuario.builder()
                .nombreCompleto("Administrador del sistema")
                .email(ADMIN_EMAIL)
                .username(adminInicialUsername)
                .passwordHash(passwordEncoder.encode(adminInicialPassword))
                .rol(Rol.ADMIN)
                .area(sistemas)
                .debeCambiarPassword(true)
                .esDemo(false)
                .build();
        usuarioRepository.save(admin);
        log.info("Admin inicial de produccion creado con username='{}' (debe cambiar la contrasena al primer login)",
                adminInicialUsername);
    }
}
