package org.jmol.modelsetbio;

import org.jmol.c.STR;

public class Annotation extends ProteinStructure {

  private String atype;

  public Annotation(AlphaPolymer alphaPolymer, int monomerIndex,
      int monomerCount, String annotationType) {
    setupPS(alphaPolymer, STR.ANNOTATION, monomerIndex,
        monomerCount);
    this.atype = annotationType;
  }

}
