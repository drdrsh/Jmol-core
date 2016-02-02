package org.jmol.api;





import javajs.util.Lst;
import javajs.util.T3;

import org.jmol.java.BS;


public interface MOCalculationInterface {

  public abstract boolean setupCalculation(VolumeDataInterface volumeData,
                                           BS bsSelected, BS bsExclude,
                                           BS[] bsMolecules,
                                           String calculationType,
                                           T3[] atomCoordAngstroms,
                                           T3[] atoms,
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
                                           float[] parameters, int testFlags);
  
  public abstract void createCube();
  public abstract float processPt(T3 pt);
}
