# Contrato de salida de revisiones

Las revisiones devuelven JSON con `fixed_point`, `findings` y `validation`.

Cada hallazgo incluye `id`, `priority`, `axis`, `file`, `finding` y `evidence`. Las prioridades son `P1` (bloquea integración o publicación), `P2` (debe planificarse) y `P3` (mejora opcional).
