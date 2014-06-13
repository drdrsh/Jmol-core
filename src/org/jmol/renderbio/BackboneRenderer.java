/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-11-17 10:45:52 -0600 (Fri, 17 Nov 2006) $
 * $Revision: 6250 $

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

import javajs.util.Lst;

import org.jmol.modelset.Atom;
import org.jmol.modelsetbio.BasePair;
import org.jmol.modelsetbio.NucleicMonomer;
import org.jmol.script.T;
import org.jmol.shapebio.BioShape;
import org.jmol.util.C;
import org.jmol.util.GData;

public class BackboneRenderer extends BioShapeRenderer {

  private boolean isDataFrame;

  @Override
  protected void renderBioShape(BioShape bioShape) {
    boolean showSteps = vwr.getBoolean(T.backbonesteps)
        && bioShape.bioPolymer.isNucleic();
    isDataFrame = vwr.ms.isJmolDataFrameForModel(bioShape.modelIndex);
    for (int i = bsVisible.nextSetBit(0); i >= 0; i = bsVisible
        .nextSetBit(i + 1)) {
      Atom atomA = ms.at[leadAtomIndices[i]];
      short cA = colixes[i];
      mad = mads[i];
      drawSegment(atomA, ms.at[leadAtomIndices[i + 1]], cA, colixes[i + 1], 100);
      if (showSteps) {
        NucleicMonomer g = (NucleicMonomer) monomers[i];
        Lst<BasePair> bps = g.getBasePairs();
        if (bps != null) {
          for (int j = bps.size(); --j >= 0;) {
            int iAtom = bps.get(j).getPartnerAtom(g);
            if (iAtom > i)
              drawSegment(atomA, ms.at[iAtom], cA, cA, 1000);
          }
        }
      }
    }
  }

  private void drawSegment(Atom atomA, Atom atomB, short colixA, short colixB, float max) {    
    if (atomA.getNBackbonesDisplayed() == 0
        || atomB.getNBackbonesDisplayed() == 0
        || ms.isAtomHidden(atomB.i)
        || !isDataFrame && atomA.distanceSquared(atomB) > max)
      return;
    colixA = C.getColixInherited(colixA, atomA.getColix());
    colixB = C.getColixInherited(colixB, atomB.getColix());
    if (!isExport && !isPass2 && !setBioColix(colixA) && !setBioColix(colixB))
      return;
    int xA = atomA.sX, yA = atomA.sY, zA = atomA.sZ;
    int xB = atomB.sX, yB = atomB.sY, zB = atomB.sZ;
    int mad = this.mad;
    if (max == 1000)
      mad = mad >> 1;
    if (mad < 0) {
      g3d.drawLine(colixA, colixB, xA, yA, zA, xB, yB, zB);
    } else {
      int width = (int) (exportType == GData.EXPORT_CARTESIAN ? mad : vwr
          .tm.scaleToScreen((zA + zB) / 2, mad));
      g3d.fillCylinderXYZ(colixA, colixB, GData.ENDCAPS_SPHERICAL, width, xA,
          yA, zA, xB, yB, zB);
    }
  }  
}
