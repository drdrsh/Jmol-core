package org.jmol.util;

import java.util.Map;

import org.jmol.api.SymmetryInterface;

import javajs.util.T3;
import javajs.util.V3;

/**
 * A class to allow for more complex vibrations and associated 
 * phenomena, such as modulated crystals.
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */

public class Vibration extends V3 {

  protected final static double twoPI = 2 * Math.PI;

  public static final int TYPE_MODULATION = 0; // or higher; really minimum is 1, not 0
  public static final int TYPE_VIBRATION = -1;
  public static final int TYPE_SPIN = -2;
  public static final int TYPE_DISPLACEMENT = -3;

  public int modDim = -1; // -1 is vib, -2 is spin, -3 is displacement
  
  /**
   * @param pt 
   * @param t456 
   * @param scale 
   * @param modulationScale 
   */
  public void setTempPoint(T3 pt, T3 t456, float scale, float modulationScale) {
    if (modDim >= TYPE_VIBRATION)
      pt.scaleAdd2((float) (Math.cos(t456.x * twoPI) * scale), this, pt); 
  }

  public void getInfo(Map<String, Object> info) {
    info.put("vibVector", V3.newV(this));
    info.put("vibType", (modDim == TYPE_DISPLACEMENT ? "displacement" 
        : modDim == TYPE_SPIN ? "spin" : modDim == TYPE_VIBRATION ? "vib" : "mod"));
  }

  public SymmetryInterface getUnitCell() {
    // ModulationSet only
    return null;
  }

  @Override
  public Object clone() {
    Vibration v = new Vibration();
    v.setT(this);
    v.modDim = modDim;
    return v;
  }

}
