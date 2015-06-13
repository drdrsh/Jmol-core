/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-29 04:39:40 -0500 (Thu, 29 Mar 2007) $
 * $Revision: 7248 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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

package org.jmol.shapespecial;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.AtomIndexIterator;
import org.jmol.c.PAL;
import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.script.T;
import org.jmol.shape.AtomShape;
import org.jmol.util.C;
import org.jmol.util.Logger;
import org.jmol.util.Normix;

import javajs.util.AU;
import javajs.util.Measure;
import javajs.util.P4;
import javajs.util.SB;
import javajs.util.P3;
import javajs.util.P3i;
import javajs.util.V3;



public class Polyhedra extends AtomShape {

  private final static float DEFAULT_DISTANCE_FACTOR = 1.85f;
  private final static float DEFAULT_MANY_VERTEX_DISTANCE_FACTOR = 1.5f;
  private final static float DEFAULT_FACECENTEROFFSET = 0.25f;
  private final static int EDGES_NONE = 0;
  public final static int EDGES_ALL = 1;
  public final static int EDGES_FRONT = 2;
  private final static int MAX_VERTICES = 250;
  private final static int FACE_COUNT_MAX = MAX_VERTICES - 3;
  private P3[] otherAtoms = new P3[MAX_VERTICES + 1];
  private short[] normixesT = new short[MAX_VERTICES];
  private P3i[] planesT = new P3i[MAX_VERTICES];
  private final static P3 randomPoint = P3.new3(3141f, 2718f, 1414f);

  private BS bsTemp;

  
  public int polyhedronCount;
  public Polyhedron[] polyhedrons = new Polyhedron[32];
  public int drawEdges;

  private float radius;
  private int nVertices;

  float faceCenterOffset;
  float distanceFactor = Float.NaN;
  boolean isCollapsed;
  

  private boolean iHaveCenterBitSet;
  private boolean bondedOnly;
  private boolean haveBitSetVertices;

  private BS centers;
  private BS bsVertices;
  private BS bsVertexCount;

  @Override
  public void setProperty(String propertyName, Object value, BS bs) {

    if ("init" == propertyName) {
      faceCenterOffset = DEFAULT_FACECENTEROFFSET;
      distanceFactor = Float.NaN;
      radius = 0.0f;
      nVertices = 0;
      bsVertices = null;
      centers = null;
      bsVertexCount = new BS();
      bondedOnly = isCollapsed = iHaveCenterBitSet = false;
      haveBitSetVertices = false;
      if (Boolean.TRUE == value)
        drawEdges = EDGES_NONE;
      return;
    }

    if ("generate" == propertyName) {
      if (!iHaveCenterBitSet) {
        centers = bs;
        iHaveCenterBitSet = true;
      }
      deletePolyhedra();
      buildPolyhedra();
      return;
    }

    if ("collapsed" == propertyName) {
      isCollapsed = ((Boolean) value).booleanValue();
      return;
    }

    if ("nVertices" == propertyName) {
      nVertices = ((Integer) value).intValue();
      bsVertexCount.set(nVertices);
      return;
    }

    if ("centers" == propertyName) {
      centers = (BS) value;
      iHaveCenterBitSet = true;
      return;
    }

    if ("to" == propertyName) {
      bsVertices = (BS) value;
      return;
    }

    if ("toBitSet" == propertyName) {
      bsVertices = (BS) value;
      haveBitSetVertices = true;
      return;
    }

    if ("faceCenterOffset" == propertyName) {
      faceCenterOffset = ((Float) value).floatValue();
      return;
    }

    if ("distanceFactor" == propertyName) {
      // not a general user option
      distanceFactor = ((Float) value).floatValue();
      return;
    }

    if ("bonds" == propertyName) {
      bondedOnly = true;
      return;
    }

    if ("delete" == propertyName) {
      if (!iHaveCenterBitSet)
        centers = bs;
      deletePolyhedra();
      return;
    }
    if ("on" == propertyName) {
      if (!iHaveCenterBitSet)
        centers = bs;
      setVisible(true);
      return;
    }
    if ("off" == propertyName) {
      if (!iHaveCenterBitSet)
        centers = bs;
      setVisible(false);
      return;
    }
    if ("noedges" == propertyName) {
      drawEdges = EDGES_NONE;
      return;
    }
    if ("edges" == propertyName) {
      drawEdges = EDGES_ALL;
      return;
    }
    if ("frontedges" == propertyName) {
      drawEdges = EDGES_FRONT;
      return;
    }
    if (propertyName.indexOf("color") == 0) {
      // from polyhedra command, we may not be using the prior select
      // but from Color we need to identify the centers.
      bs = ("colorThis" == propertyName && iHaveCenterBitSet ? centers
          : andBitSet(bs));
      short colixEdge = ("colorPhase" == propertyName ? C
          .getColix(((Integer) ((Object[]) value)[0]).intValue())
          : C.INHERIT_ALL);
      for (int i = polyhedronCount; --i >= 0;)
        if (bs.get(polyhedrons[i].centralAtom.i))
          polyhedrons[i].colixEdge = colixEdge;
      if ("colorPhase" == propertyName)
        value = ((Object[]) value)[1];
      propertyName = "color";
      //allow super
    }

    if (propertyName.indexOf("translucency") == 0) {
      // from polyhedra command, we may not be using the prior select
      // but from Color we need to identify the centers.
      bs = ("translucentThis".equals(value) && iHaveCenterBitSet ? centers
          : andBitSet(bs));
      if (value.equals("translucentThis"))
        value = "translucent";
      //allow super
    }

    if ("token" == propertyName) {
      setLighting(((Integer) value).intValue() == T.fullylit, bs);
      return;
    }

    if ("radius" == propertyName) {
      radius = ((Float) value).floatValue();
      return;
    }

    if (propertyName == "deleteModelAtoms") {
      int modelIndex = ((int[]) ((Object[]) value)[2])[0];
      for (int i = polyhedronCount; --i >= 0;) {
        if (polyhedrons[i].modelIndex == modelIndex) {
          polyhedronCount--;
          polyhedrons = (Polyhedron[]) AU.deleteElements(polyhedrons, i, 1);
        } else if (polyhedrons[i].modelIndex > modelIndex) {
          polyhedrons[i].modelIndex--;
        }
      }
      //pass on to AtomShape
    }

    setPropAS(propertyName, value, bs);
  }

  private void setLighting(boolean isFullyLit, BS bs) {
    for (int i = polyhedronCount; --i >= 0;)
      if (bs.get(polyhedrons[i].centralAtom.i)) {
        short[] normixes = polyhedrons[i].normixes;
        polyhedrons[i].isFullyLit = isFullyLit;
        for (int j = normixes.length; --j >= 0;) {
          if (normixes[j] < 0 != isFullyLit)
            normixes[j] = (short) ~normixes[j];
        }
      }
  }

  private BS andBitSet(BS bs) {
    BS bsCenters = new BS();
    for (int i = polyhedronCount; --i >= 0;)
      bsCenters.set(polyhedrons[i].centralAtom.i);
    bsCenters.and(bs);
    return bsCenters;
  }

  private void deletePolyhedra() {
    int newCount = 0;
    byte pid = PAL.pidOf(null);
    for (int i = 0; i < polyhedronCount; ++i) {
      Polyhedron p = polyhedrons[i];
      int iAtom = p.centralAtom.i;
      if (centers.get(iAtom))
        setColixAndPalette(C.INHERIT_ALL, pid, iAtom);
      else
        polyhedrons[newCount++] = p;
    }
    for (int i = newCount; i < polyhedronCount; ++i)
      polyhedrons[i] = null;
    polyhedronCount = newCount;
  }

  private void setVisible(boolean visible) {
    for (int i = polyhedronCount; --i >= 0;) {
      Polyhedron p = polyhedrons[i];
      if (p != null && centers.get(p.centralAtom.i))
        p.visible = visible;
    }
  }

  private void buildPolyhedra() {
    boolean useBondAlgorithm = radius == 0 || bondedOnly;
    AtomIndexIterator iter = ms.getSelectedAtomIterator(null, false, false, false, false);
    for (int i = centers.nextSetBit(0); i >= 0; i = centers.nextSetBit(i + 1)) {
      Polyhedron p = (haveBitSetVertices ? constructBitSetPolyhedron(i)
          : useBondAlgorithm ? constructBondsPolyhedron(i)
              : constructRadiusPolyhedron(i, iter));
      if (p != null) {
        if (polyhedronCount == polyhedrons.length)
          polyhedrons = (Polyhedron[]) AU.doubleLength(polyhedrons);
        polyhedrons[polyhedronCount++] = p;
      }
      if (haveBitSetVertices)
        break;
    }
    iter.release();
  }

  private Polyhedron constructBondsPolyhedron(int atomIndex) {
    Atom atom = atoms[atomIndex];
    Bond[] bonds = atom.bonds;
    if (bonds == null)
      return null;
    int bondCount = 0;
    for (int i = bonds.length; --i >= 0;) {
      Bond bond = bonds[i];
      Atom otherAtom = bond.atom1 == atom ? bond.atom2 : bond.atom1;
      if (bsVertices != null && !bsVertices.get(otherAtom.i))
        continue;
      if (radius > 0f && bond.atom1.distance(bond.atom2) > radius)
        continue;
      otherAtoms[bondCount++] = otherAtom;
      if (bondCount == MAX_VERTICES)
        break;
    }
    return (bondCount < 3 || nVertices > 0 && !bsVertexCount.get(bondCount) ? null
        : validatePolyhedronNew(atom, bondCount, otherAtoms));
  }

  private Polyhedron constructBitSetPolyhedron(int atomIndex) {
    int otherAtomCount = 0;
    for (int i = bsVertices.nextSetBit(0); i >= 0; i = bsVertices
        .nextSetBit(i + 1))
      otherAtoms[otherAtomCount++] = atoms[i];
    return validatePolyhedronNew(atoms[atomIndex], otherAtomCount, otherAtoms);
  }

  private Polyhedron constructRadiusPolyhedron(int atomIndex,
                                               AtomIndexIterator iter) {
    Atom atom = atoms[atomIndex];
    int otherAtomCount = 0;
    vwr.setIteratorForAtom(iter, atomIndex, radius);
    while (iter.hasNext()) {
      Atom other = atoms[iter.next()];
      if (bsVertices != null && !bsVertices.get(other.i)
          || atom.distance(other) > radius)
        continue;
      if (other.altloc != atom.altloc && other.altloc != 0 && atom.altloc != 0)
        continue;
      if (otherAtomCount == MAX_VERTICES)
        break;
      otherAtoms[otherAtomCount++] = other;
    }
    return (otherAtomCount < 3 || nVertices > 0
        && !bsVertexCount.get(otherAtomCount) ? null : validatePolyhedronNew(
        atom, otherAtomCount, otherAtoms));
  }

  private Polyhedron validatePolyhedronNew(Atom centralAtom, int vertexCount,
                                           P3[] otherAtoms) {
    V3 normal = new V3();
    int planeCount = 0;
    int ipt = 0;
    int ptCenter = vertexCount;
    int nPoints = ptCenter + 1;
    float distMax = 0;
    float dAverage = 0;

    P3[] points = new P3[MAX_VERTICES * 3];
    points[ptCenter] = otherAtoms[ptCenter] = centralAtom;
    for (int i = 0; i < ptCenter; i++) {
      points[i] = otherAtoms[i];
      dAverage += points[ptCenter].distance(points[i]);
    }
    dAverage = dAverage / ptCenter;

    int nother1 = ptCenter - 1;
    int nother2 = ptCenter - 2;
    // for many-vertex polygons we reduce the  distance allowed to avoid through-polyhedron faces
    float factor = (!Float.isNaN(distanceFactor) ? distanceFactor
        : nother1 <= 6 ? DEFAULT_DISTANCE_FACTOR
            : DEFAULT_MANY_VERTEX_DISTANCE_FACTOR);
    BS bs = BS.newN(ptCenter);
    boolean isOK = (dAverage == 0);

    // here we are assuring that at least ONE face is drawn to 
    // all matching vertices

    while (!isOK && factor < 10.0f) {
      distMax = dAverage * factor;
      bs.setBits(0, ptCenter);
      for (int i = 0; i < nother2; i++)
        for (int j = i + 1; j < nother1; j++) {
          if (points[i].distance(points[j]) > distMax)
            continue;
          for (int k = j + 1; k < ptCenter; k++) {
            if (points[i].distance(points[k]) > distMax
                || points[j].distance(points[k]) > distMax)
              continue;
            bs.clear(i);
            bs.clear(j);
            bs.clear(k);
          }
        }
      isOK = true;
      for (int i = 0; i < ptCenter; i++)
        if (bs.get(i)) {
          isOK = false;
          factor *= 1.05f;
          if (Logger.debugging) {
            Logger.debug("Polyhedra distanceFactor for " + ptCenter
                + " atoms increased to " + factor + " in order to include "
                + ((Atom) otherAtoms[i]).getInfo());
          }
          break;
        }
    }

    /*  Start by defining a face to be when all three distances
     *  are < distanceFactor * (longest central) but if a vertex is missed, 
     *  then expand the range. The collapsed trick is to introduce 
     *  a "simple" atom near the center but not quite the center, 
     *  so that our planes on either side of the facet don't overlap. 
     *  We step out faceCenterOffset * normal from the center.
     *  
     *  Alan Hewat pointed out the issue of faces that CONTAIN the center --
     *  square planar, trigonal and square pyramids, see-saw. In these cases with no
     *  additional work, you get a brilliance effect when two faces are drawn over
     *  each other. The solution is to identify this sort of face and, if not collapsed,
     *  to cut them into smaller pieces and only draw them ONCE by producing a little
     *  catalog. This uses the Point3i().toString() method.
     *  
     *  For these special cases, then, we define a reference point just behind the plane
     */

    // produce face-centered catalog and facet-aligned catalog
    String faceCatalog = "";
    String facetCatalog = "";
    for (int i = 0; i < nother2; i++)
      for (int j = i + 1; j < nother1; j++)
        for (int k = j + 1; k < ptCenter; k++)
          if (isPlanar(points[i], points[j], points[k], points[ptCenter]))
            faceCatalog += faceId(i, j, k);
    for (int j = 0; j < nother1; j++)
      for (int k = j + 1; k < ptCenter; k++) {
        if (isAligned(points[j], points[k], points[ptCenter]))
          facetCatalog += faceId(j, k, -1);
      }
    P3 ptRef = new P3();
    // this next check for distance allows for bond AND distance constraints
    if (bsTemp == null)
      bsTemp = Normix.newVertexBitSet();
    P3i[] p = planesT;
    P4 plane = new P4();
    V3 vTemp = new V3();
    boolean collapsed = isCollapsed;
    float offset = faceCenterOffset;
    int fmax = FACE_COUNT_MAX;
    int vmax = MAX_VERTICES;
    P3 rpt = randomPoint;
    BS bsT = bsTemp;
    short[] n = normixesT;
    boolean doCheckPlane  = (nother1 > 5);
    for (int i = 0; i < nother2; i++)
      for (int j = i + 1; j < nother1; j++) {
        if (points[i].distance(points[j]) > distMax)
          continue;
        for (int k = j + 1; k < ptCenter; k++) {
          //System.out.println("checking poly " + i + " " + j + " " + k);
          //System.out.println("checking poly " + points[i] + " " + points[j] + " " + points[k]);

          if (points[i].distance(points[k]) > distMax
              || points[j].distance(points[k]) > distMax)
            continue;
          //System.out.println("checking poly " + i + " " + j + " " + k + " ok ");

          if (planeCount >= fmax) {
            Logger.error("Polyhedron error: maximum face(" + fmax
                + ") -- reduce RADIUS or DISTANCEFACTOR");
            return null;
          }
          if (nPoints >= vmax) {
            Logger.error("Polyhedron error: maximum vertex count("
                + vmax + ") -- reduce RADIUS");
            return null;
          }
          boolean isFlat = (faceCatalog.indexOf(faceId(i, j, k)) >= 0);
          // if center is on the face, then we need a different point to 
          // define the normal
          //System.out.println("# polyhedra\n");
          boolean isWindingOK = (isFlat ? Measure.getNormalFromCenter(
              rpt, points[i], points[j], points[k], false, normal)
              : Measure.getNormalFromCenter(points[ptCenter], points[i],
                  points[j], points[k], true, normal));
          normal.scale(collapsed && !isFlat ? offset : 0.001f);
          int nRef = nPoints;
          ptRef.setT(points[ptCenter]);
          if (collapsed && !isFlat) {
            points[nPoints] = P3.newP(points[ptCenter]);
            points[nPoints].add(normal);
            otherAtoms[nPoints] = points[nPoints];
          } else if (isFlat) {
            ptRef.sub(normal);
            nRef = ptCenter;
          }
          String facet;
          facet = faceId(i, j, -1);
          if (collapsed || isFlat && facetCatalog.indexOf(facet) < 0) {
            facetCatalog += facet;
            p[planeCount] = P3i.new3(isWindingOK ? i : j, isWindingOK ? j : i, nRef);
            Measure.getNormalFromCenter(points[k], points[i], points[j], ptRef,
                false, normal);
            n[planeCount++] = (isFlat ? Normix.get2SidedNormix(normal,
                bsT) : Normix.getNormixV(normal, bsT));
          }
          facet = faceId(i, k, -1);
          if (collapsed || isFlat && facetCatalog.indexOf(facet) < 0) {
            facetCatalog += facet;
            p[planeCount] = P3i.new3(isWindingOK ? i : k, nRef,isWindingOK ? k : i);
            Measure.getNormalFromCenter(points[j], points[i], ptRef, points[k],
                false, normal);
            n[planeCount++] = (isFlat ? Normix.get2SidedNormix(normal,
                bsT) : Normix.getNormixV(normal, bsT));
          }
          facet = faceId(j, k, -1);
          if (collapsed || isFlat && facetCatalog.indexOf(facet) < 0) {
            facetCatalog += facet;
            p[planeCount] = P3i.new3(nRef, isWindingOK ? j : k, isWindingOK ? k : j);
            Measure.getNormalFromCenter(points[i], ptRef, points[j], points[k],
                false, normal);
            n[planeCount++] = (isFlat ? Normix.get2SidedNormix(normal,
                bsT) : Normix.getNormixV(normal, bsT));
          }
          if (!isFlat) {
            if (collapsed) {
              nPoints++;
            } else {
              // finally, the standard face:
              p[planeCount] = P3i.new3(isWindingOK ? i : j, isWindingOK ? j : i, k);
              n[planeCount] = Normix.getNormixV(normal, bsT);
              if (!doCheckPlane || checkPlane(points, ptCenter, p, n, planeCount, plane, vTemp))
                planeCount++;
            }
          }
        }
      }
    
    
    if (Logger.debugging)
      Logger.info("planeCount=" + planeCount + " nPoints=" + nPoints);
    return new Polyhedron(centralAtom, ptCenter, nPoints, planeCount,
        otherAtoms, n, p, collapsed, offset, factor);
  }
  

  Map<Integer,String> htNormMap = new Hashtable<Integer, String>();


  private boolean checkPlane(P3[] points, int ptCenter, P3i[] planes, short[] normals, int index, P4 plane, V3 vNorm) {
    P3i pt = planes[index];
    Integer norm = Integer.valueOf(normals[index]);
    String list = htNormMap.get(norm);
    String key = "_" + pt.x + "_" + pt.y + "_" + pt.z + "_" + pt.x + "_;" ;
    if (list == null) {
      htNormMap.put(norm, key);
    } else {
      if (list.indexOf("_" + pt.x + "_" + pt.y + "_") >= 0
          || list.indexOf("_" + pt.y + "_" + pt.z + "_") >= 0
          || list.indexOf("_" + pt.z + "_" + pt.x + "_") >= 0)
        return  false;
      htNormMap.put(norm, list + key);
    }
    
    plane = Measure.getPlaneThroughPoints(points[pt.x], points[pt.y], points[pt.z], 
        vNorm, vAB, plane);
    float d = Measure.distanceToPlane(plane, points[ptCenter]);
    System.out.println("" + norm + " " + pt + " " + d);
    return true;
  }

  private String faceId(int i, int j, int k) {
    return (P3i.new3(i, j, k)).toString();
  }

  private V3 align1 = new V3();
  private V3 align2 = new V3();

  private boolean isAligned(P3 pt1, P3 pt2, P3 pt3) {
    align1.sub2(pt1, pt3);
    align2.sub2(pt2, pt3);
    float angle = align1.angle(align2);
    return (angle < 0.01f || angle > 3.13f);
  }

  private final V3 vAB = new V3();

  private static float minDistanceForPlanarity = 0.1f;

  private boolean isPlanar(P3 pt1, P3 pt2, P3 pt3, P3 ptX) {
    /*
     * what is the quickest way to find out if four points are planar? 
     * here we determine the plane through three and then the distance to that plane
     * of the fourth
     * 
     */
    V3 norm = new V3();
    float w = Measure.getNormalThroughPoints(pt1, pt2, pt3, norm, vAB);
    float d = Measure.distanceToPlaneV(norm, w, ptX);
    return (Math.abs(d) < minDistanceForPlanarity);
  }

  @Override
  public void setModelVisibilityFlags(BS bsModels) {
    /*
     * set all fixed objects visible; others based on model being displayed note
     * that this is NOT done with atoms and bonds, because they have mads. When
     * you say "frame 0" it is just turning on all the mads.
     */
    for (int i = polyhedronCount; --i >= 0;) {
      Polyhedron p = polyhedrons[i];
      if (ms.at[p.centralAtom.i].isDeleted())
        p.isValid = false;
      p.visibilityFlags = (p.visible 
          && bsModels.get(p.modelIndex)
          && !ms.isAtomHidden(p.centralAtom.i) 
          && !ms.at[p.centralAtom.i].isDeleted() ? vf
          : 0);
    }
  }
  
  @Override
  public String getShapeState() {
    if (polyhedronCount == 0)
      return "";
    SB s = new SB();
    for (int i = 0; i < polyhedronCount; i++)
      if (polyhedrons[i].isValid)
        s.append(polyhedrons[i].getState());
    if (drawEdges == EDGES_FRONT)
      appendCmd(s, "polyhedra frontedges");
    else if (drawEdges == EDGES_ALL)
      appendCmd(s, "polyhedra edges");
    s.append(vwr.getAtomShapeState(this));
    for (int i = 0; i < polyhedronCount; i++) {
      Polyhedron p = polyhedrons[i];
      if (p.isValid && p.colixEdge != C.INHERIT_ALL && bsColixSet.get(p.centralAtom.i))
        appendCmd(s, "select ({" + p.centralAtom.i + "}); color polyhedra " 
      + (C.isColixTranslucent(colixes[p.centralAtom.i]) ? "translucent " : "") 
      + C.getHexCode(colixes[p.centralAtom.i]) + " "  + C.getHexCode(p.colixEdge));
  }
    return s.toString();
  }
}
