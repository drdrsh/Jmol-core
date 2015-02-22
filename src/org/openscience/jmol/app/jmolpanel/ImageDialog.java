/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2014-03-20 17:22:16 -0500 (Thu, 20 Mar 2014) $
 * $Revision: 19476 $
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
package org.openscience.jmol.app.jmolpanel;


import javajs.util.PT;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.Image;
import java.util.Hashtable;
import java.util.Map;

import org.jmol.viewer.Viewer;

class ImageDialog extends JDialog implements WindowListener, ActionListener {


  private JMenuBar menubar;

  protected Image image;

  protected Viewer vwr;
  protected Canvas canvas;
  private String title;
  private Map<String, ImageDialog> imageMap;
  
  ImageDialog(JmolPanel jmol, Viewer vwr, String title, Map<String, ImageDialog> imageMap) {
    super(jmol.frame, title, false);
    this.vwr = vwr;
    addWindowListener(this);
    this.title = title;
    this.imageMap = imageMap;
    imageMap.put(title, this);
    JPanel wrapper = new JPanel(new BorderLayout());
    canvas = new ImageCanvas();
    wrapper.add(canvas, BorderLayout.CENTER);
    JPanel container = new JPanel();
    container.setLayout(new BorderLayout());
    menubar = new JMenuBar();
    // see app.jmolpanel.jmol.Properties.Jmol-reseources.properties
    menubar.add(createMenu(jmol.guimap, "idfileMenu"));
    setJMenuBar(menubar);
    container.add(wrapper, BorderLayout.CENTER);
    getContentPane().add(container);
    pack();
    setLocation(100, 100);
    setVisible(true);
  }

  private JMenu createMenu(GuiMap guimap, String key) {

    // Get list of items from resource file:
    String[] itemKeys = PT.getTokens(JmolResourceHandler.getStringX(key));
    // Get label associated with this menu:
    JMenu menu = guimap.newJMenu(key);
    ImageIcon f = JmolResourceHandler.getIconX(key + "Image");
    if (f != null) {
      menu.setHorizontalTextPosition(SwingConstants.RIGHT);
      menu.setIcon(f);
    }

    // Loop over the items in this menu:
    for (int i = 0; i < itemKeys.length; i++) {
      String item = itemKeys[i];
      if (item.equals("-")) {
        menu.addSeparator();
      } else if (item.endsWith("Menu")) {
        menu.add(createMenu(guimap, item));
      } else {
        JMenuItem mi = createMenuItem(guimap, item);
        menu.add(mi);
      }
    }
    menu.setVisible(true);
    return menu;
  }

  private JMenuItem createMenuItem(GuiMap guimap, String cmd) {

    JMenuItem mi = guimap.newJMenuItem(cmd);
    ImageIcon f = JmolResourceHandler.getIconX(cmd + "Image");
    if (f != null) {
      mi.setHorizontalTextPosition(SwingConstants.RIGHT);
      mi.setIcon(f);
    }
    mi.setActionCommand(cmd);
    mi.addActionListener(this);
    mi.setVisible(true);
    return mi;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String cmd = e.getActionCommand();
    if (cmd.equals("close")) {
      closeMe();
    } else if (cmd.equals("saveas")) {
      saveAs();
    }
  }

  private void saveAs() {
    (new Thread(new Runnable() {
      @Override
      public void run() {
        Map<String, Object>params = new Hashtable<String, Object>();
        String fname = vwr.dialogAsk("Save Image", "jmol.png", params);
        if (fname == null)
          return;
        String type = "PNG";
        int pt = fname.lastIndexOf(".");
        if (pt > 0)
          type = fname.substring(pt + 1).toUpperCase();
        params.put("fileName", fname);
        params.put("type", type);
        params.put("image", image);
        vwr.showString(vwr.processWriteOrCapture(params), false);        
      }
    }) {
    }).start();
  }

  private void closeMe() {
    imageMap.remove(title);
    dispose();
  }

  public void setImage(Image image) {
    if (image != null) {
      this.image = image;
      int w = image.getWidth(null);
      int h = image.getHeight(null);
      setTitle(title.substring(title.lastIndexOf("/") + 1) + " [" + w + " x " + h + "]");
      Dimension d = new Dimension(w, h);
      canvas.setSize(d);
      //canvas.setBackground(new Color(55,0,0));
      //setPreferredSize(d);
      pack();
    }
    repaint();
  }  
  

  class ImageCanvas extends Canvas {
    @Override
    public void paint(Graphics g) {
      g.drawImage(image, 0, 0, null);
    }
  }
  
  @Override
  public void windowClosed(WindowEvent e) {
  }
  
  @Override
  public void windowOpened(WindowEvent e) {
  }
  @Override
  public void windowClosing(WindowEvent e) {
    closeMe();
  }
  @Override
  public void windowIconified(WindowEvent e) {
  }
  @Override
  public void windowDeiconified(WindowEvent e) {
  }
  @Override
  public void windowActivated(WindowEvent e) {
  }
  @Override
  public void windowDeactivated(WindowEvent e) {
  }

}
