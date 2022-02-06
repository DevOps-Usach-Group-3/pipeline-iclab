/*

    forma de invocación de método call:

    def ejecucion = load 'script.groovy'
    ejecucion.call()

*/

def call(){
  
pipeline {
    agent any
    environment {
        NEXUS_USER         = credentials('nexus-user-name')
        NEXUS_PASSWORD     = credentials('nexus-user-pass')
    }
    /*
    parameters {
        choice(
            name:'compileTool',
            choices: ['Maven', 'Gradle'],
            description: 'Seleccione herramienta de compilacion'
        )
    }*/
    stages {
        stage("Pipeline"){
            steps {
                script{
                    gradle.call()
                    /*
                  switch(params.compileTool)
                    {
                        case 'Maven':
                            maven.call()
                        break;
                        case 'Gradle':
                            gradle.call()
                        break;
                    } */
                }
            }
            post{
                success{
                    slackSend color: 'good', message: "[Su Nombre] [${JOB_NAME}] [${BUILD_TAG}] Ejecucion Exitosa", teamDomain: 'dipdevopsusac-tr94431', tokenCredentialId: 'slack-token'
                }
                failure{
                    slackSend color: 'danger', message: "[Su Nombre] [${env.JOB_NAME}] [${BUILD_TAG}] Ejecucion fallida en stage [${env.TAREA}]", teamDomain: 'dipdevopsusac-tr94431', tokenCredentialId: 'slack-token'
                }
            }
        }
    }
}

}

return this;
