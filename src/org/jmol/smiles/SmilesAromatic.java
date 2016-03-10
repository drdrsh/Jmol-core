/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2010-05-08 19:20:44 -0500 (Sat, 08 May 2010) $
 * $Revision: 13038 $
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




import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.V3;

import org.jmol.java.BS;
import org.jmol.util.Edge;
import org.jmol.util.Node;

public class SmilesAromatic {
  /**
   * 3D-SEARCH aromaticity test.
   * 
   * A simple and unambiguous test for aromaticity based on 3D geometry and
   * connectivity only, not Hueckel theory.
   * @param n 
   * 
   * @param atoms
   *        a set of atoms with coordinate positions and associated bonds.
   * @param bs
   *        a bitset of atoms within the set of atoms, defining the ring
   * @param bsSelected
   *        must not be null
   * @param cutoff
   *        an arbitrary value to test the standard deviation against. 0.01 is
   *        appropriate here.
   * @param isOSGenerator 
   * @return true if standard deviation of vNorm.dot.vMean is less than cutoff
   */

  public final static boolean isFlatSp2Ring(int n, Node[] atoms, BS bsSelected,
                                            BS bs, float cutoff,
                                            boolean isOSGenerator) {
///
 // 
 // Bob Hanson, hansonr@stolaf.edu
 // 
 //   Given a ring of N atoms...
 //   
 //                 1
 //               /   \
 //              2     6 -- 6a
 //              |     |
 //        5a -- 5     4
 //               \   /
 //                 3  
 //   
 //   with arbitrary order and up to N substituents
 //   
 //   1) Check to see if all ring atoms have no more than 3 connections.
 //      Note: An alternative definition might include "and no substituent
 //      is explicitly double-bonded to its ring atom, as in quinone.
 //      Here we opt to allow the atoms of quinone to be called "aromatic."
 //   2) Select a cutoff value close to zero. We use 0.01 here. 
 //   3) Generate a set of normals as follows:
 //      a) For each ring atom, construct the normal associated with the plane
 //         formed by that ring atom and its two nearest ring-atom neighbors.
 //      b) For each ring atom with a substituent, construct a normal 
 //         associated with the plane formed by its connecting substituent
 //         atom and the two nearest ring-atom neighbors.
 //      c) If this is the first normal, assign vMean to it. 
 //      d) If this is not the first normal, check vNorm.dot.vMean. If this
 //         value is less than zero, scale vNorm by -1.
 //      e) Add vNorm to vMean. 
 //   4) Calculate the standard deviation of the dot products of the 
 //      individual vNorms with the normalized vMean. 
 //   5) The ring is deemed flat if this standard deviation is less 
 //      than the selected cutoff value. 
 //      
 //   Efficiencies:
 //   
 //   1) Precheck bond counts.
 //   
 //   2) Each time a normal is added to the running mean, test to see if 
 //      its dot product with the mean is within 5 standard deviations. 
 //      If it is not, return false. Note that it can be shown that for 
 //      a set of normals, even if all are aligned except one, with dot product
 //      to the mean x, then the standard deviation will be (1 - x) / sqrt(N).
 //      Given even an 8-membered ring, this still
 //      results in a minimum value of x of about 1-4c (allowing for as many as
 //      8 substituents), considerably better than our 1-5c. 
 //      So 1-5c is a very conservative test.   
 //      
 //   3) One could probably dispense with the actual standard deviation 
 //      calculation, as it is VERY unlikely that an actual nonaromatic rings
 //      (other than quinones and other such compounds)
 //      would have any chance of passing the first two tests.
 //      
 //   OpenSMILES Generator:
 //     
 //   (a) the atom is in a 6-membered ring containing only C, N, or O and
 //       matching SMARTS [#6X3+0,#6X2+1,#6X2-1,#7X2+0,#7X3+1,#8X2+1]
 //   (b) the atom is not doubly bonded to any nonaromatic atom (prevents case of exocyclic double bonds)
 //   (c) the atom connects with at least two other aromatic atoms

    if (isOSGenerator && n != 6)
      return false;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      Node ringAtom = atoms[i];
      Edge[] bonds = ringAtom.getEdges();
      if (bonds.length < 3)
        continue;
      if (bonds.length > 3)
        return false;
    }
    if (cutoff == Float.MAX_VALUE)
      return true;

    if (cutoff <= 0)
      cutoff = 0.01f;

    V3 vTemp = new V3();
    V3 vA = new V3();
    V3 vB = new V3();
    V3 vMean = null;
    int nPoints = bs.cardinality();
    V3[] vNorms = new V3[nPoints * 2];
    int nNorms = 0;
    float maxDev = (1 - cutoff * 5);
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      Node ringAtom = atoms[i];
      if (isOSGenerator) {
        int elemno = ringAtom.getElementNumber();
        switch (elemno) {
        case 8:
          // allow [o+1]
          if (ringAtom.getFormalCharge() != 1)
            return false;
          break;
        case 6:
        case 7:
          // trivalent C, [N+1]; divalent [N+0]
          if (ringAtom.getCovalentBondCount() != (elemno == 6
              || ringAtom.getFormalCharge() == 1 ? 3 : 2))
            return false;
          break;
        default:
          return false;
        }
      }
      Edge[] bonds = ringAtom.getEdges();
      // if more than three connections, ring cannot be fully conjugated
      // identify substituent and two ring atoms
      int iSub = -1;
      int r1 = -1;
      int r2 = -1;
      for (int k = bonds.length; --k >= 0;) {
        int iAtom = ringAtom.getBondedAtomIndex(k);
        if (!bsSelected.get(iAtom))
          continue;
        if (!bs.get(iAtom))
          iSub = iAtom;
        else if (r1 < 0)
          r1 = iAtom;
        else
          r2 = iAtom;
      }
      // get the normals through r1 - k - r2 and r1 - iSub - r2
      getNormalThroughPoints(atoms[r1], atoms[i], atoms[r2], vTemp, vA, vB);
      if (vMean == null)
        vMean = new V3();
      if (!addNormal(vTemp, vMean, maxDev))
        return false;
      vNorms[nNorms++] = V3.newV(vTemp);
      if (iSub >= 0) {
        getNormalThroughPoints(atoms[r1], atoms[iSub], atoms[r2], vTemp, vA, vB);
        if (!addNormal(vTemp, vMean, maxDev))
          return false;
        vNorms[nNorms++] = V3.newV(vTemp);
      }
    }
    boolean isFlat = checkStandardDeviation(vNorms, vMean, nNorms, cutoff);
    //System.out.println(Escape.escape(bs) + " aromatic ? " + isAromatic);
    return isFlat;
  }

  private final static boolean addNormal(V3 vTemp, V3 vMean,
                                         float maxDev) {
    float similarity = vMean.dot(vTemp);
    if (similarity != 0 && Math.abs(similarity) < maxDev)
      return false;
    if (similarity < 0)
      vTemp.scale(-1);
    vMean.add(vTemp);
    vMean.normalize();
    return true;
  }

  private final static boolean checkStandardDeviation(V3[] vNorms,
                                                      V3 vMean, int n,
                                                      float cutoff) {
    double sum = 0;
    double sum2 = 0;
    for (int i = 0; i < n; i++) {
      float v = vNorms[i].dot(vMean);
      sum += v;
      sum2 += ((double) v) * v;
    }
    sum = Math.sqrt((sum2 - sum * sum / n) / (n - 1));
    //System.out.println("stdev = " + sum);
    return (sum < cutoff);
  }

  static float getNormalThroughPoints(Node pointA, Node pointB,
                                      Node pointC, V3 vNorm,
                                      V3 vAB, V3 vAC) {
    vAB.sub2((P3) pointB, (P3) pointA);
    vAC.sub2((P3) pointC, (P3) pointA);
    vNorm.cross(vAB, vAC);
    vNorm.normalize();
    // ax + by + cz + d = 0
    // so if a point is in the plane, then N dot X = -d
    vAB.setT((P3) pointA);
    return -vAB.dot(vNorm);
  }

  /**
   * set aromatic atoms based on predefined BOND_AROMATIC definitions
   * @param jmolAtoms
   * @param bsAtoms
   * @return bsAromatic
   */
  static BS checkAromaticDefined(Node[] jmolAtoms, BS bsAtoms) {
    BS bsDefined = new BS();
    for (int i = bsAtoms.nextSetBit(0); i >= 0; i = bsAtoms.nextSetBit(i + 1)) {
      Edge[] bonds = jmolAtoms[i].getEdges();
      for (int j = 0; j < bonds.length; j++) {
        switch (bonds[j].order) {
        case Edge.BOND_AROMATIC:
        case Edge.BOND_AROMATIC_DOUBLE:
        case Edge.BOND_AROMATIC_SINGLE:
          bsDefined.set(bonds[j].getAtomIndex1());
          bsDefined.set(bonds[j].getAtomIndex2());
        }
      }
    }
    return bsDefined;
  }
  
  static void checkAromaticStrict(Node[] jmolAtoms, BS bsAromatic,
                                  Lst<Object> v5, Lst<Object> v6) {
    BS bsStrict = new BS();
    BS bsTest = new BS();
    checkAromaticStrict56(5, v5, v6, jmolAtoms, bsAromatic, bsTest, bsStrict);
    checkAromaticStrict56(6, v5, v6, jmolAtoms, bsAromatic, bsTest, bsStrict);
    bsAromatic.clearAll();
    bsAromatic.or(bsStrict);
  }

  /**
   * uses an MMFF94 strategy for determining aromaticity for a specific ring.
   * 
   * @param n 
   * @param jmolAtoms
   * @param bsStrict  growing list of aromatic atoms
   * @param v5
   * @param v6
   * @param bsTest
   * @param bsAromatic
   */
  private static void checkAromaticStrict56(int n, Lst<Object> v5, Lst<Object> v6,
                                            Node[] jmolAtoms, BS bsAromatic,
                                            BS bsTest, BS bsStrict) {

    // I believe this gives the wrong answer for mmff94_dative.mol2 CIKSEU10
    // but at least it agrees with MMFF94.  -- Bob Hanson

    boolean is5 = (n == 5);
    Lst<Object> lst = (is5 ? v5 : v6);
    for (int ir = lst.size(); --ir >= 0;) {
      BS bsRing = (BS) lst.get(ir);
      bsTest.clearAll();
      bsTest.or(bsRing);
      bsTest.and(bsAromatic);
      if (bsTest.cardinality() != n)
        continue;
      int piElectronCount = countInternalPairsStrict(jmolAtoms, bsRing, is5) << 1;
      switch (piElectronCount) {
      case -3:
        break;
      default:
        out: for (int i = bsRing.nextSetBit(0); i >= 0; i = bsRing.nextSetBit(i + 1)) {
          Edge[] bonds = jmolAtoms[i].getEdges();
          for (int j = 0; j < bonds.length; j++)
            if (bonds[j].order == Edge.BOND_COVALENT_DOUBLE) {
              int i2 = bonds[j].getOtherAtomNode(jmolAtoms[i]).getIndex();
              if (!bsRing.get(i2)) {
                boolean piShared = false;
                for (int k = v5.size(); --k >= 0 && !piShared;) {
                  BS bs = (BS) v5.get(k);
                  if (bs.get(i2)
                      && (bsStrict.get(i2) || Math.abs(countInternalPairsStrict(
                          jmolAtoms, bs, true)) == 3))
                    piShared = true;
                }
                for (int k = v6.size(); --k >= 0 && !piShared;) {
                  BS bs = (BS) v6.get(k);
                  if (bs.get(i2)
                      && (bsStrict.get(i2) || Math.abs(countInternalPairsStrict(
                          jmolAtoms, bs, false)) == 3))
                    piShared = true;
                }
                if (!piShared)
                  break out;
                piElectronCount++;
              }
            }
        }
        break;
      }
      if (piElectronCount == 6)
        bsStrict.or(bsRing);
    }
  }

  /**
   * Counts the electron pairs that are internal to this ring. 
   * Allows for aromatic bond types.
   * Note that Jmol has already determined that the ring is flat
   * so there is no need to worry about hybridization.
   * 
   * @param jmolAtoms
   * @param bsRing
   * @param is5
   * @return  number of pairs
   */
  private static int countInternalPairsStrict(Node[] jmolAtoms, BS bsRing,
                                        boolean is5) {
    int nDouble = 0;
    int nAromatic = 0;
    int nLonePairs = 0;
    for (int i = bsRing.nextSetBit(0); i >= 0; i = bsRing.nextSetBit(i + 1)) {
      Node atom = jmolAtoms[i];
      
      Edge[] bonds = atom.getEdges();
      boolean haveDouble = false;
      for (int k = 0; k < bonds.length; k++) {
        int j = bonds[k].getOtherAtomNode(atom).getIndex();
        if (bsRing.get(j)) {
          switch (bonds[k].order) {
          case Edge.BOND_AROMATIC_DOUBLE:
          case Edge.BOND_AROMATIC_SINGLE:
          case Edge.BOND_AROMATIC:
            nAromatic++;
            break;
          case Edge.BOND_COVALENT_DOUBLE:
            nDouble++;
            haveDouble = true;
          }
        }
      }
      if (is5 && nAromatic == 0) {
        switch (atom.getElementNumber()) {
        case 7:
        case 8:
        case 16:
          if (!haveDouble)
            nLonePairs++;
          break;
        }
      }
    }
    return (nAromatic == 0 ? nDouble / 2 + nLonePairs
        : nAromatic == (is5 ? 5 : 6) ? -3 : 0);
  }

  public static void setAromatic(int i, Node[] jmolAtoms, BS bsSelected,
                                 Lst<Object> v5, Lst<Object> vR, Lst<BS>[] vRings,
                                 BS bsAromatic,
                                 BS bsAromatic5, BS bsAromatic6,
                                 boolean aromaticDefined,
                                 boolean aromaticStrict, boolean isOSGenerator, VTemp v) {
    if (!aromaticDefined && (!aromaticStrict || i == 5 || i == 6))
      for (int r = vR.size(); --r >= 0;) {
        BS bs = (BS) vR.get(r);
        if (aromaticDefined
            || SmilesAromatic.isFlatSp2Ring(i, jmolAtoms, bsSelected, bs,
                (aromaticStrict ? 0.1f : 0.01f), isOSGenerator)) {
          bsAromatic.or(bs);
          if (!aromaticStrict)
            switch (i) {
            case 5:
              bsAromatic5.or(bs);
              break;
            case 6:
              bsAromatic6.or(bs);
              break;
            }
        }
      }
    if (aromaticStrict) {
      switch (i) {
      case 5:
        v5 = vR;
        break;
      case 6:
        if (aromaticDefined)
          bsAromatic = SmilesAromatic.checkAromaticDefined(jmolAtoms,
              bsAromatic);
        else
          SmilesAromatic.checkAromaticStrict(jmolAtoms, bsAromatic, v5, vR);
        vRings[3] = new Lst<BS>();
        setAromatic56(v5, bsAromatic5, 5, vRings[3], v, bsAromatic);
        setAromatic56(vR, bsAromatic6, 6, vRings[3], v, bsAromatic);
        break;
      }
    }
  }

  static private void setAromatic56(Lst<Object> vRings, BS bs56, int n56, Lst<BS> vAromatic56, VTemp v, BS bsAromatic) {
    for (int k = 0; k < vRings.size(); k++) {
      BS r = (BS) vRings.get(k);
      v.bsTemp.clearAll();
      v.bsTemp.or(r);
      v.bsTemp.and(bsAromatic);
      if (v.bsTemp.cardinality() == n56) {
        bs56.or(r);
        if (vAromatic56 != null)
          vAromatic56.addLast(r);
      }
    }
  }

  public static void finalizeAromatic(Node[] jmolAtoms, BS bsAromatic,
                                      BS bsAromatic5, BS bsAromatic6) {
    for (int i = bsAromatic.nextSetBit(0); i >= 0; i = bsAromatic
        .nextSetBit(i + 1)) {
      Edge[] bonds = jmolAtoms[i].getEdges();
      int naro = 0;
      for (int j = bonds.length; --j >= 0;) {
        boolean isJAro = bsAromatic.get(bonds[j].getOtherAtomNode(jmolAtoms[i])
                .getIndex());
        if (isJAro) {
          naro++;
        } else if (bonds[j].getCovalentOrder() == 2) {
          naro = 1;
          break;
        }
      }
      if (naro < 2) {
        bsAromatic.clear(i);
        bsAromatic6.clear(i);
        bsAromatic5.clear(i);
        i = -1;
      }
    }
  }

}
