package org.jmol.api;

import java.util.Map;


import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.ModelSet;
import javajs.util.Lst;
import javajs.util.P3;

import org.jmol.util.Tensor;
import org.jmol.viewer.Viewer;

import javajs.util.M3;
import javajs.util.M4;
import javajs.util.Matrix;
import javajs.util.P3i;
import javajs.util.Quat;
import javajs.util.SB;
import javajs.util.T3;
import javajs.util.V3;

public interface SymmetryInterface {

  public int addBioMoleculeOperation(M4 mat, boolean isReverse);

  public boolean addLatticeVectors(Lst<float[]> lattvecs);

  public String addOp(String code, Matrix rs, Matrix vs, Matrix sigma);

  public int addSpaceGroupOperation(String xyz, int opId);

  public boolean checkDistance(P3 f1, P3 f2, float distance, 
                                        float dx, int iRange, int jRange, int kRange, P3 ptOffset);

  public boolean checkUnitCell(SymmetryInterface uc, P3 cell, P3 ptTemp, boolean isAbsolute);

  public boolean createSpaceGroup(int desiredSpaceGroupIndex,
                                           String name,
                                           Object data);

  public String fcoord(T3 p);

  public P3[] getCanonicalCopy(float scale, boolean withOffset);

  public P3 getCartesianOffset();

  public int[] getCellRange();

  public boolean getCoordinatesAreFractional();

  public P3 getFractionalOffset();

  public Object getLatticeDesignation();

  public int getLatticeOp();

  public String getMatrixFromString(String xyz, float[] temp, boolean allowScaling, int modDim);

  Lst<String> getMoreInfo();

  public float[] getUnitCellParams();

  public Matrix getOperationRsVs(int op);
  
  public Object getPointGroupInfo(int modelIndex, boolean asDraw,
                                           boolean asInfo, String type,
                                           int index, float scale);

  public String getPointGroupName();

  public int getSiteMultiplicity(P3 a);

  public Object getSpaceGroup();

  public Map<String, Object> getSpaceGroupInfo(ModelSet modelSet, String spaceGroup);

  public String getSpaceGroupInfoStr(String name, SymmetryInterface cellInfo);

  public String getSpaceGroupName();

  public M4 getSpaceGroupOperation(int i);

  public String getSpaceGroupOperationCode(int op);

  public int getSpaceGroupOperationCount();

  public String getSpaceGroupXyz(int i, boolean doNormalize);

  public Object getSymmetryInfo(ModelSet modelSet, int iModel, int iAtom, SymmetryInterface uc, String xyz, int op,
                                P3 pt, P3 pt2, String id, int type);

  String getSymmetryInfoString(ModelSet modelSet, int modelIndex, int symOp, P3 pt1,
                               P3 pt2, String drawID, String type);

  public String getSymmetryInfoStr();

  public M4[] getSymmetryOperations();

  public float getSpinOp(int op);

  public SymmetryInterface getUnitCell(T3[] points, boolean setRelative, String name);

  public float[] getUnitCellAsArray(boolean vectorsOnly);

  public String getUnitCellInfo();

  public float getUnitCellInfoType(int infoType);

  public P3 getUnitCellMultiplier();

  public String getUnitCellState();
  
  public P3[] getUnitCellVectors();

  public P3[] getUnitCellVerticesNoOffset();

  public boolean haveUnitCell();

  public boolean isBio();

  public boolean isPeriodic();

  public boolean isPolymer();

  public boolean isSlab();

  public boolean isSupercell();

  public void newSpaceGroupPoint(int i, P3 atom1, P3 atom2,
                                          int transX, int transY, int transZ);

  public BS notInCentroid(ModelSet modelSet, BS bsAtoms,
                          int[] minmax);

  public V3[] rotateAxes(int iop, V3[] axes, P3 ptTemp, M3 mTemp);

  public void setFinalOperations(String name, P3[] atoms,
                                          int iAtomFirst,
                                          int noSymmetryCount, boolean doNormalize, String filterSymop);

  /**
   * set symmetry lattice type using Hall rotations
   * 
   * @param latt SHELX index or character lattice character P I R F A B C S T or \0
   * 
   */
  public void setLattice(int latt);

  public void setMinMaxLatticeParameters(P3i minXYZ, P3i maxXYZ);

  public void setOffset(int nnn);

  public void setOffsetPt(T3 pt);

  public SymmetryInterface setPointGroup(
                                     SymmetryInterface pointGroupPrevious,
                                     T3 center, T3[] atomset,
                                     BS bsAtoms,
                                     boolean haveVibration,
                                     float distanceTolerance, float linearTolerance, boolean localEnvOnly);

  public void setSpaceGroup(boolean doNormalize);

  public void setSpaceGroupFrom(SymmetryInterface symmetry);

  public void setSymmetryInfo(int modelIndex, Map<String, Object> modelAuxiliaryInfo, float[] notionalCell);

  public Object getSymmetryInfoAtom(ModelSet ms, BS bsAtoms, String xyz, int op,
                                    P3 pt, P3 pt2, String id, int type);

  public void setTimeReversal(int op, int val);

  public void setUnitCell(float[] params, boolean setRelative);

  public void initializeOrientation(M3 matUnitCellOrientation);

  public void toCartesian(T3 pt, boolean asAbsolute);

  public void toFractional(T3 pt, boolean asAbsolute);

  public P3 toSupercell(P3 fpt);

  public void toUnitCell(P3 pt, P3 offset);

  public boolean unitCellEquals(SymmetryInterface uc2);

  public void unitize(P3 ptFrac);

  public T3[] getV0abc(Object def);

  public Quat getQuaternionRotation(String abc);

  public Tensor getTensor(Viewer vwr, float[] anisoBorU);

  public T3 getFractionalOrigin();

  public boolean getState(SB commands);

  public AtomIndexIterator getIterator(Viewer vwr, Atom atom, Atom[] atoms, BS bstoms, float radius);

  public boolean toFromPrimitive(boolean toPrimitive, char type, T3[] oabc);

  public String getLatticeType();

}
