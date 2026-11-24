package com.magali.correspondencia.config;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Valida la configuracion del modo demo en el momento de construccion del contexto,
 * antes de aceptar trafico. Si el modo demo esta habilitado en produccion se exige
 * una contrasena explicita distinta del default de desarrollo; si falta, la app no
 * arranca.
 */
@Component
public class DemoConfigValidator {

    private final DemoProperties demo;
    private final Environment environment;

    public DemoConfigValidator(DemoProperties demo, Environment environment) {
        this.demo = demo;
        this.environment = environment;
    }

    @PostConstruct
    void validar() {
        if (!demo.habilitado()) {
            return;
        }
        boolean esProd = environment.acceptsProfiles(Profiles.of("prod"));
        if (!esProd) {
            return;
        }
        if (DemoProperties.PASSWORD_DEFAULT_DEV.equals(demo.password())) {
            throw new IllegalStateException(
                    "El modo demo esta habilitado en produccion pero CORRESPONDENCIA_DEMO_PASSWORD no esta "
                            + "configurada (o conserva el valor por defecto de desarrollo). Define una "
                            + "contrasena especifica para el entorno de demo antes de arrancar.");
        }
    }
}
