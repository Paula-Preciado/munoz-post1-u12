package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static com.empresa.reembolsos.soporte.Solicitudes.valida;
import static org.junit.jupiter.api.Assertions.*;

class ValidadorMontoMaximoTest {

    private final ValidadorMontoMaximo validador = new ValidadorMontoMaximo();

    @ParameterizedTest(name = "{0}: monto {1} -> valida={2}")
    @CsvSource({
            "VIAJE,          150000,  true",
            "VIAJE,          150001,  false",
            "REPRESENTACION, 2000000, true",
            "REPRESENTACION, 2000001, false",
            "CAPACITACION,   3000000, true",
            "CAPACITACION,   3000001, false"
    })
    void respetaElTopeDeCadaCategoria(CategoriaGasto categoria, String monto, boolean esperado) {
        ResultadoValidacion resultado = validador.validar(valida(categoria, monto), List.of());
        assertEquals(esperado, resultado.valida());
        if (!esperado) {
            assertEquals("Monto excede el tope de la categoria " + categoria, resultado.motivoRechazo());
        }
    }
}
