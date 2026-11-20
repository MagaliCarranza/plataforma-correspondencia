package com.magali.correspondencia.controller;

import com.magali.correspondencia.config.DemoProperties;
import com.magali.correspondencia.config.DemoSeeder;
import com.magali.correspondencia.dto.DemoInfoResponse;
import com.magali.correspondencia.dto.DemoInfoResponse.UsuarioDemoInfo;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints del modo demo. {@code GET /demo/info} es publico (se consulta desde
 * la pantalla de login para mostrar los botones "Entrar como ...").
 */
@RestController
@RequestMapping("/demo")
public class DemoController {

    private static final List<UsuarioDemoInfo> USUARIOS_DEMO = List.of(
            new UsuarioDemoInfo(DemoSeeder.USERNAME_ADMIN_DEMO, "ADMIN",
                    "Gestiona usuarios, areas y toda la correspondencia."),
            new UsuarioDemoInfo(DemoSeeder.USERNAME_RECEP_DEMO, "RECEPCIONISTA",
                    "Registra correspondencia entrante y asigna mensajeros."),
            new UsuarioDemoInfo(DemoSeeder.USERNAME_SUPER_DEMO, "SUPERVISOR",
                    "Consulta, asigna mensajeros, archiva y genera reportes PDF."),
            new UsuarioDemoInfo(DemoSeeder.USERNAME_MENSA_1_DEMO, "MENSAJERO",
                    "Registra la entrega de la correspondencia asignada (mensajero 1)."),
            new UsuarioDemoInfo(DemoSeeder.USERNAME_MENSA_2_DEMO, "MENSAJERO",
                    "Registra la entrega de la correspondencia asignada (mensajero 2).")
    );

    private final DemoProperties demo;

    public DemoController(DemoProperties demo) {
        this.demo = demo;
    }

    @GetMapping("/info")
    public DemoInfoResponse info() {
        if (!demo.habilitado()) {
            return new DemoInfoResponse(false, null, List.of());
        }
        return new DemoInfoResponse(true, demo.password(), USUARIOS_DEMO);
    }
}
