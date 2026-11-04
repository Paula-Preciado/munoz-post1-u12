package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.empresa.reembolsos.soporte.Solicitudes.valida;
import static org.junit.jupiter.api.Assertions.*;

class CalculadoraRepresentacionTest {

    private final CalculadoraRepresentacion calculadora = new CalculadoraRepresentacion();

    @Test
    void reembolsaMontoCompletoHastaElTopeMensual() {
        assertEquals(0, new BigDecimal("1200000").compareTo(
                calculadora.calcular(valida(CategoriaGasto.REPRESENTACION, "1200000"))));
        assertEquals(0, new BigDecimal("2000000").compareTo(
                calculadora.calcular(valida(CategoriaGasto.REPRESENTACION, "2500000"))));
        assertEquals(CategoriaGasto.REPRESENTACION, calculadora.getCategoria());
    }
}
