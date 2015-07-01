package org.jmol.renderbio;

import javajs.util.P3;
import javajs.util.V3;

import org.jmol.api.JmolRendererInterface;
import org.jmol.c.STR;
import org.jmol.java.BS;
import org.jmol.modelsetbio.AlphaMonomer;
import org.jmol.modelsetbio.Helix;
import org.jmol.modelsetbio.ProteinStructure;
import org.jmol.modelsetbio.Sheet;
import org.jmol.util.GData;
import org.jmol.viewer.TransformManager;
import org.jmol.viewer.Viewer;

public class RocketRenderer {
  private boolean tPending;
  private ProteinStructure proteinstructurePending;
  private int startIndexPending;
  private int endIndexPending;

  private V3 vtemp;
  private P3 screenA, screenB, screenC;
  private short colix;
  private short mad;
  private RocketsRenderer rr;
  private Viewer vwr;
  private JmolRendererInterface g3d;
  private TransformManager tm;
  private boolean renderArrowHeads;
  private boolean isRockets;

  public RocketRenderer(){
  }
  
  RocketRenderer set(RocketsRenderer rr) {
    screenA = new P3();
    screenB = new P3();
    screenC = new P3();
    vtemp = new V3();
    this.rr = rr;
    vwr = rr.vwr;
    tm = rr.vwr.tm;
    isRockets = rr.isRockets;
    return this;
  }

  void renderRockets() {
    // doing the cylinders separately because we want to connect them if we can.

    // Key structures that must render properly
    // include 1crn and 7hvp

    g3d = rr.g3d;
    tPending = false;
    renderArrowHeads = rr.renderArrowHeads;
    BS bsVisible = rr.bsVisible;
    for (int i = bsVisible.nextSetBit(0); i >= 0; i = bsVisible
        .nextSetBit(i + 1)) {
      if (rr.structureTypes[i] == STR.HELIX || isRockets && rr.structureTypes[i] == STR.SHEET) {
        renderSpecialSegment((AlphaMonomer) rr.monomers[i], rr.getLeadColix(i), rr.mads[i]);
      } else if (isRockets) {
        renderPending();
        rr.renderHermiteConic(i, true, 7);
      }
    }
    renderPending();
  }

  private void renderSpecialSegment(AlphaMonomer monomer, short thisColix,
                                      short thisMad) {
    ProteinStructure proteinstructure = monomer.proteinStructure;
    if (tPending) {
      if (proteinstructure == proteinstructurePending && thisMad == mad
          && thisColix == colix
          && proteinstructure.getIndex(monomer) == endIndexPending + 1) {
        ++endIndexPending;
        return;
      }
      renderPending();
    }
    proteinstructurePending = proteinstructure;
    startIndexPending = endIndexPending = proteinstructure.getIndex(monomer);
    colix = thisColix;
    mad = thisMad;
    tPending = true;
  }

  private void renderPending() {
    if (!tPending)
      return;
    P3[] segments = proteinstructurePending.getSegments();
    boolean renderArrowHead = (renderArrowHeads && endIndexPending == proteinstructurePending.nRes - 1);
    if (proteinstructurePending instanceof Helix)
      renderPendingRocketSegment(endIndexPending, segments[startIndexPending],
          segments[endIndexPending], segments[endIndexPending + 1],
          renderArrowHead);
    else if (proteinstructurePending instanceof Sheet)
      renderPendingSheetPlank(segments[startIndexPending],
          segments[endIndexPending], segments[endIndexPending + 1],
          renderArrowHead);
    tPending = false;
  }

  /**
   * @param i
   * @param pointStart
   * @param pointBeforeEnd
   *        ignored now that arrow heads protrude beyond end of rocket
   * @param pointEnd
   * @param renderArrowHead
   */
  private void renderPendingRocketSegment(int i, P3 pointStart,
                                          P3 pointBeforeEnd, P3 pointEnd,
                                          boolean renderArrowHead) {
    if (g3d.setC(colix)) {
      tm.transformPt3f(pointStart, screenA);
      tm.transformPt3f((renderArrowHead ? pointBeforeEnd : pointEnd), screenB);
      int zMid = (int) Math.floor((screenA.z + screenB.z) / 2f);
      int diameter = ((int) vwr.tm.scaleToScreen(zMid, mad));
      if (!renderArrowHead || pointStart != pointBeforeEnd)
        g3d.fillCylinderBits(GData.ENDCAPS_FLAT, diameter, screenA, screenB);
      if (renderArrowHead) {
        screenA.sub2(pointEnd, pointBeforeEnd);
        tm.transformPt3f(pointEnd, screenC);
        int coneDiameter = (mad << 1) - (mad >> 1);
        coneDiameter = (int) vwr.tm.scaleToScreen(
            (int) Math.floor(screenB.z), coneDiameter);
        g3d.fillConeScreen3f(GData.ENDCAPS_FLAT, coneDiameter, screenB,
            screenC, false);
      } else {
        
      }
      if (startIndexPending == endIndexPending)
        return;
      P3 t = screenB;
      screenB = screenC;
      screenC = t;
    }
  }

  private final static byte[] boxFaces =
  {
    0, 1, 3, 2,
    0, 2, 6, 4,
    0, 4, 5, 1,
    7, 5, 4, 6,
    7, 6, 2, 3,
    7, 3, 1, 5 };

  private final static byte arrowHeadFaces[] =
  {0, 1, 3, 2,
   0, 4, 5, 2,
   1, 4, 5, 3};

  private P3 ptC, ptTip;
  private P3[] corners, screenCorners;
  private V3 vW, vH;

  private void renderPendingSheetPlank(P3 ptStart, P3 pointBeforeEnd,
                                  P3 ptEnd, boolean renderArrowHead) {
    if (!g3d.setC(colix))
      return;
    if (corners == null) {
      ptC = new P3();
      ptTip = new P3();
      vW = new V3();
      vH = new V3();
      screenCorners = new P3[8];
      corners = new P3[8];
    }
    if (corners[0] == null)
      for (int i = 8; --i >= 0;) {
        corners[i] = new P3();
        screenCorners[i] = new P3();
      }
    if (renderArrowHead) {
      setBox(1.25f, 0.333f, pointBeforeEnd);
      ptTip.scaleAdd2(-0.5f, vH, ptEnd);
      for (int i = 4; --i >= 0;) {
        P3 corner = corners[i];
        corner.setT(ptC);
        if ((i & 1) != 0)
          corner.add(vW);
        if ((i & 2) != 0)
          corner.add(vH);
        tm.transformPt3f(corner, screenCorners[i]);
      }
      corners[4].setT(ptTip);
      tm.transformPt3f(ptTip, screenCorners[4]);
      corners[5].add2(ptTip, vH);
      tm.transformPt3f(corners[5], screenCorners[5]);

      g3d.fillTriangle3f(screenCorners[0], screenCorners[1], screenCorners[4],
          true);
      g3d.fillTriangle3f(screenCorners[2], screenCorners[3], screenCorners[5],
          true);
      for (int i = 0; i < 12; i += 4) {
        int i0 = arrowHeadFaces[i];
        int i1 = arrowHeadFaces[i + 1];
        int i2 = arrowHeadFaces[i + 2];
        int i3 = arrowHeadFaces[i + 3];
        g3d.fillQuadrilateral(screenCorners[i0], screenCorners[i1],
            screenCorners[i2], screenCorners[i3]);
      }
      ptEnd = pointBeforeEnd;
    }
    setBox(1f, 0.25f, ptStart);
    vtemp.sub2(ptEnd, ptStart);
    if (vtemp.lengthSquared() == 0)
      return;
    buildBox(ptC, vW, vH, vtemp);
    for (int i = 0; i < 6; ++i) {
      int i0 = boxFaces[i * 4];
      int i1 = boxFaces[i * 4 + 1];
      int i2 = boxFaces[i * 4 + 2];
      int i3 = boxFaces[i * 4 + 3];
      g3d.fillQuadrilateral(screenCorners[i0], screenCorners[i1],
          screenCorners[i2], screenCorners[i3]);
    }
  }

  private void setBox(float w, float h, P3 pt) {
    ((Sheet) proteinstructurePending).setBox(w, h, pt, vW, vH, ptC, mad / 1000f);
  }

  private void buildBox(P3 pointCorner, V3 scaledWidthVector,
                        V3 scaledHeightVector, V3 lengthVector) {
    for (int i = 8; --i >= 0;) {
      P3 corner = corners[i];
      corner.setT(pointCorner);
      if ((i & 1) != 0)
        corner.add(scaledWidthVector);
      if ((i & 2) != 0)
        corner.add(scaledHeightVector);
      if ((i & 4) != 0)
        corner.add(lengthVector);
      tm.transformPt3f(corner, screenCorners[i]);
    }
  }

}
