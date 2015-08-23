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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
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
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jmol.i18n.GT;
import org.openscience.jmol.app.jmolpanel.JmolPanel;

abstract class NBODialogView extends NBODialogRun {

  protected JComboBox<String> basis;
  protected String keyword = "PNBO";
  protected int keywordNumber = 6;
  protected final static String[] basSet = { "AO", "PNAO", "NAO", "PNHO",
      "NHO", "PNBO", "NBO", "PNLMO", "NLMO", "MO" };
  protected JLabel sLab, vLab, pLab;
  protected JButton btnSign, btnVec, btnPla, btnCam, btnStip, goBtn, goBtn2;
  protected JRadioButton btnProf, btnCont, btnShow, btnView3D;
  protected boolean oneD = true;
  protected boolean inLobes = true;
  protected JComboBox<String> list;
  protected JCheckBox selToggle;
  protected Hashtable<String, String[]> lists;
  protected int viewState;

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
    //numStor = 0;
    java.util.Properties props = JmolPanel.historyFile.getProperties();
    workingPath = (props.getProperty("workingPath",
        System.getProperty("user.home")));
    p.removeAll();
    p.setLayout(new BorderLayout());
    if(topPanel==null)topPanel = buildTopPanel();
    p.add(topPanel, BorderLayout.PAGE_START);
    JPanel p2 = new JPanel(new BorderLayout());
    JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,select(),display());
    sp.setDividerLocation(430);
    p2.add(sp,BorderLayout.CENTER);
    Box box = Box.createHorizontalBox();
    box.add(new JLabel("JobFile")).setFont(new Font("Arial",Font.BOLD,25));
    box.add(folderBox());
    browse.setEnabled(true);
    p2.add(box,BorderLayout.NORTH);
    p.add(p2,BorderLayout.CENTER);
    p.add(statusPanel, BorderLayout.PAGE_END);
    if(isJmolNBO) setInputFile(inputFile,"nbo",null);
  }

  /**
   * select panel
   * @return select panel
   */
  protected JPanel select() {
    JPanel selectPanel = new JPanel();
    selectPanel.setLayout(new BoxLayout(selectPanel,BoxLayout.Y_AXIS));
    selectPanel.setBorder(BorderFactory.createLoweredBevelBorder());
    Box box;
    //basis/////////////
    (box = Box.createHorizontalBox()).add(new JLabel("Basis ")).setFont(
        new Font("Arial",Font.BOLD, 25));
    box.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 5));
    (basis = new JComboBox<String>(basSet)).setMaximumSize(new Dimension(100,25));
    box.add(basis).setEnabled(isJmolNBO);
    box.add(Box.createRigidArea(new Dimension(300,0)));
    basis.setSelectedIndex(5);
    basis.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        basisSel();
      }
    });
    selectPanel.add(box);
    //orbital//////////
    (box = Box.createHorizontalBox()).add(new JLabel("Orbital ")).setFont(
        new Font("Arial",Font.BOLD, 25));
    selectPanel.add(new JSeparator());
    box.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 5));
    DefaultComboBoxModel<String> listModel = new DefaultComboBoxModel<String>();
    (list = new JComboBox<String>(listModel)).setFont(nboFont);
    list.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int i = list.getSelectedIndex();
        if (i == -1)
          return;
        showOrbJmol(basis.getSelectedItem().toString(), list.getSelectedIndex()+1);
      }
    });
    box.add(list).setMaximumSize(new Dimension(225,25));
    box.add(Box.createRigidArea(new Dimension(110,0)));
    selectPanel.add(box);
    selectPanel.add(new JSeparator());
    int i = iLast;
    iLast = -1;
    if (i > 0 && i < list.getComponentCount())
      i = 0;
    final int i0 = i;
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        list.setSelectedIndex(i0);
      }
    });
    //store////////////
    (box = Box.createHorizontalBox()).add(new JLabel("Store ")).setFont(
        new Font("Arial",Font.BOLD, 25));
    Box box2 = Box.createVerticalBox();
    (btnProf = new JRadioButton("1D Profile")).setFont(nboFont);
    box2.add(btnProf).setEnabled(isJmolNBO);
    (btnCont = new JRadioButton("2D Contour")).setFont(nboFont);
    btnProf.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        oneD = true;
        btnProf.setSelected(true);
        btnCont.setSelected(false);
        btnPla.setEnabled(false);
        btnVec.setEnabled(true);
        viewState = VIEW_STATE_VECTOR;
        showSelected(vLab.getText().split(", "));
      }
    });
    box2.add(btnCont).setEnabled(isJmolNBO);
    box.add(box2);
    btnCont.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        oneD = false;
        btnCont.setSelected(true);
        btnProf.setSelected(false);
        btnPla.setEnabled(true);
        btnVec.setEnabled(false);
        viewState = VIEW_STATE_PLANE;
        showSelected(pLab.getText().split(", "));
      }
    });
    goBtn = new JButton("GO");
    goBtn.setEnabled(isJmolNBO);
    goBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        goViewClicked();
      }
    });
    box.add(Box.createRigidArea(new Dimension(165,0)));
    box.add(goBtn).setFont(new Font("Arial",Font.BOLD,18));
    selectPanel.add(box);
    selToggle = new JCheckBox("Show Atoms Defining Axis/Plane");
    selToggle.setAlignmentX(0.0f);
    selToggle.setSelected(true);
    selToggle.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if(!selToggle.isSelected()) nboService.runScriptQueued("select off");
        else
          if(btnProf.isSelected()) showSelected(vLab.getText().split(", "));
          else showSelected(pLab.getText().split(", "));
      }
    });
    selectPanel.add(selToggle);
    selectPanel.add(new JSeparator());
    //display///////////
    (box = Box.createHorizontalBox()).add(new JLabel("Display ")).setFont(
        new Font("Arial",Font.BOLD, 25));
    box2 = Box.createVerticalBox();
    (btnShow = new JRadioButton("1D/2D")).setFont(nboFont);
    btnShow.setEnabled(numStor>0);
    btnShow.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        btnView3D.setSelected(false);
        btnShow.setSelected(true);
      }
    });
    box2.add(Box.createRigidArea(new Dimension(30,0)));
    box2.add(btnShow);
    (btnView3D = new JRadioButton("3D")).setFont(nboFont);
    btnView3D.setEnabled(numStor>0);
    btnView3D.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        btnView3D.setSelected(true);
        btnShow.setSelected(false);
      }
    });
    box2.add(btnView3D);
    box.add(box2);
    box.add(Box.createRigidArea(new Dimension(176,0)));
    goBtn2 = new JButton("GO");
    goBtn2.setEnabled(isJmolNBO);
    goBtn2.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if(btnView3D.isSelected()) view3D();
        else showView1D2D();
      }
    });
    box.add(goBtn2).setFont(new Font("Arial",Font.BOLD,18));
    selectPanel.add(box);
    box2 = Box.createHorizontalBox();
    //box2.add(selToggle);
    jCheckAtomNum = new JCheckBox("Show Atom #'s");
    box2.add(jCheckAtomNum);
    jCheckAtomNum.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) { 
        if(!jCheckAtomNum.isSelected())
          nboService.runScriptQueued("select {*};label off");
        else
          nboService.runScriptQueued("select {*};label %a");
      nboService.runScriptQueued("color labels white;select remove {*}");
      if(selToggle.isSelected())
        if(btnProf.isSelected()) showSelected(vLab.getText().split(", "));
        else showSelected(pLab.getText().split(", "));
      }
    });
    JCheckBox orientation = new JCheckBox("Use Jmol orientation");
    box2.add(orientation);
    //bo.add(box2);
    selectPanel.add(box2,BorderLayout.PAGE_END);
    selectPanel.add(Box.createRigidArea(new Dimension(0,200)));
    if(isJmolNBO)
      basisSel();
    return selectPanel;
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

  protected void showSelected(String[] s){
    if(!selToggle.isSelected()) return;
    nboService.runScriptQueued("select remove {*}");
    for(String x:s)
      nboService.runScriptQueued("select add {*}["+x+"]");
    nboService.runScriptQueued("select on;refresh");
  }

  protected int iLast = -1, iLastD = -1;

  /**
   * builds orbital display panel
   * @return display panel
   */
  private JPanel display() {
    JPanel s = new JPanel(new BorderLayout());
    s.setBorder(BorderFactory.createLoweredBevelBorder());
    JLabel lab = new JLabel("Storage:");
    lab.setFont(nboFont);
    s.add(lab,BorderLayout.PAGE_START);
    JScrollPane p = new JScrollPane();
    if(dList==null){
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
        }
      });
    }
    p.getViewport().add(dList);
    s.add(p, BorderLayout.CENTER);
    //Settings Panel//////////////////////////////
    JPanel p2 = new JPanel(new GridLayout(4,1));
    p2.add(btnSign = new JButton("Sign")).setFont(nboFont);
    btnSign.setEnabled(isJmolNBO);
    btnSign.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (sLab.getText().trim().equals("+"))
          sLab.setText(" -");
        else
          sLab.setText(" +");
      }
    });
    p2.add(btnVec = new JButton("1D Axis")).setFont(nboFont);
    btnVec.setEnabled(isJmolNBO);
    btnVec.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        vector();
      }
    });
    p2.add(btnPla = new JButton("2D Plane")).setFont(nboFont);
    btnPla.setEnabled(isJmolNBO);
    btnPla.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        plane();
      }
    });
    (btnCam = new JButton("Camera")).setFont(nboFont);
    p2.add(btnCam);
    btnCam.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cam1();
      }
    });
    btnCam.setEnabled(isJmolNBO);
    JPanel p3 = new JPanel(new BorderLayout());
    p3.add(p2, BorderLayout.CENTER);
    p2 = new JPanel(new GridLayout(4,1));
    p2.add(sLab = new JLabel(" +")).setFont(new Font("Monospaced",Font.BOLD,20));
    p2.add(vLab = new JLabel("1, 2")).setFont(nboFont);
    p2.add(pLab = new JLabel("1, 2, 3")).setFont(nboFont);
    (btnStip = new JButton("Lines")).setFont(nboFont);
    p2.add(btnStip);
    btnStip.setEnabled(isJmolNBO);
    btnStip.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        stip();
      }
    });
    p3.add(p2, BorderLayout.LINE_END);
    p3.setBorder(BorderFactory.createTitledBorder("Settings:"));
    s.add(p3, BorderLayout.PAGE_END);
    btnProf.doClick();
    return s;
  }
  
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
    nboService.runScriptQueued("MO delete; NBO delete; NBO TYPE " + type
        + "; NBO " + item.substring(pt+1, item.indexOf("]")));
  }
  
  protected void showOrbJmol(String type, int i){
    nboService.runScriptQueued("MO delete; NBO delete; NBO TYPE " + type
        + "; NBO " + i);
  }

  synchronized protected void basisSel() {
    keywordNumber = basis.getSelectedIndex() + 1;
    String keyword = basis.getSelectedItem().toString();
    final SB sb = new SB();
    if(lists.get("o "+basis.getSelectedItem().toString())!=null){
      setOrbitalList(lists.get("o "+keyword));
      return;
    }
    if (keyword.equals("MO")) {
      for (int i = 1; i <= list.getModel().getSize(); i++) {
        sb.append((i + ".  MO " + i + "                 ").substring(0, 20));
      }
      nboService.queueJob("view", "getting orbital list", new Runnable() {
        @Override
        public void run() {
          processOrbitalList(sb.toString());
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
          processOrbitalList(reqInfo);
        }
      });
    }
  }

  @Override
  protected void showWorkpathDialogV(String workingPath) {
    JFileChooser myChooser = new JFileChooser();
    myChooser.setFileFilter(new FileNameExtensionFilter("NBO", "nbo"));
    myChooser.setFileHidingEnabled(true);
    String fname = workingPath;
    myChooser.setSelectedFile(new File(fname));
    int button = myChooser.showDialog(this, GT._("Select"));
    if (button == JFileChooser.APPROVE_OPTION) {
      setInputFile(newNBOFile(myChooser.getSelectedFile(), "47"), "nbo", showWorkPathDone);
      if(basis == null) basis = new JComboBox<String>(basSet);
      isJmolNBO = true;
      saveHistory();
      basisSel();
      nboResetV();
      buildView(this.getContentPane());
      setComponents(this.getContentPane());
    }
  }

  @Override
  protected void nboResetV() {
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
    final int index = list.getSelectedIndex() + 1;
    appendToFile("GLOBAL C_PATH " + inputFile.getParent() + sep, sb);
    appendToFile("GLOBAL C_JOBSTEM " + jobStem + sep, sb);
    appendToFile("GLOBAL I_BAS_1 " + (basis.getSelectedIndex() + 1) + sep, sb);
    if (sLab.getText().equals(" +"))
      appendToFile("GLOBAL SIGN +1 " + sep, sb);
    else
      appendToFile("GLOBAL SIGN -1 " + sep, sb);
    if (oneD) {
      appendToFile(getVectorParams(), sb);
      appendToFile("CMD PROFILE " + (list.getSelectedIndex() + 1), sb);
    } else {
      appendToFile(getPlaneParams(), sb);
      appendToFile("CMD CONTOUR " + (list.getSelectedIndex() + 1), sb);
    }
    final boolean oneD = this.oneD;
    String s = list.getSelectedItem().toString();
    final String val = s.substring(s.indexOf(".")+1).replaceAll(" ", "");
    final String orb = basis.getSelectedItem().toString().replaceAll(" ", "");
    nboService.queueJob("view", "getting " + (oneD ? "profile" : "contour"),
        new Runnable() {
          @Override
          public void run() {
            nboService.rawCmdNew("v", sb, false, NBOService.MODE_IMAGE);
            String s = (numStor%9 + 1) + ")*[" + (oneD ? "P-" : "C-") + index
                + "] " + orb + " " + val +"  {"+sLab.getText().trim()+": "+(oneD ? vLab.getText():pLab.getText())+"}";
            if(numStor%9==0)
              model.add(8,model.remove(8).replace("*", ""));
            else
              model.add((numStor % 9)-1,model.remove((numStor % 9)-1).replace("*", ""));
            model.remove(numStor % 9);
            model.add(numStor % 9, s);
            dList.setSelectedIndex(numStor % 9);
            numStor++;
            btnView3D.setEnabled(true);
            btnShow.setEnabled(true);
            goBtn2.setEnabled(true);
          }
        });
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
    appendToFile("GLOBAL C_FNAME " + jobStem + num + sep, sb);
    appendToFile("CMD VIEW ", sb);
    int[] indices = dList.getSelectedIndices();
    final SB title = new SB();
    for (int x : indices) {
      appendToFile(Integer.toString(x + 1) + " ", sb);
      title.append(PT.split(dList.getModel().getElementAt(x), "]")[1] + " ");
    }
    final String fname = inputFile.getParent() + "\\" + jobStem + num + ".bmp";
    nboService.queueJob("view", "Raytracing, please be patient...",
        new Runnable() {
          @Override
          public void run() {
            String err = null;
            File f = new File(fname);
              String id = "id " + PT.esc(title.toString().trim());
            if (f.exists()){
              nboService.runScriptQueued("image " + id + " close;image " + id + " "
                  + PT.esc(f.toString().replace('\\', '/')));
              return;
            }
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

  protected void processOrbitalList(String list) {
    System.out.println("processing list " + list);
    try {
      String[] st = new String[list.length() / 20];
      for (int i = 0; (i + 1) * 20 <= list.length(); i++)
        st[i] = list.substring(i * 20, (i + 1) * 20);
      lists.put("o "+basis.getSelectedItem().toString(), st);
      setOrbitalList(st);
    } catch (Exception e) {
      System.out.println(e);
    }
  }
  
  private void setOrbitalList(String[] s){
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
        appendOutputWithCaret("Basis changed:\n  " + cmd.split(" ")[1]);
      } catch (Exception e) {
        appendOutputWithCaret("NBO View can't do that");
      }
    else if (cmd.startsWith("CON")) {
      try {
        int i = Integer.parseInt(cmd.split(" ")[1]);
        list.setSelectedIndex(i - 1);
        oneD = false;
        goViewClicked();
      } catch (Exception e) {
        appendOutputWithCaret("NBO View can't do that");
      }
    } else if (cmd.startsWith("PR")) {
      try {
        int i = Integer.parseInt(cmd.split(" ")[1]);
        list.setSelectedIndex(i - 1);
        oneD = true;
        goViewClicked();
      } catch (Exception e) {
        appendOutputWithCaret("NBO View can't do that");
      }
    } else if (cmd.startsWith("VIEW")) {
      try {
        int i = Integer.parseInt(cmd.split(" ")[1]);
        dList.setSelectedIndex(i - 1);
        view3D();
      } catch (Exception e) {
        appendOutputWithCaret("NBO View can't do that");
      }
    }
  }


}
