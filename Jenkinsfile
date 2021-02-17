#!groovy
@Library('github.com/cloudogu/ces-build-lib@1.45.0')
import com.cloudogu.ces.cesbuildlib.*

node('docker') {

  Maven mvn = new MavenWrapperInDocker(this, 'azul/zulu-openjdk-alpine:11.0.10')

  catchError {

    stage('Checkout') {
      checkout scm
    }

    mvn.useRepositoryCredentials([id: 'ecosystem.cloudogu.com', credentialsId: 'jenkins'])
    String versionName = mvn.version

    stage('Build') {
      mvn 'clean package -DskipTests'
    }

    stage('Test') {
      mvn 'test -Dmaven.test.failure.ignore=true'
      // Archive test results. Makes build unstable on failed tests.
      junit testResults: '**/target/surefire-reports/TEST-*.xml'
    }

    // We should enable this next time we're working on this class. For now build and tests are already a huge step forward!
  /* stage('Static Code Analysis') {
      generateCoverageReportForSonarQube(mvn)
      def sonarQube = new SonarQube(this, [sonarQubeEnv: 'ces-sonar'])

      sonarQube.analyzeWith(mvn)

      if (!sonarQube.waitForQualityGateWebhookToBeCalled()) {
        unstable("Pipeline unstable due to SonarQube quality gate failure")
      }
    }*/

  }

  // Find maven warnings and visualize in job
  publishIssues issues: [ scanForIssues(tool: mavenConsole()) ]

  mailIfStatusChanged(new Git(this).commitAuthorEmail)
}

void generateCoverageReportForSonarQube(Maven mvn) {
  mvn 'org.jacoco:jacoco-maven-plugin:0.8.5:report'
}
