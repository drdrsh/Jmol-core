/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-05 07:42:12 -0500 (Fri, 05 Jun 2009) $
 * $Revision: 10958 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.script;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;


import org.jmol.java.BS;
import org.jmol.modelset.BondSet;
import org.jmol.util.BSUtil;
import org.jmol.util.Escape;

import javajs.api.JSONEncodable;
import javajs.util.Lst;
import javajs.util.SB;


import javajs.util.AU;
import javajs.util.BArray;
import javajs.util.Base64;
import javajs.util.M3;
import javajs.util.M34;
import javajs.util.M4;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.PT;
import javajs.util.Quat;
import javajs.util.T3;

import org.jmol.util.Txt;
import javajs.util.V3;


/**
 * ScriptVariable class
 * 
 */
public class SV extends T implements JSONEncodable {

  public final static SV vT = newSV(on, 1, "true");
  final private static SV vF = newSV(off, 0, "false");

  public int index = Integer.MAX_VALUE;    

  private final static int FLAG_CANINCREMENT = 1;
  private final static int FLAG_MODIFIED = 2;

  private int flags = ~FLAG_CANINCREMENT & FLAG_MODIFIED;
  public String myName;

  public static SV newV(int tok, Object value) {
    SV sv = new SV();
    sv.tok = tok;
    sv.value = value;
    return sv;
  }

  public static SV newI(int i) {
    SV sv = new SV();
    sv.tok = integer;
    sv.intValue = i;
    return sv;
  }
  
  public static SV newS(String s) {
    return newV(string, s);
  }
  
  public static SV newT(T x) {
    return newSV(x.tok, x.intValue, x.value);
  }

  static SV newSV(int tok, int intValue, Object value) {
    SV sv = newV(tok, value);
    sv.intValue = intValue;
    return sv;
  }

  /**
   * 
   * Creates a NEW version of the variable.
   * Object values are not copied. (Just found no 
   * use for that.)
   * 
   * 
   * @param v
   * @return  new ScriptVariable
   */
  SV setv(SV v) {
    index = v.index;
    intValue = v.intValue;
    tok = v.tok;
    value = v.value;
// never necessary: 
//    if (asCopy) {
//      switch (tok) {
//      case hash:
//        value = new Hashtable<String, SV>(
//            (Map<String, SV>) v.value);
//        break;
//      case varray:
//        List<SV> o2 = new  List<SV>();
//        List<SV> o1 = v.getList();
//        for (int i = 0; i < o1.size(); i++)
//          o2.addLast(o1.get(i));
//        value = o2;
//        break;
//      }
//    }
    return this;
  }

  @SuppressWarnings("unchecked")
  static int sizeOf(T x) {
    switch (x == null ? nada : x.tok) {
    case bitset:
      return BSUtil.cardinalityOf(bsSelectToken(x));
    case on:
    case off:
      return -1;
    case integer:
      return -2;
    case decimal:
      return -4;
    case point3f:
      return -8;
    case point4f:
      return -16;
    case matrix3f:
      return -32;
    case matrix4f:
      return -64;
    case barray:
      return ((BArray) x.value).data.length;
    case string:
      return ((String) x.value).length();
    case varray:
      return x.intValue == Integer.MAX_VALUE ? ((SV)x).getList().size()
          : sizeOf(selectItemTok(x, Integer.MIN_VALUE));
    case hash:
      return ((Map<String, SV>) x.value).size();
    case context:
      return ((ScriptContext) x.value).getFullMap().size();
    default:
      return 0;
    }
  }

  /**
   * Must be updated if getVariable is updated!
   * 
   * @param x
   * @return if we recognize this as a variable
   * 
   */
  public static boolean isVariableType(Object x) {
    return (x instanceof SV
        || x instanceof Boolean
        || x instanceof Integer
        || x instanceof Float
        || x instanceof String
        || x instanceof T3    // stored as point3f
        || x instanceof BS
        || x instanceof P4    // stored as point4f
        || x instanceof Quat // stored as point4f
        || x instanceof M34
        || x instanceof Map<?, ?>  // stored as Map<String, ScriptVariable>
        || x instanceof Lst<?>
        || x instanceof BArray
        || x instanceof ScriptContext
    // in JavaScript, all these will be "Array" which is fine;
        || isArray(x)); // stored as list
  }

  /**
   * Must be updated if getVariable is updated!
   * 
   * @param x
   * @return if we recognize this as an primitive array type
   * 
   */
  private static boolean isArray(Object x) {
    /**
     * @j2sNative
     * 
     *            return Clazz.instanceOf(x, Array);
     */
    {
       return x instanceof SV[] 
           || x instanceof int[] 
           || x instanceof byte[] 
           || x instanceof float[]
           || x instanceof double[] 
           || x instanceof String[]
           || x instanceof P3[]
           || x instanceof int[][] 
           || x instanceof float[][] 
           || x instanceof String[][] 
           || x instanceof double[][] 
           || x instanceof Float[];
    }
  }


  /**
   * @param x
   * @return a ScriptVariable of the input type, or if x is null, then a new
   *         ScriptVariable, or, if the type is not found, a string version
   */
  @SuppressWarnings("unchecked")
  public static SV getVariable(Object x) {
    if (x == null)
      return newS("");
    if (x instanceof SV)
      return (SV) x;

    // the eight basic types are:
    // boolean, integer, decimal, string, point3f, point4f, bitset, and list
    // listf is a special temporary type for storing results
    // of .all in preparation for .bin in the case of xxx.all.bin
    // but with some work, this could be developed into a storage class

    if (x instanceof Boolean)
      return getBoolean(((Boolean) x).booleanValue());
    if (x instanceof Integer)
      return newI(((Integer) x).intValue());
    if (x instanceof Float)
      return newV(decimal, x);
    if (x instanceof String) {
      x = unescapePointOrBitsetAsVariable(x);
      if (x instanceof SV)
        return (SV) x;
      return newV(string, x);
    }
    if (x instanceof P3)
      return newV(point3f, x);
    if (x instanceof V3) // point3f is not mutable anyway
      return newV(point3f, P3.newP((V3) x));
    if (x instanceof BS)
      return newV(bitset, x);
    if (x instanceof P4)
      return newV(point4f, x);
    // note: for quaternions, we save them {q1, q2, q3, q0} 
    // While this may seem odd, it is so that for any point4 -- 
    // planes, axisangles, and quaternions -- we can use the 
    // first three coordinates to determine the relavent axis
    // the fourth then gives us offset to {0,0,0} (plane), 
    // rotation angle (axisangle), and cos(theta/2) (quaternion).
    if (x instanceof Quat)
      return newV(point4f, ((Quat) x).toPoint4f());
    if (x instanceof M34)
      return newV(x instanceof M4 ? matrix4f : matrix3f, x);
    if (x instanceof Map)
      return getVariableMap((Map<String, ?>)x);
    if (x instanceof Lst)
      return getVariableList((Lst<?>) x);
    if (x instanceof BArray)
      return newV(barray, x);
    if (x instanceof ScriptContext)
      return newV(context, x);
    // rest are specific array types supported
    if (Escape.isAV(x))
      return getVariableAV((SV[]) x);
    if (PT.isAI(x))
      return getVariableAI((int[]) x);
    if (PT.isAB(x))
      return getVariableAB((byte[]) x);
    if (PT.isAF(x))
      return getVariableAF((float[]) x);
    if (PT.isAD(x))
      return getVariableAD((double[]) x);
    if (PT.isAS(x))
      return getVariableAS((String[]) x);
    if (PT.isAP(x))
      return getVariableAP((P3[]) x);
    if (PT.isAII(x))
      return getVariableAII((int[][]) x);
    if (PT.isAFF(x))
      return getVariableAFF((float[][]) x);
    if (PT.isASS(x))
      return getVariableASS((String[][]) x);
    if (PT.isADD(x))
      return getVariableADD((double[][]) x);
    if (PT.isAFloat(x))
      return newV(listf, x);
    return newS(x.toString());
  }

  @SuppressWarnings("unchecked")
  public
  static SV getVariableMap(Map<String, ?> x) {
    Map<String, Object> ht = (Map<String, Object>) x;
    Object o = null;
    for (Object oo : ht.values()) {
      o = oo;
      break;
    }
    if (!(o instanceof SV)) {
      Map<String, SV> x2 = new Hashtable<String, SV>();
      for (Map.Entry<String, Object> entry : ht.entrySet())
        x2.put(entry.getKey(), getVariable(entry.getValue()));
      x = x2;
    }
    return newV(hash, x);
  }

  public static SV getVariableList(Lst<?> v) {
    int len = v.size();
    if (len > 0 && v.get(0) instanceof SV)
      return newV(varray, v);
    Lst<SV> objects = new  Lst<SV>();
    for (int i = 0; i < len; i++)
      objects.addLast(getVariable(v.get(i)));
    return newV(varray, objects);
  }

  static SV getVariableAV(SV[] v) {
    Lst<SV> objects = new  Lst<SV>();
    for (int i = 0; i < v.length; i++)
      objects.addLast(v[i]);
    return newV(varray, objects);
  }

  public static SV getVariableAD(double[] f) {
    Lst<SV> objects = new  Lst<SV>();
    for (int i = 0; i < f.length; i++)
      objects.addLast(newV(decimal, Float.valueOf((float) f[i])));
    return newV(varray, objects);
  }

  static SV getVariableAS(String[] s) {
    Lst<SV> objects = new  Lst<SV>();
    for (int i = 0; i < s.length; i++)
      objects.addLast(newV(string, s[i]));
    return newV(varray, objects);
  }

  static SV getVariableAP(P3[] p) {
    Lst<SV> objects = new  Lst<SV>();
    for (int i = 0; i < p.length; i++)
      objects.addLast(newV(point3f, p[i]));
    return newV(varray, objects);
  }

  static SV getVariableAFF(float[][] fx) {
    Lst<SV> objects = new  Lst<SV>();
    for (int i = 0; i < fx.length; i++)
      objects.addLast(getVariableAF(fx[i]));
    return newV(varray, objects);
  }

  static SV getVariableADD(double[][] fx) {
    Lst<SV> objects = new  Lst<SV>();
    for (int i = 0; i < fx.length; i++)
      objects.addLast(getVariableAD(fx[i]));
    return newV(varray, objects);
  }

  static SV getVariableASS(String[][] fx) {
    Lst<SV> objects = new  Lst<SV>();
    for (int i = 0; i < fx.length; i++)
      objects.addLast(getVariableAS(fx[i]));
    return newV(varray, objects);
  }

  static SV getVariableAII(int[][] ix) {
    Lst<SV> objects = new  Lst<SV>();
    for (int i = 0; i < ix.length; i++)
      objects.addLast(getVariableAI(ix[i]));
    return newV(varray, objects);
  }

  static SV getVariableAF(float[] f) {
    Lst<SV> objects = new  Lst<SV>();
    for (int i = 0; i < f.length; i++)
      objects.addLast(newV(decimal, Float.valueOf(f[i])));
    return newV(varray, objects);
  }

  static SV getVariableAI(int[] ix) {
    Lst<SV> objects = new  Lst<SV>();
    for (int i = 0; i < ix.length; i++)
      objects.addLast(newI(ix[i]));
    return newV(varray, objects);
  }

  static SV getVariableAB(byte[] ix) {
    Lst<SV> objects = new  Lst<SV>();
    for (int i = 0; i < ix.length; i++)
      objects.addLast(newI(ix[i]));
    return newV(varray, objects);
  }

  public SV setName(String name) {
    this.myName = name;
    flags |= FLAG_CANINCREMENT;
    //System.out.println("Variable: " + name + " " + intValue + " " + value);
    return this;
  }

  public boolean isModified() {
    return tokAttr(flags, FLAG_MODIFIED);
  }
  
  public void setModified(boolean tf) {
    if (tf)
      flags |= FLAG_MODIFIED;
    else
      flags &= ~FLAG_MODIFIED;
  }
  
  boolean canIncrement() {
    return true;//tokAttr(flags, FLAG_CANINCREMENT);
  }

  boolean increment(int n) {
    if (!canIncrement())
      return false;
    switch (tok) {
    case integer:
      intValue += n;
      break;
    case decimal:
      value = Float.valueOf(((Float) value).floatValue() + n);
      break;
    default:
      return false;
    }
    return true;
  }

  public boolean asBoolean() {
    return bValue(this);
  }

  public int asInt() {
    return iValue(this);
  }

  public float asFloat() {
    return fValue(this);
  }

  public String asString() {
    return sValue(this);
  }

  // math-related Token static methods

  private final static P3 pt0 = new P3();

  /**
   * 
   * @param x
   * @return   Object-wrapped value
   */
  
  public static Object oValue(SV x) {
    switch (x == null ? nada : x.tok) {
    case on:
      return Boolean.TRUE;
    case nada:
    case off:
      return Boolean.FALSE;
    case integer:
      return Integer.valueOf(x.intValue);
    case bitset:
    case array:
      return selectItemVar(x).value; // TODO: matrix3f?? 
    default:
      return x.value;
    }
  }

  /**
   * 
   * @param x
   * @return  numeric value -- integer or decimal
   */
  static Object nValue(T x) {
    int iValue;
    switch (x == null ? nada : x.tok) {
    case decimal:
      return x.value;
    case integer:
      iValue = x.intValue;
      break;
    case string:
      if (((String) x.value).indexOf(".") >= 0)
        return Float.valueOf(toFloat((String) x.value));
      iValue = (int) toFloat((String) x.value);
      break;
    case point3f:
      return Float.valueOf(((T3) x.value).length());
    default:
      iValue = 0;
    }
    return Integer.valueOf(iValue);
  }

  // there are reasons to use Token here rather than ScriptVariable
  // some of these functions, in particular iValue, fValue, and sValue
  
  public static boolean bValue(T x) {
    switch (x == null ? nada : x.tok) {
    case on:
    case hash:
    case context:
      return true;
    case off:
      return false;
    case integer:
      return x.intValue != 0;
    case decimal:
    case string:
    case varray:
      return fValue(x) != 0;
    case bitset:
    case barray:
      return iValue(x) != 0;
    case point3f:
    case point4f:
    case matrix3f:
    case matrix4f:
      return Math.abs(fValue(x)) > 0.0001f;
    default:
      return false;
    }
  }

  public static int iValue(T x) {
    switch (x == null ? nada : x.tok) {
    case on:
      return 1;
    case off:
      return 0;
    case integer:
      return x.intValue;
    case decimal:
    case varray:
    case string:
    case point3f:
    case point4f:
    case matrix3f:
    case matrix4f:
      return (int) fValue(x);
    case bitset:
      return BSUtil.cardinalityOf(bsSelectToken(x));
    case barray:
      return ((BArray) x.value).data.length;
    default:
      return 0;
    }
  }

  public static float fValue(T x) {
    switch (x == null ? nada : x.tok) {
    case on:
      return 1;
    case off:
      return 0;
    case integer:
      return x.intValue;
    case decimal:
      return ((Float) x.value).floatValue();
    case varray:
      int i = x.intValue;
      if (i == Integer.MAX_VALUE)
        return ((SV)x).getList().size();
      //$FALL-THROUGH$
    case string:
      return toFloat(sValue(x));
    case bitset:
    case barray:
      return iValue(x);
    case point3f:
      return ((P3) x.value).length();
    case point4f:
      return Measure.distanceToPlane((P4) x.value, pt0);
    case matrix3f:
      P3 pt = new P3();
      ((M3) x.value).rotate(pt);
      return pt.length();
    case matrix4f:
      P3 pt1 = new P3();
      ((M4) x.value).rotTrans(pt1);
      return pt1.length();
    default:
      return 0;
    }
  }

  public static String sValue(T x) {
    if (x == null)
      return "";
    int i;
    SB sb;
    switch (x.tok) {
    case on:
      return "true";
    case off:
      return "false";
    case integer:
      return "" + x.intValue;
    case bitset:
      BS bs = bsSelectToken(x);
      return (x.value instanceof BondSet ? Escape.eBond(bs) : Escape.eBS(bs));
    case varray:
      Lst<SV> sv = ((SV) x).getList();
      i = x.intValue;
      if (i <= 0)
        i = sv.size() - i;
      if (i != Integer.MAX_VALUE)
        return (i < 1 || i > sv.size() ? "" : sValue(sv.get(i - 1)));
      //$FALL-THROUGH$
    case hash:
    case context:
      if (x.value instanceof String)
        return (String) x.value; // just the command
      sb = new SB();
      sValueArray(sb, (SV) x, "", "", false, true, true, Integer.MAX_VALUE, false);
      return sb.toString();
    case string:
      String s = (String) x.value;
      i = x.intValue;
      if (i <= 0)
        i = s.length() - i;
      if (i == Integer.MAX_VALUE)
        return s;
      if (i < 1 || i > s.length())
        return "";
      return "" + s.charAt(i - 1);
    case point3f:
      return Escape.eP((P3) x.value);
    case point4f:
      return Escape.eP4((P4) x.value);
    case matrix3f:
    case matrix4f:
      return Escape.e(x.value);
    default:
      return x.value.toString();
    }
  }

  private static void sValueArray(SB sb, SV vx, String path, String tabs,
                                  boolean isEscaped, boolean isRaw, boolean addValues, int maxLevels, boolean skipEmpty) {
    switch (vx.tok) {
    case hash:
    case context:
    case varray:
      String thiskey = ";" + vx.hashCode() + ";";
      if (path.indexOf(thiskey) >= 0) {
        sb.append(isEscaped ? "{}" : vx.myName == null ? "<circular reference>"
            : "<" + vx.myName + ">");
        break;
      }
      path += thiskey;
      if (vx.tok == varray) {
        if (!addValues)
          return;
        if (!isRaw)
          sb.append(isEscaped ? "[ " : tabs + "[\n");
        Lst<SV> sx = vx.getList();
        for (int i = 0; i < sx.size(); i++) {
          if (isEscaped && i > 0)
            sb.append(",");
          SV sv = sx.get(i);
          sValueArray(sb, sv, path, tabs + "\t", isEscaped, tabs.length() == 0 && !isEscaped && isRawType(sv.tok), addValues, maxLevels, skipEmpty);
          if (!isEscaped)
            sb.append("\n");
        }
        if (!isRaw)
          sb.append(isEscaped ? " ]" : tabs + "]");
      } else if (--maxLevels >= 0){
        Map<String, SV> ht = (vx.tok == context ? ((ScriptContext) vx.value)
            .getFullMap() : vx.getMap());
        addKeys(sb, path, ht, tabs, isEscaped, addValues, maxLevels, skipEmpty);
      }
      break;
    default:
      if (!addValues)
        return;
      if (!isRaw && !isEscaped)
        sb.append(tabs);
      sb.append(isEscaped ? vx.escape() : sValue(vx));
    }
  }
  
  private static void addKeys(SB sb, String path, Map<String, SV> ht, String tabs, boolean isEscaped, boolean addValues, int maxLevels, boolean skipEmpty) {
    if (maxLevels < 0)
      return;
    Set<String> keyset = ht.keySet();
    String[] keys = ht.keySet().toArray(new String[keyset.size()]);
    Arrays.sort(keys);
    if (isEscaped) {
      sb.append("{ ");
      String sep = "";
      for (int i = 0; i < keys.length; i++) {
        String key = keys[i];
        if (addValues)
          sb.append(sep).append(PT.esc(key));
        else
           sb.appendC(' ').append(key);
        sValueArray(sb, ht.get(key), path, tabs+"\t", true, false, addValues, maxLevels, skipEmpty);
        sep = ",";
      }
      sb.append(" }");
      if (!addValues)
        sb.append("\n");

      return;
    }
    sb.append(tabs).append("{\n");
    tabs += "\t";
    for (int i = 0; i < keys.length; i++) {
      sb.append(tabs);
      String key = keys[i];
      sb.append(key).append("\t:");
      SB sb2 = new SB();
      SV v = ht.get(key);
      isEscaped = isRawType(v.tok);
      sValueArray(sb2, v, path, tabs, isEscaped, false, addValues, maxLevels, skipEmpty);
      String value = sb2.toString();
      if (isEscaped && addValues)
        sb.append("\t");
      else 
        sb.append("\n");
      sb.append(value).append("\n");
    }
    sb.append(tabs.substring(1)).append("}");
  }

  private static boolean isRawType(int tok) {
    switch (tok) {
    case T.string:
    case T.decimal:
    case T.integer:
    case T.point3f:
    case T.point4f:
    case T.bitset:
    case T.barray:
    case T.on:
    case T.off:
      return true;
    }
    return false;
  }

  public static P3 ptValue(SV x) {
    switch (x.tok) {
    case point3f:
      return (P3) x.value;
    case string:
      Object o = Escape.uP((String) x.value);
      if (o instanceof P3)
        return (P3) o;
    }
    return null;
  }  

  public static P4 pt4Value(SV x) {
    switch (x.tok) {
    case point4f:
      return (P4) x.value;
    case string:
      Object o = Escape.uP((String) x.value);
      if (!(o instanceof P4))
        break;
      return (P4) o;
    }
    return null;
  }

  private static float toFloat(String s) {
    return (s.equalsIgnoreCase("true") ? 1 
        : s.length() == 0 || s.equalsIgnoreCase("false") ? 0 
        : PT.parseFloatStrict(s));
  }

  public static SV concatList(SV x1, SV x2, boolean asNew) {
    Lst<SV> v1 = x1.getList();
    Lst<SV> v2 = x2.getList();
    if (!asNew) {
      if (v2 == null)
        v1.addLast(newT(x2));
      else
        for (int i = 0; i < v2.size(); i++)
          v1.addLast(v2.get(i));
      return x1;
    }
    Lst<SV> vlist = new Lst<SV>();
    //(v1 == null ? 1 : v1.size()) + (v2 == null ? 1 : v2.size())
    if (v1 == null)
      vlist.addLast(x1);
    else
      for (int i = 0; i < v1.size(); i++)
        vlist.addLast(v1.get(i));
    if (v2 == null)
      vlist.addLast(x2);
    else
      for (int i = 0; i < v2.size(); i++)
        vlist.addLast(v2.get(i));
    return getVariableList(vlist);
  }

  static BS bsSelectToken(T x) {
    x = selectItemTok(x, Integer.MIN_VALUE);
    return (BS) x.value;
  }

  public static BS bsSelectVar(SV var) {
    if (var.index == Integer.MAX_VALUE)
      var = selectItemVar(var);
    return (BS) var.value;
  }

  static BS bsSelectRange(T x, int n) {
    x = selectItemTok(x, Integer.MIN_VALUE);
    x = selectItemTok(x, (n <= 0 ? n : 1));
    x = selectItemTok(x, (n <= 0 ? Integer.MAX_VALUE - 1 : n));
    return (BS) x.value;
  }

  static SV selectItemVar(SV var) {
    // pass bitsets created by the select() or for() commands
    // and all arrays by reference
    if (var.index != Integer.MAX_VALUE || 
        (var.tok == varray || var.tok == barray) && var.intValue == Integer.MAX_VALUE)
      return var;
    return (SV) selectItemTok(var, Integer.MIN_VALUE);
  }

  static T selectItemTok(T tokenIn, int i2) {
    switch (tokenIn.tok) {
    case matrix3f:
    case matrix4f:
    case bitset:
    case varray:
    case barray:
    case string:
      break;
    default:
      return tokenIn;
    }

    // negative number is a count from the end

    BS bs = null;
    String s = null;

    int i1 = tokenIn.intValue;
    boolean isOne = (i2 == Integer.MIN_VALUE);
    if (i1 == Integer.MAX_VALUE) {
      // no selections have been made yet --
      // we just create a new token with the
      // same bitset and now indicate either
      // the selected value or "ALL" (max_value)
      return newSV(tokenIn.tok, (isOne ? i1 : i2), tokenIn.value);
    }
    int len = 0;
    boolean isInputSelected = (tokenIn instanceof SV && ((SV) tokenIn).index != Integer.MAX_VALUE);
    SV tokenOut = newSV(tokenIn.tok, Integer.MAX_VALUE, null);

    switch (tokenIn.tok) {
    case bitset:
      if (tokenIn.value instanceof BondSet) {
        bs = new BondSet((BS) tokenIn.value,
            ((BondSet) tokenIn.value).getAssociatedAtoms());
        len = BSUtil.cardinalityOf(bs);
      } else {
        bs = BSUtil.copy((BS) tokenIn.value);
        len = (isInputSelected ? 1 : BSUtil.cardinalityOf(bs));
      }
      break;
    case barray:
      len = ((BArray)(((SV) tokenIn).value)).data.length;
      break;
    case varray:
      len = ((SV) tokenIn).getList().size();
      break;
    case string:
      s = (String) tokenIn.value;
      len = s.length();
      break;
    case matrix3f:
      len = -3;
      break;
    case matrix4f:
      len = -4;
      break;
    }

    if (len < 0) {
      // matrix mode [1][3] or [13]
      len = -len;
      if (i1 > 0 && Math.abs(i1) > len) {
        int col = i1 % 10;
        int row = (i1 - col) / 10;
        if (col > 0 && col <= len && row <= len) {
          if (tokenIn.tok == matrix3f)
            return newV(decimal, Float.valueOf(((M3) tokenIn.value).getElement(
                row - 1, col - 1)));
          return newV(decimal,
              Float.valueOf(((M4) tokenIn.value).getElement(row - 1, col - 1)));
        }
        return newV(string, "");
      }
      if (Math.abs(i1) > len)
        return newV(string, "");
      float[] data = new float[len];
      if (len == 3) {
        if (i1 < 0)
          ((M3) tokenIn.value).getColumn(-1 - i1, data);
        else
          ((M3) tokenIn.value).getRow(i1 - 1, data);
      } else {
        if (i1 < 0)
          ((M4) tokenIn.value).getColumn(-1 - i1, data);
        else
          ((M4) tokenIn.value).getRow(i1 - 1, data);
      }
      if (isOne)
        return getVariableAF(data);
      if (i2 < 1 || i2 > len)
        return newV(string, "");
      return newV(decimal, Float.valueOf(data[i2 - 1]));
    }

    // "testing"[0] gives "g"
    // "testing"[-1] gives "n"
    // "testing"[3][0] gives "sting"
    // "testing"[-1][0] gives "ng"
    // "testing"[0][-2] gives just "g" as well
    // "testing"[-10] gives ""
    if (i1 <= 0)
      i1 = len + i1;
    if (!isOne) {
      if (i1 < 1)
        i1 = 1;
      if (i2 == 0)
        i2 = len;
      else if (i2 < 0)
        i2 = len + i2;
      if (i2 < i1)
        i2 = i1;
    }

    switch (tokenIn.tok) {
    case bitset:
      tokenOut.value = bs;
      if (isInputSelected) {
        if (i1 > 1)
          bs.clearAll();
        break;
      }
      if (isOne) {
        // i2 will be Integer.MIN_VALUE at this point
        // take care of easy ones the easy way
        if (i1 == len) {
          // {xxx}[0]
          i2 = bs.length() - 1;
        } else if (i1 == 1) {
          // {xxx}[1]
          i2 = bs.nextSetBit(0);
        } 
        if (i2 >= -1) {
          bs.clearAll();
          if (i2 >= 0)
            bs.set(i2);
          break;          
        }
        i2 = i1;
      }
      int n = 0;
      for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1))
        if (++n < i1 || n > i2)
          bs.clear(j);
      break;
    case string:
      tokenOut.value = (--i1 < 0 || i1 >= len ? "" : isOne ? s.substring(i1,
          i1 + 1) : s.substring(i1, Math.min(i2, len)));
      break;
    case varray:
      if (--i1 < 0 || i1 >= len)
        return newV(string, "");
      if (isOne)
        return ((SV) tokenIn).getList().get(i1);
      Lst<SV> o2 = new Lst<SV>();
      Lst<SV> o1 = ((SV) tokenIn).getList();
      
      int nn = Math.min(i2, len) - i1;
      for (int i = 0; i < nn; i++)
        o2.addLast(newT(o1.get(i + i1)));
      tokenOut.value = o2;
      break;
    case barray:
      if (--i1 < 0 || i1 >= len)
        return newV(string, "");
      byte[] data = ((BArray)(((SV) tokenIn).value)).data;
      if (isOne)
        return newI(data[i1]);
      byte[] b = new byte[Math.min(i2, len) - i1];
      for (int i = b.length; --i >= 0;)
        b[i] = data[i1 + i];
      tokenOut.value = new BArray(b);
      break;
    }
    return tokenOut;
  }

  void setSelectedValue(int pt1, int pt2, SV var) {
    if (pt1 == Integer.MAX_VALUE)
      return;
    int len;
    switch (tok) {
    case matrix3f:
    case matrix4f:
      len = (tok == matrix3f ? 3 : 4);
      if (pt2 != Integer.MAX_VALUE) {
        int col = pt2;
        int row = pt1;
        if (col > 0 && col <= len && row <= len) {
          if (tok == matrix3f)
            ((M3) value).setElement(row - 1, col - 1, fValue(var));
          else
            ((M4) value).setElement(row - 1, col - 1, fValue(var));
          return;
        }
      }
      if (pt1 != 0 && Math.abs(pt1) <= len
          && var.tok == varray) {
        Lst<SV> sv = var.getList();
        if (sv.size() == len) {
          float[] data = new float[len];
          for (int i = 0; i < len; i++)
            data[i] = fValue(sv.get(i));
          if (pt1 > 0) {
            if (tok == matrix3f)
              ((M3) value).setRowA(pt1 - 1, data);
            else
              ((M4) value).setRowA(pt1 - 1, data);
          } else {
            if (tok == matrix3f)
              ((M3) value).setColumnA(-1 - pt1, data);
            else
              ((M4) value).setColumnA(-1 - pt1, data);
          }
          break;
        }
      }
      break;
    case string:
      String str = (String) value;
      int pt = str.length();
      if (pt1 <= 0)
        pt1 = pt + pt1;
      if (--pt1 < 0)
        pt1 = 0;
      while (pt1 >= str.length())
        str += " ";
      if (pt2 == Integer.MAX_VALUE){
        pt2 = pt1;
      } else {
        if (--pt2 < 0)
          pt2 = pt + pt2;
        while (pt2 >= str.length())
          str += " ";
      }
      if (pt2 >= pt1)
        value = str.substring(0, pt1) + sValue(var)
          + str.substring(++pt2);
      intValue = index = Integer.MAX_VALUE;
      break;
    case varray:
      len = getList().size();
      if (pt1 <= 0)
        pt1 = len + pt1;
      if (--pt1 < 0)
        pt1 = 0;
      if (len <= pt1) {
        for (int i = len; i <= pt1; i++)
          getList().addLast(newV(string, ""));
      }
      getList().set(pt1, var);
      break;
    }
  }

  public String escape() {
    switch (tok) {
    case string:
      return PT.esc((String) value);
    case matrix3f:
    case matrix4f:
      return PT.toJSON(null, value);
    case varray:
    case hash:
    case context:
      SB sb = new SB();
      sValueArray(sb, this, "", "", true, false, true, Integer.MAX_VALUE, false);
      return sb.toString();
    default:
      return sValue(this);
    }
  }

  public static Object unescapePointOrBitsetAsVariable(Object o) {
    if (o == null)
      return o;
    Object v = null;
    String s = null;
    if (o instanceof SV) {
      SV sv = (SV) o;
      switch (sv.tok) {
      case point3f:
      case point4f:
      case matrix3f:
      case matrix4f:
      case bitset:
        v = sv.value;
        break;
      case string:
        s = (String) sv.value;
        break;
      default:
        s = sValue(sv);
        break;
      }
    } else if (o instanceof String) {
      s = (String) o;
    }
    if (s != null && s.length() == 0)
      return s;
    if (v == null)
      v = Escape.uABsM(s);
    if (v instanceof P3)
      return (newV(point3f, v));
    if (v instanceof P4)
      return newV(point4f, v);
    if (v instanceof BS) {
      if (s != null && s.indexOf("[{") == 0)
        v = new BondSet((BS) v);
      return newV(bitset, v);
    }
    if (v instanceof M34)
      return (newV(v instanceof M3 ? matrix3f : matrix4f, v));
    return o;
  }

  public static SV getBoolean(boolean value) {
    return newT(value ? vT : vF);
  }
  
  public static Object sprintf(String strFormat, SV var) {
    if (var == null)
      return strFormat;
    int[] vd = (strFormat.indexOf("d") >= 0 || strFormat.indexOf("i") >= 0 ? new int[1]
        : null);
    boolean isArray = (var.tok == varray);
    float[] vf = (strFormat.indexOf("f") >= 0 ? new float[1] : null);
    double[] ve = (strFormat.indexOf("e") >= 0 ? new double[1] : null);
    boolean getS = (strFormat.indexOf("s") >= 0);
    boolean getP = (strFormat.indexOf("p") >= 0 && (isArray || var.tok == point3f));
    boolean getQ = (strFormat.indexOf("q") >= 0 && (isArray || var.tok == point4f));
    Object[] of = new Object[] { vd, vf, ve, null, null, null};
    if (!isArray)
      return sprintf(strFormat, var, of, vd, vf, ve, getS, getP, getQ);
    Lst<SV> sv = var.getList();
    String[] list2 = new String[sv.size()];
    for (int i = 0; i < list2.length; i++)
      list2[i] = sprintf(strFormat, sv.get(i), of, vd, vf, ve, getS, getP, getQ);
    return list2;
  }

  private static String sprintf(String strFormat, SV var, Object[] of, 
                                int[] vd, float[] vf, double[] ve, boolean getS, boolean getP, boolean getQ) {
    if (vd != null)
      vd[0] = iValue(var);
    if (vf != null)
      vf[0] = fValue(var);
    if (ve != null)
      ve[0] = fValue(var);
    if (getS)
      of[3] = sValue(var);
    if (getP)
      of[4]= var.value;
    if (getQ)
      of[5]= var.value;
    return Txt.sprintf(strFormat, "IFDspq", of );
  }

  /**
   * 
   * @param format
   * @return 0: JSON, 5: base64, 12: bytearray, 22: array
   */
  public static int getFormatType(String format) {
    return (format.indexOf(";") >= 0 ? -1 :
        ";json;base64;bytearray;array;"
    //   0    5      12        22
        .indexOf(";" + format.toLowerCase() + ";"));
  }

 /**
   * Accepts arguments from the format() function First argument is a
   * format string.
   * 
   * @param args
   * @param pt 0: to JSON, 5: to base64, 12: to bytearray, 22: to array
   * @return formatted string
   */
  public static Object format(SV[] args, int pt) {
    switch (args.length) {
    case 0:
      return "";
    case 1:
      return sValue(args[0]);
    case 2:
      if (pt == Integer.MAX_VALUE)
        pt = getFormatType(args[0].asString());
      switch (pt) {
      case 0:
        return args[1].toJSON();
      case 5:
      case 12:
      case 22:
        byte[] bytes;
        switch (args[1].tok) {
        case barray:
          bytes = AU.arrayCopyByte(((BArray) args[1].value).data, -1);
          break;
        case varray:
          Lst<SV> l = args[1].getList();
          if (pt == 22) {
            Lst<SV> l1 = new Lst<SV>();
            for (int i = l.size(); --i >= 0;)
              l1.addLast(l.get(i));
            return l1;
          }
          bytes = new byte[l.size()];
          for (int i = bytes.length; --i >= 0;)
            bytes[i] = (byte) l.get(i).asInt();
          break;
        default:
          String s = args[1].asString();
          if (s.startsWith(";base64,")){
            if (pt == 5)
              return s;
            bytes = Base64.decodeBase64(s);
          } else {
            bytes = s.getBytes();
          }
        }
        return (pt == 22 ? getVariable(bytes) : pt == 12 ? new BArray(bytes) : ";base64,"
            + javajs.util.Base64.getBase64(bytes).toString());
      }
    }
    // use values to replace codes in format string
    SB sb = new SB();
    String[] format = PT.split(PT.rep(sValue(args[0]), "%%", "\1"), "%");
    sb.append(format[0]);
    for (int i = 1; i < format.length; i++) {
      Object ret = sprintf(Txt.formatCheck("%" + format[i]),
          (i < args.length ? args[i] : null));
      if (PT.isAS(ret)) {
        String[] list = (String[]) ret;
        for (int j = 0; j < list.length; j++)
          sb.append(list[j]).append("\n");
        continue;
      }
      sb.append((String) ret);
    }
    return sb.toString();
  }
  
  @SuppressWarnings("unchecked")
  public static BS getBitSet(SV x, boolean allowNull) {
    switch (x.tok) {
    case bitset:
      return bsSelectVar(x);
    case varray:
      BS bs = new BS();
      Lst<SV> sv = (Lst<SV>) x.value;
      for (int i = 0; i < sv.size(); i++)
        if (!sv.get(i).unEscapeBitSetArray(bs) && allowNull)
          return null;
      return bs;
    }
    return (allowNull ? null : new BS());
  }

  /**
   * For legacy reasons, "x" == "X" but see isLike()
   * 
   * @param x1
   * @param x2
   * @return x1 == x2
   */
  public static boolean areEqual(SV x1, SV x2) {
    if (x1 == null || x2 == null)
      return false;
    if (x1.tok == x2.tok) {
      switch (x1.tok) {
      case string:
        return ((String)x1.value).equalsIgnoreCase((String) x2.value);
      case bitset:
      case barray:
      case hash:
      case varray:
      case context:
        return x1.equals(x2);
      case point3f:
        return (((P3) x1.value).distance((P3) x2.value) < 0.000001);
      case point4f:
        return (((P4) x1.value).distance4((P4) x2.value) < 0.000001);
      }
    }
    return (Math.abs(fValue(x1) - fValue(x2)) < 0.000001);
  }

  /**
   * a LIKE "x"    a is a string and equals x
   * a LIKE "*x"   a is a string and ends with x
   * a LIKE "x*"   a is a string and starts with x
   * a LIKE "*x*"  a is a string and contains x
   *  
   * @param x1
   * @param x2
   * @return  x1 LIKE x2
   */
  public static boolean isLike(SV x1, SV x2) {
    return (x1 != null && x2 != null 
        && x1.tok == string && x2.tok == string
        && PT.isLike((String)x1.value, (String) x2.value));
  }

  protected class Sort implements Comparator<SV> {
    private int arrayPt;
    
    protected Sort(int arrayPt) {
      this.arrayPt = arrayPt;
    }
    
    @Override
    public int compare(SV x, SV y) {
      if (x.tok != y.tok) {
        if (x.tok == decimal || x.tok == integer
            || y.tok == decimal || y.tok == integer) {
          float fx = fValue(x);
          float fy = fValue(y);
          return (fx < fy ? -1 : fx > fy ? 1 : 0);
        }
        if (x.tok == string || y.tok == string)
          return sValue(x).compareTo(sValue(y));
      }
      switch (x.tok) {
      case string:
        return sValue(x).compareTo(sValue(y));
      case varray:
        Lst<SV> sx = x.getList();
        Lst<SV> sy = y.getList();
        if (sx.size() != sy.size())
          return (sx.size() < sy.size() ? -1 : 1);
        int iPt = arrayPt;
        if (iPt < 0)
          iPt += sx.size();
        if (iPt < 0 || iPt >= sx.size())
          return 0;
        return compare(sx.get(iPt), sy.get(iPt));
      default:
        float fx = fValue(x);
        float fy = fValue(y);
        return (fx < fy ? -1 : fx > fy ? 1 : 0);
      }
    } 
  }
  
  /**
   * 
   * @param arrayPt
   *        1-based or Integer.MIN_VALUE to reverse
   * @return sorted or reversed array
   */
  public SV sortOrReverse(int arrayPt) {
    Lst<SV> x = getList();
    if (x != null && x.size() > 1) {
      if (arrayPt == Integer.MIN_VALUE) {
        // reverse
        int n = x.size();
        for (int i = 0; i < n; i++) {
          SV v = x.get(i);
          x.set(i, x.get(--n));
          x.set(n, v);
        }
      } else {
        Collections.sort(getList(), new Sort(--arrayPt));
      }
    }
    return this;
  }

  /**
   * 
   * Script variables are pushed after cloning, because
   * the name comes with them when we do otherwise
   * they are not mutable anyway. We do want to have actual
   * references to points, lists, and associative arrays
   * 
   * @param value
   *        null to pop
   * @param mapKey
   * @return array
   */
  public SV pushPop(SV value, SV mapKey) {
    if (mapKey != null) {
      Map<String, SV> m = getMap();
      if (value == null) {
        SV v;
        return (m == null || (v = m.remove(mapKey.asString())) == null ? 
            newS("") : v);
      }
      if (m != null)
        m.put(mapKey.asString(), newI(0).setv(value));
    } else {
      Lst<SV> x = getList();
      if (value == null || x == null)
        return (x == null || x.size() == 0 ? newS("") : x.remove(x.size() - 1));
      x.addLast(newI(0).setv(value));
    }
    return this;
  }

  boolean unEscapeBitSetArray(BS bs) {
    switch(tok) {
    case string:
      BS bs1 = BS.unescape((String) value);
      if (bs1 == null)
        return false;
      bs.or(bs1);
      return true;
    case bitset:
      bs.or((BS) value);
      return true;
    }
    return false;   
  }

  static BS unEscapeBitSetArray(ArrayList<SV> x, boolean allowNull) {
    BS bs = new BS();
    for (int i = 0; i < x.size(); i++)
      if (!x.get(i).unEscapeBitSetArray(bs) && allowNull)
        return null;
    return bs;
  }

  public static String[] strListValue(T x) {
    if (x.tok != varray)
      return new String[] { sValue(x) };
    Lst<SV> sv = ((SV) x).getList();
    String[] list = new String[sv.size()];
    for (int i = sv.size(); --i >= 0;)
      list[i] = sValue(sv.get(i));
    return list;
  }

// I have no idea! 
//
//  static List<Object> listAny(SV x) {
//    List<Object> list = new List<Object>();
//    List<SV> l = x.getList();
//    for (int i = 0; i < l.size(); i++) {
//      SV v = l.get(i);
//      List<SV> l2 = v.getList();
//      if (l2 == null) {
//        list.addLast(v.value);        
//      } else {
//        List<Object> o = new List<Object>();
//        for (int j = 0; j < l2.size(); j++) {
//          v = l2.get(j);
//        }
//        list.addLast(o);
//      }
//    }
//    return list;    
//  }
  
  public static float[] flistValue(T x, int nMin) {
    if (x.tok != varray)
      return new float[] { fValue(x) };
    Lst<SV> sv = ((SV) x).getList();
    float[] list;
    list = new float[Math.max(nMin, sv.size())];
    if (nMin == 0)
      nMin = list.length;
    for (int i = Math.min(sv.size(), nMin); --i >= 0;)
      list[i] = fValue(sv.get(i));
    return list;
  }

  public void toArray() {
    int dim;
    M3 m3 = null;
    M4 m4 = null;
    switch (tok) {
    case matrix3f:
      m3 = (M3) value;
      dim = 3;
      break;
    case matrix4f:
      m4 = (M4) value;
      dim = 4;
      break;
    default:
      return;
    }
    tok = varray;
    Lst<SV> o2 = new  Lst<SV>(); //dim;
    for (int i = 0; i < dim; i++) {
      float[] a = new float[dim];
      if (m3 == null)
        m4.getRow(i, a);
      else
        m3.getRow(i, a);
      o2.addLast(getVariableAF(a));
    }
    value = o2;
  }

  @SuppressWarnings("unchecked")
  SV mapValue(String key) {
    switch (tok) {
    case hash:
      return ((Map<String, SV>) value).get(key);
    case context:
      ScriptContext sc = ((ScriptContext) value);
      return (key.equals("_path") ? newS(sc.contextPath) : sc.getVariable(key));
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public Lst<SV> getList() {
    return (tok == varray ? (Lst<SV>) value : null);
  }

  public static boolean isScalar(SV x) {
    switch (x.tok) {
    case varray:
      return false;
    case string:
      return (((String) x.value).indexOf("\n") < 0);
    default:
      return true;
    }
  }

  @Override
  public String toJSON() {
    switch (tok) {
    case on:
    case off:
    case integer:
    case decimal:
      return sValue(this);
    case barray:
      return PT.byteArrayToJSON(((BArray) value).data);
    case context:
      return PT.toJSON(null, ((ScriptContext) value).getFullMap());
    default:
     return PT.toJSON(null, value);
    }
  }

  public void mapPut(String key, SV v) {
    getMap().put(key, v);
  }

  @SuppressWarnings("unchecked")
  public SV mapGet(String key) {
    return (tok == hash ? ((Map<String, SV>) value).get(key) : SV.newS(""));
  }

  @SuppressWarnings("unchecked")
  public Map<String, SV> getMap() {
    switch (tok) {
    case hash:
      return (Map<String, SV>) value;
    case context:
      return ((ScriptContext) value).vars;
    }
    return null;
  }

  @Override
  public String toString() {
    return toString2() + "[" + myName + " index =" + index + " intValue=" + intValue + "]";
  }

  public String getMapKeys(int nLevels, boolean skipEmpty) {
    if (tok != hash)
      return "";
    SB sb = new SB();
    sValueArray(sb, this, "", "", true, false, false, nLevels + 1, skipEmpty);
    return sb.toString();
  }

}
