# 🌿 Guía Rápida - Convenciones de Ramas Veld

## 🚀 Inicio Rápido

### Crear Nueva Rama
```bash
# Usar el script automático (recomendado)
./scripts/create-branch.sh feature mi-nueva-funcionalidad

# O manualmente
git checkout -b feature/mi-nueva-funcionalidad
```

### Validar Rama Actual
```bash
./scripts/validate-branch-name.sh --current
```

## 📋 Formato de Nombres

### Estructura
```
<tipo>/<descripción>
```

### Tipos de Ramas
| Tipo | Descripción | Ejemplo |
|------|-------------|---------|
| `feature/` | Nueva funcionalidad | `feature/dependency-injection-api` |
| `bugfix/` | Corrección de bug | `bugfix/npe-in-container-init` |
| `hotfix/` | Corrección urgente | `hotfix/security-patch-critical` |
| `refactor/` | Refactorización | `refactor/container-initialization` |
| `docs/` | Documentación | `docs/api-reference-update` |
| `test/` | Tests | `test/integration-coverage` |
| `chore/` | Mantenimiento | `chore/dependency-updates` |
| `perf/` | Performance | `perf/memory-optimization` |
| `style/` | Formato | `style/code-formatting` |
| `release/` | Releases | `release/v1.2.0` |

## ✅ Ejemplos Válidos

```bash
# ✅ Correctos
feature/user-authentication-system
bugfix/memory-leak-in-dependency-scanner
hotfix/critical-security-vulnerability
docs/api-documentation-completeness
refactor/container-initialization-logic
test/integration-test-expansion
chore/build-optimization
perf/annotation-processing-speed

# ❌ Incorrectos
MyFeatureBranch (mayúsculas, sin prefijo)
feature update (espacios)
fix/ (descripción vacía)
very-long-branch-name-that-exceeds-sixty-characters-limit
```

## 🛠️ Herramientas Disponibles

### Scripts Automáticos
- **`./scripts/create-branch.sh`** - Crear ramas con formato correcto
- **`./scripts/validate-branch-name.sh`** - Validar nombres de ramas
- **`./scripts/setup-git-hooks.sh`** - Instalar validación automática

### Git Hooks (Automáticos)
- **Pre-commit** - Valida antes de cada commit
- **Pre-push** - Valida antes de hacer push

### GitHub Actions
- **PR Quality Gate** - Valida nombres en pull requests
- **Security Compliance** - Verifica convenciones de commit

## 🔧 Configuración Inicial

### Instalar Git Hooks
```bash
./scripts/setup-git-hooks.sh
```

### Hacer Scripts Ejecutables
```bash
chmod +x scripts/*.sh
```

## 📝 Flujo de Trabajo Recomendado

### 1. Crear Rama
```bash
# Opción 1: Script automático (recomendado)
./scripts/create-branch.sh feature nueva-api --checkout --push

# Opción 2: Manual
git checkout -b feature/nueva-api
git push -u origin feature/nueva-api
```

### 2. Desarrollo
- Hacer commits siguiendo [Conventional Commits](https://www.conventionalcommits.org/)
- Mantener PRs pequeños y enfocados

### 3. Crear Pull Request
- El título se basa en el nombre de la rama
- Agregar contexto en la descripción
- Usar las plantillas de PR creadas

### 4. Review y Merge
- Después del review, squash merge
- La rama se elimina automáticamente

## 🚫 Restricciones

### ❌ No Permitido
- Espacios en nombres
- Caracteres especiales (`!@#$%^&*`)
- Mayúsculas
- Nombres sin prefijo
- Descripciones genéricas (`test`, `update`, `fix`)

### ✅ Reglas de Longitud
- **General**: Máximo 60 caracteres
- **Hotfix**: Máximo 40 caracteres
- **Release**: Formato `vX.Y.Z`

## 🔍 Validación Manual

### Validar Rama Actual
```bash
./scripts/validate-branch-name.sh --current
```

### Validar Rama Específica
```bash
./scripts/validate-branch-name.sh feature/mi-rama
```

### Listar Tipos Permitidos
```bash
./scripts/validate-branch-name.sh --list
```

## 🆘 Solución de Problemas

### Error: "Branch name doesn't follow conventions"
```bash
# Renombrar rama actual
git branch -m nombre-actual nuevo-nombre-válido

# O crear nueva rama y eliminar la anterior
git checkout -b feature/nuevo-nombre
git branch -D nombre-actual
```

### Error: "Hooks not working"
```bash
# Reinstalar hooks
./scripts/setup-git-hooks.sh --force

# Verificar estado
./scripts/setup-git-hooks.sh --status
```

### Error: "Script not found"
```bash
# Verificar que estás en la raíz del proyecto
pwd
# Debe mostrar la carpeta raíz de Veld

# Verificar que los scripts existen
ls scripts/
```

## 📚 Documentación Completa

- **[Convenciones Completas](BRANCH_NAMING_CONVENTIONS.md)** - Documentación detallada
- **[Setup GitHub](../docs/SETUP_BRANCH_PROTECTION.md)** - Configuración de protecciones
- **[Contributing Guidelines](../CONTRIBUTING.md)** - Guías de contribución

## 💡 Tips y Mejores Prácticas

### ✅ Haz
- Usa nombres descriptivos y específicos
- Mantén las ramas pequeñas y enfocadas
- Usa los scripts automáticos cuando sea posible
- Lee las sugerencias de validación

### ❌ Evita
- Nombres genéricos como `feature/test`
- Ramas muy largas o complejas
- Mezclar múltiples cambios en una rama
- Ignorar las validaciones automáticas

## 🎯 Próximos Pasos

1. **Instalar hooks**: `./scripts/setup-git-hooks.sh`
2. **Crear tu primera rama**: `./scripts/create-branch.sh feature/mi-primer-feature`
3. **Explorar workflows**: Revisar los GitHub Actions configurados
4. **Leer documentación**: `docs/BRANCH_NAMING_CONVENTIONS.md`

---

**¡Listo para contribuir al proyecto Veld con ramas bien organizadas!** 🚀

*Para más información, consulta la documentación completa o contacta al equipo de desarrollo.*