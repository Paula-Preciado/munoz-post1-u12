package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Selecciona la estrategia de calculo segun la categoria.
 *
 * <p>Spring inyecta todas las {@link CalculadoraReembolso} registradas; el mapa se arma una
 * sola vez. Si dos calculadoras declaran la misma categoria, {@code toMap} falla al arrancar,
 * lo que detecta el error de configuracion de inmediato (ver Decision de diseno 2).</p>
 */
@Component
public class SelectorCalculadora {

    private final Map<CategoriaGasto, CalculadoraReembolso> calculadoras;

    public SelectorCalculadora(List<CalculadoraReembolso> lista) {
        this.calculadoras = lista.stream()
                .collect(Collectors.toMap(CalculadoraReembolso::getCategoria, Function.identity()));
    }

    public CalculadoraReembolso obtener(CategoriaGasto categoria) {
        return Optional.ofNullable(calculadoras.get(categoria))
                .orElseThrow(() -> new IllegalArgumentException("Categoria no soportada: " + categoria));
    }
}
