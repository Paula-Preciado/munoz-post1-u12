package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.empresa.reembolsos.soporte.Solicitudes.solicitud;
import static org.junit.jupiter.api.Assertions.*;

class CalculadoraViajeTest {

    private final CalculadoraViaje calculadora = new CalculadoraViaje();
    private final LocalDate fecha = LocalDate.of(2026, 10, 15);

    @Test
    void reembolsaElMenorEntreLoSolicitadoYTopeDiarioPorDias() {
        // 3 dias -> tope 450.000; solicitado 400.000 -> se reembolsa completo
        assertEquals(0, new BigDecimal("400000").compareTo(
                calculadora.calcular(solicitud("E-1", CategoriaGasto.VIAJE, "400000", fecha, 3, true))));
        // 1 dia -> tope 150.000; solicitado 140.000 -> completo
        assertEquals(0, new BigDecimal("140000").compareTo(
                calculadora.calcular(solicitud("E-1", CategoriaGasto.VIAJE, "140000", fecha, 1, true))));
        // 2 dias -> tope 300.000; solicitado 350.000 -> se recorta al tope
        assertEquals(0, new BigDecimal("300000").compareTo(
                calculadora.calcular(solicitud("E-1", CategoriaGasto.VIAJE, "350000", fecha, 2, true))));
        assertEquals(CategoriaGasto.VIAJE, calculadora.getCategoria());
    }
}
