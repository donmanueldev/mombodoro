# ADR 0001: separar estado de tareas y persistencia de la UI

## Contexto

La composición de la ventana abría SQLite y aplicaba mutaciones de tareas directamente.

## Decisión

`FocusTasksController` contiene el estado y las acciones de tareas; depende del contrato `FocusTaskStore`. `FocusTaskRepository` es la implementación SQLite. `PomodoroApp` funciona como composición y entrega acciones a las vistas.

## Consecuencias

Las interacciones se prueban con un almacén en memoria y un futuro almacén no requiere editar los composables.
