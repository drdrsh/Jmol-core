package org.jmol.shapespecial;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.AU;
import javajs.util.P3;

import org.jmol.api.SmilesMatcherInterface;
import org.jmol.api.SymmetryInterface;
import org.jmol.modelset.Atom;
import org.jmol.script.T;
import org.jmol.util.C;
import org.jmol.viewer.Viewer;

public class Polyhedron {
  /**
   * 
   */

  int modelIndex;
  public final Atom centralAtom;
  public final P3[] vertices;
  int nVertices;
  boolean visible;
  public final short[] normixes;
  public int[][] faces;
  //int planeCount;
  public int visibilityFlags = 0;
  boolean collapsed = false;
  float faceCenterOffset, distanceFactor, planarParam;
  boolean isFullyLit;
  public boolean isValid = true;
  public short colixEdge = C.INHERIT_ALL;
  public String smiles, smarts;
  private SymmetryInterface pointGroup;

  Map<String, Object> getInfo(Viewer vwr) {
    Map<String, Object> info = new Hashtable<String, Object>();
    Object energy = vwr.ms.getInfo(centralAtom.mi, "Energy");
    if (energy != null)
      info.put("energy", energy);
    info.put("modelIndex", Integer.valueOf(centralAtom.mi));
    info.put("modelNumber", Integer.valueOf(centralAtom.getModelNumber()));
    info.put("center", P3.newP(centralAtom));
    info.put("atomIndex", Integer.valueOf(centralAtom.i));
    info.put("atomNumber", Integer.valueOf(centralAtom.getAtomNumber()));
    info.put("atomName", centralAtom.getInfo());
    info.put("element", centralAtom.getElementSymbol());
    info.put("vertexCount", Integer.valueOf(nVertices));
    info.put("vertices", AU.arrayCopyPt(vertices, nVertices));
    info.put("faceCount", Integer.valueOf(faces.length));
    info.put("faces", AU.arrayCopyII(faces, faces.length));
    SmilesMatcherInterface sm = vwr.getSmilesMatcher();
    String smiles = getSmiles(sm, true);
    if (smiles != null)
      info.put("smarts", smiles);
    smiles = getSmiles(sm, false);
    if (smiles != null)
      info.put("smiles", smiles);
    if (pointGroup == null)
      pointGroup = vwr.ms.getSymTemp(true).setPointGroup(null, vertices, null,
          false, vwr.getFloat(T.pointgroupdistancetolerance),
          vwr.getFloat(T.pointgrouplineartolerance), true);
    if (pointGroup != null)
      info.put("pointGroup", pointGroup.getPointGroupName());
    return info;
  }



  public String getSmiles(SmilesMatcherInterface sm, boolean asSmarts) {
    try {
      if (asSmarts)
        return (smarts == null ? (smarts = sm.polyhedronToSmiles(faces, nVertices, null)) : smarts);
      return (smiles == null ? (smiles = sm.polyhedronToSmiles(faces, nVertices, vertices)) : smiles);
    } catch (Exception e) {
      return null;
    }
  }



  Polyhedron(Atom centralAtom, int nVertices, int nPoints, int planeCount,
      P3[] otherAtoms, short[] normixes, int[][] planes, boolean collapsed, float faceCenterOffset, float distanceFactor, float planarParam) {
    this.centralAtom = centralAtom;
    modelIndex = centralAtom.mi;
    this.nVertices = nVertices;
    this.vertices = new P3[nPoints + 1];
    this.visible = true;
    this.normixes = new short[planeCount];
    //this.planeCount = planeCount;
    this.faces = AU.newInt2(planeCount);
    for (int i = nPoints + 1; --i >= 0;) // includes central atom as last atom or possibly reference point
      vertices[i] = otherAtoms[i];
    for (int i = planeCount; --i >= 0;)
      this.normixes[i] = normixes[i];
    for (int i = planeCount; --i >= 0;)
      this.faces[i] = planes[i];
    this.collapsed = collapsed;
    this.faceCenterOffset = faceCenterOffset;
    this.distanceFactor = distanceFactor;
    this.planarParam = planarParam;
  }

  String getState() {
    P3 ptemp = new P3();
    String s = "({" + centralAtom.i + "}) TO [";
    for (int i = 0; i < nVertices; i++) {
      ptemp.setT(vertices[i]);
      s += ptemp; 
    }
    s += "]";
    return "  polyhedra " + s
    + (collapsed ? " collapsed" : "") 
    +  " distanceFactor " + distanceFactor
    +  " faceCenterOffset " + faceCenterOffset 
    +  " planarParam " + planarParam 
    + (isFullyLit ? " fullyLit" : "" ) + ";"
    + (visible ? "" : "polyhedra off;") + "\n";
  }
  
}