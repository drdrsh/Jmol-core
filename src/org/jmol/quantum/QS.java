/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.quantum;

import java.util.Map;

import javajs.util.Lst;
import javajs.util.SB;


/**
 * Constants and static methods for quantum shells.
 */
public class QS {

  public QS() {
    //
  }
  final public static int S = 0;
  final public static int P = 1;
  final public static int SP = 2;
  final public static int DS = 3;
  final public static int DC = 4;
  final public static int FS = 5;
  final public static int FC = 6;
  final public static int GS = 7;
  final public static int GC = 8;
  final public static int HS = 9;
  final public static int HC = 10;
  final public static int IS = 11;
  final public static int IC = 12;
  
  private final static int MAXID = 13;

  final public static int[] idSpherical = { S,   P,   SP,   DS,   DS,  FS,   FS,  GS,   GS,  HS,    HS,  IS,    IS};
  final public static String[] tags =     {"S", "P", "SP", "5D", "D", "7F", "F", "9G", "G", "11H", "H", "13I", "I"};
  final public static String[] tags2 =    {"S", "X", "SP", "5D", "XX", "7F", "XXX", "9G", "XXXX", "11H", "XXXXX", "13I", "XXXXXX"};
  
//  S("S","S",0,0),
//  P("P","X",1,1),
//  SP("SP","SP",2,2),
//  D_SPHERICAL("5D","5D",3,3),
//  D_CARTESIAN("D","XX",4,3),
//  F_SPHERICAL("7F","7F",5,5),
//  F_CARTESIAN("F","XXX",6,5),
//  G_SPHERICAL("9G","9G",7,7),
//  G_CARTESIAN("G","XXXX",8,7),
//  H_SPHERICAL("11H","11H",9,9),
//  H_CARTESIAN("H","XXXXX",10,9),
//  I_SPHERICAL("13I","13I",11,11),
//  I_CARTESIAN("I","XXXXXX",12,11);

  public static boolean isQuantumBasisSupported(char ch) {
    return ("SPLDF".indexOf(Character.toUpperCase(ch)) >= 0);
  }

  public static int[][] getNewDfCoefMap() {
    return new int[][] { 
        new int[1],  //S 0
        new int[3],  //P
        new int[4],  //SP
        new int[5],  //D5 3
        new int[6],  //D6 
        new int[7],  //F7 5
        new int[10], //F10
        new int[9],  //G9 7
        new int[15], //G15
        new int[11], //H11 9 == 2*5 + 1
        new int[21], //H21 == (5+1)(5+2)/2
        new int[13], //I13 == 2*6 + 1
        new int[28]  //I28 == (6+1)(6+2)/2
    };
  }

  public static int getItem(int i) {
    return (i >= 0 && i < MAXID ? i : -1);
  }

  public static int getQuantumShellTagID(String tag) {
    if (tag.equals("L"))
      return SP;
    return getQuantumShell(tag);
  }

  private static int getQuantumShell(String tag) {
    for (int i = 0; i < MAXID; i++)
      if (tags[i].equals(tag) || tags2[i].equals(tag))
        return i;
    return -1;
  }

  final public static int getQuantumShellTagIDSpherical(String tag) {
    if (tag.equals("L"))
      return SP;
    int id = getQuantumShell(tag);
    return (id < 0 ? id : idSpherical[id]);
  }
  
  final public static String getQuantumShellTag(int id) {
    return (id >= 0 && id < MAXID ? tags[id] : "" + id);
  }

  final public static String getMOString(float[] lc) {
    SB sb = new SB();
    if (lc.length == 2)
      return "" + (int)(lc[0] < 0 ? -lc[1] : lc[1]);
    sb.appendC('[');
    for (int i = 0; i < lc.length; i += 2) {
      if (i > 0)
        sb.append(", ");
      sb.appendF(lc[i]).append(" ").appendI((int) lc[i + 1]);
    }
    sb.appendC(']');
    return sb.toString();
  }

  public void setNboLabels(String[] tokens, int nLabels,
                                  Lst<Map<String, Object>> orbitals, int nOrbitals0, String moType) {
    for (int i = 0; i < tokens.length; i += nLabels + 2)
      if (moType.indexOf(tokens[i]) >= 0) {
        for (int j = 0; j < nLabels; j++) {
          Map<String, Object> mo = orbitals.get(j + nOrbitals0);
          String type = tokens[i + j + 2];
          mo.put("type", moType + " " + type);
          // TODO: does not account for SOMO
          mo.put("occupancy", Float.valueOf(type.indexOf("*") >= 0
              || type.indexOf("(ry)") >= 0 ? 0 : 2));
        }
        return;
      }
  }

}
