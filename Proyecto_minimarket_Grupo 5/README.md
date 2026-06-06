# MiniMarket Plus - Backend Seguro con Spring Security y JWT

Backend REST desarrollado con Spring Boot para el caso academico **MiniMarket Plus**. El proyecto integra autenticacion stateless con JWT, autorizacion por roles, validacion de entrada, manejo global de excepciones, auditoria basica, proteccion contra patrones de XSS/SQL Injection, cabeceras de seguridad y pruebas automatizadas de seguridad.

## Tabla de contenidos

- [Objetivo del proyecto](#objetivo-del-proyecto)
- [Tecnologias y librerias](#tecnologias-y-librerias)
- [Base de datos](#base-de-datos)
- [Requisitos previos](#requisitos-previos)
- [Configuracion](#configuracion)
- [Como levantar el proyecto](#como-levantar-el-proyecto)
- [Usuarios de prueba](#usuarios-de-prueba)
- [Autenticacion con JWT](#autenticacion-con-jwt)
- [Endpoints principales](#endpoints-principales)
- [Seguridad implementada](#seguridad-implementada)
- [Pruebas](#pruebas)
- [Empaquetado](#empaquetado)
- [Estructura relevante](#estructura-relevante)
- [Notas para replicar la implementacion](#notas-para-replicar-la-implementacion)

## Objetivo del proyecto

MiniMarket Plus requiere modernizar su backend para controlar recursos como productos, categorias, inventario, ventas, carrito y usuarios. Como parte de la actividad sumativa, el foco del proyecto esta en incorporar mecanismos de seguridad backend que permitan:

- Autenticar usuarios mediante credenciales almacenadas en base de datos.
- Emitir tokens JWT para operar sin sesiones de servidor.
- Autorizar el acceso segun roles: cliente, empleado y administrador.
- Validar datos de entrada antes de persistir.
- Reducir riesgos de XSS, SQL Injection, CSRF y acceso no autorizado.
- Registrar eventos relevantes para trazabilidad basica.
- Responder errores REST de forma estandarizada y segura.

## Tecnologias y librerias

El proyecto usa Java 17, Spring Boot 3.4.1 y Maven Wrapper.

| Libreria | Uso dentro del proyecto |
| --- | --- |
| `spring-boot-starter-web` | Creacion de API REST, controladores y endpoints HTTP. |
| `spring-boot-starter-data-jpa` | Persistencia con JPA/Hibernate y repositorios. |
| `spring-boot-starter-security` | Autenticacion, autorizacion, filtros de seguridad, roles, BCrypt y seguridad por metodos. |
| `spring-boot-starter-validation` | Validacion de DTOs con anotaciones como `@Valid`, `@NotBlank`, `@Size` y reglas de entrada. |
| `jjwt-api` | API principal para crear, firmar y validar tokens JWT. |
| `jjwt-impl` | Implementacion runtime de JJWT. |
| `jjwt-jackson` | Serializacion/deserializacion JSON de claims JWT. |
| `h2` | Base de datos en memoria para desarrollo y pruebas. |
| `lombok` | Reduccion de codigo repetitivo en entidades/modelos cuando aplica. |
| `spring-boot-devtools` | Herramientas de desarrollo local. |
| `spring-boot-starter-test` | Pruebas automatizadas con JUnit/Spring Test. |
| `spring-security-test` | Utilidades de prueba para validar comportamiento de Spring Security. |

> Nota: LDAP/LDAPS fue analizado como estrategia teorica de autenticacion, pero no esta implementado en el codigo practico porque la rubrica actual solicita aplicar JWT como estrategia tecnica principal.

## Base de datos

El proyecto usa **H2 en memoria**. Esto permite levantar el backend sin instalar un motor externo como MySQL o PostgreSQL.

Configuracion actual en `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
```

La consola H2 queda disponible en:

```text
http://localhost:8080/h2-console
```

Datos para entrar:

```text
JDBC URL: jdbc:h2:mem:testdb
User Name: sa
Password: dejar vacio
```

Como la base es en memoria, los datos se reinician cada vez que se detiene la aplicacion.

## Requisitos previos

Antes de ejecutar el proyecto se necesita:

- Java JDK 17 instalado.
- Git instalado, si se clonara desde GitHub.
- PowerShell, CMD o una terminal compatible.
- No es necesario instalar Maven globalmente, porque el proyecto incluye Maven Wrapper (`mvnw` y `mvnw.cmd`).

Verificar Java:

```powershell
java -version
```

Debe aparecer una version 17 o superior compatible.

## Configuracion

La configuracion principal esta en:

```text
src/main/resources/application.properties
```

Propiedades de seguridad importantes:

```properties
minimarket.jwt.secret=${JWT_SECRET:MiniMarketPlusSuperSecretKeyForJwt2026ChangeMe}
minimarket.jwt.expiration-ms=3600000
minimarket.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:4200}
minimarket.login.max-attempts=5
minimarket.login.lock-minutes=15
minimarket.password.min-length=10
```

### Variables de entorno recomendadas

Para una ejecucion mas segura, se recomienda definir el secreto JWT por variable de entorno.

En PowerShell:

```powershell
$env:JWT_SECRET="CambiarPorUnSecretoLargoDeAlMenos32Caracteres"
$env:CORS_ALLOWED_ORIGINS="http://localhost:3000,http://localhost:4200"
```

Luego levantar la aplicacion en la misma terminal.

En desarrollo se puede usar el valor por defecto, pero en un entorno real debe cambiarse obligatoriamente.

## Como levantar el proyecto

Desde la raiz del proyecto:

```powershell
cd "C:\Users\Marialex\Documents\Backend II\minimarket-security\minimarket_Sumativa_S3"
```

Ejecutar con Maven Wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

Cuando la aplicacion levante correctamente, la API quedara disponible en:

```text
http://localhost:8080
```

Endpoint publico de prueba:

```text
GET http://localhost:8080/public/hola
```

Ejemplo con PowerShell:

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/public/hola"
```

## Usuarios de prueba

La clase `DataInitializer` crea usuarios y roles al iniciar el proyecto si no existen.

| Usuario | Password | Rol |
| --- | --- | --- |
| `cliente` | `Cliente123!` | `ROLE_CLIENTE` |
| `empleado` | `Empleado123!` | `ROLE_EMPLEADO` |
| `admin` | `Admin123!` | `ROLE_ADMIN` |

Las contrasenas se guardan usando BCrypt, no en texto plano.

## Autenticacion con JWT

El login se realiza en:

```text
POST /api/auth/login
```

Body JSON:

```json
{
  "username": "admin",
  "password": "Admin123!"
}
```

Ejemplo en PowerShell:

```powershell
$login = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/auth/login" `
  -ContentType "application/json" `
  -Body '{"username":"admin","password":"Admin123!"}'

$token = $login.token
$token
```

Para consumir endpoints protegidos se debe enviar el token en la cabecera `Authorization`:

```text
Authorization: Bearer <token>
```

Ejemplo:

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/usuarios" `
  -Headers @{ Authorization = "Bearer $token" }
```

## Endpoints principales

| Recurso | Ruta base | Acceso |
| --- | --- | --- |
| Login | `/api/auth/login` | Publico |
| Prueba publica | `/public/hola` | Publico |
| Usuarios | `/api/usuarios` | Solo `ADMIN` |
| Inventario | `/api/inventario` | `EMPLEADO` o `ADMIN` |
| Detalle de ventas | `/api/detalle-ventas` | `EMPLEADO` o `ADMIN` |
| Carrito | `/api/carrito` | `CLIENTE` o `ADMIN` |
| Ventas | `/api/ventas` | `CLIENTE`, `EMPLEADO` o `ADMIN` |
| Productos | `/api/productos` | Usuario autenticado. Crear/editar requiere `EMPLEADO` o `ADMIN`; eliminar requiere `ADMIN`. |
| Categorias | `/api/categorias` | Usuario autenticado. Crear/editar requiere `EMPLEADO` o `ADMIN`; eliminar requiere `ADMIN`. |
| Consola H2 | `/h2-console` | Permitida para desarrollo local |

## Seguridad implementada

### Autenticacion

La autenticacion se realiza contra usuarios almacenados en base de datos mediante `CustomUserDetailsService`, `DaoAuthenticationProvider` y `BCryptPasswordEncoder`.

Flujo general:

1. El usuario envia `username` y `password` a `/api/auth/login`.
2. Spring Security valida las credenciales.
3. Si las credenciales son correctas, `JwtUtil` genera un token firmado.
4. El cliente envia el token en cada request protegida.
5. `JwtAuthenticationFilter` valida el token y reconstruye la autenticacion.

### Autorizacion por roles

La autorizacion se aplica en dos capas:

- Reglas por rutas en `SecurityConfig`.
- Reglas por metodo con `@EnableMethodSecurity` y `@PreAuthorize`.

Esto permite proteger recursos sensibles incluso si en el futuro cambia el mapeo de rutas.

### Validacion de entrada

El proyecto usa DTOs y Bean Validation para validar entradas antes de persistir datos. Esto evita exponer directamente entidades JPA en operaciones criticas y reduce errores de datos malformados.

### Manejo global de excepciones

`GlobalExceptionHandler` centraliza las respuestas de error REST. Su objetivo es entregar mensajes consistentes sin exponer stack traces, nombres internos de clases o detalles tecnicos innecesarios.

### Proteccion contra XSS y SQL Injection

`ThreatDetectionFilter` revisa query string, parametros y cuerpos JSON antes de que la solicitud llegue a los controladores. Si detecta patrones sospechosos, responde con error controlado y registra el evento.

Esta proteccion complementa otras medidas como:

- Uso de JPA/repositories en lugar de concatenar SQL manualmente.
- Validacion de DTOs.
- Errores controlados.
- Auditoria de actividad sospechosa.

### Proteccion contra CSRF

El backend funciona como API REST stateless con JWT:

- No usa sesiones HTTP de servidor.
- No depende de cookies de sesion para autenticar.
- Requiere `Authorization: Bearer <token>` en endpoints protegidos.

Por eso CSRF se deshabilita en Spring Security y se compensa con autenticacion por token en cabecera.

### CORS estricto

Los origenes permitidos se configuran mediante:

```properties
minimarket.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:4200}
```

Solo esos origenes pueden consumir la API desde navegador. Para agregar un frontend distinto, se debe actualizar `CORS_ALLOWED_ORIGINS`.

### Cabeceras de seguridad

`SecurityHeadersFilter` agrega cabeceras para reducir exposicion desde navegadores:

- `Content-Security-Policy`
- `X-Content-Type-Options`
- `Referrer-Policy`
- `Permissions-Policy`
- `X-Frame-Options`

### Monitoreo basico

`RequestAuditFilter`, `AuthController`, `LoginAttemptService` y `ThreatDetectionFilter` registran eventos relevantes como:

- Login exitoso.
- Login fallido.
- Bloqueo temporal por intentos repetidos.
- Solicitudes con payload sospechoso.
- IP del cliente.
- User-Agent.
- Request ID para correlacion.

## Pruebas

Ejecutar todas las pruebas:

```powershell
.\mvnw.cmd test
```

La clase principal de pruebas de seguridad es:

```text
src/test/java/com/minimarket/SecurityThreatProtectionTests.java
```

Estas pruebas verifican:

- Cabeceras de seguridad.
- Bloqueo de payload XSS en JSON.
- Bloqueo de payload XSS en query string.
- Bloqueo de patrones de SQL Injection.
- Rechazo de operaciones protegidas sin JWT.
- Rechazo de CORS desde origen no confiable.
- Acceso autorizado con usuario administrador y JWT valido.
- Validacion de contrasenas debiles.

## Empaquetado

Para generar el archivo `.jar`:

```powershell
.\mvnw.cmd -DskipTests package
```

El artefacto queda en:

```text
target/minimarket-0.0.1-SNAPSHOT.jar
```

Ejecutar el JAR:

```powershell
java -jar target/minimarket-0.0.1-SNAPSHOT.jar
```

## Estructura relevante

```text
src/main/java/com/minimarket
├── config
│   └── DataInitializer.java
├── controller
│   ├── AuthController.java
│   ├── UsuarioController.java
│   ├── ProductoController.java
│   ├── CategoriaController.java
│   ├── InventarioController.java
│   ├── VentaController.java
│   ├── DetalleVentaController.java
│   └── CarritoController.java
├── dto
│   ├── UsuarioRequest.java
│   └── UsuarioResponse.java
├── exception
│   ├── ApiError.java
│   └── GlobalExceptionHandler.java
├── security
│   ├── config
│   │   └── SecurityConfig.java
│   ├── filter
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── RequestAuditFilter.java
│   │   ├── SecurityHeadersFilter.java
│   │   └── ThreatDetectionFilter.java
│   ├── model
│   │   ├── LoginRequest.java
│   │   └── LoginResponse.java
│   ├── service
│   │   ├── CustomUserDetailsService.java
│   │   ├── LoginAttemptService.java
│   │   └── PasswordPolicyService.java
│   └── util
│       └── JwtUtil.java
└── service / repository / entity
```

## Notas para replicar la implementacion

Para replicar esta implementacion en otro backend Spring Boot:

1. Agregar dependencias de Spring Security, Validation, JPA y JJWT.
2. Crear entidades `Usuario` y `Rol`, con relacion entre usuarios y roles.
3. Guardar contrasenas con `BCryptPasswordEncoder`.
4. Implementar `UserDetailsService` para cargar usuarios desde base de datos.
5. Crear un endpoint `/api/auth/login` que autentique y emita JWT.
6. Crear `JwtUtil` para generar, firmar y validar tokens.
7. Crear un filtro JWT que lea `Authorization: Bearer <token>`.
8. Configurar `SecurityFilterChain` con sesiones `STATELESS`.
9. Definir reglas por rutas y reforzarlas con `@PreAuthorize`.
10. Separar entidades JPA de la entrada/salida usando DTOs.
11. Validar DTOs con Bean Validation.
12. Centralizar errores con `@RestControllerAdvice`.
13. Configurar CORS solo para origenes confiables.
14. Agregar cabeceras de seguridad.
15. Registrar eventos de seguridad con IP, User-Agent y request ID.
16. Probar XSS, SQL Injection, CSRF, CORS, roles y JWT con pruebas automatizadas.

## Comandos rapidos

```powershell
# Entrar al proyecto
cd "C:\Users\Marialex\Documents\Backend II\minimarket-security\minimarket_Sumativa_S3"

# Levantar aplicacion
.\mvnw.cmd spring-boot:run

# Ejecutar pruebas
.\mvnw.cmd test

# Generar JAR
.\mvnw.cmd -DskipTests package

# Ejecutar JAR generado
java -jar target/minimarket-0.0.1-SNAPSHOT.jar
```
