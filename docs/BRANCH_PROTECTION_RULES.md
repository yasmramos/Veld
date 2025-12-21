# 🔒 Reglas de Protección de Ramas - Veld Framework

## 📋 Resumen de Configuración

Este documento define las reglas de protección para la rama `main` del proyecto Veld Framework. Estas reglas aseguran que todos los cambios pasen por el proceso de pull request y revisión antes de ser integrados.

## 🎯 Objetivos

- ✅ Garantizar revisión de código obligatoria
- ✅ Prevenir merges accidentales sin revisión
- ✅ Mantener la calidad del código en la rama principal
- ✅ Proteger contra regresiones y cambios no autorizados

## 🔧 Configuración Requerida en GitHub

### 1. **Protección de la Rama `main`**

#### Configuración Básica
- **Rama protegida**: `main`
- **Restricciones de push**: Solo vía Pull Request
- **Aplicar a administradores**: ✅ SÍ

#### Reglas Requeridas
- [x] **Require pull request reviews before merging**
  - **Mínimo de aprobaciones**: 1
  - **Requerir revisión de los owners del código**: Opcional
  - **Dismiss stale PR approvals when new commits are pushed**: ✅ SÍ

- [x] **Require status checks to pass before merging**
  - **Require branches to be up to date before merging**: ✅ SÍ
  - **Status checks requeridos**:
    - `Build and Test`
    - `Code Quality Checks`
    - `Security Scan`
    - `License Check`

- [x] **Require conversation resolution before merging**
  - Todos los comentarios del PR deben ser resueltos antes del merge

- [x] **Require signed commits**
  - Todos los commits deben estar firmados con GPG

- [x] **Require linear history**
  - Solo merges squash están permitidos

### 2. **Reglas Adicionales para PRs**

#### **Tamaños de PR**
- [x] **PR Large**: > 400 líneas → Requerir 2+ aprobaciones
- [x] **PR Medium**: 100-400 líneas → Requerir 1+ aprobación
- [x] **PR Small**: < 100 líneas → 1 aprobación mínima

#### **Etiquetas Requeridas**
- [x] **Al menos una etiqueta de tipo**:
  - `feature`
  - `bugfix`
  - `hotfix`
  - `refactor`
  - `documentation`
  - `test`
  - `build`

- [x] **Al menos una etiqueta de prioridad**:
  - `priority:high`
  - `priority:medium`
  - `priority:low`

#### **Asignación de Reviewers**
- [x] **Auto-assign**: Al menos 1 reviewer automáticamente
- [x] **Team assignment**: Asignar al equipo de desarrollo para PRs grandes

## 📋 Checklist para Configuración Manual

### En GitHub → Settings → Branches

1. **Agregar regla de rama**
   - Nombre: `main`
   - ☑️ Require pull request reviews before merging
     - Required number of reviewers: 1
     - ☑️ Dismiss stale PR approvals when new commits are pushed
     - Require review from Code Owners: (Opcional)

2. **Status checks**
   - ☑️ Require branches to be up to date before merging
   - ☑️ Require status checks to pass before merging
   - ☑️ Require conversation resolution before merging

3. **Restricciones**
   - ☑️ Restrict pushes that create files larger than 100 MB
   - ☑️ Block force pushes
   - ☑️ Restrict deletions

4. **Configuración de merge**
   - ☑️ Require signed commits
   - ☑️ Require linear history
   - ☑️ Allow merge commits (Opcional)
   - ☑️ Allow squash merging (Recomendado)
   - ☑️ Allow rebase merging (Opcional)

## 🚀 Configuración Automática (Opcional)

### GitHub Actions para Enforcement

Si prefieres enforcement automático, estas acciones pueden ayudar:

1. **PR Label Enforcement**: Verifica que los PRs tengan las etiquetas requeridas
2. **Size-based Review**: Automáticamente asigna reviewers adicionales para PRs grandes
3. **Quality Gates**: Ejecuta verificaciones adicionales antes del merge

## 🔄 Proceso de Actualización

### Para Cambios a las Reglas
1. Crear PR con cambios a este documento
2. Revisar con el equipo de desarrollo
3. Aplicar cambios en GitHub Settings
4. Comunicar cambios al equipo

### Para Nuevas Ramas de Release
- Aplicar las mismas reglas a ramas de release (ej: `release/v1.x.x`)
- Permitir excepciones solo para hotfixes urgentes

## ⚠️ Excepciones Permitidas

### Hotfixes de Emergencia
- **Situación**: Bug crítico en producción
- **Proceso**: 
  1. Crear branch `hotfix/nombre-del-fix`
  2. PR directo con aprobación de emergencia
  3. Merge inmediato después de revisión rápida
- **Documentación**: Documentar la emergencia en el PR

### Actualizaciones de Dependencias de Seguridad
- **Situación**: Actualizaciones críticas de seguridad
- **Proceso**: Proceso acelerado con 1 aprobación mínima

## 📊 Métricas y Monitoreo

### KPIs a Monitorear
- **Tiempo promedio de review**: < 24 horas
- **PRs rechazados**: < 10% del total
- **Rollbacks**: 0 en producción
 reglas- **Compliance de**: 100%

### Reportes Mensuales
- Resumen de PRs procesados
- Tiempo promedio de merge
- Violaciones de reglas (si las hay)
- Mejoras sugeridas al proceso

## 🎓 Entrenamiento del Equipo

### Nuevos Miembros
- [ ] Orientación sobre el proceso de PR
- [ ] Configuración de GPG signing
- [ ] Uso de Git CLI para mejores prácticas
- [ ] Review de este documento

### Recordatorios Periódicos
- [ ] Refresher mensual sobre reglas
- [ ] Mejores prácticas de code review
- [ ] Actualizaciones de herramientas

## 📞 Contacto

Para preguntas sobre las reglas de protección:
- **Maintainer**: YasMRamos
- **Team**: Veld Core Team
- **Issues**: Usar GitHub Issues para discutir cambios

---

*Última actualización: 2025-12-21*
*Versión del documento: 1.0*