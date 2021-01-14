properties([
    parameters ([
        string(name: 'DOCKER_REGISTRY_DOWNLOAD_URL', defaultValue: 'nexus-docker-private-group.ossim.io', description: 'Repository of docker images')
    ]),
    pipelineTriggers([
        [$class: "GitHubPushTrigger"]
    ]),
    [$class: 'GithubProjectProperty', displayName: '', projectUrlStr: 'https://github.com/ossimlabs/omar-lite-wms'],
    buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '3', daysToKeepStr: '', numToKeepStr: '20')),
    disableConcurrentBuilds()
])

podTemplate(
    containers: [
        containerTemplate(
            name: 'docker',
            image: 'docker:19.03.11',
            ttyEnabled: true,
            command: 'cat',
            privileged: true
        ),
        containerTemplate(
            image: "${DOCKER_REGISTRY_DOWNLOAD_URL}/alpine/helm:3.2.3",
            name: 'helm',
            command: 'cat',
            ttyEnabled: true
        ),
        containerTemplate(
            name: 'git',
            image: 'alpine/git:latest',
            ttyEnabled: true,
            command: 'cat',
            envVars: [
                envVar(key: 'HOME', value: '/root')
            ]
        )
    ],
    volumes: [
        hostPathVolume(
            hostPath: '/var/run/docker.sock',
            mountPath: '/var/run/docker.sock'
        ),
    ]
)
    {
      node(POD_LABEL){
        stage("Checkout branch") {
          scmVars = checkout(scm)

          GIT_BRANCH_NAME = scmVars.GIT_BRANCH
          BRANCH_NAME = "${sh(returnStdout: true, script: "echo ${GIT_BRANCH_NAME} | awk -F'/' '{print \$2}'").trim()}"

          CHART_APP_VERSION = "${sh(returnStdout: true, script: "grep -Po \"(?<=appVersion: ).*\" chart/Chart.yaml")}".trim()
          GRADLE_APP_VERSION = "${sh(returnStdout: true, script: "grep -Po \"(?<=buildVersion=).*\" gradle.properties")}".trim()

          GIT_TAG_NAME = "omar-lite-wms-${GRADLE_APP_VERSION}"
          ARTIFACT_NAME = "ArtifactName"

          script {
            if (BRANCH_NAME != 'master') {
              buildName "${GRADLE_APP_VERSION} - ${BRANCH_NAME}-SNAPSHOT"
            } else {
              buildName "${GRADLE_APP_VERSION} - ${BRANCH_NAME}"
            }
          }
        }

        stage("Load Variables") {
          step([$class     : "CopyArtifact",
                projectName: "gegd-dgcs-jenkins-artifacts",
                filter     : "common-variables.groovy",
                flatten    : true])
        }

        load "common-variables.groovy"

        DOCKER_IMAGE_PATH = "${DOCKER_REGISTRY_PRIVATE_UPLOAD_URL}/omar-lite-wms"

        stage("Build Docker Image") {
          container('docker'){
            withGradle {
              script {
                sh """
                  apk add gradle
                  gradle jDB
                """
              }
            }
          }
        }

        stage("Push Docker Image") {
          container('docker') {
            withDockerRegistry(credentialsId: 'dockerCredentials', url: "https://${DOCKER_REGISTRY_PRIVATE_UPLOAD_URL}") {

              if (BRANCH_NAME == 'master') {
                TAG_NAME = GRADLE_APP_VERSION
              } else {
                TAG_NAME = BRANCH_NAME + "-" + System.currentTimeMillis()
              }

              sh """
                docker tag ${DOCKER_IMAGE_PATH} ${DOCKER_IMAGE_PATH}:${TAG_NAME}
                docker push ${DOCKER_IMAGE_PATH}:${TAG_NAME}
              """
            }
          }
        }

        stage('Package Chart'){
          container('helm') {
            script {
              sh 'helm package chart'
            }
          }
        }

        stage('Upload Chart'){
          container('helm') {
            withCredentials([usernameColonPassword(credentialsId: 'helmCredentials', variable: 'HELM_CREDENTIALS')]) {
              script {
                sh 'apk add curl'
                sh 'curl -u ${HELM_CREDENTIALS} ${HELM_UPLOAD_URL} --upload-file *.tgz -v'
              }
            }
          }
        }

        stage('Tag Repo') {
          when (BRANCH_NAME == 'master') {
            container('git') {
              withCredentials([sshUserPrivateKey(
                  credentialsId: env.GIT_SSH_CREDENTIALS_ID,
                  keyFileVariable: 'SSH_KEY_FILE',
                  passphraseVariable: '',
                  usernameVariable: 'SSH_USERNAME')]) {

                script {
                  sh """
                mkdir ~/.ssh
                echo -e "StrictHostKeyChecking=no\nIdentityFile ${SSH_KEY_FILE}" >> ~/.ssh/config
                git config user.email "radiantcibot@gmail.com"
                git config user.name "Jenkins"
                git tag -a "${GIT_TAG_NAME}" \
                  -m "Generated by: ${env.JENKINS_URL}" \
                  -m "Job: ${env.JOB_NAME}" \
                  -m "Build: ${env.BUILD_NUMBER}"
                git push -v origin "${GIT_TAG_NAME}"
              """
                }
              }
            }
          }
        }
      }
    }

