/*
	forma de invocación de método call:
	def ejecucion = load 'script.groovy'
	ejecucion.call()
*/
def call(){


    echo 'El pipeline se ejecutará segun la rama ' + env.GIT_BRANCH
    String rama = env.GIT_BRANCH
    if (rama.indexOf("develop") > 0 || rama.indexOf("feature") > 0) {
        figlet "Integración Continua"
        stagesCI()
    }
    if (rama.indexOf('release') > 0) {
        figlet "Despliegue Continuo"
        stagesCD()
    }
}

def stageCleanBuildTest(){
    env.DESCRIPTION_STAGE = 'Paso 1: Build - Test'
    stage("${env.DESCRIPTION_STAGE}"){
        env.STAGE = "build - ${env.DESCRIPTION_STAGE}"
        sh "echo  ${env.STAGE}"
        sh "gradle clean build"
    }
}

/*    stage("Paso 1: Build && Test"){
        sh "gradle clean build"
    } */

def stageSonar(){
    env.DESCRIPTION_STAGE = "Paso 2: Sonar - Análisis Estático"
    stage("${env.DESCRIPTION_STAGE}"){
        env.STAGE = "sonar - ${DESCRIPTION_STAGE}"
        withSonarQubeEnv('sonarqube') {
            sh "echo  ${env.STAGE}"
            sh './gradlew sonarqube -Dsonar.projectKey=pipeline-iclab-prueba -Dsonar.java.binaries=build'
        }
    }
}

/*
    stage("Paso 2: Sonar - Análisis Estático"){
        sh "echo 'Análisis Estático!'"
        withSonarQubeEnv('sonarqube') {
            sh './gradlew sonarqube -Dsonar.projectKey=pipeline-iclab-prueba2 -Dsonar.java.binaries=build'
        }
    }
*/

def stageRunSpringCurl(){
    env.DESCRIPTION_STAGE = "Paso 3: Curl Springboot Gradle durmiendo 40 segundos"
    stage("${env.DESCRIPTION_STAGE}"){
        env.STAGE = "run_spring_curl - ${DESCRIPTION_STAGE}"
        sh "echo  ${env.STAGE}"
        sh "gradle bootRun&"
        sh "sleep 40 && curl -X GET 'http://localhost:8081/rest/mscovid/test?msg=testing'"
    }
}

/*
    stage("Paso 3: Curl Springboot con Gradle durmiendo 20 segundos"){
        sh "gradle bootRun&"
        sh "sleep 60 && curl -X GET 'http://localhost:8081/rest/mscovid/test?msg=testing'"
    }
*/

def stageUploadNexus(){
    env.DESCRIPTION_STAGE = "Paso 4: Subir Nexus"
    stage("${env.DESCRIPTION_STAGE}"){
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
        env.STAGE = "upload_nexus - ${DESCRIPTION_STAGE}"
        sh "echo  ${env.STAGE}"
    }
}

/*    stage("Paso 4: Subir Nexus"){
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
*/

def stageDownloadNexus(){
    env.DESCRIPTION_STAGE = "Paso 5: Descargar Nexus"
   stage("${env.DESCRIPTION_STAGE}"){
        env.STAGE = "download_nexus - ${DESCRIPTION_STAGE}"
        sh "echo  ${env.STAGE}"
        sh ' curl -X GET -u $NEXUS_USER:$NEXUS_PASSWORD "http://nexus:8081/repository/pipeline-iclab-prueba/com/devopsusach2020/IClabPrueba/0.0.1/IClabPrueba-0.0.1.jar" -O'
    }
}


/*    stage("Paso 5: Descargar Nexus"){
        sh ' curl -X GET -u $NEXUS_USER:$NEXUS_PASSWORD "http://nexus:8081/repository/pipeline-iclab-prueba/com/devopsusach2020/IClabPrueba/0.0.1/IClabPrueba-0.0.1.jar" -O'
    }

*/

def stageRunJar(){
    env.DESCRIPTION_STAGE = "Paso 6: Levantar Artefacto Jar"
    stage("${env.DESCRIPTION_STAGE}"){
        env.STAGE = "run_jar - ${DESCRIPTION_STAGE}"
        sh "echo  ${env.STAGE}"
        sh 'nohup bash java -jar IClabPrueba-0.0.1.jar & >/dev/null'
    }
}

/*    stage("Paso 6: Levantar Artefacto Jar"){
        sh 'nohup bash java -jar IClabPrueba-0.0.1.jar & >/dev/null'
    }
*/

def stageCurlJar(){
    env.DESCRIPTION_STAGE = "Paso 7: Testear Artefacto durmiendo 20 segundos"
    stage("${env.DESCRIPTION_STAGE}"){
        env.STAGE = "curl_jar - ${DESCRIPTION_STAGE}"
        sh "echo  ${env.STAGE}"
        sh "sleep 20 && curl -X GET 'http://localhost:8081/rest/mscovid/test?msg=testing'"
    }
    stage("Paso 8: Generando la rama Release"){
        if(env.GIT_BRANCH == "origin/develop"){
            sh 'echo "Crear release"'
            sh "git checkout -b release-v${env.VERSION}"
        }
    }
}


/*    stage("Paso 7: Testear Artefacto durmiendo 20 segundos"){
        sh "sleep 20 && curl -X GET 'http://localhost:8081/rest/mscovid/test?msg=testing'"
    } */

//  Stage 9

def stageMergeMaster() {
    env.DESCRIPTION_STAGE = "Paso 9: Realizar merge directo hacia la rama master."
    stage("${env.DESCRIPTION_STAGE}"){
        env.STAGE = "gitMergeMaster - ${DESCRIPTION_STAGE}"
        sh "echo ${env.STAGE}"
        sh "git checkout main && git merge release-v${env.VERSION}"
    }
}

// stage 10
def stageMergeDevelop(){
    env.DESCRIPTION_STAGE ="Paso 10: Realizar merge a rama develop"
    stage("${env.DESCRIPTION_STAGE}"){
        env.STAGE = "gitMergeDevelop - ${DESCRIPTION_STAGE}"
        sh "echo ${env.STAGE}"
        sh "git checkout develop && git merge main"
    }
}

// stage 11
def stageGitTag(){
    env.DESCRIPTION_STAGE = "Paso 11: Realizar git Tag a rama main"
    stage("${env.DESCRIPTION_STAGE}"){
        env.STAGE = "gitTagRelease - ${DESCRIPTION_STAGE}"
        sh "echo ${env.STAGE}"
        sh "git tag -a v${env.VERSION}"
    }
}

def stagesCI(){
    stageCleanBuildTest()
    stageSonar()
    stageRunSpringCurl()
    stageRest()
    stageUploadNexus()
}

def stagesCD(){
    stageDownloadNexus()
    stageRunJar()
    stageUploadNexus()
    stageMergeMaster()
    stageMergeDevelop()
    stageGitTag()
}


return this;