# Repzi

App Android para registrar entrenamientos de gimnasio (rutinas predefinidas y personalizadas). Es un proyecto de práctica para repasar arquitectura y herramientas de Android moderno.

## Stack

Kotlin, Coroutines y Flow, Jetpack Compose (Material 3), Hilt, Room, Navigation Compose, Gradle con convention plugins y version catalog.

## Arquitectura

Clean Architecture con MVVM, modularizada por capa y por feature.

```
app                      raíz de composición (Application, Activity, NavHost)
core/
  domain                 Kotlin puro: modelos, repositorios (interfaces), casos de uso
  data                   implementaciones de repositorio (offline-first sobre Room)
  database               Room: entidades, DAOs, base de datos
  ui                     tema Material 3 y componentes comunes
  testing                fakes y reglas de test compartidas
feature/
  exercises              catálogo de ejercicios con filtro por grupo muscular (implementada)
  routines, session, history   módulos creados, sin implementar
build-logic/convention   convention plugins (repzi.android.*, repzi.jvm.library)
```

Dependencias permitidas: `feature` → `core:domain` + `core:ui`; `core:data` → `core:domain` + `core:database`; `app` conoce todo para armar el grafo de Hilt. `core:domain` no depende de Android, para poder pasarlo a Kotlin Multiplatform más adelante.

## Cómo abrirlo

1. Abrir la carpeta del proyecto en Android Studio y esperar el sync de Gradle (JDK 17 o superior, el JBR de Android Studio sirve).
2. Ejecutar la configuración `app` en un emulador o dispositivo.
3. Tests: `./gradlew test` (unitarios) y `./gradlew :core:database:connectedAndroidTest` (Room, necesita emulador).

Nota: el proyecto se generó sin compilarlo. Si el primer sync falla por algún cambio de API de las versiones del catálogo, se corrige en `gradle/libs.versions.toml` o en `build-logic`.

## Roadmap

- **Nivel 1 (MVP):** biblioteca de ejercicios, rutinas personalizadas, registro de sesión (series, repeticiones, peso), historial.
- **Nivel 2:** rutinas predefinidas, temporizador de descanso, gráficas de progreso, búsqueda.
- **Nivel 3:** catálogo remoto con sincronización, WorkManager, notificaciones, widget (Glance).
- **Después:** Baseline Profiles y Macrobenchmark, screenshot tests, migración de `core:domain` y `core:data` a KMP.

## Mediciones

| Medición | Antes | Después | Cambio aplicado |
|---|---|---|---|
| Build limpio | | | |
| Build incremental | | | |
| Arranque en frío | | | |
