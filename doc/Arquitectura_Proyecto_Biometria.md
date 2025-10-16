# Arquitectura y Funcionamiento del Proyecto

Este documento describe, de forma práctica y alineada con la rúbrica de evaluación de Buenas Prácticas, cómo está organizado el proyecto, qué archivos intervienen en cada parte (API REST, Web, Android, Arduino) y cómo fluye la información extremo a extremo.

---

## 1) Visión general (end-to-end)

Secuencia resumida del sistema:

1. Arduino emite tramas BLE (iBeacon) con los datos de una medición en los campos major/minor.
2. La app Android detecta el beacon, extrae tipo y valor de la medición y realiza una llamada HTTP al backend (API REST) para insertarla en base de datos.
3. La Web consulta Supabase (tabla `Registros`) y muestra los datos en tiempo real.

Diagrama (lógico):

```
[Arduino (iBeacon)] --BLE--> [Android Scanner] --HTTP--> [API REST / Supabase] --HTTP--> [Web Next.js]
```

---

## 2) Web y API REST

La carpeta de la Web está en:

- `src/web/`

Componentes clave:

- Cliente Supabase (uso en el navegador y en APIs):
  - `src/web/src/utils/supabaseClient.js`
    - Define `supabaseUrl` y `supabaseAnonKey` (clave pública usando `@supabase/supabase-js`).

- Rutas API en Next.js:
  - Pages Router (ruta clásica Next.js):
    - `src/web/src/pages/api/records.js`
      - GET: recupera registros desde Supabase (tabla `Registros`).
      - Estructura de respuesta esperada en el cliente: `{ data: [...] }`.
  - App Router (ruta moderna Next.js):
    - `src/web/src/app/api/routes/route.js` (si está presente en tu repo)
      - GET: proxy servidor → Supabase REST (`/rest/v1/Registros?select=*...`).
      - Requiere variables de entorno del lado servidor: `SUPABASE_URL` y `SUPABASE_SERVICE_ROLE_KEY`.
      - Respuesta: `{ data: [...] }`.

- Página principal de la web:
  - `src/web/src/app/page.js`
    - Muestra los registros en tiempo real (lee de Supabase) y maneja estados de carga/errores.

### 2.1 Contratos de la API (Web)

- `GET /api/records` (Pages Router):
  - Respuesta 200: `{ data: Registro[] }`
  - Error (fallback): `{ data: mockData[], fallback: true, supabaseError?: string }`

- `GET /api/routes` (App Router, opcional):
  - Requiere `SUPABASE_URL` y `SUPABASE_SERVICE_ROLE_KEY` (solo en servidor).
  - Respuesta 200: `{ data: Registro[] }`
  - Error: `{ error: string, details?: any }`

Nota: Mantén una sola ruta para evitar conflictos (o `pages/api/records.js` o `app/api/...`). Si conviven, deben publicar endpoints diferentes.

### 2.2 Variables de entorno

- Cliente (público, navegador): Usa `supabaseAnonKey` en `supabaseClient.js`.
- Servidor (seguras):
  - `SUPABASE_URL`
  - `SUPABASE_SERVICE_ROLE_KEY`
  - Estas NUNCA deben exponerse al cliente. Úsalas solo en el App Router o en servicios del servidor.

### 2.3 Ejecución Web

1. Instalar dependencias (en `src/web`):
   - `npm install`
2. Arrancar en desarrollo:
   - `npm run dev`
3. Visitar: `http://localhost:3000`

Consejo: Si ves "Failed to fetch" en la UI:
- Verifica conexión a Internet.
- Comprueba que `supabaseUrl` y `supabaseAnonKey` son correctos (`src/web/src/utils/supabaseClient.js`).
- Si consultas `GET /api/routes`, valida que existen `SUPABASE_URL` y `SUPABASE_SERVICE_ROLE_KEY` en el entorno de ejecución del servidor (no en el navegador).

---

## 3) Android (detección BLE + POST de mediciones)

Código Android en:

- `src/android/app/src/main/java/com/example/pruebasprint0/`

Archivos clave:

- Actividad principal (escaneo BLE, parseo iBeacon y POST a API):
  - `MainActivity.java`
    - Escanea BLE y obtiene `major/minor` de la trama iBeacon (clase `TramaIBeacon`).
    - Interpreta `tipoMedida` (IDs de ejemplo en el código: 11 → CO2, 12 → Temperatura, 13 → Ruido) y el `valorMedicion`.
    - Llama a `insertarMedicion(valor, tipo)` para enviar al backend.

- Cliente HTTP (Retrofit):
  - `API/RetrofitClient.java`
    - Configura Retrofit con `baseUrl` (actualmente apunta a Supabase host). Añade `GsonConverterFactory`.

- Interfaz de API:
  - `API/ApiService.java`
    - `@GET("/ping")`
    - `@POST("/insertar")` → `insertarMedicion(@Body Medicion)`.

- DTO de la medición enviada:
  - `POJO/Medicion.java`
    - Campos: `tipo: String`, `Valor: int`.

### 3.1 Flujo Android

1. Escaneo BLE → obtiene `tipo` y `valor`.
2. `insertarMedicion()` invoca `ApiService.insertarMedicion(new Medicion(tipo, valor))`.
3. El backend debe exponer la ruta POST `/insertar` o, alternativamente, Android debe integrarse contra Supabase REST (`/rest/v1/Registros`) con cabeceras `apikey` y `Authorization`.

Sugerencia de interoperabilidad (si usas Supabase REST):
- Añade un `OkHttp` interceptor en `RetrofitClient` para incluir `apikey`/`Authorization` y cambia el endpoint a `/rest/v1/Registros` (método `POST`) con `Content-Type: application/json`.
- Alinea los nombres de campos del DTO con las columnas reales de Supabase.

---

## 4) Arduino (emisión BLE iBeacon)

Proyecto Arduino en:

- `arduino/HolaMundoIBeacon/HolaMundoIBeacon/`
  - `HolaMundoIBeacon.ino`
  - `EmisoraBLE.h`, `ServicioEnEmisora.h`, `LED.h`, `Medidor.h`, `Publicador.h`, `PuertoSerie.h` (clases de soporte).

Descripción:
- Configura un emisor BLE modo iBeacon.
- Publica tramas con:
  - `major` (2 bytes): en el código Android, el primer byte se utiliza como ID del tipo de medida y el segundo como contador.
  - `minor` (2 bytes): valor de la medición (convertido a entero por Android).

Compilación / carga (recomendado con Arduino IDE o arduino-cli):
- Board: la que corresponda a tu dispositivo.
- Verificar que `setup()`/`loop()` publican la trama periódicamente.

---

## 5) Base de datos (Supabase)

- Proyecto Supabase apuntado por la web: `https://feghqusjsotnrxhfadrm.supabase.co`.
- Tabla utilizada en la web: `Registros`.
- Columnas:
  - `ID_medicion` (PK / autoincremental)
  - `Tipo` (o `tipo`)
  - `Valor` (o `valor`)

Si integras Android con Supabase REST directamente, asegúrate de:
- Usar el endpoint `/rest/v1/Registros`.
- Cabeceras: `apikey: <anon o service key>` y `Authorization: Bearer <key>`.
- `Prefer: return=representation` si quieres que te devuelva el registro insertado.

---

## 6) Relación con la Rúbrica

- Proyecto / Estructura: Existe `src`, `doc`, y `arduino`; se documenta arquitectura y cómo ejecutar.
- Base de Datos: El front web está alineado con la tabla `Registros` en Supabase.
- Comentarios: Clases y métodos Android incluyen comentarios Javadoc básicos; este documento cubre el diseño general.
- Lógica de Negocio: Android mapea IDs de tipo → nombres (CO2, Temperatura, Ruido) y calcula `valor` desde la trama.
- API REST: Web expone endpoints GET y, opcionalmente, un proxy server-side a Supabase; Android puede usar POST `/insertar` (si existe) o Supabase REST.
- Tests: (No incluidos ahora) puedes añadir tests de UI/web y unitarios en Android; se recomienda para completar la rúbrica.
- Arduino: Emite tramas iBeacon funcionales para el flujo.
- Android: Funciona el escaneo y la composición de la medición.

---

## 7) Cómo diferenciar Lógica REAL vs Lógica FAKE (según la rúbrica)

La rúbrica penaliza la "lógica fake" (simulada) cuando no hay una lógica de negocio coherente con los datos reales ni alineada entre el cliente y el servidor. A continuación, criterios prácticos para distinguirlas y aplicarlos a este proyecto:

### 7.1 Señales de Lógica REAL
- Los datos fluyen de extremo a extremo y son persistentes:
  - Arduino emite medidas → Android las interpreta → backend/API las inserta → la Web las visualiza desde la base de datos.
- Hay coherencia de modelos entre capas (nombres y tipos de campos):
  - Android `Medicion.java` ⇄ columnas en Supabase (por ejemplo, `Tipo`/`Valor`).
- El backend existe y responde con endpoints documentados:
  - Por ejemplo, Supabase REST `/rest/v1/Registros` o un endpoint propio `/insertar` desplegado.
- Se usan claves/entornos correctos y seguridad básica (apikey/Authorization en server-side cuando procede).
- La Web muestra datos reales de la BD (no constantes ni mocks) y refleja cambios sin tocar el código.

### 7.2 Señales de Lógica FAKE
- Los datos no salen del proceso o no llegan a BD:
  - La Web enseña listas "hardcodeadas" o mocks aunque el sistema esté en marcha.
  - El endpoint llamado por Android no existe (404) o ignora el cuerpo de la petición.
- Inconsistencia de modelos:
  - Android envía `{ tipo, Valor }` pero la BD espera `{ Tipo, Valor }` (o nombres distintos), y no hay mapeo en el backend.
- La integración depende de variables ausentes o mal ubicadas:
  - Se intenta usar `SUPABASE_SERVICE_ROLE_KEY` en el cliente (navegador) o falta en el servidor.
- Lógica duplicada o no alineada entre rutas:
  - Dos endpoints para lo mismo (`/api/records` y `/api/routes`) con comportamientos distintos y sin documentación.

### 7.3 Aplicado al repositorio
- Web (Next.js):
  - Lógica REAL: `src/web/src/pages/api/records.js` devuelve `{ data }` desde Supabase con claves públicas (anon) y la UI consume ese `data`.
  - Lógica REAL (server-side más segura): `src/web/src/app/api/routes/route.js` usa `SUPABASE_URL` + `SERVICE_ROLE_KEY` y responde `{ data }`.
  - Lógica FAKE: mostrar `mockRecords.json` como datos por defecto sin intentar la consulta real (aceptable solo como "fallback" con logging y claramente marcado `fallback: true`).
- Android:
  - Lógica REAL: `MainActivity.insertarMedicion()` llama a un endpoint operativo (Supabase REST o backend `/insertar`) y la tabla `Registros` refleja la inserción.
  - Lógica FAKE: el endpoint `/insertar` no existe y la app siempre registra 404/errores sin alternativa real, o la petición se pierde y no hay registros nuevos.
- Arduino:
  - Lógica REAL: el sketch emite iBeacon y Android extrae consistentemente `tipo` y `valor` del payload.
  - Lógica FAKE: valores ficticios impresos por serial sin emisión BLE consumida por Android.

### 7.4 Checklist rápido (para cumplir la rúbrica)
- [ ] Un único endpoint público para la Web (elige `pages/api/records` o `app/api/routes`) y documenta su contrato `{ data: Registro[] }`.
- [ ] Android inserta en la misma tabla que la Web lee (campos alineados y endpoint verificado con 200/201).
- [ ] Si hay fallback/mocks, están etiquetados (`fallback: true`) y se registran errores reales; no sustituyen permanentemente a la fuente de datos.
- [ ] Variables de entorno server-side configuradas y nunca expuestas al cliente.
- [ ] Logs claros en Android y Web para diagnosticar errores (404/401/CORS) y no dejar "lógica vacía".

---
