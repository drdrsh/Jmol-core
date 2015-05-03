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

import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.jmol.api.Interface;
import org.jmol.api.JmolDataManager;
import org.jmol.api.MinimizerInterface;
import org.jmol.atomdata.RadiusData;
import org.jmol.c.STER;
import org.jmol.c.VDW;
import org.jmol.i18n.GT;
import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.AtomCollection;
import org.jmol.modelset.Bond;
import org.jmol.modelset.BondSet;
import org.jmol.modelset.LabelToken;
import org.jmol.modelset.ModelSet;
import org.jmol.modelset.StateScript;
import org.jmol.modelset.Text;
import org.jmol.modelset.TickInfo;
import org.jmol.script.SV;
import org.jmol.script.ScriptCompiler;
import org.jmol.script.ScriptContext;
import org.jmol.script.ScriptError;
import org.jmol.script.ScriptEval;
import org.jmol.script.ScriptException;
import org.jmol.script.ScriptInterruption;
import org.jmol.script.ScriptMathProcessor;
import org.jmol.script.ScriptParam;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.Elements;
import org.jmol.util.Escape;
import org.jmol.util.Edge;
import org.jmol.util.Parser;
import org.jmol.util.Point3fi;

import javajs.awt.Font;
import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.SB;

import org.jmol.util.Logger;

import javajs.util.BArray;
import javajs.util.Base64;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.Quat;
import javajs.util.V3;

import org.jmol.viewer.FileManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.JmolAsyncException;
import org.jmol.viewer.ShapeManager;
import org.jmol.viewer.StateManager;
import org.jmol.viewer.TransformManager;
import org.jmol.viewer.Viewer.ACCESS;

public class CmdExt extends ScriptExt {

  final static int ERROR_invalidArgument = 22;

  public CmdExt() {
    // used by Reflection
  }

  @Override
  public String dispatch(int iTok, boolean b, T[] st) throws ScriptException {
    chk = e.chk;
    slen = e.slen;
    this.st = st;
    switch (iTok) {
    case T.assign:
      assign();
      break;
    case T.cache:
      cache();
      break;
    case T.calculate:
      calculate();
      break;
    case T.capture:
      capture();
      break;
    case T.centerat:
      centerAt();
      break;
    case T.compare:
      compare();
      break;
    case T.console:
      console();
      break;
    case T.connect:
      connect(1);
      break;
    case T.configuration:
      configuration();
      break;
    case T.data:
      data();
      break;
    case T.hbond:
      connect(0);
      break;
    case T.image:
      image();
      break;
    case T.mapproperty:
      mapProperty();
      break;
    case T.minimize:
      minimize();
      break;
    case T.modulation:
      modulation();
      break;
    case T.mutate:
      mutate();
      break;
    case T.navigate:
      navigate();
      break;
    case T.plot:
    case T.quaternion:
    case T.ramachandran:
      plot(st);
      break;
    case T.show:
      show();
      break;
    case T.stereo:
      stereo();
      break;
    case T.write:
      return write(b ? st : null);
    case JC.SHAPE_MEASURES:
      measure();
      break;
    case JC.SHAPE_POLYHEDRA:
      polyhedra();
      break;
    case JC.SHAPE_ELLIPSOIDS:
      ellipsoid();
      break;
    case JC.SHAPE_STRUTS:
      struts();
      break;
    }
    return null;
  }


  /**
   * used for TRY command
   * 
   * @param context
   * @param shapeManager
   * @return true if successful; false if not
   */
  public boolean evalParallel(ScriptContext context,
                                  ShapeManager shapeManager) {
    ScriptEval se = new ScriptEval().setViewer(vwr);
    se.historyDisabled = true;
    se.compiler = new ScriptCompiler(vwr);
    se.sm = shapeManager;
    try {
      se.restoreScriptContext(context, true, false, false);
      // TODO: This will disallow some motion commands
      //       within a TRY/CATCH block in JavaScript, and
      //       the code will block. 
      se.allowJSThreads = false;
      se.dispatchCommands(false, false, false);
    } catch (Exception ex) {
      e.vwr.setStringProperty("_errormessage", "" + ex);
      if (se.thisContext == null) {
        Logger.error("Error evaluating context " + ex);
        if (!vwr.isJS)
          ex.printStackTrace();
      }
      return false;
    }
    return true;
  }

  @SuppressWarnings("static-access")
  public Object getBitsetIdent(BS bs, String label, Object tokenValue,
                               boolean useAtomMap, int index,
                               boolean isExplicitlyAll) {
    boolean isAtoms = !(tokenValue instanceof BondSet);
    if (isAtoms) {
      if (label == null)
        label = vwr.getStandardLabelFormat(0);
      else if (label.length() == 0)
        label = "%[label]";
    }
    int pt = (label == null ? -1 : label.indexOf("%"));
    boolean haveIndex = (index != Integer.MAX_VALUE);
    if (bs == null || chk || isAtoms && pt < 0) {
      if (label == null)
        label = "";
      return isExplicitlyAll ? new String[] { label } : (Object) label;
    }
    ModelSet modelSet = vwr.ms;
    int n = 0;
    LabelToken labeler = modelSet.getLabeler();
    int[] indices = (isAtoms || !useAtomMap ? null : ((BondSet) tokenValue)
        .associatedAtoms);
    if (indices == null && label != null && label.indexOf("%D") > 0)
      indices = vwr.ms.getAtomIndices(bs);
    boolean asIdentity = (label == null || label.length() == 0);
    Map<String, Object> htValues = (isAtoms || asIdentity ? null : LabelToken
        .getBondLabelValues());
    LabelToken[] tokens = (asIdentity ? null : isAtoms ? labeler.compile(
        vwr, label, '\0', null) : labeler.compile(vwr, label, '\1',
        htValues));
    int nmax = (haveIndex ? 1 : BSUtil.cardinalityOf(bs));
    String[] sout = new String[nmax];
    P3 ptTemp = new P3();
    for (int j = (haveIndex ? index : bs.nextSetBit(0)); j >= 0; j = bs
        .nextSetBit(j + 1)) {
      String str;
      if (isAtoms) {
        if (asIdentity)
          str = modelSet.at[j].getInfo();
        else
          str = labeler.formatLabelAtomArray(vwr, modelSet.at[j], tokens,
              '\0', indices, ptTemp);
      } else {
        Bond bond = modelSet.bo[j];
        if (asIdentity)
          str = bond.getIdentity();
        else
          str = labeler
              .formatLabelBond(vwr, bond, tokens, htValues, indices, ptTemp);
      }
      str = PT.formatStringI(str, "#", (n + 1));
      sout[n++] = str;
      if (haveIndex)
        break;
    }
    return nmax == 1 && !isExplicitlyAll ? sout[0] : (Object) sout;
  }

  ///////////////// Jmol script commands ////////////
  
  private void cache() throws ScriptException {
    int tok = tokAt(1);
    String fileName = null;
    int n = 2;
    switch (tok) {
    case T.add:
    case T.remove:
      fileName = e.optParameterAsString(n++);
      //$FALL-THROUGH$
    case T.clear:
      checkLength(n);
      if (!chk) {
        if ("all".equals(fileName))
          fileName = null;
        int nBytes = vwr.cacheFileByName(fileName, tok == T.add);
        showString(nBytes < 0 ? "cache cleared" : nBytes + " bytes "
            + (tok == T.add ? " cached" : " removed"));
      }
      break;
    default:
      invArg();
    }
  }

  private void calculate() throws ScriptException {
    boolean isSurface = false;
    boolean asDSSP = false;
    BS bs1 = null;
    BS bs2 = null;
    ScriptEval eval = this.e;
    int n = Integer.MIN_VALUE;
    if ((eval.iToken = eval.slen) >= 2) {
      eval.clearDefinedVariableAtomSets();
      switch (getToken(1).tok) {
      case T.identifier:
        checkLength(2);
        break;
      case T.formalcharge:
        checkLength(2);
        if (chk)
          return;
        n = vwr.calculateFormalCharges(null);
        showString(GT.i(GT._("{0} charges modified"), n));
        return;
      case T.aromatic:
        checkLength(2);
        if (!chk)
          vwr.ms.assignAromaticBondsBs(true, null);
        return;
      case T.hbond:
        if (eval.slen != 2) {
          // calculate hbonds STRUCTURE -- only the DSSP/DSSR structurally-defining H bonds
          asDSSP = (tokAt(++eval.iToken) == T.structure);
          if (asDSSP)
            bs1 = vwr.bsA();
          else
            bs1 = atomExpressionAt(eval.iToken);
          if (!asDSSP && !(asDSSP = (tokAt(++eval.iToken) == T.structure)))
            bs2 = atomExpressionAt(eval.iToken);
        }
        if (chk)
          return;
        n = vwr.autoHbond(bs1, bs2, false);
        if (n != Integer.MIN_VALUE)
          eval.report(GT.i(GT._("{0} hydrogen bonds"), Math.abs(n)));
        return;
      case T.hydrogen:
        bs1 = (slen == 2 ? null : atomExpressionAt(2));
        eval.checkLast(eval.iToken);
        if (!chk)
          vwr.addHydrogens(bs1, false, false);
        return;
      case T.partialcharge:
        eval.iToken = 1;
        bs1 = (slen == 2 ? null : atomExpressionAt(2));
        eval.checkLast(eval.iToken);
        if (!chk)
          try {
            vwr.calculatePartialCharges(bs1);
          } catch (JmolAsyncException e1) {
            eval.loadFileResourceAsync(e1.getFileName());
          }
        return;
      case T.pointgroup:
        if (!chk)
          showString(vwr.ms.calculatePointGroup(vwr.bsA()));
        return;
      case T.straightness:
        checkLength(2);
        if (!chk) {
          vwr.calculateStraightness();
          vwr.addStateScript("set quaternionFrame '" + vwr.getQuaternionFrame()
              + "'; calculate straightness", false, true);
        }
        return;
      case T.structure:
        bs1 = (slen < 4 ? null : atomExpressionAt(2));
        switch (tokAt(++eval.iToken)) {
        case T.ramachandran:
          break;
        case T.dssr:
          if (chk)
            return;
          eval.showString(vwr.getAnnotationParser().calculateDSSRStructure(vwr, bs1));
          return;
        case T.dssp:
          asDSSP = true;
          break;
        case T.nada:
          asDSSP = vwr.getBoolean(T.defaultstructuredssp);
          break;
        default:
          invArg();
        }
        if (!chk)
          showString(vwr.calculateStructures(bs1, asDSSP, true));
        return;
      case T.struts:
        bs1 = (eval.iToken + 1 < slen ? atomExpressionAt(++eval.iToken) : null);
        bs2 = (eval.iToken + 1 < slen ? atomExpressionAt(++eval.iToken) : null);
        checkLength(++eval.iToken);
        if (!chk) {
          n = vwr.calculateStruts(bs1, bs2);
          if (n > 0) {
            setShapeProperty(JC.SHAPE_STICKS, "type",
                Integer.valueOf(Edge.BOND_STRUT));
            eval.setShapePropertyBs(JC.SHAPE_STICKS, "color",
                Integer.valueOf(0x0FFFFFF), null);
            eval.setShapeTranslucency(JC.SHAPE_STICKS, "", "translucent", 0.5f,
                null);
            setShapeProperty(JC.SHAPE_STICKS, "type",
                Integer.valueOf(Edge.BOND_COVALENT_MASK));
          }
          showString(GT.i(GT._("{0} struts added"), n));
        }
        return;
      case T.surface:
        isSurface = true;
        // deprecated
        //$FALL-THROUGH$
      case T.surfacedistance:
        // preferred
        // calculate surfaceDistance FROM {...}
        // calculate surfaceDistance WITHIN {...}
        boolean isFrom = false;
        switch (tokAt(2)) {
        case T.within:
          eval.iToken++;
          break;
        case T.nada:
          isFrom = !isSurface;
          break;
        case T.from:
          isFrom = true;
          eval.iToken++;
          break;
        default:
          isFrom = true;
        }
        bs1 = (eval.iToken + 1 < slen ? atomExpressionAt(++eval.iToken) : vwr.bsA());
        checkLength(++eval.iToken);
        if (!chk)
          vwr.calculateSurface(bs1, (isFrom ? Float.MAX_VALUE : -1));
        return;
      }
    }
    eval.errorStr2(
        ScriptError.ERROR_what,
        "CALCULATE",
        "aromatic? hbonds? hydrogen? formalCharge? partialCharge? pointgroup? straightness? structure? struts? surfaceDistance FROM? surfaceDistance WITHIN?");
  }

  private void capture() throws ScriptException {
    // capture "filename"
    // capture "filename" ROTATE axis degrees // y 5 assumed; axis and degrees optional
    // capture "filename" SPIN axis  // y assumed; axis optional
    // capture off/on
    // capture "" or just capture   -- end
    if (!chk && !vwr.allowCapture()) {
      showString("Cannot capture on this platform");
      return;
    }
    Map<String, Object> params = vwr.captureParams;
    String type = (params == null ? "GIF" : (String) params.get("type"));
    float endTime = 0; // indefinitely by default
    int mode = 0;
    int slen = e.slen;
    String fileName = "";
    boolean looping = (vwr.am.animationReplayMode != T.once);
    int i = 1;
    int tok = tokAt(i);
    boolean isTransparent = (tok == T.translucent);
    if (isTransparent)
      tok = tokAt(++i);
    switch (tok == T.nada ? (tok = T.end) : tok) {
    case T.string:
      fileName = e.optParameterAsString(i++);
      if (fileName.length() == 0) {
        mode = T.end;
        break;
      }
      String lc = fileName.toLowerCase();
      if (lc.endsWith(".gift") || lc.endsWith(".pngt")) {
        isTransparent = true;
        fileName = fileName.substring(0, fileName.length() - 1);
        lc = fileName.toLowerCase();
      } else if (!lc.endsWith(".gif") && !lc.contains(".png")) {
        fileName += ".gif";
      }
      if (lc.endsWith(".png")) {
        if (!lc.endsWith("0.png"))
          fileName = fileName.substring(0, fileName.length() - 4) + "0000.png";
        type = "PNG";
      } else {
        type = "GIF";
      }
      if (isTransparent)
        type += "T";
      int pt = fileName.indexOf("0000.");
      boolean streaming = (pt < 0 || pt != fileName.lastIndexOf(".") - 4);    
      boolean isRock = false;
      if (tokAt(i) == T.loop) {
        looping = true;
        tok = tokAt(++i);
      }
      switch (tokAt(i)) {
      case T.rock:
        isRock = true;
        //$FALL-THROUGH$
      case T.spin:
        String s = null;
        String axis = "y";
        looping = true;
        i++;
        if (isRock) {
          if (i < slen && tokAt(i) != T.integer)
            axis = e.optParameterAsString(i++).toLowerCase();
          s = "rotate Y 10 10;rotate Y -10 -10;rotate Y -10 -10;rotate Y 10 10";
          s = PT.rep(s, "10", "" + (i < slen ? intParameter(i++) : 5));
        } else {
          if (i < slen)
            axis = e.optParameterAsString(i++).toLowerCase();
          s = "rotate Y 360 30;";
        }
        if (chk)
          return;
        vwr.setNavigationMode(false);
        if (axis == "" || "xyz".indexOf(axis) < 0)
          axis = "y";
        boolean wf = vwr.g.waitForMoveTo;
        s = "set waitformoveto true;" + PT.rep(s, "Y", axis)
            + ";set waitformoveto " + wf;
        s = "capture " + (isTransparent ? "transparent " : "") + PT.esc(fileName) + " LOOP;"
             + s + ";capture end;";
        e.cmdScript(0, null, s);
        return;
      case T.decimal:
      case T.integer:
        endTime = floatParameter(i++);
        break;
      }
      if (chk)
        return;
      mode = T.movie;
      params = new Hashtable<String, Object>();
      int fps = vwr.getInt(T.animationfps);
      if (streaming) {
        params.put("streaming", Boolean.TRUE);
        if (!looping)
          showString(GT.o(GT._("Note: Enable looping using {0}"),
              new Object[] { "ANIMATION MODE LOOP" }));
        showString(GT.o(GT._("Animation delay based on: {0}"),
            new Object[] { "ANIMATION FPS " + fps }));
      }
      params.put("captureFps", Integer.valueOf(fps));
      break;
    case T.end:
    case T.cancel:
      if (params != null)
        params.put("captureSilent", Boolean.TRUE);
      //$FALL-THROUGH$
    case T.on:
    case T.off:
      checkLength(-2);
      mode = tok;
      break;
    default:
      invArg();
    }
    if (chk || params == null)
      return;
    params.put("type", type);
    Integer c = Integer.valueOf(vwr.getBackgroundArgb());
    params.put("backgroundColor", c);
    params.put("fileName", fileName);
    params.put("quality", Integer.valueOf(-1));
    params.put(
        "endTime",
        Long.valueOf(endTime <= 0 ? -1 : System.currentTimeMillis()
            + (long) (endTime * 1000)));
    params.put("captureMode", T.nameOf(mode).toLowerCase());
    params.put("captureLooping", looping ? Boolean.TRUE : Boolean.FALSE);
    String msg = vwr.processWriteOrCapture(params);
    Logger.info(msg);
  }


  private void centerAt() throws ScriptException {
    int tok = getToken(1).tok;
    switch (tok) {
    case T.absolute:
    case T.average:
    case T.boundbox:
      break;
    default:
      invArg();
    }
    P3 pt = P3.new3(0, 0, 0);
    if (slen == 5) {
      // centerAt xxx x y z
      pt.x = floatParameter(2);
      pt.y = floatParameter(3);
      pt.z = floatParameter(4);
    } else if (e.isCenterParameter(2)) {
      pt = centerParameter(2);
      e.checkLast(e.iToken);
    } else {
      checkLength(2);
    }
    if (!chk && !vwr.isJmolDataFrame())
        vwr.tm.setCenterAt(tok, pt);
  }

  private void compare() throws ScriptException {
    // compare {model1} {model2} 
    // compare {model1} {model2} ATOMS {bsAtoms1} {bsAtoms2}
    // compare {model1} {model2} ORIENTATIONS
    // compare {model1} {model2} ORIENTATIONS {bsAtoms1} {bsAtoms2}
    // compare {model1} {model2} ORIENTATIONS [quaternionList1] [quaternionList2]
    // compare {model1} {model2} SMILES "....." (empty quotes use SMILES for model1
    // compare {model1} {model2} SMARTS "....."
    // compare {model1} {model2} FRAMES
    // compare {model1} ATOMS {bsAtoms1} [coords]
    // compare {model1} [coords] ATOMS {bsAtoms1} [coords]
    // compare {model1} {model2} BONDS "....."   /// flexible fit
    // compare {model1} {model2} BONDS SMILES   /// flexible fit

    ScriptEval eval = e;
    boolean isQuaternion = false;
    boolean doRotate = false;
    boolean doTranslate = false;
    boolean doAnimate = false;
    boolean isFlexFit = false;
    Quat[] data1 = null, data2 = null;
    BS bsAtoms1 = null, bsAtoms2 = null;
    Lst<Object[]> vAtomSets = null;
    Lst<Object[]> vQuatSets = null;
    eval.iToken = 0;
    float nSeconds = (isFloatParameter(1) ? floatParameter(++eval.iToken)
        : Float.NaN);
    ///BS bsFrom = (tokAt(++iToken) == T.subset ? null : atomExpressionAt(iToken));
    //BS bsTo = (tokAt(++iToken) == T.subset ? null : atomExpressionAt(iToken));
    //if (bsFrom == null || bsTo == null)
    ///invArg();
    BS bsFrom = atomExpressionAt(++eval.iToken);
    P3[] coordTo = null;
    BS bsTo = null;
    if (eval.isArrayParameter(++eval.iToken)) {
      coordTo = eval.getPointArray(eval.iToken, -1, false);
    } else if (tokAt(eval.iToken) != T.atoms) {
      bsTo = atomExpressionAt(eval.iToken);
    }
    BS bsSubset = null;
    boolean isSmiles = false;
    String strSmiles = null;
    BS bs = BSUtil.copy(bsFrom);
    if (bsTo != null)
      bs.or(bsTo);
    boolean isToSubsetOfFrom = (coordTo == null && bsTo != null && bs
        .equals(bsFrom));
    boolean isFrames = isToSubsetOfFrom;
    for (int i = eval.iToken + 1; i < slen; ++i) {
      switch (getToken(i).tok) {
      case T.frame:
        isFrames = true;
        break;
      case T.smiles:
        isSmiles = true;
        if (tokAt(i + 1) != T.string) {
          strSmiles = "*";
          break;
        }
        //$FALL-THROUGH$
      case T.search: // SMARTS
        strSmiles = stringParameter(++i);
        break;
      case T.bonds:
        isFlexFit = true;
        doRotate = true;
        strSmiles = paramAsStr(++i);
        if (strSmiles.equalsIgnoreCase("SMILES")) {
          isSmiles = true;
          strSmiles = "*";
        }
        break;
      case T.decimal:
      case T.integer:
        nSeconds = Math.abs(floatParameter(i));
        if (nSeconds > 0)
          doAnimate = true;
        break;
      case T.comma:
        break;
      case T.subset:
        bsSubset = atomExpressionAt(++i);
        i = eval.iToken;
        break;
      case T.bitset:
      case T.expressionBegin:
        if (vQuatSets != null)
          invArg();
        bsAtoms1 = atomExpressionAt(eval.iToken);
        int tok = (isToSubsetOfFrom ? 0 : tokAt(eval.iToken + 1));
        bsAtoms2 = (coordTo == null && eval.isArrayParameter(eval.iToken + 1) ? null
            : (tok == T.bitset || tok == T.expressionBegin ? atomExpressionAt(++eval.iToken)
                : BSUtil.copy(bsAtoms1)));
        if (bsSubset != null) {
          bsAtoms1.and(bsSubset);
          if (bsAtoms2 != null)
            bsAtoms2.and(bsSubset);
        }

        if (bsAtoms2 == null)
          coordTo = eval.getPointArray(++eval.iToken, -1, false);
        else
          bsAtoms2.and(bsTo);
        if (vAtomSets == null)
          vAtomSets = new Lst<Object[]>();
        vAtomSets.addLast(new BS[] { bsAtoms1, bsAtoms2 });
        i = eval.iToken;
        break;
      case T.varray:
        if (vAtomSets != null)
          invArg();
        isQuaternion = true;
        data1 = eval.getQuaternionArray(((SV) eval.theToken).getList(), T.list);
        getToken(++i);
        data2 = eval.getQuaternionArray(((SV) eval.theToken).getList(), T.list);
        if (vQuatSets == null)
          vQuatSets = new Lst<Object[]>();
        vQuatSets.addLast(new Object[] { data1, data2 });
        break;
      case T.orientation:
        isQuaternion = true;
        break;
      case T.point:
      case T.atoms:
        isQuaternion = false;
        break;
      case T.rotate:
        doRotate = true;
        break;
      case T.translate:
        doTranslate = true;
        break;
      default:
        invArg();
      }
    }
    if (chk)
      return;

    // processing
    if (isFrames)
      nSeconds = 0;
    if (Float.isNaN(nSeconds) || nSeconds < 0)
      nSeconds = 1;
    else if (!doRotate && !doTranslate)
      doRotate = doTranslate = true;
    doAnimate = (nSeconds != 0);

    boolean isAtoms = (!isQuaternion && strSmiles == null || coordTo != null);
    if (isAtoms)
      Interface.getInterface("javajs.util.Eigen", vwr, "script"); // preload interface
    if (vAtomSets == null && vQuatSets == null) {
      if (bsSubset == null) {
        bsAtoms1 = (isAtoms ? vwr.getAtomBitSet("spine") : new BS());
        if (bsAtoms1.nextSetBit(0) < 0) {
          bsAtoms1 = bsFrom;
          bsAtoms2 = bsTo;
        } else {
          bsAtoms2 = BSUtil.copy(bsAtoms1);
          bsAtoms1.and(bsFrom);
          bsAtoms2.and(bsTo);
        }
      } else {
        bsAtoms1 = BSUtil.copy(bsFrom);
        bsAtoms2 = BSUtil.copy(bsTo);
        bsAtoms1.and(bsSubset);
        bsAtoms2.and(bsSubset);
        bsAtoms1.and(bsFrom);
        bsAtoms2.and(bsTo);
      }
      vAtomSets = new Lst<Object[]>();
      vAtomSets.addLast(new BS[] { bsAtoms1, bsAtoms2 });
    }

    BS[] bsFrames;
    if (isFrames) {
      BS bsModels = vwr.ms.getModelBS(bsFrom, false);
      bsFrames = new BS[bsModels.cardinality()];
      for (int i = 0, iModel = bsModels.nextSetBit(0); iModel >= 0; iModel = bsModels
          .nextSetBit(iModel + 1), i++)
        bsFrames[i] = vwr.getModelUndeletedAtomsBitSet(iModel);
    } else {
      bsFrames = new BS[] { bsFrom };
    }
    for (int iFrame = 0; iFrame < bsFrames.length; iFrame++) {
      bsFrom = bsFrames[iFrame];
      float[] retStddev = new float[2]; // [0] final, [1] initial for atoms
      Quat q = null;
      Lst<Quat> vQ = new Lst<Quat>();
      P3[][] centerAndPoints = null;
      Lst<Object[]> vAtomSets2 = (isFrames ? new Lst<Object[]>() : vAtomSets);
      for (int i = 0; i < vAtomSets.size(); ++i) {
        BS[] bss = (BS[]) vAtomSets.get(i);
        if (isFrames)
          vAtomSets2.addLast(bss = new BS[] { BSUtil.copy(bss[0]), bss[1] });
        bss[0].and(bsFrom);
      }
      P3 center = null;
      V3 translation = null;
      if (isAtoms) {
        if (coordTo != null) {
          vAtomSets2.clear();
          vAtomSets2.addLast(new Object[] { bsAtoms1, coordTo });
        }
        try {
          centerAndPoints = vwr.getCenterAndPoints(vAtomSets2, true);
        } catch (Exception ex) {
          invArg();
        }
        int n = centerAndPoints[0].length - 1;
        for (int i = 1; i <= n; i++) {
          P3 aij = centerAndPoints[0][i];
          P3 bij = centerAndPoints[1][i];
          if (!(aij instanceof Atom) || !(bij instanceof Atom))
            break;
          Logger.info(" atom 1 " + ((Atom) aij).getInfo() + "\tatom 2 "
              + ((Atom) bij).getInfo());
        }
        q = Measure.calculateQuaternionRotation(centerAndPoints, retStddev);
        float r0 = (Float.isNaN(retStddev[1]) ? Float.NaN : Math
            .round(retStddev[0] * 100) / 100f);
        float r1 = (Float.isNaN(retStddev[1]) ? Float.NaN : Math
            .round(retStddev[1] * 100) / 100f);
        showString("RMSD " + r0 + " --> " + r1 + " Angstroms");
      } else if (isQuaternion) {
        if (vQuatSets == null) {
          for (int i = 0; i < vAtomSets2.size(); i++) {
            BS[] bss = (BS[]) vAtomSets2.get(i);
            data1 = vwr.getAtomGroupQuaternions(bss[0], Integer.MAX_VALUE);
            data2 = vwr.getAtomGroupQuaternions(bss[1], Integer.MAX_VALUE);
            for (int j = 0; j < data1.length && j < data2.length; j++) {
              vQ.addLast(data2[j].div(data1[j]));
            }
          }
        } else {
          for (int j = 0; j < data1.length && j < data2.length; j++) {
            vQ.addLast(data2[j].div(data1[j]));
          }
        }
        retStddev[0] = 0;
        data1 = vQ.toArray(new Quat[vQ.size()]);
        q = Quat.sphereMean(data1, retStddev, 0.0001f);
        showString("RMSD = " + retStddev[0] + " degrees");
      } else {
        // SMILES
        /* not sure why this was like this:
        if (vAtomSets == null) {
          vAtomSets = new  List<BitSet[]>();
        }
        bsAtoms1 = BitSetUtil.copy(bsFrom);
        bsAtoms2 = BitSetUtil.copy(bsTo);
        vAtomSets.add(new BitSet[] { bsAtoms1, bsAtoms2 });
        */

        M4 m4 = new M4();
        center = new P3();
        if (("*".equals(strSmiles) || "".equals(strSmiles)) && bsFrom != null)
          try {
            strSmiles = vwr.getSmiles(bsFrom);
          } catch (Exception ex) {
            eval.evalError(ex.getMessage(), null);
          }
        if (isFlexFit) {
          float[] list;
          if (bsFrom == null
              || bsTo == null
              || (list = eval.getSmilesExt().getFlexFitList(bsFrom, bsTo,
                  strSmiles, !isSmiles)) == null)
            return;
          vwr.setDihedrals(list, null, 1);
        }
        float stddev = eval.getSmilesExt().getSmilesCorrelation(bsFrom, bsTo,
            strSmiles, null, null, m4, null, !isSmiles, false, null, center,
            false, false);
        if (Float.isNaN(stddev)) {
          showString("structures do not match");
          return;
        }
        if (doTranslate) {
          translation = new V3();
          m4.getTranslation(translation);
        }
        if (doRotate) {
          M3 m3 = new M3();
          m4.getRotationScale(m3);
          q = Quat.newM(m3);
        }
        showString("RMSD = " + stddev + " Angstroms");
      }
      if (centerAndPoints != null)
        center = centerAndPoints[0][0];
      if (center == null) {
        centerAndPoints = vwr.getCenterAndPoints(vAtomSets2, true);
        center = centerAndPoints[0][0];
      }
      P3 pt1 = new P3();
      float endDegrees = Float.NaN;
      if (doTranslate) {
        if (translation == null)
          translation = V3.newVsub(centerAndPoints[1][0], center);
        endDegrees = 1e10f;
      }
      if (doRotate) {
        if (q == null)
          eval.evalError("option not implemented", null);
        pt1.add2(center, q.getNormal());
        endDegrees = q.getTheta();
        if (endDegrees == 0 && doTranslate) {
          if (translation.length() > 0.01f)
            endDegrees = 1e10f;
          else
            doRotate = doTranslate = doAnimate = false;
        }
      }
      if (Float.isNaN(endDegrees) || Float.isNaN(pt1.x))
        continue;
      Lst<P3> ptsB = null;
      if (doRotate && doTranslate && nSeconds != 0) {
        Lst<P3> ptsA = vwr.ms.getAtomPointVector(bsFrom);
        M4 m4 = ScriptMathProcessor.getMatrix4f(q.getMatrix(), translation);
        ptsB = Measure.transformPoints(ptsA, m4, center);
      }
      if (!eval.useThreads())
        doAnimate = false;
      if (vwr.rotateAboutPointsInternal(eval, center, pt1, endDegrees / nSeconds,
          endDegrees, doAnimate, bsFrom, translation, ptsB, null, null)
          && doAnimate
          && eval.isJS)
        throw new ScriptInterruption(eval, "compare", 1);
    }
  }

  private void configuration() throws ScriptException {
    // if (!chk && vwr.getDisplayModelIndex() <= -2)
    // error(ERROR_backgroundModelError, "\"CONFIGURATION\"");
    BS bsAtoms = null;
    BS bsSelected = vwr.bsA();
    if (slen == 1) {
      // configuration
      bsAtoms = vwr.ms.setConformation(bsSelected);
      vwr.ms.addStateScript("select", null, bsSelected, null, "configuration",
          true, false);
      if (chk)
        return;
    } else {
      int n;
      if (isFloatParameter(1)) {
        n = intParameter(e.checkLast(1));
        if (chk)
          return;
        bsAtoms = vwr.ms.getConformation(vwr.am.cmi, n - 1, true, null);
        vwr.addStateScript("configuration " + n + ";", true, false);
      } else {
        bsAtoms = atomExpressionAt(1);
        n = intParameter(e.checkLast(e.iToken + 1));
        vwr.addStateScript("configuration " + bsAtoms + " " + n + ";", true, false);
        bsAtoms = vwr.ms.getConformation(vwr.am.cmi, n - 1, true, bsAtoms);
      }
    }
    setShapeProperty(JC.SHAPE_STICKS, "type",
        Integer.valueOf(Edge.BOND_HYDROGEN_MASK));
    e.setShapeSizeBs(JC.SHAPE_STICKS, 0, bsAtoms);
    vwr.autoHbond(bsAtoms, bsAtoms, true);
    vwr.select(bsAtoms, false, 0, e.tQuiet);
  }

  @SuppressWarnings("static-access")
  private void measure() throws ScriptException {
    ScriptEval eval = e;
    String id = null;
    int pt = 1;
    short colix = 0;
    float[] offset = null;
    if (slen == 2)
      switch (tokAt(1)) {
      case T.off:
        setShapeProperty(JC.SHAPE_MEASURES, "hideAll", Boolean.TRUE);
        return;
      case T.delete:
        if (!chk)
          vwr.clearAllMeasurements();
        return;
      }
    vwr.shm.loadShape(JC.SHAPE_MEASURES);
    switch (tokAt(1)) {
    case T.search:
      String smarts = stringParameter(slen == 3 ? 2 : 4);
      if (chk)
        return;
      Atom[] atoms = vwr.ms.at;
      int ac = vwr.ms.ac;
      int[][] maps = null;
      try {
        maps = vwr.getSmilesMatcher().getCorrelationMaps(smarts, atoms,
            ac, vwr.bsA(), true, false);
      } catch (Exception ex) {
        eval.evalError(ex.getMessage(), null);
      }
      if (maps == null)
        return;
      setShapeProperty(JC.SHAPE_MEASURES, "maps", maps);
      return;
    }
    switch (slen) {
    case 2:
      switch (getToken(pt).tok) {
      case T.nada:
      case T.on:
        vwr.shm.loadShape(JC.SHAPE_MEASURES);
        setShapeProperty(JC.SHAPE_MEASURES, "hideAll", Boolean.FALSE);
        return;
      case T.list:
        if (!chk)
          eval.showStringPrint(vwr.getMeasurementInfoAsString(), false);
        return;
      case T.string:
        setShapeProperty(JC.SHAPE_MEASURES, "setFormats", stringParameter(1));
        return;
      }
      eval.errorStr(ScriptError.ERROR_keywordExpected, "ON, OFF, DELETE");
      break;
    case 3: // measure delete N
      // search "smartsString"
      switch (getToken(1).tok) {
      case T.delete:
        if (getToken(2).tok == T.all) {
          if (!chk)
            vwr.clearAllMeasurements();
        } else {
          int i = intParameter(2) - 1;
          if (!chk)
            vwr.deleteMeasurement(i);
        }
        return;
      }
    }

    int nAtoms = 0;
    int expressionCount = 0;
    int modelIndex = -1;
    int atomIndex = -1;
    int ptFloat = -1;
    int[] countPlusIndexes = new int[5];
    float[] rangeMinMax = new float[] { Float.MAX_VALUE, Float.MAX_VALUE };
    boolean isAll = false;
    boolean isAllConnected = false;
    boolean isNotConnected = false;
    boolean isRange = true;
    RadiusData rd = null;
    Boolean intramolecular = null;
    int tokAction = T.opToggle;
    String strFormat = null;
    Font font = null;

    Lst<Object> points = new Lst<Object>();
    BS bs = new BS();
    Object value = null;
    TickInfo tickInfo = null;
    int nBitSets = 0;
    int mad = 0;
    for (int i = 1; i < slen; ++i) {
      switch (getToken(i).tok) {
      case T.id:
        if (i != 1)
          invArg();
        id = eval.optParameterAsString(++i);
        continue;
      case T.identifier:
        eval.errorStr(ScriptError.ERROR_keywordExpected,
            "ALL, ALLCONNECTED, DELETE");
        break;
      default:
        error(ScriptError.ERROR_expressionOrIntegerExpected);
        break;
      case T.opNot:
        if (tokAt(i + 1) != T.connected)
          invArg();
        i++;
        isNotConnected = true;
        break;
      case T.connected:
      case T.allconnected:
      case T.all:
        isAllConnected = (eval.theTok == T.allconnected);
        atomIndex = -1;
        isAll = true;
        if (isAllConnected && isNotConnected)
          invArg();
        break;
      case T.color:
        colix = C.getColix(eval.getArgbParam(++i));
        i = eval.iToken;
        break;
      case T.offset:
        if (eval.isPoint3f(++i)) {
          // PyMOL offsets -- {x, y, z} in angstroms
          P3 p = getPoint3f(i, false);
          offset = new float[] { 1, p.x, p.y, p.z, 0, 0, 0 };
        } else {
          offset = eval.floatParameterSet(i, 7, 7);
        }
        i = eval.iToken;
        break;
      case T.radius:
      case T.diameter:
        mad = (int) ((eval.theTok == T.radius ? 2000 : 1000) * floatParameter(++i));
        if (id != null && mad <= 0)
          mad = -1;
        break;
      case T.decimal:
        if (rd != null)
          invArg();
        isAll = true;
        isRange = true;
        ptFloat = (ptFloat + 1) % 2;
        rangeMinMax[ptFloat] = floatParameter(i);
        break;
      case T.delete:
        if (tokAction != T.opToggle)
          invArg();
        tokAction = T.delete;
        break;
      case T.font:
        float fontsize = floatParameter(++i);
        String fontface = paramAsStr(++i);
        String fontstyle = paramAsStr(++i);
        if (!chk)
          font = vwr.getFont3D(fontface, fontstyle, fontsize);
        break;
      case T.integer:
        int iParam = intParameter(i);
        if (isAll) {
          isRange = true; // irrelevant if just four integers
          ptFloat = (ptFloat + 1) % 2;
          rangeMinMax[ptFloat] = iParam;
        } else {
          atomIndex = vwr.ms.getFirstAtomIndexFromAtomNumber(iParam, vwr.getVisibleFramesBitSet());
          if (!chk && atomIndex < 0)
            return;
          if (value != null)
            invArg();
          if ((countPlusIndexes[0] = ++nAtoms) > 4)
            eval.bad();
          countPlusIndexes[nAtoms] = atomIndex;
        }
        break;
      case T.modelindex:
        modelIndex = intParameter(++i);
        break;
      case T.off:
        if (tokAction != T.opToggle)
          invArg();
        tokAction = T.off;
        break;
      case T.on:
        if (tokAction != T.opToggle)
          invArg();
        tokAction = T.on;
        break;
      case T.range:
        isAll = true;
        isRange = true; // unnecessary
        atomIndex = -1;
        break;
      case T.intramolecular:
      case T.intermolecular:
        intramolecular = Boolean.valueOf(eval.theTok == T.intramolecular);
        isAll = true;
        isNotConnected = (eval.theTok == T.intermolecular);
        break;
      case T.vanderwaals:
        if (ptFloat >= 0)
          invArg();
        rd = eval.encodeRadiusParameter(i, false, true);
        if (rd == null)
          return;
        rd.values = rangeMinMax;
        i = eval.iToken;
        isNotConnected = true;
        isAll = true;
        intramolecular = Boolean.valueOf(false);
        if (nBitSets == 1) {
          nBitSets++;
          nAtoms++;
          BS bs2 = BSUtil.copy(bs);
          BSUtil.invertInPlace(bs2, vwr.ms.ac);
          bs2.and(vwr.ms.getAtomsWithinRadius(5, bs, false, null));
          points.addLast(bs2);
        }
        break;
      case T.bitset:
      case T.expressionBegin:
      case T.leftbrace:
      case T.point3f:
      case T.dollarsign:
        if (eval.theTok == T.bitset || eval.theTok == T.expressionBegin)
          nBitSets++;
        if (atomIndex >= 0)
          invArg();
        eval.expressionResult = Boolean.FALSE;
        value = centerParameter(i);
        if (eval.expressionResult instanceof BS) {
          value = bs = (BS) eval.expressionResult;
          if (!chk && bs.length() == 0)
            return;
        }
        if (value instanceof P3) {
          Point3fi v = new Point3fi();
          v.setT((P3) value);
          v.mi = (short) modelIndex;
          value = v;
        }
        if ((nAtoms = ++expressionCount) > 4)
          eval.bad();
        i = eval.iToken;
        points.addLast(value);
        break;
      case T.string:
        // measures "%a1 %a2 %v %u"
        strFormat = stringParameter(i);
        break;
      case T.ticks:
        tickInfo = eval.tickParamAsStr(i, false, true, true);
        i = eval.iToken;
        tokAction = T.define;
        break;
      }
    }
    if (rd != null && (ptFloat >= 0 || nAtoms != 2) || nAtoms < 2 && id == null
        && (tickInfo == null || nAtoms == 1))
      eval.bad();
    if (strFormat != null && strFormat.indexOf(nAtoms + ":") != 0)
      strFormat = nAtoms + ":" + strFormat;
    if (isRange) {
      if (rangeMinMax[1] < rangeMinMax[0]) {
        rangeMinMax[1] = rangeMinMax[0];
        rangeMinMax[0] = (rangeMinMax[1] == Float.MAX_VALUE ? Float.MAX_VALUE
            : -200);
      }
    }
    if (chk)
      return;
    if (value != null || tickInfo != null) {
      if (rd == null)
        rd = new RadiusData(rangeMinMax, 0, null, null);
      if (value == null)
        tickInfo.id = "default";
      if (value != null && strFormat != null && tokAction == T.opToggle)
        tokAction = T.define;
      Text text = null;
      if (font != null)
        text = ((Text) Interface.getInterface("org.jmol.modelset.Text", vwr, "script")).newLabel(
            vwr, font, "", colix, (short) 0, 0, 0, null);
      if (text != null)
        text.pymolOffset = offset;
      setShapeProperty(
          JC.SHAPE_MEASURES,
          "measure",
          vwr.newMeasurementData(id, points).set(tokAction, null, rd, strFormat,
              null, tickInfo, isAllConnected, isNotConnected, intramolecular,
              isAll, mad, colix, text));
      return;
    }
    Object propertyValue = (id == null ? countPlusIndexes : id);
    switch (tokAction) {
    case T.delete:
      setShapeProperty(JC.SHAPE_MEASURES, "delete", propertyValue);
      break;
    case T.on:
      setShapeProperty(JC.SHAPE_MEASURES, "show", propertyValue);
      break;
    case T.off:
      setShapeProperty(JC.SHAPE_MEASURES, "hide", propertyValue);
      break;
    default:
      setShapeProperty(JC.SHAPE_MEASURES, (strFormat == null ? "toggle"
          : "toggleOn"), propertyValue);
      if (strFormat != null)
        setShapeProperty(JC.SHAPE_MEASURES, "setFormats", strFormat);
    }
  }

  /**
   * 
   * @param index
   *        0 indicates hbond command
   * 
   * @throws ScriptException
   */
  private void connect(int index) throws ScriptException {
    ScriptEval eval = e;
    final float[] distances = new float[2];
    BS[] atomSets = new BS[2];
    atomSets[0] = atomSets[1] = vwr.bsA();
    float radius = Float.NaN;
    int[] colorArgb = new int[] { Integer.MIN_VALUE };
    int distanceCount = 0;
    int bondOrder = Edge.BOND_ORDER_NULL;
    int bo;
    int operation = T.modifyorcreate;
    boolean isDelete = false;
    boolean haveType = false;
    boolean haveOperation = false;
    float translucentLevel = Float.MAX_VALUE;
    boolean isColorOrRadius = false;
    int nAtomSets = 0;
    int nDistances = 0;
    BS bsBonds = new BS();
    boolean isBonds = false;
    int expression2 = 0;
    int ptColor = 0;
    float energy = 0;
    boolean addGroup = false;
    /*
     * connect [<=2 distance parameters] [<=2 atom sets] [<=1 bond type] [<=1
     * operation]
     */

    if (slen == 1) {
      if (!chk)
        vwr.rebondState(eval.isStateScript);
      return;
    }

    for (int i = index; i < slen; ++i) {
      switch (getToken(i).tok) {
      case T.on:
      case T.off:
        checkLength(2);
        if (!chk)
          vwr.rebondState(eval.isStateScript);
        return;
      case T.integer:
      case T.decimal:
        if (nAtomSets > 0) {
          if (haveType || isColorOrRadius)
            eval.error(ScriptError.ERROR_invalidParameterOrder);
          bo = Edge.getBondOrderFromFloat(floatParameter(i));
          if (bo == Edge.BOND_ORDER_NULL)
            invArg();
          bondOrder = bo;
          haveType = true;
          break;
        }
        if (++nDistances > 2)
          eval.bad();
        float dist = floatParameter(i);
        if (tokAt(i + 1) == T.percent) {
          dist = -dist / 100f;
          i++;
        }
        distances[distanceCount++] = dist;
        break;
      case T.bitset:
      case T.expressionBegin:
        if (nAtomSets > 2 || isBonds && nAtomSets > 0)
          eval.bad();
        if (haveType || isColorOrRadius)
          invArg();
        atomSets[nAtomSets++] = atomExpressionAt(i);
        isBonds = eval.isBondSet;
        if (nAtomSets == 2) {
          int pt = eval.iToken;
          for (int j = i; j < pt; j++)
            if (tokAt(j) == T.identifier && paramAsStr(j).equals("_1")) {
              expression2 = i;
              break;
            }
          eval.iToken = pt;
        }
        i = eval.iToken;
        break;
      case T.group:
        addGroup = true;
        break;
      case T.color:
      case T.translucent:
      case T.opaque:
        isColorOrRadius = true;
        translucentLevel = getColorTrans(eval, i, false, colorArgb);
        i = eval.iToken;
        break;
      case T.pdb:
        boolean isAuto = (tokAt(2) == T.auto);
        checkLength(isAuto ? 3 : 2);
        if (chk)
          return;
        // from eval
        vwr.clearModelDependentObjects();
        vwr.ms.deleteAllBonds();
        BS bsExclude = new BS();
        vwr.ms.setPdbConectBonding(0, 0, bsExclude);
        if (isAuto) {
          boolean isLegacy = eval.isStateScript && vwr.getBoolean(T.legacyautobonding);
          vwr.ms.autoBondBs4(null, null, bsExclude, null, vwr.getMadBond(), isLegacy);
          vwr.addStateScript(
              (isLegacy ? "set legacyAutoBonding TRUE;connect PDB AUTO;set legacyAutoBonding FALSE;"
                  : "connect PDB auto;"), false, true);
          return;
        }
        vwr.addStateScript("connect PDB;", false, true);
        return;
      case T.adjust:
      case T.auto:
      case T.create:
      case T.modify:
      case T.modifyorcreate:
        // must be an operation and must be last argument
        haveOperation = true;
        if (++i != slen)
          invArg();
        operation = eval.theTok;
        if (operation == T.auto
            && !(bondOrder == Edge.BOND_ORDER_NULL
                || bondOrder == Edge.BOND_H_REGULAR || bondOrder == Edge.BOND_AROMATIC))
          invArg();
        break;
      case T.struts:
        if (!isColorOrRadius) {
          colorArgb[0] = 0xFFFFFF;
          translucentLevel = 0.5f;
          radius = vwr.getFloat(T.strutdefaultradius);
          isColorOrRadius = true;
        }
        if (!haveOperation)
          operation = T.modifyorcreate;
        haveOperation = true;
        //$FALL-THROUGH$
      case T.identifier:
        if (eval.isColorParam(i)) {
          ptColor = -i;
          break;
        }
        //$FALL-THROUGH$
      case T.aromatic:
      case T.hbond:
        //if (i > 0) {
        // not hbond command
        // I know -- should have required the COLOR keyword
        //}
        String cmd = paramAsStr(i);
        if ((bo = ScriptParam.getBondOrderFromString(cmd)) == Edge.BOND_ORDER_NULL) {
          invArg();
        }
        // must be bond type
        if (haveType)
          eval.error(ScriptError.ERROR_incompatibleArguments);
        haveType = true;
        switch (bo) {
        case Edge.BOND_PARTIAL01:
          switch (tokAt(i + 1)) {
          case T.decimal:
            bo = ScriptParam.getPartialBondOrderFromFloatEncodedInt(st[++i].intValue);
            break;
          case T.integer:
            bo = (short) intParameter(++i);
            break;
          }
          break;
        case Edge.BOND_H_REGULAR:
          if (tokAt(i + 1) == T.integer) {
            bo = (short) (intParameter(++i) << Edge.BOND_HBOND_SHIFT);
            energy = floatParameter(++i);
          }
          break;
        }
        bondOrder = bo;
        break;
      case T.radius:
        radius = floatParameter(++i);
        isColorOrRadius = true;
        break;
      case T.none:
      case T.delete:
        if (++i != slen)
          invArg();
        operation = T.delete;
        // if (isColorOrRadius) / for struts automatic color
        // invArg();
        isDelete = true;
        isColorOrRadius = false;
        break;
      default:
        ptColor = i;
        break;
      }
      // now check for color -- -i means we've already checked
      if (i > 0) {
        if (ptColor == -i || ptColor == i && eval.isColorParam(i)) {
          isColorOrRadius = true;
          colorArgb[0] = eval.getArgbParam(i);
          i = eval.iToken;
        } else if (ptColor == i) {
          invArg();
        }
      }
    }
    if (chk)
      return;
    if (distanceCount < 2) {
      if (distanceCount == 0)
        distances[0] = JC.DEFAULT_MAX_CONNECT_DISTANCE;
      distances[1] = distances[0];
      distances[0] = JC.DEFAULT_MIN_CONNECT_DISTANCE;
    }
    if (isColorOrRadius) {
      if (!haveType)
        bondOrder = Edge.BOND_ORDER_ANY;
      if (!haveOperation)
        operation = T.modify;
    }
    int nNew = 0;
    int nModified = 0;
    int[] result;
    if (expression2 > 0) {
      BS bs = new BS();
      vwr.definedAtomSets.put("_1", bs);
      BS bs0 = atomSets[0];
      for (int atom1 = bs0.nextSetBit(0); atom1 >= 0; atom1 = bs0
          .nextSetBit(atom1 + 1)) {
        bs.set(atom1);
        result = vwr.makeConnections(distances[0], distances[1], bondOrder,
            operation, bs, atomExpressionAt(expression2), bsBonds, isBonds,
            false, 0);
        nNew += Math.abs(result[0]);
        nModified += result[1];
        bs.clear(atom1);
      }
    } else {
      result = vwr.makeConnections(distances[0], distances[1], bondOrder,
          operation, atomSets[0], atomSets[1], bsBonds, isBonds, addGroup,
          energy);
      nNew += Math.abs(result[0]);
      nModified += result[1];
    }
    boolean report = eval.doReport(); 
    if (isDelete) {
      if (report)
        eval.report(GT.i(GT._("{0} connections deleted"), nModified));
      return;
    }
    if (isColorOrRadius) {
      vwr.selectBonds(bsBonds);
      if (!Float.isNaN(radius))
        eval.setShapeSizeBs(JC.SHAPE_STICKS, Math.round(radius * 2000), null);
      finalizeObject(JC.SHAPE_STICKS, colorArgb[0], translucentLevel, 0, false,
          null, 0, bsBonds);
      vwr.selectBonds(null);
    }
    if (report)
      eval.report(GT.o(GT._("{0} new bonds; {1} modified"),
          new Object[] { Integer.valueOf(nNew), Integer.valueOf(nModified) }));
  }

  private void console() throws ScriptException {
    switch (getToken(1).tok) {
    case T.off:
      if (!chk)
        vwr.showConsole(false);
      break;
    case T.on:
      if (!chk)
        vwr.showConsole(true);
      break;
    case T.clear:
      if (!chk)
        vwr.sm.clearConsole();
      break;
    case T.write:
      showString(stringParameter(2));
      break;
    default:
      invArg();
    }
  }

  private void data() throws ScriptException {
    ScriptEval eval = e;
    String dataString = null;
    String dataLabel = null;
    boolean isOneValue = false;
    int i;
    switch (eval.iToken = slen) {
    case 5:
      // parameters 3 and 4 are just for the ride: [end] and ["key"]
      dataString = paramAsStr(2);
      //$FALL-THROUGH$
    case 4:
    case 2:
      dataLabel = paramAsStr(1);
      if (dataLabel.equalsIgnoreCase("clear")) {
        if (!chk)
          vwr.setData(null, null, 0, 0, 0, 0, 0);
        return;
      }
      if ((i = dataLabel.indexOf("@")) >= 0) {
        dataString = ""
            + eval.getParameter(dataLabel.substring(i + 1), T.string, true);
        dataLabel = dataLabel.substring(0, i).trim();
      } else if (dataString == null && (i = dataLabel.indexOf(" ")) >= 0) {
        dataString = dataLabel.substring(i + 1).trim();
        dataLabel = dataLabel.substring(0, i).trim();
        isOneValue = true;
      }
      break;
    default:
      eval.bad();
    }
    String dataType = dataLabel + " ";
    dataType = dataType.substring(0, dataType.indexOf(" ")).toLowerCase();
    if (dataType.equals("model") || dataType.equals("append")) {
      eval.cmdLoad();
      return;
    }
    if (chk)
      return;
    boolean isDefault = (dataLabel.toLowerCase().indexOf("(default)") >= 0);
    if (dataType.equals("connect_atoms")) {
      vwr.ms.connect((float[][]) parseDataArray(dataString, false));
      return;
    }
    if (dataType.indexOf("ligand_") == 0) {
      // ligand structure for pdbAddHydrogen
      vwr.setLigandModel(dataLabel.substring(7).toUpperCase() + "_data",
          dataString.trim());
      return;
    }
    if (dataType.indexOf("file_") == 0) {
      // ligand structure for pdbAddHydrogen
      vwr.setLigandModel(dataLabel.substring(5) + "_file",
          dataString.trim());
      return;
    }
    Object[] d = new Object[4];
    // not saving this data in the state?
    if (dataType.equals("element_vdw")) {
      // vdw for now
      d[0] = dataType;
      d[1] = dataString.replace(';', '\n');
      int n = Elements.elementNumberMax;
      int[] eArray = new int[n + 1];
      for (int ie = 1; ie <= n; ie++)
        eArray[ie] = ie;
      d[2] = eArray;
      d[3] = Integer.valueOf(JmolDataManager.DATA_TYPE_STRING);
      vwr.setData("element_vdw", d, n, 0, 0, 0, 0);
      return;
    }
    if (dataType.indexOf("data2d_") == 0) {
      // data2d_someName
      d[0] = dataLabel;
      d[1] = parseDataArray(dataString, false);
      d[3] = Integer.valueOf(JmolDataManager.DATA_TYPE_AFF);
      vwr.setData(dataLabel, d, 0, 0, 0, 0, 0);
      return;
    }
    if (dataType.indexOf("data3d_") == 0) {
      // data3d_someName
      d[0] = dataLabel;
      d[1] = parseDataArray(dataString, true);
      d[3] = Integer.valueOf(JmolDataManager.DATA_TYPE_AFFF);
      vwr.setData(dataLabel, d, 0, 0, 0, 0, 0);
      return;
    }
    String[] tokens = PT.getTokens(dataLabel);
    if (dataType.indexOf("property_") == 0
        && !(tokens.length == 2 && tokens[1].equals("set"))) {
      BS bs = vwr.bsA();
      d[0] = dataType;
      int atomNumberField = (isOneValue ? 0 : ((Integer) vwr
          .getP("propertyAtomNumberField")).intValue());
      int atomNumberFieldColumnCount = (isOneValue ? 0 : ((Integer) vwr
          .getP("propertyAtomNumberColumnCount")).intValue());
      int propertyField = (isOneValue ? Integer.MIN_VALUE : ((Integer) vwr
          .getP("propertyDataField")).intValue());
      int propertyFieldColumnCount = (isOneValue ? 0 : ((Integer) vwr
          .getP("propertyDataColumnCount")).intValue());
      if (!isOneValue && dataLabel.indexOf(" ") >= 0) {
        if (tokens.length == 3) {
          // DATA "property_whatever [atomField] [propertyField]"
          dataLabel = tokens[0];
          atomNumberField = PT.parseInt(tokens[1]);
          propertyField = PT.parseInt(tokens[2]);
        }
        if (tokens.length == 5) {
          // DATA
          // "property_whatever [atomField] [atomFieldColumnCount] [propertyField] [propertyDataColumnCount]"
          dataLabel = tokens[0];
          atomNumberField = PT.parseInt(tokens[1]);
          atomNumberFieldColumnCount = PT.parseInt(tokens[2]);
          propertyField = PT.parseInt(tokens[3]);
          propertyFieldColumnCount = PT.parseInt(tokens[4]);
        }
      }
      if (atomNumberField < 0)
        atomNumberField = 0;
      if (propertyField < 0)
        propertyField = 0;
      int ac = vwr.ms.ac;
      int[] atomMap = null;
      BS bsTemp = BS.newN(ac);
      if (atomNumberField > 0) {
        atomMap = new int[ac + 2];
        for (int j = 0; j <= ac; j++)
          atomMap[j] = -1;
        for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1)) {
          int atomNo = vwr.ms.at[j].getAtomNumber();
          if (atomNo > ac + 1 || atomNo < 0 || bsTemp.get(atomNo))
            continue;
          bsTemp.set(atomNo);
          atomMap[atomNo] = j;
        }
        d[2] = atomMap;
      } else {
        d[2] = BSUtil.copy(bs);
      }
      d[1] = dataString;
      d[3] = Integer.valueOf(JmolDataManager.DATA_TYPE_STRING);
      vwr.setData(dataType, d, ac, atomNumberField,
          atomNumberFieldColumnCount, propertyField, propertyFieldColumnCount);
      return;
    }
    if ("occupany".equals(dataType))
        dataType = "occupancy"; // legacy misspelling in states
    int userType = AtomCollection.getUserSettableType(dataType);
    if (userType >= 0) {
      // this is a known settable type or "property_xxxx"
      vwr.setAtomData(userType, dataType, dataString, isDefault);
      return;
    }
    // this is just information to be stored.
    d[0] = dataLabel;
    d[1] = dataString;
    d[3] = Integer.valueOf(JmolDataManager.DATA_TYPE_STRING);
    vwr.setData(dataType, d, 0, 0, 0, 0, 0);
  }

  private void ellipsoid() throws ScriptException {
    ScriptEval eval = e;
    int mad = 0;
    int i = 1;
    float translucentLevel = Float.MAX_VALUE;
    boolean checkMore = false;
    boolean isSet = false;
    setShapeProperty(JC.SHAPE_ELLIPSOIDS, "thisID", null);
    // the first three options, ON, OFF, and (int)scalePercent
    // were implemented long before the idea of customized 
    // ellipsoids was considered. "ON" will produce an ellipsoid
    // with a standard radius, and "OFF" will reduce its scale to 0,
    // effectively elliminating it.

    // The new options SET and ID are much more powerful. In those, 
    // ON and OFF simply do that -- turn the ellipsoid on or off --
    // and there are many more options.

    // The SET type ellipsoids, introduced in Jmol 13.1.19 in 7/2013,
    // are created by all readers that read ellipsoid (PDB/CIF) or 
    // tensor (Castep, MagRes) data.

    switch (getToken(1).tok) {
    case T.on:
      mad = Integer.MAX_VALUE; // default for this type
      break;
    case T.off:
      break;
    case T.integer:
      mad = intParameter(1);
      break;
    case T.set:
      e.sm.loadShape(JC.SHAPE_ELLIPSOIDS);
      setShapeProperty(JC.SHAPE_ELLIPSOIDS, "select", paramAsStr(2));
      i = eval.iToken;
      checkMore = true;
      isSet = true;
      break;
    case T.id:
    case T.times:
    case T.identifier:
      e.sm.loadShape(JC.SHAPE_ELLIPSOIDS);
      if (eval.theTok == T.id)
        i++;
      setShapeId(JC.SHAPE_ELLIPSOIDS, i, false);
      i = eval.iToken;
      checkMore = true;
      break;
    default:
      invArg();
    }
    if (!checkMore) {
      eval.setShapeSizeBs(JC.SHAPE_ELLIPSOIDS, mad, null);
      return;
    }
    int[] colorArgb = new int[] { Integer.MIN_VALUE };
    while (++i < slen) {
      String key = paramAsStr(i);
      Object value = null;
      getToken(i);
      if (!isSet)
        switch (eval.theTok) {
        case T.dollarsign:
          key = "points";
          Object[] data = new Object[3];
          data[0] = eval.objectNameParameter(++i);
          if (chk)
            continue;
          eval.getShapePropertyData(JC.SHAPE_ISOSURFACE, "getVertices", data);
          value = data;
          break;
        case T.axes:
          V3[] axes = new V3[3];
          for (int j = 0; j < 3; j++) {
            axes[j] = new V3();
            axes[j].setT(centerParameter(++i));
            i = eval.iToken;
          }
          value = axes;
          break;
        case T.center:
          value = centerParameter(++i);
          i = eval.iToken;
          break;
        case T.modelindex:
          value = Integer.valueOf(intParameter(++i));
          break;
        case T.delete:
          value = Boolean.TRUE;
          checkLength(i + 1);
          break;
        }
      // these next are for SET "XXX" or ID "XXX" syntax only
      if (value == null)
        switch (eval.theTok) {
        case T.on:
          key = "on";
          value = Boolean.TRUE;
          break;
        case T.off:
          key = "on";
          value = Boolean.FALSE;
          break;
        case T.scale:
          value = Float.valueOf(floatParameter(++i));
          break;
        case T.bitset:
        case T.expressionBegin:
          key = "atoms";
          value = atomExpressionAt(i);
          i = eval.iToken;
          break;
        case T.color:
        case T.translucent:
        case T.opaque:
          translucentLevel = getColorTrans(eval, i, true, colorArgb);
          i = eval.iToken;
          continue;
        case T.options:
          value = paramAsStr(++i);
          break;
        }
      if (value == null)
        invArg();
      setShapeProperty(JC.SHAPE_ELLIPSOIDS, key.toLowerCase(), value);
    }
    finalizeObject(JC.SHAPE_ELLIPSOIDS, colorArgb[0], translucentLevel, 0,
        false, null, 0, null);
    setShapeProperty(JC.SHAPE_ELLIPSOIDS, "thisID", null);
  }

  private void image() throws ScriptException {
    // image ...
    // image id XXXX...
    // ... "filename"
    // ... close
    // ... close "filename"
    if (!chk)
      vwr.getConsole();// especially important for JavaScript
    int pt = 1;
    String id = null;
    if (tokAt(1) == T.id) {
      // image ID ...
      id = e.optParameterAsString(++pt);
      pt++;
    }
    String fileName = e.optParameterAsString(pt);
    boolean isClose = e.optParameterAsString(slen - 1).equalsIgnoreCase("close");
    if (!isClose && (slen == pt || slen == pt + 2)) {
      // image
      // image 400 400
      // image id "testing"
      // image id "testing" 400 400
      int width = (slen == pt + 2 ? intParameter(pt++) : -1);
      int height = (width < 0 ? -1 : intParameter(pt));
      Map<String, Object> params = new Hashtable<String, Object>();
      params.put("fileName", "\1\1" + id);
      params.put("backgroundColor", Integer.valueOf(vwr.getBackgroundArgb()));
      params.put("type", "png");
      params.put("quality", Integer.valueOf(-1));
      params.put("width", Integer.valueOf(width));
      params.put("height", Integer.valueOf(height));
      if (!chk)
        vwr.processWriteOrCapture(params);
      return;
    }
    pt++;
    if (isClose) {
      switch (slen) {
      case 2:
        // image close
        fileName = "closeall";
        break;
      case 3: 
      case 4:
        // image "filename" close
        // image ID "testing" close
        break;
      default:
        checkLength(0);
      }
    }
    if (!chk)
      vwr.fm.loadImage(isClose ? "\1close" : fileName, "\1" + fileName + "\1"
          + ("".equals(id) || id == null ? null : id));
  }
  
  private void mapProperty() throws ScriptException {
    // map {1.1}.straightness  {2.1}.property_x resno
    BS bsFrom, bsTo;
    String property1, property2, mapKey;
    int tokProp1 = 0;
    int tokProp2 = 0;
    int tokKey = 0;
    ScriptEval eval = this.e;
    while (true) {
      if (tokAt(1) == T.selected) {
        bsFrom = vwr.bsA();
        bsTo = atomExpressionAt(2);
        property1 = property2 = "selected";
      } else {
        bsFrom = atomExpressionAt(1);
        if (tokAt(++eval.iToken) != T.per
            || !T.tokAttr(tokProp1 = tokAt(++eval.iToken), T.atomproperty))
          break;
        property1 = paramAsStr(eval.iToken);
        bsTo = atomExpressionAt(++eval.iToken);
        if (tokAt(++eval.iToken) != T.per
            || !T.tokAttr(tokProp2 = tokAt(++eval.iToken), T.settable))
          break;
        property2 = paramAsStr(eval.iToken);
      }
      if (T.tokAttr(tokKey = tokAt(eval.iToken + 1), T.atomproperty))
        mapKey = paramAsStr(++eval.iToken);
      else
        mapKey = T.nameOf(tokKey = T.atomno);
      eval.checkLast(eval.iToken);
      if (chk)
        return;
      BS bsOut = null;
      showString("mapping " + property1.toUpperCase() + " for "
          + bsFrom.cardinality() + " atoms to " + property2.toUpperCase()
          + " for " + bsTo.cardinality() + " atoms using "
          + mapKey.toUpperCase());
      if (T.tokAttrOr(tokProp1, T.intproperty, T.floatproperty)
          && T.tokAttrOr(tokProp2, T.intproperty, T.floatproperty)
          && T.tokAttrOr(tokKey, T.intproperty, T.floatproperty)) {
        float[] data1 = eval.getBitsetPropertyFloat(bsFrom, tokProp1
            | T.selectedfloat, Float.NaN, Float.NaN);
        float[] data2 = eval.getBitsetPropertyFloat(bsFrom, tokKey
            | T.selectedfloat, Float.NaN, Float.NaN);
        float[] data3 = eval.getBitsetPropertyFloat(bsTo, tokKey
            | T.selectedfloat, Float.NaN, Float.NaN);
        boolean isProperty = (tokProp2 == T.property);
        float[] dataOut = new float[isProperty ? vwr.ms.ac
            : data3.length];
        bsOut = new BS();
        if (data1.length == data2.length) {
          Map<Float, Float> ht = new Hashtable<Float, Float>();
          for (int i = 0; i < data1.length; i++) {
            ht.put(Float.valueOf(data2[i]), Float.valueOf(data1[i]));
          }
          int pt = -1;
          int nOut = 0;
          for (int i = 0; i < data3.length; i++) {
            pt = bsTo.nextSetBit(pt + 1);
            Float F = ht.get(Float.valueOf(data3[i]));
            if (F == null)
              continue;
            bsOut.set(pt);
            dataOut[(isProperty ? pt : nOut)] = F.floatValue();
            nOut++;
          }
          // note: this was DATA_TYPE_STRING ?? 
          if (isProperty)
            vwr.setData(property2, new Object[] { property2, dataOut, bsOut,
                Integer.valueOf(JmolDataManager.DATA_TYPE_AF), Boolean.TRUE }, vwr.ms.ac, 0,
                0, Integer.MAX_VALUE, 0);
          else
            vwr.setAtomProperty(bsOut, tokProp2, 0, 0, null, dataOut, null);
        }
      }
      if (bsOut == null) {
        String format = "{" + mapKey + "=%[" + mapKey + "]}." + property2
            + " = %[" + property1 + "]";
        String[] data = (String[]) getBitsetIdent(bsFrom, format, null, false,
            Integer.MAX_VALUE, false);
        SB sb = new SB();
        for (int i = 0; i < data.length; i++)
          if (data[i].indexOf("null") < 0)
            sb.append(data[i]).appendC('\n');
        if (Logger.debugging)
          Logger.debug(sb.toString());
        BS bsSubset = BSUtil.copy(vwr.slm.bsSubset);
        vwr.slm.setSelectionSubset(bsTo);
        try {
          eval.runScript(sb.toString());
        } catch (Exception ex) {
          vwr.slm.setSelectionSubset(bsSubset);
          eval.errorStr(-1, "Error: " + ex.getMessage());
        } catch (Error er) {
          vwr.slm.setSelectionSubset(bsSubset);
          eval.errorStr(-1, "Error: " + er.toString());
        }
        vwr.slm.setSelectionSubset(bsSubset);
      }
      showString("DONE");
      return;
    }
    invArg();
  }

  private void minimize() throws ScriptException {
    BS bsSelected = null;
    int steps = Integer.MAX_VALUE;
    float crit = 0;
    boolean addHydrogen = false;
    boolean isSilent = false;
    BS bsFixed = null;
    boolean isOnly = false;
    MinimizerInterface minimizer = vwr.getMinimizer(false);
    // may be null
    for (int i = 1; i < slen; i++)
      switch (getToken(i).tok) {
      case T.addhydrogens:
        addHydrogen = true;
        continue;
      case T.cancel:
      case T.stop:
        checkLength(2);
        if (chk || minimizer == null)
          return;
        minimizer.setProperty(paramAsStr(i), null);
        return;
      case T.clear:
        checkLength(2);
        if (chk || minimizer == null)
          return;
        minimizer.setProperty("clear", null);
        return;
      case T.constraint:
        if (i != 1)
          invArg();
        int n = 0;
        float targetValue = 0;
        int[] aList = new int[5];
        if (tokAt(++i) == T.clear) {
          checkLength(3);
        } else {
          while (n < 4 && !isFloatParameter(i)) {
            aList[++n] = atomExpressionAt(i).nextSetBit(0);
            i = e.iToken + 1;
          }
          aList[0] = n;
          if (n == 1)
            invArg();
          targetValue = floatParameter(e.checkLast(i));
        }
        if (!chk)
          vwr.getMinimizer(true).setProperty("constraint",
              new Object[] { aList, new int[n], Float.valueOf(targetValue) });
        return;
      case T.criterion:
        crit = floatParameter(++i);
        continue;
      case T.energy:
        steps = 0;
        continue;
      case T.fixed:
        if (i != 1)
          invArg();
        bsFixed = atomExpressionAt(++i);
        if (bsFixed.nextSetBit(0) < 0)
          bsFixed = null;
        i = e.iToken;
        if (!chk)
          vwr.getMinimizer(true).setProperty("fixed", bsFixed);
        if (i + 1 == slen)
          return;
        continue;
      case T.bitset:
      case T.expressionBegin:
        isOnly = true;
        //$FALL-THROUGH$
      case T.select:
        if (e.theTok == T.select)
          i++;
        bsSelected = atomExpressionAt(i);
        i = e.iToken;
        if (tokAt(i + 1) == T.only) {
          i++;
          isOnly = true;
        }
        continue;
      case T.silent:
        isSilent = true;
        break;
      case T.step:
        steps = intParameter(++i);
        continue;
      default:
        invArg();
        break;
      }
    if (!chk)
      try {
        vwr.minimize(e, steps, crit, bsSelected, bsFixed, 0, addHydrogen, isOnly,
            isSilent, false);
      } catch (Exception e1) {
        // actually an async exception
        throw new ScriptInterruption(e, "minimize", 1);
      }
  }

  /**
   * Allows for setting one or more specific t-values as well as full unit-cell
   * shifts (multiples of q).
   * 
   * @throws ScriptException
   */
  private void modulation() throws ScriptException {

    // modulation on/off  (all atoms)
    // modulation vectors on/off
    // modulation {atom set} on/off
    // modulation int  q-offset
    // modulation x.x  t-offset
    // modulation {t1 t2 t3} 
    // modulation {q1 q2 q3} TRUE 
    P3 qtOffset = null;
    //    int frameN = Integer.MAX_VALUE;
    ScriptEval eval = e;
    boolean mod = true;
    boolean isQ = false;
    BS bs = null;
    int i = 1;
    switch (getToken(i).tok) {
    case T.off:
      mod = false;
      //$FALL-THROUGH$
    case T.nada:
    case T.on:
      break;
    case T.bitset:
    case T.expressionBegin:
      bs = atomExpressionAt(1);
      switch (tokAt(eval.iToken + 1)) {
      case T.nada:
        break;
      case T.off:
        mod = false;
        //$FALL-THROUGH$
      case T.on:
        eval.iToken++;
        break;
      }
      eval.checkLast(eval.iToken);
      break;
    case T.leftbrace:
    case T.point3f:
      qtOffset = eval.getPoint3f(1, false);
      isQ = (tokAt(eval.iToken + 1) == T.on);
      break;
    case T.identifier:
      String s = eval.theToken.value.toString();
      i++;
      if (s.equalsIgnoreCase("t")) {
        eval.theTok = T.decimal;
      } else if (s.equalsIgnoreCase("m") || s.equalsIgnoreCase("q")) {
        eval.theTok = T.integer;
      } else {
        invArg();
      }
      //$FALL-THROUGH$
    case T.decimal:
    case T.integer:
      // allows for form of number -- integer or float -- to determine type,
      // but allso allows using "t" or "q" followed by a number or {t1 t2 t3}
      switch (eval.theTok) {
      case T.decimal:
        if (isFloatParameter(i)) {
          float t1 = floatParameter(i);
          qtOffset = P3.new3(t1, t1, t1);
        } else {
          qtOffset = eval.getPoint3f(i, false);
        }
        break;
      case T.integer:
        if (tokAt(i) == T.integer) {
          int t = intParameter(i);
          qtOffset = P3.new3(t, t, t);
        } else {
          qtOffset = eval.getPoint3f(i, false);
        }
        isQ = true;
        break;
      }
      break;
    case T.scale:
      float scale = floatParameter(2);
      if (!chk)
        vwr.setFloatProperty("modulationScale", scale);
      return;
      //    case T.fps:
      //      float f = floatParameter(2);
      //      if (!chk)
      //        vwr.setModulationFps(f);
      //      return;
      //    case T.play:
      //      int t0 = intParameter(2);
      //      frameN = intParameter(3);
      //      qtOffset = P3.new3(t0, t0, t0);
      //      isQ = true;
      //      break;
    default:
      invArg();
    }
    if (!chk) {
      vwr.tm.setVibrationPeriod(0);
      vwr.setModulation(bs, mod, qtOffset, isQ);
    }
  }

  private void mutate() throws ScriptException {
    // mutate {resno} "LYS" or "file identifier"
    // mutate {1-3} ~GGGL
    BS bs;
    int i;
    switch (tokAt(1)) {
    case T.integer:
      st[1] = T.o(T.string, "" + st[1].value); // allows @x and "*"
      //$FALL-THROUGH$
    default:
      bs = atomExpressionAt(1);
      i = ++e.iToken;
      break;
    case T.times:
      bs = vwr.getAllAtoms();
      i = 2;
      break;
    }

    // check for last model 
    bs.and(vwr.getModelUndeletedAtomsBitSet(vwr.ms.mc - 1));
    int iatom = bs.length() - 1;
    if (iatom < 0 || vwr.ms.at[iatom].mi + 1 != vwr.ms.mc
        || vwr.ms.isTrajectory(vwr.ms.at[iatom].mi))
      return;
    String group = e.optParameterAsString(i);
    e.checkLast(i);
    if (chk || !vwr.ms.am[vwr.ms.mc - 1].isBioModel)
      return;
    boolean isFile = (tokAt(i) == T.string && !group.startsWith("~"));
    String[] list = null;
    if (isFile) {
      list = new String[] { group };
      group = null;
    } else {
      group = PT.replaceAllCharacters(group, ",; \t\n", " ").trim()
          .toUpperCase();
      boolean isOneLetter = group.startsWith("~"); 
      if (isOneLetter || group.length() != 3
          || !vwr.getJBR().isKnownPDBGroup(group, 20))
        group = vwr.getJBR().toStdAmino3(isOneLetter ? group.substring(1) : group);
      list = PT.getTokens(group);
    }
    if (list.length > 0)
      vwr.ms.bioModelset.mutate(bs, group, list);
  }

  private void navigate() throws ScriptException {
    /*
     * navigation on/off navigation depth p # would be as a depth value, like
     * slab, in percent, but could be negative navigation nSec translate X Y #
     * could be percentages navigation nSec translate $object # could be a draw
     * object navigation nSec translate (atom selection) #average of values
     * navigation nSec center {x y z} navigation nSec center $object navigation
     * nSec center (atom selection) navigation nSec path $object navigation nSec
     * path {x y z theta} {x y z theta}{x y z theta}{x y z theta}... navigation
     * nSec trace (atom selection)
     */
    ScriptEval eval = e;
    if (slen == 1) {
      eval.setBooleanProperty("navigationMode", true);
      return;
    }
    V3 rotAxis = V3.new3(0, 1, 0);
    Lst<Object[]> list = new Lst<Object[]>();
    P3 pt;
    if (slen == 2) {
      switch (getToken(1).tok) {
      case T.on:
      case T.off:
        if (chk)
          return;
        eval.setObjectMad(JC.SHAPE_AXES, "axes", 1);
        setShapeProperty(JC.SHAPE_AXES, "position",
            P3.new3(50, 50, Float.MAX_VALUE));
        eval.setBooleanProperty("navigationMode", true);
        vwr.tm.setNavOn(eval.theTok == T.on);
        return;
      case T.stop:
        if (!chk)
          vwr.tm.setNavXYZ(0, 0, 0);
        return;
      case T.point3f:
      case T.trace:
        break;
      default:
        invArg();
      }
    }
    if (!chk && !vwr.getBoolean(T.navigationmode))
      eval.setBooleanProperty("navigationMode", true);
    for (int i = 1; i < slen; i++) {
      float timeSec = (isFloatParameter(i) ? floatParameter(i++) : 2f);
      if (timeSec < 0)
        invArg();
      if (!chk && timeSec > 0)
        eval.refresh(false);
      switch (getToken(i).tok) {
      case T.point3f:
      case T.leftbrace:
        // navigate {x y z}
        pt = getPoint3f(i, true);
        eval.iToken++;
        if (eval.iToken != slen)
          invArg();
        if (!chk)
          vwr.tm.setNavXYZ(pt.x, pt.y, pt.z);
        return;
      case T.depth:
        float depth = floatParameter(++i);
        if (!chk)
          list.addLast(new Object[] { Integer.valueOf(T.depth),
              Float.valueOf(timeSec), Float.valueOf(depth) });
        //vwr.setNavigationDepthPercent(timeSec, depth);
        continue;
      case T.center:
        pt = centerParameter(++i);
        i = eval.iToken;
        if (!chk)
          list.addLast(new Object[] { Integer.valueOf(T.point),
              Float.valueOf(timeSec), pt });
        //vwr.navigatePt(timeSec, pt);
        continue;
      case T.rotate:
        switch (getToken(++i).tok) {
        case T.x:
          rotAxis.set(1, 0, 0);
          i++;
          break;
        case T.y:
          rotAxis.set(0, 1, 0);
          i++;
          break;
        case T.z:
          rotAxis.set(0, 0, 1);
          i++;
          break;
        case T.point3f:
        case T.leftbrace:
          rotAxis.setT(getPoint3f(i, true));
          i = eval.iToken + 1;
          break;
        case T.identifier:
          invArg(); // for now
          break;
        }
        float degrees = floatParameter(i);
        if (!chk)
          list.addLast(new Object[] { Integer.valueOf(T.rotate),
              Float.valueOf(timeSec), rotAxis, Float.valueOf(degrees) });
        //          vwr.navigateAxis(timeSec, rotAxis, degrees);
        continue;
      case T.translate:
        float x = Float.NaN;
        float y = Float.NaN;
        if (isFloatParameter(++i)) {
          x = floatParameter(i);
          y = floatParameter(++i);
        } else {
          switch (tokAt(i)) {
          case T.x:
            x = floatParameter(++i);
            break;
          case T.y:
            y = floatParameter(++i);
            break;
          default:
            pt = centerParameter(i);
            i = eval.iToken;
            if (!chk)
              list.addLast(new Object[] { Integer.valueOf(T.translate),
                  Float.valueOf(timeSec), pt });
            //vwr.navTranslate(timeSec, pt);
            continue;
          }
        }
        if (!chk)
          list.addLast(new Object[] { Integer.valueOf(T.percent),
              Float.valueOf(timeSec), Float.valueOf(x), Float.valueOf(y) });
        //vwr.navTranslatePercent(timeSec, x, y);
        continue;
      case T.divide:
        continue;
      case T.trace:
        P3[][] pathGuide;
        Lst<P3[]> vp = new Lst<P3[]>();
        BS bs;
        if (tokAt(i + 1) == T.bitset || tokAt(i + 1) == T.expressionBegin) {
          bs = atomExpressionAt(++i);
          i = eval.iToken;
        } else {
          bs = vwr.bsA();
        }
        if (chk)
          return;
        vwr.getPolymerPointsAndVectors(bs, vp);
        int n;
        if ((n = vp.size()) > 0) {
          pathGuide = new P3[n][];
          for (int j = 0; j < n; j++) {
            pathGuide[j] = vp.get(j);
          }
          list.addLast(new Object[] { Integer.valueOf(T.trace),
              Float.valueOf(timeSec), pathGuide });
          //vwr.navigateGuide(timeSec, pathGuide);
          continue;
        }
        break;
      case T.path:
        P3[] path;
        float[] theta = null; // orientation; null for now
        if (getToken(i + 1).tok == T.dollarsign) {
          i++;
          // navigate timeSeconds path $id indexStart indexEnd
          String pathID = eval.objectNameParameter(++i);
          if (chk)
            return;
          setShapeProperty(JC.SHAPE_DRAW, "thisID", pathID);
          path = (P3[]) getShapeProperty(JC.SHAPE_DRAW, "vertices");
          eval.refresh(false);
          if (path == null)
            invArg();
          int indexStart = (int) (isFloatParameter(i + 1) ? floatParameter(++i)
              : 0);
          int indexEnd = (int) (isFloatParameter(i + 1) ? floatParameter(++i)
              : Integer.MAX_VALUE);
          list.addLast(new Object[] { Integer.valueOf(T.path),
              Float.valueOf(timeSec), path, theta,
              new int[] { indexStart, indexEnd } });
          //vwr.navigatePath(timeSec, path, theta, indexStart, indexEnd);
          continue;
        }
        Lst<P3> v = new Lst<P3>();
        while (eval.isCenterParameter(i + 1)) {
          v.addLast(centerParameter(++i));
          i = eval.iToken;
        }
        if (v.size() > 0) {
          path = v.toArray(new P3[v.size()]);
          if (!chk)
            list.addLast(new Object[] { Integer.valueOf(T.path),
                Float.valueOf(timeSec), path, theta,
                new int[] { 0, Integer.MAX_VALUE } });
          //vwr.navigatePath(timeSec, path, theta, 0, Integer.MAX_VALUE);
          continue;
        }
        //$FALL-THROUGH$
      default:
        invArg();
      }
    }
    if (!chk && !vwr.isJmolDataFrame())
      vwr.tm.navigateList(eval, list);
  }

  protected String plot(T[] args) throws ScriptException {
    ScriptEval eval = this.e;
    // also used for draw [quaternion, helix, ramachandran] 
    // and write quaternion, ramachandran, plot, ....
    // and plot property propertyX, propertyY, propertyZ //
    int modelIndex = vwr.am.cmi;
    if (modelIndex < 0)
      eval.errorStr(ScriptError.ERROR_multipleModelsDisplayedNotOK, "plot");
    modelIndex = vwr.ms.getJmolDataSourceFrame(modelIndex);
    int pt = args.length - 1;
    boolean isReturnOnly = (args != st);
    boolean pdbFormat = true;
    T[] statementSave = st;
    if (isReturnOnly)
      eval.st = st = args;
    int tokCmd = (isReturnOnly ? T.show : args[0].tok);
    int pt0 = (isReturnOnly || tokCmd == T.quaternion
        || tokCmd == T.ramachandran ? 0 : 1);
    String filename = null;
    boolean makeNewFrame = true;
    boolean isDraw = false;
    switch (tokCmd) {
    case T.plot:
    case T.quaternion:
    case T.ramachandran:
      break;
    case T.draw:
      makeNewFrame = false;
      isDraw = true;
      break;
    case T.show:
      makeNewFrame = false;
      pdbFormat = false;
      break;
    case T.write:
      makeNewFrame = false;
      if (tokAtArray(pt, args) == T.string) {
        filename = stringParameter(pt--);
      } else if (tokAtArray(pt - 1, args) == T.per) {
        filename = paramAsStr(pt - 2) + "." + paramAsStr(pt);
        pt -= 3;
      } else {
        eval.st = st = statementSave;
        eval.iToken = st.length;
        error(ScriptError.ERROR_endOfStatementUnexpected);
      }
      break;
    }
    String qFrame = "";
    Object[] parameters = null;
    String stateScript = "";
    boolean isQuaternion = false;
    boolean isDerivative = false;
    boolean isSecondDerivative = false;
    boolean isRamachandranRelative = false;
    int propertyX = 0, propertyY = 0, propertyZ = 0;
    BS bs = BSUtil.copy(vwr.bsA());
    String preSelected = "; select " + Escape.eBS(bs) + ";\n ";
    String type = eval.optParameterAsString(pt).toLowerCase();
    P3 minXYZ = null;
    P3 maxXYZ = null;
    String format = null;
    int tok = tokAtArray(pt0, args);
    if (tok == T.string)
      tok = T.getTokFromName((String) args[pt0].value);
    switch (tok) {
    default:
      eval.iToken = 1;
      invArg();
      break;
    case T.data:
      eval.iToken = 1;
      type = "data";
      preSelected = "";
      break;
    case T.property:
      eval.iToken = pt0 + 1;
      propertyX = plotProp();
      if (propertyX == 0)
        invArg();
      propertyY = plotProp();
      propertyZ = plotProp();
      if (tokAt(eval.iToken) == T.format) {
        format = stringParameter(++eval.iToken);
        pdbFormat = false;
        eval.iToken++;
      }
      if (tokAt(eval.iToken) == T.min) {
        minXYZ = getPoint3f(++eval.iToken, false);
        eval.iToken++;
      }
      if (tokAt(eval.iToken) == T.max) {
        maxXYZ = getPoint3f(++eval.iToken, false);
        eval.iToken++;
      }
      type = "property " + T.nameOf(propertyX)
          + (propertyY == 0 ? "" : " " + T.nameOf(propertyY))
          + (propertyZ == 0 ? "" : " " + T.nameOf(propertyZ));
      if (bs.nextSetBit(0) < 0)
        bs = vwr.getModelUndeletedAtomsBitSet(modelIndex);
      stateScript = "select " + Escape.eBS(bs) + ";\n ";
      break;
    case T.ramachandran:
      if (type.equalsIgnoreCase("draw")) {
        isDraw = true;
        type = eval.optParameterAsString(--pt).toLowerCase();
      }
      isRamachandranRelative = (pt > pt0 && type.startsWith("r"));
      type = "ramachandran" + (isRamachandranRelative ? " r" : "")
          + (tokCmd == T.draw ? " draw" : "");
      break;
    case T.quaternion:
    case T.helix:
      qFrame = " \"" + vwr.getQuaternionFrame() + "\"";
      stateScript = "set quaternionFrame" + qFrame + ";\n  ";
      isQuaternion = true;
      // working backward this time:
      if (type.equalsIgnoreCase("draw")) {
        isDraw = true;
        type = eval.optParameterAsString(--pt).toLowerCase();
      }
      isDerivative = (type.startsWith("deriv") || type.startsWith("diff"));
      isSecondDerivative = (isDerivative && type.indexOf("2") > 0);
      if (isDerivative)
        pt--;
      if (type.equalsIgnoreCase("helix") || type.equalsIgnoreCase("axis")) {
        isDraw = true;
        isDerivative = true;
        pt = -1;
      }
      type = ((pt <= pt0 ? "" : eval.optParameterAsString(pt)) + "w").substring(0,
          1);
      if (type.equals("a") || type.equals("r"))
        isDerivative = true;
      if (!PT.isOneOf(type, ";w;x;y;z;r;a;")) // a absolute; r relative
        eval.evalError("QUATERNION [w,x,y,z,a,r] [difference][2]", null);
      type = "quaternion " + type + (isDerivative ? " difference" : "")
          + (isSecondDerivative ? "2" : "") + (isDraw ? " draw" : "");
      break;
    }
    st = statementSave;
    if (chk) // just in case we later mp.add parameter options to this
      return "";

    // if not just drawing check to see if there is already a plot of this type

    if (makeNewFrame) {
      stateScript += "plot " + type;
      int ptDataFrame = vwr.ms.getJmolDataFrameIndex(modelIndex, stateScript);
      if (ptDataFrame > 0 && tokCmd != T.write && tokCmd != T.show) {
        // no -- this is that way we switch frames. vwr.deleteAtoms(vwr.getModelUndeletedAtomsBitSet(ptDataFrame), true);
        // data frame can't be 0.
        vwr.setCurrentModelIndexClear(ptDataFrame, true);
        // BitSet bs2 = vwr.getModelAtomBitSet(ptDataFrame);
        // bs2.and(bs);
        // need to be able to set data directly as well.
        // vwr.display(BitSetUtil.setAll(vwr.getAtomCount()), bs2, tQuiet);
        return "";
      }
    }

    // prepare data for property plotting

    float[] dataX = null, dataY = null, dataZ = null;
    if (tok == T.property) {
      dataX = eval.getBitsetPropertyFloat(bs, propertyX | T.selectedfloat,
          (minXYZ == null ? Float.NaN : minXYZ.x), (maxXYZ == null ? Float.NaN
              : maxXYZ.x));
      if (propertyY != 0)
        dataY = eval.getBitsetPropertyFloat(bs, propertyY | T.selectedfloat,
            (minXYZ == null ? Float.NaN : minXYZ.y),
            (maxXYZ == null ? Float.NaN : maxXYZ.y));
      if (propertyZ != 0)
        dataZ = eval.getBitsetPropertyFloat(bs, propertyZ | T.selectedfloat,
            (minXYZ == null ? Float.NaN : minXYZ.z),
            (maxXYZ == null ? Float.NaN : maxXYZ.z));
      if (minXYZ == null)
        minXYZ = P3.new3(getPlotMinMax(dataX, false, propertyX),
            getPlotMinMax(dataY, false, propertyY),
            getPlotMinMax(dataZ, false, propertyZ));
      if (maxXYZ == null)
        maxXYZ = P3.new3(getPlotMinMax(dataX, true, propertyX),
            getPlotMinMax(dataY, true, propertyY),
            getPlotMinMax(dataZ, true, propertyZ));
      Logger.info("plot min/max: " + minXYZ + " " + maxXYZ);
      P3 center = null;
      P3 factors = null;

      if (pdbFormat) {
        factors = P3.new3(1, 1, 1);
        center = new P3();
        center.ave(maxXYZ, minXYZ);
        factors.sub2(maxXYZ, minXYZ);
        factors.set(factors.x / 200, factors.y / 200, factors.z / 200);
        if (T.tokAttr(propertyX, T.intproperty)) {
          factors.x = 1;
          center.x = 0;
        } else if (factors.x > 0.1 && factors.x <= 10) {
          factors.x = 1;
        }
        if (T.tokAttr(propertyY, T.intproperty)) {
          factors.y = 1;
          center.y = 0;
        } else if (factors.y > 0.1 && factors.y <= 10) {
          factors.y = 1;
        }
        if (T.tokAttr(propertyZ, T.intproperty)) {
          factors.z = 1;
          center.z = 0;
        } else if (factors.z > 0.1 && factors.z <= 10) {
          factors.z = 1;
        }
        if (propertyZ == 0 || propertyY == 0)
          center.z = minXYZ.z = maxXYZ.z = factors.z = 0;
        for (int i = 0; i < dataX.length; i++)
          dataX[i] = (dataX[i] - center.x) / factors.x;
        if (propertyY != 0)
          for (int i = 0; i < dataY.length; i++)
            dataY[i] = (dataY[i] - center.y) / factors.y;
        if (propertyZ != 0)
          for (int i = 0; i < dataZ.length; i++)
            dataZ[i] = (dataZ[i] - center.z) / factors.z;
      }
      parameters = new Object[] { bs, dataX, dataY, dataZ, minXYZ, maxXYZ,
          factors, center, format};
    }

    // all set...

    if (tokCmd == T.write)
      return vwr
          .writeFileData(filename, "PLOT_" + type, modelIndex, parameters);

    String data = (type.equals("data") ? "1 0 H 0 0 0 # Jmol PDB-encoded data"
        : vwr
            .getPdbData(modelIndex, type, null, parameters, null, true));

    if (tokCmd == T.show)
      return data;

    if (Logger.debugging)
      Logger.debug(data);

    if (tokCmd == T.draw) {
      eval.runScript(data);
      return "";
    }

    // create the new model

    String[] savedFileInfo = vwr.fm.getFileInfo();
    boolean oldAppendNew = vwr.getBoolean(T.appendnew);
    vwr.g.appendNew = true;
    boolean isOK = (data != null && vwr.openStringInlineParamsAppend(data,
        null, true) == null);
    vwr.g.appendNew = oldAppendNew;
    vwr.fm.setFileInfo(savedFileInfo);
    if (!isOK)
      return "";
    int modelCount = vwr.ms.mc;
    vwr.ms.setJmolDataFrame(stateScript, modelIndex, modelCount - 1);
    if (tok != T.property)
      stateScript += ";\n" + preSelected;
    StateScript ss = vwr.addStateScript(stateScript, true, false);

    // get post-processing script

    float radius = 150;
    String script;
    switch (tok) {
    default:
      script = "frame 0.0; frame last; reset;select visible;wireframe only;";
      radius = 10;
      break;
    case T.property:
      vwr.setFrameTitle(modelCount - 1,
          type + " plot for model " + vwr.getModelNumberDotted(modelIndex));
      float f = 3;
      script = "frame 0.0; frame last; reset;" + "select visible; spacefill "
          + f + "; wireframe 0;" + "draw plotAxisX" + modelCount
          + " {100 -100 -100} {-100 -100 -100} \"" + T.nameOf(propertyX)
          + "\";" + "draw plotAxisY" + modelCount
          + " {-100 100 -100} {-100 -100 -100} \"" + T.nameOf(propertyY)
          + "\";";
      if (propertyZ != 0)
        script += "draw plotAxisZ" + modelCount
            + " {-100 -100 100} {-100 -100 -100} \"" + T.nameOf(propertyZ)
            + "\";";
      break;
    case T.ramachandran:
      vwr.setFrameTitle(modelCount - 1,
          "ramachandran plot for model " + vwr.getModelNumberDotted(modelIndex));
      script = "frame 0.0; frame last; reset;"
          + "select visible; color structure; spacefill 3.0; wireframe 0;"
          + "draw ramaAxisX" + modelCount + " {100 0 0} {-100 0 0} \"phi\";"
          + "draw ramaAxisY" + modelCount + " {0 100 0} {0 -100 0} \"psi\";";
      break;
    case T.quaternion:
    case T.helix:
      vwr.setFrameTitle(modelCount - 1, type.replace('w', ' ') + qFrame
          + " for model " + vwr.getModelNumberDotted(modelIndex));
      String color = (C.getHexCode(vwr.cm.colixBackgroundContrast));
      script = "frame 0.0; frame last; reset;"
          + "select visible; wireframe 0; spacefill 3.0; "
          + "isosurface quatSphere" + modelCount + " color " + color
          + " sphere 100.0 mesh nofill frontonly translucent 0.8;"
          + "draw quatAxis" + modelCount
          + "X {100 0 0} {-100 0 0} color red \"x\";" + "draw quatAxis"
          + modelCount + "Y {0 100 0} {0 -100 0} color green \"y\";"
          + "draw quatAxis" + modelCount
          + "Z {0 0 100} {0 0 -100} color blue \"z\";" + "color structure;"
          + "draw quatCenter" + modelCount + "{0 0 0} scale 0.02;";
      break;
    }

    // run the post-processing script and set rotation radius and display frame title
    eval.runScript(script + preSelected);
    ss.setModelIndex(vwr.am.cmi);
    vwr.setRotationRadius(radius, true);
    eval.sm.loadShape(JC.SHAPE_ECHO);
    showString("frame "
        + vwr.getModelNumberDotted(modelCount - 1)
        + (type.length() > 0 ? " created: " + type
            + (isQuaternion ? qFrame : "") : ""));
    return "";
  }

  private int plotProp() {
    int p = 0;
    switch (tokAt(e.iToken)) {
    case T.format:
    case T.min:
    case T.max:
      break;
    default:
      if (T.tokAttr(p = tokAt(e.iToken), T.atomproperty))
        e.iToken++;
      break;
    }
    return p;
  }

  private boolean polyhedra() throws ScriptException {
    ScriptEval eval = e;
    /*
     * needsGenerating:
     * 
     * polyhedra [number of vertices and/or basis] [at most two selection sets]
     * [optional type and/or edge] [optional design parameters]
     * 
     * OR else:
     * 
     * polyhedra [at most one selection set] [type-and/or-edge or on/off/delete]
     */
    boolean needsGenerating = false;
    boolean onOffDelete = false;
    boolean typeSeen = false;
    boolean edgeParameterSeen = false;
    boolean isDesignParameter = false;
    int lighting = 0;
    int nAtomSets = 0;
    e.sm.loadShape(JC.SHAPE_POLYHEDRA);
    setShapeProperty(JC.SHAPE_POLYHEDRA, "init", Boolean.TRUE);
    String setPropertyName = "centers";
    String decimalPropertyName = "radius_";
    float translucentLevel = Float.MAX_VALUE;
    int[] colorArgb = new int[] { Integer.MIN_VALUE };
    for (int i = 1; i < slen; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      switch (getToken(i).tok) {
      case T.delete:
      case T.on:
      case T.off:
        if (i + 1 != slen || needsGenerating || nAtomSets > 1 || nAtomSets == 0
            && "to".equals(setPropertyName))
          error(ScriptError.ERROR_incompatibleArguments);
        propertyName = (eval.theTok == T.off ? "off"
            : eval.theTok == T.on ? "on" : "delete");
        onOffDelete = true;
        break;
      case T.opEQ:
      case T.comma:
        continue;
      case T.bonds:
        if (nAtomSets > 0)
          invPO();
        needsGenerating = true;
        propertyName = "bonds";
        break;
      case T.radius:
        decimalPropertyName = "radius";
        continue;
      case T.integer:
      case T.decimal:
        if (nAtomSets > 0 && !isDesignParameter)
          invPO();
        if (eval.theTok == T.integer) {
          if (decimalPropertyName == "radius_") {
            propertyName = "nVertices";
            propertyValue = Integer.valueOf(intParameter(i));
            needsGenerating = true;
            break;
          }
        }
        propertyName = (decimalPropertyName == "radius_" ? "radius"
            : decimalPropertyName);
        propertyValue = Float.valueOf(floatParameter(i));
        decimalPropertyName = "radius_";
        isDesignParameter = false;
        needsGenerating = true;
        break;
      case T.bitset:
      case T.expressionBegin:
        if (typeSeen)
          invPO();
        if (++nAtomSets > 2)
          eval.bad();
        if ("to".equals(setPropertyName))
          needsGenerating = true;
        propertyName = setPropertyName;
        setPropertyName = "to";
        propertyValue = atomExpressionAt(i);
        i = eval.iToken;
        break;
      case T.to:
        if (nAtomSets > 1)
          invPO();
        if (tokAt(i + 1) == T.bitset || tokAt(i + 1) == T.expressionBegin
            && !needsGenerating) {
          propertyName = "toBitSet";
          propertyValue = atomExpressionAt(++i);
          i = eval.iToken;
          needsGenerating = true;
          break;
        } else if (!needsGenerating) {
          error(ScriptError.ERROR_insufficientArguments);
        }
        setPropertyName = "to";
        continue;
      case T.facecenteroffset:
        if (!needsGenerating)
          error(ScriptError.ERROR_insufficientArguments);
        decimalPropertyName = "faceCenterOffset";
        isDesignParameter = true;
        continue;
      case T.distancefactor:
        if (nAtomSets == 0)
          error(ScriptError.ERROR_insufficientArguments);
        decimalPropertyName = "distanceFactor";
        isDesignParameter = true;
        continue;
      case T.color:
      case T.translucent:
      case T.opaque:
        translucentLevel = getColorTrans(eval, i, true, colorArgb);
        i = eval.iToken;
        continue;
      case T.collapsed:
      case T.flat:
        propertyName = "collapsed";
        propertyValue = (eval.theTok == T.collapsed ? Boolean.TRUE
            : Boolean.FALSE);
        if (typeSeen)
          error(ScriptError.ERROR_incompatibleArguments);
        typeSeen = true;
        break;
      case T.noedges:
      case T.edges:
      case T.frontedges:
        if (edgeParameterSeen)
          error(ScriptError.ERROR_incompatibleArguments);
        propertyName = paramAsStr(i);
        edgeParameterSeen = true;
        break;
      case T.fullylit:
        lighting = eval.theTok;
        continue;
      default:
        if (eval.isColorParam(i)) {
          colorArgb[0] = eval.getArgbParam(i);
          i = eval.iToken;
          continue;
        }
        invArg();
      }
      setShapeProperty(JC.SHAPE_POLYHEDRA, propertyName, propertyValue);
      if (onOffDelete)
        return false;
    }
    if (!needsGenerating && !typeSeen && !edgeParameterSeen && lighting == 0)
      error(ScriptError.ERROR_insufficientArguments);
    if (needsGenerating)
      setShapeProperty(JC.SHAPE_POLYHEDRA, "generate", null);
    if (colorArgb[0] != Integer.MIN_VALUE)
      setShapeProperty(JC.SHAPE_POLYHEDRA, "colorThis",
          Integer.valueOf(colorArgb[0]));
    if (translucentLevel != Float.MAX_VALUE)
      eval.setShapeTranslucency(JC.SHAPE_POLYHEDRA, "", "translucentThis",
          translucentLevel, null);
    if (lighting != 0)
      setShapeProperty(JC.SHAPE_POLYHEDRA, "token", Integer.valueOf(lighting));
    setShapeProperty(JC.SHAPE_POLYHEDRA, "init", Boolean.FALSE);
    return true;
  }

  /**
   * 
   * @param args
   * @return string for write() function
   * @throws ScriptException
   */
  private String write(T[] args) throws ScriptException {
    ScriptEval eval = e;
    int pt = 1, pt0 = 1;
    String[] scripts = null;
    String msg = null;
    String localPath = null;
    String remotePath = null;
    String type = "SPT";
    boolean isCommand = true;
    boolean showOnly = false;
    if (args == null) {
      // write command
      args = st;
      showOnly = (vwr.isApplet && !vwr.isSignedApplet
          || !vwr.haveAccess(ACCESS.ALL) || vwr.fm.getPathForAllFiles()
          .length() > 0);
    } else {
      // write() function or image
      pt = pt0 = 0;
      isCommand = (args == st);
      showOnly = !isCommand;
    }

    // check for special considerations involving first parameter

    int tok = tokAtArray(pt, args);
    if (tok == T.string && !isCommand) {
      T t0 = T.getTokenFromName(SV.sValue(args[0]).toLowerCase());
      if (t0 != null)
        tok = t0.tok;
    }
    switch (tok) {
    case T.nada:
      break;
    case T.quaternion:
    case T.ramachandran:
    case T.property:
      msg = plot(args);
      return (showOnly ? msg : writeMsg(msg));
    case T.script:
      // would fail in write() command.
      // not documented?
      // use?
      if (eval.isArrayParameter(pt + 1)) {
        scripts = eval.stringParameterSet(++pt);
        localPath = ".";
        remotePath = ".";
        pt0 = pt = eval.iToken + 1;
        tok = tokAt(pt);
      }
      break;
    default:
      type = SV.sValue(tokenAt(pt, args)).toUpperCase();
    }

    String driverList = vwr.getExportDriverList();
    String data = null;
    int argCount = (isCommand ? slen : args.length);
    String type2 = "";
    String val = null;
    SV tVar = null;
    int nVibes = 0;
    String sceneType = "PNGJ";
    boolean isCoord = false;
    BS bsFrames = null;
    int width = -1;
    int height = -1;
    boolean isExport = false;
    String fileName = null;

    // accept write ...... AS type

    if (tok != T.nada && isCommand && slen > 1 && tokAt(slen - 2) == T.as) {
      type = paramAsStr(slen - 1).toUpperCase();
      pt0 = argCount;
      argCount -= 2;
      tok = T.nada;
    }

    // check type

    switch (tok) {
    case T.nada:
      break;
    case T.barray:
    case T.hash:
      type = "VAR";
      tVar = (SV) tokenAt(pt++, args);
      break;
    case T.inline:
      type = "INLINE";
      data = SV.sValue(tokenAt(++pt, args));
      pt++;
      break;
    case T.pointgroup:
      type = "PGRP";
      pt++;
      type2 = SV.sValue(tokenAt(pt, args)).toLowerCase();
      if (type2.equals("draw"))
        pt++;
      break;
    case T.coord:
      pt++;
      isCoord = true;
      break;
    case T.state:
    case T.script:
      val = SV.sValue(tokenAt(++pt, args)).toLowerCase();
      while (val.equals("localpath") || val.equals("remotepath")) {
        if (val.equals("localpath"))
          localPath = SV.sValue(tokenAt(++pt, args));
        else
          remotePath = SV.sValue(tokenAt(++pt, args));
        val = SV.sValue(tokenAt(++pt, args)).toLowerCase();
      }
      type = "SPT";
      break;
    case T.file:
    case T.function:
    case T.history:
    case T.isosurface:
    case T.menu:
    case T.mesh:
    case T.nbo:
    case T.mo:
    case T.pmesh:
      pt++;
      break;
    case T.jmol:
      type = "ZIPALL";
      pt++;
      break;
    case T.var:
      type = "VAR";
      pt += 2;
      break;
    case T.frame:
    case T.image:
    case T.scene:
    case T.vibration:
    case T.identifier:
    case T.string:
      switch (tok) {
      case T.frame:
        BS bsAtoms;
        if (pt + 1 < argCount && args[++pt].tok == T.expressionBegin
            || args[pt].tok == T.bitset) {
          bsAtoms = eval.atomExpression(args, pt, 0, true, false, true, true);
          pt = eval.iToken + 1;
        } else {
          bsAtoms = vwr.getAllAtoms();
        }
        if (!chk)
          bsFrames = vwr.ms.getModelBS(bsAtoms, true);
        break;
      case T.image:
        type = "IMAGE";
        pt++;
        break;
      case T.scene:
        val = SV.sValue(tokenAt(++pt, args)).toUpperCase();
        if (PT.isOneOf(val, ";PNG;PNGJ;")) {
          sceneType = val;
          pt++;
        }
        break;
      case T.vibration:
        nVibes = eval.intParameterRange(++pt, 1, 10);
        if (nVibes == Integer.MAX_VALUE)
          return "";
        if (!chk) {
          vwr.tm.setVibrationPeriod(0);
          if (!eval.isJS)
            eval.delayScript(100);
        }
        pt++;
        break;
      default:
        // identifier or string
        tok = T.image;
        break;
      }
      if (tok == T.image && pt < args.length) {
        // write IMAGE JPG
        // write JPG
        T t = T.getTokenFromName(SV.sValue(args[pt]).toLowerCase());
        if (t != null)
          type = SV.sValue(t).toUpperCase();
        if (PT.isOneOf(type, driverList.toUpperCase())) {
          // povray, maya, vrml, idtf
          pt++;
          type = type.substring(0, 1).toUpperCase()
              + type.substring(1).toLowerCase();
          // Povray, Maya, Vrml, Idtf
          isExport = true;
          if (isCommand)
            fileName = "Jmol." + type.toLowerCase();
          break;
        } else if (PT.isOneOf(type, ";ZIP;ZIPALL;SPT;STATE;")) {
          pt++;
          break;
        } else {
          type = "IMAGE";
        }
      }
      if (tokAtArray(pt, args) == T.integer) {
        width = SV.iValue(tokenAt(pt++, args));
        height = SV.iValue(tokenAt(pt++, args));
      }
      break;
    }

    int quality = Integer.MIN_VALUE;
    if (pt0 < argCount) {
      val = SV.sValue(tokenAt(pt, args));
      if (val.equalsIgnoreCase("clipboard")) {
        if (chk)
          return "";
        // if (isApplet)
        // evalError(GT._("The {0} command is not available for the applet.",
        // "WRITE CLIPBOARD"));
      } else if (PT.isOneOf(val.toLowerCase(), JC.IMAGE_TYPES)) {
        if (tokAtArray(pt + 1, args) == T.integer
            && tokAtArray(pt + 2, args) == T.integer) {
          width = SV.iValue(tokenAt(++pt, args));
          height = SV.iValue(tokenAt(++pt, args));
        }
        if (tokAtArray(pt + 1, args) == T.integer)
          quality = SV.iValue(tokenAt(++pt, args));
      } else if (PT.isOneOf(val.toLowerCase(),
          ";xyz;xyzrn;xyzvib;mol;sdf;v2000;v3000;json;pdb;pqr;cml;")) {
        type = val.toUpperCase();
        if (pt + 1 == argCount)
          pt++;
      }

      // write [image|history|state] clipboard

      // write [optional image|history|state] [JPG quality|JPEG quality|JPG64
      // quality|PNG|PPM|SPT] "filename"
      // write script "filename"
      // write isosurface t.jvxl

      if (type.equals("IMAGE")
          && PT.isOneOf(val.toLowerCase(), JC.IMAGE_OR_SCENE)) {
        type = val.toUpperCase();
        pt++;
      }
    }
    if (pt + 2 == argCount) {
      String s = SV.sValue(tokenAt(++pt, args));
      if (s.length() > 0 && s.charAt(0) != '.')
        type = val.toUpperCase();
    }

    // set the file name

    switch (tokAtArray(pt, args)) {
    case T.nada:
      // WRITE by itself will do WRITE SPT to console
      showOnly = true;
      break;
    case T.clipboard:
      break;
    case T.opIf:
      // ? by itself
      fileName = (type.equals("IMAGE") ? "?jmol.png" : "?jmol."
          + type.toLowerCase());
      break;
    case T.identifier:
    case T.string:
      fileName = SV.sValue(tokenAt(pt, args));
      if (fileName.equalsIgnoreCase("clipboard") || !vwr.haveAccess(ACCESS.ALL))
        fileName = null;
      break;
    default:
      invArg();
    }

    // adjust the type from the filename and set whether this is an export

    if (type.equals("IMAGE") || type.equals("FRAME")
        || type.equals("VIBRATION")) {
      type = (fileName != null && fileName.indexOf(".") >= 0 ? fileName
          .substring(fileName.lastIndexOf(".") + 1).toUpperCase() : "JPG");
      // Introduced in 14.3.9_2014.11.23; removed 14.3.12_2015.02.24
      // This was a bad idea; changing the file name automatically is not appropriate -- that is what AS is for
      // if (PT.isOneOf(type, ";PNGJ;PNGT;GIFT;"))
      //   fileName = fileName.substring(0, fileName.length() - 1);
    }

    boolean isImage = PT.isOneOf(type.toLowerCase(), JC.IMAGE_OR_SCENE);
    if (!isImage) {
      if (type.equals("MNU")) {
        type = "MENU";
      } else if (type.equals("WRL") || type.equals("VRML")) {
        type = "Vrml";
        isExport = true;
      } else if (type.equals("X3D")) {
        type = "X3d";
        isExport = true;
      } else if (type.equals("IDTF")) {
        type = "Idtf";
        isExport = true;
      } else if (type.equals("MA")) {
        type = "Maya";
        isExport = true;
      } else if (type.equals("JS")) {
        type = "Js";
        isExport = true;
      } else if (type.equals("OBJ")) {
        type = "Obj";
        isExport = true;
      } else if (type.equals("JVXL")) {
        type = "ISOSURFACE";
      } else if (type.equals("XJVXL")) {
        type = "ISOSURFACE";
      } else if (type.equals("JMOL")) {
        type = "ZIPALL";
      } else if (type.equals("HIS")) {
        type = "HISTORY";
      }
      if (type.equals("COORD") || type.equals("COORDS"))
        type = (fileName != null && fileName.indexOf(".") >= 0 ? fileName
            .substring(fileName.lastIndexOf(".") + 1).toUpperCase() : "XYZ");
    }
    if (scripts != null) {
      if (type.equals("PNG"))
        type = "PNGJ";
      if (!type.equals("PNGJ") && !type.equals("ZIPALL") && !type.equals("ZIP"))
        invArg();
    }
    if (!isImage
        && !isExport
        && !PT
            .isOneOf(
                type,
                ";SCENE;JMOL;ZIP;ZIPALL;SPT;HISTORY;MO;NBO;ISOSURFACE;MESH;PMESH;VAR;FILE;FUNCTION;CML;JSON;XYZ;XYZRN;XYZVIB;MENU;MOL;PDB;PGRP;PQR;QUAT;RAMA;SDF;V2000;V3000;INLINE;"))
      eval.errorStr2(
          ScriptError.ERROR_writeWhat,
          "COORDS|FILE|FUNCTIONS|HISTORY|IMAGE|INLINE|ISOSURFACE|JMOL|MENU|MO|NBO|POINTGROUP|QUATERNION [w,x,y,z] [derivative]"
              + "|RAMACHANDRAN|SPT|STATE|VAR x|ZIP|ZIPALL  CLIPBOARD",
          "CML|GIF|GIFT|JPG|JPG64|JMOL|JVXL|MESH|MOL|PDB|PMESH|PNG|PNGJ|PNGT|PPM|PQR|SDF|CD|JSON|V2000|V3000|SPT|XJVXL|XYZ|XYZRN|XYZVIB|ZIP"
              + driverList.toUpperCase().replace(';', '|'));
    if (chk)
      return "";

    String[] fullPath = new String[1];
    Map<String, Object> params;
    boolean timeMsg = vwr.getBoolean(T.showtiming);

    // process write command based on data type

    if (isExport) {
      if (timeMsg)
        Logger.startTimer("export");
      Map<String, Object> eparams = new Hashtable<String, Object>();
      eparams.put("type", type);
      if (fileName != null)
        eparams.put("fileName", fileName);
      if (isCommand || fileName != null)
        eparams.put("fullPath", fullPath);
      eparams.put("width", Integer.valueOf(width));
      eparams.put("height", Integer.valueOf(height));
      data = vwr.generateOutputForExport(eparams);
      if (data == null || data.length() == 0)
        return "";
      if (showOnly)
        return data;
      if (!type.equals("Povray") && !type.equals("Idtf") || fullPath[0] == null)
        return writeMsg(data);
      // must also produce .ini or .tex file
      String ext = (type.equals("Idtf") ? ".tex" : ".ini");
      fileName = fullPath[0] + ext;
      params = new Hashtable<String, Object>();
      params.put("fileName", fileName);
      params.put("type", ext);
      params.put("text", data);
      params.put("fullPath", fullPath);
      msg = vwr.processWriteOrCapture(params);
      // fullPath may be changed here
      if (type.equals("Idtf"))
        data = data.substring(0, data.indexOf("\\begin{comment}"));
      data = "Created " + fullPath[0] + ":\n\n" + data;
      if (timeMsg)
        showString(Logger.getTimerMsg("export", 0));
      if (msg != null) {
        if (!msg.startsWith("OK"))
          eval.evalError(msg, null);
        eval.report(data);
      }
      return "";
    }

    // creating bytes or data

    Object bytes = null;
    boolean writeFileData = false;
    if (data == null) {
      int len = 0;
      data = type.intern();
      if (data == "MENU") {
        data = vwr.getMenu("");
      } else if (data == "PGRP") {
        data = vwr.ms.getPointGroupAsString(vwr.bsA(), type2.equals("draw"),
            null, 0, 1.0f);
      } else if (data == "PDB" || data == "PQR") {
        if (showOnly) {
          data = vwr.getPdbAtomData(null, null, (data == "PQR"), isCoord);
        } else {
          writeFileData = true;
          type = "PDB_" + data + "-coord " + isCoord;
        }
      } else if (data == "FILE") {
        if ("?".equals(fileName))
          fileName = "?Jmol." + vwr.getP("_fileType");
        if (showOnly)
          data = vwr.getCurrentFileAsString("script");
        else
          writeFileData = true;
      } else if (data == "SDF" || data == "MOL" || data == "V2000"
          || data == "V3000" || data == "CD" || data == "JSON" || data == "XYZ"
          || data == "XYZRN" || data == "XYZVIB" || data == "CML") {
        data = vwr.getModelExtract("selected", isCoord, false, data);
        if (data.startsWith("ERROR:"))
          bytes = data;
      } else if (data == "FUNCTION") {
        data = vwr.getFunctionCalls(null);
        type = "TXT";
      } else if (data == "VAR") {
        if (tVar == null) {
          tVar = (SV) eval.getParameter(
              SV.sValue(tokenAt(isCommand ? 2 : 1, args)), T.variable, true);
        }
        Lst<Object> v = null;
        if (tVar.tok == T.barray) {
          v = new Lst<Object>();
          v.addLast(((BArray) tVar.value).data);
        } else if (tVar.tok == T.hash) {
          @SuppressWarnings("unchecked")
          Map<String, SV> m = (Map<String, SV>) tVar.value;
          if (m.containsKey("$_BINARY_$")) {
            v = new Lst<Object>();
            if (fileName != null)
              for (Entry<String, SV> e : m.entrySet()) {
                String key = e.getKey();
                if (key.equals("$_BINARY_$"))
                  continue;
                SV o = e.getValue();
                bytes = (o.tok == T.barray ? ((BArray) o.value).data : null);
                if (bytes == null) {
                  String s = o.asString();
                  bytes = (s.startsWith(";base64,") ? Base64.decodeBase64(s)
                      : s.getBytes());
                }
                if (key.equals("_DATA_")) {
                  v = null;
                  if (bytes == null)
                    bytes = ((BArray) o.value).data;
                  break;
                } else if (key.equals("_IMAGE_")) {
                  v.add(0, key);
                  v.add(1, null);
                  v.add(2, bytes);
                } else {
                  v.addLast(key);
                  v.addLast(null);
                  v.addLast(bytes);
                }
              }
          }
        }
        if (v == null) {
          if (bytes == null) {
            data = tVar.asString();
            type = "TXT";
          }
        } else {
          if (fileName != null
              && (bytes = data = vwr.createZip(fileName,
                  v.size() == 1 ? "BINARY" : "ZIPDATA", v)) == null)
            eval.evalError("#CANCELED#", null);
        }
      } else if (data == "SPT") {
        if (isCoord) {
          BS tainted = vwr.ms.getTaintedAtoms(AtomCollection.TAINT_COORD);
          vwr.setAtomCoordsRelative(P3.new3(0, 0, 0), null);
          data = vwr.getStateInfo();
          vwr.ms.setTaintedAtoms(tainted, AtomCollection.TAINT_COORD);
        } else {
          data = vwr.getStateInfo();
          if (localPath != null || remotePath != null)
            data = FileManager.setScriptFileReferences(data, localPath,
                remotePath, null);
        }
      } else if (data == "ZIP" || data == "ZIPALL") {
        if (fileName != null
            && (bytes = data = vwr.createZip(fileName, type, scripts)) == null)
          eval.evalError("#CANCELED#", null);
      } else if (data == "HISTORY") {
        data = vwr.getSetHistory(Integer.MAX_VALUE);
        type = "SPT";
      } else if (data == "MO" || data == "NBO") {
        data = getMoJvxl(Integer.MAX_VALUE, data == "NBO");
        type = "XJVXL";
      } else if (data == "PMESH") {
        if ((data = getIsosurfaceJvxl(true, JC.SHAPE_PMESH)) == null)
          error(ScriptError.ERROR_noData);
        type = "XJVXL";
      } else if (data == "ISOSURFACE" || data == "MESH") {
        if ((data = getIsosurfaceJvxl(data == "MESH", JC.SHAPE_ISOSURFACE)) == null)
          error(ScriptError.ERROR_noData);
        type = (data.indexOf("<?xml") >= 0 ? "XJVXL" : "JVXL");
        if (!showOnly)
          showString((String) getShapeProperty(JC.SHAPE_ISOSURFACE,
              "jvxlFileInfo"));
      } else {
        // image
        if (isCommand && showOnly && fileName == null) {
          showOnly = false;
          fileName = "\1";
        }
        len = -1;
        if (quality < 0)
          quality = -1;
      }
      if (data == null)
        data = "";
      if (len == 0)
        len = (bytes == null ? data.length()
            : bytes instanceof String ? ((String) bytes).length()
                : ((byte[]) bytes).length);
    }

    // if write() function, then just return data
    if (!isCommand)
      return data;
    // if WRITE command, but cannot write files, send through PRINT channel
    if (showOnly) {
      eval.showStringPrint(data, true);
      return "";
    }
    // String bytes indicates an error message 
    if (bytes != null && bytes instanceof String)
      return writeMsg((String) bytes);
    // Just save the file data and return a confirmation message 
    if (writeFileData)
      return writeMsg(vwr.writeFileData(fileName, type, 0, null));
    // use vwr.processWriteOrCapture(params) for all other situations
    if (type.equals("SCENE"))
      bytes = sceneType;
    else if (bytes == null && (!isImage || fileName != null))
      bytes = data;
    if (timeMsg)
      Logger.startTimer("write");
    if (isImage) {
      eval.refresh(false);
      if (width < 0)
        width = vwr.getScreenWidth();
      if (height < 0)
        height = vwr.getScreenHeight();
    }
    params = new Hashtable<String, Object>();
    if (fileName != null)
      params.put("fileName", fileName);
    params.put("backgroundColor", Integer.valueOf(vwr.getBackgroundArgb()));
    params.put("type", type);
    if (bytes instanceof String && quality == Integer.MIN_VALUE)
      params.put("text", bytes);
    else if (bytes instanceof byte[])
      params.put("bytes", bytes);
    if (scripts != null)
      params.put("scripts", scripts);
    if (bsFrames != null)
      params.put("bsFrames", bsFrames);
    params.put("fullPath", fullPath);
    params.put("quality", Integer.valueOf(quality));
    params.put("width", Integer.valueOf(width));
    params.put("height", Integer.valueOf(height));
    params.put("nVibes", Integer.valueOf(nVibes));
    msg = vwr.processWriteOrCapture(params);
    if (isImage && msg.startsWith("OK"))
      msg += "; width=" + width + "; height=" + height;
    if (timeMsg)
      showString(Logger.getTimerMsg("write", 0));
    return writeMsg(msg);
  }

  private String writeMsg(String msg) throws ScriptException {
    if (chk || msg == null)
      return "";
    if (!msg.startsWith("OK")) {
      e.evalError(msg, null);
      /**
       * @j2sNative
       * 
       *            alert(msg);
       */
      {
      }
    }
    e.report(msg);
    return msg;
  }

  private void show() throws ScriptException {
    ScriptEval eval = e;
    String value = null;
    String str = paramAsStr(1);
    String msg = null;
    String name = null;
    int len = 2;
    T token = getToken(1);
    int tok = (token instanceof SV ? T.nada : token.tok);
    if (tok == T.string) {
      token = T.getTokenFromName(str.toLowerCase());
      if (token != null)
        tok = token.tok;
    }
    if (tok != T.symop && tok != T.state && tok != T.property)
      checkLength(-3);
    if (slen == 2 && str.indexOf("?") >= 0) {
      showString(vwr.getAllSettings(str.substring(0, str.indexOf("?"))));
      return;
    }
    switch (tok) {
    case T.nada:
      if (!chk)
        msg = ((SV) eval.theToken).escape();
      break;
    case T.domains:
      eval.checkLength23();
      len = st.length;
      if (!chk) {
        Object d = vwr.ms.getInfo(vwr.am.cmi, "domains");
        if (d instanceof SV)
          msg = vwr.getAnnotationInfo((SV) d, eval.optParameterAsString(2),
              T.domains);
        else
          msg = "domain information has not been loaded";
      }
      break;
    case T.property:
      msg = plot(st);
      len = st.length;
      break;
    case T.validation:
      eval.checkLength23();
      len = st.length;
      if (!chk) {
        Object d = vwr.ms.getInfo(vwr.am.cmi, "validation");
        if (d instanceof SV)
          msg = vwr.getAnnotationInfo((SV) d, eval.optParameterAsString(2),
              T.validation);
        else
          msg = "validation information has not been loaded";
      }
      break;
    case T.cache:
      if (!chk)
        msg = Escape.e(vwr.fm.cacheList());
      break;
    case T.dssr:
      eval.checkLength23();
      len = st.length;
      if (!chk) {
        Object d = vwr.ms.getInfo(vwr.am.cmi, "dssr");
        if (d == null)
          msg = "no DSSR information has been read";
        else if (len > 2)
          msg = SV.getVariable(vwr.extractProperty(d, stringParameter(2), -1))
              .asString();
        else
          msg = "" + SV.getVariable(d).asString();
      }
      break;
    case T.dssp:
      checkLength(2);
      if (!chk)
        msg = vwr.calculateStructures(null, true, false);
      break;
    case T.pathforallfiles:
      checkLength(2);
      if (!chk)
        msg = vwr.fm.getPathForAllFiles();
      break;
    case T.nmr:
      if (eval.optParameterAsString(2).equalsIgnoreCase("1H")) {
        len = 3;
        if (!chk)
          msg = vwr.getNMRPredict(false);
        break;
      }
      if (!chk)
        vwr.getNMRPredict(true);
      return;
    case T.drawing:
    case T.chemical:
    case T.smiles:
      checkLength(tok == T.chemical ? 3 : 2);
      if (chk)
        return;
      try {
        if (tok != T.smiles)
          msg = vwr.getDataBaseName(null);
        if (msg != null && (msg.startsWith("$") || msg.startsWith(":"))) {
          msg = msg.substring(1);
        } else {
          msg = null;
        }
        if (msg == null)
          msg = vwr.getSmiles(null);
      } catch (Exception ex) {
        msg = ex.getMessage();
      }
      switch (tok) {
      case T.smiles:
        break;
      case T.drawing:
        if (msg.length() > 0) {
          vwr.fm.loadImage(vwr.setLoadFormat("_" + msg, '2', false), "\1" + msg);
          return;
        }
        msg = "Could not show drawing -- Either insufficient atoms are selected or the model is a PDB file.";
        break;
      case T.chemical:
        len = 3;
        if (msg.length() > 0) {
          msg = vwr.getChemicalInfo(msg, getToken(2));
          if (msg.indexOf("FileNotFound") >= 0)
            msg = "?";
        } else {
          msg = "Could not show name -- Either insufficient atoms are selected or the model is a PDB file.";
        }
      }
      break;
    case T.symop:
      int iop = 0;
      P3 pt1 = null,
      pt2 = null;
      if (slen > 3 && tokAt(3) != T.string) {
        pt1 = centerParameter(2);
        pt2 = centerParameter(++eval.iToken);
      } else {
        // show symop 3 "fmatrix"
        iop = (tokAt(2) == T.integer ? intParameter(2) : 0);
      }
      String type = (tokAt(eval.iToken + 1) == T.string ? stringParameter(++eval.iToken)
          : null);
      checkLength(len = ++eval.iToken);
      if (!chk)
        msg = vwr.ms.getSymTemp(true).getSymmetryInfoString(vwr.ms, vwr.am.cmi,
          iop, pt1, pt2, null, type);
      break;
    case T.vanderwaals:
      VDW vdwType = null;
      if (slen > 2) {
        vdwType = VDW.getVdwType(paramAsStr(2));
        if (vdwType == null)
          invArg();
      }
      if (!chk)
        showString(vwr.getDefaultVdwNameOrData(0, vdwType, null));
      return;
    case T.function:
      eval.checkLength23();
      if (!chk)
        showString(vwr.getFunctionCalls(eval.optParameterAsString(2)));
      return;
    case T.set:
      checkLength(2);
      if (!chk)
        showString(vwr.getAllSettings(null));
      return;
    case T.url:
      // in a new window
      if ((len = slen) == 2) {
        if (!chk)
          vwr.showUrl(eval.getFullPathName());
        return;
      }
      name = paramAsStr(2);
      if (!chk)
        vwr.showUrl(name);
      return;
    case T.color:
      str = "defaultColorScheme";
      break;
    case T.scale3d:
      str = "scaleAngstromsPerInch";
      break;
    case T.quaternion:
    case T.ramachandran:
      if (chk)
        return;
      int modelIndex = vwr.am.cmi;
      if (modelIndex < 0)
        eval.errorStr(ScriptError.ERROR_multipleModelsDisplayedNotOK, "show "
            + eval.theToken.value);
      msg = plot(st);
      len = slen;
      break;
    case T.context:
    case T.trace:
      if (!chk)
        msg = getContext(false);
      break;
    case T.colorscheme:
      name = eval.optParameterAsString(2);
      if (name.length() > 0)
        len = 3;
      if (!chk)
        value = vwr.cm.getColorSchemeList(name);
      break;
    case T.variables:
      if (!chk)
        msg = vwr.getAtomDefs(vwr.definedAtomSets) + vwr.g.getVariableList()
            + getContext(true);
      break;
    case T.trajectory:
      if (!chk)
        msg = vwr.getTrajectoryState();
      break;
    case T.historylevel:
      value = "" + eval.commandHistoryLevelMax;
      break;
    case T.loglevel:
      value = "" + Logger.getLogLevel();
      break;
    case T.debugscript:
      value = "" + vwr.getBoolean(T.debugscript);
      break;
    case T.strandcount:
      msg = "set strandCountForStrands " + vwr.getStrandCount(JC.SHAPE_STRANDS)
          + "; set strandCountForMeshRibbon "
          + vwr.getStrandCount(JC.SHAPE_MESHRIBBON);
      break;
    case T.timeout:
      msg = vwr.showTimeout((len = slen) == 2 ? null : paramAsStr(2));
      break;
    case T.defaultlattice:
      value = Escape.eP(vwr.getDefaultLattice());
      break;
    case T.minimize:
      if (!chk)
        msg = vwr.getMinimizationInfo();
      break;
    case T.axes:
      switch (vwr.g.axesMode) {
      case T.axesunitcell:
        msg = "set axesUnitcell";
        break;
      case T.axesmolecular:
        msg = "set axesMolecular";
        break;
      default:
        msg = "set axesWindow";
      }
      break;
    case T.bondmode:
      msg = "set bondMode " + (vwr.getBoolean(T.bondmodeor) ? "OR" : "AND");
      break;
    case T.strands:
      if (!chk)
        msg = "set strandCountForStrands "
            + vwr.getStrandCount(JC.SHAPE_STRANDS)
            + "; set strandCountForMeshRibbon "
            + vwr.getStrandCount(JC.SHAPE_MESHRIBBON);
      break;
    case T.hbond:
      msg = "set hbondsBackbone " + vwr.getBoolean(T.hbondsbackbone)
          + ";set hbondsSolid " + vwr.getBoolean(T.hbondssolid);
      break;
    case T.spin:
      if (!chk)
        msg = vwr.getSpinState();
      break;
    case T.ssbond:
      msg = "set ssbondsBackbone " + vwr.getBoolean(T.ssbondsbackbone);
      break;
    case T.display:// deprecated
    case T.selectionhalos:
      msg = "selectionHalos "
          + (vwr.getSelectionHalosEnabled() ? "ON" : "OFF");
      break;
    case T.hetero:
      msg = "set selectHetero " + vwr.getBoolean(T.hetero);
      break;
    case T.addhydrogens:
      msg = Escape.eAP(vwr.getAdditionalHydrogens(null, true, true, null));
      break;
    case T.hydrogen:
      msg = "set selectHydrogens " + vwr.getBoolean(T.hydrogen);
      break;
    case T.ambientpercent:
    case T.diffusepercent:
    case T.specular:
    case T.specularpower:
    case T.specularexponent:
    case T.lighting:
      if (!chk)
        msg = vwr.getLightingState();
      break;
    case T.saved:
    case T.save:
      if (!chk)
        msg = vwr.stm.listSavedStates();
      break;
    case T.unitcell:
      if (!chk)
        msg = vwr.getUnitCellInfoText();
      break;
    case T.coord:
      if ((len = slen) == 2) {
        if (!chk)
          msg = vwr.getCoordinateState(vwr.bsA());
        break;
      }
      String nameC = paramAsStr(2);
      if (!chk)
        msg = vwr.stm.getSavedCoordinates(nameC);
      break;
    case T.state:
      if (!chk && eval.outputBuffer == null)
        vwr.sm.clearConsole();
      if ((len = slen) == 2) {
        if (!chk)
          msg = vwr.getStateInfo();
        break;
      }
      name = paramAsStr(2);
      if (name.startsWith("/") && (len = slen) == 3) {
        if (!chk) {
          name = name.substring(1).toLowerCase();
          String[] info = PT.split(vwr.getStateInfo(), "\n");
          SB sb = new SB();
          for (int i = 0; i < info.length; i++)
            if (info[i].toLowerCase().indexOf(name) >= 0)
              sb.append(info[i]).appendC('\n');
          msg = sb.toString();
        }
        break;
      } else if (tokAt(2) == T.file && (len = slen) == 4) {
        if (!chk)
          msg = vwr.fm.getEmbeddedFileState(paramAsStr(3), true);
        break;
      }
      len = 3;
      if (!chk)
        msg = vwr.stm.getSavedState(name);
      break;
    case T.structure:
      if ((len = slen) == 2) {
        if (!chk)
          msg = vwr.ms.getProteinStructureState(vwr.bsA(), T.show);
        break;
      }
      String shape = paramAsStr(2);
      if (!chk)
        msg = vwr.stm.getSavedStructure(shape);
      break;
    case T.data:
      String dtype = ((len = slen) == 3 ? paramAsStr(2) : null);
      if (!chk) {
        Object[] data = vwr.getData(dtype);
        msg = (data == null ? "no data" : Escape.encapsulateData(
            (String) data[0], data[1], ((Integer) data[3]).intValue()));
      }
      break;
    case T.spacegroup:
      Map<String, Object> info = null;
      if ((len = slen) == 2) {
        if (!chk) {
          info = vwr.ms.getSymTemp(true).getSpaceGroupInfo(vwr.ms, null);
        }
      } else {
        String sg = paramAsStr(2);
        if (!chk)
          info = vwr.ms.getSymTemp(true).getSpaceGroupInfo(vwr.ms, PT.rep(sg, "''", "\""));
      }
      if (info != null)
        msg = "" + info.get("spaceGroupInfo") + info.get("symmetryInfo");
      break;
    case T.dollarsign:
      len = 3;
      msg = eval.setObjectProperty();
      break;
    case T.boundbox:
      if (!chk) {
        msg = vwr.ms.getBoundBoxCommand(true);
      }
      break;
    case T.center:
      if (!chk)
        msg = "center " + Escape.eP(vwr.tm.fixedRotationCenter);
      break;
    case T.draw:
      if (!chk)
        msg = (String) getShapeProperty(JC.SHAPE_DRAW, "command");
      break;
    case T.file:
      // as a string
      if (!chk)
        vwr.sm.clearConsole();
      if (slen == 2) {
        if (!chk)
          msg = vwr.getCurrentFileAsString("script");
        if (msg == null)
          msg = "<unavailable>";
        break;
      }
      len = 3;
      value = paramAsStr(2);
      if (!chk)
        msg = vwr.getFileAsString3(value, true, null);
      break;
    case T.frame:
      if (tokAt(2) == T.all && (len = 3) > 0)
        msg = vwr.getModelFileInfoAll();
      else
        msg = vwr.getModelFileInfo();
      break;
    case T.history:
      int n = ((len = slen) == 2 ? Integer.MAX_VALUE : intParameter(2));
      if (n < 1)
        invArg();
      if (!chk) {
        vwr.sm.clearConsole();
        if (eval.scriptLevel == 0)
          vwr.removeCommand();
        msg = vwr.getSetHistory(n);
      }
      break;
    case T.isosurface:
      if (!chk)
        msg = (String) getShapeProperty(JC.SHAPE_ISOSURFACE, "jvxlDataXml");
      break;
    case T.nbo:
    case T.mo:
      if (eval.optParameterAsString(2).equalsIgnoreCase("list")) {
        e.sm.loadShape(JC.SHAPE_MO);
        msg = (chk ? "" : (String) getShapeProperty(JC.SHAPE_MO, "list -1"));
        len = 3;
      } else {
        int ptMO = ((len = slen) == 2 ? Integer.MIN_VALUE : intParameter(2));
        if (!chk)
          msg = getMoJvxl(ptMO, tok == T.nbo);
      }
      break;
    case T.model:
      if (!chk)
        msg = vwr.ms.getModelInfoAsString();
      break;
    case T.measurements:
      if (!chk)
        msg = vwr.getMeasurementInfoAsString();
      break;
    case T.best:
      len = 3;
      if (!chk && slen == len)
        msg = vwr.getOrientationText(tokAt(2), null);
      break;
    case T.rotation:
      tok = tokAt(2);
      if (tok == T.nada)
        tok = T.rotation;
      else
        len = 3;
      //$FALL-THROUGH$
    case T.translation:
    case T.moveto:
      if (!chk)
        msg = vwr.getOrientationText(tok, null);
      break;
    case T.orientation:
      len = 2;
      if (slen > 3)
        break;
      switch (tok = tokAt(2)) {
      case T.translation:
      case T.rotation:
      case T.moveto:
      case T.nada:
        if (!chk)
          msg = vwr.getOrientationText(tok, null);
        break;
      default:
        name = eval.optParameterAsString(2);
        msg = vwr.getOrientationText(T.name, name);
      }
      len = slen;
      break;
    case T.pdbheader:
      if (!chk)
        msg = vwr.ms.getPDBHeader(vwr.am.cmi);
      break;
    case T.pointgroup:
      if (!chk)
        showString(vwr.ms.getPointGroupAsString(vwr.bsA(), false, null, 0, 0));
      return;
    case T.symmetry:
      if (!chk)
        msg = vwr.ms.getSymmetryInfoAsString();
      break;
    case T.transform:
      if (!chk)
        msg = "transform:\n" + vwr.tm.matrixRotate.toString();
      break;
    case T.zoom:
      msg = "zoom "
          + (vwr.tm.zoomEnabled ? ("" + vwr.tm.getZoomSetting()) : "off");
      break;
    case T.frank:
      msg = (vwr.getShowFrank() ? "frank ON" : "frank OFF");
      break;
    case T.radius:
      str = "solventProbeRadius";
      break;
    // Chime related
    case T.sequence:
      if ((len = slen) == 3 && tokAt(2) == T.off)
        tok = T.group1;
      //$FALL-THROUGH$
    case T.basepair:
    case T.chain:
    case T.residue:
    case T.selected:
    case T.group:
    case T.atoms:
    case T.info:
      //case T.bonds: // ?? was this ever implemented? in Chime?
      if (!chk)
        msg = vwr.getChimeInfo(tok);
      break;
    // not implemented
    case T.echo:
    case T.fontsize:
    case T.help:
    case T.solvent:
      value = "?";
      break;
    case T.mouse:
      String qualifiers = ((len = slen) == 2 ? null : paramAsStr(2));
      if (!chk)
        msg = vwr.getBindingInfo(qualifiers);
      break;
    case T.menu:
      if (!chk)
        value = vwr.getMenu("");
      break;
    case T.identifier:
      if (str.equalsIgnoreCase("fileHeader")) {
        if (!chk)
          msg = vwr.ms.getPDBHeader(vwr.am.cmi);
      }
      break;
    case T.json:
    case T.var:
      str = paramAsStr(len++);
      SV v = (SV) eval.getParameter(str, T.variable, true);
      if (!chk)
        if (tok == T.json) {
          msg = v.toJSON();
        } else {
          msg = v.escape();
        }
      break;
    }
    checkLength(len);
    if (chk)
      return;
    if (msg != null)
      showString(msg);
    else if (value != null)
      showString(str + " = " + value);
    else if (str != null) {
      if (str.indexOf(" ") >= 0)
        showString(str);
      else
        showString(str + " = "
            + ((SV) eval.getParameter(str, T.variable, true)).escape());
    }
  }

  private void stereo() throws ScriptException {
    STER stereoMode = STER.DOUBLE;
    // see www.usm.maine.edu/~rhodes/0Help/StereoViewing.html
    // stereo on/off
    // stereo color1 color2 6
    // stereo redgreen 5

    float degrees = TransformManager.DEFAULT_STEREO_DEGREES;
    boolean degreesSeen = false;
    int[] colors = null;
    int colorpt = 0;
    for (int i = 1; i < slen; ++i) {
      if (e.isColorParam(i)) {
        if (colorpt > 1)
          e.bad();
        if (colorpt == 0)
          colors = new int[2];
        if (!degreesSeen)
          degrees = 3;
        colors[colorpt] = e.getArgbParam(i);
        if (colorpt++ == 0)
          colors[1] = ~colors[0];
        i = e.iToken;
        continue;
      }
      switch (getToken(i).tok) {
      case T.on:
        e.checkLast(e.iToken = 1);
        e.iToken = 1;
        break;
      case T.off:
        e.checkLast(e.iToken = 1);
        stereoMode = STER.NONE;
        break;
      case T.integer:
      case T.decimal:
        degrees = floatParameter(i);
        degreesSeen = true;
        break;
      case T.identifier:
        if (!degreesSeen)
          degrees = 3;
        stereoMode = STER.getStereoMode(paramAsStr(i));
        if (stereoMode != null)
          break;
        //$FALL-THROUGH$
      default:
        invArg();
      }
    }
    if (chk)
      return;
    vwr.setStereoMode(colors, stereoMode, degrees);
  }

  private boolean struts() throws ScriptException {
    ScriptEval eval = e;
    boolean defOn = (tokAt(1) == T.only || tokAt(1) == T.on || slen == 1);
    int mad = eval.getMadParameter();
    if (mad == Integer.MAX_VALUE)
      return false;
    if (defOn)
      mad = Math.round(vwr.getFloat(T.strutdefaultradius) * 2000f);
    setShapeProperty(JC.SHAPE_STICKS, "type",
        Integer.valueOf(Edge.BOND_STRUT));
    eval.setShapeSizeBs(JC.SHAPE_STICKS, mad, null);
    setShapeProperty(JC.SHAPE_STICKS, "type",
        Integer.valueOf(Edge.BOND_COVALENT_MASK));
    return true;
  }



  ///////// private methods used by commands ///////////

  
  private void assign() throws ScriptException {
    int atomsOrBonds = tokAt(1);
    int index = atomExpressionAt(2).nextSetBit(0);
    int index2 = -1;
    String type = null;
    if (index < 0)
      return;
    if (atomsOrBonds == T.connect) {
      index2 = atomExpressionAt(++e.iToken).nextSetBit(0);
    } else {
      type = paramAsStr(++e.iToken);
    }
    P3 pt = (++e.iToken < slen ? centerParameter(e.iToken) : null);
    if (chk)
      return;
    switch (atomsOrBonds) {
    case T.atoms:
      e.clearDefinedVariableAtomSets();
      assignAtom(index, pt, type);
      break;
    case T.bonds:
      assignBond(index, (type + "p").charAt(0));
      break;
    case T.connect:
      assignConnect(index, index2);
    }
  }

  private void assignAtom(int atomIndex, P3 pt, String type) {
    if (type.equals("X"))
      vwr.setRotateBondIndex(-1);
    if (vwr.ms.at[atomIndex].mi != vwr.ms.mc - 1)
      return;
    vwr.clearModelDependentObjects();
    int ac = vwr.ms.ac;
    if (pt == null) {
      vwr.sm.modifySend(atomIndex,
          vwr.ms.at[atomIndex].mi, 1, e.fullCommand);
      // After this next command, vwr.modelSet will be a different instance
      vwr.ms.assignAtom(atomIndex, type, true);
      if (!PT.isOneOf(type, ";Mi;Pl;X;"))
        vwr.ms.setAtomNamesAndNumbers(atomIndex, -ac, null);
      vwr.sm.modifySend(atomIndex,
          vwr.ms.at[atomIndex].mi, -1, "OK");
      vwr.refresh(3, "assignAtom");
      return;
    }
    Atom atom = vwr.ms.at[atomIndex];
    BS bs = BSUtil.newAndSetBit(atomIndex);
    P3[] pts = new P3[] { pt };
    Lst<Atom> vConnections = new Lst<Atom>();
    vConnections.addLast(atom);
    int modelIndex = atom.mi;
    vwr.sm.modifySend(atomIndex, modelIndex, 3, e.fullCommand);
    try {
      bs = vwr.addHydrogensInline(bs, vConnections, pts);
      // new ModelSet here
      atomIndex = bs.nextSetBit(0);
      vwr.ms.assignAtom(atomIndex, type, false);
    } catch (Exception ex) {
      //
    }
    vwr.ms.setAtomNamesAndNumbers(atomIndex, -ac, null);
    vwr.sm.modifySend(atomIndex, modelIndex, -3, "OK");
  }

  private void assignBond(int bondIndex, char type) {
    int modelIndex = -1;
    try {
      modelIndex = vwr.ms.bo[bondIndex].atom1.mi;
      vwr.sm.modifySend(bondIndex, modelIndex, 2,
          e.fullCommand);
      BS bsAtoms = vwr.ms.setBondOrder(bondIndex, type);
      if (bsAtoms == null || type == '0')
        vwr.refresh(3, "setBondOrder");
      else
        vwr.addHydrogens(bsAtoms, false, true);
      vwr.sm.modifySend(bondIndex, modelIndex, -2, "" + type);
    } catch (Exception ex) {
      Logger.error("assignBond failed");
      vwr.sm.modifySend(bondIndex, modelIndex, -2, "ERROR " + ex);
    }
  }

  private void assignConnect(int index, int index2) {
    vwr.clearModelDependentObjects();
    float[][] connections = AU.newFloat2(1);
    connections[0] = new float[] { index, index2 };
    int modelIndex = vwr.ms.at[index].mi;
    vwr.sm.modifySend(index, modelIndex, 2, e.fullCommand);
    vwr.ms.connect(connections);
    // note that vwr.ms changes during the assignAtom command 
    vwr.ms.assignAtom(index, ".", true);
    vwr.ms.assignAtom(index2, ".", true);
    vwr.sm.modifySend(index, modelIndex, -2, "OK");
    vwr.refresh(3, "assignConnect");
  }

  private String getContext(boolean withVariables) {
    SB sb = new SB();
    ScriptContext context = e.thisContext;
    while (context != null) {
      if (withVariables) {
        if (context.vars != null) {
          sb.append(getScriptID(context));
          sb.append(StateManager.getVariableList(context.vars, 80,
              true, false));
        }
      } else {
        sb.append(ScriptError.getErrorLineMessage(context.functionName,
            context.scriptFileName, e.getLinenumber(context), context.pc,
            ScriptEval.statementAsString(vwr, context.statement, -9999,
                e.debugHigh)));
      }
      context = context.parentContext;
    }
    if (withVariables) {
      if (e.contextVariables != null) {
        sb.append(getScriptID(null));
        sb.append(StateManager.getVariableList(e.contextVariables, 80, true,
            false));
      }
    } else {
      sb.append(e.getErrorLineMessage2());
    }

    return sb.toString();
  }

  private String getIsosurfaceJvxl(boolean asMesh, int iShape) {
    return (chk ? "" : (String) getShapeProperty(iShape, asMesh ? "jvxlMeshX"
        : "jvxlDataXml"));
  }

  @SuppressWarnings("unchecked")
  private String getMoJvxl(int ptMO, boolean isNBO) throws ScriptException {
    // 0: all; Integer.MAX_VALUE: current;
    int iShape = (isNBO ? JC.SHAPE_NBO : JC.SHAPE_MO);
    e.sm.loadShape(iShape);
    int modelIndex = vwr.am.cmi;
    if (modelIndex < 0)
      e.errorStr(ScriptError.ERROR_multipleModelsDisplayedNotOK,
          "show/write MO/NBO");
    Map<String, Object> moData = (Map<String, Object>) vwr
        .ms.getInfo(modelIndex, "moData");
    if (moData == null)
      error(ScriptError.ERROR_moModelError);
    Integer n = (Integer) getShapeProperty(iShape, "moNumber");
    if (n == null || n.intValue() == 0)
      setShapeProperty(iShape, "init", Integer.valueOf(modelIndex));
    setShapeProperty(iShape, "moData", moData);
    return (String) getShapePropertyIndex(iShape, "showMO", ptMO);
  }

  private String getScriptID(ScriptContext context) {
    String fuName = (context == null ? e.functionName : "function "
        + context.functionName);
    String fiName = (context == null ? e.scriptFileName
        : context.scriptFileName);
    return "\n# " + fuName + " (file " + fiName
        + (context == null ? "" : " context " + context.id) + ")\n";
  }

  private Object getShapePropertyIndex(int shapeType, String propertyName,
                                       int index) {
    return e.sm.getShapePropertyIndex(shapeType, propertyName, index);
  }

  private T tokenAt(int i, T[] args) {
    return (i < args.length ? args[i] : null);
  }

  private static int tokAtArray(int i, T[] args) {
    return (i < args.length && args[i] != null ? args[i].tok : T.nada);
  }

  private float getPlotMinMax(float[] data, boolean isMax, int tok) {
    if (data == null)
      return 0;
    switch (tok) {
    case T.omega:
    case T.phi:
    case T.psi:
      return (isMax ? 180 : -180);
    case T.eta:
    case T.theta:
      return (isMax ? 360 : 0);
    case T.straightness:
      return (isMax ? 1 : -1);
    }
    float fmax = (isMax ? -1E10f : 1E10f);
    for (int i = data.length; --i >= 0;) {
      float f = data[i];
      if (Float.isNaN(f))
        continue;
      if (isMax == (f > fmax))
        fmax = f;
    }
    return fmax;
  }

  private Object parseDataArray(String str, boolean is3D) {
    str = Parser.fixDataString(str);
    int[] lines = Parser.markLines(str, '\n');
    int nLines = lines.length;
    if (!is3D) {
      float[][] data = AU.newFloat2(nLines);
      for (int iLine = 0, pt = 0; iLine < nLines; pt = lines[iLine++]) {
        String[] tokens = PT.getTokens(str.substring(pt, lines[iLine]));
        PT.parseFloatArrayData(tokens, data[iLine] = new float[tokens.length]);
      }
      return data;
    }

    String[] tokens = PT.getTokens(str.substring(0, lines[0]));
    if (tokens.length != 3)
      return new float[0][0][0];
    int nX = PT.parseInt(tokens[0]);
    int nY = PT.parseInt(tokens[1]);
    int nZ = PT.parseInt(tokens[2]);
    if (nX < 1 || nY < 1 || nZ < 1)
      return new float[1][1][1];
    float[][][] data = AU.newFloat3(nX, nY);
    int iX = 0;
    int iY = 0;
    for (int iLine = 1, pt = lines[0]; iLine < nLines && iX < nX; pt = lines[iLine++]) {
      tokens = PT.getTokens(str.substring(pt, lines[iLine]));
      if (tokens.length < nZ)
        continue;
      PT.parseFloatArrayData(tokens, data[iX][iY] = new float[tokens.length]);
      if (++iY == nY) {
        iX++;
        iY = 0;
      }
    }
    if (iX != nX) {
      System.out.println("Error reading 3D data -- nX = " + nX + ", but only "
          + iX + " blocks read");
      return new float[1][1][1];
    }
    return data;
  }


}
