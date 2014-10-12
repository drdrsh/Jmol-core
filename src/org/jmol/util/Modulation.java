package org.jmol.util;

import java.util.Hashtable;

/**
 * A class to allow for more complex vibrations and associated phenomena, such
 * as modulated crystals, including Fourier series, Crenel functions, and
 * sawtooth functions
 * 
 * @author Bob Hanson hansonr@stolaf.edu 8/8/2013
 * 
 */

public class Modulation {

  private static final double TWOPI = 2 * Math.PI;

  private double[] qCoefs;

  private double a1;
  private double a2;
  private double center;
  private double left, right;
  private int order;

  private char axis;
  private final char type;
  private double[] params;

  private String utens;

  private double delta2;

  public static final char TYPE_DISP_FOURIER = 'f';
  public static final char TYPE_SPIN_FOURIER = 'm';
  public static final char TYPE_SPIN_SAWTOOTH = 't';
  public static final char TYPE_DISP_SAWTOOTH = 's';
  public static final char TYPE_OCC_FOURIER = 'o';
  public static final char TYPE_OCC_CRENEL = 'c';
  public static final char TYPE_U_FOURIER = 'u';

  public static final char TYPE_DISP_LEGENDRE = 'l';
  public static final char TYPE_U_LEGENDRE = 'L'; // not implemented


  /**
   * Each atomic modulation involves a fractional coordinate wave vector q, a
   * Fourier power n, a modulation axis (x, y, or, z), and specified parameters
   * that depend upon the type of function. Types supported:
   * 
   * Fourier [csin, ccos] 
   * 
   * Legendre [center, width, coeff, order]
   * 
   * Crenel [center, width, amplitude] 
   * 
   * Sawtooth [center, width, amplitude]
   * 
   * 
   * @param axis
   * @param type
   * @param params
   * @param utens
   *        TODO
   * @param qCoefs
   */
  public Modulation(char axis, char type, double[] params, String utens,
      double[] qCoefs) {
    //if (Logger.debuggingHigh)
      Logger
          .info("MOD create " + Escape.e(qCoefs) + " axis=" + axis + " type="
              + type + " params=" + Escape.e(params) + " utens=" + utens);
    this.axis = axis;
    this.type = type;
    this.utens = utens;
    this.params = params;
    this.qCoefs = qCoefs;
    switch (type) {
    case TYPE_SPIN_FOURIER:
    case TYPE_DISP_FOURIER:
    case TYPE_OCC_FOURIER:
    case TYPE_U_FOURIER:
      a1 = params[0]; // sin
      a2 = params[1]; // cos
      break;
    case TYPE_DISP_LEGENDRE:
    case TYPE_U_LEGENDRE:
      a1= params[2]; // coeff
      order = (int) params[3];
      calcLegendre(order);
      //$FALL-THROUGH$
    case TYPE_SPIN_SAWTOOTH:
    case TYPE_DISP_SAWTOOTH:
    case TYPE_OCC_CRENEL:
      center = params[0];
      delta2 = params[1] / 2;
      if (delta2 > 0.5)
        delta2 = 0.5; // http://b-incstrdb.ehu.es/incstrdb/CIFFile.php?RefCode=Bi-Sr-Ca-Cu-O_rNdCbetq
      left = center - delta2;
      right = center + delta2;
      if (left < 0)
        left += 1;
      if (right > 1)
        right -= 1;
      if (left >= right && left - right < 0.01f)
        left = right + 0.01f;
      if (a1 == 0) {
        // not Legendre
        // sawtooth only, actually
        a1 = params[2] / delta2;
      }
      break;
    }
  }

  
  /**
   * see note in ModulationSet
   * 
   * @param ms
   * @param t
   *        -- Vector of coordinates for [x4, x5, x6, ...]
   * 
   * 
   */

  void apply(ModulationSet ms, double[][] t) {
    double v = 0, nt = 0;
    boolean isSpin = false;
    for (int i = qCoefs.length; --i >= 0;)
      nt += qCoefs[i] * t[i][0];
    switch (type) {
    case TYPE_SPIN_FOURIER:
      isSpin = true;
      //$FALL-THROUGH$
    case TYPE_DISP_FOURIER:
    case TYPE_OCC_FOURIER:
    case TYPE_U_FOURIER:
      double theta = TWOPI * nt;
      if (a1 != 0)
        v += a1 * Math.sin(theta);
      if (a2 != 0)
        v += a2 * Math.cos(theta);
      if (Logger.debuggingHigh)
        Logger.info("MOD " + ms.id + " " + Escape.e(qCoefs) + " axis=" + axis
            + " v=" + v + " csin,ccos=" + a1 + "," + a2 + " / theta=" + theta);
      break;
    case TYPE_DISP_LEGENDRE:
    case TYPE_U_LEGENDRE:
      ms.vOcc0 = Float.NaN; // absolute
      nt -= Math.floor(nt);
      if (!range(nt))
        return;
      ms.vOcc = 1;
      // normalize to [-1,1]
      double x = (nt - center) / delta2;
      // shift into [-1,1]
      x = ((x + 1) % 2) + (x < -1 ? 1 : -1);
      // calc a1*P{i}(x)
      double xp = 1;
      double[] p = legendre[order];
      for (int i = 0, n = p.length; i < n; i++) {
        v += p[i] * xp;
        xp *= x;
      }
      v *= a1;
      break;
    case TYPE_OCC_CRENEL:

      //  An occupational crenel function along the internal space is
      //  defined as follows:
      //
      //           p(x4)=1   if x4 belongs to the interval [c-w/2,c+w/2]
      //           p(x4)=0   if x4 is outside the interval [c-w/2,c+w/2],

      ms.vOcc = (range(nt - Math.floor(nt)) ? 1 : 0);
      ms.vOcc0 = Float.NaN; // absolute
      //System.out.println("MOD " + ms.r + " " +  ms.delta + " " + ms.epsilon + " " + ms.id + " " + ms.v + " l=" + left + " x=" + x4 + " r=" + right);
      return;
    case TYPE_SPIN_SAWTOOTH:
      isSpin = true;
      //$FALL-THROUGH$
    case TYPE_DISP_SAWTOOTH:

      //  _atom_site_displace_special_func_sawtooth_ items are the
      //  adjustable parameters of a sawtooth function. A displacive sawtooth
      //  function along the internal space is defined as follows:
      //
      //    u_x = 2a_x[(x4 − c)/w] 
      //             
      //  for x4 belonging to the interval [c − (w/2), c + (w/2)], where ax,
      //  ay and az are the amplitudes (maximum displacements) along each
      //  crystallographic axis, w is its width, x4 is the internal coordinate
      //  and c is the centre of the function in internal space. ux, uy and
      //  uz must be expressed in relative units.

      // here we have set a1 = 2a_xyz/w 

      nt -= Math.floor(nt);
      if (!range(nt))
        return;

      // x < L < c
      //
      //           /|
      //          / |
      //         / x------------->
      //         |  |   L     /|
      // --------+--|---|----c-+------
      //         0  R   |   /  1       
      //                |  /         
      //                | /
      //                |/

      // becomes

      //                         /|
      //                        / |
      //                       / x|
      //                L     /   |
      // --------+------|----c-+--|----
      //         0      |   /  1  R     
      //                |  /         
      //                | /
      //                |/

      // x > R > c
      //
      //              /|
      //             / |
      //            /  |
      //           /   |         L
      // --------+c----|---------|---+-------
      //         0     R         |   1
      //        <-----------------x / 
      //                         | /
      //                         |/

      // becomes

      //              /|
      //             / |
      //            /  |
      //     L     /   |
      // ----|---+c----|-------------+--------
      //     |   0     R             1
      //     |x /                  
      //     | /
      //     |/

      if (left > right) {
        if (nt < left && left < center)
          nt += 1;
        else if (nt > right && right > center)
          nt -= 1;
      }
      v = a1 * (nt - center);
      break;
    }

    if (isSpin) {
      float[] f = ms.getAxesLengths();
      switch (axis) {
      case 'x':
        ms.mxyz.x += v / f[0];
        break;
      case 'y':
        ms.mxyz.y += v / f[1];
        break;
      case 'z':
        ms.mxyz.z += v / f[2];
        break;
      }
    } else {
      switch (axis) {
      case 'x':
        ms.x += v;
        break;
      case 'y':
        ms.y += v;
        break;
      case 'z':
        ms.z += v;
        break;
      case 'U':
        ms.addUTens(utens, (float) v);
        break;
      default:
        if (Float.isNaN(ms.vOcc))
          ms.vOcc = 0;
        ms.vOcc += (float) v;
      }
    }
  }

  /**
   * Check that left < x4 < right, but allow for folding
   * 
   * @param x4
   * @return true only if x4 is in the (possibly folded) range of left and right
   * 
   */
  private boolean range(double x4) {
    return (left < right ? left <= x4 && x4 <= right : left <= x4
        || x4 <= right);
  }

  public Hashtable<String, Object> getInfo() {
    Hashtable<String, Object> info = new Hashtable<String, Object>();
    info.put("type", "" + type + axis);
    info.put("params", params);
    info.put("qCoefs", qCoefs);
    if (utens != null)
      info.put("Utens", utens);
    return info;
  }

  static double[][] legendre;

  synchronized void calcLegendre(int m) {
    if (legendre != null && legendre.length > m)
      return;
    legendre = new double[m + 5][];
    double[] pn_1 = legendre[0] = new double[] { 1 };
    double[] pn = legendre[1] = new double[] { 0, 1 };
    //(n+1) P_{n+1}(x) = (2n+1) x P_n(x) - n P_{n-1}(x)
    for (int n = 1; n < m + 3; n++) {
      double[] p = legendre[n + 1] = new double[n + 2];
      for (int i = 0; i <= n; i++) {
        p[i + 1] = (2 * n + 1) * pn[i] / (n + 1);
        if (i < n)
          p[i] += -n * pn_1[i] / (n + 1);
      }
      pn_1 = pn;
      pn = p;
    }
  }
//  static {
//    for (double n = -5; n < 5; n+=0.2 ){
//      double fact = (n < -1 ? 1 : -1);
//      double nt = n;
//    System.out.println(nt + "\t" + (((nt + 1)%2) + (fact)) + "\t");
//    }
//    System.out.println("ok" + (-4.35%2));
//  }
}
