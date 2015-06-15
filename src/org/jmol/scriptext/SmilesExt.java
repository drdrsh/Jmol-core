/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-03-05 12:22:08 -0600 (Sun, 05 Mar 2006) $
 * $Revision: 4545 $
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

package org.jmol.scriptext;

import org.jmol.api.Interface;
import org.jmol.api.SmilesMatcherInterface;
import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.BondSet;
import org.jmol.script.ScriptEval;
import org.jmol.script.ScriptException;
import org.jmol.util.BSUtil;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.Measure;

import org.jmol.util.Logger;
import org.jmol.viewer.JC;

import javajs.util.M4;
import javajs.util.P3;

public class SmilesExt {
  
  private ScriptEval e;
  private SmilesMatcherInterface sm;
  
  public SmilesExt() {
    // used by Reflection
  }

  public SmilesExt init(Object se) {
    e = (ScriptEval) se;
    sm = e.vwr.getSmilesMatcher();
    return this;
  }

  ///////////// ScriptMathProcessor extensions ///////////

  /**
   * The major interface to org.jmol.smiles, this method allows for a wide
   * variety of correlation functionality.
   * 
   * @param bsA
   * @param bsB
   * @param smiles
   * @param ptsA
   * @param ptsB
   * @param m4
   * @param vReturn
   * @param asMap
   * @param mapSet
   * @param center
   * @param bestMap
   * @param flags
   * @return standard deviation
   * @throws ScriptException
   */
  public float getSmilesCorrelation(BS bsA, BS bsB, String smiles,
                                    Lst<P3> ptsA, Lst<P3> ptsB, M4 m4,
                                    Lst<BS> vReturn, 
                                    boolean asMap, int[][] mapSet, P3 center,
                                    boolean bestMap, int flags)
      throws ScriptException {

//   middle two: boolean isSmarts,boolean firstMatchOnly, 
    float tolerance = (mapSet == null ? 0.1f : Float.MAX_VALUE);
    try {
      if (ptsA == null) {
        ptsA = new Lst<P3>();
        ptsB = new Lst<P3>();
      }
      M4 m = new M4();
      P3 c = new P3();

      Atom[] atoms = e.vwr.ms.at;
      int ac = e.vwr.ms.ac;
      int[][] maps = sm.getCorrelationMaps(smiles, atoms, ac, bsA,
          flags | JC.SMILES_RETURN_FIRST);
      if (maps == null)
        e.evalError(sm.getLastException(), null);
      if (maps.length == 0)
        return Float.NaN;
      int[] mapFirst = maps[0];
      for (int i = 0; i < mapFirst.length; i++)
        ptsA.addLast(atoms[mapFirst[i]]);
      maps = sm.getCorrelationMaps(smiles, atoms, ac, bsB, flags);
      if (maps == null)
        e.evalError(sm.getLastException(), null);
      if (maps.length == 0)
        return Float.NaN;
      Logger.info(maps.length + " mappings found");
      if (bestMap || !asMap) {
        float lowestStdDev = Float.MAX_VALUE;
        int[] mapBest = null;
        for (int i = 0; i < maps.length; i++) {
          ptsB.clear();
          for (int j = 0; j < maps[i].length; j++)
            ptsB.addLast(atoms[maps[i][j]]);
          Interface.getInterface("javajs.util.Eigen", e.vwr, "script");
          float stddev = Measure.getTransformMatrix4(ptsA, ptsB, m, c);
          Logger.info("getSmilesCorrelation stddev=" + stddev);
          if (vReturn != null) {
            if (stddev < tolerance) {
              BS bs = new BS();
              for (int j = 0; j < maps[i].length; j++)
                bs.set(maps[i][j]);
              vReturn.addLast(bs);
            }
          }
          if (stddev < lowestStdDev) {
            mapBest = maps[i];
            if (m4 != null)
              m4.setM4(m);
            if (center != null)
              center.setT(c);
            lowestStdDev = stddev;
          }
        }
        if (mapSet != null) {
          mapSet[0] = mapFirst;
          mapSet[1] = mapBest;
        }
        ptsB.clear();
        for (int i = 0; i < mapBest.length; i++)
          ptsB.addLast(atoms[mapBest[i]]);
        return lowestStdDev;
      }
      // deliver all maps as a list of points
      for (int i = 0; i < maps.length; i++)
        for (int j = 0; j < maps[i].length; j++)
          ptsB.addLast(atoms[maps[i][j]]);
    } catch (Exception ex) {
      e.evalError(ex.getMessage(), null);
    }
    return 0;
  }

  public Object getSmilesMatches(String pattern, String smiles, BS bsSelected,
                                 BS bsMatch3D, boolean isSmarts,
                                 boolean asOneBitset) throws ScriptException {

    // just retrieving the SMILES or bioSMILES string

    if (pattern.length() == 0 || pattern.equals("H") || pattern.equals("*")) {
      boolean isBioSmiles = (!asOneBitset);
      try {
        return e.vwr.getSmilesOpt(bsSelected, 0, 0, 
              (pattern.equals("H") ? JC.SMILES_EXPLICIT_H : 0)
            | (pattern.equals("*") ? JC.SMILES_TOPOLOGY : 0)
            | (isBioSmiles ? JC.SMILES_BIO | JC.SMILES_BIO_CROSSLINK | JC.SMILES_BIO_COMMENT : 0)
        );
      } catch (Exception ex) {
        e.evalError(ex.getMessage(), null);
      }
    }

    boolean asAtoms = true;
    BS[] b;
    if (bsMatch3D == null) {

      // getting a BitSet or BitSet[] from a set of atoms or a pattern.

      asAtoms = (smiles == null);
      try {
        if (asAtoms)
          b = sm.getSubstructureSetArray(pattern,
              e.vwr.ms.at, e.vwr.ms.ac, bsSelected, null,
              isSmarts ? JC.SMILES_TYPE_SMARTS : JC.SMILES_TYPE_SMILES);
        else
          b = sm.find(pattern, smiles, isSmarts, false);

      } catch (Exception ex) {
        e.evalError(ex.getMessage(), null);
        //if (!asAtoms && !isSmarts)
        //return Integer.valueOf(-1);
        return null;
      }
    } else {

      // getting a correlation

      Lst<BS> vReturn = new Lst<BS>();
      float stddev = getSmilesCorrelation(bsMatch3D, bsSelected, pattern, null,
          null, null, vReturn, false, null, null, false, 
          isSmarts ? JC.SMILES_TYPE_SMARTS : JC.SMILES_TYPE_SMILES);
      if (Float.isNaN(stddev)) {
        if (asOneBitset)
          return new BS();
        return new String[] {};
      }
      e.showString("RMSD " + stddev + " Angstroms");
      b = vReturn.toArray(new BS[vReturn.size()]);
    }
    if (asOneBitset) {
      // sum total of all now, not just first
      BS bs = new BS();
      for (int j = 0; j < b.length; j++)
        bs.or(b[j]);
      if (asAtoms)
        return bs;
      if (!isSmarts)
        return Integer.valueOf(bs.cardinality());
      int[] iarray = new int[bs.cardinality()];
      int pt = 0;
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        iarray[pt++] = i + 1;
      return iarray;
    }
    if (!asAtoms)
      for (int j = 0; j < b.length; j++)
        b[j] = BSUtil.copy2(b[j], new BondSet());
    Lst<BS> list = new Lst<BS>();
    for (int j = 0; j < b.length; j++)
      list.addLast(b[j]);
    return list;
  }

  public float[] getFlexFitList(BS bs1, BS bs2, String smiles1,
                                 boolean isSmarts) throws ScriptException {
    int[][] mapSet = AU.newInt2(2);
    getSmilesCorrelation(bs1, bs2, smiles1, null, null, null, null,
        false, mapSet, null, false, isSmarts ? JC.SMILES_TYPE_SMARTS : JC.SMILES_TYPE_SMILES);
    if (mapSet[0] == null)
      return null;
    int[][] bondMap1 = e.vwr.ms.getDihedralMap(mapSet[0]);
    int[][] bondMap2 = (bondMap1 == null ? null : e.vwr
        .ms.getDihedralMap(mapSet[1]));
    if (bondMap2 == null || bondMap2.length != bondMap1.length)
      return null;
    float[][] angles = new float[bondMap1.length][3];
    Atom[] atoms = e.vwr.ms.at;
    getTorsions(atoms, bondMap2, angles, 0);
    getTorsions(atoms, bondMap1, angles, 1);
    float[] data = new float[bondMap1.length * 6];
    for (int i = 0, pt = 0; i < bondMap1.length; i++) {
      int[] map = bondMap1[i];
      data[pt++] = map[0];
      data[pt++] = map[1];
      data[pt++] = map[2];
      data[pt++] = map[3];
      data[pt++] = angles[i][0];
      data[pt++] = angles[i][1];
    }
    return data;
  }

  private static void getTorsions(Atom[] atoms, int[][] bondMap,
                                  float[][] diff, int pt) {
    for (int i = bondMap.length; --i >= 0;) {
      int[] map = bondMap[i];
      float v = Measure.computeTorsion(atoms[map[0]], atoms[map[1]],
          atoms[map[2]], atoms[map[3]], true);
      if (pt == 1) {
        if (v - diff[i][0] > 180)
          v -= 360;
        else if (v - diff[i][0] <= -180)
          v += 360;
      }
      diff[i][pt] = v;
    }
  }

}
