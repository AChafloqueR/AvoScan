# AvoScan — Detección de patologías en frutos de palta en smartphones de gama baja

Proyecto de tesis: *Evaluación de MobileNetV3-Small optimizado para detección de patologías en frutos de palta en dispositivos smartphones de gama baja.*

Aplicación Android que diagnostica enfermedades en frutos de palta a partir de una fotografía, usando un modelo de clasificación de imágenes optimizado para ejecutarse de forma local en dispositivos de bajos recursos.

## Objetivo

Comparar tres arquitecturas de redes neuronales convolucionales (MobileNetV3-Small, MobileNetV2 y EfficientNetB0), evaluar su cuantización y desplegar la opción más adecuada para gama baja en una app Android con inferencia local.

**Clases detectadas:** `Antracnosis` · `Sano` · `Roña`

## Benchmark comparativo (validación cruzada 10-fold, misma receta para los 3 modelos)

| Modelo | F1-score | Kappa | ¿Cumple F1 ≥ 0.85? |
|---|---|---|---|
| **MobileNetV3-Small** | 0.8882 | 0.8315 | ✅ |
| MobileNetV2 | 0.9104 | 0.8650 | ✅ |
| EfficientNetB0 | 0.9161 | 0.8735 | ✅ |

Los tres superan el umbral. EfficientNetB0 lidera en precisión pura, pero **MobileNetV3-Small se eligió como modelo final** por ser el más liviano y rápido —el criterio clave para gama baja— con una precisión competitiva.

## Modelo final desplegado: MobileNetV3-Small

Entrenado con una receta optimizada (data augmentation reforzado + fine-tuning amplio) y evaluado de forma honesta sobre un conjunto de test independiente (split 80/10/10):

| Variante | F1 (test) | Tamaño |
|---|---|---|
| **Float16 (desplegado)** | **≈ 0.93** | 1.97 MB |
| INT8 (rango dinámico) | ≈ 0.86 | 1.14 MB |
| Float32 (referencia) | ≈ 0.93 | 3.87 MB |

Se desplegó la variante **Float16**: conserva la precisión completa (~0.93) en un tamaño mínimo (1.97 MB, muy por debajo del límite de 10 MB).

> **Nota sobre cuantización:** la cuantización entera completa (INT8 por *post-training quantization*) degrada fuertemente a MobileNetV3 por su activación *hard-swish*. Por eso se usaron rango dinámico (pesos en INT8) y float16, que sí preservan la precisión.

## Estructura del repositorio

```
├── README.md
├── dataset/            # Protocolo de captura, clases y splits
├── notebooks/          # Colab: entrenamiento de los 3 modelos, comparación y exportación a TFLite
├── modelos_tflite/     # Modelos MobileNetV3-Small (float32 / float16 / INT8) + etiquetas.txt
├── docs/               # Documentación: benchmark y arquitectura móvil
└── app-android/        # App final en Android Studio (Jetpack Compose + CameraX + TFLite)
```

## Modelos (`modelos_tflite/`)

- `MobileNetV3Small_float16.tflite` (1.97 MB) — **modelo desplegado en la app**.
- `MobileNetV3Small_INT8.tflite` (1.14 MB) — versión INT8 de rango dinámico.
- `MobileNetV3Small_float32.tflite` (3.87 MB) — referencia sin cuantizar.
- `etiquetas.txt` — orden de las clases: `Antracnosis`, `Sano`, `Roña`.

## App (`app-android/`)

Aplicación Android desarrollada en **Android Studio** con Jetpack Compose, CameraX y TensorFlow Lite. Toma una foto del fruto y muestra el diagnóstico con su porcentaje de confianza, ejecutando la inferencia **localmente y sin conexión**.


## Estado del proyecto

**Sprint 1 (cerrado):** dataset preparado, data augmentation, 3 modelos entrenados, benchmark comparativo resuelto, modelo final (MobileNetV3-Small) entrenado, cuantizado y verificado, y app Android funcional con inferencia local.


## Autoría

Anabel Mariliana Chafloque — Tesis 1.
