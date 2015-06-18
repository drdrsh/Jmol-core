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
import org.jmol.script.T;
import org.jmol.shape.AtomShape;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.Logger;
import org.jmol.util.Normix;



public class Polyhedra extends AtomShape {

  private final static float DEFAULT_DISTANCE_FACTOR = 1.85f;
//  private final static float DEFAULT_MANY_VERTEX_DISTANCE_FACTOR = 1.5f;
  private final static float DEFAULT_FACECENTEROFFSET = 0.25f;
  private final static int EDGES_NONE = 0;
  public final static int EDGES_ALL = 1;
  public final static int EDGES_FRONT = 2;
  private final static int MAX_VERTICES = 250;
  private final static int FACE_COUNT_MAX = MAX_VERTICES - 3;
  private P3[] otherAtoms = new P3[MAX_VERTICES + 1];
  private short[] normixesT = new short[MAX_VERTICES];
  private int[][] planesT = new int[MAX_VERTICES][3];
  private final static P3 randomPoint = P3.new3(3141f, 2718f, 1414f);
  
  private static final int MODE_BONDING = 1;
  private static final int MODE_POINTS = 2;
  private static final int MODE_ITERATE = 3;
  private static final int MODE_BITSET = 4;
  private static final int MODE_UNITCELL = 5;
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
  float distanceFactor = Float.NaN;
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

  @Override
  public void setProperty(String propertyName, Object value, BS bs) {

    if ("init" == propertyName) {
      faceCenterOffset = DEFAULT_FACECENTEROFFSET;
      distanceFactor = planarParam = Float.NaN;
      radius = 0.0f;
      nVertices = 0;
      nPoints = 0;
      bsVertices = null;
      useUnitCell = false;
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
      int n = ((Integer) value).intValue();
      if(n < 0) {
        if (-n >= nVertices) {
          bsVertexCount.setBits(nVertices, -n);
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
      nPoints = points.length;
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
      distanceFactor = ((Float) value).floatValue();
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
    if (property == "centers") {
      BS bs = new BS();
      String smiles = (String) data[1];
      SmilesMatcherInterface sm = (smiles == null ? null : vwr
          .getSmilesMatcher());
      Integer n = (Integer) data[0];
      
      int nv = (smiles != null ? PT.countChar(smiles, '*') : n == null ? Integer.MIN_VALUE : n.intValue());
      if (smiles !=  null && nv == 0)
        nv = Integer.MIN_VALUE;
      for (int i = polyhedronCount; --i >= 0;) {
        if (nv > 0 && polyhedrons[i].nVertices != nv 
            || nv > Integer.MIN_VALUE && nv < 0 && polyhedrons[i].faces.length != -nv)
          continue;
        if (smiles == null) {
            bs.set(polyhedrons[i].centralAtom.i);
        } else if (sm != null){
          String smiles0 = polyhedrons[i].getSmiles(sm, false);
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
          data[1] = polyhedrons[i].getInfo(vwr);
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
      lst.addLast(polyhedrons[i].getInfo(vwr));
    return lst;
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
    int mode = (nPoints > 0 ? MODE_POINTS 
        : haveBitSetVertices ? MODE_BITSET 
        : useUnitCell ? MODE_UNITCELL 
        : useBondAlgorithm ? MODE_BONDING
        : MODE_ITERATE);
    AtomIndexIterator iter = (mode == MODE_ITERATE ? ms.getSelectedAtomIterator(null,
        false, false, false, false) : null);
    for (int i = centers.nextSetBit(0); i >= 0; i = centers.nextSetBit(i + 1)) {
      Atom atom = atoms[i];
      Polyhedron p = null;
      switch (mode) {
      case MODE_BITSET:
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

  private Polyhedron constructUnitCellPolygon(Atom atom, boolean useBondAlgorithm) {
    SymmetryInterface unitcell = vwr.ms.getUnitCellForAtom(atom.i);
    if (unitcell == null)
      return null;
    BS bsAtoms = BSUtil.copy(vwr.getModelUndeletedAtomsBitSet(atom.mi));
    if (bsVertices != null)
      bsAtoms.and(bsVertices);
    if (bsAtoms.isEmpty())
      return null;
    AtomIndexIterator iter = unitcell.getIterator(vwr, atom, atoms, bsAtoms, useBondAlgorithm ? 5f : radius);
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
      if (!vwr.ms.isBondable(myBondingRadius, otherRadius, distance2, minBondDistance2, bondTolerance))
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
        if (bsVertices != null && !bsVertices.get(i) 
            || radius > 0 && other.distance(atom) > radius)
          continue;
        otherAtoms[bondCount++] = other;
        if (bondCount >= MAX_VERTICES)
          break;
      }
    }
    return (bondCount < 3 || bondCount >= MAX_VERTICES 
        || nVertices > 0 && !bsVertexCount.get(bondCount) ? null
        : validatePolyhedron(atom, bondCount, otherAtoms));
  }

  private Polyhedron constructBitSetPolyhedron(Atom atom) {
    int otherAtomCount = 0;
    for (int i = bsVertices.nextSetBit(0); i >= 0; i = bsVertices
        .nextSetBit(i + 1))
      otherAtoms[otherAtomCount++] = atoms[i];
    return validatePolyhedron(atom, otherAtomCount, otherAtoms);
  }

  private Polyhedron constructRadiusPolyhedron(Atom atom,
                                               AtomIndexIterator iter) {
    int otherAtomCount = 0;
    while (iter.hasNext()) {
      Atom other = atoms[iter.next()];
      P3 pt = iter.getPosition();
      if (pt == null) {
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
        && !bsVertexCount.get(otherAtomCount) ? null : validatePolyhedron(
        atom, otherAtomCount, otherAtoms));
  }

  private Polyhedron validatePolyhedron(Atom centralAtom, int vertexCount,
                                           P3[] otherAtoms) {
    V3 normal = new V3();
    int planeCount = 0;
    int iCenter = vertexCount;
    int nPoints = iCenter + 1;
    float distMax = 0;
    float dAverage = 0;
    float planarParam = (Float.isNaN(this.planarParam) ? DEFAULT_PLANAR_PARAM : this.planarParam);

    P3[] points = new P3[MAX_VERTICES * 3];
    points[iCenter] = otherAtoms[iCenter] = centralAtom;
    for (int i = 0; i < iCenter; i++) {
      points[i] = otherAtoms[i];
      dAverage += points[iCenter].distance(points[i]);
    }
    dAverage = dAverage / iCenter;

    int nother1 = iCenter - 1;
    int nother2 = iCenter - 2;
    boolean isComplex = (nother1 > 6);
    // for many-vertex polygons we reduce the  distance allowed to avoid through-polyhedron faces
    float factor = (!Float.isNaN(distanceFactor) ? distanceFactor
        //: isComplex ? DEFAULT_MANY_VERTEX_DISTANCE_FACTOR 
            : DEFAULT_DISTANCE_FACTOR);
    BS bs = BS.newN(iCenter);
    boolean isOK = (dAverage == 0);

    // here we are assuring that at least ONE face is drawn to 
    // all matching vertices

    while (!isOK && factor < 10.0f) {
      distMax = dAverage * factor;
      bs.setBits(0, iCenter);
      for (int i = 0; i < nother2; i++)
        for (int j = i + 1; j < nother1; j++) {
          if (points[i].distance(points[j]) > distMax)
            continue;
          for (int k = j + 1; k < iCenter; k++) {
            if (points[i].distance(points[k]) > distMax
                || points[j].distance(points[k]) > distMax)
              continue;
            bs.clear(i);
            bs.clear(j);
            bs.clear(k);
          }
        }
      isOK = true;
      for (int i = 0; i < iCenter; i++)
        if (bs.get(i)) {
          isOK = false;
          factor *= 1.05f;
          if (Logger.debugging) {
            Logger.debug("Polyhedra distanceFactor for " + iCenter
                + " atoms increased to " + factor + " in order to include "
                + otherAtoms[i] );
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
        for (int k = j + 1; k < iCenter; k++)
          if (isPlanar(points[i], points[j], points[k], points[iCenter]))
            faceCatalog += faceId(i, j, k);
    for (int j = 0; j < nother1; j++)
      for (int k = j + 1; k < iCenter; k++) {
        if (isAligned(points[j], points[k], points[iCenter]))
          facetCatalog += faceId(j, k, -1);
      }
    P3 ptRef = new P3();
    // this next check for distance allows for bond AND distance constraints
    int[][] p = planesT;
    P4 plane = new P4();
    V3 vTemp = new V3();
    boolean collapsed = isCollapsed;
    float offset = faceCenterOffset;
    int fmax = FACE_COUNT_MAX;
    int vmax = MAX_VERTICES;
    P3 rpt = randomPoint;
    BS bsTemp = Normix.newVertexBitSet();
    short[] n = normixesT;
    Map<Integer,String> htNormMap = new Hashtable<Integer, String>();
    boolean doCheckPlane = isComplex;
    for (int i = 0; i < nother2; i++)
      for (int j = i + 1; j < nother1; j++) {
        if (points[i].distance(points[j]) > distMax)
          continue;
        for (int k = j + 1; k < iCenter; k++) {
          if (points[i].distance(points[k]) > distMax
              || points[j].distance(points[k]) > distMax)
            continue;
          if (planeCount >= fmax) {
            Logger.error("Polyhedron error: maximum face(" + fmax
                + ") -- reduce RADIUS or DISTANCEFACTOR");
            return null;
          }
          if (nPoints >= vmax) {
            Logger.error("Polyhedron error: maximum vertex count(" + vmax
                + ") -- reduce RADIUS");
            return null;
          }
          boolean isFlat = (faceCatalog.indexOf(faceId(i, j, k)) >= 0);
          // if center is on the face, then we need a different point to 
          // define the normal
          boolean isWindingOK = (isFlat ? getNormalFromCenter(rpt, points[i],
              points[j], points[k], false, normal) : getNormalFromCenter(
              points[iCenter], points[i], points[j], points[k], true, normal));
          normal.scale(collapsed && !isFlat ? offset : 0.001f);
          int nRef = nPoints;
          ptRef.setT(points[iCenter]);
          if (collapsed && !isFlat) {
            points[nPoints] = P3.newP(points[iCenter]);
            points[nPoints].add(normal);
            otherAtoms[nPoints] = points[nPoints];
          } else if (isFlat) {
            ptRef.sub(normal);
            nRef = iCenter;
            if (useUnitCell)
              continue;
          }
          String facet;
          facet = faceId(i, j, -1);
          if (collapsed || isFlat && facetCatalog.indexOf(facet) < 0) {
            facetCatalog += facet;
            p[planeCount] = new int[] { isWindingOK ? i : j,
                isWindingOK ? j : i, nRef , isFlat ? -7 : -6};
            getNormalFromCenter(points[k], points[i], points[j], ptRef, false,
                normal);
            n[planeCount++] = (isFlat ? Normix.get2SidedNormix(normal, bsTemp)
                : Normix.getNormixV(normal, bsTemp));
          }
          facet = faceId(i, k, -1);
          if (collapsed || isFlat && facetCatalog.indexOf(facet) < 0) {
            facetCatalog += facet;
            p[planeCount] = new int[] { isWindingOK ? i : k, nRef,
                isWindingOK ? k : i , isFlat ? -7 : -5};
            getNormalFromCenter(points[j], points[i], ptRef, points[k], false,
                normal);
            n[planeCount++] = (isFlat ? Normix.get2SidedNormix(normal, bsTemp)
                : Normix.getNormixV(normal, bsTemp));
          }
          facet = faceId(j, k, -1);
          if (collapsed || isFlat && facetCatalog.indexOf(facet) < 0) {
            facetCatalog += facet;
            p[planeCount] = new int[] { nRef, isWindingOK ? j : k,
                isWindingOK ? k : j , isFlat ? -7 : -4};
            getNormalFromCenter(points[i], ptRef, points[j], points[k], false,
                normal);
            n[planeCount++] = (isFlat ? Normix.get2SidedNormix(normal, bsTemp)
                : Normix.getNormixV(normal, bsTemp));
          }
          if (!isFlat) {
            if (collapsed) {
              nPoints++;
            } else {
              // finally, the standard face:
              p[planeCount] = new int[] { isWindingOK ? i : j,
                  isWindingOK ? j : i, k, -7};
              n[planeCount] = Normix.getNormixV(normal, bsTemp);
              if (!doCheckPlane
                  || checkPlane(points, iCenter, p, n, planeCount, plane,
                      vTemp, htNormMap, planarParam))
                planeCount++;
            }
          }
        }
      }
    nPoints--;
    
    if (Logger.debugging) {
      Logger
          .info("Polyhedron planeCount=" + planeCount + " nPoints=" + nPoints);
      for (int i = 0; i < planeCount; i++) 
        Logger.info("Polyhedron " + getKey(p[i], i));
    }
    return new Polyhedron(centralAtom, iCenter, nPoints, planeCount,
        otherAtoms, n, p, collapsed, offset, factor, planarParam);
  }
  
  /**
   * 
   * @param ptCenter
   * @param ptA
   * @param ptB
   * @param ptC
   * @param isOutward
   * @param normal
   * @return        true if winding is proper; false if not
   */
  private boolean getNormalFromCenter(P3 ptCenter, P3 ptA, P3 ptB,
                            P3 ptC, boolean isOutward, V3 normal) {
    V3 vAB = new V3();
    float d = Measure.getNormalThroughPoints(ptA, ptB, ptC, normal, vAB);
    boolean isReversed = (Measure.distanceToPlaneV(normal, d, ptCenter) > 0);
    if (isReversed == isOutward)
      normal.scale(-1f);
    //System.out.println("Draw v vector scale 2.0 " + Escape.escape(ptCenter) + Escape.escape(normal));
    return !isReversed;
  }


  /**
   * Clean out oeverlapping triangles based on normals. For now, we use
   * normixes, which are approximations of normals. It is not 100% guaranteed
   * that this will work.
   * 
   * @param points
   * @param ptCenter
   * @param planes
   * @param normals
   * @param index
   * @param plane
   * @param vNorm
   * @param htNormMap
   * @return true if valid
   */
  private boolean checkPlane(P3[] points, int ptCenter, int[][] planes,
                             short[] normals, int index, P4 plane, V3 vNorm,
                             Map<Integer, String> htNormMap, float planarParam) {
    
    int[] p1 = planes[index];
    
    
    // Check here for a 3D convex hull: 
    plane = Measure.getPlaneThroughPoints(points[p1[0]], points[p1[1]],
        points[p1[2]], vNorm, vAB, plane);
//    P3 ptest = P3.newP(points[p1[0]]);
//    ptest.add(points[p1[1]]);
//    ptest.add(points[p1[2]]);
//    ptest.scale(1/3f);
//    System.out.println("$draw ID p" + index +" vector " + ptest  + vNorm);

    // See if all vertices are OUTSIDE the the plane we are considering.      
    for (int j = 0; j < ptCenter; j++) {
      vAB.sub2(points[p1[0]], points[j]);
      if (vAB.dot(vNorm) < -0.1) {
        //System.out.println("$draw ID p" + index + "_" + j + points[j]); 
        return false;
      }
    }

    Integer normix = Integer.valueOf(normals[index]);
    String list = htNormMap.get(normix);
    if (list == null) {
      // we must see if there is a close normix to this
      V3[] norms = Normix.getVertexVectors();
      V3 norm = norms[normals[index]];
      for (Entry<Integer, String> e: htNormMap.entrySet()) {
        if (norms[e.getKey().intValue()].dot(norm) > planarParam) {
          list = e.getValue();
          break;
        }
      }      
    }
    int ipt;
    String match = getKey(p1, index);
    if (list == null) {
      htNormMap.put(normix, match);
    } else {
      for (int i = 0; i < 3; i++) {
        // first, look for  (a b c) and (a b d), which invalids one of these 
        if (list.indexOf("_" + p1[i] + "_" + p1[(i + 1) % 3] + "_") >= 0)
          return false;
        // second, look for (a b c) and (b a d), which indicates a larger polygon
        // we only do this if there is one single match in the list
        if ((ipt = list.indexOf("_" + p1[(i + 1) % 3] + "_" + p1[i] + "_")) < 0)
          continue;
        if (list.indexOf(";") == list.lastIndexOf(";")) {
          ipt = PT.parseInt(list.substring(list.indexOf(",") + 1));
          int[] p0 = planes[ipt];
          int n = p0.length - 1;
          int[] pnew = new int[p0.length];
          boolean found = false;
          for (int i0 = 0, j = 0; i0 < n; i0++) {
            pnew[j++] = p0[i0];
            if (!found)
              for (int i1 = 0; i1 < 3; i1++) {
                if (p0[i0] == p1[(i1 + 1) % 3] && p0[(i0 + 1) % n] == p1[i1]) {
                  // (.... a b ....) + (b a c) --> (....a c b....)
                  pnew[j++] = p1[(i1 + 2) % 3];
                  found = true;
                  break;
                }
              }
          }
          planes[ipt] = pnew;
          list = PT.rep(list, getKey(p0, ipt), getKey(pnew, ipt));
          htNormMap.put(normix, list);
//          System.out.println("" + normix + " " + Normix.getVertexVectors()[normix] + " " + getKey(p0, ipt) + " + "
  //            + getKey(p1, index) + " = " + getKey(pnew, ipt));
          return false;
        }
      }
      htNormMap.put(normix, list + match);
    }

    //float d = Measure.distanceToPlane(plane, points[ptCenter]);
    //System.out.println("" + normix + " "  + Normix.getVertexVectors()[normix] + " " + getKey(p1, index) + " " + d);
    return true;
  }

  private String getKey(int[] p1, int index) {
    SB sb = new SB();
    for (int i = 0, n = p1.length; i < n; i++)
      if (p1[i] >= 0)
        sb.append("_").appendI(p1[i]);
    sb.append("_").appendI(p1[0]);
    sb.append("_,").appendI(index).append(";");
    return sb.toString();
  }

  private String faceId(int i, int j, int k) {
    return "[" + i + "," + j + "," + k + "]";
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
