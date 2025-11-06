# Application Java Web - Maven + Tomcat + Docker

Ceci est un guide pour installer et déployer une application Java Web sur srveur Ubuntu.

---

## 🔧 Prérequis

- Ubuntu 20.04+ ou Debian 10+
- Accès root ou sudo
- Connexion Internet
- Au moins 2 GB de RAM
- 25 GB d'espace disque disponible

---

## 🐧 Installation sur Ubuntu

### Étape 1 : Mise à jour du système

```bash
sudo apt update
sudo apt upgrade -y
```

---

### Étape 2 : Installation de Java

**Installer OpenJDK 11**

```bash
sudo apt install -y openjdk-11-jdk
```

**Vérifier l'installation**

```bash
java -version
javac -version
```

**Configurer JAVA_HOME**

```bash
echo "export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64" >> ~/.bashrc
echo "export PATH=\$PATH:\$JAVA_HOME/bin" >> ~/.bashrc
source ~/.bashrc
```

---

### Étape 3 : Installation de Maven

```bash
sudo apt install -y maven
mvn -version
```

---

### Étape 4 : Installation de Tomcat 9

**Créer un utilisateur dédié**

```bash
sudo useradd -r -m -U -d /opt/tomcat -s /bin/false tomcat
```

**Télécharger Tomcat**

```bash
TOMCAT_VERSION=9.0.82
cd /tmp
wget https://archive.apache.org/dist/tomcat/tomcat-9/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz
```

**Extraire et installer**

```bash
sudo tar -xzf apache-tomcat-${TOMCAT_VERSION}.tar.gz -C /opt/tomcat --strip-components=1
```

**Donner les permissions**

```bash
sudo chown -R tomcat: /opt/tomcat
sudo chmod -R u+x /opt/tomcat/bin
```

---

### Étape 5 : Configuration de Tomcat

**Configurer les utilisateurs** (`/opt/tomcat/conf/tomcat-users.xml`)

```bash
sudo tee /opt/tomcat/conf/tomcat-users.xml > /dev/null << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<tomcat-users xmlns="http://tomcat.apache.org/xml"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://tomcat.apache.org/xml tomcat-users.xsd"
              version="1.0">

  <role rolename="manager-gui"/>
  <role rolename="manager-script"/>
  <role rolename="manager-jmx"/>
  <role rolename="manager-status"/>
  <role rolename="admin-gui"/>
  <role rolename="admin-script"/>
  
  <user username="admin"
        password="SecurePassword123!"
        roles="manager-gui,manager-script,manager-jmx,manager-status,admin-gui,admin-script"/>
  
  <user username="deployer"
        password="DeployPass123!"
        roles="manager-script"/>

</tomcat-users>
EOF
```

**Autoriser l'accès distant au Manager**

```bash
sudo tee /opt/tomcat/webapps/manager/META-INF/context.xml > /dev/null << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<Context antiResourceLocking="false" privileged="true" >
  <CookieProcessor className="org.apache.tomcat.util.http.Rfc6265CookieProcessor"
                   sameSiteCookies="strict" />
  <!-- Accès distant autorisé -->
  <Manager sessionAttributeValueClassNameFilter="java\.lang\.(?:Boolean|Integer|Long|Number|String)|org\.apache\.catalina\.filters\.CsrfPreventionFilter\$LruCache(?:\$1)?|java\.util\.(?:Linked)?HashMap"/>
</Context>
EOF
```

---

### Étape 6 : Créer un service systemd pour Tomcat

```bash
sudo tee /etc/systemd/system/tomcat.service > /dev/null << 'EOF'
[Unit]
Description=Apache Tomcat Web Application Container
After=network.target

[Service]
Type=forking

User=tomcat
Group=tomcat

Environment="JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64"
Environment="JAVA_OPTS=-Djava.security.egd=file:///dev/urandom -Djava.awt.headless=true"
Environment="CATALINA_BASE=/opt/tomcat"
Environment="CATALINA_HOME=/opt/tomcat"
Environment="CATALINA_PID=/opt/tomcat/temp/tomcat.pid"
Environment="CATALINA_OPTS=-Xms512M -Xmx1024M -server -XX:+UseParallelGC"

ExecStart=/opt/tomcat/bin/startup.sh
ExecStop=/opt/tomcat/bin/shutdown.sh

RestartSec=10
Restart=always

[Install]
WantedBy=multi-user.target
EOF
```

**Démarrer Tomcat**

```bash
sudo systemctl daemon-reload
sudo systemctl start tomcat
sudo systemctl enable tomcat
```

**Vérifier le statut**

```bash
sudo systemctl status tomcat
```

---

### Étape 7 : Configuration du firewall

```bash
sudo ufw allow 8080/tcp
sudo ufw allow 22/tcp
```

---

## ⚙️ Configuration Maven

### Créer le fichier settings.xml

```bash
mkdir -p ~/.m2

tee ~/.m2/settings.xml > /dev/null << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
          http://maven.apache.org/xsd/settings-1.0.0.xsd">
    
    <servers>
        <server>
            <id>TomcatServer</id>
            <username>deployer</username>
            <password>DeployPass123!</password>
        </server>
    </servers>
    
</settings>
EOF
```

---

## 🏗️ Build et Déploiement

### Cloner le repository

```bash
git clone https://github.com/sdkhili/maven.git
cd maven
```

### Commandes Maven

**Compiler le projet**

```bash
mvn clean compile
```

**Créer le package WAR**

```bash
mvn clean package
```

**Déployer sur Tomcat**

```bash
mvn tomcat7:deploy
```

**Re-déployer (mise à jour)**

```bash
mvn tomcat7:redeploy
```

---

## 🌐 Accès à l'application

- **Application** : http://localhost:8080/maven
- **Tomcat Manager** : http://localhost:8080/manager/html
- **Credentials** :
  - Username: `admin`
  - Password: `SecurePassword123!`

---

## 🐳 Conteneurisation avec Docker

### Créer le Dockerfile

```dockerfile
# Stage 1 : Build avec Maven
FROM maven:3.9-eclipse-temurin-11 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2 : Runtime avec Tomcat
FROM tomcat:9.0-jdk11-temurin
LABEL maintainer="dkhili.saber@gmail.com"

# Supprimer les apps par défaut
RUN rm -rf /usr/local/tomcat/webapps/*

# Copier le WAR
COPY --from=builder /app/target/*.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]
```

### Créer .dockerignore

```
target/
.git/
.gitignore
*.md
.DS_Store
.idea/
*.iml
.vscode/
```

### Commandes Docker

**Build l'image**

```bash
docker build -t maven-app:latest .
```

**Lancer le conteneur**

```bash
docker run -d -p 8080:8080 --name maven-app maven-app:latest
```

**Voir les logs**

```bash
docker logs -f maven-app
```

**Arrêter le conteneur**

```bash
docker stop maven-app
docker rm maven-app
```

---

## 📚 Commandes Utiles

### Gestion de Tomcat

```bash
# Démarrer
sudo systemctl start tomcat

# Arrêter
sudo systemctl stop tomcat

# Redémarrer
sudo systemctl restart tomcat

# Voir le statut
sudo systemctl status tomcat

# Voir les logs
sudo tail -f /opt/tomcat/logs/catalina.out
```

### Gestion Maven

```bash
# Voir la version
mvn -version

# Nettoyer le projet
mvn clean

# Compiler
mvn compile

# Tester
mvn test

# Packager
mvn package

# Build complet
mvn clean package
```

### Gestion Docker

```bash
# Lister les conteneurs
docker ps

# Lister les images
docker images

# Entrer dans le conteneur
docker exec -it maven-app bash

# Supprimer un conteneur
docker rm -f maven-app

# Supprimer une image
docker rmi maven-app:latest
```

---

## 🔍 Troubleshooting

### Port 8080 déjà utilisé

```bash
# Trouver le processus
sudo lsof -i :8080

# Tuer le processus
sudo kill -9 <PID>
```

### Tomcat ne démarre pas

```bash
# Voir les logs
sudo journalctl -u tomcat -n 50

# Vérifier JAVA_HOME
echo $JAVA_HOME
```

### Erreur de déploiement Maven

```bash
# Vérifier que Tomcat est démarré
sudo systemctl status tomcat

# Vérifier les credentials
cat ~/.m2/settings.xml

# Vérifier l'URL du manager
curl http://localhost:8080/manager/text/list
```

---

## 📦 Structure du Projet

```
maven/
├── pom.xml
├── Dockerfile
├── .dockerignore
├── README.md
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   ├── resources/
│   │   └── webapp/
│   │       ├── WEB-INF/
│   │       └── index.jsp
│   └── test/
└── target/
    └── maven.war
```

---

## 🔐 Sécurité

⚠️ **IMPORTANT** : Changez les mots de passe par défaut en production !

```bash
# Modifier les credentials Tomcat
sudo nano /opt/tomcat/conf/tomcat-users.xml

# Redémarrer Tomcat
sudo systemctl restart tomcat
```

**Bonnes pratiques** :
- Utilisez des mots de passe forts
- Limitez l'accès au Manager Tomcat
- Activez HTTPS en production
- Mettez à jour régulièrement Java et Tomcat
- Utilisez un utilisateur non-root

---

## 📖 Ressources

- [Maven Documentation](https://maven.apache.org/guides/)
- [Tomcat Documentation](https://tomcat.apache.org/tomcat-9.0-doc/)
- [Docker Documentation](https://docs.docker.com/)
- [Repository GitHub](https://github.com/sdkhili/maven/)

---

## 👤 Auteur

**Saber Dkhili**
- GitHub: [@sdkhili](https://github.com/sdkhili)
- Email: dkhili.saber@gmail.com

---

**Dernière mise à jour** : Novembre 2025
