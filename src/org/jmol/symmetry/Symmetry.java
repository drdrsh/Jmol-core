/* $RCSfiodelle$allrueFFFF
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.symmetry;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.SymmetryInterface;
import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.ModelSet;
import org.jmol.script.T;
import org.jmol.util.Escape;
import javajs.util.Lst;
import javajs.util.SB;

import org.jmol.util.JmolMolecule;
import javajs.util.P3;
import org.jmol.util.Tensor;
import org.jmol.util.Logger;
import org.jmol.util.SimpleUnitCell;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.Matrix;
import javajs.util.P3i;
import javajs.util.Quat;
import javajs.util.T3;
import javajs.util.V3;

public class Symmetry implements SymmetryInterface {

  // NOTE: THIS CLASS IS VERY IMPORTANT.
  // IN ORDER TO MODULARIZE IT, IT IS REFERENCED USING 
  // xxxx = Interface.getSymmetry();

  /* Symmetry is a wrapper class that allows access to the package-local
   * classes PointGroup, SpaceGroup, SymmetryInfo, and UnitCell.
   * 
   * When symmetry is detected in ANY model being loaded, a SymmetryInterface
   * is established for ALL models.
   * 
   * The SpaceGroup information could be saved with each model, but because this 
   * depends closely on what atoms have been selected, and since tracking that with atom
   * deletion is a bit complicated, instead we just use local instances of that class.
   * 
   * The three PointGroup methods here could be their own interface; they are just here
   * for convenience.
   * 
   * The file readers use SpaceGroup and UnitCell methods
   * 
   * The modelSet and modelLoader classes use UnitCell and SymmetryInfo 
   * 
   */
  private PointGroup pointGroup;
  private SpaceGroup spaceGroup;
  private SymmetryInfo symmetryInfo;
  private UnitCell unitCell;
  private boolean isBio;

  @Override
  public boolean isBio() {
    return isBio;
  }

  public Symmetry() {
    // instantiated ONLY using
    // symmetry = Interface.getSymmetry();
    // DO NOT use symmetry = new Symmetry();
    // as that will invalidate the Jar file modularization    
  }

  @Override
  public SymmetryInterface setPointGroup(SymmetryInterface siLast,
                                         Atom[] atomset, BS bsAtoms,
                                         boolean haveVibration,
                                         float distanceTolerance,
                                         float linearTolerance) {
    pointGroup = PointGroup.getPointGroup(siLast == null ? null
        : ((Symmetry) siLast).pointGroup, atomset, bsAtoms, haveVibration,
        distanceTolerance, linearTolerance);
    return this;
  }

  @Override
  public String getPointGroupName() {
    return pointGroup.getName();
  }

  @Override
  public Object getPointGroupInfo(int modelIndex, boolean asDraw,
                                  boolean asInfo, String type, int index,
                                  float scale) {
    if (!asDraw && !asInfo && pointGroup.textInfo != null)
      return pointGroup.textInfo;
    else if (asDraw && pointGroup.isDrawType(type, index, scale))
      return pointGroup.drawInfo;
    else if (asInfo && pointGroup.info != null)
      return pointGroup.info;
    return pointGroup.getInfo(modelIndex, asDraw, asInfo, type, index, scale);
  }

  // SpaceGroup methods

  @Override
  public void setSpaceGroup(boolean doNormalize) {
    if (spaceGroup == null)
      spaceGroup = (SpaceGroup.getNull(true)).set(doNormalize);
  }

  @Override
  public int addSpaceGroupOperation(String xyz, int opId) {
    return spaceGroup.addSymmetry(xyz, opId, false);
  }

  @Override
  public int addBioMoleculeOperation(M4 mat, boolean isReverse) {
    isBio = spaceGroup.isBio = true;
    return spaceGroup.addSymmetry((isReverse ? "!" : "") + "[[bio" + mat, 0,
        false);
  }

  @Override
  public void setLattice(int latt) {
    spaceGroup.setLatticeParam(latt);
  }

  @Override
  public Object getSpaceGroup() {
    return spaceGroup;
  }

  @Override
  public void setSpaceGroupFrom(SymmetryInterface symmetry) {
    spaceGroup = (SpaceGroup) symmetry.getSpaceGroup();
  }

  @Override
  public boolean createSpaceGroup(int desiredSpaceGroupIndex, String name,
                                  Object object) {
    spaceGroup = SpaceGroup.createSpaceGroup(desiredSpaceGroupIndex, name,
        object);
    if (spaceGroup != null && Logger.debugging)
      Logger.debug("using generated space group " + spaceGroup.dumpInfo(null));
    return spaceGroup != null;
  }

  @Override
  public String getSpaceGroupInfoStr(String name, SymmetryInterface cellInfo) {
    return SpaceGroup.getInfo(name, cellInfo);
  }

  @Override
  public Object getLatticeDesignation() {
    return spaceGroup.getLatticeDesignation();
  }

  @Override
  public void setFinalOperations(String name, P3[] atoms, int iAtomFirst,
                                 int noSymmetryCount, boolean doNormalize, String filterSymop) {
    if (name != null && (name.startsWith("bio") || name.indexOf(" *(") >= 0))  // filter SYMOP
      spaceGroup.name = name;
    if (filterSymop != null) {
      Lst<SymmetryOperation> lst = new Lst<SymmetryOperation>();
      lst.addLast(spaceGroup.operations[0]);
      for (int i = 1; i < spaceGroup.operationCount; i++)
        if (filterSymop.contains(" " + (i + 1) + " "))
          lst.addLast(spaceGroup.operations[i]);
      spaceGroup = SpaceGroup.createSpaceGroup(-1,  name + " *(" + filterSymop.trim() + ")", lst);
    }
    spaceGroup.setFinalOperations(atoms, iAtomFirst, noSymmetryCount,
        doNormalize);
  }

  @Override
  public M4 getSpaceGroupOperation(int i) {
    return (i >= spaceGroup.operations.length ? null
        : spaceGroup.finalOperations == null ? spaceGroup.operations[i]
            : spaceGroup.finalOperations[i]);
  }

  @Override
  public String getSpaceGroupXyz(int i, boolean doNormalize) {
    return spaceGroup.getXyz(i, doNormalize);
  }

  @Override
  public void newSpaceGroupPoint(int i, P3 atom1, P3 atom2, int transX,
                                 int transY, int transZ) {
    if (spaceGroup.finalOperations == null) {
      // temporary spacegroups don't have to have finalOperations
      if (!spaceGroup.operations[i].isFinalized)
        spaceGroup.operations[i].doFinalize();
      spaceGroup.operations[i].newPoint(atom1, atom2, transX, transY, transZ);
      return;
    }
    spaceGroup.finalOperations[i]
        .newPoint(atom1, atom2, transX, transY, transZ);
  }

  @Override
  public V3[] rotateAxes(int iop, V3[] axes, P3 ptTemp, M3 mTemp) {
    return (iop == 0 ? axes : spaceGroup.finalOperations[iop].rotateAxes(axes,
        unitCell, ptTemp, mTemp));
  }

  @Override
  public String getSpaceGroupOperationCode(int iOp) {
    return spaceGroup.operations[iOp].subsystemCode;
  }

  @Override
  public void setTimeReversal(int op, int val) {
    spaceGroup.operations[op].setTimeReversal(val);
  }

  @Override
  public float getSpinOp(int op) {
    return spaceGroup.operations[op].getSpinOp();
  }

  @Override
  public boolean addLatticeVectors(Lst<float[]> lattvecs) {
    return spaceGroup.addLatticeVectors(lattvecs);
  }

  @Override
  public int getLatticeOp() {
    return spaceGroup.latticeOp;
  }

  @Override
  public Matrix getOperationRsVs(int iop) {
    return (spaceGroup.finalOperations == null ? spaceGroup.operations
        : spaceGroup.finalOperations)[iop].rsvs;
  }

  @Override
  public int getSiteMultiplicity(P3 pt) {
    return spaceGroup.getSiteMultiplicity(pt, unitCell);
  }

  @Override
  /**
   * @param rot is a full (3+d)x(3+d) array of epsilons
   * @param trans is a (3+d)x(1) array of translations
   * @return Jones-Faithful representation
   */
  public String addOp(String code, Matrix rs, Matrix vs, Matrix sigma) {
    spaceGroup.isSSG = true;
    String s = SymmetryOperation.getXYZFromRsVs(rs, vs, false);
    int i = spaceGroup.addSymmetry(s, -1, true);
    spaceGroup.operations[i].setSigma(code, sigma);
    return s;
  }
  
  @Override
  public String getMatrixFromString(String xyz, float[] rotTransMatrix,
                                    boolean allowScaling, int modDim) {
    return SymmetryOperation.getMatrixFromString(null, xyz, rotTransMatrix,
        allowScaling);
  }

  /// symmetryInfo ////

  @Override
  public String getSpaceGroupName() {
    return (symmetryInfo != null ? symmetryInfo.sgName
        : spaceGroup != null ? spaceGroup.getName() : unitCell != null && unitCell.name.length() > 0 ? "cell=" + unitCell.name : "");
  }

  @Override
  public int getSpaceGroupOperationCount() {
    return (symmetryInfo != null ? symmetryInfo.symmetryOperations.length
        : spaceGroup != null && spaceGroup.finalOperations != null ? spaceGroup.finalOperations.length
            : 0);
  }

  @Override
  public boolean getCoordinatesAreFractional() {
    return symmetryInfo == null || symmetryInfo.coordinatesAreFractional;
  }

  @Override
  public int[] getCellRange() {
    return symmetryInfo.cellRange;
  }

  @Override
  public String getSymmetryInfoStr() {
    return symmetryInfo.infoStr;
  }

  @Override
  public M4[] getSymmetryOperations() {
    return symmetryInfo == null ? spaceGroup.finalOperations
        : symmetryInfo.symmetryOperations;
  }

  @Override
  public boolean isPeriodic() {
    return (symmetryInfo == null ? false : symmetryInfo.isPeriodic());
  }

  /**
   * Set the symmetry in  the  
   */
  @SuppressWarnings("unchecked")
  @Override
  public void setSymmetryInfo(int modelIndex,
                              Map<String, Object> modelAuxiliaryInfo, float[] notionalCell) {
    symmetryInfo = new SymmetryInfo();
    float[] notionalUnitcell = symmetryInfo.setSymmetryInfo(modelAuxiliaryInfo, notionalCell);
    if (notionalUnitcell == null)
      return;
    setUnitCell(notionalUnitcell, modelAuxiliaryInfo.containsKey("jmolData"));
    unitCell.moreInfo = (Lst<String>) modelAuxiliaryInfo.get("moreUnitCellInfo");
    modelAuxiliaryInfo.put("infoUnitCell", getUnitCellAsArray(false));
    setOffsetPt((P3) modelAuxiliaryInfo.get("unitCellOffset"));
    M3 matUnitCellOrientation = (M3) modelAuxiliaryInfo
        .get("matUnitCellOrientation");
    if (matUnitCellOrientation != null)
      initializeOrientation(matUnitCellOrientation);
    if (Logger.debugging)
      Logger.debug("symmetryInfos[" + modelIndex + "]:\n"
          + unitCell.dumpInfo(true));
  }

  // UnitCell methods

  @Override
  public boolean haveUnitCell() {
    return (unitCell != null);
  }

  @Override
  public boolean checkUnitCell(SymmetryInterface uc, P3 cell, P3 ptTemp,
                               boolean isAbsolute) {
    uc.toFractional(ptTemp, isAbsolute);
    float slop = 0.02f;
    // {1 1 1} here is the original cell
    return (ptTemp.x >= cell.x - 1f - slop && ptTemp.x <= cell.x + slop
        && ptTemp.y >= cell.y - 1f - slop && ptTemp.y <= cell.y + slop
        && ptTemp.z >= cell.z - 1f - slop && ptTemp.z <= cell.z + slop);
  }

  @Override
  public void setUnitCell(float[] notionalUnitCell, boolean setRelative) {
    unitCell = UnitCell.newA(notionalUnitCell, setRelative);
  }

  @Override
  public boolean unitCellEquals(SymmetryInterface uc2) {
    return ((Symmetry) (uc2)).unitCell.isSameAs(unitCell);
  }

  @Override
  public String getUnitCellState() {
    return (unitCell == null ? "" : unitCell.getState());
  }

  @Override
  public Lst<String> getMoreInfo() {
   return unitCell.moreInfo;
  }
  public String getUnitsymmetryInfo() {
    // not used in Jmol?
    return unitCell.dumpInfo(false);
  }

  @Override
  public void initializeOrientation(M3 mat) {
    unitCell.initOrientation(mat);
  }

  @Override
  public void unitize(P3 ptFrac) {
    unitCell.unitize(ptFrac);
  }

  @Override
  public void toUnitCell(P3 pt, P3 offset) {
    unitCell.toUnitCell(pt, offset);
  }

  @Override
  public void toCartesian(T3 fpt, boolean ignoreOffset) {
    if (!isBio)
      unitCell.toCartesian(fpt, ignoreOffset);
  }

  @Override
  public P3 toSupercell(P3 fpt) {
    return unitCell.toSupercell(fpt);
  }

  @Override
  public void toFractional(T3 pt, boolean isAbsolute) {
    if (!isBio)
      unitCell.toFractional(pt, isAbsolute);
  }

  @Override
  public float[] getNotionalUnitCell() {
    return unitCell.getNotionalUnitCell();
  }

  @Override
  public float[] getUnitCellAsArray(boolean vectorsOnly) {
    return unitCell.getUnitCellAsArray(vectorsOnly);
  }

  @Override
  public Tensor getTensor(float[] parBorU) {
    if (parBorU == null)
      return null;
    if (unitCell == null)
      unitCell = UnitCell.newA(new float[] { 1, 1, 1, 90, 90, 90 }, true);
    return unitCell.getTensor(parBorU);
  }

  @Override
  public P3[] getUnitCellVertices() {
    return unitCell.getVertices();
  }

  @Override
  public P3 getCartesianOffset() {
    return unitCell.getCartesianOffset();
  }

  @Override
  public P3 getFractionalOffset() {
    return unitCell.getFractionalOffset();
  }

  @Override
  public void setOffsetPt(T3 pt) {
    unitCell.setOffset(pt);
  }

  @Override
  public void setOffset(int nnn) {
    P3 pt = new P3();
    SimpleUnitCell.ijkToPoint3f(nnn, pt, 0);
    unitCell.setOffset(pt);
  }

  @Override
  public P3 getUnitCellMultiplier() {
    return unitCell.getUnitCellMultiplier();
  }

  @Override
  public P3[] getCanonicalCopy(float scale, boolean withOffset) {
    return unitCell.getCanonicalCopy(scale, withOffset);
  }

  @Override
  public float getUnitCellInfoType(int infoType) {
    return unitCell.getInfo(infoType);
  }

  @Override
  public String getUnitCellInfo() {
    return unitCell.dumpInfo(false);
  }

  @Override
  public boolean isSlab() {
    return unitCell.isSlab();
  }

  @Override
  public boolean isPolymer() {
    return unitCell.isPolymer();
  }

  @Override
  public void setMinMaxLatticeParameters(P3i minXYZ, P3i maxXYZ) {
    unitCell.setMinMaxLatticeParameters(minXYZ, maxXYZ);
  }

  @Override
  public boolean checkDistance(P3 f1, P3 f2, float distance, float dx,
                               int iRange, int jRange, int kRange, P3 ptOffset) {
    return unitCell.checkDistance(f1, f2, distance, dx, iRange, jRange, kRange,
        ptOffset);
  }

  @Override
  public V3[] getUnitCellVectors() {
    return unitCell.getUnitCellVectors();
  }

  @Override
  public SymmetryInterface getUnitCell(T3[] points, boolean setRelative, String name) {
    unitCell = UnitCell.newP(points, setRelative);
    if (name != null)
      unitCell.name = name;
    return this;
  }

  @Override
  public boolean isSupercell() {
    return unitCell.isSupercell();
  }

  @Override
  public BS notInCentroid(ModelSet modelSet, BS bsAtoms, int[] minmax) {
    try {
      BS bsDelete = new BS();
      int iAtom0 = bsAtoms.nextSetBit(0);
      JmolMolecule[] molecules = modelSet.getMolecules();
      int moleculeCount = molecules.length;
      Atom[] atoms = modelSet.at;
      boolean isOneMolecule = (molecules[moleculeCount - 1].firstAtomIndex == modelSet.am[atoms[iAtom0].mi].firstAtomIndex);
      P3 center = new P3();
      boolean centroidPacked = (minmax[6] == 1);
      nextMol: for (int i = moleculeCount; --i >= 0
          && bsAtoms.get(molecules[i].firstAtomIndex);) {
        BS bs = molecules[i].atomList;
        center.set(0, 0, 0);
        int n = 0;
        for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1)) {
          if (isOneMolecule || centroidPacked) {
            center.setT(atoms[j]);
            if (isNotCentroid(center, 1, minmax, centroidPacked)) {
              if (isOneMolecule)
                bsDelete.set(j);
            } else if (!isOneMolecule) {
              continue nextMol;
            }
          } else {
            center.add(atoms[j]);
            n++;
          }
        }
        if (centroidPacked || n > 0 && isNotCentroid(center, n, minmax, false))
          bsDelete.or(bs);
      }
      return bsDelete;
    } catch (Exception e) {
      return null;
    }
  }

  private boolean isNotCentroid(P3 center, int n, int[] minmax,
                                boolean centroidPacked) {
    center.scale(1f / n);
    toFractional(center, false);
    // we have to disallow just a tiny slice of atoms due to rounding errors
    // so  -0.000001 is OK, but 0.999991 is not.
    if (centroidPacked)
      return (center.x + 0.000005f <= minmax[0]
          || center.x - 0.000005f > minmax[3]
          || center.y + 0.000005f <= minmax[1]
          || center.y - 0.000005f > minmax[4]
          || center.z + 0.000005f <= minmax[2] || center.z - 0.000005f > minmax[5]);

    return (center.x + 0.000005f <= minmax[0]
        || center.x + 0.00005f > minmax[3] || center.y + 0.000005f <= minmax[1]
        || center.y + 0.00005f > minmax[4] || center.z + 0.000005f <= minmax[2] || center.z + 0.00005f > minmax[5]);
  }

  // info

  @Override
  @SuppressWarnings("unchecked")
  public Map<String, Object> getSpaceGroupInfo(ModelSet modelSet,
                                               int modelIndex,
                                               String sgName, int symOp,
                                               P3 pt1, P3 pt2, String drawID,
                                               String type) {
    String strOperations = null;
    Map<String, Object> info = null;
    SymmetryInterface cellInfo = null;
    Object[][] infolist = null;
    boolean isStandard = true;
    if (sgName == null) {
      if (modelIndex <= 0)
        modelIndex = (pt1 instanceof Atom ? ((Atom) pt1).mi
            : modelSet.vwr.am.cmi);
      boolean isBio = false;
      if (modelIndex < 0)
        strOperations = "no single current model";
      else if (!(isBio = (cellInfo = modelSet.am[modelIndex].biosymmetry) != null)
          && (cellInfo = modelSet.getUnitCell(modelIndex)) == null)
        strOperations = "not applicable";
      if (strOperations != null) {
        info = new Hashtable<String, Object>();
        info.put("spaceGroupInfo", strOperations);
        info.put("symmetryInfo", "");
      } else if (pt1 == null && drawID == null && symOp != 0) {
        info = (Map<String, Object>) modelSet.getInfo(modelIndex,
            "spaceGroupInfo");
      }
      if (info != null)
        return info;
      info = new Hashtable<String, Object>();
      if (pt1 == null && drawID == null && symOp == 0)
        modelSet.setInfo(modelIndex, "spaceGroupInfo", info);
      sgName = cellInfo.getSpaceGroupName();
      M4[] ops = cellInfo.getSymmetryOperations();
      SpaceGroup sg = (isBio ? ((Symmetry) cellInfo).spaceGroup : null);
      String jf = "";
      if (ops == null) {
        strOperations = "\n no symmetry operations";
      } else {
        isStandard = !isBio;
        if (isBio)
          this.spaceGroup = (SpaceGroup.getNull(false)).set(false);
        else
          setSpaceGroup(false);
        strOperations = "\n" + ops.length + " symmetry operations:";
        infolist = new Object[ops.length][];
        V3 centering = null;
        for (int i = 0; i < ops.length; i++) {
          SymmetryOperation op = ((SymmetryOperation) ops[i]);
          String xyz = op.xyz;
          int iop = (isBio ? addBioMoleculeOperation(sg.finalOperations[i],
              false) : addSpaceGroupOperation("=" + xyz, i + 1));
          if (iop < 0)
            continue;
          op = (SymmetryOperation) getSpaceGroupOperation(i);
          if (op.timeReversal != 0 || op.modDim > 0)
            isStandard = false;
          centering = op.setCentering(centering, false);
          jf += ";" + xyz;
          infolist[i] = (symOp > 0 && symOp - 1 != iop ? null : op
              .getDescription(modelSet, cellInfo, pt1, pt2, drawID));
          if (infolist[i] != null)
            strOperations += "\n" + (i + 1) + "\t" + infolist[i][0] + "\t"
                + infolist[i][2];
        }
      }
      jf = jf.substring(jf.indexOf(";") + 1);
      if (sgName.indexOf("[--]") >= 0)
        sgName = jf;
    } else {
      info = new Hashtable<String, Object>();
    }
    info.put("spaceGroupName", sgName);
    if (infolist != null) {
      info.put("operations", infolist);
      info.put("symmetryInfo", strOperations);
    }
    String data;
    if (isStandard) {
      data = getSpaceGroupInfoStr(sgName, cellInfo);
      if (data == null || data.equals("?"))
        data = "could not identify space group from name: " + sgName
            + "\nformat: show spacegroup \"2\" or \"P 2c\" "
            + "or \"C m m m\" or \"x, y, z;-x ,-y, -z\"";
    } else {
      data = sgName;
    }
    info.put("spaceGroupInfo", data);
    return info;
  }

  @Override
  public String getSymmetryInfoString(ModelSet modelSet, int modelIndex, int symOp,
                                      P3 pt1, P3 pt2, String drawID, String type) {
    Map<String, Object> sginfo = getSpaceGroupInfo(
        modelSet, modelIndex, null, symOp, pt1, pt2, drawID, type);
    if (sginfo == null)
      return "";
    boolean labelOnly = "label".equals(type);
    boolean prettyMat = "fmatrix".equals(type); 
    Object[][] infolist = (Object[][]) sginfo.get("operations");
    if (infolist == null)
      return "";
    SB sb = new SB();
    symOp--;
    for (int i = 0; i < infolist.length; i++) {
      if (infolist[i] == null || symOp >= 0 && symOp != i)
        continue;
      if (drawID != null)
        return (String) infolist[i][3];
      if (sb.length() > 0)
        sb.appendC('\n');
      if (prettyMat) {
        sb.append(SymmetryOperation.cleanMatrix((M4)infolist[i][10])).append("\t");
      } else if (!labelOnly) {
        if (symOp < 0)
          sb.appendI(i + 1).append("\t");
        sb.append((String) infolist[i][0]).append("\t"); //xyz
      }
      sb.append((String) infolist[i][2]); //desc
    }
    if (sb.length() == 0 && drawID != null)
      sb.append("draw " + drawID + "* delete");
    return sb.toString();
  }

  @Override
  public Object getSymmetryInfo(ModelSet modelSet, int iModel, int iAtom,
                                SymmetryInterface uc, String xyz, int op,
                                P3 pt, P3 pt2, String id, int type) {
    if (pt2 != null)
      return getSymmetryInfoString(modelSet, iModel, op, pt, pt2,
          (id == null ? "sym" : id), type == T.label ? "label" : null);
    boolean isBio = uc.isBio();
    Symmetry sym = (Symmetry) uc;
    int iop = op;
    V3 centering = null;
    if (xyz == null) {
      SymmetryOperation[] ops = (SymmetryOperation[]) sym
          .getSymmetryOperations();
      if (ops == null || op == 0 || Math.abs(op) > ops.length) {
        return (type == T.draw ? "draw ID sym_* delete": "");
      }
      if (op > 0) {
        xyz = ops[iop = op - 1].xyz;
      } else {
        xyz = ops[iop = -1 - op].xyz;
      }
      centering = ops[iop].centering;
    } else {
      iop = op = 0;
    }
    SymmetryInterface symTemp = modelSet.getSymTemp(true);
    symTemp.setSpaceGroup(false);
    int i = (isBio ? symTemp.addBioMoleculeOperation(
        sym.spaceGroup.finalOperations[iop], op < 0) : symTemp
        .addSpaceGroupOperation((op < 0 ? "!" : "=") + xyz, Math.abs(op)));

    if (i < 0)
      return "";
    SymmetryOperation opTemp = (SymmetryOperation) symTemp
        .getSpaceGroupOperation(i);
    if (!isBio)
      opTemp.centering = centering;
    Object[] info;
    if (pt != null || iAtom >= 0)
      pt = P3.newP(pt == null ? modelSet.at[iAtom] : pt);
    if (type == T.point) {
      if (isBio)
        return "";
      symTemp.setUnitCell(uc.getNotionalUnitCell(), false);
      uc.toFractional(pt, false);
      if (Float.isNaN(pt.x))
        return "";
      P3 sympt = new P3();
      symTemp.newSpaceGroupPoint(i, pt, sympt, 0, 0, 0);
      symTemp.toCartesian(sympt, false);
      return sympt;
    }
    // null id means "array info only" but here we want the draw commands
    info = opTemp.getDescription(modelSet, uc, pt, pt2, (id == null ? "sym"
        : id));
    int ang = ((Integer) info[9]).intValue();
    /*
     *  xyz (Jones-Faithful calculated from matrix)
     *  xyzOriginal (Provided by operation) 
     *  description ("C2 axis", for example) 
     *  translation vector (fractional)  
     *  translation vector (cartesian)
     *  inversion point 
     *  axis point 
     *  axis vector
     *  angle of rotation
     *  matrix representation
     */
    switch (type) {
    case T.array:
      return info;
    case T.list:
      String[] sinfo = new String[] { (String) info[0],
          (String) info[1],
          (String) info[2],
          // skipping DRAW commands here
          Escape.eP((V3) info[4]), Escape.eP((V3) info[5]),
          Escape.eP((P3) info[6]), Escape.eP((P3) info[7]),
          Escape.eP((V3) info[8]), "" + info[9], "" + Escape.e(info[10]) };
      return sinfo;
    case T.info:
      return info[0];
    default:
    case T.label:
      return info[2];
    case T.draw:
      return info[3];
    case T.translation:
      // skipping fractional translation
      return info[5]; // cartesian translation
    case T.center:
      return info[6];
    case T.point:
      return info[7];
    case T.axis:
    case T.plane:
      return ((ang == 0) == (type == T.plane) ? (V3) info[8] : null);
    case T.angle:
      return info[9];
    case T.matrix4f:
      return info[10];
    }
  }

  @Override
  public String fcoord(T3 p) {
    return SymmetryOperation.fcoord(p);
  }

  /**
   * Accepts a string, a 3x3 matrix, or a 4x4 matrix.
   * 
   * Returns a set of four values as a P3 array consisting of an origin and
   * three unit cell vectors a, b, and c. 
   */
  @Override
  public T3[] getV0abc(Object def) {
    if (unitCell == null)
      return null;
    M4 m;
    boolean isRev = false;
    if (def instanceof String) {
      String sdef = (String) def;
      // a,b,c;0,0,0
      if (sdef.indexOf(";") < 0)
        sdef += ";0,0,0";
      isRev = sdef.startsWith("!");
      if (isRev)
        sdef = sdef.substring(1);
      Symmetry symTemp = new Symmetry();
      symTemp.setSpaceGroup(false);
      int i = symTemp.addSpaceGroupOperation("=" + sdef, 0);
      if (i < 0)
        return null;
      m = symTemp.getSpaceGroupOperation(i);
      ((SymmetryOperation) m).doFinalize();
    } else {
      m = (def instanceof M3 ? M4.newMV((M3) def, new P3()) : (M4) def);
    }
    // We have an operator that may need reversing.
    // Note that translations are limited to 1/2, 1/3, 1/4, 1/6, 1/8.
    
    V3[] pts = new V3[4];
    P3 pt = new P3();
    M3 m3 = new M3();
    m.getRotationScale(m3);
    m.getTranslation(pt);
    if (isRev) {
      m3.invert();
      m3.transpose();
      m3.rotate(pt);
      pt.scale(-1);
    } else {
      m3.transpose();
    }
    
    // Note that only the origin is translated;
    // the others are vectors from the origin.
    
    // this is a point, so we do not ignore offset
    unitCell.toCartesian(pt, false);
    pts[0] = V3.newV(pt);
    pts[1] = V3.new3(1, 0, 0);
    pts[2] = V3.new3(0, 1, 0);
    pts[3] = V3.new3(0, 0, 1);
    for (int i = 1; i < 4; i++) {
      m3.rotate(pts[i]);
      // these are vectors, so we ignore offset
      unitCell.toCartesian(pts[i], true);
    }
    return pts;
  }

  @Override
  public Quat getQuaternionRotation(String abc) {
    return (unitCell == null ? null : unitCell.getQuaternionRotation(abc));
  }

}
