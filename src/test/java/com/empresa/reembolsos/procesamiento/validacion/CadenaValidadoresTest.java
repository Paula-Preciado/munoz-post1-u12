package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.empresa.reembolsos.soporte.Solicitudes.valida;
import static org.junit.jupiter.api.Assertions.*;

class CadenaValidadoresTest {

    private final CadenaValidadores cadena = new CadenaValidadores(
            new ValidadorDocumentosSoporte(), new ValidadorMontoMaximo(), new ValidadorDuplicados());

    @Test
    void solicitudCorrecta_superaLosTresEslabones() {
        assertTrue(cadena.validar(valida(CategoriaGasto.CAPACITACION, "1000000"), List.of()).valida());
    }

    @Test
    void elPrimerEslabonQueFallaCortaLaCadena() {
        SolicitudReembolso sinDocsYMontoExcesivo = valida(CategoriaGasto.VIAJE, "999999");
        sinDocsYMontoExcesivo.setDocumentosSoporte(false);
        // documentos va primero: se reporta su motivo y no se llega a evaluar el monto
        assertEquals("Faltan documentos soporte",
                cadena.validar(sinDocsYMontoExcesivo, List.of()).motivoRechazo());
    }

    @Test
    void duplicadoSeDetectaAlFinalDeLaCadena() {
        SolicitudReembolso pendiente = valida(CategoriaGasto.CAPACITACION, "500000");
        assertEquals("Ya existe una solicitud pendiente de la misma categoria en el periodo",
                cadena.validar(valida(CategoriaGasto.CAPACITACION, "800000"), List.of(pendiente)).motivoRechazo());
    }
}
