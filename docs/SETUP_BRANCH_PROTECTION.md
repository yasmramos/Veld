# 🔧 Guía de Configuración - Sistema de Protección de Ramas

## 📋 Descripción General

Esta guía te ayudará a configurar el sistema completo de protección de ramas para el proyecto Veld Framework en GitHub. El sistema incluye:

- ✅ Protección automática de la rama `main`
- ✅ Verificación de calidad de PRs
- ✅ Security & compliance checks
- ✅ Auto-assignment de reviewers
- ✅ Validación de formato y convenciones

## 🚀 Configuración Rápida

### 1. **Configurar GitHub CLI (Recomendado)**

```bash
# Instalar GitHub CLI si no está instalado
# https://cli.github.com/

# Autenticarse
gh auth login

# Verificar permisos
gh auth status
```

### 2. **Configurar Protecciones de Rama**

#### Opción A: Configuración Manual en GitHub

1. Ir a **Settings** → **Branches** en tu repositorio
2. Click en **"Add rule"**
3. Enter `main` como branch name pattern
4. Seleccionar las siguientes opciones:

```
☑️ Require pull request reviews before merging
   - Required number of reviewers: 1
   ☑️ Dismiss stale PR approvals when new commits are pushed
   ☑️ Require review from Code Owners (opcional)

☑️ Require status checks to pass before merging
   ☑️ Require branches to be up to date before merging
   ☑️ Require conversation resolution before merging

☑️ Restrict pushes that create files larger than 100 MB
☑️ Block force pushes
☑️ Restrict deletions

☑️ Require signed commits
☑️ Require linear history
☐ Allow merge commits (opcional)
☑️ Allow squash merging (recomendado)
☐ Allow rebase merging (opcional)
```

#### Opción B: Configuración Automática con CLI

```bash
# Configurar protección usando GitHub CLI
gh api repos/$GITHUB_REPOSITORY/branches/main/protection \
  --method PUT \
  --field required_status_checks='{"strict":true,"contexts":["pr-quality-gate","security-compliance","code-quality"]}' \
  --field enforce_admins=true \
  --field required_pull_request_reviews='{"required_approving_review_count":1,"dismiss_stale_reviews":true}' \
  --field restrictions='{"users":[],"teams":[],"apps":[]}' \
  --field required_linear_history=true \
  --field allow_force_pushes=false \
  --field allow_deletions=false
```

### 3. **Configurar GPG Signing**

```bash
# Configurar GPG signing globalmente
git config --global user.signingkey YOUR_GPG_KEY_ID
git config --global commit.gpgsign true

# Para proyectos específicos
git config commit.gpgsign true
```

### 4. **Configurar Labels (Opcional pero Recomendado)**

Crear los siguientes labels en el repositorio:

```bash
# Labels de tipo
gh label create "feature" --description "New functionality" --color "0366d6"
gh label create "bugfix" --description "Bug fix" --color "d73a4a"
gh label create "hotfix" --description "Urgent fix" --color "b31d28"
gh label create "refactor" --description "Code refactoring" --color "fbca04"
gh label create "documentation" --description "Documentation changes" --color "0075ca"
gh label create "test" --description "Test updates" --color "c5def5"
gh label create "build" --description "Build/CI changes" --color "fbca04"

# Labels de prioridad
gh label create "priority:high" --description "High priority" --color "b31d28"
gh label create "priority:medium" --description "Medium priority" --color "fbca04"
gh label create "priority:low" --description "Low priority" --color "28a745"

# Labels de estado
gh label create "triage" --description "Needs initial review" --color "d73a4a"
gh label create "in-review" --description "Currently under review" --color "fbca04"
gh label create "approved" --description "Ready to merge" --color "28a745"
```

## 📊 Configuración de Workflows

### Workflows Automáticos Incluidos

1. **`pr-quality-gate.yml`**: Verifica calidad de PRs
2. **`security-compliance.yml`**: Security y compliance checks
3. **`code-quality.yml`**: Code quality y tests
4. **`benchmark.yml`**: Performance benchmarks (existente)

### Verificar Workflows

```bash
# Listar workflows
gh workflow list

# Ver status de workflows
gh run list --limit 10
```

## 🧪 Testing de la Configuración

### Test 1: Crear PR de Prueba

```bash
# Crear branch de prueba
git checkout -b test-branch-protection

# Hacer un cambio pequeño
echo "# Test" >> README.md

# Commit y push
git add .
git commit -m "test: verify branch protection"
git push origin test-branch-protection

# Crear PR via CLI
gh pr create --title "test: verify branch protection" --body "Testing branch protection setup"
```

### Test 2: Verificar Protecciones

```bash
# Verificar protecciones de rama
gh api repos/$GITHUB_REPOSITORY/branches/main/protection

# Verificar status checks
gh pr checks
```

## 🔧 Configuración Avanzada

### Code Owners

Crear archivo `.github/CODEOWNERS`:

```bash
# Global code owners
* @YasMRamos

# Java code owners
*.java @YasMRamos
**/src/main/java/ @YasMRamos

# Documentation owners
*.md @YasMRamos
docs/ @YasMRamos

# CI/CD owners
.github/ @YasMRamos
```

### Configuración de Dependabot

Crear `.github/dependabot.yml`:

```yaml
version: 2
updates:
  - package-ecosystem: "maven"
    directory: "/"
    schedule:
      interval: "weekly"
      day: "monday"
    open-pull-requests-limit: 5
    reviewers:
      - "YasMRamos"
```

## 🚨 Resolución de Problemas

### Problema: "PR requires approvals"

**Solución:**
1. Asignar reviewers manualmente: `gh pr edit PR_NUMBER --add-reviewers USERNAME`
2. O configurar auto-assignment en `.github/CODEOWNERS`

### Problema: "Status checks failed"

**Solución:**
1. Verificar que todos los workflows estén habilitados
2. Revisar logs de los failed checks
3. Corregir issues identificados

### Problema: "Unsigned commits"

**Solución:**
1. Configurar GPG signing
2. Re-firmar commits: `git rebase --exec 'git commit --amend --no-edit -n -S' -i --root`

### Problema: "Merge commits not allowed"

**Solución:**
1. Usar `git rebase` en lugar de `git merge`
2. Squash commits antes del merge

## 📈 Monitoreo y Métricas

### Configurar GitHub Insights

1. Ir a **Insights** → **Dependency graph**
2. Configurar alerts para vulnerabilities
3. Habilitar Dependabot security updates

### Configurar Notifications

```bash
# Configurar webhooks para monitoreo
gh webhook create --repo $GITHUB_REPOSITORY \
  --events push,pull_request,pull_request_review \
  --endpoint https://your-webhook-endpoint.com/webhook
```

## 📞 Soporte

### Documentación Adicional
- [GitHub Branch Protection Rules](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/defining-the-mergeability-of-pull-requests/managing-a-branch-protection-rule)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Conventional Commits](https://www.conventionalcommits.org/)

### Contacto
- **Issues**: Usar GitHub Issues para reportar problemas
- **Discussions**: Usar GitHub Discussions para preguntas generales

---

**¡Listo!** 🎉 Tu repositorio ahora tiene protección completa de ramas con verificación automática de calidad.

*Última actualización: 2025-12-21*