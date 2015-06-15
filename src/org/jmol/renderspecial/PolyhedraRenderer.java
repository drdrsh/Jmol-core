/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-12 11:05:36 -0500 (Mon, 12 Mar 2007) $
 * $Revision: 7077 $
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
package org.jmol.renderspecial;


import org.jmol.modelset.Atom;
import org.jmol.render.ShapeRenderer;
import org.jmol.script.T;
import org.jmol.shapespecial.Polyhedra;
import org.jmol.shapespecial.Polyhedron;
import org.jmol.util.C;
import org.jmol.util.GData;

import javajs.util.P3;
import javajs.util.P3i;

public class PolyhedraRenderer extends ShapeRenderer {

  private int drawEdges;
  private boolean isAll;
  private boolean frontOnly;
  private P3[] screens3f;
  private P3i scrVib;
  private boolean vibs;

  @Override
  protected boolean render() {
    Polyhedra polyhedra = (Polyhedra) shape;
    Polyhedron[] polyhedrons = polyhedra.polyhedrons;
    drawEdges = polyhedra.drawEdges;
    g3d.addRenderer(T.triangles);
    vibs = (ms.vibrations != null && tm.vibrationOn);
    boolean needTranslucent = false;
    for (int i = polyhedra.polyhedronCount; --i >= 0;) 
      if (polyhedrons[i].isValid && render1(polyhedrons[i]))
        needTranslucent = true;
    return needTranslucent;
  }

  private boolean render1(Polyhedron p) {
    if (p.visibilityFlags == 0)
      return false;
    short[] colixes = ((Polyhedra) shape).colixes;
    int iAtom = p.centralAtom.i;
    short colix = (colixes == null || iAtom >= colixes.length ? C.INHERIT_ALL
        : colixes[iAtom]);
    colix = C.getColixInherited(colix, p.centralAtom.colixAtom);
    boolean needTranslucent = false;
    if (C.renderPass2(colix)) {
      needTranslucent = true;
    } else if (!g3d.setC(colix)) {
      return false;
    }
    P3[] vertices = p.points;
    if (screens3f == null || screens3f.length < vertices.length) {
      screens3f = new P3[vertices.length];
      for (int i = vertices.length; --i >= 0;)
        screens3f[i] = new P3();
    }
    P3[] sc = this.screens3f;
    int[][] planes = p.planes;
    for (int i = vertices.length; --i >= 0;) {
      Atom atom = (vertices[i] instanceof Atom ? (Atom) vertices[i] : null);
      if (atom == null) {
        tm.transformPtScrT3(vertices[i], sc[i]);
      } else if (atom.isVisible(myVisibilityFlag)) {
        sc[i].set(atom.sX, atom.sY, atom.sZ);
      } else if (vibs && atom.hasVibration()) {
        scrVib = tm.transformPtVib(atom, ms.vibrations[atom.i]);
        sc[i].set(scrVib.x, scrVib.y, scrVib.z);
      } else {
        tm.transformPt3f(atom, sc[i]);
      }
    }

    isAll = (drawEdges == Polyhedra.EDGES_ALL);
    frontOnly = (drawEdges == Polyhedra.EDGES_FRONT);

    // no edges to new points when not collapsed
       // int m = (int) ( Math.random() * 16);
    if (!needTranslucent || g3d.setC(colix))
      for (int i = planes.length; --i >= 0;) {
        int[] pl = planes[i];
        //if (i != m)continue;
        //if (p.normixes[i] == 206 || p.normixes[i] == 226)continue;
        //System.out.println("pr " + p.normixes[i]);
        try {
        g3d.fillTriangleTwoSided(p.normixes[i], sc[pl[0]], sc[pl[1]], sc[pl[2]]);
        } catch (Exception e){
          System.out.println("heorhe");
        }
        if (pl[3] >= 0)
          g3d.fillTriangleTwoSided(p.normixes[i], sc[pl[2]], sc[pl[3]], sc[pl[0]]);          
      }
    // edges are not drawn translucently ever
    if (p.colixEdge != C.INHERIT_ALL)
      colix = p.colixEdge;
    if (g3d.setC(C.getColixTranslucent3(colix, false, 0)))
      for (int i = planes.length; --i >= 0;) {
        int[] pl = planes[i];
        if (pl[3] < 0) {
          drawFace(p.normixes[i], sc[pl[0]], sc[pl[1]], sc[pl[2]], -pl[3]);
        } else {
          drawFace(p.normixes[i], sc[pl[0]], sc[pl[1]], sc[pl[2]], 3);
          drawFace(p.normixes[i], sc[pl[0]], sc[pl[2]], sc[pl[3]], 6);          
        }
          
      }
    return needTranslucent;
  }

  private void drawFace(short normix, P3 a, P3 b, P3 c, int edgeMask) {
    if (isAll || frontOnly && vwr.gdata.isDirectedTowardsCamera(normix)) {
      int d = (g3d.isAntialiased() ? 6 : 3);
      if ((edgeMask & 1) == 1)
        g3d.fillCylinderBits(GData.ENDCAPS_SPHERICAL, d, a, b);
      if ((edgeMask & 2) == 2)
        g3d.fillCylinderBits(GData.ENDCAPS_SPHERICAL, d, b, c);
      if ((edgeMask & 4) == 4)
        g3d.fillCylinderBits(GData.ENDCAPS_SPHERICAL, d, a, c);
    }
  }


}
