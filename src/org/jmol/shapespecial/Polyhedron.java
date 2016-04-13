package org.jmol.shapespecial;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.M4;
import javajs.util.Measure;
import javajs.util.P3;
import javajs.util.T3;
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

  Map<String, Object> info;

  public String id;
  public P3 center;

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
  /**
   * includes vertices as atoms, with atomic numbers
   */
  private SymmetryInterface pointGroup;
  /**
   * includes vertices as generic points
   */
  private SymmetryInterface pointGroupFamily;
  private Float volume;

  boolean visible = true;
  boolean isFullyLit;
  public boolean isValid = true;
  public short colixEdge = C.INHERIT_ALL;
  public int visibilityFlags = 0;

  public short colix = C.GOLD;
  public int modelIndex = Integer.MIN_VALUE;

  private P3 offset;

  public float scale = 1;

  public float pointScale;

  Polyhedron() {
  }

  Polyhedron set(String id, int modelIndex, P3 atomOrPt, P3[] points,
                 int nPoints, int vertexCount, int[][] triangles,
                 int triangleCount, int[][] faces, V3[] normals, BS bsFlat,
                 boolean collapsed, float distanceRef, float pointScale) {
    this.pointScale = pointScale;
    this.distanceRef = distanceRef;
    if (id == null) {
      centralAtom = (Atom) atomOrPt;
      this.modelIndex = centralAtom.mi;
    } else {
      this.id = id;
      center = atomOrPt;
      this.modelIndex = modelIndex;
    }
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
      id = (info.containsKey("id") ? info.get("id").asString() : null);
      if (id == null) {
        centralAtom = at[info.get("atomIndex").intValue];
        modelIndex = centralAtom.mi;
      } else {
        center = P3.newP(SV.ptValue(info.get("center")));
        modelIndex = info.get("modelIndex").intValue;
        colix = C.getColixS(info.get("color").asString());
        colixEdge = C.getColixS(info.get("colorEdge").asString());
        if (info.containsKey("offset"))
          offset = P3.newP(SV.ptValue(info.get("offset")));
        if (info.containsKey("scale"))
          scale = SV.fValue(info.get("scale"));
      }
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
      if (info.containsKey("pointScale"))
        pointScale = Math.max(0, SV.fValue(info.get("pointScale")));
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

  Map<String, Object> getInfo(Viewer vwr, boolean isState) {
    if (!isState && this.info != null && !Logger.debugging)
      return this.info;
    Map<String, Object> info = new Hashtable<String, Object>();

    info.put("vertexCount", Integer.valueOf(nVertices));

    // get COPY of vertices to prevent script variable from referencing Atom
    int nv = (isState ? vertices.length : nVertices);
    P3[] pts = new P3[nv];
    for (int i = 0; i < nv; i++)
      pts[i] = P3.newP(vertices[i]);
    info.put("vertices", pts);
    info.put("elemNos", getElemNos());

    if (id == null) {
      info.put("atomIndex", Integer.valueOf(centralAtom.i));
    } else {
      info.put("id", id);
      info.put("center", P3.newP(center));
      info.put("color", C.getHexCode(colix));
      info.put("colorEdge", C.getHexCode(colixEdge == 0 ? colix : colixEdge));
      if (offset != null)
        info.put("offset", offset);
      if (scale != 1)
        info.put("scale", Float.valueOf(scale));
    }
    if (id != null || !isState)
      info.put("modelIndex", Integer.valueOf(modelIndex));
    if (!isState) {
      this.info = info;
      if (id == null) {
        info.put("center", P3.newP(centralAtom));
        info.put("modelNumber", Integer.valueOf(centralAtom.getModelNumber()));
        info.put("atomNumber", Integer.valueOf(centralAtom.getAtomNumber()));
        info.put("atomName", centralAtom.getInfo());
        info.put("element", centralAtom.getElementSymbol());
        Object energy = vwr.ms.getInfo(centralAtom.mi, "Energy");
        if (energy != null)
          info.put("energy", energy);
      }
      info.put("triangleCount", Integer.valueOf(triangles.length));
      info.put("volume", getVolume());

      String[] names = new String[nVertices];
      int[] indices = new int[nVertices];
      for (int i = nVertices; --i >= 0;) {
        P3 pt = vertices[i];
        boolean isNode = pt instanceof Node;
        names[i] = (isNode ? ((Node) pt).getAtomName()
            : pt instanceof Point3fi ? Elements
                .elementSymbolFromNumber(((Point3fi) pt).sD) : "");
        indices[i] = (isNode ? ((Node) pt).getIndex() : -1);
      }
      info.put("atomNames", names);
      info.put("vertexIndices", indices);

      if (faces != null)
        info.put("faceCount", Integer.valueOf(faces.length));

      if (smarts != null)
        info.put("smarts", smarts);
      if (smiles != null)
        info.put("smiles", smiles);
      if (polySmiles != null)
        info.put("polySmiles", polySmiles);

      if (pointGroup != null)
        info.put("pointGroup", pointGroup.getPointGroupName());
      if (pointGroupFamily != null)
        info.put("pointGroupFamily", pointGroupFamily.getPointGroupName());
    }
    if (pointScale > 0)
      info.put("pointScale", Float.valueOf(pointScale));
    if (faces != null)
      info.put("faces", faces);
    if (isState || Logger.debugging) {
      info.put("bsFlat", bsFlat);
      if (collapsed)
        info.put("collapsed", Boolean.valueOf(collapsed));
      if (distanceRef != 0)
        info.put("r", Float.valueOf(distanceRef));
      P3[] n = new P3[normals.length];
      for (int i = n.length; --i >= 0;)
        n[i] = P3.newP(normals[i]);
      if (!isState)
        info.put("normals", n);
      info.put("triangles", AU.arrayCopyII(triangles, triangles.length));
    }
    return info;
  }

  private int[] elemNos;
  
  public int[] getElemNos() {
    if (elemNos == null) {      
      elemNos = new int[nVertices];
      for (int i = 0; i < nVertices; i++) {
        P3 pt = vertices[i];
        elemNos[i] = (pt instanceof Node ? ((Node) pt).getElementNumber()
            : pt instanceof Point3fi ? ((Point3fi) pt).sD : -2);
      }
    }
    return elemNos;
  }

  String getSymmetry(Viewer vwr, boolean withPointGroup) {
    if (id == null) {
      if (smarts == null) {
        info = null;
        SmilesMatcherInterface sm = vwr.getSmilesMatcher();
        try {
          String details = (distanceRef <= 0 ? null : "r=" + distanceRef);
          smarts = sm.polyhedronToSmiles(centralAtom, faces, nVertices, null,
              JC.SMILES_GEN_TOPOLOGY, null);
          smiles = sm.polyhedronToSmiles(centralAtom, faces, nVertices,
              vertices, JC.SMILES_TYPE_SMILES, null);
          polySmiles = sm.polyhedronToSmiles(centralAtom, faces, nVertices,
              vertices, JC.SMILES_TYPE_SMILES | JC.SMILES_GEN_POLYHEDRAL
                  | JC.SMILES_GEN_ATOM_COMMENT, details);
        } catch (Exception e) {
        }
      }
    }
    if (!withPointGroup)
      return null;
    if (pointGroup == null) {
      T3[] pts = new T3[nVertices];
      // first time through includes all atoms as atoms
      for (int i = pts.length; --i >= 0;)
        pts[i] = vertices[i];
      pointGroup = vwr.getSymTemp().setPointGroup(null, null, pts, null,
          false, vwr.getFloat(T.pointgroupdistancetolerance),
          vwr.getFloat(T.pointgrouplineartolerance), true);
      // second time through includes all atoms as points only
      for (int i = pts.length; --i >= 0;)
        pts[i] = P3.newP(vertices[i]);
      pointGroupFamily = vwr.getSymTemp().setPointGroup(null, null, pts,
          null, false, vwr.getFloat(T.pointgroupdistancetolerance),
          vwr.getFloat(T.pointgrouplineartolerance), true);
    }
    return (center == null ? centralAtom : center) + "    \t"
        + pointGroup.getPointGroupName() + "\t"
        + pointGroupFamily.getPointGroupName();
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
        v += triangleVolume(face[0], face[1], face[2], vAB, vAC, vTemp);
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
    String ident = (id == null ? "({" + centralAtom.i + "})" : "ID "
        + Escape.e(id));
    return "  polyhedron" + " @{" + Escape.e(getInfo(vwr, true)) + "} "
        + (isFullyLit ? " fullyLit" : "") + ";"
        + (visible ? "" : "polyhedra " + ident + " off;") + "\n";
  }

  void move(M4 mat, BS bsMoved) {
    info = null;
    for (int i = 0; i < nVertices; i++) {
      P3 p = vertices[i];
      if (p instanceof Atom) {
        if (bsMoved.get(((Atom) p).i))
          continue;
        p = vertices[i] = P3.newP(p);
      }
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

  void setOffset(P3 value) {
    if (center == null)
      return; // ID  polyhedra only
    P3 v = P3.newP(value);
    if (offset != null)
      value.sub(offset);
    offset = v;
    center.add(value);
    for (int i = vertices.length; --i >= 0;)
      vertices[i].add(value);
  }

}
