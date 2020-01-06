/*
 * 
 */
package de.isas.lipidomics.transitionextractor.services.transitions;

import java.util.Arrays;

/**
 *
 * @author Nils Hoffmann <nils.hoffmann@isas.de>
 */
public enum TransitionListColumnKeys {
    /*
    MoleculeGroup   PrecursorName   PrecursorFormula        PrecursorAdduct Precurso
rMz     PrecursorCharge ProductName     ProductFormula  ProductAdduct   ProductM
z       ProductCharge   Note
    */
    MOLECULE_GROUP("MoleculeGroup"),
    PRECURSOR_NAME("PrecursorName"),
    PRECURSOR_MOLECULE_SUM_FORMULA("PrecursorFormula"),
    PRECURSOR_ADDUCT("PrecursorAdduct"),
    PRECURSOR_MZ("PrecursorMz"),
    PRECURSOR_CHARGE("PrecursorCharge"),
    PRODUCT_NAME("ProductName"),
    PRODUCT_ADDUCT("ProductAdduct"),
    PRODUCT_MOLECULE_SUM_FORMULA("ProductFormula"),
    PRODUCT_MZ("ProductMz"),
    PRODUCT_CHARGE("ProductCharge"),
    NOTE("Note");
//    ,
//    FILE("File"),
//    PPMS("PPMS");

    private final String name;

    private TransitionListColumnKeys(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public static TransitionListColumnKeys fromName(String name) {
        return Arrays.stream(TransitionListColumnKeys.values()).
                filter((v) ->
                        v.toString().
                                equals(name)).
                findFirst().
                orElseThrow(() ->
                        new IllegalArgumentException("Unknown column name: '" + name+"'"));
    }
}
