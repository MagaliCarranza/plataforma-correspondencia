package com.magali.correspondencia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametros del modo demo. Todos tienen default razonable para desarrollo;
 * en produccion se validan por DemoConfigValidator al construir el contexto.
 */
@ConfigurationProperties(prefix = "correspondencia.demo")
public record DemoProperties(
        boolean habilitado,
        String password,
        String cron,
        String zona) {

    public static final String PASSWORD_DEFAULT_DEV = "demo1234";

    public DemoProperties {
        if (password == null || password.isBlank()) {
            password = PASSWORD_DEFAULT_DEV;
        }
        if (cron == null || cron.isBlank()) {
            cron = "0 0 3 * * *";
        }
        if (zona == null || zona.isBlank()) {
            zona = "America/Mexico_City";
        }
    }
}
