/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2014-01-10 09:19:33 -0600 (Fri, 10 Jan 2014) $
 * $Revision: 19162 $
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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.adapter.smarter;

import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.P3;
import javajs.util.P3i;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.T3;
import javajs.util.V3;

import org.jmol.api.JmolModulationSet;
import org.jmol.api.SymmetryInterface;
import org.jmol.java.BS;
import org.jmol.symmetry.Symmetry;
import org.jmol.util.BSUtil;
import org.jmol.util.Logger;
import org.jmol.util.SimpleUnitCell;
import org.jmol.util.Tensor;
import org.jmol.util.Vibration;

/**
 * 
 * A class used by AtomSetCollection for building the symmetry of a model and
 * generating new atoms based on that symmetry.
 * 
 */
public class XtalSymmetry {

  private AtomSetCollection asc;

  private AtomSetCollectionReader acr;

  public XtalSymmetry() {
    // for reflection
  }

  public XtalSymmetry set(AtomSetCollectionReader reader) {
    this.acr = reader;
    this.asc = reader.asc;
    getSymmetry();
    return this;
  }

  public SymmetryInterface symmetry;

  SymmetryInterface getSymmetry() {
    return (symmetry == null ?(symmetry = (Symmetry) acr.getInterface("org.jmol.symmetry.Symmetry")) : symmetry);
  }

  SymmetryInterface setSymmetry(SymmetryInterface symmetry) {
    return (this.symmetry = symmetry);
  }

  private float[] unitCellParams = new float[6];
  private float[] baseUnitCell;
  // expands to 26 for cartesianToFractional matrix as array (PDB) and supercell

  private float symmetryRange;
  private boolean doCentroidUnitCell;
  private boolean centroidPacked;
  private float packingError;
  private String filterSymop;

  private void setSymmetryRange(float factor) {
    symmetryRange = factor;
    asc.setInfo("symmetryRange", Float.valueOf(factor));
  }

  private boolean applySymmetryToBonds = false;

  private int[] latticeCells;

  private void setLatticeCells() {

    //    int[] latticeCells, boolean applySymmetryToBonds,
    //  }
    //                       boolean doPackUnitCell, boolean doCentroidUnitCell,
    //                       boolean centroidPacked, String strSupercell,
    //                       P3 ptSupercell) {
    //set when unit cell is determined
    // x <= 555 and y >= 555 indicate a range of cells to load
    // AROUND the central cell 555 and that
    // we should normalize (z = 1) or pack unit cells (z = -1) or not (z = 0)
    // in addition (Jmol 11.7.36) z = -2 does a full 3x3x3 around the designated cells
    // but then only delivers the atoms that are within the designated cells. 
    // Normalization is the moving of the center of mass into the unit cell.
    // Starting with Jmol 12.0.RC23 we do not normalize a CIF file that 
    // is being loaded without {i j k} indicated.

    latticeCells = acr.latticeCells;
    boolean isLatticeRange = (latticeCells[0] <= 555 && latticeCells[1] >= 555 && (latticeCells[2] == 0
        || latticeCells[2] == 1 || latticeCells[2] == -1));
    doNormalize = latticeCells[0] != 0
        && (!isLatticeRange || latticeCells[2] == 1);
    applySymmetryToBonds = acr.applySymmetryToBonds;
    doPackUnitCell = acr.doPackUnitCell;
    doCentroidUnitCell = acr.doCentroidUnitCell;
    centroidPacked = acr.centroidPacked;
    filterSymop = acr.filterSymop;
    if (acr.strSupercell == null)
      setSupercellFromPoint(acr.ptSupercell);
  }

  private P3 ptSupercell;
  //private float[] fmatSupercell;
  private M4 matSupercell;

  public void setSupercellFromPoint(P3 pt) {
    ptSupercell = pt;
    if (pt == null) {
      matSupercell = null;
      return;
    }
    matSupercell = new M4();
    matSupercell.m00 = pt.x;
    matSupercell.m11 = pt.y;
    matSupercell.m22 = pt.z;
    matSupercell.m33 = 1;
    Logger.info("Using supercell \n" + matSupercell);
  }
  
  private Lst<float[]> trajectoryUnitCells;
  
  private void setUnitCell(float[] info, M3 matUnitCellOrientation,
                                   P3 unitCellOffset) {
    unitCellParams = new float[info.length];
    this.unitCellOffset = unitCellOffset;
    for (int i = 0; i < info.length; i++)
      unitCellParams[i] = info[i];
    asc.haveUnitCell = true;
    asc.setCurrentModelInfo("unitCellParams", unitCellParams);
    if (asc.isTrajectory) {
      if (trajectoryUnitCells == null) {
        trajectoryUnitCells = new Lst<float[]>();
        asc.setInfo("unitCells", trajectoryUnitCells);
      }
      trajectoryUnitCells.addLast(unitCellParams);
    }
    asc.setGlobalBoolean(AtomSetCollection.GLOBAL_UNITCELLS);
    getSymmetry().setUnitCell(unitCellParams, false);
    // we need to set the auxiliary info as well, because 
    // ModelLoader creates a new symmetry object.
    if (unitCellOffset != null) {
      symmetry.setOffsetPt(unitCellOffset);
      asc.setCurrentModelInfo("unitCellOffset", unitCellOffset);
    }
    if (matUnitCellOrientation != null) {
      symmetry.initializeOrientation(matUnitCellOrientation);
      asc.setCurrentModelInfo("matUnitCellOrientation",
          matUnitCellOrientation);
    }
  }

  int addSpaceGroupOperation(String xyz, boolean andSetLattice) {
    if (andSetLattice)
      setLatticeCells();
    symmetry.setSpaceGroup(doNormalize);
    return symmetry.addSpaceGroupOperation(xyz, 0);
  }

  public void setLatticeParameter(int latt) {
    symmetry.setSpaceGroup(doNormalize);
    symmetry.setLattice(latt);
  }

  private boolean doNormalize = true;
  private boolean doPackUnitCell = false;

  private SymmetryInterface baseSymmetry;

  SymmetryInterface applySymmetryFromReader(SymmetryInterface readerSymmetry)
      throws Exception {
    asc.setCoordinatesAreFractional(acr.iHaveFractionalCoordinates);
    setUnitCell(acr.unitCellParams, acr.matUnitCellOrientation,
        acr.unitCellOffset);
    setAtomSetSpaceGroupName(acr.sgName);
    setSymmetryRange(acr.symmetryRange);
    if (acr.doConvertToFractional || acr.fileCoordinatesAreFractional) {
      setLatticeCells();
      boolean doApplySymmetry = true;
      if (acr.ignoreFileSpaceGroupName || !acr.iHaveSymmetryOperators) {
        if (!acr.merging || readerSymmetry == null)
          readerSymmetry = acr.getNewSymmetry();
        doApplySymmetry = readerSymmetry.createSpaceGroup(
            acr.desiredSpaceGroupIndex, (acr.sgName.indexOf("!") >= 0 ? "P1"
                : acr.sgName), acr.unitCellParams);
      } else {
        acr.doPreSymmetry();
        readerSymmetry = null;
      }
      packingError = acr.packingError;
      if (doApplySymmetry) {
        if (readerSymmetry != null)
          symmetry.setSpaceGroupFrom(readerSymmetry);
        //parameters are counts of unit cells as [a b c]
        applySymmetryLattice(acr.ms, acr.strSupercell);
        if (readerSymmetry != null && filterSymop == null)
          setAtomSetSpaceGroupName(readerSymmetry.getSpaceGroupName());
      }
    }
    if (acr.iHaveFractionalCoordinates && acr.merging && readerSymmetry != null) {
      
      // when merging (with appendNew false), we must return cartesians
      Atom[] atoms = asc.atoms;
      for (int i = asc.getLastAtomSetAtomIndex(), n = asc.ac; i < n; i++)
          readerSymmetry.toCartesian(atoms[i], true);
      asc.setCoordinatesAreFractional(false);
      
      // We no longer allow merging of multiple-model files
      // when the file to be appended has fractional coordinates and vibrations
      acr.addVibrations = false;
    }
    return symmetry;
  }

  private void setAtomSetSpaceGroupName(String spaceGroupName) {
    asc.setCurrentModelInfo("spaceGroup", spaceGroupName + "");
  }

  private void applySymmetryLattice(MSInterface ms, String supercell)
      throws Exception {

    if (!asc.coordinatesAreFractional || symmetry.getSpaceGroup() == null)
      return;

    int maxX = latticeCells[0];
    int maxY = latticeCells[1];
    int maxZ = Math.abs(latticeCells[2]);
    firstSymmetryAtom = asc.getLastAtomSetAtomIndex();
    BS bsAtoms = null;
    rminx = rminy = rminz = Float.MAX_VALUE;
    rmaxx = rmaxy = rmaxz = -Float.MAX_VALUE;
    T3[] oabc = null;
    P3 offset = null;
    P3 pt0 = null;
    nVib = 0;
    T3 va = null, vb = null, vc = null;
    baseSymmetry = symmetry;
    if (supercell != null && supercell.indexOf(",") >= 0) {
      // expand range to accommodate this alternative cell
      // oabc will be cartesian
      oabc = symmetry.getV0abc(supercell);
      if (oabc != null) {
        // set the bounds for atoms in the new unit cell
        // in terms of the old unit cell
        minXYZ = new P3i();
        maxXYZ = P3i.new3(maxX, maxY, maxZ);
        symmetry.setMinMaxLatticeParameters(minXYZ, maxXYZ);

        // base origin for new unit cell
        pt0 = P3.newP(oabc[0]);

        // base vectors for new unit cell
        va = P3.newP(oabc[1]);
        vb = P3.newP(oabc[2]);
        vc = P3.newP(oabc[3]);
        
        // be sure to add in packing error adjustments 
        // so that we include all needed atoms
        // load "" packed x.x supercell...
        P3 pa = new P3();
        P3 pb = new P3();
        P3 pc = new P3();
        if (acr.forcePacked) {
          pa.setT(va);
          pb.setT(vb);
          pc.setT(vc);
          pa.scale(packingError);
          pb.scale(packingError);
          pc.scale(packingError);
        }
        
        // account for lattice specification
        // load "" {x y z} supercell...
        oabc[0].scaleAdd2(minXYZ.x, va, oabc[0]);
        oabc[0].scaleAdd2(minXYZ.y, vb, oabc[0]);
        oabc[0].scaleAdd2(minXYZ.z, vc, oabc[0]);
        // add in packing adjustment
        oabc[0].sub(pa);
        oabc[0].sub(pb);
        oabc[0].sub(pc);
        // fractionalize and adjust min/max
        P3 pt = P3.newP(oabc[0]);
        symmetry.toFractional(pt, true);
        setSymmetryMinMax(pt);
        
        // account for lattice specification
        oabc[1].scale(maxXYZ.x - minXYZ.x);
        oabc[2].scale(maxXYZ.y - minXYZ.y);
        oabc[3].scale(maxXYZ.z - minXYZ.z);
        // add in packing adjustment
        oabc[1].scaleAdd2(2,  pa,  oabc[1]);
        oabc[2].scaleAdd2(2,  pb,  oabc[2]);
        oabc[3].scaleAdd2(2,  pc,  oabc[3]);
        // run through six of the corners -- a, b, c, ab, ac, bc
        for (int i = 0; i < 3; i++) {
          for (int j = i + 1; j < 4; j++) {
            pt.add2(oabc[i], oabc[j]);
            if (i != 0)
              pt.add(oabc[0]);
            symmetry.toFractional(pt, false);
            setSymmetryMinMax(pt);
          }
        }
        // bc in the end, so we need abc
        symmetry.toCartesian(pt, false);
        pt.add(oabc[1]);
        symmetry.toFractional(pt, false);
        setSymmetryMinMax(pt);
        // allow for some imprecision
        minXYZ = P3i.new3((int) Math.floor(rminx + 0.001f), (int) Math.floor(rminy + 0.001f),
            (int) Math.floor(rminz + 0.001f));
        maxXYZ = P3i.new3((int) Math.ceil(rmaxx - 0.001f), (int) Math.ceil(rmaxy - 0.001f),
            (int) Math.ceil(rmaxz - 0.001f));
      }
//    } else if (fmatSupercell != null) {
//
//      // supercell of the form {nx,ny,nz} or "x-y,x+y,..."
//
//      // 1) get all atoms for cells necessary
//
//      P3 pt = new P3();
//      for (int i = 0; i <= 1; i++)
//        for (int j = 0; j <= 1; j++)
//          for (int k = 0; k <= 1; k++) {
//            pt.set(i, j, k);
//            setSym(pt);
//          }
//      offset = (P3) asc.getAtomSetAuxiliaryInfoValue(-1, "unitCellOffset");
//      minXYZ = P3i.new3((int) rminx, (int) rminy, (int) rminz);
//      maxXYZ = P3i.new3((int) rmaxx, (int) rmaxy, (int) rmaxz);
//      va = setSym(P3.new3(1, 0, 0));
//      vb = setSym(P3.new3(0, 1, 0));
//      vc = setSym(P3.new3(0, 0, 1));      
    }
    if (rminx == Float.MAX_VALUE) {
//      fmatSupercell = null;
      matSupercell = null;
      supercell = null;
      oabc = null;
    } else {
      Logger.info("setting min/max for original lattice to " + minXYZ + " and "
          + maxXYZ);
      boolean doPack0 = doPackUnitCell;
      doPackUnitCell = doPack0;//(doPack0 || oabc != null && acr.forcePacked);
      if (asc.bsAtoms == null)
        asc.bsAtoms = BSUtil.setAll(asc.ac);
      bsAtoms = asc.bsAtoms;
      applyAllSymmetry(ms, null);
      doPackUnitCell = doPack0;

      
      // 2) set all atom coordinates to Cartesians

      Atom[] atoms = asc.atoms;
      int atomCount = asc.ac;
      int iAtomFirst = asc.getLastAtomSetAtomIndex();
      for (int i = iAtomFirst; i < atomCount; i++)
        symmetry.toCartesian(atoms[i], true);

      // 3) create the supercell unit cell

      symmetry = null;
      symmetry = getSymmetry();
      setUnitCell(new float[] { 0, 0, 0, 0, 0, 0, va.x, va.y, va.z,
          vb.x, vb.y, vb.z, vc.x, vc.y, vc.z }, null, offset);
      setAtomSetSpaceGroupName(oabc == null ? "P1" : "cell=" + supercell);
      symmetry.setSpaceGroup(doNormalize);
      symmetry.addSpaceGroupOperation("x,y,z", 0);

      // 4) reset atoms to fractional values in this new system

      if (pt0 != null)
        symmetry.toFractional(pt0, true);
      for (int i = iAtomFirst; i < atomCount; i++) {
        symmetry.toFractional(atoms[i], true);
        if (pt0 != null)
          atoms[i].sub(pt0);
      }

      // 5) apply the full lattice symmetry now

      asc.haveAnisou = false;

      // ?? TODO
      asc.setCurrentModelInfo("matUnitCellOrientation", null);

    }
    minXYZ = new P3i();
    maxXYZ = P3i.new3(maxX, maxY, maxZ);
    if (oabc == null) {
      minXYZ = new P3i();
      maxXYZ = P3i.new3(maxX, maxY, maxZ);
      applyAllSymmetry(ms, bsAtoms);      
    } else if (acr.forcePacked || doPackUnitCell) {
      // trim atom set based on original unit cell
      symmetry.setMinMaxLatticeParameters(minXYZ, maxXYZ);
      BS bs = asc.bsAtoms;
      Atom[] atoms = asc.atoms;
      if (bs == null)
        bs = asc.bsAtoms = BSUtil.newBitSet2(0, asc.ac);
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        if (!isWithinCell(dtype, atoms[i], minXYZ.x, maxXYZ.x,
            minXYZ.y, maxXYZ.y, minXYZ.z, maxXYZ.z, packingError))
          bs.clear(i);
      }
    }
    
    // but we leave matSupercell, because we might need it for vibrations in CASTEP
  }

  private float rminx, rminy, rminz, rmaxx, rmaxy, rmaxz;

  private void setSymmetryMinMax(P3 c) {
    if (rminx > c.x)
      rminx = c.x;
    if (rminy > c.y)
      rminy = c.y;
    if (rminz > c.z)
      rminz = c.z;
    if (rmaxx < c.x)
      rmaxx = c.x;
    if (rmaxy < c.y)
      rmaxy = c.y;
    if (rmaxz < c.z)
      rmaxz = c.z;
  }

  private boolean isInSymmetryRange(P3 c) {
    return (c.x >= rminx && c.y >= rminy && c.z >= rminz && c.x <= rmaxx
        && c.y <= rmaxy && c.z <= rmaxz);
  }

  private final P3 ptOffset = new P3();

  private P3 unitCellOffset;

  private P3i minXYZ, maxXYZ;
  private P3 minXYZ0, maxXYZ0;

  public boolean isWithinCell(int dtype, P3 pt, float minX, float maxX,
                              float minY, float maxY, float minZ, float maxZ,
                              float slop) {
    return (pt.x > minX - slop && pt.x < maxX + slop
        && (dtype < 2 || pt.y > minY - slop && pt.y < maxY + slop) 
        && (dtype < 3 || pt.z > minZ - slop && pt.z < maxZ + slop));
    //    System.out.println(pt + " " + minX + " " + maxX + " " + slop + " " + xxx);
    //    return xxx;
  }
  
//  /**
//   * A problem arises when converting to JavaScript, because JavaScript numbers are all
//   * doubles, while here we have floats. So what we do is to multiply by a number that
//   * is beyond the precision of our data but within the range of floats -- namely, 100000,
//   * and then integerize. This ensures that both doubles and floats compare the same number.
//   * Unfortunately, it will break Java reading of older files, so we check for legacy versions. 
//   * 
//   * @param dtype
//   * @param pt
//   * @param minX
//   * @param maxX
//   * @param minY
//   * @param maxY
//   * @param minZ
//   * @param maxZ
//   * @param slop
//   * @return  true if within range
//   */
//  private boolean xxxisWithinCellInt(int dtype, P3 pt, float minX, float maxX,
//                              float minY, float maxY, float minZ, float maxZ,
//                              int slop) {
//    switch (dtype) {
//    case 3:
//      if (Math.round((minZ - pt.z) * 100000) >= slop || Math.round((pt.z - maxZ) * 100000) >= slop)
//        return false;
//      //$FALL-THROUGH$
//    case 2:
//      if (Math.round((minY - pt.y) * 100000) >= slop || Math.round((pt.y - maxY) * 100000) >= slop)
//        return false;
//      //$FALL-THROUGH$
//    case 1:
//      if (Math.round((minX - pt.x) * 100000) >= slop || Math.round((pt.x - maxX) * 100000) >= slop)
//        return false;
//      break;
//    }
//    return true;
//  }

  /**
   * @param ms
   *        modulated structure interface
   * @param bsAtoms
   *        relating to supercells
   * @throws Exception
   */
  private void applyAllSymmetry(MSInterface ms, BS bsAtoms) throws Exception {
    if (asc.ac == 0)
      return;
    noSymmetryCount = (asc.baseSymmetryAtomCount == 0 ? asc
        .getLastAtomSetAtomCount() : asc.baseSymmetryAtomCount);
    asc.setTensors();
    bondCount0 = asc.bondCount;
    finalizeSymmetry(symmetry);
    int operationCount = symmetry.getSpaceGroupOperationCount();
    dtype = (int) symmetry.getUnitCellInfoType(SimpleUnitCell.INFO_DIMENSIONS);
    symmetry.setMinMaxLatticeParameters(minXYZ, maxXYZ);
    if (doCentroidUnitCell)
      asc.setInfo("centroidMinMax", new int[] { minXYZ.x, minXYZ.y, minXYZ.z,
          maxXYZ.x, maxXYZ.y, maxXYZ.z, (centroidPacked ? 1 : 0) });
    if (ptSupercell != null) {
      asc.setCurrentModelInfo("supercell", ptSupercell);
      switch (dtype) {
      case 3:
        // standard
        minXYZ.z *= (int) Math.abs(ptSupercell.z);
        maxXYZ.z *= (int) Math.abs(ptSupercell.z);
        //$FALL-THROUGH$;
      case 2:
        // slab or standard
        minXYZ.y *= (int) Math.abs(ptSupercell.y);
        maxXYZ.y *= (int) Math.abs(ptSupercell.y);
        //$FALL-THROUGH$;
      case 1:
        // slab, polymer, or standard
        minXYZ.x *= (int) Math.abs(ptSupercell.x);
        maxXYZ.x *= (int) Math.abs(ptSupercell.x);
      }
    }
    if (doCentroidUnitCell || doPackUnitCell || symmetryRange != 0
        && maxXYZ.x - minXYZ.x == 1 && maxXYZ.y - minXYZ.y == 1
        && maxXYZ.z - minXYZ.z == 1) {
      // weird Mac bug does not allow   Point3i.new3(minXYZ) !!
      minXYZ0 = P3.new3(minXYZ.x, minXYZ.y, minXYZ.z);
      maxXYZ0 = P3.new3(maxXYZ.x, maxXYZ.y, maxXYZ.z);
      if (ms != null) {
        ms.setMinMax0(minXYZ0, maxXYZ0);
        minXYZ.set((int) minXYZ0.x, (int) minXYZ0.y, (int) minXYZ0.z);
        maxXYZ.set((int) maxXYZ0.x, (int) maxXYZ0.y, (int) maxXYZ0.z);
      }
      switch (dtype) {
      case 3:
        // standard
        minXYZ.z--;
        maxXYZ.z++;
        //$FALL-THROUGH$;
      case 2:
        // slab or standard
        minXYZ.y--;
        maxXYZ.y++;
        //$FALL-THROUGH$;
      case 1:
        // slab, polymer, or standard
        minXYZ.x--;
        maxXYZ.x++;
      }
    }
    int nCells = (maxXYZ.x - minXYZ.x) * (maxXYZ.y - minXYZ.y)
        * (maxXYZ.z - minXYZ.z);
    int cartesianCount = (asc.checkSpecial ? noSymmetryCount * operationCount
        * nCells : symmetryRange > 0 ? noSymmetryCount * operationCount // checking
    // against
    // {1 1
    // 1}
        : symmetryRange < 0 ? 1 // checking against symop=1555 set; just a box
            : 1 // not checking
    );
    P3[] cartesians = new P3[cartesianCount];
    for (int i = 0; i < noSymmetryCount; i++)
      asc.atoms[i + firstSymmetryAtom].bsSymmetry = BS.newN(operationCount
          * (nCells + 1));
    int pt = 0;
    int[] unitCells = new int[nCells];
    unitCellTranslations = new V3[nCells];
    int iCell = 0;
    int cell555Count = 0;
    float absRange = Math.abs(symmetryRange);
    boolean checkCartesianRange = (symmetryRange != 0);
    boolean checkRangeNoSymmetry = (symmetryRange < 0);
    boolean checkRange111 = (symmetryRange > 0);
    if (checkCartesianRange) {
      rminx = rminy = rminz = Float.MAX_VALUE;
      rmaxx = rmaxy = rmaxz = -Float.MAX_VALUE;
    }
    // always do the 555 cell first

    // incommensurate symmetry can have lattice centering, resulting in 
    // duplication of operators. There's a bug later on that requires we 
    // only do this with the first atom set for now, at least.
    SymmetryInterface symmetry = this.symmetry;
    SymmetryInterface lastSymmetry = symmetry;
    latticeOp = symmetry.getLatticeOp();
    checkAll = (asc.atomSetCount == 1 && asc.checkSpecial
        && latticeOp >= 0);
    latticeOnly = (asc.checkLatticeOnly && latticeOp >= 0); // CrystalReader
    P3 pttemp = null;
    M4 op = symmetry.getSpaceGroupOperation(0);
    if (doPackUnitCell){
      pttemp = new P3();
      ptOffset.set(0, 0, 0);
    }
    
    for (int tx = minXYZ.x; tx < maxXYZ.x; tx++)
      for (int ty = minXYZ.y; ty < maxXYZ.y; ty++)
        for (int tz = minXYZ.z; tz < maxXYZ.z; tz++) {
          unitCellTranslations[iCell] = V3.new3(tx, ty, tz);
          unitCells[iCell++] = 555 + tx * 100 + ty * 10 + tz;
          if (tx != 0 || ty != 0 || tz != 0 || cartesians.length == 0)
            continue;

          // base cell only

          for (pt = 0; pt < noSymmetryCount; pt++) {
            Atom atom = asc.atoms[firstSymmetryAtom + pt];
            if (ms != null) {
              symmetry = ms.getAtomSymmetry(atom, this.symmetry);
              if (symmetry != lastSymmetry) {
                if (symmetry.getSpaceGroupOperationCount() == 0)
                  finalizeSymmetry(lastSymmetry = symmetry);
                op = symmetry.getSpaceGroupOperation(0);
              }
            }
            P3 c = P3.newP(atom);
            op.rotTrans(c);
            symmetry.toCartesian(c, false);
            if (doPackUnitCell) {
              symmetry.toUnitCell(c, ptOffset);
              pttemp.setT(c);
              symmetry.toFractional(pttemp, false);
              if (acr.fixJavaFloat)
                PT.fixPtFloats(pttemp, PT.FRACTIONAL_PRECISION);
              // when bsAtoms != null, we are
              // setting it to be correct for a 
              // second unit cell -- the supercell
              if (bsAtoms == null)
                atom.setT(pttemp);
              else if (atom.distance(pttemp) < 0.0001f)
                bsAtoms.set(atom.index);
              else {// not in THIS unit cell
                bsAtoms.clear(atom.index);
                continue;
              }
            }
            if (bsAtoms != null)
              atom.bsSymmetry.clearAll();
            atom.bsSymmetry.set(iCell * operationCount);
            atom.bsSymmetry.set(0);
            if (checkCartesianRange)
              setSymmetryMinMax(c);
            if (pt < cartesianCount)
              cartesians[pt] = c;
          }
          if (checkRangeNoSymmetry) {
            rminx -= absRange;
            rminy -= absRange;
            rminz -= absRange;
            rmaxx += absRange;
            rmaxy += absRange;
            rmaxz += absRange;
          }
          cell555Count = pt = symmetryAddAtoms(0, 0, 0, 0, pt, iCell
              * operationCount, cartesians, ms);
        }
    if (checkRange111) {
      rminx -= absRange;
      rminy -= absRange;
      rminz -= absRange;
      rmaxx += absRange;
      rmaxy += absRange;
      rmaxz += absRange;
    }

    // now apply all the translations
    iCell = 0;
    for (int tx = minXYZ.x; tx < maxXYZ.x; tx++)
      for (int ty = minXYZ.y; ty < maxXYZ.y; ty++)
        for (int tz = minXYZ.z; tz < maxXYZ.z; tz++) {
          iCell++;
          if (tx != 0 || ty != 0 || tz != 0)
            pt = symmetryAddAtoms(tx, ty, tz, cell555Count, pt, iCell
                * operationCount, cartesians, ms);
        }
    if (iCell * noSymmetryCount == asc.ac - firstSymmetryAtom)
      duplicateAtomProperties(iCell);
    setSymmetryOps();
    asc.setCurrentModelInfo("presymmetryAtomIndex",
        Integer.valueOf(firstSymmetryAtom));
    asc.setCurrentModelInfo("presymmetryAtomCount",
        Integer.valueOf(noSymmetryCount));
    asc.setCurrentModelInfo("latticeDesignation",
        symmetry.getLatticeDesignation());
    asc.setCurrentModelInfo("unitCellRange", unitCells);
    asc.setCurrentModelInfo("unitCellTranslations", unitCellTranslations);
    baseUnitCell = unitCellParams;
    unitCellParams = new float[6];
    reset();
  }

  private boolean checkAll;
  private int bondCount0;

  private int symmetryAddAtoms(int transX, int transY, int transZ,
                               int baseCount, int pt, int iCellOpPt,
                               P3[] cartesians, MSInterface ms) throws Exception {
    boolean isBaseCell = (baseCount == 0);
    boolean addBonds = (bondCount0 > asc.bondIndex0 && applySymmetryToBonds);
    int[] atomMap = (addBonds ? new int[noSymmetryCount] : null);
    if (doPackUnitCell)
      ptOffset.set(transX, transY, transZ);

    //symmetryRange < 0 : just check symop=1 set
    //symmetryRange > 0 : check against {1 1 1}

    // if we are not checking special atoms, then this is a PDB file
    // and we return all atoms within a cubical volume around the 
    // target set. The user can later use select within() to narrow that down
    // This saves immensely on time.

    float range2 = symmetryRange * symmetryRange;
    boolean checkRangeNoSymmetry = (symmetryRange < 0);
    boolean checkRange111 = (symmetryRange > 0);
    boolean checkSymmetryMinMax = (isBaseCell && checkRange111);
    checkRange111 &= !isBaseCell;
    int nOperations = symmetry.getSpaceGroupOperationCount();
    boolean checkSpecial = (nOperations == 1 && !doPackUnitCell ? false
        : asc.checkSpecial);
    boolean checkSymmetryRange = (checkRangeNoSymmetry || checkRange111);
    boolean checkDistances = (checkSpecial || checkSymmetryRange);
    boolean addCartesian = (checkSpecial || checkSymmetryMinMax);
    SymmetryInterface symmetry = this.symmetry;
    if (checkRangeNoSymmetry)
      baseCount = noSymmetryCount;
    int atomMax = firstSymmetryAtom + noSymmetryCount;
    P3 ptAtom = new P3();
    String code = null;
    char subSystemId = '\0';
    for (int iSym = 0; iSym < nOperations; iSym++) {
      if (isBaseCell && iSym == 0 || latticeOnly && iSym > 0
          && iSym != latticeOp)
        continue;

      /* pt0 sets the range of points cross-checked. 
       * If we are checking special positions, then we have to check
       *   all previous atoms. 
       * If we are doing a symmetry range check relative to {1 1 1}, then
       *   we have to check only the base set. (checkRange111 true)
       * If we are doing a symmetry range check on symop=1555 (checkRangeNoSymmetry true), 
       *   then we don't check any atoms and just use the box.
       *    
       */

      int pt0 = (checkSpecial ? pt
          : checkRange111 ? baseCount : 0);
      float spinOp = (asc.vibScale == 0 ? symmetry.getSpinOp(iSym)
          : asc.vibScale);
      for (int i = firstSymmetryAtom; i < atomMax; i++) {
        Atom a = asc.atoms[i];
        if (a.ignoreSymmetry)
          continue;
        if (asc.bsAtoms != null && !asc.bsAtoms.get(i))
          continue;

        if (ms == null) {
          symmetry.newSpaceGroupPoint(iSym, a, ptAtom, transX, transY, transZ);
        } else {
          symmetry = ms.getAtomSymmetry(a, this.symmetry);
          symmetry.newSpaceGroupPoint(iSym, a, ptAtom, transX, transY, transZ);
          // COmmensurate structures may use a symmetry operator
          // to changes space groups.
          code = symmetry.getSpaceGroupOperationCode(iSym);
          if (code != null) {
            subSystemId = code.charAt(0);
            symmetry = ms.getSymmetryFromCode(code);
            if (symmetry.getSpaceGroupOperationCount() == 0)
              finalizeSymmetry(symmetry);
          }
        }
        if (acr.fixJavaFloat)
          PT.fixPtFloats(ptAtom, PT.FRACTIONAL_PRECISION);
        P3 cartesian = P3.newP(ptAtom);
        symmetry.toCartesian(cartesian, false);
        if (doPackUnitCell) {
          // note that COmmensurate structures may need 
          // modulation at this point.
          symmetry.toUnitCell(cartesian, ptOffset);
          ptAtom.setT(cartesian);
          symmetry.toFractional(ptAtom, false);
          if (acr.fixJavaFloat)
            PT.fixPtFloats(ptAtom, PT.FRACTIONAL_PRECISION);
          if (!isWithinCell(dtype, ptAtom, minXYZ0.x, maxXYZ0.x,
              minXYZ0.y, maxXYZ0.y, minXYZ0.z, maxXYZ0.z, packingError))
            continue;
        }
        if (checkSymmetryMinMax)
          setSymmetryMinMax(cartesian);
        Atom special = null;
        if (checkDistances) {

          /* checkSpecial indicates that we are looking for atoms with (nearly) the
           * same cartesian position.  
           */
          float minDist2 = Float.MAX_VALUE;
          if (checkSymmetryRange && !isInSymmetryRange(cartesian))
            continue;
          int j0 = (checkAll ? asc.ac : pt0);
          String name = a.atomName;
          char id = (code == null ? a.altLoc : subSystemId);
          for (int j = j0; --j >= 0;) {
            P3 pc = cartesians[j];
            if (pc == null)
              continue;
            float d2 = cartesian.distanceSquared(pc);
            if (checkSpecial && d2 < 0.0001) {
              special = asc.atoms[firstSymmetryAtom + j];
              if ((special.atomName == null || special.atomName.equals(name))
                  && special.altLoc == id)
                break;
              special = null;
            }
            if (checkRange111 && j < baseCount && d2 < minDist2)
              minDist2 = d2;
          }
          if (checkRange111 && minDist2 > range2)
            continue;
        }
        int atomSite = a.atomSite;
        if (special != null) {
          if (addBonds)
            atomMap[atomSite] = special.index;
          special.bsSymmetry.set(iCellOpPt + iSym);
          special.bsSymmetry.set(iSym);
        } else {
          if (addBonds)
            atomMap[atomSite] = asc.ac;
          Atom atom1 = asc.newCloneAtom(a);
          if (asc.bsAtoms != null)
            asc.bsAtoms.set(atom1.index);
          atom1.setT(ptAtom);
          if (spinOp != 0 && atom1.vib != null) {
            //System.out.println("vib for iSym " + iSym + " " + atom1 + " " +  atom1.vib);
            //System.out.println(symmetry.getSpaceGroupOperation(iSym));
            symmetry.getSpaceGroupOperation(iSym).rotate(atom1.vib);
            atom1.vib.scale(spinOp);
            //System.out.println("vib for iSym " + iSym + " " + atom1 + " " +  atom1.vib);
          }
          atom1.atomSite = atomSite;
          if (code != null)
            atom1.altLoc = subSystemId;
          atom1.bsSymmetry = BSUtil.newAndSetBit(iCellOpPt + iSym);
          atom1.bsSymmetry.set(iSym);
          if (addCartesian)
            cartesians[pt++] = cartesian;
          Lst<Object> tensors = a.tensors;
          if (tensors != null) {
            atom1.tensors = null;
            for (int j = tensors.size(); --j >= 0;) {
              Tensor t = (Tensor) tensors.get(j);
              if (t == null)
                continue;
              if (nOperations == 1)
                atom1.addTensor(t.copyTensor(), null, false);
              else
                addRotatedTensor(atom1, t, iSym, false, symmetry);
            }
          }
        }
      }
      if (addBonds) {
        // Clone bonds
        Bond[] bonds = asc.bonds;
        Atom[] atoms = asc.atoms;
        for (int bondNum = asc.bondIndex0; bondNum < bondCount0; bondNum++) {
          Bond bond = bonds[bondNum];
          Atom atom1 = atoms[bond.atomIndex1];
          Atom atom2 = atoms[bond.atomIndex2];
          if (atom1 == null || atom2 == null)
            continue;
          int iAtom1 = atomMap[atom1.atomSite];
          int iAtom2 = atomMap[atom2.atomSite];
          if (iAtom1 >= atomMax || iAtom2 >= atomMax)
            asc.addNewBondWithOrder(iAtom1, iAtom2, bond.order);
        }
      }
    }
    return pt;
  }

  @SuppressWarnings("unchecked")
  private void duplicateAtomProperties(int nTimes) {
    Map<String, Object> p = (Map<String, Object>) asc
        .getAtomSetAuxiliaryInfoValue(-1, "atomProperties");
    if (p != null)
      for (Map.Entry<String, Object> entry : p.entrySet()) {
        String key = entry.getKey();
        Object val = entry.getValue();
        if (val instanceof String) {
          String data = (String) val;
          SB s = new SB();
          for (int i = nTimes; --i >= 0;)
            s.append(data);
          p.put(key, s.toString());
        } else {
          float[] f = (float[]) val;
          float[] fnew = new float[f.length * nTimes];
          for (int i = nTimes; --i >= 0;)
            System.arraycopy(f, 0, fnew, i * f.length, f.length);
        }
      }
  }

  private void finalizeSymmetry(SymmetryInterface symmetry) {
    String name = (String) asc.getAtomSetAuxiliaryInfoValue(-1, "spaceGroup");
    symmetry.setFinalOperations(name, asc.atoms, firstSymmetryAtom,
        noSymmetryCount, doNormalize, filterSymop);
    if (filterSymop != null || name == null || name.equals("unspecified!"))
      setAtomSetSpaceGroupName(symmetry.getSpaceGroupName());
  }

  private void setSymmetryOps() {
    int operationCount = symmetry.getSpaceGroupOperationCount();
    if (operationCount > 0) {
      String[] symmetryList = new String[operationCount];
      for (int i = 0; i < operationCount; i++)
        symmetryList[i] = "" + symmetry.getSpaceGroupXyz(i, doNormalize);
      asc.setCurrentModelInfo("symmetryOperations", symmetryList);
      asc.setCurrentModelInfo("symmetryOps",
          symmetry.getSymmetryOperations());
    }
    asc.setCurrentModelInfo("symmetryCount",
        Integer.valueOf(operationCount));
  }

  private int dtype = 3;
  private V3[] unitCellTranslations;
  private int latticeOp;
  private boolean latticeOnly;
  private int noSymmetryCount;
  private int firstSymmetryAtom;

  private final static int PARTICLE_NONE = 0;
  private final static int PARTICLE_CHAIN = 1;
  private final static int PARTICLE_SYMOP = 2;

  @SuppressWarnings("unchecked")
  public void applySymmetryBio(Map<String, Object> thisBiomolecule,
                               float[] unitCellParams,
                               boolean applySymmetryToBonds, String filter) {
    if (latticeCells != null && latticeCells[0] != 0) {
      Logger.error("Cannot apply biomolecule when lattice cells are indicated");
      return;
    }
    int particleMode = (filter.indexOf("BYCHAIN") >= 0 ? PARTICLE_CHAIN
        : filter.indexOf("BYSYMOP") >= 0 ? PARTICLE_SYMOP : PARTICLE_NONE);

    doNormalize = false;
    Lst<M4> biomts = (Lst<M4>) thisBiomolecule.get("biomts");
    if (biomts.size() < 2)
      return;
    symmetry = null;
    // it's not clear to me why you would do this:
    if (!Float.isNaN(unitCellParams[0])) // PDB can do this; 
      setUnitCell(unitCellParams, null, unitCellOffset);
    getSymmetry().setSpaceGroup(doNormalize);
    //symmetry.setUnitCell(null);
    addSpaceGroupOperation("x,y,z", false);
    String name = (String) thisBiomolecule.get("name");
    setAtomSetSpaceGroupName(acr.sgName = name);
    int len = biomts.size();
    this.applySymmetryToBonds = applySymmetryToBonds;
    bondCount0 = asc.bondCount;
    boolean addBonds = (bondCount0 > asc.bondIndex0 && applySymmetryToBonds);
    int[] atomMap = (addBonds ? new int[asc.ac] : null);
    firstSymmetryAtom = asc.getLastAtomSetAtomIndex();
    int atomMax = asc.ac;
    Map<Integer, BS> ht = new Hashtable<Integer, BS>();
    int nChain = 0;
    Atom[] atoms = asc.atoms;
    switch (particleMode) {
    case PARTICLE_CHAIN:
      for (int i = atomMax; --i >= firstSymmetryAtom;) {
        Integer id = Integer.valueOf(atoms[i].chainID);
        BS bs = ht.get(id);
        if (bs == null) {
          nChain++;
          ht.put(id, bs = new BS());
        }
        bs.set(i);
      }
      asc.bsAtoms = new BS();
      for (int i = 0; i < nChain; i++) {
        asc.bsAtoms.set(atomMax + i);
        Atom a = new Atom();
        a.set(0, 0, 0);
        a.radius = 16;
        asc.addAtom(a);
      }
      int ichain = 0;
      for (Entry<Integer, BS> e : ht.entrySet()) {
        Atom a = atoms[atomMax + ichain++];
        BS bs = e.getValue();
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
          a.add(atoms[i]);
        a.scale(1f / bs.cardinality());
        a.atomName = "Pt" + ichain;
        a.chainID = e.getKey().intValue();
      }
      firstSymmetryAtom = atomMax;
      atomMax += nChain;
      break;
    case PARTICLE_SYMOP:
      asc.bsAtoms = new BS();
      asc.bsAtoms.set(atomMax);
      Atom a = atoms[atomMax] = new Atom();
      a.set(0, 0, 0);
      for (int i = atomMax; --i >= firstSymmetryAtom;)
        a.add(atoms[i]);
      a.scale(1f / (atomMax - firstSymmetryAtom));
      a.atomName = "Pt";
      a.radius = 16;
      asc.addAtom(a);
      firstSymmetryAtom = atomMax++;
      break;
    }
    if (filter.indexOf("#<") >= 0) {
      len = Math.min(len,
          PT.parseInt(filter.substring(filter.indexOf("#<") + 2)) - 1);
      filter = PT.rep(filter, "#<", "_<");
    }
    for (int iAtom = firstSymmetryAtom; iAtom < atomMax; iAtom++)
      atoms[iAtom].bsSymmetry = BSUtil.newAndSetBit(0);
    for (int i = 1; i < len; i++) {
      if (filter.indexOf("!#") >= 0) {
        if (filter.indexOf("!#" + (i + 1) + ";") >= 0)
          continue;
      } else if (filter.indexOf("#") >= 0
          && filter.indexOf("#" + (i + 1) + ";") < 0) {
        continue;
      }
      M4 mat = biomts.get(i);
      //Vector3f trans = new Vector3f();    
      for (int iAtom = firstSymmetryAtom; iAtom < atomMax; iAtom++) {
        if (asc.bsAtoms != null && !asc.bsAtoms.get(iAtom))
          continue;
        try {
          int atomSite = atoms[iAtom].atomSite;
          Atom atom1;
          if (addBonds)
            atomMap[atomSite] = asc.ac;
          atom1 = asc.newCloneAtom(atoms[iAtom]);
          if (asc.bsAtoms != null)
            asc.bsAtoms.set(atom1.index);
          atom1.atomSite = atomSite;
          mat.rotTrans(atom1);
          atom1.bsSymmetry = BSUtil.newAndSetBit(i);
          if (addBonds) {
            // Clone bonds
            for (int bondNum = asc.bondIndex0; bondNum < bondCount0; bondNum++) {
              Bond bond = asc.bonds[bondNum];
              int iAtom1 = atomMap[atoms[bond.atomIndex1].atomSite];
              int iAtom2 = atomMap[atoms[bond.atomIndex2].atomSite];
              if (iAtom1 >= atomMax || iAtom2 >= atomMax)
                asc.addNewBondWithOrder(iAtom1, iAtom2, bond.order);
            }
          }
        } catch (Exception e) {
          asc.errorMessage = "appendAtomCollection error: " + e;
        }
      }
      if (i > 0)
        symmetry.addBioMoleculeOperation(mat, false);
    }
    noSymmetryCount = atomMax - firstSymmetryAtom;
    asc.setCurrentModelInfo("presymmetryAtomIndex",
        Integer.valueOf(firstSymmetryAtom));
    asc.setCurrentModelInfo("presymmetryAtomCount",
        Integer.valueOf(noSymmetryCount));
    asc.setCurrentModelInfo("biosymmetryCount", Integer.valueOf(len));
    asc.setCurrentModelInfo("biosymmetry", symmetry);
    finalizeSymmetry(symmetry);
    setSymmetryOps();
    reset();
    //TODO: need to clone bonds
  }

  private void reset() {
    asc.coordinatesAreFractional = false;
    asc.setCurrentModelInfo("hasSymmetry", Boolean.TRUE);
    asc.setGlobalBoolean(AtomSetCollection.GLOBAL_SYMMETRY);
  }

  private P3 ptTemp;
  private M3 mTemp;

  public Tensor addRotatedTensor(Atom a, Tensor t, int iSym, boolean reset,
                                 SymmetryInterface symmetry) {
    if (ptTemp == null) {
      ptTemp = new P3();
      mTemp = new M3();
    }
    return a.addTensor(((Tensor) acr.getInterface("org.jmol.util.Tensor"))
        .setFromEigenVectors(
            symmetry.rotateAxes(iSym, t.eigenVectors, ptTemp, mTemp),
            t.eigenValues, t.isIsotropic ? "iso" : t.type, t.id, t), null,
        reset);
  }

  void setTensors() {
    int n = asc.ac;
    for (int i = asc.getLastAtomSetAtomIndex(); i < n; i++) {
      Atom a = asc.atoms[i];
      if (a.anisoBorU == null)
        continue;
      // getTensor will return correct type
      a.addTensor(symmetry.getTensor(acr.vwr, a.anisoBorU), null, false);
      if (Float.isNaN(a.bfactor))
        a.bfactor = a.anisoBorU[7] * 100f;
      // prevent multiple additions
      a.anisoBorU = null;
    }
  }

  public void setTimeReversal(int op, int timeRev) {
    symmetry.setTimeReversal(op, timeRev);
  }

  public void rotateToSuperCell(V3 t) {
    if (matSupercell != null)
      matSupercell.rotTrans(t);
  }

  private int nVib;

  public int setSpinVectors() {
    // return spin vectors to cartesians
    if (nVib > 0 || asc.iSet < 0 || !acr.vibsFractional)
      return nVib; // already done
    int i0 = asc.getAtomSetAtomIndex(asc.iSet);
    SymmetryInterface sym = getBaseSymmetry();
    for (int i = asc.ac; --i >= i0;) {
      Vibration v = (Vibration) asc.atoms[i].vib;
      if (v != null) {
        if (v.modDim > 0) {
          ((JmolModulationSet) v).setMoment();
        } else {
          //System.out.println("xytalsym v=" + v + "  "+ i + "  ");
          v = (Vibration) v.clone(); // this could be a modulation set
          sym.toCartesian(v, true);
          asc.atoms[i].vib = v;
        }
        nVib++;
      }
    }
    return nVib;
  }

  /**
   * magCIF files have moments expressed as Bohr magnetons along
   * the cryrstallographic axes. These have to be "fractionalized" in order
   * to be properly handled by symmetry operations, then, in the end, turned
   * into Cartesians.
   * 
   * It is not clear to me at all how this would be handled if there are subsystems.
   * This method must be run PRIOR to applying symmetry and thus prior to creation of 
   * modulation sets.
   * 
   */
  public void scaleFractionalVibs() {
    float[] params = getBaseSymmetry().getUnitCellParams();
    P3 ptScale = P3.new3(1 / params[0], 1 / params[1], 1 / params[2]);
    int i0 = asc.getAtomSetAtomIndex(asc.iSet);
    for (int i = asc.ac; --i >= i0;) {
      Vibration v = (Vibration) asc.atoms[i].vib;
      if (v != null) {
        v.scaleT(ptScale);
      }
    }
  }

  /**
   * Get the symmetry that was in place prior to any supercell business
   * @return base symmetry
   */
  public SymmetryInterface getBaseSymmetry() {
    return (baseSymmetry == null ? symmetry : baseSymmetry);
  }
  
  /**
   * Ensure that ModelLoader sets up the supercell unit cell.
   * 
   * @param ptSupercell
   */
  public void finalizeUnitCell(P3 ptSupercell) {
    if (ptSupercell != null && baseUnitCell != null) {
      baseUnitCell[22] = Math.max(1, (int) ptSupercell.x);
      baseUnitCell[23] = Math.max(1, (int) ptSupercell.y);
      baseUnitCell[24] = Math.max(1, (int) ptSupercell.z);
    }
  }
  
//  static {
//    System.out.println(.01999998f);
//    System.out.println(1.01999998f);
//    System.out.println(2.01999998f);
//    System.out.println(9910.01999998f);
//    System.out.println(Math.round(100000*.01999998f));
//    System.out.println(Math.round(100000*.020000000000015));
//    System.out.println(Math.round(100000*-.01999998f));
//    System.out.println(Math.round(100000*-.020000000000015));
//    System.out.println(.01999998f+ Integer.MAX_VALUE);
//  }

}
