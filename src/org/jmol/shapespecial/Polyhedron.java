package org.jmol.shapespecial;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.AU;
import javajs.util.P3;

import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.util.C;
import org.jmol.util.Escape;

public class Polyhedron {
  /**
   * 
   */

  int modelIndex;
  public final Atom centralAtom;
  public final P3[] points;
  int nVertices;
  boolean visible;
  public final short[] normixes;
  public int[][] planes;
  //int planeCount;
  public int visibilityFlags = 0;
  boolean collapsed = false;
  float faceCenterOffset, distanceFactor;
  boolean isFullyLit;
  public boolean isValid = true;
  public short colixEdge = C.INHERIT_ALL;

  Map<String, Object> getInfo() {
    Map<String, Object> info = new Hashtable<String, Object>();
    info.put("vertexCount", Integer.valueOf(nVertices));
    info.put("modelIndex", Integer.valueOf(centralAtom.mi));
    info.put("planeCount", Integer.valueOf(planes.length));
    info.put("_ipt", Integer.valueOf(centralAtom.i));
    info.put("center", P3.newP(centralAtom));
    info.put("vertices", AU.arrayCopyPt(points, nVertices));
    info.put("polygons", AU.arrayCopyII(planes, planes.length));
    return info;
  }



  Polyhedron(Atom centralAtom, int nVertices, int nPoints, int planeCount,
      P3[] otherAtoms, short[] normixes, int[][] planes, boolean collapsed, float faceCenterOffset, float distanceFactor) {
    this.centralAtom = centralAtom;
    modelIndex = centralAtom.mi;
    this.nVertices = nVertices;
    this.points = new P3[nPoints + 1];
    this.visible = true;
    this.normixes = new short[planeCount];
    //this.planeCount = planeCount;
    this.planes = AU.newInt2(planeCount);
    for (int i = nPoints + 1; --i >= 0;) // includes central atom as last atom or possibly reference point
      points[i] = otherAtoms[i];
    for (int i = planeCount; --i >= 0;)
      this.normixes[i] = normixes[i];
    for (int i = planeCount; --i >= 0;)
      this.planes[i] = planes[i];
    this.collapsed = collapsed;
    this.faceCenterOffset = faceCenterOffset;
    this.distanceFactor = distanceFactor;
  }

  String getState() {
    P3 ptemp = new P3();
    String s = "({" + centralAtom.i + "}) TO [";
    for (int i = 0; i < nVertices; i++) {
      ptemp.setT(points[i]);
      s += ptemp; 
    }
    s += "]";
    return "  polyhedra " + s
    + (collapsed ? " collapsed" : "") 
    +  " distanceFactor " + distanceFactor
    +  " faceCenterOffset " + faceCenterOffset 
    + (isFullyLit ? " fullyLit" : "" ) + ";"
    + (visible ? "" : "polyhedra off;") + "\n";
  }
  
}