package org.jmol.util;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.java.BS;

import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.Measure;
import javajs.util.P4;
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
public class MeshCapper {

  public MeshCapper() {
    // for reflection
  }
  
  private Map<Integer, Integer> capMap;
  private Lst<int[]> capEdges;

  protected MeshSlicer slicer;
  private V3d vab, vac, vap;
  protected V3 vTemp0, vTemp1, vTemp2;
  protected Lst<CapTriangle> triangles;
  protected Map<Integer, int[]> vtMap;
  protected Map<String, int[]> edgeMap;
  private Lst<CapVertex> vertices;


  MeshCapper set(MeshSlicer slicer) {
    this.slicer = slicer;
    return this;
  }

  void clear() {
    vab = new V3d();
    vac = new V3d();
    vap = new V3d();
    vTemp0 = new V3();
    vTemp1 = new V3();
    vTemp2 = new V3();
    capEdges = new Lst<int[]>();
    capMap = new Hashtable<Integer, Integer>();
    triangles = new Lst<CapTriangle>();
    vtMap = new Hashtable<Integer, int[]>();
    edgeMap = new Hashtable<String, int[]>();
    vertices = new Lst<CapVertex>();
  }
  
  protected int addPoint(int thisSet, int i, T3 pt) {
    boolean isEdge = (pt == null);
    Integer ii = Integer.valueOf(i);
    Integer inew = capMap.get(ii);
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
      System.out.println(i + "\t" + slicer.m.vs[i]);
    }
    return inew.intValue();
  }
  
  void addEdge(int iE, int iD, int thisSet) {
    capEdges.addLast(new int[] { addPoint(thisSet, iE, null),
        addPoint(thisSet, iD, null), -1, -1, 0, thisSet, 0 });
  }
  
  void createCap() {

    if (capEdges.size() < 3)
      return;

    MeshSurface m = slicer.m;

    // map edges for recall

    for (int i = capEdges.size(); --i >= 0;)
      addEdgeMap(capEdges.get(i));

    // right-hand rule for outward normal
    int iF = capEdges.get(0)[0];
    int iG = capEdges.get(capEdges.size() - 1)[0];
    if (iF == iG)
      return;
    V3 vab = V3.newVsub(m.vs[iF], m.vs[iG]);
    V3 vac = V3.newV(slicer.norm);
    vac.cross(vac, vab);
    V3 vap = new V3();

    // get transform to xy plane

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
    //System.out.println("draw d0 " + p0 + " color red");
    //System.out.println("draw d1 " + p1 + " color green");
    //System.out.println("draw d2 " + p2 + " color blue");

    // split into net of points, all vertices of triangles

    triangles
        .addLast(new CapTriangle(new CapVertex[] { p0, p1, p2 }, vertices));
    for (int i = 0; i < triangles.size();)
      i += triangles.get(i).split();

    for (int i = 0; i < triangles.size(); i++) {
      CapTriangle t = triangles.get(i);
      t.draw();

    }

    System.out.println("#OK");

    // check for edges that are not part of a triangle

    for (int i = capEdges.size(); --i >= 0;) {
      int[] e = capEdges.get(i);
      if (e[2] < 0 || e[6] == 1) {
        int v0 = e[0];
        int v1 = e[1];
        int[] vt0 = vtMap.get(Integer.valueOf(v0));
        int[] vt1 = vtMap.get(Integer.valueOf(v1));

        System.out.println("/////");
        System.out.println(PT.toJSON(null, e)
            + PT.toJSON(null, vt0)
            + PT.toJSON(null, vt1));
        
        int[] swapInfo = new int[2];
        CapTriangle t = getSwappableLink(v0, v1, vt0, vt1, swapInfo);
        if (t != null){
          t.swap(triangles.get(swapInfo[0]), swapInfo[1], true, true);
          continue;
        }

        int[] t_v = vtMap.get(Integer.valueOf(e[0]));
        int iuv;
        t = triangles.get(t_v[0]);
        int iv = t_v[1];
        
        // get first point's triangle 

        while ((iuv = t.checkEdgeSplit(e, iv)) < 0) {
          iuv = -1 - iuv;
          iv = (t.ilink[iuv] + 3 + iuv - iv) % 3;
          t = t.link[iuv];
        }
        if (iuv == 3)
          i++;
        
        
      }
    }

      BS bsCheck = new BS();
      for (int i = triangles.size(); --i >= 0;) {
        CapTriangle t = triangles.get(i);
        //t.ok = true;
        t.checkMaps(false);
        if (t.ok && !t.hasEdge) {
          System.out.println(t);
          if (!t.checkLinks()) {
            t.ok = false;
          } else {
            bsCheck.set(i);
          }
        }
      }

      boolean ok = true;
      while (ok) {
        ok = false;
        for (int i = bsCheck.nextSetBit(0); i >= 0; i = bsCheck
            .nextSetBit(i + 1)) {
          CapTriangle t = triangles.get(i);
          if (!t.checkLinks()) {
            t.ok = false;
            bsCheck.clear(i);
            ok = true;
          }
        }
      }

    for (int i = triangles.size(); --i >= 0;) {
      CapTriangle t = triangles.get(i);
      if (t.ok) {
        m.addPolygonV3(t.vs[0].ipt, t.vs[1].ipt, t.vs[2].ipt, 0, 0, 0,
            m.bsSlabDisplay);
      }
    }

    clear();

    //    // old centering code
    //    P3 center = new P3();
    //    for (int i = capEdges.size(); --i >= 0;) {
    //      int[] edge = capEdges.get(i);
    //      center.add(m.vs[edge[0]]);
    //      center.add(m.vs[edge[1]]);
    //    }
    //    center.scale(1 / (2 * capEdges.size()));
    //    int thisSet = (m.vertexSets == null ? 0 : m.vertexSets[0]);
    //    int ic = slicer.addIntersectionVertex(center, 0, -1, thisSet, null, -1, -1);
    //    for (int i = capEdges.size(); --i >= 0;) {
    //      int[] edge = capEdges.get(i);
    //      m.addPolygonV3(edge[0], edge[1], ic, 0, 0, 0, m.bsSlabDisplay);
    //    }
  }

  private CapTriangle getSwappableLink(int i0, int i1, int[] vt0, int[] vt1,
                                       int[] swapLink) {
    CapTriangle t0 = triangles.get(vt0[0]);
    int iv = vt0[1];
    if (t0.index == 526)
      System.out.println("test526");

    CapTriangle t1 = t0;
    do {
      CapTriangle t2 = t1.link[iv];
      CapVertex v2;
      if (t2 != null && (v2 = t2.vs[t1.ilink[iv]]).ipt == i1) {
        if (t1.getUV(v2, iv) == 3) {
          swapLink[1] = iv;
          swapLink[0] = t2.index;

          return t1;
        }
        System.out.println("no swap  " + t1 + " " + t2 + " " + u + " " + v);
        t1.draw();
        t2.draw();
      }
      iv = (iv + 1) % 3;
      t2 = t1.link[iv];
      iv = (t1.ilink[iv] + 1) % 3;
      t1 = t2;
      System.out.println(t0 + " " + t1);
    } while (t1 != t0);

    return null;
  }

  protected void addEdgeMap(int[] e) {
    edgeMap.put(slicer.getKey(e[0], e[1]), e);
  }

  double u, v;
  
  /**
   * Also sets v (normalized distance along v) and u (normalized distance along c
   * @param p
   * @param a
   * @param b
   * @param c
   * @return true when within abc and not on ab and not on bc but possibly on bc
   */
  protected boolean isInTriangle(T3 p, T3 a, T3 b, T3 c, int id) {
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
    double invDenom = 1 / (cdotc * bdotb - cdotb * cdotb);  // 1/(cc*bb - cb*cb)
    if (invDenom > 1e12)
      invDenom = 1e12;
    u = (bdotb * cdotp - cdotb * bdotp) * invDenom;  // 0 when p == b   1 when p == c
    v = (cdotc * bdotp - cdotb * cdotp) * invDenom;  // 1 when p == b   0 when p == c
    System.out.println("draw p" + id + p);
    System.out.println("draw a" + id + a);
    System.out.println("draw ab" + id + " vector " + a + vab + " width 0.001 '>"+id+"'");
    System.out.println("draw ac" + id + " vector " + a + vac + " width 0.001");
    if (invDenom > 1e12) {
      // 180-degree angle in triangle
      System.out.println("draw p " + p);
      System.out.println("draw a" + a);
      System.out.println("draw abc " + a + b + c);
      return false;
    }
    if (Double.isNaN(u))
      System.out.println("HOH");
    // allow on new edge bc only
    return (u >= 0 && v >= 0 && u + v <= 1);
  }
  
  protected void drawFind(int ipt) {
    for (int i = triangles.size(); --i >= 0;)
      if (triangles.get(i).containsPt(ipt)) {
        triangles.get(i).draw();
      }
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
    
    protected int index;
    private Lst<CapVertex> vertices;
    protected CapVertex[] vs;
    protected CapTriangle[] link;
    protected int[] ilink;
    protected boolean ok;
    protected boolean hasEdge;
    protected boolean isValid;
        
    CapTriangle(CapVertex[] points, Lst<CapVertex> vertices) {
      link = new CapTriangle[3];
      ilink = new int[3];
      index = triangles.size();
      isValid = ok = true;
      this.vertices = vertices;
      vs = points;
      
      if (containsPt(22988)) {
        draw();
        drawFind(22988);
      }


      checkMaps(true);
    }

    
    /**
     * splits a triangle into two adjacent triangles, adding a new links for edge 0
     *  
     * @param iv0
     * @param v2   end of edge
     */
    
    protected CapVertex split1(int iv0, CapVertex v2, CapTriangle l1, int i1, CapTriangle l2, int i2, boolean isEdge) {
      
      //
      //     0           0  0
      //    / \         /|  |\
      //   /   \  -->  / |  | \
      //  1--v--2     1--v  v--2
      //                   
      
      int iv1 = (iv0 + 1) % 3;
      int iv2 = (iv0 + 2) % 3;
      CapVertex v;
      if (isEdge) {
      vTemp0.add2(vs[iv1], slicer.norm);
      P4 plane = Measure.getPlaneThroughPoints(vs[iv1], vs[iv2], vTemp0, vTemp1, vTemp2, new P4());
      vTemp0.sub2(v2, vs[iv0]);
      v = new CapVertex(Measure.getIntersection(vs[iv0], vTemp0, plane, new P3(), vTemp1, vTemp2), slicer.m.vc, vs[iv0].set);
      addPoint(0, -1, v);
      } else {
        v = v2;
      }
      if (l2 == null) {
        l2 = link[iv0];
        i2 = ilink[iv0];
      }
      CapTriangle t = new CapTriangle(new CapVertex[] {vs[iv0], v, vs[iv1] }, null);
      t.setLinks(iv0, t, 2, 1, l2, i2, link[iv1], ilink[iv1], this, iv1);
      vs[iv2] = v;
      if (l1 != null) {
        link[iv0] = l1;
        ilink[iv0] = i1;
      }
      link[iv1] = t;
      ilink[iv1] = 2;
      
      if (isEdge) {
        ok = true;
        t.ok = false;
      }
      return v;
    }

    /**
     * joins two triangles into 1 -- reverse of split; presumse common edge 
     * @param iva0 
     * @param l2 
     * @param i2 
     * 
     */
    
    protected void join(int iva0, CapTriangle l2, int i2) {
      
      //
      //     2           2  1
      //    / \         /|  |\
      //   /   \  <--  / |  | \
      //  0-----1     0--1  2--0
      //                   
      
      int iva1 = (iva0 + 1) % 3;
      int iva2 = (iva0 + 2) % 3;
      int ivb0 = ilink[iva0];
      int ivb1 = (ivb0 + 1) % 3;
      int ivb2 = (ivb0 + 2) % 3;
      
      CapTriangle t = link[iva0];

      vs[iva1] = t.vs[iva0];
      
      link[iva0] = t.link[ivb2];
      ilink[iva0] = t.ilink[ivb2];
      
      link[iva2] = l2;
      ilink[iva2] = i2;

      t.isValid = false;
    }

    /**
     * swap parallelogram triangles;
     * @param b 
     * @param iva0 
     * @param isCCW 
     * @param isEdge 
     */
    protected void swap(CapTriangle b, int iva0, boolean isCCW, boolean isEdge) {
      //
      //     0           0 2
      //    / \         /| |\
      //   1-A-2       1 A B 1
      //   2-B-1  ===>  \| |/ 
      //    \ /          2 0
      //     0

      int ivb0 = ilink[iva0];

      if (index == 175 || b.index == 175)
      System.out.println("swapping "+ this + " with" + b + " across " + iva0 + " " + ivb0);

      int i1 = (isCCW ? 1 : 2);
      
      int iva1 = (iva0 + i1) % 3;
      int iva2 = (iva0 + 3 - i1) % 3;
      int ivb1 = (ivb0 + i1) % 3;
      int ivb2 = (ivb0 + 3 - i1) % 3;

      vs[iva2] = b.vs[ivb0];
      b.vs[ivb2] = vs[iva0];
      
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
      
      checkMaps(true);
      b.checkMaps(true);
      if (isEdge) {
        b.ok = true;
        ok = false;
      }
      System.out.println("now "+ this + " and" + b);
      
    }

    protected boolean containsPt(int i) {
      return vs[0].ipt == i || vs[1].ipt == i || vs[2].ipt == i;
    }


    protected boolean checkLinks() {
      for (int i = 0; i < 3; i++)
        if (link[i] != null && link[i].ok)
          return true;
      return false;
    }


    protected void draw() {
      System.out.println("draw d" +index + "/* " +vs[0].ipt + " " + vs[1].ipt + " " + vs[2].ipt+" */" + vs[0] + vs[1] + vs[2] + " color red");
    }
    
    protected void checkMaps(boolean doMap) {
      hasEdge = false;
      if (doMap)
        ok = true;
      for (int i = 0; i < 3; i++) {
        boolean isEdge = checkMap(vs[i].ipt, vs[(i + 1) % 3].ipt, (i + 2)%3, true);
        hasEdge |= isEdge;
        if (doMap)
          vtMap.put(Integer.valueOf(vs[i].ipt), new int[] { index, i });
      }
    }


    protected int checkEdgeSplit(int[] e, int iv) {

      // corner start:
      //  1) check terminal -- if so, swap CCW 1,2 and return
      //  2) project point 1->2
      //  3) split 2->2'
      //  4) swap CCW(2',1)
      //  5) join(2, 2')--> 2
      //  6) go to edge start

      // edge start
      //  1) split 2->2'
      //  2) swap CW(2',1)
      //  3) if (terminating) return
      //  4) goto corner start

      // get second point's direction relative to this point

      T3 pt = slicer.m.vs[e[1]];
      int iuv = getUV(pt, iv);

      System.out.println("\n for iv=" + iv + " " + vs[iv].ipt + " iuv=" + iuv
          + " u=" + u + " v=" + v + "\n" + this + PT.toJSON(null, e) + "\n");
      switch (iuv) {
      case 0:
      case 1:
      case 2:
        // other point is in this triangle.
        // need to OK this one AND linked
        ok = ((e[0] == vs[iv].ipt) == ((iuv + 1) % 3 == iv));
        link[iuv].ok = !ok;
        break;
      case 3:

        //        if (index==551) {
        //          drawFind(22984);
        //        }

        CapTriangle t = link[iv];
        int i = ilink[iv];
        CapVertex vnew,
        vend;

        if (false) {
//          vend = (CapVertex) slicer.m.vs[e[1]];
//          vnew = split1(iv, vend, null, -1, null, -1, true);
//          int j;
//          CapVertex vnew = t.project(vs[iv], vend, j = (i + 1) % 3);
//          if (vnew == null)
//            vnew = t.project(vs[iv], vend, j = (i + 2) % 3);
//          t.split1(j, vnew, null, -1, null, -1, false);
//          swap(t, iv, true, true);
//          while (true) {
//            t = t.link[j];
//            i = t.ilink[j];
//            if (t.vs[i].ipt == e[1])
//              break;
//            vnew2 = t.project(vs[iv], vend, j = (i + 1) % 3);
//            if (vnew == null)
//              vnew = t.project(vs[iv], vend, j = (i + 2) % 3);
//          }
//          // we are done
        } else {
          
          V3d p = new V3d();
          p.set(pt.x - vs[iv].x, pt.y - vs[iv].y, pt.z - vs[iv].z);
          p.scale(1 / (u + v));
          p.set(p.x + vs[iv].x, p.y + vs[iv].y, p.z + vs[iv].z);
          vnew = new CapVertex(P3.new3((float) p.x, (float) p.y, (float) p.z),
              slicer.m.vc, e[5]);
          addPoint(0, -1, vnew);
          addEdgeMap(new int[] { e[0], vnew.ipt, 0, 0 });

          CapTriangle t1 = split2(iv, vnew, null, 0);
          t.split2(i, vnew, t1, iv);
          e[0] = vnew.ipt;
          e[6] = 1;// flag to continue
          checkMaps(true);

        }
        //        if (containsPt(22984) || t.containsPt(22984) || t1.containsPt(22984)) {
        //          drawFind(22984);
        //        }

        break;
      case -1:
      case -2:
      case -3:
        //        if (containsPt(22988))
        //          drawFind(22988);
        // need to scan linked triangle
        break;
      }
      return iuv;
    }

    private CapTriangle split2(int iv0, CapVertex p, CapTriangle tprev, int ivprev) {
      // case iv0 == 0      
      //              0               0[0]          
      //             /|\ L[1]         | \           
      //       L[2] / |t\       this  |t \ this[1]    
      //           1--p--2         [p]1---2
      //            **
      //           2--p--1
      //            \ | /
      //             \|/
      //              0

      int iv1 = (iv0 + 1) % 3;
      int iv2 = (iv0 + 2) % 3;

      CapTriangle t = new CapTriangle(new CapVertex[] { vs[iv0], p, vs[iv2] }, null);
      triangles.addLast(t);
      setLinks(iv1, t, 2, 1, link[iv0], ilink[iv0], link[iv1], ilink[iv1], this, iv1);
      vs[iv2] = p;
      if (tprev != null) {
        // link **
        tprev.link[2].link[ivprev] = t;
        tprev.link[2].ilink[ivprev] = 0;
        link[iv0] = tprev;
        ilink[iv0] = 0;
      }
      return t;
    }

    /**
     * sets uv for specified point p and vertex i
     * 
     * @param p
     * @param i
     * @return indicator of directionality
     *     on edge: edge number 0-2
     *     off edge: -1 - (edge number)
     *     in range: 3
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

      isInTriangle(p, vs[i], vs[(i + 1)%3], vs[(i + 2)%3], index);
      return (u == 0 ? (i + 2) % 3 
          : v == 0 ? (i + 1) % 3 
          : u > 0 && v > 0 ? 3
//          : u < 0 ? -1 - (i + 2) % 3 
          : -1 - (i + 1) % 3);
    }
    

    private boolean checkMap(int ia, int ib, int i, boolean markOK) {
      if (ia < 0 || ib < 0)
        return (ok = false);
      if(ia == 22092 || ib == 22092)
        System.out.println("testing3333");
      // note that this is only for selected triangles
      // there is a 1:2 relationship between edges and triangles
      int[] e = edgeMap.get(slicer.getKey(ia, ib));
      if (e == null)
        return false;
      // [v[0], v[1], triangleIndex, edgeIndex]
      e[2] = index;
      e[3] = i;
      if (markOK)
        ok = (e[0] == ia);
      return true;
    }


    protected int split() {
      if (index==12) {
        draw();
      }
      int i = vertices.size();
      if (i == 0)
        return 1;
      i = i / 2;
      CapVertex v = vertices.remove(i);
      Lst<CapVertex> list2 = new Lst<CapVertex>();
      Lst<CapVertex> list3 = new Lst<CapVertex>();
      CapTriangle t2 = new CapTriangle(new CapVertex[] {v, vs[1], vs[2]}, list2);
      triangles.addLast(t2);
      CapTriangle t3 = new CapTriangle(new CapVertex[] {v, vs[2], vs[0]}, list3);
      triangles.addLast(t3);
      

      //              2               2[2]                    1[2]
      //             / \ L[0]         | \                    / |
      //       L[1] / v \       t3[2] |  \ this[0]  this[1] /  | t2[1]  
      //           0-----1         [v]0 t2\                /t3 0[v] 
      //             L[2]               ^  \              /  ^ 
      //                        this[0]  ^ 1[1]       [0]2 ^  this[1]
      
      setLinks(0, t2, 2, 0, link[0], ilink[0], t3, 2, this, 0);
      setLinks(1, t3, 1, 0, link[1], ilink[1], this, 1, t2, 1);
      vs[2] = v;

//        if (containsPt(22988) || t3.containsPt(22988) || t2.containsPt(22988)) {
//          drawFind(22988);
//        }

        checkMaps(true);
      t2.checkMaps(true);
      t3.checkMaps(true);
      for (i = vertices.size(); --i >= 0;) {
        CapVertex v3 = vertices.get(i);
        if (t2.contains(v3))
          list2.addLast(vertices.remove(i));
        else if (t3.contains(v3))
          list3.addLast(vertices.remove(i));
        else if (index == 12)
          System.out.println("draw v" + i + " " + v3 + "'" + (v3.ipt%100) + "'");
      }
      return 0;
    }


    private void setLinks(int i, CapTriangle t, int edgeToThis, int edgeToLink, CapTriangle t0,
                          int i0, CapTriangle t1, int i1,
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


    private boolean contains(CapVertex v) {
      return isInTriangle(v, vs[1], vs[2], vs[0], 0);
    }
    
    private String spts(CapTriangle t) {
      return t == null ? "" : "[" + t.index + ": " + t.vs[0].ipt + " " + t.vs[1].ipt + " " + t.vs[2].ipt + "]";
    }

    @Override
    public String toString() {
      return spts(this) + " " + ok + 
          "\n (0)" + spts(link[0]) + ilink[0] + "," +
          "\n (1)" + spts(link[1]) + ilink[1] + "," + 
          "\n (2)" + spts(link[2]) + ilink[2] + "\n";
    }


  }

}
