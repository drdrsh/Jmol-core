/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2006  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.viewer;

import org.jmol.script.T;

import javajs.util.AU;

import org.jmol.util.C;
import org.jmol.util.Elements;
import org.jmol.util.GData;
import org.jmol.util.Logger;
import org.jmol.c.PAL;
import org.jmol.c.StaticConstants;
import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.Model;
import org.jmol.modelset.ModelSet;
import org.jmol.util.ColorEncoder;


public class ColorManager {

  /*
   * ce is a "master" colorEncoded. It will be used
   * for all atom-based schemes (Jmol, Rasmol, shapely, etc.)
   * and it will be the 
   * 
   * 
   */

  public ColorEncoder ce = new ColorEncoder(null);
  private Viewer vwr;
  private GData g3d;

  // for atoms -- color CPK:

  private int[] argbsCpk;
  private int[] altArgbsCpk;

  // for properties.

  private float[] colorData;

  ColorManager(Viewer vwr, GData gdata) {
    this.vwr = vwr;
    g3d = gdata;
    argbsCpk = PAL.argbsCpk;
    altArgbsCpk = AU.arrayCopyRangeI(JC.altArgbsCpk, 0, -1);
  }

  void clear() {
    //causes problems? flushCaches();
  }

  private boolean isDefaultColorRasmol;

  boolean getDefaultColorRasmol() {
    return isDefaultColorRasmol;
  }

  void resetElementColors() {
    setDefaultColors(false);
  }

  void setDefaultColors(boolean isRasmol) {
    if (isRasmol) {
      isDefaultColorRasmol = true;
      argbsCpk = AU.arrayCopyI(ColorEncoder.getRasmolScale(), -1);
    } else {
      isDefaultColorRasmol = false;
      argbsCpk = PAL.argbsCpk;
    }
    altArgbsCpk = AU.arrayCopyRangeI(JC.altArgbsCpk, 0, -1);
    ce.createColorScheme((isRasmol ? "Rasmol="
        : "Jmol="), true, true);
    for (int i = PAL.argbsCpk.length; --i >= 0;)
      g3d.changeColixArgb(i, argbsCpk[i]);
    for (int i = JC.altArgbsCpk.length; --i >= 0;)
      g3d.changeColixArgb(Elements.elementNumberMax + i,
          altArgbsCpk[i]);
  }

  public short colixRubberband = C.HOTPINK;

  public void setRubberbandArgb(int argb) {
    colixRubberband = (argb == 0 ? 0 : C.getColix(argb));
  }

  /*
   * black or white, whichever contrasts more with the current background
   *
   *
   * @return black or white colix value
   */
  short colixBackgroundContrast;

  void setColixBackgroundContrast(int argb) {
    colixBackgroundContrast = C.getBgContrast(argb);
  }

  short getColixBondPalette(Bond bond, int pid) {
    int argb = 0;
    switch (pid) {
    case StaticConstants.PALETTE_ENERGY:
      return ce.getColorIndexFromPalette(bond.getEnergy(),
          -2.5f, -0.5f, ColorEncoder.BWR, false);
    }
    return (argb == 0 ? C.RED : C.getColix(argb));
  }

  short getColixAtomPalette(Atom atom, byte pid) {
    int argb = 0;
    int index;
    int id;
    ModelSet modelSet = vwr.ms;
    int modelIndex;
    float lo, hi;
    // we need to use the byte form here for speed
    switch (pid) {
    case StaticConstants.PALETTE_PROPERTY:
      return (colorData == null || atom.i >= colorData.length ? C.GRAY
          : getColixForPropertyValue(colorData[atom.i]));
    case StaticConstants.PALETTE_NONE:
    case StaticConstants.PALETTE_CPK:
      // Note that CPK colors can be changed based upon user preference
      // therefore, a changeable colix is allocated in this case
      id = atom.getAtomicAndIsotopeNumber();
      if (id < Elements.elementNumberMax)
        return g3d.getChangeableColix(id, argbsCpk[id]);
      int id0 = id;
      id = Elements.altElementIndexFromNumber(id);
      if (id > 0)
        return g3d.getChangeableColix(Elements.elementNumberMax + id,
            altArgbsCpk[id]);
      id = Elements.getElementNumber(id0);
      return g3d.getChangeableColix(id, argbsCpk[id]);
    case StaticConstants.PALETTE_PARTIAL_CHARGE:
      // This code assumes that the range of partial charges is [-1, 1].
      index = ColorEncoder.quantize4(atom.getPartialCharge(), -1, 1,
          JC.PARTIAL_CHARGE_RANGE_SIZE);
      return g3d.getChangeableColix(JC.PARTIAL_CHARGE_COLIX_RED + index,
          JC.argbsRwbScale[index]);
    case StaticConstants.PALETTE_FORMAL_CHARGE:
      index = atom.getFormalCharge() - Elements.FORMAL_CHARGE_MIN;
      return g3d.getChangeableColix(JC.FORMAL_CHARGE_COLIX_RED + index,
          JC.argbsFormalCharge[index]);
    case StaticConstants.PALETTE_TEMP:
    case StaticConstants.PALETTE_FIXEDTEMP:
      if (pid == StaticConstants.PALETTE_TEMP) {
        lo = vwr.ms.getBfactor100Lo();
        hi = vwr.ms.getBfactor100Hi();
      } else {
        lo = 0;
        hi = 100 * 100; // scaled by 100
      }
      return ce.getColorIndexFromPalette(
          atom.getBfactor100(), lo, hi, ColorEncoder.BWR, false);
    case StaticConstants.PALETTE_STRAIGHTNESS:
      return ce.getColorIndexFromPalette(
          atom.getGroupParameter(T.straightness), -1, 1, ColorEncoder.BWR,
          false);
    case StaticConstants.PALETTE_SURFACE:
      hi = vwr.ms.getSurfaceDistanceMax();
      return ce.getColorIndexFromPalette(
          atom.getSurfaceDistance100(), 0, hi, ColorEncoder.BWR, false);
    case StaticConstants.PALETTE_AMINO:
      return ce.getColorIndexFromPalette(atom.getGroupID(),
          0, 0, ColorEncoder.AMINO, false);
    case StaticConstants.PALETTE_SHAPELY:
      return ce.getColorIndexFromPalette(atom.getGroupID(),
          0, 0, ColorEncoder.SHAPELY, false);
    case StaticConstants.PALETTE_GROUP:
      // vwr.calcSelectedGroupsCount() must be called first ...
      // before we call getSelectedGroupCountWithinChain()
      // or getSelectedGropuIndexWithinChain
      // however, do not call it here because it will get recalculated
      // for each atom
      // therefore, we call it in Eval.colorObject();
      return ce.getColorIndexFromPalette(
          atom.getSelectedGroupIndexWithinChain(), 0,
          atom.getSelectedGroupCountWithinChain() - 1, ColorEncoder.BGYOR,
          false);
    case StaticConstants.PALETTE_POLYMER:
      Model m = vwr.ms.am[atom.mi];
      return ce.getColorIndexFromPalette(
          atom.getPolymerIndexInModel(), 0, m.getBioPolymerCount() - 1,
          ColorEncoder.BGYOR, false);
    case StaticConstants.PALETTE_MONOMER:
      // vwr.calcSelectedMonomersCount() must be called first ...
      return ce.getColorIndexFromPalette(
          atom.getSelectedMonomerIndexWithinPolymer(), 0,
          atom.getSelectedMonomerCountWithinPolymer() - 1, ColorEncoder.BGYOR,
          false);
    case StaticConstants.PALETTE_MOLECULE:
      return ce.getColorIndexFromPalette(
          modelSet.getMoleculeIndex(atom.i, true), 0,
          modelSet.getMoleculeCountInModel(atom.mi) - 1,
          ColorEncoder.ROYGB, false);
    case StaticConstants.PALETTE_ALTLOC:
      //very inefficient!
      modelIndex = atom.mi;
      return ce
          .getColorIndexFromPalette(
              modelSet.getAltLocIndexInModel(modelIndex,
                  atom.getAlternateLocationID()), 0,
              modelSet.getAltLocCountInModel(modelIndex), ColorEncoder.ROYGB,
              false);
    case StaticConstants.PALETTE_INSERTION:
      //very inefficient!
      modelIndex = atom.mi;
      return ce.getColorIndexFromPalette(
          modelSet.getInsertionCodeIndexInModel(modelIndex,
              atom.getInsertionCode()), 0,
          modelSet.getInsertionCountInModel(modelIndex), ColorEncoder.ROYGB,
          false);
    case StaticConstants.PALETTE_JMOL:
      id = atom.getAtomicAndIsotopeNumber();
      argb = getJmolOrRasmolArgb(id, T.jmol);
      break;
    case StaticConstants.PALETTE_RASMOL:
      id = atom.getAtomicAndIsotopeNumber();
      argb = getJmolOrRasmolArgb(id, T.rasmol);
      break;
    case StaticConstants.PALETTE_STRUCTURE:
      argb = atom.getProteinStructureSubType().getColor();
      break;
    case StaticConstants.PALETTE_CHAIN:
      int chain = atom.getChainID();
      chain = ((chain < 0 ? 0 : chain >= 256 ? chain - 256 : chain) & 0x1F)
          % JC.argbsChainAtom.length;
      argb = (atom.isHetero() ? JC.argbsChainHetero : JC.argbsChainAtom)[chain];
      break;
    }
    return (argb == 0 ? C.HOTPINK : C.getColix(argb));
  }

  private int getJmolOrRasmolArgb(int id, int argb) {
    switch (argb) {
    case T.jmol:
      if (id >= Elements.elementNumberMax)
        break;
      return ce.getArgbFromPalette(id, 0, 0,
          ColorEncoder.JMOL);
    case T.rasmol:
      if (id >= Elements.elementNumberMax)
        break;
      return ce.getArgbFromPalette(id, 0, 0,
          ColorEncoder.RASMOL);
    default:
      return argb;
    }
    return JC.altArgbsCpk[Elements.altElementIndexFromNumber(id)];
  }

  void setElementArgb(int id, int argb) {
    if (argb == T.jmol && argbsCpk == PAL.argbsCpk)
      return;
    argb = getJmolOrRasmolArgb(id, argb);
    if (argbsCpk == PAL.argbsCpk) {
      argbsCpk = AU.arrayCopyRangeI(PAL.argbsCpk, 0, -1);
      altArgbsCpk = AU.arrayCopyRangeI(JC.altArgbsCpk, 0, -1);
    }
    if (id < Elements.elementNumberMax) {
      argbsCpk[id] = argb;
      g3d.changeColixArgb(id, argb);
      return;
    }
    id = Elements.altElementIndexFromNumber(id);
    altArgbsCpk[id] = argb;
    g3d.changeColixArgb(Elements.elementNumberMax + id, argb);
  }

  ///////////////////  propertyColorScheme ///////////////

  float[] getPropertyColorRange() {
    if (ce.isReversed)
      return new float[] { ce.hi, ce.lo };
    return new float[] { ce.lo, ce.hi };
  }

  public void setPropertyColorRangeData(float[] data, BS bs) {
    colorData = data;
    ce.currentPalette = ce.createColorScheme(
        vwr.g.propertyColorScheme, true, false);
    ce.hi = -Float.MAX_VALUE;
    ce.lo = Float.MAX_VALUE;
    if (data == null)
      return;
    boolean isAll = (bs == null);
    float d;
    int i0 = (isAll ? data.length - 1 : bs.nextSetBit(0));
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bs.nextSetBit(i + 1))) {
      if (Float.isNaN(d = data[i]))
        continue;
      ce.hi = Math.max(ce.hi, d);
      ce.lo = Math.min(ce.lo, d);
    }
    setPropertyColorRange(ce.lo, ce.hi);
  }

  public void setPropertyColorRange(float min, float max) {
    ce.setRange(min, max, min > max);
    if (Logger.debugging)
      Logger.debug("ColorManager: color \""
        + ce.getCurrentColorSchemeName() + "\" range " + min + " "
        + max);
  }

  void setPropertyColorScheme(String colorScheme, boolean isTranslucent,
                              boolean isOverloaded) {
    boolean isReset = (colorScheme.length() == 0);
    if (isReset)
      colorScheme = "="; // reset roygb
    float[] range = getPropertyColorRange();
    ce.currentPalette = ce.createColorScheme(
        colorScheme, true, isOverloaded);
    if (!isReset)
      setPropertyColorRange(range[0], range[1]);
    ce.isTranslucent = isTranslucent;
  }

  void setUserScale(int[] scale) {
    ce.setUserScale(scale);
  }

  String getColorSchemeList(String colorScheme) {
    // isosurface sets ifDefault FALSE so that any default schemes are returned
    int iPt = (colorScheme == null || colorScheme.length() == 0) ? ce.currentPalette
        : ce
            .createColorScheme(colorScheme, true, false);
    return ColorEncoder.getColorSchemeList(ce
        .getColorSchemeArray(iPt));
  }

  short getColixForPropertyValue(float val) {
    return ce.getColorIndex(val);
  }

  public ColorEncoder getColorEncoder(String colorScheme) {
    if (colorScheme == null || colorScheme.length() == 0)
      return ce;
    ColorEncoder c = new ColorEncoder(ce);
    c.currentPalette = c.createColorScheme(colorScheme, false, true);
    return (c.currentPalette == Integer.MAX_VALUE ? null : c);
  }
}
