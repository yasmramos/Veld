# Release Checklist - Veld Framework

## Preparación Pre-Release

### 1. Verificación de Código
- [ ] Todos los tests pasan localmente: `mvn clean test`
- [ ] Cobertura de tests >= 80%
- [ ] No hay errores de compilación
- [ ] Documentación actualizada
- [ ] CHANGELOG.md actualizado con los cambios de esta versión

### 2. Verificación de Versiones
- [ ] Versión en pom.xml actualizada (sin -SNAPSHOT)
- [ ] Versión consistente entre todos los módulos
- [ ] Tags de Git actualizados

### 3. Secrets de GitHub Actions Configurados
- [ ] `GPG_PRIVATE_KEY` - Clave privada GPG codificada en base64
- [ ] `GPG_PASSPHRASE` - Passphrase de la clave GPG
- [ ] `GPG_KEYNAME` - ID o email de la clave GPG
- [ ] `SONATYPE_USERNAME` - Usuario de Sonatype/Jira
- [ ] `SONATYPE_TOKEN` - Token de acceso a Sonatype

### 4. Verificación de GPG
```bash
# Verificar que la clave funciona
echo "$GPG_PRIVATE_KEY" | base64 -d | gpg --import
gpg --list-keys
```

### 5. Verificación de Sonatype
- [ ] Cuenta de Sonatype creada y verificada
- [ ] Permisos de deploy para el groupId `io.github.yasmramos`
- [ ] 2FA habilitado en la cuenta

## Ejecución del Release

### 1. Crear Tag de Release
```bash
# Actualizar versión a release (sin -SNAPSHOT)
mvn versions:set -DnewVersion=1.0.0
git add -A
git commit -m "Release 1.0.0"
git push origin main

# Crear tag
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0
```

### 2. Monitorear Workflow
- Ir a: https://github.com/yasmramos/Veld/actions/workflows/release.yml
- Verificar que el workflow se ejecuta correctamente
- Revisar logs en caso de errores

### 3. Verificar Deployment a Sonatype
- Ir a: https://s01.oss.sonatype.org/
- Login con cuenta de Sonatype
- Ir a "Staging Repositories"
- Verificar que el repositorio de staging fue creado
- Cerrar (Close) el repositorio de staging
- Verificar los artefactos subidos

## Post-Release

### 1. Verificación en Maven Central
- [ ] Buscar en: https://search.maven.org/
- Verificar que los artefactos aparecen
- Verificar que la documentación (Javadoc) está disponible

### 2. Actualizar para Próxima Versión
```bash
# Actualizar a siguiente versión SNAPSHOT
mvn versions:set -DnewVersion=1.0.1-SNAPSHOT
git add -A
git commit -m "Bump to 1.0.1-SNAPSHOT"
git push origin main
```

### 3. Anunciar el Release
- [ ] Crear release en GitHub
- [ ] Actualizar README.md con nueva versión
- [ ] Anunciar en redes sociales/blog

## Solución de Problemas Comunes

### Error: GPG Key Import Failed
- Verificar que `GPG_PRIVATE_KEY` está codificado correctamente en base64
- Verificar que no hay caracteres extra o saltos de línea
- Regenerar el secret desde el archivo original

### Error: Sonatype Authentication Failed
- Verificar que `SONATYPE_USERNAME` y `SONATYPE_TOKEN` son correctos
- Regenerar token de Sonatype si es necesario
- Verificar permisos del token

### Error: Missing central Server
- Verificar que settings.xml incluye el servidor `central`
- Verificar que el ID coincide con `publishingServerId` en pom.xml

## Comandos Útiles

```bash
# Verificar build local
mvn clean verify

# Solo ejecutar tests
mvn test

# Generar Javadoc
mvn javadoc:javadoc

# Verificar dependencias vulnerables
mvn org.owasp:dependency-check-maven:check

# Ver estructura del proyecto
mvn depgraph:graph
```
