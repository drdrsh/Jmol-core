/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2006  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.render;

import org.jmol.script.T;
import org.jmol.shape.Axes;
import org.jmol.util.GData;

import javajs.awt.Font;
import javajs.util.P3;

import org.jmol.viewer.StateManager;


public class AxesRenderer extends CageRenderer {

  private final static String[] axisLabels = { "+X", "+Y", "+Z", null, null, null, 
                                  "a", "b", "c", 
                                  "X", "Y", "Z", null, null, null,
                                  "X", null, "Z", null, "(Y)", null};

  private final P3 originScreen = new P3();
  
  private short[] colixes = new short[3];

  private final static String[] axesTypes = {"a", "b", "c"};

  @Override
  protected void initRenderer() {
    endcap = GData.ENDCAPS_FLAT; 
    draw000 = false;
  }

  @Override
  protected boolean render() {
    Axes axes = (Axes) shape;
    int mad = vwr.getObjectMad(StateManager.OBJ_AXIS1);
    // no translucent axes
    if (mad == 0 || !g3d.checkTranslucent(false))
      return false;
    boolean isXY = (axes.axisXY.z != 0);
    if (!isXY && tm.isNavigating() && vwr.getBoolean(T.navigationperiodic))
      return false;
    imageFontScaling = vwr.imageFontScaling;
    if (vwr.areAxesTainted()) {
      Font f = axes.font3d;
      axes.initShape();
      if (f != null)
        axes.font3d = f;
    }
    font3d = vwr.gdata.getFont3DScaled(axes.font3d, imageFontScaling);

    int modelIndex = vwr.am.cmi;
    // includes check here for background model present
    boolean isUnitCell = (vwr.g.axesMode == T.axesunitcell);
    if (vwr.ms.isJmolDataFrameForModel(modelIndex)
        && !vwr.ms.getJmolFrameType(modelIndex).equals("plot data"))
      return false;
    if (isUnitCell && modelIndex < 0 && vwr.getCurrentUnitCell() == null)
      return false;
    int nPoints = 6;
    int labelPtr = 0;
    if (isUnitCell && ms.unitCells != null) {
      nPoints = 3;
      labelPtr = 6;
    } else if (isXY) {
      nPoints = 3;
      labelPtr = 9;
    } else if (vwr.g.axesMode == T.axeswindow) {
      nPoints = 6;
      labelPtr = (vwr.getBoolean(T.axesorientationrasmol) ? 15 : 9);
    }
    if (axes.labels != null) {
      if (nPoints != 3)
        nPoints = (axes.labels.length < 6 ? 3 : 6);
      labelPtr = -1;
    }
    boolean isDataFrame = vwr.isJmolDataFrame();

    int slab = vwr.gdata.slab;
    int diameter = mad;
    boolean drawTicks = false;
    if (isXY) {
      if (exportType == GData.EXPORT_CARTESIAN)
        return false;
      if (mad >= 20) {
        // width given in angstroms as mAng.
        // max out at 500
        diameter = (mad > 500 ? 5 : mad / 100);
        if (diameter == 0)
          diameter = 2;
      } else {
        if (g3d.isAntialiased())
          diameter += diameter;
      }
      g3d.setSlab(0);
      float z = axes.axisXY.z;
      pt0i.setT(z == Float.MAX_VALUE || z == -Float.MAX_VALUE ? tm
          .transformPt2D(axes.axisXY) : tm.transformPt(axes.axisXY));
      originScreen.set(pt0i.x, pt0i.y, pt0i.z);
      float zoomDimension = vwr.getScreenDim();
      float scaleFactor = zoomDimension / 10f * axes.scale;
      if (g3d.isAntialiased())
        scaleFactor *= 2;
      for (int i = 0; i < 3; i++) {
        tm.rotatePoint(axes.getAxisPoint(i, false), p3Screens[i]);
        p3Screens[i].z *= -1;
        p3Screens[i].scaleAdd2(scaleFactor, p3Screens[i], originScreen);
      }
    } else {
      drawTicks = (axes.tickInfos != null);
      if (drawTicks) {
        checkTickTemps();
        tickA.setT(axes.getOriginPoint(isDataFrame));
      }
      tm.transformPtScrP3(axes.getOriginPoint(isDataFrame), originScreen);
      diameter = getDiameter((int) originScreen.z, mad);
      for (int i = nPoints; --i >= 0;)
        tm.transformPtScrP3(axes.getAxisPoint(i, isDataFrame), p3Screens[i]);
    }
    float xCenter = originScreen.x;
    float yCenter = originScreen.y;
    colixes[0] = vwr.getObjectColix(StateManager.OBJ_AXIS1);
    colixes[1] = vwr.getObjectColix(StateManager.OBJ_AXIS2);
    colixes[2] = vwr.getObjectColix(StateManager.OBJ_AXIS3);
    for (int i = nPoints; --i >= 0;) {
      if (isXY && axes.axisType != null
          && !axes.axisType.contains(axesTypes[i]))
        continue;
      colix = colixes[i % 3];
      g3d.setC(colix);
      String label = (axes.labels == null ? axisLabels[i + labelPtr]
          : i < axes.labels.length ? axes.labels[i] : null);
      if (label != null && label.length() > 0)
        renderLabel(label, p3Screens[i].x, p3Screens[i].y, p3Screens[i].z,
            xCenter, yCenter);
      if (drawTicks) {
        tickInfo = axes.tickInfos[(i % 3) + 1];
        if (tickInfo == null)
          tickInfo = axes.tickInfos[0];
        tickB.setT(axes.getAxisPoint(i, isDataFrame));
        if (tickInfo != null) {
          tickInfo.first = 0;
          tickInfo.signFactor = (i % 6 >= 3 ? -1 : 1);
        }
      }
      renderLine(originScreen, p3Screens[i], diameter, drawTicks
          && tickInfo != null);
    }
    if (nPoints == 3 && !isXY) { // a b c [orig]
      String label0 = (axes.labels == null || axes.labels.length == 3
          || axes.labels[3] == null ? "0" : axes.labels[3]);
      if (label0 != null && label0.length() != 0) {
        colix = vwr.cm.colixBackgroundContrast;
        g3d.setC(colix);
        renderLabel(label0, originScreen.x, originScreen.y, originScreen.z,
            xCenter, yCenter);
      }
    }
    if (isXY)
      g3d.setSlab(slab);
    return false;
  }
  
  private void renderLabel(String str, float x, float y, float z, float xCenter, float yCenter) {
    int strAscent = font3d.getAscent();
    int strWidth = font3d.stringWidth(str);
    float dx = x - xCenter;
    float dy = y - yCenter;
    if ((dx != 0 || dy != 0)) {
      float dist = (float) Math.sqrt(dx * dx + dy * dy);
      dx = (strWidth * 0.75f * dx / dist);
      dy = (strAscent * 0.75f * dy / dist);
      x += dx;
      y += dy;
    }
    double xStrBaseline = Math.floor(x - strWidth / 2f);
    double yStrBaseline = Math.floor(y + strAscent / 2f);
    g3d.drawString(str, font3d, (int) xStrBaseline, (int) yStrBaseline, (int) z, (int) z, (short) 0);
  }
}
