plugins {
    java
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"

    // Code Quality Plugins
    checkstyle
    pmd
    id("com.github.spotbugs") version "6.0.27"
    id("org.owasp.dependencycheck") version "11.1.1"
    jacoco
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "football-season-simulator"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.h2database:h2")
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // Swagger/OpenAPI Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5")

    // SpotBugs annotations for suppressing warnings
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.6")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// =============================================================================
// CHECKSTYLE CONFIGURATION
// =============================================================================
checkstyle {
    toolVersion = "10.21.1"
    configFile = file("${rootDir}/config/checkstyle/checkstyle.xml")
    // Set to true to generate reports without failing the build
    // Change to false once codebase is compliant
    isIgnoreFailures = true
    maxWarnings = 500
}

tasks.withType<Checkstyle> {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// =============================================================================
// PMD CONFIGURATION
// =============================================================================
pmd {
    toolVersion = "7.9.0"
    isConsoleOutput = true
    // Set to true to generate reports without failing the build
    // Change to false once codebase is compliant
    isIgnoreFailures = true
    rulesMinimumPriority.set(5)
    ruleSetFiles = files("${rootDir}/config/pmd/pmd-ruleset.xml")
}

tasks.withType<Pmd> {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// =============================================================================
// SPOTBUGS CONFIGURATION
// =============================================================================
spotbugs {
    // Set to true to generate reports without failing the build
    // Change to false once codebase is compliant
    ignoreFailures.set(true)
    showStackTraces.set(true)
    showProgress.set(true)
    effort.set(com.github.spotbugs.snom.Effort.MAX)
    reportLevel.set(com.github.spotbugs.snom.Confidence.LOW)
    excludeFilter.set(file("${rootDir}/config/spotbugs/spotbugs-exclude.xml"))
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
    reports.create("html") {
        required.set(true)
        outputLocation.set(file("${layout.buildDirectory.get()}/reports/spotbugs/${name}.html"))
    }
    reports.create("xml") {
        required.set(true)
        outputLocation.set(file("${layout.buildDirectory.get()}/reports/spotbugs/${name}.xml"))
    }
}

// =============================================================================
// OWASP DEPENDENCY CHECK CONFIGURATION
// =============================================================================
dependencyCheck {
    failBuildOnCVSS = 7.0f
    formats = listOf("HTML", "JSON", "XML")
    outputDirectory = "${layout.buildDirectory.get()}/reports/owasp"
    suppressionFile = "${rootDir}/config/owasp/owasp-suppressions.xml"

    analyzers.apply {
        assemblyEnabled = false
        nodeEnabled = false
        nodeAuditEnabled = false
        retirejs.enabled = false
    }
}

// =============================================================================
// JACOCO CODE COVERAGE CONFIGURATION
// =============================================================================
jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/model/**",
                    "**/config/**",
                    "**/FootballSeasonSimulatorApplication.class",
                    "**/engine/SeasonRunner.class",
                    "**/service/SimulationService\$LeagueSimulation.class",
                    "**/service/SimulationService\$LiveStandingData.class"
                )
            }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)

    // Use the same exclusions as the report
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/model/**",
                    "**/config/**",
                    "**/FootballSeasonSimulatorApplication.class",
                    "**/engine/SeasonRunner.class",
                    "**/service/SimulationService\$LeagueSimulation.class",
                    "**/service/SimulationService\$LiveStandingData.class"
                )
            }
        })
    )

    violationRules {
        // Overall project coverage - 90% minimum
        rule {
            limit {
                minimum = "0.90".toBigDecimal()
            }
        }

        // Per-class coverage with extensive exclusions
        rule {
            element = "CLASS"
            excludes = listOf(
                // Model classes (POJOs/DTOs)
                "com.example.footballseasonsimulator.model.*",
                // Configuration classes
                "com.example.footballseasonsimulator.config.*",
                // Main application class
                "com.example.footballseasonsimulator.FootballSeasonSimulatorApplication",
                // WebSocket publisher and its inner classes
                "com.example.footballseasonsimulator.websocket.*",
                // Engine classes that are hard to unit test
                "com.example.footballseasonsimulator.engine.SeasonRunner",
                // Service classes with complex async behavior
                "com.example.footballseasonsimulator.service.SimulationService",
                // Page controller (Thymeleaf views)
                "com.example.footballseasonsimulator.controller.PageController",
                // Explicitly exclude inner classes (JaCoCo reports with dot notation)
                "*.LiveStandingData",
                "*.MatchDTO",
                "*.FixtureDTO",
                "*.EventDTO",
                "*.PredictionDTO"
            )
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                // Per-class minimum - 50%
                minimum = "0.50".toBigDecimal()
            }
        }
    }
}

// =============================================================================
// TEST CONFIGURATION
// =============================================================================
tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

// =============================================================================
// AGGREGATE TASK FOR ALL QUALITY CHECKS
// =============================================================================
tasks.register("qualityCheck") {
    group = "verification"
    description = "Runs all code quality checks: Checkstyle, PMD, SpotBugs, and tests with coverage"
    dependsOn(
        tasks.checkstyleMain,
        tasks.checkstyleTest,
        tasks.pmdMain,
        tasks.pmdTest,
        tasks.spotbugsMain,
        tasks.spotbugsTest,
        tasks.test,
        tasks.jacocoTestReport,
        tasks.jacocoTestCoverageVerification
    )
}

tasks.register("securityCheck") {
    group = "verification"
    description = "Runs OWASP dependency check for security vulnerabilities"
    dependsOn(tasks.dependencyCheckAnalyze)
}

tasks.register("fullCheck") {
    group = "verification"
    description = "Runs all quality and security checks"
    dependsOn("qualityCheck", "securityCheck")
}
