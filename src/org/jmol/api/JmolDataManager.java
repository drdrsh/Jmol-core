package org.jmol.api;

import javajs.util.SB;

import org.jmol.c.VDW;
import org.jmol.java.BS;
import org.jmol.viewer.JmolStateCreator;
import org.jmol.viewer.Viewer;

public interface JmolDataManager {

  JmolDataManager set(Viewer vwr);

  boolean getDataState(JmolStateCreator stateCreator, SB commands);

  void clear();

  Object[] getData(String type);

  float[][] getDataFloat2D(String label);

  float[][][] getDataFloat3D(String label);

  float getDataFloat(String label, int atomIndex);

  float[] getDataFloatA(String label);

  String getDefaultVdwNameOrData(VDW type, BS bs);

  void deleteModelAtoms(int firstAtomIndex, int nAtoms, BS bsDeleted);

  void setData(String type, Object[] data, int arrayCount, int ac,
               int matchField, int matchFieldColumnCount, int field,
               int fieldColumnCount);

}
