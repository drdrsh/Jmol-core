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
import org.jmol.api.MepCalculationInterface;
import org.jmol.api.MinimizerInterface;
import org.jmol.api.SymmetryInterface;
import org.jmol.atomdata.RadiusData;
import org.jmol.atomdata.RadiusData.EnumType;
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
import org.jmol.script.JmolCmdExtension;
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
import org.jmol.shape.MeshCollection;
import org.jmol.util.BSUtil;
import org.jmol.util.BoxInfo;
import org.jmol.util.C;
import org.jmol.util.ColorEncoder;
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
import javajs.util.P4;
import javajs.util.PT;
import javajs.util.Quat;
import javajs.util.V3;

import org.jmol.util.SimpleUnitCell;
import org.jmol.util.TempArray;
import org.jmol.viewer.FileManager;
import org.jmol.viewer.JC;
import org.jmol.viewer.ShapeManager;
import org.jmol.viewer.StateManager;
import org.jmol.viewer.Viewer;
import org.jmol.viewer.Viewer.ACCESS;

public class CmdExt implements JmolCmdExtension {
  private Viewer vwr;
  private ScriptEval e;
  private ShapeManager sm;
  private boolean chk;
  private String fullCommand;
  private String thisCommand;
  private T[] st;
  private int slen;

  private Object[] lastData;

  final static int ERROR_invalidArgument = 22;

  public CmdExt() {
    // used by Reflection
  }

  @Override
  public JmolCmdExtension init(Object se) {
    e = (ScriptEval) se;
    vwr = e.vwr;
    sm = e.sm;
    return this;
  }

  @Override
  public boolean dispatch(int iTok, boolean b, T[] st) throws ScriptException {
    chk = e.chk;
    fullCommand = e.fullCommand;
    thisCommand = e.thisCommand;
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
    case T.mapproperty:
      mapProperty();
      break;
    case T.minimize:
      minimize();
      break;
    case T.modulation:
      modulation();
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
      write(null);
      break;
    case JC.SHAPE_CGO:
      return cgo();
    case JC.SHAPE_CONTACT:
      return contact();
    case JC.SHAPE_DIPOLES:
      return dipole();
    case JC.SHAPE_DRAW:
      return draw();
    case JC.SHAPE_ISOSURFACE:
    case JC.SHAPE_PLOT3D:
    case JC.SHAPE_PMESH:
      return isosurface(iTok);
    case JC.SHAPE_LCAOCARTOON:
      return lcaoCartoon();
    case JC.SHAPE_MEASURES:
      measure();
      return true;
    case JC.SHAPE_MO:
      return mo(b);
    case JC.SHAPE_POLYHEDRA:
      return polyhedra();
    case JC.SHAPE_ELLIPSOIDS:
      ellipsoid();
      break;
    case JC.SHAPE_STRUTS:
      return struts();
    }
    return false;
  }

  private BS atomExpressionAt(int i) throws ScriptException {
    return e.atomExpressionAt(i);
  }

  private void checkLength(int i) throws ScriptException {
    e.checkLength(i);
  }
  
  private void error(int err) throws ScriptException {
    e.error(err);
  }

  private void invArg() throws ScriptException {
    e.invArg();
  }

  private void invPO() throws ScriptException {
    error(ScriptError.ERROR_invalidParameterOrder);
  }

  private Object getShapeProperty(int shapeType, String propertyName) {
    return e.getShapeProperty(shapeType, propertyName);
  }

  private String paramAsStr(int i) throws ScriptException {
    return e.paramAsStr(i);
  }

  private P3 centerParameter(int i) throws ScriptException {
    return e.centerParameter(i);
  }

  private float floatParameter(int i) throws ScriptException {
    return e.floatParameter(i);
  }

  private P3 getPoint3f(int i, boolean allowFractional) throws ScriptException {
    return e.getPoint3f(i, allowFractional);
  }

  private P4 getPoint4f(int i) throws ScriptException {
    return e.getPoint4f(i);
  }

  private int intParameter(int index) throws ScriptException {
    return e.intParameter(index);
  }

  private boolean isFloatParameter(int index) {
    return e.isFloatParameter(index);
  }

  private void setShapeProperty(int shapeType, String propertyName,
                                Object propertyValue) {
    e.setShapeProperty(shapeType, propertyName, propertyValue);
  }

  private void showString(String s) {
    e.showString(s);
  }

  private String stringParameter(int index) throws ScriptException {
    return e.stringParameter(index);
  }

  private T getToken(int i) throws ScriptException {
    return e.getToken(i);
  }

  private int tokAt(int i) {
    return e.tokAt(i);
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
    int n = Integer.MIN_VALUE;
    if ((e.iToken = e.slen) >= 2) {
      e.clearDefinedVariableAtomSets();
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
          vwr.ms.assignAromaticBonds();
        return;
      case T.hbond:
        if (e.slen != 2) {
          // calculate hbonds STRUCTURE -- only the DSSP/DSSR structurally-defining H bonds
          asDSSP = (tokAt(++e.iToken) == T.structure);
          if (asDSSP)
            bs1 = vwr.bsA();
          else
            bs1 = atomExpressionAt(e.iToken);
          if (!asDSSP && !(asDSSP = (tokAt(++e.iToken) == T.structure)))
            bs2 = atomExpressionAt(e.iToken);
        }
        if (chk)
          return;
        n = vwr.autoHbond(bs1, bs2, false);
        if (n != Integer.MIN_VALUE)
          e.report(GT.i(GT._("{0} hydrogen bonds"), Math.abs(n)));
        return;
      case T.hydrogen:
        bs1 = (slen == 2 ? null : atomExpressionAt(2));
        e.checkLast(e.iToken);
        if (!chk)
          vwr.addHydrogens(bs1, false, false);
        return;
      case T.partialcharge:
        e.iToken = 1;
        bs1 = (slen == 2 ? null : atomExpressionAt(2));
        e.checkLast(e.iToken);
        if (!chk)
          vwr.calculatePartialCharges(bs1);
        return;
      case T.pointgroup:
        if (!chk)
          showString(vwr.calculatePointGroup());
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
        switch (tokAt(++e.iToken)) {
        case T.ramachandran:
          break;
        case T.dssr:
          if (chk)
            return;
          e.showString(vwr.getAnnotationParser().calculateDSSRStructure(vwr, bs1));
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
        bs1 = (e.iToken + 1 < slen ? atomExpressionAt(++e.iToken) : null);
        bs2 = (e.iToken + 1 < slen ? atomExpressionAt(++e.iToken) : null);
        checkLength(++e.iToken);
        if (!chk) {
          n = vwr.calculateStruts(bs1, bs2);
          if (n > 0) {
            setShapeProperty(JC.SHAPE_STICKS, "type",
                Integer.valueOf(Edge.BOND_STRUT));
            e.setShapePropertyBs(JC.SHAPE_STICKS, "color",
                Integer.valueOf(0x0FFFFFF), null);
            e.setShapeTranslucency(JC.SHAPE_STICKS, "", "translucent", 0.5f,
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
          e.iToken++;
          break;
        case T.nada:
          isFrom = !isSurface;
          break;
        case T.from:
          isFrom = true;
          e.iToken++;
          break;
        default:
          isFrom = true;
        }
        bs1 = (e.iToken + 1 < slen ? atomExpressionAt(++e.iToken) : vwr.bsA());
        checkLength(++e.iToken);
        if (!chk)
          vwr.calculateSurface(bs1, (isFrom ? Float.MAX_VALUE : -1));
        return;
      }
    }
    e.errorStr2(
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
    int fps = vwr.getInt(T.animationfps);
    float endTime = 10; // ten seconds by default
    int mode = 0;
    String fileName = "";
    Map<String, Object> params = vwr.captureParams;
    boolean looping = !vwr.am.animationReplayMode.name().equals("ONCE");
    int tok = tokAt(1);
    String sfps = "";
    switch (tok) {
    case T.nada:
      mode = T.end;
      break;
    case T.string:
      fileName = e.optParameterAsString(1);
      if (fileName.length() == 0) {
        mode = T.end;
        break;
      }
      if (!fileName.endsWith(".gif"))
        fileName += ".gif";
      String s = null;
      String axis = "y";
      int i = 2;
      switch (tokAt(i)) {
      case T.rock:
        looping = true;
        i = 3;
        axis = (tokAt(3) == T.integer ? "y" : e.optParameterAsString(i++)
            .toLowerCase());
        int n = (tokAt(i) == T.nada ? 5 : intParameter(i++));
        s = "; rotate Y 10 10;delay 2.0; rotate Y -10 -10; delay 2.0;rotate Y -10 -10; delay 2.0;rotate Y 10 10;delay 2.0";
        s = PT.rep(s, "10", "" + n);
        break;
      case T.spin:
        looping = true;
        i = 3;
        axis = e.optParameterAsString(i).toLowerCase();
        if (axis.length() > 0)
          i++;
        s = "; rotate Y 360 30;delay 15.0;";
        if (tokAt(i) == T.integer)
          sfps = " " + (fps = intParameter(i++));
        break;
      case T.decimal:
        endTime = floatParameter(2);
        break;
      case T.integer:
        fps = intParameter(2);
        break;
      }
      if (s != null) {
        if (!chk)
          vwr.setNavigationMode(false);
        if (axis == "" || "xyz".indexOf(axis) < 0)
          axis = "y";
        s = PT.rep(s, "Y", axis);
        s = "capture " + PT.esc(fileName) + sfps + s + ";capture;";
        e.cmdScript(0, null, s);
        return;
      }
      if (params != null)
        params = new Hashtable<String, Object>();
      mode = T.movie;
      params = new Hashtable<String, Object>();
      if (!looping)
        showString(GT.o(GT._("Note: Enable looping using {0}"),
            new Object[] { "ANIMATION MODE LOOP" }));
      showString(GT.o(GT._("Animation delay based on: {0}"),
          new Object[] { "ANIMATION FPS " + fps }));
      params.put("captureFps", Integer.valueOf(fps));
      break;
    case T.cancel:
    case T.on:
    case T.off:
      checkLength(2);
      mode = tok;
      break;
    default:
      invArg();
    }
    if (chk || params == null)
      return;
    params.put("type", "GIF");
    params.put("fileName", fileName);
    params.put("quality", Integer.valueOf(-1));
    params.put("endTime",
        Long.valueOf(System.currentTimeMillis() + (long) (endTime * 1000)));
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

  private boolean cgo() throws ScriptException {
    ScriptEval eval = e;
    sm.loadShape(JC.SHAPE_CGO);
    if (tokAt(1) == T.list && listIsosurface(JC.SHAPE_CGO))
      return false;
    int iptDisplayProperty = 0;
    String thisId = initIsosurface(JC.SHAPE_CGO);
    boolean idSeen = (thisId != null);
    boolean isWild = (idSeen && getShapeProperty(JC.SHAPE_CGO, "ID") == null);
    boolean isInitialized = false;
    Lst<Object> data = null;
    float translucentLevel = Float.MAX_VALUE;
    int[] colorArgb = new int[] { Integer.MIN_VALUE };
    int intScale = 0;
    for (int i = eval.iToken; i < slen; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      switch (getToken(i).tok) {
      case T.varray:
      case T.leftsquare:
      case T.spacebeforesquare:
        if (data != null || isWild)
          invArg();
        data = eval.listParameter(i, 2, Integer.MAX_VALUE);
        i = eval.iToken;
        continue;
      case T.scale:
        if (++i >= slen)
          error(ScriptError.ERROR_numberExpected);
        switch (getToken(i).tok) {
        case T.integer:
          intScale = intParameter(i);
          continue;
        case T.decimal:
          intScale = Math.round(floatParameter(i) * 100);
          continue;
        }
        error(ScriptError.ERROR_numberExpected);
        break;
      case T.color:
      case T.translucent:
      case T.opaque:
        translucentLevel = getColorTrans(eval, i, false, colorArgb);
        i = eval.iToken;
        idSeen = true;
        continue;
      case T.id:
        thisId = setShapeId(JC.SHAPE_CGO, ++i, idSeen);
        isWild = (getShapeProperty(JC.SHAPE_CGO, "ID") == null);
        i = eval.iToken;
        break;
      default:
        if (!eval.setMeshDisplayProperty(JC.SHAPE_CGO, 0, eval.theTok)) {
          if (eval.theTok == T.times || T.tokAttr(eval.theTok, T.identifier)) {
            thisId = setShapeId(JC.SHAPE_CGO, i, idSeen);
            i = eval.iToken;
            break;
          }
          invArg();
        }
        if (iptDisplayProperty == 0)
          iptDisplayProperty = i;
        i = eval.iToken;
        continue;
      }
      idSeen = (eval.theTok != T.delete);
      if (data != null && !isInitialized) {
        propertyName = "points";
        propertyValue = Integer.valueOf(intScale);
        isInitialized = true;
        intScale = 0;
      }
      if (propertyName != null)
        setShapeProperty(JC.SHAPE_CGO, propertyName, propertyValue);
    }
    finalizeObject(JC.SHAPE_CGO, colorArgb[0], translucentLevel,
        intScale, data != null, data, iptDisplayProperty, null);
    return true;
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

    boolean isQuaternion = false;
    boolean doRotate = false;
    boolean doTranslate = false;
    boolean doAnimate = false;
    boolean isFlexFit = false;
    Quat[] data1 = null, data2 = null;
    BS bsAtoms1 = null, bsAtoms2 = null;
    Lst<Object[]> vAtomSets = null;
    Lst<Object[]> vQuatSets = null;
    e.iToken = 0;
    float nSeconds = (isFloatParameter(1) ? floatParameter(++e.iToken)
        : Float.NaN);
    ///BS bsFrom = (tokAt(++iToken) == T.subset ? null : atomExpressionAt(iToken));
    //BS bsTo = (tokAt(++iToken) == T.subset ? null : atomExpressionAt(iToken));
    //if (bsFrom == null || bsTo == null)
    ///invArg();
    BS bsFrom = atomExpressionAt(++e.iToken);
    P3[] coordTo = null;
    BS bsTo = null;
    if (e.isArrayParameter(++e.iToken)) {
      coordTo = e.getPointArray(e.iToken, -1);
    } else if (tokAt(e.iToken) != T.atoms) {
      bsTo = atomExpressionAt(e.iToken);
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
    for (int i = e.iToken + 1; i < slen; ++i) {
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
        i = e.iToken;
        break;
      case T.bitset:
      case T.expressionBegin:
        if (vQuatSets != null)
          invArg();
        bsAtoms1 = atomExpressionAt(e.iToken);
        int tok = (isToSubsetOfFrom ? 0 : tokAt(e.iToken + 1));
        bsAtoms2 = (coordTo == null && e.isArrayParameter(e.iToken + 1) ? null
            : (tok == T.bitset || tok == T.expressionBegin ? atomExpressionAt(++e.iToken)
                : BSUtil.copy(bsAtoms1)));
        if (bsSubset != null) {
          bsAtoms1.and(bsSubset);
          if (bsAtoms2 != null)
            bsAtoms2.and(bsSubset);
        }

        if (bsAtoms2 == null)
          coordTo = e.getPointArray(++e.iToken, -1);
        else
          bsAtoms2.and(bsTo);
        if (vAtomSets == null)
          vAtomSets = new Lst<Object[]>();
        vAtomSets.addLast(new BS[] { bsAtoms1, bsAtoms2 });
        i = e.iToken;
        break;
      case T.varray:
        if (vAtomSets != null)
          invArg();
        isQuaternion = true;
        data1 = e.getQuaternionArray(((SV) e.theToken).getList(), T.list);
        getToken(++i);
        data2 = e.getQuaternionArray(((SV) e.theToken).getList(), T.list);
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
            e.evalError(ex.getMessage(), null);
          }
        if (isFlexFit) {
          float[] list;
          if (bsFrom == null
              || bsTo == null
              || (list = e.getSmilesExt().getFlexFitList(bsFrom, bsTo,
                  strSmiles, !isSmiles)) == null)
            return;
          vwr.setDihedrals(list, null, 1);
        }
        float stddev = e.getSmilesExt().getSmilesCorrelation(bsFrom, bsTo,
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
          e.evalError("option not implemented", null);
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
      if (!e.useThreads())
        doAnimate = false;
      if (vwr.rotateAboutPointsInternal(e, center, pt1, endDegrees / nSeconds,
          endDegrees, doAnimate, bsFrom, translation, ptsB, null)
          && doAnimate
          && e.isJS)
        throw new ScriptInterruption(e, "compare", 1);
    }
  }

  private void configuration() throws ScriptException {
    // if (!chk && vwr.getDisplayModelIndex() <= -2)
    // error(ERROR_backgroundModelError, "\"CONFIGURATION\"");
    BS bsAtoms;
    BS bsSelected = vwr.bsA();
    if (slen == 1) {
      bsAtoms = vwr.ms.setConformation(bsSelected);
      vwr.ms.addStateScript("select", null, bsSelected, null,
          "configuration", true, false);
    } else {
      int n = intParameter(e.checkLast(1));
      if (chk)
        return;
      bsAtoms = vwr.getConformation(vwr.am.cmi, n - 1,
          true);
      vwr.addStateScript("configuration " + n + ";", true, false);
    }
    if (chk)
      return;
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
      int ac = vwr.getAtomCount();
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
          atomIndex = vwr.getAtomIndexFromAtomNumber(iParam);
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
          BSUtil.invertInPlace(bs2, vwr.getAtomCount());
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
            vwr.gdata, font, "", colix, (short) 0, 0, 0, null);
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
          boolean isLegacy = eval.isStateScript && vwr.g.legacyAutoBonding;
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
      eval.definedAtomSets.put("_1", bs);
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

  private boolean contact() throws ScriptException {
    ScriptEval eval = e;
    sm.loadShape(JC.SHAPE_CONTACT);
    if (tokAt(1) == T.list && listIsosurface(JC.SHAPE_CONTACT))
      return false;
    int iptDisplayProperty = 0;
    eval.iToken = 1;
    String thisId = initIsosurface(JC.SHAPE_CONTACT);
    boolean idSeen = (thisId != null);
    boolean isWild = (idSeen && getShapeProperty(JC.SHAPE_CONTACT, "ID") == null);
    BS bsA = null;
    BS bsB = null;
    BS bs = null;
    RadiusData rd = null;
    float[] params = null;
    boolean colorDensity = false;
    SB sbCommand = new SB();
    int minSet = Integer.MAX_VALUE;
    int displayType = T.plane;
    int contactType = T.nada;
    float distance = Float.NaN;
    float saProbeRadius = Float.NaN;
    boolean localOnly = true;
    Boolean intramolecular = null;
    Object userSlabObject = null;
    int colorpt = 0;
    boolean colorByType = false;
    int tok;
    int modelIndex = Integer.MIN_VALUE;
    boolean okNoAtoms = (eval.iToken > 1);
    for (int i = eval.iToken; i < slen; ++i) {
      switch (tok = getToken(i).tok) {
      // these first do not need atoms defined
      default:
        okNoAtoms = true;
        if (!eval.setMeshDisplayProperty(JC.SHAPE_CONTACT, 0, eval.theTok)) {
          if (eval.theTok != T.times && !T.tokAttr(eval.theTok, T.identifier))
            invArg();
          thisId = setShapeId(JC.SHAPE_CONTACT, i, idSeen);
          i = eval.iToken;
          break;
        }
        if (iptDisplayProperty == 0)
          iptDisplayProperty = i;
        i = eval.iToken;
        continue;
      case T.id:
        okNoAtoms = true;
        setShapeId(JC.SHAPE_CONTACT, ++i, idSeen);
        isWild = (getShapeProperty(JC.SHAPE_CONTACT, "ID") == null);
        i = eval.iToken;
        break;
      case T.color:
        switch (tokAt(i + 1)) {
        case T.density:
          tok = T.nada;
          colorDensity = true;
          sbCommand.append(" color density");
          i++;
          break;
        case T.type:
          tok = T.nada;
          colorByType = true;
          sbCommand.append(" color type");
          i++;
          break;
        }
        if (tok == T.nada)
          break;
        //$FALL-THROUGH$ to translucent
      case T.translucent:
      case T.opaque:
        okNoAtoms = true;
        if (colorpt == 0)
          colorpt = i;
        eval.setMeshDisplayProperty(JC.SHAPE_CONTACT, i, eval.theTok);
        i = eval.iToken;
        break;
      case T.slab:
        okNoAtoms = true;
        userSlabObject = getCapSlabObject(i, false);
        setShapeProperty(JC.SHAPE_CONTACT, "slab", userSlabObject);
        i = eval.iToken;
        break;

      // now after this you need atoms

      case T.density:
        colorDensity = true;
        sbCommand.append(" density");
        if (isFloatParameter(i + 1)) {
          if (params == null)
            params = new float[1];
          params[0] = -Math.abs(floatParameter(++i));
          sbCommand.append(" " + -params[0]);
        }
        break;
      case T.resolution:
        float resolution = floatParameter(++i);
        if (resolution > 0) {
          sbCommand.append(" resolution ").appendF(resolution);
          setShapeProperty(JC.SHAPE_CONTACT, "resolution",
              Float.valueOf(resolution));
        }
        break;
      case T.model:
      case T.modelindex:
        modelIndex = (eval.theTok == T.modelindex ? intParameter(++i) : eval
            .modelNumberParameter(++i));
        sbCommand.append(" modelIndex " + modelIndex);
        break;
      case T.within:
      case T.distance:
        distance = floatParameter(++i);
        sbCommand.append(" within ").appendF(distance);
        break;
      case T.plus:
      case T.integer:
      case T.decimal:
        rd = eval.encodeRadiusParameter(i, false, false);
        if (rd == null)
          return false;
        sbCommand.append(" ").appendO(rd);
        i = eval.iToken;
        break;
      case T.intermolecular:
      case T.intramolecular:
        intramolecular = (tok == T.intramolecular ? Boolean.TRUE
            : Boolean.FALSE);
        sbCommand.append(" ").appendO(eval.theToken.value);
        break;
      case T.minset:
        minSet = intParameter(++i);
        break;
      case T.hbond:
      case T.clash:
      case T.vanderwaals:
        contactType = tok;
        sbCommand.append(" ").appendO(eval.theToken.value);
        break;
      case T.sasurface:
        if (isFloatParameter(i + 1))
          saProbeRadius = floatParameter(++i);
        //$FALL-THROUGH$
      case T.cap:
      case T.nci:
      case T.surface:
        localOnly = false;
        //$FALL-THROUGH$
      case T.trim:
      case T.full:
      case T.plane:
      case T.connect:
        displayType = tok;
        sbCommand.append(" ").appendO(eval.theToken.value);
        if (tok == T.sasurface)
          sbCommand.append(" ").appendF(saProbeRadius);
        break;
      case T.parameters:
        params = eval.floatParameterSet(++i, 1, 10);
        i = eval.iToken;
        break;
      case T.bitset:
      case T.expressionBegin:
        if (isWild || bsB != null)
          invArg();
        bs = BSUtil.copy(atomExpressionAt(i));
        i = eval.iToken;
        if (bsA == null)
          bsA = bs;
        else
          bsB = bs;
        sbCommand.append(" ").append(Escape.eBS(bs));
        break;
      }
      idSeen = (eval.theTok != T.delete);
    }
    if (!okNoAtoms && bsA == null)
      error(ScriptError.ERROR_endOfStatementUnexpected);
    if (chk)
      return false;

    if (bsA != null) {
      // bond mode, intramolec set here
      if (contactType == T.vanderwaals && rd == null)
        rd = new RadiusData(null, 0, EnumType.OFFSET, VDW.AUTO);
      RadiusData rd1 = (rd == null ? new RadiusData(null, 0.26f,
          EnumType.OFFSET, VDW.AUTO) : rd);
      if (displayType == T.nci && bsB == null && intramolecular != null
          && intramolecular.booleanValue())
        bsB = bsA;
      else
        bsB = eval.getMathExt().setContactBitSets(bsA, bsB, localOnly, distance, rd1, true);
      switch (displayType) {
      case T.cap:
      case T.sasurface:
        BS bsSolvent = eval.lookupIdentifierValue("solvent");
        bsA.andNot(bsSolvent);
        bsB.andNot(bsSolvent);
        bsB.andNot(bsA);
        break;
      case T.surface:
        bsB.andNot(bsA);
        break;
      case T.nci:
        if (minSet == Integer.MAX_VALUE)
          minSet = 100;
        setShapeProperty(JC.SHAPE_CONTACT, "minset", Integer.valueOf(minSet));
        sbCommand.append(" minSet ").appendI(minSet);
        if (params == null)
          params = new float[] { 0.5f, 2 };
      }

      if (intramolecular != null) {
        params = (params == null ? new float[2] : AU.ensureLengthA(params, 2));
        params[1] = (intramolecular.booleanValue() ? 1 : 2);
      }

      if (params != null)
        sbCommand.append(" parameters ").append(Escape.eAF(params));

      // now adjust for type -- HBOND or HYDROPHOBIC or MISC
      // these are just "standard shortcuts" they are not necessary at all
      setShapeProperty(
          JC.SHAPE_CONTACT,
          "set",
          new Object[] { Integer.valueOf(contactType),
              Integer.valueOf(displayType), Boolean.valueOf(colorDensity),
              Boolean.valueOf(colorByType), bsA, bsB, rd,
              Float.valueOf(saProbeRadius), params, Integer.valueOf(modelIndex), sbCommand.toString() });
      if (colorpt > 0)
        eval.setMeshDisplayProperty(JC.SHAPE_CONTACT, colorpt, 0);
    }
    if (iptDisplayProperty > 0) {
      if (!eval.setMeshDisplayProperty(JC.SHAPE_CONTACT, iptDisplayProperty, 0))
        invArg();
    }
    if (userSlabObject != null && bsA != null)
      setShapeProperty(JC.SHAPE_CONTACT, "slab", userSlabObject);
    if (bsA != null && (displayType == T.nci || localOnly)) {
      Object volume = getShapeProperty(JC.SHAPE_CONTACT, "volume");
      double v;
      if (PT.isAD(volume)) {
        double[] vs = (double[]) volume;
        v = 0;
        for (int i = 0; i < vs.length; i++)
          v += Math.abs(vs[i]);
      } else {
        v = ((Float) volume).floatValue();
      }
      if (colorDensity || displayType != T.trim) {
        int nsets = ((Integer) getShapeProperty(JC.SHAPE_CONTACT, "nSets"))
            .intValue();
        String s = "Contacts: " + nsets;
        if (v != 0)
          s += " with net volume " + v + " A^3";
        showString(s);
      }
    }
    return true;
  }

  private boolean dipole() throws ScriptException {
    ScriptEval eval = e;
    // dipole intWidth floatMagnitude OFFSET floatOffset {atom1} {atom2}
    String propertyName = null;
    Object propertyValue = null;
    boolean iHaveAtoms = false;
    boolean iHaveCoord = false;
    boolean idSeen = false;

    sm.loadShape(JC.SHAPE_DIPOLES);
    if (tokAt(1) == T.list && listIsosurface(JC.SHAPE_DIPOLES))
      return false;
    setShapeProperty(JC.SHAPE_DIPOLES, "init", null);
    if (slen == 1) {
      setShapeProperty(JC.SHAPE_DIPOLES, "thisID", null);
      return false;
    }
    for (int i = 1; i < slen; ++i) {
      propertyName = null;
      propertyValue = null;
      switch (getToken(i).tok) {
      case T.all:
        propertyName = "all";
        break;
      case T.on:
        propertyName = "on";
        break;
      case T.off:
        propertyName = "off";
        break;
      case T.delete:
        propertyName = "delete";
        break;
      case T.integer:
      case T.decimal:
        propertyName = "value";
        propertyValue = Float.valueOf(floatParameter(i));
        break;
      case T.bitset:
        if (tokAt(i + 1) == T.bitset) {
          // fix for atomno2 < atomno1
          setShapeProperty(JC.SHAPE_DIPOLES, "startSet", atomExpressionAt(i++));
        } else {
          // early Jmol
          propertyName = "atomBitset";
        }
        //$FALL-THROUGH$
      case T.expressionBegin:
        if (propertyName == null)
          propertyName = (iHaveAtoms || iHaveCoord ? "endSet" : "startSet");
        propertyValue = atomExpressionAt(i);
        i = eval.iToken;
        if (tokAt(i + 1) == T.nada && propertyName == "startSet")
          propertyName = "atomBitset";
        iHaveAtoms = true;
        break;
      case T.leftbrace:
      case T.point3f:
        // {X, Y, Z}
        P3 pt = getPoint3f(i, true);
        i = eval.iToken;
        propertyName = (iHaveAtoms || iHaveCoord ? "endCoord" : "startCoord");
        propertyValue = pt;
        iHaveCoord = true;
        break;
      case T.bonds:
        propertyName = "bonds";
        break;
      case T.calculate:
        propertyName = "calculate";
        if (tokAt(i+1) == T.bitset || tokAt(i + 1) == T.expressionBegin) {
          propertyValue = atomExpressionAt(++i);
          i = eval.iToken;
        }
        break;
      case T.id:
        setShapeId(JC.SHAPE_DIPOLES, ++i, idSeen);
        i = eval.iToken;
        break;
      case T.cross:
        propertyName = "cross";
        propertyValue = Boolean.TRUE;
        break;
      case T.nocross:
        propertyName = "cross";
        propertyValue = Boolean.FALSE;
        break;
      case T.offset:
        if (isFloatParameter(i + 1)) {
        float v = floatParameter(++i);
        if (eval.theTok == T.integer) {
          propertyName = "offsetPercent";
          propertyValue = Integer.valueOf((int) v);
        } else {
          propertyName = "offset";
          propertyValue = Float.valueOf(v);
        }
        } else {
          propertyName = "offsetPt";
          propertyValue = centerParameter(++i);
          i = eval.iToken;
        }
        break;
      case T.offsetside:
        propertyName = "offsetSide";
        propertyValue = Float.valueOf(floatParameter(++i));
        break;
      case T.val:
        propertyName = "value";
        propertyValue = Float.valueOf(floatParameter(++i));
        break;
      case T.width:
        propertyName = "width";
        propertyValue = Float.valueOf(floatParameter(++i));
        break;
      default:
        if (eval.theTok == T.times || T.tokAttr(eval.theTok, T.identifier)) {
          setShapeId(JC.SHAPE_DIPOLES, i, idSeen);
          i = eval.iToken;
          break;
        }
        invArg();
      }
      idSeen = (eval.theTok != T.delete && eval.theTok != T.calculate);
      if (propertyName != null)
        setShapeProperty(JC.SHAPE_DIPOLES, propertyName, propertyValue);
    }
    if (iHaveCoord || iHaveAtoms)
      setShapeProperty(JC.SHAPE_DIPOLES, "set", null);
    return true;
  }

  private boolean draw() throws ScriptException {
    ScriptEval eval = e;
    sm.loadShape(JC.SHAPE_DRAW);
    switch (tokAt(1)) {
    case T.list:
      if (listIsosurface(JC.SHAPE_DRAW))
        return false;
      break;
    case T.pointgroup:
      // draw pointgroup [C2|C3|Cs|Ci|etc.] [n] [scale x]
      int pt = 2;
      String type = (tokAt(pt) == T.scale ? "" : e.optParameterAsString(pt));
      if (type.equals("chemicalShift"))
        type = "cs";
      float scale = 1;
      int index = 0;
      if (type.length() > 0) {
        if (isFloatParameter(++pt))
          index = intParameter(pt++);
      }
      if (tokAt(pt) == T.scale)
        scale = floatParameter(++pt);
      if (!chk)
        e.runScript(vwr.getPointGroupAsString(true, type, index, scale));
      return false;
    case T.helix:
    case T.quaternion:
    case T.ramachandran:
      plot(st);
      return false;
    }
    boolean havePoints = false;
    boolean isInitialized = false;
    boolean isSavedState = false;
    boolean isIntersect = false;
    boolean isFrame = false;
    P4 plane;
    int tokIntersect = 0;
    float translucentLevel = Float.MAX_VALUE;
    int[] colorArgb = new int[] { Integer.MIN_VALUE };
    int intScale = 0;
    String swidth = "";
    int iptDisplayProperty = 0;
    P3 center = null;
    String thisId = initIsosurface(JC.SHAPE_DRAW);
    boolean idSeen = (thisId != null);
    boolean isWild = (idSeen && getShapeProperty(JC.SHAPE_DRAW, "ID") == null);
    int[] connections = null;
    int iConnect = 0;
    for (int i = eval.iToken; i < slen; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      switch (getToken(i).tok) {
      case T.unitcell:
      case T.boundbox:
        if (chk)
          break;
        Lst<Object> vp = vwr.getPlaneIntersection(eval.theTok, null,
            intScale / 100f, 0);
        intScale = 0;
        propertyName = "polygon";
        propertyValue = vp;
        havePoints = true;
        break;
      case T.connect:
        connections = new int[4];
        iConnect = 4;
        float[] farray = eval.floatParameterSet(++i, 4, 4);
        i = eval.iToken;
        for (int j = 0; j < 4; j++)
          connections[j] = (int) farray[j];
        havePoints = true;
        break;
      case T.bonds:
      case T.atoms:
        if (connections == null
            || iConnect > (eval.theTok == T.bondcount ? 2 : 3)) {
          iConnect = 0;
          connections = new int[] { -1, -1, -1, -1 };
        }
        connections[iConnect++] = atomExpressionAt(++i).nextSetBit(0);
        i = eval.iToken;
        connections[iConnect++] = (eval.theTok == T.bonds ? atomExpressionAt(
            ++i).nextSetBit(0) : -1);
        i = eval.iToken;
        havePoints = true;
        break;
      case T.slab:
        switch (getToken(++i).tok) {
        case T.dollarsign:
          propertyName = "slab";
          propertyValue = eval.objectNameParameter(++i);
          i = eval.iToken;
          havePoints = true;
          break;
        default:
          invArg();
        }
        break;
      case T.intersection:
        switch (getToken(++i).tok) {
        case T.unitcell:
        case T.boundbox:
          tokIntersect = eval.theTok;
          isIntersect = true;
          continue;
        case T.dollarsign:
          propertyName = "intersect";
          propertyValue = eval.objectNameParameter(++i);
          i = eval.iToken;
          isIntersect = true;
          havePoints = true;
          break;
        default:
          invArg();
        }
        break;
      case T.polygon:
        propertyName = "polygon";
        havePoints = true;
        Lst<Object> v = new Lst<Object>();
        int nVertices = 0;
        int nTriangles = 0;
        P3[] points = null;
        Lst<SV> vpolygons = null;
        if (eval.isArrayParameter(++i)) {
          points = eval.getPointArray(i, -1);
          nVertices = points.length;
        } else {
          nVertices = Math.max(0, intParameter(i));
          points = new P3[nVertices];
          for (int j = 0; j < nVertices; j++)
            points[j] = centerParameter(++eval.iToken);
        }
        switch (getToken(++eval.iToken).tok) {
        case T.matrix3f:
        case T.matrix4f:
          SV sv = SV.newT(eval.theToken);
          sv.toArray();
          vpolygons = sv.getList();
          nTriangles = vpolygons.size();
          break;
        case T.varray:
          vpolygons = ((SV) eval.theToken).getList();
          nTriangles = vpolygons.size();
          break;
        default:
          nTriangles = Math.max(0, intParameter(eval.iToken));
        }
        int[][] polygons = AU.newInt2(nTriangles);
        for (int j = 0; j < nTriangles; j++) {
          float[] f = (vpolygons == null ? eval.floatParameterSet(
              ++eval.iToken, 3, 4) : SV.flistValue(vpolygons.get(j), 0));
          if (f.length < 3 || f.length > 4)
            invArg();
          polygons[j] = new int[] { (int) f[0], (int) f[1], (int) f[2],
              (f.length == 3 ? 7 : (int) f[3]) };
        }
        if (nVertices > 0) {
          v.addLast(points);
          v.addLast(polygons);
        } else {
          v = null;
        }
        propertyValue = v;
        i = eval.iToken;
        break;
      case T.symop:
        String xyz = null;
        int iSym = 0;
        plane = null;
        P3 target = null;
        switch (tokAt(++i)) {
        case T.string:
          xyz = stringParameter(i);
          break;
        case T.matrix4f:
          xyz = SV.sValue(getToken(i));
          break;
        case T.integer:
        default:
          if (!eval.isCenterParameter(i))
            iSym = intParameter(i++);
          if (eval.isCenterParameter(i))
            center = centerParameter(i);
          if (eval.isCenterParameter(eval.iToken + 1))
            target = centerParameter(++eval.iToken);
          if (chk)
            return false;
          i = eval.iToken;
        }
        BS bsAtoms = null;
        if (center == null && i + 1 < slen) {
          center = centerParameter(++i);
          // draw ID xxx symop [n or "x,-y,-z"] [optional {center}]
          // so we also check here for the atom set to get the right model
          bsAtoms = (tokAt(i) == T.bitset || tokAt(i) == T.expressionBegin ? atomExpressionAt(i)
              : null);
          i = eval.iToken + 1;
        }
        eval.checkLast(eval.iToken);
        if (!chk) {
          String s = (String) vwr.getSymmetryInfo(bsAtoms, xyz, iSym,
              center, target, thisId, T.draw);
          eval.runScript(s.length() > 0 ? s : "draw ID \"sym_" + thisId + "*\" delete");
        }
        return false;
      case T.frame:
        isFrame = true;
        // draw ID xxx frame {center} {q1 q2 q3 q4}
        continue;
      case T.leftbrace:
      case T.point4f:
      case T.point3f:
        // {X, Y, Z}
        if (eval.theTok == T.point4f || !eval.isPoint3f(i)) {
          propertyValue = getPoint4f(i);
          if (isFrame) {
            eval.checkLast(eval.iToken);
            if (!chk)
              eval.runScript(Escape.drawQuat(Quat.newP4((P4) propertyValue),
                  (thisId == null ? "frame" : thisId), " " + swidth,
                  (center == null ? new P3() : center), intScale / 100f));
            return false;
          }
          propertyName = "planedef";
        } else {
          propertyValue = center = getPoint3f(i, true);
          propertyName = "coord";
        }
        i = eval.iToken;
        havePoints = true;
        break;
      case T.hkl:
      case T.plane:
        if (!havePoints && !isIntersect && tokIntersect == 0
            && eval.theTok != T.hkl) {
          propertyName = "plane";
          break;
        }
        if (eval.theTok == T.plane) {
          plane = eval.planeParameter(i);
        } else {
          plane = eval.hklParameter(++i);
        }
        i = eval.iToken;
        if (tokIntersect != 0) {
          if (chk)
            break;
          Lst<Object> vpc = vwr.getPlaneIntersection(tokIntersect, plane,
              intScale / 100f, 0);
          intScale = 0;
          propertyName = "polygon";
          propertyValue = vpc;
        } else {
          propertyValue = plane;
          propertyName = "planedef";
        }
        havePoints = true;
        break;
      case T.linedata:
        propertyName = "lineData";
        propertyValue = eval.floatParameterSet(++i, 0, Integer.MAX_VALUE);
        i = eval.iToken;
        havePoints = true;
        break;
      case T.bitset:
      case T.expressionBegin:
        propertyName = "atomSet";
        propertyValue = atomExpressionAt(i);
        if (isFrame)
          center = centerParameter(i);
        i = eval.iToken;
        havePoints = true;
        break;
      case T.varray:
        propertyName = "modelBasedPoints";
        propertyValue = SV.strListValue(eval.theToken);
        havePoints = true;
        break;
      case T.spacebeforesquare:
      case T.comma:
        break;
      case T.leftsquare:
        // [x y] or [x y %]
        propertyValue = eval.xypParameter(i);
        if (propertyValue != null) {
          i = eval.iToken;
          propertyName = "coord";
          havePoints = true;
          break;
        }
        if (isSavedState)
          invArg();
        isSavedState = true;
        break;
      case T.rightsquare:
        if (!isSavedState)
          invArg();
        isSavedState = false;
        break;
      case T.reverse:
        propertyName = "reverse";
        break;
      case T.string:
        propertyValue = stringParameter(i);
        propertyName = "title";
        break;
      case T.vector:
        propertyName = "vector";
        break;
      case T.length:
        propertyValue = Float.valueOf(floatParameter(++i));
        propertyName = "length";
        break;
      case T.decimal:
        // $drawObject
        propertyValue = Float.valueOf(floatParameter(i));
        propertyName = "length";
        break;
      case T.modelindex:
        propertyName = "modelIndex";
        propertyValue = Integer.valueOf(intParameter(++i));
        break;
      case T.integer:
        if (isSavedState) {
          propertyName = "modelIndex";
          propertyValue = Integer.valueOf(intParameter(i));
        } else {
          intScale = intParameter(i);
        }
        break;
      case T.scale:
        if (++i >= slen)
          error(ScriptError.ERROR_numberExpected);
        switch (getToken(i).tok) {
        case T.integer:
          intScale = intParameter(i);
          continue;
        case T.decimal:
          intScale = Math.round(floatParameter(i) * 100);
          continue;
        }
        error(ScriptError.ERROR_numberExpected);
        break;
      case T.id:
        thisId = setShapeId(JC.SHAPE_DRAW, ++i, idSeen);
        isWild = (getShapeProperty(JC.SHAPE_DRAW, "ID") == null);
        i = eval.iToken;
        break;
      case T.modelbased:
        propertyName = "fixed";
        propertyValue = Boolean.FALSE;
        break;
      case T.fixed:
        propertyName = "fixed";
        propertyValue = Boolean.TRUE;
        break;
      case T.offset:
        P3 pt = getPoint3f(++i, true);
        i = eval.iToken;
        propertyName = "offset";
        propertyValue = pt;
        break;
      case T.crossed:
        propertyName = "crossed";
        break;
      case T.width:
        propertyValue = Float.valueOf(floatParameter(++i));
        propertyName = "width";
        swidth = propertyName + " " + propertyValue;
        break;
      case T.line:
        propertyName = "line";
        propertyValue = Boolean.TRUE;
        break;
      case T.curve:
        propertyName = "curve";
        break;
      case T.arc:
        propertyName = "arc";
        break;
      case T.arrow:
        propertyName = "arrow";
        break;
      case T.circle:
        propertyName = "circle";
        break;
      case T.cylinder:
        propertyName = "cylinder";
        break;
      case T.vertices:
        propertyName = "vertices";
        break;
      case T.nohead:
        propertyName = "nohead";
        break;
      case T.barb:
        propertyName = "isbarb";
        break;
      case T.rotate45:
        propertyName = "rotate45";
        break;
      case T.perpendicular:
        propertyName = "perp";
        break;
      case T.radius:
      case T.diameter:
        boolean isRadius = (eval.theTok == T.radius);
        float f = floatParameter(++i);
        if (isRadius)
          f *= 2;
        propertyValue = Float.valueOf(f);
        propertyName = (isRadius || tokAt(i) == T.decimal ? "width"
            : "diameter");
        swidth = propertyName
            + (tokAt(i) == T.decimal ? " " + f : " " + ((int) f));
        break;
      case T.dollarsign:
        // $drawObject[m]
        if ((tokAt(i + 2) == T.leftsquare || isFrame)) {
          P3 pto = center = centerParameter(i);
          i = eval.iToken;
          propertyName = "coord";
          propertyValue = pto;
          havePoints = true;
          break;
        }
        // $drawObject
        propertyValue = eval.objectNameParameter(++i);
        propertyName = "identifier";
        havePoints = true;
        break;
      case T.color:
      case T.translucent:
      case T.opaque:
        idSeen = true;
        translucentLevel = getColorTrans(eval, i, false, colorArgb);
        i = eval.iToken;
        continue;
      default:
        if (!eval.setMeshDisplayProperty(JC.SHAPE_DRAW, 0, eval.theTok)) {
          if (eval.theTok == T.times || T.tokAttr(eval.theTok, T.identifier)) {
            thisId = setShapeId(JC.SHAPE_DRAW, i, idSeen);
            i = eval.iToken;
            break;
          }
          invArg();
        }
        if (iptDisplayProperty == 0)
          iptDisplayProperty = i;
        i = eval.iToken;
        continue;
      }
      idSeen = (eval.theTok != T.delete);
      if (havePoints && !isInitialized && !isFrame) {
        setShapeProperty(JC.SHAPE_DRAW, "points", Integer.valueOf(intScale));
        isInitialized = true;
        intScale = 0;
      }
      if (havePoints && isWild)
        invArg();
      if (propertyName != null)
        setShapeProperty(JC.SHAPE_DRAW, propertyName, propertyValue);
    }
    finalizeObject(JC.SHAPE_DRAW, colorArgb[0], translucentLevel, intScale,
        havePoints, connections, iptDisplayProperty, null);
    return true;
  }

  public void data() throws ScriptException {
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
      vwr.connect((float[][]) parseDataArray(dataString, false));
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
    Object[] d = lastData = new Object[4];
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
      int ac = vwr.getAtomCount();
      int[] atomMap = null;
      BS bsTemp = BS.newN(ac);
      if (atomNumberField > 0) {
        atomMap = new int[ac + 2];
        for (int j = 0; j <= ac; j++)
          atomMap[j] = -1;
        for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1)) {
          int atomNo = vwr.getAtomNumber(j);
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
      sm.loadShape(JC.SHAPE_ELLIPSOIDS);
      setShapeProperty(JC.SHAPE_ELLIPSOIDS, "select", paramAsStr(2));
      i = eval.iToken;
      checkMore = true;
      isSet = true;
      break;
    case T.id:
    case T.times:
    case T.identifier:
      sm.loadShape(JC.SHAPE_ELLIPSOIDS);
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

  private boolean isosurface(int iShape) throws ScriptException {
    // also called by lcaoCartoon
    ScriptEval eval = e;
    sm.loadShape(iShape);
    if (tokAt(1) == T.list && listIsosurface(iShape))
      return false;
    int iptDisplayProperty = 0;
    boolean isIsosurface = (iShape == JC.SHAPE_ISOSURFACE);
    boolean isPmesh = (iShape == JC.SHAPE_PMESH);
    boolean isPlot3d = (iShape == JC.SHAPE_PLOT3D);
    boolean isLcaoCartoon = (iShape == JC.SHAPE_LCAOCARTOON);
    boolean surfaceObjectSeen = false;
    boolean planeSeen = false;
    boolean isMapped = false;
    boolean isBicolor = false;
    boolean isPhased = false;
    boolean doCalcArea = false;
    boolean doCalcVolume = false;
    boolean isCavity = false;
    boolean haveRadius = false;
    boolean toCache = false;
    boolean isFxy = false;
    boolean haveSlab = false;
    boolean haveIntersection = false;
    boolean isFrontOnly = false;
    float[] data = null;
    String cmd = null;
    int thisSetNumber = Integer.MIN_VALUE;
    int nFiles = 0;
    int nX, nY, nZ, ptX, ptY;
    float sigma = Float.NaN;
    float cutoff = Float.NaN;
    int ptWithin = 0;
    Boolean smoothing = null;
    int smoothingPower = Integer.MAX_VALUE;
    BS bs = null;
    BS bsSelect = null;
    BS bsIgnore = null;
    SB sbCommand = new SB();
    P3 pt;
    P4 plane = null;
    P3 lattice = null;
    P3[] pts;
    int color = 0;
    String str = null;
    int modelIndex = (chk ? 0 : Integer.MIN_VALUE);
    eval.setCursorWait(true);
    boolean idSeen = (initIsosurface(iShape) != null);
    boolean isWild = (idSeen && getShapeProperty(iShape, "ID") == null);
    boolean isColorSchemeTranslucent = false;
    boolean isInline = false;
    Object onlyOneModel = null;
    String translucency = null;
    String colorScheme = null;
    String mepOrMlp = null;
    M4[] symops = null;
    short[] discreteColixes = null;
    Lst<Object[]> propertyList = new Lst<Object[]>();
    boolean defaultMesh = false;
    if (isPmesh || isPlot3d)
      addShapeProperty(propertyList, "fileType", "Pmesh");

    for (int i = eval.iToken; i < slen; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      getToken(i);
      if (eval.theTok == T.identifier)
        str = paramAsStr(i);
      switch (eval.theTok) {
      // settings only
      case T.isosurfacepropertysmoothing:
        smoothing = (getToken(++i).tok == T.on ? Boolean.TRUE
            : eval.theTok == T.off ? Boolean.FALSE : null);
        if (smoothing == null)
          invArg();
        continue;
      case T.isosurfacepropertysmoothingpower:
        smoothingPower = intParameter(++i);
        continue;
        // offset, rotate, and scale3d don't need to be saved in sbCommand
        // because they are display properties
      case T.move: // Jmol 13.0.RC2 -- required for state saving after coordinate-based translate/rotate
        propertyName = "moveIsosurface";
        if (tokAt(++i) != T.matrix4f)
          invArg();
        propertyValue = getToken(i++).value;
        break;
      case T.symop:
        float[][] ff = floatArraySet(i + 2, intParameter(i + 1), 16);
        symops = new M4[ff.length];
        for (int j = symops.length; --j >= 0;)
          symops[j] = M4.newA16(ff[j]);
        i = eval.iToken;
        break;
      case T.symmetry:
        if (modelIndex < 0)
          modelIndex = Math.min(vwr.am.cmi, 0);
        boolean needIgnore = (bsIgnore == null);
        if (bsSelect == null)
          bsSelect = BSUtil.copy(vwr.bsA());
        // and in symop=1
        bsSelect.and(vwr.ms.getAtoms(T.symop, Integer.valueOf(1)));
        if (!needIgnore)
          bsSelect.andNot(bsIgnore);
        addShapeProperty(propertyList, "select", bsSelect);
        if (needIgnore) {
          bsIgnore = BSUtil.copy(bsSelect);
          BSUtil.invertInPlace(bsIgnore, vwr.getAtomCount());
          isFrontOnly = true;
          addShapeProperty(propertyList, "ignore", bsIgnore);
          sbCommand.append(" ignore ").append(Escape.eBS(bsIgnore));
        }
        sbCommand.append(" symmetry");
        if (color == 0)
          addShapeProperty(propertyList, "colorRGB", Integer.valueOf(T.symop));
        symops = vwr.ms.getSymMatrices(modelIndex);
        break;
      case T.offset:
        propertyName = "offset";
        propertyValue = centerParameter(++i);
        i = eval.iToken;
        break;
      case T.rotate:
        propertyName = "rotate";
        propertyValue = (tokAt(eval.iToken = ++i) == T.none ? null
            : getPoint4f(i));
        i = eval.iToken;
        break;
      case T.scale3d:
        propertyName = "scale3d";
        propertyValue = Float.valueOf(floatParameter(++i));
        break;
      case T.period:
        sbCommand.append(" periodic");
        propertyName = "periodic";
        break;
      case T.origin:
      case T.step:
      case T.point:
        propertyName = eval.theToken.value.toString();
        sbCommand.append(" ").appendO(eval.theToken.value);
        propertyValue = centerParameter(++i);
        sbCommand.append(" ").append(Escape.eP((P3) propertyValue));
        i = eval.iToken;
        break;
      case T.boundbox:
        if (fullCommand.indexOf("# BBOX=") >= 0) {
          String[] bbox = PT.split(
              PT.getQuotedAttribute(fullCommand, "# BBOX"), ",");
          pts = new P3[] { (P3) Escape.uP(bbox[0]), (P3) Escape.uP(bbox[1]) };
        } else if (eval.isCenterParameter(i + 1)) {
          pts = new P3[] { getPoint3f(i + 1, true),
              getPoint3f(eval.iToken + 1, true) };
          i = eval.iToken;
        } else {
          pts = vwr.ms.getBBoxVertices();
        }
        sbCommand.append(" boundBox " + Escape.eP(pts[0]) + " "
            + Escape.eP(pts[pts.length - 1]));
        propertyName = "boundingBox";
        propertyValue = pts;
        break;
      case T.pmesh:
        isPmesh = true;
        sbCommand.append(" pmesh");
        propertyName = "fileType";
        propertyValue = "Pmesh";
        break;
      case T.intersection:
        // isosurface intersection {A} {B} VDW....
        // isosurface intersection {A} {B} function "a-b" VDW....
        bsSelect = atomExpressionAt(++i);
        if (chk) {
          bs = new BS();
        } else if (tokAt(eval.iToken + 1) == T.expressionBegin
            || tokAt(eval.iToken + 1) == T.bitset) {
          bs = atomExpressionAt(++eval.iToken);
          bs.and(vwr.ms.getAtomsWithinRadius(5.0f, bsSelect, false, null));
        } else {
          // default is "within(5.0, selected) and not within(molecule,selected)"
          bs = vwr.ms.getAtomsWithinRadius(5.0f, bsSelect, true, null);
          bs.andNot(vwr.ms.getAtoms(T.molecule, bsSelect));
        }
        bs.andNot(bsSelect);
        sbCommand.append(" intersection ").append(Escape.eBS(bsSelect))
            .append(" ").append(Escape.eBS(bs));
        i = eval.iToken;
        if (tokAt(i + 1) == T.function) {
          i++;
          String f = (String) getToken(++i).value;
          sbCommand.append(" function ").append(PT.esc(f));
          if (!chk)
            addShapeProperty(
                propertyList,
                "func",
                (f.equals("a+b") || f.equals("a-b") ? f : createFunction(
                    "__iso__", "a,b", f)));
        } else {
          haveIntersection = true;
        }
        propertyName = "intersection";
        propertyValue = new BS[] { bsSelect, bs };
        break;
      case T.display:
      case T.within:
        boolean isDisplay = (eval.theTok == T.display);
        if (isDisplay) {
          sbCommand.append(" display");
          iptDisplayProperty = i;
          int tok = tokAt(i + 1);
          if (tok == T.nada)
            continue;
          i++;
          addShapeProperty(propertyList, "token", Integer.valueOf(T.on));
          if (tok == T.bitset || tok == T.all) {
            propertyName = "bsDisplay";
            if (tok == T.all) {
              sbCommand.append(" all");
            } else {
              propertyValue = st[i].value;
              sbCommand.append(" ").append(Escape.eBS((BS) propertyValue));
            }
            eval.checkLast(i);
            break;
          } else if (tok != T.within) {
            eval.iToken = i;
            invArg();
          }
        } else {
          ptWithin = i;
        }
        float distance;
        P3 ptc = null;
        bs = null;
        boolean havePt = false;
        if (tokAt(i + 1) == T.expressionBegin) {
          // within ( x.x , .... )
          distance = floatParameter(i + 3);
          if (eval.isPoint3f(i + 4)) {
            ptc = centerParameter(i + 4);
            havePt = true;
            eval.iToken = eval.iToken + 2;
          } else if (eval.isPoint3f(i + 5)) {
            ptc = centerParameter(i + 5);
            havePt = true;
            eval.iToken = eval.iToken + 2;
          } else {
            bs = eval.atomExpression(st, i + 5, slen, true, false, false, true);
            if (bs == null)
              invArg();
          }
        } else {
          distance = floatParameter(++i);
          ptc = centerParameter(++i);
        }
        if (isDisplay)
          eval.checkLast(eval.iToken);
        i = eval.iToken;
        if (fullCommand.indexOf("# WITHIN=") >= 0)
          bs = BS.unescape(PT.getQuotedAttribute(fullCommand, "# WITHIN"));
        else if (!havePt)
          bs = (eval.expressionResult instanceof BS ? (BS) eval.expressionResult
              : null);
        if (!chk) {
          if (bs != null && modelIndex >= 0) {
            bs.and(vwr.getModelUndeletedAtomsBitSet(modelIndex));
          }
          if (ptc == null)
            ptc = (bs == null ? new P3() : vwr.ms.getAtomSetCenter(bs));

          getWithinDistanceVector(propertyList, distance, ptc, bs, isDisplay);
          sbCommand.append(" within ").appendF(distance).append(" ")
              .append(bs == null ? Escape.eP(ptc) : Escape.eBS(bs));
        }
        continue;
      case T.parameters:
        propertyName = "parameters";
        // if > 1 parameter, then first is assumed to be the cutoff. 
        float[] fparams = eval.floatParameterSet(++i, 1, 10);
        i = eval.iToken;
        propertyValue = fparams;
        sbCommand.append(" parameters ").append(Escape.eAF(fparams));
        break;
      case T.property:
      case T.variable:
        onlyOneModel = eval.theToken.value;
        boolean isVariable = (eval.theTok == T.variable);
        int tokProperty = tokAt(i + 1);
        if (mepOrMlp == null) { // not mlp or mep
          if (!surfaceObjectSeen && !isMapped && !planeSeen) {
            addShapeProperty(propertyList, "sasurface", Float.valueOf(0));
            //if (surfaceObjectSeen)
            sbCommand.append(" vdw");
            surfaceObjectSeen = true;
          }
          propertyName = "property";
          if (smoothing == null) {
            boolean allowSmoothing = T.tokAttr(tokProperty, T.floatproperty);
            smoothing = (allowSmoothing
                && vwr.getIsosurfacePropertySmoothing(false) == 1 ? Boolean.TRUE
                : Boolean.FALSE);
          }
          addShapeProperty(propertyList, "propertySmoothing", smoothing);
          sbCommand.append(" isosurfacePropertySmoothing " + smoothing);
          if (smoothing == Boolean.TRUE) {
            if (smoothingPower == Integer.MAX_VALUE)
              smoothingPower = vwr.getIsosurfacePropertySmoothing(true);
            addShapeProperty(propertyList, "propertySmoothingPower",
                Integer.valueOf(smoothingPower));
            sbCommand.append(" isosurfacePropertySmoothingPower "
                + smoothingPower);
          }
          if (vwr.g.rangeSelected)
            addShapeProperty(propertyList, "rangeSelected", Boolean.TRUE);
        } else {
          propertyName = mepOrMlp;
        }
        str = paramAsStr(i);
        //        if (surfaceObjectSeen)
        sbCommand.append(" ").append(str);

        if (str.toLowerCase().indexOf("property_") == 0) {
          data = new float[vwr.getAtomCount()];
          if (chk)
            continue;
          data = vwr.getDataFloat(str);
          if (data == null)
            invArg();
          addShapeProperty(propertyList, propertyName, data);
          continue;
        }

        int ac = vwr.getAtomCount();
        data = new float[ac];

        if (isVariable) {
          String vname = paramAsStr(++i);
          if (vname.length() == 0) {
            data = eval.floatParameterSet(i, ac, ac);
          } else {
            data = new float[ac];
            if (!chk)
              Parser.parseStringInfestedFloatArray(
                  "" + eval.getParameter(vname, T.string, true), null, data);
          }
          if (!chk/* && (surfaceObjectSeen)*/)
            sbCommand.append(" \"\" ").append(Escape.eAF(data));
        } else {
          getToken(++i);
          if (!chk) {
            sbCommand.append(" " + eval.theToken.value);
            Atom[] atoms = vwr.ms.at;
            vwr.autoCalculate(tokProperty);
            if (tokProperty != T.color) {
              pt = new P3();
              for (int iAtom = ac; --iAtom >= 0;)
                data[iAtom] = Atom.atomPropertyFloat(vwr, atoms[iAtom],
                    tokProperty, pt);
            }
          }
          if (tokProperty == T.color)
            colorScheme = "inherit";
          if (tokAt(i + 1) == T.within) {
            float d = floatParameter(i = i + 2);
            sbCommand.append(" within " + d);
            addShapeProperty(propertyList, "propertyDistanceMax",
                Float.valueOf(d));
          }
        }
        propertyValue = data;
        break;
      case T.modelindex:
      case T.model:
        if (surfaceObjectSeen)
          invArg();
        modelIndex = (eval.theTok == T.modelindex ? intParameter(++i) : eval
            .modelNumberParameter(++i));
        sbCommand.append(" modelIndex " + modelIndex);
        if (modelIndex < 0) {
          propertyName = "fixed";
          propertyValue = Boolean.TRUE;
          break;
        }
        propertyName = "modelIndex";
        propertyValue = Integer.valueOf(modelIndex);
        break;
      case T.select:
        // in general, vwr.getCurrentSelection() is used, but we may
        // override that here. But we have to be careful that
        // we PREPEND the selection to the command if no surface object
        // has been seen yet, and APPEND it if it has.
        propertyName = "select";
        BS bs1 = atomExpressionAt(++i);
        propertyValue = bs1;
        i = eval.iToken;
        boolean isOnly = (tokAt(i + 1) == T.only);
        if (isOnly) {
          i++;
          bsIgnore = BSUtil.copy(bs1);
          BSUtil.invertInPlace(bsIgnore, vwr.getAtomCount());
          addShapeProperty(propertyList, "ignore", bsIgnore);
          sbCommand.append(" ignore ").append(Escape.eBS(bsIgnore));
          isFrontOnly = true;
        }
        if (surfaceObjectSeen || isMapped) {
          sbCommand.append(" select " + Escape.eBS(bs1));
        } else {
          bsSelect = (BS) propertyValue;
          if (modelIndex < 0 && bsSelect.nextSetBit(0) >= 0)
            modelIndex = vwr.getAtomModelIndex(bsSelect.nextSetBit(0));
        }
        break;
      case T.set:
        thisSetNumber = intParameter(++i);
        break;
      case T.center:
        propertyName = "center";
        propertyValue = centerParameter(++i);
        sbCommand.append(" center " + Escape.eP((P3) propertyValue));
        i = eval.iToken;
        break;
      case T.sign:
      case T.color:
        idSeen = true;
        boolean isSign = (eval.theTok == T.sign);
        if (isSign) {
          sbCommand.append(" sign");
          addShapeProperty(propertyList, "sign", Boolean.TRUE);
        } else {
          if (tokAt(i + 1) == T.density) {
            i++;
            propertyName = "colorDensity";
            sbCommand.append(" color density");
            if (isFloatParameter(i + 1)) {
              float ptSize = floatParameter(++i);
              sbCommand.append(" " + ptSize);
              propertyValue = Float.valueOf(ptSize);
            }
            break;
          }
          /*
           * "color" now is just used as an equivalent to "sign" and as an
           * introduction to "absolute" any other use is superfluous; it has
           * been replaced with MAP for indicating "use the current surface"
           * because the term COLOR is too general.
           */

          if (getToken(i + 1).tok == T.string) {
            colorScheme = paramAsStr(++i);
            if (colorScheme.indexOf(" ") > 0) {
              discreteColixes = C.getColixArray(colorScheme);
              if (discreteColixes == null)
                error(ScriptError.ERROR_badRGBColor);
            }
          } else if (eval.theTok == T.mesh) {
            i++;
            sbCommand.append(" color mesh");
            color = eval.getArgbParam(++i);
            addShapeProperty(propertyList, "meshcolor", Integer.valueOf(color));
            sbCommand.append(" ").append(Escape.escapeColor(color));
            i = eval.iToken;
            continue;
          }
          if ((eval.theTok = tokAt(i + 1)) == T.translucent
              || eval.theTok == T.opaque) {
            sbCommand.append(" color");
            translucency = setColorOptions(sbCommand, i + 1,
                JC.SHAPE_ISOSURFACE, -2);
            i = eval.iToken;
            continue;
          }
          switch (tokAt(i + 1)) {
          case T.absolute:
          case T.range:
            getToken(++i);
            sbCommand.append(" color range");
            addShapeProperty(propertyList, "rangeAll", null);
            if (tokAt(i + 1) == T.all) {
              i++;
              sbCommand.append(" all");
              continue;
            }
            float min = floatParameter(++i);
            float max = floatParameter(++i);
            addShapeProperty(propertyList, "red", Float.valueOf(min));
            addShapeProperty(propertyList, "blue", Float.valueOf(max));
            sbCommand.append(" ").appendF(min).append(" ").appendF(max);
            continue;
          }
          if (eval.isColorParam(i + 1)) {
            color = eval.getArgbParam(i + 1);
            if (tokAt(i + 2) == T.to) {
              colorScheme = eval.getColorRange(i + 1);
              i = eval.iToken;
              break;
            }
          }
          sbCommand.append(" color");
        }
        if (eval.isColorParam(i + 1)) {
          color = eval.getArgbParam(++i);
          sbCommand.append(" ").append(Escape.escapeColor(color));
          i = eval.iToken;
          addShapeProperty(propertyList, "colorRGB", Integer.valueOf(color));
          idSeen = true;
          if (eval.isColorParam(i + 1)) {
            color = eval.getArgbParam(++i);
            i = eval.iToken;
            addShapeProperty(propertyList, "colorRGB", Integer.valueOf(color));
            sbCommand.append(" ").append(Escape.escapeColor(color));
            isBicolor = true;
          } else if (isSign) {
            invPO();
          }
        } else if (!isSign && discreteColixes == null) {
          invPO();
        }
        continue;
      case T.cache:
        if (!isIsosurface)
          invArg();
        toCache = !chk;
        continue;
      case T.file:
        if (tokAt(i + 1) != T.string)
          invPO();
        continue;
      case T.bondingradius:
      case T.vanderwaals:
        //if (surfaceObjectSeen)
        sbCommand.append(" ").appendO(eval.theToken.value);
        RadiusData rd = eval.encodeRadiusParameter(i, false, true);
        if (rd == null)
          return false;
        //if (surfaceObjectSeen)
        sbCommand.append(" ").appendO(rd);
        if (Float.isNaN(rd.value))
          rd.value = 100;
        propertyValue = rd;
        propertyName = "radius";
        haveRadius = true;
        if (isMapped)
          surfaceObjectSeen = false;
        i = eval.iToken;
        break;
      case T.plane:
        // plane {X, Y, Z, W}
        planeSeen = true;
        propertyName = "plane";
        propertyValue = eval.planeParameter(i);
        i = eval.iToken;
        //if (surfaceObjectSeen)
        sbCommand.append(" plane ").append(Escape.eP4((P4) propertyValue));
        break;
      case T.scale:
        propertyName = "scale";
        propertyValue = Float.valueOf(floatParameter(++i));
        sbCommand.append(" scale ").appendO(propertyValue);
        break;
      case T.all:
        if (idSeen)
          invArg();
        propertyName = "thisID";
        break;
      case T.ellipsoid:
        // ellipsoid {xc yc zc f} where a = b and f = a/c
        // NOT OR ellipsoid {u11 u22 u33 u12 u13 u23}
        surfaceObjectSeen = true;
        ++i;
        //        ignoreError = true;
        //      try {
        propertyValue = getPoint4f(i);
        propertyName = "ellipsoid";
        i = eval.iToken;
        sbCommand.append(" ellipsoid ").append(Escape.eP4((P4) propertyValue));
        break;
      //        } catch (Exception e) {
      //        }
      //        try {
      //          propertyName = "ellipsoid";
      //          propertyValue = eval.floatParameterSet(i, 6, 6);
      //          i = eval.iToken;
      //          sbCommand.append(" ellipsoid ").append(
      //              Escape.eAF((float[]) propertyValue));
      //          break;
      //        } catch (Exception e) {
      //        }
      //        ignoreError = false;
      //        bs = atomExpressionAt(i);
      //        sbCommand.append(" ellipsoid ").append(Escape.eBS(bs));
      //        int iAtom = bs.nextSetBit(0);
      //        if (iAtom < 0)
      //          return;
      //        Atom[] atoms = vwr.modelSet.atoms;
      //        Tensor[] tensors = atoms[iAtom].getTensors();
      //        if (tensors == null || tensors.length < 1 || tensors[0] == null
      //            || (propertyValue = vwr.getQuadricForTensor(tensors[0], null)) == null)
      //          return;
      //        i = eval.iToken;
      //        propertyName = "ellipsoid";
      //        if (!chk)
      //          addShapeProperty(propertyList, "center", vwr.getAtomPoint3f(iAtom));
      //        break;
      case T.hkl:
        // miller indices hkl
        planeSeen = true;
        propertyName = "plane";
        propertyValue = eval.hklParameter(++i);
        i = eval.iToken;
        sbCommand.append(" plane ").append(Escape.eP4((P4) propertyValue));
        break;
      case T.lcaocartoon:
        surfaceObjectSeen = true;
        String lcaoType = paramAsStr(++i);
        addShapeProperty(propertyList, "lcaoType", lcaoType);
        sbCommand.append(" lcaocartoon ").append(PT.esc(lcaoType));
        switch (getToken(++i).tok) {
        case T.bitset:
        case T.expressionBegin:
          // automatically selects just the model of the first atom in the set.
          propertyName = "lcaoCartoon";
          bs = atomExpressionAt(i);
          i = eval.iToken;
          if (chk)
            continue;
          int atomIndex = bs.nextSetBit(0);
          if (atomIndex < 0)
            error(ScriptError.ERROR_expressionExpected);
          sbCommand.append(" ({").appendI(atomIndex).append("})");
          modelIndex = vwr.getAtomModelIndex(atomIndex);
          addShapeProperty(propertyList, "modelIndex",
              Integer.valueOf(modelIndex));
          V3[] axes = { new V3(), new V3(),
              V3.newV(vwr.getAtomPoint3f(atomIndex)), new V3() };
          if (!lcaoType.equalsIgnoreCase("s")
              && vwr.getHybridizationAndAxes(atomIndex, axes[0], axes[1],
                  lcaoType) == null)
            return false;
          propertyValue = axes;
          break;
        default:
          error(ScriptError.ERROR_expressionExpected);
        }
        break;
      case T.mo:
        // mo 1-based-index
        int moNumber = Integer.MAX_VALUE;
        int offset = Integer.MAX_VALUE;
        boolean isNegOffset = (tokAt(i + 1) == T.minus);
        if (isNegOffset)
          i++;
        float[] linearCombination = null;
        switch (tokAt(++i)) {
        case T.nada:
          eval.bad();
          break;
        case T.density:
          sbCommand.append("mo [1] squared ");
          addShapeProperty(propertyList, "squareLinear", Boolean.TRUE);
          linearCombination = new float[] { 1 };
          offset = moNumber = 0;
          i++;
          break;
        case T.homo:
        case T.lumo:
          offset = moOffset(i);
          moNumber = 0;
          i = eval.iToken;
          //if (surfaceObjectSeen) {
          sbCommand.append(" mo " + (isNegOffset ? "-" : "") + "HOMO ");
          if (offset > 0)
            sbCommand.append("+");
          if (offset != 0)
            sbCommand.appendI(offset);
          //}
          break;
        case T.integer:
          moNumber = intParameter(i);
          //if (surfaceObjectSeen)
          sbCommand.append(" mo ").appendI(moNumber);
          break;
        default:
          if (eval.isArrayParameter(i)) {
            linearCombination = eval.floatParameterSet(i, 1, Integer.MAX_VALUE);
            i = eval.iToken;
          }
        }
        boolean squared = (tokAt(i + 1) == T.squared);
        if (squared) {
          addShapeProperty(propertyList, "squareLinear", Boolean.TRUE);
          sbCommand.append(" squared");
          if (linearCombination == null)
            linearCombination = new float[0];
        } else if (tokAt(i + 1) == T.point) {
          ++i;
          int monteCarloCount = intParameter(++i);
          int seed = (tokAt(i + 1) == T.integer ? intParameter(++i)
              : ((int) -System.currentTimeMillis()) % 10000);
          addShapeProperty(propertyList, "monteCarloCount",
              Integer.valueOf(monteCarloCount));
          addShapeProperty(propertyList, "randomSeed", Integer.valueOf(seed));
          sbCommand.append(" points ").appendI(monteCarloCount).appendC(' ')
              .appendI(seed);
        }
        setMoData(propertyList, moNumber, linearCombination, offset,
            isNegOffset, modelIndex, null);
        surfaceObjectSeen = true;
        continue;
      case T.nci:
        propertyName = "nci";
        //if (surfaceObjectSeen)
        sbCommand.append(" " + propertyName);
        int tok = tokAt(i + 1);
        boolean isPromolecular = (tok != T.file && tok != T.string && tok != T.mrc);
        propertyValue = Boolean.valueOf(isPromolecular);
        if (isPromolecular)
          surfaceObjectSeen = true;
        break;
      case T.mep:
      case T.mlp:
        boolean isMep = (eval.theTok == T.mep);
        propertyName = (isMep ? "mep" : "mlp");
        //if (surfaceObjectSeen)
        sbCommand.append(" " + propertyName);
        String fname = null;
        int calcType = -1;
        surfaceObjectSeen = true;
        if (tokAt(i + 1) == T.integer) {
          calcType = intParameter(++i);
          sbCommand.append(" " + calcType);
          addShapeProperty(propertyList, "mepCalcType",
              Integer.valueOf(calcType));
        }
        if (tokAt(i + 1) == T.string) {
          fname = stringParameter(++i);
          //if (surfaceObjectSeen)
          sbCommand.append(" /*file*/" + PT.esc(fname));
        } else if (tokAt(i + 1) == T.property) {
          mepOrMlp = propertyName;
          continue;
        }
        if (!chk)
          try {
            data = (fname == null && isMep ? vwr.getPartialCharges()
                : getAtomicPotentials(bsSelect, bsIgnore, fname));
          } catch (Exception ex) {
            // ignore
          }
        if (!chk && data == null)
          error(ScriptError.ERROR_noPartialCharges);
        propertyValue = data;
        break;
      case T.volume:
        doCalcVolume = !chk;
        sbCommand.append(" volume");
        break;
      case T.id:
        setShapeId(iShape, ++i, idSeen);
        isWild = (getShapeProperty(iShape, "ID") == null);
        i = eval.iToken;
        break;
      case T.colorscheme:
        // either order NOT OK -- documented for TRANSLUCENT "rwb"
        if (tokAt(i + 1) == T.translucent) {
          isColorSchemeTranslucent = true;
          i++;
        }
        colorScheme = paramAsStr(++i).toLowerCase();
        if (colorScheme.equals("sets")) {
          sbCommand.append(" colorScheme \"sets\"");
        } else if (eval.isColorParam(i)) {
          colorScheme = eval.getColorRange(i);
          i = eval.iToken;
        }
        break;
      case T.addhydrogens:
        propertyName = "addHydrogens";
        propertyValue = Boolean.TRUE;
        sbCommand.append(" mp.addHydrogens");
        break;
      case T.angstroms:
        propertyName = "angstroms";
        sbCommand.append(" angstroms");
        break;
      case T.anisotropy:
        propertyName = "anisotropy";
        propertyValue = getPoint3f(++i, false);
        sbCommand.append(" anisotropy").append(Escape.eP((P3) propertyValue));
        i = eval.iToken;
        break;
      case T.area:
        doCalcArea = !chk;
        sbCommand.append(" area");
        break;
      case T.atomicorbital:
      case T.orbital:
        surfaceObjectSeen = true;
        if (isBicolor && !isPhased) {
          sbCommand.append(" phase \"_orb\"");
          addShapeProperty(propertyList, "phase", "_orb");
        }
        float[] nlmZprs = new float[7];
        nlmZprs[0] = intParameter(++i);
        nlmZprs[1] = intParameter(++i);
        nlmZprs[2] = intParameter(++i);
        nlmZprs[3] = (isFloatParameter(i + 1) ? floatParameter(++i) : 6f);
        //if (surfaceObjectSeen)
        sbCommand.append(" atomicOrbital ").appendI((int) nlmZprs[0])
            .append(" ").appendI((int) nlmZprs[1]).append(" ")
            .appendI((int) nlmZprs[2]).append(" ").appendF(nlmZprs[3]);
        if (tokAt(i + 1) == T.point) {
          i += 2;
          nlmZprs[4] = intParameter(i);
          nlmZprs[5] = (tokAt(i + 1) == T.decimal ? floatParameter(++i) : 0);
          nlmZprs[6] = (tokAt(i + 1) == T.integer ? intParameter(++i)
              : ((int) -System.currentTimeMillis()) % 10000);
          //if (surfaceObjectSeen)
          sbCommand.append(" points ").appendI((int) nlmZprs[4]).appendC(' ')
              .appendF(nlmZprs[5]).appendC(' ').appendI((int) nlmZprs[6]);
        }
        propertyName = "hydrogenOrbital";
        propertyValue = nlmZprs;
        break;
      case T.binary:
        sbCommand.append(" binary");
        // for PMESH, specifically
        // ignore for now
        continue;
      case T.blockdata:
        sbCommand.append(" blockData");
        propertyName = "blockData";
        propertyValue = Boolean.TRUE;
        break;
      case T.cap:
      case T.slab:
        haveSlab = true;
        propertyName = (String) eval.theToken.value;
        propertyValue = getCapSlabObject(i, false);
        i = eval.iToken;
        break;
      case T.cavity:
        if (!isIsosurface)
          invArg();
        isCavity = true;
        if (chk)
          continue;
        float cavityRadius = (isFloatParameter(i + 1) ? floatParameter(++i)
            : 1.2f);
        float envelopeRadius = (isFloatParameter(i + 1) ? floatParameter(++i)
            : 10f);
        if (envelopeRadius > 10f) {
          eval.integerOutOfRange(0, 10);
          return false;
        }
        sbCommand.append(" cavity ").appendF(cavityRadius).append(" ")
            .appendF(envelopeRadius);
        addShapeProperty(propertyList, "envelopeRadius",
            Float.valueOf(envelopeRadius));
        addShapeProperty(propertyList, "cavityRadius",
            Float.valueOf(cavityRadius));
        propertyName = "cavity";
        break;
      case T.contour:
      case T.contours:
        propertyName = "contour";
        sbCommand.append(" contour");
        switch (tokAt(i + 1)) {
        case T.discrete:
          propertyValue = eval.floatParameterSet(i + 2, 1, Integer.MAX_VALUE);
          sbCommand.append(" discrete ").append(
              Escape.eAF((float[]) propertyValue));
          i = eval.iToken;
          break;
        case T.increment:
          pt = getPoint3f(i + 2, false);
          if (pt.z <= 0 || pt.y < pt.x)
            invArg(); // from to step
          if (pt.z == (int) pt.z && pt.z > (pt.y - pt.x))
            pt.z = (pt.y - pt.x) / pt.z;
          propertyValue = pt;
          i = eval.iToken;
          sbCommand.append(" increment ").append(Escape.eP(pt));
          break;
        default:
          propertyValue = Integer
              .valueOf(tokAt(i + 1) == T.integer ? intParameter(++i) : 0);
          sbCommand.append(" ").appendO(propertyValue);
        }
        break;
      case T.decimal:
      case T.integer:
      case T.plus:
      case T.cutoff:
        sbCommand.append(" cutoff ");
        if (eval.theTok == T.cutoff)
          i++;
        if (tokAt(i) == T.plus) {
          propertyName = "cutoffPositive";
          propertyValue = Float.valueOf(cutoff = floatParameter(++i));
          sbCommand.append("+").appendO(propertyValue);
        } else if (isFloatParameter(i)) {
          propertyName = "cutoff";
          propertyValue = Float.valueOf(cutoff = floatParameter(i));
          sbCommand.appendO(propertyValue);
        } else {
          propertyName = "cutoffRange";
          propertyValue = eval.floatParameterSet(i, 2, 2);
          addShapeProperty(propertyList, "cutoff", Float.valueOf(0));
          sbCommand.append(Escape.eAF((float[]) propertyValue));
          i = eval.iToken;
        }
        break;
      case T.downsample:
        propertyName = "downsample";
        propertyValue = Integer.valueOf(intParameter(++i));
        //if (surfaceObjectSeen)
        sbCommand.append(" downsample ").appendO(propertyValue);
        break;
      case T.eccentricity:
        propertyName = "eccentricity";
        propertyValue = getPoint4f(++i);
        //if (surfaceObjectSeen)
        sbCommand.append(" eccentricity ").append(
            Escape.eP4((P4) propertyValue));
        i = eval.iToken;
        break;
      case T.ed:
        sbCommand.append(" ed");
        // electron density - never documented
        setMoData(propertyList, -1, null, 0, false, modelIndex, null);
        surfaceObjectSeen = true;
        continue;
      case T.debug:
      case T.nodebug:
        sbCommand.append(" ").appendO(eval.theToken.value);
        propertyName = "debug";
        propertyValue = (eval.theTok == T.debug ? Boolean.TRUE : Boolean.FALSE);
        break;
      case T.fixed:
        sbCommand.append(" fixed");
        propertyName = "fixed";
        propertyValue = Boolean.TRUE;
        break;
      case T.fullplane:
        sbCommand.append(" fullPlane");
        propertyName = "fullPlane";
        propertyValue = Boolean.TRUE;
        break;
      case T.functionxy:
      case T.functionxyz:
        // isosurface functionXY "functionName"|"data2d_xxxxx"
        // isosurface functionXYZ "functionName"|"data3d_xxxxx"
        // {origin} {ni ix iy iz} {nj jx jy jz} {nk kx ky kz}
        // or
        // isosurface origin.. step... count... functionXY[Z] = "x + y + z"
        boolean isFxyz = (eval.theTok == T.functionxyz);
        propertyName = "" + eval.theToken.value;
        Lst<Object> vxy = new Lst<Object>();
        propertyValue = vxy;
        isFxy = surfaceObjectSeen = true;
        //if (surfaceObjectSeen)
        sbCommand.append(" ").append(propertyName);
        String name = paramAsStr(++i);
        if (name.equals("=")) {
          //if (surfaceObjectSeen)
          sbCommand.append(" =");
          name = paramAsStr(++i);
          //if (surfaceObjectSeen)
          sbCommand.append(" ").append(PT.esc(name));
          vxy.addLast(name);
          if (!chk)
            addShapeProperty(propertyList, "func",
                createFunction("__iso__", "x,y,z", name));
          //surfaceObjectSeen = true;
          break;
        }
        // override of function or data name when saved as a state
        String dName = PT.getQuotedAttribute(fullCommand, "# DATA"
            + (isFxy ? "2" : ""));
        if (dName == null)
          dName = "inline";
        else
          name = dName;
        boolean isXYZ = (name.indexOf("data2d_") == 0);
        boolean isXYZV = (name.indexOf("data3d_") == 0);
        isInline = name.equals("inline");
        //if (!surfaceObjectSeen)
        sbCommand.append(" inline");
        vxy.addLast(name); // (0) = name
        P3 pt3 = getPoint3f(++i, false);
        //if (!surfaceObjectSeen)
        sbCommand.append(" ").append(Escape.eP(pt3));
        vxy.addLast(pt3); // (1) = {origin}
        P4 pt4;
        ptX = ++eval.iToken;
        vxy.addLast(pt4 = getPoint4f(ptX)); // (2) = {ni ix iy iz}
        //if (!surfaceObjectSeen)
        sbCommand.append(" ").append(Escape.eP4(pt4));
        nX = (int) pt4.x;
        ptY = ++eval.iToken;
        vxy.addLast(pt4 = getPoint4f(ptY)); // (3) = {nj jx jy jz}
        //if (!surfaceObjectSeen)
        sbCommand.append(" ").append(Escape.eP4(pt4));
        nY = (int) pt4.x;
        vxy.addLast(pt4 = getPoint4f(++eval.iToken)); // (4) = {nk kx ky kz}
        //if (!surfaceObjectSeen)
        sbCommand.append(" ").append(Escape.eP4(pt4));
        nZ = (int) pt4.x;

        if (nX == 0 || nY == 0 || nZ == 0)
          invArg();
        if (!chk) {
          float[][] fdata = null;
          float[][][] xyzdata = null;
          if (isFxyz) {
            if (isInline) {
              nX = Math.abs(nX);
              nY = Math.abs(nY);
              nZ = Math.abs(nZ);
              xyzdata = floatArraySetXYZ(++eval.iToken, nX, nY, nZ);
            } else if (isXYZV) {
              xyzdata = vwr.getDataFloat3D(name);
            } else {
              xyzdata = vwr.functionXYZ(name, nX, nY, nZ);
            }
            nX = Math.abs(nX);
            nY = Math.abs(nY);
            nZ = Math.abs(nZ);
            if (xyzdata == null) {
              eval.iToken = ptX;
              eval.errorStr(ScriptError.ERROR_what, "xyzdata is null.");
            }
            if (xyzdata.length != nX || xyzdata[0].length != nY
                || xyzdata[0][0].length != nZ) {
              eval.iToken = ptX;
              eval.errorStr(ScriptError.ERROR_what, "xyzdata["
                  + xyzdata.length + "][" + xyzdata[0].length + "]["
                  + xyzdata[0][0].length + "] is not of size [" + nX + "]["
                  + nY + "][" + nZ + "]");
            }
            vxy.addLast(xyzdata); // (5) = float[][][] data
            //if (!surfaceObjectSeen)
            sbCommand.append(" ").append(Escape.e(xyzdata));
          } else {
            if (isInline) {
              nX = Math.abs(nX);
              nY = Math.abs(nY);
              fdata = floatArraySet(++eval.iToken, nX, nY);
            } else if (isXYZ) {
              fdata = vwr.getDataFloat2D(name);
              nX = (fdata == null ? 0 : fdata.length);
              nY = 3;
            } else {
              fdata = vwr.functionXY(name, nX, nY);
              nX = Math.abs(nX);
              nY = Math.abs(nY);
            }
            if (fdata == null) {
              eval.iToken = ptX;
              eval.errorStr(ScriptError.ERROR_what, "fdata is null.");
            }
            if (fdata.length != nX && !isXYZ) {
              eval.iToken = ptX;
              eval.errorStr(ScriptError.ERROR_what,
                  "fdata length is not correct: " + fdata.length + " " + nX
                      + ".");
            }
            for (int j = 0; j < nX; j++) {
              if (fdata[j] == null) {
                eval.iToken = ptY;
                eval.errorStr(ScriptError.ERROR_what, "fdata[" + j
                    + "] is null.");
              }
              if (fdata[j].length != nY) {
                eval.iToken = ptY;
                eval.errorStr(ScriptError.ERROR_what, "fdata[" + j
                    + "] is not the right length: " + fdata[j].length + " "
                    + nY + ".");
              }
            }
            vxy.addLast(fdata); // (5) = float[][] data
            //if (!surfaceObjectSeen)
            sbCommand.append(" ").append(Escape.e(fdata));
          }
        }
        i = eval.iToken;
        break;
      case T.gridpoints:
        propertyName = "gridPoints";
        sbCommand.append(" gridPoints");
        break;
      case T.ignore:
        propertyName = "ignore";
        propertyValue = bsIgnore = atomExpressionAt(++i);
        sbCommand.append(" ignore ").append(Escape.eBS(bsIgnore));
        i = eval.iToken;
        break;
      case T.insideout:
        propertyName = "insideOut";
        sbCommand.append(" insideout");
        break;
      case T.internal:
      case T.interior:
      case T.pocket:
        //if (!surfaceObjectSeen)
        sbCommand.append(" ").appendO(eval.theToken.value);
        propertyName = "pocket";
        propertyValue = (eval.theTok == T.pocket ? Boolean.TRUE : Boolean.FALSE);
        break;
      case T.lobe:
        // lobe {eccentricity}
        propertyName = "lobe";
        propertyValue = getPoint4f(++i);
        i = eval.iToken;
        //if (!surfaceObjectSeen)
        sbCommand.append(" lobe ").append(Escape.eP4((P4) propertyValue));
        surfaceObjectSeen = true;
        break;
      case T.lonepair:
      case T.lp:
        // lp {eccentricity}
        propertyName = "lp";
        propertyValue = getPoint4f(++i);
        i = eval.iToken;
        //if (!surfaceObjectSeen)
        sbCommand.append(" lp ").append(Escape.eP4((P4) propertyValue));
        surfaceObjectSeen = true;
        break;
      case T.mapproperty:
        if (isMapped || slen == i + 1)
          invArg();
        isMapped = true;
        if ((isCavity || haveRadius || haveIntersection) && !surfaceObjectSeen) {
          surfaceObjectSeen = true;
          addShapeProperty(
              propertyList,
              "bsSolvent",
              (haveRadius || haveIntersection ? new BS() : eval
                  .lookupIdentifierValue("solvent")));
          addShapeProperty(propertyList, "sasurface", Float.valueOf(0));
        }
        if (sbCommand.length() == 0) {
          plane = (P4) getShapeProperty(JC.SHAPE_ISOSURFACE, "plane");
          if (plane == null) {
            if (getShapeProperty(JC.SHAPE_ISOSURFACE, "contours") != null) {
              addShapeProperty(propertyList, "nocontour", null);
            }
          } else {
            addShapeProperty(propertyList, "plane", plane);
            sbCommand.append("plane ").append(Escape.eP4(plane));
            planeSeen = true;
            plane = null;
          }
        } else if (!surfaceObjectSeen && !planeSeen) {
          invArg();
        }
        sbCommand.append("; isosurface map");
        addShapeProperty(propertyList, "map", (surfaceObjectSeen ? Boolean.TRUE
            : Boolean.FALSE));
        break;
      case T.maxset:
        propertyName = "maxset";
        propertyValue = Integer.valueOf(intParameter(++i));
        sbCommand.append(" maxSet ").appendO(propertyValue);
        break;
      case T.minset:
        propertyName = "minset";
        propertyValue = Integer.valueOf(intParameter(++i));
        sbCommand.append(" minSet ").appendO(propertyValue);
        break;
      case T.radical:
        // rad {eccentricity}
        surfaceObjectSeen = true;
        propertyName = "rad";
        propertyValue = getPoint4f(++i);
        i = eval.iToken;
        //if (!surfaceObjectSeen)
        sbCommand.append(" radical ").append(Escape.eP4((P4) propertyValue));
        break;
      case T.modelbased:
        propertyName = "fixed";
        propertyValue = Boolean.FALSE;
        sbCommand.append(" modelBased");
        break;
      case T.molecular:
      case T.sasurface:
      case T.solvent:
        onlyOneModel = eval.theToken.value;
        float radius;
        if (eval.theTok == T.molecular) {
          propertyName = "molecular";
          sbCommand.append(" molecular");
          radius = (isFloatParameter(i + 1) ? floatParameter(++i) : 1.4f);
        } else {
          addShapeProperty(propertyList, "bsSolvent",
              eval.lookupIdentifierValue("solvent"));
          propertyName = (eval.theTok == T.sasurface ? "sasurface" : "solvent");
          sbCommand.append(" ").appendO(eval.theToken.value);
          radius = (isFloatParameter(i + 1) ? floatParameter(++i) : vwr
              .getFloat(T.solventproberadius));
        }
        sbCommand.append(" ").appendF(radius);
        propertyValue = Float.valueOf(radius);
        if (tokAt(i + 1) == T.full) {
          addShapeProperty(propertyList, "doFullMolecular", null);
          //if (!surfaceObjectSeen)
          sbCommand.append(" full");
          i++;
        }
        surfaceObjectSeen = true;
        break;
      case T.mrc:
        addShapeProperty(propertyList, "fileType", "Mrc");
        sbCommand.append(" mrc");
        continue;
      case T.object:
      case T.obj:
        addShapeProperty(propertyList, "fileType", "Obj");
        sbCommand.append(" obj");
        continue;
      case T.msms:
        addShapeProperty(propertyList, "fileType", "Msms");
        sbCommand.append(" msms");
        continue;
      case T.phase:
        if (surfaceObjectSeen)
          invArg();
        propertyName = "phase";
        isPhased = true;
        propertyValue = (tokAt(i + 1) == T.string ? stringParameter(++i)
            : "_orb");
        sbCommand.append(" phase ").append(PT.esc((String) propertyValue));
        break;
      case T.pointsperangstrom:
      case T.resolution:
        propertyName = "resolution";
        propertyValue = Float.valueOf(floatParameter(++i));
        sbCommand.append(" resolution ").appendO(propertyValue);
        break;
      case T.reversecolor:
        propertyName = "reverseColor";
        propertyValue = Boolean.TRUE;
        sbCommand.append(" reversecolor");
        break;
      case T.sigma:
        propertyName = "sigma";
        propertyValue = Float.valueOf(sigma = floatParameter(++i));
        sbCommand.append(" sigma ").appendO(propertyValue);
        break;
      case T.geosurface:
        // geosurface [radius]
        propertyName = "geodesic";
        propertyValue = Float.valueOf(floatParameter(++i));
        //if (!surfaceObjectSeen)
        sbCommand.append(" geosurface ").appendO(propertyValue);
        surfaceObjectSeen = true;
        break;
      case T.sphere:
        // sphere [radius]
        propertyName = "sphere";
        propertyValue = Float.valueOf(floatParameter(++i));
        //if (!surfaceObjectSeen)
        sbCommand.append(" sphere ").appendO(propertyValue);
        surfaceObjectSeen = true;
        break;
      case T.squared:
        propertyName = "squareData";
        propertyValue = Boolean.TRUE;
        sbCommand.append(" squared");
        break;
      case T.inline:
        propertyName = (!surfaceObjectSeen && !planeSeen && !isMapped ? "readFile"
            : "mapColor");
        str = stringParameter(++i);
        if (str == null)
          invArg();
        // inline PMESH data
        if (isPmesh)
          str = PT.replaceWithCharacter(str, "{,}|", ' ');
        if (eval.debugHigh)
          Logger.debug("pmesh inline data:\n" + str);
        propertyValue = (chk ? null : str);
        addShapeProperty(propertyList, "fileName", "");
        sbCommand.append(" INLINE ").append(PT.esc(str));
        surfaceObjectSeen = true;
        break;
      case T.string:
        boolean firstPass = (!surfaceObjectSeen && !planeSeen);
        propertyName = (firstPass && !isMapped ? "readFile" : "mapColor");
        String filename = paramAsStr(i);
        /*
         * A file name, optionally followed by a calculation type and/or an integer file index.
         * Or =xxxx, an EDM from Uppsala Electron Density Server
         * If the model auxiliary info has "jmolSufaceInfo", we use that.
         */
        if (filename.startsWith("=") && filename.length() > 1) {
          String[] info = (String[]) vwr.setLoadFormat(filename, '_', false);
          filename = info[0];
          String strCutoff = (!firstPass || !Float.isNaN(cutoff) ? null
              : info[1]);
          if (strCutoff != null && !chk) {
            cutoff = Float.NaN;
            try {
            String sfdat = vwr.getFileAsString3(strCutoff, false, null);
            Logger.info(sfdat);
            sfdat = PT.split(sfdat,  "MAP_SIGMA_DENS")[1];
            cutoff = PT.parseFloat(sfdat);
            showString("using cutoff = " + cutoff);
            } catch (Exception e) {
              Logger.error("MAP_SIGMA_DENS -- could  not read " + info[1]);
            }
            if (cutoff > 0) {
              if (!Float.isNaN(sigma)) {
                cutoff *= sigma;
                sigma = Float.NaN;
                addShapeProperty(propertyList, "sigma", Float.valueOf(sigma));
              }
              addShapeProperty(propertyList, "cutoff", Float.valueOf(cutoff));
              sbCommand.append(" cutoff ").appendF(cutoff);
            }
          }
          if (ptWithin == 0) {
            onlyOneModel = "=xxxx";
            if (modelIndex < 0)
              modelIndex = vwr.am.cmi;
            bs = vwr.getModelUndeletedAtomsBitSet(modelIndex);
            if (bs.nextSetBit(0) >= 0) {
              getWithinDistanceVector(propertyList, 2.0f, null, bs, false);
              sbCommand.append(" within 2.0 ").append(Escape.eBS(bs));
            } 
          }
          if (firstPass)
            defaultMesh = true;
        }

        if (firstPass && vwr.getP("_fileType").equals("Pdb")
            && Float.isNaN(sigma) && Float.isNaN(cutoff)) {
          // negative sigma just indicates that 
          addShapeProperty(propertyList, "sigma", Float.valueOf(-1));
          sbCommand.append(" sigma -1.0");
        }
        if (filename.length() == 0) {
          if (modelIndex < 0)
            modelIndex = vwr.am.cmi;
          filename = eval.getFullPathName();
          propertyValue = vwr.getModelAuxiliaryInfoValue(modelIndex,
              "jmolSurfaceInfo");
        }
        int fileIndex = -1;
        if (propertyValue == null && tokAt(i + 1) == T.integer)
          addShapeProperty(propertyList, "fileIndex",
              Integer.valueOf(fileIndex = intParameter(++i)));
        String stype = (tokAt(i + 1) == T.string ? stringParameter(++i) : null);
        // done reading parameters
        surfaceObjectSeen = true;
        if (chk) {
          break;
        }
        String[] fullPathNameOrError;
        String localName = null;
        if (propertyValue == null) {
          if (fullCommand.indexOf("# FILE" + nFiles + "=") >= 0) {
            // old way, abandoned
            filename = PT.getQuotedAttribute(fullCommand, "# FILE" + nFiles);
            if (tokAt(i + 1) == T.as)
              i += 2; // skip that
          } else if (tokAt(i + 1) == T.as) {
            localName = vwr.getFilePath(
                stringParameter(eval.iToken = (i = i + 2)), false);
            fullPathNameOrError = vwr.getFullPathNameOrError(localName);
            localName = fullPathNameOrError[0];
            if (vwr.getPathForAllFiles() != "") {
              // we use the LOCAL name when reading from a local path only (in the case of JMOL files)
              filename = localName;
              localName = null;
            } else {
              addShapeProperty(propertyList, "localName", localName);
            }
          }
        }
        // just checking here, and getting the full path name
        if (!filename.startsWith("cache://") && stype == null) {
          fullPathNameOrError = vwr.getFullPathNameOrError(filename);
          filename = fullPathNameOrError[0];
          if (fullPathNameOrError[1] != null)
            eval.errorStr(ScriptError.ERROR_fileNotFoundException, filename
                + ":" + fullPathNameOrError[1]);
        }
        showString("reading isosurface data from " + filename);

        if (stype != null) {
          propertyValue = vwr.cacheGet(filename);
          addShapeProperty(propertyList, "calculationType", stype);
        }
        if (propertyValue == null) {
          addShapeProperty(propertyList, "fileName", filename);
          if (localName != null)
            filename = localName;
          if (fileIndex >= 0)
            sbCommand.append(" ").appendI(fileIndex);
        }
        sbCommand.append(" /*file*/").append(PT.esc(filename));
        if (stype != null)
          sbCommand.append(" ").append(PT.esc(stype));
        break;
      case T.connect:
        propertyName = "connections";
        switch (tokAt(++i)) {
        case T.bitset:
        case T.expressionBegin:
          propertyValue = new int[] { atomExpressionAt(i).nextSetBit(0) };
          break;
        default:
          propertyValue = new int[] { (int) eval.floatParameterSet(i, 1, 1)[0] };
          break;
        }
        i = eval.iToken;
        break;
      case T.atomindex:
        propertyName = "atomIndex";
        propertyValue = Integer.valueOf(intParameter(++i));
        break;
      case T.link:
        propertyName = "link";
        sbCommand.append(" link");
        break;
      case T.lattice:
        if (iShape != JC.SHAPE_ISOSURFACE)
          invArg();
        pt = getPoint3f(eval.iToken + 1, false);
        i = eval.iToken;
        if (pt.x <= 0 || pt.y <= 0 || pt.z <= 0)
          break;
        pt.x = (int) pt.x;
        pt.y = (int) pt.y;
        pt.z = (int) pt.z;
        sbCommand.append(" lattice ").append(Escape.eP(pt));
        if (isMapped) {
          propertyName = "mapLattice";
          propertyValue = pt;
        } else {
          lattice = pt;
        }
        break;
      default:
        if (eval.theTok == T.identifier) {
          propertyName = "thisID";
          propertyValue = str;
        }
        /* I have no idea why this is here....
        if (planeSeen && !surfaceObjectSeen) {
          addShapeProperty(propertyList, "nomap", Float.valueOf(0));
          surfaceObjectSeen = true;
        }
        */
        if (!eval.setMeshDisplayProperty(iShape, 0, eval.theTok)) {
          if (T.tokAttr(eval.theTok, T.identifier) && !idSeen) {
            setShapeId(iShape, i, idSeen);
            i = eval.iToken;
            break;
          }
          invArg();
        }
        if (iptDisplayProperty == 0)
          iptDisplayProperty = i;
        i = slen - 1;
        break;
      }
      idSeen = (eval.theTok != T.delete);
      if (isWild && surfaceObjectSeen)
        invArg();
      if (propertyName != null)
        addShapeProperty(propertyList, propertyName, propertyValue);
    }

    // OK, now send them all

    if (!chk) {
      if ((isCavity || haveRadius) && !surfaceObjectSeen) {
        surfaceObjectSeen = true;
        addShapeProperty(propertyList, "bsSolvent", (haveRadius ? new BS()
            : eval.lookupIdentifierValue("solvent")));
        addShapeProperty(propertyList, "sasurface", Float.valueOf(0));
      }
      if (planeSeen && !surfaceObjectSeen && !isMapped) {
        // !isMapped mp.added 6/14/2012 12.3.30
        // because it was preventing planes from being mapped properly
        addShapeProperty(propertyList, "nomap", Float.valueOf(0));
        surfaceObjectSeen = true;
      }
      if (thisSetNumber >= -1)
        addShapeProperty(propertyList, "getSurfaceSets",
            Integer.valueOf(thisSetNumber - 1));
      if (discreteColixes != null) {
        addShapeProperty(propertyList, "colorDiscrete", discreteColixes);
      } else if ("sets".equals(colorScheme)) {
        addShapeProperty(propertyList, "setColorScheme", null);
      } else if (colorScheme != null) {
        ColorEncoder ce = vwr.cm.getColorEncoder(colorScheme);
        if (ce != null) {
          ce.isTranslucent = isColorSchemeTranslucent;
          ce.hi = Float.MAX_VALUE;
          addShapeProperty(propertyList, "remapColor", ce);
        }
      }
      if (surfaceObjectSeen && !isLcaoCartoon && sbCommand.indexOf(";") != 0) {
        propertyList.add(0, new Object[] { "newObject", null });
        boolean needSelect = (bsSelect == null);
        if (needSelect)
          bsSelect = BSUtil.copy(vwr.bsA());
        if (modelIndex < 0)
          modelIndex = vwr.am.cmi;
        bsSelect.and(vwr.getModelUndeletedAtomsBitSet(modelIndex));
        if (onlyOneModel != null) {
          BS bsModels = vwr.ms.getModelBS(bsSelect, false);
          if (bsModels.cardinality() > 1)
            eval.errorStr(ScriptError.ERROR_multipleModelsDisplayedNotOK,
                "ISOSURFACE " + onlyOneModel);
          if (needSelect) {
            propertyList.add(0, new Object[] { "select", bsSelect });
            if (sbCommand.indexOf("; isosurface map") == 0) {
              sbCommand = new SB().append("; isosurface map select ")
                  .append(Escape.eBS(bsSelect)).append(sbCommand.substring(16));
            }
          }
        }
      }
      if (haveIntersection && !haveSlab) {
        if (!surfaceObjectSeen)
          addShapeProperty(propertyList, "sasurface", Float.valueOf(0));
        if (!isMapped) {
          addShapeProperty(propertyList, "map", Boolean.TRUE);
          addShapeProperty(propertyList, "select", bs);
          addShapeProperty(propertyList, "sasurface", Float.valueOf(0));
        }
        addShapeProperty(propertyList, "slab", getCapSlabObject(-100, false));
      }

      boolean timeMsg = (surfaceObjectSeen && vwr.getBoolean(T.showtiming));
      if (timeMsg)
        Logger.startTimer("isosurface");
      setShapeProperty(iShape, "setProperties", propertyList);
      if (timeMsg)
        showString(Logger.getTimerMsg("isosurface", 0));
      if (defaultMesh) {
        setShapeProperty(iShape, "token", Integer.valueOf(T.mesh));
        setShapeProperty(iShape, "token", Integer.valueOf(T.nofill));
        isFrontOnly = true;
        sbCommand.append(" mesh nofill frontOnly");
      }
    }
    if (lattice != null) // before MAP, this is a display option
      setShapeProperty(iShape, "lattice", lattice);
    if (symops != null) // before MAP, this is a display option
      setShapeProperty(iShape, "symops", symops);
    if (isFrontOnly)
      setShapeProperty(iShape, "token", Integer.valueOf(T.frontonly));
    if (iptDisplayProperty > 0) {
      if (!eval.setMeshDisplayProperty(iShape, iptDisplayProperty, 0))
        invArg();
    }
    if (chk)
      return false;
    Object area = null;
    Object volume = null;
    if (doCalcArea) {
      area = getShapeProperty(iShape, "area");
      if (area instanceof Float)
        vwr.setFloatProperty("isosurfaceArea", ((Float) area).floatValue());
      else
        vwr.g.setUserVariable("isosurfaceArea",
            SV.getVariableAD((double[]) area));
    }
    if (doCalcVolume) {
      volume = (doCalcVolume ? getShapeProperty(iShape, "volume") : null);
      if (volume instanceof Float)
        vwr.setFloatProperty("isosurfaceVolume",
            ((Float) volume).floatValue());
      else
        vwr.g.setUserVariable("isosurfaceVolume",
            SV.getVariableAD((double[]) volume));
    }
    if (!isLcaoCartoon) {
      String s = null;
      if (isMapped && !surfaceObjectSeen) {
        setShapeProperty(iShape, "finalize", sbCommand.toString());
      } else if (surfaceObjectSeen) {
        cmd = sbCommand.toString();
        setShapeProperty(
            iShape,
            "finalize",
            (cmd.indexOf("; isosurface map") == 0 ? "" : " select "
                + Escape.eBS(bsSelect) + " ")
                + cmd);
        s = (String) getShapeProperty(iShape, "ID");
        if (s != null && !eval.tQuiet) {
          cutoff = ((Float) getShapeProperty(iShape, "cutoff")).floatValue();
          if (Float.isNaN(cutoff) && !Float.isNaN(sigma)) {
            Logger.error("sigma not supported");
          }
          s += " created";
          if (isIsosurface)
            s += " with cutoff=" + cutoff;
          float[] minMax = (float[]) getShapeProperty(iShape, "minMaxInfo");
          if (minMax[0] != Float.MAX_VALUE)
            s += " min=" + minMax[0] + " max=" + minMax[1];
          s += "; " + JC.shapeClassBases[iShape].toLowerCase() + " count: "
              + getShapeProperty(iShape, "count");
          s += eval.getIsosurfaceDataRange(iShape, "\n");
        }
      }
      String sarea, svol;
      if (doCalcArea || doCalcVolume) {
        sarea = (doCalcArea ? "isosurfaceArea = "
            + (area instanceof Float ? "" + area : Escape.eAD((double[]) area))
            : null);
        svol = (doCalcVolume ? "isosurfaceVolume = "
            + (volume instanceof Float ? "" + volume : Escape
                .eAD((double[]) volume)) : null);
        if (s == null) {
          if (doCalcArea)
            showString(sarea);
          if (doCalcVolume)
            showString(svol);
        } else {
          if (doCalcArea)
            s += "\n" + sarea;
          if (doCalcVolume)
            s += "\n" + svol;
        }
      }
      if (s != null)
        showString(s);
    }
    if (translucency != null)
      setShapeProperty(iShape, "translucency", translucency);
    setShapeProperty(iShape, "clear", null);
    if (toCache)
      setShapeProperty(iShape, "cache", null);
    if (iShape != JC.SHAPE_LCAOCARTOON)
      listIsosurface(iShape);
    return true;
  }

  private boolean lcaoCartoon() throws ScriptException {
    ScriptEval eval = e;
    sm.loadShape(JC.SHAPE_LCAOCARTOON);
    if (tokAt(1) == T.list && listIsosurface(JC.SHAPE_LCAOCARTOON))
      return false;
    setShapeProperty(JC.SHAPE_LCAOCARTOON, "init", fullCommand);
    if (slen == 1) {
      setShapeProperty(JC.SHAPE_LCAOCARTOON, "lcaoID", null);
      return false;
    }
    boolean idSeen = false;
    String translucency = null;
    for (int i = 1; i < slen; i++) {
      String propertyName = null;
      Object propertyValue = null;
      switch (getToken(i).tok) {
      case T.cap:
      case T.slab:
        propertyName = (String) eval.theToken.value;
        if (tokAt(i + 1) == T.off)
          eval.iToken = i + 1;
        propertyValue = getCapSlabObject(i, true);
        i = eval.iToken;
        break;
      case T.center:
        // serialized lcaoCartoon in isosurface format
        isosurface(JC.SHAPE_LCAOCARTOON);
        return false;
      case T.rotate:
        float degx = 0;
        float degy = 0;
        float degz = 0;
        switch (getToken(++i).tok) {
        case T.x:
          degx = floatParameter(++i) * JC.radiansPerDegree;
          break;
        case T.y:
          degy = floatParameter(++i) * JC.radiansPerDegree;
          break;
        case T.z:
          degz = floatParameter(++i) * JC.radiansPerDegree;
          break;
        default:
          invArg();
        }
        propertyName = "rotationAxis";
        propertyValue = V3.new3(degx, degy, degz);
        break;
      case T.on:
      case T.display:
      case T.displayed:
        propertyName = "on";
        break;
      case T.off:
      case T.hide:
      case T.hidden:
        propertyName = "off";
        break;
      case T.delete:
        propertyName = "delete";
        break;
      case T.bitset:
      case T.expressionBegin:
        propertyName = "select";
        propertyValue = atomExpressionAt(i);
        i = eval.iToken;
        break;
      case T.color:
        translucency = setColorOptions(null, i + 1, JC.SHAPE_LCAOCARTOON, -2);
        if (translucency != null)
          setShapeProperty(JC.SHAPE_LCAOCARTOON, "settranslucency",
              translucency);
        i = eval.iToken;
        idSeen = true;
        continue;
      case T.translucent:
      case T.opaque:
        eval.setMeshDisplayProperty(JC.SHAPE_LCAOCARTOON, i, eval.theTok);
        i = eval.iToken;
        idSeen = true;
        continue;
      case T.spacefill:
      case T.string:
        propertyValue = paramAsStr(i).toLowerCase();
        if (propertyValue.equals("spacefill"))
          propertyValue = "cpk";
        propertyName = "create";
        if (eval.optParameterAsString(i + 1).equalsIgnoreCase("molecular")) {
          i++;
          propertyName = "molecular";
        }
        break;
      case T.select:
        if (tokAt(i + 1) == T.bitset || tokAt(i + 1) == T.expressionBegin) {
          propertyName = "select";
          propertyValue = atomExpressionAt(i + 1);
          i = eval.iToken;
        } else {
          propertyName = "selectType";
          propertyValue = paramAsStr(++i);
          if (propertyValue.equals("spacefill"))
            propertyValue = "cpk";
        }
        break;
      case T.scale:
        propertyName = "scale";
        propertyValue = Float.valueOf(floatParameter(++i));
        break;
      case T.lonepair:
      case T.lp:
        propertyName = "lonePair";
        break;
      case T.radical:
      case T.rad:
        propertyName = "radical";
        break;
      case T.molecular:
        propertyName = "molecular";
        break;
      case T.create:
        propertyValue = paramAsStr(++i);
        propertyName = "create";
        if (eval.optParameterAsString(i + 1).equalsIgnoreCase("molecular")) {
          i++;
          propertyName = "molecular";
        }
        break;
      case T.id:
        propertyValue = eval.setShapeNameParameter(++i);
        i = eval.iToken;
        if (idSeen)
          invArg();
        propertyName = "lcaoID";
        break;
      default:
        if (eval.theTok == T.times || T.tokAttr(eval.theTok, T.identifier)) {
          if (eval.theTok != T.times)
            propertyValue = paramAsStr(i);
          if (idSeen)
            invArg();
          propertyName = "lcaoID";
          break;
        }
        break;
      }
      if (eval.theTok != T.delete)
        idSeen = true;
      if (propertyName == null)
        invArg();
      setShapeProperty(JC.SHAPE_LCAOCARTOON, propertyName, propertyValue);
    }
    setShapeProperty(JC.SHAPE_LCAOCARTOON, "clear", null);
    return true;
  }

  private void mapProperty() throws ScriptException {
    // map {1.1}.straightness  {2.1}.property_x resno
    BS bsFrom, bsTo;
    String property1, property2, mapKey;
    int tokProp1 = 0;
    int tokProp2 = 0;
    int tokKey = 0;
    while (true) {
      if (tokAt(1) == T.selected) {
        bsFrom = vwr.bsA();
        bsTo = atomExpressionAt(2);
        property1 = property2 = "selected";
      } else {
        bsFrom = atomExpressionAt(1);
        if (tokAt(++e.iToken) != T.per
            || !T.tokAttr(tokProp1 = tokAt(++e.iToken), T.atomproperty))
          break;
        property1 = paramAsStr(e.iToken);
        bsTo = atomExpressionAt(++e.iToken);
        if (tokAt(++e.iToken) != T.per
            || !T.tokAttr(tokProp2 = tokAt(++e.iToken), T.settable))
          break;
        property2 = paramAsStr(e.iToken);
      }
      if (T.tokAttr(tokKey = tokAt(e.iToken + 1), T.atomproperty))
        mapKey = paramAsStr(++e.iToken);
      else
        mapKey = T.nameOf(tokKey = T.atomno);
      e.checkLast(e.iToken);
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
        float[] data1 = e.getBitsetPropertyFloat(bsFrom, tokProp1
            | T.selectedfloat, Float.NaN, Float.NaN);
        float[] data2 = e.getBitsetPropertyFloat(bsFrom, tokKey
            | T.selectedfloat, Float.NaN, Float.NaN);
        float[] data3 = e.getBitsetPropertyFloat(bsTo, tokKey
            | T.selectedfloat, Float.NaN, Float.NaN);
        boolean isProperty = (tokProp2 == T.property);
        float[] dataOut = new float[isProperty ? vwr.getAtomCount()
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
                Integer.valueOf(JmolDataManager.DATA_TYPE_AF), Boolean.TRUE }, vwr.getAtomCount(), 0,
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
        BS bsSubset = BSUtil.copy(vwr.getSelectionSubset());
        vwr.setSelectionSubset(bsTo);
        try {
          e.runScript(sb.toString());
        } catch (Exception ex) {
          vwr.setSelectionSubset(bsSubset);
          e.errorStr(-1, "Error: " + ex.getMessage());
        } catch (Error er) {
          vwr.setSelectionSubset(bsSubset);
          e.errorStr(-1, "Error: " + er.toString());
        }
        vwr.setSelectionSubset(bsSubset);
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
      vwr.minimize(steps, crit, bsSelected, bsFixed, 0, addHydrogen, isOnly,
          isSilent, false);
  }

  private boolean mo(boolean isInitOnly) throws ScriptException {
    ScriptEval eval = e;
    int offset = Integer.MAX_VALUE;
    boolean isNegOffset = false;
    BS bsModels = vwr.getVisibleFramesBitSet();
    Lst<Object[]> propertyList = new Lst<Object[]>();
    int i0 = 1;
    if (tokAt(1) == T.model || tokAt(1) == T.frame) {
      i0 = eval.modelNumberParameter(2);
      if (i0 < 0)
        invArg();
      bsModels.clearAll();
      bsModels.set(i0);
      i0 = 3;
    }
    for (int iModel = bsModels.nextSetBit(0); iModel >= 0; iModel = bsModels
        .nextSetBit(iModel + 1)) {
      sm.loadShape(JC.SHAPE_MO);
      int i = i0;
      if (tokAt(i) == T.list && listIsosurface(JC.SHAPE_MO))
        return true;
      setShapeProperty(JC.SHAPE_MO, "init", Integer.valueOf(iModel));
      String title = null;
      int moNumber = ((Integer) getShapeProperty(JC.SHAPE_MO, "moNumber"))
          .intValue();
      float[] linearCombination = (float[]) getShapeProperty(JC.SHAPE_MO,
          "moLinearCombination");
      if (isInitOnly)
        return true;// (moNumber != 0);
      if (moNumber == 0)
        moNumber = Integer.MAX_VALUE;
      String propertyName = null;
      Object propertyValue = null;

      switch (getToken(i).tok) {
      case T.cap:
      case T.slab:
        propertyName = (String) eval.theToken.value;
        propertyValue = getCapSlabObject(i, false);
        i = eval.iToken;
        break;
      case T.density:
        propertyName = "squareLinear";
        propertyValue = Boolean.TRUE;
        linearCombination = new float[] { 1 };
        offset = moNumber = 0;
        break;
      case T.integer:
        moNumber = intParameter(i);
        linearCombination = moCombo(propertyList);
        if (linearCombination == null && moNumber < 0)
          linearCombination = new float[] { -100, -moNumber };
        break;
      case T.minus:
        switch (tokAt(++i)) {
        case T.homo:
        case T.lumo:
          break;
        default:
          invArg();
        }
        isNegOffset = true;
        //$FALL-THROUGH$
      case T.homo:
      case T.lumo:
        if ((offset = moOffset(i)) == Integer.MAX_VALUE)
          invArg();
        moNumber = 0;
        linearCombination = moCombo(propertyList);
        break;
      case T.next:
        moNumber = T.next;
        linearCombination = moCombo(propertyList);
        break;
      case T.prev:
        moNumber = T.prev;
        linearCombination = moCombo(propertyList);
        break;
      case T.color:
        setColorOptions(null, i + 1, JC.SHAPE_MO, 2);
        break;
      case T.plane:
        // plane {X, Y, Z, W}
        propertyName = "plane";
        propertyValue = eval.planeParameter(i);
        break;
      case T.point:
        addShapeProperty(propertyList, "randomSeed",
            tokAt(i + 2) == T.integer ? Integer.valueOf(intParameter(i + 2))
                : null);
        propertyName = "monteCarloCount";
        propertyValue = Integer.valueOf(intParameter(i + 1));
        break;
      case T.scale:
        propertyName = "scale";
        propertyValue = Float.valueOf(floatParameter(i + 1));
        break;
      case T.cutoff:
        if (tokAt(i + 1) == T.plus) {
          propertyName = "cutoffPositive";
          propertyValue = Float.valueOf(floatParameter(i + 2));
        } else {
          propertyName = "cutoff";
          propertyValue = Float.valueOf(floatParameter(i + 1));
        }
        break;
      case T.debug:
        propertyName = "debug";
        break;
      case T.noplane:
        propertyName = "plane";
        break;
      case T.pointsperangstrom:
      case T.resolution:
        propertyName = "resolution";
        propertyValue = Float.valueOf(floatParameter(i + 1));
        break;
      case T.squared:
        propertyName = "squareData";
        propertyValue = Boolean.TRUE;
        break;
      case T.titleformat:
        if (i + 1 < slen && tokAt(i + 1) == T.string) {
          propertyName = "titleFormat";
          propertyValue = paramAsStr(i + 1);
        }
        break;
      case T.identifier:
        invArg();
        break;
      default:
        if (eval.isArrayParameter(i)) {
          linearCombination = eval.floatParameterSet(i, 1, Integer.MAX_VALUE);
          if (tokAt(eval.iToken + 1) == T.squared) {
            addShapeProperty(propertyList, "squareLinear", Boolean.TRUE);
            eval.iToken++;
          }
          break;
        }
        int ipt = eval.iToken;
        if (!eval.setMeshDisplayProperty(JC.SHAPE_MO, 0, eval.theTok))
          invArg();
        setShapeProperty(JC.SHAPE_MO, "setProperties", propertyList);
        eval.setMeshDisplayProperty(JC.SHAPE_MO, ipt, tokAt(ipt));
        return true;
      }
      if (propertyName != null)
        addShapeProperty(propertyList, propertyName, propertyValue);
      if (moNumber != Integer.MAX_VALUE || linearCombination != null) {
        if (tokAt(eval.iToken + 1) == T.string)
          title = paramAsStr(++eval.iToken);
        eval.setCursorWait(true);
        setMoData(propertyList, moNumber, linearCombination, offset,
            isNegOffset, iModel, title);
        addShapeProperty(propertyList, "finalize", null);
      }
      if (propertyList.size() > 0)
        setShapeProperty(JC.SHAPE_MO, "setProperties", propertyList);
      propertyList.clear();
    }
    return true;
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
    boolean mod = true;
    boolean isQ = false;
    BS bs = null;
    switch (getToken(1).tok) {
    case T.off:
      mod = false;
      //$FALL-THROUGH$
    case T.nada:
    case T.on:
      break;
    case T.bitset:
    case T.expressionBegin:
      bs = atomExpressionAt(1);
      switch (tokAt(e.iToken + 1)) {
      case T.nada:
        break;
      case T.off:
        mod = false;
        //$FALL-THROUGH$
      case T.on:
        e.iToken++;
        break;
      }
      e.checkLast(e.iToken);
      break;
    case T.leftbrace:
    case T.point3f:
      qtOffset = e.getPoint3f(1, false);
      isQ = (tokAt(e.iToken + 1) == T.on);
      break;
    case T.decimal:
      float t1 = floatParameter(1);
      qtOffset = P3.new3(t1, t1, t1);
      break;
    case T.integer:
      int t = intParameter(1);
      qtOffset = P3.new3(t, t, t);
      isQ = true;
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
      vwr.setVibrationOff();
      vwr.setModulation(bs, mod, qtOffset, isQ);
    }
  }

  public void navigate() throws ScriptException {
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

  @Override
  public String plot(T[] args) throws ScriptException {
    st = e.st;
    chk = e.chk;
    // also used for draw [quaternion, helix, ramachandran] 
    // and write quaternion, ramachandran, plot, ....
    // and plot property propertyX, propertyY, propertyZ //
    int modelIndex = vwr.am.cmi;
    if (modelIndex < 0)
      e.errorStr(ScriptError.ERROR_multipleModelsDisplayedNotOK, "plot");
    modelIndex = vwr.ms.getJmolDataSourceFrame(modelIndex);
    int pt = args.length - 1;
    boolean isReturnOnly = (args != st);
    boolean pdbFormat = true;
    T[] statementSave = st;
    if (isReturnOnly)
      e.st = st = args;
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
        e.st = st = statementSave;
        e.iToken = st.length;
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
    String type = e.optParameterAsString(pt).toLowerCase();
    P3 minXYZ = null;
    P3 maxXYZ = null;
    String format = null;
    int tok = tokAtArray(pt0, args);
    if (tok == T.string)
      tok = T.getTokFromName((String) args[pt0].value);
    switch (tok) {
    default:
      e.iToken = 1;
      invArg();
      break;
    case T.data:
      e.iToken = 1;
      type = "data";
      preSelected = "";
      break;
    case T.property:
      e.iToken = pt0 + 1;
      propertyX = plotProp();
      if (propertyX == 0)
        invArg();
      propertyY = plotProp();
      propertyZ = plotProp();
      if (tokAt(e.iToken) == T.format) {
        format = stringParameter(++e.iToken);
        pdbFormat = false;
        e.iToken++;
      }
      if (tokAt(e.iToken) == T.min) {
        minXYZ = getPoint3f(++e.iToken, false);
        e.iToken++;
      }
      if (tokAt(e.iToken) == T.max) {
        maxXYZ = getPoint3f(++e.iToken, false);
        e.iToken++;
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
        type = e.optParameterAsString(--pt).toLowerCase();
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
        type = e.optParameterAsString(--pt).toLowerCase();
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
      type = ((pt <= pt0 ? "" : e.optParameterAsString(pt)) + "w").substring(0,
          1);
      if (type.equals("a") || type.equals("r"))
        isDerivative = true;
      if (!PT.isOneOf(type, ";w;x;y;z;r;a;")) // a absolute; r relative
        e.evalError("QUATERNION [w,x,y,z,a,r] [difference][2]", null);
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
      dataX = e.getBitsetPropertyFloat(bs, propertyX | T.selectedfloat,
          (minXYZ == null ? Float.NaN : minXYZ.x), (maxXYZ == null ? Float.NaN
              : maxXYZ.x));
      if (propertyY != 0)
        dataY = e.getBitsetPropertyFloat(bs, propertyY | T.selectedfloat,
            (minXYZ == null ? Float.NaN : minXYZ.y),
            (maxXYZ == null ? Float.NaN : maxXYZ.y));
      if (propertyZ != 0)
        dataZ = e.getBitsetPropertyFloat(bs, propertyZ | T.selectedfloat,
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
      e.runScript(data);
      return "";
    }

    // create the new model

    String[] savedFileInfo = vwr.getFileInfo();
    boolean oldAppendNew = vwr.getBoolean(T.appendnew);
    vwr.g.appendNew = true;
    boolean isOK = (data != null && vwr.openStringInlineParamsAppend(data,
        null, true) == null);
    vwr.g.appendNew = oldAppendNew;
    vwr.setFileInfo(savedFileInfo);
    if (!isOK)
      return "";
    int modelCount = vwr.getModelCount();
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
      String color = (C.getHexCode(vwr.getColixBackgroundContrast()));
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
    e.runScript(script + preSelected);
    ss.setModelIndex(vwr.am.cmi);
    vwr.setRotationRadius(radius, true);
    sm.loadShape(JC.SHAPE_ECHO);
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
    sm.loadShape(JC.SHAPE_POLYHEDRA);
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
   * used for TRY command
   * 
   * @param context
   * @param shapeManager
   * @return true if successful; false if not
   */
  @Override
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
      se.dispatchCommands(false, false);
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

  @Override
  public String write(T[] args) throws ScriptException {
    int pt = 0, pt0 = 0;
    boolean isCommand, isShow;
    if (args == null) {
      args = st;
      pt = pt0 = 1;
      isCommand = true;
      isShow = (vwr.isApplet() && !vwr.isSignedApplet()
          || !vwr.haveAccess(ACCESS.ALL) || vwr.getPathForAllFiles().length() > 0);
    } else {
      isCommand = false;
      isShow = true;
    }
    int argCount = (isCommand ? slen : args.length);
    int len = 0;
    int nVibes = 0;
    int width = -1;
    int height = -1;
    int quality = Integer.MIN_VALUE;
    boolean timeMsg = vwr.getBoolean(T.showtiming);
    String driverList = vwr.getExportDriverList();
    String sceneType = "PNGJ";
    String data = null;
    String type2 = "";
    String fileName = null;
    String localPath = null;
    String remotePath = null;
    String val = null;
    String msg = null;
    SV tVar = null;
    String[] fullPath = new String[1];
    boolean isCoord = false;
    boolean isExport = false;
    boolean isImage = false;
    BS bsFrames = null;
    String[] scripts = null;
    Map<String, Object> params;
    String type = "SPT";
    int tok = (isCommand && args.length == 1 ? T.clipboard : tokAtArray(pt,
        args));
    switch (tok) {
    case T.nada:
      break;
    case T.script:
      // would fail in write() command.
      if (e.isArrayParameter(pt + 1)) {
        scripts = e.stringParameterSet(++pt);
        localPath = ".";
        remotePath = ".";
        pt0 = pt = e.iToken + 1;
        tok = tokAt(pt);
      }
      break;
    default:
      type = SV.sValue(tokenAt(pt, args)).toUpperCase();
    }
    if (isCommand && tokAt(slen - 2) == T.as) {
      type = paramAsStr(slen - 1).toUpperCase();
      pt0 = argCount;
      argCount -= 2;
      tok = T.nada;
    }
    switch (tok) {
    case T.nada:
      break;
    case T.barray:
    case T.hash:
      type = "VAR";
      tVar = (SV) tokenAt(pt++, args);
      break;
    case T.quaternion:
    case T.ramachandran:
    case T.property:
      msg = plot(args);
      if (!isCommand)
        return msg;
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
    case T.identifier:
    case T.image:
    case T.scene:
    case T.string:
    case T.vibration:
      switch (tok) {
      case T.image:
        pt++;
        break;
      case T.vibration:
        nVibes = e.intParameterRange(++pt, 1, 10);
        if (nVibes == Integer.MAX_VALUE)
          return "";
        if (!chk) {
          vwr.setVibrationOff();
          if (!e.isJS)
            e.delayScript(100);
        }
        pt++;
        break;
      case T.frame:
        BS bsAtoms;
        if (pt + 1 < argCount && args[++pt].tok == T.expressionBegin
            || args[pt].tok == T.bitset) {
          bsAtoms = e.atomExpression(args, pt, 0, true, false, true, true);
          pt = e.iToken + 1;
        } else {
          bsAtoms = vwr.getAllAtoms();
        }
        if (!chk)
          bsFrames = vwr.ms.getModelBS(bsAtoms, true);
        break;
      case T.scene:
        val = SV.sValue(tokenAt(++pt, args)).toUpperCase();
        if (PT.isOneOf(val, ";PNG;PNGJ;")) {
          sceneType = val;
          pt++;
        }
        break;
      default:
        tok = T.image;
        break;
      }
      if (tok == T.image) {
        T t = T.getTokenFromName(SV.sValue(args[pt]).toLowerCase());
        if (t != null) {
          type = SV.sValue(t).toUpperCase();
          isCoord = (t.tok == T.coord);
          if (isCoord)
            pt++;
        }
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
        } else if (!isCoord){
          type = "(image)";
        }
      }
      if (tokAtArray(pt, args) == T.integer) {
        width = SV.iValue(tokenAt(pt++, args));
        height = SV.iValue(tokenAt(pt++, args));
      }
      break;
    }

    if (msg == null) {
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

        if (type.equals("(image)")
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
      switch (tokAtArray(pt, args)) {
      case T.nada:
        isShow = true;
        break;
      case T.clipboard:
        break;
      case T.identifier:
      case T.string:
        fileName = SV.sValue(tokenAt(pt, args));
        if (pt == argCount - 3 && tokAtArray(pt + 1, args) == T.per) {
          // write filename.xxx gets separated as filename .spt
          // write isosurface filename.xxx also
          fileName += "." + SV.sValue(tokenAt(pt + 2, args));
        }
        if (type != "VAR" && pt == pt0 && !isCoord)
          type = "IMAGE";
        else if (fileName.length() > 0 && fileName.charAt(0) == '.'
            && (pt == pt0 + 1 || pt == pt0 + 2)) {
          fileName = SV.sValue(tokenAt(pt - 1, args)) + fileName;
          if (type != "VAR" && pt == pt0 + 1)
            type = "IMAGE";
        }
        if (fileName.equalsIgnoreCase("clipboard")
            || !vwr.haveAccess(ACCESS.ALL))
          fileName = null;
        break;
      default:
        invArg();
      }
      if (type.equals("IMAGE") || type.equals("(image)")
          || type.equals("FRAME") || type.equals("VIBRATION")) {
        type = (fileName != null && fileName.indexOf(".") >= 0 ? fileName
            .substring(fileName.lastIndexOf(".") + 1).toUpperCase() : "JPG");
      }
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
      isImage = PT.isOneOf(type.toLowerCase(), JC.IMAGE_OR_SCENE);
      if (scripts != null) {
        if (type.equals("PNG"))
          type = "PNGJ";
        if (!type.equals("PNGJ") && !type.equals("ZIPALL"))
          invArg();
      }
      if (!isImage
          && !isExport
          && !PT
              .isOneOf(
                  type,
                  ";SCENE;JMOL;ZIP;ZIPALL;SPT;HISTORY;MO;ISOSURFACE;MESH;PMESH;VAR;FILE;FUNCTION;CML;JSON;XYZ;XYZRN;XYZVIB;MENU;MOL;PDB;PGRP;PQR;QUAT;RAMA;SDF;V2000;V3000;INLINE;"))
        e.errorStr2(
            ScriptError.ERROR_writeWhat,
            "COORDS|FILE|FUNCTIONS|HISTORY|IMAGE|INLINE|ISOSURFACE|JMOL|MENU|MO|POINTGROUP|QUATERNION [w,x,y,z] [derivative]"
                + "|RAMACHANDRAN|SPT|STATE|VAR x|ZIP|ZIPALL  CLIPBOARD",
            "CML|GIF|JPG|JPG64|JMOL|JVXL|MESH|MOL|PDB|PMESH|PNG|PNGJ|PNGT|PPM|PQR|SDF|CD|JSON|V2000|V3000|SPT|XJVXL|XYZ|XYZRN|XYZVIB|ZIP"
                + driverList.toUpperCase().replace(';', '|'));
      if (chk)
        return "";
      Object bytes = null;
      boolean doDefer = false;
      if (data == null || isExport) {
        data = type.intern();
        if (isExport) {
          if (timeMsg)
            Logger.startTimer("export");
          Map<String, Object> eparams = new Hashtable<String, Object>();
          eparams.put("type", data);
          if (fileName != null)
            eparams.put("fileName", fileName);
          if (isCommand || fileName != null)
            eparams.put("fullPath", fullPath);
          eparams.put("width", Integer.valueOf(width));
          eparams.put("height", Integer.valueOf(height));
          data = vwr.generateOutputForExport(eparams);
          if (data == null || data.length() == 0)
            return "";
          if (!isCommand)
            return data;
          if ((type.equals("Povray") || type.equals("Idtf"))
              && fullPath[0] != null) {
            String ext = (type.equals("Idtf") ? ".tex" : ".ini");
            fileName = fullPath[0] + ext;
            params = new Hashtable<String, Object>();
            params.put("fileName", fileName);
            params.put("type", ext);
            params.put("text", data);
            params.put("fullPath", fullPath);
            msg = vwr.processWriteOrCapture(params);
            if (type.equals("Idtf"))
              data = data.substring(0, data.indexOf("\\begin{comment}"));
            data = "Created " + fullPath[0] + ":\n\n" + data;
            if (timeMsg)
              showString(Logger.getTimerMsg("export", 0));
          } else {
            msg = data;
          }
          if (msg != null) {
            if (!msg.startsWith("OK"))
              e.evalError(msg, null);
            e.report(data);
          }
          return "";
        } else if (data == "MENU") {
          data = vwr.getMenu("");
        } else if (data == "PGRP") {
          data = vwr.getPointGroupAsString(type2.equals("draw"), null, 0, 1.0f);
        } else if (data == "PDB" || data == "PQR") {
          if (isShow) {
            data = vwr.getPdbAtomData(null, null);
          } else {
            doDefer = true;
            /*
             * OutputStream os = vwr.getOutputStream(fileName, fullPath); msg =
             * vwr.getPdbData(null, new BufferedOutputStream(os)); if (msg !=
             * null) msg = "OK " + msg + " " + fullPath[0]; try { os.close(); }
             * catch (IOException e) { // TODO }
             */
          }
        } else if (data == "FILE") {
          if (isShow)
            data = vwr.getCurrentFileAsString("script");
          else
            doDefer = true;
          if ("?".equals(fileName))
            fileName = "?Jmol." + vwr.getP("_fileType");
        } else if ((data == "SDF" || data == "MOL" || data == "V2000"
            || data == "V3000" || data == "CD" || data == "JSON")
            && isCoord) {
          data = vwr.getModelExtract("selected", true, false, data);
          if (data.startsWith("ERROR:"))
            bytes = data;
        } else if (data == "XYZ" || data == "XYZRN" || data == "XYZVIB"
            || data == "MOL" || data == "SDF" || data == "V2000"
            || data == "V3000" || data == "CML" || data == "CD"
            || data == "JSON") {
          data = vwr.getData("selected", data);
          if (data.startsWith("ERROR:"))
            bytes = data;
        } else if (data == "FUNCTION") {
          data = vwr.getFunctionCalls(null);
          type = "TXT";
        } else if (data == "VAR") {
          if (tVar == null) {
            tVar = (SV) e.getParameter(
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
                    v.add(1, bytes);
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
                && (bytes = data = vwr.createZip(fileName, v.size() == 1 ? "BINARY" : "ZIPDATA", v)) == null)
              e.evalError("#CANCELED#", null);
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
            e.evalError("#CANCELED#", null);
        } else if (data == "HISTORY") {
          data = vwr.getSetHistory(Integer.MAX_VALUE);
          type = "SPT";
        } else if (data == "MO") {
          data = getMoJvxl(Integer.MAX_VALUE);
          type = "XJVXL";
        } else if (data == "PMESH") {
          if ((data = getIsosurfaceJvxl(true, JC.SHAPE_PMESH)) == null)
            error(ScriptError.ERROR_noData);
          type = "XJVXL";
        } else if (data == "ISOSURFACE" || data == "MESH") {
          if ((data = getIsosurfaceJvxl(data == "MESH", JC.SHAPE_ISOSURFACE)) == null)
            error(ScriptError.ERROR_noData);
          type = (data.indexOf("<?xml") >= 0 ? "XJVXL" : "JVXL");
          if (!isShow)
            showString((String) getShapeProperty(JC.SHAPE_ISOSURFACE,
                "jvxlFileInfo"));
        } else {
          // image
          len = -1;
          if (quality < 0)
            quality = -1;
        }
        if (data == null && !doDefer)
          data = "";
        if (len == 0 && !doDefer)
          len = (bytes == null ? data.length()
              : bytes instanceof String ? ((String) bytes).length()
                  : ((byte[]) bytes).length);
        if (isImage) {
          e.refresh(false);
          if (width < 0)
            width = vwr.getScreenWidth();
          if (height < 0)
            height = vwr.getScreenHeight();
        }
      }
      if (!isCommand)
        return data;
      if (isShow) {
        e.showStringPrint(data, true);
        return "";
      }
      if (bytes != null && bytes instanceof String) {
        // load error or completion message here
        /**
         * @j2sNative
         * 
         *            if (bytes.indexOf("OK") != 0)alert(bytes);
         * 
         */
        {
        }
        e.report((String) bytes);
        return (String) bytes;
      }
      if (type.equals("SCENE"))
        bytes = sceneType;
      else if (bytes == null && (!isImage || fileName != null))
        bytes = data;
      if (timeMsg)
        Logger.startTimer("write");
      if (doDefer) {
        msg = vwr.writeFileData(fileName, type, 0, null);
      } else {
        params = new Hashtable<String, Object>();
        if (fileName != null)
          params.put("fileName", fileName);
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
        //? (byte[]) bytes : null), scripts,  quality, width, height, bsFrames, nVibes, fullPath);
      }
      if (timeMsg)
        showString(Logger.getTimerMsg("write", 0));
    }
    if (!chk && msg != null) {
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
      e.report(msg + (isImage ? "; width=" + width + "; height=" + height : ""));
      return msg;
    }
    return "";
  }

  private void show() throws ScriptException {
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
        msg = ((SV) e.theToken).escape();
      break;
    case T.domains:
      e.checkLength23();
      len = st.length;
      if (!chk) {
        Object d = vwr.ms.getInfo(vwr.am.cmi, "domains");
        if (d instanceof SV)
          msg = vwr.getAnnotationInfo((SV) d, e.optParameterAsString(2),
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
      e.checkLength23();
      len = st.length;
      if (!chk) {
        Object d = vwr.ms.getInfo(vwr.am.cmi, "validation");
        if (d instanceof SV)
          msg = vwr.getAnnotationInfo((SV) d, e.optParameterAsString(2),
              T.validation);
        else
          msg = "validation information has not been loaded";
      }
      break;
    case T.cache:
      if (!chk)
        msg = Escape.e(vwr.cacheList());
      break;
    case T.dssr:
      e.checkLength23();
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
        msg = vwr.getPathForAllFiles();
      break;
    case T.nmr:
      if (e.optParameterAsString(2).equalsIgnoreCase("1H")) {
        len = 3;
        if (!chk)
          msg = vwr.getNMRPredict(false);
        break;
      }
      if (!chk)
        vwr.getNMRPredict(true);
      return;
    case T.smiles:
    case T.drawing:
    case T.chemical:
      checkLength(tok == T.chemical ? 3 : 2);
      if (chk)
        return;
      try {
        msg = vwr.getSmiles(null);
      } catch (Exception ex) {
        msg = ex.getMessage();
      }
      switch (tok) {
      case T.drawing:
        if (msg.length() > 0) {
          vwr.show2D(msg);
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
      String type;
      int iop = 0;
      P3 pt1 = null,
      pt2 = null;
      if (slen > 3 && tokAt(3) != T.string) {
        pt1 = centerParameter(2);
        pt2 = centerParameter(++e.iToken);
      } else {
        // show symop 3 "fmatrix"
        iop = (tokAt(2) == T.integer ? intParameter(2) : 0);
      }
      type = (tokAt(e.iToken + 1) == T.string ? stringParameter(++e.iToken)
          : null);
      checkLength(len = ++e.iToken);
      if (!chk)
        msg = vwr.getSymmetryOperation(iop, pt1, pt2, type);
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
      e.checkLength23();
      if (!chk)
        showString(vwr.getFunctionCalls(e.optParameterAsString(2)));
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
          vwr.showUrl(e.getFullPathName());
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
        e.errorStr(ScriptError.ERROR_multipleModelsDisplayedNotOK, "show "
            + e.theToken.value);
      msg = plot(st);
      len = slen;
      break;
    case T.context:
    case T.trace:
      if (!chk)
        msg = getContext(false);
      break;
    case T.colorscheme:
      name = e.optParameterAsString(2);
      if (name.length() > 0)
        len = 3;
      if (!chk)
        value = vwr.getColorSchemeList(name);
      break;
    case T.variables:
      if (!chk)
        msg = vwr.getAtomDefs(e.definedAtomSets) + vwr.g.getVariableList()
            + getContext(true);
      break;
    case T.trajectory:
      if (!chk)
        msg = vwr.getTrajectoryState();
      break;
    case T.historylevel:
      value = "" + e.commandHistoryLevelMax;
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
      case UNITCELL:
        msg = "set axesUnitcell";
        break;
      case BOUNDBOX:
        msg = "set axesWindow";
        break;
      default:
        msg = "set axesMolecular";
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
          + (vwr.getSelectionHaloEnabled(false) ? "ON" : "OFF");
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
        msg = vwr.getSpecularState();
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
      if (!chk && e.outputBuffer == null)
        vwr.sm.clearConsole();
      if ((len = slen) == 2) {
        if (!chk)
          msg = vwr.getStateInfo();
        break;
      }
      name = paramAsStr(2);
      if (name.equals("/") && (len = slen) == 4) {
        name = paramAsStr(3).toLowerCase();
        if (!chk) {
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
          msg = vwr.getEmbeddedFileState(paramAsStr(3), true);
        break;
      }
      len = 3;
      if (!chk)
        msg = vwr.stm.getSavedState(name);
      break;
    case T.structure:
      if ((len = slen) == 2) {
        if (!chk)
          msg = vwr.getProteinStructureState();
        break;
      }
      String shape = paramAsStr(2);
      if (!chk)
        msg = vwr.stm.getSavedStructure(shape);
      break;
    case T.data:
      type = ((len = slen) == 3 ? paramAsStr(2) : null);
      if (!chk) {
        Object[] data = (type == null ? this.lastData : vwr.getData(type));
        msg = (data == null ? "no data" : Escape.encapsulateData(
            (String) data[0], data[1], ((Integer) data[3]).intValue()));
      }
      break;
    case T.spacegroup:
      Map<String, Object> info = null;
      if ((len = slen) == 2) {
        if (!chk) {
          info = vwr.getSpaceGroupInfo(null);
        }
      } else {
        String sg = paramAsStr(2);
        if (!chk)
          info = vwr.getSpaceGroupInfo(PT.rep(sg, "''", "\""));
      }
      if (info != null)
        msg = "" + info.get("spaceGroupInfo") + info.get("symmetryInfo");
      break;
    case T.dollarsign:
      len = 3;
      msg = e.setObjectProperty();
      break;
    case T.boundbox:
      if (!chk) {
        msg = vwr.ms.getBoundBoxCommand(true);
      }
      break;
    case T.center:
      if (!chk)
        msg = "center " + Escape.eP(vwr.tm.getRotationCenter());
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
        if (e.scriptLevel == 0)
          vwr.removeCommand();
        msg = vwr.getSetHistory(n);
      }
      break;
    case T.isosurface:
      if (!chk)
        msg = (String) getShapeProperty(JC.SHAPE_ISOSURFACE, "jvxlDataXml");
      break;
    case T.mo:
      if (e.optParameterAsString(2).equalsIgnoreCase("list")) {
        msg = vwr.getMoInfo(-1);
        len = 3;
      } else {
        int ptMO = ((len = slen) == 2 ? Integer.MIN_VALUE : intParameter(2));
        if (!chk)
          msg = getMoJvxl(ptMO);
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
        name = e.optParameterAsString(2);
        msg = vwr.getOrientationText(T.name, name);
      }
      len = slen;
      break;
    case T.pdbheader:
      if (!chk)
        msg = vwr.getPDBHeader();
      break;
    case T.pointgroup:
      if (!chk)
        showString(vwr.getPointGroupAsString(false, null, 0, 0));
      return;
    case T.symmetry:
      if (!chk)
        msg = vwr.ms.getSymmetryInfoAsString();
      break;
    case T.transform:
      if (!chk)
        msg = "transform:\n" + vwr.tm.getTransformText();
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
          msg = vwr.getPDBHeader();
      }
      break;
    case T.json:
    case T.var:
      str = paramAsStr(len++);
      SV v = (SV) e.getParameter(str, T.variable, true);
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
            + ((SV) e.getParameter(str, T.variable, true)).escape());
    }
  }

  private void stereo() throws ScriptException {
    STER stereoMode = STER.DOUBLE;
    // see www.usm.maine.edu/~rhodes/0Help/StereoViewing.html
    // stereo on/off
    // stereo color1 color2 6
    // stereo redgreen 5

    float degrees = STER.DEFAULT_STEREO_DEGREES;
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

  
  private void addShapeProperty(Lst<Object[]> propertyList, String key,
                                Object value) {
    if (chk)
      return;
    propertyList.addLast(new Object[] { key, value });
  }

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
    int ac = vwr.ms.getAtomCount();
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
      modelIndex = vwr.getAtomModelIndex(vwr.ms.bo[bondIndex]
          .getAtomIndex1());
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

  /**
   * 
   * @param bsSelected
   * @param bsIgnore
   * @param fileName
   * @return calculated atom potentials
   * @throws Exception
   */
  private float[] getAtomicPotentials(BS bsSelected, BS bsIgnore,
                                      String fileName) throws Exception {
    float[] potentials = new float[vwr.getAtomCount()];
    MepCalculationInterface m = (MepCalculationInterface) Interface
        .getOption("quantum.MlpCalculation", vwr, "script");
    m.set(vwr);
    String data = (fileName == null ? null : vwr.getFileAsString3(fileName,
        false, null));
    m.assignPotentials(vwr.ms.at, potentials, vwr
        .getSmartsMatch("a", bsSelected), vwr.getSmartsMatch(
        "/noAromatic/[$(C=O),$(O=C),$(NC=O)]", bsSelected), bsIgnore, data);
    return potentials;
  }

  /**
   * Checks color, translucent, opaque parameters.
   * @param eval 
   * @param i
   * @param allowNone
   * @param ret returned int argb color
   * @return translucentLevel and sets iToken and ret[0]
   * 
   * @throws ScriptException
   */
  private float getColorTrans(ScriptEval eval, int i, boolean allowNone, int ret[]) throws ScriptException {
    float translucentLevel = Float.MAX_VALUE;
    if (eval.theTok != T.color)
      --i;
    switch (tokAt(i + 1)) {
    case T.translucent:
      i++;
      translucentLevel = (isFloatParameter(i + 1) ? eval.getTranslucentLevel(++i)
          : vwr.getFloat(T.defaulttranslucent));
      break;
    case T.opaque:
      i++;
      translucentLevel = 0;
      break;
    }
    if (eval.isColorParam(i + 1)) {
      ret[0] = eval.getArgbParam(++i);
    } else if (tokAt(i + 1) == T.none) {
      ret[0] = 0;
      eval.iToken = i + 1;
    } else if (translucentLevel == Float.MAX_VALUE) {
      invArg();
    } else {
      ret[0] = Integer.MIN_VALUE;
    }
    i = eval.iToken;
    return translucentLevel;
  }

  private Object getCapSlabObject(int i, boolean isLcaoCartoon)
      throws ScriptException {
    if (i < 0) {
      // standard range -100 to 0
      return TempArray.getSlabWithinRange(i, 0);
    }
    ScriptEval eval = e;
    Object data = null;
    int tok0 = tokAt(i);
    boolean isSlab = (tok0 == T.slab);
    int tok = tokAt(i + 1);
    P4 plane = null;
    P3[] pts = null;
    float d, d2;
    BS bs = null;
    Short slabColix = null;
    Integer slabMeshType = null;
    if (tok == T.translucent) {
      float slabTranslucency = (isFloatParameter(++i + 1) ? floatParameter(++i)
          : 0.5f);
      if (eval.isColorParam(i + 1)) {
        slabColix = Short.valueOf(C.getColixTranslucent3(
            C.getColix(eval.getArgbParam(i + 1)), slabTranslucency != 0,
            slabTranslucency));
        i = eval.iToken;
      } else {
        slabColix = Short.valueOf(C.getColixTranslucent3(C.INHERIT_COLOR,
            slabTranslucency != 0, slabTranslucency));
      }
      switch (tok = tokAt(i + 1)) {
      case T.mesh:
      case T.fill:
        slabMeshType = Integer.valueOf(tok);
        tok = tokAt(++i + 1);
        break;
      default:
        slabMeshType = Integer.valueOf(T.fill);
        break;
      }
    }
    //TODO: check for compatibility with LCAOCARTOONS
    switch (tok) {
    case T.off:
      eval.iToken = i + 1;
      return Integer.valueOf(Integer.MIN_VALUE);
    case T.none:
      eval.iToken = i + 1;
      break;
    case T.dollarsign:
      // do we need distance here? "-" here?
      i++;
      data = new Object[] { Float.valueOf(1), paramAsStr(++i) };
      tok = T.mesh;
      break;
    case T.within:
      // isosurface SLAB WITHIN RANGE f1 f2
      i++;
      if (tokAt(++i) == T.range) {
        d = floatParameter(++i);
        d2 = floatParameter(++i);
        data = new Object[] { Float.valueOf(d), Float.valueOf(d2) };
        tok = T.range;
      } else if (isFloatParameter(i)) {
        // isosurface SLAB WITHIN distance {atomExpression}|[point array]
        d = floatParameter(i);
        if (eval.isCenterParameter(++i)) {
          P3 pt = centerParameter(i);
          if (chk || !(eval.expressionResult instanceof BS)) {
            pts = new P3[] { pt };
          } else {
            Atom[] atoms = vwr.ms.at;
            bs = (BS) eval.expressionResult;
            pts = new P3[bs.cardinality()];
            for (int k = 0, j = bs.nextSetBit(0); j >= 0; j = bs
                .nextSetBit(j + 1), k++)
              pts[k] = atoms[j];
          }
        } else {
          pts = eval.getPointArray(i, -1);
        }
        if (pts.length == 0) {
          eval.iToken = i;
          invArg();
        }
        data = new Object[] { Float.valueOf(d), pts, bs };
      } else {
        data = eval.getPointArray(i, 4);
        tok = T.boundbox;
      }
      break;
    case T.boundbox:
      eval.iToken = i + 1;
      data = BoxInfo.getCriticalPoints(vwr.ms.getBBoxVertices(), null);
      break;
    //case Token.slicebox:
    // data = BoxInfo.getCriticalPoints(((JmolViewer)(vwr)).slicer.getSliceVert(), null);
    //eval.iToken = i + 1;
    //break;  
    case T.brillouin:
    case T.unitcell:
      eval.iToken = i + 1;
      SymmetryInterface unitCell = vwr.getCurrentUnitCell();
      if (unitCell == null) {
        if (tok == T.unitcell)
          invArg();
      } else {
        pts = BoxInfo.getCriticalPoints(unitCell.getUnitCellVertices(),
            unitCell.getCartesianOffset());
        int iType = (int) unitCell
            .getUnitCellInfoType(SimpleUnitCell.INFO_DIMENSIONS);
        V3 v1 = null;
        V3 v2 = null;
        switch (iType) {
        case 3:
          break;
        case 1: // polymer
          v2 = V3.newVsub(pts[2], pts[0]);
          v2.scale(1000f);
          //$FALL-THROUGH$
        case 2: // slab
          // "a b c" is really "z y x"
          v1 = V3.newVsub(pts[1], pts[0]);
          v1.scale(1000f);
          pts[0].sub(v1);
          pts[1].scale(2000f);
          if (iType == 1) {
            pts[0].sub(v2);
            pts[2].scale(2000f);
          }
          break;
        }
        data = pts;
      }
      break;
    case T.bitset:
    case T.expressionBegin:
      data = atomExpressionAt(i + 1);
      tok = T.decimal;
      if (!eval.isCenterParameter(++eval.iToken))
        break;
      data = null;
      //$FALL-THROUGH$
    default:
      // isosurface SLAB n
      // isosurface SLAB -100. 0.  as "within range" 
      if (!isLcaoCartoon && isSlab && isFloatParameter(i + 1)) {
        d = floatParameter(++i);
        if (!isFloatParameter(i + 1))
          return Integer.valueOf((int) d);
        d2 = floatParameter(++i);
        data = new Object[] { Float.valueOf(d), Float.valueOf(d2) };
        tok = T.range;
        break;
      }
      // isosurface SLAB [plane]
      plane = eval.planeParameter(++i);
      float off = (isFloatParameter(eval.iToken + 1) ? floatParameter(++eval.iToken)
          : Float.NaN);
      if (!Float.isNaN(off))
        plane.w -= off;
      data = plane;
      tok = T.plane;
    }
    Object colorData = (slabMeshType == null ? null : new Object[] {
        slabMeshType, slabColix });
    return TempArray.getSlabObjectType(tok, data, !isSlab, colorData);
  }

  private String getIsosurfaceJvxl(boolean asMesh, int iShape) {
    if (chk)
      return "";
    return (String) getShapeProperty(iShape, asMesh ? "jvxlMeshX"
        : "jvxlDataXml");
  }

  @SuppressWarnings("unchecked")
  private String getMoJvxl(int ptMO) throws ScriptException {
    // 0: all; Integer.MAX_VALUE: current;
    sm.loadShape(JC.SHAPE_MO);
    int modelIndex = vwr.am.cmi;
    if (modelIndex < 0)
      e.errorStr(ScriptError.ERROR_multipleModelsDisplayedNotOK,
          "MO isosurfaces");
    Map<String, Object> moData = (Map<String, Object>) vwr
        .getModelAuxiliaryInfoValue(modelIndex, "moData");
    if (moData == null)
      error(ScriptError.ERROR_moModelError);
    Integer n = (Integer) getShapeProperty(JC.SHAPE_MO, "moNumber");
    if (n == null || n.intValue() == 0) {
      setShapeProperty(JC.SHAPE_MO, "init", Integer.valueOf(modelIndex));
      //} else if (ptMO == Integer.MAX_VALUE) {
    }
    setShapeProperty(JC.SHAPE_MO, "moData", moData);
    return (String) getShapePropertyIndex(JC.SHAPE_MO, "showMO", ptMO);
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
    return sm.getShapePropertyIndex(shapeType, propertyName, index);
  }

  private T tokenAt(int i, T[] args) {
    return (i < args.length ? args[i] : null);
  }

  private static int tokAtArray(int i, T[] args) {
    return (i < args.length && args[i] != null ? args[i].tok : T.nada);
  }

  private void finalizeObject(int shapeID, int colorArgb,
                              float translucentLevel, int intScale,
                              boolean doSet, Object data,
                              int iptDisplayProperty, BS bs)
       throws ScriptException {
     if (doSet) {
       setShapeProperty(shapeID, "set", data);
     }
     if (colorArgb != Integer.MIN_VALUE)
       e.setShapePropertyBs(shapeID, "color", Integer.valueOf(colorArgb), bs);
     if (translucentLevel != Float.MAX_VALUE)
       e.setShapeTranslucency(shapeID, "", "translucent", translucentLevel, bs);
     if (intScale != 0) {
       setShapeProperty(shapeID, "scale", Integer.valueOf(intScale));
     }
     if (iptDisplayProperty > 0) {
       if (!e.setMeshDisplayProperty(shapeID, iptDisplayProperty, 0))
         invArg();
     }
   }

  private float[] moCombo(Lst<Object[]> propertyList) {
    if (tokAt(e.iToken + 1) != T.squared)
      return null;
    addShapeProperty(propertyList, "squareLinear", Boolean.TRUE);
    e.iToken++;
    return new float[0];
  }

  private int moOffset(int index) throws ScriptException {
    boolean isHomo = (getToken(index).tok == T.homo);
    int offset = (isHomo ? 0 : 1);
    int tok = tokAt(++index);
    if (tok == T.integer && intParameter(index) < 0)
      offset += intParameter(index);
    else if (tok == T.plus)
      offset += intParameter(++index);
    else if (tok == T.minus)
      offset -= intParameter(++index);
    return offset;
  }

  @SuppressWarnings("unchecked")
  private void setMoData(Lst<Object[]> propertyList, int moNumber, float[] lc,
                         int offset, boolean isNegOffset, int modelIndex,
                         String title) throws ScriptException {
    ScriptEval eval = e;
    if (chk)
      return;
    if (modelIndex < 0) {
      modelIndex = vwr.am.cmi;
      if (modelIndex < 0)
        eval.errorStr(ScriptError.ERROR_multipleModelsDisplayedNotOK,
            "MO isosurfaces");
    }
    Map<String, Object> moData = (Map<String, Object>) vwr
        .getModelAuxiliaryInfoValue(modelIndex, "moData");
    Lst<Map<String, Object>> mos = null;
    Map<String, Object> mo;
    Float f;
    int nOrb = 0;
    if (lc == null || lc.length < 2) {
      if (lc != null && lc.length == 1)
        offset = 0;
      if (moData == null)
        error(ScriptError.ERROR_moModelError);
      int lastMoNumber = (moData.containsKey("lastMoNumber") ? ((Integer) moData
          .get("lastMoNumber")).intValue() : 0);
      int lastMoCount = (moData.containsKey("lastMoCount") ? ((Integer) moData
          .get("lastMoCount")).intValue() : 1);
      if (moNumber == T.prev)
        moNumber = lastMoNumber - 1;
      else if (moNumber == T.next)
        moNumber = lastMoNumber + lastMoCount;
      mos = (Lst<Map<String, Object>>) (moData.get("mos"));
      nOrb = (mos == null ? 0 : mos.size());
      if (nOrb == 0)
        error(ScriptError.ERROR_moCoefficients);
      if (nOrb == 1 && moNumber > 1)
        error(ScriptError.ERROR_moOnlyOne);
      if (offset != Integer.MAX_VALUE) {
        // 0: HOMO;
        if (moData.containsKey("HOMO")) {
          moNumber = ((Integer) moData.get("HOMO")).intValue() + offset;
        } else {
          moNumber = -1;
          for (int i = 0; i < nOrb; i++) {
            mo = mos.get(i);
            if ((f = (Float) mo.get("occupancy")) != null) {
              if (f.floatValue() < 0.5f) {
                // go for LUMO = first unoccupied
                moNumber = i;
                break;
              }
              continue;
            } else if ((f = (Float) mo.get("energy")) != null) {
              if (f.floatValue() > 0) {
                // go for LUMO = first positive
                moNumber = i;
                break;
              }
              continue;
            }
            break;
          }
          if (moNumber < 0)
            error(ScriptError.ERROR_moOccupancy);
          moNumber += offset;
        }
        Logger.info("MO " + moNumber);
      }
      if (moNumber < 1 || moNumber > nOrb)
        eval.errorStr(ScriptError.ERROR_moIndex, "" + nOrb);
    }
    moNumber = Math.abs(moNumber);
    moData.put("lastMoNumber", Integer.valueOf(moNumber));
    moData.put("lastMoCount", Integer.valueOf(1));
    if (isNegOffset && lc == null)
      lc = new float[] { -100, moNumber };
    if (lc != null && lc.length < 2) {
      mo = mos.get(moNumber - 1);
      if ((f = (Float) mo.get("energy")) == null) {
        lc = new float[] { 100, moNumber };
      } else {

        // constuct set of equivalent energies and square this

        float energy = f.floatValue();
        BS bs = BS.newN(nOrb);
        int n = 0;
        boolean isAllElectrons = (lc.length == 1 && lc[0] == 1);
        for (int i = 0; i < nOrb; i++) {
          if ((f = (Float) mos.get(i).get("energy")) == null)
            continue;
          float e = f.floatValue();
          if (isAllElectrons ? e <= energy : e == energy) {
            bs.set(i + 1);
            n += 2;
          }
        }
        lc = new float[n];
        for (int i = 0, pt = 0; i < n; i += 2) {
          lc[i] = 1;
          lc[i + 1] = (pt = bs.nextSetBit(pt + 1));
        }
        moData.put("lastMoNumber", Integer.valueOf(bs.nextSetBit(0)));
        moData.put("lastMoCount", Integer.valueOf(n / 2));
      }
      addShapeProperty(propertyList, "squareLinear", Boolean.TRUE);
    }
    addShapeProperty(propertyList, "moData", moData);
    if (title != null)
      addShapeProperty(propertyList, "title", title);
    addShapeProperty(propertyList, "molecularOrbital", lc != null ? lc
        : Integer.valueOf(Math.abs(moNumber)));
    addShapeProperty(propertyList, "clear", null);
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

  private String initIsosurface(int iShape) throws ScriptException {

    // handle isosurface/mo/pmesh delete and id delete here

    ScriptEval eval = e;
    setShapeProperty(iShape, "init", fullCommand);
    eval.iToken = 0;
    int tok1 = tokAt(1);
    int tok2 = tokAt(2);
    if (tok1 == T.delete || tok2 == T.delete && tokAt(++eval.iToken) == T.all) {
      setShapeProperty(iShape, "delete", null);
      eval.iToken += 2;
      if (slen > eval.iToken) {
        setShapeProperty(iShape, "init", fullCommand);
        setShapeProperty(iShape, "thisID", MeshCollection.PREVIOUS_MESH_ID);
      }
      return null;
    }
    eval.iToken = 1;
    if (!eval.setMeshDisplayProperty(iShape, 0, tok1)) {
      setShapeProperty(iShape, "thisID", MeshCollection.PREVIOUS_MESH_ID);
      if (iShape != JC.SHAPE_DRAW)
        setShapeProperty(iShape, "title", new String[] { thisCommand });
      if (tok1 != T.id
          && (tok2 == T.times || tok1 == T.times
              && eval.setMeshDisplayProperty(iShape, 0, tok2))) {
        String id = setShapeId(iShape, 1, false);
        eval.iToken++;
        return id;
      }
    }
    return null;
  }

  private void getWithinDistanceVector(Lst<Object[]> propertyList,
                                       float distance, P3 ptc, BS bs,
                                       boolean isShow) {
    Lst<P3> v = new Lst<P3>();
    P3[] pts = new P3[2];
    if (bs == null) {
      P3 pt1 = P3.new3(distance, distance, distance);
      P3 pt0 = P3.newP(ptc);
      pt0.sub(pt1);
      pt1.add(ptc);
      pts[0] = pt0;
      pts[1] = pt1;
      v.addLast(ptc);
    } else {
      BoxInfo bbox = vwr.ms.getBoxInfo(bs, -Math.abs(distance));
      pts[0] = bbox.getBoundBoxVertices()[0];
      pts[1] = bbox.getBoundBoxVertices()[7];
      if (bs.cardinality() == 1)
        v.addLast(vwr.getAtomPoint3f(bs.nextSetBit(0)));
    }
    if (v.size() == 1 && !isShow) {
      addShapeProperty(propertyList, "withinDistance", Float.valueOf(distance));
      addShapeProperty(propertyList, "withinPoint", v.get(0));
    }
    addShapeProperty(propertyList, (isShow ? "displayWithin" : "withinPoints"),
        new Object[] { Float.valueOf(distance), pts, bs, v });
  }

  private String setColorOptions(SB sb, int index, int iShape, int nAllowed)
      throws ScriptException {
    ScriptEval eval = e;
    getToken(index);
    String translucency = "opaque";
    if (eval.theTok == T.translucent) {
      translucency = "translucent";
      if (nAllowed < 0) {
        float value = (isFloatParameter(index + 1) ? floatParameter(++index)
            : Float.MAX_VALUE);
        eval.setShapeTranslucency(iShape, null, "translucent", value, null);
        if (sb != null) {
          sb.append(" translucent");
          if (value != Float.MAX_VALUE)
            sb.append(" ").appendF(value);
        }
      } else {
        eval.setMeshDisplayProperty(iShape, index, eval.theTok);
      }
    } else if (eval.theTok == T.opaque) {
      if (nAllowed >= 0)
        eval.setMeshDisplayProperty(iShape, index, eval.theTok);
    } else {
      eval.iToken--;
    }
    nAllowed = Math.abs(nAllowed);
    for (int i = 0; i < nAllowed; i++) {
      if (eval.isColorParam(eval.iToken + 1)) {
        int color = eval.getArgbParam(++eval.iToken);
        setShapeProperty(iShape, "colorRGB", Integer.valueOf(color));
        if (sb != null)
          sb.append(" ").append(Escape.escapeColor(color));
      } else if (eval.iToken < index) {
        invArg();
      } else {
        break;
      }
    }
    return translucency;
  }

  /**
   * for the ISOSURFACE command
   * 
   * @param fname
   * @param xyz
   * @param ret
   * @return [ ScriptFunction, Params ]
   */
  private Object[] createFunction(String fname, String xyz, String ret) {
    ScriptEval e = (new ScriptEval()).setViewer(vwr);
    try {
      e.compileScript(null, "function " + fname + "(" + xyz + ") { return "
          + ret + "}", false);
      Lst<SV> params = new Lst<SV>();
      for (int i = 0; i < xyz.length(); i += 2)
        params.addLast(SV.newV(T.decimal, Float.valueOf(0f)).setName(
            xyz.substring(i, i + 1)));
      return new Object[] { e.aatoken[0][1].value, params };
    } catch (Exception ex) {
      return null;
    }
  }

  private float[][] floatArraySet(int i, int nX, int nY) throws ScriptException {
    int tok = tokAt(i++);
    if (tok == T.spacebeforesquare)
      tok = tokAt(i++);
    if (tok != T.leftsquare)
      invArg();
    float[][] fparams = AU.newFloat2(nX);
    int n = 0;
    while (tok != T.rightsquare) {
      tok = getToken(i).tok;
      switch (tok) {
      case T.spacebeforesquare:
      case T.rightsquare:
        continue;
      case T.comma:
        i++;
        break;
      case T.leftsquare:
        i++;
        float[] f = new float[nY];
        fparams[n++] = f;
        for (int j = 0; j < nY; j++) {
          f[j] = floatParameter(i++);
          if (tokAt(i) == T.comma)
            i++;
        }
        if (tokAt(i++) != T.rightsquare)
          invArg();
        tok = T.nada;
        if (n == nX && tokAt(i) != T.rightsquare)
          invArg();
        break;
      default:
        invArg();
      }
    }
    return fparams;
  }

  private float[][][] floatArraySetXYZ(int i, int nX, int nY, int nZ)
      throws ScriptException {
    ScriptEval eval = e;
    int tok = tokAt(i++);
    if (tok == T.spacebeforesquare)
      tok = tokAt(i++);
    if (tok != T.leftsquare || nX <= 0)
      invArg();
    float[][][] fparams = AU.newFloat3(nX, -1);
    int n = 0;
    while (tok != T.rightsquare) {
      tok = getToken(i).tok;
      switch (tok) {
      case T.spacebeforesquare:
      case T.rightsquare:
        continue;
      case T.comma:
        i++;
        break;
      case T.leftsquare:
        fparams[n++] = floatArraySet(i, nY, nZ);
        i = ++eval.iToken;
        tok = T.nada;
        if (n == nX && tokAt(i) != T.rightsquare)
          invArg();
        break;
      default:
        invArg();
      }
    }
    return fparams;
  }

  @Override
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
        .getAssociatedAtoms());
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
        Bond bond = modelSet.getBondAt(j);
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

  private boolean listIsosurface(int iShape) throws ScriptException {
    String s = (slen > 3 ? "0" : tokAt(2) == T.nada ? "" : " "
        + getToken(2).value);
    if (!chk)
      showString((String) getShapeProperty(iShape, "list" + s));
    return true;
  }

  private String setShapeId(int iShape, int i, boolean idSeen)
      throws ScriptException {
      if (idSeen)
        invArg();
      String name = e.setShapeNameParameter(i).toLowerCase();
      setShapeProperty(iShape, "thisID", name);
      return name;
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
