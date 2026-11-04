package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.empresa.reembolsos.soporte.Solicitudes.solicitud;
import static org.junit.jupiter.api.Assertions.*;

class ValidadorDuplicadosTest {

    private final ValidadorDuplicados validador = new ValidadorDuplicados();
    private final SolicitudReembolso nueva =
            solicitud("E-001", CategoriaGasto.REPRESENTACION, "500000", LocalDate.of(2026, 10, 20), 0, true);

    @Test
    void mismaCategoriaYMismoPeriodo_rechaza() {
        var pendiente = solicitud("E-001", CategoriaGasto.REPRESENTACION, "300000", LocalDate.of(2026, 10, 2), 0, true);
        ResultadoValidacion resultado = validador.validar(nueva, List.of(pendiente));
        assertFalse(resultado.valida());
        assertEquals("Ya existe una solicitud pendiente de la misma categoria en el periodo", resultado.motivoRechazo());
    }

    @Test
    void otraCategoria_otroMes_uOtroAnio_noEsDuplicado() {
        var otraCategoria = solicitud("E-001", CategoriaGasto.CAPACITACION, "300000", LocalDate.of(2026, 10, 2), 0, true);
        var otroMes = solicitud("E-001", CategoriaGasto.REPRESENTACION, "300000", LocalDate.of(2026, 9, 30), 0, true);
        var mismoMesOtroAnio = solicitud("E-001", CategoriaGasto.REPRESENTACION, "300000", LocalDate.of(2025, 10, 20), 0, true);
        assertTrue(validador.validar(nueva, List.of(otraCategoria, otroMes, mismoMesOtroAnio)).valida());
    }
}
