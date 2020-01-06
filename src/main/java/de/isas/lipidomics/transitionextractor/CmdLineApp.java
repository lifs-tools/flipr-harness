/*
 * 
 */
package de.isas.lipidomics.transitionextractor;

import de.isas.lipidomics.transitionextractor.services.LipidTargetFinder;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 *
 * @author Nils Hoffmann <nils.hoffmann@isas.de>
 */
public class CmdLineApp {

    private static final Logger LOG = LoggerFactory.getLogger(CmdLineApp.class);

    public static void main(String[] args) {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        Options options = new Options();
        options.addOption("p", true,
                "the base directory to use to locate lipidcreator-parameters.tsv files for concatenation. (optional)");
        options.addOption("i", true,
                "the transition file to use. (optional)");
        options.addOption("j", true,
                "the mapping file to use. (optional)");
        options.addOption("o", true, "the output directory (optional)");
        options.addOption("t", true,
                "the number of threads / cpus to use for processing (optional)");
        options.addOption("m", true,
                "the minimum inclusive collision energy to use for model calculations (optional), default: 0");
        options.addOption("n", true,
                "the total number of parameter combinations to use for grid search optimization of the prediction model parameters (optional), default: 5000");
        options.addOption("f", true,
                "the plot file format, one of 'png', 'svg' or 'pdf' (optional), default: 'png'");
        Option opt = Option.builder().hasArg(true).longOpt("flipr").desc("the path to the flipr Rscript file").numberOfArgs(1).optionalArg(true).argName("flipr").build();
        options.addOption(opt);
        options.addOption("d", true,
                "whether to plot diagnostic plots of the data (optional), default: false");
        options.addOption("c", true, "the configuration file (optional)");
        options.addOption("x", true, "the flipr configuration settings file for regression parameter bounds (optional)");
        options.addOption("h", "help", false,
                "print help");
        options.addOption("?", false,
                "print help");
        CommandLineParser parser = new DefaultParser();
        try {
            PropertiesConfiguration defaultProperties = new PropertiesConfiguration("config.properties");

            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("h") || cmd.hasOption("?")) {
                printHelpAndExit(options, defaultProperties.getString("version", "<VERSION>"));
            }
            if (cmd.hasOption("p")) {
                concatenateLipidCreatorParameters(cmd);
            }

            CompositeConfiguration cc = new CompositeConfiguration();

            if (cmd.hasOption("c")) {
                try {
                    PropertiesConfiguration pc = new PropertiesConfiguration(cmd.getOptionValue("c"));
                    cc.addConfiguration(pc);
                } catch (ConfigurationException ex) {
                    LOG.error("Caught exception:", ex);
                    System.exit(1);
                }
            }
            cc.addConfiguration(defaultProperties);
            Path fliprCfg = null;
            if (cmd.hasOption("x")) {
                String xOption = cmd.getOptionValue("x");
                if (xOption == null || xOption.isEmpty()) {
                    LOG.warn(
                            "x option (flipr R configuration) was provided but no value was set. Please provide with -x <x>, where <x> needs to be a proper file path to a .R file.");
                    printHelpAndExit(options, defaultProperties.getString("version", "<VERSION>"));
                }
                fliprCfg = new File(xOption).toPath();
            }

            Path fliprPath = null;
            if (cmd.hasOption("flipr")) {
                String fliprOption = cmd.getOptionValue("flipr");
                if (fliprOption == null || fliprOption.isEmpty()) {
                    LOG.warn(
                            "Flipr option was provided but no value was set. Please provide with --flipr <flipr>, where <flipr> needs to be a proper file path.");
                    printHelpAndExit(options, defaultProperties.getString("version", "<VERSION>"));
                }
                fliprPath = new File(fliprOption).toPath();
            }

            Integer availableThreads = Runtime.getRuntime().
                    availableProcessors() - 1;
            Integer threads = cc.getInt("threads", availableThreads);
            if (cmd.hasOption("t")) {
                threads = Math.min(availableThreads, Integer.parseInt(cmd.
                        getOptionValue("t")));
            }
            LOG.info(String.format(
                    "Using %d of %d available processors!",
                    threads, Runtime.getRuntime().
                            availableProcessors()));
            final Integer nThreads = threads;
            Path dataFileInputDir = null;
            DateTime now = DateTime.now(DateTimeZone.getDefault());
            DateTimeFormatter dateFormatter = ISODateTimeFormat.
                    dateHourMinuteSecond();
            String output = dateFormatter.print(now).
                    replace(":", "-") + "Z";
            Path outputDir = Paths.get(".", output);
            String transitionFileStr = cc.getString("transitionFile");
            if (cmd.hasOption("i")) {
                transitionFileStr = cmd.getOptionValue("i");
            }
            if (transitionFileStr == null) {
                LOG.warn(
                        "Transition file was not set. Either provide with -i <transition-file.tsv> or set the transitionFile property in the configuration file.");
                printHelpAndExit(options, defaultProperties.getString("version", "<VERSION>"));
            }
            LOG.info("Using transition file " + transitionFileStr);
            File transitionFile = new File(transitionFileStr);

            String mappingFileStr = cc.getString("mappingFile");
            if (cmd.hasOption("j")) {
                mappingFileStr = cmd.getOptionValue("j");
            }
            if (mappingFileStr == null) {
                LOG.warn(
                        "Mapping file was not set. Either provide with -i <mapping-file.tsv> or set the mappingFile property in the configuration file.");
                printHelpAndExit(options, defaultProperties.getString("version", "<VERSION>"));
            }
            LOG.info("Using mapping file " + mappingFileStr);
            Path mappingFile = new File(mappingFileStr).toPath();

            dataFileInputDir = transitionFile.getAbsoluteFile().
                    getParentFile().
                    toPath();
            if (cmd.hasOption("o")) {
                outputDir = Paths.get(cmd.getOptionValue("o"));
                outputDir.toFile().
                        mkdirs();
            }
            double minCollisionEnergy = cc.getDouble(
                    "minPrecursorCollisionEnergy", 0.0d);
            if (cmd.hasOption("m")) {
                minCollisionEnergy = Double.parseDouble(cmd.getOptionValue("m"));
            }
            String plotFormat = cc.getString("plotFormat", "png");
            if (cmd.hasOption("f")) {
                plotFormat = cmd.getOptionValue("f");
            }
            boolean diagnosticPlots = cc.getBoolean("diagnosticPlots", false);
            if (cmd.hasOption("d")) {
                diagnosticPlots = Boolean.parseBoolean(cmd.getOptionValue("d"));
            }
            boolean stopAtSmallestPpm = cc.
                    getBoolean("stopAtSmallestPpm", false);
            final File outputDirectory = outputDir.toFile();
            
            Integer maxCombinations = cc.getInteger("maxCombinations", 5000);
            if (cmd.hasOption("n")) {
                maxCombinations = Integer.parseInt(cmd.getOptionValue("x"));
            }

            runConversion(transitionFile.toPath(), mappingFile, dataFileInputDir,
                    outputDirectory,
                    plotFormat, diagnosticPlots,
                    minCollisionEnergy, nThreads, stopAtSmallestPpm, fliprCfg, fliprPath, maxCombinations);

        } catch (ParseException | IOException | ConfigurationException ex) {
            LOG.error("Caught exception:", ex);
        }
    }

    protected static void runConversion(Path transitionFile, Path mappingFile,
            Path inputDir,
            final File outputDirectory,
            final String plotFormat, final boolean diagnosticPlots,
            final double minCollisionEnergy, final Integer nThreads,
            final boolean stopAtSmallestPpm, Path fliprConfigFile, Path fliprPath, Integer maxCombinations) throws IOException {
        if (transitionFile == null) {
            throw new NullPointerException(
                    "Parameter transitionFile must not be null!");
        }
        if (mappingFile == null) {
            throw new NullPointerException(
                    "Parameter mappingFile must not be null!");
        }
        if (inputDir == null) {
            throw new NullPointerException(
                    "Parameter inputDir must not be null!");
        }
        if (outputDirectory == null) {
            throw new NullPointerException(
                    "Parameter outputDirectory must not be null!");
        }
        if (fliprConfigFile == null) {
            LOG.warn("No config.R file for flipr provided, running with default parameter bounds!");
        }
        if (fliprPath == null) {
            LOG.warn("No flipr path was provided, using embedded flipr.R script, this may be incompatible with the installed flipr version!");
        }

//        final AtomicInteger filesConverted = new AtomicInteger();
//        final AtomicInteger filesToConvert = new AtomicInteger();
        final LipidTargetFinder ltf = new LipidTargetFinder();

//            Path infile = filePath.getParent();
//            Path configFile = transitionFile;
        String configOutputDir = transitionFile.getFileName().
                toString().
                toLowerCase().
                replace(".tsv", "");
        Path outPath = outputDirectory.toPath().
                resolve(configOutputDir);
        Converter.Arguments convArgs = new Converter.Arguments(ltf,
                inputDir, outPath, transitionFile, mappingFile, plotFormat,
                diagnosticPlots, minCollisionEnergy, fliprConfigFile, fliprPath, maxCombinations);
        LOG.info(String.format("Running with arguments: %s", convArgs));
        Converter conv = new Converter(convArgs);
        try {
            conv.convert(nThreads, stopAtSmallestPpm);
        } catch (IOException ex) {
            LOG.error("Caught exception:", ex);
        } catch (InterruptedException ex) {
            LOG.error("Caught exception:", ex);
        } catch (ExecutionException ex) {
            LOG.error("Caught exception:", ex);
        }
    }

    protected static void printHelpAndExit(Options options, String version) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar transition-extractor-" + version + ".jar", options);
        System.exit(-1);
    }

    protected static void concatenateLipidCreatorParameters(CommandLine cmd) {
        String outputDir = cmd.getOptionValue("p");
        if (outputDir == null) {
            throw new NullPointerException(
                    "Parameter p must not be null!");
        }
        Path outputDirPath = Paths.get(outputDir);
        LOG.info(String.format("Collecting files from below %s", outputDirPath));
        List<Path> parameters;
        try {
            parameters = Files.walk(outputDirPath).
                    filter(Files::isRegularFile).
                    filter((t)
                            -> {
                        LOG.info(String.format("Checking file %s", t));
                        return true;
                    }).
                    filter((t)
                            -> {
                        return t.getFileName().
                                toString().
                                endsWith("-lipidcreator-parameters.csv");
                    }).
                    collect(Collectors.toList());
            String header = null;
            Path lipidCreatorMergedParamsFile = outputDirPath.
                    resolve("lipidcreator-parameters.csv");
            LOG.info(String.format(
                    "Writing concatenated lipidcreator parameters to %s",
                    lipidCreatorMergedParamsFile));
            try (BufferedWriter fw = new BufferedWriter(Files.
                    newBufferedWriter(lipidCreatorMergedParamsFile, Charset.
                            forName(
                                    "UTF-8")))) {
                for (Path p : parameters) {
                    LOG.info(String.format("Appending file %s", p));
                    List<String> lines = Files.readAllLines(p, Charset.forName(
                            "UTF-8"));
                    if (!lines.isEmpty()) {
                        for (int i = 0; i < lines.size(); i++) {
                            String s = lines.get(i);
                            if (header == null) {
                                header = s;
                                fw.write(header);
                                fw.newLine();
                            } else {
                                if (i > 0) {
                                    fw.write(s);
                                    fw.newLine();
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            LOG.error(
                    "Caught exception while trying to concatenate lipid creator parameters:",
                    ex);
            System.exit(-1);
        }

        System.exit(0);
    }
}
