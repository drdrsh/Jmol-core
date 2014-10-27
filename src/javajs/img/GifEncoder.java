/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-06-02 12:14:13 -0500 (Sat, 02 Jun 2007) $
 * $Revision: 7831 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
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

// useful page: http://www.htmlhexcolor.com/0ac906 
//
//  GifEncoder - write out an image as a GIF
// 
//  Transparency handling and variable bit size courtesy of Jack Palevich.
//  
//  Copyright (C)1996,1998 by Jef Poskanzer <jef@mail.acme.com>. All rights reserved.
//  
//  Redistribution and use in source and binary forms, with or without
//  modification, are permitted provided that the following conditions
//  are met:
//  1. Redistributions of source code must retain the above copyright
//     notice, this list of conditions and the following disclaimer.
//  2. Redistributions in binary form must reproduce the above copyright
//     notice, this list of conditions and the following disclaimer in the
//     documentation and/or other materials provided with the distribution.
// 
//  THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
//  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
//  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
//  ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
//  FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
//  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
//  OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
//  HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
//  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
//  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
//  SUCH DAMAGE.
// 
//  Visit the ACME Labs Java page for up-to-date versions of this and other
//  fine Java utilities: http://www.acme.com/java/
// 
/// Write out an image as a GIF.
// <P>
// <A HREF="/resources/classes/Acme/JPM/Encoders/GifEncoder.java">Fetch the software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>
// <P>
// @see ToGif


package javajs.img;

import javajs.util.AU;
import javajs.util.CU;
import javajs.util.Lst;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.io.IOException;

/**
 * 
 * GifEncoder extensively modified for Jmol by Bob Hanson
 * 
 * -- using median-cut with rgb
 * 
 * -- TODO use median-cut with HSL
 * 
 * -- much simplified interface with ImageEncoder
 * 
 * -- uses simple Hashtable with Integer()
 * 
 * -- adds adaptive color reduction to generate 256 colors
 *      Reduction algorithm simply removes lower bits of red, green, and blue
 *      one at a time until the number of sets is <= 256. Then it creates a
 *      color for each set that is a weighted average of all the colors for that set.
 *      Seems to work reasonably well. Mapped isosurfaces look pretty crude.
 * 
 * -- allows progressive production of animated GIF via Jmol CAPTURE command
 * 
 * -- uses general purpose javajs.util.OutputChannel for byte-handling options
 *    such as posting to a server, writing to disk, and retrieving bytes.
 *    
 * -- allows JavaScript port
 *    
 * -- Bob Hanson, 24 Sep 2013
 *    
 * @author Bob Hanson hansonr@stolaf.edu
 */

public class GifEncoder extends ImageEncoder {

  private int bitsPerPixel = 1;
  private int[][] errors;
  private boolean interlaced;
  private boolean addHeader = true;
  private boolean addImage = true;
  private boolean addTrailer = true;
  private int delayTime100ths = -1;
  private boolean looping;
  private Map<String, Object> params;
  private int byteCount;
  private boolean isTransparent;
  private boolean floydSteinberg = true;
  int backgroundColor;
  Map<Integer, ColorCell> colorMap;
  Map<Integer, ColorCell> colors256;
  int[] red, green, blue;
  int[] indexes;

  private class ColorItem {

    int rgb;
    int r;
    int g;
    int b;
    int count;

    ColorItem(int rgb) {
      this.rgb = rgb;
      r = (rgb >> 16) & 0xFF;
      g = (rgb >> 8) & 0xFF;
      b = rgb & 0xFF;
    }
    
    @Override
    public String toString() {
      return Integer.toHexString(rgb) + " " + r + "  " + g + " " + b;
    }
  }

  protected class ColorVector extends Lst<ColorItem> {

    private Lst<ColorCell> boxes;
    
    void indexColors() {
      // goal is to create an index set and and to generate rgb errors for each color
      boxes = new Lst<ColorCell>();
      // start with just two boxes -- fixed background color and all others
      ColorCell cc = new ColorCell(0);
      cc.addItem(new ColorItem(backgroundColor));
      boxes.addLast(cc);
      cc = new ColorCell(1);
      for (int i = size(); --i >= 0;) {
        ColorItem c = get(i);
        if (c.rgb != backgroundColor)
          cc.addItem(c);
      }
      boxes.addLast(cc);
      int n;
      while ((n = boxes.size()) < 256 && splitBoxes()) {
        // loop
      }
      clear();
      colorMap = new Hashtable<Integer, ColorCell>();
      colors256 = new Hashtable<Integer, ColorCell>();
      for (int i = 0; i < n; i++)
        addLast(boxes.get(i).average());
      for (int i = 0; i < n; i++)
        boxes.get(i).setErrors();
    }
    
    private boolean splitBoxes() {
      int n = boxes.size();
      double maxVol = 0;
      int imax = -1;
      for (int i = n; --i >= 1;) {
        double v = boxes.get(i).getVolume();
        if (v > maxVol) {
          maxVol = v;
          imax = i;
        }
      }
      if (imax < 0)
        return false;
      //System.out.println("splitting box " + imax);
      boxes.get(imax).splitBox(boxes);
      return true;
    }

  }

  private class ColorCell {
    protected int index;
    // counts here are counts of color occurances for this grouped set.
    // ints here allow for 2147483647/0x100 = count of 8388607 for THIS average color, which should be fine.
    private int r, g, b;
    // min and max based on 0 0 0 for this rgb
    private int maxr = Integer.MAX_VALUE, 
        minr = -Integer.MAX_VALUE, 
        maxg = Integer.MAX_VALUE, 
        ming = -Integer.MAX_VALUE, 
        maxb = Integer.MAX_VALUE, 
        minb = -Integer.MAX_VALUE;
    private int rmaxr = -Integer.MAX_VALUE, 
        rminr = Integer.MAX_VALUE, 
        rmaxg = -Integer.MAX_VALUE, 
        rming = Integer.MAX_VALUE, 
        rmaxb = -Integer.MAX_VALUE, 
        rminb = Integer.MAX_VALUE;
    private int maxre = Integer.MAX_VALUE, 
        minre = -Integer.MAX_VALUE, 
        maxge = Integer.MAX_VALUE, 
        minge = -Integer.MAX_VALUE, 
        maxbe = Integer.MAX_VALUE, 
        minbe = -Integer.MAX_VALUE;
    private ColorCell nextr, prevr, nextg, prevg, nextb, prevb;
    int rgb;
    Lst<ColorItem> lst;
    private double volume;

    ColorCell(int index) {
      this.index = index;
      lst = new Lst<ColorItem>();
    }
      
    public double getVolume() {
      if (volume != 0)
        return volume;
      if (lst.size() < 2)
        return -1;
      double d;
      rmaxr = -Integer.MAX_VALUE;
      rminr = Integer.MAX_VALUE;
      rmaxg = -Integer.MAX_VALUE;
      rming = Integer.MAX_VALUE;
      rmaxb = -Integer.MAX_VALUE;
      rminb = Integer.MAX_VALUE;
      int n = lst.size();
      for (int i = n; --i >= 0;) {
        ColorItem c = lst.get(i);
        if (c.r < rminr)
          rminr = c.r;
        if (c.g < rming)
          rming = c.g;
        if (c.b < rminb)
          rminb = c.b;
        if (c.r > rmaxr)
          rmaxr = c.r;
        if (c.g > rmaxg)
          rmaxg = c.g;
        if (c.b > rmaxb)
          rmaxb = c.b;
      }
      return volume = ((d = rmaxr - rminr) * d
          + (d = rmaxg - rming) * d + (d = rmaxb - rminb) * d);
    }

    void setErrors() {
      if (nextr != null)
        maxre = ((nextr.minr + maxr) >> 1) - r;
      if (nextg != null)
        maxge = ((nextg.ming + maxg) >> 1) - g;
      if (nextb != null)
        maxbe = ((nextb.minb + maxb) >> 1) - b;
      if (prevr != null)
        minre = ((prevr.maxr + minr) >> 1) - r;
      if (prevg != null)
        minge = ((prevg.maxg + ming) >> 1) - g;
      if (prevb != null)
        minbe = ((prevb.maxb + minb) >> 1) - b;
    }

    void addItem(ColorItem c) {
      lst.addLast(c);
    }

    ColorItem average() {
      int count = lst.size();
      for (int i = count; --i >= 0;) {
        ColorItem c = lst.get(i);
        colorMap.put(Integer.valueOf(c.rgb), this);
        r += c.r;
        g += c.g;
        b += c.b;
      }
      r = (r / count) & 0xff;
      g = (g / count) & 0xff;
      b = (b / count) & 0xff;
      rgb = CU.rgb(r, g, b);
      red[index] = r;
      green[index] = g;
      blue[index] = b;
      /*
      for (int i = size(); --i >= 0;) {
        int rgb = get(i).rgb;
        r = (rgb & 0xFCFCFC)>> 2;
        System.out.println("draw id 'd"+index+"_"+i+"' width 0.5 " + CU.colorPtFromInt(r, null) + " color "+CU.colorPtFromInt(rgb, null)+"");

      }
      r = (rgb & 0xFCFCFC)>> 2;      
      System.out.println("draw id 'c"+index+"' width 1.0 " + CU.colorPtFromInt(r, null) + " color "+CU.colorPtFromInt(rgb, null)+"");
      
      */
      colors256.put(Integer.valueOf(this.rgb), this);
      //System.out.println(index + " " + r + " " + g + " " + b + " " + (maxr - minr)+ " " + (maxg - ming) + " " + (maxb-minb));
      return new ColorItem(rgb);
    }

    private int[] ar, ag, ab;
        
    /**
     * use median_cut algorithm to split the box, 
     * creating a doubly linked list
     * 
     * @param boxes
     */
    protected void splitBox(Lst<ColorCell> boxes) {
      int  n = lst.size();
      if (n < 2)
        return;
      int newIndex = boxes.size();
      ColorCell newBox = new ColorCell(newIndex);
      boxes.addLast(newBox);
      for (int i = 0; i < 3; i++)
        getArray(i);
      int ranger = ar[n - 1] - ar[0];
      int rangeg = ag[n - 1] - ag[0];
      int rangeb = ab[n - 1] - ab[0];
      int mode = (ranger >= rangeg ? (ranger >= rangeb ? 0 : 2)
          : rangeg >= rangeb ? 1 : 2);
      int[] a = (mode == 0 ? ar : mode == 1 ? ag : ab);
      int median = n / 2;
      int val = a[median];
      int dir = (val == a[0] ? 1 : -1);
      while (median >= 0 && median < n && a[median] == val) {
        median += dir;
      }        
      if (dir == -1)
        median++;
      val = a[median];
      newBox.nextr = nextr;
      newBox.nextg = nextg;
      newBox.nextb = nextb;
      newBox.prevr = prevr;
      newBox.prevg = prevg;
      newBox.prevb = prevb;
      newBox.minr = minr;
      newBox.ming = ming;
      newBox.minb = minb;
      newBox.maxr = maxr;
      newBox.maxg = maxg;
      newBox.maxb = maxb;
      //System.out.println("split " + index + " " + newBox.index);
      volume = 0;
      switch (mode) {
      case 0:
        for (int i = lst.size(); --i >= 0;)
          if (lst.get(i).r >= val)
            newBox.addItem(lst.remove(i));
        newBox.prevr = this;
        nextr = newBox;
        maxr = val - 1;
        newBox.minr = val;
        break;
      case 1:
        for (int i = lst.size(); --i >= 0;)
          if (lst.get(i).g >= val)
            newBox.addItem(lst.remove(i));
        newBox.prevg = this;
        nextg = newBox;
        maxg = val - 1;
        newBox.ming = val;
        break;
      case 2:
        for (int i = lst.size(); --i >= 0;)
          if (lst.get(i).b >= val)
            newBox.addItem(lst.remove(i));
        newBox.prevb = this;
        nextb = newBox;
        maxb = val - 1;
        newBox.minb = val;
        break;      
      }
      //System.out.println(this + " -"+mode+"-> " + newBox +" " + lst.size() + "/" + newBox.lst.size());
    }

    /**
     * Get sorted array of unique component entries
     * 
     * @param ic 0(red) 1(green) 2(blue)
     */
    private void getArray(int ic) {
      int[] a = new int[lst.size()];
      for (int i = a.length; --i >= 0;) {
        ColorItem c = lst.get(i);
        a[i] = (ic == 0 ? c.r : ic == 1 ? c.g : c.b);
      }
      Arrays.sort(a);
      switch (ic) {
      case 0:
        ar = a;
        break;
      case 1:
        ag = a;
        break;
      case 2:
        ab = a;
      }
    }

    /**
     * 
     * Find nearest cell; return errors in [r g b]
     * @param rgb
     * @param err
     * @return color cell
     * 
     */
    ColorCell findCell(int rgb, int[] err) {
      err[0] = ((rgb >> 16) & 0xFF) - r;
      //System.out.println(Integer.toHexString(rgb) + " " + this + " " + PT.toJSON(null, err));
      if (err[0] > maxre && nextr != null)
        return nextr.findCell(rgb, err);
      if (err[0] < minre && prevr != null)
        return prevr.findCell(rgb, err);
      
      err[1] = ((rgb >> 8) & 0xFF) - g;
      if (err[1] > maxge && nextg != null)
        return nextg.findCell(rgb, err);
      if (err[1] < minge && prevg != null)
        return prevg.findCell(rgb, err);
      
      err[2] = (rgb & 0xFF) - b;
      if (err[2] > maxbe && nextb != null)
        return nextb.findCell(rgb, err);
      if (err[2] < minbe && prevb != null)
        return prevb.findCell(rgb, err);
      return this; // in this box or best we can do
    }    
    
    @Override
    public String toString() {
      return index + " " + Integer.toHexString(rgb);
    }
  }

  /**
   * we allow for animated GIF by being able to re-enter
   * the code with different parameters held in params
   * 
   * 
   */
  @Override
  protected void setParams(Map<String, Object> params) {
    this.params = params;    
     Integer ic = (Integer) params.get("transparentColor");
     if (ic == null) {
       ic = (Integer) params.get("backgroundColor");
       if (ic != null)
         backgroundColor = ic.intValue();
     } else {
       backgroundColor = ic.intValue();
       isTransparent = true;
     }

     floydSteinberg = false;
     
     logging = true;
     
    interlaced = (Boolean.TRUE == params.get("interlaced"));
    if (interlaced || !params.containsKey("captureMode"))
      return;
    try {
      byteCount = ((Integer) params.get("captureByteCount")).intValue();
    } catch (Exception e) {
      // ignore
    }
    int imode = "maec".indexOf(((String) params.get("captureMode")).substring(0, 1));
    
    if (logging)
      System.out.println("GIF capture mode " + imode);
    switch (imode) {
    case 0: //"movie"
      params.put("captureMode", "add");
      addImage = false;
      addTrailer = false;
      break;
    case 1: // add 
      addHeader = false;
      addTrailer = false;
      int fps = Math.abs(((Integer) params.get("captureFps")).intValue());
      delayTime100ths =  (fps == 0 ? 0 : 100 / fps);
      looping = (Boolean.FALSE != params.get("captureLooping"));
      break;
    case 2: // end
      addHeader = false;
      addImage = false;
      break;
    case 3: // cancel
      addHeader = false;
      addImage = false;
      out.cancel();
      break;
    }
  }


  @Override
  protected void generate() throws IOException {
    if (addHeader)
      writeHeader();
    addHeader = false; // only one header
    if (addImage) {
      createColorTable();
      writeGraphicControlExtension();
      if (delayTime100ths >= 0 && looping)
        writeNetscapeLoopExtension();
      writeImage();
    }
  }

  @Override
  protected void close() {
    if (addTrailer) {
      writeTrailer();
    } else {
      doClose = false;
    }
    params.put("captureByteCount", Integer.valueOf(byteCount));
  }

  /**
   * includes logical screen descriptor
   * @throws IOException
   */
  private void writeHeader() throws IOException {
    putString("GIF89a");
    putWord(width);
    putWord(height);
    putByte(0); // no global color table -- using local instead
    putByte(0); // no background
    putByte(0); // no pixel aspect ratio given
  }

  /**
   * generates a 256-color or fewer color table consisting of a set of red,
   * green, blue arrays and a hash table pointing to a color index; adapts to
   * situations where more than 256 colors are present.
   * 
   */
  private void createColorTable() {
    ColorVector colors = getColors();
    int nTotal = colors.size();//colors256.size();
    setBitsPerPixel(nTotal);
    colors.indexColors();
    ditherPixels();
  }

  private void setBitsPerPixel(int nTotal) {
    bitsPerPixel = (nTotal <= 2 ? 1 : nTotal <= 4 ? 2 : nTotal <= 16 ? 4 : 8);
    int mapSize = 1 << bitsPerPixel;
    red = new int[mapSize];
    green = new int[mapSize];
    blue = new int[mapSize];
  }

  /**
   * Generate a list of all unique colors in the image.
   * 
   * @return the vector
   */
  private ColorVector getColors() {
    int n = pixels.length;
    errors = AU.newInt2(n);
    indexes = new int[n];
    ColorVector colorVector = new ColorVector();
    Map<Integer, ColorItem> ciHash = new Hashtable<Integer, ColorItem>();
    int nColors = 0;
    for (int i = 0; i < n; i++)
      nColors += addColor(colorVector, ciHash, i);
    ciHash = null;
    //colorVector.sort();
    System.out.println("# total image colors = " + nColors);
    // dont sort by frequency
    return colorVector;
  }

  private int addColor(ColorVector colorVector, Map<Integer, ColorItem> ciHash,
                       int pt) {
    int rgb = pixels[pt];
    Integer key = Integer.valueOf(rgb);
    ColorItem item = ciHash.get(key);
    if (item == null) {
      item = new ColorItem(rgb);
      ciHash.put(key, item);
      colorVector.addLast(item);
      return 1;
    }
    item.count++;
    return 0;
  }

  /**
   * 
   * Idea is to find the closest known color
   * and then spread out the error over four pixels
   * 
   */
  private void ditherPixels() {
    for (int i = 0, p = 0; i < height; ++i) {
      boolean notLastRow = (i != height - 1);
      for (int j = 0; j < width; ++j, p++) {
        int rgb = getRGB(p);
        try {
          ColorCell app = colors256.get(Integer.valueOf(rgb));
          if (app == null) {
            int[] err = new int[3];
            app = colorMap.get(Integer.valueOf(pixels[p]));
            if (floydSteinberg) {
              app = app.findCell(rgb, err);
              colorMap.put(Integer.valueOf(rgb), app);
              boolean notLastCol = (j < width - 1);
              if (notLastCol)
                addError(err, 7, p + 1);
              if (notLastRow) {
                if (j > 0)
                  addError(err, 3, p + width - 1);
                addError(err, 5, p + width);
                if (notLastCol)
                  addError(err, 1, p + width + 1);
              }
            }
          }
          indexes[p] = app.index;
        } catch (Throwable e) {
          System.out.println("GIF error: " + e);
        }
      }
    }
  }
  
  private int getRGB(int p) {
    int[] err = errors[p];
    int rgb = pixels[p];
    if (err == null)
      return rgb;
    int r = clamp(((rgb >> 16) & 0xFF) + err[0]);
    int g = clamp(((rgb >> 8) & 0xFF) + err[1]);
    int b = clamp(((rgb) & 0xFF) + err[2]);   
    return CU.rgb(r, g, b); 
  }


  private void addError(int[] err, int f, int p) {
    int[] errp = errors[p];
    if (errp == null)
      errp = errors[p] = new int[3];
    for (int i = 0; i < 3; i++)
      errp[i] += (err[i] * f) >> 4;
  }

  int clamp(int c) {
    return c < 0 ? 0 : c > 255 ? 255 : c;
  }

  
  private void writeGraphicControlExtension() {
    if (isTransparent || delayTime100ths >= 0) {
      putByte(0x21); // graphic control extension
      putByte(0xf9); // graphic control label
      putByte(4); // block size
      putByte((isTransparent ? 9 : 0) | (delayTime100ths > 0 ? 2 : 0)); // packed bytes 
      putWord(delayTime100ths > 0 ? delayTime100ths : 0);
      putByte(0); // transparent index
      putByte(0); // end-of-block
    }
  }

// see  http://www.vurdalakov.net/misc/gif/netscape-looping-application-extension
//      +---------------+
//   0  |     0x21      |  Extension Label
//      +---------------+
//   1  |     0xFF      |  Application Extension Label
//      +---------------+
//   2  |     0x0B      |  Block Size
//      +---------------+
//   3  |               | 
//      +-             -+
//   4  |               | 
//      +-             -+
//   5  |               | 
//      +-             -+
//   6  |               | 
//      +-  NETSCAPE   -+  Application Identifier (8 bytes)
//   7  |               | 
//      +-             -+
//   8  |               | 
//      +-             -+
//   9  |               | 
//      +-             -+
//  10  |               | 
//      +---------------+
//  11  |               | 
//      +-             -+
//  12  |      2.0      |  Application Authentication Code (3 bytes)
//      +-             -+
//  13  |               | 
//      +===============+                      --+
//  14  |     0x03      |  Sub-block Data Size   |
//      +---------------+                        |
//  15  |     0x01      |  Sub-block ID          |
//      +---------------+                        | Application Data Sub-block
//  16  |               |                        |
//      +-             -+  Loop Count (2 bytes)  |
//  17  |               |                        |
//      +===============+                      --+
//  18  |     0x00      |  Block Terminator
//      +---------------+

  private void writeNetscapeLoopExtension() {
    putByte(0x21); // graphic control extension
    putByte(0xff); // netscape loop extension
    putByte(0x0B); // block size
    putString("NETSCAPE2.0");
    putByte(3); 
    putByte(1); 
    putWord(0); // loop indefinitely
    putByte(0); // end-of-block
    
  }

  private int initCodeSize;
  private int curpt;

  private void writeImage() {
    putByte(0x2C);
    putWord(0); //left
    putWord(0); //top
    putWord(width);
    putWord(height);

    //    <Packed Fields>  =      LISx xZZZ

    //    L Local Color Table Flag
    //    I Interlace Flag
    //    S Sort Flag
    //    x Reserved
    //    ZZZ Size of Local Color Table

    int packedFields = 0x80 | (interlaced ? 0x40 : 0) | (bitsPerPixel - 1);
    putByte(packedFields);
    int colorMapSize = 1 << bitsPerPixel;
    for (int i = 0; i < colorMapSize; i++) {
      putByte(red[i]);
      putByte(green[i]);
      putByte(blue[i]);
    }
    putByte(initCodeSize = (bitsPerPixel <= 1 ? 2 : bitsPerPixel));
    compress();
    putByte(0);
  }

  private void writeTrailer() {
    // Write the GIF file terminator
    putByte(0x3B);
  }

  ///// compression routines /////
  
  private static final int EOF = -1;

  // Return the next pixel from the image
  private int nextPixel() {
    if (countDown-- == 0)
      return EOF;
    int colorIndex = indexes[curpt];
    // Bump the current X position
    ++curx;
    if (curx == width) {
      // If we are at the end of a scan line, set curx back to the beginning
      // If we are interlaced, bump the cury to the appropriate spot,
      // otherwise, just increment it.
      curx = 0;
      if (interlaced)
        updateY(INTERLACE_PARAMS[pass], INTERLACE_PARAMS[pass + 4]);
      else
       ++cury;
    }
    curpt = cury * width + curx;
    return colorIndex & 0xff;
  }

  private static final int[] INTERLACE_PARAMS = {
    8, 8, 4, 2, 
    4, 2, 1, 0};

  /**
   * 
   *   Group 1 : Every 8th. row, starting with row 0.              (Pass 1)
   *   
   *   Group 2 : Every 8th. row, starting with row 4.              (Pass 2)
   *   
   *   Group 3 : Every 4th. row, starting with row 2.              (Pass 3)
   *   
   *   Group 4 : Every 2nd. row, starting with row 1.              (Pass 4)
   * 
   * @param yNext
   * @param yNew
   */
  private void updateY(int yNext, int yNew) {
    cury += yNext;
    if (yNew >= 0 && cury >= height) {
      cury = yNew;
      ++pass;
    }
  }

  // Write out a word to the GIF file
  private void putWord(int w) {
    putByte(w);
    putByte(w >> 8);
  }

  // GIFCOMPR.C       - GIF Image compression routines
  //
  // Lempel-Ziv compression based on 'compress'.  GIF modifications by
  // David Rowley (mgardi@watdcsu.waterloo.edu)

  // General DEFINEs

  private static final int BITS = 12;

  private static final int HSIZE = 5003; // 80% occupancy

  // GIF Image compression - modified 'compress'
  //
  // Based on: compress.c - File compression ala IEEE Computer, June 1984.
  //
  // By Authors:  Spencer W. Thomas      (decvax!harpo!utah-cs!utah-gr!thomas)
  //              Jim McKie              (decvax!mcvax!jim)
  //              Steve Davies           (decvax!vax135!petsd!peora!srd)
  //              Ken Turkowski          (decvax!decwrl!turtlevax!ken)
  //              James A. Woods         (decvax!ihnp4!ames!jaw)
  //              Joe Orost              (decvax!vax135!petsd!joe)

  private int nBits; // number of bits/code
  private int maxbits = BITS; // user settable max # bits/code
  private int maxcode; // maximum code, given n_bits
  private int maxmaxcode = 1 << BITS; // should NEVER generate this code

  private final static int MAXCODE(int nBits) {
    return (1 << nBits) - 1;
  }

  private int[] htab = new int[HSIZE];
  private int[] codetab = new int[HSIZE];

  private int hsize = HSIZE; // for dynamic table sizing

  private int freeEnt = 0; // first unused entry

  // block compression parameters -- after all codes are used up,
  // and compression rate changes, start over.
  private boolean clearFlag = false;

  // Algorithm:  use open addressing double hashing (no chaining) on the
  // prefix code / next character combination.  We do a variant of Knuth's
  // algorithm D (vol. 3, sec. 6.4) along with G. Knott's relatively-prime
  // secondary probe.  Here, the modular division first probe is gives way
  // to a faster exclusive-or manipulation.  Also do block compression with
  // an adaptive reset, whereby the code table is cleared when the compression
  // ratio decreases, but after the table fills.  The variable-length output
  // codes are re-sized at this point, and a special CLEAR code is generated
  // for the decompressor.  Late addition:  construct the table according to
  // file size for noticeable speed improvement on small files.  Please direct
  // questions about this implementation to ames!jaw.

  private int clearCode;
  private int EOFCode;

  private int countDown;
  private int pass = 0;
  private int curx, cury;

  private void compress() {

    // Calculate number of bits we are expecting
    countDown = width * height;

    // Indicate which pass we are on (if interlace)
    pass = 0;
    // Set up the current x and y position
    curx = 0;
    cury = 0;

    // Set up the necessary values
    clearFlag = false;
    nBits = initCodeSize + 1;
    maxcode = MAXCODE(nBits);

    clearCode = 1 << initCodeSize;
    EOFCode = clearCode + 1;
    freeEnt = clearCode + 2;

    // Set up the 'byte output' routine
    bufPt = 0;

    int ent = nextPixel();

    int hshift = 0;
    int fcode;
    for (fcode = hsize; fcode < 65536; fcode *= 2)
      ++hshift;
    hshift = 8 - hshift; // set hash code range bound

    int hsizeReg = hsize;
    clearHash(hsizeReg); // clear hash table

    output(clearCode);

    int c;
    outer_loop: while ((c = nextPixel()) != EOF) {
      fcode = (c << maxbits) + ent;
      int i = (c << hshift) ^ ent; // xor hashing

      if (htab[i] == fcode) {
        ent = codetab[i];
        continue;
      } else if (htab[i] >= 0) // non-empty slot
      {
        int disp = hsizeReg - i; // secondary hash (after G. Knott)
        if (i == 0)
          disp = 1;
        do {
          if ((i -= disp) < 0)
            i += hsizeReg;

          if (htab[i] == fcode) {
            ent = codetab[i];
            continue outer_loop;
          }
        } while (htab[i] >= 0);
      }
      output(ent);
      ent = c;
      if (freeEnt < maxmaxcode) {
        codetab[i] = freeEnt++; // code -> hashtable
        htab[i] = fcode;
      } else {
        clearBlock();
      }
    }
    // Put out the final code.
    output(ent);
    output(EOFCode);
  }

  // output
  //
  // Output the given code.
  // Inputs:
  //      code:   A n_bits-bit integer.  If == -1, then EOF.  This assumes
  //              that n_bits =< wordsize - 1.
  // Outputs:
  //      Outputs code to the file.
  // Assumptions:
  //      Chars are 8 bits long.
  // Algorithm:
  //      Maintain a BITS character long buffer (so that 8 codes will
  // fit in it exactly).  Use the VAX insv instruction to insert each
  // code in turn.  When the buffer fills up empty it and start over.

  private int curAccum = 0;
  private int curBits = 0;

  private int masks[] = { 0x0000, 0x0001, 0x0003, 0x0007, 0x000F, 0x001F,
      0x003F, 0x007F, 0x00FF, 0x01FF, 0x03FF, 0x07FF, 0x0FFF, 0x1FFF, 0x3FFF,
      0x7FFF, 0xFFFF };

  private void output(int code) {
    curAccum &= masks[curBits];

    if (curBits > 0)
      curAccum |= (code << curBits);
    else
      curAccum = code;

    curBits += nBits;

    while (curBits >= 8) {
      byteOut((byte) (curAccum & 0xff));
      curAccum >>= 8;
      curBits -= 8;
    }

    // If the next entry is going to be too big for the code size,
    // then increase it, if possible.
    if (freeEnt > maxcode || clearFlag) {
      if (clearFlag) {
        maxcode = MAXCODE(nBits = initCodeSize + 1);
        clearFlag = false;
      } else {
        ++nBits;
        if (nBits == maxbits)
          maxcode = maxmaxcode;
        else
          maxcode = MAXCODE(nBits);
      }
    }

    if (code == EOFCode) {
      // At EOF, write the rest of the buffer.
      while (curBits > 0) {
        byteOut((byte) (curAccum & 0xff));
        curAccum >>= 8;
        curBits -= 8;
      }
      flushBytes();
    }
  }

  // Clear out the hash table

  // table clear for block compress
  private void clearBlock() {
    clearHash(hsize);
    freeEnt = clearCode + 2;
    clearFlag = true;

    output(clearCode);
  }

  // reset code table
  private void clearHash(int hsize) {
    for (int i = 0; i < hsize; ++i)
      htab[i] = -1;
  }

  // GIF-specific routines (byte array buffer)

  // Number of bytes so far in this 'packet'
  private int bufPt;

  // Define the storage for the packet accumulator
  final private byte[] buf = new byte[256];

  // Add a byte to the end of the current packet, and if it is 254
  // byte, flush the packet to disk.
  private void byteOut(byte c) {
    buf[bufPt++] = c;
    if (bufPt >= 254)
      flushBytes();
  }

  // Flush the packet to disk, and reset the accumulator
  protected void flushBytes() {
    if (bufPt > 0) {
      putByte(bufPt);
      out.write(buf, 0, bufPt);
      byteCount += bufPt;
      bufPt = 0;
    }
  }
}
