package org.jmol.modelset;

import java.util.Map;

import javajs.util.Lst;
import javajs.util.P3;

import org.jmol.c.STR;
import org.jmol.java.BS;

public interface JmolBioModelSet {


  //  static-like general methods -- called on ModelSet.bioModel only

  void calcAllRasmolHydrogenBonds(BS bsA, BS bsB, Lst<Bond> vHBonds,
                                         boolean nucleicOnly, int nMax,
                                         boolean dsspIgnoreHydrogens,
                                         BS bsHBonds);

  void calcSelectedMonomersCount();

  void calculateAllPolymers(Group[] groups, int groupCount,
                                   int baseGroupIndex, BS modelsExcluded);

  String calculateAllStuctures(BS bsAtoms, boolean asDSSP,
                                      boolean doReport,
                                      boolean dsspIgnoreHydrogen,
                                      boolean setStructure);

  String calculateAllStructuresExcept(BS alreadyDefined, boolean asDSSP,
                                             boolean doReport,
                                             boolean dsspIgnoreHydrogen,
                                             boolean setStructure,
                                             boolean includeAlpha);

  void calculateStraightnessAll();

  int calculateStruts(BS bs1, BS bs2);

  String getAllDefaultStructures(BS bsAtoms, BS bsModified);

  Map<String, String> getAllHeteroList(int modelIndex);

  void getAllPolymerPointsAndVectors(BS bs, Lst<P3[]> vList,
                                            boolean isTraceAlpha,
                                            float sheetSmoothing);

  BS getAllSequenceBits(String specInfo, BS bs);

  BS getAtomBitsBS(int tokType, BS bsInfo, BS bs);

  BS getAtomBitsStr(int tokType, String specInfo, BS bs);

  int getBioPolymerCountInModel(int modelIndex);

  String getFullProteinStructureState(BS bsAtoms, int mode);

  BS getIdentifierOrNull(String identifier);

  BS getGroupsWithinAll(int nResidues, BS bs);

  boolean mutate(BS bs, String group, String[] sequence);

  void recalculateAllPolymers(BS bsModelsExcluded, Group[] groups);

  void recalculatePoints(int modelIndex);

  void setAllConformation(BS bsAtoms);

  void setAllProteinType(BS bs, STR type);

  void setAllStructureList(Map<STR, float[]> structureList);

  void getAllPolymerInfo(BS bs, Map<String, Lst<Map<String, Object>>> info);

 }
