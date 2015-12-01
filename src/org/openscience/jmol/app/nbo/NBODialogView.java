/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2014-12-13 22:43:17 -0600 (Sat, 13 Dec 2014) $
 * $Revision: 20162 $
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
package org.openscience.jmol.app.nbo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;

import javajs.util.PT;
import javajs.util.SB;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.metal.MetalComboBoxUI;

abstract class NBODialogView extends NBODialogRun {

  protected final static String[] basSet = { "AO", "PNAO", "NAO", "PNHO",
      "NHO", "PNBO", "NBO", "PNLMO", "NLMO", "MO" };
  protected int keywordNumber = 6;
  protected JLabel vLab, pLab;
  protected JButton goBtn2;
  protected JButton btnShow;
  protected JButton btnView3D;
  protected JRadioButton btnProf;
  protected boolean oneD = true, inLobes = true;
  protected JComboBox<String> list;
  protected JComboBox<String> basis;
  protected Hashtable<String, String[]> lists;
  protected int viewState;
  protected boolean positiveSign;
  protected Box orbBox, profBox, dispBox;
  protected JPanel selectPanel;

  protected final JTextField[] vectorFields = new JTextField[8];
  {
    String[] vecVal = { "1", "2", "0.5", "-2.0", "2.0", "-1.0", "1.0", "100" };
    for (int i = 0; i < vectorFields.length; i++)
      vectorFields[i] = new JTextField(vecVal[i]);
  }
  protected final String[] vectorFieldIDs = {
      "a", "b", "c", "d", "e", "f", "g", "h" // 0 - 11  
  };
  private String getVectorParams() {
    String s = "";
    for (int i = 0; i < vectorFields.length; i++)
      s += "GLOBAL VECTOR_" + planeFieldIDs[i] + " "
          + (vectorFields[i].getText()) + sep;
    return s;
  }

  protected final JTextField[] planeFields = new JTextField[12];
  {
    String[] plVal = { "1", "2", "3", "0.5", "0.0", "0.0", "0.0", "-3.0",
        "3.0", "-3.0", "3.0", "25" };
    for (int i = 0; i < planeFields.length; i++)
      planeFields[i] = new JTextField(plVal[i]);

  }
  protected final String[] planeFieldIDs = {
      // tf   tf2  tf3
      "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l" // 0 - 11  
  };
  private String getPlaneParams() {
    String s = "";
    for (int i = 0; i < planeFields.length; i++)
      s += "GLOBAL PLANE_" + planeFieldIDs[i] + " "
          + (planeFields[i].getText()) + sep;
    return s;
  }

  protected final JTextField[] camFields = new JTextField[52];
  {
    String[] camVal = { "6.43", "0.0", "0.0", "50.0", "2.0", "2.0", "0.0",
        "0.60", "1.0", "1.0", "40.0", "0.0", "0.60", "1.0", "1.0", "40.0",
        "0.0", "0.60", "1.0", "1.0", "40.0", "0.0", "0.60", "1.0", "1.0",
        "40.0", "0.5", "1.0", "1.0", "1.0", "0.8", "0.0", "0.0", "1.0", "0.8",
        "0.4", "0.0", "1.0", "1.0", "0.5", "0.5", "0.5", "0.0", "0.7", "1.0",
        "0.22", "0.40", "0.10", "0.05", "0.0316", "0.0001", "0.4000" };
    for (int i = 0; i < camFields.length; i++)
      camFields[i] = new JTextField(camVal[i]);
  }

  String[] camFieldIDs = { 
      // 0 - 6
      "1a", "1b", "1c", "1d", "1e", "1f", "1g",
      // 7 - 26
      "2a", "2b", "2c", "2d", "2e", "2f", "2g", "2h", "2i", "2j", "2k", "2l", "2m",
      "2n", "2o", "2p", "2q", "2r", "2s", "2t",
      // 27 - 44
      "3a", "3b", "3c", "3d", "3e", "3f", "3g", "3h", "3i", "3j", "3k", "3l", 
      "3m", "3n", "3o", "3p", "3q", "3r",
      // 45 - 48
      "4a", "4b", "4c", "4d",
      // 49 - 51
      "5a", "5b", "5c"  
    };
  private String getCameraParams() {
    String s = "";
    for (int i = 0; i < camFields.length; i++)
      s += "GLOBAL CAMERA_" + camFieldIDs[i] + " " + camFields[i].getText()
          + sep;
    s += "GLOBAL CAMERA_6 " + (inLobes ? 0 : 1) + sep;
    return s;

  }

  /**
   * TODO this was sli business. not implemented?
   * @return countouring parameters, I think contour levels
   */
  private String getContourParams() {
    String s = "";
    for (int i = 0; i < contourFields.length; i++)
      s += "GLOBAL CONTOUR_" + contourFieldIDs[i] + " " + contourFields[i].getText()
          + sep;
    return s;

  }

  protected final JTextField[] contourFields = new JTextField[7];
  {
    String[] contVal = { "0.03", "0.05", "4", "0.05", "0.05", "0.10", "0.10" };
    for (int i = 0; i < contourFields.length; i++)
      contourFields[i] = new JTextField(contVal[i]);

  }
  protected final String[] contourFieldIDs = { "a", "b", "c", "d", "e", "f",
      "g", "h" // 0 - 7  
  };
  
  protected DefaultListModel<String> model;
  protected JList<String> dList;
  protected int numStor = 0;

  protected final static int VIEW_STATE_MAIN = 0;
  protected final static int VIEW_STATE_PLANE = 1;
  protected final static int VIEW_STATE_VECTOR = 2;
  protected final static int VIEW_STATE_CAMERA = 3;

  protected NBODialogView(JFrame f) {
    super(f);
  }

  protected void buildView(Container p) {
    viewState = VIEW_STATE_MAIN;
    p.removeAll();
    p.setLayout(new BorderLayout());
    p.add(topPanel, BorderLayout.PAGE_START);
    JPanel jp = new JPanel(new BorderLayout());
    jp.add(modelOut(),BorderLayout.CENTER);
    JPanel jp2 = new JPanel(new BorderLayout());
    jp2.add(profileBox(),BorderLayout.CENTER);
    JLabel lab = new JLabel("Settings");
    lab.setOpaque(true);
    lab.setBackground(Color.black);
    lab.setForeground(Color.white);
    lab.setFont(nboFont);
    jp2.add(lab,BorderLayout.NORTH);
    jp.add(jp2,BorderLayout.NORTH);
    JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,select(),jp);
    sp.setDividerLocation(350);

//    if(tfExt!=null&&inputFile!=null)
//      if(tfExt.getText().equals("47")&& !isJmolNBO && newNBOFile(inputFile,"31").exists()){
//        setInputFile(inputFile,"31",showWorkPathDone);
//        orbBox.setVisible(true);
//        basis.setSelectedIndex(5);
//      }
    p.add(sp,BorderLayout.CENTER);
    p.add(statusPanel, BorderLayout.PAGE_END);
    tfExt.setText("31");
    tfExt.setEditable(false);
    if(isJmolNBO)
      basis.setSelectedIndex(5);
  }

  /**
   * select panel
   * @return select panel
   */
  protected JPanel select() {
    selectPanel = new JPanel();
    selectPanel.setLayout(new BoxLayout(selectPanel,BoxLayout.Y_AXIS));
    selectPanel.setBorder(BorderFactory.createLoweredBevelBorder());
    //JOBFILE////////
    Box box = Box.createHorizontalBox();
    JLabel title = new JLabel(" Select Job ");
    title.setAlignmentX(0.0f);
    title.setBackground(titleColor);
    title.setForeground(Color.white);
    title.setFont(titleFont);
    title.setOpaque(true);
    Box box2 = Box.createVerticalBox();
    box2.add(title);
    box.setAlignmentX(0.0f);
    box2.setAlignmentX(0.0f);
    box2.add(box);
    box.add(folderBox());
    box.setBorder(BorderFactory.createLineBorder(Color.black));
    box.setMaximumSize(new Dimension(355,65));
    selectPanel.add(box2);
    //BASIS/////////////
    orbBox = Box.createVerticalBox();
    orbBox.setAlignmentX(0.0f);
    title = new JLabel(" Select Orbital ");
    title.setAlignmentX(0.0f);
    title.setBackground(titleColor);
    title.setForeground(Color.white);
    title.setFont(titleFont);
    title.setOpaque(true);
    orbBox.add(title);
    Box b2 = Box.createVerticalBox();
    b2.setAlignmentX(0.0f);
    orbBox.add(b2);
    Box b = Box.createHorizontalBox();
    final DefaultComboBoxModel<String> listModel = new DefaultComboBoxModel<String>();
    (basis = new JComboBox<String>(basSet)).setMaximumSize(new Dimension(70,25));
    basis.setUI(new StyledComboBoxUI(180,-1));
    b.add(basis);
    basis.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        basisSel(listModel);
      }
    });
    //ORBITAL//////////
    (list = new JComboBox<String>(listModel)).setFont(nboFont);
    list.setMaximumSize(new Dimension(270,25));
    list.setAlignmentX(0.5f);
    list.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int i = list.getSelectedIndex();
        if (i <1){
          nboService.runScriptQueued("nbo delete; mo delete");
          return;
        }
        profBox.setVisible(true);
        showOrbJmol(basis.getSelectedItem().toString(), list.getSelectedIndex());
        if(nboView)
          nboService.runScriptQueued("nbo color yellow [134,254,253]; nbo fill nomesh translucent 0.3");
      }
    });
    list.setUI(new StyledComboBoxUI(125,-1));
    b.add(list);
    b.setAlignmentX(0.0f);
    b2.add(b);
    b = Box.createHorizontalBox();
    b.add(new JLabel("Basis"));
    b.add(Box.createRigidArea(new Dimension(50,0)));
    b.add(new JLabel("Orbital"));
    b.setAlignmentX(0.0f);
    b2.add(b);
    b2.setBorder(BorderFactory.createLineBorder(Color.black));
    selectPanel.add(orbBox);
    orbBox.setMaximumSize(new Dimension(355,65));
    orbBox.setVisible(isJmolNBO);
    int j = iLast;
    iLast = -1;
    if (j > 0 &&j < list.getComponentCount())
      j = 0;
    //PROFILE-CONTOUR////////////
    profBox = Box.createVerticalBox();
    profBox.setAlignmentX(0.0f);
    title = new JLabel(" Select Image Type ");
    title.setAlignmentX(0.0f);
    title.setBackground(titleColor);
    title.setForeground(Color.white);
    title.setFont(titleFont);
    title.setOpaque(true);
    profBox.add(title);
    b = Box.createHorizontalBox();
    b.setAlignmentX(0.0f);
    profBox.add(b);
    b.add(Box.createRigidArea(new Dimension(75,0)));
    JButton btn = new JButton("2D Contour");
    b.add(btn);
    btn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        oneD = false;
        viewState = VIEW_STATE_PLANE;
        goViewClicked();
      }
    });
    btn = new JButton("1D Profile");
    btn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        oneD = true;
        viewState = VIEW_STATE_VECTOR;
        goViewClicked();
      }
    });
    b.add(btn);
    b.add(Box.createRigidArea(new Dimension(75,0)));
    //selectPanel.add(profBox=profileBox());
    //profBox.setMaximumSize(new Dimension(350,105));
    profBox.setVisible(false);
    //profBox.setBorder(BorderFactory.create);
    selectPanel.add(profBox);
    //DISPLAY/////////////
    dispBox();
    return selectPanel;
  }
  
  protected void dispBox(){
    if(dispBox!=null)
      selectPanel.remove(dispBox);
    dispBox = Box.createVerticalBox();
    JLabel title = new JLabel(" Select Image Type ");
    title.setAlignmentX(0.0f);
    title.setBackground(titleColor);
    title.setForeground(Color.white);
    title.setFont(titleFont);
    title.setOpaque(true);
    dispBox.add(title);
    dispBox.setAlignmentX(0.0f);
    Box box = Box.createHorizontalBox();
    box.setAlignmentX(0.0f);
    dispBox.setVisible(numStor>0);
    Box box2 = Box.createVerticalBox();
    (btnShow = new JButton("1D/2D")).setFont(nboFont);
    btnShow.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        //TODO
      }
    });
    box2.add(btnShow);
    (btnView3D = new JButton("3D")).setFont(nboFont);
    btnView3D.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        view3D();
      }
    });
    box2.add(btnView3D);
    JScrollPane sp = new JScrollPane();
    model = new DefaultListModel<String>();
    for(int i = 1; i < 10; i++){
      model.addElement(""+ i + ")");
    }
    (dList = new JList<String>(model)).setFont(nboFont);
    dList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    dList.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        int i = dList.getSelectedIndex();
        System.out.println("-----------------------" + iLastD + " " + i);
        if (i == -1)
          return;
        showOrbJmol(dList);
        if(nboView)
          nboService.runScriptQueued("nbo color yellow [181,229,255]; nbo fill nomesh translucent 0.3");
      }
    });
    sp.getViewport().add(dList);
    dList.setBackground(new Color(245,245,220));
    sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    sp.setPreferredSize(new Dimension(200,280));
    box.add(sp);
    box.setMaximumSize(new Dimension(350,230));
    box.add(box2);
    box.setBorder(BorderFactory.createLineBorder(Color.black));
    dispBox.add(box);
    selectPanel.add(dispBox);
    selectPanel.repaint();
    selectPanel.revalidate();
  }
  
  private Component profileBox(){
    JPanel p = new JPanel(new GridLayout(4,2));
    final JButton btnVec = new JButton("Axis");
    final JButton btnPla = new JButton("Plane");
    vLab = new JLabel("1, 2");
    p.add(btnVec);
    btnVec.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        vector();
      }
    });
    p.add(vLab).setFont(nboFont);
    
    p.add(btnPla);
    btnPla.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        plane();
      }
    });
    p.add(pLab = new JLabel("1, 2, 3")).setFont(nboFont);
    
    JButton btnSign = new JButton("Sign");
    p.add(btnSign);
    final JLabel sLab = new JLabel(" +");
    positiveSign = true;
    btnSign.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (sLab.getText().trim().equals("+")){
          positiveSign = true;
          sLab.setText(" -");
        }else{
          sLab.setText(" +");
          positiveSign = false;
        }
      }
    });
    p.add(sLab).setFont(new Font("Monospaced",Font.BOLD,20));
    JButton btnStip = new JButton("Lines");
    p.add(btnStip);
    btnStip.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        stip();
      }
    });
    JButton btnCam = new JButton("Camera");
    p.add(btnCam);
    btnCam.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cam1();
      }
    });
    return p;
  }

  protected int iLast = -1, iLastD = -1;

  /**
   * Plane dialog
   */
  protected void plane() {
    viewPlanePt = 0;
    final JDialog plane = new JDialog(this, "Definition of Plane:", false);
    plane.setLayout(new BorderLayout());
    plane.setMinimumSize(new Dimension(300, 300));
    centerDialog(plane);
    JPanel labs = new JPanel(new GridLayout(7, 1, 5, 0));
    labs.add(new JLabel("Enter or select three atoms:"));
    labs.add(new JLabel("Enter fraction to locate origin:"));
    labs.add(new JLabel("Enter two rotation angles:"));
    labs.add(new JLabel("Enter shift of plane along normal:"));
    labs.add(new JLabel("Enter min and max X values:"));
    labs.add(new JLabel("Enter min and max Y values:"));
    labs.add(new JLabel("Enter number of steps NX:"));
    plane.add(labs, BorderLayout.WEST);
    JPanel in = new JPanel(new GridLayout(7, 1, 5, 0));
    Box bo = Box.createHorizontalBox();
    bo.add(planeFields[0]);
    bo.add(planeFields[1]);
    bo.add(planeFields[2]);
    in.add(bo);
    in.add(planeFields[3]);
    bo = Box.createHorizontalBox();
    bo.add(planeFields[4]);
    bo.add(planeFields[5]);
    in.add(bo);
    in.add(planeFields[6]);
    bo = Box.createHorizontalBox();
    bo.add(planeFields[7]);
    bo.add(planeFields[8]);
    in.add(bo);
    bo = Box.createHorizontalBox();
    bo.add(planeFields[9]);
    bo.add(planeFields[10]);
    in.add(bo);
    in.add(planeFields[11]);
    plane.add(in, BorderLayout.CENTER);
    JButton b = new JButton("OK");
    plane.add(b, BorderLayout.SOUTH);
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        pLab.setText(planeFields[0].getText() + ", " + planeFields[1].getText()
            + ", " + planeFields[2].getText());
        plane.dispose();
        viewState = VIEW_STATE_MAIN;
      }
    });

    plane.setVisible(true);
  }

  /**
   * Vector dialog
   */
  protected void vector() {
    viewVectorPt = 0;
    final JDialog vect = new JDialog(this, "Definition of Vector:", false);
    vect.setLayout(new BorderLayout());
    vect.setMinimumSize(new Dimension(300, 250));
    centerDialog(vect);
    JPanel labs = new JPanel(new GridLayout(5, 1, 5, 0));
    labs.add(new JLabel("Enter or select two atom numbers:"));
    labs.add(new JLabel("Enter fraction to locate origin:"));
    labs.add(new JLabel("Enter min and max X values:"));
    labs.add(new JLabel("Enter min and max function values:"));
    labs.add(new JLabel("Enter number of steps NX:"));
    vect.add(labs, BorderLayout.WEST);
    JPanel in = new JPanel(new GridLayout(5, 1, 5, 0));
    Box bo = Box.createHorizontalBox();
    bo.add(vectorFields[0]);
    bo.add(vectorFields[1]);
    in.add(bo);
    in.add(vectorFields[2]);
    bo = Box.createHorizontalBox();
    bo.add(vectorFields[3]);
    bo.add(vectorFields[4]);
    in.add(bo);
    bo = Box.createHorizontalBox();
    bo.add(vectorFields[5]);
    bo.add(vectorFields[6]);
    in.add(bo);
    in.add(vectorFields[7]);
    vect.add(in, BorderLayout.CENTER);

    JButton b = new JButton("OK");
    vect.add(b, BorderLayout.SOUTH);
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        vLab.setText(vectorFields[0].getText() + ", " + vectorFields[1].getText());
        vect.dispose();
        viewState = VIEW_STATE_MAIN;
      }
    });
    vect.setVisible(true);
  }

  /**
   * Camera Dialogues, could be done terribly right now I'm not sure
   */
  protected void cam1() {
    viewState = VIEW_STATE_CAMERA;
    final JDialog cam1 = new JDialog(this, "Camera and Light-Source:", false);
    cam1.setLayout(new BorderLayout());
    cam1.setMinimumSize(new Dimension(350, 200));
    cam1.setVisible(true);
    centerDialog(cam1);
    JPanel labs = new JPanel(new GridLayout(4, 1, 5, 0));
    labs.add(new JLabel("Camera distance from screen center:"));
    labs.add(new JLabel("Two rotation angles (about X, Y):"));
    labs.add(new JLabel("Camera view angle:"));
    labs.add(new JLabel("Lighting (RL, UD, BF w.r.t. camera):"));
    cam1.add(labs, BorderLayout.WEST);
    JPanel in = new JPanel(new GridLayout(4, 1, 5, 0));
    Box bo = Box.createHorizontalBox();
    in.add(camFields[0]);
    bo.add(camFields[1]);
    bo.add(camFields[2]);
    in.add(camFields[3]);
    in.add(bo);
    bo = Box.createHorizontalBox();
    bo.add(camFields[4]);
    bo.add(camFields[5]);
    bo.add(camFields[6]);
    in.add(bo);
    cam1.add(in, BorderLayout.CENTER);
    JButton b = new JButton("OK");
    cam1.add(b, BorderLayout.SOUTH);
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cam1.dispose();
        cam2();
      }
    });
  }

  protected void cam2() {
    final JDialog cam2 = new JDialog(this, "Surface Optical Parameters:", false);
    cam2.setLayout(new BorderLayout());
    cam2.setMinimumSize(new Dimension(350, 200));
    cam2.setVisible(true);
    centerDialog(cam2);
    cam2.setResizable(false);
    JPanel labs = new JPanel(new GridLayout(4, 1, 5, 0));
    labs.add(new JLabel("atoms:"));
    labs.add(new JLabel("bonds:"));
    labs.add(new JLabel("H-bonds:"));
    labs.add(new JLabel("orbitals:"));
    cam2.add(labs, BorderLayout.WEST);
    JPanel in = new JPanel(new GridLayout(4, 1, 5, 0));
    Box bo = Box.createHorizontalBox();
    bo.add(camFields[7]);
    bo.add(camFields[8]);
    bo.add(camFields[9]);
    bo.add(camFields[10]);
    bo.add(camFields[11]);
    in.add(bo);
    bo = Box.createHorizontalBox();
    bo.add(camFields[12]);
    bo.add(camFields[13]);
    bo.add(camFields[14]);
    bo.add(camFields[15]);
    bo.add(camFields[16]);
    in.add(bo);
    bo = Box.createHorizontalBox();
    bo.add(camFields[17]);
    bo.add(camFields[18]);
    bo.add(camFields[19]);
    bo.add(camFields[20]);
    bo.add(camFields[21]);
    in.add(bo);
    bo = Box.createHorizontalBox();
    bo.add(camFields[22]);
    bo.add(camFields[23]);
    bo.add(camFields[24]);
    bo.add(camFields[25]);
    bo.add(camFields[26]);
    in.add(bo);
    cam2.add(in, BorderLayout.CENTER);
    cam2.add(
        new JLabel(
            "                    amb              diff            spec          pow          transp"),
        BorderLayout.NORTH);
    JButton b = new JButton("OK");
    cam2.add(b, BorderLayout.SOUTH);
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cam2.dispose();
        cam3();
      }
    });
  }

  protected void cam3() {
    final JDialog cam3 = new JDialog(this,
        "Color (Blue/Green/Red) Parameters:", false);
    cam3.setLayout(new BorderLayout());
    cam3.setMinimumSize(new Dimension(350, 200));
    cam3.setVisible(true);
    centerDialog(cam3);
    cam3.setResizable(false);
    JPanel labs = new JPanel(new GridLayout(6, 1, 5, 0));
    labs.add(new JLabel("light source color:"));
    labs.add(new JLabel("background color:"));
    labs.add(new JLabel("orbital (+ phase) color:"));
    labs.add(new JLabel("orbital (- phase) color:"));
    labs.add(new JLabel("bond color"));
    labs.add(new JLabel("H-Bond color"));
    cam3.add(labs, BorderLayout.WEST);
    JPanel in = new JPanel(new GridLayout(6, 1, 5, 0));

    Box bo = Box.createHorizontalBox();
    bo.add(camFields[27]);
    bo.add(camFields[28]);
    bo.add(camFields[29]);
    in.add(bo);

    bo = Box.createHorizontalBox();
    bo.add(camFields[30]);
    bo.add(camFields[31]);
    bo.add(camFields[32]);
    in.add(bo);

    bo = Box.createHorizontalBox();
    bo.add(camFields[33]);
    bo.add(camFields[34]);
    bo.add(camFields[35]);
    in.add(bo);

    bo = Box.createHorizontalBox();
    bo.add(camFields[36]);
    bo.add(camFields[37]);
    bo.add(camFields[38]);
    in.add(bo);

    bo = Box.createHorizontalBox();
    bo.add(camFields[39]);
    bo.add(camFields[40]);
    bo.add(camFields[41]);
    in.add(bo);

    bo = Box.createHorizontalBox();
    bo.add(camFields[42]);
    bo.add(camFields[43]);
    bo.add(camFields[44]);
    in.add(bo);

    cam3.add(in, BorderLayout.CENTER);
    cam3.add(
        new JLabel(
            "                                                 Blue               Green             Red"),
        BorderLayout.NORTH);
    JButton b = new JButton("OK");
    cam3.add(b, BorderLayout.SOUTH);
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cam3.dispose();
        cam4();
      }
    });
  }

  protected void cam4() {
    final JDialog cam4 = new JDialog(this, "Atomic and Bond Radii:", false);
    cam4.setLayout(new BorderLayout());
    cam4.setMinimumSize(new Dimension(250, 200));
    cam4.setVisible(true);
    centerDialog(cam4);
    JPanel labs = new JPanel(new GridLayout(4, 1, 5, 0));
    labs.add(new JLabel("Atomic radius for H:"));
    labs.add(new JLabel("Atomic radius for C:"));
    labs.add(new JLabel("Bond radius:"));
    labs.add(new JLabel("H-bond radius:"));
    cam4.add(labs, BorderLayout.WEST);
    JPanel in = new JPanel(new GridLayout(4, 1, 5, 0));
    in.add(camFields[45]);
    in.add(camFields[46]);
    in.add(camFields[47]);
    in.add(camFields[48]);
    cam4.add(in, BorderLayout.CENTER);
    JButton b = new JButton("OK");
    cam4.add(b, BorderLayout.SOUTH);
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cam4.dispose();
        cam5();
      }
    });
  }

  protected void cam5() {
    final JDialog cam5 = new JDialog(this, "Contour Parameters:", false);
    cam5.setLayout(new BorderLayout());
    cam5.setMinimumSize(new Dimension(250, 150));
    cam5.setVisible(true);
    centerDialog(cam5);
    JPanel labs = new JPanel(new GridLayout(3, 1, 5, 0));
    labs.add(new JLabel("Contour value:"));
    labs.add(new JLabel("Contour tolerance:"));
    labs.add(new JLabel("Stepsize:"));
    cam5.add(labs, BorderLayout.WEST);
    JPanel in = new JPanel(new GridLayout(3, 1, 5, 0));
    in.add(camFields[49]);
    in.add(camFields[50]);
    in.add(camFields[51]);
    cam5.add(in, BorderLayout.CENTER);
    JButton b = new JButton("OK");
    cam5.add(b, BorderLayout.SOUTH);
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cam5.dispose();
        cam6();
      }
    });
  }

  protected void cam6() {
    final JDialog cam6 = new JDialog(this, "Inner Lobes:", false);
    cam6.setLayout(new BorderLayout());
    cam6.setMinimumSize(new Dimension(320, 100));
    cam6.setVisible(true);
    centerDialog(cam6);
    Box bo = Box.createHorizontalBox();
    bo.add(new JLabel("  Inner lobes of orbitals included? "));
    //JPanel in = new JPanel(new GridLayout(1, 1));
    JButton b = new JButton("YES");
    bo.add(b);
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        inLobes = true;
        cam6.dispose();
        viewState = VIEW_STATE_MAIN;
      }
    });
    b = new JButton("NO");
    bo.add(b);
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        inLobes = false;
        cam6.dispose();
        viewState = VIEW_STATE_MAIN;
      }
    });
    cam6.add(bo, BorderLayout.CENTER);
  }

  protected void stip() {
    final JDialog stip = new JDialog(this, "Specification of contour lines:",
        false);
    stip.setLayout(new BorderLayout());
    stip.setMinimumSize(new Dimension(300, 300));
    stip.setVisible(true);
    centerDialog(stip);
    JPanel labs = new JPanel(new GridLayout(8, 1, 0, 0));
    labs.add(new JLabel("Enter first contour line:"));
    labs.add(new JLabel("Enter contour step size:"));
    labs.add(new JLabel("Enter number of contours:"));
    labs.add(new JLabel("Enter length of dash (cm):"));
    labs.add(new JLabel("Enter length of space (cm):"));
    labs.add(new JLabel("  Specification of orbital diagram lines"));
    labs.add(new JLabel("Enter length of dash (cm):"));
    labs.add(new JLabel("Enter length of space (cm):"));
    stip.add(labs, BorderLayout.WEST);
    JPanel in = new JPanel(new GridLayout(8, 1, 0, 0));
    in.add(contourFields[0]);
    in.add(contourFields[1]);
    in.add(contourFields[2]);
    in.add(contourFields[3]);
    in.add(contourFields[4]);
    in.add(Box.createRigidArea(new Dimension(0, 0)));
    in.add(contourFields[5]);
    in.add(contourFields[6]);
    stip.add(in, BorderLayout.CENTER);
    JButton b = new JButton("OK");
    stip.add(b, BorderLayout.SOUTH);
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // this cannot be right.
        stip.dispose();
      }
    });
  }

  protected void showOrbJmol(JList<String> list) {
    String item = list.getSelectedValue();
    if(item.split(" ").length<2)
      return;
    int pt = item.indexOf("-");
    String type = item.split(" ")[1];
    if(type.equals("NAO")) 
      type = "PNAO";
    nboService.runScriptQueued("MO delete; NBO delete; NBO TYPE " + type
        + "; NBO " + item.substring(pt+1, item.indexOf("]")));
  }
  
  protected void showOrbJmol(String type, int i){
    System.out.println("----"+type);
    if(type.trim().equals("NAO"))
      type = "PNAO";
    System.out.println("----"+type);
    nboService.runScriptQueued("MO delete; NBO delete; NBO TYPE " + type
        + "; NBO " + i);
    if(nboView)
      nboService.runScriptQueued("nbo color yellow [134,254,253]; nbo fill nomesh translucent 0.3");
  }

  synchronized protected void basisSel(final DefaultComboBoxModel<String> listModel) {
    keywordNumber = basis.getSelectedIndex() + 1;
    reqInfo = "";
    String keyword = basis.getSelectedItem().toString();
    final SB sb = new SB();
    if(lists.get("o "+keyword)!=null){
      setOrbitalList(lists.get("o "+keyword), listModel);
      return;
    }
    if (keyword.equals("MO")) {
      for (int i = 1; i <= listModel.getSize(); i++) {
        sb.append((i + ".  MO " + i + "                 ").substring(0, 20));
      }
      nboService.queueJob("view", "getting orbital list", new Runnable() {
        @Override
        public void run() {
          processOrbitalList(sb.toString(),listModel);
        }
      });
    } else {
      appendToFile("GLOBAL C_PATH " + inputFile.getParent() + sep, sb);
      appendToFile("GLOBAL C_JOBSTEM " + jobStem + sep, sb);
      appendToFile("GLOBAL I_BAS_1 " + keywordNumber + sep, sb);
      appendToFile("CMD LABEL", sb);
      nboService.queueJob("view", "getting orbital list", new Runnable() {
        @Override
        public void run() {
          nboService.rawCmdNew("v", sb, true, NBOService.MODE_VIEW_LIST);
          processOrbitalList(reqInfo, listModel);
        }
      });
    }
  }
  
  @Override
  protected void nboResetV() {
    resetRunFile();
    dList = null;
    for(int i = 1; i<=numStor;i++){
      String dir = new File(workingPath).getParent();
      File f = new File(dir + "/" + jobStem + i + ".bmp");
      File f2 = new File(dir + "/" + jobStem + i + ".ps");
      System.out.println("----"+f2.toString());
      f.delete();
      f2.delete();
    }
    lists = new Hashtable<String, String[]>();
    numStor = 0;
    iLast = iLastD = -1;
  }

  /////////////////////// RAW NBOSERVE API ////////////////////

  protected void goViewClicked() {
    final SB sb = new SB();
    final int index = list.getSelectedIndex()+1;
    appendToFile("GLOBAL C_PATH " + inputFile.getParent() + sep, sb);
    appendToFile("GLOBAL C_JOBSTEM " + jobStem + sep, sb);
    appendToFile("GLOBAL I_BAS_1 " + (basis.getSelectedIndex() + 1) + sep, sb);
    if (positiveSign)
      appendToFile("GLOBAL SIGN +1 " + sep, sb);
    else
      appendToFile("GLOBAL SIGN -1 " + sep, sb);
    int ind = list.getSelectedIndex();
    if (oneD) {
      appendToFile(getVectorParams(), sb);
      appendToFile("CMD PROFILE " + ind, sb);
      appendOutputWithCaret("Profile " + ind, 'i');
    } else {
      appendToFile(getPlaneParams(), sb);
      appendToFile("CMD CONTOUR " + ind, sb);
      appendOutputWithCaret("Contour " + ind, 'i');
    }
    final boolean oneD = this.oneD;
    String s = list.getSelectedItem().toString();
    final String val = s.substring(s.indexOf(".")+1).replaceAll(" ", "");
    final String orb = basis.getSelectedItem().toString().replaceAll(" ", "");
    nboService.queueJob("view", "getting " + (oneD ? "profile" : "contour"),
        new Runnable() {
          @Override
          public void run() {
            dispBox.setVisible(true);
            nboService.rawCmdNew("v", sb, false, NBOService.MODE_IMAGE);
            String s = (numStor%9 + 1) + ")*[" + (oneD ? "P-" : "C-") + index
                + "] " + orb + " " + val +"  {"+(positiveSign?"+":"-")+": "+(oneD ? vLab.getText():pLab.getText())+"}";
            if(numStor%9==0)
              model.add(8,model.remove(8).replace("*", ""));
            else
              model.add((numStor % 9)-1,model.remove((numStor % 9)-1).replace("*", ""));
            model.remove(numStor % 9);
            model.add(numStor % 9, s);
            dList.setSelectedIndex(numStor % 9);
            numStor++;
          }
        });
//    nboService.queueJob("view", "getting .eps", new Runnable(){
//      @Override
//      public void run(){
//        SB sb = new SB();
//        BufferedReader b = null;
//        try {
//          b = new BufferedReader(new FileReader(new File(inputFile.getParent()+"/"+jobStem+".ps")));
//        } catch (FileNotFoundException e1) {
//          System.out.println("------ERROR------");
//        }
//        sb.append("%%Header"+sep+"%%BoundingBox(0,0,200,200)"+sep);
//        String line;
//        try {
//          while((line = b.readLine())!= null){
//            if(line.contains("showpage"))
//              sb.append("%%tailer"+sep);
//            sb.append(line + sep);
//          }
//        nboService.writeToFile(sb.toString(), new File(inputFile.getParent()+"/"+jobStem+".eps"));
//        } catch (IOException e) {
//          // TODO
//        }
//      }
//    });
  }

  protected void showView1D2D() {
    final SB sb = new SB();
    appendToFile("GLOBAL C_PATH " + inputFile.getParent() + sep, sb);
    appendToFile("GLOBAL C_JOBSTEM " + jobStem + sep, sb);
    appendToFile("CMD SHOW " + (dList.getSelectedIndex() + 1) + sep, sb);
    //final String title = PT.split(dList.getSelectedValue(), "]")[1].trim();
    final String fname = inputFile.getParent() + "\\" + jobStem + ".ps";
    nboService.queueJob("view", "creating PostScript file...", new Runnable() {
      @Override
      public void run() {
        File f = new File(fname);
        if (f.exists())
          f.delete();
        nboService.rawCmdNew("v", sb, false, NBOService.MODE_IMAGE);
        while (!f.exists() && !nboService.jobCanceled) {
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            // TODO
          }
        }
        vwr.alert("file " + fname + " has been created");
      }
    });
  }

  protected void view3D() {
    final SB sb = new SB();
    final int num = (numStor%9<=dList.getSelectedIndex())?
        ((numStor/9)-1)*9+dList.getSelectedIndex()+1:(numStor/9)*9+dList.getSelectedIndex()+1;
    appendToFile("GLOBAL C_PATH " + inputFile.getParent() + sep, sb);
    appendToFile("GLOBAL C_JOBSTEM " + jobStem + sep, sb);
    appendToFile(getCameraParams(), sb);
    //appendToFile("GLOBAL C_FNAME " + jobStem + num + sep, sb);
    appendToFile("CMD VIEW ", sb);
    int[] indices = dList.getSelectedIndices();
    final SB title = new SB();
    for (int x : indices) {
      appendToFile(Integer.toString(x + 1) + " ", sb);
      title.append(PT.split(dList.getModel().getElementAt(x), "]")[1] + " ");
    }
    final String fname = inputFile.getParent() + "\\" + jobStem + ".bmp";
    nboService.queueJob("view", "Raytracing, please be patient...",
        new Runnable() {
          @Override
          public void run() {
            String err = null;
            File f = new File(fname);
              String id = "id " + PT.esc(title.toString().trim());
//            if (f.exists()){
//              nboService.runScriptQueued("image " + id + " close;image " + id + " "
//                  + PT.esc(f.toString().replace('\\', '/')));
//              return;
//            }
            f.delete();
            nboService.rawCmdNew("v", sb, false, NBOService.MODE_IMAGE);
            while (!f.exists()&&!nboService.jobCanceled) {
              try {
                Thread.sleep(10);
              } catch (InterruptedException e) {
                // TODO
              }
            }
            try {
              String script = "image " + id + " close;image " + id + " "
                  + PT.esc(f.toString().replace('\\', '/'));
              nboService.runScriptQueued(script);
              statusLab.setText(err);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        });
  }

  protected void processOrbitalList(String list, DefaultComboBoxModel<String> listModel) {
    System.out.println("processing list " + list);
    try {
      String[] st = new String[list.length() / 20];
      for (int i = 0; (i + 1) * 20 <= list.length(); i++)
        st[i] = list.substring(i * 20, (i + 1) * 20);
      lists.put("o "+basis.getSelectedItem().toString(), st);
      setOrbitalList(st, listModel);
    } catch (Exception e) {
      System.out.println(e);
    }
  }
  
  private void setOrbitalList(String[] s,DefaultComboBoxModel<String> listModel){
    int select = this.list.getSelectedIndex();
    listModel.removeAllElements();
    for(String x:s)
      listModel.addElement(x);
    if(select!=-1)
      this.list.setSelectedIndex(select);
    else list.setSelectedIndex(0);
  }

  private int viewVectorPt = 0;
  private int viewPlanePt = 0;
  
  /**
   * Set the value of the atom number for vectors (profiles) or planes (contours)
   * via a callback from Jmol atom picking.
   *  
   * @param atomno
   */
  protected void notifyCallbackV(String atomno) {
    switch (viewState) {
    case VIEW_STATE_VECTOR:
      vectorFields[viewVectorPt++].setText(atomno); 
      vLab.setText(vectorFields[0].getText()+", "+vectorFields[1].getText());
      showSelected(vLab.getText().split(", "));
      viewVectorPt = viewVectorPt % 2;
      break;
    case VIEW_STATE_PLANE:
      planeFields[viewPlanePt++].setText(atomno); 
      pLab.setText(planeFields[0].getText()+", "+planeFields[1].getText()+", "+planeFields[2].getText());
      showSelected(pLab.getText().split(", ")); 
      viewPlanePt = viewPlanePt % 3;
    }    
  }

  protected void rawInputV(String cmd) {
    if (!checkJmolNBO())
      return;
    if (cmd.startsWith("BAS"))
      try {
        basis.setSelectedItem(cmd.split(" ")[1]);
        appendOutputWithCaret("Basis changed:  " + cmd.split(" ")[1],'i');
      } catch (Exception e) {
        appendOutputWithCaret("NBO View can't do that",'b');
      }
    else if (cmd.startsWith("CON")) {
      try {
        int i = Integer.parseInt(cmd.split(" ")[1]);
        list.setSelectedIndex(i - 1);
        oneD = false;
        goViewClicked();
      } catch (Exception e) {
        appendOutputWithCaret("NBO View can't do that",'b');
      }
    } else if (cmd.startsWith("PR")) {
      try {
        int i = Integer.parseInt(cmd.split(" ")[1]);
        list.setSelectedIndex(i - 1);
        oneD = true;
        goViewClicked();
      } catch (Exception e) {
        appendOutputWithCaret("NBO View can't do that",'b');
      }
    } else if (cmd.startsWith("VIEW")) {
      try {
        int i = Integer.parseInt(cmd.split(" ")[1]);
        dList.setSelectedIndex(i - 1);
        view3D();
      } catch (Exception e) {
        appendOutputWithCaret("NBO View can't do that",'b');
      }
    }
  }


}
