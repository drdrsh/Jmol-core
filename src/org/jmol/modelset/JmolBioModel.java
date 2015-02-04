package org.jmol.modelset;

import java.util.Map;

import javajs.util.Lst;
import javajs.util.OC;
import javajs.util.P3;
import javajs.util.SB;

import org.jmol.c.STR;
import org.jmol.java.BS;

public interface JmolBioModel {

  //  static-like general methods

  public void calcAllRasmolHydrogenBonds(BS bsA, BS bsB, Lst<Bond> vHBonds,
                                         boolean nucleicOnly, int nMax,
                                         boolean dsspIgnoreHydrogens,
                                         BS bsHBonds);

  public void calcSelectedMonomersCount();

  public void calculateAllPolymers(Group[] groups, int groupCount,
                                   int baseGroupIndex, BS modelsExcluded);

  public String calculateAllStructuresExcept(BS alreadyDefined, boolean asDSSP,
                                             boolean doReport,
                                             boolean dsspIgnoreHydrogen,
                                             boolean setStructure,
                                             boolean includeAlpha);

  public void calculateStraightnessAll();

  public int calculateStruts(BS bs1, BS bs2);

  public String getAllDefaultStructures(BS bsAtoms, BS bsModified);

  public void getAllPolymerPointsAndVectors(BS bs, Lst<P3[]> vList,
                                            boolean isTraceAlpha,
                                            float sheetSmoothing);

  public BS getAllSequenceBits(String specInfo, BS bs);

  public int getBioPolymerCountInModel(int modelIndex);

  public void getConformations(int modelIndex, int conformationIndex,
                               boolean doSet, BS bsAtoms, BS bsRet);

  public String getFullProteinStructureState(BS bsAtoms, boolean taintedOnly,
                                             boolean needPhiPsi, int mode);

  public BS getGroupsWithinAll(int nResidues, BS bs);

  public void getPolymerInfo(BS bs,
                             Map<String, Lst<Map<String, Object>>> finalInfo,
                             Lst<Map<String, Object>> modelVector);

  public void recalculatePoints(int modelIndex);

  public void setAllConformation(BS bsAtoms);

  public void setAllProteinType(BS bs, STR type);

  public void setAllStructureList(Map<STR, float[]> structureList);

  // others

  public String calculateAllStuctures(BS bsAtoms, boolean asDSSP,
                                      boolean doReport,
                                      boolean dsspIgnoreHydrogen,
                                      boolean setStructure);

  public Map<String, String> getAllHeteroList(int modelIndex);

  public BS getAtomBits(int tokType, Object specInfo, BS bs);

  BS getAtomBitsMaybeDeleted(int tokType, Object specInfo, BS bs);

  Lst<BS> getBioBranches(Lst<BS> biobranches);

  public int getBioPolymerCount();

  public void getDefaultLargePDBRendering(SB sb, int maxAtoms);

  public String getFullPDBHeader();

  public BS getIdentifierOrNull(String identifier);

  public void getPdbData(String type, char ctype, boolean isDraw,
                         BS bsSelected, OC out, LabelToken[] tokens,
                         SB pdbCONECT, BS bsWritten);

  public void recalculateAllPolymers(BS bsModelsExcluded, Group[] groups);

  public void resetRasmolBonds(BS bs);

  boolean mutate(BS bs, String group, String[] sequence);

}
