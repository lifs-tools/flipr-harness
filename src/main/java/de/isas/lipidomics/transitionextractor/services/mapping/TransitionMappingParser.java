/*
 * 
 */
package de.isas.lipidomics.transitionextractor.services.mapping;

import de.isas.lipidomics.transitionextractor.ConfigKey;
import de.isas.lipidomics.transitionextractor.services.MappingSpecification;
import de.isas.lipidomics.transitionextractor.services.MappingUtils;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

/**
 *
 * @author Nils Hoffmann <nils.hoffmann@isas.de>
 */
@Slf4j
public class TransitionMappingParser {

    public MultiValuedMap<ConfigKey, MappingSpecification> parse(
        Path mappingFilePath,
        Path mzMlDirectoryPath) throws IOException {
        MultiValuedMap<ConfigKey, MappingSpecification> mappingSpecs = new HashSetValuedHashMap<>();
        List<String> lines = Files.readAllLines(mappingFilePath, Charset.
            forName(
                "UTF-8"));
        String[] keys = null;
        for (int i = 0; i < lines.size(); i++) {
            if (i == 0) {
                keys = lines.get(i).
                    split("\t");
            } else {
                Map<TransitionMappingColumnKeys, String> lineElements = new HashMap<>();
                String[] lineContents = lines.get(i).
                    split("\t");
                for (int j = 0; j < lineContents.length; j++) {
                    lineElements.put(TransitionMappingColumnKeys.fromName(
                        keys[j]),
                        MappingUtils.cleanString(lineContents[j]));
                }
                log.debug(String.format("Processing line: '%s'", lines.get(i)));
                MappingSpecification lts = MappingSpecification.
                    builder().
                    instrument(pick(TransitionMappingColumnKeys.INSTRUMENT,
                        lineElements)).
                    moleculeGroup(pick(
                        TransitionMappingColumnKeys.MOLECULE_GROUP, lineElements)).
                    precursorName(pick(
                        TransitionMappingColumnKeys.PRECURSOR_NAME, lineElements)).
                    precursorAdduct(pick(
                        TransitionMappingColumnKeys.PRECURSOR_ADDUCT,
                        lineElements)).
                    file(mzMlDirectoryPath.resolve(pick(
                        TransitionMappingColumnKeys.FILE, lineElements))
                    ).
                    ppms(pickPpms(lineElements)).
                    group(pick(TransitionMappingColumnKeys.GROUP, lineElements)).
                    minDataPoints(Integer.parseInt(pickOrDefault(TransitionMappingColumnKeys.MIN_DATA_POINTS, lineElements, "50"))).
                    build();
                ConfigKey key = new ConfigKey(lts.getMoleculeGroup(), lts.
                    getPrecursorName(), lts.getPrecursorAdduct());
                log.debug(String.format(
                    "Adding MappingSpecification for key '%s': '%s'", key, lts));
                mappingSpecs.put(key, lts);
            }
        }
        return mappingSpecs;
    }

    protected String[] pickFiles(
        Map<TransitionMappingColumnKeys, String> lineElements) {
        String filesString = pick(TransitionMappingColumnKeys.FILE, lineElements);
        String[] files = filesString.contains("|") ? filesString.split(
            "\\|") : new String[]{filesString};
        return files;
    }

    private String pickOrDefault(TransitionMappingColumnKeys key,
            Map<TransitionMappingColumnKeys, String> lineElements, String defaultValue) {
        return lineElements.containsKey(key)?lineElements.get(key):defaultValue;
    }
    
    private String pick(TransitionMappingColumnKeys key,
        Map<TransitionMappingColumnKeys, String> lineElements) {
        return lineElements.get(key);
    }

    private List<Integer> pickPpms(
        Map<TransitionMappingColumnKeys, String> lineElements) {
        String[] ppms = pick(TransitionMappingColumnKeys.PPMS, lineElements).
            split("\\|");
        return Arrays.stream(ppms).
            mapToInt((value) ->
            {
                return Integer.valueOf(value.trim());
            }).
            boxed().
            collect(Collectors.toList());
    }

}
