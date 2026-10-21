# eco-air-backend: Taller Docker (semana 13)

Julio Bolaños Benavides · Código 908632 · Programación Orientada a Objetos

Este repositorio tiene el backend de alertas ambientales eco-air-backend, hecho con Spring Boot 4.1.1, Java 17, Spring Data JPA (Hibernate) y H2. El backend está empaquetado en una imagen Docker multietapa que no corre como root y que tiene un límite de memoria calibrado.

## Contenido

| Archivo | Para qué sirve |
|---|---|
| `Dockerfile` y `.dockerignore` | Construcción multietapa (Reto 1) |
| `audit_session.log` | Registro crudo de la terminal, grabado con `script` el 20 y el 21 de octubre de 2026 |
| `FORENSIC_REPORT.md` | Tabla de identidad, hashes y evidencias de los retos 1, 2 y 3 |
| `pom.xml` y `src/` | Código del proyecto |

## Construir y ejecutar

```bash
docker build --build-arg STUDENT_ID="908632" -t eco-air-backend:1.0.0 .
docker run -d --name eco_air_active -p 8080:8080 -m 208m eco-air-backend:1.0.0
curl http://localhost:8080/api/alertas
```

## Resultados principales

- **Hash de la imagen auditada:** `sha256:c2bba550c077fff5a98887aa624da27c1172f3c2989613f779eb075b908b26fe`
- **Variable inyectada:** `OPERATOR_ID=908632`
- **Usuario del proceso:** `sensor_user` (UID 10001)
- **Límite de memoria calibrado:** `208m`. Con `48m` el contenedor termina con `OOMKilled: true`.
