package org.jmol.modelset;

import javajs.util.Lst;
import javajs.util.OC;
import javajs.util.SB;

import org.jmol.java.BS;

public interface JmolBioModel {

  Lst<BS> getBioBranches(Lst<BS> biobranches);

  int getBioPolymerCount();

  void getDefaultLargePDBRendering(SB sb, int maxAtoms);

  String getFullPDBHeader();

  void getPdbData(String type, char ctype, boolean isDraw,
                         BS bsSelected, OC out, LabelToken[] tokens,
                         SB pdbCONECT, BS bsWritten);

  void resetRasmolBonds(BS bs);

}
