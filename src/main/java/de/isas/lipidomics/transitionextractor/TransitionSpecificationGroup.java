/*
 * 
 */
package de.isas.lipidomics.transitionextractor;

import de.isas.lipidomics.transitionextractor.services.GroupedConfigKey;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 *
 * @author nilshoffmann
 */
@Data
public class TransitionSpecificationGroup {

    private final List<TransitionSpecification> specifications = new ArrayList<>();
    private final String instrument;
    private final String moleculeGroup;
    private final String precursorName;
    private final String precursorAdduct;
    private final Path targetFile;
    private final String group;

    private final GroupedConfigKey configKey;

    public TransitionSpecificationGroup(String instrument, String moleculeGroup,
        String precursorName, String precursorAdduct, Path targetFile,
        String group
    ) {
        this.instrument = instrument;
        this.moleculeGroup = moleculeGroup;
        this.precursorName = precursorName;
        this.precursorAdduct = precursorAdduct;
        this.targetFile = targetFile;
        this.group = group;
        this.configKey = new GroupedConfigKey(moleculeGroup, precursorName,
            precursorAdduct, group);
    }

    public void addTransitionSpecification(TransitionSpecification ts) throws IllegalArgumentException {
        if (!instrument.equals(ts.getInstrument())) {
            throw new IllegalArgumentException(
                "Instrument of molecule group specification " + ts.
                    getInstrument()+ " does not match expected one: " + instrument + " for target file " + targetFile);
        }
        if (!moleculeGroup.equals(ts.getMoleculeGroup())) {
            throw new IllegalArgumentException(
                "Adduct of molecule group specification " + ts.
                    getMoleculeGroup() + " does not match expected one: " + moleculeGroup + " for target file " + targetFile);
        }
        if (!precursorName.equals(ts.getPrecursorName())) {
            throw new IllegalArgumentException(
                "Precursor name of transition specification " + ts.
                    getPrecursorName() + " does not match expected one: " + precursorName + " for target file " + targetFile);
        }
        if (!precursorAdduct.equals(ts.getPrecursorAdduct())) {
            throw new IllegalArgumentException(
                "Precursor adduct of transition specification " + ts.
                    getPrecursorAdduct() + " does not match expected one: " + precursorAdduct + " for target file " + targetFile);
        }
        if (!group.equals(ts.getGroup())) {
            throw new IllegalArgumentException(
                "Group of transition specification " + ts.
                    getGroup() + " does not match expected one: " + group + " for target file " + targetFile);
        }
        this.specifications.add(ts);
    }

}
