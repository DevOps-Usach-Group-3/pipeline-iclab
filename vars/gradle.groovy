/*
	forma de invocación de método call:
	def ejecucion = load 'script.groovy'
	ejecucion.call()
*/
def call(){
    stage("Paso 0: Definiendo el ambiente"){
        sh "printenv"
        if(env.GIT_BRANCH == "origin/feature-ellery"){
            sh 'echo "Se esta compilando el feature de Ellery"'
        }else{
            sh 'echo "otro branch"'
        }
    }

    stage("Paso 1: Build && Test"){
        echo "${env.GIT_BRANCH}"
        sh "gradle clean build"
    }

    return this;

    stage("Paso 2: Sonar - Análisis Estático"){
        sh "echo 'Análisis Estático!'"
        withSonarQubeEnv('sonarqube') {
            sh './gradlew sonarqube -Dsonar.projectKey=pipeline-iclab-prueba -Dsonar.java.binaries=build'
        }
    }
    stage("Paso 3: Curl Springboot con Gradle durmiendo 20 segundos"){
        sh "gradle bootRun&"
        sh "sleep 20 && curl -X GET 'http://localhost:8081/rest/mscovid/test?msg=testing'"
    }
    stage("Paso 4: Subir Nexus"){
        nexusPublisher nexusInstanceId: 'nexus',
        nexusRepositoryId: 'pipeline-iclab-prueba',
        packages: [
            [$class: 'MavenPackage',
                mavenAssetList: [
                    [classifier: '',
                    extension: 'jar',
                    filePath: 'build/libs/IClabPrueba-0.0.1.jar'
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
    stage("Paso 5: Descargar Nexus"){
        sh ' curl -X GET -u $NEXUS_USER:$NEXUS_PASSWORD "http://nexus:8081/repository/pipeline-iclab-prueba/com/devopsusach2020/IClabPrueba/0.0.1/IClabPrueba-0.0.1.jar" -O'
    }
    stage("Paso 6: Levantar Artefacto Jar"){
        sh 'nohup bash java -jar IClabPrueba-0.0.1.jar & >/dev/null'
    }
    stage("Paso 7: Testear Artefacto durmiendo 20 segundos"){
        sh "sleep 20 && curl -X GET 'http://localhost:8081/rest/mscovid/test?msg=testing'"
    }
}
return this;