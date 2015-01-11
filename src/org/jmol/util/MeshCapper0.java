package org.jmol.util;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.java.BS;

import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.Measure;
import javajs.util.PT;
import javajs.util.Quat;
import javajs.util.P3;
import javajs.util.V3;
import javajs.util.T3;
import javajs.util.V3d;

/**
 * A class to properly cap a convoluted, closed slice of an isosurface
 * 
 */
public class MeshCapper0 {

  public MeshCapper0() {
    // for reflection
  }

  private Map<Integer, Integer> capMap;
  private Lst<int[]> edges;

  private float minEdgeLength;

  protected MeshSlicer slicer;
  protected V3d vab, vac, vap;
  protected V3 vTemp0, vTemp1, vTemp2;
  protected Lst<CapTriangle> triangles;
  protected Map<Integer, int[]> vtMap;
  protected Map<Integer, int[]> veMap1, veMap2;
  protected Map<String, int[]> edgeMap;
  private Lst<CapVertex> vertices;
  private float minEdge;
  protected boolean dumping;

  MeshCapper0 set(MeshSlicer slicer, float resolution) {
    this.slicer = slicer;
    minEdgeLength = resolution / 10;
    return this;
  }

  void clear() {
    minEdge = Float.MAX_VALUE;
    vab = new V3d();
    vac = new V3d();
    vap = new V3d();
    vTemp0 = new V3();
    vTemp1 = new V3();
    vTemp2 = new V3();
    edges = new Lst<int[]>();
    capMap = new Hashtable<Integer, Integer>();
    triangles = new Lst<CapTriangle>();
    vtMap = new Hashtable<Integer, int[]>();
    veMap1 = new Hashtable<Integer, int[]>();
    veMap2 = new Hashtable<Integer, int[]>();
    edgeMap = new Hashtable<String, int[]>();
    vertices = new Lst<CapVertex>();
  }

  protected int addPoint(int thisSet, int i, T3 pt) {
    boolean isEdge = (pt == null);
    Integer ii = Integer.valueOf(i);
    Integer inew = (capMap == null ? null : capMap.get(ii));
    if (inew == null) {
      if (isEdge)
        pt = slicer.m.vs[i];
      else
        thisSet = ((CapVertex) pt).set;
      i = slicer.addIntersectionVertex(pt, 0, -1, thisSet, null, -1, -1);
      inew = Integer.valueOf(i);
      if (isEdge) {
        vertices.addLast(new CapVertex(slicer.m.vs[i], i, thisSet));
        capMap.put(ii, inew);
      }
      if (Logger.debugging)
        Logger.debug(i + "\t" + slicer.m.vs[i]);
    }
    return inew.intValue();
  }

  void addEdge(int iE, int iD, int thisSet) {
    int ia = addPoint(thisSet, iE, null);
    int ib = addPoint(thisSet, iD, null);
    float d = slicer.m.vs[ia].distance(slicer.m.vs[ib]);
    minEdge = Math.min(minEdge, d);
    int[] e = new int[] { ia, ib, -1, -1, 0, thisSet, 0 };
    addVEMap(ia, e, veMap1);
    addVEMap(ib, e, veMap2);
    edges.addLast(e);

  }

  private void addVEMap(int i, int[] e, Map<Integer, int[]> veMap) {
    veMap.put(Integer.valueOf(i), e);
  }

  void createCap() {

    Logger.info("MeshCapper for " + veMap2.size() + " edges and "
        + vertices.size() + " vertices, minEdge = " + minEdge);

    capMap = null;

    if (edges.size() < 3)
      return;

    // right-hand rule for outward normal
    int iF = edges.get(0)[0];
    int iG = edges.get(edges.size() - 1)[0];
    if (iF == iG)
      return;

    MeshSurface m = slicer.m;

    // remove really tiny edges

    cleanEdges();

    // get transform to xy plane

    V3 vab = V3.newVsub(m.vs[iF], m.vs[iG]);
    V3 vac = V3.newV(slicer.norm);
    vac.cross(vac, vab);
    V3 vap = new V3();

    Quat q = Quat.getQuaternionFrameV(vab, vac, null, false);
    M3 m3 = q.getMatrix();
    M4 m4 = M4.newMV(m3, m.vs[iF]);
    M4 m4inv = M4.newM4(m4).invert();

    // get vertex set

    BoxInfo b = new BoxInfo();
    b.setMargin(1);
    for (int i = vertices.size(); --i >= 0;)
      b.addBoundBoxPoint(m4inv.rotTrans2(vertices.get(i), vap));

    // create first triangle, guaranteed to contain all

    P3 p = P3.new3(b.bbCorner0.x, b.bbCorner0.y, 0);
    m4.rotTrans(p);
    CapVertex p0 = new CapVertex(p, -1, -1);
    p.set(2 * b.bbCorner1.x - b.bbCorner0.x, b.bbCorner0.y, 0);
    m4.rotTrans(p);
    CapVertex p1 = new CapVertex(p, -1, -1);
    p.set(b.bbCorner0.x, 2 * b.bbCorner1.y - b.bbCorner0.y, 0);
    m4.rotTrans(p);
    CapVertex p2 = new CapVertex(p, -1, -1);

    // split into net of points, all vertices of triangles

    new CapTriangle(new CapVertex[] { p0, p1, p2 }).set(vertices, "origin\n");

    for (int i = 0; i < triangles.size();)
      if (!triangles.get(i).split3())
        i++;
    if (true) {

      // check for edges that are not already part of a triangle
      // but span two adjacent triangles

      for (int i = edges.size(); --i >= 0;) {
        int[] e = edges.get(i);
        if (e[0] < 0)
          continue;
        // e[2] is a triangle edge marker
        if (e[2] < 0) {
          int v0 = e[0];
          int v1 = e[1];
          int[] vt0 = vtMap.get(Integer.valueOf(v0));

          int[] swapInfo = new int[2];
          CapTriangle t = triangles.get(vt0[0]).getSwappableLink(v1, vt0[1],
              swapInfo);
          if (t != null)
            t.swap(swapInfo[1], true);
        }
      }

      Logger.info("MeshCapper for " + veMap2.size() + " edges, minEdge = "
          + minEdge);

      // complete all remaining segments

      for (int i = edges.size(); --i >= 0;) {
        int[] e = edges.get(i);
        // e[6] is a flag for recycling
        if (e[0] >= 0 && (e[2] < 0 || e[6] == 1)) {
          int[] t_v = vtMap.get(Integer.valueOf(e[0]));
          int iuv;
          CapTriangle t = triangles.get(t_v[0]);
          int iv = t_v[1];
          CapTriangle t0 = t;
          dumping = false;
          while ((iuv = t.checkEdgeSplit(e, iv)) < 0) {
            iuv = -1 - iuv;
            iv = (t.ilink[iuv] + 3 + iuv - iv) % 3;
            t = t.link[iuv];
            if (t == t0) {
              System.out.println("capper is stuck on vertex " + e[0]);
              if (dumping) {
                dumping = false;
                break;
              }
              dumping = true;
              drawFind(e[0]);
            }
          }
          if (iuv == 3)
            i++;

        }
      }

      // remap all edges, throwing out all triangles that
      // are still associated with a reference point

      // reset 

      Logger.info("MeshCapper minEdge=" + minEdge);

      BS bsCheck = new BS();
      for (int i = triangles.size(); --i >= 0;) {
        CapTriangle t = triangles.get(i);
        t.update(false);
        if (t.edgeType == 0)// && !t.checkLinks())
          bsCheck.set(i);
        //        }
        //    }
      }

      // iteratively complete all triangles that have no edges

      boolean working = true;
      if (true)
        while (working) {
          working = false;
          for (int i = bsCheck.nextSetBit(0); i >= 0; i = bsCheck
              .nextSetBit(i + 1)) {
            CapTriangle t = triangles.get(i);
            if (t.checkLinks()) {
              bsCheck.clear(i);
              working = true;
            }
          }
        }
    }
    for (int i = triangles.size(); --i >= 0;) {
      CapTriangle t = triangles.get(i);
      if (t.ok && t.isValid) {
        t.draw("blue");
        m.addPolygonV3(t.vs[0].ipt, t.vs[1].ipt, t.vs[2].ipt, 0, 0, 0,
            m.bsSlabDisplay);
      } else {
        t.draw("red");
      }
    }

    if (Logger.debugging)
      Logger.debug("draw * mesh nofill #OK");

    clear();

  }

  /**
   * condense really tiny edges
   * 
   */
  private void cleanEdges() {
    BS bsVertices = BSUtil.newBitSet2(0, slicer.m.vc);
    minEdge = Float.MAX_VALUE;
    float d;
    for (int i = 0; i < edges.size(); i++) {
      int[] e = edges.get(i);
      int a = e[0];
      if (a < 0)
        continue;
      int b = e[1];
      d = slicer.m.vs[a].distance(slicer.m.vs[b]);
      if (d < minEdgeLength) {
        // remove next-door links
        int[] e2a = veMap2.remove(Integer.valueOf(a));
        //        int[] e1b = veMap1.remove(Integer.valueOf(b));
        if (e == e2a) {
          Logger.error("odd bug in MeshCapper -" + e);
          //        } if (e2a == e1b) {
          //        Logger.info("loop end " + PT.toJSON(null, e2a));
        } else if (e2a[0] == e[1]) {
          // put them back
          veMap2.put(Integer.valueOf(a), e2a);
          //          veMap1.put(Integer.valueOf(b), e);
        } else {
          if (Logger.debugging)
            Logger.debug(PT.toJSON(null, e2a) + PT.toJSON(null, e) + d);
          //    e2(a)       e    
          //  [x a...]   [a b...] 
          //becomes   [a...b...]
          bsVertices.clear(a);
          e[0] = e2a[0];
          e2a[0] = e2a[1] = -1;
          T3 a1 = slicer.m.vs[e[0]];
          T3 a2 = slicer.m.vs[e[1]];
          d = a1.distance(a2);
          if (d < minEdgeLength) {
            i--;
            d = 0;
          }
          addVEMap(e[0], e, veMap1);
          addVEMap(e[1], e, veMap2);
        }
      }
      if (d > 0) {
        minEdge = Math.min(minEdge, d);
        if (Logger.debugging)
          Logger.debug(PT.toJSON(null, e) + d);
      }

    }

    Logger.info("MeshCapper initial vertices: " + vertices.size());
    for (int i = vertices.size(); --i >= 0;)
      if (bsVertices.get(vertices.get(i).ipt))
        Logger.debug("draw v" + vertices.get(i).ipt + " " + vertices.get(i));
      else
        vertices.remove(i);

    Logger.info("MeshCapper cleaned vertices: " + vertices.size());

    for (int i = edges.size(); --i >= 0;) {
      edges.get(i)[4] = i; // for debugging
      addEdgeMap(edges.get(i));
    }

    System.out.println("edges");
  }

  protected void addEdgeMap(int[] e) {
    if (e[0] < 0)
      return;
    edgeMap.put(slicer.getKey(e[0], e[1]), e);
    if (Logger.debugging)
      Logger.debug("draw e_" + e[0] + "_" + e[1] + " " + slicer.m.vs[e[0]]
          + slicer.m.vs[e[1]]);
  }

  protected void drawFind(int ipt) {
    for (int i = triangles.size(); --i >= 0;)
      if (triangles.get(i).containsPt(ipt))
        triangles.get(i).draw("orange");
  }

  protected void drawFindPt(T3 p) {
    for (int i = triangles.size(); --i >= 0;)
      if (triangles.get(i).contains(p))
        triangles.get(i).draw("orange");
  }

  private class CapVertex extends P3 {
    int ipt;
    int set;

    CapVertex(T3 pt, int i, int iset) {
      ipt = i;
      set = iset;
      setT(pt);
    }
  }

  private class CapTriangle {

    private String info = "";
    protected int index;
    private Lst<CapVertex> tempVlist;
    protected CapVertex[] vs;
    protected CapTriangle[] link;
    protected int[] ilink;
    protected boolean ok;
    protected int edgeType, winding;
    protected boolean isValid;
    private float area2, aspect2, min2, max2;
    private boolean smallArea, isTiny;
    int shortEdge = -1;
    private float l0, l1, l2;

    CapTriangle(CapVertex[] points) {
      vs = points;
      checkGeometry();
    }

    protected void set(Lst<CapVertex> vertices, String info) {
      link = new CapTriangle[3];
      ilink = new int[3];
      index = triangles.size();
      if (index == 15 || index == 13)
        System.out.println("???");
      triangles.addLast(this);
      isValid = true;
      this.tempVlist = (vertices == null ? new Lst<CapVertex>() : vertices);
      addInfo(info);
      update(true);
    }

    /**
     * Used any time there is a point on an edge; redistributes interior points
     * to proper homes
     * 
     * @param t2
     * @param t3
     *        may be null
     */
    private void upldateVList(CapTriangle t2, CapTriangle t3) {
      if (tempVlist != null)
        for (int i = tempVlist.size(); --i >= 0;) {
          CapVertex v3 = tempVlist.get(i);
          if (t2.contains(v3))
            t2.tempVlist.addLast(tempVlist.remove(i));
          else if (t3 != null && t3.contains(v3))
            t3.tempVlist.addLast(tempVlist.remove(i));
        }
    }

    void checkGeometry() {
      vTemp0.sub2(vs[2], vs[1]);
      vTemp1.sub2(vs[1], vs[0]);
      vTemp2.sub2(vs[2], vs[0]);
      l0 = vTemp0.lengthSquared();
      l1 = vTemp2.lengthSquared();
      l2 = vTemp1.lengthSquared();
      min2 = Math.min(l0, Math.min(l1, l2));
      max2 = Math.max(l0, Math.max(l1, l2));
      aspect2 = max2 / min2;
      vTemp0.cross(vTemp1, vTemp2);
      winding = (vTemp0.dot(slicer.norm) < 0 ? -1 : 1);
      area2 = vTemp0.lengthSquared() / 4;
      smallArea = (area2 < 1e-7f);
      isTiny = (max2 < 1e-6f);
      if (min2 < 1e-6f) {
        // we can't handle this
        shortEdge = (min2 == l0 ? 0 : min2 == l1 ? 1 : 2);
        Logger.error("MeshCapper: extremely small edge(" + shortEdge
            + "), must be a bug!!!:" + this);
      } else if (smallArea) {
        // we can handle this?
        //System.out.println(this);
      }
      if (isTiny)
        Logger.error("MeshCapper: extremely small triangle:" + this);
      if (winding < 0)
        Logger.error("winding ERROR! " + this);
    }

    protected String getDims() {
      return "\n# " + Math.sqrt(l0) + " " + Math.sqrt(l1) + " " + Math.sqrt(l2) + " "
          + "\n# "+Measure.computeAngleABC(vs[2], vs[0], vs[1], true) + " "
          + Measure.computeAngleABC(vs[0], vs[1], vs[2], true) + " "
          + Measure.computeAngleABC(vs[1], vs[2], vs[0], true)
          + "\n# " + "area=" + Math.sqrt(area2) + " aspect=" + Math.sqrt(aspect2) + " min=" + Math.sqrt(min2)
          + " max=" + Math.sqrt(max2) + " winding=" + winding;
    }

    /**
     * check around this triangle for others that have been set
     * @return true and set OK true if adjacent triangle is OK
     */
    protected boolean checkLinks() {
      if (isValid)
        for (int i = 3; --i >= 0;)
          if (link[i] != null && link[i].isValid && link[i].ok)
            return (ok = true);
      return false;
    }

    /**
     * Used in initial process; splits a triangle into three subtriangles using
     * an interior vertex
     * 
     * @return true if more to do
     * 
     */
    protected boolean split3() {

      //              2                   1         2           
      //             / \                / |         | \                    
      //            / v \              /  |         |  \                    
      //           0-----1     ==>    /t3 v    v    v t2\                 
      //                             /  ^     / \     ^  \               
      //                            2 ^      0---1      ^ 1         
      //              

      int i = tempVlist.size();
      if (i == 0)
        return false;
      i = i / 2;
      CapVertex vp = tempVlist.remove(i);
      if (!contains(vp)) {
        drawFindPt(vp);
        System.out.println("?????");
      }
      CapTriangle t1 = new CapTriangle(new CapVertex[] { vp, vs[0], vs[1] });
      CapTriangle t2 = new CapTriangle(new CapVertex[] { vp, vs[1], vs[2] });
      CapTriangle t3 = new CapTriangle(new CapVertex[] { vp, vs[2], vs[0] });
      int itest = (t1.smallArea ? 2 : t2.smallArea ? 0 : t3.smallArea ? 1 : -1);
      if (itest >= 0) {
        //System.out.println(this + "\ndraw p " + vp);
        // NOTE THAT THIS MOVES THE POINT
        getUV(vp, itest);
        getUVProjection(vp, vs[itest], vp, u, v);
        split2(itest, vp, true);
      } else {
        t1 = null;
        t2.set(null, (Logger.debugging ? "---split3 t2" + this.info + this
            + "----" : null));
        t3.set(null, (Logger.debugging ? "---split3 t3" + this.info + this
            + "----" : null));
        setV(2, vp, "split3");
        setLinks(0, t2, 2, 0, link[0], ilink[0], t3, 2, this, 0);
        setLinks(1, t3, 1, 0, link[1], ilink[1], this, 1, t2, 1);
        update(true);
        t2.update(true);
        t3.update(true);
        upldateVList(t2, t3);
      }
      //if (smallArea)
        //System.out.println("split3 created a very small triangle: " + this);
      return (tempVlist.size() > 0);
    }

    /**
     * splits two adjacent triangles into four new ones
     * 
     * @param iv0
     * @param p
     * @param doAll
     * @return intermediate new triangle
     */
    private CapTriangle split2(int iv0, CapVertex p, boolean doAll) {
      // case iv0 == 0      
      //              0           00          
      //             /|\         /||\ t           
      //            / | \       / || \     
      //           1--p--2     1--pp--2
      //           2--p--1     2--pp--1
      //            \ | /       \ || /
      //             \|/      t1 \||/
      //              0           00

      int iv1 = (iv0 + 1) % 3;
      int iv2 = (iv0 + 2) % 3;

      CapTriangle t = new CapTriangle(new CapVertex[] { vs[iv0], p, vs[iv2] });
//      if (t.shortEdge >= 0) {
//        System.out.println(vs[iv0].distance(p));
//        System.out.println(vs[iv1].distance(p));
//        System.out.println(vs[iv2].distance(p));
//        System.out.println(getDims());
//      }
      t.set(null, "---split2" + this.info + this + "-----");
      CapTriangle t0 = link[iv0];
      //      if (link[iv0].tempVlist != null)
      t0.tempVlist.removeObj(p);
      int i0 = ilink[iv0];
      setLinks(iv1, t, 2, 1, link[iv0], ilink[iv0], link[iv1], ilink[iv1],
          this, iv1);
      setV(iv2, p, "split2 move " + iv2 + " to " + p);
      upldateVList(t, null);
      if (!doAll)
        return t;
      CapTriangle t1 = t0.split2(i0, p, false);
      t.link[2].link[iv0] = t1;
      t.link[2].ilink[iv0] = 0;
      t0.link[i0] = t;
      t0.ilink[i0] = 0;
      update(true);
      t.update(true);
      t0.update(true);
      t1.update(true);
      return null;
    }

    //    /**
    //     * joins two triangles into 1 -- reverse of split; presumse common edge 
    //     * @param iv 
    //     * @param iva0 
    //     * @param l2 
    //     * @param i2 
    //     * @return 
    //     * 
    //     */
    //    protected void join(int iva0, CapTriangle l2, int i2) {
    //      
    //      //
    //      //     2           2  1
    //      //    / \         /|  |\
    //      //   /   \  <--  / |  | \
    //      //  0-----1     0--1  2--0
    //      //                   
    //      
    //      int iva1 = (iva0 + 1) % 3;
    //      int iva2 = (iva0 + 2) % 3;
    //      int ivb0 = ilink[iva0];
    //      int ivb2 = (ivb0 + 2) % 3;
    //      
    //      CapTriangle t = link[iva0];
    //
    //      setV(iva1, t.vs[iva0]);
    //      
    //      link[iva0] = t.link[ivb2];
    //      ilink[iva0] = t.ilink[ivb2];
    //      
    //      link[iva2] = l2;
    //      ilink[iva2] = i2;
    //
    //      t.isValid = false;
    //      updateEdges(true);
    //    }

    protected boolean isSwappable(int iv) {

      //
      //     0           0 2
      //    / \         /| |\
      //   1-A-2       1 A B 1
      //   2-B-1  ===>  \| |/ 
      //    \ /          2 0
      //     0

      return (link[iv] != null && getUV(link[iv].vs[ilink[iv]], iv) == 3);
    }

    /**
     * swap parallelogram triangles;
     * 
     * @param iva0
     * @param isCCW
     */
    protected void swap(int iva0, boolean isCCW) {
      //
      //     0           0 2
      //    / \         /| |\
      //   1-A-2       1 A B 1
      //   2-B-1  ===>  \| |/ 
      //    \ /          2 0
      //     0

      CapTriangle b = link[iva0];

      int ivb0 = ilink[iva0];

      int i1 = (isCCW ? 1 : 2);

      int iva1 = (iva0 + i1) % 3;
      int iva2 = (iva0 + 3 - i1) % 3;
      int ivb1 = (ivb0 + i1) % 3;
      int ivb2 = (ivb0 + 3 - i1) % 3;

      setV(iva2, b.vs[ivb0], "swap with " + b.index);
      b.setV(ivb2, vs[iva0], "swap with " + index);

      link[iva0] = b.link[ivb1];
      ilink[iva0] = b.ilink[ivb1];
      link[iva0].link[ilink[iva0]] = this;
      link[iva0].ilink[ilink[iva0]] = iva0;

      b.link[ivb0] = link[iva1];
      b.ilink[ivb0] = ilink[iva1];
      b.link[ivb0].link[b.ilink[ivb0]] = b;
      b.link[ivb0].ilink[b.ilink[ivb0]] = ivb0;

      link[iva1] = b;
      ilink[iva1] = ivb1;
      b.link[ivb1] = this;
      b.ilink[ivb1] = iva1;

      update(true);
      b.update(true);
    }

    protected CapTriangle getSwappableLink(int i1, int iv, int[] swapLink) {
      CapTriangle t1 = this;
      do {
        CapTriangle t2 = t1.link[iv];
        if (t2 != null && (t2.vs[t1.ilink[iv]]).ipt == i1) {
          if (t1.isSwappable(iv)) {
            swapLink[1] = iv;
            swapLink[0] = t2.index;
            return t1;
          }
        }
        iv = (iv + 1) % 3;
        t2 = t1.link[iv];
        iv = (t1.ilink[iv] + 1) % 3;
        t1 = t2;
      } while (t1 != this);

      return null;
    }

    /**
     * check for point inclusively within a triangle
     * 
     * @param v
     * @return true if point is in or along edge of this triangle
     */
    private boolean contains(T3 v) {
      return isInTriangle(v, vs[1], vs[2], vs[0], 0);
    }

    protected boolean containsPt(int i) {
      return vs[0].ipt == i || vs[1].ipt == i || vs[2].ipt == i;
    }

    protected void draw(String color) {
      if (dumping || Logger.debugging)
        Logger.info(getDrawString(color));
    }

    private String getDrawString(String color) {
      return "draw " + color + index + "/* " + vs[0].ipt + " " + vs[1].ipt
          + " " + vs[2].ipt + " */" + vs[0] + vs[1] + vs[2] + " color " + color;
    }

    protected void update(boolean doMap) {
      edgeType = 0;
      ok = false;
      checkGeometry();
      isValid = true;
      for (int i = 3; --i >= 0;) {
        boolean isEdge = checkEdge(vs[(i + 1) % 3].ipt, vs[(i + 2) % 3].ipt, i);
        edgeType += (isEdge ? 1 << i : 0);
        info += "" + ok + i;
        if (doMap)
          vtMap.put(Integer.valueOf(vs[i].ipt), new int[] { index, i });
      }
      info += "\n";
    }

    private boolean checkEdge(int ia, int ib, int i) {

      if (ia < 0 || ib < 0)
        return (ok = isValid = false);
      // note that this is only for selected triangles
      // there is a 1:2 relationship between edges and triangles
      int[] e = edgeMap.get(slicer.getKey(ia, ib));
      if (e == null)
        return false;
      // [v[0], v[1], triangleIndex, edgeIndex]
      e[2] = index;
      e[3] = i;
      ok = (e[0] == ia);
      return true;
    }

    protected int checkEdgeSplit(int[] e, int iv) {

      T3 pt = slicer.m.vs[e[1]];
      int iuv = getUV(pt, iv);
      double u = this.u;
      double v = this.v;
      if (dumping || Logger.debugging)
        Logger.info("\n" + this + "for iv=" + iv + " " + vs[iv].ipt + " iuv="
            + iuv + " u=" + u + " v=" + v + "\n" + PT.toJSON(null, e) + "\n");
      switch (iuv) {
      case 0:
      case 1:
      case 2:
        // other point is in this triangle.
        if ((edgeType & (1 << iuv)) == 0) {
          addEdgeMap(new int[] { e[0], e[1], 0, 0 });
          update(true);
        }
        break;
      case 3:
        CapTriangle t = link[iv];
        int i = ilink[iv];
        if (isSwappable(iv)) {
          swap(iv, true);
          if (t.vs[i].ipt == e[1])
            addEdgeMap(new int[] { e[0], e[1], 0, 0 });
          break;
        }

        getUVProjection(pt, vs[iv], vTemp1, u, v);
        CapVertex vnew = new CapVertex(vTemp1, slicer.m.vc, e[5]);
        addPoint(0, -1, vnew);
        addEdgeMap(new int[] { e[0], vnew.ipt, 0, 0 });
        split2(iv, vnew, true);
        e[0] = vnew.ipt;
        e[6] = 1;// flag to continue
        break;
      case -1:
      case -2:
      case -3:
        //  continue around point
        break;
      }
      return iuv;
    }

    /**
     * given a source point pv and a target point pt, and 
     * barycentric coordinates u and v, 
     * @param pt
     * @param pv
     * @param vRet
     */
    private void getUVProjection(T3 pt, T3 pv, T3 vRet, double u, double v) {
      double f = 1 / (u + v);
      vab.set(pt.x - pv.x, pt.y - pv.y, pt.z - pv.z);
      vab.scale(f);
      vab.set(vab.x + pv.x, vab.y + pv.y, vab.z + pv.z);
      vRet.set((float) vab.x, (float) vab.y, (float) vab.z);
    }

    private void setV(int i, CapVertex p, String info) {
      vs[i] = p;
      addInfo(info);
      checkGeometry();
    }

    private void addInfo(String s) {
      info = "|" + index + " " + info + "\n" + s;
    }

    /**
     * sets uv for specified point p and vertex i
     * 
     * @param p
     * @param i
     * @return indicator of directionality on edge: edge number 0-2 off edge: -1
     *         - (edge number) in range: 3
     */
    protected int getUV(T3 p, int i) {

      // i=0; [return]
      //     u
      //    /__v     [1]          
      //              2(v=0)  p [3]
      // [-1-1] v<0  / \  *          
      //        u>0 / * \     
      //       ----0-----1(u=0)[2]
      //      v<0 /   u<0
      //[-1-2]u<0/    v>0 [-1-2]  

      isInTriangle(p, vs[i], vs[(i + 1) % 3], vs[(i + 2) % 3], index);
      return (u == 0 && v > 0 ? (i + 2) % 3 : v == 0 && u > 0 ? (i + 1) % 3
          : u > 0 && v > 0 ? 3
          //          : u < 0 ? -1 - (i + 2) % 3 
              : -1 - (i + 1) % 3);
    }

    private double u, v;

    /**
     * Also sets v (normalized distance along v) and u (normalized distance along
     * c
     * 
     * @param p
     * @param a
     * @param b
     * @param c
     * @param id
     * @return true when within abc and not on ab and not on bc but possibly on bc
     */
    private boolean isInTriangle(T3 p, T3 a, T3 b, T3 c, int id) {
      // from http://www.blackpawn.com/texts/pointinpoly/default.html
      // Compute barycentric coordinates

      //                v      
      //               /  
      //              c(v=0,u=1)   
      //             / \      
      //            / p \     
      //           a-----b(u=0,v=1) -- u
      //                 
      //             

      vap.set(-a.x, -a.y, -a.z);
      vab.set(b.x, b.y, b.z);
      vab.add(vap);
      vac.set(c.x, c.y, c.z);
      vac.add(vap);
      vap.x += p.x;
      vap.y += p.y;
      vap.z += p.z;
      double cdotc = vac.dot(vac);
      double cdotb = vac.dot(vab);
      double cdotp = vac.dot(vap);
      double bdotb = vab.dot(vab);
      double bdotp = vab.dot(vap);
      double invDenom = 1 / (cdotc * bdotb - cdotb * cdotb); // 1/(cc*bb - cb*cb)
      if (invDenom > 1e12)
        invDenom = 1e12;
      u = (bdotb * cdotp - cdotb * bdotp) * invDenom; // 0 when p == b   1 when p == c
      v = (cdotc * bdotp - cdotb * cdotp) * invDenom; // 1 when p == b   0 when p == c
      if (u != 0 && Math.abs(u) < 1e-4)
        u = 0;
      if (v != 0 && Math.abs(v) < 1e-4)
        v = 0;
      // allow on new edge bc only
      return (u >= 0 && v >= 0 && u + v <= 1);
    }

    private void setLinks(int i, CapTriangle t, int edgeToThis, int edgeToLink,
                          CapTriangle t0, int i0, CapTriangle t1, int i1,
                          CapTriangle t2, int i2) {

      //              2               2[2]                    1[2]
      //             / \ L[0]         | \                    / |
      //       L[1] / v \       t3[0] |  \ this[0]  this[1] /  | t2[1]  
      //           0-----1         [v]0 t2\                /t3 0[v] 
      //             L[2]               ^  \              /  ^ 
      //                        this[0]  ^ 1[1]       [0]2 ^  this[1]

      t.link[0] = t0;
      t.ilink[0] = i0;
      t.link[1] = t1;
      t.ilink[1] = i1;
      t.link[2] = t2;
      t.ilink[2] = i2;

      if (link[i] != null) {
        link[i].link[ilink[i]] = t;
        link[i].ilink[ilink[i]] = edgeToLink;
      }

      link[i] = t;
      ilink[i] = edgeToThis;

    }

    private String spts(int iv0) {
      CapTriangle t = (iv0 < 0 ? this : link[iv0]);
      if (t == null)
        return "";
      int iv = (iv0 < 0 ? -1 : ilink[iv0]);
      return (iv < 0 ? "#" : "\n# (" + iv0 + ")") 
          + "["
          + t.index
          + ": "
          + t.vs[0].ipt
          + " "
          + t.vs[1].ipt
          + " "
          + t.vs[2].ipt
          + "]"
          + iv
          + " "
          + t.ok
          + " "
          + t.edgeType
          + t.getDims()
          + "\n"
          + t.getDrawString(iv == -1 ? (ok ? "green" : "red") 
              : iv0 == 0 ? "yellow"
              : iv0 == 1 ? "orange" : "blue");
    }

    @Override
    public String toString() {
      return spts(-1)
          + (link == null ? "" : spts(0) + spts(1) + spts(2)) + "\n";
    }

  }

}
