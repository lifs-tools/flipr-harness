/*
 * 
 */
package de.isas.lipidomics.transitionextractor;

import de.isas.lipidomics.transitionextractor.services.LipidTargetFinder;
import io.github.msdk.MSDKException;
import io.github.msdk.datamodel.ActivationInfo;
import io.github.msdk.datamodel.ActivationType;
import io.github.msdk.datamodel.IsolationInfo;
import io.github.msdk.datamodel.MsScan;
import io.github.msdk.io.mzml.MzMLFileImportMethod;
import io.github.msdk.io.mzml.data.MzMLCVParam;
import io.github.msdk.io.mzml.data.MzMLMsScan;
import io.github.msdk.io.mzml.data.MzMLPrecursorElement;
import io.github.msdk.io.mzml.data.MzMLRawDataFile;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author nilshoffmann
 */
@Data
@Slf4j
public class ConversionSupplier implements Supplier<Optional<Path>> {

    private final Path outputDirPath;
    private final TransitionSpecificationGroup transitionSpecGroup;
    private final boolean stopAtSmallestPpm;
    private final LipidTargetFinder ltf;
    private final LocalDateTime dateTimeCreated;

    public ConversionSupplier(LipidTargetFinder ltf, Path outputDirPath,
            TransitionSpecificationGroup transitionSpecGroup,
            boolean stopAtSmallestPpm, LocalDateTime dateTimeCreated) {
        this.outputDirPath = outputDirPath;
        this.transitionSpecGroup = transitionSpecGroup;
        this.stopAtSmallestPpm = stopAtSmallestPpm;
        this.ltf = ltf;
        this.dateTimeCreated = dateTimeCreated;
        log.debug("Conversion supplier: " + this.toString());
    }

    private Map<String, String> prepareScanDefaults(
            Path mzMlFile, MzMLMsScan msScan) {
        Map<String, String> scanDefaults = new LinkedHashMap<>();
        scanDefaults.put("instrument", transitionSpecGroup.getInstrument());
        scanDefaults.put("localDateTimeCreated", dateTimeCreated.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        scanDefaults.put("origin", mzMlFile.getFileName().
                toString());
        scanDefaults.put("scanNumber", safeToString(msScan.
                getScanNumber()));
        scanDefaults.put("polarity", msScan.getPolarity().
                toString());
        for (MzMLCVParam param : msScan.getCVParams().
                getCVParamsList()) {
            switch (param.getAccession().
                    trim()) {
                case "MS:1000504":
                    // <cvParam cvRef="MS" accession="MS:1000504" name="base peak m/z" value="464.4473309" unitCvRef="MS" unitAccession="MS:1000040" unitName="m/z"/>
                    scanDefaults.put(
                            "basePeakMz",
                            param.getValue().
                                    get());
                    break;
                case "MS:1000505":
                    // <cvParam cvRef="MS" accession="MS:1000505" name="base peak intensity" value="4.222928e06" unitCvRef="MS" unitAccession="MS:1000131" unitName="number of detector counts"/>
                    scanDefaults.put(
                            "basePeakIntensity",
                            param.getValue().
                                    get());
                    break;
                case "MS:1000285":
                    // <cvParam cvRef="MS" accession="MS:1000285" name="total ion current" value="5.3244205e06"/>
                    scanDefaults.put(
                            "totalIonCurrent",
                            param.getValue().
                                    get());
                    break;
                default:
                    log.debug(
                            "Unsupported CV term: " + param.
                                    getAccession() + " " + param.
                                    getName());
            }
        }
        if (!scanDefaults.containsKey("totalIonCurrent")) {
            scanDefaults.put("totalIonCurrent", msScan.
                    getCVValue(
                            "MS:1000285").
                    get());
        }
        String id = msScan.getId();
        scanDefaults.put("id", id);
        scanDefaults.put("scanDefinition", msScan.
                getScanDefinition());
        scanDefaults.put("msLevel",
                safeToString(msScan.getMsLevel()));
        handlePrecursorList(transitionSpecGroup.getInstrument(), msScan, scanDefaults);
        handleSourceInducedFragmentation(msScan,
                scanDefaults);
        handleIsolationInfo(msScan, scanDefaults);
        scanDefaults.put("msFunction", msScan.
                getMsFunction());
        scanDefaults.put("retentionTime", safeToString(
                msScan.
                        getRetentionTime()));
        scanDefaults.put("spectrumType", msScan.
                getSpectrumType().
                toString());
        Float rawTic = msScan.getTIC();
        scanDefaults.put("rawTic", safeToString(rawTic));
        return scanDefaults;
    }

    private boolean processLipidTargetSpecification(
            TransitionSpecification spec,
            double[] mzValues, Map<String, String> scanDefaults, MzMLMsScan msScan,
            float[] intensityValues, Float rawTic, boolean firstLine,
            final BufferedWriter fw, boolean stopAtSmallestPpm) throws IOException {
        boolean isFirstLine = firstLine;
        log.debug(String.format(
                "Applying lipid target specification '%s'",
                spec));
        for (int j = 0; j < mzValues.length; j++) {
            if (spec.isInRange(mzValues[j])) {
                log.debug(String.format(
                        "Checking m/z %.8f of scan %d",
                        mzValues[j], msScan.
                                getScanNumber()));
                List<Map<String, String>> results = ltf.apply(spec, mzValues[j],
                        intensityValues[j], rawTic,
                        scanDefaults, stopAtSmallestPpm);
                for (Map<String, String> line : results) {
                    if (isFirstLine) {
                        //write the header
                        fw.write(line.keySet().
                                stream().
                                collect(Collectors.joining("\t")));
                        fw.newLine();
                        isFirstLine = false;
                    }
                    fw.write(line.values().
                            stream().
                            collect(Collectors.joining("\t")));
                    fw.newLine();
                }
            }
        }
        return isFirstLine;
    }

    private void handlePrecursorList(String machineCvParam, MzMLMsScan msScan,
            Map<String, String> line) {
        List<MzMLPrecursorElement> precursorElementsList = msScan.
                getPrecursorList().
                getPrecursorElements();
        for (int i = 0; i < precursorElementsList.size(); i++) {
            MzMLPrecursorElement pel = precursorElementsList.get(i);
            final int index = i;
            pel.getIsolationWindow().
                    ifPresent((isolationWindow)
                            -> {
                        for (MzMLCVParam param : isolationWindow.
                                getCVParamsList()) {
                            switch (param.getAccession()) {
                                case "MS:1000827":
                                    // <cvParam cvRef="MS" accession="MS:1000827" name="isolation window target m/z" value="482.456756591797" unitCvRef="MS" unitAccession="MS:1000040" unitName="m/z"/>
                                    line.put(
                                            "isolationWindowTargetMz[" + index + "]",
                                            param.getValue().
                                                    get());
                                    break;
                                case "MS:1000828":
                                    // <cvParam cvRef="MS" accession="MS:1000828" name="isolation window lower offset" value="0.699999988079" unitCvRef="MS" unitAccession="MS:1000040" unitName="m/z"/>
                                    line.put(
                                            "isolationWindowLowerOffset[" + index + "]",
                                            param.getValue().
                                                    get());
                                    break;
                                case "MS:1000829":
                                    // <cvParam cvRef="MS" accession="MS:1000829" name="isolation window upper offset" value="0.699999988079" unitCvRef="MS" unitAccession="MS:1000040" unitName="m/z"/>
                                    line.put(
                                            "isolationWindowUpperOffset[" + index + "]",
                                            param.getValue().
                                                    get());
                                    break;
                                default:
                                    log.debug("Unsupported CV term: " + param.
                                            getAccession() + " " + param.
                                                    getName());
                            }
                            if (!line.containsKey(
                                    "isolationWindowTargetMz[" + index + "]")) {
                                throw new IllegalStateException(
                                        "PSI MS:1000827 term isolation window target m/z must be present on precursor list element " + index + " of scan " + msScan);
                            }
                            line.putIfAbsent(
                                    "isolationWindowLowerOffset[" + index + "]", Double.
                                            toString(0));
                            line.putIfAbsent(
                                    "isolationWindowUpperOffset[" + index + "]", Double.
                                            toString(0));
                        }
                    });
            for (MzMLCVParam param : pel.getActivation().
                    getCVParamsList()) {
                switch (param.getAccession()) {
                    /*
                    <activation>
                <cvParam cvRef="MS" accession="MS:1000422" name="beam-type collision-induced dissociation" value=""/>
                <cvParam cvRef="MS" accession="MS:1000045" name="collision energy" value="60.0" unitCvRef="UO" unitAccession="UO:0000266" unitName="electronvolt"/>
              </activation>

                     */
                    case "MS:1000422":
                        if (machineCvParam.equals("MS:1002523")) { //set this to HCD for Thermo QExHF
                            // <cvParam cvRef="MS" accession="MS:1000422" name="beam-type collision-induced dissociation" value=""/>
                            line.put("precursorActivationType", ActivationType.HCD.
                                    toString());
                        } else if (machineCvParam.equals("MS:1002791") || machineCvParam.equals("MS:1000490")) { // set this to CID for Agilent 6545 Q-TOF LC/MS  or Agilent instrument models
                            line.put("precursorActivationType", ActivationType.CID.
                                    toString());
                        } else {
                            line.put("precursorActivationType", "beam-type CID");
                        }
                        break;
                    case "MS:1000045":
                        // <cvParam cvRef="MS" accession="MS:1000045" name="collision energy" value="10.0" unitCvRef="UO" unitAccession="UO:0000266" unitName="electronvolt"/>
                        line.put("precursorCollisionEnergy",
                                param.getValue().
                                        get());
                        if (machineCvParam.equals("MS:1002523")) { //set this to Normalized Collision Energy for Thermo QExHF, the value reported by msConvert is wrongly reported as electronvolt
                            line.put("precursorCollisionEnergyUnit",
                                    "normalized");
                        } else {
                            if (param.getUnitAccession().
                                    get().
                                    equals("UO:0000266")) {
                                line.put("precursorCollisionEnergyUnit",
                                        "electronvolt");
                            } else {
                                line.put("precursorCollisionEnergyUnit",
                                        param.getUnitAccession().
                                                get());
                            }
                        }
                        break;
                    default:
                        log.debug("Unsupported CV term: " + param.
                                getAccession() + " " + param.getName());
                }
            }

        }
    }

    private void handleSourceInducedFragmentation(MzMLMsScan msScan,
            Map<String, String> line) {
        ActivationInfo sourceInducedFragmentation = msScan.
                getSourceInducedFragmentation();
        if (sourceInducedFragmentation != null) {
            line.put("sifActivationType", msScan.
                    getSourceInducedFragmentation().
                    getActivationType().
                    toString());
            line.put("sifActivationEnergy", safeToString(msScan.
                    getSourceInducedFragmentation().
                    getActivationEnergy()));
        }
    }

    private void handleIsolationInfo(MzMLMsScan msScan,
            Map<String, String> line) {
        if (msScan.getIsolations().
                size() > 1) {
            log.warn(
                    "MsScan " + msScan.getId() + " has multiple isolations: " + msScan.
                    getIsolations().
                    size());
        }
        log.debug("Number of isolations: " + msScan.
                getIsolations().
                size());
        for (int j = 0; j < msScan.getIsolations().
                size(); j++) {
            IsolationInfo isolationInfo = msScan.getIsolations().
                    get(j);
            if (isolationInfo != null) {
                line.put("ionInjectionTime[" + j + "]",
                        safeToString(
                                isolationInfo.
                                        getIonInjectTime()));
                line.put("isolationMzMin[" + j + "]", safeToString(
                        isolationInfo.
                                getIsolationMzRange().
                                lowerEndpoint()));
                line.put("isolationMzMax[" + j + "]", safeToString(
                        isolationInfo.
                                getIsolationMzRange().
                                upperEndpoint()));
                line.put("precursorCharge[" + j + "]", safeToString(
                        isolationInfo.
                                getPrecursorCharge()));
                line.put("precursorMz[" + j + "]", safeToString(
                        isolationInfo.
                                getPrecursorMz()));
                ActivationInfo ai = isolationInfo.
                        getActivationInfo();
                if (ai != null) {
                    line.put("activationEnergy[" + j + "]",
                            safeToString(
                                    ai.
                                            getActivationEnergy()));
                    line.put("activationType[" + j + "]", ai.
                            getActivationType().
                            toString());
                }
            }
        }
    }

    public static String safeToString(Float value) {
        if (value == null) {
            return "";
        }
        return Float.toString(value);
    }

    public static String safeToString(Double value) {
        if (value == null) {
            return "";
        }
        return Double.toString(value);
    }

    public static String safeToString(Integer value) {
        if (value == null) {
            return "";
        }
        return Integer.toString(value);
    }

    @Override
    public Optional<Path> get() {
        boolean firstLine = true;
        outputDirPath.toFile().mkdirs();
        Path lipidOutputFile = outputDirPath.resolve(
                transitionSpecGroup.getConfigKey().
                        toString().
                        replaceAll("/", "-").
                        replaceAll(";", "_").
                        replaceAll(":", "_").
                        replaceAll(" ", "_") + "_fip.tsv");
        try (BufferedWriter fw = new BufferedWriter(Files.
                newBufferedWriter(lipidOutputFile, Charset.
                        forName(
                                "UTF-8")))) {
            Path mzMlFile = transitionSpecGroup.getTargetFile();
            MzMLFileImportMethod parser = new MzMLFileImportMethod(
                    mzMlFile);
            final MzMLRawDataFile df;
            try {
                df = parser.execute();
                log.info(String.format(
                        "Processing file '%s' with %d transition target definitions.",
                        mzMlFile.toString(),
                        transitionSpecGroup.getSpecifications().
                                size()));
                log.debug(String.format(
                        "with MS functions: '%s'", df.getMsFunctions()));
                List<MsScan> scans = df.getScans();
                for (int i = 0; i < scans.size(); i++) {
                    MzMLMsScan msScan = (MzMLMsScan) scans.get(i);
                    if (msScan.getMsLevel() == 2) {
                        //potentially slow, so do not perform in loop below
                        Float rawTic = msScan.getTIC();
                        Map<String, String> scanDefaults = prepareScanDefaults(
                                mzMlFile, msScan);
                        double[] mzValues = msScan.getMzValues();
                        float[] intensityValues = msScan.
                                getIntensityValues();
                        for (TransitionSpecification spec : transitionSpecGroup.
                                getSpecifications()) {
                            firstLine = processLipidTargetSpecification(spec,
                                    mzValues, scanDefaults, msScan,
                                    intensityValues, rawTic, firstLine, fw,
                                    stopAtSmallestPpm);
                        }
                    } else {
                        log.debug(
                                "Skipping MS " + msScan.getMsLevel() + " scan at index " + i);
                    }
                }
            } catch (MSDKException ex) {
                log.error("Caught exception:", ex);
            } catch (IOException ex) {
                log.error("Caught exception:", ex);
                return Optional.empty();
            }

        } catch (IOException ex) {
            log.error("Caught exception:", ex);
            return Optional.empty();
        }
        return Optional.of(lipidOutputFile);
    }

}
