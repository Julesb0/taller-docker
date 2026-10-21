# FORENSIC_REPORT: eco-air-backend

**Taller Docker, semana 13**
Julio Bolaños Benavides · Código 908632
Repositorio: https://github.com/Julesb0/taller-docker

Las pruebas se hicieron el 20 y el 21 de octubre de 2026 en Windows con WSL2 (Ubuntu 24.04), Docker Desktop 4.93.0 y Docker Engine 29.8.1. Todas las salidas de este reporte están en `audit_session.log`, que se grabó con `script -a -f` en dos sesiones: el 20/10 desde las 16:31:54 y el 21/10 desde la 01:15.

Hay dos cosas que tener en cuenta al leer las horas:

- El prompt del log muestra la hora de Colombia (-05:00). Esa hora es la del momento en que aparece el prompt, no la del momento en que se escribe el comando.
- Los logs de los contenedores (`docker logs`, Maven) usan UTC. Por ejemplo, 06:18Z equivale a la 01:18 en Colombia.

---

## 1. Tabla de identidad

| Dato | Comando | Resultado |
|---|---|---|
| Hash de la imagen | `docker inspect --format='{{.Id}}' eco-air-backend:1.0.0` | `sha256:c2bba550c077fff5a98887aa624da27c1172f3c2989613f779eb075b908b26fe` |
| Variable de entorno inyectada | `docker exec eco_air_active env \| grep OPERATOR_ID` | `OPERATOR_ID=908632` |
| Consumo final calibrado (`-m 208m`) | `docker stats --no-stream eco_air_active` | Ver bloque siguiente |

Esta es la salida completa de `docker stats --no-stream eco_air_active`, del 21/10 a las 02:41. Corresponde al contenedor `68be9e269d97` con `-m 208m`, después de la prueba de carga:

```
CONTAINER ID   NAME             CPU %     MEM USAGE / LIMIT   MEM %     NET I/O         BLOCK I/O       PIDS
68be9e269d97   eco_air_active   0.13%     194.1MiB / 208MiB   93.32%    619kB / 638kB   80.9MB / 13MB   31
```

El hash de la imagen es el mismo en tres partes del log:

- en el build: `exporting manifest list sha256:c2bba550c077...`
- en `docker images`: ID `c2bba550c077`
- en `docker inspect`

---

## 2. Huellas SHA-256 de los archivos entregados

| Archivo | SHA-256 |
|---|---|
| `Dockerfile` | `0c7c259e4f54e84be92dd13059ae1b2cbc802386fb1e21641cbc69b33a495b44` |
| `.dockerignore` | `05828b9c97c9934bb1c11f9795d3edd6ce077f286b1aa0d2aa3c1172b4bf0bf0` |
| `audit_session.log` (después de cerrar la grabación, 21/10 02:46) | `05a068713942adcab86680dae78dc726da99a7924db35142883cd300a524b66a` |
| `pom.xml` del proyecto (registrado en el log, 20/10 16:33) | `680ffe144f5818bf86eeb3ef0c1ba2dc962f2b91d1e0f4f17a11c0723972afc2` |

---

## 3. Entorno

| Dato | Valor |
|---|---|
| Sistema | Windows + WSL2, Ubuntu 24.04, kernel `6.6.87.2-microsoft-standard-WSL2` |
| Docker | Desktop 4.93.0 (240920), Engine 29.8.1, API 1.56, containerd v2.3.5, runc 1.5.1 |
| cgroups | v2, driver `cgroupfs` |
| Almacenamiento | `overlayfs` con `io.containerd.snapshotter.v1` |
| Recursos asignados a Docker | 12 CPU, 7.376 GiB |
| ID del daemon | `ac142785-c1b0-48cf-87b0-fc2cf0c48c58` |
| Proyecto | Spring Boot 4.1.1, Java 17, Spring Web MVC, Spring Data JPA (Hibernate), H2; clase principal `EcoAirApplication` |

---

## 4. Tabla de evidencias

| # | Fecha y hora | Comando | Resultado |
|---|---|---|---|
| R1.1 | 20/10 16:35–16:37 | `docker build -t eco-air-backend:1.0.0 .` (sin `--build-arg`) | Falla en `[builder 6/7]` con `ERROR: falta --build-arg STUDENT_ID`, exit code 1 |
| R1.2 | 20/10 16:39 | `docker build --build-arg STUDENT_ID="908632" -t eco-air-backend:1.0.0` (comando del enunciado) | `ERROR: docker: 'docker buildx build' requires 1 argument` (falta el contexto `.`) |
| R1.3 | 20/10 16:43 | `docker build --build-arg STUDENT_ID="908632" -t eco-air-backend:1.0.0 .` | Éxito, `echo $?` = 0, imagen `c2bba550c077` |
| R2.1 | 21/10 01:18 | `docker history --human --format "table {{.Size}}\t{{.CreatedBy}}" eco-air-backend:1.0.0` | Solo capas del runtime; ninguna de Maven ni de `src` |
| R2.2 | 21/10 01:18 | `docker run -d --name eco_air_active -p 8080:8080 eco-air-backend:1.0.0` y `docker top eco_air_active -o pid,user,comm` | Contenedor `999e5a8efdc0...`; `528  10001  java` |
| R2.3 | 21/10 01:25 | `docker exec eco_air_active touch /etc/crontab_fake` | `Permission denied`, exit 1 |
| R2.4 | 21/10 01:25 | `docker exec eco_air_active which mvn` y `docker exec eco_air_active find / -name "*.java" 2>/dev/null` | Sin salida en ambos (`which` devuelve 1) |
| R3.1 | 21/10 01:33 | `docker run -d --name eco_air_crash -m 48m eco-air-backend:1.0.0` y `docker inspect` | `Status: exited \| ExitCode: 137 \| OOMKilled: true` |
| R3.2 | 21/10 01:47–02:41 | Pruebas de `-m` de 512m a 96m y prueba de carga con 208m | Umbral calibrado: **208m** |

---

## 5. Reto 1: construcción multietapa

### 5.1 Build con el ID (R1.3)

Las imágenes base, con su digest:

```
FROM docker.io/library/maven:3.9.6-eclipse-temurin-17-alpine@sha256:ffddac7b04101358048f872fba9d978b2a1f9647955d158c1e565233cb95577d
FROM docker.io/library/eclipse-temurin:17-jre-alpine@sha256:3c472129dc75a8d1d7a3f2df5b2093a8077e4493deff046754d4764d0371de63
```

Pasos más importantes del build:

```
#11 [internal] load build context
#11 transferring context: 927B done
#12 [builder 4/7] RUN mvn dependency:go-offline -B
#12 CACHED
#16 [builder 6/7] RUN if [ -z "908632" ]; then echo "ERROR: falta --build-arg STUDENT_ID"; exit 1; fi;     echo "Compilando eco-air-backend para el estudiante: 908632"
#16 0.285 Compilando eco-air-backend para el estudiante: 908632
#17 [builder 7/7] RUN mvn package -DskipTests -B
#17 2.741 [INFO] Compiling 4 source files with javac [debug parameters release 17] to target/classes
#17 3.811 [INFO] Tests are skipped.
#17 4.093 [INFO] Building jar: /build/target/eco-air-backend-0.0.1-SNAPSHOT.jar
#17 4.737 [INFO] BUILD SUCCESS
#18 [runtime 4/4] COPY --from=builder --chown=sensor_user:sensorgroup /build/target/*.jar app.jar
#19 exporting manifest sha256:1ca63404c6c86574ac8c9b2acb849bb3923281068f94eb137075922087c30193 0.0s done
#19 exporting config sha256:70d6c800b206ae4a27d48f46a6ca8462564df02682fc64c4deab4e9b4522244f 0.0s done
#19 exporting attestation manifest sha256:844f4c0e2c91d505207c6ab4f1da80c89f362a0290a8d4e3de950ff5e61c8e84 0.0s done
#19 exporting manifest list sha256:c2bba550c077fff5a98887aa624da27c1172f3c2989613f779eb075b908b26fe 0.0s done
#19 naming to docker.io/library/eco-air-backend:1.0.0 done
```

```
IMAGE                   ID             DISK USAGE   CONTENT SIZE
eco-air-backend:1.0.0   c2bba550c077        365MB          118MB
```

**Lectura:**

- El `go-offline` salió `CACHED`; en R1.1 había tardado 100.2 s. Cambiar el ID no invalidó la capa de dependencias, porque el `ARG` va después de esa capa.
- El contexto que viajó al build fue de solo 927 B, gracias al `.dockerignore`.

---

## 6. Reto 2: auditoría forense

### 6.1 Desacoplamiento de capas (R2.1)

```
SIZE      CREATED BY
0B        ENTRYPOINT ["java" "-jar" "/app/app.jar"]
0B        EXPOSE [8080/tcp]
0B        USER sensor_user
0B        ENV OPERATOR_ID=908632
0B        ARG STUDENT_ID=908632
0B        ENV JAVA_TOOL_OPTIONS=-XX:+UseContainerSuppo…
52.5MB    COPY --chown=sensor_user:sensorgroup /build/…
4.1kB     WORKDIR /app
36.9kB    RUN /bin/sh -c addgroup -S -g 10001 sensorgr…
0B        ENTRYPOINT ["/__cacert_entrypoint.sh"]
12.3kB    COPY --chmod=755 entrypoint.sh /__cacert_ent…
12.3kB    RUN /bin/sh -c set -eux;     echo "Verifying…
141MB     RUN /bin/sh -c set -eux;     ARCH="$(apk --p…
0B        ENV JAVA_VERSION=jdk-17.0.20.1+1
44MB      RUN /bin/sh -c set -eux;     apk add --no-ca…
0B        ENV LANG=en_US.UTF-8 LANGUAGE=en_US:en LC_AL…
0B        ENV PATH=/opt/java/openjdk/bin:/usr/local/sb…
0B        ENV JAVA_HOME=/opt/java/openjdk
0B        CMD ["/bin/sh"]
9.08MB    ADD alpine-minirootfs-3.24.2-x86_64.tar.gz /…
```

**Lectura:**

- Las capas de abajo son de `eclipse-temurin:17-jre-alpine`: Alpine 3.24.2 y el JRE 17.0.20.1 (141 MB).
- Las nueve de arriba son las del Dockerfile. No aparece ninguna capa de Maven, de `COPY src` ni del `go-offline`: la etapa builder no está en la imagen final.
- El jar aparece una sola vez (52.5 MB).
- El ID quedó registrado en `ARG STUDENT_ID=908632` y en `ENV OPERATOR_ID=908632`.

### 6.2 Aislamiento de privilegios (R2.2)

```
$ docker run -d --name eco_air_active -p 8080:8080 eco-air-backend:1.0.0
999e5a8efdc02870fa2d11d80124106ca41784da6f7fda8e330eebd96618619a

$ docker top eco_air_active -o pid,user,comm
PID                 USER                COMMAND
528                 10001               java

$ docker exec eco_air_active id
uid=10001(sensor_user) gid=10001(sensorgroup) groups=10001(sensorgroup)

$ docker logs eco_air_active 2>&1 | grep -E "Picked up|Starting EcoAirApplication|Started EcoAirApplication"
Picked up JAVA_TOOL_OPTIONS: -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+ExitOnOutOfMemoryError
2026-10-21T06:18:20.539Z  INFO 1 --- [eco-air-backend] [           main] com.ecoair.EcoAirApplication             : Starting EcoAirApplication v0.0.1-SNAPSHOT using Java 17.0.20.1 with PID 1 (/app/app.jar started by sensor_user in /app)
2026-10-21T06:18:24.801Z  INFO 1 --- [eco-air-backend] [           main] com.ecoair.EcoAirApplication             : Started EcoAirApplication in 4.888 seconds (process running for 6.337)
```

**Lectura:**

- `java` corre con el UID 10001 (`sensor_user`), no como root.
- `docker top` muestra el número y no el nombre porque mira el proceso desde fuera del contenedor, y ahí no existe ningún usuario con ese UID.
- El mismo proceso aparece como PID 528 desde fuera y como PID 1 desde dentro del contenedor. Eso es el namespace de PID.

Prueba de que la API responde por el puerto publicado:

```
$ curl -s -X POST http://localhost:8080/api/alertas -H "Content-Type: application/json" -d '{"sensor":"ESTACION-01-PM25","tipo":"PM2.5","valor":187.4,"nivel":"CRITICO"}'
{"id":1,"nivel":"CRITICO","registradaEn":"2026-10-21T06:18:28.708014003Z","sensor":"ESTACION-01-PM25","tipo":"PM2.5","valor":187.4}
$ curl -s http://localhost:8080/api/alertas
[{"id":1,"nivel":"CRITICO","registradaEn":"2026-10-21T06:18:28.708014Z","sensor":"ESTACION-01-PM25","tipo":"PM2.5","valor":187.4}]
```

### 6.3 Inmutabilidad de runtime (R2.3)

```
$ docker exec eco_air_active touch /etc/crontab_fake
touch: cannot touch '/etc/crontab_fake': Permission denied
$ echo $?
1
$ docker exec eco_air_active ls -ld /etc
drwxr-xr-x 1 root root 4096 Oct 21 06:18 /etc
```

**Lectura:** `/etc` es de root, con permisos 755, y el proceso corre como `sensor_user`. Por eso no puede crear ni modificar archivos del sistema. Ojo: la protección viene de los permisos del usuario, no de un sistema de archivos de solo lectura.

### 6.4 Artefactos de desarrollo (R2.4)

```
$ docker exec eco_air_active which mvn
$ echo $?
1
$ docker exec eco_air_active which javac
$ echo $?
1
$ docker exec eco_air_active find / -name "*.java" 2>/dev/null
$ docker exec eco_air_active ls -la /app
total 51304
drwxr-xr-x 1 sensor_user sensorgroup     4096 Oct 20 21:43 .
drwxr-xr-x 1 root        root            4096 Oct 21 06:18 ..
-rw-r--r-- 1 sensor_user sensorgroup 52524678 Oct 20 21:43 app.jar
```

**Lectura:**

- No hay Maven, no hay compilador (`javac`) y no hay ningún archivo `.java`: solo está el JRE y `app.jar`.
- `/app` y `app.jar` son de `sensor_user`.
- La fecha del jar (21:43 UTC) coincide con el `Finished at: 2026-10-20T21:43:39Z` de Maven en el build.

---

## 7. Reto 3: calibración de cgroups y OOM

### 7.1 Colapso con 48 MB (R3.1)

Como línea base, el contenedor sin límite medía esto:

```
CONTAINER ID   NAME             CPU %     MEM USAGE / LIMIT     MEM %     NET I/O           BLOCK I/O        PIDS
999e5a8efdc0   eco_air_active   0.14%     283.9MiB / 7.376GiB   3.76%     2.85kB / 1.19kB   93.9MB / 463kB   49
```

```
$ docker run -d --name eco_air_crash -m 48m eco-air-backend:1.0.0
88ac69bca7401db6b3fb47780817d138c50f9cf57f19c6f66fa257f615392eec
$ sleep 10
$ docker inspect eco_air_crash --format='Status: {{.State.Status}} | ExitCode: {{.State.ExitCode}} | OOMKilled: {{.State.OOMKilled}}'
Status: exited | ExitCode: 137 | OOMKilled: true
$ docker inspect eco_air_crash --format='Memory: {{.HostConfig.Memory}} | MemorySwap: {{.HostConfig.MemorySwap}}'
Memory: 50331648 | MemorySwap: 100663296
```

Últimas líneas del log antes de morir:

```
2026-10-21T06:33:50.698Z  INFO 1 ... Starting EcoAirApplication v0.0.1-SNAPSHOT using Java 17.0.20.1 with PID 1 (/app/app.jar started by sensor_user in /app)
2026-10-21T06:33:50.789Z  INFO 1 ... No active profile set, falling back to 1 default profile: "default"
2026-10-21T06:33:56.372Z  INFO 1 ... Bootstrapping Spring Data JPA repositories in DEFAULT mode.
```

**Lectura:**

- El código 137 es 128 + 9, o sea la señal SIGKILL que envía el OOM killer del kernel.
- El log se corta cuando Hibernate empieza a cargar, y no aparece ningún `OutOfMemoryError` de Java. Fue el kernel el que mató el proceso, no la JVM.
- `MemorySwap` muestra 96 MiB: como solo se usó `-m`, Docker deja la misma cantidad en swap.

### 7.2 Calibración del límite

Para cada valor se ejecutó lo mismo (función `probar` en el log):

1. `docker rm -f eco_air_active`
2. `docker run -d --name eco_air_active -p 8080:8080 -m <valor> eco-air-backend:1.0.0`
3. `sleep 20`
4. `docker inspect` con el formato del reto
5. `docker logs --timestamps eco_air_active | grep "Started EcoAirApplication"`
6. `docker stats --no-stream eco_air_active`
7. `cat /sys/fs/cgroup/memory.swap.current` dentro del contenedor

| `-m` | Estado | Started in | CPU % | MEM USAGE / LIMIT | MEM % | Swap usado |
|---|---|---|---|---|---|---|
| 512m | running | 4.138 s | 0.12% | 249.8MiB / 512MiB | 48.78% | 0 |
| 384m | running | 3.026 s | 0.12% | 203.6MiB / 384MiB | 53.03% | 0 |
| 256m | running | 2.925 s | 0.13% | 187.4MiB / 256MiB | 73.22% | 0 |
| 224m | running | 3.045 s | 0.13% | 180MiB / 224MiB | 80.38% | 0 |
| 216m | running | 3.64 s | 0.12% | 180.9MiB / 216MiB | 83.73% | 0 |
| **208m** | **running** | **3.529 s** | **0.13%** | **180.6MiB / 208MiB** | **86.85%** | **0** |
| 200m | running | 3.386 s | 0.13% | 171.1MiB / 200MiB | 85.54% | 11194368 B (10.7 MiB) |
| 192m | running | 3.091 s | 0.14% | 170.8MiB / 192MiB | 88.97% | 15519744 B (14.8 MiB) |
| 160m | running | 3.88 s | 0.14% | 132.7MiB / 160MiB | 82.93% | 63975424 B (61.0 MiB) |
| 128m | running | 5.388 s | 0.13% | 101.4MiB / 128MiB | 79.20% | 75276288 B (71.8 MiB) |
| 112m | running | 7.573 s | 0.13% | 100.3MiB / 112MiB | 89.55% | 76632064 B (73.1 MiB) |
| 96m | exited, 137, OOMKilled | — | — | — | — | — |
| 128m con `--memory-swap 128m` (`eco_air_noswap`) | exited, 137, OOMKilled | — | — | — | — | — |

### 7.3 Ergonomía de la JVM

Comando: `docker run --rm [-m 208m] --entrypoint java eco-air-backend:1.0.0 -XX:+PrintFlagsFinal -version`

| Flag | Con `-m 208m` | Sin límite |
|---|---|---|
| `InitialHeapSize` | 109051904 (104 MiB) | 3963617280 (3.69 GiB) |
| `MaxHeapSize` | 109051904 (104 MiB) | 5943328768 (5.54 GiB) |
| `UseSerialGC` | true | false |
| `UseG1GC` | false | true |

**Lectura:**

- Con el límite, la JVM cambia de G1 a Serial. Por eso los hilos (PIDS) bajan de 49 a 31.
- Sin límite, el heap máximo es el 75% de 7.376 GiB.
- Con 208m, el heap máximo es el 50% y no el 75%. Con poca memoria, la JVM usa `MinRAMPercentage`, que vale 50% por defecto, en vez de `MaxRAMPercentage`.

### 7.4 Prueba en caliente con 208m

Se lanzó una ráfaga de 1000 `POST /api/alertas` en segundo plano y se midió durante la ráfaga y después.

| Momento | CPU % | MEM USAGE / LIMIT | MEM % | NET I/O | BLOCK I/O |
|---|---|---|---|---|---|
| Recién arrancado (`Started EcoAirApplication in 4.555 seconds`) | 0.19% | 183.7MiB / 208MiB | 88.32% | 1.17kB / 126B | 66.7MB / 3.13MB |
| Ráfaga, a los 5 s | 56.89% | 201.7MiB / 208MiB | 96.99% | 242kB / 199kB | 75.2MB / 12.8MB |
| Ráfaga, a los 10 s | 33.57% | 192.9MiB / 208MiB | 92.72% | 551kB / 454kB | 75.3MB / 12.8MB |
| 15 s después de la ráfaga | 0.15% | 191.3MiB / 208MiB | 91.99% | 617kB / 509kB | 75.3MB / 12.8MB |
| Final, para la tabla de identidad | 0.13% | 194.1MiB / 208MiB | 93.32% | 619kB / 638kB | 80.9MB / 13MB |

Resultado al terminar:

- Alertas guardadas: 1000.
- Swap usado: 12771328 B (12.2 MiB).
- Estado: `Status: running | ExitCode: 0 | OOMKilled: false`.

### 7.5 Umbral elegido

**208m** es el valor más bajo que cumple las tres condiciones:

1. Arranca bien.
2. Lo hace en menos de 15 s (3.5 a 4.6 s).
3. Se mantiene estable en caliente: en reposo no usa swap, aguanta la ráfaga sin morir y vuelve a estabilizarse entre 92% y 93%.

Los valores más bajos no sirven:

- 200m ya usa swap en reposo.
- 112m arranca, pero vive del swap. La prueba sin swap con 128m muestra que esos valores solo sobreviven gracias a él.

Durante la ráfaga, 208m movió 12.2 MiB al swap, así que el margen es poco. Para producción usaría unos 256m.

---

## 8. Contenedores al cerrar la grabación

```
NAMES            STATUS                           CONTAINER ID
eco_air_active   Up 8 minutes                     68be9e269d97
eco_air_noswap   Exited (137) 49 minutes ago      15307cb65004
eco_air_crash    Exited (137) About an hour ago   88ac69bca740
```
