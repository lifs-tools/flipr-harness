/*
 * 
 */
package de.isas.lipidomics.transitionextractor.services;

import java.nio.file.Path;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 *
 * @author Nils Hoffmann <nils.hoffmann@isas.de>
 */
@Data
@Builder
public class MappingSpecification {

    private String instrument;
    private String moleculeGroup;
    private String precursorName;
    private String precursorAdduct;
    private Path file;
    private List<Integer> ppms;
    private String group;
    private Integer minDataPoints;

}
