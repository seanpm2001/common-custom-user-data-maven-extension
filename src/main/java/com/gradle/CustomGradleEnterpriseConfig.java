package com.gradle;

import com.gradle.maven.extension.api.GradleEnterpriseApi;
import com.gradle.maven.extension.api.cache.BuildCacheApi;
import com.gradle.maven.extension.api.scan.BuildScanApi;
import com.gradle.maven.extension.api.cache.MojoMetadataProvider;

import java.net.URI;

/**
 * Provide standardized Gradle Enterprise configuration.
 * By applying the extension, these settings will automatically be applied.
 */
final class CustomGradleEnterpriseConfig {

    void configureGradleEnterprise(GradleEnterpriseApi gradleEnterprise) {
        gradleEnterprise.setServer("http://localhost:5086");
        gradleEnterprise.setAllowUntrustedServer(true);
        //gradleEnterprise.setServer("https://ge-helm-standalone-unstable.grdev.net/");
    }

    void configureBuildScanPublishing(BuildScanApi buildScans) {
        boolean isCiServer = System.getenv().containsKey("CI");

        buildScans.publishAlways();
        buildScans.setCaptureGoalInputFiles(true);
        buildScans.setUploadInBackground(!isCiServer);

        buildScans.tag("api-config");
    }

    void configureBuildCache(BuildCacheApi buildCache) {
        buildCache.getLocal().setEnabled(true);
        //buildCache.getRemote().getServer().setUrl(URI.create("http://localhost:5086/cache/"));
        //buildCache.getRemote().getServer().setUrl(URI.create("https://ge.solutions-team.gradle.com/cache/"));
        buildCache.getRemote().setEnabled(false);

        //TODO test runtime normalization
        configureBuildCacheAvroPlugin(buildCache);
        configureBuildCacheEnforcerPlugin(buildCache);
    }

    //TODO
    void configureBuildCacheAvroPlugin(BuildCacheApi buildCache) {

    }

    void configureBuildCacheEnforcerPlugin(BuildCacheApi buildCache) {
        buildCache.registerMojoMetadataProvider(context -> {
            context.withPlugin("maven-enforcer-plugin",() -> {
                if ("enforce-bytecode-version".equals(context.getMojoExecution().getExecutionId())) {
                    context.inputs(inputs -> inputs
                                    .ignore(
                                            "commandLineRules",
                                            "fail",
                                            "failFast",
                                            "ignoreCache",
                                            "mojoExecution",
                                            "session",
                                            "skip"
                                    )
                            )
                            .nested("project", nestedContext ->
                                    nestedContext.iterate("artifacts", iteratedContext -> iteratedContext.inputs(nestedInputs -> nestedInputs
                                            .fileSet("file", fileSet -> fileSet.normalizationStrategy(MojoMetadataProvider.Context.FileSet.NormalizationStrategy.COMPILE_CLASSPATH)
                                                    .lineEndingHandling(MojoMetadataProvider.Context.FileSet.LineEndingHandling.NORMALIZE))
                                    ))
                            )
                            .iterate("rules", iteratedContext -> iteratedContext
                                    .inputs(inputs -> inputs.properties("maxJdkVersion", "maxJavaMajorVersionNumber", "ignoredScopes", "ignoreClasses", "ignoreOptionals", "excludes"))
                            )
                            .outputs(outputs -> outputs.cacheable("bytecode version check should only run when class files change"));
                } else if ("enforce-banned-dependencies".equals(context.getMojoExecution().getExecutionId())) {
                    context.inputs(inputs -> inputs
                                    .properties(
                                            "commandLineRules",
                                            "fail",
                                            "failFast",
                                            "ignoreCache",
                                            "skip"
                                    )
                                    .ignore(
                                            "session",
                                            "project",
                                            "mojoExecution"
                                    )
                            )
                            .nested("project", nestedContext ->
                                    nestedContext.iterate("dependencies", iteratedContext -> iteratedContext.inputs(nestedInputs -> nestedInputs
                                            .properties("groupId", "artifactId", "version", "type", "scope")
                                    ))
                            )
                            .iterate("rules", iteratedContext -> iteratedContext
                                    .inputs(inputs -> inputs.properties("includes", "excludes", "searchTransitive", "message"))
                            )
                            .outputs(outputs -> outputs.cacheable("banned dependency check should run when dependencies change"));
                }
            });
        });
    }

    //TODO
    void configureBuildCacheSpecificKeysInMap(BuildCacheApi buildCache) {

    }

    //TODO
    void configureBuildCacheSeveralExecutionId(BuildCacheApi buildCache) {

    }
}
