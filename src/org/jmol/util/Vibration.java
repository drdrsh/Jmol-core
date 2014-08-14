package org.jmol.util;

import java.util.Map;

import javajs.util.T3;
import javajs.util.V3;

/**
 * A class to allow for more complex vibrations and associated 
 * phenomena, such as modulated crystals. In the case of modulations,
 * ModulationSet extends Vibration and is implemented that way, 
 * and, as well, magnetic spin is also a form of Vibration that 
 * may have an associated ModulationSet, as indicated here
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */

public class Vibration extends V3 {

  protected final static double twoPI = 2 * Math.PI;

  public static final int TYPE_VIBRATION = -1;
  public static final int TYPE_SPIN = -2;
 // public static final int TYPE_DISPLACEMENT = -3; // not used

  /**
   * modDim will be > 0 for modulation
   */
  public int modDim = TYPE_VIBRATION; 

  /**
   * @param pt 
   * @param t456 
   * @param scale 
   * @param modulationScale 
   */
  public void setTempPoint(T3 pt, T3 t456, float scale, float modulationScale) {
    switch (modDim) {
//    case TYPE_DISPLACEMENT:
//      break;
    case TYPE_SPIN:
      break;
    default:
      pt.scaleAdd2((float) (Math.cos(t456.x * twoPI) * scale), this, pt);    
      break;
    }
  }

  public void getInfo(Map<String, Object> info) {
    info.put("vibVector", V3.newV(this));
    info.put("vibType", (
      //  modDim == TYPE_DISPLACEMENT ? "displacement" 
      modDim == TYPE_SPIN ? "spin" 
      : modDim == TYPE_VIBRATION ? "vib" 
      : "mod"));
  }

  @Override
  public Object clone() {
    Vibration v = new Vibration();
    v.setT(this);
    v.modDim = modDim;
    return v;
  }

  public void setXYZ(T3 vib) {
    setT(vib);
  }

  public Vibration setType(int type) {
    this.modDim = type;
    return this;
  }

  public boolean isNonzero() {
    return x != 0 || y != 0 || z != 0;
  }

}
