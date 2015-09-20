package org.jmol.scriptext;

import javajs.util.P3;

import org.jmol.java.BS;
import org.jmol.script.ScriptError;
import org.jmol.script.ScriptEval;
import org.jmol.script.ScriptException;
import org.jmol.script.T;
import org.jmol.viewer.Viewer;

public abstract class ScriptExt {

  protected Viewer vwr;
  protected ScriptEval e;
  protected boolean chk;
  protected T[] st;
  protected int slen;
  
  public ScriptExt init(Object eval) {
    e = (ScriptEval) eval;
    vwr = e.vwr;
    return this;
  }

  public abstract String dispatch(int iTok, boolean b, T[] st) throws ScriptException;
  
  protected BS atomExpressionAt(int i) throws ScriptException {
    return e.atomExpressionAt(i);
  }

  protected void checkLength(int i) throws ScriptException {
    e.checkLength(i);
  }
  
  protected void error(int err) throws ScriptException {
    e.error(err);
  }

  protected void invArg() throws ScriptException {
    e.invArg();
  }

  protected void invPO() throws ScriptException {
    error(ScriptError.ERROR_invalidParameterOrder);
  }

  protected Object getShapeProperty(int shapeType, String propertyName) {
    return e.getShapeProperty(shapeType, propertyName);
  }

  protected String paramAsStr(int i) throws ScriptException {
    return e.paramAsStr(i);
  }

  protected P3 centerParameter(int i) throws ScriptException {
    return e.centerParameter(i, null);
  }

  protected float floatParameter(int i) throws ScriptException {
    return e.floatParameter(i);
  }

  protected P3 getPoint3f(int i, boolean allowFractional) throws ScriptException {
    return e.getPoint3f(i, allowFractional);
  }

  protected int intParameter(int index) throws ScriptException {
    return e.intParameter(index);
  }

  protected boolean isFloatParameter(int index) {
    switch (e.tokAt(index)) {
    case T.integer:
    case T.decimal:
      return true;
    }
    return false;
  }

  protected void setShapeProperty(int shapeType, String propertyName,
                                Object propertyValue) {
    e.setShapeProperty(shapeType, propertyName, propertyValue);
  }

  protected void showString(String s) {
    e.showString(s);
  }

  protected String stringParameter(int index) throws ScriptException {
    return e.stringParameter(index);
  }

  protected T getToken(int i) throws ScriptException {
    return e.getToken(i);
  }

  protected int tokAt(int i) {
    return e.tokAt(i);
  }

  protected String setShapeId(int iShape, int i, boolean idSeen)
      throws ScriptException {
      if (idSeen)
        invArg();
      String name = e.setShapeNameParameter(i).toLowerCase();
      setShapeProperty(iShape, "thisID", name);
      return name;
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
  protected float getColorTrans(ScriptEval eval, int i, boolean allowNone, int ret[]) throws ScriptException {
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

  protected void finalizeObject(int shapeID, int colorArgb,
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


}
