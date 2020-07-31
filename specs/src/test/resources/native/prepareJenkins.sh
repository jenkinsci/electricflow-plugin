JENKINS_HOST=docker-host
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_251.jdk/Contents/Home
JENKINS_CLI="http://$JENKINS_HOST:8080/jnlpJars/jenkins-cli.jar"
JENKINS_CONFIG_FILE=jenkinsProjectsConfig.zip
JENKINS_CONFIG="gs://flow-plugins-ec-jenkins-configurations/$JENKINS_CONFIG_FILE"
PLUGINS_LIST=<<
https://updates.jenkins.io/latest/thinBackup.hpi
PLUGINS_LIST

# Download CLI
curl -O jenkins-cli.jar $JENKINS_CLI

# Install plugins
for x in $PLUGINS_LIST
do
  java -jar jenkins-cli.jar -auth admin:changeme install-plugin $x
done

# Download archive from bucket to the machine
ssh $JENKINS_HOST "gsutil -m cp ${JENKINS_CONFIG} /tmp/$JENKINS_CONFIG_FILE"

# Copy to a docker container
ssh $JENKINS_HOST "unzip /tmp/$JENKINS_CONFIG_FILE -d /tmp/"

# Restore the backup
ssh $JENKINS_HOST "docker exec -it mkdir /tmp/"

# PROFIT!