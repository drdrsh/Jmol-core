/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-09-30 10:16:53 -0500 (Sat, 30 Sep 2006) $
 * $Revision: 5778 $
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
package org.jmol.adapter.readers.xtal;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.adapter.smarter.AtomSetCollectionReader;
import org.jmol.adapter.smarter.Atom;
import org.jmol.adapter.smarter.Bond;
import org.jmol.util.Logger;

import javajs.util.M3;
import javajs.util.P3;
import javajs.util.V3;

/**
 * A reader for TOPOS systre file Crystal Graph Data format.
 * 
 *  http://www.topos.samsu.ru/manuals.html
 * 
 * 
 */

public class CgdReader extends AtomSetCollectionReader {

  @Override
  public void initializeReader() {
    setFractionalCoordinates(true);
    asc.setNoAutoBond();
    asc.vibScale = 1;
  }

  private String[] tokens;
  private Map<Atom, V3[]> htEdges;

  @Override
  protected boolean checkLine() throws Exception {
    tokens = getTokens();
    if (tokens.length > 0) {
      int pt = "NAME |CELL |GROUP|ATOM |EDGE |"
          .indexOf(tokens[0].toUpperCase());
      if (pt == 0 || doProcessLines)
        switch (pt) {
        //       0     6     12    18    24
        case 0:
          if (!doGetModel(++modelNumber, null))
            return checkLastModel();
          applySymmetryAndSetTrajectory();
          setFractionalCoordinates(true);
          asc.newAtomSet();
          asc.setAtomSetName(line.substring(6).trim());
          break;
        case 6:
          //cell 1.1548 1.1548 1.1548 90.000 90.000 90.000
          for (int i = 0; i < 6; i++)
            setUnitCellItem(i, (i < 3 ? 10 : 1) * parseFloatStr(tokens[i + 1]));
          break;
        case 12:
          setSpaceGroupName("bilbao:" + tokens[1]);
          break;
        case 18:
          atom();
          break;
        case 24:
          edges();
          break;
        }
    }
    return true;
  }

  private void atom() {
    String name = "C" + tokens[1];
    Atom a = addAtomXYZSymName(tokens, 3, "C", name);
    asc.atomSymbolicMap.put(name, a);
    asc.addVibrationVector(a.index, 1f, 3f, 7f);
    int edgeCount = parseIntStr(tokens[2]);
    if (htEdges == null)
      htEdges = new Hashtable<Atom, V3[]>();
    htEdges.put(a, new V3[edgeCount]);
  }

  private void edges() throws Exception {
    V3[] atomEdges = htEdges.get(asc.getAtomFromName("C" + tokens[1]));
    for (int i = 0, n = atomEdges.length; i < n; i++) {
      if (i > 0) {
        while (rd().length() == 0)
          rd();
        tokens = getTokens();
      }
      atomEdges[i] = V3.new3(parseFloatStr(tokens[2]),
          parseFloatStr(tokens[3]), parseFloatStr(tokens[4]));
    }
  }
  
  @Override
  public void finalizeSubclassReader() throws Exception {
    finalizeReaderASCR();
    finalizeNet();
  }

  // a.vib holds {1 3 7}, corresponding to 
  // x, y, and z and allowing for 
  // x-y (-2) and y-x (2)
  // x-z (-6) and z-x (6)
  // y-z (-4) and z-y (4)

  private final static V3[] vecs = new V3[] {
    V3.new3(0, 0, -1), // -z   -7
    V3.new3(1, 0, -1), //  x-z -6
    null,
    V3.new3(0, 1, -1), //  y-z -4
    V3.new3(0, -1, 0), // -y   -3
    V3.new3(1, -1, 0), //  x-y -2
    V3.new3(-1, 0, 0), // -x   -1
    null,
    V3.new3(1, 0, 0),  //  x    1
    V3.new3(-1, 1, 0), //  y-x  2    
    V3.new3(0, 1, 0),  //  y    3
    V3.new3(0, -1, 1), //  z-y  4
    null,
    V3.new3(-1, 0, 1), //  z-x  6
    V3.new3(0, 0, 1)   //  z    7
  };
  private void finalizeNet() {
    // atom vibration vector has been rotated and now gives us the needed orientations for the edges. 
    // could be a bit slow without partition. Let's see...
    M3 m = new M3();
    P3 pt = new P3();
    for (int i = 0, n = asc.ac; i < n; i++) {
      Atom a = asc.atoms[i];
      Atom a0 = asc.atoms[a.atomSite];
      V3[] edges = htEdges.get(a0);
      if (edges == null)
        continue;
      int ix = (int) a.vib.x + 7;
      int iy = (int) a.vib.y + 7;
      int iz = (int) a.vib.z + 7;
      // ix, iy, iz now range from 0 to 13
      m.setRowV(0, vecs[ix]);
      m.setRowV(1, vecs[iy]);
      m.setRowV(2, vecs[iz]);
      for (int j = 0, n1 = edges.length; j < n1; j++) {
        pt.sub2(edges[j], a0);
        m.rotate(pt);
        pt.add(a);
        Atom b = findAtom(pt);
        if (b != null)
          asc.addBond(new Bond(a.index, b.index, 1));
        else if (pt.x >= 0 && pt.x <= 1 
            && pt.y >= 0 && pt.y <= 1
            && pt.z >= 0 && pt.z <= 1)
          Logger.error(" not found: i=" + i +"  pt="+pt + " for a=" + a +  "\n a0=" + a0 + " edge["+j+"]=" + edges[j] + "\n a.vib="+a.vib+"\n m=" + m);
      }
      a.vib = null;
    }

  }

  private Atom findAtom(P3 pt) {
    for (int i = asc.ac; --i >= 0;)
      if (asc.atoms[i].distanceSquared(pt) < 0.00001f)
        return asc.atoms[i];
    return null;
  }

  
}
