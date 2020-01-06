/*
 * 
 */
package de.isas.lipidomics.transitionextractor;

import java.util.Arrays;

/**
 *
 * @author Nils Hoffmann <nils.hoffmann@isas.de>
 */
public enum Element {

    //carbon
    C("C", "Carbon", 6, 6, 12.0d, 12.010736),
    C_12("[12]C", "Carbon", 6, 6, 12.0d, 12.0d),
    C_13("[13]C", "Carbon", 6, 7, 13.003355d, 13.003355d),
    C_14("[14]C", "Carbon", 6, 8, 14.003242d, 14.003242d),
    //hydrogen
    H("H", "Hydrogen", 1, 0, 1.007825d, 1.007941d),
    H_0("[0]H", "Hydrogen", 1, 0, 1.007825d, 1.007825d),
    H_1("d", "Deuterium", 1, 1, 2.014102d, 2.014102d),
    H_2("t", "Tritium", 1, 2, 3.016049, 3.016049),
    //oxygen
    O("O", "Oxygen", 8, 8, 15.99491463d, 15.999405d),
    O_16("[16]O", "Oxygen", 8, 8, 15.99491463d, 15.99491463d),
    O_17("[17]O", "Oxygen", 8, 9, 16.999132d, 16.999132d),
    O_18("[18]O", "Oxygen", 8, 10, 17.999160d, 17.999160d),
    //nitrogen
    N("N", "Nitrogen", 7, 7, 14.003074d, 14.006743d),
    N_14("[14]N", "Nitrogen", 7, 7, 14.003074d, 14.003074d),
    N_15("[15]N", "Nitrogen", 7, 8, 15.000109d, 15.000109d),
    //phosphorus
    P("P", "Phosphorus", 15, 16, 30.973762d, 30.973762d),
    P_31("[31]P", "Phosphorus", 15, 16, 30.973762d, 30.973762d),
    //sulfur
    S("S", "Sulfur", 16, 16, 31.972071d, 32.066085d),
    S_32("[32]S", "Sulfur", 16, 16, 31.972071d, 31.972071d),
    S_33("[33]S", "Sulfur", 16, 17, 32.971458d, 32.971458d),
    S_34("[34]S", "Sulfur", 16, 18, 33.967867d, 33.967867d),
    S_36("[36]S", "Sulfur", 16, 20, 35.967081, 35.967081),
    //sodium
    Na("Na", "Sodium", 11, 12, 22.989770d, 22.989770d),
    Na_23("[23]Na", "Sodium", 11, 12, 22.989770d, 22.989770d),
    //electron
    e("e-", "Electron", 0, 0, 0.00054858026d, 0.00054858026d);

    private final String symbol;
    private final String name;
    private final Integer protons;
    private final Integer neutrons;
    private final Double mass;
    private final Double averageIsotopicMass;

    private Element(String symbol, String name, Integer protons,
        Integer neutrons, Double mass, Double averageIsotopicMass) {
        this.symbol = symbol;
        this.name = name;
        this.protons = protons;
        this.neutrons = neutrons;
        this.mass = mass;
        this.averageIsotopicMass = averageIsotopicMass;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public String getSymbol() {
        return this.symbol;
    }

    public String getName() {
        return this.name;
    }

    public Integer getProtons() {
        return this.protons;
    }

    public Integer getNeutrons() {
        return this.neutrons;
    }

    public Double getMass() {
        return mass;
    }
    
    public Double getAverageIsotopicMass() {
        return averageIsotopicMass;
    }

    public static Element fromName(String name) {
        return Arrays.stream(Element.values()).
            filter((v) ->
                v.toString().
                    equals(name)).
            findFirst().
            orElseThrow(() ->
                new IllegalArgumentException("Unknown element name:" + name));
    }

    public static Element fromSymbol(String symbol) {
        return Arrays.stream(Element.values()).
            filter((v) ->
                v.toString().
                    equals(symbol)).
            findFirst().
            orElseThrow(() ->
                new IllegalArgumentException("Unknown element symbol:" + symbol));
    }
}
