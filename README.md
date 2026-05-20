# 🏦 BancoDigital — Plataforma Bancaria Digital

<div align="center">

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.4-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![React](https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-5-646CFF?style=for-the-badge&logo=vite&logoColor=white)

**Plataforma bancaria digital fullstack para gestión de cuentas, tarjetas, facturas y transferencias en tiempo real.**

</div>

---

## 📋 Tabla de Contenidos

- [Descripción General](#-descripción-general)
- [Arquitectura del Sistema](#-arquitectura-del-sistema)
- [Stack Tecnológico](#-stack-tecnológico)
- [Estructura del Proyecto](#-estructura-del-proyecto)
- [Modelo de Datos](#-modelo-de-datos)
- [API REST — Endpoints](#-api-rest--endpoints)
- [Flujos de Negocio](#-flujos-de-negocio)
- [Requisitos Previos](#-requisitos-previos)
- [Instalación y Configuración](#-instalación-y-configuración)
- [Variables de Entorno](#-variables-de-entorno)
- [Ejecución en Desarrollo](#-ejecución-en-desarrollo)
- [Roles y Permisos](#-roles-y-permisos)

---

## 🔍 Descripción General

**BancoDigital** es una aplicación bancaria fullstack que permite a usuarios gestionar cuentas, solicitar tarjetas (crédito/débito), pagar facturas de servicios y realizar transferencias, con un panel de administración para la revisión y aprobación de operaciones.

### Características Principales

| Módulo                | Descripción                                                             |
| --------------------- | ----------------------------------------------------------------------- |
| 👤 **Autenticación**  | Login por documento + contraseña con gestión de roles                   |
| 🏦 **Cuentas**        | Gestión de cuentas bancarias con saldo en tiempo real                   |
| 💳 **Tarjetas**       | Solicitud, aprobación admin, crédito y débito con saldos independientes |
| 🧾 **Facturas**       | Generación y pago de facturas de servicios públicos                     |
| 🔄 **Transferencias** | Transferencias entre cuentas con validación antifraude                  |
| 🛡️ **Admin Panel**    | Aprobación/rechazo de tarjetas y transferencias pendientes              |

---

## 🏛️ Arquitectura del Sistema

```mermaid
graph TB
    subgraph Frontend["🖥️ Frontend — React 18 + Vite (puerto 5173)"]
        UI[Interfaz de Usuario]
        Pages[Pages: Login / Dashboard / Tarjetas / Facturas / Admin]
        Services[Services: authService / transaccionService]
        UI --> Pages --> Services
    end

    subgraph Backend["☕ Backend — Spring Boot 4 (puerto 8080)"]
        Controller[Controllers REST]
        Service[Services / Business Logic]
        Repository[Repositories JPA]
        Controller --> Service --> Repository
    end

    subgraph DB["🐘 Base de Datos — PostgreSQL 18"]
        tbl_usuario[(tbl_usuario)]
        tbl_cuenta[(tbl_cuenta)]
        tbl_tarjeta[(tbl_tarjeta)]
        tbl_factura[(tbl_factura)]
        tbl_transaccion[(tbl_transaccion)]
    end

    Services -- "HTTP REST / JSON" --> Controller
    Repository -- "JPA / Hibernate" --> DB
```

---

## 🗂️ Estructura del Proyecto

```
fraude-detection/
│
├── 📁 src/main/java/com/fraude/
│   ├── 🏦 cuenta/               # Modelo, repositorio de cuentas bancarias
│   ├── 💳 tarjeta/              # Solicitud, aprobación, recarga de tarjetas
│   │   ├── controller/          # TarjetaController — endpoints REST
│   │   ├── model/               # Tarjeta.java — entidad JPA
│   │   ├── repository/          # TarjetaRepository
│   │   └── service/             # TarjetaService — lógica de negocio
│   ├── 🧾 factura/              # Generación y pago de facturas
│   ├── 🔄 transaccion/          # Procesamiento y validación de transferencias
│   ├── 👤 usuario/              # Registro, login y gestión de usuarios
│   ├── 🔑 rol/                  # Roles del sistema (ADMIN / USER)
│   ├── 📊 reporte/              # Módulo de reportes
│   └── ⚙️ config/               # CORS, seguridad, configuración general
│
├── 📁 src/main/resources/
│   └── application.properties   # Configuración de BD, servidor, Stripe
│
├── 📁 frontend/promptly-fluent/
│   ├── 📁 src/
│   │   ├── pages/               # Login, Dashboard, Tarjetas, Facturas, Admin
│   │   ├── components/          # AppLayout, NavLink, StatusBadge + shadcn/ui
│   │   ├── services/            # Llamadas a la API REST
│   │   ├── hooks/               # useAuth, use-toast, use-mobile
│   │   ├── lib/                 # utils, roles
│   │   └── data/                # Datos mock
│   ├── vite.config.ts
│   ├── tailwind.config.ts
│   └── package.json
│
├── pom.xml                      # Dependencias Maven
├── mvnw / mvnw.cmd              # Maven Wrapper
└── README.md
```

---

## 🗄️ Modelo de Datos

```mermaid
erDiagram
    USUARIO {
        string num_documento PK
        string nombre
        string contrasena
        string correo
        int tipo_documento_id
        int rol_id
    }

    CUENTA {
        string numero_cuenta PK
        decimal saldo
        string num_documento FK
        int tipo_documento_id
    }

    TARJETA {
        int id PK
        string num_documento FK
        string tipo_tarjeta
        string ultimos_cuatro
        string nombre_titular
        string fecha_expiracion
        string marca
        int estado_id
        double limite_credito
        double credito_disponible
        double saldo_tarjeta
        string motivo_rechazo
        datetime fecha_creacion
    }

    FACTURA {
        int id PK
        string num_documento FK
        string tipo_servicio
        string descripcion
        string referencia
        double monto
        string estado
        datetime fecha_vencimiento
        datetime fecha_pago
        int tarjeta_id FK
    }

    TRANSACCION {
        int id PK
        double monto
        string cuenta_origen_id FK
        string cuenta_destino_id FK
        int estado_id
        int tipo_transaccion_id
        datetime fecha
    }

    USUARIO ||--o{ CUENTA : "tiene"
    USUARIO ||--o{ TARJETA : "solicita"
    USUARIO ||--o{ FACTURA : "paga"
    CUENTA ||--o{ TRANSACCION : "origina"
    CUENTA ||--o{ TRANSACCION : "recibe"
    TARJETA ||--o{ FACTURA : "paga"
```

### Estados de Tarjeta

| `estado_id` | Estado       | Descripción                    |
| :---------: | ------------ | ------------------------------ |
|     `1`     | ✅ ACTIVA    | Lista para usar                |
|     `2`     | ⏳ PENDIENTE | Esperando aprobación del admin |
|     `3`     | 🗑️ ELIMINADA | Cancelada por el usuario       |
|     `4`     | ❌ RECHAZADA | Rechazada por el admin         |

---

## 🔌 API REST — Endpoints

### Transacciones

| Método | Ruta                             | Descripción               | Rol     |
| ------ | -------------------------------- | ------------------------- | ------- |
| `POST` | `/api/transacciones`             | Crear nueva transferencia | Usuario |
| `GET`  | `/api/transacciones/cuenta/{id}` | Historial de cuenta       | Usuario |
| `GET`  | `/api/transacciones`             | Todas las transacciones   | Admin   |
| `GET`  | `/api/transacciones/pendientes`  | Transacciones pendientes  | Admin   |
| `PUT`  | `/api/transacciones/{id}/estado` | Aprobar / Rechazar        | Admin   |

### Tarjetas

| Método   | Ruta                             | Descripción                | Rol     |
| -------- | -------------------------------- | -------------------------- | ------- |
| `GET`    | `/api/tarjetas`                  | Mis tarjetas               | Usuario |
| `POST`   | `/api/tarjetas`                  | Solicitar tarjeta          | Usuario |
| `POST`   | `/api/tarjetas/{id}/recargar`    | Recargar saldo (débito)    | Usuario |
| `DELETE` | `/api/tarjetas/{id}`             | Cancelar tarjeta           | Usuario |
| `GET`    | `/api/tarjetas/admin/pendientes` | Ver solicitudes pendientes | Admin   |
| `GET`    | `/api/tarjetas/admin/todas`      | Ver todas las tarjetas     | Admin   |
| `POST`   | `/api/tarjetas/{id}/aprobar`     | Aprobar solicitud          | Admin   |
| `POST`   | `/api/tarjetas/{id}/rechazar`    | Rechazar solicitud         | Admin   |

### Facturas

| Método | Ruta                           | Descripción           | Rol     |
| ------ | ------------------------------ | --------------------- | ------- |
| `GET`  | `/api/facturas`                | Mis facturas          | Usuario |
| `POST` | `/api/facturas/generar-prueba` | Generar facturas demo | Usuario |
| `POST` | `/api/facturas/{id}/pagar`     | Pagar factura         | Usuario |

> **Header requerido:** `X-User-Documento: {numDocumento}` en todos los endpoints de usuario.

---

## 🔄 Flujos de Negocio

### Flujo: Solicitud y Aprobación de Tarjeta

```mermaid
sequenceDiagram
    actor U as Usuario
    participant F as Frontend
    participant B as Backend
    participant DB as Base de Datos
    actor A as Administrador

    U->>F: Solicita nueva tarjeta
    F->>B: POST /api/tarjetas
    B->>DB: Guarda con estado_id = 2 (PENDIENTE)
    B-->>F: { mensaje: "Solicitud enviada" }
    F-->>U: Muestra badge PENDIENTE

    A->>F: Ve panel de administración
    F->>B: GET /api/tarjetas/admin/pendientes
    B-->>F: Lista de solicitudes

    alt Tarjeta CRÉDITO
        A->>F: Ingresa límite de crédito y aprueba
        F->>B: POST /api/tarjetas/{id}/aprobar { limiteCredito }
        B->>DB: estado_id=1, limiteCredito, creditoDisponible asignados
    else Tarjeta DÉBITO
        A->>F: Aprueba directamente
        F->>B: POST /api/tarjetas/{id}/aprobar
        B->>DB: estado_id=1, saldoTarjeta=0
    end

    B-->>F: Tarjeta aprobada
    F-->>U: Badge cambia a ACTIVA
```

### Flujo: Pago de Factura con Tarjeta

```mermaid
sequenceDiagram
    actor U as Usuario
    participant F as Frontend
    participant B as Backend
    participant DB as Base de Datos

    U->>F: Selecciona factura y tarjeta activa
    F->>B: POST /api/facturas/{id}/pagar { tarjetaId }
    B->>DB: Valida tarjeta activa

    alt Tarjeta CRÉDITO
        B->>DB: Verifica creditoDisponible >= monto
        B->>DB: creditoDisponible -= monto
    else Tarjeta DÉBITO
        B->>DB: Verifica saldoTarjeta >= monto
        B->>DB: saldoTarjeta -= monto
    end

    B->>DB: factura.estado = "PAGADA"
    B-->>F: { mensaje: "Factura pagada exitosamente" }
    F-->>U: Confirmación de pago
```

### Flujo: Transferencia con Validación Antifraude

```mermaid
flowchart TD
    A([Usuario inicia transferencia]) --> B{¿Cuenta origen existe?}
    B -- No --> ERR1[❌ Error: cuenta no encontrada]
    B -- Sí --> C{¿Saldo suficiente?}
    C -- No --> ERR2[❌ Error: saldo insuficiente]
    C -- Sí --> D{¿Es transferencia sospechosa?}
    D -- Sí --> E[⏳ Estado: PENDIENTE — requiere admin]
    D -- No --> F[✅ Estado: APROBADA automáticamente]
    E --> G[Admin revisa en panel]
    G --> H{Decisión admin}
    H -- Aprobar --> I[💸 Fondos transferidos]
    H -- Rechazar --> J[🔒 Fondos bloqueados / devueltos]
    F --> I
```

---

## ⚙️ Requisitos Previos

Asegúrate de tener instalados:

| Herramienta | Versión mínima           | Verificar         |
| ----------- | ------------------------ | ----------------- |
| Java JDK    | 17+                      | `java -version`   |
| Maven       | 3.9+ (incluido con mvnw) | `./mvnw -version` |
| Node.js     | 18+                      | `node -version`   |
| npm         | 9+                       | `npm -version`    |
| PostgreSQL  | 14+                      | `psql -version`   |

---

## 🚀 Instalación y Configuración

### 1. Clonar el repositorio

```bash
git clone https://github.com/tu-usuario/fraude-detection.git
cd fraude-detection
```

### 2. Configurar la base de datos

```sql
-- Conectar como superusuario (postgres)
CREATE USER fraude_user WITH PASSWORD 'fraude_pass';
CREATE DATABASE fraude_detection OWNER fraude_user;
GRANT ALL PRIVILEGES ON DATABASE fraude_detection TO fraude_user;
```

### 3. Configurar application.properties

Edita `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/fraude_detection
spring.datasource.username=fraude_user
spring.datasource.password=fraude_pass
spring.jpa.hibernate.ddl-auto=update
server.port=8080
```

> ⚠️ Las tablas se crean automáticamente gracias a `ddl-auto=update` al arrancar la app.

### 4. Instalar dependencias del frontend

```bash
cd frontend/promptly-fluent
npm install
```

---

## 🔐 Variables de Entorno

| Variable                     | Descripción                  | Ejemplo                                             |
| ---------------------------- | ---------------------------- | --------------------------------------------------- |
| `spring.datasource.url`      | URL de conexión a PostgreSQL | `jdbc:postgresql://localhost:5432/fraude_detection` |
| `spring.datasource.username` | Usuario de BD                | `fraude_user`                                       |
| `spring.datasource.password` | Contraseña de BD             | `fraude_pass`                                       |
| `server.port`                | Puerto del backend           | `8080`                                              |

> 🔒 **Seguridad**: Nunca subas credenciales reales al repositorio. Usa variables de entorno del sistema en producción.

---

## ▶️ Ejecución en Desarrollo

### Backend (Spring Boot)

```bash
# Desde la raíz del proyecto
./mvnw spring-boot:run          # Linux/Mac
.\mvnw.cmd spring-boot:run      # Windows
```

El servidor arranca en → **http://localhost:8080**

### Frontend (React + Vite)

```bash
cd frontend/promptly-fluent
npm run dev
```

La aplicación arranca en → **http://localhost:5173**

### Build de producción

```bash
# Backend
./mvnw clean package -DskipTests

# Frontend
cd frontend/promptly-fluent
npm run build
```

---

## 👥 Roles y Permisos

```mermaid
graph LR
    subgraph USUARIO["👤 Rol: USUARIO"]
        U1[Ver mis tarjetas]
        U2[Solicitar tarjeta]
        U3[Recargar saldo débito]
        U4[Pagar facturas]
        U5[Hacer transferencias]
        U6[Ver historial]
    end

    subgraph ADMIN["🛡️ Rol: ADMINISTRADOR"]
        A1[Todo lo de USUARIO]
        A2[Ver solicitudes de tarjetas]
        A3[Aprobar / Rechazar tarjetas]
        A4[Ver todas las transacciones]
        A5[Aprobar / Rechazar transferencias]
    end
```

| Acción                          | Usuario | Admin |
| ------------------------------- | :-----: | :---: |
| Ver mis tarjetas                |   ✅    |  ✅   |
| Solicitar tarjeta               |   ✅    |  ✅   |
| Aprobar/rechazar tarjetas       |   ❌    |  ✅   |
| Pagar facturas                  |   ✅    |  ✅   |
| Realizar transferencias         |   ✅    |  ✅   |
| Ver todas las transacciones     |   ❌    |  ✅   |
| Aprobar/rechazar transferencias |   ❌    |  ✅   |

---

## 🏗️ Tecnologías Utilizadas

### Backend

- **Spring Boot 4.0.4** — Framework principal
- **Spring Data JPA + Hibernate 7** — ORM y acceso a datos
- **PostgreSQL** — Base de datos relacional
- **Lombok** — Reducción de código boilerplate
- **Jakarta Validation** — Validación de datos en endpoints

### Frontend

- **React 18** — Librería de UI
- **TypeScript 5** — Tipado estático
- **Vite 5** — Bundler y servidor de desarrollo
- **TailwindCSS** — Framework de estilos utilitarios
- **shadcn/ui + Radix UI** — Componentes accesibles de alta calidad
- **React Hook Form** — Gestión de formularios
- **Sonner** — Notificaciones toast

---

<div align="center">

**Desarrollado con ❤️ — FraudeShield © 2026**

</div>
