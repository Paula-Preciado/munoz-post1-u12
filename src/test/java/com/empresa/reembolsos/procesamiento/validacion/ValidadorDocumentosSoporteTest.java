package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.empresa.reembolsos.soporte.Solicitudes.valida;
import static org.junit.jupiter.api.Assertions.*;

class ValidadorDocumentosSoporteTest {

    private final ValidadorDocumentosSoporte validador = new ValidadorDocumentosSoporte();

    @Test
    void conDocumentos_apruebaYSinDocumentos_rechazaConMotivo() {
        SolicitudReembolso con = valida(CategoriaGasto.VIAJE, "100000");
        SolicitudReembolso sin = valida(CategoriaGasto.VIAJE, "100000");
        sin.setDocumentosSoporte(false);

        assertTrue(validador.validar(con, List.of()).valida());
        ResultadoValidacion resultado = validador.validar(sin, List.of());
        assertFalse(resultado.valida());
        assertEquals("Faltan documentos soporte", resultado.motivoRechazo());
    }
}
