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

import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jmol.api.Interface;
import org.jmol.api.JmolNMRInterface;
import org.jmol.api.JmolPatternMatcher;
import org.jmol.atomdata.RadiusData;
import org.jmol.atomdata.RadiusData.EnumType;
import org.jmol.c.VDW;
import org.jmol.i18n.GT;
import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.BondSet;
import org.jmol.modelset.ModelSet;
import org.jmol.script.JmolMathExtension;
import org.jmol.script.SV;
import org.jmol.script.ScriptEval;
import org.jmol.script.ScriptException;
import org.jmol.script.ScriptMathProcessor;
import org.jmol.script.ScriptParam;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.ColorEncoder;
import org.jmol.util.Escape;
import org.jmol.util.Edge;
import org.jmol.util.Parser;
import org.jmol.util.Point3fi;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.SB;

import org.jmol.util.JmolMolecule;
import org.jmol.util.Logger;

import javajs.util.CU;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.PT;
import javajs.util.Quat;
import javajs.util.T3;
import javajs.util.V3;

import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

public class MathExt implements JmolMathExtension {
  
  private Viewer vwr;
  private ScriptEval e;

  public MathExt() {
    // used by Reflection
  }

  @Override
  public JmolMathExtension init(Object se) {
    e = (ScriptEval) se;
    vwr = e.vwr;
    return this;
  }

  ///////////// ScriptMathProcessor extensions ///////////

  
  @Override
  public boolean evaluate(ScriptMathProcessor mp, T op, SV[] args, int tok)
      throws ScriptException {
    switch (tok) {
    case T.abs:
    case T.acos:
    case T.cos:
    case T.now:
    case T.sin:
    case T.sqrt:
      return evaluateMath(mp, args, tok);
    case T.add:
    case T.div:
    case T.mul:
    case T.mul3:
    case T.sub:
    case T.push:
    case T.pop:
      return evaluateList(mp, op.intValue, args);
    case T.array:
    case T.leftsquare:
      return evaluateArray(mp, args, tok == T.leftsquare);
    case T.axisangle:
    case T.quaternion:
      return evaluateQuaternion(mp, args, tok);
    case T.bin:
      return evaluateBin(mp, args);
    case T.cache:
      return evaluateCache(mp, args);
    case T.col:
    case T.row:
      return evaluateRowCol(mp, args, tok);
    case T.color:
      return evaluateColor(mp, args);
    case T.compare:
      return evaluateCompare(mp, args);
    case T.connected:
      return evaluateConnected(mp, args);
    case T.contact:
      return evaluateContact(mp, args);
    case T.cross:
      return evaluateCross(mp, args);
    case T.data:
      return evaluateData(mp, args);
    case T.distance:
    case T.dot:
      if (op.tok == T.propselector)
        return evaluateDot(mp, args, tok, op.intValue);
      //$FALL-THROUGH$
    case T.angle:
    case T.measure:
      return evaluateMeasure(mp, args, op.tok);
    case T.file:
    case T.load:
      return evaluateLoad(mp, args, tok == T.file);
    case T.find:
      return evaluateFind(mp, args);
    case T.format:
    case T.label:
      return evaluateFormat(mp, op.intValue, args, tok == T.format);
    case T.function:
      return evaluateUserFunction(mp, (String) op.value, args, op.intValue,
          op.tok == T.propselector);
    case T.getproperty:
      return evaluateGetProperty(mp, args, op.tok == T.propselector);
    case T.helix:
      return evaluateHelix(mp, args);
    case T.hkl:
    case T.plane:
    case T.intersection:
      return evaluatePlane(mp, args, tok);
    case T.eval:
    case T.javascript:
    case T.script:
    case T.show:
      return evaluateScript(mp, args, tok);
    case T.join:
    case T.split:
    case T.trim:
      return evaluateString(mp, op.intValue, args);
    case T.point:
      return evaluatePoint(mp, args);
    case T.prompt:
      return evaluatePrompt(mp, args);
    case T.random:
      return evaluateRandom(mp, args);
    case T.in:
      return evaluateIn(mp, args);
    case T.replace:
      return evaluateReplace(mp, args);
    case T.search:
    case T.smiles:
    case T.substructure:
      return evaluateSubstructure(mp, args, tok);
    case T.modulation:
      return evaluateModulation(mp, args);
    case T.sort:
    case T.count:
      return evaluateSort(mp, args, tok);
    case T.symop:
      return evaluateSymop(mp, args, op.tok == T.propselector);
      //    case Token.volume:
      //    return evaluateVolume(args);
    case T.tensor:
      return evaluateTensor(mp, args);
    case T.within:
      return evaluateWithin(mp, args);
    case T.write:
      return evaluateWrite(mp, args);
    }
    return false;
  }

  private boolean evaluateArray(ScriptMathProcessor mp, SV[] args,
                                boolean allowMatrix) {
    int len = args.length;
    if (allowMatrix && (len == 4 || len == 3)) {
      boolean isMatrix = true;
      for (int i = 0; i < len && isMatrix; i++)
        isMatrix = (args[i].tok == T.varray && args[i].getList().size() == len);
      if (isMatrix) {
        float[] m = new float[len * len];
        int pt = 0;
        for (int i = 0; i < len && isMatrix; i++) {
          Lst<SV> list = args[i].getList();
          for (int j = 0; j < len; j++) {
            float x = SV.fValue(list.get(j));
            if (Float.isNaN(x)) {
              isMatrix = false;
              break;
            }
            m[pt++] = x;
          }
        }
        if (isMatrix)
          return (len == 3 ? mp.addXM3(M3.newA9(m)) : mp.addXM4(M4.newA16(m)));
      }
    }
    SV[] a = new SV[args.length];
    for (int i = a.length; --i >= 0;)
      a[i] = SV.newT(args[i]);
    return mp.addXAV(a);
  }

  private boolean evaluateBin(ScriptMathProcessor mp, SV[] args)
      throws ScriptException {
    if (args.length != 3)
      return false;
    SV x1 = mp.getX();
    boolean isListf = (x1.tok == T.listf);
    if (!isListf && x1.tok != T.varray)
      return mp.addX(x1);
    float f0 = SV.fValue(args[0]);
    float f1 = SV.fValue(args[1]);
    float df = SV.fValue(args[2]);
    float[] data;
    if (isListf) {
      data = (float[]) x1.value;
    } else {
      Lst<SV> list = x1.getList();
      data = new float[list.size()];
      for (int i = list.size(); --i >= 0;)
        data[i] = SV.fValue(list.get(i));
    }
    int nbins = Math.max((int) Math.floor((f1 - f0) / df + 0.01f), 1);
    int[] array = new int[nbins];
    int nPoints = data.length;
    for (int i = 0; i < nPoints; i++) {
      float v = data[i];
      int bin = (int) Math.floor((v - f0) / df);
      if (bin < 0)
        bin = 0;      
      else if (bin >= nbins)
        bin = nbins - 1;
      array[bin]++;
    }
    return mp.addXAI(array);
  }

  private boolean evaluateCache(ScriptMathProcessor mp, SV[] args) {
    if (args.length > 0)
      return false;
    return mp.addXMap(vwr.cacheList());
  }

  private boolean evaluateColor(ScriptMathProcessor mp, SV[] args) {
    // color("toHSL", {r g b})         # r g b in 0 to 255 scale 
    // color("toRGB", "colorName or hex code") # r g b in 0 to 255 scale 
    // color("toRGB", {h s l})         # h s l in 360, 100, 100 scale 
    // color("rwb")                  # "" for most recently used scheme for coloring by property
    // color("rwb", min, max)        # min/max default to most recent property mapping 
    // color("rwb", min, max, value) # returns color
    // color("$isosurfaceId")        # info for a given isosurface
    // color("$isosurfaceId", value) # color for a given mapped isosurface value
    // color(ptColor1, ptColor2, n, asHSL)

    String colorScheme = (args.length > 0 ? SV.sValue(args[0]) : "");
    boolean isIsosurface = colorScheme.startsWith("$");
    if (args.length == 2 && colorScheme.equalsIgnoreCase("TOHSL"))
      return mp.addXPt(CU.rgbToHSL(P3.newP(args[1].tok == T.point3f ? SV
          .ptValue(args[1])
          : CU.colorPtFromString(args[1].asString(), new P3())), true));
    if (args.length == 2 && colorScheme.equalsIgnoreCase("TORGB")) {
      P3 pt = P3.newP(args[1].tok == T.point3f ? SV.ptValue(args[1]) : CU
          .colorPtFromString(args[1].asString(), new P3()));
      return mp.addXPt(args[1].tok == T.point3f ? CU.hslToRGB(pt) : pt);
    }
    if (args.length == 4 && (args[3].tok == T.on || args[3].tok == T.off)) {
      P3 pt1 = P3.newP(args[0].tok == T.point3f ? SV.ptValue(args[0]) : CU
          .colorPtFromString(args[0].asString(), new P3()));
      P3 pt2 = P3.newP(args[1].tok == T.point3f ? SV.ptValue(args[1]) : CU
          .colorPtFromString(args[1].asString(), new P3()));
      boolean usingHSL = (args[3].tok == T.on);
      if (usingHSL) {
        pt1 = CU.rgbToHSL(pt1, false);
        pt2 = CU.rgbToHSL(pt2, false);
      }

      SB sb = new SB();
      V3 vd = V3.newVsub(pt2, pt1);
      int n = args[2].asInt();
      if (n < 2)
        n = 20;
      vd.scale(1f / (n - 1));
      for (int i = 0; i < n; i++) {
        sb.append(Escape.escapeColor(CU.colorPtToFFRGB(usingHSL ? CU
            .hslToRGB(pt1) : pt1)));
        pt1.add(vd);
      }
      return mp.addXStr(sb.toString());
    }

    ColorEncoder ce = (isIsosurface ? null : vwr
        .cm.getColorEncoder(colorScheme));
    if (!isIsosurface && ce == null)
      return mp.addXStr("");
    float lo = (args.length > 1 ? SV.fValue(args[1]) : Float.MAX_VALUE);
    float hi = (args.length > 2 ? SV.fValue(args[2]) : Float.MAX_VALUE);
    float value = (args.length > 3 ? SV.fValue(args[3]) : Float.MAX_VALUE);
    boolean getValue = (value != Float.MAX_VALUE || lo != Float.MAX_VALUE
        && hi == Float.MAX_VALUE);
    boolean haveRange = (hi != Float.MAX_VALUE);
    if (!haveRange && colorScheme.length() == 0) {
      value = lo;
      float[] range = vwr.getCurrentColorRange();
      lo = range[0];
      hi = range[1];
    }
    if (isIsosurface) {
      // isosurface color scheme      
      String id = colorScheme.substring(1);
      Object[] data = new Object[] { id, null };
      if (!vwr.shm.getShapePropertyData(JC.SHAPE_ISOSURFACE, "colorEncoder",
          data))
        return mp.addXStr("");
      ce = (ColorEncoder) data[1];
    } else {
      ce.setRange(lo, hi, lo > hi);
    }
    Map<String, Object> key = ce.getColorKey();
    if (getValue)
      return mp.addXPt(CU.colorPtFromInt(ce.getArgb(hi == Float.MAX_VALUE ? lo
          : value), null));
    return mp.addX(SV.getVariableMap(key));
  }

  private boolean evaluateCompare(ScriptMathProcessor mp, SV[] args)
      throws ScriptException {
    // compare([{bitset} or {positions}],[{bitset} or {positions}] [,"stddev"])
    // compare({bitset},{bitset}[,"SMARTS"|"SMILES"],smilesString [,"stddev"])
    // returns matrix4f for rotation/translation or stddev
    // compare({bitset},{bitset},"ISOMER")  12.1.5
    // compare({bitset},{bitset},smartsString, "BONDS") 13.1.17
    // compare({bitset},{bitset},"SMILES", "BONDS") 13.3.9
    // compare({bitest},{bitset},"MAP","smilesString")
    // compare({bitset},{bitset},"MAP")

    if (args.length < 2 || args.length > 5)
      return false;
    float stddev;
    String sOpt = SV.sValue(args[args.length - 1]);
    boolean isStdDev = sOpt.equalsIgnoreCase("stddev");
    boolean isIsomer = sOpt.equalsIgnoreCase("ISOMER");
    boolean isBonds = sOpt.equalsIgnoreCase("BONDS");
    boolean isSmiles = (!isIsomer && args.length > (isStdDev ? 3 : 2));
    BS bs1 = (args[0].tok == T.bitset ? (BS) args[0].value : null);
    BS bs2 = (args[1].tok == T.bitset ? (BS) args[1].value : null);
    String smiles1 = (bs1 == null ? SV.sValue(args[0]) : "");
    String smiles2 = (bs2 == null ? SV.sValue(args[1]) : "");
    M4 m = new M4();
    stddev = Float.NaN;
    Lst<P3> ptsA, ptsB;
    if (isSmiles) {
      if (bs1 == null || bs2 == null)
        return false;
    }
    if (isBonds) {
      if (args.length != 4)
        return false;
      smiles1 = SV.sValue(args[2]);
      isSmiles = smiles1.equalsIgnoreCase("SMILES");
      try {
        if (isSmiles)
          smiles1 = vwr.getSmiles(bs1);
      } catch (Exception ex) {
        e.evalError(ex.getMessage(), null);
      }
      float[] data = e.getSmilesExt().getFlexFitList(bs1, bs2, smiles1, !isSmiles);
      return (data == null ? mp.addXStr("") : mp.addXAF(data));
    }
    try {
      if (isIsomer) {
        if (args.length != 3)
          return false;
        if (bs1 == null && bs2 == null)
          return mp.addXStr(vwr.getSmilesMatcher()
              .getRelationship(smiles1, smiles2).toUpperCase());
        String mf1 = (bs1 == null ? vwr.getSmilesMatcher()
            .getMolecularFormula(smiles1, false) : JmolMolecule
            .getMolecularFormula(vwr.ms.at, bs1, false));
        String mf2 = (bs2 == null ? vwr.getSmilesMatcher()
            .getMolecularFormula(smiles2, false) : JmolMolecule
            .getMolecularFormula(vwr.ms.at, bs2, false));
        if (!mf1.equals(mf2))
          return mp.addXStr("NONE");
        if (bs1 != null)
          smiles1 = (String) e.getSmilesExt().getSmilesMatches("", null, bs1, null, false, true);
        boolean check;
        if (bs2 == null) {
          // note: find smiles1 IN smiles2 here
          check = (vwr.getSmilesMatcher().areEqual(smiles2, smiles1) > 0);
        } else {
          check = (((BS) e.getSmilesExt().getSmilesMatches(smiles1, null, bs2, null, false, true))
              .nextSetBit(0) >= 0);
        }
        if (!check) {
          // MF matched, but didn't match SMILES
          String s = smiles1 + smiles2;
          if (s.indexOf("/") >= 0 || s.indexOf("\\") >= 0
              || s.indexOf("@") >= 0) {
            if (smiles1.indexOf("@") >= 0
                && (bs2 != null || smiles2.indexOf("@") >= 0)) {
              // reverse chirality centers
              smiles1 = vwr.getSmilesMatcher().reverseChirality(smiles1);
              if (bs2 == null) {
                check = (vwr.getSmilesMatcher().areEqual(smiles1, smiles2) > 0);
              } else {
                check = (((BS) e.getSmilesExt().getSmilesMatches(smiles1, null, bs2, null,
                    false, true)).nextSetBit(0) >= 0);
              }
              if (check)
                return mp.addXStr("ENANTIOMERS");
            }
            // remove all stereochemistry from SMILES string
            if (bs2 == null) {
              check = (vwr.getSmilesMatcher().areEqual(
                  "/nostereo/" + smiles2, smiles1) > 0);
            } else {
              Object ret = e.getSmilesExt().getSmilesMatches("/nostereo/" + smiles1, null, bs2,
                  null, false, true);
              check = (((BS) ret).nextSetBit(0) >= 0);
            }
            if (check)
              return mp.addXStr("DIASTERIOMERS");
          }
          // MF matches, but not enantiomers or diasteriomers
          return mp.addXStr("CONSTITUTIONAL ISOMERS");
        }
        //identical or conformational 
        if (bs1 == null || bs2 == null)
          return mp.addXStr("IDENTICAL");
        stddev = e.getSmilesExt().getSmilesCorrelation(bs1, bs2, smiles1, null, null, null,
            null, false, false, null, null, false, false);
        return mp.addXStr(stddev < 0.2f ? "IDENTICAL"
            : "IDENTICAL or CONFORMATIONAL ISOMERS (RMSD=" + stddev + ")");
      } else if (isSmiles) {
        ptsA = new Lst<P3>();
        ptsB = new Lst<P3>();
        sOpt = SV.sValue(args[2]);
        boolean isMap = sOpt.equalsIgnoreCase("MAP");
        isSmiles = (sOpt.equalsIgnoreCase("SMILES"));
        boolean isSearch = (isMap || sOpt.equalsIgnoreCase("SMARTS"));
        if (isSmiles || isSearch)
          sOpt = (args.length > 3 ? SV.sValue(args[3]) : null);
        boolean hMaps = (("H".equals(sOpt) || "allH".equals(sOpt) || "bestH"
            .equals(sOpt)));
        boolean allMaps = (("all".equals(sOpt) || "allH".equals(sOpt)));
        boolean bestMap = (("best".equals(sOpt) || "bestH".equals(sOpt)));
        if (sOpt == null || hMaps || allMaps || bestMap) {
          // with explicitH we set to find only the first match.
          if (isMap || isSmiles) {
            sOpt = "/noaromatic"
                + (allMaps || bestMap ? "/" : " nostereo/")
                + e.getSmilesExt().getSmilesMatches((hMaps ? "H" : ""), null, bs1, null, false,
                    true);
          } else {
            return false;
          }
        } else {
          allMaps = true;
        }
        stddev = e.getSmilesExt().getSmilesCorrelation(bs1, bs2, sOpt, ptsA, ptsB, m, null,
            !isSmiles, isMap, null, null, !allMaps && !bestMap, bestMap);
        if (isMap) {
          int nAtoms = ptsA.size();
          if (nAtoms == 0)
            return mp.addXStr("");
          int nMatch = ptsB.size() / nAtoms;
          Lst<int[][]> ret = new Lst<int[][]>();
          for (int i = 0, pt = 0; i < nMatch; i++) {
            int[][] a = AU.newInt2(nAtoms);
            ret.addLast(a);
            for (int j = 0; j < nAtoms; j++, pt++)
              a[j] = new int[] { ((Atom) ptsA.get(j)).i,
                  ((Atom) ptsB.get(pt)).i };
          }
          if (!allMaps)
            return (ret.size() > 0 ? mp.addXAII(ret.get(0)) : mp.addXStr(""));
          return mp.addXList(ret);
        }
      } else {
        ptsA = e.getPointVector(args[0], 0);
        ptsB = e.getPointVector(args[1], 0);
        if (ptsA != null && ptsB != null)
          stddev = Measure.getTransformMatrix4(ptsA, ptsB, m, null);
      }
      return (isStdDev || Float.isNaN(stddev) ? mp.addXFloat(stddev) : mp
          .addXM4(m));
    } catch (Exception ex) {
      e.evalError(ex.getMessage() == null ? ex.toString() : ex.getMessage(),
          null);
      return false;
    }
  }

  private boolean evaluateConnected(ScriptMathProcessor mp, SV[] args) {
    /*
     * Several options here:
     * 
     * connected(1, 3, "single", {carbon})
     * 
     * means "atoms connected to carbon by from 1 to 3 single bonds"
     * and returns an atom set.
     * 
     * connected(1.0, 1.5, "single", {carbon}, {oxygen})
     * 
     * means "single bonds from 1.0 to 1.5 Angstroms between carbon and oxygen"
     * and returns a bond bitset.
     * 
     * connected({*}.bonds, "DOUBLE")
     * 
     * means just that and returns a bond set
     * 
     * 
     */

    if (args.length > 5)
      return false;
    float min = Integer.MIN_VALUE, max = Integer.MAX_VALUE;
    float fmin = 0, fmax = Float.MAX_VALUE;

    int order = Edge.BOND_ORDER_ANY;
    BS atoms1 = null;
    BS atoms2 = null;
    boolean haveDecimal = false;
    boolean isBonds = false;
    for (int i = 0; i < args.length; i++) {
      SV var = args[i];
      switch (var.tok) {
      case T.bitset:
        isBonds = (var.value instanceof BondSet);
        if (isBonds && atoms1 != null)
          return false;
        if (atoms1 == null)
          atoms1 = SV.bsSelectVar(var);
        else if (atoms2 == null)
          atoms2 = SV.bsSelectVar(var);
        else
          return false;
        break;
      case T.string:
        String type = SV.sValue(var);
        if (type.equalsIgnoreCase("hbond"))
          order = Edge.BOND_HYDROGEN_MASK;
        else
          order = ScriptParam.getBondOrderFromString(type);
        if (order == Edge.BOND_ORDER_NULL)
          return false;
        break;
      case T.decimal:
        haveDecimal = true;
        //$FALL-THROUGH$
      default:
        int n = var.asInt();
        float f = var.asFloat();
        if (max != Integer.MAX_VALUE)
          return false;

        if (min == Integer.MIN_VALUE) {
          min = Math.max(n, 0);
          fmin = f;
        } else {
          max = n;
          fmax = f;
        }
      }
    }
    if (min == Integer.MIN_VALUE) {
      min = 1;
      max = 100;
      fmin = JC.DEFAULT_MIN_CONNECT_DISTANCE;
      fmax = JC.DEFAULT_MAX_CONNECT_DISTANCE;
    } else if (max == Integer.MAX_VALUE) {
      max = min;
      fmax = fmin;
      fmin = JC.DEFAULT_MIN_CONNECT_DISTANCE;
    }
    if (atoms1 == null)
      atoms1 = vwr.getAllAtoms();
    if (haveDecimal && atoms2 == null)
      atoms2 = atoms1;
    if (atoms2 != null) {
      BS bsBonds = new BS();
      vwr.makeConnections(fmin, fmax, order, T.identify, atoms1, atoms2,
          bsBonds, isBonds, false, 0);
      return mp.addX(SV.newV(
          T.bitset,
          new BondSet(bsBonds, vwr.ms.getAtomIndices(vwr.ms.getAtoms(
              T.bonds, bsBonds)))));
    }
    return mp.addXBs(vwr.ms.getAtomsConnected(min, max, order, atoms1));
  }

  private boolean evaluateContact(ScriptMathProcessor mp, SV[] args) {
    if (args.length < 1 || args.length > 3)
      return false;
    int i = 0;
    float distance = 100;
    int tok = args[0].tok;
    switch (tok) {
    case T.decimal:
    case T.integer:
      distance = SV.fValue(args[i++]);
      break;
    case T.bitset:
      break;
    default:
      return false;
    }
    if (i == args.length || !(args[i].value instanceof BS))
      return false;
    BS bsA = BSUtil.copy(SV.bsSelectVar(args[i++]));
    BS bsB = (i < args.length ? BSUtil.copy(SV.bsSelectVar(args[i])) : null);
    RadiusData rd = new RadiusData(null, (distance > 10 ? distance / 100
        : distance), (distance > 10 ? EnumType.FACTOR : EnumType.OFFSET),
        VDW.AUTO);
    bsB = setContactBitSets(bsA, bsB, true, Float.NaN, rd, false);
    bsB.or(bsA);
    return mp.addXBs(bsB);
  }

  private boolean evaluateCross(ScriptMathProcessor mp, SV[] args) {
    if (args.length != 2)
      return false;
    SV x1 = args[0];
    SV x2 = args[1];
    if (x1.tok != T.point3f || x2.tok != T.point3f)
      return false;
    V3 a = V3.newV((P3) x1.value);
    V3 b = V3.newV((P3) x2.value);
    a.cross(a, b);
    return mp.addXPt(P3.newP(a));
  }

  private boolean evaluateData(ScriptMathProcessor mp, SV[] args) {

    // x = data("somedataname") # the data
    // x = data("data2d_xxxx") # 2D data (x,y paired values)
    // x = data("data2d_xxxx", iSelected) # selected row of 2D data, with <=0
    // meaning "relative to the last row"
    // x = data("property_x", "property_y") # array mp.addition of two property
    // sets
    // x = data({atomno < 10},"xyz") # (or "pdb" or "mol") coordinate data in
    // xyz, pdb, or mol format
    // x = data(someData,ptrFieldOrColumn,nBytes,firstLine) # extraction of a
    // column of data based on a field (nBytes = 0) or column range (nBytes >
    // 0)
    if (args.length != 1 && args.length != 2 && args.length != 4)
      return false;
    String selected = SV.sValue(args[0]);
    String type = (args.length == 2 ? SV.sValue(args[1]) : "");

    if (args.length == 4) {
      int iField = args[1].asInt();
      int nBytes = args[2].asInt();
      int firstLine = args[3].asInt();
      float[] f = Parser.parseFloatArrayFromMatchAndField(selected, null, 0, 0,
          null, iField, nBytes, null, firstLine);
      return mp.addXStr(Escape.escapeFloatA(f, false));
    }

    if (selected.indexOf("data2d_") == 0) {
      // tab, newline separated data
      float[][] f1 = vwr.getDataFloat2D(selected);
      if (f1 == null)
        return mp.addXStr("");
      if (args.length == 2 && args[1].tok == T.integer) {
        int pt = args[1].intValue;
        if (pt < 0)
          pt += f1.length;
        if (pt >= 0 && pt < f1.length)
          return mp.addXStr(Escape.escapeFloatA(f1[pt], false));
        return mp.addXStr("");
      }
      return mp.addXStr(Escape.escapeFloatAA(f1, false));
    }

    // parallel mp.addition of float property data sets

    if (selected.indexOf("property_") == 0) {
      float[] f1 = vwr.getDataFloat(selected);
      if (f1 == null)
        return mp.addXStr("");
      float[] f2 = (type.indexOf("property_") == 0 ? vwr.getDataFloat(type)
          : null);
      if (f2 != null) {
        f1 = AU.arrayCopyF(f1, -1);
        for (int i = Math.min(f1.length, f2.length); --i >= 0;)
          f1[i] += f2[i];
      }
      return mp.addXStr(Escape.escapeFloatA(f1, false));
    }

    // some other data type -- just return it

    if (args.length == 1) {
      Object[] data = vwr.getData(selected);
      return mp.addXStr(data == null ? "" : "" + data[1]);
    }
    // {selected atoms} XYZ, MOL, PDB file format
    return mp.addXStr(vwr.getData(selected, type));
  }

  private boolean evaluateDot(ScriptMathProcessor mp, SV[] args, int tok,
                              int intValue) throws ScriptException {
    // distance and dot
    switch (args.length) {
    case 1:
      if (tok == T.dot)
        return false;
      //$FALL-THROUGH$
    case 2:
      break;
    default:
      return false;
    }
    SV x1 = mp.getX();
    SV x2 = args[0];
    P3 pt2 = (x2.tok == T.varray ? null : mp.ptValue(x2, false));
    P4 plane2 = mp.planeValue(x2);
    if (tok == T.distance) {
      int minMax = intValue & T.minmaxmask;
      boolean isMinMax = (minMax == T.min || minMax == T.max);
      boolean isAll = minMax == T.minmaxmask;
      switch (x1.tok) {
      case T.bitset:
        BS bs = SV.bsSelectVar(x1);
        BS bs2 = null;
        boolean returnAtom = (isMinMax && args.length == 2 && args[1]
            .asBoolean());
        switch (x2.tok) {
        case T.bitset:
          bs2 = (x2.tok == T.bitset ? SV.bsSelectVar(x2) : null);
          //$FALL-THROUGH$
        case T.point3f:
          Atom[] atoms = vwr.ms.at;
          if (returnAtom) {
            float dMinMax = Float.NaN;
            int iMinMax = Integer.MAX_VALUE;
            for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
              float d = (bs2 == null ? atoms[i].distanceSquared(pt2)
                  : ((Float) e.getBitsetProperty(bs2, intValue, atoms[i],
                      plane2, x1.value, null, false, x1.index, false))
                      .floatValue());
              if (minMax == T.min ? d >= dMinMax : d <= dMinMax)
                continue;
              dMinMax = d;
              iMinMax = i;
            }
            return mp.addXBs(iMinMax == Integer.MAX_VALUE ? new BS() : BSUtil
                .newAndSetBit(iMinMax));
          }
          if (isAll) {
            if (bs2 == null) {
              float[] data = new float[bs.cardinality()];
              for (int p = 0, i = bs.nextSetBit(0); i >= 0; i = bs
                  .nextSetBit(i + 1), p++)
                data[p] = atoms[i].distance(pt2);
              return mp.addXAF(data);
            }
            float[][] data2 = new float[bs.cardinality()][bs2.cardinality()];
            for (int p = 0, i = bs.nextSetBit(0); i >= 0; i = bs
                .nextSetBit(i + 1), p++)
              for (int q = 0, j = bs2.nextSetBit(0); j >= 0; j = bs2
                  .nextSetBit(j + 1), q++)
                data2[p][q] = atoms[i].distance(atoms[j]);
            return mp.addXAFF(data2);
          }
          if (isMinMax) {
            float[] data = new float[bs.cardinality()];
            for (int i = bs.nextSetBit(0), p = 0; i >= 0; i = bs
                .nextSetBit(i + 1))
              data[p++] = ((Float) e.getBitsetProperty(bs2, intValue,
                  atoms[i], plane2, x1.value, null, false, x1.index, false))
                  .floatValue();
            return mp.addXAF(data);
          }
          return mp.addXObj(e.getBitsetProperty(bs, intValue, pt2, plane2,
              x1.value, null, false, x1.index, false));
        }
      }
    }
    return mp.addXFloat(getDistance(mp, x1, x2, tok));
  }

  private boolean evaluateHelix(ScriptMathProcessor mp, SV[] args)
      throws ScriptException {
    if (args.length < 1 || args.length > 5)
      return false;
    // helix({resno=3})
    // helix({resno=3},"point|axis|radius|angle|draw|measure|array")
    // helix(resno,"point|axis|radius|angle|draw|measure|array")
    // helix(pt1, pt2, dq, "point|axis|radius|angle|draw|measure|array|")
    // helix(pt1, pt2, dq, "draw","someID")
    // helix(pt1, pt2, dq)
    int pt = (args.length > 2 ? 3 : 1);
    String type = (pt >= args.length ? "array" : SV.sValue(args[pt]));
    int tok = T.getTokFromName(type);
    if (args.length > 2) {
      // helix(pt1, pt2, dq ...)
      P3 pta = mp.ptValue(args[0], true);
      P3 ptb = mp.ptValue(args[1], true);
      if (tok == T.nada || args[2].tok != T.point4f)
        return false;
      Quat dq = Quat.newP4((P4) args[2].value);
      T3[] data = Measure.computeHelicalAxis(pta, ptb, dq);
      //new T3[] { pt_a_prime, n, r, P3.new3(theta, pitch, residuesPerTurn) };
      return (data == null ? false : mp.addXObj(Escape.escapeHelical(type, tok,
          pta, ptb, data)));
    }
    BS bs = (args[0].value instanceof BS ? (BS) args[0].value : vwr.ms
        .getAtoms(T.resno, new Integer(args[0].asInt())));
    switch (tok) {
    case T.point:
      return mp.addXObj(getHelixData(bs, T.point));
    case T.axis:
      return mp.addXObj(getHelixData(bs, T.axis));
    case T.radius:
      return mp.addXObj(getHelixData(bs, T.radius));
    case T.angle:
      return mp.addXFloat(((Float) getHelixData(bs, T.angle)).floatValue());
    case T.draw:
    case T.measure:
      return mp.addXObj(getHelixData(bs, tok));
    case T.array:
      String[] data = (String[]) getHelixData(bs, T.list);
      if (data == null)
        return false;
      return mp.addXAS(data);
    }
    return false;
  }

  private Object getHelixData(BS bs, int tokType) {
    int iAtom = bs.nextSetBit(0);
    return (iAtom < 0 ? "null"
        : vwr.ms.at[iAtom].group.getHelixData(tokType, 
        vwr.getQuaternionFrame(), vwr.getInt(T.helixstep)));
  }
  
  private boolean evaluateFind(ScriptMathProcessor mp, SV[] args)
      throws ScriptException {
    if (args.length == 0)
      return false;

    // {*}.find("MF")
    // {*}.find("SEQENCE")
    // {*}.find("SMARTS", "CCCC")
    // "CCCC".find("SMARTS", "CC")
    // "CCCC".find("SMILES", "MF")
    // {2.1}.find("CCCC",{1.1}) // find pattern "CCCC" in {2.1} with conformation given by {1.1}
    // {*}.find("ccCCN","BONDS")
    // {*}.find("SMILES","H")
    // {*}.find("chemical",type)

    SV x1 = mp.getX();
    String sFind = SV.sValue(args[0]);
    String flags = (args.length > 1 && args[1].tok != T.on
        && args[1].tok != T.off ? SV.sValue(args[1]) : "");
    boolean isSequence = sFind.equalsIgnoreCase("SEQUENCE");
    boolean isSmiles = sFind.equalsIgnoreCase("SMILES");
    boolean isSearch = sFind.equalsIgnoreCase("SMARTS");
    boolean isChemical = sFind.equalsIgnoreCase("CHEMICAL");
    boolean isMF = sFind.equalsIgnoreCase("MF");
    try {
      if (isChemical) {
        String data = (x1.tok == T.bitset ? vwr.getSmiles(SV.getBitSet(x1, false)) : SV.sValue(x1));
        data = data.length() == 0 ? "" : vwr.getChemicalInfo(data, args.length > 1 ? T.getTokenFromName(flags.toLowerCase()) : null);
        if (data.endsWith("\n"))
          data = data.substring(0, data.length() - 1);
        if (data.startsWith("InChI"))
          data = PT.rep(PT.rep(data, "InChI=", ""), "InChIKey=", "");
        return mp.addXStr(data);
      }
      if (isSmiles || isSearch || x1.tok == T.bitset) {
        int iPt = (isSmiles || isSearch ? 2 : 1);
        BS bs2 = (iPt < args.length && args[iPt].tok == T.bitset ? (BS) args[iPt++].value
            : null);
        boolean asBonds = ("bonds".equalsIgnoreCase(SV
            .sValue(args[args.length - 1])));
        boolean isAll = (asBonds || args[args.length - 1].tok == T.on);
        Object ret = null;
        switch (x1.tok) {
        case T.string:
          String smiles = SV.sValue(x1);
          if (bs2 != null)
            return false;
          if (flags.equalsIgnoreCase("mf")) {
            ret = vwr.getSmilesMatcher().getMolecularFormula(smiles,
                isSearch);
          } else {
            ret = e.getSmilesExt().getSmilesMatches(flags, smiles, null, null, isSearch, !isAll);
          }
          break;
        case T.bitset:
          if (isMF)
            return mp.addXStr(JmolMolecule.getMolecularFormula(
                vwr.ms.at, (BS) x1.value, false));
          if (isSequence)
            return mp.addXStr(vwr.getSmilesOpt((BS) x1.value, -1, -1, false,
                true, isAll, isAll, false));
          if (isSmiles || isSearch)
            sFind = flags;
          BS bsMatch3D = bs2;
          if (asBonds) {
            // this will return a single match
            int[][] map = vwr.getSmilesMatcher().getCorrelationMaps(sFind,
                vwr.ms.at, vwr.getAtomCount(), (BS) x1.value,
                !isSmiles, true);
            ret = (map.length > 0 ? vwr.getDihedralMap(map[0]) : new int[0]);
          } else {
            ret = e.getSmilesExt().getSmilesMatches(sFind, null, (BS) x1.value, bsMatch3D,
                !isSmiles, !isAll);
          }
          break;
        }
        if (ret == null)
          e.invArg();
        return mp.addXObj(ret);
      }
    } catch (Exception ex) {
      e.evalError(ex.getMessage(), null);
    }
    boolean isReverse = (flags.indexOf("v") >= 0);
    boolean isCaseInsensitive = (flags.indexOf("i") >= 0);
    boolean asMatch = (flags.indexOf("m") >= 0);
    boolean isList = (x1.tok == T.varray);
    boolean isPattern = (args.length == 2);
    if (isList || isPattern) {
      JmolPatternMatcher pm = getPatternMatcher();
      Pattern pattern = null;
      try {
        pattern = pm.compile(sFind, isCaseInsensitive);
      } catch (Exception ex) {
        e.evalError(ex.toString(), null);
      }
      String[] list = SV.strListValue(x1);
      if (Logger.debugging)
        Logger.debug("finding " + sFind);
      BS bs = new BS();
      int ipt = 0;
      int n = 0;
      Matcher matcher = null;
      Lst<String> v = (asMatch ? new Lst<String>() : null);
      for (int i = 0; i < list.length; i++) {
        String what = list[i];
        matcher = pattern.matcher(what);
        boolean isMatch = matcher.find();
        if (asMatch && isMatch || !asMatch && isMatch == !isReverse) {
          n++;
          ipt = i;
          bs.set(i);
          if (asMatch)
            v.addLast(isReverse ? what.substring(0, matcher.start())
                + what.substring(matcher.end()) : matcher.group());
        }
      }
      if (!isList) {
        return (asMatch ? mp.addXStr(v.size() == 1 ? (String) v.get(0) : "")
            : isReverse ? mp.addXBool(n == 1) : asMatch ? mp
                .addXStr(n == 0 ? "" : matcher.group()) : mp.addXInt(n == 0 ? 0
                : matcher.start() + 1));
      }
      if (n == 1)
        return mp.addXStr(asMatch ? (String) v.get(0) : list[ipt]);
      String[] listNew = new String[n];
      if (n > 0)
        for (int i = list.length; --i >= 0;)
          if (bs.get(i)) {
            --n;
            listNew[n] = (asMatch ? (String) v.get(n) : list[i]);
          }
      return mp.addXAS(listNew);
    }
    return mp.addXInt(SV.sValue(x1).indexOf(sFind) + 1);
  }

  private boolean evaluateGetProperty(ScriptMathProcessor mp, SV[] args,
                                      boolean isAtomProperty)
      throws ScriptException {
    int pt = 0;
    int tok = (args.length == 0 ? T.nada : args[0].tok);
    if (args.length == 2 && (tok == T.varray || tok == T.hash || tok == T.context)) {
      return mp.addXObj(vwr.extractProperty(args[0].value, args[1].value.toString(), -1));
    }
    String propertyName = (args.length > 0 ? SV.sValue(args[pt++]) : "");
    String lc = propertyName.toLowerCase();
    if (lc.indexOf("[select ") < 0)
      propertyName = lc;
    boolean isJSON = false;
    if (propertyName.equals("json") && args.length > pt) {
      isJSON = true;
      propertyName = SV.sValue(args[pt++]);
    }

    if (propertyName.startsWith("$")) {
      // TODO
    }
    if (isAtomProperty && !propertyName.equalsIgnoreCase("bondInfo"))
      propertyName = "atomInfo." + propertyName;
    Object propertyValue = "";
    if (propertyName.equalsIgnoreCase("fileContents") && args.length > 2) {
      String s = SV.sValue(args[1]);
      for (int i = 2; i < args.length; i++)
        s += "|" + SV.sValue(args[i]);
      propertyValue = s;
      pt = args.length;
    } else if (args.length > pt) {
      switch (args[pt].tok) {
      case T.bitset:
        propertyValue = SV.bsSelectVar(args[pt++]);
        if (propertyName.equalsIgnoreCase("bondInfo") && args.length > pt
            && args[pt].tok == T.bitset)
          propertyValue = new BS[] { (BS) propertyValue,
              SV.bsSelectVar(args[pt]) };
        break;
      case T.string:
        if (vwr.checkPropertyParameter(propertyName))
          propertyValue = args[pt++].value;
        break;
      }
    }
    if (isAtomProperty) {
      SV x = mp.getX();
      if (x.tok != T.bitset)
        return false;
      int iAtom = SV.bsSelectVar(x).nextSetBit(0);
      if (iAtom < 0)
        return mp.addXStr("");
      propertyValue = BSUtil.newAndSetBit(iAtom);
    }
    Object property = vwr.getProperty(null, propertyName, propertyValue);
    if (pt < args.length)
      property = vwr.extractProperty(property, args, pt);
    if (isAtomProperty && property instanceof Lst)
      property = (((Lst<?>) property).size() > 0 ? ((Lst<?>) property).get(0)
          : "");
    return mp.addXObj(isJSON ? "{" + PT.toJSON("value", property) + "}" : SV
        .isVariableType(property) ? property : Escape.toReadable(propertyName,
        property));
  }

  private boolean evaluateFormat(ScriptMathProcessor mp, int intValue,
                                 SV[] args, boolean isLabel)
      throws ScriptException {
    // NOT {xxx}.label
    // {xxx}.label("....")
    // {xxx}.yyy.format("...")
    // (value).format("...")
    // format("....",a,b,c...)
    // format("....",[a1, a2, a3, a3....])
    // format("base64", x)
    // format("JSON", x)
    // format("byteArray", x)
    // format("array", x)
    SV x1 = (args.length < 2 ? mp.getX() : null);
    String format = (args.length == 0 ? "%U" : SV.sValue(args[0]));
    if (x1 == null) {
      int pt = (isLabel ? -1 : SV.getFormatType(format));
      if (pt >= 0 && args.length != 2)
        return false;
      if (pt >= 0 || args.length < 2 || args[1].tok != T.varray)
        return mp.addXObj(SV.format(args, pt));
      // fill an array with applied formats
      Lst<SV> a = args[1].getList();
      SV[] args2 = new SV[] { args[0], null };
      String[] sa = new String[a.size()];
      for (int i = sa.length; --i >= 0;) {
        args2[1] = a.get(i);
        sa[i] = SV.format(args2, pt).toString();
      }
      return mp.addXAS(sa);
    }
    BS bs = SV.getBitSet(x1, true);
    boolean asArray = T.tokAttr(intValue, T.minmaxmask); // "all"
    return mp.addXObj(bs == null ? SV.sprintf(PT.formatCheck(format), x1) : e
        .getCmdExt().getBitsetIdent(bs, format, x1.value, true, x1.index,
            asArray));
  }

  /**
   * array.add(x) array.add(sep, x) array.sub(x) array.mul(x) array.mul3(x)
   * array.div(x) array.push() array.pop()
   * 
   * @param mp
   * @param tok
   * @param args
   * @return T/F
   * @throws ScriptException
   */
  private boolean evaluateList(ScriptMathProcessor mp, int tok, SV[] args)
      throws ScriptException {
    int len = args.length;
    SV x1 = mp.getX();
    boolean isArray1 = (x1.tok == T.varray);
    SV x2;
    switch (tok) {
    case T.push:
      return (len == 2 && mp.addX(x1.pushPop(args[1], args[0])) || len == 1 && mp.addX(x1.pushPop(args[0], null)));
    case T.pop:
      return (len == 1 && mp.addX(x1.pushPop(null, args[0])) || len == 0 && mp.addX(x1.pushPop(null, null)));
    case T.add:
      if (len != 1 && len != 2)
        return false;
      break;
    case T.join:
      break;
    default:
      if (len != 1)
        return false;
    }
    String[] sList1 = null, sList2 = null, sList3 = null;

    if (len == 2) {
      // [xxxx].add("\t", [...])
      int itab = (args[0].tok == T.string ? 0 : 1);
      String tab = SV.sValue(args[itab]);
      sList1 = (isArray1 ? SV.strListValue(x1) : PT.split(SV.sValue(x1),
          "\n"));
      x2 = args[1 - itab];
      sList2 = (x2.tok == T.varray ? SV.strListValue(x2) : PT.split(SV.sValue(x2),
          "\n"));
      sList3 = new String[len = Math.max(sList1.length, sList2.length)];
      for (int i = 0; i < len; i++)
        sList3[i] = (i >= sList1.length ? "" : sList1[i]) + tab
            + (i >= sList2.length ? "" : sList2[i]);
      return mp.addXAS(sList3);
    }
    x2 = (len == 0 ? SV.newV(T.all, "all") : args[0]);
    boolean isAll = (x2.tok == T.all);
    if (!isArray1 && x1.tok != T.string)
      return mp.binaryOp(opTokenFor(tok), x1, x2);
    boolean isScalar1 = SV.isScalar(x1);
    boolean isScalar2 = SV.isScalar(x2);

    float[] list1 = null;
    float[] list2 = null;
    Lst<SV> alist1 = x1.getList();
    Lst<SV> alist2 = x2.getList();

    if (isArray1) {
      len = alist1.size();
    } else if (isScalar1) {
      len = Integer.MAX_VALUE;
    } else {
      sList1 = (PT.split(SV.sValue(x1), "\n"));
      list1 = new float[len = sList1.length];
      PT.parseFloatArrayData(sList1, list1);
    }
    if (isAll && tok != T.join) {
      float sum = 0f;
      if (isArray1) {
        for (int i = len; --i >= 0;)
          sum += SV.fValue(alist1.get(i));
      } else if (!isScalar1) {
        for (int i = len; --i >= 0;)
          sum += list1[i];
      }
      return mp.addXFloat(sum);
    }
    if (tok == T.join && x2.tok == T.string) {
      SB sb = new SB();
      if (isScalar1) {
        sb.append(SV.sValue(x1));
      } else {
        String s = (isAll ? "" : x2.value.toString());
        for (int i = 0; i < len; i++)
          sb.append(i > 0 ? s : "").append(SV.sValue(alist1.get(i)));
      }
      return mp.addXStr(sb.toString());
    }

    SV scalar = null;
    if (isScalar2) {
      scalar = x2;
    } else if (x2.tok == T.varray) {
      len = Math.min(len, alist2.size());
    } else {
      sList2 = PT.split(SV.sValue(x2), "\n");
      list2 = new float[sList2.length];
      PT.parseFloatArrayData(sList2, list2);
      len = Math.min(len, list2.length);
    }

    T token = opTokenFor(tok);

    SV[] olist = new SV[len];
    if (isArray1 && isAll) {
      Lst<SV>  llist = new Lst<SV>();
      return mp.addXList(addAllLists(x1.getList(), llist));
    }
    SV a = (isScalar1 ? x1 : null);
    SV b;
    for (int i = 0; i < len; i++) {
      if (isScalar2)
        b = scalar;
      else if (x2.tok == T.varray)
        b = alist2.get(i);
      else if (Float.isNaN(list2[i]))
        b = SV.getVariable(SV.unescapePointOrBitsetAsVariable(sList2[i]));
      else
        b = SV.newV(T.decimal, Float.valueOf(list2[i]));
      if (!isScalar1) {
        if (isArray1)
          a = alist1.get(i);
        else if (Float.isNaN(list1[i]))
          a = SV.getVariable(SV.unescapePointOrBitsetAsVariable(sList1[i]));
        else
          a = SV.newV(T.decimal, Float.valueOf(list1[i]));
      }
      if (tok == T.join) {
        if (a.tok != T.varray) {
          Lst<SV> l = new Lst<SV>();
          l.addLast(a);
          a = SV.getVariableList(l);
        }
      }
      if (!mp.binaryOp(token, a, b))
        return false;
      olist[i] = mp.getX();
    }
    return mp.addXAV(olist);
  }

  private Lst<SV> addAllLists(Lst<SV> list, Lst<SV> l) {
    int n = list.size(); 
    for (int i = 0; i < n; i++) {
      SV v = list.get(i);
      if (v.tok == T.varray)
        addAllLists(v.getList(), l);
      else
        l.addLast(v);
    }
    return l;
  }

  private boolean evaluateLoad(ScriptMathProcessor mp, SV[] args, boolean isFile)
      throws ScriptException {
    // file("myfile.xyz")
    // load("myfile.png",true)
    // load("myfile.txt",1000)
    // load("myfil.xyz",0,true)

    if (args.length > 3 || args.length < 1)
      return false;
    int nBytesMax = -1;
    boolean asBytes = false;
    boolean async = false;
    switch (args.length) {
    case 3:
      async = SV.bValue(args[2]);
      //$FALL-THROUGH$
    case 2:
      nBytesMax = (args[1].tok == T.integer ? args[1].asInt() : -1);
      asBytes = args[1].tok == T.on;
      break;
    default:
      return false;
    }
    String file = SV.sValue(args[0]);
    file = file.replace('\\', '/');
    if (asBytes)
      return mp.addXMap(vwr.getFileAsMap(file));
    boolean isQues = file.startsWith("?");
    if (vwr.isJS && (isQues || async)) {
      if (isFile && isQues)
        return mp.addXStr("");
      file = e.loadFileAsync("load()_", file, mp.oPt, true);
      // A ScriptInterrupt will be thrown, and an asynchronous
      // file load will initiate, which will return to the script 
      // at this command when the load operation has completed.
      // Note that we need to have just a simple command here.
      // The evaluation will be repeated up to this point, so for example,
      // x = (i++) + load("?") would increment i twice.
    }
    return mp.addXStr(isFile ? vwr.getFilePath(file, false) : vwr
        .getFileAsString4(file, nBytesMax, false, false, true));
  }

  private boolean evaluateMath(ScriptMathProcessor mp, SV[] args, int tok) {
    if (tok == T.now) {
      if (args.length == 1 && args[0].tok == T.string)
        return mp.addXStr((new Date()) + "\t" + SV.sValue(args[0]));
      return mp.addXInt(((int) System.currentTimeMillis() & 0x7FFFFFFF)
          - (args.length == 0 ? 0 : args[0].asInt()));
    }
    if (args.length != 1)
      return false;
    if (tok == T.abs) {
      if (args[0].tok == T.integer)
        return mp.addXInt(Math.abs(args[0].asInt()));
      return mp.addXFloat(Math.abs(args[0].asFloat()));
    }
    double x = SV.fValue(args[0]);
    switch (tok) {
    case T.acos:
      return mp.addXFloat((float) (Math.acos(x) * 180 / Math.PI));
    case T.cos:
      return mp.addXFloat((float) Math.cos(x * Math.PI / 180));
    case T.sin:
      return mp.addXFloat((float) Math.sin(x * Math.PI / 180));
    case T.sqrt:
      return mp.addXFloat((float) Math.sqrt(x));
    }
    return false;
  }

  //  private boolean evaluateVolume(ScriptVariable[] args) throws ScriptException {
  //    ScriptVariable x1 = mp.getX();
  //    if (x1.tok != Token.bitset)
  //      return false;
  //    String type = (args.length == 0 ? null : ScriptVariable.sValue(args[0]));
  //    return mp.addX(vwr.getVolume((BitSet) x1.value, type));
  //  }

  private boolean evaluateMeasure(ScriptMathProcessor mp, SV[] args, int tok)
      throws ScriptException {
    int nPoints = 0;
    switch (tok) {
    case T.measure:
      // note: min/max are always in Angstroms
      // note: order is not important (other than min/max)
      // measure({a},{b},{c},{d}, min, max, format, units)
      // measure({a},{b},{c}, min, max, format, units)
      // measure({a},{b}, min, max, format, units)
      // measure({a},{b},{c},{d}, min, max, format, units)
      // measure({a} {b} "minArray") -- returns array of minimum distance values

      Lst<Object> points = new Lst<Object>();
      float[] rangeMinMax = new float[] { Float.MAX_VALUE, Float.MAX_VALUE };
      String strFormat = null;
      String units = null;
      boolean isAllConnected = false;
      boolean isNotConnected = false;
      int rPt = 0;
      boolean isNull = false;
      RadiusData rd = null;
      int nBitSets = 0;
      float vdw = Float.MAX_VALUE;
      boolean asMinArray = false;
      boolean asArray = false;
      for (int i = 0; i < args.length; i++) {
        switch (args[i].tok) {
        case T.bitset:
          BS bs = (BS) args[i].value;
          if (bs.length() == 0)
            isNull = true;
          points.addLast(bs);
          nPoints++;
          nBitSets++;
          break;
        case T.point3f:
          Point3fi v = new Point3fi();
          v.setT((P3) args[i].value);
          points.addLast(v);
          nPoints++;
          break;
        case T.integer:
        case T.decimal:
          rangeMinMax[rPt++ % 2] = SV.fValue(args[i]);
          break;

        case T.string:
          String s = SV.sValue(args[i]);
          if (s.equalsIgnoreCase("vdw") || s.equalsIgnoreCase("vanderwaals"))
            vdw = (i + 1 < args.length && args[i + 1].tok == T.integer ? args[++i]
                .asInt() : 100) / 100f;
          else if (s.equalsIgnoreCase("notConnected"))
            isNotConnected = true;
          else if (s.equalsIgnoreCase("connected"))
            isAllConnected = true;
          else if (s.equalsIgnoreCase("minArray"))
            asMinArray = (nBitSets >= 1);
          else if (s.equalsIgnoreCase("asArray"))
            asArray = (nBitSets >= 1);
          else if (PT.isOneOf(s.toLowerCase(),
              ";nm;nanometers;pm;picometers;angstroms;ang;au;")
              || s.endsWith("hz"))
            units = s.toLowerCase();
          else
            strFormat = nPoints + ":" + s;
          break;
        default:
          return false;
        }
      }
      if (nPoints < 2 || nPoints > 4 || rPt > 2 || isNotConnected
          && isAllConnected)
        return false;
      if (isNull)
        return mp.addXStr("");
      if (vdw != Float.MAX_VALUE && (nBitSets != 2 || nPoints != 2))
        return mp.addXStr("");
      rd = (vdw == Float.MAX_VALUE ? new RadiusData(rangeMinMax, 0, null, null)
          : new RadiusData(null, vdw, EnumType.FACTOR, VDW.AUTO));
      return mp.addXObj((vwr.newMeasurementData(null, points)).set(0, null, rd,
          strFormat, units, null, isAllConnected, isNotConnected, null, true,
          0, (short) 0, null).getMeasurements(asArray, asMinArray));
    case T.angle:
      if ((nPoints = args.length) != 3 && nPoints != 4)
        return false;
      break;
    default: // distance
      if ((nPoints = args.length) != 2)
        return false;
    }
    P3[] pts = new P3[nPoints];
    for (int i = 0; i < nPoints; i++)
      pts[i] = mp.ptValue(args[i], true);
    switch (nPoints) {
    case 2:
      return mp.addXFloat(pts[0].distance(pts[1]));
    case 3:
      return mp
          .addXFloat(Measure.computeAngleABC(pts[0], pts[1], pts[2], true));
    case 4:
      return mp.addXFloat(Measure.computeTorsion(pts[0], pts[1], pts[2],
          pts[3], true));
    }
    return false;
  }

  private boolean evaluateModulation(ScriptMathProcessor mp, SV[] args)
      throws ScriptException {
    String type = "D";
    float t = Float.NaN;
    P3 t456 = null;
    int pt = -1;
    switch (args.length) {
    case 0:
      break;
    case 1:
      pt = 0;
      break;
    case 2:
      type = SV.sValue(args[0]).toUpperCase();
      t = SV.fValue(args[1]);
      break;
    default:
      return false;
    }
    if (pt >= 0) {
      if (args[pt].tok == T.point3f)
        t456 = (P3) args[pt].value;
      else
        t = SV.fValue(args[pt]);
    }
    if (t456 == null && t < 1e6)
      t456 = P3.new3(t, t, t);
    BS bs = SV.getBitSet(mp.getX(), false);
    return mp.addXList(vwr.ms.getModulationList(bs, type, t456));
  }

  private boolean evaluatePlane(ScriptMathProcessor mp, SV[] args, int tok)
      throws ScriptException {
    if (tok == T.hkl && args.length != 3 || tok == T.intersection
        && args.length != 2 && args.length != 3 || args.length == 0
        || args.length > 4)
      return false;
    P3 pt1, pt2, pt3;
    P4 plane;
    V3 norm, vTemp;

    switch (args.length) {
    case 1:
      if (args[0].tok == T.bitset) {
        BS bs = SV.getBitSet(args[0], false);
        if (bs.cardinality() == 3) {
          Lst<P3> pts = vwr.ms.getAtomPointVector(bs);
          V3 vNorm = new V3();
          V3 vAB = new V3();
          V3 vAC = new V3();
          plane = new P4();
          Measure.getPlaneThroughPoints(pts.get(0), pts.get(1), pts.get(2),
              vNorm, vAB, vAC, plane);
          return mp.addXPt4(plane);
        }
      }
      Object pt = Escape.uP(SV.sValue(args[0]));
      if (pt instanceof P4)
        return mp.addXPt4((P4) pt);
      return mp.addXStr("" + pt);
    case 2:
      if (tok == T.intersection) {
        // intersection(plane, plane)
        // intersection(point, plane)
        if (args[1].tok != T.point4f)
          return false;
        pt3 = new P3();
        norm = new V3();
        vTemp = new V3();

        plane = (P4) args[1].value;
        if (args[0].tok == T.point4f) {
          Lst<Object> list = Measure.getIntersectionPP((P4) args[0].value,
              plane);
          if (list == null)
            return mp.addXStr("");
          return mp.addXList(list);
        }
        pt2 = mp.ptValue(args[0], false);
        if (pt2 == null)
          return mp.addXStr("");
        return mp.addXPt(Measure.getIntersection(pt2, null, plane, pt3, norm,
            vTemp));
      }
      //$FALL-THROUGH$
    case 3:
    case 4:
      switch (tok) {
      case T.hkl:
        // hkl(i,j,k)
        return mp.addXPt4(e.getHklPlane(P3.new3(SV.fValue(args[0]),
            SV.fValue(args[1]), SV.fValue(args[2]))));
      case T.intersection:
        pt1 = mp.ptValue(args[0], false);
        pt2 = mp.ptValue(args[1], false);
        if (pt1 == null || pt2 == null)
          return mp.addXStr("");
        V3 vLine = V3.newV(pt2);
        vLine.normalize();
        if (args[2].tok == T.point4f) {
          // intersection(ptLine, vLine, plane)
          pt3 = new P3();
          norm = new V3();
          vTemp = new V3();
          pt1 = Measure.getIntersection(pt1, vLine, (P4) args[2].value, pt3,
              norm, vTemp);
          if (pt1 == null)
            return mp.addXStr("");
          return mp.addXPt(pt1);
        }
        pt3 = mp.ptValue(args[2], false);
        if (pt3 == null)
          return mp.addXStr("");
        // intersection(ptLine, vLine, pt2); 
        // IE intersection of plane perp to line through pt2
        V3 v = new V3();
        Measure.projectOntoAxis(pt3, pt1, vLine, v);
        return mp.addXPt(pt3);
      }
      switch (args[0].tok) {
      case T.integer:
      case T.decimal:
        if (args.length == 3) {
          // plane(r theta phi)
          float r = SV.fValue(args[0]);
          float theta = SV.fValue(args[1]); // longitude, azimuthal, in xy plane
          float phi = SV.fValue(args[2]); // 90 - latitude, polar, from z
          // rotate {0 0 r} about y axis need to stay in the x-z plane
          norm = V3.new3(0, 0, 1);
          pt2 = P3.new3(0, 1, 0);
          Quat q = Quat.newVA(pt2, phi);
          q.getMatrix().rotate(norm);
          // rotate that vector around z
          pt2.set(0, 0, 1);
          q = Quat.newVA(pt2, theta);
          q.getMatrix().rotate(norm);
          pt2.setT(norm);
          pt2.scale(r);
          plane = new P4();
          Measure.getPlaneThroughPoint(pt2, norm, plane);
          return mp.addXPt4(plane);
        }
        break;
      case T.bitset:
      case T.point3f:
        pt1 = mp.ptValue(args[0], false);
        pt2 = mp.ptValue(args[1], false);
        if (pt2 == null)
          return false;
        pt3 = (args.length > 2
            && (args[2].tok == T.bitset || args[2].tok == T.point3f) ? mp
            .ptValue(args[2], false) : null);
        norm = V3.newV(pt2);
        if (pt3 == null) {
          plane = new P4();
          if (args.length == 2 || !args[2].asBoolean()) {
            // plane(<point1>,<point2>) or 
            // plane(<point1>,<point2>,false)
            pt3 = P3.newP(pt1);
            pt3.add(pt2);
            pt3.scale(0.5f);
            norm.sub(pt1);
            norm.normalize();
          } else {
            // plane(<point1>,<vLine>,true)
            pt3 = pt1;
          }
          Measure.getPlaneThroughPoint(pt3, norm, plane);
          return mp.addXPt4(plane);
        }
        // plane(<point1>,<point2>,<point3>)
        // plane(<point1>,<point2>,<point3>,<pointref>)
        V3 vAB = new V3();
        V3 vAC = new V3();
        float nd = Measure.getDirectedNormalThroughPoints(pt1, pt2, pt3,
            (args.length == 4 ? mp.ptValue(args[3], true) : null), norm, vAB,
            vAC);
        return mp.addXPt4(P4.new4(norm.x, norm.y, norm.z, nd));
      }
    }
    if (args.length != 4)
      return false;
    float x = SV.fValue(args[0]);
    float y = SV.fValue(args[1]);
    float z = SV.fValue(args[2]);
    float w = SV.fValue(args[3]);
    return mp.addXPt4(P4.new4(x, y, z, w));
  }

  private boolean evaluatePoint(ScriptMathProcessor mp, SV[] args) {
    switch (args.length) {
    default:
      return false;
    case 1:
      if (args[0].tok == T.decimal || args[0].tok == T.integer)
        return mp.addXInt(args[0].asInt());
      String s = SV.sValue(args[0]);
      if (args[0].tok == T.varray)
        s = "{" + s + "}";
      Object pt = Escape.uP(s);
      return (pt instanceof P3 ? mp.addXPt((P3) pt) : mp.addXStr("" + pt));
    case 2:
      // to/from screen coordinates
      P3 pt3 = SV.ptValue(args[0]);
      if (pt3 == null)
        return false;
      if (args[1].tok == T.off) {
        // these are screen coordinates
        vwr.tm.unTransformPoint(pt3, pt3);
      } else {
        // this is TO screen coordinates
        vwr.tm.transformPt3f(pt3, pt3);
      }
      return mp.addXPt(pt3);      
    case 3:
      return mp.addXPt(P3.new3(args[0].asFloat(), args[1].asFloat(),
          args[2].asFloat()));
    case 4:
      return mp.addXPt4(P4.new4(args[0].asFloat(), args[1].asFloat(),
          args[2].asFloat(), args[3].asFloat()));
    }
  }

  private boolean evaluatePrompt(ScriptMathProcessor mp, SV[] args) {
    //x = prompt("testing")
    //x = prompt("testing","defaultInput")
    //x = prompt("testing","yes|no|cancel", true)
    //x = prompt("testing",["button1", "button2", "button3"])

    if (args.length != 1 && args.length != 2 && args.length != 3)
      return false;
    String label = SV.sValue(args[0]);
    String[] buttonArray = (args.length > 1 && args[1].tok == T.varray ? SV
        .strListValue(args[1]) : null);
    boolean asButtons = (buttonArray != null || args.length == 1 || args.length == 3
        && args[2].asBoolean());
    String input = (buttonArray != null ? null : args.length >= 2 ? SV
        .sValue(args[1]) : "OK");
    String s = "" + vwr.prompt(label, input, buttonArray, asButtons);
    return (asButtons && buttonArray != null ? mp
        .addXInt(Integer.parseInt(s) + 1) : mp.addXStr(s));
  }

  private boolean evaluateQuaternion(ScriptMathProcessor mp, SV[] args, int tok)
      throws ScriptException {
    P3 pt0 = null;
    // quaternion([quaternion array]) // mean
    // quaternion([quaternion array1], [quaternion array2], "relative") //
    // difference array
    // quaternion(matrix)
    // quaternion({atom1}) // quaternion (1st if array)
    // quaternion({atomSet}, nMax) // nMax quaternions, by group; 0 for all
    // quaternion({atom1}, {atom2}) // difference 
    // quaternion({atomSet1}, {atomset2}, nMax) // difference array, by group; 0 for all
    // quaternion(vector, theta)
    // quaternion(q0, q1, q2, q3)
    // quaternion("{x, y, z, w"})
    // quaternion("best")
    // quaternion(center, X, XY)
    // quaternion(mcol1, mcol2)
    // quaternion(q, "id", center) // draw code
    // axisangle(vector, theta)
    // axisangle(x, y, z, theta)
    // axisangle("{x, y, z, theta"})
    int nArgs = args.length;
    int nMax = Integer.MAX_VALUE;
    boolean isRelative = false;
    if (tok == T.quaternion) {
      if (nArgs > 1 && args[nArgs - 1].tok == T.string
          && ((String) args[nArgs - 1].value).equalsIgnoreCase("relative")) {
        nArgs--;
        isRelative = true;
      }
      if (nArgs > 1 && args[nArgs - 1].tok == T.integer
          && args[0].tok == T.bitset) {
        nMax = args[nArgs - 1].asInt();
        if (nMax <= 0)
          nMax = Integer.MAX_VALUE - 1;
        nArgs--;
      }
    }

    switch (nArgs) {
    case 0:
    case 1:
    case 4:
      break;
    case 2:
      if (tok == T.quaternion) {
        if (args[0].tok == T.varray
            && (args[1].tok == T.varray || args[1].tok == T.on))
          break;
        if (args[0].tok == T.bitset
            && (args[1].tok == T.integer || args[1].tok == T.bitset))
          break;
      }
      if ((pt0 = mp.ptValue(args[0], false)) == null || tok != T.quaternion
          && args[1].tok == T.point3f)
        return false;
      break;
    case 3:
      if (tok != T.quaternion)
        return false;
      if (args[0].tok == T.point4f) {
        if (args[2].tok != T.point3f && args[2].tok != T.bitset)
          return false;
        break;
      }
      for (int i = 0; i < 3; i++)
        if (args[i].tok != T.point3f && args[i].tok != T.bitset)
          return false;
      break;
    default:
      return false;
    }
    Quat q = null;
    Quat[] qs = null;
    P4 p4 = null;
    switch (nArgs) {
    case 0:
      return mp.addXPt4(Quat.newQ(vwr.tm.getRotationQuaternion()).toPoint4f());
    case 1:
    default:
      if (tok == T.quaternion && args[0].tok == T.varray) {
        Quat[] data1 = e.getQuaternionArray(args[0].getList(), T.list);
        Object mean = Quat.sphereMean(data1, null, 0.0001f);
        q = (mean instanceof Quat ? (Quat) mean : null);
        break;
      } else if (tok == T.quaternion && args[0].tok == T.bitset) {
        qs = vwr.getAtomGroupQuaternions((BS) args[0].value, nMax);
      } else if (args[0].tok == T.matrix3f) {
        q = Quat.newM((M3) args[0].value);
      } else if (args[0].tok == T.point4f) {
        p4 = (P4) args[0].value;
      } else {
        String s = SV.sValue(args[0]);
        Object v = Escape.uP(s.equalsIgnoreCase("best") ? vwr
            .getOrientationText(T.best, null) : s);
        if (!(v instanceof P4))
          return false;
        p4 = (P4) v;
      }
      if (tok == T.axisangle)
        q = Quat.newVA(P3.new3(p4.x, p4.y, p4.z), p4.w);
      break;
    case 2:
      if (tok == T.quaternion) {
        if (args[0].tok == T.varray && args[1].tok == T.varray) {
          Quat[] data1 = e.getQuaternionArray(args[0].getList(), T.list);
          Quat[] data2 = e.getQuaternionArray(args[1].getList(), T.list);
          qs = Quat.div(data2, data1, nMax, isRelative);
          break;
        }
        if (args[0].tok == T.varray && args[1].tok == T.on) {
          Quat[] data1 = e.getQuaternionArray(args[0].getList(), T.list);
          float[] stddev = new float[1];
          Quat.sphereMean(data1, stddev, 0.0001f);
          return mp.addXFloat(stddev[0]);
        }
        if (args[0].tok == T.bitset && args[1].tok == T.bitset) {
          Quat[] data1 = vwr.getAtomGroupQuaternions((BS) args[0].value,
              Integer.MAX_VALUE);
          Quat[] data2 = vwr.getAtomGroupQuaternions((BS) args[1].value,
              Integer.MAX_VALUE);
          qs = Quat.div(data2, data1, nMax, isRelative);
          break;
        }
      }
      P3 pt1 = mp.ptValue(args[1], false);
      p4 = mp.planeValue(args[0]);
      if (pt1 != null)
        q = Quat.getQuaternionFrame(P3.new3(0, 0, 0), pt0, pt1);
      else
        q = Quat.newVA(pt0, SV.fValue(args[1]));
      break;
    case 3:
      if (args[0].tok == T.point4f) {
        P3 pt = (args[2].tok == T.point3f ? (P3) args[2].value : vwr
            .ms.getAtomSetCenter((BS) args[2].value));
        return mp.addXStr(Escape.drawQuat(Quat.newP4((P4) args[0].value), "q",
            SV.sValue(args[1]), pt, 1f));
      }
      P3[] pts = new P3[3];
      for (int i = 0; i < 3; i++)
        pts[i] = (args[i].tok == T.point3f ? (P3) args[i].value : vwr
            .ms.getAtomSetCenter((BS) args[i].value));
      q = Quat.getQuaternionFrame(pts[0], pts[1], pts[2]);
      break;
    case 4:
      if (tok == T.quaternion)
        p4 = P4.new4(SV.fValue(args[1]), SV.fValue(args[2]),
            SV.fValue(args[3]), SV.fValue(args[0]));
      else
        q = Quat
            .newVA(
                P3.new3(SV.fValue(args[0]), SV.fValue(args[1]),
                    SV.fValue(args[2])), SV.fValue(args[3]));
      break;
    }
    if (qs != null) {
      if (nMax != Integer.MAX_VALUE) {
        Lst<P4> list = new Lst<P4>();
        for (int i = 0; i < qs.length; i++)
          list.addLast(qs[i].toPoint4f());
        return mp.addXList(list);
      }
      q = (qs.length > 0 ? qs[0] : null);
    }
    return mp.addXPt4((q == null ? Quat.newP4(p4) : q).toPoint4f());
  }

  private boolean evaluateRandom(ScriptMathProcessor mp, SV[] args) {
    if (args.length > 2)
      return false;
    float lower = (args.length < 2 ? 0 : SV.fValue(args[0]));
    float range = (args.length == 0 ? 1 : SV.fValue(args[args.length - 1]));
    range -= lower;
    return mp.addXFloat((float) (Math.random() * range) + lower);
  }

  private boolean evaluateRowCol(ScriptMathProcessor mp, SV[] args, int tok)
      throws ScriptException {
    if (args.length != 1)
      return false;
    int n = args[0].asInt() - 1;
    SV x1 = mp.getX();
    float[] f;
    switch (x1.tok) {
    case T.matrix3f:
      if (n < 0 || n > 2)
        return false;
      M3 m = (M3) x1.value;
      switch (tok) {
      case T.row:
        f = new float[3];
        m.getRow(n, f);
        return mp.addXAF(f);
      case T.col:
      default:
        f = new float[3];
        m.getColumn(n, f);
        return mp.addXAF(f);
      }
    case T.matrix4f:
      if (n < 0 || n > 2)
        return false;
      M4 m4 = (M4) x1.value;
      switch (tok) {
      case T.row:
        f = new float[4];
        m4.getRow(n, f);
        return mp.addXAF(f);
      case T.col:
      default:
        f = new float[4];
        m4.getColumn(n, f);
        return mp.addXAF(f);
      }
    }
    return false;

  }

  private boolean evaluateIn(ScriptMathProcessor mp, SV[] args)
      throws ScriptException {
    SV x1 = mp.getX();
    switch (args.length) {
    case 1:
      Lst<SV> lst = args[0].getList();
      if (lst != null)
        for (int i = 0, n = lst.size(); i < n; i++)
          if (SV.areEqual(x1, lst.get(i)))
            return mp.addXInt(i + 1);
      break;
    default:
      for (int i = 0; i < args.length; i++)
        if (SV.areEqual(x1, args[i]))
          return mp.addXInt(i + 1);
      break;
    }
    return mp.addXInt(0);
  }
  
  private boolean evaluateReplace(ScriptMathProcessor mp, SV[] args)
      throws ScriptException {
    boolean isAll = false;
    String sFind, sReplace;
    switch (args.length) {
    case 0:
      isAll = true;
      sFind = sReplace = null;
      break;
    case 3:
      isAll = SV.bValue(args[2]);
      //$FALL-THROUGH$
    case 2:
      sFind = SV.sValue(args[0]);
      sReplace = SV.sValue(args[1]);
      break;
    default:
      return false;
    }
    SV x = mp.getX();
    if (x.tok == T.varray) {
      String[] list = SV.strListValue(x);
      String[] l = new String[list.length];
      for (int i = list.length; --i >= 0;)
        l[i] = (sFind == null ? PT.clean(list[i]) : isAll ? PT
            .replaceAllCharacters(list[i], sFind, sReplace) : PT.rep(list[i],
            sFind, sReplace));
      return mp.addXAS(l);
    }
    String s = SV.sValue(x);
    return mp.addXStr(sFind == null ? PT.clean(s) : isAll ? PT
        .replaceAllCharacters(s, sFind, sReplace) : PT.rep(s, sFind, sReplace));
  }

  private boolean evaluateScript(ScriptMathProcessor mp, SV[] args, int tok)
      throws ScriptException {
    // javascript(cmd)
    // script(cmd)
    // script(cmd, syncTarget)
    // show(showCmd)
    if ((tok == T.eval || tok == T.show || tok == T.javascript) && args.length != 1 
        || args.length == 0
        || args.length > 2)
      return false;
    String s = SV.sValue(args[0]);
    SB sb = new SB();
    switch (tok) {
    case T.eval:
      return mp.addXObj(vwr.evaluateExpressionAsVariable(s));
    case T.script:
      String appID = (args.length == 2 ? SV.sValue(args[1]) : ".");
      // options include * > . or an appletID with or without "jmolApplet"
      if (!appID.equals("."))
        sb.append(vwr.jsEval(appID + "\1" + s));
      if (appID.equals(".") || appID.equals("*"))
        e.runScriptBuffer(s, sb);
      break;
    case T.show:
      e.runScriptBuffer("show " + s, sb);
      break;
    case T.javascript:
      sb.append(vwr.jsEval(s));
      break;
    }
    s = sb.toString();
    float f;
    return (Float.isNaN(f = PT.parseFloatStrict(s)) ? mp.addXStr(s) : s
        .indexOf(".") >= 0 ? mp.addXFloat(f) : mp.addXInt(PT.parseInt(s)));
  }

  /**
   * sort() or sort(n) or count() or count("xxxx")
   * 
   * @param mp
   * @param args
   * @param tok
   * @return true if no error
   * @throws ScriptException
   */
  private boolean evaluateSort(ScriptMathProcessor mp, SV[] args, int tok)
      throws ScriptException {
    if (args.length > 1)
      return false;
    if (tok == T.sort) {
      int n = (args.length == 0 ? 0 : args[0].asInt());
      return mp.addX(mp.getX().sortOrReverse(n));
    }
    SV x = mp.getX();
    SV match = (args.length == 0 ? null : args[0]);
    if (x.tok == T.string) {
      int n = 0;
      String s = SV.sValue(x);
      if (match == null)
        return mp.addXInt(0);
      String m = SV.sValue(match);
      for (int i = 0; i < s.length(); i++) {
        int pt = s.indexOf(m, i);
        if (pt < 0)
          break;
        n++;
        i = pt;
      }
      return mp.addXInt(n);
    }
    Lst<SV> counts = new Lst<SV>();
    SV last = null;
    SV count = null;
    Lst<SV> xList = SV.getVariable(x.value).sortOrReverse(0).getList();
    if (xList == null)
      return (match == null ? mp.addXStr("") : mp.addXInt(0));
    for (int i = 0, nLast = xList.size(); i <= nLast; i++) {
      SV a = (i == nLast ? null : xList.get(i));
      if (match != null && a != null && !SV.areEqual(a, match))
        continue;
      if (SV.areEqual(a, last)) {
        count.intValue++;
        continue;
      } else if (last != null) {
        Lst<SV> y = new Lst<SV>();
        y.addLast(last);
        y.addLast(count);
        counts.addLast(SV.getVariableList(y));
      }
      count = SV.newI(1);
      last = a;
    }
    if (match == null)
      return mp.addX(SV.getVariableList(counts));
    if (counts.isEmpty())
      return mp.addXInt(0);
    return mp.addX(counts.get(0).getList().get(1));
  }

  private boolean evaluateString(ScriptMathProcessor mp, int tok, SV[] args)
      throws ScriptException {
    if (args.length > 1)
      return false;
    SV x = mp.getX();
    if (x.tok == T.varray && tok != T.split && tok != T.trim) {
      mp.addX(x);
      return evaluateList(mp, tok, args);
    }
    String s = (tok == T.split && x.tok == T.bitset || tok == T.trim
        && x.tok == T.varray ? null : SV.sValue(x));
    String sArg = (args.length == 1 ? SV.sValue(args[0]) : tok == T.trim ? ""
        : "\n");
    switch (tok) {
    case T.split:
      if (x.tok == T.bitset) {
        BS bsSelected = SV.bsSelectVar(x);
        sArg = "\n";
        int modelCount = vwr.getModelCount();
        s = "";
        for (int i = 0; i < modelCount; i++) {
          s += (i == 0 ? "" : "\n");
          BS bs = vwr.getModelUndeletedAtomsBitSet(i);
          bs.and(bsSelected);
          s += Escape.eBS(bs);
        }
      }
      return mp.addXAS(PT.split(s, sArg));
    case T.join:
      if (s.length() > 0 && s.charAt(s.length() - 1) == '\n')
        s = s.substring(0, s.length() - 1);
      return mp.addXStr(PT.rep(s, "\n", sArg));
    case T.trim:
      if (s != null)
        return mp.addXStr(PT.trim(s, sArg));
      String[] list = SV.strListValue(x);
      for (int i = list.length; --i >= 0;)
        list[i] = PT.trim(list[i], sArg);
      return mp.addXAS(list);
    }
    return mp.addXStr("");
  }

  private boolean evaluateSubstructure(ScriptMathProcessor mp, SV[] args,
                                       int tok) throws ScriptException {
    // select substucture(....) legacy - was same as smiles(), now search()
    // select smiles(...)
    // select search(...)  now same as substructure
    if (args.length == 0)
      return false;
    BS bs = new BS();
    String pattern = SV.sValue(args[0]);
    if (pattern.length() > 0)
      try {
        BS bsSelected = (args.length == 2 && args[1].tok == T.bitset ? SV
            .bsSelectVar(args[1]) : null);
        bs = vwr.getSmilesMatcher().getSubstructureSet(pattern,
            vwr.ms.at, vwr.getAtomCount(), bsSelected,
            tok != T.smiles, false);
      } catch (Exception ex) {
        e.evalError(ex.getMessage(), null);
      }
    return mp.addXBs(bs);
  }

  private boolean evaluateSymop(ScriptMathProcessor mp, SV[] args,
                                boolean haveBitSet) throws ScriptException {
    // {xxx}.symop()
    // symop({xxx}    
    if (args.length == 0)
      return false;
    SV x1 = (haveBitSet ? mp.getX() : null);
    if (x1 != null && x1.tok != T.bitset)
      return false;
    BS bs = (x1 != null ? (BS) x1.value : args.length > 2
        && args[1].tok == T.bitset ? (BS) args[1].value : vwr.getAllAtoms());
    String xyz;
    switch (args[0].tok) {
    case T.string:
      xyz = SV.sValue(args[0]);
      break;
    case T.matrix4f:
      xyz = args[0].escape();
      break;
    default:
      xyz = null;
    }
    int iOp = (xyz == null ? args[0].asInt() : 0);
    P3 pt = (args.length > 1 ? mp.ptValue(args[1], true) : null);
    if (args.length == 2 && !Float.isNaN(pt.x))
      return mp.addXObj(vwr.getSymmetryInfo(bs, xyz, iOp, pt, null, null,
          T.point));
    String desc = (args.length == 1 ? "" : SV.sValue(args[args.length - 1]))
        .toLowerCase();
    int tok = T.draw;
    if (args.length == 1 || desc.equalsIgnoreCase("matrix")) {
      tok = T.matrix4f;
    } else if (desc.equalsIgnoreCase("array") || desc.equalsIgnoreCase("list")) {
      tok = T.list;
    } else if (desc.equalsIgnoreCase("description")) {
      tok = T.label;
    } else if (desc.equalsIgnoreCase("xyz")) {
      tok = T.info;
    } else if (desc.equalsIgnoreCase("translation")) {
      tok = T.translation;
    } else if (desc.equalsIgnoreCase("axis")) {
      tok = T.axis;
    } else if (desc.equalsIgnoreCase("plane")) {
      tok = T.plane;
    } else if (desc.equalsIgnoreCase("angle")) {
      tok = T.angle;
    } else if (desc.equalsIgnoreCase("axispoint")) {
      tok = T.point;
    } else if (desc.equalsIgnoreCase("center")) {
      tok = T.center;
    }
    return mp
        .addXObj(vwr.getSymmetryInfo(bs, xyz, iOp, pt, null, desc, tok));
  }

  private boolean evaluateTensor(ScriptMathProcessor mp, SV[] args)
      throws ScriptException {
    // {*}.tensor()
    // {*}.tensor("isc")            // only within this atom set
    // {atomindex=1}.tensor("isc")  // all to this atom
    // {*}.tensor("efg","eigenvalues")
    if (args.length > 2)
      return false;
    BS bs = SV.getBitSet(mp.getX(), false);
    String tensorType = (args.length == 0 ? null : SV.sValue(args[0])
        .toLowerCase());
    JmolNMRInterface calc = vwr.getNMRCalculation();
    if ("unique".equals(tensorType))
      return mp.addXBs(calc.getUniqueTensorSet(bs));
    String infoType = (args.length < 2 ? null : SV.sValue(args[1])
        .toLowerCase());
    return mp.addXList(calc.getTensorInfo(tensorType, infoType, bs));
  }

  private boolean evaluateUserFunction(ScriptMathProcessor mp, String name,
                                       SV[] args, int tok, boolean isSelector)
      throws ScriptException {
    SV x1 = null;
    if (isSelector) {
      x1 = mp.getX();
      if (x1.tok != T.bitset)
        return false;
    }
    mp.wasX = false;
    Lst<SV> params = new Lst<SV>();
    for (int i = 0; i < args.length; i++) {
      params.addLast(args[i]);
    }
    if (isSelector) {
      return mp
          .addXObj(e.getBitsetProperty(SV.bsSelectVar(x1), tok, null, null,
              x1.value, new Object[] { name, params }, false, x1.index, false));
    }
    SV var = e.getUserFunctionResult(name, params, null);
    return (var == null ? false : mp.addX(var));
  }

  private boolean evaluateWithin(ScriptMathProcessor mp, SV[] args)
      throws ScriptException {
    if (args.length < 1 || args.length > 5)
      return false;
    int i = args.length;
    float distance = 0;
    Object withinSpec = args[0].value;
    String withinStr = "" + withinSpec;
    int tok = args[0].tok;
    if (tok == T.string)
      tok = T.getTokFromName(withinStr);
    boolean isVdw = (tok == T.vanderwaals);
    if (isVdw) {
      distance = 100;
      withinSpec = null;
    }
    BS bs;
    ModelSet ms = vwr.ms;
    boolean isWithinModelSet = false;
    boolean isWithinGroup = false;
    boolean isDistance = (isVdw || tok == T.decimal || tok == T.integer);
    RadiusData rd = null;
    switch (tok) {
    case T.branch:
      return (i == 3 && args[1].value instanceof BS 
      && args[2].value instanceof BS && mp.addXBs(vwr.getBranchBitSet(
          ((BS) args[2].value).nextSetBit(0),
          ((BS) args[1].value).nextSetBit(0), true)));
    case T.smiles:
    case T.substructure: // same as "SMILES"
    case T.search:
      // within("smiles", "...", {bitset})
      // within("smiles", "...", {bitset})
      BS bsSelected = null;
      boolean isOK = true;
      switch (i) {
      case 2:
        break;
      case 3:
        isOK = (args[2].tok == T.bitset);
        if (isOK)
          bsSelected = (BS) args[2].value;
        break;
      default:
        isOK = false;
      }
      if (!isOK)
        e.invArg();
      return mp.addXObj(e.getSmilesExt().getSmilesMatches(SV.sValue(args[1]), null, bsSelected,
          null, tok == T.search, mp.asBitSet));
    }
    if (withinSpec instanceof String) {
      if (tok == T.nada) {
        tok = T.spec_seqcode;
        if (i > 2)
          return false;
        i = 2;
      }
    } else if (isDistance) {
      if (!isVdw)
        distance = SV.fValue(args[0]);
      if (i < 2)
        return false;
      switch (tok = args[1].tok) {
      case T.on:
      case T.off:
        isWithinModelSet = args[1].asBoolean();
        i = 0;
        break;
      case T.string:
        String s = SV.sValue(args[1]);
        if (s.startsWith("$"))
          return mp.addXBs(getAtomsNearSurface(distance, s.substring(1)));
        isWithinGroup = (s.equalsIgnoreCase("group"));
        isVdw = (s.equalsIgnoreCase("vanderwaals"));
        if (isVdw) {
          withinSpec = null;
          tok = T.vanderwaals;
        } else {
          tok = T.group;
        }
        break;
      }
    } else {
      return false;
    }
    P3 pt = null;
    P4 plane = null;
    switch (i) {
    case 1:
      // within (sheet)
      // within (helix)
      // within (boundbox)
      switch (tok) {
      case T.helix:
      case T.sheet:
      case T.boundbox:
        return mp.addXBs(ms.getAtoms(tok, null));
      case T.basepair:
        return mp.addXBs(ms.getAtoms(tok, ""));
      case T.spec_seqcode:
        return mp.addXBs(ms.getAtoms(T.sequence, withinStr));
      }
      return false;
    case 2:
      // within (atomName, "XX,YY,ZZZ")
      switch (tok) {
      case T.spec_seqcode:
        tok = T.sequence;
        break;
      case T.atomname:
      case T.atomtype:
      case T.basepair:
      case T.sequence:
      case T.dssr:
      case T.domains:
      case T.validation:
        return mp.addXBs(vwr.ms.getAtoms(tok,
            SV.sValue(args[args.length - 1])));
      }
      break;
    case 3:
      switch (tok) {
      case T.on:
      case T.off:
      case T.group:
      case T.vanderwaals:
      case T.plane:
      case T.hkl:
      case T.coord:
        break;
      case T.sequence:
        // within ("sequence", "CII", *.ca)
        withinStr = SV.sValue(args[2]);
        break;
      default:
        return false;
      }
      // within (distance, group, {atom collection})
      // within (distance, true|false, {atom collection})
      // within (distance, plane|hkl, [plane definition] )
      // within (distance, coord, [point or atom center] )
      break;
    }
    i = args.length - 1;
    if (args[i].value instanceof P4) {
      plane = (P4) args[i].value;
    } else if (args[i].value instanceof P3) {
      pt = (P3) args[i].value;
      if (SV.sValue(args[1]).equalsIgnoreCase("hkl"))
        plane = e.getHklPlane(pt);
    }
    if (i > 0 && plane == null && pt == null && !(args[i].value instanceof BS))
      return false;
    if (plane != null)
      return mp.addXBs(ms.getAtomsNearPlane(distance, plane));
    if (pt != null)
      return mp.addXBs(vwr.getAtomsNearPt(distance, pt));
    bs = (args[i].tok == T.bitset ? SV.bsSelectVar(args[i]) : null);
    if (tok == T.sequence)
      return mp.addXBs(vwr.ms.getSequenceBits(withinStr, bs));
    if (bs == null)
      bs = new BS();
    if (!isDistance)
      return mp.addXBs(vwr.ms.getAtoms(tok, bs));
    if (isWithinGroup)
      return mp.addXBs(vwr.getGroupsWithin((int) distance, bs));
    if (isVdw)
      rd = new RadiusData(null, (distance > 10 ? distance / 100 : distance),
          (distance > 10 ? EnumType.FACTOR : EnumType.OFFSET), VDW.AUTO);
    return mp.addXBs(vwr.ms.getAtomsWithinRadius(distance, bs,
        isWithinModelSet, rd));
  }

  private boolean evaluateWrite(ScriptMathProcessor mp, SV[] args)
      throws ScriptException {
    switch (args.length) {
    case 0:
      return false;
    case 1:
      if (!args[0].asString().toUpperCase().equals("PNGJ"))
        break;
      return mp.addXMap(vwr.getFileAsMap(null));
    }
    return mp.addXStr(e.getCmdExt().write(args));
  }

  ///////// private methods used by evaluateXXXXX ////////
  
  private BS getAtomsNearSurface(float distance, String surfaceId) {
    Object[] data = new Object[] { surfaceId, null, null };
    if (e.getShapePropertyData(JC.SHAPE_ISOSURFACE, "getVertices", data))
      return vwr.ms.getAtomsNearPts(distance, (P3[]) data[1], (BS) data[2]);
    data[1] = Integer.valueOf(0);
    data[2] = Integer.valueOf(-1);
    if (e.getShapePropertyData(JC.SHAPE_DRAW, "getCenter", data))
      return vwr.getAtomsNearPt(distance, (P3) data[2]);
    return new BS();
  }

  private float getDistance(ScriptMathProcessor mp, SV x1, SV x2, int tok)
      throws ScriptException {
    P3 pt1 = mp.ptValue(x1, true);
    P4 plane1 = mp.planeValue(x1);
    P3 pt2 = mp.ptValue(x2, true);
    P4 plane2 = mp.planeValue(x2);
    if (tok == T.dot) {
      if (plane1 != null && plane2 != null)
        // q1.dot(q2) assume quaternions
        return plane1.x * plane2.x + plane1.y * plane2.y + plane1.z * plane2.z
            + plane1.w * plane2.w;
      // plane.dot(point) =
      if (plane1 != null)
        pt1 = P3.new3(plane1.x, plane1.y, plane1.z);
      // point.dot(plane)
      if (plane2 != null)
        pt2 = P3.new3(plane2.x, plane2.y, plane2.z);
      return pt1.x * pt2.x + pt1.y * pt2.y + pt1.z * pt2.z;
    }

    if (plane1 == null)
      return (plane2 == null ? pt2.distance(pt1) : Measure.distanceToPlane(
          plane2, pt1));
    return Measure.distanceToPlane(plane1, pt2);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object getMinMax(Object floatOrSVArray, int tok) {
    float[] data = null;
    Lst<SV> sv = null;
    int ndata = 0;
    while (true) {
      if (PT.isAF(floatOrSVArray)) {
        data = (float[]) floatOrSVArray;
        ndata = data.length;
        if (ndata == 0)
          break;
      } else if (floatOrSVArray instanceof Lst<?>) {
        sv = (Lst<SV>) floatOrSVArray;
        ndata = sv.size();
        if (ndata == 0)
          break;
        SV sv0 = sv.get(0);
        if (sv0.tok == T.string && ((String) sv0.value).startsWith("{")) {
          Object pt = SV.ptValue(sv0);
          if (pt instanceof P3)
            return getMinMaxPoint(sv, tok);
          if (pt instanceof P4)
            return getMinMaxQuaternion(sv, tok);
          break;
        }
      } else {
        break;
      }
      double sum;
      switch (tok) {
      case T.min:
        sum = Float.MAX_VALUE;
        break;
      case T.max:
        sum = -Float.MAX_VALUE;
        break;
      default:
        sum = 0;
      }
      double sum2 = 0;
      int n = 0;
      for (int i = ndata; --i >= 0;) {
        float v = (data == null ? SV.fValue(sv.get(i)) : data[i]);
        if (Float.isNaN(v))
          continue;
        n++;
        switch (tok) {
        case T.sum2:
        case T.stddev:
          sum2 += ((double) v) * v;
          //$FALL-THROUGH$
        case T.sum:
        case T.average:
          sum += v;
          break;
        case T.min:
          if (v < sum)
            sum = v;
          break;
        case T.max:
          if (v > sum)
            sum = v;
          break;
        }
      }
      if (n == 0)
        break;
      switch (tok) {
      case T.average:
        sum /= n;
        break;
      case T.stddev:
        if (n == 1)
          break;
        sum = Math.sqrt((sum2 - sum * sum / n) / (n - 1));
        break;
      case T.min:
      case T.max:
      case T.sum:
        break;
      case T.sum2:
        sum = sum2;
        break;
      }
      return Float.valueOf((float) sum);
    }
    return "NaN";
  }

  /**
   * calculates the statistical value for x, y, and z independently
   * 
   * @param pointOrSVArray
   * @param tok
   * @return Point3f or "NaN"
   */
  @SuppressWarnings("unchecked")
  private Object getMinMaxPoint(Object pointOrSVArray, int tok) {
    P3[] data = null;
    Lst<SV> sv = null;
    int ndata = 0;
    if (pointOrSVArray instanceof Quat[]) {
      data = (P3[]) pointOrSVArray;
      ndata = data.length;
    } else if (pointOrSVArray instanceof Lst<?>) {
      sv = (Lst<SV>) pointOrSVArray;
      ndata = sv.size();
    }
    if (sv != null || data != null) {
      P3 result = new P3();
      float[] fdata = new float[ndata];
      boolean ok = true;
      for (int xyz = 0; xyz < 3 && ok; xyz++) {
        for (int i = 0; i < ndata; i++) {
          P3 pt = (data == null ? SV.ptValue(sv.get(i)) : data[i]);
          if (pt == null) {
            ok = false;
            break;
          }
          switch (xyz) {
          case 0:
            fdata[i] = pt.x;
            break;
          case 1:
            fdata[i] = pt.y;
            break;
          case 2:
            fdata[i] = pt.z;
            break;
          }
        }
        if (!ok)
          break;
        Object f = getMinMax(fdata, tok);
        if (f instanceof Float) {
          float value = ((Float) f).floatValue();
          switch (xyz) {
          case 0:
            result.x = value;
            break;
          case 1:
            result.y = value;
            break;
          case 2:
            result.z = value;
            break;
          }
        } else {
          break;
        }
      }
      return result;
    }
    return "NaN";
  }

  private Object getMinMaxQuaternion(Lst<SV> svData, int tok) {
    Quat[] data;
    switch (tok) {
    case T.min:
    case T.max:
    case T.sum:
    case T.sum2:
      return "NaN";
    }

    // only stddev and average

    while (true) {
      data = e.getQuaternionArray(svData, T.list);
      if (data == null)
        break;
      float[] retStddev = new float[1];
      Quat result = Quat.sphereMean(data, retStddev, 0.0001f);
      switch (tok) {
      case T.average:
        return result;
      case T.stddev:
        return Float.valueOf(retStddev[0]);
      }
      break;
    }
    return "NaN";
  }

  private JmolPatternMatcher pm;

  private JmolPatternMatcher getPatternMatcher() {
    return (pm == null ? pm = (JmolPatternMatcher) Interface
        .getUtil("PatternMatcher") : pm);
  }

  private T opTokenFor(int tok) {
    switch (tok) {
    case T.add:
    case T.join:
      return T.tokenPlus;
    case T.sub:
      return T.tokenMinus;
    case T.mul:
      return T.tokenTimes;
    case T.mul3:
      return T.tokenMul3;
    case T.div:
      return T.tokenDivide;
    }
    return null;
  }

  @Override
  public BS setContactBitSets(BS bsA, BS bsB, boolean localOnly,
                               float distance, RadiusData rd,
                               boolean warnMultiModel) {
    boolean withinAllModels;
    BS bs;
    if (bsB == null) {
      // default is within just one model when {B} is missing
      bsB = BSUtil.setAll(vwr.getAtomCount());
      BSUtil.andNot(bsB, vwr.getDeletedAtoms());
      bsB.andNot(bsA);
      withinAllModels = false;
    } else {
      // two atom sets specified; within ALL MODELS here
      bs = BSUtil.copy(bsA);
      bs.or(bsB);
      int nModels = vwr.ms.getModelBS(bs, false).cardinality();
      withinAllModels = (nModels > 1);
      if (warnMultiModel && nModels > 1 && !e.tQuiet)
        e.showString(GT
            ._("Note: More than one model is involved in this contact!"));
    }
    // B always within some possibly extended VDW of A or just A itself
    if (!bsA.equals(bsB)) {
      boolean setBfirst = (!localOnly || bsA.cardinality() < bsB.cardinality());
      if (setBfirst) {
        bs = vwr.ms.getAtomsWithinRadius(distance, bsA, withinAllModels,
            (Float.isNaN(distance) ? rd : null));
        bsB.and(bs);
      }
      if (localOnly) {
        // we can just get the near atoms for A as well.
        bs = vwr.ms.getAtomsWithinRadius(distance, bsB, withinAllModels,
            (Float.isNaN(distance) ? rd : null));
        bsA.and(bs);
        if (!setBfirst) {
          bs = vwr.ms.getAtomsWithinRadius(distance, bsA, withinAllModels,
              (Float.isNaN(distance) ? rd : null));
          bsB.and(bs);
        }
        // If the two sets are not the same,
        // we AND them and see if that is A. 
        // If so, then the smaller set is
        // removed from the larger set.
        bs = BSUtil.copy(bsB);
        bs.and(bsA);
        if (bs.equals(bsA))
          bsB.andNot(bsA);
        else if (bs.equals(bsB))
          bsA.andNot(bsB);
      }
    }
    return bsB;
  }
}
