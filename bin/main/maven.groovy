/*
	forma de invocación de método call:
	def ejecucion = load 'script.groovy'
	ejecucion.call()
*/
def call(){
  stage("Paso 1: Compilar con Maven"){
    sh "mvn clean compile -e"
  }

  stage("Paso 2: Testear con Maven"){
    sh "mvn clean test -e"
  }

  stage("Paso 3: Build .Jar con Maven"){
    sh "mvn clean package -e"
  }

  stage("Paso 4: Sonarqube - Análisis Estático"){
      sh "echo 'Análisis Estático!'"
      withSonarQubeEnv('sonarqube') {
          sh 'mvn clean verify sonar:sonar -Dsonar.projectKey=pipeline-iclab-prueba -Dsonar.java.binaries=build'
      }
  }

  stage("Paso 5: Curl Springboot con Maven durmiendo 20 segundos"){
      sh 'mvn spring-boot:run &'
      sh "sleep 20 && curl -X GET 'http://localhost:8081/rest/mscovid/test?msg=testing'"
  }

  stage("Paso 6: Subir Nexus"){
      nexusPublisher nexusInstanceId: 'nexus',
      nexusRepositoryId: 'pipeline-iclab-prueba',
      packages: [
          [$class: 'MavenPackage',
              mavenAssetList: [
                  [classifier: '',
                  extension: 'jar',
                  filePath: 'build/IClabPrueba-0.0.1.jar'
              ]
          ],
              mavenCoordinate: [
                  artifactId: 'IClabPrueba',
                  groupId: 'com.devopsusach2020',
                  packaging: 'jar',
                  version: '0.0.1'
              ]
          ]
      ]
  }
  stage("Paso 7: Descargar desde Nexus"){
      sh ' curl -X GET -u $NEXUS_USER:$NEXUS_PASSWORD "http://nexus:8081/repository/pipeline-iclab-prueba/com/devopsusach2020/IClabPrueba/0.0.1/IClabPrueba-0.0.1.jar" -O'
  }
  stage("Paso 8: Levantar Artefacto Jar"){
      sh 'nohup bash java -jar IClabPrueba-0.0.1.jar & >/dev/null'
  }
  stage("Paso 9: Testear Artefacto durmiendo 20 segundos"){
      sh "sleep 20 && curl -X GET 'http://localhost:8081/rest/mscovid/test?msg=testing'"
  }
}
return this;