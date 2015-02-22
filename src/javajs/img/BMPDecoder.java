/* $RCSfile$
 * $Author: nicove $
 * $Date: 2007-03-30 12:26:16 -0500 (Fri, 30 Mar 2007) $
 * $Revision: 7275 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
package javajs.img;

import java.io.BufferedInputStream;
import java.io.IOException;

import javajs.util.Rdr;

/**
 * src: http://www.javaworld.com/article/2077542/learn-java/java-tip-43--how-to-
 * read-8--and-24-bit-microsoft-windows-bitmaps-in-java-applications.html
 *
 * see also: http://en.wikipedia.org/wiki/BMP_file_format
 * 
 * Modified by Bob Hanson hansonr@stolaf.edu
 * 
 * @author Bob Hanson (hansonr@stolaf.edu)
 * 
 */
public class BMPDecoder {

  public BMPDecoder() {
    // for reflection
  }
  
  private BufferedInputStream bis;

  /**
   * original comment:
   * 
   * loadbitmap() method converted from Windows C code. Reads only uncompressed
   * 24- and 8-bit images. Tested with images saved using Microsoft Paint in
   * Windows 95. If the image is not a 24- or 8-bit image, the program refuses
   * to even try. I guess one could include 4-bit images by masking the byte by
   * first 1100 and then 0011. I am not really interested in such images. If a
   * compressed image is attempted, the routine will probably fail by generating
   * an IOException. Look for variable ncompression to be different from 0 to
   * indicate compression is present.
   * 
   * @param bytes
   * @return [image byte array, width, height]
   */
  public Object[] decodeWindowsBMP(byte[] bytes) {
    try {
      bis = Rdr.getBIS(bytes);
      temp = new byte[4];
      // read BITMAPFILEHEADER
      if (readByte() != 'B' || readByte() != 'M')
        return null;
      readInt();   // file size; ignored
      readShort(); // reserved
      readShort(); // reserved
      readInt();   // ptr to pixel array; ignored
      int imageWidth, imageHeight, bitsPerPixel, nColors = 0, imageSize = 0;
      // read BITMAP header
      int headerSize = readInt();
      switch (headerSize) {
      case 12:
        // BITMAPCOREHEADER
        imageWidth = readShort();
        imageHeight = readShort();
        readShort(); // planes
        bitsPerPixel = readShort();
        break;
      case 40:
        // BITMAPINFOHEADER
        imageWidth = readInt();
        imageHeight = readInt();
        readShort(); // planes
        bitsPerPixel = readShort();
        int ncompression = readInt();
        if (ncompression != 0) {
          System.out.println("BMP Compression is :" + ncompression
              + " -- aborting");
          return null;
        }
        imageSize = readInt();
        readInt(); // hres
        readInt(); // vres
        nColors = readInt(); 
        readInt(); // colors used
        break;
      default:
        System.out.println("BMP Header unrecognized, length=" + headerSize
            + " -- aborting");
        return null;
      }
      int nPixels = imageHeight * imageWidth;
      int nread = bitsPerPixel / 8;
      int npad = (imageSize == 0 ? 4 - (imageWidth % 4)
          : (imageSize / imageHeight) - imageWidth * nread) % 4;
      int[] buf = new int[nPixels];
      switch (bitsPerPixel) {
      case 32:
      case 24:
        for (int pt = nPixels - imageWidth; pt >= 0; pt -= imageWidth) {
          for (int i = 0; i < imageWidth; i++)
            buf[pt + i] = readColor(nread);
          for (int i = 0; i < npad; i++)
            readByte();
        }
        break;
      case 8:
        nColors = (nColors > 0 ? nColors : 1 << bitsPerPixel);
        int[] palette = new int[nColors];
        for (int i = 0; i < nColors; i++)
          palette[i] = readColor(4);
        for (int pt = nPixels - imageWidth; pt >= 0; pt -= imageWidth) {
          for (int i = 0; i < imageWidth; i++)
            buf[pt + i] = palette[readByte()];
          for (int i = 0; i < npad; i++)
            readByte();
        }
        break;
      case 1:
        int color1 = readColor(3);
        int color2 = readColor(3);
        npad = (4 - (((imageWidth + 7) / 8) % 4)) % 4;
        int b = 0;
        for (int pt = nPixels - imageWidth; pt >= 0; pt -= imageWidth) {
          for (int i = 0, bpt = -1; i < imageWidth; i++, bpt--) {
            if (bpt < 0) {
              b = readByte();
              bpt = 7;
            }
            buf[pt + i] = ((b & (1 << bpt)) == 0 ? color1
                : color2);
          }
          for (int i = 0; i < npad; i++)
            readByte();
        }
        break;
      default:
        System.out
            .println("Not a 32-, 24-, 8-, or 1-bit Windows Bitmap, aborting...");
        return null;
      }
      return new Object[] { buf, Integer.valueOf(imageWidth),
          Integer.valueOf(imageHeight) };
    } catch (Exception e) {
      System.out.println("Caught exception in loadbitmap!");
    }
    return null;
  }

  private byte[] temp;
  
  private int readColor(int n) throws IOException {
    bis.read(temp, 0, n);
    return 0xff << 24 | ((temp[2] & 0xff) << 16)
        | ((temp[1] & 0xff) << 8) | temp[0] & 0xff;
  }

  private int readInt() throws IOException {
    bis.read(temp, 0, 4);
    return ((temp[3] & 0xff) << 24) | ((temp[2] & 0xff) << 16)
        | ((temp[1] & 0xff) << 8) | temp[0] & 0xff;
  }

  private int readShort() throws IOException {
    bis.read(temp, 0, 2);
    return ((temp[1] & 0xff) << 8) | temp[0] & 0xff;
  }

  private int readByte() throws IOException {
    bis.read(temp, 0, 1);
    return temp[0] & 0xff;
  }

}
