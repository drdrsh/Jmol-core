/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
package org.jmol.modelset;

import java.util.Hashtable;

import java.util.Map;
import java.util.Properties;

import org.jmol.api.SymmetryInterface;
import org.jmol.c.STR;
import org.jmol.java.BS;


import javajs.util.AU;
import javajs.util.OC;
import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.SB;

import org.jmol.util.BSUtil;
import org.jmol.viewer.Viewer;

public class Model {

  /*
   * In Jmol all atoms and bonds are kept as a set of arrays in 
   * the AtomCollection and BondCollection objects. 
   * Thus, "Model" is not atoms and bonds. 
   * It is a description of all the:
   * 
   * chains (as defined in the file)
   *   and their associated file-associated groups,  
   * polymers (same, I think, but in terms of secondary structure)
   *   and their associated monomers
   * molecules (as defined by connectivity)
   *  
   * A Model then is just a small set of fields, a few arrays pointing
   * to other objects, and a couple of hash tables for information storage
   * 
   * Additional information here includes
   * how many atoms there were before symmetry was applied
   * as well as a bit about insertions and alternative locations.
   * 
   * 
   * one model = one animation "frame", but we don't use the "f" word
   * here because that would confuse the issue.
   * 
   * If multiple files are loaded, then they will appear here in 
   * at least as many Model objects. Each vibration will be a complete
   * set of atoms as well.
   * 
   * Jmol 11.3.58 developed the trajectory idea -- where
   * multiple models may share the same structures, bonds, etc., but
   * just differ in atom positions, saved in the Trajectories Vector
   * in ModelCollection.
   *  
   */

  public ModelSet ms;

  /**
   * BE CAREFUL: FAILURE TO NULL REFERENCES TO modelSet WILL PREVENT
   * FINALIZATION AND CREATE A MEMORY LEAK.
   * 
   * @return associated ModelSet
   */
  public ModelSet getModelSet() {
    return ms;
  }

  public int modelIndex; // our 0-based reference
  int fileIndex; // 0-based file reference

  public int hydrogenCount;
  public boolean isBioModel;
  public boolean isPdbWithMultipleBonds;
  protected boolean hasRasmolHBonds;

  public String loadState = "";
  public SB loadScript = new SB();

  public boolean isModelKit;

  public boolean isModelkit() {
    return isModelKit;
  }

  Map<String, Integer> dataFrames;
  int dataSourceFrame = -1;
  String jmolData; // from a PDB remark "Jmol PDB-encoded data"
  String jmolFrameType;

  // set in ModelLoader phase:
  public int firstAtomIndex;
  public int ac = 0; // includes deleted atoms
  public final BS bsAtoms = new BS();
  public final BS bsAtomsDeleted = new BS();

  // this one is variable and calculated only if necessary:
  public int getTrueAtomCount() {
    return bsAtoms.cardinality() - bsAtomsDeleted.cardinality();
  }

  public int trajectoryBaseIndex;
  boolean isTrajectory;
  public int selectedTrajectory = -1;

  private int bondCount = -1;

  public void resetBoundCount() {
    bondCount = -1;
  }

  public int getBondCount() {
    if (bondCount >= 0)
      return bondCount;
    Bond[] bonds = ms.bo;
    bondCount = 0;
    for (int i = ms.bondCount; --i >= 0;)
      if (bonds[i].atom1.mi == modelIndex)
        bondCount++;
    return bondCount;
  }

  int firstMoleculeIndex;
  public int moleculeCount;

  public int nAltLocs;
  int nInsertions;

  public int groupCount = -1;

  protected int chainCount = 0;
  public Chain[] chains = new Chain[8];

  int biosymmetryCount;

  protected Map<String, Object> auxiliaryInfo;
  public Properties properties;
  float defaultRotationRadius;
  String defaultStructure;
  public SymmetryInterface biosymmetry;

  public Orientation orientation;

  public Model() {
    
  }
  
  public Model set(ModelSet modelSet, int modelIndex, int trajectoryBaseIndex,
      String jmolData, Properties properties, Map<String, Object> auxiliaryInfo) {
    ms = modelSet;
    dataSourceFrame = this.modelIndex = modelIndex;
    isTrajectory = (trajectoryBaseIndex >= 0);
    this.trajectoryBaseIndex = (isTrajectory ? trajectoryBaseIndex : modelIndex);
    if (auxiliaryInfo == null) {
      auxiliaryInfo = new Hashtable<String, Object>();
    }
    this.auxiliaryInfo = auxiliaryInfo;
    if (auxiliaryInfo.containsKey("biosymmetryCount")) {
      biosymmetryCount = ((Integer) auxiliaryInfo.get("biosymmetryCount"))
          .intValue();
      biosymmetry = (SymmetryInterface) auxiliaryInfo.get("biosymmetry");
    }
    this.properties = properties;
    if (jmolData == null) {
      jmolFrameType = "modelSet";
    } else {
      this.jmolData = jmolData;
      isJmolDataFrame = true;
      auxiliaryInfo.put("jmolData", jmolData);
      auxiliaryInfo.put("title", jmolData);
      jmolFrameType = (jmolData.indexOf("ramachandran") >= 0 ? "ramachandran"
          : jmolData.indexOf("quaternion") >= 0 ? "quaternion" : "data");
    }
    return this;
  }

  public boolean structureTainted;
  public boolean isJmolDataFrame;
  public long frameDelay;
  public SymmetryInterface simpleCage;
  public Map<String, Object> dssrCache;

  public int getChainCount(boolean countWater) {
    if (chainCount > 1 && !countWater)
      for (int i = 0; i < chainCount; i++)
        if (chains[i].chainID == '\0')
          return chainCount - 1;
    return chainCount;
  }

  void calcSelectedGroupsCount(BS bsSelected) {
    for (int i = chainCount; --i >= 0;)
      chains[i].calcSelectedGroupsCount(bsSelected);
  }

  public int getGroupCount() {
    if (groupCount < 0) {
      groupCount = 0;
      for (int i = chainCount; --i >= 0;)
        groupCount += chains[i].groupCount;
    }
    return groupCount;
  }

  public Chain getChainAt(int i) {
    return (i < chainCount ? chains[i] : null);
  }

  Chain getChain(int chainID) {
    for (int i = chainCount; --i >= 0;) {
      Chain chain = chains[i];
      if (chain.chainID == chainID)
        return chain;
    }
    return null;
  }

  public void fixIndices(int modelIndex, int nAtomsDeleted, BS bsDeleted) {
    fixIndicesM(modelIndex, nAtomsDeleted, bsDeleted);
  }

  protected void fixIndicesM(int modelIndex, int nAtomsDeleted, BS bsDeleted) {
    if (dataSourceFrame > modelIndex)
      dataSourceFrame--;
    if (trajectoryBaseIndex > modelIndex)
      trajectoryBaseIndex--;
    firstAtomIndex -= nAtomsDeleted;
    for (int i = 0; i < chainCount; i++)
      chains[i].fixIndices(nAtomsDeleted, bsDeleted);
    BSUtil.deleteBits(bsAtoms, bsDeleted);
    BSUtil.deleteBits(bsAtomsDeleted, bsDeleted);
  }

  public void freeze() {
    freezeM();
  }

  protected void freezeM() {
    chains = (Chain[]) AU.arrayCopyObject(chains, chainCount);
    groupCount = -1;
    getGroupCount();
    for (int i = 0; i < chainCount; ++i)
      chains[i].groups = (Group[]) AU.arrayCopyObject(chains[i].groups,
          chains[i].groupCount);
  }

  /////// BioModel only ///////

  /**
   * @param vwr
   * @param type
   * @param ctype
   * @param isDraw
   * @param bsSelected
   * @param out
   * @param bsWritten
   * @param pdbCONECT
   * @param tokens
   */
  public void getPdbData(Viewer vwr, String type, char ctype,
                         boolean isDraw, BS bsSelected, OC out,
                         LabelToken[] tokens, SB pdbCONECT, BS bsWritten) {
  }

  /**
   * @param bioBranches
   * @return updated bioBranches
   */
  public Lst<BS> getBioBranches(Lst<BS> bioBranches) {
    return bioBranches;
  }

  public void clearBioPolymers() {
  }

  /**
   * @param bs
   * @param finalInfo
   * @param modelVector
   */
  public void getPolymerInfo(
                                BS bs,
                                Map<String, Lst<Map<String, Object>>> finalInfo,
                                Lst<Map<String, Object>> modelVector) {
  }

  public int getBioPolymerCount() {
    return 0;
  }

  /**
   * @param asDSSP
   * @param doReport
   * @param dsspIgnoreHydrogen
   * @param setStructure
   * @param includeAlpha
   * @return structure list
   */
  public String calculateStructures(boolean asDSSP, boolean doReport,
                                    boolean dsspIgnoreHydrogen,
                                    boolean setStructure, boolean includeAlpha) {
    return "";
  }

  /**
   * @param structureList
   */
  public void setStructureList(Map<STR, float[]> structureList) {
  }

  public void getAllChimeInfo(SB sb) {
    getChimeInfoM(sb, 0);
  }

  protected void getChimeInfoM(SB sb, int nHetero) {
    sb.append("\nNumber of Atoms ..... " + (ms.ac - nHetero));
    if (nHetero > 0)
      sb.append(" (" + nHetero + ")");
    sb.append("\nNumber of Bonds ..... " + ms.bondCount);
    sb.append("\nNumber of Models ...... " + ms.mc);
  }

  /**
   * @param bsConformation
   */
  public void setConformation(BS bsConformation) {
    //
  }

  public String getFullPDBHeader() {
    return null;
  }

  /**
   * @param ms 
   * @param specInfo 
   * @param bs   
   * @return sequence bits
   */
  public BS getAllSequenceBits(ModelSet ms, String specInfo, BS bs) {
    // biomodel only
    return null;
  }

  /**
   * @param ms  
   * @param specInfo 
   * @return base pair bits
   */
  public BS getAllBasePairBits(ModelSet ms, String specInfo) {
    // biomodel only
    return null;
  }

  /**
   * @param bs 
   */
  public void resetRasmolBonds(BS bs) {
    // biomodel only
  }

  /**
   * @param ms 
   * @param bsA 
   * @param bsB 
   * @param vHBonds 
   * @param nucleicOnly 
   * @param nMax 
   * @param dsspIgnoreHydrogens  
   * @param bsHBonds 
   */
  public void calcAllRasmolHydrogenBonds(ModelSet ms, BS bsA, BS bsB, Lst<Bond> vHBonds,
                                      boolean nucleicOnly, int nMax,
                                      boolean dsspIgnoreHydrogens, BS bsHBonds) {
    // biomodel only
  }

  /**
   * @param modelSet  
   * @param bsAtoms2 
   * @param taintedOnly 
   * @param needPhiPsi 
   * @param mode 
   * @return state
   */
  public String getFullProteinStructureState(ModelSet modelSet, BS bsAtoms2,
                                             boolean taintedOnly,
                                             boolean needPhiPsi, int mode) {
    // biomodel only
    return null;
  }

  /**
   * @param ms 
   * @param groups  
   * @param groupCount2 
   * @param baseGroupIndex 
   * @param modelsExcluded 
   */
  public void calculateAllPolymers(ModelSet ms, Group[] groups, int groupCount2,
                                   int baseGroupIndex, BS modelsExcluded) {
    // biomodel only
  }

  /**
   * @param ms  
   * @param nResidues 
   * @param bs 
   * @return bitset 
   */
  public BS getGroupsWithinAll(ModelSet ms, int nResidues, BS bs) {
    // biomodel only
    return null;
  }

  /**
   * @param ms  
   * @param specInfo 
   * @return bitset
   */
  public BS getSelectCodeRange(ModelSet ms, int[] specInfo) {
    // biomodel only
    return null;
  }

  /**
   * @param ms  
   * @param bs1 
   * @param bs2 
   * @return number of struts
   */
  public int calculateStruts(ModelSet ms, BS bs1, BS bs2) {
    // biomodel only
    return 0;
  }

  /**
   * @param bs 
   * @param vList 
   * @param isTraceAlpha 
   * @param sheetSmoothing 
   */
  public void getPolymerPointsAndVectors(BS bs, Lst<P3[]> vList,
                                         boolean isTraceAlpha,
                                         float sheetSmoothing) {
    // biomodel only
  }

  /**
   * @param ms 
   * @param modelIndex  
   */
  public void recalculatePoints(ModelSet ms, int modelIndex) {
    // biomodel only
  }

  /**
   * @param sb  
   * @param maxAtoms 
   */
  public void getDefaultLargePDBRendering(SB sb, int maxAtoms) {
    // biomodel only    
  }

  /**
   * @param bsSelected  
   */
  public void calcSelectedMonomersCount(BS bsSelected) {
    // biomodel only    
  }

  /**
   * @param ms  
   * @param modelIndex 
   * @return count
   */
  public int getBioPolymerCountInModel(ModelSet ms, int modelIndex) {
    // biomodel only
    return 0;
  }

  /**
   * @param ms  
   */
  public void calculateStraightnessAll(ModelSet ms) {
    // biomodel only
  }

  /**
   * @param conformationIndex  
   * @param doSet 
   * @param bsAtoms 
   * @param bs 
   */
  public void getConformation(int conformationIndex, boolean doSet, BS bsAtoms,
                              BS bs) {
    // biomodel only
  }

 }
