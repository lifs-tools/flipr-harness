/*
 * 
 */
package de.isas.lipidomics.transitionextractor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Nils Hoffmann <nils.hoffmann@isas.de>
 */
@Slf4j
public class RJobRunner {

    @Data
    public static class Arguments {

        private Path outputDir;
        private Path file;
        private String fileFormat;
        private String baseName;
        private boolean dataPlots;
        private double minCollisionEnergy;
        private Path fliprConfigFile;
        private Path fliprPath;
        private Integer maxCombinations;
    }

    /**
     * Export a resource embedded into a Jar file to the local file path.
     *
     * @param resourceName ie.: "/plotFragmentDistributions.R"
     * @param outputDir the directory to export the resource to
     * @return The path to the exported resource
     * @throws Exception
     */
    static public Path extractResource(String resourceName, Path outputDir) throws Exception {
        File outputFile = new File(outputDir.toFile(), resourceName);
        if (!outputFile.exists()) {
            try (InputStream stream = RJobRunner.class.getResourceAsStream(
                    resourceName)) {
                if (stream == null) {
                    throw new Exception(
                            "Cannot get resource \"" + resourceName + "\" from Jar file.");
                }
                int readBytes;
                byte[] buffer = new byte[4096];
                try (OutputStream resStreamOut = new FileOutputStream(outputFile)) {
                    while ((readBytes = stream.read(buffer)) > 0) {
                        resStreamOut.write(buffer, 0, readBytes);
                    }
                    return outputFile.toPath();
                }
            } catch (Exception ex) {
                throw ex;
            }
        } else {
            log.debug("R script exists!");
            return outputFile.toPath();
        }
    }

    public CompletableFuture<Integer> apply(Arguments arguments) {
        try {
            File outputDir = arguments.getOutputDir().
                    toFile();
            outputDir.mkdirs();
            log.debug("Extracting R script");
            Path script = null;
            if (arguments.fliprPath == null) {
                log.info("No flipr path provided, using embedded one!");
                script = extractResource("/flipr.R",
                        arguments.getOutputDir());
            } else {
                log.info("Using provided flipr path: " + arguments.fliprPath);
                if (arguments.fliprPath.toFile().exists()) {
                    script = arguments.getOutputDir().resolve("flipr.R");
                    Files.copy(arguments.fliprPath, script, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    throw new IOException("Provided flipr file path does not exists at " + arguments.fliprPath);
                }
            }
            File shellScript = new File(arguments.getOutputDir().
                    toFile(), arguments.getBaseName() + "-run.sh");
            List<String> args = new ArrayList<>();
            args.addAll(Arrays.asList(
                    "Rscript", script.
                            getFileName().
                            toString(), "--filePattern=" + "*_fip.tsv",
                    "--plotFormat=" + arguments.getFileFormat(),
                    "--dataPlots=" + Boolean.valueOf(arguments.isDataPlots()).
                            toString().
                            toUpperCase(), "--minPrecursorCollisionEnergy=" + arguments.
                            getMinCollisionEnergy(), "--trainModel=TRUE", "--maxCombinations=" + arguments.getMaxCombinations()
            ));
            if (arguments.getFliprConfigFile() != null) {
                log.info("Using flipr config file {}", arguments.
                        getFliprConfigFile());
                Files.copy(arguments.getFliprConfigFile(), arguments.
                        getOutputDir().
                        resolve(arguments.getFliprConfigFile().
                                getFileName().
                                toString()), StandardCopyOption.REPLACE_EXISTING);
                args.add("--config=" + arguments.getFliprConfigFile().
                        getFileName().
                        toString());
            }
            Files.write(shellScript.toPath(), ("#!/bin/bash\n" + args.stream().
                    collect(Collectors.joining(" "))).
                    getBytes(Charset.forName("UTF8")));
            log.debug("Creating process");
            ProcessBuilder pb = new ProcessBuilder(args).redirectErrorStream(
                    true).
                    redirectOutput(Redirect.to(new File(arguments.getOutputDir().
                            toFile(), arguments.getBaseName() + "-run.out"))).
                    directory(arguments.getOutputDir().
                            toFile());
            Process p = pb.start();
            return CompletableFuture.completedFuture(p.waitFor());
        } catch (Exception ex) {
            log.error("Caught exception:", ex);
        }
        return CompletableFuture.completedFuture(-1);
    }
}
