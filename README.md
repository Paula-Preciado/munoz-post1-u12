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
