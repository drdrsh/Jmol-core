/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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

package org.jmol.shape;

import org.jmol.c.PAL;
import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.LabelToken;
import org.jmol.modelset.Text;
import org.jmol.script.SV;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.C;

import javajs.awt.Font;
import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.P3;

import org.jmol.viewer.ActionManager;
import org.jmol.viewer.JC;

import java.util.Hashtable;


import java.util.Map;

public class Labels extends AtomShape {

  public String[] strings;
  public String[] formats;
  public short[] bgcolixes;
  public byte[] fids;
  public int[] offsets;

  private Map<Integer, Text> atomLabels = new Hashtable<Integer, Text>();
  private Text text;

  private Map<Integer, float[]> labelBoxes;

  public BS bsFontSet;
  public BS bsBgColixSet;

  public int defaultOffset;
  public int defaultAlignment;
  public int defaultZPos;
  public byte defaultFontId;
  public short defaultColix;
  public short defaultBgcolix;
  public byte defaultPaletteID;
  public int defaultPointer;
  public byte zeroFontId;

  private boolean defaultsOnlyForNone = true;
  private boolean setDefaults = false;
  
  //labels

  @Override
  public void initShape() {
    super.initShape();
    defaultFontId = zeroFontId = vwr.gdata.getFont3DFSS(JC.DEFAULT_FONTFACE,
        JC.DEFAULT_FONTSTYLE, JC.LABEL_DEFAULT_FONTSIZE).fid;
    defaultColix = 0; //"none" -- inherit from atom
    defaultBgcolix = 0; //"none" -- off
    defaultOffset = JC.LABEL_DEFAULT_OFFSET;
    defaultZPos = 0;
    translucentAllowed = false;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void setProperty(String propertyName, Object value, BS bsSelected) {
    isActive = true;

    //System.out.println(propertyName + " Labels " + value);

    if ("setDefaults" == propertyName) {
      setDefaults = ((Boolean) value).booleanValue();
      return;
    }

    if ("color" == propertyName) {
      byte pid = PAL.pidOf(value);
      short colix = C.getColixO(value);
      if (!setDefaults)
        for (int i = bsSelected.nextSetBit(0); i >= 0 && i < ac; i = bsSelected
            .nextSetBit(i + 1))
          setLabelColix(i, colix, pid);
      if (setDefaults || !defaultsOnlyForNone) {
        defaultColix = colix;
        defaultPaletteID = pid;
      }
      return;
    }

    if ("scalereference" == propertyName) {
      if (strings == null)
        return;
      float val = ((Float) value).floatValue();
      float scalePixelsPerMicron = (val == 0 ? 0 : 10000f / val);
      for (int i = bsSelected.nextSetBit(0); i >= 0 && i < ac; i = bsSelected
          .nextSetBit(i + 1)) {
        if (strings.length <= i)
          continue;
        text = getLabel(i);
        if (text == null) {
          text = Text.newLabel(vwr, null, strings[i], C.INHERIT_ALL, (short) 0,
              0, scalePixelsPerMicron, null);
          putLabel(i, text);
        } else {
          text.setScalePixelsPerMicron(scalePixelsPerMicron);
        }
      }
      return;
    }

    if ("label" == propertyName) {
      setScaling();
      LabelToken[][] tokens = null;
      if (value instanceof Lst) {
        Lst<SV> list = (Lst<SV>) value;
        int n = list.size();
        tokens = new LabelToken[][] { null };
        for (int pt = 0, i = bsSelected.nextSetBit(0); i >= 0 && i < ac; i = bsSelected
            .nextSetBit(i + 1)) {
          if (pt >= n) {
            setLabel(nullToken, "", i);
            return;
          }
          tokens[0] = null;
          setLabel(tokens, SV.sValue(list.get(pt++)), i);
        }
      } else {
        String strLabel = (String) value;
        tokens = (strLabel == null || strLabel.length() == 0 ? nullToken
            : new LabelToken[][] { null });
        for (int i = bsSelected.nextSetBit(0); i >= 0 && i < ac; i = bsSelected
            .nextSetBit(i + 1))
          setLabel(tokens, strLabel, i);
      }
      return;
    }

    if ("labels" == propertyName) {
      setScaling();
      Lst<String> labels = (Lst<String>) value;
      for (int i = bsSelected.nextSetBit(0), pt = 0; i >= 0 && i < ac; i = bsSelected
          .nextSetBit(i + 1)) {
        String strLabel = labels.get(pt++);
        LabelToken[][] tokens = (strLabel == null || strLabel.length() == 0 ? nullToken
            : new LabelToken[][] { null });
        setLabel(tokens, strLabel, i);
      }
      return;
    }

    if ("clearBoxes" == propertyName) {
      labelBoxes = null;
      return;
    }

    if ("translucency" == propertyName || "bgtranslucency" == propertyName) {
      // no translucency
      return;
    }

    if ("bgcolor" == propertyName) {
      isActive = true;
      if (bsBgColixSet == null)
        bsBgColixSet = new BS();
      short bgcolix = C.getColixO(value);
      if (!setDefaults)
        for (int i = bsSelected.nextSetBit(0); i >= 0 && i < ac; i = bsSelected
            .nextSetBit(i + 1))
          setBgcolix(i, bgcolix);
      if (setDefaults || !defaultsOnlyForNone)
        defaultBgcolix = bgcolix;
      return;
    }

    // the rest require bsFontSet setting

    if (bsFontSet == null)
      bsFontSet = new BS();

    if ("textLabels" == propertyName) {
      setScaling();
      Map<Integer, Text> labels = (Map<Integer, Text>) value;
      for (int i = bsSelected.nextSetBit(0); i >= 0 && i < ac; i = bsSelected
          .nextSetBit(i + 1))
        setTextLabel(i, labels.get(Integer.valueOf(i)));
      return;
    }

    if ("fontsize" == propertyName) {
      int fontsize = ((Integer) value).intValue();
      if (fontsize < 0) {
        fids = null;
        return;
      }
      byte fid = vwr.gdata.getFontFid(fontsize);
      if (!setDefaults)
        for (int i = bsSelected.nextSetBit(0); i >= 0 && i < ac; i = bsSelected
            .nextSetBit(i + 1))
          setFont(i, fid);
      if (setDefaults || !defaultsOnlyForNone)
        defaultFontId = fid;
      return;
    }

    if ("font" == propertyName) {
      byte fid = ((Font) value).fid;
      if (!setDefaults)
        for (int i = bsSelected.nextSetBit(0); i >= 0 && i < ac; i = bsSelected
            .nextSetBit(i + 1))
          setFont(i, fid);
      if (setDefaults || !defaultsOnlyForNone)
        defaultFontId = fid;
      return;
    }

    if ("offset" == propertyName) {
      if (!(value instanceof Integer)) {
        if (!setDefaults)
          for (int i = bsSelected.nextSetBit(0); i >= 0 && i < ac; i = bsSelected
              .nextSetBit(i + 1))
            setPymolOffset(i, (float[]) value);
        return;
      }

      int offset = ((Integer) value).intValue();
      if (!setDefaults)
        for (int i = bsSelected.nextSetBit(0); i >= 0 && i < ac; i = bsSelected
            .nextSetBit(i + 1))
          setOffsets(i, offset);
      if (setDefaults || !defaultsOnlyForNone)
        defaultOffset = offset;
      return;
    }

    if ("align" == propertyName) {
      String type = (String) value;
      int hAlignment = JC.TEXT_ALIGN_LEFT;
      if (type.equalsIgnoreCase("right"))
        hAlignment = JC.TEXT_ALIGN_RIGHT;
      else if (type.equalsIgnoreCase("center"))
        hAlignment = JC.TEXT_ALIGN_CENTER;
      for (int i = bsSelected.nextSetBit(0); i >= 0 && i < ac; i = bsSelected
          .nextSetBit(i + 1))
        setHorizAlignment(i, hAlignment);
      if (setDefaults || !defaultsOnlyForNone)
        defaultAlignment = hAlignment;
      return;
    }

    if ("pointer" == propertyName) {
      int pointer = ((Integer) value).intValue();
      if (!setDefaults)
        for (int i = bsSelected.nextSetBit(0); i >= 0 && i < ac; i = bsSelected
            .nextSetBit(i + 1))
          setPointer(i, pointer);
      if (setDefaults || !defaultsOnlyForNone)
        defaultPointer = pointer;
      return;
    }

    if ("front" == propertyName) {
      boolean TF = ((Boolean) value).booleanValue();
      if (!setDefaults)
        for (int i = bsSelected.nextSetBit(0); i >= 0 && i < ac; i = bsSelected
            .nextSetBit(i + 1))
          setZPos(i, JC.LABEL_ZPOS_FRONT, TF);
      if (setDefaults || !defaultsOnlyForNone)
        defaultZPos = (TF ? JC.LABEL_ZPOS_FRONT : 0);
      return;
    }

    if ("group" == propertyName) {
      boolean TF = ((Boolean) value).booleanValue();
      if (!setDefaults)
        for (int i = bsSelected.nextSetBit(0); i >= 0 && i < ac; i = bsSelected
            .nextSetBit(i + 1))
          setZPos(i, JC.LABEL_ZPOS_GROUP, TF);
      if (setDefaults || !defaultsOnlyForNone)
        defaultZPos = (TF ? JC.LABEL_ZPOS_GROUP : 0);
      return;
    }

    if ("display" == propertyName || "toggleLabel" == propertyName) {
      // toggle
      int mode = ("toggleLabel" == propertyName ? 0 : ((Boolean) value)
          .booleanValue() ? 1 : -1);
      if (mads == null)
        mads = new short[ac];
      String strLabelPDB = null;
      LabelToken[] tokensPDB = null;
      String strLabelUNK = null;
      LabelToken[] tokensUNK = null;
      String strLabel;
      LabelToken[] tokens;
      for (int i = bsSelected.nextSetBit(0); i >= 0 && i < ac; i = bsSelected
          .nextSetBit(i + 1)) {
        Atom atom = atoms[i];
        if (formats == null || i >= formats.length)
          formats = AU.ensureLengthS(formats, i + 1);
        if (strings != null && strings.length > i && strings[i] != null) {
          mads[i] = (short) (mode == 0 && mads[i] < 0 || mode == 1 ? 1 : -1);
        } else {
          if (bsSizeSet == null)
            bsSizeSet = new BS();
          strings = AU.ensureLengthS(strings, i + 1);
          if (atom.getGroup3(false).equals("UNK")) {
            if (strLabelUNK == null) {
              strLabelUNK = vwr.getStandardLabelFormat(1);
              tokensUNK = LabelToken.compile(vwr, strLabelUNK, '\0', null);
            }
            strLabel = strLabelUNK;
            tokens = tokensUNK;
          } else {
            if (strLabelPDB == null) {
              strLabelPDB = vwr.getStandardLabelFormat(2);
              tokensPDB = LabelToken.compile(vwr, strLabelPDB, '\0', null);
            }
            strLabel = strLabelPDB;
            tokens = tokensPDB;
          }
          strings[i] = LabelToken.formatLabelAtomArray(vwr, atom, tokens, '\0',
              null, ptTemp);
          formats[i] = strLabel;
          bsSizeSet.set(i);
          if ((bsBgColixSet == null || !bsBgColixSet.get(i))
              && defaultBgcolix != 0)
            setBgcolix(i, defaultBgcolix);
          mads[i] = (short) (mode >= 0 ? 1 : -1);
        }
        setShapeVisibility(atom, strings != null && i < strings.length
            && strings[i] != null && mads[i] >= 0);
        //        } else if (strings != null && atomIndex < strings.length) {
        //        strings[atomIndex] = null;          
      }
      return;
    }

    if (propertyName.startsWith("label:")) {
      setScaling();
      setLabel(new LabelToken[1][], propertyName.substring(6),
          ((Integer) value).intValue());
      return;
    }

    if (propertyName == "deleteModelAtoms") {
      labelBoxes = null;
      int firstAtomDeleted = ((int[]) ((Object[]) value)[2])[1];
      int nAtomsDeleted = ((int[]) ((Object[]) value)[2])[2];
      fids = (byte[]) AU.deleteElements(fids, firstAtomDeleted, nAtomsDeleted);
      bgcolixes = (short[]) AU.deleteElements(bgcolixes, firstAtomDeleted,
          nAtomsDeleted);
      offsets = (int[]) AU.deleteElements(offsets, firstAtomDeleted,
          nAtomsDeleted);
      formats = (String[]) AU.deleteElements(formats, firstAtomDeleted,
          nAtomsDeleted);
      strings = (String[]) AU.deleteElements(strings, firstAtomDeleted,
          nAtomsDeleted);
      BSUtil.deleteBits(bsFontSet, bsSelected);
      BSUtil.deleteBits(bsBgColixSet, bsSelected);
      // pass to super
    }

    setPropAS(propertyName, value, bsSelected);

  }

  private void setPymolOffset(int i, float[] value) {
    Text text = getLabel(i);
    if (text == null) {
      if (strings == null || strings.length <= i || strings[i] == null)
        return;
      byte fid = (bsFontSet != null && bsFontSet.get(i) ? fids[i] : -1);
      if (fid < 0)
        setFont(i, fid = defaultFontId);
      Font font = Font.getFont3D(fid);
      short colix = getColix2(i, atoms[i], false);
      text = Text.newLabel(vwr, font, strings[i], colix, getColix2(i, atoms[i], true), 
          0, scalePixelsPerMicron, value);
      setTextLabel(i, text);
    } else {
      text.pymolOffset = value;
    }
  }

  private final static LabelToken[][] nullToken = new LabelToken[][] { null };
  private boolean isScaled;
  private float scalePixelsPerMicron;
  private P3 ptTemp = new P3();
  
  private void setScaling() {
    isActive = true;
    if (bsSizeSet == null)
      bsSizeSet = new BS();
    isScaled = vwr.getBoolean(T.fontscaling);
    scalePixelsPerMicron = (isScaled ? vwr
        .getScalePixelsPerAngstrom(false) * 10000f : 0);
  }
  
  private void setTextLabel(int i, Text t) {
    if (t == null)
      return;
    String label = t.getText();
    Atom atom = atoms[i];
    addString(atom, i, label, label);
    setShapeVisibility(atom, true);
    if (t.colix >= 0)
      setLabelColix(i, t.colix, PAL.UNKNOWN.id);
    setFont(i, t.font.fid);
    putLabel(i, t);
  }

  private void setLabel(LabelToken[][] temp, String strLabel, int i) {
    Atom atom = atoms[i];
    LabelToken[] tokens = temp[0];
    if (tokens == null)
      tokens = temp[0] = LabelToken.compile(vwr, strLabel, '\0', null);
    String label = (tokens == null ? null : LabelToken.formatLabelAtomArray(
        vwr, atom, tokens, '\0', null, ptTemp ));
    addString(atom, i, label, strLabel);
    text = getLabel(i);
    if (isScaled) {
      text = Text.newLabel(vwr, null, label, C.INHERIT_ALL, (short) 0, 0, scalePixelsPerMicron, null);
      putLabel(i, text);
    } else if (text != null && label != null) {
      text.setText(label);
    }
    if (defaultOffset != JC.LABEL_DEFAULT_OFFSET)
      setOffsets(i, defaultOffset);
    if (defaultAlignment != JC.TEXT_ALIGN_LEFT)
      setHorizAlignment(i, defaultAlignment);
    if ((defaultZPos & JC.LABEL_ZPOS_FRONT) != 0)
      setZPos(i, JC.LABEL_ZPOS_FRONT, true);
    else if ((defaultZPos & JC.LABEL_ZPOS_GROUP) != 0)
      setZPos(i, JC.LABEL_ZPOS_GROUP, true);
    if (defaultPointer != JC.LABEL_POINTER_NONE)
      setPointer(i, defaultPointer);
    if (defaultColix != 0 || defaultPaletteID != 0)
      setLabelColix(i, defaultColix, defaultPaletteID);
    if (defaultBgcolix != 0)
      setBgcolix(i, defaultBgcolix);
    if (defaultFontId != zeroFontId)
      setFont(i, defaultFontId);
  }

  private void addString(Atom atom, int i, String label, String strLabel) {
    setShapeVisibility(atom, label != null);
    if (strings == null || i >= strings.length)
      strings = AU.ensureLengthS(strings, i + 1);
    if (formats == null || i >= formats.length)
      formats = AU.ensureLengthS(formats, i + 1);
    strings[i] = label;
    formats[i] = (strLabel != null && strLabel.indexOf("%{") >= 0 ? label
        : strLabel);
    bsSizeSet.setBitTo(i, (strLabel != null));
  }

  @Override
  public Object getProperty(String property, int index) {
    if (property.equals("offsets"))
      return offsets;
    if (property.equals("label"))
      return (strings != null && index < strings.length && strings[index] != null 
          ? strings[index] : "");
    return null;
  }

  public void putLabel(int i, Text text) {
    if (text == null)
      atomLabels.remove(Integer.valueOf(i));
    else
      atomLabels.put(Integer.valueOf(i), text);
  }

  public Text getLabel(int i) {
    return atomLabels.get(Integer.valueOf(i));
  }

  public void putBox(int i, float[] boxXY) {
    if (labelBoxes == null)
      labelBoxes = new Hashtable<Integer, float[]>(); 
    labelBoxes.put(Integer.valueOf(i), boxXY);
  }

  public float[] getBox(int i) {
    if (labelBoxes == null)
      return null;
    return labelBoxes.get(Integer.valueOf(i));
  }
  
  private void setLabelColix(int i, short colix, byte pid) {
    setColixAndPalette(colix, pid, i);
    // text is only created by labelsRenderer
    if (colixes != null && ((text = getLabel(i)) != null))
      text.colix = colixes[i];
  }

  private void setBgcolix(int i, short bgcolix) {
    if (bgcolixes == null || i >= bgcolixes.length) {
      if (bgcolix == 0)
        return;
      bgcolixes = AU.ensureLengthShort(bgcolixes, i + 1);
    }
    bgcolixes[i] = bgcolix;
    bsBgColixSet.setBitTo(i, bgcolix != 0);
    text = getLabel(i);
    if (text != null)
      text.bgcolix = bgcolix;
  }

  private void setOffsets(int i, int offset) {
    
    if (offsets == null || i >= offsets.length) {
      if (offset == 0)
        return;
      offsets = AU.ensureLengthI(offsets, i + 1);
    }
    offsets[i] = (offsets[i] & JC.LABEL_FLAGS) | offset;

    text = getLabel(i);
    if (text != null)
      text.setOffset(offset);
  }

  private void setHorizAlignment(int i, int hAlign) {
    if (offsets == null || i >= offsets.length) {
      if (hAlign == JC.TEXT_ALIGN_LEFT)
        return;
      offsets = AU.ensureLengthI(offsets, i + 1);
    }
    offsets[i] = JC.setHorizAlignment(offsets[i], hAlign);
    text = getLabel(i);
    if (text != null)
      text.setAlignment(hAlign);
  }

  private void setPointer(int i, int pointer) {
    if (offsets == null || i >= offsets.length) {
      if (pointer == JC.LABEL_POINTER_NONE)
        return;
      offsets = AU.ensureLengthI(offsets, i + 1);
    }
    offsets[i] = JC.setPointer(offsets[i], pointer);
    text = getLabel(i);
    if (text != null)
      text.pointer = pointer;
  }

  private void setZPos(int i, int flag, boolean TF) {
    if (offsets == null || i >= offsets.length) {
      if (!TF)
        return;
      offsets = AU.ensureLengthI(offsets, i + 1);
    }
    offsets[i] = JC.setZPosition(offsets[i], TF ? flag : 0);
  }

  private void setFont(int i, byte fid) {
    if (fids == null || i >= fids.length) {
      if (fid == zeroFontId)
        return;
      fids = AU.ensureLengthByte(fids, i + 1);
    }
    fids[i] = fid;
    bsFontSet.set(i);
    text = getLabel(i);
    if (text != null) {
      text.setFontFromFid(fid);
    }
  }

  @Override
  public void setAtomClickability() {
    if (strings == null)
      return;
    for (int i = strings.length; --i >= 0;) {
      String label = strings[i];
      if (label != null && ms.at.length > i
          && !ms.isAtomHidden(i))
        ms.at[i].setClickable(vf);
    }
  }

  @Override
  public String getShapeState() {
    if (!isActive || bsSizeSet == null)
      return "";
    return vwr.getShapeState(this);
  }

  private int pickedAtom = -1;
  private int pickedOffset = 0;
  private int pickedX;
  private int pickedY;
  
  @Override
  public synchronized boolean checkObjectDragged(int prevX, int prevY, int x,
                                                 int y, int dragAction,
                                                 BS bsVisible) {

    if (vwr.getPickingMode() != ActionManager.PICKING_LABEL
        || labelBoxes == null)
      return false;
    // mouse down ?
    if (prevX == Integer.MIN_VALUE) {
      int iAtom = findNearestLabel(x, y);
      if (iAtom >= 0) {
        pickedAtom = iAtom;
        vwr.acm.setDragAtomIndex(iAtom);
        pickedX = x;
        pickedY = y;
        pickedOffset = (offsets == null || pickedAtom >= offsets.length ? 
            JC.LABEL_DEFAULT_OFFSET : offsets[pickedAtom]);

        System.out.println("LABEL OFFSET=" + pickedOffset);

        return true;
      }
      return false;
    }
    // mouse up ?
    if (prevX == Integer.MAX_VALUE) {
      pickedAtom = -1;
      return false;
    }
    if (pickedAtom < 0)
      return false;
    move2D(pickedAtom, x, y);
    return true;
  }
                         
  private int findNearestLabel(int x, int y) {
    if (labelBoxes == null)
      return -1;
    float dmin = Float.MAX_VALUE;
    int imin = -1;
    float zmin = Float.MAX_VALUE;
    for (Map.Entry<Integer, float[]> entry : labelBoxes.entrySet()) {
      if (!atoms[entry.getKey().intValue()].isVisible(vf | Atom.ATOM_INFRAME_NOTHIDDEN))
        continue;
      float[] boxXY = entry.getValue();
      float dx = x - boxXY[0];
      float dy = y - boxXY[1];
      if (dx <= 0 || dy <= 0 || dx >= boxXY[2] || dy >= boxXY[3] || boxXY[4] > zmin)
        continue;
      zmin = boxXY[4];
      float d = Math.min(Math.abs(dx - boxXY[2]/2), Math.abs(dy - boxXY[3]/2));
      if (d <= dmin) {
        dmin = d;
        imin = entry.getKey().intValue();
      }
    }
    return imin;
  }

  private void move2D(int pickedAtom, int x, int y) {
    int xOffset = JC.getXOffset(pickedOffset);
    int yOffset = JC.getYOffset(pickedOffset);        
    xOffset += x - pickedX;
    yOffset -= y - pickedY;
    int offset = JC.getOffset(xOffset, yOffset, true);
    setOffsets(pickedAtom, offset);
  }

  public short getColix2(int i, Atom atom, boolean isBg) {
    short colix;
    if (isBg) {
      colix = (bgcolixes == null || i >= bgcolixes.length) ? 0 : bgcolixes[i];
    } else {
      colix = (colixes == null || i >= colixes.length) ? 0 : colixes[i];
      colix = C.getColixInherited(colix, atom.colixAtom);
      if (C.isColixTranslucent(colix))
        colix = C.getColixTranslucent3(colix, false, 0);
    }
    return colix;
  }
  
}
