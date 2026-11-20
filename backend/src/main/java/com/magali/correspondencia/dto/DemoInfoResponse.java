package com.magali.correspondencia.dto;

import java.util.List;

/**
 * Respuesta publica de {@code GET /demo/info}. Si {@code habilitado=false},
 * {@code usuarios} queda vacio y {@code password=null}.
 */
public record DemoInfoResponse(
        boolean habilitado,
        String password,
        List<UsuarioDemoInfo> usuarios) {

    public record UsuarioDemoInfo(String username, String rol, String descripcion) {}
}
