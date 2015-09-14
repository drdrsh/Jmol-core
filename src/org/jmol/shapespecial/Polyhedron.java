package org.jmol.shapespecial;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.M4;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.V3;

import org.jmol.api.SmilesMatcherInterface;
import org.jmol.api.SymmetryInterface;
import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.script.SV;
import org.jmol.script.T;
import org.jmol.util.C;
import org.jmol.util.Elements;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Node;
import org.jmol.util.Normix;
import org.jmol.util.Point3fi;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

public class Polyhedron {

  public Atom centralAtom;
  public P3[] vertices;
  public int[][] triangles;
  public int[][] faces;
  int nVertices;
  boolean collapsed;
  private BS bsFlat;
  private float distanceRef;  
  private V3[] normals;
  private short[] normixes;

  public String smiles, smarts, polySmiles;
  private SymmetryInterface pointGroup;
  private Float volume;

  boolean visible = true;
  boolean isFullyLit;
  public boolean isValid = true;
  public short colixEdge = C.INHERIT_ALL;
  public int visibilityFlags = 0;

  Polyhedron() {  
  }
  
  Polyhedron set(Atom centralAtom, P3[] points, int nPoints, int vertexCount,
      int[][] triangles, int triangleCount, int[][] faces, V3[] normals, BS bsFlat, boolean collapsed, float distanceRef) {
    this.distanceRef = distanceRef;
    this.centralAtom = centralAtom;
    this.nVertices = vertexCount;
    this.vertices = new P3[nPoints + 1];
    this.normals = new V3[triangleCount];
    this.faces = faces;
    this.bsFlat = bsFlat;
    this.triangles = AU.newInt2(triangleCount);
    for (int i = nPoints + 1; --i >= 0;)
      // includes central atom as last atom or possibly reference point
      vertices[i] = points[i];
    for (int i = triangleCount; --i >= 0;)
      this.normals[i] = V3.newV(normals[i]);
    for (int i = triangleCount; --i >= 0;)
      this.triangles[i] = triangles[i];
    this.collapsed = collapsed;
    return this;
  }

  Polyhedron setInfo(Map<String, SV> info, Atom[] at) {
    try {
      collapsed = info.containsKey("collapsed");
      centralAtom = at[info.get("atomIndex").intValue];
      Lst<SV> lst = info.get("vertices").getList();
      SV vc = info.get("vertexCount");
      if (vc == null) {
        // old style
        nVertices = lst.size();
        vertices = new P3[nVertices + 1];
        vertices[nVertices] = SV.ptValue(info.get("ptRef"));
      } else {
        nVertices = vc.intValue;
        vertices = new P3[lst.size()];
        vc = info.get("r");
        if (vc != null)
          distanceRef = vc.asFloat();
      }
      // note that nVertices will be smaller than lst.size()
      // because lst will contain the central atom and any collapsed points
      for (int i = lst.size(); --i >= 0;)
        vertices[i] = SV.ptValue(lst.get(i));
      lst = info.get("elemNos").getList();
      for (int i = nVertices; --i >= 0;) {
        int n = lst.get(i).intValue;
        if (n > 0) {
          Point3fi p = new Point3fi();
          p.setT(vertices[i]);
          p.sD = (short) n;
          vertices[i] = p;
        }
      }
      SV faces = info.get("faces");
      SV o = info.get("triangles");
      if (o == null) { // formerly
        o = faces;
      } else {
        this.faces = toInt2(faces);
      }
      triangles = toInt2(o);
      normals = new V3[triangles.length];
      V3 vAB = new V3();
      for (int i = triangles.length; --i >= 0;) {
        normals[i] = new V3();
        int[] a = triangles[i];
        Measure.getNormalThroughPoints(vertices[a[0]], vertices[a[1]],
            vertices[a[2]], normals[i], vAB);
      }
      bsFlat = SV.getBitSet(info.get("bsFlat"), false);
    } catch (Exception e) {
      return null;
    }
    return this;
  }

  private int[][] toInt2(SV o) {
    Lst<SV> lst = o.getList();
    int[][] ai = AU.newInt2(lst.size());
    for (int i = ai.length; --i >= 0;) {
      Lst<SV> lst2 = lst.get(i).getList();
      int[] a = ai[i] = new int[lst2.size()];
      for (int j = a.length; --j >= 0;)
        a[j] = lst2.get(j).intValue;
    }
    return ai;
  }

  Map<String, Object> info;
  
  Map<String, Object> getInfo(Viewer vwr, boolean isAll) {
    if (isAll && this.info != null && !Logger.debugging)
      return this.info;
    Map<String, Object> info = new Hashtable<String, Object>();
    if (isAll) {
      this.info = info;
      info.put("modelIndex", Integer.valueOf(centralAtom.mi));
      info.put("modelNumber", Integer.valueOf(centralAtom.getModelNumber()));
      info.put("center", P3.newP(centralAtom));
      info.put("atomNumber", Integer.valueOf(centralAtom.getAtomNumber()));
      info.put("atomName", centralAtom.getInfo());
      info.put("element", centralAtom.getElementSymbol());
      info.put("triangleCount", Integer.valueOf(triangles.length));
      info.put("volume", getVolume());
      String[] names = new String[nVertices];
      for (int i = nVertices; --i >= 0;) {
        P3 pt = vertices[i];
        names[i] = (pt instanceof Node ? ((Node) pt).getAtomName()
            : pt instanceof Point3fi ? Elements
                .elementSymbolFromNumber(((Point3fi) pt).sD) : "");
      }
      if (faces != null)
        info.put("faceCount", Integer.valueOf(faces.length));
      info.put("atomNames", names);
      if (smarts != null)
        info.put("smarts", smarts);
      if (smiles != null)
        info.put("smiles", smiles);
      if (polySmiles != null)
        info.put("polySmiles", polySmiles);
      if (pointGroup != null)
        info.put("pointGroup", pointGroup.getPointGroupName());
      Object energy = vwr.ms.getInfo(centralAtom.mi, "Energy");
      if (energy != null)
        info.put("energy", energy);
    }
    if (faces != null)
      info.put("faces", faces);
    if (!isAll || Logger.debugging) {
      info.put("bsFlat", bsFlat);
      if (collapsed)
        info.put("collapsed", Boolean.valueOf(collapsed));
      if (distanceRef != 0)
        info.put("r", Float.valueOf(distanceRef));
      P3[] n = new P3[normals.length];
      for (int i = n.length; --i >= 0;)
        n[i] = P3.newP(normals[i]);
      info.put("normals", n);
      info.put("triangles", AU.arrayCopyII(triangles, triangles.length));
    }
    info.put("vertexCount", Integer.valueOf(nVertices));
    info.put("atomIndex", Integer.valueOf(centralAtom.i));
    info.put("vertices",
        AU.arrayCopyPt(vertices, (isAll ? nVertices : vertices.length)));
    int[] elemNos = new int[nVertices];
    for (int i = 0; i < nVertices; i++) {
      P3 pt = vertices[i];
      elemNos[i] = (pt instanceof Node ? ((Node) pt).getElementNumber()
          : pt instanceof Point3fi ? ((Point3fi) pt).sD : -2);
    }
    info.put("elemNos", elemNos);
    return info;
  }

  String getSymmetry(Viewer vwr, boolean withPointGroup) {
    info = null;
    SmilesMatcherInterface sm = vwr.getSmilesMatcher();
    try {
      String details = (distanceRef <= 0 ? null : "r=" + distanceRef);
      if (smarts == null) {
        smarts = sm.polyhedronToSmiles(centralAtom, faces, nVertices, null, JC.SMILES_TOPOLOGY, null);
        smiles = sm.polyhedronToSmiles(centralAtom, faces, nVertices, vertices, JC.SMILES_TYPE_SMILES, null);
        polySmiles = sm.polyhedronToSmiles(centralAtom, faces, nVertices, vertices,  JC.SMILES_TYPE_SMILES | JC.SMILES_POLYHEDRAL | JC.SMILES_ATOM_COMMENT, details);
      }
    } catch (Exception e) {
    }
    if (pointGroup == null && withPointGroup)
      pointGroup = vwr.ms.getSymTemp(true).setPointGroup(null, vertices, null,
          false, vwr.getFloat(T.pointgroupdistancetolerance),
          vwr.getFloat(T.pointgrouplineartolerance), true);
    return centralAtom + " " + pointGroup.getPointGroupName();

  }

  /**
   * allows for n-gon, not just triangle; if last component index is negative,
   * then that's a mesh code
   * 
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
    if (bsFlat.cardinality() < triangles.length)
      for (int i = triangles.length; --i >= 0;) {
        int[] face = triangles[i];
        for (int j = face.length - 2; --j >= 0;)
          if (face[j + 2] >= 0)
            v += triangleVolume(face[j], face[j + 1], face[j + 2], vAB, vAC,
                vTemp);
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

  String getState(Viewer vwr) {
    return "  polyhedron @{" + Escape.e(getInfo(vwr, false)) + "} " 
        + (isFullyLit ? " fullyLit" : "") + ";"
        + (visible ? "" : "polyhedra ({"+centralAtom.i+"}) off;") + "\n";
  }

  public void move(M4 mat) {
    info = null;
    for (int i = 0; i < nVertices; i++) {
      P3 p = vertices[i];
      if (p instanceof Atom)
        p = vertices[i] = P3.newP(p);
      mat.rotTrans(p);
    }
    for (int i = normals.length; --i >= 0;)
      mat.rotate(normals[i]);
    normixes = null;
  }

  public short[] getNormixes() {
    if (normixes == null) {
      normixes = new short[normals.length];
      BS bsTemp = new BS();
      for (int i = normals.length; --i >= 0;)
        normixes[i] = (bsFlat.get(i) ? Normix.get2SidedNormix(normals[i],
            bsTemp) : Normix.getNormixV(normals[i], bsTemp));
    }
    return normixes;
  }
}
