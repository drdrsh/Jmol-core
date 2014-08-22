package org.jmol.util;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.api.JmolModulationSet;
import org.jmol.api.SymmetryInterface;

import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.Matrix;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.T3;
import javajs.util.V3;

/**
 * A class to group a set of modulations for an atom as a "vibration"
 * Extends V3 so that it will be a displacement, and its value will be an occupancy
 * 
 * @author Bob Hanson hansonr@stolaf.edu 8/9/2013
 * 
 */

public class ModulationSet extends Vibration implements JmolModulationSet {

  public float vOcc = Float.NaN;
  public Map<String, Float> htUij;
  public float vOcc0;

  String id;
  
  private Lst<Modulation> mods;
  private int iop;
  private P3 r0;
  /**
   * vib is a spin vector when the model has modulation; 
   * otherwise an unmodulated vibration.
   * 
   */
  public Vibration vib;
  public V3 mxyz;
  
  private SymmetryInterface symmetry;  
  private M3 gammaE;
  private Matrix gammaIinv;
  private Matrix sigma;
  private Matrix sI;
  private Matrix tau;
  
  private boolean enabled;
  private float scale = 1;
 
  private P3 qtOffset = new P3();
  private boolean isQ;

  private Matrix t;
  
  private ModulationSet modTemp;
  private String strop;
  private boolean isSubsystem;
  
  private Matrix tFactor;

  @Override
  public float getScale() {
    return scale;
  }
  
  @Override
  public boolean isEnabled() {
    return enabled;
  }

  public ModulationSet() {
    
  }
  
  /**
   * A collection of modulations for a specific atom.
   * 
   * The set of cell wave vectors form the sigma (d x 3) array, one vector per row. 
   * Multiplying sigma by the atom vector r (3 x 1) gives us a (d x 1) column vector.
   * This column vector is the set of coefficients of [x4, x5, x6...] that I will
   * call X. 

   * Similarly, we express the x1' - xn' aspects of the operators as the matrices
   * Gamma_I (d x d epsilons) and s_I (d x 1 shifts). 
   * 
   * In the case of subsystems, these are extracted from:
   * 
   * {Rs_nu | Vs_nu} = W_nu {Rs|Vs} W_nu^-1
   * 
   * Then for X defined as [x4,x5,x6...] we have:
   * 
   * X' = Gamma_I * X + S_I
   * 
   * or
   * 
   * X = (Gamma_I^-1)(X' - S_I)
   * 
   * I call this array "tau". We can think of this as a
   * distance along the asn axis, as in a t-plot. 
   * Ultimately we will need to add in a term allowing 
   * us to adjust the t-value:
   * 
   * X = (Gamma_I^-1)(X' - S_I + t)
   * 
   * X = tau + (Gamma_I^-1)(t)
   * 
   * or, below:
   * 
   *   xt = gammaIinv.mul(t).add(tau)
   * 
   * For subsystem nu, we need to use t_nu, which will be
   * 
   * t_nu = (W_dd - sigma W_3d) t   (van Smaalen, p. 101)
   * 
   * t_nu = (tFactor) t
   * 
   * so this becomes
   * 
   * xt = gammaIinv.mul(tFactor.inverse().mul(t)).add(tau)
   * 
   * Thus we have two subsystem-dependent modulation factors we
   * need to bring in, sigma and tFactor, and two we need to compute,
   * GammaIinv and tau.
   * 
   * @param id
   * @param r0        unmodulated (average) position
   * @param modDim
   * @param mods
   * @param gammaE
   * @param factors   including sigma and tFactor
   * @param iop
   * @param symmetry
   * @param v TODO
   * @return this
   * 
   * 
   */

  public ModulationSet setMod(String id, P3 r0, int modDim,
                           Lst<Modulation> mods, M3 gammaE, Matrix[] factors,
                           int iop, SymmetryInterface symmetry, Vibration v) {
    this.r0 = r0;
    vib = v;
    if (v != null) {
      mxyz = new V3(); // modulations of spin
      vib.modScale = 1;
    }
    //Logger.info("ModulationSet atom " + id + " at " + r0);
    this.modDim = modDim;
    this.mods = mods;
    this.iop = iop;
    this.symmetry = symmetry;
    strop = symmetry.getSpaceGroupXyz(iop, false);
    this.id = id + "_" + symmetry.getSpaceGroupName();
    sigma = factors[0];
    tFactor = factors[1];
    isSubsystem = (tFactor != null);
    
    if (isSubsystem) {
      tFactor = tFactor.inverse();
      //gammaE = new M3();
      //symmetry.getSpaceGroupOperation(iop).getRotationScale(gammaE);
    // no, didn't work, but it really should work, I think....
      // why would we want to use the global gammaE?
    }
    
    this.gammaE = gammaE; // gammaE_nu
    
    Matrix rsvs = symmetry.getOperationRsVs(iop);
    gammaIinv = rsvs.getSubmatrix(3,  3,  modDim,  modDim).inverse();
    sI = rsvs.getSubmatrix(3, 3 + modDim, modDim, 1);
    
    tau = gammaIinv.mul(sigma.mul(Matrix.newT(r0, true)).sub(sI));
    if (Logger.debuggingHigh)
      Logger.debug("MODSET create r=" + Escape.eP(r0) + " si=" + Escape.e(sI.getArray())
              + " ginv=" + gammaIinv.toString().replace('\n', ' '));
    
    t = new Matrix(null, modDim, 1);
    return this;
  }

  @Override
  public SymmetryInterface getSubSystemUnitCell() {
    return (isSubsystem ? symmetry : null);
  }
  /**
   * In general, we have, for Fourier:
   * 
   * u_axis(x) = sum[A1 cos(theta) + B1 sin(theta)]
   * 
   * where axis is x, y, or z, and theta = 2n pi x
   * 
   * More generally, we have for a given rotation that is characterized by
   * 
   * X {x4 x5 x6 ...}
   * 
   * Gamma_E (R3 rotation)
   * 
   * Gamma_I (X rotation)
   * 
   * S_I (X translation)
   * 
   * We allow here only up to x6, simply because we are using standard R3
   * rotation objects Matrix3f, P3, V3.
   * 
   * We desire:
   * 
   * u'(X') = Gamma_E u(X)
   * 
   * which is defined as [private communication, Vaclav Petricek]:
   * 
   * u'(X') = Gamma_E sum[ U_c cos(2 pi (n m).Gamma_I^-1{X - S_I}) + U_s sin(2
   * pi (n m).Gamma_I^-1{X - S_I}) ]
   * 
   * where
   * 
   * U_c and U_s are coefficients for cos and sin, respectively (will be a1 and
   * a2 here)
   * 
   * (n m) is an array of Fourier number coefficients, such as (1 0), (1 -1), or
   * (0 2)
   * 
   * In Jmol we precalculate Gamma_I^-1(X - S_I) as tau, but we still have to
   * factor in Gamma_I^-1(t).
   * 
   * @param fracT
   * @param isQ
   * @return this
   */
  
  public synchronized ModulationSet calculate(T3 fracT, boolean isQ) {
    x = y = z = 0;
    if (mxyz != null)
      mxyz.set(0, 0, 0);
    htUij = null;
    vOcc = Float.NaN;
    double[][] a = t.getArray();
    for (int i = 0; i < modDim; i++)
      a[i][0] = 0;
    if (isQ && qtOffset != null) {
      Matrix q = new Matrix(null, 3, 1);
      a = q.getArray();
      a[0][0] = qtOffset.x;
      a[1][0] = qtOffset.y;
      a[2][0] = qtOffset.z;
      a = (t = sigma.mul(q)).getArray();
    }
    if (fracT != null) {
      switch (modDim) {
      default:
        a[2][0] += fracT.z;
        //$FALL-THROUGH$
      case 2:
        a[1][0] += fracT.y;
        //$FALL-THROUGH$
      case 1:
        a[0][0] += fracT.x;
        break;
      }
      if (isSubsystem)
        t = tFactor.mul(t);
    }
    t = gammaIinv.mul(t).add(tau);
    double[][] ta = t.getArray();
    //System.out.println("this1 =" + this + " " + ta[0][0] + " " + ta[1][0]);
    for (int i = mods.size(); --i >= 0;)
      mods.get(i).apply(this, ta);
    gammaE.rotate(this);
    //System.out.println("this2 =" + this);
    if (mxyz != null)
      gammaE.rotate(mxyz);
    return this;
  }
  
  public void addUTens(String utens, float v) {
    if (htUij == null)
      htUij = new Hashtable<String, Float>();
    Float f = htUij.get(utens);
    if (Logger.debuggingHigh)
      Logger.debug("MODSET " + id + " utens=" + utens + " f=" + f + " v="+ v);
    if(f != null)
      v += f.floatValue();
    htUij.put(utens, Float.valueOf(v));
  }

  
  /**
   * Set modulation "t" value, which sets which unit cell in sequence we are
   * looking at; d=1 only.
   * 
   * @param isOn
   * @param qtOffset
   * @param isQ
   * @param scale
   * 
   */
  @Override
  public synchronized void setModTQ(T3 a, boolean isOn, T3 qtOffset, boolean isQ,
                       float scale) {
    if (enabled)
      addTo(a, Float.NaN);
    enabled = false;
    this.scale = scale;
    if (qtOffset != null) {
      this.qtOffset.setT(qtOffset);
      this.isQ = isQ;
      if (isQ)
        qtOffset = null;
      calculate(qtOffset, isQ);
    }
    if (isOn) {
      addTo(a, 1);
      enabled = true;
    }
  }

  @Override
  public void addTo(T3 a, float scale) {
    boolean isReset = (Float.isNaN(scale));
    if (isReset)
      scale = -1;
    ptTemp.setT(this);
    ptTemp.scale(this.scale * scale);
    if (a != null) {
      //if (!isReset)
        //System.out.println(a + " ms " + ptTemp);
      symmetry.toCartesian(ptTemp, true);
      a.add(ptTemp);
    }
    // magnetic moment part
    if (mxyz != null)
      setVib(isReset);
  }
    
  private void setVib(boolean isReset) {
    vib.setT(v0);
    if (isReset)
      return;
    ptTemp.setT(mxyz);
    ptTemp.scale(this.scale * scale);
    symmetry.toCartesian(ptTemp, true);
    PT.fixPtFloats(ptTemp, PT.CARTESIAN_PRECISION);
    ptTemp.scale(vib.modScale);
    vib.add(ptTemp);
  }

  @Override
  public String getState() {
    String s = "";
    if (qtOffset != null && qtOffset.length() > 0)
      s += "; modulation " + Escape.eP(qtOffset) + " " + isQ + ";\n";
    s += "modulation {selected} " + (enabled ? "ON" : "OFF");
    return s;
  }

  @Override
  public T3 getModPoint(boolean asEnabled) {
    return (asEnabled ? this : r0);
  }
  @Override
  public T3 getModulation(String type, T3 t456) {
    getModTemp();
    if (type.equals("D")) {
      // return r0 if t456 is null, otherwise calculate dx,dy,dz for a given t4,5,6
      return P3.newP(t456 == null ? r0 : modTemp.calculate(t456, false));
    }
    if (type.equals("M")) {
      // return r0 if t456 is null, otherwise calculate dx,dy,dz for a given t4,5,6
      return P3.newP(t456 == null ? v0 : modTemp.calculate(t456, false).mxyz);
    }
    if (type.equals("T")) {
      modTemp.calculate(t456, false);
      double[][] ta = modTemp.t.getArray();
      return P3.new3((float) ta[0][0], (modDim > 1 ? (float) ta[1][0] : 0), (modDim > 1 ? (float) ta[2][0] : 0));
    }
    return null;
  }

  P3 ptTemp = new P3();
  private V3 v0;
  
  @Override
  public void setTempPoint(T3 a, T3 t456, float vibScale, float scale) {
    if (!enabled)
      return;
    getModTemp();
    addTo(a, Float.NaN);
    modTemp.calculate(t456, false).addTo(a, scale);
  }
    
  private void getModTemp() {
    if (modTemp == null) {
      modTemp = new ModulationSet();
      modTemp.id = id;
      modTemp.tau = tau;
      modTemp.mods = mods;
      modTemp.gammaE = gammaE;
      modTemp.modDim = modDim;
      modTemp.gammaIinv = gammaIinv;
      modTemp.sigma = sigma;
      modTemp.r0 = r0;
      modTemp.v0 = v0;
      modTemp.vib = vib;
      modTemp.symmetry = symmetry;
      modTemp.t = t;
      if (mxyz != null) {
        modTemp.mxyz = new V3();
      }
    }
  }

  @Override
  public void getInfo(Map<String, Object> info) {
    Hashtable<String, Object> modInfo = new Hashtable<String, Object>();
    modInfo.put("id", id);
    modInfo.put("r0", r0);
    modInfo.put("tau", tau.getArray());
    modInfo.put("modDim", Integer.valueOf(modDim));
    modInfo.put("gammaE", gammaE);
    modInfo.put("gammaIinv", gammaIinv.getArray());
    modInfo.put("sI", sI.getArray());
    modInfo.put("sigma", sigma.getArray());
    modInfo.put("symop", Integer.valueOf(iop + 1));
    modInfo.put("strop", strop);
    modInfo.put("unitcell", symmetry.getUnitCellInfo());

    Lst<Hashtable<String, Object>> mInfo = new Lst<Hashtable<String, Object>>();
    for (int i = 0; i < mods.size(); i++)
      mInfo.addLast(mods.get(i).getInfo());
    modInfo.put("mods", mInfo);
    info.put("modulation", modInfo);
  }

  @Override
  public void setXYZ(T3 v) {
    // we do not allow setting of the modulation vector,
    // but if there is an associated magnetic spin "vibration"
    // or an associated simple vibration,
    // then we allow setting of that.
    // but this is temporary, since really we set these from v0.
    if (vib == null) 
      return;
    if (vib.modDim == Vibration.TYPE_SPIN) {
      if (v.x == PT.FLOAT_MIN_SAFE && v.y == PT.FLOAT_MIN_SAFE) {
        // written by StateCreator -- for modulated magnetic moments
        // 957 Fe Fe_1_#957 1.4E-45 1.4E-45 0.3734652 ;
        vib.modScale = v.z;
        return;
      }
    }
    vib.setT(v);
  }

  @Override
  public Vibration getVibration(boolean forceNew) {
    // ModulationSets can be place holders for standard vibrations
    if (vib == null && forceNew)
      vib = new Vibration();
    return vib;
  }

  @Override
  public V3 getV3() {
    return (mxyz == null ? this : mxyz);
  }

  @Override
  public void scaleVibration(float m) {
    if (vib != null)
      vib.scale(m);
    vib.modScale *= m;
  }

  @Override
  public void setMoment() {
    if (mxyz == null)
      return;
    symmetry.toCartesian(vib, true);
    v0 = V3.newV(vib);
  }

  @Override
  public boolean isNonzero() {
    return x != 0 || y != 0 || z != 0 || 
        mxyz != null && (mxyz.x != 0 || mxyz.y != 0 || mxyz.z != 0);
  }

  private float[] axesLengths;
  float[] getAxesLengths() {
    return (axesLengths == null ? (axesLengths = symmetry.getNotionalUnitCell()) : axesLengths);
  }

}
