package com.novoda.staticanalysis.internal.detekt

import com.novoda.staticanalysis.StaticAnalysisExtension
import com.novoda.staticanalysis.Violations
import com.novoda.staticanalysis.internal.Configurator
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task

class DetektConfigurator implements Configurator {

    private static final String DETEKT_PLUGIN = 'io.gitlab.arturbosch.detekt'
    private static final String DETEKT_NOT_APPLIED = 'The Detekt plugin is configured but not applied. Please apply the plugin in your build script.\nFor more information see https://github.com/arturbosch/detekt.'
    private static final String OUTPUT_NOT_DEFINED = 'Output not defined! To analyze the results, `output` needs to be defined in Detekt profile.'

    private final Project project
    private final Violations violations
    private final Task evaluateViolations

    static DetektConfigurator create(Project project,
                                     NamedDomainObjectContainer<Violations> violationsContainer,
                                     Task evaluateViolations) {
        Violations violations = violationsContainer.maybeCreate('Detekt')
        return new DetektConfigurator(project, violations, evaluateViolations)
    }

    private DetektConfigurator(Project project, Violations violations, Task evaluateViolations) {
        this.project = project
        this.violations = violations
        this.evaluateViolations = evaluateViolations
    }

    @Override
    void execute() {
        project.plugins.withId(DETEKT_PLUGIN) {
            project.extensions.findByType(StaticAnalysisExtension).ext.detekt = { Closure config ->
                def detektExtension = project.extensions.findByName('detekt')
                config.delegate = detektExtension
                config()
                configureToolTask(detektExtension)
            }
        }
    }

    private void configureToolTask(detekt) {
        def detektTask = project.tasks['detektCheck']
        detektTask.group = 'verification'

        // run detekt as part of check
        project.tasks['check'].dependsOn(detektTask)

        // evaluate violations after detekt
        def output = getSystemOrDefaultProfile(detekt).output
        if (!output) {
            throw new IllegalArgumentException(OUTPUT_NOT_DEFINED)
        }
        def collectViolations = createCollectViolationsTask(violations, project.file(output))
        evaluateViolations.dependsOn collectViolations
        collectViolations.dependsOn detektTask
    }

    private def getSystemOrDefaultProfile(def detektExtension) {
        try {
            // For Detekt 1.0.0-RC3 and lower
            return detektExtension.systemOrDefaultProfile()
        } catch (RuntimeException ignored) {
            // For Detekt 1.0.0-RC4 and higher
            return io.gitlab.arturbosch.detekt.extensions.ProfileStorageKt.systemOrDefault
        }
    }

    private CollectDetektViolationsTask createCollectViolationsTask(Violations violations, File outputFolder) {
        project.tasks.create('collectDetektViolations', CollectDetektViolationsTask) { task ->
            task.xmlReportFile = new File(outputFolder, 'detekt-checkstyle.xml')
            task.htmlReportFile = new File(outputFolder, 'detekt-report.html')
            task.violations = violations
        }
    }

    private static boolean isKotlinProject(final Project project) {
        final boolean isKotlin = project.plugins.hasPlugin('kotlin')
        final boolean isKotlinAndroid = project.plugins.hasPlugin('kotlin-android')
        final boolean isKotlinPlatformCommon = project.plugins.hasPlugin('kotlin-platform-common')
        final boolean isKotlinPlatformJvm = project.plugins.hasPlugin('kotlin-platform-jvm')
        final boolean isKotlinPlatformJs = project.plugins.hasPlugin('kotlin-platform-js')
        return isKotlin || isKotlinAndroid || isKotlinPlatformCommon || isKotlinPlatformJvm || isKotlinPlatformJs
    }
}
