package org.jmol.awtjs2d;


import javajs.api.PlatformViewer;
import javajs.util.P3;

/**
 * methods required by Jmol that access java.awt.Component
 * 
 * private to org.jmol.awt
 * 
 */

class Display {

  /**
   * @param canvas
   * @param widthHeight
   *   
   */
  static void getFullScreenDimensions(Object canvas, int[] widthHeight) {
    /**
     * @j2sNative
     * 
     * widthHeight[0] = canvas.width;
     * widthHeight[1] = canvas.height;
     * 
     */
    {}
  }
  
  static boolean hasFocus(Object canvas) {
    /**
     * @j2sNative
     * 
     */
    {
      System.out.println(canvas);
    }
    return true;
  }

  static void requestFocusInWindow(Object canvas) {
    /**
     * @j2sNative
     * 
     */
    {
      System.out.println(canvas);
    }
  }

  /**
   * legacy apps will use this
   * 
   * @param vwr
   * @param g
   * @param size
   */
  static void renderScreenImage(PlatformViewer vwr, Object g, Object size) {
    /**
     * @j2sNative
     * 
     */
    {
      System.out.println("" + vwr + g + size);
    }
  }

  static void setTransparentCursor(Object canvas) {
    /**
     * @j2sNative
     * 
     */
    {
      System.out.println(canvas);
    }
  }

  static void setCursor(int c, Object canvas) {
    /**
     * @j2sNative
     * 
     */
    {
      System.out.println("" + c + canvas);
    }
  }

  public static String prompt(String label, String data, String[] list,
                              boolean asButtons) {
    /**
     * @j2sNative
     * 
     * var s = prompt(label, data);
     * if (s != null)return s;
     */
    {}
    //TODO -- list and asButtons business
    return "null";
  }

  public static void convertPointFromScreen(Object canvas, P3 ptTemp) {
    /**
     * @j2sNative
     * 
     */
    {
      System.out.println("" + canvas + ptTemp);
    }
  }

  /**
   * Draw the completed image from rendering. Note that the
   * image buffer (org.jmol.g3d.Graphics3D.
   * @param context
   * @param canvas
   * @param x
   * @param y
   * @param width  unused in Jmol proper
   * @param height unused in Jmol proper
   */
  static void drawImage(Object context, Object canvas, int x, int y, int width, int height) {
    /*
     * red=imgData.data[0];
     * green=imgData.data[1];
     * blue=imgData.data[2];
     * alpha=imgData.data[3];
     */
  
    /**
     * @j2sNative
     * 
     var buf8 = canvas.buf8;
     var buf32 = canvas.buf32;
      var n = width * height;
      var dw = (canvas.width - width) * 4;
      for (var i = 0, j = x * 4; i < n;) {
        buf8[j++] = (buf32[i] >> 16) & 0xFF;
        buf8[j++] = (buf32[i] >> 8) & 0xFF;
        buf8[j++] = buf32[i] & 0xFF;
        buf8[j++] = 0xFF;
        if (((++i)%width)==0) j += dw;
      }
      context.putImageData(canvas.imgdata,x,y);
     */
    {
    }
  }



}
