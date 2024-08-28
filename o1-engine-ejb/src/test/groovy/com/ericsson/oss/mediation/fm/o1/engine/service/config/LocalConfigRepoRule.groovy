package com.ericsson.oss.mediation.fm.o1.engine.service.config

import java.nio.file.DirectoryStream
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

import com.ericsson.oss.mediation.fm.o1.engine.service.config.jar.JarNioUtil

class LocalConfigRepoRule implements TestRule {

    Path localRepoPath = Paths.get(System.getProperty("user.dir"), "target", "med_config")

    @Override
    Statement apply(Statement statement, Description description) {
        return new ConfigRepoLocal(statement)
    }

    class ConfigRepoLocal extends Statement {

        private final Statement base

        ConfigRepoLocal(Statement base) {
            this.base = base;
        }
        @Override
        void evaluate() throws Throwable {
            System.setProperty("o1.mediation.config.repository", localRepoPath.toString())
            base.evaluate()
        }
    }

    Path getLocalRepoPath() {
        return localRepoPath
    }

    Path getTransformationJarPath() {
        final DirectoryStream.Filter jarFilter = JarNioUtil.createFilter(FileSystems.getDefault(), "**.jar");
        final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(localRepoPath, jarFilter)
        return directoryStream.getAt(0)
    }
}
