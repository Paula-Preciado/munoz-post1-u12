# Proyecto Integrador — Sistema de Solicitudes de Reembolso de Gastos Corporativos

| | |
|---|---|
| **Estudiante** | Paula Andrea Muñoz Preciado |
| **Código** | 02230131025 |
| **Programa** | Ingeniería de Sistemas — UDES |
| **Repositorio** | `munoz-post1-u12` |

Proyecto Maven / Spring Boot 3 (Java 17, Spring Web, Spring Data JPA, H2) ubicado en la raíz
del repositorio: `com.empresa:reembolsos-integrado`, paquete base `com.empresa.reembolsos`.

---

## Diseño Propuesto

> Esta sección se escribió **antes** de implementar cualquier patrón (primer commit del
> repositorio: `docs: analizar requisitos y proponer diseno de patrones antes de implementar`).
> Para cada requisito se identifica primero **qué varía**, y solo después qué patrón lo encapsula.

### Análisis de los requisitos

| Requisito | Lo que es estable | Lo que varía (punto de variabilidad) | Evidencia en la especificación |
|---|---|---|---|
| **RF1** Validación en varias etapas | "Una solicitud debe pasar verificaciones antes de aprobarse; si una falla, se rechaza con un motivo" | **El número y el tipo de verificaciones**, que se ejecutan en secuencia y donde cualquiera puede cortar el proceso | "planea agregar más verificaciones (límite anual acumulado) **sin modificar las existentes**" |
| **RF2** Cálculo por categoría | "A toda solicitud aceptada se le calcula un monto aprobado" | **La fórmula** de cálculo, una por categoría (tope diario × días, monto hasta tope mensual, 80 % hasta tope anual) | "agregar categorías nuevas (bienestar), cada una con su fórmula, **sin modificar el cálculo de las existentes**" |
| **RF3** Notificación simultánea | "Cuando una solicitud cambia de estado, se avisa" | **Quiénes** se enteran y **cómo reacciona** cada uno (empleado: todo; contabilidad: solo aprobadas; auditoría: todo sin excepción) | "el número de partes interesadas **puede crecer** (panel de métricas)" |

### Patrón propuesto para cada requisito

| Requisito | Patrón propuesto | Justificación (test de explicabilidad: una oración) |
|---|---|---|
| **RF1** | **Chain of Responsibility** (cadena de validadores) | Porque las verificaciones son pasos independientes que se ejecutan en orden y cualquiera puede detener el proceso con un motivo de rechazo, Chain of Responsibility permite agregar el "límite anual acumulado" como un eslabón nuevo sin tocar los validadores existentes. |
| **RF2** | **Strategy** (una calculadora por categoría, seleccionada por la categoría) | Porque lo único que cambia entre categorías es la fórmula y en cada solicitud se aplica exactamente una, Strategy encapsula cada fórmula en su propia clase y agregar "bienestar" es crear una clase nueva, no editar un `switch`. |
| **RF3** | **Observer** (observadores de cambio de estado) | Porque un mismo evento (cambio de estado) debe llegar a varios interesados que reaccionan de forma distinta y cuyo número crecerá, Observer deja que el servicio solo emita el evento mientras cada interesado decide qué hacer con él. |

### Por qué estos patrones y no otros (alternativas evaluadas)

- **RF1 — ¿por qué no Strategy o una simple `List<Validador>`?** Strategy elige *una* variante entre
  varias; aquí se aplican *todas* en secuencia, así que no encaja. Una lista recorrida con un `for`
  también funcionaría; se prefiere la cadena porque cada eslabón decide si corta o delega, y el
  **orden** queda explícito en un solo lugar (`CadenaValidadores`), lo que permite justificarlo por
  costo (Decisión de diseño 1). *Decorator* se descartó: no se agrega comportamiento a un objeto,
  se filtra una solicitud.
- **RF2 — ¿por qué no Template Method?** Las tres fórmulas no comparten un esqueleto de pasos con
  "huecos" a rellenar; cada una es una función completa distinta. Template Method obligaría a
  inventar una jerarquía de herencia sin pasos comunes reales. *State* tampoco aplica: la categoría
  no cambia durante la vida de la solicitud.
- **RF3 — ¿por qué no Mediator o un bus de eventos?** No hay interacción *entre* los interesados
  (no se hablan entre sí), así que un Mediator no coordina nada. Un bus genérico
  (`ApplicationEventPublisher`) se analiza y se descarta en la sección "Qué se descartó y por qué".

### Mapa de paquetes previsto

```text
com.empresa.reembolsos
├── dominio/                       SolicitudReembolso, CategoriaGasto, EstadoSolicitud (Java puro, sin Spring ni JPA)
├── procesamiento/
│   ├── validacion/                RF1 → Chain of Responsibility
│   ├── calculo/                   RF2 → Strategy
│   └── notificacion/              RF3 → Observer
├── aplicacion/                    SolicitudReembolsoService: único punto que coordina los tres patrones
├── infraestructura/               RepositorioSolicitudesJpa (Spring Data JPA)
└── adaptadores/rest/              SolicitudReembolsoController
```

---

## Parte 1 — Implementación

### Arquitectura resultante

```mermaid
graph TD
    REST[adaptadores.rest<br/>SolicitudReembolsoController<br/>ManejadorErrores] --> APP[aplicacion<br/>SolicitudReembolsoService]
    APP --> VAL[procesamiento.validacion<br/>CadenaValidadores → Documentos → Monto → Duplicados]
    APP --> CAL[procesamiento.calculo<br/>SelectorCalculadora → Viaje / Representacion / Capacitacion]
    APP --> NOT[procesamiento.notificacion<br/>List&lt;ObservadorCambioEstado&gt; → Empleado / Contabilidad / Auditoria]
    APP --> INF[infraestructura<br/>RepositorioSolicitudesJpa]
    VAL --> DOM[dominio<br/>SolicitudReembolso · CategoriaGasto · EstadoSolicitud]
    CAL --> DOM
    NOT --> DOM
    INF --> DOM
```

| Paquete | Responsabilidad | Depende de |
|---|---|---|
| `dominio` | Modelo puro (`SolicitudReembolso`, enums). **No importa Spring ni JPA.** | nada |
| `procesamiento.validacion` | RF1 — Chain of Responsibility | `dominio` + estereotipo `@Component` |
| `procesamiento.calculo` | RF2 — Strategy + selector por mapa | `dominio` + `@Component` |
| `procesamiento.notificacion` | RF3 — Observer | `dominio` + `@Component`, SLF4J |
| `aplicacion` | Coordina los 3 patrones y la persistencia; excepciones de aplicación | todos los anteriores + `infraestructura` |
| `infraestructura` | `RepositorioSolicitudesJpa` (Spring Data) + mapeo `META-INF/orm.xml` | `dominio`, Spring Data |
| `adaptadores.rest` | API REST y traducción de errores a HTTP 400/404 | `aplicacion`, `dominio`, Spring Web |

**Nota de implementación — dominio sin JPA:** la especificación pide que `SolicitudReembolso` no
dependa de JPA, pero `RepositorioSolicitudesJpa extends JpaRepository<SolicitudReembolso, Long>`
necesita que la clase sea una entidad. Lo resolví con un **mapeo XML** en
`src/main/resources/META-INF/orm.xml` (tabla, id autogenerado, enums como `STRING`, precisión de
montos): el dominio queda como Java puro y el detalle de persistencia vive del lado de la
infraestructura.

**Nota de implementación — periodo de duplicados:** el ejemplo del enunciado comparaba solo
`getMonth()`, así que octubre de 2025 y octubre de 2026 se tomaban como el mismo periodo. Lo cambié
por `YearMonth` (mes **y** año); `ValidadorDuplicadosTest` cubre ese caso.

### Evidencia de funcionamiento (aplicación real con `java -jar`, H2 en memoria)

**RF2 — `POST /api/solicitudes` en las 3 categorías, con monto aprobado calculado:**
VIAJE 140.000 (1 día, tope 150.000) → 140.000 · REPRESENTACIÓN 1.800.000 → 1.800.000 ·
CAPACITACIÓN 2.500.000 → 80 % = 2.000.000.

![POST tres categorias](docs/capturas/01-post-tres-categorias.png)

**RF1 — rechazos con mensaje claro (HTTP 400):** sin documentos, monto sobre el tope, y
duplicado pendiente de la misma categoría y periodo.

![POST rechazos](docs/capturas/02-post-rechazos.png)

**RF3 — `PUT /api/solicitudes/{id}/estado` dispara los tres observadores.** Contabilidad solo
reacciona a `APROBADA`; empleado y auditoría reaccionan a todo cambio:

![PUT estado y observadores](docs/capturas/03-put-estado-observadores.png)

**`ValidadorDuplicados` no importa infraestructura ni Spring Data, y `dominio` no importa Spring ni JPA:**

![grep de imports](docs/capturas/04-imports-validador-duplicados.png)

### Pruebas de la Parte 1

| Tipo | Clase | Pruebas |
|---|---|---|
| Unitaria (validador) | `ValidadorDocumentosSoporteTest` | 1 |
| Unitaria (validador) | `ValidadorMontoMaximoTest` (parametrizada, 3 categorías × límite) | 6 |
| Unitaria (validador) | `ValidadorDuplicadosTest` | 2 |
| Unitaria (cadena) | `CadenaValidadoresTest` (orden y corte de la cadena) | 3 |
| Unitaria (calculadora) | `CalculadoraViajeTest`, `CalculadoraRepresentacionTest`, `CalculadoraCapacitacionTest` | 3 |
| Unitaria (selector) | `SelectorCalculadoraTest` | 2 |
| Unitaria (observadores) | `NotificadoresTest` | 3 |
| **Integración `@SpringBootTest`** | `FlujoSolicitudReembolsoIntegracionTest` (MockMvc + H2 + `@SpyBean` de los 3 observadores) | 4 |
| | **Total** | **24** |

![mvn clean package](docs/capturas/05-mvn-test-parte1.png)

---

## Decisiones de diseño

### Decisión de diseño 1 — Orden de la cadena de validadores

**Orden elegido: `documentos → monto → duplicados`** (de más barato a más caro).

| Eslabón | Costo | Por qué en esa posición |
|---|---|---|
| 1. `ValidadorDocumentosSoporte` | O(1): leer un `boolean` | Es el más barato y el rechazo más frecuente esperable (olvidar adjuntar soportes) |
| 2. `ValidadorMontoMaximo` | O(1): buscar en un `Map` y una comparación `BigDecimal` | Barato; descarta montos imposibles antes de mirar el historial |
| 3. `ValidadorDuplicados` | O(n): recorre la lista de pendientes del empleado y construye un `YearMonth` por elemento | Es el más caro, así que va **último**: solo corre si los otros dos aprobaron |

**Qué se gana con el más caro al final:** como la cadena corta en el primer rechazo, toda
solicitud que falla por documentos o por monto nunca llega a recorrer la lista; el costo O(n) solo
se paga cuando la solicitud ya es válida en lo demás. En general, con eslabones independientes
conviene ordenar por *costo ascendente* y, a igual costo, por *probabilidad de rechazo descendente*.

**Qué se pierde:** (1) el empleado recibe **un solo motivo** a la vez: si su solicitud está
duplicada *y* no tiene soportes, primero corrige los soportes y solo al reenviar se entera del
duplicado; (2) el mensaje que ve depende del orden de la cadena, lo que se documenta en
`CadenaValidadoresTest.elPrimerEslabonQueFallaCortaLaCadena`.

**Con el orden inverso (duplicados primero)** se ganaría avisar antes del caso "ya lo enviaste",
pero se pagaría el recorrido O(n) en *todas* las solicitudes, incluso en las que se rechazarían
gratis por un booleano.

**Matiz honesto:** con el diseño actual, la consulta a BD
(`findByEmpleadoIdAndEstado`) la hace el servicio **antes** de entrar a la cadena, así que el
ahorro del orden es de CPU en memoria, no de I/O. Hacer perezosa esa consulta se evaluó y se
descartó (ver "Qué se descartó y por qué", punto 3).

### Decisión de diseño 2 — Selección de la calculadora: mapa inyectado vs. `switch`

**Elegí el mapa construido a partir de `List<CalculadoraReembolso>` que Spring inyecta.**

- **Cumple RF2 al pie de la letra:** para agregar "bienestar" se crea `CalculadoraBienestar`
  con `@Component` y se agrega la constante al enum (que es un dato del dominio, no un cálculo).
  **No se modifica ninguna línea** de `SelectorCalculadora`, del servicio ni de las otras tres
  calculadoras: Spring la descubre y `toMap` la registra al arrancar.
- Con un `switch` manual sobre `CategoriaGasto`, cada categoría nueva obliga a **editar el
  selector** (viola OCP), y si se olvida, el error aparece en ejecución.
- **Falla rápido ante errores de configuración:** si dos calculadoras declaran la misma
  categoría, `Collectors.toMap` lanza `IllegalStateException` al arrancar el contexto. Si falta
  una, `obtener()` responde con un mensaje explícito (`SelectorCalculadoraTest`).
- Costo asumido: el vínculo categoría→clase ya no se ve en un solo `switch`; hay que mirar el
  `getCategoria()` de cada estrategia. Lo acepto porque es justo la información que cada
  estrategia debe conocer de sí misma.

### Observación para Finanzas (hallazgo durante la implementación)

`ValidadorMontoMaximo` usa para VIAJE un tope **por solicitud** de 150.000, y `CalculadoraViaje`
usa 150.000 como tope **diario** multiplicado por los días. Así, un viaje de 3 días por 400.000
(dentro del tope de cálculo, 450.000) se **rechaza** en la validación, y el `min()` de la
calculadora nunca llega a recortar nada. Implementé las reglas tal como están en la
especificación, sin cambiarlas por mi cuenta, y queda como pregunta para Finanzas: ¿el tope de
validación de viajes debería ser `150.000 × díasViaje`? Si la respuesta es sí, el cambio queda
dentro de `ValidadorMontoMaximo` sin tocar ningún otro eslabón ni estrategia, y eso confirma que
la separación por patrones funciona.
