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
 * 
 */
class Pixelator {
  protected final Graphics3D g;
  Pixelator p0;
  protected int[] zb, pb;
  int width;


  /**
   * @param graphics3d
   */
  Pixelator(Graphics3D graphics3d) {
    g = graphics3d;
    setBuf();
  }

  void setBuf() {
    zb = g.zbuf;
    pb = g.pbuf;
    
  }

  void clearPixel(int offset, int z) {
    // first-pass only; ellipsoids
    if (zb[offset] > z)
      zb[offset] = Integer.MAX_VALUE;
  }
  
  void addPixel(int offset, int z, int p) {
    zb[offset] = z;
    pb[offset] = p;
  }

}