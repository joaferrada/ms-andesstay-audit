# ms-andesstay-audit

Timeline de auditoria de las reservas de AndesStay (puerto **8086**): quien creo, confirmo, hizo check-in o check-out de una reserva y cuando. Spring Boot 4.1.1 · Java 17 · Resource Server JWT (Microsoft Entra ID) · Oracle Autonomous Database.

```
reservations :8081 --POST /api/audit/events (JWT del usuario)--> audit :8086
Frontend --> API Gateway --> BFF :8080 --GET--> audit :8086      (el POST no se expone en el BFF)
report :8087 --GET timeline (JWT propagado)--> audit :8086
```

## Ejecutar

```powershell
$env:DB_URL="jdbc:oracle:thin:@andesstaydb_tp?TNS_ADMIN=C:/oracle/wallet_andesstay"
$env:DB_USERNAME="ANDESSTAY_AUDIT"
$env:DB_PASSWORD="<password de ANDESSTAY_AUDIT>"
.\mvnw.cmd spring-boot:run
```

| Variable | Default | Descripcion |
|---|---|---|
| `DB_URL` | `jdbc:oracle:thin:@andesstaydb_tp?TNS_ADMIN=C:/oracle/wallet_andesstay` | Alias TNS + carpeta del wallet |
| `DB_USERNAME` | `ANDESSTAY_AUDIT` | Usuario (schema) de auditoria |
| `DB_PASSWORD` | (sin default) | Obligatoria; nunca se versiona |

Ver `.env.example`. El wallet, los `.zip` y el `.env` estan en `.gitignore`.

## Base de datos

Misma BD Oracle ADB `andesstaydb` que reservations (ver su README para el wallet), con un schema propio. Como `ADMIN`:

```sql
CREATE USER ANDESSTAY_AUDIT IDENTIFIED BY "<password segura>";
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE TO ANDESSTAY_AUDIT;
ALTER USER ANDESSTAY_AUDIT QUOTA UNLIMITED ON DATA;
```

Hibernate crea la tabla `AUDIT_EVENTS` con indices por `reservationId` y `occurredAt` (`ddl-auto: update`).

## Endpoints

Todos requieren `Authorization: Bearer <JWT de Entra ID>`. No hay PUT ni DELETE: los eventos solo se insertan.

| Metodo | Ruta | Descripcion | Codigos |
|---|---|---|---|
| POST | `/api/audit/events` | Registra un evento | 201, 400 |
| GET | `/api/audit/events` | Busqueda paginada, `occurredAt` desc | 200, 400 |
| GET | `/api/audit/reservations/{id}/timeline` | Eventos de una reserva en orden cronologico (lista vacia si no hay) | 200 |

**POST** (`dto/AuditEventRequest`):

| Campo | Regla |
|---|---|
| `tipo` | obligatorio: `RESERVA_CREADA`, `CAMBIO_ESTADO` o `RESERVA_ELIMINADA` |
| `reservationId` | obligatorio, > 0 |
| `actor` | obligatorio; **debe ser el usuario del token** (sin distinguir mayusculas), si no 400 |
| `estadoAnterior`, `estadoNuevo` | texto; `estadoNuevo` obligatorio en `CAMBIO_ESTADO` |
| `detalle` | opcional, max. 500 |
| `occurredAt` | opcional (ISO-8601); por defecto el instante del registro. `registradoEn` lo pone audit |

**GET /api/audit/events**, filtros opcionales: `actor`, `tipo`, `reservationId`, `desde`, `hasta` (ISO-8601, ej. `2026-10-01T00:00:00Z`, inclusivos sobre `occurredAt`), `page` (0) y `size` (20, max. 100). Respuesta: `{content, page, size, totalElements, totalPages}`.

Errores JSON `{timestamp, status, error, message}`: 400 (validacion, actor distinto al token, `tipo` o fecha invalidos) y 503 (BD no disponible).

```bash
curl -X POST http://localhost:8086/api/audit/events \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"tipo":"CAMBIO_ESTADO","reservationId":1,"actor":"op@andesstay.cl","estadoAnterior":"CREADA","estadoNuevo":"CONFIRMADA"}'

curl "http://localhost:8086/api/audit/events?tipo=CAMBIO_ESTADO&desde=2026-10-01T00:00:00Z&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN_AUDITOR"
```

## Roles

Claim `roles` -> `ROLE_*` (`security/JwtAuthConverter`), aplicados con `@PreAuthorize`. Si no se cumplen: **403**.

| Endpoint | Admin | Operador_Dominio | Cliente_Dominio | Auditor |
|---|---|---|---|---|
| POST `/api/audit/events` | si | si | si | no |
| GET `/api/audit/events` y `/timeline` | si | no | no | si |

El POST lo llama reservations con el JWT del usuario que hizo la accion (un Cliente al crear su reserva, Admin/Operador al cambiar estados). No se expone en el BFF.

## Tests

```powershell
.\mvnw.cmd clean package
```

No necesitan OCI: perfil `test` con H2 en memoria.

- `AuditServiceTest` (`@DataJpaTest`): registro, actor distinto al token, filtros por actor/tipo/reserva/rango, paginacion desc con tope de 100 y timeline cronologico.
- `AuditControllerSecurityTest`: 201/200/403 por rol, 401 sin token, sin PUT/DELETE (405), 400 con detalle, filtros y fechas invalidas.
