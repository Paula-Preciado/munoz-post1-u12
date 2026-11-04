package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;

import java.math.BigDecimal;

/**
 * Estrategia de calculo del monto aprobado para una categoria de gasto (patron Strategy, RF2).
 *
 * <p>Cada categoria tiene su propia implementacion registrada como {@code @Component};
 * {@link SelectorCalculadora} las descubre automaticamente. Agregar una categoria nueva
 * consiste en crear una clase nueva que implemente esta interfaz.</p>
 */
public interface CalculadoraReembolso {

    /** Categoria de gasto que atiende esta estrategia. */
    CategoriaGasto getCategoria();

    /** Monto aprobado para la solicitud segun la formula de la categoria. */
    BigDecimal calcular(SolicitudReembolso solicitud);
}
