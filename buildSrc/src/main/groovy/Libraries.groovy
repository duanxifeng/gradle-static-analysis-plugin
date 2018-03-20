import org.gradle.api.Project

class Libraries {
    final junit = 'junit:junit:4.12'
    final truth = 'com.google.truth:truth:0.30'
    final guava = 'com.google.guava:guava:19.0'
    final mockito = 'org.mockito:mockito-core:2.13.0'
    final findbugs = new Findbugs()
    final detektPlugin = 'io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.0.0.RC6-4'

    private final Project project

    Libraries(Project project) {
        this.project = project
    }

    def getGradleApi() {
        project.dependencies.gradleApi()
    }

    def getGradleTestKit() {
        project.dependencies.gradleTestKit()
    }

    private static class Findbugs {
        final annotations = 'com.google.code.findbugs:jsr305:3.0.0'
    }
}
