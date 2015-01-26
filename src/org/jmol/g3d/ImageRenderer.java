/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2011-10-10 20:09:00 -0500 (Mon, 10 Oct 2011) $
 * $Revision: 16309 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.g3d;

import org.jmol.api.JmolRendererInterface;

class ImageRenderer {

  /**
   * 
   * @param x
   * @param y
   * @param z
   * @param image
   * @param g3d
   * @param jmolRenderer
   * @param antialias
   *        UNUSED
   * @param argbBackground
   * @param textWidth
   * @param textHeight
   */
  static void plotImage(int x, int y, int z, Object image, Graphics3D g3d,
                        JmolRendererInterface jmolRenderer, boolean antialias,
                        int argbBackground, int textWidth, int textHeight) {
    boolean isBackground = (x == Integer.MIN_VALUE);
    int bgcolor = (isBackground ? g3d.bgcolor : argbBackground);
    /*
     *  this was for transparent background, which we have disabled, I think, in Jmol 12
    boolean haveTranslucent = false;
    PixelGrabber pg1 = new PixelGrabber(image, 0, 0, width0, height0, true);
    if (pg1.getColorModel().hasAlpha())
      try {
        pg1.grabPixels();
        int[] buffer = (int[]) pg1.getPixels();
        for (int i = 0; i < buffer.length; i++)
          if ((buffer[i] & 0xFF00000) != 0xFF000000) {
            haveTranslucent = true;
            break;
          }
        System.out.println(buffer.length + " " + haveTranslucent + " "
            + pg1.getColorModel().hasAlpha());
      } catch (InterruptedException e) {
        // impossible?
        return;
      }
      */
    if (isBackground) {
      x = 0;
      z = Integer.MAX_VALUE - 1;
      textWidth = g3d.width;
      textHeight = g3d.height;
    }
    if (x + textWidth <= 0 || x >= g3d.width || y + textHeight <= 0
        || y >= g3d.height)
      return;
    Object g;
    /**
     * @j2sNative
     * 
     *            g = null;
     * 
     */
    {
      g = g3d.platform.getGraphicsForTextOrImage(textWidth, textHeight);
    }
    int[] buffer = g3d.apiPlatform.drawImageToBuffer(g,
        g3d.platform.offscreenImage, image, textWidth, textHeight,
        isBackground ? bgcolor : 0);
    if (buffer == null)
      return; // not supported on this platform (yet)
      /*    
          int n = 0;
          for (int i = 0; i < buffer.length; i++) {
            if ((buffer[i] & 0xFF000000) != 0xFF000000) {
              //System.out.println("testing " + i + " " + buffer[i]);
              n++;
            }
          }
          System.out.println(n + " transparent argbBackground=" + argbBackground);
      */
    int[] zbuf = g3d.zbuf;
    int width = g3d.width;
    Pixelator p = g3d.pixel;
    int height = g3d.height;
    int tlog = g3d.translucencyLog;
    if (jmolRenderer != null
        || (x < 0 || x + textWidth > width || y < 0 || y + textHeight > height)) {
      if (jmolRenderer == null)
        jmolRenderer = g3d;
      for (int i = 0, offset = 0; i < textHeight; i++)
        for (int j = 0; j < textWidth; j++)
          jmolRenderer.plotImagePixel(buffer[offset++], x + j, y + i, z, 8, bgcolor, width,
              height, zbuf, p, tlog);
    } else {
      for (int i = 0, offset = 0, pbufOffset = y * width + x; i < textHeight; i++, pbufOffset += (width - textWidth)) {
        for (int j = 0; j < textWidth; j++, pbufOffset++, offset++)
          if (z < zbuf[pbufOffset])
            p.addPixel(pbufOffset, z, buffer[offset]);
      }
    }
  }

}
