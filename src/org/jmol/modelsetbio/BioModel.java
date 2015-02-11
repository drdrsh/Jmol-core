/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2011-08-05 21:10:46 -0500 (Fri, 05 Aug 2011) $
 * $Revision: 15943 $
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
package org.jmol.modelsetbio;

import javajs.util.AU;
import javajs.util.OC;
import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;

import java.util.Hashtable;

import java.util.Map;
import java.util.Properties;


import org.jmol.api.DSSPInterface;
import org.jmol.api.Interface;
import org.jmol.api.JmolAnnotationParser;
import org.jmol.c.STR;
import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.Group;
import org.jmol.modelset.HBond;
import org.jmol.modelset.JmolBioModel;
import org.jmol.modelset.JmolBioModelSet;
import org.jmol.modelset.LabelToken;
import org.jmol.modelset.Model;
import org.jmol.modelset.ModelSet;
import org.jmol.script.SV;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.Escape;
import org.jmol.util.Edge;
import org.jmol.util.Logger;

import javajs.util.P3;


import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;


public final class BioModel extends Model implements JmolBioModelSet, JmolBioModel {

  /*
   *   
   * Note that "monomer" extends group. A group only becomes a 
   * monomer if it can be identified as one of the following 
   * PDB/mmCIF types:
   * 
   *   amino  -- has an N, a C, and a CA
   *   alpha  -- has just a CA
   *   nucleic -- has C1',C2',C3',C4',C5',O3', and O5'
   *   phosphorus -- has P
   *   
   * The term "conformation" is a bit loose. It means "what you get
   * when you go with one or another set of alternative locations.
   *
   *  
   */
  
  private int bioPolymerCount = 0;
  public BioPolymer[] bioPolymers;
  boolean isMutated;

  private String defaultStructure;
  private Viewer vwr;


  //// effectively static methods, but called nonstatically because BioModel is hidden to JavaScript

  @Override
  public Map<String, String> getAllHeteroList(int modelIndex) {
    Map<String, String> htFull = new Hashtable<String, String>();
    boolean ok = false;
    for (int i = ms.mc; --i >= 0;)
      if (modelIndex < 0 || i == modelIndex) {
        @SuppressWarnings("unchecked")
        Map<String, String> ht = (Map<String, String>) ms.getInfo(i, "hetNames");
        if (ht == null)
          continue;
        ok = true;
        for (Map.Entry<String, String> entry : ht.entrySet()) {
          String key = entry.getKey();
          htFull.put(key, entry.getValue());
        }
      }
    return (ok ? htFull : null);
  }

  @Override
  public void setAllProteinType(BS bs, STR type) {
    int monomerIndexCurrent = -1;
    int iLast = -1;
    BS bsModels = ms.getModelBS(bs, false);
    setAllDefaultStructure(bsModels);
    Atom[] at = ms.at;
    Model[] am = ms.am;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      if (iLast != i - 1)
        monomerIndexCurrent = -1;
      monomerIndexCurrent = at[i].group.setProteinStructureType(type,
          monomerIndexCurrent);
      int modelIndex = at[i].mi;
      ms.proteinStructureTainted = am[modelIndex].structureTainted = true;
      iLast = i = at[i].group.lastAtomIndex;
    }
    int[] lastStrucNo = new int[ms.mc];
    for (int i = 0; i < ms.ac;) {
      int modelIndex = at[i].mi;
      if (!bsModels.get(modelIndex)) {
        i = am[modelIndex].firstAtomIndex + am[modelIndex].act;
        continue;
      }
      iLast = at[i].group.getStrucNo();
      if (iLast < 1000 && iLast > lastStrucNo[modelIndex])
        lastStrucNo[modelIndex] = iLast;
      i = at[i].group.lastAtomIndex + 1;
    }
    for (int i = 0; i < ms.ac;) {
      int modelIndex = at[i].mi;
      if (!bsModels.get(modelIndex)) {
        i = am[modelIndex].firstAtomIndex + am[modelIndex].act;
        continue;
      }
      if (at[i].group.getStrucNo() > 1000)
        at[i].group.setStrucNo(++lastStrucNo[modelIndex]);
      i = at[i].group.lastAtomIndex + 1;
    }
  }


  /**
   * general purpose; return models associated with specific atoms
   * @param bsAtoms
   * @param bsAtomsRet all atoms associated with these models. 
   * @return bitset of base models
   */
  private BS modelsOf(BS bsAtoms, BS bsAtomsRet) {
    BS bsModels = BS.newN(ms.mc);
    boolean isAll = (bsAtoms == null);
    int i0 = (isAll ? ms.ac - 1 : bsAtoms.nextSetBit(0));
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bsAtoms.nextSetBit(i + 1))) {
      int modelIndex = ms.am[ms.at[i].mi].trajectoryBaseIndex;
      if (ms.isJmolDataFrameForModel(modelIndex))
        continue;
      bsModels.set(modelIndex);
      bsAtomsRet.set(i);
    }
    return bsModels;
  }

  @Override
  public String getAllDefaultStructures(BS bsAtoms, BS bsModified) {
    BS bsModels = modelsOf(bsAtoms, bsModified);
    SB ret = new SB();
    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1)) 
      if (ms.am[i].isBioModel && ((BioModel) ms.am[i]).defaultStructure != null)
        ret.append(((BioModel) ms.am[i]).defaultStructure);
    return ret.toString();
  }

  @Override
  public String calculateAllStuctures(BS bsAtoms, boolean asDSSP,
                                        boolean doReport,
                                        boolean dsspIgnoreHydrogen,
                                        boolean setStructure) {
    BS bsAllAtoms = new BS();
    BS bsModelsExcluded = BSUtil.copyInvert(modelsOf(bsAtoms, bsAllAtoms),
        ms.mc);
    if (!setStructure)
      return ms.calculateStructuresAllExcept(bsModelsExcluded, asDSSP, doReport,
          dsspIgnoreHydrogen, false, false);
    ms.recalculatePolymers(bsModelsExcluded);
    String ret = ms.calculateStructuresAllExcept(bsModelsExcluded, asDSSP, doReport,
        dsspIgnoreHydrogen, true, false);
    vwr.shm.resetBioshapes(bsAllAtoms);
    ms.setStructureIndexes();
    return ret;
  }

  @Override
  public String calculateAllStructuresExcept(BS alreadyDefined, boolean asDSSP,
                                       boolean doReport,
                                       boolean dsspIgnoreHydrogen,
                                       boolean setStructure,
                                       boolean includeAlpha) {
    String ret = "";
    BS bsModels = BSUtil.copyInvert(alreadyDefined, ms.mc);
    //working here -- testing reset
    //TODO bsModels first for not setStructure, after that for setstructure....
    if (setStructure)
      setAllDefaultStructure(bsModels);
    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1))
      if (ms.am[i].isBioModel)
        ret += ((BioModel) ms.am[i]).calculateStructures(asDSSP, doReport,
            dsspIgnoreHydrogen, setStructure, includeAlpha);
    if (setStructure)
      ms.setStructureIndexes();
    return ret;
  }

  public void setAllDefaultStructure(BS bsModels) {
    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1))
      if (ms.am[i].isBioModel) {
        BioModel m = (BioModel) ms.am[i];
        if (m.defaultStructure == null)
          m.defaultStructure = getFullProteinStructureState(m.bsAtoms, false,
              false, 0);
      }
  }

  @Override
  public void setAllStructureList(Map<STR, float[]> structureList) {
    for (int iModel = ms.mc; --iModel >= 0;)
      if (ms.am[iModel].isBioModel) {
        BioModel m = (BioModel) ms.am[iModel];
        m.bioPolymers = (BioPolymer[]) AU.arrayCopyObject(m.bioPolymers,
            m.bioPolymerCount);
        for (int i = m.bioPolymerCount; --i >= 0;)
          m.bioPolymers[i].setStructureList(structureList);
      }
  }

  @Override
  public void setAllConformation(BS bsAtoms) {
    BS bsModels = ms.getModelBS(bsAtoms, false);
    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1))
      if (ms.am[i].isBioModel) {
        BioModel m = (BioModel) ms.am[i];
        if (m.altLocCount > 0)
          for (int j = m.bioPolymerCount; --j >= 0;)
            m.bioPolymers[j].setConformation(bsAtoms);
      }
  }


  @Override
  public void getAllPolymerPointsAndVectors(BS bs, Lst<P3[]> vList,
                                         boolean isTraceAlpha,
                                         float sheetSmoothing) {
    for (int i = 0; i < ms.mc; ++i)
      if (ms.am[i].isBioModel) {
        BioModel m = (BioModel) ms.am[i];
        int last = Integer.MAX_VALUE - 1;
        for (int ip = 0; ip < m.bioPolymerCount; ip++)
          last = m.bioPolymers[ip].getPolymerPointsAndVectors(last, bs, vList,
              isTraceAlpha, sheetSmoothing);
      }
  }

  @Override
  public void calcSelectedMonomersCount() {
    BS bsSelected = vwr.bsA();
    for (int i = ms.mc; --i >= 0;)
      if (ms.am[i].isBioModel) {
        BioModel m = (BioModel) ms.am[i];
        for (int j = m.bioPolymerCount; --j >= 0;)
          m.bioPolymers[j].calcSelectedMonomersCount(bsSelected);
      }
  }

  /**
   * @param modelIndex
   * @return number of polymers
   */
  @Override
  public int getBioPolymerCountInModel(int modelIndex) {
    if (modelIndex < 0) {
      int polymerCount = 0;
      for (int i = ms.mc; --i >= 0;)
        if (!ms.isTrajectorySubFrame(i) && ms.am[i].isBioModel)
          polymerCount += ((BioModel) ms.am[i]).getBioPolymerCount();
      return polymerCount;
    }
    return (ms.isTrajectorySubFrame(modelIndex) || !ms.am[modelIndex].isBioModel ? 0
        : ((BioModel) ms.am[modelIndex]).getBioPolymerCount());
  }

  @Override
  public void calculateAllPolymers(Group[] groups, int groupCount,
                                   int baseGroupIndex, BS modelsExcluded) {
    boolean checkConnections = !vwr.getBoolean(T.pdbsequential);
    if (groupCount < 0)
      groupCount = groups.length;
    
    if (modelsExcluded != null)
      for (int j = 0; j < groupCount; ++j) {
        Group group = groups[j];
        if (group instanceof Monomer) {
          if (((Monomer) group).bioPolymer != null
              && (!modelsExcluded.get(group.chain.model.modelIndex)))
            ((Monomer) group).setBioPolymer(null, -1);
        }
      }
    for (int i = 0, mc = ms.mc; i < mc; i++)
      if ((modelsExcluded == null || !modelsExcluded.get(i))
          && ms.am[i].isBioModel) {
        for (int j = baseGroupIndex; j < groupCount; ++j) {
          Group g = groups[j];
          Model model = g.getModel();
          if (!model.isBioModel || !(g instanceof Monomer))
            continue;
          boolean doCheck = checkConnections
              && !ms.isJmolDataFrameForModel(ms.at[g.firstAtomIndex].mi);
          BioPolymer bp = (((Monomer) g).bioPolymer == null ? Resolver
              .allocateBioPolymer(groups, j, doCheck) : null);
          if (bp == null || bp.monomerCount == 0)
            continue;
          ((BioModel) model).addBioPolymer(bp);
          j += bp.monomerCount - 1;
        }
      }
  }
  
  @Override
  public void recalculateAllPolymers(BS bsModelsExcluded, Group[] groups) {
    for (int i = 0; i < ms.mc; i++)
      if (ms.am[i].isBioModel && !bsModelsExcluded.get(i))
        ((BioModel) ms.am[i]).clearBioPolymers();
    calculateAllPolymers(groups, -1, 0, bsModelsExcluded);
  }

  @Override
  public BS getGroupsWithinAll(int nResidues, BS bs) {
    BS bsResult = new BS();
    BS bsCheck = ms.getIterativeModels(false);
    for (int iModel = ms.mc; --iModel >= 0;)
      if (bsCheck.get(iModel) && ms.am[iModel].isBioModel) {
        BioModel m = (BioModel) ms.am[iModel];
        for (int i = m.bioPolymerCount; --i >= 0;)
          m.bioPolymers[i].getRangeGroups(nResidues, bs, bsResult);
      }
    return bsResult;
  }

  @Override
  public int calculateStruts(BS bs1, BS bs2) {
    vwr.setModelVisibility();
    // select only ONE model
    ms.makeConnections2(0, Float.MAX_VALUE, Edge.BOND_STRUT, T.delete, bs1,
        bs2, null, false, false, 0);
    int iAtom = bs1.nextSetBit(0);
    if (iAtom < 0)
      return 0;
    Model m = ms.am[ms.at[iAtom].mi];
    if (!m.isBioModel)
      return 0;
    // only check the atoms in THIS model
    Lst<Atom> vCA = new  Lst<Atom>();
    Atom a1 = null;
    BS bsCheck;
    if (bs1.equals(bs2)) {
      bsCheck = bs1;
    } else {
      bsCheck = BSUtil.copy(bs1);
      bsCheck.or(bs2);
    }
    Atom[] atoms = ms.at;
    bsCheck.and(vwr.getModelUndeletedAtomsBitSet(m.modelIndex));
    for (int i = bsCheck.nextSetBit(0); i >= 0; i = bsCheck.nextSetBit(i + 1))
      if (atoms[i].checkVisible()
          && atoms[i].atomID == JC.ATOMID_ALPHA_CARBON
          && atoms[i].group.groupID != JC.GROUPID_CYSTEINE)
        vCA.addLast((a1 = atoms[i]));
    if (vCA.size() == 0)
      return 0;    
    float thresh = vwr.getFloat(T.strutlengthmaximum);
    short mad = (short) (vwr.getFloat(T.strutdefaultradius) * 2000);
    int delta = vwr.getInt(T.strutspacing);
    boolean strutsMultiple = vwr.getBoolean(T.strutsmultiple);
    Lst<Atom[]> struts = ((BioModel) m).bioPolymers[a1.group.getBioPolymerIndexInModel()]
        .calculateStruts(ms, bs1, bs2, vCA, thresh, delta, strutsMultiple);
    for (int i = 0; i < struts.size(); i++) {
      Atom[] o = struts.get(i);
      ms.bondAtoms(o[0], o[1], Edge.BOND_STRUT, mad, null, 0, false, true);
    }
    return struts.size(); 
  }

  @Override
  public void recalculatePoints(int modelIndex) {
    if (modelIndex < 0) {
      for (int i = ms.mc; --i >= 0;)
        if (!ms.isTrajectorySubFrame(i) && ms.am[i].isBioModel)
          ((BioModel) ms.am[i]).recalculateLeadMidpointsAndWingVectors();
      return;
    }
    if (!ms.isTrajectorySubFrame(modelIndex) && ms.am[modelIndex].isBioModel)
      ((BioModel) ms.am[modelIndex]).recalculateLeadMidpointsAndWingVectors();
  }

  @SuppressWarnings("incomplete-switch")
  @Override
  public String getFullProteinStructureState(BS bsAtoms2,
                                             boolean taintedOnly,
                                             boolean needPhiPsi, int mode) {
    for (int im = 0, mc = ms.mc; im < mc; im++) {
      if (!ms.am[im].isBioModel)
        continue;
      BioModel m = (BioModel) ms.am[im];
      boolean showMode = (mode == 3);
      boolean pdbFileMode = (mode == 1);
      boolean scriptMode = (mode == 0);
      BS bs = null;
      SB cmd = new SB();
      SB sbTurn = new SB();
      SB sbHelix = new SB();
      SB sbSheet = new SB();
      STR type = STR.NONE;
      STR subtype = STR.NONE;
      int id = 0;
      int iLastAtom = 0;
      int iLastModel = -1;
      int lastId = -1;
      int res1 = 0;
      int res2 = 0;
      String sid = "";
      String group1 = "";
      String group2 = "";
      String chain1 = "";
      String chain2 = "";
      int n = 0;
      int nHelix = 0;
      int nTurn = 0;
      int nSheet = 0;
      BS bsTainted = null;
      Model[] models = ms.am;
      Atom[] atoms = ms.at;
      int ac = ms.ac;

      if (taintedOnly) {
        if (!ms.proteinStructureTainted)
          return "";
        bsTainted = new BS();
        for (int i = m.firstAtomIndex; i < ac; i++)
          if (models[atoms[i].mi].structureTainted)
            bsTainted.set(i);
        bsTainted.set(ac);
      }
      for (int i = 0; i <= ac; i++)
        if (i == ac || bsAtoms == null || bsAtoms.get(i)) {
          if (taintedOnly && !bsTainted.get(i))
            continue;
          id = 0;
          if (i == ac || (id = atoms[i].group.getStrucNo()) != lastId) {
            if (bs != null) {
              switch (type) {
              case HELIX:
              case TURN:
              case SHEET:
                n++;
                if (scriptMode) {
                  int iModel = atoms[iLastAtom].mi;
                  String comment = "    \t# model="
                      + ms.getModelNumberDotted(iModel);
                  if (iLastModel != iModel) {
                    iLastModel = iModel;
                    cmd.append("  structure none ")
                        .append(
                            Escape.eBS(ms.getModelAtomBitSetIncludingDeleted(
                                iModel, false))).append(comment).append(";\n");
                  }
                  comment += " & (" + res1 + " - " + res2 + ")";
                  String stype = subtype.getBioStructureTypeName(false);
                  cmd.append("  structure ").append(stype).append(" ")
                      .append(Escape.eBS(bs)).append(comment).append(";\n");
                } else {
                  String str;
                  int nx;
                  SB sb;
                  // NNN III GGG C RRRR GGG C RRRR
                  // HELIX 99 99 LYS F 281 LEU F 293 1
                  // NNN III 2 GGG CRRRR GGG CRRRR
                  // SHEET 1 A 8 ILE A 43 ASP A 45 0
                  // NNN III GGG CRRRR GGG CRRRR
                  // TURN 1 T1 PRO A 41 TYR A 44
                  switch (type) {
                  case HELIX:
                    nx = ++nHelix;
                    if (sid == null || pdbFileMode)
                      sid = PT.formatStringI("%3N %3N", "N", nx);
                    str = "HELIX  %ID %3GROUPA %1CA %4RESA  %3GROUPB %1CB %4RESB";
                    sb = sbHelix;
                    String stype = null;
                    switch (subtype) {
                    case HELIX:
                    case HELIXALPHA:
                      stype = "  1";
                      break;
                    case HELIX310:
                      stype = "  5";
                      break;
                    case HELIXPI:
                      stype = "  3";
                      break;
                    }
                    if (stype != null)
                      str += stype;
                    break;
                  case SHEET:
                    nx = ++nSheet;
                    if (sid == null || pdbFileMode) {
                      sid = PT.formatStringI("%3N %3A 0", "N", nx);
                      sid = PT.formatStringS(sid, "A", "S" + nx);
                    }
                    str = "SHEET  %ID %3GROUPA %1CA%4RESA  %3GROUPB %1CB%4RESB";
                    sb = sbSheet;
                    break;
                  case TURN:
                  default:
                    nx = ++nTurn;
                    if (sid == null || pdbFileMode)
                      sid = PT.formatStringI("%3N %3N", "N", nx);
                    str = "TURN   %ID %3GROUPA %1CA%4RESA  %3GROUPB %1CB%4RESB";
                    sb = sbTurn;
                    break;
                  }
                  str = PT.formatStringS(str, "ID", sid);
                  str = PT.formatStringS(str, "GROUPA", group1);
                  str = PT.formatStringS(str, "CA", chain1);
                  str = PT.formatStringI(str, "RESA", res1);
                  str = PT.formatStringS(str, "GROUPB", group2);
                  str = PT.formatStringS(str, "CB", chain2);
                  str = PT.formatStringI(str, "RESB", res2);
                  sb.append(str);
                  if (showMode)
                    sb.append(" strucno= ").appendI(lastId);
                  sb.append("\n");

                  /*
                   * HELIX 1 H1 ILE 7 PRO 19 1 3/10 CONFORMATION RES 17,19 1CRN 55
                   * HELIX 2 H2 GLU 23 THR 30 1 DISTORTED 3/10 AT RES 30 1CRN 56
                   * SHEET 1 S1 2 THR 1 CYS 4 0 1CRNA 4 SHEET 2 S1 2 CYS 32 ILE 35
                   */
                }
              }
              bs = null;
            }
            if (id == 0
                || bsAtoms != null
                && needPhiPsi
                && (Float.isNaN(atoms[i].group.getGroupParameter(T.phi)) || Float
                    .isNaN(atoms[i].group.getGroupParameter(T.psi))))
              continue;
          }
          String ch = atoms[i].getChainIDStr();
          if (bs == null) {
            bs = new BS();
            res1 = atoms[i].getResno();
            group1 = atoms[i].getGroup3(false);
            chain1 = ch;
          }
          type = atoms[i].group.getProteinStructureType();
          subtype = atoms[i].group.getProteinStructureSubType();
          sid = atoms[i].group.getProteinStructureTag();
          bs.set(i);
          lastId = id;
          res2 = atoms[i].getResno();
          group2 = atoms[i].getGroup3(false);
          chain2 = ch;
          iLastAtom = i;
        }
      if (n > 0)
        cmd.append("\n");
      return (scriptMode ? cmd.toString() : sbHelix.appendSB(sbSheet)
          .appendSB(sbTurn).appendSB(cmd).toString());
    }
    return "";
  }

  @Override
  public BS getAllSequenceBits(String specInfo, BS bs) {
    BS bsResult = new BS();
    if (specInfo.length() > 0) {
      if (bs == null)
        bs = vwr.getAllAtoms();
      Model[] am = ms.am;
      for (int i = ms.mc; --i >= 0;)
        if (am[i].isBioModel) {
          BioModel m = (BioModel) am[i];
          int lenInfo = specInfo.length();
          for (int ip = 0; ip < m.bioPolymerCount; ip++) {
            String sequence = m.bioPolymers[ip].getSequence();
            int j = -1;
            while ((j = sequence.indexOf(specInfo, ++j)) >= 0)
              m.bioPolymers[ip].getPolymerSequenceAtoms(j, lenInfo, bs,
                  bsResult);
          }
        }
    }
    return bsResult;
  }
  
  private BS getAllBasePairBits(String specInfo) {
    BS bsA = null;
    BS bsB = null;
    Lst<Bond> vHBonds = new Lst<Bond>();
    if (specInfo.length() == 0) {
      bsA = bsB = vwr.getAllAtoms();
      calcAllRasmolHydrogenBonds(bsA, bsB, vHBonds, true, 1, false, null);
    } else {
      for (int i = 0; i < specInfo.length();) {
        bsA = ms.getSequenceBits(specInfo.substring(i, ++i), null);
        if (bsA.cardinality() == 0)
          continue;
        bsB = ms.getSequenceBits(specInfo.substring(i, ++i), null);
        if (bsB.cardinality() == 0)
          continue;
        calcAllRasmolHydrogenBonds(bsA, bsB, vHBonds, true, 1, false, null);
      }
    }
    BS bsAtoms = new BS();
    for (int i = vHBonds.size(); --i >= 0;) {
      Bond b = vHBonds.get(i);
      bsAtoms.set(b.atom1.i);
      bsAtoms.set(b.atom2.i);
    }
    return bsAtoms;
  }

  /**
   *  only for base models, not trajectories
   * @param bsA 
   * @param bsB 
   * @param vHBonds will be null for autobonding
   * @param nucleicOnly 
   * @param nMax 
   * @param dsspIgnoreHydrogens 
   * @param bsHBonds 
   */
  @Override
  public void calcAllRasmolHydrogenBonds(BS bsA, BS bsB, Lst<Bond> vHBonds,
                                      boolean nucleicOnly, int nMax,
                                      boolean dsspIgnoreHydrogens, BS bsHBonds) {
    Model[] am = ms.am;
    if (vHBonds == null) {
      // autobond -- clear all hydrogen bonds
      BS bsAtoms = bsA;
      if (bsB != null && !bsA.equals(bsB))
        (bsAtoms = BSUtil.copy(bsA)).or(bsB);
      BS bsDelete = new BS();
      BS bsOK = new BS();
      Model[] models = ms.am;
      Bond[] bonds = ms.bo;
      for (int i = ms.bondCount; --i >= 0;) {
        Bond bond = bonds[i];
        if ((bond.order & Edge.BOND_H_CALC_MASK) == 0)
          continue;
        // trajectory atom .mi will be pointing to their trajectory;
        // here we check to see if their base model is this model
        if (bsAtoms.get(bond.atom1.i))
          bsDelete.set(i);
        else
          bsOK.set(models[bond.atom1.mi].trajectoryBaseIndex);
      }
      for (int i = ms.mc; --i >= 0;)
        if (models[i].isBioModel)
          ((BioModel) models[i]).hasRasmolHBonds = bsOK.get(i);
      if (bsDelete.nextSetBit(0) >= 0)
        ms.deleteBonds(bsDelete, false);
    }
    for (int i = ms.mc; --i >= 0;)
      if (am[i].isBioModel && !ms.isTrajectorySubFrame(i))
        ((BioModel) am[i]).getRasmolHydrogenBonds(bsA, bsB, vHBonds, nucleicOnly, nMax,
            dsspIgnoreHydrogens, bsHBonds);
  }

  private void getRasmolHydrogenBonds(BS bsA, BS bsB,
                                      Lst<Bond> vHBonds, boolean nucleicOnly,
                                      int nMax, boolean dsspIgnoreHydrogens,
                                      BS bsHBonds) {    
     boolean doAdd = (vHBonds == null);
     if (doAdd)
       vHBonds = new  Lst<Bond>();
     if (nMax < 0)
       nMax = Integer.MAX_VALUE;
     boolean asDSSX = (bsB == null);
     BioPolymer bp, bp1;
     if (asDSSX && bioPolymerCount > 0) {
       
       calculateDssx(vHBonds, false, dsspIgnoreHydrogens, false);
       
     } else {
       for (int i = bioPolymerCount; --i >= 0;) {
         bp = bioPolymers[i];
         int type = bp.getType();
         if ((nucleicOnly || type != BioPolymer.TYPE_AMINO)
             && type != BioPolymer.TYPE_NUCLEIC)
           continue;
         boolean isRNA = bp.isRna();
         boolean isAmino = (type == BioPolymer.TYPE_AMINO);
         if (isAmino)
           bp.calcRasmolHydrogenBonds(null, bsA, bsB, vHBonds, nMax, null, true,
               false);
         for (int j = bioPolymerCount; --j >= 0;) {
           if ((bp1 = bioPolymers[j]) != null && (isRNA || i != j)
               && type == bp1.getType()) {
             bp1.calcRasmolHydrogenBonds(bp, bsA, bsB, vHBonds, nMax, null,
                 true, false);
           }
         }
       }
     }
     
     if (vHBonds.size() == 0 || !doAdd)
       return;
     hasRasmolHBonds = true;
     for (int i = 0; i < vHBonds.size(); i++) {
       HBond bond = (HBond) vHBonds.get(i);
       Atom atom1 = bond.atom1;
       Atom atom2 = bond.atom2;
       if (atom1.isBonded(atom2))
         continue;
       int index = ms.addHBond(atom1, atom2, bond.order, bond.getEnergy());
       if (bsHBonds != null)
         bsHBonds.set(index);
     }
   }

  @Override
  public void calculateStraightnessAll() {
    char ctype = 'S';//(vwr.getTestFlag3() ? 's' : 'S');
    char qtype = vwr.getQuaternionFrame();
    int mStep = vwr.getInt(T.helixstep);
    // testflag3 ON  --> preliminary: Hanson's original normal-based straightness
    // testflag3 OFF --> final: Kohler's new quaternion-based straightness
    for (int i = ms.mc; --i >= 0;)
      if (ms.am[i].isBioModel) {
        BioModel m = (BioModel)ms.am[i];
        P3 ptTemp = new P3();
        for (int p = 0; p < m.bioPolymerCount; p++)
          m.bioPolymers[p].getPdbData(vwr, ctype, qtype, mStep, 2, null, 
              null, false, false, false, null, null, null, new BS(), ptTemp);        
      }
    ms.haveStraightness = true;
  }

  @Override
  public boolean mutate(BS bs, String group, String[] sequence) {
    
    int i0 = bs.nextSetBit(0);
    if (sequence == null)
      return mutateAtom(i0, group);
    boolean isFile = (group == null);
    if (isFile)
      group = sequence[0];
    Group lastGroup = null;
    boolean isOK = true;
    for (int i = i0, pt = 0; i >= 0; i = bs.nextSetBit(i + 1)) {
      Group g = ms.at[i].group;
      if (g == lastGroup)
        continue;
      lastGroup = g;
      if (!isFile) {
        group = sequence[pt++ % sequence.length];
        if (group.equals("UNK"))
          continue;
        group = "==" + group;
      }
      mutateAtom(i, group);
    }
    return isOK;
  }
  
  private boolean mutateAtom(int iatom, String fileName) {
    // no mutating a trajectory. What would that mean???
    int iModel = ms.at[iatom].mi;
    if (ms.isTrajectory(iModel))
      return false; 
    
    String[] info = vwr.fm.getFileInfo();
    boolean b = vwr.getBoolean(T.appendnew);
    Group g = ms.at[iatom].group;
    //try {
      // get the initial group -- protein for now
      if (!(g instanceof AminoMonomer))
        return false;
      ((BioModel) ms.am[iModel]).isMutated = true;
      AminoMonomer res0 = (AminoMonomer) g;
      int ac = ms.ac;
      BS bsRes0 = new BS();
      res0.setAtomBits(bsRes0);
      Atom[] backbone = getBackbone(res0, null);
      int r = g.getResno();
      
      // just use a script -- it is much easier!
      
      fileName = PT.esc(fileName);
      String script = "" +
            "try{\n"
          + "  var atoms0 = {*}\n"
          + "  var res0 = " + BS.escape(bsRes0,'(',')')  + "\n"
          + "  set appendNew false\n"
          + "  load append "+fileName+"\n"
          + "  set appendNew " + b + "\n"
          + "  var res1 = {!atoms0};var r1 = res1[1];var r0 = res1[0]\n"
          + "  if ({r1 & within(group, r0)}){\n" 
          + "    var haveHs = ({_H & connected(res0)} != 0)\n"
          + "    if (!haveHs) {delete _H & res1}\n"
          + "    var sm = '[*.N][*.CA][*.C][*.O]'\n"
          + "    var keyatoms = res1.find(sm)\n"
          + "    var x = compare(res1,res0,sm,'BONDS')\n"
          + "    if(x){\n"
          + "      print 'mutating ' + res0[1].label('%n%r') + ' to ' + "+fileName+".trim('=')\n"
          + "      rotate branch @x\n"
          + "      compare res1 res0 SMARTS @sm rotate translate 0\n"
          + "      var c = {!res0 & connected(res0)}\n"
          + "      var N2 = {*.N & c}\n"
          + "      var C0 = {*.C & c}\n"
          + "      var angleH = ({*.H and res0} ? angle({*.C and res0},{*.CA and res0},{*.N and res0},{*.H and res0}) : 1000)\n"
          + "      delete res0\n"
          + "      if (N2) {\n"
          + "        delete (*.OXT,*.HXT) and res1\n"
          + "        connect {N2} {keyatoms & *.C}\n"
          + "      }\n"
          + "      if (C0) {\n" // not terminal
          + "        if ({res1 and _H & connected(*.N)} == 1) {\n" // proline or proline-like
          + "          delete *.H and res1\n"
          + "        } else {\n"
          + "          var x = angle({*.C and res1},{*.CA and res1},{*.N and res1},{*.H and res1})\n"
          + "          rotate branch {*.CA and res1} {*.N and res1} @{angleH-x}\n"
          + "          delete *.H2 and res1\n"
          + "          delete *.H3 and res1\n"
          + "        }\n"
          + "        connect {C0} {keyatoms & *.N}\n"
          + "      }\n"
          + "    }\n"
          + "  }\n"
          + "}catch(e){print e}\n";
      try {
        if (Logger.debugging)
          Logger.debug(script);
        vwr.eval.runScript(script);
      } catch (Exception e) {
        // TODO
      }
      if (ms.ac == ac)
        return false;
      // check for protein monomer
      g = ms.at[ms.ac - 1].group;
      if (g != ms.at[ac + 1].group || !(g instanceof AminoMonomer)) {
        BS bsAtoms = new BS();
        g.setAtomBits(bsAtoms);
        vwr.deleteAtoms(bsAtoms, false);
        return  false;
      }
      AminoMonomer res1 = (AminoMonomer) g;

      // fix h position as same as previous group
      getBackbone(res1, backbone);
      // must get new group into old chain
      
      // note that the terminal N if replacing the N-terminus will only have two H atoms
      
      Group[] groups = res0.chain.groups;
      for (int i = groups.length; --i >= 0;)
        if (groups[i] == res0) {
          groups[i] = res1;
          break;
        }
      res1.setResno(r);
      res1.chain.groupCount = 0;
      res1.chain = res0.chain;
      BS bsExclude = BSUtil.newBitSet2(0, ms.mc);
      bsExclude.clear(res1.chain.model.modelIndex);
      res1.chain.model.groupCount = -1;
      res1.chain.model.freeze();
      ms.recalculatePolymers(bsExclude);      
    //} catch (Exception e) {
     // System.out.println("" + e);
   // }
    vwr.setBooleanProperty("appendNew", b);
    vwr.fm.setFileInfo(info);
    return true;

  }


/////////////////////////////////////////////////////////////////////////  
  

  /**
   * @param res1
   * @param backbone
   * @return [C O CA N H]
   */
  private Atom[] getBackbone(AminoMonomer res1, Atom[] backbone) {
    Atom[] b = new Atom[] {res1.getCarbonylCarbonAtom(), res1.getCarbonylOxygenAtom(), 
        res1.getLeadAtom(), res1.getNitrogenAtom(), res1.getExplicitNH() };
    if (backbone == null) {
      // don't place H if there is more than one covalent H on res0.N
      if (b[3].getCovalentHydrogenCount() > 1)
        b[4] = null;
    } else {
      for (int i = 0; i < 5; i++) {
        Atom a0 = backbone[i];
        Atom a1 = b[i];
        if (a0 != null && a1 != null)
          a1.setT(a0);
      }
    }
    return b;
      


    // TODO
    
  }

  BioModel(ModelSet modelSet, int modelIndex, int trajectoryBaseIndex, 
      String jmolData, Properties properties, Map<String, Object> auxiliaryInfo) {
    vwr = modelSet.vwr;
    set(modelSet, modelIndex, trajectoryBaseIndex, jmolData, properties, auxiliaryInfo);
    isBioModel = true;
    modelSet.bioModelset = this;
    clearBioPolymers();
  }

  private void clearBioPolymers() {
    bioPolymers = new BioPolymer[8];
    bioPolymerCount = 0;
  }

  @Override
  public int getBioPolymerCount() {
    return bioPolymerCount;
  }

  @Override
  public void fixIndices(int modelIndex, int nAtomsDeleted, BS bsDeleted) {
    fixIndicesM(modelIndex, nAtomsDeleted, bsDeleted);
    recalculateLeadMidpointsAndWingVectors();
  }

  private void recalculateLeadMidpointsAndWingVectors() {
    for (int ip = 0; ip < bioPolymerCount; ip++)
      bioPolymers[ip].recalculateLeadMidpointsAndWingVectors();
  }

  
  @Override
  public boolean freeze() {
    freezeM();
    bioPolymers = (BioPolymer[])AU.arrayCopyObject(bioPolymers, bioPolymerCount);
    return true;
  }
  
  public void addSecondaryStructure(STR type, String structureID,
                                    int serialID, int strandCount,
                                    int startChainID, int startSeqcode,
                                    int endChainID, int endSeqcode, int istart,
                                    int iend, BS bsAssigned) {
    for (int i = bioPolymerCount; --i >= 0;)
      if (bioPolymers[i] instanceof AlphaPolymer)
        ((AlphaPolymer) bioPolymers[i]).addStructure(type, structureID,
            serialID, strandCount, startChainID, startSeqcode, endChainID,
            endSeqcode, istart, iend, bsAssigned);
  }

  private String calculateStructures(boolean asDSSP, boolean doReport,
                                    boolean dsspIgnoreHydrogen,
                                    boolean setStructure, boolean includeAlpha) {
    if (bioPolymerCount == 0 || !setStructure && !asDSSP)
      return "";
    ms.proteinStructureTainted = structureTainted = true;
    if (setStructure)
      for (int i = bioPolymerCount; --i >= 0;)
        if (!asDSSP || bioPolymers[i].monomers[0].getNitrogenAtom() != null)
          bioPolymers[i].clearStructures();
    if (!asDSSP || includeAlpha)
      for (int i = bioPolymerCount; --i >= 0;)
        if (bioPolymers[i] instanceof AlphaPolymer)
          ((AlphaPolymer) bioPolymers[i]).calculateStructures(includeAlpha);
    return (asDSSP ? calculateDssx(null, doReport, dsspIgnoreHydrogen, setStructure) : "");
  }
  
  private String calculateDssx(Lst<Bond> vHBonds, boolean doReport,
                               boolean dsspIgnoreHydrogen, boolean setStructure) {
    boolean haveProt = false;
    boolean haveNucl = false;
    for (int i = 0; i < bioPolymerCount && !(haveProt && haveNucl); i++) {
      if (bioPolymers[i].isNucleic())
        haveNucl = true;
      else if (bioPolymers[i] instanceof AminoPolymer)
        haveProt = true;
    }
    String s = "";
    if (haveProt)
      s += ((DSSPInterface) Interface.getOption("dssx.DSSP", vwr, "ms"))
        .calculateDssp(bioPolymers, bioPolymerCount, vHBonds, doReport,
            dsspIgnoreHydrogen, setStructure);
    if (haveNucl && auxiliaryInfo.containsKey("dssr") && vHBonds != null)
      s += vwr.getAnnotationParser().getHBonds(ms, modelIndex, vHBonds, doReport);
    return s;
  }

  @Override
  public void getConformations(int modelIndex, int conformationIndex,
                              boolean doSet, BS bsAtoms, BS bsRet) {
    for (int i = ms.mc; --i >= 0;)
      if (i == modelIndex || modelIndex < 0)
        if (ms.am[i].isBioModel)
        ((BioModel) ms.am[i]).getConformation(conformationIndex, doSet, bsAtoms, bsRet);
  }

  /**
   * @param conformationIndex
   * @param doSet 
   * @param bsRet
   * @param bsSelected
   */
  public void getConformation(int conformationIndex, boolean doSet, BS bsSelected, BS bsRet) {
    String altLocs = ms.getAltLocListInModel(modelIndex);
    int nAltLocs = ms.getAltLocCountInModel(modelIndex);
    if (conformationIndex > 0 && conformationIndex >= nAltLocs)
      return;
    BS bsConformation = vwr.getModelUndeletedAtomsBitSet(modelIndex);
    if (bsSelected != null)
      bsConformation.and(bsSelected);
    if (bsConformation.nextSetBit(0) < 0)
      return;
    if (conformationIndex >= 0) {
      if (nAltLocs > 0)
        for (int i = bioPolymerCount; --i >= 0;)
          bioPolymers[i].getConformation(bsConformation, conformationIndex);
      BS bs = new BS();
      for (int c = nAltLocs; --c >= 0;)
        if (c != conformationIndex)
          bsConformation.andNot(ms.getAtomBitsMDa(T.spec_alternate,
              altLocs.substring(c, c + 1), bs));
    }
    if (bsConformation.nextSetBit(0) >= 0) {
      bsRet.or(bsConformation);      
      if (doSet)
        for (int j = bioPolymerCount; --j >= 0;)
          bioPolymers[j].setConformation(bsConformation);
    }
  }

  private void addBioPolymer(BioPolymer polymer) {
    if (bioPolymers.length == 0)
      clearBioPolymers();
    if (bioPolymerCount == bioPolymers.length)
      bioPolymers = (BioPolymer[])AU.doubleLength(bioPolymers);
    polymer.bioPolymerIndexInModel = bioPolymerCount;
    bioPolymers[bioPolymerCount++] = polymer;
  }

  @Override
  public Lst<BS> getBioBranches(Lst<BS> biobranches) {
    // scan through biopolymers quickly -- 
    BS bsBranch;
    for (int j = 0; j < bioPolymerCount; j++) {
      bsBranch = new BS();
      bioPolymers[j].getRange(bsBranch, isMutated);
      int iAtom = bsBranch.nextSetBit(0);
      if (iAtom >= 0) {
        if (biobranches == null)
          biobranches = new  Lst<BS>();
        biobranches.addLast(bsBranch);
      }
    }
    return biobranches;
  }

  @Override
  public void getPolymerInfo(
                                BS bs,
                                Map<String, Lst<Map<String, Object>>> finalInfo,
                                Lst<Map<String, Object>> modelVector) {
    Map<String, Object> modelInfo = new Hashtable<String, Object>();
    Lst<Map<String, Object>> info = new  Lst<Map<String, Object>>();
    for (int ip = 0; ip < bioPolymerCount; ip++) {
      Map<String, Object> polyInfo = bioPolymers[ip].getPolymerInfo(bs); 
      if (!polyInfo.isEmpty())
        info.addLast(polyInfo);
    }
    if (info.size() > 0) {
      modelInfo.put("modelIndex", Integer.valueOf(modelIndex));
      modelInfo.put("polymers", info);
      modelVector.addLast(modelInfo);
    }
  }
  
  private final static String[] pdbRecords = { "ATOM  ", "MODEL ", "HETATM" };

  @Override
  public String getFullPDBHeader() {
    if (modelIndex < 0)
      return "";
    String info = (String) auxiliaryInfo.get("fileHeader");
    if (info != null)
      return info;
    info = vwr.getCurrentFileAsString("biomodel");
    int ichMin = info.length();
    for (int i = pdbRecords.length; --i >= 0;) {
      int ichFound;
      String strRecord = pdbRecords[i];
      switch (ichFound = (info.startsWith(strRecord) ? 0 : info.indexOf("\n"
          + strRecord))) {
      case -1:
        continue;
      case 0:
        auxiliaryInfo.put("fileHeader", "");
        return "";
      default:
        if (ichFound < ichMin)
          ichMin = ++ichFound;
      }
    }
    info = info.substring(0, ichMin);
    auxiliaryInfo.put("fileHeader", info);
    return info;
  }

  @Override
  public void getPdbData(String type, char ctype, boolean isDraw,
                         BS bsSelected, OC out,
                         LabelToken[] tokens, SB pdbCONECT, BS bsWritten) {
    boolean bothEnds = false;
    char qtype = (ctype != 'R' ? 'r' : type.length() > 13
        && type.indexOf("ramachandran ") >= 0 ? type.charAt(13) : 'R');
    if (qtype == 'r')
      qtype = vwr.getQuaternionFrame();
    int mStep = vwr.getInt(T.helixstep);
    int derivType = (type.indexOf("diff") < 0 ? 0 : type.indexOf("2") < 0 ? 1
        : 2);
    if (!isDraw) {
      out.append("REMARK   6 Jmol PDB-encoded data: " + type + ";");
      if (ctype != 'R') {
        out.append("  quaternionFrame = \"" + qtype + "\"");
        bothEnds = true; //???
      }
      out.append("\nREMARK   6 Jmol Version ").append(Viewer.getJmolVersion())
          .append("\n");
      if (ctype == 'R')
        out
            .append("REMARK   6 Jmol data min = {-180 -180 -180} max = {180 180 180} "
                + "unScaledXyz = xyz * {1 1 1} + {0 0 0} plotScale = {100 100 100}\n");
      else
        out
            .append("REMARK   6 Jmol data min = {-1 -1 -1} max = {1 1 1} "
                + "unScaledXyz = xyz * {0.1 0.1 0.1} + {0 0 0} plotScale = {100 100 100}\n");
    }
    
    P3 ptTemp = new P3();
    for (int p = 0; p < bioPolymerCount; p++)
      bioPolymers[p].getPdbData(vwr, ctype, qtype, mStep, derivType,
          bsAtoms, bsSelected, bothEnds, isDraw, p == 0, tokens, out, 
          pdbCONECT, bsWritten, ptTemp);
  }
  
  /**
   * from ModelSet.setAtomPositions
   * 
   * base models only; not trajectories
   */
  @Override
  public void resetRasmolBonds(BS bs) {
    BS bsDelete = new BS();
    hasRasmolHBonds = false;
    Model[] am = ms.am;
    Bond[] bo = ms.bo;
    for (int i = ms.bondCount; --i >= 0;) {
      Bond bond = bo[i];
      // trajectory atom .mi will be pointing to the trajectory;
      // here we check to see if their base model is this model
      if ((bond.order & Edge.BOND_H_CALC_MASK) != 0
          && am[bond.atom1.mi].trajectoryBaseIndex == modelIndex)
        bsDelete.set(i);
    }
    if (bsDelete.nextSetBit(0) >= 0)
      ms.deleteBonds(bsDelete, false);
    getRasmolHydrogenBonds(bs, bs, null, false, Integer.MAX_VALUE, false, null);
  }

  @Override
  public void getDefaultLargePDBRendering(SB sb, int maxAtoms) {
    BS bs = new BS();
    if (getBondCount() == 0)
      bs = bsAtoms;
    // all biopolymer atoms...
    if (bs != bsAtoms)
      for (int i = 0; i < bioPolymerCount; i++)
        bioPolymers[i].getRange(bs, isMutated);
    if (bs.nextSetBit(0) < 0)
      return;
    // ...and not connected to backbone:
    BS bs2 = new BS();
    if (bs == bsAtoms) {
      bs2 = bs;
    } else {
      for (int i = 0; i < bioPolymerCount; i++)
        if (bioPolymers[i].getType() == BioPolymer.TYPE_NOBONDING)
          bioPolymers[i].getRange(bs2, isMutated);
    }
    if (bs2.nextSetBit(0) >= 0)
      sb.append("select ").append(Escape.eBS(bs2)).append(";backbone only;");
    if (act <= maxAtoms)
      return;
    // ...and it's a large model, to wireframe:
      sb.append("select ").append(Escape.eBS(bs)).append(" & connected; wireframe only;");
    // ... and all non-biopolymer and not connected to stars...
    if (bs != bsAtoms) {
      bs2.clearAll();
      bs2.or(bsAtoms);
      bs2.andNot(bs);
      if (bs2.nextSetBit(0) >= 0)
        sb.append("select " + Escape.eBS(bs2) + " & !connected;stars 0.5;spacefill off;");
    }
  }
  
  @Override
  public BS getAtomBitsStr(int tokType, String specInfo, BS bs) {
    switch (tokType) {
    default:
      return new BS();
    case T.domains:
      return getAnnotationBits("domains", T.domains, specInfo);
    case T.validation:
      return getAnnotationBits("validation", T.validation, specInfo);
      //    case T.annotations:
      //      TODO -- generalize this
    case T.dssr:
      return getAnnotationBits("dssr", T.dssr, specInfo);
    case T.rna3d:
      return getAnnotationBits("rna3d", T.rna3d, specInfo);
    case T.basepair:
      String s = specInfo;
      bs = new BS();
      return (s.length() % 2 != 0 ? bs 
          : ms.getAtomBitsMDa(T.group, getAllBasePairBits(s), bs));
    case T.sequence:
      return getAllSequenceBits(specInfo, null);
    }
  }

  @Override
  public BS getAtomBitsBS(int tokType, BS bsInfo, BS bs) {

    // this first set does not assume sequential order in the file

    Atom[] at = ms.at;
    int ac = ms.ac;
    int i = 0;
    switch (tokType) {
    case T.carbohydrate:
      for (i = ac; --i >= 0;)
        if (at[i].group.isCarbohydrate())
          bs.set(i);
      break;
    case T.dna:
      for (i = ac; --i >= 0;)
        if (at[i].isDna())
          bs.set(i);
      break;
    case T.helix: // WITHIN -- not ends
    case T.sheet: // WITHIN -- not ends
      STR type = (tokType == T.helix ? STR.HELIX
          : STR.SHEET);
      for (i = ac; --i >= 0;)
        if (at[i].group.isWithinStructure(type))
          bs.set(i);
      break;
    case T.nucleic:
      for (i = ac; --i >= 0;)
        if (at[i].isNucleic())
          bs.set(i);
      break;
    case T.protein:
      for (i = ac; --i >= 0;)
        if (at[i].isProtein())
          bs.set(i);
      break;
    case T.purine:
      for (i = ac; --i >= 0;)
        if (at[i].isPurine())
          bs.set(i);
      break;
    case T.pyrimidine:
      for (i = ac; --i >= 0;)
        if (at[i].isPyrimidine())
          bs.set(i);
      break;
    case T.rna:
      for (i = ac; --i >= 0;)
        if (at[i].isRna())
          bs.set(i);
      break;
    }
    if (i < 0)
      return bs;

    // these next assume sequential position in the file
    // speeding delivery -- Jmol 11.9.24

    // TODO WHAT ABOUT MUTATED?

    int i0 = bsInfo.nextSetBit(0);
    if (i0 < 0)
      return bs;    
    i = 0;
    switch (tokType) {
    case T.polymer:
      // within(polymer,...)
      for (i = i0; i >= 0; i = bsInfo.nextSetBit(i+1)) {
        if (bs.get(i))
          continue;
        int iPolymer = at[i].group.getBioPolymerIndexInModel();
        bs.set(i);
        for (int j = i; --j >= 0;)
          if (at[j].group.getBioPolymerIndexInModel() == iPolymer)
            bs.set(j);
          else
            break;
        for (; ++i < ac;)
          if (at[i].group.getBioPolymerIndexInModel() == iPolymer)
            bs.set(i);
          else
            break;
      }
      break;
    case T.structure:
      // within(structure,...)
      for (i = i0; i >= 0; i = bsInfo.nextSetBit(i+1)) {
        if (bs.get(i))
          continue;
        Object structure = at[i].group.getStructure();
        bs.set(i);
        for (int j = i; --j >= 0;)
          if (at[j].group.getStructure() == structure)
            bs.set(j);
          else
            break;
        for (; ++i < ac;)
          if (at[i].group.getStructure() == structure)
            bs.set(i);
          else
            break;
      }
      break;
    }
    if (i == 0)
      Logger.error("MISSING getAtomBits entry for " + T.nameOf(tokType));
    return bs;
  }

  private BS getAnnotationBits(String name, int tok, String specInfo) {
    BS bs = new BS();
    JmolAnnotationParser pa = vwr.getAnnotationParser();
    Object ann;
    for (int i = ms.mc; --i >= 0;)
      if ((ann = ms.getInfo(i, name)) != null)
        bs.or(pa.getAtomBits(vwr, specInfo,
            ((BioModel) ms.am[i]).getCachedAnnotationMap(name + " V ", ann), ms.am[i].dssrCache, tok,
            i, ms.am[i].bsAtoms));
    return bs;
  }

  Object getCachedAnnotationMap(String key, Object ann) {
    Map<String, Object> cache = (dssrCache == null && ann != null ? dssrCache = new Hashtable<String, Object>()
        : dssrCache);
    if (cache == null)
      return null;
    Object annotv = cache.get(key);
    if (annotv == null && ann != null) {
      annotv = (ann instanceof SV || ann instanceof Hashtable ? ann
              : vwr.evaluateExpressionAsVariable(ann));
      cache.put(key, annotv);
    }
    return (annotv instanceof SV || annotv instanceof Hashtable ? annotv : null);
  }

  @Override
  public BS getIdentifierOrNull(String identifier) {
    int len = identifier.length();
    int pt = 0;
    while (pt < len && PT.isLetter(identifier.charAt(pt)))
      ++pt;
    BS bs = ms.getSpecNameOrNull(identifier.substring(0, pt), false);
    if (pt == len)
      return bs;
    if (bs == null)
      bs = new BS();
    //
    // look for a sequence number or sequence number ^ insertion code
    //
    int pt0 = pt;
    while (pt < len && PT.isDigit(identifier.charAt(pt)))
      ++pt;
    int seqNumber = 0;
    try {
      seqNumber = Integer.parseInt(identifier.substring(pt0, pt));
    } catch (NumberFormatException nfe) {
      return null;
    }
    char insertionCode = ' ';
    if (pt < len && identifier.charAt(pt) == '^')
      if (++pt < len)
        insertionCode = identifier.charAt(pt);
    int seqcode = Group.getSeqcodeFor(seqNumber, insertionCode);
    BS bsInsert = ms.getSeqcodeBits(seqcode, false);
    if (bsInsert == null) {
      if (insertionCode != ' ')
        bsInsert = ms.getSeqcodeBits(Character.toUpperCase(identifier.charAt(pt)),
            false);
      if (bsInsert == null)
        return null;
      pt++;
    }
    bs.and(bsInsert);
    if (pt >= len)
      return bs;
    if(pt != len - 1)
      return null;
    // ALA32B  (no colon; not ALA32:B)
    // old school; not supported for multi-character chains
    bs.and(ms.getChainBits(identifier.charAt(pt)));
    return bs;
  }

}
