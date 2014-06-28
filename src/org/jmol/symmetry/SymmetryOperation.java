/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.symmetry;

import javajs.util.Lst;
import javajs.util.SB;



import org.jmol.api.SymmetryInterface;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Measure;
import javajs.util.PT;
import org.jmol.util.Parser;

import javajs.util.M3;
import javajs.util.M4;
import javajs.util.Matrix;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.Quat;
import javajs.util.T3;
import javajs.util.V3;
import org.jmol.modelset.ModelSet;
import org.jmol.script.T;

/*
 * Bob Hanson 4/2006
 * 
 * references: International Tables for Crystallography Vol. A. (2002) 
 *
 * http://www.iucr.org/iucr-top/cif/cifdic_html/1/cif_core.dic/Ispace_group_symop_operation_xyz.html
 * http://www.iucr.org/iucr-top/cif/cifdic_html/1/cif_core.dic/Isymmetry_equiv_pos_as_xyz.html
 *
 * LATT : http://macxray.chem.upenn.edu/LATT.pdf thank you, Patrick Carroll
 * 
 * NEVER ACCESS THESE METHODS DIRECTLY! ONLY THROUGH CLASS Symmetry
 */


class SymmetryOperation extends M4 {
  String xyzOriginal;
  String xyz;
  private boolean doNormalize = true;
  boolean isFinalized;
  private int opId;
  V3 centering;

  private P3 atomTest;

  private String[] myLabels;
  int modDim;

// rsvs:
//    [ [(3+modDim)*x + 1]    
//      [(3+modDim)*x + 1]     [ Gamma_R   [0x0]   | Gamma_S
//      [(3+modDim)*x + 1]  ==    [0x0]    Gamma_e | Gamma_d 
//      ...                       [0]       [0]    |   1     ]
//      [0 0 0 0 0...   1] ]
  
  float[] linearRotTrans;
  
  Matrix rsvs;
  private boolean isBio;
  private Matrix sigma;
  int index;
  String subsystemCode;
  public int timeReversal;
  
  void setSigma(String subsystemCode, Matrix sigma) {
    this.subsystemCode = subsystemCode;
    this.sigma = sigma;
  }

  /**
   * @j2sIgnoreSuperConstructor
   * @j2sOverride
   * 
   * @param op
   * @param atoms
   * @param atomIndex
   * @param countOrId
   * @param doNormalize
   */
  SymmetryOperation(SymmetryOperation op, P3[] atoms,
                           int atomIndex, int countOrId, boolean doNormalize) {
    this.doNormalize = doNormalize;
    if (op == null) {
      opId = countOrId;
      return;
    }
    /*
     * externalizes and transforms an operation for use in atom reader
     * 
     */
    xyzOriginal = op.xyzOriginal;
    xyz = op.xyz;
    opId = op.opId;
    modDim = op.modDim;
    myLabels = op.myLabels;
    index = op.index;
    linearRotTrans = op.linearRotTrans;
    sigma = op.sigma;
    subsystemCode = op.subsystemCode;
    timeReversal = op.timeReversal;
    setMatrix(false);
    if (!op.isFinalized)
      doFinalize();
    if (doNormalize && sigma == null)
      setOffset(atoms, atomIndex, countOrId);
  }

  

  /**
   * rsvs is the superspace group rotation-translation matrix.
   * It is a (3 + modDim + 1) x (3 + modDim + 1) matrix from 
   * which we can extract all necessary parts;
   * @param isReverse 
   * 
   */
  private void setGamma(boolean isReverse) {
  // standard M4 (this)
  //
  //  [ [rot]   | [trans] 
  //     [0]    |   1     ]
  //
  // becomes for a superspace group
  //
  //  [ Gamma_R   [0x0]   | Gamma_S
  //    [0x0]     Gamma_e | Gamma_d 
  //     [0]       [0]    |   1     ]
    
    int n = 3 + modDim;
    double[][] a = (rsvs = new Matrix(null, n + 1, n + 1)).getArray();
    double[] t = new double[n];
    int pt = 0;
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++)
        a[i][j] = linearRotTrans[pt++];
      t[i] = (isReverse ? -1 : 1) * linearRotTrans[pt++];
    }
    a[n][n] = 1;
    if (isReverse)
      rsvs = rsvs.inverse();
    for (int i = 0; i < n; i++)
      a[i][n] = t[i];
    a = rsvs.getSubmatrix(0,  0,  3,  3).getArray();
    for (int i = 0; i < 3; i++)
      for (int j = 0; j < 4; j++)
        setElement(i,  j, (float) (j < 3 ? a[i][j] : t[i]));
    setElement(3,3,1);
  }

  void doFinalize() {
    m03 /= 12;
    m13 /= 12;
    m23 /= 12;
    if (modDim > 0) {
      double[][] a = rsvs.getArray();
      for (int i = a.length - 1; --i >= 0;)
        a[i][3 + modDim] /= 12;
    }
    isFinalized = true;
  }
  
  String getXyz(boolean normalized) {
    return (normalized && modDim == 0 || xyzOriginal == null ? xyz : xyzOriginal);
  }

  void newPoint(P3 atom1, P3 atom2, int x, int y, int z) {
    rotTrans2(atom1, atom2);
    atom2.add3(x,  y,  z);
  }

  String dumpInfo() {
    return "\n" + xyz + "\ninternal matrix representation:\n"
        + toString();
  }

  final static String dumpSeitz(M4 s, boolean isCanonical) {
    SB sb = new SB();
    float[] r = new float[4];
    for (int i = 0; i < 3; i++) {
      s.getRow(i,r);
      sb.append("[\t");
      for (int j = 0; j < 3; j++)
        sb.appendI((int) r[j]).append("\t");
      sb.append(twelfthsOf(isCanonical ? ((int)r[3] + 12)%12 : (int) r[3])).append("\t]\n");
    }
    return sb.toString();
  }
  
  boolean setMatrixFromXYZ(String xyz, int modDim, boolean allowScaling) {
    /*
     * sets symmetry based on an operator string "x,-y,z+1/2", for example
     * 
     */
    if (xyz == null)
      return false;
    xyzOriginal = xyz;
    xyz = xyz.toLowerCase();
    int n = (modDim + 4) * (modDim + 4);
    this.modDim = modDim;
    if (modDim > 0)
      myLabels = labelsXn;
    linearRotTrans = new float[n];
    boolean isReverse = (xyz.startsWith("!"));
    if (isReverse)
      xyz = xyz.substring(1);
    if (xyz.indexOf("xyz matrix:") == 0) {
      /* note: these terms must in unit cell fractional coordinates!
       * CASTEP CML matrix is in fractional coordinates, but do not take into account
       * hexagonal systems. Thus, in wurtzite.cml, for P 6c 2'c:
       *
       * "transform3": 
       * 
       * -5.000000000000e-1  8.660254037844e-1  0.000000000000e0   0.000000000000e0 
       * -8.660254037844e-1 -5.000000000000e-1  0.000000000000e0   0.000000000000e0 
       *  0.000000000000e0   0.000000000000e0   1.000000000000e0   0.000000000000e0 
       *  0.000000000000e0   0.000000000000e0   0.000000000000e0   1.000000000000e0
       *
       * These are transformations of the STANDARD xyz axes, not the unit cell. 
       * But, then, what coordinate would you feed this? Fractional coordinates of what?
       * The real transform is something like x-y,x,z here.
       * 
       */
      this.xyz = xyz;
      Parser.parseStringInfestedFloatArray(xyz, null, linearRotTrans);        
      return setFromMatrix(null, isReverse);
    }
    if (xyz.indexOf("[[") == 0) {
      xyz = xyz.replace('[',' ').replace(']',' ').replace(',',' ');
      Parser.parseStringInfestedFloatArray(xyz, null, linearRotTrans);
      for (int i = 0; i < n; i++) {
        float v = linearRotTrans[i];
        if (Float.isNaN(v))
          return false;
      }
      setMatrix(isReverse);
      isFinalized = true;
      isBio = (xyz.indexOf("bio") >= 0);
      this.xyz = (isBio ? toString() : getXYZFromMatrix(this, false, false, false));
      return true;
    }
    if (xyz.indexOf(",m") >= 0) {
      timeReversal = (xyz.indexOf(",m-1") >= 0 ? -1 : 1);
      allowScaling = true;
    }
    String strOut = getMatrixFromString(this, xyz, linearRotTrans, allowScaling);
    if (strOut == null)
      return false;
    setMatrix(isReverse);
    this.xyz = (isReverse ? getXYZFromMatrix(this, true, false, false) : strOut);
    if (timeReversal != 0)
      this.xyz += (timeReversal == 1 ? ",m+1" : ",m-1");
    //System.out.println("testing " + xyz +  " == " + this.xyz + " " + this + "\n" + Escape.eAF(linearRotTrans));
    if (Logger.debugging)
      Logger.debug("" + this);
    return true;
  }


  private void setMatrix(boolean isReverse) {
    if (linearRotTrans.length > 16) {
      setGamma(isReverse);
    } else {
      setA(linearRotTrans);
      if (isReverse) {
        P3 p3 = P3.new3(m03,  m13,  m23);
        invertM(this);
        rotate(p3);
        p3.scale(-1);
        setTranslation(p3);
      }
    }
  }

  boolean setFromMatrix(float[] offset, boolean isReverse) {
    float v = 0;
    int pt = 0;
    myLabels = (modDim == 0 ? labelsXYZ : labelsXn);
    int rowPt = 0;
    int n = 3 + modDim;
    for (int i = 0; rowPt < n; i++) {
      if (Float.isNaN(linearRotTrans[i]))
        return false;
      v = linearRotTrans[i];
      if (Math.abs(v) < 0.00001f)
        v = 0;
      boolean isTrans = ((i + 1) % (n + 1) == 0);
      if (isTrans) {
        if (offset != null) {
          v /= 12;
          if (pt < offset.length)
            v += offset[pt++];
        }
        v = normalizeTwelfths((v < 0 ? -1 : 1) * Math.round(Math.abs(v * 12))
            / 12f, doNormalize);
        rowPt++;
      }
      linearRotTrans[i] = v;
    }
    linearRotTrans[linearRotTrans.length - 1] = 1;
    setMatrix(isReverse);
    isFinalized = (offset == null);
    xyz = getXYZFromMatrix(this, true, false, false);
    //System.out.println("testing " + xyz + " " + this + "\n" + Escape.eAF(linearRotTrans));
    return true;
  }

  /**
   * Convert the Jones-Faithful notation 
   *   "x, -z+1/2, y"  or "x1, x3-1/2, x2, x5+1/2, -x6+1/2, x7..."
   * to a linear array
   * 
   * Also allows a-b,-5a-5b,-c;0,0,0  format
   * 
   * @param op
   * @param xyz
   * @param linearRotTrans
   * @param allowScaling
   * @return canonized Jones-Faithful string
   */
  static String getMatrixFromString(SymmetryOperation op, String xyz,
                                    float[] linearRotTrans, boolean allowScaling) {
    boolean isDenominator = false;
    boolean isDecimal = false;
    boolean isNegative = false;
    int modDim = (op == null ? 0 : op.modDim);
    int nRows = 4 + modDim;
    boolean doNormalize = (op != null && op.doNormalize);
    int dimOffset = (modDim > 0 ? 3 : 0); // allow a b c to represent x y z
    linearRotTrans[linearRotTrans.length - 1] = 1;
    // may be a-b,-5a-5b,-c;0,0,0 form
    int transPt = xyz.indexOf(';') + 1;
    if (transPt != 0) {
      allowScaling = true;
      if (transPt == xyz.length())
        xyz += "0,0,0";
    }
    int rotPt = -1;
    String[] myLabels = (op == null || modDim == 0 ? null : op.myLabels);
    if (myLabels == null)
      myLabels = labelsXYZ;
    xyz = xyz.toLowerCase() + ",";
    if (modDim > 0)
      for (int i = modDim + 3; --i >= 0;)
        xyz = PT.rep(xyz, labelsXn[i], labelsXnSub[i]);
    int xpt = 0;
    int tpt0 = 0;
    int rowPt = 0;
    char ch;
    float iValue = 0;
    float decimalMultiplier = 1f;
    String strT = "";
    String strOut = "";
    for (int i = 0; i < xyz.length(); i++) {
      switch (ch = xyz.charAt(i)) {
      case ';':
        break;
      case '\'':
      case ' ':
      case '{':
      case '}':
      case '!':
        continue;
      case '-':
        isNegative = true;
        continue;
      case '+':
        isNegative = false;
        continue;
      case '/':
        isDenominator = true;
        continue;
      case 'x':
      case 'y':
      case 'z':
      case 'a':
      case 'b':
      case 'c':
      case 'd':
      case 'e':
      case 'f':
      case 'g':
      case 'h':
        tpt0 = rowPt * nRows;
        int ipt = (ch >= 'x' ? ch - 'x' :ch - 'a' + dimOffset);
        xpt = tpt0 + ipt;
        int val = (isNegative ? -1 : 1);
        if (allowScaling && iValue != 0) {
          linearRotTrans[xpt] = iValue; 
          val = (int) iValue;
          iValue = 0;
        } else {
          linearRotTrans[xpt] = val; 
        }
        strT += plusMinus(strT, val, myLabels[ipt]);
        break;
      case ',':
        if (transPt != 0) {
          if (transPt > 0) {
            // now read translation
            rotPt = i;
            i = transPt - 1;
            transPt = -i;
            iValue = 0;
            continue;
          }
          transPt = i + 1;
          i = rotPt;
        }
        // add translation in 12ths
        iValue = normalizeTwelfths(iValue, doNormalize);
        linearRotTrans[tpt0 + nRows - 1] = iValue;
        strT += xyzFraction(iValue, false, true);
        strOut += (strOut == "" ? "" : ",") + strT;
        if (rowPt == nRows - 2)
          return strOut;
        iValue = 0;
        strT = "";
        if (rowPt++ > 2 && modDim == 0) {
          Logger.warn("Symmetry Operation? " + xyz);
          return null;
        }
        break;
      case '.':
        isDecimal = true;
        decimalMultiplier = 1f;
        continue;
      case '0':
        if (!isDecimal && (isDenominator || !allowScaling))
          continue;
        //$FALL-THROUGH$
      default:
        //Logger.debug(isDecimal + " " + ch + " " + iValue);
        int ich = ch - '0';
        if (isDecimal && ich >= 0 && ich <= 9) {
          decimalMultiplier /= 10f;
          if (iValue < 0)
            isNegative = true;
          iValue += decimalMultiplier * ich * (isNegative ? -1 : 1);
          continue;
        }
        if (ich >= 0 && ich <= 9) {
          if (isDenominator) {
            if (iValue == 0) {
              // a/2,....
              linearRotTrans[xpt] /= ich;
            } else {
              iValue /= ich;
            }
          } else {
            iValue = iValue * 10 + (isNegative ? -1 : 1) * ich;
            isNegative = false;
          }
        } else {
          Logger.warn("symmetry character?" + ch);
        }
      }
      isDecimal = isDenominator = isNegative = false;
    }
    return null;
  }

  private final static String xyzFraction(float n12ths, boolean allPositive, boolean halfOrLess) {
    n12ths = Math.round(n12ths);
    if (allPositive) {
      while (n12ths < 0)
        n12ths += 12f;
    } else if (halfOrLess) {
      while (n12ths > 6f)
        n12ths -= 12f;
      while (n12ths < -6f)
        n12ths += 12f;
    }
    String s = twelfthsOf(n12ths);
    return (s.charAt(0) == '0' ? "" : n12ths > 0 ? "+" + s : s);
  }

  private final static String twelfthsOf(float n12ths) {
    String str = "";
    int i12ths = Math.round(n12ths);
    if (i12ths == 12)
      return "1";
    if (i12ths == -12)
      return "-1";
    if (i12ths < 0) {
      i12ths = -i12ths;
      if (i12ths % 12 != 0)
        str = "-";
    }
    int n = i12ths / 12;
    if (n < 1)
      return str + twelfths[i12ths % 12];
    int m = 0;
    switch (i12ths % 12) {
    case 0:
      return str + n;
    case 1:
    case 5:
    case 7:
    case 11:
      m = 12;
      break;
    case 2:
    case 10:
      m = 6;
      break;
    case 3:
    case 9:
      m = 4;
      break;
    case 4:
    case 8:
      m = 3;
      break;
    case 6:
      m = 2;
      break;
    }
    return str + (i12ths * m / 12) + "/" + m;
  }

  private final static String[] twelfths = { "0", "1/12", "1/6", "1/4", "1/3",
  "5/12", "1/2", "7/12", "2/3", "3/4", "5/6", "11/12" };

  private static String plusMinus(String strT, float x, String sx) {
    return (x == 0 ? "" : (x < 0 ? "-" : strT.length() == 0 ? "" : "+") + (x == 1 || x == -1 ? "" : "" + (int) Math.abs(x)) + sx);
  }

  private static float normalizeTwelfths(float iValue, boolean doNormalize) {
    iValue *= 12f;
    if (doNormalize) {
      while (iValue > 6)
        iValue -= 12;
      while (iValue <= -6)
        iValue += 12;
    }
    return iValue;
  }

  final static String[] labelsXYZ = new String[] {"x", "y", "z"};

  final static String[] labelsXn = new String[] {"x1", "x2", "x3", "x4", "x5", "x6", "x7", "x8", "x9", "x10", "x11", "x12", "x13"};
  final static String[] labelsXnSub = new String[] {"x", "y", "z", "a",  "b",  "c",  "d",  "e",  "f",  "g",   "h",   "i",   "j"};

  final static String getXYZFromMatrix(M4 mat, boolean is12ths,
                                       boolean allPositive, boolean halfOrLess) {
    String str = "";
    SymmetryOperation op = (mat instanceof SymmetryOperation ? (SymmetryOperation) mat
        : null);
    if (op != null && op.modDim > 0)
      return getXYZFromRsVs(op.rsvs.getRotation(), op.rsvs.getTranslation(), is12ths);
    float[] row = new float[4];
    for (int i = 0; i < 3; i++) {
      int lpt = (i < 3 ? 0 : 3);
      mat.getRow(i, row);
      String term = "";
      for (int j = 0; j < 3; j++)
        if (row[j] != 0)
          term += plusMinus(term, row[j], labelsXYZ[j + lpt]);
      term += xyzFraction((is12ths ? row[3] : row[3] * 12), allPositive,
          halfOrLess);
      str += "," + term;
    }
    return str.substring(1);
  }

  private void setOffset(P3[] atoms, int atomIndex, int count) {
    /*
     * the center of mass of the full set of atoms is moved into the cell with this
     *  
     */
    int i1 = atomIndex;
    int i2 = i1 + count;
    float x = 0;
    float y = 0;
    float z = 0;
    if (atomTest == null)
      atomTest = new P3();
    for (int i = i1; i < i2; i++) {
      newPoint(atoms[i], atomTest, 0, 0, 0);
      x += atomTest.x;
      y += atomTest.y;
      z += atomTest.z;
    }
    
    while (x < -0.001 || x >= count + 0.001) {
      m03 += (x < 0 ? 1 : -1);
      x += (x < 0 ? count : -count);
    }
    while (y < -0.001 || y >= count + 0.001) {
      m13 += (y < 0 ? 1 : -1);
      y += (y < 0 ? count : -count);
    }
    while (z < -0.001 || z >= count + 0.001) {
      m23 += (z < 0 ? 1 : -1);
      z += (z < 0 ? count : -count);
    }
  }

//  // action of this method depends upon setting of unitcell
//  private void transformCartesian(UnitCell unitcell, P3 pt) {
//    unitcell.toFractional(pt, false);
//    transform(pt);
//    unitcell.toCartesian(pt, false);
//
//  }
  
  V3[] rotateAxes(V3[] vectors, UnitCell unitcell, P3 ptTemp, M3 mTemp) {
    V3[] vRot = new V3[3];
    getRotationScale(mTemp);    
    for (int i = vectors.length; --i >=0;) {
      ptTemp.setT(vectors[i]);
      unitcell.toFractional(ptTemp, true);
      mTemp.rotate(ptTemp);
      unitcell.toCartesian(ptTemp, true);
      vRot[i] = V3.newV(ptTemp);
    }
    return vRot;
  }
  
  /**
   * 
   * @param modelSet
   * @param uc
   * @param pta00
   *        optional initial atom point
   * @param ptTarget
   *        optional target atom point
   * @param id
   * @return Object[] containing:
   * 
   *         [0] xyz (Jones-Faithful calculated from matrix)
   * 
   *         [1] xyzOriginal (Provided by calling method)
   * 
   *         [2] info ("C2 axis", for example)
   * 
   *         [3] draw commands
   * 
   *         [4] translation vector (fractional)
   * 
   *         [5] translation vector (Cartesian)
   * 
   *         [6] inversion point
   * 
   *         [7] axis point
   * 
   *         [8] axis vector (defines plane if angle = 0
   * 
   *         [9] angle of rotation
   * 
   *         [10] matrix representation
   * 
   *         [11] vtrans ?
   * 
   *         [12] centering
   * 
   */
  Object[] getDescription(ModelSet modelSet, SymmetryInterface uc, P3 pta00,
                          P3 ptTarget, String id) {

    if (!isFinalized)
      doFinalize();
    // remove centering translation
    V3 vtemp = new V3();
    P3 ptemp = new P3();
    P3 ptemp2 = new P3();
    P3 pta01 = new P3();
    P3 pta02 = new P3();
    V3 ftrans = new V3();
    V3 vtrans = new V3();
    V3 vcentering = null;
    if (centering != null) {
      vcentering = V3.newV(centering);
      uc.toCartesian(vcentering, false);
    }
    boolean haveCentering = false;// better without this. (vcentering != null);

    if (pta00 == null || Float.isNaN(pta00.x))
      pta00 = new P3();
    if (ptTarget != null) {

      // Check to see that this is the correct operator
      setFractional(uc, pta01, pta00, ptemp);
      rotTrans(pta01);
      uc.toCartesian(pta01, false);
      uc.toUnitCell(pta01, ptemp);
      pta02.setT(ptTarget);
      uc.toUnitCell(pta02, ptemp);
      if (pta01.distance(pta02) > 0.1f)
        return null;

      // Check to see if the two points only differ by
      // a translation after transformation.
      // If so, add that difference to the matrix transformation 
      setFractional(uc, pta01, pta00, null);
      rotTrans(pta01);
      setFractional(uc, pta02, ptTarget, null);
      vtrans.sub2(pta02, pta01);
    }

    // get the frame vectors and points

    pta01.set(1, 0, 0);
    pta02.set(0, 1, 0);
    P3 pta03 = P3.new3(0, 0, 1);
    pta01.add(pta00);
    pta02.add(pta00);
    pta03.add(pta00);

    // we remove centering for now

    if (haveCentering)
      vtrans.sub(centering);

    // target point, rotated, inverted, and translated, but not centered

    P3 pt0 = rotTransCart(pta00, uc, vtrans);
    P3 pt1 = rotTransCart(pta01, uc, vtrans);
    P3 pt2 = rotTransCart(pta02, uc, vtrans);
    P3 pt3 = rotTransCart(pta03, uc, vtrans);

    V3 vt1 = V3.newVsub(pt1, pt0);
    V3 vt2 = V3.newVsub(pt2, pt0);
    V3 vt3 = V3.newVsub(pt3, pt0);

    approx(vtrans);

    // check for inversion
    vtemp.cross(vt1, vt2);
    boolean haveInversion = (vtemp.dot(vt3) < 0);

    // The first trick is to check cross products to see if we still have a
    // right-hand axis.

    if (haveInversion) {

      // undo inversion for quaternion analysis (requires proper rotations only)

      pt1.sub2(pt0, vt1);
      pt2.sub2(pt0, vt2);
      pt3.sub2(pt0, vt3);

    }

    // The second trick is to use quaternions. Each of the three faces of the
    // frame (xy, yz, and zx)
    // is checked. The helix() function will return data about the local helical
    // axis, and the
    // symop(sym,{0 0 0}) function will return the overall translation.

    Object[] info;
    info = (Object[]) Measure.computeHelicalAxis(
        null,
        T.array,
        pta00,
        pt0,
        Quat.getQuaternionFrame(pt0, pt1, pt2).div(
            Quat.getQuaternionFrame(pta00, pta01, pta02)));
    P3 pa1 = (P3) info[0];
    V3 ax1 = (V3) info[1];
    int ang1 = (int) Math.abs(PT.approx(((P3) info[3]).x, 1));
    float pitch1 = approxF(((P3) info[3]).y);

    if (haveInversion) {

      // redo inversion

      pt1.add2(pt0, vt1);
      pt2.add2(pt0, vt2);
      pt3.add2(pt0, vt3);

    }

    V3 trans = V3.newVsub(pt0, pta00);
    if (trans.length() < 0.1f)
      trans = null;

    // ////////// determination of type of operation from first principles

    P3 ptinv = null; // inverted point for translucent frame
    P3 ipt = null; // inversion center
    P3 ptref = null; // reflection center

    boolean isTranslation = (ang1 == 0);
    boolean isRotation = !isTranslation;
    boolean isInversionOnly = false;
    boolean isMirrorPlane = false;

    if (isRotation || haveInversion)
      trans = null;

    // handle inversion

    if (haveInversion && isTranslation) {

      // simple inversion operation

      ipt = P3.newP(pta00);
      ipt.add(pt0);
      ipt.scale(0.5f);
      ptinv = pt0;
      isInversionOnly = true;

    } else if (haveInversion) {

      /*
       * 
       * We must convert simple rotations to rotation-inversions; 2-screws to
       * planes and glide planes.
       * 
       * The idea here is that there is a relationship between the axis for a
       * simple or screw rotation of an inverted frame and one for a
       * rotation-inversion. The relationship involves two adjacent equilateral
       * triangles:
       * 
       * 
       *       o 
       *      / \
       *     /   \    i'
       *    /     \ 
       *   /   i   \
       * A/_________\A' 
       *  \         / 
       *   \   j   / 
       *    \     / 
       *     \   / 
       *      \ / 
       *       x
       *      
       * Points i and j are at the centers of the triangles. Points A and A' are
       * the frame centers; an operation at point i, j, x, or o is taking A to
       * A'. Point i is 2/3 of the way from x to o. In addition, point j is half
       * way between i and x.
       * 
       * The question is this: Say you have an rotation/inversion taking A to
       * A'. The relationships are:
       * 
       * 6-fold screw x for inverted frame corresponds to 6-bar at i for actual
       * frame 3-fold screw i for inverted frame corresponds to 3-bar at x for
       * actual frame
       * 
       * The proof of this follows. Consider point x. Point x can transform A to
       * A' as a clockwise 6-fold screw axis. So, say we get that result for the
       * inverted frame. What we need for the real frame is a 6-bar axis
       * instead. Remember, though, that we inverted the frame at A to get this
       * result. The real frame isn't inverted. The 6-bar must do that inversion
       * AND also get the frame to point A' with the same (clockwise) rotation.
       * The key is to see that there is another axis -- at point i -- that does
       * the trick.
       * 
       * Take a look at the angles and distances that arise when you project A
       * through point i. The result is a frame at i'. Since the distance i-i'
       * is the same as i-A (and thus i-A') and the angle i'-i-A' is 60 degrees,
       * point i is also a 6-bar axis transforming A to A'.
       * 
       * Note that both the 6-fold screw axis at x and the 6-bar axis at i are
       * both clockwise.
       * 
       * Similar analysis shows that the 3-fold screw i corresponds to the 3-bar
       * axis at x.
       * 
       * So in each case we just calculate the vector i-j or x-o and then factor
       * appropriately.
       * 
       * The 4-fold case is simpler -- just a parallelogram.
       */

      V3 d = (pitch1 == 0 ? new V3() : ax1);
      float f = 0;
      switch (ang1) {
      case 60: // 6_1 at x to 6-bar at i
        f = 2f / 3f;
        break;
      case 120: // 3_1 at i to 3-bar at x
        f = 2;
        break;
      case 90: // 4_1 to 4-bar at opposite corner
        f = 1;
        break;
      case 180: // 2_1 to mirror plane
        // C2 with inversion is a mirror plane -- but could have a glide
        // component.
        ptref = P3.newP(pta00);
        ptref.add(d);
        pa1.scaleAdd2(0.5f, d, pta00);
        if (ptref.distance(pt0) > 0.1f) {
          trans = V3.newVsub(pt0, ptref);
          setFractional(uc, ptemp, trans, null);
          ftrans.setT(ptemp);
        } else {
          trans = null;
        }
        isRotation = false;
        haveInversion = false;
        isMirrorPlane = true;
      }
      if (f != 0) {
        // pa1 = pa1 + ((pt00 - pa1) + (p0 - (pa1 + d))) * f

        vtemp.sub2(pta00, pa1);
        vtemp.add(pt0);
        vtemp.sub(pa1);
        vtemp.sub(d);
        vtemp.scale(f);
        pa1.add(vtemp);
        ipt = new P3();
        ipt.scaleAdd2(0.5f, d, pa1);
        ptinv = new P3();
        ptinv.scaleAdd2(-2, ipt, pta00);
        ptinv.scale(-1);
      }

    } else if (trans != null) {

      // get rid of unnecessary translations added to keep most operations
      // within cell 555

      ptemp.setT(trans);
      uc.toFractional(ptemp, false);
      if (approxF(ptemp.x) == 1) {
        ptemp.x = 0;
      }
      if (approxF(ptemp.y) == 1) {
        ptemp.y = 0;
      }
      if (approxF(ptemp.z) == 1) {
        ptemp.z = 0;
      }
      ftrans.setT(ptemp);
      uc.toCartesian(ptemp, false);
      trans.setT(ptemp);
    }

    // fix angle based on direction of axis

    int ang = ang1;
    approx0(ax1);

    if (isRotation) {

      P3 ptr = new P3();

      vtemp.setT(ax1);

      // draw the lines associated with a rotation

      int ang2 = ang1;
      if (haveInversion) {
        ptr.add2(pa1, vtemp);
        ang2 = Math.round(Measure.computeTorsion(ptinv, pa1, ptr, pt0, true));
      } else if (pitch1 == 0) {
        ptr.setT(pa1);
        ptemp.scaleAdd2(1, ptr, vtemp);
        ang2 = Math.round(Measure.computeTorsion(pta00, pa1, ptemp, pt0, true));
      } else {
        ptemp.add2(pa1, vtemp);
        ptr.scaleAdd2(0.5f, vtemp, pa1);
        ang2 = Math.round(Measure.computeTorsion(pta00, pa1, ptemp, pt0, true));
      }

      if (ang2 != 0)
        ang1 = ang2;
    }

    if (isRotation && !haveInversion && pitch1 == 0) {
      if (ax1.z < 0 || ax1.z == 0 && (ax1.y < 0 || ax1.y == 0 && ax1.x < 0)) {
        ax1.scale(-1);
        ang1 = -ang1;
      }
    }

    // time to get the description

    String info1 = "identity";

    if (isInversionOnly) {
      ptemp.setT(ipt);
      uc.toFractional(ptemp, false);
      info1 = "Ci: " + strCoord(ptemp);
    } else if (isRotation) {
      if (haveInversion) {
        info1 = "" + (360 / ang) + "-bar axis";
      } else if (pitch1 != 0) {
        info1 = "" + (360 / ang) + "-fold screw axis";
        ptemp.setT(ax1);
        uc.toFractional(ptemp, false);
        info1 += "|translation: " + strCoord(ptemp);
      } else {
        info1 = "C" + (360 / ang) + " axis";
      }
    } else if (trans != null) {
      String s = " " + strCoord(ftrans);
      if (isTranslation) {
        info1 = "translation:" + s;
      } else if (isMirrorPlane) {
        float fx = approxF(ftrans.x);
        float fy = approxF(ftrans.y);
        float fz = approxF(ftrans.z);
        s = " " + strCoord(ftrans);
        if (fx != 0 && fy != 0 && fz != 0) {
          if (Math.abs(fx) == Math.abs(fy)
              && Math.abs(fy) == Math.abs(fz))
            info1 = "d-";
          else
            info1 = "g-"; // see group 167
        } else if (fx != 0 && fy != 0 || fy != 0 && fz != 0 || fz != 0
            && fx != 0)
          info1 = "n-";
        else if (fx != 0)
          info1 = "a-";
        else if (fy != 0)
          info1 = "b-";
        else
          info1 = "c-";
        info1 += "glide plane|translation:" + s;
      }
    } else if (isMirrorPlane) {
      info1 = "mirror plane";
    }

    if (haveInversion && !isInversionOnly) {
      ptemp.setT(ipt);
      uc.toFractional(ptemp, false);
      info1 += "|inversion center at " + strCoord(ptemp);
    }
    if (haveCentering)
      info1 += "|centering " + strCoord(centering);

    if (timeReversal != 0)
      info1 += "|spin timeReversal " + (timeReversal == 1 ? "+1" : "-1");
    
    String cmds = null;
    String xyzNew = (isBio ? xyzOriginal : getXYZFromMatrix(this, false, false,
        false));

    // check for drawing

    SB draw1 = new SB();

    if (id != null) {

      String drawid;
      String opType = null;
      drawid = "\ndraw ID " + id + "_";

      // delete previous elements of this user-settable ID

      draw1 = new SB();
      draw1
          .append(
              ("// " + xyzOriginal + "|" + xyzNew + "|" + info1).replace('\n',
                  ' ')).append("\n").append(drawid).append("* delete");

      // draw the initial frame

      drawLine(draw1, drawid + "frame1X", 0.15f, pta00, pta01, "red");
      drawLine(draw1, drawid + "frame1Y", 0.15f, pta00, pta02, "green");
      drawLine(draw1, drawid + "frame1Z", 0.15f, pta00, pta03, "blue");

      String color;

      if (isRotation) {

        P3 ptr = new P3();

        color = "red";

        ang = ang1;
        float scale = 1.0f;
        vtemp.setT(ax1);

        // draw the lines associated with a rotation

        if (haveInversion) {
          opType = drawid + "rotinv";
          ptr.add2(pa1, vtemp);
          if (pitch1 == 0) {
            ptr.setT(ipt);
            vtemp.scale(3);
            ptemp.scaleAdd2(-1, vtemp, pa1);
            draw1.append(drawid).append("rotVector2 diameter 0.1 ")
                .append(Escape.eP(pa1)).append(Escape.eP(ptemp))
                .append(" color red");
          }
          scale = pt0.distance(ptr);
          draw1.append(drawid).append("rotLine1 ").append(Escape.eP(ptr))
              .append(Escape.eP(ptinv)).append(" color red");
          draw1.append(drawid).append("rotLine2 ").append(Escape.eP(ptr))
              .append(Escape.eP(pt0)).append(" color red");
        } else if (pitch1 == 0) {
          opType = drawid + "rot";
          boolean isSpecial = (pta00.distance(pt0) < 0.2f);
          if (!isSpecial) {
            draw1.append(drawid).append("rotLine1 ").append(Escape.eP(pta00))
                .append(Escape.eP(pa1)).append(" color red");
            draw1.append(drawid).append("rotLine2 ").append(Escape.eP(pt0))
                .append(Escape.eP(pa1)).append(" color red");
          }
          vtemp.scale(3);
          ptemp.scaleAdd2(-1, vtemp, pa1);
          draw1.append(drawid).append("rotVector2 diameter 0.1 ")
              .append(Escape.eP(pa1)).append(Escape.eP(ptemp))
              .append(" color red");
          ptr.setT(pa1);
          if (pitch1 == 0 && pta00.distance(pt0) < 0.2)
            ptr.scaleAdd2(0.5f, ptr, vtemp);
        } else {
          opType = drawid + "screw";
          color = "orange";
          draw1.append(drawid).append("rotLine1 ").append(Escape.eP(pta00))
              .append(Escape.eP(pa1)).append(" color red");
          ptemp.add2(pa1, vtemp);
          draw1.append(drawid).append("rotLine2 ").append(Escape.eP(pt0))
              .append(Escape.eP(ptemp)).append(" color red");
          ptr.scaleAdd2(0.5f, vtemp, pa1);
        }

        // draw arc arrow

        ptemp.add2(ptr, vtemp);
        if (haveInversion && pitch1 != 0) {
          draw1.append(drawid).append("rotRotLine1").append(Escape.eP(ptr))
              .append(Escape.eP(ptinv)).append(" color red");
          draw1.append(drawid).append("rotRotLine2").append(Escape.eP(ptr))
              .append(Escape.eP(pt0)).append(" color red");
        }
        draw1.append(drawid)
            .append("rotRotArrow arrow width 0.10 scale " + scale + " arc ")
            .append(Escape.eP(ptr)).append(Escape.eP(ptemp));
        ptemp.setT(haveInversion ? ptinv : pta00);
        if (ptemp.distance(pt0) < 0.1f)
          ptemp.set((float) Math.random(), (float) Math.random(),
              (float) Math.random());
        draw1.append(Escape.eP(ptemp));
        ptemp.set(0, ang, 0);
        draw1.append(Escape.eP(ptemp)).append(" color red");
        // draw the main vector

        draw1.append(drawid).append("rotVector1 vector diameter 0.1 ")
            .append(Escape.eP(pa1)).append(Escape.eP(vtemp)).append("color ")
            .append(color);

      }

      if (isMirrorPlane) {

        // indigo arrow across plane from pt00 to pt0

        if (pta00.distance(ptref) > 0.2)
          draw1.append(drawid).append("planeVector arrow ")
              .append(Escape.eP(pta00)).append(Escape.eP(ptref))
              .append(" color indigo");

        // faint inverted frame if mirror trans is not null

        opType = drawid + "plane";
        if (trans != null) {
          drawFrameLine("X", ptref, vt1, 0.15f, ptemp, draw1, opType, "red");
          drawFrameLine("Y", ptref, vt2, 0.15f, ptemp, draw1, opType, "green");
          drawFrameLine("Z", ptref, vt3, 0.15f, ptemp, draw1, opType, "blue");
          opType = drawid + "glide";
        }

        color = (trans == null ? "green" : "blue");

        // ok, now HERE's a good trick. We use the Marching Cubes
        // algorithm to find the intersection points of a plane and the unit
        // cell.
        // We expand the unit cell by 5% in all directions just so we are
        // guaranteed to get cutoffs.

        vtemp.setT(ax1);
        vtemp.normalize();
        // ax + by + cz + d = 0
        // so if a point is in the plane, then N dot X = -d
        float w = -vtemp.x * pa1.x - vtemp.y * pa1.y - vtemp.z * pa1.z;
        P4 plane = P4.new4(vtemp.x, vtemp.y, vtemp.z, w);
        Lst<Object> v = new Lst<Object>();
        float margin = 1.05f;//(Math.abs(w) < 0.01f && vtemp.x * vtemp.y < 0 ? 1.30f : 1.05f);
        v.addLast(uc.getCanonicalCopy(margin, false));
        modelSet.intersectPlane(plane, v, 3);

        // returns triangles and lines
        for (int i = v.size(); --i >= 0;) {
          P3[] pts = (P3[]) v.get(i);
          draw1.append(drawid).append("planep").appendI(i).append(" ")
              .append(Escape.eP(pts[0])).append(Escape.eP(pts[1]));
          if (pts.length == 3)
            draw1.append(Escape.eP(pts[2]));
          draw1.append(" color translucent ").append(color);
        }

        // and JUST in case that does not work, at least draw a circle

        if (v.size() == 0) {
          ptemp.add2(pa1, ax1);
          draw1.append(drawid).append("planeCircle scale 2.0 circle ")
              .append(Escape.eP(pa1)).append(Escape.eP(ptemp))
              .append(" color translucent ").append(color).append(" mesh fill");
        }
      }

      if (haveInversion) {

        opType = drawid + "inv";

        // draw a faint frame showing the inversion

        draw1.append(drawid).append("invPoint diameter 0.4 ")
            .append(Escape.eP(ipt));
        draw1.append(drawid).append("invArrow arrow ").append(Escape.eP(pta00))
            .append(Escape.eP(ptinv)).append(" color indigo");
        if (!isInversionOnly && !haveCentering) {
          drawFrameLine("X", ptinv, vt1, 0.15f, ptemp, draw1, opType, "red");
          drawFrameLine("Y", ptinv, vt2, 0.15f, ptemp, draw1, opType, "green");
          drawFrameLine("Z", ptinv, vt3, 0.15f, ptemp, draw1, opType, "blue");
        }
      }

      // and display translation if still not {0 0 0}

      if (trans != null) {
        if (ptref == null)
          ptref = P3.newP(pta00);
        draw1.append(drawid).append("transVector vector ")
            .append(Escape.eP(ptref)).append(Escape.eP(trans));
      }

      // draw the centering frame and arrow

      if (haveCentering) {
        if (opType != null) {
          drawFrameLine("X", pt0, vt1, 0.15f, ptemp, draw1, opType, "red");
          drawFrameLine("Y", pt0, vt2, 0.15f, ptemp, draw1, opType, "green");
          drawFrameLine("Z", pt0, vt3, 0.15f, ptemp, draw1, opType, "blue");
        }
        if (ptTarget == null) {
          ptTarget = ptemp;
          ptemp.add2(pt0, vcentering);
        }
        draw1.append(drawid).append("centeringVector arrow ")
            .append(Escape.eP(pt0)).append(Escape.eP(ptTarget))
            .append(" color cyan");

      }

      // draw the final frame just a bit fatter and shorter, in case they
      // overlap

      ptemp2.setT(pt0);
      if (haveCentering)
        ptemp2.add(vcentering);
      ptemp.sub2(pt1, pt0);
      ptemp.scaleAdd2(0.9f, ptemp, ptemp2);
      drawLine(draw1, drawid + "frame2X", 0.2f, ptemp2, ptemp, "red");
      ptemp.sub2(pt2, pt0);
      ptemp.scaleAdd2(0.9f, ptemp, ptemp2);
      drawLine(draw1, drawid + "frame2Y", 0.2f, ptemp2, ptemp, "green");
      ptemp.sub2(pt3, pt0);
      ptemp.scaleAdd2(0.9f, ptemp, ptemp2);
      drawLine(draw1, drawid + "frame2Z", 0.2f, ptemp2, ptemp, "purple");

      // color the targeted atoms opaque and add another frame if necessary

      draw1.append("\nvar pt00 = " + Escape.eP(pta00));
      draw1.append("\nsym_point = pt00");
      draw1.append("\nvar p0 = " + Escape.eP(ptemp2));
      draw1.append("\nvar set2 = within(0.2,p0);if(!set2){set2 = within(0.2,p0.uxyz.xyz)}");
      draw1.append("\nsym_target = set2;if (set2) {");
      //      if (haveCentering)
      //      draw1.append(drawid).append(
      //        "cellOffsetVector arrow @p0 @set2 color grey");
      draw1.append(drawid)
          .append("offsetFrameX diameter 0.20 @{set2.xyz} @{set2.xyz + ")
          .append(Escape.eP(vt1)).append("*0.9} color red");
      draw1.append(drawid)
          .append("offsetFrameY diameter 0.20 @{set2.xyz} @{set2.xyz + ")
          .append(Escape.eP(vt2)).append("*0.9} color green");
      draw1.append(drawid)
          .append("offsetFrameZ diameter 0.20 @{set2.xyz} @{set2.xyz + ")
          .append(Escape.eP(vt3)).append("*0.9} color purple");
      draw1.append("\n}}\n");
      cmds = draw1.toString();
      draw1 = null;
      drawid = null;
    }
    if (trans == null)
      ftrans = null;
    if (isRotation) {
      if (haveInversion) {
      } else if (pitch1 == 0) {
      } else {
        // screw
        trans = V3.newV(ax1);
        ptemp.setT(trans);
        uc.toFractional(ptemp, false);
        ftrans = V3.newV(ptemp);
      }
    }
    if (isMirrorPlane) {
      ang1 = 0;
    }
    if (haveInversion) {
      if (isInversionOnly) {
        pa1 = null;
        ax1 = null;
        trans = null;
        ftrans = null;
      }
    } else if (isTranslation) {
      pa1 = null;
      ax1 = null;
    }

    // and display translation if still not {0 0 0}
    if (ax1 != null)
      ax1.normalize();
    M4 m2 = null;
    m2 = M4.newM4(this);
    if (haveCentering)
      vtrans.add(centering);
    if (vtrans.length() != 0) {
      m2.m03 += vtrans.x;
      m2.m13 += vtrans.y;
      m2.m23 += vtrans.z;
    }
    xyzNew = (isBio ? m2.toString() : modDim > 0 ? xyzOriginal : getXYZFromMatrix(m2, false, false, false));
    if (timeReversal != 0)
      xyzNew  += (timeReversal == 1 ? ",m+1" : ",m-1");
    return new Object[] { xyzNew, xyzOriginal, info1, cmds, approx0(ftrans),
        approx0(trans), approx0(ipt), approx0(pa1), approx0(ax1),
        Integer.valueOf(ang1), m2, vtrans, centering };
  }

  /**
   * Set pt01 to pt00, possibly adding offset into unit cell 
   * @param uc
   * @param pt01
   * @param pt00
   * @param offset
   */
  private static void setFractional(SymmetryInterface uc, P3 pt01, T3 pt00, P3 offset) {
    pt01.setT(pt00);
    if (offset != null)
      uc.toUnitCell(pt01, offset);
    uc.toFractional(pt01, false);
  }

  private void drawFrameLine(String xyz, P3 pt, V3 v,
                           float width, P3 ptemp, SB draw1,
                           String key, String color) {
    ptemp.setT(pt);
    ptemp.add(v);
    drawLine(draw1, key + "Frame" + xyz, width, pt, ptemp, "translucent " + color);
  }

  private P3 rotTransCart(P3 pt00, SymmetryInterface uc, V3 vtrans) {
    P3 p0 = P3.newP(pt00);
    uc.toFractional(p0, false);
    rotTrans2(p0, p0);
    p0.add(vtrans);
    uc.toCartesian(p0, false);
    return p0;
  }

  private String strCoord(T3 p) {
    approx0(p);
    return (isBio ? p.x + " " + p.y + " " + p.z : fcoord(p));
  }

  private static void drawLine(SB s, String id, float diameter, P3 pt0, P3 pt1,
                        String color) {
    s.append(id).append(" diameter ").appendF(diameter)
        .append(Escape.eP(pt0)).append(Escape.eP(pt1))
        .append(" color ").append(color);
  }

  static String fcoord(T3 p) {
    return fc(p.x) + " " + fc(p.y) + " " + fc(p.z);
  }

  private static String fc(float x) {
    float xabs = Math.abs(x);
    int x24 = (int) approxF(xabs * 24);
    String m = (x < 0 ? "-" : "");
    if (x24%8 != 0)
      return m + twelfthsOf(x24 >> 1);
    return (x24 == 0 ? "0" : x24 == 24 ? m + "1" : m + (x24/8) + "/3");
  }

  private static T3 approx0(T3 pt) {
    if (pt != null) {
      if (Math.abs(pt.x) < 0.0001f)
        pt.x = 0;
      if (Math.abs(pt.y) < 0.0001f)
        pt.y = 0;
      if (Math.abs(pt.z) < 0.0001f)
        pt.z = 0;
    }
    return pt;
  }
  
  private static T3 approx(T3 pt) {
    if (pt != null) {
      pt.x = approxF(pt.x);
      pt.y = approxF(pt.y);
      pt.z = approxF(pt.z);
    }
    return pt;
  }
  
  private static float approxF(float f) {
    return PT.approx(f, 100);
  }

  public static void normalizeTranslation(M4 operation) {
    operation.m03 = ((int)operation.m03 + 12) % 12;
    operation.m13 = ((int)operation.m13 + 12) % 12;
    operation.m23 = ((int)operation.m23 + 12) % 12;    
  }

  public static String getXYZFromRsVs(Matrix rs, Matrix vs, boolean is12ths) {
    double[][] ra = rs.getArray();
    double[][] va = vs.getArray();
    int d = ra.length;
    String s = "";
    for (int i = 0; i < d; i++) {
      s += ",";
      for (int j = 0; j < d; j++) {
        double r = ra[i][j];
        if (r != 0) {
          s += (r < 0 ? "-" : s.endsWith(",") ? "" : "+") + (Math.abs(r) == 1 ? "" : "" + (int) Math.abs(r)) + "x" + (j + 1);
        }
      }
      s += xyzFraction((int) (va[i][0] * (is12ths ? 1 : 12)), false, true);
    }
    return PT.rep(s.substring(1), ",+", ",");
  }

  @Override
  public String toString() {
    return (rsvs == null ? super.toString() : super.toString() + " " + rsvs.toString());
  }

  float magOp = Float.MAX_VALUE;
  boolean isCenteringOp;
  private boolean unCentered;
  public float getSpinOp() {
    if (magOp == Float.MAX_VALUE)
      magOp = determinant3() * timeReversal;
    //System.out.println("sym op " + index + " " + xyz + " has tr " + timeReversal + " and magop " + magOp);
    return magOp;
  }

  public void setTimeReversal(int magRev) {
    timeReversal = magRev;
    if (xyz.indexOf(",m") >= 0)
      xyz = xyz.substring(0, xyz.indexOf(",m"));
    xyz += (magRev == 1 ? ",m+1" : ",m-1");
  }

  public static String cleanMatrix(M4 m4) {
    SB sb = new SB();
    sb.append("[ ");
    float[] row = new float[4];
    for (int i = 0; i < 3; i++) {
      m4.getRow(i, row);
      sb.append("[ ")
        .appendI((int)row[0]).append(" ")
        .appendI((int)row[1]).append(" ")
        .appendI((int)row[2]).append(" ");      
      sb.append(twelfthsOf(row[3]*12)).append(" ]");
    }
    return sb.append(" ]").toString();
  }

  /**
   * assumption here is that these are in order of sets, as in ITA
   * 
   * @param c
   * @param isFinal
   *        TODO
   * @return centering
   */
  public V3 setCentering(V3 c, boolean isFinal) {
    if (centering == null && !unCentered) {
      if (modDim == 0 && index > 1 && m00 == 1 && m11 == 1 && m22 == 1
          && m01 == 0 && m02 == 0 && m10 == 0 && m12 == 0 && m20 == 0
          && m21 == 0) {
        centering = V3.new3(m03, m13, m23);
        if (centering.lengthSquared() == 0) {
          unCentered = true;
          centering = null;
        } else if (!isFinal)
          centering.scale(1 / 12f);
        isCenteringOp = true;
      } else {
        centering = c;
      }
    }
    return centering;
  }
  
}
