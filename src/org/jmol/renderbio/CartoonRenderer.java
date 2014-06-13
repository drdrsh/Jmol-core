/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-11 14:30:16 -0500 (Sun, 11 Mar 2007) $
 * $Revision: 7068 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.renderbio;

import org.jmol.modelsetbio.AlphaMonomer;
import org.jmol.modelsetbio.NucleicMonomer;
import org.jmol.modelsetbio.ProteinStructure;
import org.jmol.script.T;
import org.jmol.shapebio.BioShape;
import org.jmol.util.C;
import org.jmol.util.GData;
import javajs.util.P3;
import javajs.util.P3i;


public class CartoonRenderer extends RocketsRenderer {

  private boolean renderAsRockets;
  private boolean renderEdges;
  private boolean ladderOnly;
  private boolean renderRibose;
  
  @Override
  protected void renderBioShape(BioShape bioShape) {
    if (wireframeOnly) {
      renderStrands();
//      if (wingVectors == null || isCarbohydrate)
//        renderTrace();
//      else
//        renderMeshRibbon();        
      return;
    }
    newRockets = true;
    if (wingVectors == null || isCarbohydrate)
      return;
    getScreenControlPoints();
    if (isNucleic) {
      renderNucleic();
      return;
    }
    boolean val = vwr.getBoolean(T.cartoonrockets);
    if (renderAsRockets != val) {
      bioShape.falsifyMesh();
      renderAsRockets = val;
    }
    val = !vwr.getBoolean(T.rocketbarrels);
    if (renderArrowHeads != val) {
      bioShape.falsifyMesh();
      renderArrowHeads = val;
    }
    ribbonTopScreens = calcScreens(0.5f);
    ribbonBottomScreens = calcScreens(-0.5f);
    calcRopeMidPoints(newRockets);
    if (!renderArrowHeads) {
      calcScreenControlPoints(cordMidPoints);
      controlPoints = cordMidPoints;
    }
    renderRockets();
    vwr.freeTempPoints(cordMidPoints);
    vwr.freeTempScreens(ribbonTopScreens);
    vwr.freeTempScreens(ribbonBottomScreens);
  }

  P3i ptConnectScr = new P3i();
  P3 ptConnect = new P3();
  void renderNucleic() {
    renderEdges = vwr.getBoolean(T.cartoonbaseedges);
    ladderOnly = vwr.getBoolean(T.cartoonladders);
    renderRibose = vwr.getBoolean(T.cartoonribose);
    boolean isTraceAlpha = vwr.getBoolean(T.tracealpha);
    for (int i = bsVisible.nextSetBit(0); i >= 0; i = bsVisible
        .nextSetBit(i + 1)) {
      if (isTraceAlpha) {
        ptConnectScr.set(
            (controlPointScreens[i].x + controlPointScreens[i + 1].x) / 2,
            (controlPointScreens[i].y + controlPointScreens[i + 1].y) / 2,
            (controlPointScreens[i].z + controlPointScreens[i + 1].z) / 2);
        ptConnect.ave(controlPoints[i], controlPoints[i + 1]);
      } else {
        ptConnectScr.setT(controlPointScreens[i + 1]);
        ptConnect.setT(controlPoints[i + 1]);
      }
      renderHermiteConic(i, false, 4);
      colix = getLeadColix(i);
      if (setBioColix(colix))
        renderNucleicBaseStep((NucleicMonomer) monomers[i], mads[i], ptConnectScr, ptConnect);
    }
  }

  @Override
  protected void renderRockets() {
    boolean lastWasSheet = false;
    boolean lastWasHelix = false;
    ProteinStructure previousStructure = null;
    ProteinStructure thisStructure;

    // Key structures that must render properly
    // include 1crn and 7hvp

    // this loop goes monomerCount --> 0, because
    // we want to hit the heads first

    for (int i = monomerCount; --i >= 0;) {
      // runs backwards, so it can render the heads first
      thisStructure = (ProteinStructure) monomers[i].getStructure();
      if (thisStructure != previousStructure) {
        if (renderAsRockets)
          lastWasHelix = false;
        lastWasSheet = false;
      }
      previousStructure = thisStructure;
      boolean isHelix = isHelix(i);
      boolean isSheet = isSheet(i);
      boolean isHelixRocket = (renderAsRockets || !renderArrowHeads ? isHelix : false);
      if (bsVisible.get(i)) {
        if (isHelixRocket) {
          //next pass
        } else if (isSheet || isHelix) {
          if (lastWasSheet && isSheet || lastWasHelix && isHelix) {
            //uses topScreens
            renderHermiteRibbon(true, i, true);
          } else {
            renderHermiteArrowHead(i);
          }
        } else {
          renderHermiteConic(i, true, 7);
        }
      }
      lastWasSheet = isSheet;
      lastWasHelix = isHelix;
    }
    if (renderAsRockets || !renderArrowHeads)
      renderCartoonRockets();
  }

  private void renderCartoonRockets() {
    // doing the cylinders separately because we want to connect them if we can.

    // Key structures that must render properly
    // include 1crn and 7hvp

    // this loop goes 0 --> monomerCount, because
    // the special segments routine takes care of heads
    tPending = false;
    for (int i = bsVisible.nextSetBit(0); i >= 0; i = bsVisible
        .nextSetBit(i + 1))
      if (isHelix(i))
        renderSpecialSegment((AlphaMonomer) monomers[i], getLeadColix(i), mads[i]);
    renderPending();
  }
  
  //// nucleic acid base rendering
  
  private final P3[] rPt = new P3[10];
  private final P3i[] rScr = new P3i[10];
  private final P3[] rPt5 = new P3[5];
  private final P3i[] rScr5 = new P3i[5];
  private P3 basePt;
  private P3i baseScreen;

  private void renderNucleicBaseStep(NucleicMonomer nucleotide, short thisMad,
                                     P3i backboneScreen, P3 backbonePt) {
    if (rScr[0] == null)    {
      for (int i = 10; --i >= 0; )
        rScr[i] = new P3i();
      for (int i = 5; --i >= 0; )
          rScr5[i] = new P3i();
      baseScreen = new P3i();
      basePt = new P3();
      rPt[9] = new P3(); // ribose center
    }
    if (renderEdges) {
      renderLeontisWesthofEdges(nucleotide, thisMad);
      return;
    }
    nucleotide.getBaseRing6Points(rPt);
    tm.transformPoints(6, rPt, rScr);
    if (!ladderOnly)
      renderRing6();
    P3i stepScreen;
    P3 stepPt;
    int pt;

    //private final static byte[] ring6OffsetIndexes = {C5, C6, N1, C2, N3, C4};
    //private final static byte[] ring5OffsetIndexes = {C5, N7, C8, N9, C4};
    //private final static byte[] riboseOffsetIndexes = {C1P, C2P, C3P, C4P, O4P, O3P, C5P, O5P};

    boolean hasRing5 = nucleotide.maybeGetBaseRing5Points(rPt5);
    if (hasRing5) {
      if (ladderOnly) {
        stepScreen = rScr[2]; // N1
        stepPt = rPt[2];
      } else {
        tm.transformPoints(5, rPt5, rScr5);
        renderRing5();
        stepScreen = rScr5[3]; // N9
        stepPt = rPt5[3];
      }
    } else {
      pt = (ladderOnly ? 4 : 2);
      stepScreen = rScr[pt]; // N3 or N1
      stepPt = rPt[pt];
    }
    mad = (short) (thisMad > 1 ? thisMad / 2 : thisMad);
    float r = mad / 2000f;
    int w = (int) vwr.tm.scaleToScreen(backboneScreen.z, mad);
    if (ladderOnly || !renderRibose)
      g3d.fillCylinderScreen3I(GData.ENDCAPS_SPHERICAL, w, backboneScreen,
          stepScreen, backbonePt, stepPt, r);
    if (ladderOnly)
      return;
    drawEdges(rScr, rPt, 6);
    if (hasRing5)
      drawEdges(rScr5, rPt5, 5);
    else
      renderEdge(rScr, rPt, 0, 5);
    if (renderRibose) {
      baseScreen.setT(stepScreen);
      basePt.setT(stepPt);
      nucleotide.getRiboseRing5Points(rPt);
      P3 c = rPt[9];
      c.set(0,  0,  0);
      for (int i = 0; i < 5; i++) 
        c.add(rPt[i]);
      c.scale(0.2f);
      tm.transformPoints(10, rPt, rScr);
      renderRibose();
      renderEdge(rScr, rPt, 2, 5); // C3' - O3'
      renderEdge(rScr, rPt, 3, 6); // C4' - C5' 
      renderEdge(rScr, rPt, 6, 7); // C5' - O5'
      renderEdge(rScr, rPt, 7, 8); // O5' - P'
      renderCyl(rScr[0], baseScreen, rPt[0], basePt); // C1' - N1 or N9
      drawEdges(rScr, rPt, 5);
    }
  }

  private void drawEdges(P3i[] scr, P3[] pt, int n) {
    for (int i = n; --i >= 0; )
      scr[i].z--;
    for (int i = n; --i > 0; )
      renderEdge(scr, pt, i, i - 1);
  }

  private void renderLeontisWesthofEdges(NucleicMonomer nucleotide,
                                         short thisMad) {
    //                Nasalean L, Strombaugh J, Zirbel CL, and Leontis NB in 
    //                Non-Protein Coding RNAs, 
    //                Nils G. Walter, Sarah A. Woodson, Robert T. Batey, Eds.
    //                Chapter 1, p 6.
    // http://books.google.com/books?hl=en&lr=&id=se5JVEqO11AC&oi=fnd&pg=PR11&dq=Non-Protein+Coding+RNAs&ots=3uTkn7m3DA&sig=6LzQREmSdSoZ6yNrQ15zjYREFNE#v=onepage&q&f=false

    if (!nucleotide.getEdgePoints(rPt))
      return;
    tm.transformPoints(6, rPt, rScr);
    renderTriangle(rScr, rPt, 2, 3, 4, true);
    mad = (short) (thisMad > 1 ? thisMad / 2 : thisMad);
    renderEdge(rScr, rPt, 0, 1);
    renderEdge(rScr, rPt, 1, 2);
    boolean isTranslucent = C.isColixTranslucent(colix);
    float tl = C.getColixTranslucencyLevel(colix);
    short colixSugarEdge = C.getColixTranslucent3(C.RED, isTranslucent,
        tl);
    short colixWatsonCrickEdge = C.getColixTranslucent3(C.GREEN,
        isTranslucent, tl);
    short colixHoogsteenEdge = C.getColixTranslucent3(C.BLUE,
        isTranslucent, tl);
    g3d.setC(colixSugarEdge);
    renderEdge(rScr, rPt, 2, 3);
    g3d.setC(colixWatsonCrickEdge);
    renderEdge(rScr, rPt, 3, 4);
    g3d.setC(colixHoogsteenEdge);
    renderEdge(rScr, rPt, 4, 5);
  }

  private void renderEdge(P3i[] scr, P3[] pt, int i, int j) {
    renderCyl(scr[i], scr[j], pt[i], pt[j]);
  }

  private void renderCyl(P3i s1, P3i s2, P3 p1, P3 p2) {
    g3d.fillCylinderScreen3I(GData.ENDCAPS_SPHERICAL, 3, s1, s2, p1, p2, 0.005f);
  }

  /**
   * 
   * @param scr 
   * @param pt 
   * @param i 
   * @param j 
   * @param k 
   * @param doShade    if shade was not calculated previously;
   */
  private void renderTriangle(P3i[] scr, P3[] pt, int i, int j, int k, boolean doShade) {
    if (doShade)
      g3d.setNoisySurfaceShade(scr[i], scr[j], scr[k]);
    g3d.fillTriangle3i(scr[i], scr[j], scr[k], pt[i], pt[j], pt[k]);
  }

  private void renderRing6() {
    renderTriangle(rScr, rPt, 0, 2, 4, true);
    renderTriangle(rScr, rPt, 0, 1, 2, false);
    renderTriangle(rScr, rPt, 0, 4, 5, false);
    renderTriangle(rScr, rPt, 2, 3, 4, false);
  }

  private void renderRing5() {
    renderTriangle(rScr5, rPt5, 0, 1, 2, false);
    renderTriangle(rScr5, rPt5, 0, 2, 3, false);
    renderTriangle(rScr5, rPt5, 0, 3, 4, false);
  }  

  private void renderRibose() {
    renderTriangle(rScr, rPt, 0, 1, 9, true);
    renderTriangle(rScr, rPt, 1, 2, 9, true);
    renderTriangle(rScr, rPt, 2, 3, 9, true);
    renderTriangle(rScr, rPt, 3, 4, 9, true);
    renderTriangle(rScr, rPt, 4, 0, 9, true);
  }  

}
