package com.gradle;

import com.gradle.maven.extension.api.GradleEnterpriseApi;
import com.gradle.maven.extension.api.cache.BuildCacheApi;
import com.gradle.maven.extension.api.cache.MojoMetadataProvider;
import com.gradle.maven.extension.api.scan.BuildScanApi;

/**
 * Provide standardized Gradle Enterprise configuration.
 * By applying the extension, these settings will automatically be applied.
 */
final class CustomGradleEnterpriseConfig {

  void configureGradleEnterprise(GradleEnterpriseApi gradleEnterprise) {
    gradleEnterprise.setServer("http://localhost:5086");
    gradleEnterprise.setAllowUntrustedServer(true);
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
    buildCache.getRemote().setEnabled(false);

    ignoreTimestampInMetaInf(buildCache);
    configureBuildCacheAvroPlugin(buildCache);
    configureBuildCacheEnforcerPlugin(buildCache);
  }

  void ignoreTimestampInMetaInf(BuildCacheApi buildCache) {
    buildCache.registerNormalizationProvider(context -> context
            .configureRuntimeClasspathNormalization(
                    normalization -> normalization
                            .configureMetaInf(metaInf -> metaInf.setIgnoredAttributes("Build-Time"))
            ));
  }

  void configureBuildCacheAvroPlugin(BuildCacheApi buildCache) {
    buildCache.registerMojoMetadataProvider(context -> {
      context.withPlugin("avro-maven-plugin", () -> {
        context.inputs(inputs -> inputs
                        .fileSet("sourceDirectory", fileSet -> fileSet.includesProperty("includes").excludesProperty("excludes"))
                        .fileSet("testSourceDirectory", fileSet -> fileSet.includesProperty("testIncludes").excludesProperty("testExcludes"))
                        .properties(getAvroPluginInputProperties())
                        .ignore(getAvroPluginInputIgnores())
                )
                .outputs(outputs -> outputs.directory("outputDirectory").directory("testOutputDirectory").cacheable("generates consistent outputs for declared inputs"));
      });
    });
  }

  String[] getAvroPluginInputProperties() {
    return new String[]{
            "createOptionalGetters",
            "createSetters",
            "enableDecimalLogicalType",
            "gettersReturnOptional",
            "optionalGettersForNullableFieldsOnly",
            "stringType",
            "imports",
            "fieldVisibility",
            "customConversions",
            "customLogicalTypeFactories",
            "templateDirectory",
            "velocityToolsClassesNames"
    };
  }

  String[] getAvroPluginInputIgnores() {
    return new String[]{
            "project"
    };
  }

  void configureBuildCacheEnforcerPlugin(BuildCacheApi buildCache) {
    buildCache.registerMojoMetadataProvider(context -> {
      context.withPlugin("maven-enforcer-plugin", () -> {
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
