package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.empresa.reembolsos.soporte.Solicitudes.valida;
import static org.junit.jupiter.api.Assertions.*;

class CalculadoraCapacitacionTest {

    private final CalculadoraCapacitacion calculadora = new CalculadoraCapacitacion();

    @Test
    void reembolsaElOchentaPorCientoHastaElTopeAnual() {
        // 80% de 2.000.000 = 1.600.000
        assertEquals(0, new BigDecimal("1600000").compareTo(
                calculadora.calcular(valida(CategoriaGasto.CAPACITACION, "2000000"))));
        // 80% de 5.000.000 = 4.000.000 -> se recorta al tope anual de 3.000.000
        assertEquals(0, new BigDecimal("3000000").compareTo(
                calculadora.calcular(valida(CategoriaGasto.CAPACITACION, "5000000"))));
        assertEquals(CategoriaGasto.CAPACITACION, calculadora.getCategoria());
    }
}
