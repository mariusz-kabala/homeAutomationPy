def branch = '';

pipeline {
    agent { docker { image 'docker-registry.kabala.tech/ansible:latest' } }
    
    environment {
        CI = 'true'
        GIT_SSH_COMMAND = "ssh -o StrictHostKeyChecking=no"
        STATS_DB_USER = credentials('home-automation-stats-db-user')
        STATS_DB_PASS = credentials('home-automation-stats-db-pass')
    }

    stages {
        stage ('Prepare') {
            steps {
                script {
                    sh "printenv"
                }
            }
        }
        stage ('Deploy won') {
            when {
                environment name: 'app', value: 'won'
            }
             steps {
                script {
                    sshagent(['jenkins-local-ssh-key']) {
                        sh "ansible-playbook -i deploy/hosts deploy/deploy_${app}.yml -e 'app=${app} version=${ghprbActualCommit}'"
                    }
                }
            }
        }
        stage ('Deploy sgp30') {
            when {
                environment name: 'app', value: 'sgp30'
            }
             steps {
                script {
                    sshagent(['jenkins-local-ssh-key']) {
                        sh "ansible-playbook -i deploy/hosts deploy/deploy_${app}.yml -e 'app=${app} dbuser=${STATS_DB_USER} dbpass=${STATS_DB_PASS} version=${ghprbActualCommit}'"
                    }
                }
            }
        }
        stage ('Deploy miio') {
            when {
                environment name: 'app', value: 'miio'
            }
             steps {
                script {
                    sshagent(['jenkins-local-ssh-key']) {
                        configFileProvider([configFile(fileId: 'homeAutomationPy-miio-config.py', targetLocation: 'config.py')]) {
                            def configPath = "${env.WORKSPACE}/config.py"
                            sh "ansible-playbook -i deploy/hosts deploy/deploy_${app}.yml -e 'app=${app} config_path=${configPath} version=${ghprbActualCommit}'"
                        }
                    }
                }
            }
        }
    }

    post { 
        always { 
            cleanWs()
        }
    }
}
