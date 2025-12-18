#!/bin/bash
# Script para configurar Maven correctamente en CI/CD
# Resuelve problemas de settings-security.xml

set -e

echo "🔧 Configuring Maven for CI/CD..."

# Crear directorio .m2 si no existe
mkdir -p ~/.m2

# Configurar variables de entorno críticas
export MAVEN_OPTS="${MAVEN_OPTS:--Xmx2g -XX:+UseG1GC -Dmaven.settings.security.skip=true -Dmaven.security.skip=true}"
export MAVEN_CONFIG="${MAVEN_CONFIG:---no-transfer-progress --batch-mode}"
export MAVEN_USER_HOME="${MAVEN_USER_HOME:-$HOME/.m2}"
export MAVEN_HOME="${MAVEN_HOME:-$HOME/.m2}"

# Crear settings.xml básico sin requerimientos de seguridad
cat > ~/.m2/settings.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 
                              http://maven.apache.org/xsd/settings-1.0.0.xsd">
  
  <localRepository>${user.home}/.m2/repository</localRepository>
  
  <profiles>
    <profile>
      <id>default</id>
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
      <repositories>
        <repository>
          <id>central</id>
          <name>Central Repository</name>
          <url>https://repo1.maven.org/maven2</url>
          <layout>default</layout>
          <snapshots>
            <enabled>false</enabled>
          </snapshots>
        </repository>
      </repositories>
      <pluginRepositories>
        <pluginRepository>
          <id>central</id>
          <name>Central Repository</name>
          <url>https://repo1.maven.org/maven2</url>
          <layout>default</layout>
          <snapshots>
            <enabled>false</enabled>
          </snapshots>
        </pluginRepository>
      </pluginRepositories>
    </profile>
  </profiles>
  
  <activeProfiles>
    <activeProfile>default</activeProfile>
  </activeProfiles>
</settings>
EOF

# Verificar configuración
echo "✅ Maven Configuration:"
echo "   MAVEN_OPTS: $MAVEN_OPTS"
echo "   MAVEN_CONFIG: $MAVEN_CONFIG"
echo "   MAVEN_USER_HOME: $MAVEN_USER_HOME"
echo "   MAVEN_HOME: $MAVEN_HOME"

# Mostrar versión de Maven
echo "✅ Maven Version:"
mvn --version

# Verificar settings.xml
echo "✅ Settings.xml created:"
ls -la ~/.m2/settings.xml

echo "🔧 Maven CI/CD configuration completed successfully!"