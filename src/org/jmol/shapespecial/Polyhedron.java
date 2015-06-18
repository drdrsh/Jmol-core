package org.jmol.shapespecial;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.AU;
import javajs.util.P3;
import javajs.util.V3;

import org.jmol.api.SmilesMatcherInterface;
import org.jmol.api.SymmetryInterface;
import org.jmol.modelset.Atom;
import org.jmol.script.T;
import org.jmol.util.C;
import org.jmol.viewer.Viewer;

public class Polyhedron {
 
  int modelIndex;
  public final Atom centralAtom;
  public final P3[] vertices;
  int nVertices;
  boolean visible;
  public final short[] normixes;
  public int[][] faces;
  public int visibilityFlags = 0;
  boolean collapsed = false;
  float faceCenterOffset, distanceFactor, planarParam;
  boolean isFullyLit;
  public boolean isValid = true;
  public short colixEdge = C.INHERIT_ALL;
  public String smiles, smarts;
  private SymmetryInterface pointGroup;
  Float volume;



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
    info.put("volume", getVolume());
    if (smarts != null)
      info.put("smarts", smarts);
    if (smiles != null)
      info.put("smiles", smiles);
    if (pointGroup != null)
      info.put("pointGroup", pointGroup.getPointGroupName());
    return info;
  }

  void getSymmetry(Viewer vwr, boolean withPointGroup) {
    SmilesMatcherInterface sm = vwr.getSmilesMatcher();
    try {
      if (smarts == null) {
        smarts = sm.polyhedronToSmiles(faces, nVertices, null);
        smiles = sm.polyhedronToSmiles(faces, nVertices, vertices);
      }
    } catch (Exception e) {
    }
    if (pointGroup == null && withPointGroup)
      pointGroup = vwr.ms.getSymTemp(true).setPointGroup(null, vertices, null,
          false, vwr.getFloat(T.pointgroupdistancetolerance),
          vwr.getFloat(T.pointgrouplineartolerance), true);

  }

  /**
   * allows for n-gon, not just triangle; if last component index is negative, then that's a mesh code
   * @return volume
   */
  private Float getVolume() {
    // this will give spurious results for overlapping faces triangles
    if (volume != null)
      return volume;
    V3 vAB = new V3();
    V3 vAC = new V3();
    V3 vTemp = new V3();
    float v = 0;
    for (int i = faces.length; --i >= 0;) {
      int[] face = faces[i];
      for (int j = face.length - 2; --j >= 0;)
        if (face[j + 2] >= 0)
          v += triangleVolume(face[j], face[j + 1], face[j + 2], vAB, vAC, vTemp);
    }
    return Float.valueOf(v / 6);
  }



  private float triangleVolume(int i, int j, int k, V3 vAB, V3 vAC, V3 vTemp) {
    // volume
    vAB.setT(vertices[i]);
    vAC.setT(vertices[j]);
    vTemp.cross(vAB, vAC);
    vAC.setT(vertices[k]);
    return vAC.dot(vTemp);
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
//    +  " planarParam " + planarParam 
    + (isFullyLit ? " fullyLit" : "" ) + ";"
    + (visible ? "" : "polyhedra off;") + "\n";
  }
  
}