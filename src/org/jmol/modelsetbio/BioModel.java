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
  
  int bioPolymerCount = 0;
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
          m.defaultStructure = getFullProteinStructureState(m.bsAtoms, T.state);
      }
  }

  @Override
  public void setAllStructureList(Map<STR, float[]> structureList) {
    for (int iModel = ms.mc; --iModel >= 0;)
      if (ms.am[iModel].isBioModel) {
        BioModel m = (BioModel) ms.am[iModel];
        m.bioPolymers = (BioPolymer[]) AU.arrayCopyObject(m.bioPolymers,
            m.bioPolymerCount);
        for (int i = m.bioPolymerCount; --i >= 0;) {
          BioPolymer bp = 
          m.bioPolymers[i];
          if (bp instanceof AminoPolymer)
            ((AminoPolymer) bp).setStructureList(structureList);
        }
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
    return getBioExt().calculateAllstruts(vwr, ms, bs1, bs2);
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

  @Override
  public String getFullProteinStructureState(BS bsAtoms, int mode) {
    boolean taintedOnly = (mode == T.all);
    if (taintedOnly && !ms.proteinStructureTainted)
      return "";
    boolean scriptMode = (mode == T.state || mode == T.all);
    Atom[] atoms = ms.at;
    int at0 = (bsAtoms == null ? 0 : bsAtoms.nextSetBit(0));
    if (at0 < 0)
      return "";
    if (bsAtoms != null && mode == T.ramachandran) {
      bsAtoms = BSUtil.copy(bsAtoms);
      for (int i = ms.ac; --i >= 0;)
        if (Float.isNaN(atoms[i].group.getGroupParameter(T.phi))
            || Float.isNaN(atoms[i].group.getGroupParameter(T.psi)))
          bsAtoms.clear(i);
    }
    int at1 = (bsAtoms == null ? ms.ac : bsAtoms.length()) - 1;
    int im0 = atoms[at0].mi;
    int im1 = atoms[at1].mi;
    Lst<ProteinStructure> lstStr = new Lst<ProteinStructure>();
    Map<ProteinStructure, Boolean> map = new Hashtable<ProteinStructure, Boolean>();
    SB cmd = new SB();
    for (int im = im0; im <= im1; im++) {
      if (!ms.am[im].isBioModel)
        continue;
      BioModel m = (BioModel) ms.am[im];
      if (taintedOnly && !m.structureTainted)
        continue;
      BS bsA = new BS();
      bsA.or(m.bsAtoms);
      bsA.andNot(m.bsAtomsDeleted);
      int i0 = bsA.nextSetBit(0);
      if (i0 < 0)
        continue;
      if (scriptMode) {
        cmd.append("  structure none ")
            .append(
                Escape.eBS(ms.getModelAtomBitSetIncludingDeleted(im, false)))
            .append("    \t# model=" + ms.getModelNumberDotted(im))
            .append(";\n");
      }
      ProteinStructure ps;
      for (int i = i0; i >= 0; i = bsA.nextSetBit(i + 1)) {
        Atom a = atoms[i];
        if (!(a.group instanceof AlphaMonomer)
            || (ps = ((AlphaMonomer) a.group).proteinStructure) == null
            || map.containsKey(ps))
          continue;
        lstStr.addLast(ps);
        map.put(ps, Boolean.TRUE);
      }
    }
    getStructureLines(bsAtoms, cmd, lstStr, STR.HELIX, scriptMode, mode);
    getStructureLines(bsAtoms, cmd, lstStr, STR.SHEET, scriptMode, mode);
    getStructureLines(bsAtoms, cmd, lstStr, STR.TURN, scriptMode, mode);
    return cmd.toString();
  }

  @SuppressWarnings("incomplete-switch")
  private int getStructureLines(BS bsAtoms, SB cmd, Lst<ProteinStructure> lstStr, STR type,
                                boolean scriptMode, int mode) {
    //boolean pdbFileMode = (mode == T.pdb || mode == T.ramachandran);
    boolean showMode = (mode == T.show);
    int nHelix = 0, nSheet = 0, nTurn = 0;
    String sid = null;
    BS bs = new BS();
    int n = 0;
    for (int i = 0, ns = lstStr.size(); i < ns; i++) {
      ProteinStructure ps = lstStr.get(i);
      if (ps.type != type)
        continue;
      bs.clearAll();
      // could be a subset of atoms, not just the ends
      Monomer m1 = ps.findMonomer(bsAtoms, true);
      Monomer m2 = ps.findMonomer(bsAtoms, false);
      if (m1 == null || m2 == null)
        continue;
      int iModel = ps.apolymer.model.modelIndex;
      String comment = (scriptMode ? "    \t# model="
          + ms.getModelNumberDotted(iModel) : null);
      int res1 = m1.getResno();
      int res2 = m2.getResno();
      STR subtype = ps.subtype;
      switch (type) {
      case HELIX:
      case TURN:
      case SHEET:
        n++;
        if (scriptMode) {
          String stype = subtype.getBioStructureTypeName(false);
          cmd.append("  structure ").append(stype).append(" ")
              .append(Escape.eBS(ps.getAtoms(bs))).append(comment)
              .append(" & (" + res1 + " - " + res2 + ")").append(";\n");
        } else {
          String str;
          int nx;
          // NNN III GGG C RRRR GGG C RRRR
          // HELIX 99 99 LYS F 281 LEU F 293 1
          // NNN III 2 GGG CRRRR GGG CRRRR
          // SHEET 1 A 8 ILE A 43 ASP A 45 0
          // NNN III GGG CRRRR GGG CRRRR
          // TURN 1 T1 PRO A 41 TYR A 44
          switch (type) {
          case HELIX:
            nx = ++nHelix;
            sid = PT.formatStringI("%3N %3N", "N", nx);
            str = "HELIX  %ID %3GROUPA %1CA %4RESA  %3GROUPB %1CB %4RESB";
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
            sid = PT.formatStringI("%3N %3A 0", "N", nx);
            sid = PT.formatStringS(sid, "A", "S" + nx);
            str = "SHEET  %ID %3GROUPA %1CA%4RESA  %3GROUPB %1CB%4RESB";
            break;
          case TURN:
          default:
            nx = ++nTurn;
            sid = PT.formatStringI("%3N %3N", "N", nx);
            str = "TURN   %ID %3GROUPA %1CA%4RESA  %3GROUPB %1CB%4RESB";
            break;
          }
          str = PT.formatStringS(str, "ID", sid);
          str = PT.formatStringS(str, "GROUPA", m1.getGroup3());
          str = PT.formatStringS(str, "CA", m1.getLeadAtom().getChainIDStr());
          str = PT.formatStringI(str, "RESA", res1);
          str = PT.formatStringS(str, "GROUPB", m2.getGroup3());
          str = PT.formatStringS(str, "CB", m2.getLeadAtom().getChainIDStr());
          str = PT.formatStringI(str, "RESB", res2);
          cmd.append(str);
          if (showMode)
            cmd.append(" strucno= ").appendI(ps.strucNo);
          cmd.append("\n");

          /*
           * HELIX 1 1 ILE 7 PRO 19 1 3/10 CONFORMATION RES 17,19 1CRN 55
           * HELIX 2 2 GLU 23 THR 30 1 DISTORTED 3/10 AT RES 30 1CRN 56
           * SHEET 1 S1 2 THR 1 CYS 4 0 1CRNA 4 SHEET 2 S1 2 CYS 32 ILE 35
           */
        }
      }
    }
    if (n > 0)
      cmd.append("\n");
    return n;
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
        if (bsA.nextSetBit(0) < 0)
          continue;
        bsB = ms.getSequenceBits(specInfo.substring(i, ++i), null);
        if (bsB.nextSetBit(0) < 0)
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
    getBioExt().calculateStraightnessAll(vwr, ms);
  }

  @Override
  public boolean mutate(BS bs, String group, String[] sequence) {
    return getBioExt().mutate(vwr, bs, group, sequence);
  }

  /////////////////////////////////////////////////////////////////////////  

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

  /**
   * @param conformationIndex
   * @param doSet 
   * @param bsRet
   * @param bsSelected
   */
  public void getConformation(int conformationIndex, boolean doSet, BS bsSelected, BS bsRet) {
    int nAltLocs = altLocCount;
    BS bsConformation = getConformationBS(conformationIndex, bsSelected);
    if (bsConformation == null)
      return;
    if (conformationIndex >= 0) {
      if (nAltLocs > 0)
        for (int i = bioPolymerCount; --i >= 0;)
          bioPolymers[i].getConformation(bsConformation, conformationIndex);
      BS bs = new BS();
      String altLocs = ms.getAltLocListInModel(modelIndex);
      for (int c = altLocCount; --c >= 0;)
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
  public void getAllPolymerInfo(BS bs, Map<String, Lst<Map<String, Object>>> info) {
    getBioExt().getAllPolymerInfo(ms, bs, info);
  }

  private BioExt bx;
  private BioExt getBioExt() {
    return (bx == null ? (bx = ((BioExt) Interface.getInterface("org.jmol.modelsetbio.BioExt", vwr, "script"))) : bx);
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
    getBioExt().getPdbDataM(this, vwr, type, ctype, isDraw, bsSelected, out, tokens, pdbCONECT, bsWritten);
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
