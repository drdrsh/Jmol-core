package org.jmol.api;



import org.jmol.java.BS;

import javajs.util.Lst;
import org.jmol.util.Node;

public interface SmilesMatcherInterface {

  public abstract String getLastException();

  public int areEqual(String smiles1, String smiles2) throws Exception;

  public abstract BS[] find(String pattern,/* ...in... */String smiles,
                                boolean isSmarts, boolean firstMatchOnly) throws Exception;

  public abstract BS getSubstructureSet(String pattern, Node[] atoms,
                                            int ac, BS bsSelected,
                                            boolean isSmarts,
                                            boolean firstMatchOnly) throws Exception;

  public abstract BS[] getSubstructureSetArray(String pattern,
                                                   Node[] atoms,
                                                   int ac,
                                                   BS bsSelected,
                                                   BS bsAromatic,
                                                   boolean isSmarts,
                                                   boolean firstMatchOnly) throws Exception;

  public abstract int[][] getCorrelationMaps(String pattern, Node[] atoms,
                                             int ac, BS bsSelected,
                                             boolean isSmarts,
                                             boolean firstMatchOnly) throws Exception;

  public abstract String getMolecularFormula(String pattern, boolean isSearch) throws Exception;

  public abstract String getSmiles(Node[] atoms, int ac,
                                   BS bsSelected, boolean asBioSmiles,
                                   boolean bioAllowUnmatchedRings,
                                   boolean bioAddCrossLinks, String bioComment,
                                   boolean explicitH) throws Exception;

  public abstract String getRelationship(String smiles1, String smiles2) throws Exception;

  public abstract String reverseChirality(String smiles) throws Exception;

  public abstract void getSubstructureSets(String[] smarts, Node[] atoms, int ac,
                                           int flags,
                         BS bsSelected, Lst<BS> bitSets, Lst<BS>[] vRings) throws Exception;

}
