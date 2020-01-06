/*
 * 
 */
package de.isas.lipidomics.transitionextractor;

import de.isas.lipidomics.transitionextractor.services.transitions.TransitionListParser;
import de.isas.lipidomics.transitionextractor.services.LipidTargetFinder;
import de.isas.lipidomics.transitionextractor.services.MappingSpecification;
import de.isas.lipidomics.transitionextractor.services.mapping.TransitionMappingParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;

/**
 *
 * @author Nils Hoffmann <nils.hoffmann@isas.de>
 */
@Slf4j
public class Converter {

    @Data
    public static class Arguments {

        private final LipidTargetFinder ltf;
        private final Path mzMlInputPath;
        private final Path outputDirPath;
        private final Path transitionsFile;
        private final Path mappingFile;
        private final String plotFormat;
        private final boolean plotData;
        private final double minCollisionEnergy;
        private final Path fliprConfigFile;
        private final Path fliprPath;
        private final Integer maxCombinations;
    }

    private final Arguments arguments;

    public Converter(Arguments arguments) {
        this.arguments = arguments;
    }

    public void convert(Integer nThreads, boolean stopAtSmallestPpm) throws IOException, InterruptedException, ExecutionException {
        this.arguments.outputDirPath.toFile().
            mkdirs();
        Files.copy(this.arguments.mappingFile, this.arguments.outputDirPath.
            resolve(this.arguments.mappingFile.getFileName()),
            StandardCopyOption.REPLACE_EXISTING);
        TransitionMappingParser tfmp = new TransitionMappingParser();
        MultiValuedMap<ConfigKey, MappingSpecification> mappingSpecifications;
        try {
            mappingSpecifications = tfmp.parse(this.arguments.mappingFile,
                this.arguments.mzMlInputPath);
        } catch (IOException ex) {
            log.error("Caught exception:", ex);
            return;
        }
        Files.copy(this.arguments.transitionsFile, this.arguments.outputDirPath.
            resolve(this.arguments.transitionsFile.getFileName()),
            StandardCopyOption.REPLACE_EXISTING);
        TransitionListParser cfp = new TransitionListParser();
        Map<Path, TransitionSpecificationGroup> targetSpecifications;
        try {
            targetSpecifications = cfp.parse(this.arguments.transitionsFile,
                this.arguments.mzMlInputPath, mappingSpecifications);
            log.info(String.format(
                "Loaded %d target specifications for %d files.",
                targetSpecifications.values().
                    size(), targetSpecifications.keySet().
                    size()));
        } catch (IOException ex) {
            log.error("Caught exception:", ex);
            return;
        }

        final ExecutorService es = Executors.newFixedThreadPool(nThreads);
        final LocalDateTime dateTimeCreated = LocalDateTime.now(Clock.
            systemUTC());
        List<CompletableFuture<String>> futures = targetSpecifications.
            keySet().
            stream().
            map((key) ->
            {
                final TransitionSpecificationGroup tsg = targetSpecifications.
                    get(
                        key);
                final Path outputDirPath = this.arguments.outputDirPath.resolve(
                    tsg.getConfigKey().
                        toString());
                return CompletableFuture.supplyAsync(new ConversionSupplier(
                    this.arguments.ltf,
                    outputDirPath, tsg,
                    stopAtSmallestPpm, dateTimeCreated), es).
                    thenApplyAsync((t) ->
                    {
                        if (t.isPresent() && t.get().
                            toFile().
                            exists()) {
                            try {
                                RPlotter plotter = new RPlotter();
                                RPlotter.Arguments args = new RPlotter.Arguments();
                                args.setOutputDir(outputDirPath);
                                args.setDataPlots(this.arguments.plotData);
                                args.setFileFormat(this.arguments.plotFormat);
                                args.setMinCollisionEnergy(
                                    this.arguments.minCollisionEnergy);
                                args.setBaseName(tsg.getConfigKey().
                                    toString());
                                args.setFile(t.get());
                                args.setFliprConfigFile(this.arguments.fliprConfigFile);
                                args.setFliprPath(this.arguments.fliprPath);
                                args.setMaxCombinations(this.arguments.maxCombinations);
                                CompletableFuture<Integer> res = plotter.apply(
                                    args);
                                Integer returnValue = res.get();
                                if (returnValue == 0) {
                                    return "Generated result for " + this.arguments.outputDirPath + " (" + tsg.
                                        getPrecursorName() + tsg.
                                            getPrecursorAdduct() + " )" + " and key=" + key;
                                } else {
                                    return "Failed to generate result for " + this.arguments.outputDirPath + " (" + tsg.
                                        getPrecursorName() + tsg.
                                            getPrecursorAdduct() + " )" + " and key=" + key + " with return value=" + returnValue;
                                }
                            } catch (InterruptedException ex) {
                                log.error("Caught exception:", ex);
                                return "Failed to generate result for " + this.arguments.outputDirPath + " (" + tsg.
                                    getPrecursorName() + tsg.
                                        getPrecursorAdduct() + " )" + " and key=" + key + ", caused by: " + ex.
                                        getLocalizedMessage();
                            } catch (ExecutionException ex) {
                                log.error("Caught exception:", ex);
                                return "Failed to generate result for " + this.arguments.outputDirPath + " (" + tsg.
                                    getPrecursorName() + tsg.
                                        getPrecursorAdduct() + " )" + " and key=" + key + ", caused by: " + ex.
                                        getLocalizedMessage();
                            }
                        } else {
                            return "No result for " + this.arguments.outputDirPath + " and key=" + key;
                        }
                    });
            }).
            collect(Collectors.toList());
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.
            toArray(new CompletableFuture[futures.size()]));

        CompletableFuture<List<String>> allFutures = allOf.thenApply(v ->
        {
            return futures.stream().
                map(future ->
                    future.join()).
                collect(Collectors.toList());
        });
        allFutures.thenApply((t) ->
        {
            t.stream().
                forEach((output) ->
                {
                    log.
                        info(String.format(
                            "Processing results: %s", output));
                });
            return Void.TYPE;
        }).
            get();
        es.shutdown();
        es.awaitTermination(24, TimeUnit.HOURS);
    }
}
