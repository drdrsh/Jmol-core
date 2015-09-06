/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2015-08-30 01:18:16 -0500 (Sun, 30 Aug 2015) $
 * $Revision: 20742 $
 *
 * Copyright (C) 2005  The Jmol Development Team
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

package org.jmol.smiles;

import java.util.Arrays;

import javajs.util.AU;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.T3;
import javajs.util.V3;

import org.jmol.util.Edge;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Node;

//import org.jmol.util.Logger;

/**
 * This class relates to stereochemical issues.
 */
public class SmilesStereo {

  int chiralClass = Integer.MIN_VALUE;
  int chiralOrder = Integer.MIN_VALUE;
  int atomCount;
  private String details;
  private SmilesSearch search;
  private Node[] jmolAtoms;
  private String directives;
  final static int STEREOCHEMISTRY_SQUARE_PLANAR = 8;
  final static int STEREOCHEMISTRY_OCTAHEDRAL = 6;
  final static int STEREOCHEMISTRY_TRIGONAL_BIPYRAMIDAL = 5;
  final static int STEREOCHEMISTRY_TETRAHEDRAL = 4;
  final static int STEREOCHEMISTRY_TRIGONAL_PYRAMIDAL = 3;
  final static int STEREOCHEMISTRY_ALLENE = 2;
  final static int STEREOCHEMISTRY_POLYHEDRAL = 1; // Jmol idea
  final static int STEREOCHEMISTRY_DEFAULT = 0;

  private static int getChiralityClass(String xx) {
    return ("0;PH;AL;33;TH;TP;OH;77;SP;".indexOf(xx) + 1) / 3;
  }

  public static SmilesStereo newStereo(SmilesStereo stereo)
      throws InvalidSmilesException {
    return (stereo == null ? new SmilesStereo(0, 0, 0, null, null)
        : new SmilesStereo(stereo.chiralClass, stereo.chiralOrder,
            stereo.atomCount, stereo.details, stereo.directives));
  }

  SmilesStereo(int chiralClass, int chiralOrder, int atomCount, String details,
      String directives) throws InvalidSmilesException {
    this.chiralClass = chiralClass;
    this.chiralOrder = chiralOrder;
    this.atomCount = atomCount;
    this.details = details;
    this.directives = directives;
    if (chiralClass == STEREOCHEMISTRY_POLYHEDRAL)
      getPolyhedralOrders();
  }

  private int[][] polyhedralOrders;
  private boolean isNot;
  private PolyhedronStereoSorter sorter;

  private void getPolyhedralOrders() throws InvalidSmilesException {
    int[][] po = polyhedralOrders = AU.newInt2(atomCount);
    if (details == null)
      return;
    int[] temp = new int[details.length()];
    int[] ret = new int[1];
    String msg = null;
    int pt = 0;
    String s = details + "/";
    int n = 0;
    int len = s.length();
    int index = 0;
    int atomPt = 0;
    do {
      char ch = s.charAt(index);
      switch (ch) {
      case '!':
        isNot = true;
        index++;
        break;
      case '/':
      case '.':
        if ((pt = atomPt) >= atomCount) {
          msg = "Too many descriptors";
          break;
        }
        int[] a = po[atomPt] = new int[n];
        for (; --n >= 0;)
          a[n] = temp[n];
        n = 0;
        if (Logger.debugging)
          Logger.info(PT.toJSON("@PH" + atomCount + "[" + atomPt + "]", a));
        if (ch == '/')
          index = Integer.MAX_VALUE;
        else
          index++;
        atomPt++;
        break;
      default:
        index = SmilesParser.getRingNumber(s, index, ch, ret);
        pt = temp[n++] = ret[0] - 1;
        if (pt == atomPt)
          msg = "Atom cannot connect to itself";
        else if (pt < 0 || pt >= atomCount)
          msg = "Connection number outside of range (1-" + atomCount + ")";
        else if (n >= atomCount)
          msg = "Too many connections indicated";
      }
      if (msg != null) {
        msg += ": " + s.substring(0, index) + "<<";
        throw new InvalidSmilesException(msg);
      }
    } while (index < len);
  }

  /**
   * Returns the chiral class of the atom. (see <code>CHIRALITY_...</code>
   * constants)
   * 
   * @return Chiral class.
   */
  public int getChiralClass() {
    return chiralClass;
  }

  /**
   * Sets the chiral class of the atom. (see <code>CHIRALITY_...</code>
   * constants)
   * 
   * @param chiralClass
   *        Chiral class.
   */
  public void setChiralClass(int chiralClass) {
    this.chiralClass = chiralClass;
  }

  /**
   * Returns the chiral order of the atom.
   * 
   * @return Chiral order.
   */
  public int getChiralOrder() {
    return chiralOrder;
  }

  /**
   * Sets the chiral order of the atom.
   * 
   * @param chiralOrder
   *        Chiral order.
   */
  public void setChiralOrder(int chiralOrder) {
    this.chiralOrder = chiralOrder;
  }

  public void fixStereo(SmilesAtom sAtom) throws InvalidSmilesException {
    int nBonds = Math.max(sAtom.missingHydrogenCount, 0) + sAtom.getBondCount();
    switch (chiralClass) {
    case STEREOCHEMISTRY_DEFAULT:
      switch (nBonds) {
      case 2:
        chiralClass = STEREOCHEMISTRY_ALLENE;
        break;
      case 3:
        chiralClass = STEREOCHEMISTRY_TRIGONAL_PYRAMIDAL;
        break;
      case 4:
      case 5:
      case 6:
        chiralClass = nBonds;
        break;
      }
      break;
    case STEREOCHEMISTRY_SQUARE_PLANAR:
      if (nBonds != 4)
        sAtom.stereo = null;
      break;
    case STEREOCHEMISTRY_POLYHEDRAL:
      if (nBonds != atomCount)
        sAtom.stereo = null;
      break;
    case STEREOCHEMISTRY_ALLENE:
    case STEREOCHEMISTRY_OCTAHEDRAL:
    case STEREOCHEMISTRY_TETRAHEDRAL:
    case STEREOCHEMISTRY_TRIGONAL_BIPYRAMIDAL:
      if (nBonds != chiralClass)
        sAtom.stereo = null;
      break;
    }
    if (sAtom.stereo == null)
      throw new InvalidSmilesException(
          "Incorrect number of bonds for stereochemistry descriptor");
  }

  private boolean setSmilesCoordinates(SmilesAtom atom, SmilesAtom sAtom,
                                       SmilesAtom sAtom2, Node[] cAtoms) {

    // When testing equality of two SMILES strings in terms of stereochemistry,
    // we need to set the atom positions based on the ORIGINAL SMILES order,
    // which, except for the H atom, will be the same as the "matchedAtom"
    // index.
    // all the necessary information is passed via the atomSite field of Atom

    // atomSite is used by smilesMatch.find to encode chiralClass and chiralOrder 
    if (atom.stereo == null)
      return false;
    int chiralClass = atom.stereo.chiralClass;
    int chiralOrder = atom.stereo.chiralOrder;
    Node a2 = (chiralClass == STEREOCHEMISTRY_ALLENE || chiralClass == 3 ? a2 = jmolAtoms[sAtom2
        .getMatchingAtomIndex()] : null);

    // set the chirality center at the origin
    atom.set(0, 0, 0);
    atom = (SmilesAtom) jmolAtoms[sAtom.getMatchingAtomIndex()];
    atom.set(0, 0, 0);

    int[] map = search.getMappedAtoms(atom, a2, cAtoms);
    switch (chiralClass) {
    case STEREOCHEMISTRY_POLYHEDRAL:
      // todo
      break;
    case STEREOCHEMISTRY_ALLENE:
    case STEREOCHEMISTRY_TETRAHEDRAL:
      if (chiralOrder == 2) {
        int i = map[0];
        map[0] = map[1];
        map[1] = i;
      }
      cAtoms[map[0]].set(0, 0, 1);
      cAtoms[map[1]].set(1, 0, -1);
      cAtoms[map[2]].set(0, 1, -1);
      cAtoms[map[3]].set(-1, -1, -1);
      break;
    /*      
        case STEREOCHEMISTRY_DOUBLE_BOND:
          switch (chiralOrder) {
          case 1: // U-shaped 0 3 2 1
            cAtoms[map[0]].set(1, 0, 0);
            cAtoms[map[1]].set(0, 1, 0);
            cAtoms[map[2]].set(-1, 0, 0);
            cAtoms[map[3]].set(0, -1, 0);
            break;
          case 2: // 4-shaped
            cAtoms[map[0]].set(1, 0, 0);
            cAtoms[map[1]].set(-1, 0, 0);
            cAtoms[map[2]].set(0, 1, 0);
            cAtoms[map[3]].set(0, -1, 0);
            break;
          }
          break;
    */
    case STEREOCHEMISTRY_SQUARE_PLANAR:
      switch (chiralOrder) {
      case 1: // U-shaped
        cAtoms[map[0]].set(1, 0, 0);
        cAtoms[map[1]].set(0, 1, 0);
        cAtoms[map[2]].set(-1, 0, 0);
        cAtoms[map[3]].set(0, -1, 0);
        break;
      case 2: // 4-shaped
        cAtoms[map[0]].set(1, 0, 0);
        cAtoms[map[1]].set(-1, 0, 0);
        cAtoms[map[2]].set(0, 1, 0);
        cAtoms[map[3]].set(0, -1, 0);
        break;
      case 3: // Z-shaped
        cAtoms[map[0]].set(1, 0, 0);
        cAtoms[map[1]].set(0, 1, 0);
        cAtoms[map[2]].set(0, -1, 0);
        cAtoms[map[3]].set(-1, 0, 0);
        break;
      }
      break;
    case STEREOCHEMISTRY_TRIGONAL_BIPYRAMIDAL:
    case STEREOCHEMISTRY_OCTAHEDRAL:
      int n = map.length;
      if (chiralOrder == 2) {
        int i = map[0];
        map[0] = map[n - 1];
        map[n - 1] = i;
      }
      cAtoms[map[0]].set(0, 0, 1);
      cAtoms[map[n - 1]].set(0, 0, -1);
      cAtoms[map[1]].set(1, 0, 0);
      cAtoms[map[2]].set(0, 1, 0);
      cAtoms[map[3]].set(-1, 0, 0);
      if (n == 6)
        cAtoms[map[4]].set(0, -1, 0);
      break;
    }
    return true;
  }

  private void getX(SmilesAtom sAtom, Node[] jn, int pt,
                    boolean haveCoordinates, boolean needHSwitch) {
    Node atom = getJmolAtom(sAtom.getMatchingAtomIndex());
    boolean doSwitch = sAtom.isFirst || pt == 3;
    if (haveCoordinates) {
      if (search.isSmarts) {
        Edge[] b = atom.getEdges();
        for (int i = 0; i < b.length; i++) {
          if (b[i].getCovalentOrder() == 2)
            continue;
          Node a = jmolAtoms[atom.getBondedAtomIndex(i)];
          if (a == jn[pt - 1])
            continue;
          jn[pt] = a;
          break;
        }
      }
      if (jn[pt] == null) {
        // add a dummy point for stereochemical reference
        // imines and diazines only
        V3 v = new V3();
        int n = 0;
        for (int i = 0; i < 4; i++) {
          if (jn[i] == null)
            continue;
          n++;
          v.sub((P3) jn[i]);
        }
        if (v.length() == 0) {
          v.setT(((P3) jn[4]));
          doSwitch = false;
        } else {
          v.scaleAdd2(n + 1, (P3) getJmolAtom(sAtom.getMatchingAtomIndex()), v);
          doSwitch = search.isSmilesFind || doSwitch;
        }
        jn[pt] = new SmilesAtom().setIndex(-1);
        ((P3) jn[pt]).setT(v);
      }
    }
    if (jn[pt] == null) {
      jn[pt] = search.getHydrogens(atom, null);
      if (needHSwitch)
        doSwitch = true;
    }
    if (jn[pt] != null && doSwitch) {
      // a missing substituent on the SECOND atom 
      // should be placed in position 2, not 3
      // also check for the VERY first atom in a set
      // attached H is first in that case
      // so we have to switch it, since we have
      // assigned already the first atom to be
      // the first pattern atom
      Node a = jn[pt];
      jn[pt] = jn[pt - 1];
      jn[pt - 1] = a;
    }
  }

  private Node getJmolAtom(int i) {
    return (i < 0 || i >= jmolAtoms.length ? null : jmolAtoms[i]);
  }

  /**
   * Sort bond array as ccw rotation around the axis connecting the atom and the
   * reference point (polyhedron center) as seen from outside the polyhedron
   * looking in.
   * 
   * Since we are allowing no branching, all atoms will appear as separate
   * components with only numbers after them. These numbers will be processed
   * and listed in this order.
   * 
   * @param atom
   * @param atomPrev
   * @param ref
   * @param bonds
   * @param vTemp 
   */
  void sortBondsByStereo(Node atom, Node atomPrev, T3 ref, Edge[] bonds,
                          V3 vTemp) {
    if (bonds.length < 2 || !(atom instanceof T3))
      return;
    if (atomPrev == null)
      atomPrev = bonds[0].getOtherAtomNode(atom);
    Object[][] aTemp = new Object[bonds.length][0];
    if (sorter == null)
      sorter = new PolyhedronStereoSorter();
    vTemp.sub2((T3)atomPrev, ref);
    sorter.setRef(vTemp);
    for (int i = bonds.length; --i >= 0;) {
      Node a = bonds[i].getOtherAtomNode(atom);
      float f = (a == atomPrev ? 0 : 
        sorter.isAligned((T3) a, ref, (T3) atomPrev) ? -999 : 
          Measure.computeTorsion((T3) atom,
          (T3) atomPrev, ref, (T3) a, true));
      if (bonds.length > 2)
        f += 360;
      aTemp[i] = new Object[] { bonds[i], Float.valueOf(f), a };
    }
    Arrays.sort(aTemp, sorter);
    if (Logger.debugging)
      Logger.info(Escape.e(aTemp));
    for (int i = bonds.length; --i >= 0;)
      bonds[i] = (Edge) aTemp[i][0];
  }

  public boolean checkStereoChemistry(SmilesSearch smilesSearch, VTemp v) {
    search = smilesSearch;
    jmolAtoms = search.jmolAtoms;
    boolean isSmilesFind = smilesSearch.isSmilesFind;
    boolean invertStereochemistry = smilesSearch.invertStereochemistry;

    if (Logger.debugging)
      Logger.debug("checking sstereochemistry...");

    //for debugging, first try SET DEBUG
    //for (int i = 0; i < ac; i++) {
    //  SmilesAtom sAtom = patternAtoms[i];
    //  System.out.print(sAtom + "=");
    //}
    //System.out.println("");
    //for (int i = 0; i < ac; i++) {
    //  SmilesAtom sAtom = patternAtoms[i];
    //  JmolSmilesNode atom0 = jmolAtoms[sAtom.getMatchingAtom()];
    //  System.out.print(atom0.getIndex() + "-");
    //}
    //System.out.println("");

    for (int i = 0; i < smilesSearch.ac; i++) {
      SmilesAtom sAtom = smilesSearch.patternAtoms[i];
      if (sAtom.stereo == null)
        continue;
      boolean isNot = (sAtom.not != invertStereochemistry);
      Node atom1 = null, atom2 = null, atom3 = null, atom4 = null, atom5 = null, atom6 = null;
      SmilesAtom sAtom1 = null, sAtom2 = null, sAtom0 = null;
      Node[] jn;

      Node atom0 = sAtom.getMatchingAtom();
      if (isSmilesFind)
        sAtom0 = (SmilesAtom) atom0;
      int nH = Math.max(sAtom.missingHydrogenCount, 0);
      int order = sAtom.stereo.chiralOrder;
      int chiralClass = sAtom.stereo.chiralClass;
      // SMILES string must match pattern for chiral class.
      // but we could do something about changing those if desired.
      if (isSmilesFind && sAtom0.getChiralClass() != chiralClass)
        return false;
      if (Logger.debugging)
        Logger.debug("...type " + chiralClass + " for pattern atom " + sAtom
            + " " + atom0);
      switch (chiralClass) {
      case STEREOCHEMISTRY_POLYHEDRAL:
        if (sAtom.stereo.isNot)
          isNot = !isNot;
        if (nH > 1)
          continue; // no chirality for [CH2@]
        if (isSmilesFind) {
          // TODO
          continue;
        }
        SmilesBond[] bonds = sAtom.bonds;
        int jHpt = -1;
        if (nH == 1) {
          jHpt = (sAtom.isFirst ? 0 : 1);
          // can't process this unless it is tetrahedral or perhaps square planar
          if (sAtom.getBondCount() != 3)
            return false;
          v.vA.set(0, 0, 0);
          for (int j = 0; j < 3; j++)
            v.vA.add((T3) bonds[j].getOtherAtom(sAtom0).getMatchingAtom());
          v.vA.scale(0.3333f);
          v.vA.sub2((T3) atom0, v.vA);
          v.vA.add((T3) atom0);
        }
        int[][] po = sAtom.stereo.polyhedralOrders;
        int pt;
        for (int j = po.length; --j >= 0;) {
          int[] orders = po[j];
          if (orders == null || orders.length < 2)
            continue;
          // the atom we are looking down
          pt = (j > jHpt ? j - nH : j);
          T3 ta1 = (j == jHpt ? v.vA : (T3) bonds[pt].getOtherAtom(sAtom)
              .getMatchingAtom());
          float flast = (isNot ? Float.MAX_VALUE : 0);
          T3 ta2 = null;
          for (int k = 0; k < orders.length; k++) {
            pt = orders[k];
            T3 ta3;
            if (pt == jHpt) { // attached H
              ta3 = v.vA;
            } else {
              if (pt > jHpt)
                pt--;
              ta3 = (T3) bonds[pt].getOtherAtom(sAtom).getMatchingAtom();
            }
            if (k == 0) {
              ta2 = ta3;
              continue;
            }
            float f = Measure.computeTorsion(ta3, ta1, (T3) atom0, ta2, true);
            if (Float.isNaN(f))
              f = 180; // directly across the center from the previous atom
            if (orders.length == 2)
              return ((f < 0) != isNot);
            if (f < 0)
              f += 360;
            if ((f < flast) != isNot)
              return false;
            flast = f;
          }
        }
        continue;
      case STEREOCHEMISTRY_ALLENE:
        boolean isAllene = true;//(chiralClass == STEREOCHEMISTRY_ALLENE);
        if (isAllene) {
          sAtom1 = sAtom.getBond(0).getOtherAtom(sAtom);
          sAtom2 = sAtom.getBond(1).getOtherAtom(sAtom);
          if (sAtom1 == null || sAtom2 == null)
            continue; // "OK - stereochemistry is desgnated for something like C=C=O
          // cumulenes
          SmilesAtom sAtom1a = sAtom;
          SmilesAtom sAtom2a = sAtom;
          while (sAtom1.getBondCount() == 2 && sAtom2.getBondCount() == 2
              && sAtom1.getValence() == 4 && sAtom2.getValence() == 4) {
            SmilesBond b = sAtom1.getBondNotTo(sAtom1a, true);
            sAtom1a = sAtom1;
            sAtom1 = b.getOtherAtom(sAtom1);
            b = sAtom2.getBondNotTo(sAtom2a, true);
            sAtom2a = sAtom2;
            sAtom2 = b.getOtherAtom(sAtom2);
          }
          sAtom = sAtom1;
        }
        jn = new Node[6];
        jn[4] = new SmilesAtom().setIndex(604);
        int nBonds = sAtom.getBondCount();
        for (int k = 0; k < nBonds; k++) {
          sAtom1 = sAtom.bonds[k].getOtherAtom(sAtom);
          if (sAtom.bonds[k].matchingBond.getCovalentOrder() == 2) {
            if (sAtom2 == null)
              sAtom2 = sAtom1;
          } else if (jn[0] == null) {
            jn[0] = sAtom1.getMatchingAtom();
          } else {
            jn[1] = sAtom1.getMatchingAtom();
          }
        }
        if (sAtom2 == null)
          continue;
        nBonds = sAtom2.getBondCount();
        if (nBonds < 2 || nBonds > 3)
          continue; // [C@]=O always matches
        for (int k = 0; k < nBonds; k++) {
          sAtom1 = sAtom2.bonds[k].getOtherAtom(sAtom2);
          if (sAtom2.bonds[k].matchingBond.getCovalentOrder() == 2) {
          } else if (jn[2] == null) {
            jn[2] = sAtom1.getMatchingAtom();
          } else {
            jn[3] = sAtom1.getMatchingAtom();
          }
        }

        if (isSmilesFind) {
          if (jn[1] == null)
            getX(sAtom, jn, 1, false, isAllene);
          if (jn[3] == null)
            getX(sAtom2, jn, 3, false, false);
          if (!setSmilesCoordinates(sAtom0, sAtom, sAtom2, jn))
            return false;
        }
        if (jn[1] == null)
          getX(sAtom, jn, 1, true, false);
        if (jn[3] == null)
          getX(sAtom2, jn, 3, true, false);
        if (!checkStereochemistryAll(sAtom.not != invertStereochemistry, atom0,
            chiralClass, order, jn[0], jn[1], jn[2], jn[3], null, null, v))
          return false;
        continue;
      case STEREOCHEMISTRY_TRIGONAL_PYRAMIDAL:
      case STEREOCHEMISTRY_TETRAHEDRAL:
      case STEREOCHEMISTRY_SQUARE_PLANAR:
      case STEREOCHEMISTRY_TRIGONAL_BIPYRAMIDAL:
      case STEREOCHEMISTRY_OCTAHEDRAL:
        atom1 = getJmolAtom(sAtom.getMatchingBondedAtom(0));
        switch (nH) {
        case 0:
          atom2 = getJmolAtom(sAtom.getMatchingBondedAtom(1));
          break;
        case 1:
          atom2 = smilesSearch.getHydrogens(sAtom.getMatchingAtom(), null);
          if (sAtom.isFirst) {
            Node a = atom2;
            atom2 = atom1;
            atom1 = a;
          }
          break;
        default:
          continue;
        }
        atom3 = getJmolAtom(sAtom.getMatchingBondedAtom(2 - nH));
        atom4 = getJmolAtom(sAtom.getMatchingBondedAtom(3 - nH));
        atom5 = getJmolAtom(sAtom.getMatchingBondedAtom(4 - nH));
        atom6 = getJmolAtom(sAtom.getMatchingBondedAtom(5 - nH));

        // in all the checks below, we use Measure utilities to 
        // three given atoms -- the normal, in particular. We 
        // then use dot products to check the directions of normals
        // to see if the rotation is in the direction required. 

        // we only use TP1, TP2, OH1, OH2 here.
        // so we must also check that the two bookend atoms are axial

        if (isSmilesFind
            && !setSmilesCoordinates(sAtom0, sAtom, sAtom2, new Node[] { atom1,
                atom2, atom3, atom4, atom5, atom6 }))
          return false;
        if (!checkStereochemistryAll(isNot, atom0, chiralClass, order, atom1,
            atom2, atom3, atom4, atom5, atom6, v))
          return false;
        continue;

      }

    }

    return true;

  }

  /**
   * 
   * @param atom0
   * @param atoms
   * @param nAtoms
   * @param v
   * @return String
   */
  static String getStereoFlag(Node atom0, Node[] atoms, int nAtoms, VTemp v) {
    Node atom1 = atoms[0];
    Node atom2 = atoms[1];
    Node atom3 = atoms[2];
    Node atom4 = atoms[3];
    Node atom5 = atoms[4];
    Node atom6 = atoms[5];
    int chiralClass = STEREOCHEMISTRY_TETRAHEDRAL;
    // what about POLYHEDRAL?
    switch (nAtoms) {
    default:
    case 5:
    case 6:
      // like tetrahedral
      return (checkStereochemistryAll(false, atom0, chiralClass, 1, atom1,
          atom2, atom3, atom4, atom5, atom6, v) ? "@" : "@@");
    case 2: // allene
    case 4: // tetrahedral, square planar
      if (atom3 == null || atom4 == null)
        return "";
      float d = SmilesAromatic.getNormalThroughPoints(atom1, atom2, atom3,
          v.vTemp, v.vA, v.vB);
      if (Math.abs(distanceToPlane(v.vTemp, d, (P3) atom4)) < 0.2f) {
        chiralClass = STEREOCHEMISTRY_SQUARE_PLANAR;
        if (checkStereochemistryAll(false, atom0, chiralClass, 1, atom1, atom2,
            atom3, atom4, atom5, atom6, v))
          return "@SP1";
        if (checkStereochemistryAll(false, atom0, chiralClass, 2, atom1, atom2,
            atom3, atom4, atom5, atom6, v))
          return "@SP2";
        if (checkStereochemistryAll(false, atom0, chiralClass, 3, atom1, atom2,
            atom3, atom4, atom5, atom6, v))
          return "@SP3";
      } else {
        return (checkStereochemistryAll(false, atom0, chiralClass, 1, atom1,
            atom2, atom3, atom4, atom5, atom6, v) ? "@" : "@@");
      }
    }
    return "";
  }

  static boolean checkStereochemistryAll(boolean isNot, Node atom0,
                                         int chiralClass, int order,
                                         Node atom1, Node atom2, Node atom3,
                                         Node atom4, Node atom5, Node atom6,
                                         VTemp v) {

    switch (chiralClass) {
    default:
      return true;
    case STEREOCHEMISTRY_POLYHEDRAL:
      return true;
    case STEREOCHEMISTRY_TRIGONAL_PYRAMIDAL:
      return (isNot == (getHandedness(atom2, atom3, atom0, atom1, v) != order));
    case STEREOCHEMISTRY_ALLENE:
    case STEREOCHEMISTRY_TETRAHEDRAL:
      return (isNot == (getHandedness(atom2, atom3, atom4, atom1, v) != order));
    case STEREOCHEMISTRY_TRIGONAL_BIPYRAMIDAL:
      // check for axial-axial'
      return (isNot == (!isDiaxial(atom0, atom0, atom5, atom1, v, -0.95f) || getHandedness(
          atom2, atom3, atom4, atom1, v) != order));
    case STEREOCHEMISTRY_OCTAHEDRAL:
      if (isNot != (!isDiaxial(atom0, atom0, atom6, atom1, v, -0.95f)))
        return false;
      // check for CW or CCW set
      getPlaneNormals(atom2, atom3, atom4, atom5, v);
      if (isNot != (v.vNorm1.dot(v.vNorm2) < 0 || v.vNorm2.dot(v.vNorm3) < 0))
        return false;
      // now check rotation in relation to the first atom
      v.vNorm2.sub2((P3) atom0, (P3) atom1);
      return (isNot == ((v.vNorm1.dot(v.vNorm2) < 0 ? 2 : 1) == order));
      //case STEREOCHEMISTRY_DOUBLE_BOND:
      //System.out.println("draw p1 " + Point3f.new3((Point3f)atom1)+" color red");
      //System.out.println("draw p2 " + Point3f.new3((Point3f)atom2)+" color yellow");
      //System.out.println("draw p3 " + Point3f.new3((Point3f)atom3)+" color green");
      //System.out.println("draw p4 " + Point3f.new3((Point3f)atom4)+" color blue");

      //getPlaneNormals(atom1, atom2, atom3, atom4, v);
      //System.out.println(order + " "+ atom1.getAtomName() + "-" + atom2.getAtomName() + "-"  + atom3.getAtomName() + "-" + atom4.getAtomName());
      //return (isNot == ((v.vNorm1.dot(v.vNorm2) < 0 ? 2 : 1) == order));
    case STEREOCHEMISTRY_SQUARE_PLANAR:
      getPlaneNormals(atom1, atom2, atom3, atom4, v);
      // vNorm1 vNorm2 vNorm3 are right-hand normals for the given
      // triangles
      // 1-2-3, 2-3-4, 3-4-1
      // sp1 up up up U-shaped
      // sp2 up up DOWN 4-shaped
      // sp3 up DOWN DOWN Z-shaped
      return (v.vNorm1.dot(v.vNorm2) < 0 ? isNot == (order != 3) : v.vNorm2
          .dot(v.vNorm3) < 0 ? isNot == (order != 2) : isNot == (order != 1));
    }
  }

  static boolean isDiaxial(Node atomA, Node atomB, Node atom1, Node atom2,
                           VTemp v, float f) {
    v.vA.sub2((P3) atomA, (P3) atom1);
    v.vB.sub2((P3) atomB, (P3) atom2);
    v.vA.normalize();
    v.vB.normalize();
    // -0.95f about 172 degrees
    return (v.vA.dot(v.vB) < f);
  }

  /**
   * compares the
   * 
   * @param a
   * @param b
   * @param c
   * @param pt
   * @param v
   * @return 1 for "@", 2 for "@@"
   */
  private static int getHandedness(Node a, Node b, Node c, Node pt, VTemp v) {
    float d = SmilesAromatic.getNormalThroughPoints(a, b, c, v.vTemp, v.vA,
        v.vB);
    //int atat = (distanceToPlane(v.vTemp, d, (Point3f) pt) > 0 ? 1 : 2);
    //System.out.println("draw p1 " + Point3f.new3((Point3f)a) +" color red");
    //System.out.println("draw p2 " + Point3f.new3((Point3f)b)+" color green");
    //System.out.println("draw p3 " + Point3f.new3((Point3f)c)+" color blue");
    //System.out.println("draw p " + Point3f.new3((Point3f)a) +" " + Point3f.new3((Point3f)b)+" " + Point3f.new3((Point3f)c));
    //System.out.println("draw v vector " + Point3f.new3((Point3f)pt) + " " + v.vTemp+" \""+ (atat==2 ? "@@" : "@")+"\" color " + (atat == 2 ? "white" : "yellow"));
    return (distanceToPlane(v.vTemp, d, (P3) pt) > 0 ? 1 : 2);
  }

  private static void getPlaneNormals(Node atom1, Node atom2, Node atom3,
                                      Node atom4, VTemp v) {
    SmilesAromatic.getNormalThroughPoints(atom1, atom2, atom3, v.vNorm1,
        v.vTemp1, v.vTemp2);
    SmilesAromatic.getNormalThroughPoints(atom2, atom3, atom4, v.vNorm2,
        v.vTemp1, v.vTemp2);
    SmilesAromatic.getNormalThroughPoints(atom3, atom4, atom1, v.vNorm3,
        v.vTemp1, v.vTemp2);
  }

  static float distanceToPlane(V3 norm, float w, P3 pt) {
    return (norm == null ? Float.NaN : (norm.x * pt.x + norm.y * pt.y + norm.z
        * pt.z + w)
        / (float) Math
            .sqrt(norm.x * norm.x + norm.y * norm.y + norm.z * norm.z));
  }

  static int checkChirality(String pattern, int index, SmilesAtom newAtom)
      throws InvalidSmilesException {
    int stereoClass = 0;
    int order = Integer.MIN_VALUE;
    int len = pattern.length();
    String details = null;
    String directives = null;
    int atomCount = 0;
    char ch;
    stereoClass = STEREOCHEMISTRY_DEFAULT;
    order = 1;
    boolean isPoly = false;
    if (++index < len) {
      switch (ch = pattern.charAt(index)) {
      case '@':
        order = 2;
        index++;
        break;
      case 'H': // @H, @@H
        break;
      case 'P': //PH // Jmol
        isPoly = true;
        //$FALL-THROUGH$
      case 'A': //AL
      case 'O': //OH
      case 'S': //SP
      case 'T': //TH, TP
        stereoClass = (index + 1 < len ? getChiralityClass(pattern.substring(
            index, index + 2)) : -1);
        index += 2;
        break;
      default:
        order = (PT.isDigit(ch) ? 1 : -1);
      }
      int pt = index;
      if (order == 1 || isPoly) {
        while (pt < len && PT.isDigit(pattern.charAt(pt)))
          pt++;
        if (pt > index) {
          try {
            int n = Integer.parseInt(pattern.substring(index, pt));
            if (isPoly) {
              atomCount = n;
              // we may have more to get? Distance?
              if (pt < len && pattern.charAt(pt) == '(') {
                details = SmilesParser.getSubPattern(pattern, pt, '(');
                pt += details.length() + 2;
              }
            } else {
              order = n;
            }
          } catch (NumberFormatException e) {
            order = -1;
          }
          index = pt;
        }
      }
      if (order < 1 || stereoClass < 0)
        throw new InvalidSmilesException("Invalid stereochemistry descriptor");
    }
    newAtom.stereo = new SmilesStereo(stereoClass, order, atomCount, details,
        directives);
    if (SmilesParser.getChar(pattern, index) == '?') {
      Logger.info("Ignoring '?' in stereochemistry");
      index++;
    }
    return index;
  }

}
