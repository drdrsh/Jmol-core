package org.jmol.api;





import org.jmol.java.BS;

import javajs.util.Lst;
import javajs.util.M4;
import javajs.util.T3;


public interface MOCalculationInterface {

  public abstract boolean setupCalculation(VolumeDataInterface volumeData,
                                           BS bsSelected, BS bsExclude,
                                           BS[] bsMolecules,
                                           String calculationType,
                                           T3[] atomCoordAngstroms,
                                           int firstAtomOffset,
                                           Lst<int[]> shells,
                                           float[][] gaussians,
                                           int[][] dfCoefMaps, Object slaters,
                                           float[] moCoefficients,
                                           float[] linearCombination,
                                           boolean isSquaredLinear,
                                           float[][] coefs,
                                           float[] partialCharges,
                                           boolean doNormalize, T3[] points,
                                           float[] parameters, int testFlags, M4 modelInvRotation);
  
  public abstract void createCube();
  public abstract float processPt(T3 pt);
}
