package org.jmol.script;

import javajs.util.Lst;
import javajs.util.M4;
import javajs.util.P3;

import org.jmol.java.BS;

public interface JmolSmilesExtension {

  JmolSmilesExtension init(Object se);

  float getSmilesCorrelation(BS bsA, BS bsB, String smiles, Lst<P3> ptsA,
                             Lst<P3> ptsB, M4 m4, Lst<BS> vReturn,
                             boolean isSmarts, boolean asMap, int[][] mapSet,
                             P3 center, boolean firstMatchOnly, boolean bestMap)
      throws ScriptException;

  Object getSmilesMatches(String pattern, String smiles, BS bsSelected,
                          BS bsMatch3D, boolean isSmarts, boolean asOneBitset)
      throws ScriptException;

  float[] getFlexFitList(BS bs1, BS bs2, String smiles1, boolean isSmarts)
      throws ScriptException;

}
