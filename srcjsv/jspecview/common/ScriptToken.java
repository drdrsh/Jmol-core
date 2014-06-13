/* Copyright (c) 2002-2012 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
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

package jspecview.common;

import java.util.Hashtable;

import java.util.Map;
import java.util.Map.Entry;

import javajs.util.Lst;
import javajs.util.PT;
import javajs.util.SB;




/**
 * ScriptToken takes care of script command processing
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */

public enum ScriptToken {

  // null tip means DON'T SHOW
  UNKNOWN,
  APPLETID, 
  APPLETREADYCALLBACKFUNCTIONNAME, 
  AUTOINTEGRATE("TF"), 
  BACKGROUNDCOLOR("C"), 
  CLOSE("spectrumId or fileName or ALL or VIEWS or SIMULATIONS"), 
  COMPOUNDMENUON("TF"), 
  COORDCALLBACKFUNCTIONNAME, 
  COORDINATESCOLOR("C"), 
  COORDINATESON("T"), 
  DEBUG("TF"),
  DEFAULTLOADSCRIPT("\"script...\""),
  DEFAULTNMRNORMALIZATION("maxYvalue"),
  DISPLAYFONTNAME("fontName"), 
  DISPLAY1D("T"), 
  DISPLAY2D("T"), 
  ENABLEZOOM("T"), 
  ENDINDEX, 
  FINDX("x-value"),
  GETPROPERTY("[ALL] [propertyName]"),
  GETSOLUTIONCOLOR(" FILL or FILLNONE or FILLALL or FILLALLNONE"),  
  GRIDCOLOR("C"), 
  GRIDON("T"), 
  HIDDEN("TF"), 
  HIGHLIGHTCOLOR("C"),
  INTEGRALOFFSET("percent"),
  INTEGRALRANGE("percent"),
  INTEGRATE, // same as INTEGRATION
  INTEGRATION("ON/OFF/AUTO/TOGGLE/MIN value/MARK ppm1-ppm2:norm,ppm3-ppm4,... (start with 0-0 to clear)"), 
  INTEGRALPLOTCOLOR, 
  INTEGRATIONRATIOS, 
  INTERFACE, 
  IRMODE("A or T or TOGGLE"), 
  JMOL("...Jmol command..."),
  JSV,
  LABEL("x y [color and/or \"text\"]"),
  LINK("AB or ABC or NONE or ALL"),
  LOAD("[APPEND] \"fileName\" [first] [last]; use \"\" to reload current file"),
  LOADFILECALLBACKFUNCTIONNAME,
  LOADIMAGINARY("T/F - default is to NOT load imaginary spectra"),
  MENUON,
  OBSCURE, 
  OVERLAY, // same as "VIEW"
  OVERLAYSTACKED("TF"),
  PEAK("<type(IR,CNMR,HNMR,MS, etc)> id=xxx or \"match\" [ALL], for example: PEAK HNMR id=3"), 
  PEAKCALLBACKFUNCTIONNAME,
  PEAKLIST(" Example: PEAKLIST threshold=20 [%, or include=10] skip=0 interpolate=parabolic [or NONE]"),
  PEAKTABCOLOR("C"),
  PLOTAREACOLOR("C"), 
  PLOTCOLOR("C"), 
  PLOTCOLORS("color,color,color,..."), 
  PRINT,
  REVERSEPLOT("T"), 
  SCALEBY("factor"),
  SCALECOLOR("C"),
  SCRIPT("filename.jsv"),
  SELECT("spectrumID, spectrumID,..."),
  SETPEAK("x (ppm) or NONE does peak search, unlike SETX -- NMR only"),
  SETX("x (ppm) does no peak search, unlike SETPEAK -- NMR only"),
  SHIFTX("dx (ppm) or NONE -- NMR only"),
  SHOWERRORS,
  SHOWINTEGRATION("T"),
  SHOWKEY("T"),
  SHOWMEASUREMENTS("T"),
  SHOWMENU,
  SHOWPEAKLIST("T"),
  SHOWPROPERTIES,
  SHOWSOURCE,
  SPECTRUM("spectrumID"), 
  SPECTRUMNUMBER,
  STACKOFFSETY("percent"),
  STARTINDEX, 
  SYNCCALLBACKFUNCTIONNAME, 
  SYNCID, 
  TEST, 
  TITLEON("T"), // default OFF for application, ON for applet
  TITLEBOLDON("T"), 
  TITLECOLOR("C"), 
  TITLEFONTNAME("fontName"), 
  UNITSCOLOR("C"), 
  VERSION, 
  VIEW("spectrumID, spectrumID, ... Example: VIEW 3.1, 3.2  or  VIEW \"acetophenone\""),
  XSCALEON("T"), 
  XUNITSON("T"), 
  YSCALE("[ALL] lowValue highValue"), 
  YSCALEON("T"), YUNITSON("T"),
  WINDOW,
  WRITE("[XY,DIF,DIFDUP,PAC,FIX,SQZ,AML,CML,JPG,PDF,PNG,SVG] \"filename\""),
  ZOOM("OUT or x1,x2 or x1,y1 x2,y2"),
  ZOOMBOXCOLOR, ZOOMBOXCOLOR2; // not implemented

  private String tip;

  public String getTip() {
    return "  "
        + (tip == "T" ? "TRUE/FALSE/TOGGLE" 
            : tip == "TF" ? "TRUE or FALSE" 
            : tip == "C" ? "<color>" 
            : tip);
        		
  }

  private ScriptToken() {
  }

  private ScriptToken(String tip) {
    this.tip = tip;
  }

  public static Map<String, ScriptToken> htParams;

  public static ScriptToken getScriptToken(String name) {
    if (htParams == null) {
      htParams = new Hashtable<String, ScriptToken>();
      for (ScriptToken item : values())
        htParams.put(item.name(), item);
    }
    ScriptToken st = htParams.get(name.toUpperCase());
    return (st == null ? UNKNOWN : st);
  }

  public static Lst<ScriptToken> getScriptTokenList(String name,
                                                     boolean isExact) {
    name = name.toUpperCase();
    Lst<ScriptToken> list = new Lst<ScriptToken>();
    ScriptToken st = getScriptToken(name);
    if (isExact) {
      if (st != null)
        list.addLast(st);
    } else {
      for (Entry<String, ScriptToken> entry : htParams.entrySet())
        if (entry.getKey().startsWith(name) 
        		&& entry.getValue().tip != null)
          list.addLast(entry.getValue());
    }
    return list;
  }

  /**
   * tweak command options depending upon special cases
   * 
   * @param st
   * @param params
   * @param cmd
   * @return adjusted value
   */
  public static String getValue(ScriptToken st, ScriptTokenizer params,
                                String cmd) {
    if (!params.hasMoreTokens())
      return "";
    switch (st) {
    default:
      return ScriptTokenizer.nextStringToken(params, true);
    case CLOSE:
    case GETPROPERTY:
    case INTEGRATION:
    case INTEGRATE:
    case JMOL:
    case LABEL:
    case LOAD:
    case PEAK:
    case PLOTCOLORS:
    case YSCALE:
    case WRITE:
      // take full command
      return removeCommandName(cmd);
    case SELECT:
    case OVERLAY: // deprecated
    case VIEW:
    case ZOOM:
      // commas to spaces
      return removeCommandName(cmd).replace(',', ' ').trim();
    }
  }

  private static String removeCommandName(String cmd) {
    int pt = cmd.indexOf(" ");
    if (pt < 0)
      return "";
    return cmd.substring(pt).trim();
  }

  public static String getKey(ScriptTokenizer eachParam) {
    String key = eachParam.nextToken();
    if (key.startsWith("#") || key.startsWith("//"))
      return null;
    if (key.equalsIgnoreCase("SET"))
      key = eachParam.nextToken();
    return key.toUpperCase();
  }

  /**
   * read a string for possibly quoted tokens separated by space until // or #
   * is reached.
   * 
   * @param value
   * @return list of tokens
   */
  public static Lst<String> getTokens(String value) {
		if (value.startsWith("'") && value.endsWith("'"))
			value = "\"" + PT.trim(value, "'") + "\"";
    Lst<String> tokens = new Lst<String>();
    ScriptTokenizer st = new ScriptTokenizer(value, false);
    while (st.hasMoreTokens()) {
      String s = ScriptTokenizer.nextStringToken(st, false);
      if (s.startsWith("//") || s.startsWith("#"))
        break;
      tokens.addLast(s);
    }
    return tokens;
  }

  public static String getNameList(Lst<ScriptToken> list) {
    if (list.size() == 0)
      return "";
    SB sb = new SB();
    for (int i = 0; i < list.size(); i++)
      sb.append(",").append(list.get(i).toString());
    return sb.toString().substring(1);
  }

}
