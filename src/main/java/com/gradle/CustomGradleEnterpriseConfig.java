package com.gradle;

import com.gradle.maven.extension.api.GradleEnterpriseApi;
import com.gradle.maven.extension.api.cache.BuildCacheApi;
import com.gradle.maven.extension.api.scan.BuildScanApi;

/**
 * Provide standardized Gradle Enterprise configuration.
 * By applying the extension, these settings will automatically be applied.
 */
final class CustomGradleEnterpriseConfig {

    static void configureGradleEnterprise(GradleEnterpriseApi gradleEnterprise) {
        /* Example of Gradle Enterprise configuration

        // Set a different storage directory.
        // See https://docs.gradle.com/enterprise/maven-extension/#anatomy_of_the_gradle_enterprise_directory
        gradleEnterprise.setStorageDirectory(Paths.get("/path/to/new/storage/directory"));

        */
    }

    static void configureBuildScanPublishing(BuildScanApi buildScans) {
        /* Example of build scan publishing configuration

        boolean isCiServer = System.getenv().containsKey("CI");

        buildScans.publishAlways();
        buildScans.setCaptureGoalInputFiles(true);
        buildScans.setUploadInBackground(!isCiServer);

        */
    }

    static void configureBuildCache(BuildCacheApi buildCache) {
        /* Example of build cache configuration

        boolean isCiServer = System.getenv().containsKey("CI");

        // Enable the local build cache for all local and CI builds
        // For short-lived CI agents, it makes sense to disable the local build cache
        buildCache.getLocal().setEnabled(true);

        // Only permit store operations to the remote build cache for CI builds
        // Local builds will only read from the remote build cache
        buildCache.getRemote().setEnabled(true);
        buildCache.getRemote().setStoreEnabled(isCiServer);

        */
    }

    private CustomGradleEnterpriseConfig() {
    }

}
