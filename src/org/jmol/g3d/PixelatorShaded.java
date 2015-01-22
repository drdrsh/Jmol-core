/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2011  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.g3d;

/**
 * A class to do Z Shading of pixels.
 * 
 */
class PixelatorShaded extends Pixelator {


  private int[] bgRGB, tmp;
  private int zSlab, zDepth, zShadePower;

  /**
   * @param g
   * @param p0
   */
  PixelatorShaded(Graphics3D g, Pixelator p0) {
    super(g);
    tmp = new int[3];
    this.p0 = p0;
  }
  
  Pixelator set(int zSlab, int zDepth, int zShadePower) {
    bgRGB = new int[] { g.bgcolor & 0xFF, (g.bgcolor >> 8) & 0xFF,
        (g.bgcolor >> 16) & 0xFF };
    this.zSlab = zSlab < 0 ? 0 : zSlab;
    this.zDepth = zDepth < 0 ? 0 : zDepth;
    this.zShadePower = zShadePower;
    return this;
  }

  @Override
  void addPixel(int offset, int z, int p) {
    // z starts with 0 at camera and runs toward model, with zSlab <= zDepth
    if (z > zDepth)
      return;
    if (z >= zSlab) {
      int[] t = tmp;
      int[] zs = bgRGB;
      t[0] = p;
      t[1] = p >> 8;
      t[2] = p >> 16;
      float f = (float)(zDepth - z) / (zDepth - zSlab);
      if (zShadePower > 1)
        for (int i = 0; i < zShadePower; i++)
          f *= f;
      for (int i = 0; i < 3; i++)
        t[i] = zs[i] + (int) (f * ((t[i] & 0xFF) - zs[i]));
      p = (t[2] << 16) | (t[1] << 8) | t[0] | (p & 0xFF000000);
    }
    p0.addPixel(offset, z, p);
  }
}