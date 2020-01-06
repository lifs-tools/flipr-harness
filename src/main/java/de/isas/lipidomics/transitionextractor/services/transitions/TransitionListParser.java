/*
 * 
 */
package de.isas.lipidomics.transitionextractor.services.transitions;

import com.google.common.collect.Range;
import de.isas.lipidomics.transitionextractor.ConfigKey;
import de.isas.lipidomics.transitionextractor.TransitionSpecification;
import de.isas.lipidomics.transitionextractor.MsUtils;
import de.isas.lipidomics.transitionextractor.TransitionSpecificationGroup;
import de.isas.lipidomics.transitionextractor.services.MappingSpecification;
import de.isas.lipidomics.transitionextractor.services.MappingUtils;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

/**
 *
 * @author Nils Hoffmann <nils.hoffmann@isas.de>
 */
@Slf4j
public class TransitionListParser {

    public Map<Path, TransitionSpecificationGroup> parse(
        Path cfgFilePath,
        Path mzMlDirectoryPath,
        MultiValuedMap<ConfigKey, MappingSpecification> mappingSpecs) throws IOException {
        MultiValuedMap<Path, TransitionSpecification> targetSpecs = new HashSetValuedHashMap<>();
        List<String> lines = Files.readAllLines(cfgFilePath, Charset.forName(
            "UTF-8"));
        String[] keys = null;
        for (int i = 0; i < lines.size(); i++) {
            if (i == 0) {
                keys = lines.get(i).
                    split("\t");
            } else {
                Map<TransitionListColumnKeys, String> lineElements = new HashMap<>();
                String[] lineContents = lines.get(i).
                    split("\t");
                for (int j = 0; j < lineContents.length; j++) {
                    lineElements.put(TransitionListColumnKeys.fromName(keys[j]),
                        cleanString(lineContents[j]));
                }
                log.debug(String.format("Processing line: '%s'", lines.get(i)));
                ConfigKey transitionSpecKey = MappingUtils.getSafeKey(
                    pick(
                        TransitionListColumnKeys.MOLECULE_GROUP,
                        lineElements), pick(
                        TransitionListColumnKeys.PRECURSOR_NAME,
                        lineElements), pick(
                        TransitionListColumnKeys.PRECURSOR_ADDUCT,
                        lineElements));
                Collection<MappingSpecification> mappingSpecsForKey = mappingSpecs.
                    get(
                        transitionSpecKey);
                log.debug("Handling {} mapping specifications for key {}",
                    mappingSpecsForKey.size(), transitionSpecKey);
                for (MappingSpecification mapping : mappingSpecsForKey) {
                    TreeMap<Integer, Range<Double>> ppmToRangeMap = MsUtils.
                        createIonTargetMzRanges(
                            Double.parseDouble(pick(
                                TransitionListColumnKeys.PRODUCT_MZ,
                                lineElements)), mapping.getPpms());
                    TransitionSpecification lts = TransitionSpecification.
                        builder().
                        moleculeGroup(pick(
                            TransitionListColumnKeys.MOLECULE_GROUP,
                            lineElements)).
                        file(mapping.getFile()).
                        precursorName(pick(
                            TransitionListColumnKeys.PRECURSOR_NAME,
                            lineElements)).
                        fragmentName(pick(
                            TransitionListColumnKeys.PRODUCT_NAME,
                            lineElements)).
                        fragmentAdduct(pick(
                            TransitionListColumnKeys.PRODUCT_ADDUCT,
                            lineElements)).
                        calculatedMass(Double.parseDouble(pick(
                            TransitionListColumnKeys.PRODUCT_MZ,
                            lineElements))).
                        ionTargetMzRanges(ppmToRangeMap).
                        maximumMzRange(MsUtils.getMaximumMzRange(
                            ppmToRangeMap)).
                        mappingSpecification(mapping).
                        build();

                    log.
                        info(String.format(
                            "Adding target spec for moleculeGroup '%s', precursor '%s %s', fragment '%s %s' with group '%s'",
                            lts.getMoleculeGroup(), lts.getPrecursorName(), lts.
                            getPrecursorAdduct(), lts.getFragmentName(), lts.
                            getFragmentAdduct(), lts.getGroup()));
                    targetSpecs.put(lts.getFile(), lts);
                }
            }
        }
        Map<Path, TransitionSpecificationGroup> pathToGroups = new LinkedHashMap<>();
        for (Path p : targetSpecs.keySet()) {
            Collection<TransitionSpecification> transitions = targetSpecs.get(p);
            TransitionSpecificationGroup group = null;
            for (TransitionSpecification tspec : transitions) {
                log.debug("Handling transition spec {}", tspec);
                if (group == null) {
                    group = new TransitionSpecificationGroup(tspec.getInstrument(), tspec.
                        getMoleculeGroup(), tspec.getPrecursorName(), tspec.
                        getPrecursorAdduct(), p, tspec.getGroup());
                }
                group.addTransitionSpecification(tspec);
            }
            pathToGroups.put(p, group);
            log.info("Added {} specs for file {}", pathToGroups.get(
                p).
                getSpecifications().
                size(), p);
        }
        return pathToGroups;
    }

    private String pick(TransitionListColumnKeys key,
        Map<TransitionListColumnKeys, String> lineElements) {
        return lineElements.get(key);
    }

    /**
     * Trims the string, removing leading and trailing whitespace, then removes
     * leading and trailing double and single quotes. Finally trims again to
     * remove any surviving, extraneous whitespace.
     *
     * @param string
     * @return
     */
    private String cleanString(String string) {
        return string.trim().
            replaceAll("^[\"]+|[\"]+$", "").
            trim();
    }
}
