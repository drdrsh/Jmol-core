/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.renderspecial;


import org.jmol.api.JmolModulationSet;
import org.jmol.modelset.Atom;
import org.jmol.render.ShapeRenderer;
import org.jmol.script.T;
import org.jmol.shape.Shape;
import org.jmol.shapespecial.Vectors;
import org.jmol.util.GData;

import javajs.util.P3;
import javajs.util.P3i;
import javajs.util.V3;
import org.jmol.util.Vibration;

public class VectorsRenderer extends ShapeRenderer {

  private final static float arrowHeadOffset = -0.2f;
  private final P3 pointVectorStart = new P3();
  private final P3 pointVectorEnd = new P3();
  private final P3 pointArrowHead = new P3();
  private final P3i screenVectorStart = new P3i();
  private final P3i screenVectorEnd = new P3i();
  private final P3i screenArrowHead = new P3i();
  private final V3 headOffsetVector = new V3();
  
  private int diameter;
  //float headWidthAngstroms;
  private int headWidthPixels;
  private float vectorScale;
  private boolean vectorSymmetry;
  private float headScale;
  private boolean doShaft;
  private Vibration vibTemp;
  private boolean vectorsCentered;
  private boolean standardVector = true;
  private boolean vibrationOn;
  private boolean drawCap;


  @Override
  protected boolean render() {
    Vectors vectors = (Vectors) shape;
    if (!vectors.isActive)
      return false;
    short[] mads = vectors.mads;
    if (mads == null)
      return false;
    Atom[] atoms = vectors.atoms;
    short[] colixes = vectors.colixes;
    boolean needTranslucent = false;
    vectorScale = vwr.getFloat(T.vectorscale);
    if (vectorScale < 0)
      vectorScale = 1;
    vectorSymmetry = vwr.getBoolean(T.vectorsymmetry);
    vectorsCentered = vwr.getBoolean(T.vectorscentered);
    vibrationOn = vwr.tm.vibrationOn;
    headScale = arrowHeadOffset;
    if (vectorScale < 0)
      headScale = -headScale;
    for (int i = ms.getAtomCount(); --i >= 0;) {
      Atom atom = atoms[i];
      if (!isVisibleForMe(atom))
        continue;
      Vibration vibrationVector = ms.getVibration(i, false);
      if (vibrationVector == null)
        continue;
      if (!transform(mads[i], atom, vibrationVector))
        continue;
      if (!g3d.setC(Shape.getColix(colixes, i, atom))) {
        needTranslucent = true;
        continue;
      }
      renderVector(atom);
      if (vectorSymmetry) {
        if (vibTemp == null)
          vibTemp = new Vibration();
        vibTemp.setT(vibrationVector);
        vibTemp.scale(-1);
        transform(mads[i], atom, vibTemp);
        renderVector(atom);
      }
    }
    return needTranslucent;
  }

  private boolean transform(short mad, Atom atom, Vibration vib) {
    boolean isMod = (vib.modDim >= Vibration.TYPE_MODULATION);
    //boolean isDisp = (vib.modDim == Vibration.TYPE_DISPLACEMENT);
    boolean isSpin = (vib.modDim == Vibration.TYPE_SPIN);
    drawCap = true;
    if (!isMod) {
      float len = vib.length();
      // to have the vectors move when vibration is turned on
      if (Math.abs(len * vectorScale) < 0.01)
        return false;
      standardVector = true;
      doShaft = (0.1 + Math.abs(headScale / len) < Math.abs(vectorScale));
      headOffsetVector.setT(vib);
      headOffsetVector.scale(headScale / len);
    }
    if (isMod) {
      standardVector = false;
      doShaft = true;
      pointVectorStart.setT(atom);
      pointVectorEnd.setT(atom);
      JmolModulationSet mod = (JmolModulationSet) vib;
      if (!mod.isEnabled()) {
        mod.addTo(pointVectorEnd, 1);
      } else {
        if (vibrationOn)
          vwr.tm.getVibrationPoint(vib, pointVectorEnd, Float.NaN);
        mod.addTo(pointVectorStart, -1);
      }
      headOffsetVector.sub2(pointVectorEnd, pointVectorStart);
      float len = headOffsetVector.length();
      drawCap = (len + arrowHeadOffset > 0.001f);
      doShaft = (len > 0.01f);
      headOffsetVector.scale(headScale / headOffsetVector.length());
    } else if (vectorsCentered || isSpin) {
      standardVector = false;
      pointVectorEnd.scaleAdd2(0.5f * vectorScale, vib, atom);
      pointVectorStart.scaleAdd2(-0.5f * vectorScale, vib, atom);
    } else {
      pointVectorEnd.scaleAdd2(vectorScale, vib, atom);
      screenVectorEnd.setT(vibrationOn? tm.transformPtVib(pointVectorEnd, vib) : tm.transformPt(pointVectorEnd));
      pointArrowHead.add2(pointVectorEnd, headOffsetVector);
      if (atom.getAtomNumber() == 16)
        System.out.println("vecrend " + vib + atom.x + " " + atom.y + " " + atom.z + " ptH=" + pointVectorEnd);
      screenArrowHead.setT(vibrationOn ? tm.transformPtVib(pointArrowHead, vib) : tm.transformPt(pointArrowHead));
    }
    if (!standardVector) {
      screenVectorEnd.setT(tm.transformPt(pointVectorEnd));
      screenVectorStart.setT(tm.transformPt(pointVectorStart));
      if (drawCap)
        pointArrowHead.add2(pointVectorEnd, headOffsetVector);
      else
        pointArrowHead.setT(pointVectorEnd);
      screenArrowHead.setT(tm.transformPt(pointArrowHead));
    }
    diameter = (int) (mad < 0 ? -mad : mad < 1 ? 1 : vwr.tm.scaleToScreen(
        screenVectorEnd.z, mad));
    headWidthPixels = diameter << 1;
    if (headWidthPixels < diameter + 2)
      headWidthPixels = diameter + 2;
    return true;
  }
  
  private void renderVector(Atom atom) {
    if (doShaft) {
      if (standardVector)
        g3d.fillCylinderScreen(GData.ENDCAPS_OPEN, diameter, atom.sX,
            atom.sY, atom.sZ, screenArrowHead.x, screenArrowHead.y,
            screenArrowHead.z);
      else 
        g3d.fillCylinderScreen(GData.ENDCAPS_FLAT, diameter, screenVectorStart.x,
            screenVectorStart.y, screenVectorStart.z, screenArrowHead.x, screenArrowHead.y,
            screenArrowHead.z);
    }
    if (drawCap)
      g3d.fillConeScreen(GData.ENDCAPS_FLAT, headWidthPixels, screenArrowHead,
          screenVectorEnd, false);
  }
}
