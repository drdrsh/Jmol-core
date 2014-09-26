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

import javajs.util.M3;
import javajs.util.P3;
import javajs.util.PT;
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
  private Atom lastAtom;
  private int edgeCount;
  private Map<Atom, V3[]> htEdges;
  private V3[] atomEdges;

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
            setUnitCellItem(i, parseFloatStr(tokens[i + 1]));
          break;
        case 12:
          setSpaceGroupName(tokens[1]);
          break;
        case 18:
          lastAtom = addAtomXYZSymName(tokens, 3, "C", null);
          asc.addVibrationVector(lastAtom.index, 1f, 2f, 3f);
          edgeCount = parseIntStr(tokens[2]);
          atomEdges = new V3[edgeCount];
          if (htEdges == null)
            htEdges = new Hashtable<Atom, V3[]>();
          break;
        case 24:
          edges();
          break;
        }
    }
    return true;
  }

  @Override
  public void finalizeSubclassReader() throws Exception {
    finalizeReaderASCR();
    finalizeNet();
  }

  private final static V3[] vecs = new V3[] {
    V3.new3(-1, 0, 0),
    V3.new3(0, -1, 0),
    V3.new3(0, 0, -1),
    null,
    V3.new3(1, 0, 0),
    V3.new3(0, 1, 0),
    V3.new3(0, 0, 1)
  };
  private void finalizeNet() {
    // atom vibration vector has been rotated and now gives us the needed orientations for the edges. 
    // could be a bit slow without partition. Let's see...
    M3 m = new M3();
    P3 pt = new P3();
    for (int i = asc.ac; --i >= 0;) {
      Atom a = asc.atoms[i];
      V3[] edges = htEdges.get(asc.atoms[a.atomSite]);
      if (edges == null)
        continue;
      int ix = (int) a.vib.x + 3;
      int iy = (int) a.vib.y + 3;
      int iz = (int) a.vib.z + 3;
      // ix, iy, iz now range from 0 to 6 (not 3)
      m.setColumnV(0, vecs[ix]);
      m.setColumnV(1, vecs[iy]);
      m.setColumnV(2, vecs[iz]);
      a.vib = null;
      for (int j = edges.length; --j >= 0;){
        pt.sub2(edges[j], asc.atoms[a.atomSite]);
        m.rotate(pt);
        pt.add(a);
        Atom b = findAtom(pt);
        if (b != null)
          asc.addBond(new Bond(a.index, b.index, 1));
      }
    }

  }

  private Atom findAtom(P3 pt) {
    for (int i = asc.ac; --i >= 0;)
      if (asc.atoms[i].distanceSquared(pt) < 0.05f)
        return asc.atoms[i];
    return null;
  }

  private void edges() throws Exception {
    for (int i = 0; i < edgeCount; i++) {
      if (i > 0) {
        while (rd().length() == 0)
          rd();
        tokens = getTokens();
      }
      atomEdges[i] = V3.new3(parseFloatStr(tokens[2]),
          parseFloatStr(tokens[3]), parseFloatStr(tokens[4]));
    }
    htEdges.put(lastAtom, atomEdges);
  }
  
  
}
