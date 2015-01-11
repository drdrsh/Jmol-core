package org.jmol.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map;

import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.Quat;
import javajs.util.P3;
import javajs.util.V3;
import javajs.util.T3;
import javajs.util.V3d;

/**
 * A class to properly cap a convoluted, closed slice of an isosurface
 * 
 * inspired by: Computational Geometry: Algorithms and Applications Mark de
 * Berg, Marc van Kreveld, Mark Overmars, and Otfried Schwarzkopf
 * Springer-Verlag, Berlin Heidelberg 1997 Chapter 3. Polygon Triangulation
 * 
 * Thanks given to Olaf Hall-Holt for pointing me to this reference.
 * 
 * Extensively modified:
 * 
 * - quaternion transform from 3D plane to XY plane for best precision
 * 
 * - using directional edges -- no angle measurements necessary
 * 
 * - continuous splitting off of triangles
 * 
 * - independent dynamic monotonic regions created and processed as one stream
 * 
 * - no need for vertex typing
 * 
 * - no push/pop stacks
 * 
 * INPUT: stream of [a b] ordered-vertex edges such that triangle a-b-c is
 * interior if (ab.cross.ac).dot.planeNormal > 0 (right-hand rule;
 * counter-clockwise edge flow)
 * 
 * Bob Hanson - Jan 11, 2015
 * 
 * @author Bob Hanson, hansonr@stolaf.edu
 */
public class MeshCapper {

  public MeshCapper() {
    // for reflection
  }

  /**
   * source of edges; consumer of triangles
   */
  protected MeshSlicer slicer;

  /**
   * for debugging
   */
  protected boolean dumping;

  /**
   * initialization only
   */
  private Map<Integer, CapVertex> capMap;
  private Lst<CapVertex> vertices;
  private int ipt0;

  /**
   * dynamic region processing. These are just 
   * [DESCENDER, ASCENDER, LAST] for each region 
   * 
   */
  protected Lst<CapVertex[]> lstRegions;
  
  /**
   * informational only
   */
  protected int nTriangles;

  /**
   * temporary storage
   */
  protected V3d vab, vac, vap;
  protected V3 vTemp0, vTemp1, vTemp2;

  
  /////////////// initialization //////////////////

  /**
   * @param slicer 
   * @param resolution  
   * @return this
   */
  MeshCapper set(MeshSlicer slicer, float resolution) {
    // resolution has not been necessary
    this.slicer = slicer;
    return this;
  }

  void clear() {
    vab = new V3d();
    vac = new V3d();
    vap = new V3d();
    vTemp0 = new V3();
    vTemp1 = null;
    vTemp2 = new V3();
    capMap = new Hashtable<Integer, CapVertex>();
    vertices = new Lst<CapVertex>();
    lstRegions = new Lst<CapVertex[]>();
  }

  /**
   * Pointers are into MeshSlicer.m.vs[]
   * 
   * @param ipt1
   * @param ipt2
   * @param thisSet
   */

  void addEdge(int ipt1, int ipt2, int thisSet) {
    CapVertex v1 = addPoint(thisSet, ipt1);
    CapVertex v2 = addPoint(thisSet, ipt2);
    v1.link(v2);
    // temporary variables store first and last vertex
    if (vTemp1 == null) {
      vTemp1 = V3.newV(v1);
      ipt0 = ipt1;
    } else if (ipt2 != ipt0) {
      vTemp2.setT(v2);
    }
  }

  /**
   * The MeshSlicer class manages all introduction of vertices; we must pass on
   * to it the subset of vertices from the original Jmol isosurface being
   * capped.
   * 
   * @param thisSet
   * @param i
   * @return a CapVertex pointing to this new point in the isosurface or one we
   *         already have
   */
  protected CapVertex addPoint(int thisSet, int i) {
    Integer ii = Integer.valueOf(i);
    CapVertex v = capMap.get(ii);
    if (v == null) {
      T3 pt = slicer.m.vs[i];
      i = slicer.addIntersectionVertex(pt, 0, -1, thisSet, null, -1, -1);
      vertices.addLast(v = new CapVertex(pt, i));
      capMap.put(ii, v);
    }
    if (dumping)
      Logger.info(i + "\t" + slicer.m.vs[i]);
    return v;
  }

  /////////////// processing //////////////////

  /**
   * Entry point when finished generating edges.
   * 
   */
  void createCap() {

    capMap = null;

    Logger.info("MeshCapper for " + vertices.size() + " vertices");

    CapVertex[] vs = new CapVertex[vertices.size()];

    if (vs.length < 3)
      return;

    // get transform to xy plane
    // to give best precision

    V3 vab = V3.newVsub(vTemp2, vTemp1);
    V3 vac = V3.newV(slicer.norm);
    vac.cross(vac, vab);

    //Get xy plane points
    Quat q = Quat.getQuaternionFrameV(vab, vac, null, false);
    M3 m3 = q.getMatrix();
    M4 m4 = M4.newMV(m3, vTemp1);
    M4 m4inv = M4.newM4(m4).invert();
    vertices.toArray(vs);
    for (int i = vs.length; --i >= 0;)
      m4inv.rotTrans2(vs[i], vs[i]);
    vertices = null;
    
    // link by Y,X sort

    CapVertex v0 = vs[0].sort(vs);
    if (v0 == null) {
      Logger.error("two identical points");
      return;
    }
    
    // scan the plane
    
    CapVertex v = v0;
    do {
      v = v.process();
    } while (v != v0);
    clear();

    Logger.info("MeshCapper created " + nTriangles + " triangles");

  }

  private class CapVertex extends P3 implements Cloneable,
      Comparator<CapVertex> {

    /**
     * external reference
     */
    int ipt;

    /**
     * for debugging
     */
    String id = "";

    /**
     * Y-X scan queue forward link
     */
    protected CapVertex qnext;

    /**
     * edge double links
     */
    CapVertex prev;
    CapVertex next;

    /**
     * dynamic region pointers
     */

    private static final int DESCENDER = 0;
    private static final int ASCENDER = 1;
    private static final int LAST = 2;
    CapVertex[] region;

    /**
     * unique vertex test
     * 
     */
    protected int ok = 1;

    CapVertex(T3 p, int i) {
      ipt = i;
      id = "" + i;
      x = p.x;
      y = p.y;
    }

    /**
     * Generate qnext links based on scanning Y large to Y small and if Y1==Y2,
     * then X small to large
     * 
     * @param vs
     * @return null if there are two identical points (edge crossings)
     */
    public CapVertex sort(CapVertex[] vs) {
      Arrays.sort(vs, this);
      if (ok == 0)
        return null;
      for (int i = vs.length - 1; --i >= 0;)
        vs[i].qnext = vs[i + 1];
      vs[vs.length - 1].qnext = vs[0];
      return vs[0];
    }

    @Override
    public int compare(CapVertex v1, CapVertex v2) {
      // first HIGHEST Y to LOWEST Y, then LOWEST X to HIGHEST X
      return (v1.y < v2.y ? 1 : v1.y > v2.y || v1.x < v2.x ? -1
          : v1.x > v2.x ? 1 : (ok = 0));
    }

    /**
     * Handle the point; mark as processed.
     * 
     * @return next point to process
     */
    protected CapVertex process() {
      //
      //                    /\
      //                   /  \ascending
      //       descending /    \
      //                 /      \
      //                /        \
      //              -/----------*-<
      //              /            \

      CapVertex q = qnext;
      qnext = null; // indicates already processed
      if (dumping)
        Logger.info(this.toString());
      if (prev == next)
        return q;

      boolean isDescending = (prev.region != null);
      boolean isAscending = (next.region != null);

      if (dumping)
        Logger.info("#" + (isAscending ? next.id : "    ") + "    "
          + (isDescending ? prev.id : "") + "\n#"
          + (isAscending ? "   \\" : "    ")
          + (isDescending ? "    /\n" : "\n") + "#    " + id);

      if (!isDescending && !isAscending) {
        CapVertex v = getClosestMinPoint();
        if (v == null) {
          // start vertex -- just create a new region
          newRegion();
          return q;
        }
        CapVertex p = processSplit(v);
        // patch in new point as the next to process
        p.qnext = q;
        q = p;
        // process left branch
        isAscending = true;
      }

      // note that a point may be both ascending and descending:

      //
      //                    /\         /\
      //                   /  \       /  \
      //                  /    \     /    \
      //                 /   next   prev   \
      //                /        \ /        \
      //              -/----------*----------\<
      //              /                       \

      if (isDescending) {
        processMonotonic(true);
      }
      if (isAscending) {
        processMonotonic(false);
      }

      if (isDescending && isAscending) {
        if (prev.prev == next) {
          // end vertex -- draw last triangle
          lstRegions.removeObj(region);
          addTriangle(prev, this, next, "end");
          prev.clear();
          next.clear();
        } else {
          // merge vertex -- linking two separate regions
          // just mark as having no region yet
          region = null;
        }

      }
      return q;
    }

    /**
     * 
     * Process what M3O refer to as a "split" vertex, which we handle
     * differently here, cloning the "helper" point and the "split" point,
     * creating a new region if necessary, and then swapping pointers.
     * 
     * @param v
     *        "helper" or left edge
     * @return new point clone of this
     * 
     */
    private CapVertex processSplit(CapVertex v) {

      CapVertex p = null, pv = null;
      try {
        pv = (CapVertex) v.clone();
        pv.id += "a";
        p = (CapVertex) clone();
        p.id += "a";
      } catch (Exception e) {
        // ignore
      }

      if (v.region == null) {

        // split is to a merge vertex
        //
        //                    /\
        //          v.next   /  \
        //                \ /    \
        //                 v(pv)--\-
        //                         \
        //              ------*(p)--\-<
        //                   / \     
        // becomes
        //
        //                      /\
        //          v.next     /  \
        //                \   /    \
        //                 v pv-----\-
        //                  \ \      \
        //                   * p------\-<
        //                  /   \     

        v.region = v.next.region;
        pv.region = v.prev.region;

      } else {

        // split is to an edge, requiring a new region

        //                    /\
        //                   /  \
        //                  /    \
        //                 v(pv)----
        //                /        \
        //              -/----*(p)--\-<
        //           v.next  / \     \

        // becomes
        //
        //                      /\
        //                     /  \
        //                    /    \
        //                 v pv     \    
        //                / \ \      \
        //              -/---* p------\-<
        //           v.next /   \      \     

        v.newRegion();

        // It is possible for v.next to be above. This will happen
        // in the case where we have just a single edge with d above a.

        CapVertex cv = v;
        while (cv.next.region != null) {
          cv.next.region = cv.region;
          cv = cv.next;
          cv.region[DESCENDER] = cv;
        }
      }

      // fix region references

      CapVertex[] r = pv.region;
      if (r[LAST] == v)
        r[LAST] = pv;
      r[DESCENDER] = pv;
      if (r[ASCENDER] == v)
        r[ASCENDER] = pv;

      // patch new edges

      link(v);
      pv.prev.link(pv);
      pv.link(p);
      p.link(p.next);

      //System.out.println("#split v=" + v + "\n#p=" + pv);

      return p;
    }

    private void newRegion() {
      //System.out.println("\n\n#new region for " + id);
      lstRegions.addLast(region = new CapVertex[] { this, this, this });
    }

    /**
     * Find the lowest ascender or descender above scan line bounding the region
     * for this point. In the case of a region that consists of a single edge
     * with descender above ascender, this will return the ascender.
     * 
     * [This is MOST confusing in the M3O book.]
     * 
     * @return pt
     */
    private CapVertex getClosestMinPoint() {

      //  return a:
      //                    /\
      //                   /  \
      //                  /    \
      //                 d      \
      //                /        a
      //              -/----*-----\-<
      //              /

      CapVertex closest = null;
      float ymin = Float.MAX_VALUE;
      for (int i = lstRegions.size(); --i >= 0;) {
        CapVertex[] r = lstRegions.get(i);
        // check left edge
        CapVertex d = r[DESCENDER];
        if (d == r[ASCENDER])
          continue;
        float xp = (d.region == null ? d.x : interpolateX(d, d.next));
        if (xp > x)
          continue;
        // check right edge
        CapVertex a = r[ASCENDER];
        xp = (a.region == null ? a.x : interpolateX(a, a.prev));
        if (xp < x)
          continue;
        if (d.y < ymin) {
          ymin = d.y;
          closest = d;
        }
        if (a.y < ymin) {
          ymin = a.y;
          closest = a;
        }
      }
      return closest;
    }

    /**
     * Get interpolated x for the scan line intersection with an edge
     * 
     * determine
     * 
     * @param v1
     * @param v2
     * @return x
     */
    private float interpolateX(CapVertex v1, CapVertex v2) {
      double dy12 = v2.y - v1.y;
      double dx12 = v2.x - v1.x;
      double dy1v = y - v1.y;
      return (float) (v1.x + (dy1v / dy12) * dx12);
    }

    /**
     * Cleave off as many triangles as possible.
     * 
     * @param isDescending
     */
    private void processMonotonic(boolean isDescending) {
      CapVertex vEdge = (isDescending ? prev : next);
      region = vEdge.region;
      CapVertex v = region[LAST];
      if (v == this) {
        // single triangle processed already by descender
        lstRegions.removeObj(region);
        return;
      }
      CapVertex v2;

      if (v == vEdge) {

        // same side

        v2 = (isDescending ? v.prev : v.next);
        if (v2.region != null)
          while (v2 != this
              && v2.qnext == null
              && (isDescending ? checkWinding(v2, v, this) : checkWinding(this,
                  v, v2))) {
            if (isDescending) {
              // same side descending
              //
              //                    /\
              //               last2  \
              //                  /    \
              //            --last-----------
              //                /        \
              //              -*----------\-<
              //              /            \

              addTriangle(v2, v, this, "same desc " + ipt);
              v = v2;
              v2 = v2.prev;
            } else {
              // same side ascending
              //
              //                    /\
              //                   /  last2
              //                  /    \
              //              ----------last--
              //                /        \
              //              ------------*-<
              //              /            \

              addTriangle(this, v, v2, "same asc " + ipt);
              v = v2;
              v2 = v2.next;
              if (v == region[DESCENDER])
                region[DESCENDER] = next;
            }
            if (v2 == this) {
              // finalized region
              lstRegions.removeObj(region);
            }
          }
      } else {
        // opposite side
        if (v.region == null) {
          // pull out this region
          lstRegions.removeObj(region);
          region = v.region = (isDescending ? v.prev : v.next).region;
        }

        CapVertex last0 = v;
        v = vEdge;

        do {
          if (isDescending) {

            // opposite side descending
            //
            //                    /\
            //                   /  last.next
            //              vEdge    \
            //               ---------last
            //                /        \
            //              -*----------\-<
            //              /            \

            v2 = v.prev;
            addTriangle(v2, v, this, "opp desc " + id);
            v = v2;
          } else {
            // opposite side ascending
            //
            //                    /\
            //           last.prev  \
            //                  /    vEdge
            //            --last------\----
            //                /        \
            //              -/----------*-<
            //              /            \

            v2 = v.next;
            addTriangle(this, v, v2, "opp asc " + id);
            v = v2;
          }
        } while (v != last0 && v != this && v.qnext == null);
      }

      region[LAST] = region[isDescending ? DESCENDER : ASCENDER] = this;

      //System.out.println(this);
      //System.out.println("#--------------");

      // region now may have unfinished edges. No matter.

    }

    /**
     * Check for CCW winding
     * 
     * @param v0
     * @param v1
     * @param v2
     * @return true if properly wound -- (v1-v0)x(v2-v0).dot.norm > 0
     */
    private boolean checkWinding(CapVertex v0, CapVertex v1, CapVertex v2) {
      vTemp1.sub2(v1, v0);
      vTemp2.sub2(v2, v0);
      vTemp1.z = vTemp2.z = 0;
      vTemp2.cross(vTemp1, vTemp2);
      return (vTemp2.z > 0);
    }

    /**
     * add the triangle and remove v1 from the chain
     * 
     * @param v0
     * @param v1
     * @param v2
     * @param note
     *        for debugging
     */
    private void addTriangle(CapVertex v0, CapVertex v1, CapVertex v2,
                             String note) {
      //System.out.println("#" + test + " " + note);
      ++nTriangles;
      if (dumping) {
        if (!checkWinding(v0, v1, v2))
          Logger.error("!!!BAD WINDING " + note);
        draw(nTriangles, v0, v1, v2, "red");
      }
      slicer.m.addPolygonV3(v0.ipt, v1.ipt, v2.ipt, 0, 0, 0,
          slicer.m.bsSlabDisplay);
      v1.link(null);
    }

    /**
     * link this vertex with v or remove it from the chain
     * 
     * @param v
     *        null to remove
     */
    protected void link(CapVertex v) {
      if (v == null) {
        prev.next = next;
        next.prev = prev;
        clear();
      } else {
        next = v;
        v.prev = this;
      }
    }

    /**
     * free links
     */
    private void clear() {
      qnext = next = prev = null;
      region = null;
    }

    /**
     * for debugging
     * 
     * @param index
     * @param v0
     * @param v1
     * @param v2
     * @param color
     */
    private void draw(int index, CapVertex v0, CapVertex v1, CapVertex v2,
                      String color) {
      T3 p0 = slicer.m.vs[v0.ipt];
      T3 p1 = slicer.m.vs[v1.ipt];
      T3 p2 = slicer.m.vs[v2.ipt];
      Logger.info("draw " + color + index + "/* " + v0.id + " " + v1.id
          + " " + v2.id + " */" + p0 + p1 + p2 + " color " + color);
    }

    /**
     * for debugging
     * 
     * @return listing of vertices currently in a region
     * 
     */
    private String dumpRegion() {
      String s = "\n#REGION d=" + region[DESCENDER].id + " a="
          + region[ASCENDER].id + " last=" + region[LAST].id + "\n# ";
      CapVertex v = region[ASCENDER];
      while (true) {
        s += v.id + " ";
        if (v == region[DESCENDER])
          break;
        v = v.next;
      }
      return s + "\n";
    }

    @Override
    public String toString() {
      return "draw p"
          + id
          + " {"
          + x
          + " "
          + y
          + " "
          + z
          + "} # "
          + (prev == null ? null : (prev.id + " " + next.id)
              + (region == null ? null : dumpRegion()));
    }

  }

}
