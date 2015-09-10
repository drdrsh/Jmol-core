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
import java.util.Map.Entry;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.M4;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.V3;

import org.jmol.api.AtomIndexIterator;
import org.jmol.api.SmilesMatcherInterface;
import org.jmol.api.SymmetryInterface;
import org.jmol.c.PAL;
import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.script.SV;
import org.jmol.script.T;
import org.jmol.shape.AtomShape;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.Logger;
import org.jmol.util.Normix;

public class Polyhedra extends AtomShape {

  private final static float DEFAULT_DISTANCE_FACTOR = 1.85f;
  private final static float DEFAULT_FACECENTEROFFSET = 0.25f;
  private final static int EDGES_NONE = 0;
  public final static int EDGES_ALL = 1;
  public final static int EDGES_FRONT = 2;
  private final static int MAX_VERTICES = 250;
  private final static int FACE_COUNT_MAX = MAX_VERTICES - 3;
  private P3[] otherAtoms = new P3[MAX_VERTICES + FACE_COUNT_MAX + 1];
  private V3[] normalsT = new V3[MAX_VERTICES + 1];
  private int[][] planesT = AU.newInt2(MAX_VERTICES);
  private final static P3 randomPoint = P3.new3(3141f, 2718f, 1414f);

  private static final int MODE_BONDING = 1;
  private static final int MODE_POINTS = 2;
  private static final int MODE_ITERATE = 3;
  private static final int MODE_BITSET = 4;
  private static final int MODE_UNITCELL = 5;
  private static final int MODE_INFO = 6;
  /**
   * a dot product comparison term
   */
  private static final float DEFAULT_PLANAR_PARAM = 0.98f;

  public int polyhedronCount;
  public Polyhedron[] polyhedrons = new Polyhedron[32];
  public int drawEdges;

  private float radius;
  private int nVertices;

  float faceCenterOffset;
//  float distanceFactor = Float.NaN;
  boolean isCollapsed;

  private boolean iHaveCenterBitSet;
  private boolean bondedOnly;
  private boolean haveBitSetVertices;

  private BS centers;
  private BS bsVertices;
  private BS bsVertexCount;

  private boolean useUnitCell;
  private int nPoints;
  private float planarParam;
  private Map<String, SV> info;
  private float distanceRef;

  @SuppressWarnings("unchecked")
  @Override
  public void setProperty(String propertyName, Object value, BS bs) {

    if ("init" == propertyName) {
      faceCenterOffset = DEFAULT_FACECENTEROFFSET;
      //distanceFactor = 
      planarParam = Float.NaN;
      radius = 0.0f;
      nVertices = 0;
      nPoints = 0;
      bsVertices = null;
      useUnitCell = false;
      centers = null;
      info = null;
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
      int n = ((Integer) value).intValue();
      if (n < 0) {
        if (-n >= nVertices) {
          bsVertexCount.setBits(nVertices, 1 - n);
          nVertices = -n;
        }
      } else {
        bsVertexCount.set(nVertices = n);
      }
      return;
    }

    if ("centers" == propertyName) {
      centers = (BS) value;
      iHaveCenterBitSet = true;
      return;
    }

    if ("unitCell" == propertyName) {
      useUnitCell = true;
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

    if ("toVertices" == propertyName) {
      P3[] points = (P3[]) value;
      nPoints = Math.min(points.length, MAX_VERTICES);
      for (int i = nPoints; --i >= 0;)
        otherAtoms[i] = points[i];
      return;
    }

    if ("faceCenterOffset" == propertyName) {
      faceCenterOffset = ((Float) value).floatValue();
      return;
    }

    if ("distanceFactor" == propertyName) {
      // not a general user option
      // ignore 
      //distanceFactor = ((Float) value).floatValue();
      return;
    }

    if ("planarParam" == propertyName) {
      // not a general user option
      planarParam = ((Float) value).floatValue();
      return;
    }

    if ("bonds" == propertyName) {
      bondedOnly = true;
      return;
    }

    if ("info" == propertyName) {
      info = (Map<String, SV>) value;
      centers = BSUtil.newAndSetBit(info.get("atomIndex").intValue);
      iHaveCenterBitSet = true;
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

//    if ("token" == propertyName) {
//      int tok = ((Integer) value).intValue();
//      Swit
//      if (tok == T.triangles && tok == T.notriangles) {
//      } else {
//        setLighting(tok == T.fullylit, bs);
//      }
//      return;
//    }

    if ("radius" == propertyName) {
      radius = ((Float) value).floatValue();
      return;
    }

    if (propertyName == "deleteModelAtoms") {
      int modelIndex = ((int[]) ((Object[]) value)[2])[0];
      for (int i = polyhedronCount; --i >= 0;) {
        polyhedrons[i].info = null;
        int mi = polyhedrons[i].centralAtom.mi;
        if (mi == modelIndex) {
          polyhedronCount--;
          polyhedrons = (Polyhedron[]) AU.deleteElements(polyhedrons, i, 1);
        }
      }
      //pass on to AtomShape
    }

    setPropAS(propertyName, value, bs);
  }

  @Override
  public Object getProperty(String propertyName, int index) {
    if (propertyName == "symmetry") {
      String s = "";
      for (int i = polyhedronCount; --i >= 0;)
        s += polyhedrons[i].getSymmetry(vwr, true) + "\n";
      return s;
    }
    return null;
  }

  @Override
  public boolean getPropertyData(String property, Object[] data) {
    int iatom;
    if (property == "points") {
      iatom = ((Integer) data[0]).intValue();
      for (int i = polyhedronCount; --i >= 0;) {
        if (polyhedrons[i].centralAtom.i == iatom) {
          if (polyhedrons[i].collapsed)
            break;
          data[1] = polyhedrons[i].vertices;
          return true;
        }
      }
      return false;
    }
    if (property == "move") {
      M4 mat = (M4) data[1];
      if (mat == null)
        return false;
      BS bs = (BS) data[0];
      for (int i = polyhedronCount; --i >= 0;) {
        Polyhedron p = polyhedrons[i];
        if (bs.get(p.centralAtom.i))
          p.move(mat);
      }
      return true;
    }
    if (property == "centers") {
      BS bs = new BS();
      String smiles = (String) data[1];
      SmilesMatcherInterface sm = (smiles == null ? null : vwr
          .getSmilesMatcher());
      Integer n = (Integer) data[0];
      if (sm != null)
        smiles = sm.cleanSmiles(smiles);
      int nv = (smiles != null ? PT.countChar(smiles, '*')
          : n == null ? Integer.MIN_VALUE : n.intValue());
      if (smiles != null && nv == 0)
        nv = Integer.MIN_VALUE;
      for (int i = polyhedronCount; --i >= 0;) {
        if (nv > 0 && polyhedrons[i].nVertices != nv || nv > Integer.MIN_VALUE
            && nv < 0 && polyhedrons[i].faces.length != -nv)
          continue;
        if (smiles == null) {
          bs.set(polyhedrons[i].centralAtom.i);
        } else if (sm != null) {
          polyhedrons[i].getSymmetry(vwr, false);
          String smiles0 = polyhedrons[i].polySmiles;
          try {
            if (sm.areEqual(smiles, smiles0) > 0)
              bs.set(polyhedrons[i].centralAtom.i);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
      data[2] = bs;
      return true;
    }
    if (property == "info") {
      iatom = ((Integer) data[0]).intValue();
      for (int i = polyhedronCount; --i >= 0;) {
        if (polyhedrons[i].centralAtom.i == iatom) {
          data[1] = polyhedrons[i].getInfo(vwr, true);
          return true;
        }
      }
      return false;
    }
    return false;
  }

  @Override
  public Lst<Map<String, Object>> getShapeDetail() {
    Lst<Map<String, Object>> lst = new Lst<Map<String, Object>>();
    for (int i = 0; i < polyhedronCount; i++)
      lst.addLast(polyhedrons[i].getInfo(vwr, true));
    return lst;
  }

//  private void setLighting(boolean isFullyLit, BS bs) {
//    for (int i = polyhedronCount; --i >= 0;)
//      if (bs.get(polyhedrons[i].centralAtom.i)) {
//        short[] normixes = polyhedrons[i].getNormixes();
//        polyhedrons[i].isFullyLit = isFullyLit;
//        for (int j = normixes.length; --j >= 0;) {
//          if (normixes[j] < 0 != isFullyLit)
//            normixes[j] = (short) ~normixes[j];
//        }
//      }
//  }

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

  private int buildMode;

  private void buildPolyhedra() {
    boolean useBondAlgorithm = radius == 0 || bondedOnly;
    buildMode = (info != null ? MODE_INFO : nPoints > 0 ? MODE_POINTS
        : haveBitSetVertices ? MODE_BITSET : useUnitCell ? MODE_UNITCELL
            : useBondAlgorithm ? MODE_BONDING : MODE_ITERATE);
    AtomIndexIterator iter = (buildMode == MODE_ITERATE ? ms
        .getSelectedAtomIterator(null, false, false, false, false) : null);
    for (int i = centers.nextSetBit(0); i >= 0; i = centers.nextSetBit(i + 1)) {
      Atom atom = atoms[i];
      Polyhedron p = null;
      switch (buildMode) {
      case MODE_BITSET:
        bsVertices.clear(i);
        p = constructBitSetPolyhedron(atom);
        break;
      case MODE_UNITCELL:
        p = constructUnitCellPolygon(atom, useBondAlgorithm);
        break;
      case MODE_BONDING:
        p = constructBondsPolyhedron(atom, 0);
        break;
      case MODE_POINTS:
        p = constructPointPolyhedron(atom);
        break;
      case MODE_ITERATE:
        vwr.setIteratorForAtom(iter, i, radius);
        p = constructRadiusPolyhedron(atom, iter);
        break;
      case MODE_INFO:
        p = new Polyhedron().setInfo(info, vwr.ms.at);
        break;
      }
      if (p != null) {
        if (polyhedronCount == polyhedrons.length)
          polyhedrons = (Polyhedron[]) AU.doubleLength(polyhedrons);
        polyhedrons[polyhedronCount++] = p;
      }
      if (haveBitSetVertices)
        break;
    }
    if (iter != null)
      iter.release();
  }

  private Polyhedron constructPointPolyhedron(Atom atom) {
    return validatePolyhedron(atom, nPoints, otherAtoms);
  }

  private Polyhedron constructUnitCellPolygon(Atom atom,
                                              boolean useBondAlgorithm) {
    SymmetryInterface unitcell = vwr.ms.getUnitCellForAtom(atom.i);
    if (unitcell == null)
      return null;
    BS bsAtoms = BSUtil.copy(vwr.getModelUndeletedAtomsBitSet(atom.mi));
    if (bsVertices != null)
      bsAtoms.and(bsVertices);
    if (bsAtoms.isEmpty())
      return null;
    AtomIndexIterator iter = unitcell.getIterator(vwr, atom, atoms, bsAtoms,
        useBondAlgorithm ? 5f : radius);
    if (!useBondAlgorithm)
      return constructRadiusPolyhedron(atom, iter);
    float myBondingRadius = atom.getBondingRadius();
    if (myBondingRadius == 0)
      return null;
    float bondTolerance = vwr.getFloat(T.bondtolerance);
    float minBondDistance = vwr.getFloat(T.minbonddistance);
    float minBondDistance2 = minBondDistance * minBondDistance;
    int bondCount = 0;
    while (iter.hasNext()) {
      Atom other = atoms[iter.next()];
      float otherRadius = other.getBondingRadius();
      P3 pt = iter.getPosition();
      float distance2 = atom.distanceSquared(pt);
      if (!vwr.ms.isBondable(myBondingRadius, otherRadius, distance2,
          minBondDistance2, bondTolerance))
        continue;
      otherAtoms[bondCount++] = pt;
      if (bondCount >= MAX_VERTICES)
        break;
    }
    return constructBondsPolyhedron(atom, bondCount);
  }

  private Polyhedron constructBondsPolyhedron(Atom atom, int bondCount) {
    if (bondCount == 0) {
      Bond[] bonds = atom.bonds;
      if (bonds == null)
        return null;
      for (int i = bonds.length; --i >= 0;) {
        Bond bond = bonds[i];
        if (!bond.isCovalent())
          continue;
        Atom other = bond.getOtherAtom(atom);
        if (bsVertices != null && !bsVertices.get(other.i) || radius > 0
            && other.distance(atom) > radius)
          continue;
        otherAtoms[bondCount++] = other;
        if (bondCount >= MAX_VERTICES)
          break;
      }
    }
    distanceRef = 0;
    return (bondCount < 3 || bondCount >= MAX_VERTICES || nVertices > 0
        && !bsVertexCount.get(bondCount) ? null : validatePolyhedron(atom,
        bondCount, otherAtoms));
  }

  private Polyhedron constructBitSetPolyhedron(Atom atom) {
    int otherAtomCount = 0;
    distanceRef = 0;
    for (int i = bsVertices.nextSetBit(0); i >= 0; i = bsVertices
        .nextSetBit(i + 1))
      otherAtoms[otherAtomCount++] = atoms[i];
    return validatePolyhedron(atom, otherAtomCount, otherAtoms);
  }

  private Polyhedron constructRadiusPolyhedron(Atom atom, AtomIndexIterator iter) {
    int otherAtomCount = 0;
    distanceRef = radius;
    while (iter.hasNext()) {
      Atom other = atoms[iter.next()];
      P3 pt = iter.getPosition();
      if (pt == null) {
        // this will happen with standard radius atom iterator
        pt = other;
        if (bsVertices != null && !bsVertices.get(other.i)
            || atom.distance(pt) > radius)
          continue;
      }
      if (other.altloc != atom.altloc && other.altloc != 0 && atom.altloc != 0)
        continue;
      if (otherAtomCount == MAX_VERTICES)
        break;
      otherAtoms[otherAtomCount++] = pt;
    }
    return (otherAtomCount < 3 || nVertices > 0
        && !bsVertexCount.get(otherAtomCount) ? null : validatePolyhedron(atom,
        otherAtomCount, otherAtoms));
  }

  private Polyhedron validatePolyhedron(Atom centralAtom, int vertexCount,
                                        P3[] points) {
    boolean collapsed = isCollapsed;
    boolean checkDist = (distanceRef != 0);
    int planeCount = 0;
    int nPoints = vertexCount + 1;
    int ni = vertexCount - 2;
    int nj = vertexCount - 1;
    float planarParam = (Float.isNaN(this.planarParam) ? DEFAULT_PLANAR_PARAM
        : this.planarParam);
//    float factor = (!Float.isNaN(distanceFactor) ? distanceFactor
//        : DEFAULT_DISTANCE_FACTOR);
//    BS bs = BS.newN(vertexCount);

    // here we are assuring that at least ONE face is drawn to 
    // all matching vertices -- skip this for BOND polyhedra

    points[vertexCount] = centralAtom;
    P3 ptAve = new P3();
    for (int i = 0; i < vertexCount; i++)
      ptAve.add(points[i]);
    ptAve.scale(1f / vertexCount);
//    float distMax = 0;
//    if (checkDist) {
//      float dAverage = 0;
//      for (int i = 0; i < vertexCount; i++)
//        dAverage += points[vertexCount].distance(points[i]);
//      dAverage /= vertexCount;
//      boolean isOK = (dAverage == 0);
//      while (!isOK && factor < 10.0f) {
//        distMax = dAverage * factor;
//        bs.setBits(0, vertexCount);
//        for (int i = 0; i < ni; i++)
//          for (int j = i + 1; j < nj; j++) {
//            if (points[i].distance(points[j]) > distMax)
//              continue;
//            for (int k = j + 1; k < vertexCount; k++) {
//              if (points[i].distance(points[k]) > distMax
//                  || points[j].distance(points[k]) > distMax)
//                continue;
//              bs.clear(i);
//              bs.clear(j);
//              bs.clear(k);
//            }
//          }
//        isOK = true;
//        for (int i = 0; i < vertexCount; i++)
//          if (bs.get(i)) {
//            isOK = false;
//            factor *= 1.05f;
//            if (Logger.debugging) {
//              Logger.debug("Polyhedra distanceFactor for " + vertexCount
//                  + " atoms increased to " + factor + " in order to include "
//                  + points[i]);
//            }
//            break;
//          }
//      }
//    }
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

    P3 ptRef = P3.newP(ptAve);
    BS bsThroughCenter = new BS();
    for (int pt = 0, i = 0; i < ni; i++)
      for (int j = i + 1; j < nj; j++)
        for (int k = j + 1; k < vertexCount; k++, pt++)
          if (isPlanar(points[i], points[j], points[k], ptRef))
            bsThroughCenter.set(pt);
    // this next check for distance allows for bond AND distance constraints
    int[][] faces = planesT;
    P4 pTemp = new P4();
    V3 nTemp = new V3();
    float offset = faceCenterOffset;
    int fmax = FACE_COUNT_MAX;
    int vmax = MAX_VERTICES;
    BS bsTemp = Normix.newVertexBitSet();
    BS bsTemp1 = new BS();
    V3[] normals = normalsT;
    Map<Object, Object> htNormMap = new Hashtable<Object, Object>();
    BS bsCenterPlanes = new BS();
    V3 vTemp = vAC;
    for (int i = 0, pt = 0; i < ni; i++)
      for (int j = i + 1; j < nj; j++) {
//        if (checkDist && points[i].distance(points[j]) > distMax) {
//          pt += vertexCount - j - 1;
//          continue;
//        }
        for (int k = j + 1; k < vertexCount; k++, pt++) {
//          if (checkDist
//              && (points[i].distance(points[k]) > distMax || points[j]
//                  .distance(points[k]) > distMax))
//            continue;
          if (planeCount >= fmax) {
            Logger.error("Polyhedron error: maximum face(" + fmax
                + ") -- reduce RADIUS");
            return null;
          }
          if (nPoints >= vmax) {
            Logger.error("Polyhedron error: maximum vertex count(" + vmax
                + ") -- reduce RADIUS");
            return null;
          }
          boolean isThroughCenter = bsThroughCenter.get(pt);
          P3 rpt = (isThroughCenter ? randomPoint : ptAve);
          V3 normal = new V3();
          boolean isWindingOK = Measure.getNormalFromCenter(rpt, points[i], points[j],
              points[k], !isThroughCenter, normal, vTemp);
          // the standard face:
          normals[planeCount] = normal;
          faces[planeCount] = new int[] { isWindingOK ? i : j, isWindingOK ? j : i,
              k, -7 };
          if (!checkFace(points, vertexCount, faces, normals, planeCount, pTemp, nTemp,
              vTemp, htNormMap, planarParam, bsTemp, bsTemp1))
            continue;
          if (isThroughCenter) {
            bsCenterPlanes.set(planeCount++);
          } else if (collapsed) {
            ptRef.setT(points[nPoints] = new P3());
            points[nPoints].scaleAdd2(offset, normal, centralAtom);
            addFacet(i, j, k, ptRef, points, normals, faces, planeCount++, nPoints,
                isWindingOK, vTemp);
            addFacet(k, i, j, ptRef, points, normals, faces, planeCount++, nPoints,
                isWindingOK, vTemp);
            addFacet(j, k, i, ptRef, points, normals, faces, planeCount++, nPoints,
                isWindingOK, vTemp);
            nPoints++;
          } else {
            planeCount++;
          }
        }
      }
    nPoints--;
    if (Logger.debugging) {
      Logger
          .info("Polyhedron planeCount=" + planeCount + " nPoints=" + nPoints);
      for (int i = 0; i < planeCount; i++)
        Logger.info("Polyhedron " + PT.toJSON("face[" +i + "]", faces[i]));
    }
    return new Polyhedron().set(centralAtom, points, nPoints, vertexCount,
        faces, planeCount, normals, bsCenterPlanes, collapsed, distanceRef);
  }

  /**
   * Add one of the three "facets" that compose the planes of a "collapsed" polyhedron.
   * A mask of -2 ensures that only the [1-2] edge is marked as an outer edge.
   * 
   * @param i
   * @param j
   * @param k
   * @param ptRef slightly out from the center; based on centerOffset parameter
   * @param points
   * @param normals
   * @param faces
   * @param planeCount
   * @param nRef
   * @param isWindingOK
   * @param vTemp 
   */
  private void addFacet(int i, int j, int k, P3 ptRef, P3[] points,
                        V3[] normals, int[][] faces, int planeCount, int nRef,
                        boolean isWindingOK, V3 vTemp) {
    V3 normal = new V3();
    Measure.getNormalFromCenter(points[k], ptRef, points[i], points[j], false, normal, vTemp);
    normals[planeCount] = normal;
    faces[planeCount] = new int[] { nRef, isWindingOK ? i : j, isWindingOK ? j : i,
        -2 };
    //            System.out.println("draw ID \"d" + faceId(i, j, k) + "\" VECTOR "
    //              + ptRef + " " + normal + " color blue \">" + faceId(i, j, k) + isWindingOK
    //            + "\"");
  }

  /**
   * Clean out overlapping triangles based on normals and cross products. For
   * now, we use normixes, which are approximations of normals. It is not 100%
   * guaranteed that this will work.
   * 
   * @param points
   * @param nPoints
   * @param planes
   * @param normals
   * @param index
   * @param pTemp
   * @param vNorm
   * @param vTemp
   * @param htNormMap
   * @param planarParam
   * @param bsTemp
   * @param bsPts
   * @return true if valid
   */
  private boolean checkFace(P3[] points, int nPoints, int[][] planes,
                            V3[] normals, int index, P4 pTemp, V3 vNorm,
                            V3 vTemp, Map<Object, Object> htNormMap,
                            float planarParam, BS bsTemp, BS bsPts) {
    int[] p1 = planes[index];

    // Check here for a 3D convex hull: 
    pTemp = Measure.getPlaneThroughPoints(points[p1[0]], points[p1[1]],
        points[p1[2]], vNorm, vTemp, pTemp);
    // See if all vertices are OUTSIDE the the plane we are considering.      
    for (int j = 0; j < nPoints; j++) {
      vTemp.sub2(points[p1[0]], points[j]);
      float v = vTemp.dot(vNorm);
      if (v < -0.15) {
        return false;
      }
    }
    V3 norm = normals[index];
    Integer normix = Integer.valueOf(Normix.getNormixV(norm, bsTemp));
    Object o = htNormMap.get(normix);
    if (o == null) {
      // we must see if there is a close normix to this
      V3[] norms = Normix.getVertexVectors();
      for (Entry<Object, Object> e : htNormMap.entrySet()) {
        Object ikey = e.getKey();
        if (ikey instanceof Integer) {
          Integer n = (Integer) ikey;
          if (norms[n.intValue()].dot(norm) > planarParam) {
            normix = n;
            break;
          }
        }
      }
      htNormMap.put(normix, Boolean.TRUE);
    }
    bsPts.clearAll();
    for (int i = 0; i < 3; i++)
      if (!addEdge(htNormMap, normix, p1, i, points, bsPts))
        return false;
    return true;
  }

  /**
   * Check each edge to see that
   * 
   * (a) it has not been used before
   * 
   * (b) it does not have vertex points on both sides of it
   * 
   * (c) if it runs opposite another edge, then both edge masks are set properly
   * 
   * @param htNormMap
   * @param normix
   * @param p1
   * @param i
   * @param points
   * @param bsPts
   * @return true if this triangle is OK
   * 
   */
  private boolean addEdge(Map<Object, Object> htNormMap, Integer normix,
                          int[] p1, int i, P3[] points, BS bsPts) {
    // forward maps are out
    int pt1 = p1[(i + 1) % 3];
    String s1 = "_" + pt1;
    int pt = p1[i];
    String s = "_" + pt;
    String edge = normix + s + s1;
    if (htNormMap.containsKey(edge))
      return false;
    //reverse maps are in
    String edge0 = normix + s1 + s;
    Object o = htNormMap.get(edge0);
    if (o == null) {
      // first check that we have all points on the same side of this line. 
      P3 coord2 = points[pt1];
      P3 coord1 = points[pt];
      vAB.sub2(coord2, coord1);
      for (int j = bsPts.nextSetBit(0); j >= 0; j = bsPts.nextSetBit(j + 1)) {
        if (j == pt1 || j == pt)
          continue;
        vAC.sub2(points[j], coord1);
        if (o == null) {
          o = bsPts;
          vBC.cross(vAC, vAB);
          continue;
        }
        vAC.cross(vAC, vAB);
        if (vBC.dot(vAC) < 0)
          return false;
      }
      bsPts.set(pt);
      bsPts.set(pt1);
    } else {
      // set mask to exclude both of these.
      int[] p10 = (int[]) ((Object[]) o)[0];
      int i0 = ((Integer) ((Object[]) o)[1]).intValue();
      p10[3] = -((-p10[3]) ^ (1 << i0));
      p1[3] = -((-p1[3]) ^ (1 << i));
    }
    htNormMap.put(edge, new Object[] { p1, Integer.valueOf(i) });
    return true;
  }

  private final V3 vAB = new V3();
  private final V3 vAC = new V3();
  private final V3 vBC = new V3();

  private static float MAX_DISTANCE_TO_PLANE = 0.1f;

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
    return (Math.abs(d) < MAX_DISTANCE_TO_PLANE);
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
      p.visibilityFlags = (p.visible && bsModels.get(p.centralAtom.mi)
          && !ms.isAtomHidden(p.centralAtom.i)
          && !ms.at[p.centralAtom.i].isDeleted() ? vf : 0);
      if (p.visibilityFlags != 0)
        setShapeVisibility(atoms[p.centralAtom.i], true);
    }
  }

  @Override
  public String getShapeState() {
    if (polyhedronCount == 0)
      return "";
    SB s = new SB();
    for (int i = 0; i < polyhedronCount; i++)
      if (polyhedrons[i].isValid)
        s.append(polyhedrons[i].getState(vwr));
    if (drawEdges == EDGES_FRONT)
      appendCmd(s, "polyhedra frontedges");
    else if (drawEdges == EDGES_ALL)
      appendCmd(s, "polyhedra edges");
    s.append(vwr.getAtomShapeState(this));
    for (int i = 0; i < polyhedronCount; i++) {
      Polyhedron p = polyhedrons[i];
      if (p.isValid && p.colixEdge != C.INHERIT_ALL
          && bsColixSet.get(p.centralAtom.i))
        appendCmd(
            s,
            "select ({"
                + p.centralAtom.i
                + "}); color polyhedra "
                + (C.isColixTranslucent(colixes[p.centralAtom.i]) ? "translucent "
                    : "") + C.getHexCode(colixes[p.centralAtom.i]) + " "
                + C.getHexCode(p.colixEdge));
    }
    return s.toString();
  }
}
