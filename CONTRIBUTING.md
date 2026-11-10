# Guía de contribución — Grupo 9

Este documento describe **cómo cada integrante debe contribuir** para que su participación quede
evidenciada en el historial de commits del repositorio, tal como exige la actividad:

> «La participación de los integrantes deberá evidenciarse mediante el historial de commits del
> repositorio» y «cada integrante deberá evidenciar participación mediante contribuciones
> identificables en GitHub».

Un commit solo se atribuye a un integrante si fue creado con **su nombre y su correo vinculados a
su cuenta de GitHub**. Los commits hechos con la cuenta de otra persona no cuentan para quien
realmente trabajó.

---

## 1. Coordinador: crear el repositorio y habilitar al grupo

1. GitHub → **New repository**.
2. Nombre: `ProgAv-TG1-Grupo9`. Visibilidad: **Public**. Sin README ni `.gitignore`.
3. **Settings → Collaborators → Add people** → agregar a los otros tres integrantes con rol **Write**.
4. Copiar la URL HTTPS del repositorio (`https://github.com/sgsanchez/ProgAv-TG1-Grupo9.git`)
   y compartirla en el chat del grupo.

> El repositorio debe seguir siendo **público** y accesible sin solicitar permisos: esa es una
> condición explícita de la rúbrica.

## 2. Cada integrante: preparar su cuenta y su equipo

1. Iniciar sesión en GitHub con **su propia cuenta** y verificar el correo.
2. Aceptar la invitación al repositorio (llega por correo o aparece en la página del repo).
3. Instalar Git en su equipo: <https://git-scm.com/download/win>
   (o `winget install --id Git.Git -e`).
4. Configurar su identidad **en su propio equipo**:

```powershell
git config --global user.name  "Nombre Apellido"
git config --global user.email "correo@espe.edu.ec"
```

El correo debe estar registrado en **GitHub → Settings → Emails**. Si se prefiere no exponer el
correo real, usar la dirección `noreply` que GitHub asigna:

```powershell
# Reemplazar ID y usuario por los que muestra GitHub en Settings → Emails
git config --global user.email "12345678+usuario@users.noreply.github.com"
```

Verificar que quedó bien configurado:

```powershell
git config --global user.name
git config --global user.email
```

> Si el correo no pertenece a la cuenta de GitHub, el commit aparece como «autor desconocido» y no
> se refleja en *Insights → Contributors*.

## 3. Clonar el repositorio

```powershell
git clone https://github.com/sgsanchez/ProgAv-TG1-Grupo9.git
cd ProgAv-TG1-Grupo9
git checkout develop
```

## 4. Trabajar en una rama propia

| Rama | Responsable | Contenido |
|---|---|---|
| `feature/clientes` | Josué Muñoz | Entidad, CRUD y reglas de Cliente |
| `feature/ventas` | Santiago Sánchez | Venta, DetalleVenta, stock y totales |
| `feature/transversales` | Steven Sandoval | Interceptor, aspecto y excepciones |
| `feature/infra-docs` | Katherine Vargas | CI/CD, pruebas de integración e informes |

```powershell
git checkout -b feature/clientes develop

# ... desarrollo del código ...

git add .
git commit -m "feat(clientes): agrega entidad Cliente con validaciones"
git push -u origin feature/clientes
```

Después, abrir un **Pull Request** de la rama hacia `develop`. El PR deja además un registro visible
de autoría y revisión.

## 5. Verificar que el nombre quedó registrado

```powershell
git log --format="%h | %an <%ae> | %s"
```

Y en GitHub:

- En **Commits**, cada commit debe mostrar el **avatar y nombre de usuario** del integrante.
- En **Insights → Contributors** deben aparecer los cuatro integrantes.

## 6. Alternativa sin instalar Git

Si algún integrante no puede instalar Git, puede contribuir desde el navegador:

1. Abrir el repositorio en GitHub → entrar a un archivo → ✏️ **Edit**.
2. Redactar el mensaje de commit y pulsar **Commit changes**.

Ese commit queda automáticamente atribuido a su cuenta. También puede usarse **GitHub Desktop**.

---

## Reglas del grupo

- Mínimo **3 o 4 commits significativos por integrante**, en **fechas distintas** (no todos el
  último día).
- Mensajes de commit descriptivos con prefijo: `feat:`, `fix:`, `docs:`, `test:`, `chore:`, `ci:`.
- Nunca hacer commits con la cuenta de otro integrante.
- Antes de entregar: abrir el repositorio en **ventana de incógnito** y confirmar que se ve sin
  iniciar sesión.
