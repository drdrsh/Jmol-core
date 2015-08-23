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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;

import javajs.util.PT;
import javajs.util.SB;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jmol.i18n.GT;
import org.openscience.jmol.app.jmolpanel.JmolPanel;

abstract class NBODialogModel extends NBODialogConfig {

  private static final String INPUT_FILE_EXTENSIONS = 
      ";adf;cfi;gau;gms;jag;mm2;mnd;mp;nw;orc;pqs;qc;vfi;";

  private static final String[] SAVE_OPTIONS = { 
      "-Type-", 
      "(.adf) ADF",
      "(.cfi) Cartesian Coordinate", 
      "(.gau) Gaussian", 
      "(.gms) GAMESS",
      "(.jag) Jaguar", 
      "(.mm2) MM2-type", 
      "(.mnd) MINDO/AM1", 
      "(.mp) Molpro",
      "(.nw) NWChem", 
      "(.orc) Orca", 
      "(.pqs) PQS", 
      "(.qc) Q-Chem",
      "(.vfi) Valence Coordinate" 
  };

  private final static int ALTER = 1, CLIP = 2, MUTATE = 3;

  private static final String LOAD_SCRIPT = ";set zoomlarge false;zoomTo 0.5 {*} 0;";

  protected abstract void nboResetV();

  private int editMode;
  private String savePath;
  private String selected = "";
  
  protected String usePath;
  protected JComboBox<String> jComboUse, jComboSave;
  protected Box editBox;
  protected JButton jbEdit, jbSym;
  protected JTextField jtSelectAtoms, tfFolderS, tfNameS, tfExtS;
  protected JCheckBox jCheckAtomNum;
  protected JRadioButton jrJmolIn, jrLineIn, jrFileIn;
  protected JTextField jtJmolInput, jtLineInput;


  protected NBODialogModel(JFrame f) {
    super(f);
  }

  protected void buildModel(Container p) {
    java.util.Properties props = JmolPanel.historyFile.getProperties();
    savePath = (props.getProperty("savePath", System.getProperty("user.home")));
    usePath = (props.getProperty("usePath", System.getProperty("user.home")));
    p.removeAll();
    p.setLayout(new BorderLayout());
    if (topPanel == null)
      topPanel = buildTopPanel();
    p.add(topPanel, BorderLayout.PAGE_START);
    JPanel leftPanel = new JPanel();
    leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
    leftPanel.add(useBox());
    leftPanel.add(saveBox());
    leftPanel.add(editBox());
    JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel,
        modelOut());
    sp.setDividerLocation(430);
    sp.setBorder(BorderFactory.createLoweredBevelBorder());
    p.add(sp, BorderLayout.CENTER);
    p.add(statusPanel, BorderLayout.PAGE_END);
    if (vwr.ms.ac != 0 && !isJmolNBO) {
      loadModel();
      enableComps();
    }
  }

  private Component editBox() {
    JPanel p = new JPanel();
    p.setBorder(new TitledBorder("Edit"));
    Box b2 = Box.createVerticalBox();
    p.add(b2);
    Box box = Box.createHorizontalBox();
    //box.add(new JLabel("EDIT  ")).setFont(new Font("Arial", Font.BOLD, 25));
    String[] actions = { "-Action-",
        "alter - atomic charge, distance, angle, or dihedral angle", "clip - ",
        "fuse - ", "link - ", "mutate - ", "rebond - ", "switch - ",
        "twist - ", "unify - ", " 3chb - " };
    (action = new JComboBox<String>(actions)).setEnabled(false);
    box.add(action);
    action.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        actionSelected();
      }
    });
    box.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 5));
    b2.add(box).setMaximumSize(new Dimension(500, 60));
    editBox = Box.createVerticalBox();
    editBox.setBorder(BorderFactory.createLoweredBevelBorder());
    editBox.add(Box.createRigidArea(new Dimension(200, 60)));
    jtSelectAtoms = new JTextField("Select atoms...");
    jtSelectAtoms.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        editModel();
      }
    });
    jtSelectAtoms.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void changedUpdate(DocumentEvent arg0) {
      }

      @Override
      public void insertUpdate(DocumentEvent arg0) {
        if (!jtSelectAtoms.getText().equals(""))
          jbEdit.setEnabled(true);
      }

      @Override
      public void removeUpdate(DocumentEvent arg0) {
        if (jtSelectAtoms.getText().equals(""))
          jbEdit.setEnabled(false);
      }
    });
    (jbEdit = new JButton("Apply")).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        editModel();
      }
    });
    b2.add(editBox);
    //modelPanel.add(new JSeparator());
    jbSym = new JButton("Symmety");
    box = Box.createVerticalBox();
    box.setAlignmentX(0.5f);
    box.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 5));
    jbSym.setAlignmentX(0.5f);
    box.add(jbSym).setEnabled(false);
    jbSym.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        getSymmetry();
      }
    });
    box.add(Box.createRigidArea(new Dimension(30, 30)));
    jCheckAtomNum = new JCheckBox("View atom numbers");
    jCheckAtomNum.setEnabled(false);
    jCheckAtomNum.setAlignmentX(0.5f);
    jCheckAtomNum.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (!jCheckAtomNum.isSelected())
          nboService.runScriptQueued("select {*};label off");
        else
          nboService.runScriptQueued("select {*};label %a");
        nboService.runScriptQueued("color labels white;select remove {*}");
      }
    });
    box.add(jCheckAtomNum);
    b2.add(box);
    return p;
  }

  private Component saveBox() {
    JPanel p = new JPanel();
    p.setBorder(new TitledBorder("Save As"));
    Box box = Box.createHorizontalBox();
//    box.add(new JLabel("SAVE ")).setFont(new Font("Arial", Font.BOLD, 25));
    (jComboSave = new JComboBox<String>(SAVE_OPTIONS)).setEnabled(false);
    jComboSave.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Object item = jComboSave.getSelectedItem();
        if (!item.equals("-Type-")) {
          String s = item.toString();
          showSaveDialog(s.substring(s.indexOf("(") + 2, s.indexOf(")")));
        }
      }
    });
    Box b2 = Box.createVerticalBox();
    b2.add(jComboSave);
    b2.add(folderSaveBox());
    box.add(b2);
    box.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 5));
    p.add(box);
    return p;
  }

  /**
   * adds use elements to main panel
   * 
   * @return use elements
   */
  private Component useBox() {
    JPanel p = new JPanel();
    p.setBorder(new TitledBorder("Structure"));
    Box b = Box.createHorizontalBox();
    (jrJmolIn = new JRadioButton("Jmol Input")).setFont(nboFont);
    jrJmolIn.setSelected(true);
    jrJmolIn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        browse.setEnabled(false);
      }
    });
    (jrLineIn = new JRadioButton("Line Formula")).setFont(nboFont);
    jrLineIn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        browse.setEnabled(false);
      }
    });
    (jrFileIn = new JRadioButton("File Input")).setFont(nboFont);
    jrFileIn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        browse.setEnabled(true);
      }
    });
    ButtonGroup rg = new ButtonGroup();
    rg.add(jrJmolIn);
    rg.add(jrLineIn);
    rg.add(jrFileIn);
    createInput(jtJmolInput = new JTextField(), jrJmolIn);
    createInput(jtLineInput = new JTextField(), jrLineIn);
    String[] useOps = { "-Type-", "(.47) NBOarchive ", "(.adf) ADF ",
        "(.cfi) Cartesian Coordinate", "(.gau) Gaussian", "(.gms) GAMESS",
        "(.jag) Jaguar", "(.log) Gaussian log", "(.mp) Molpro", "(.nw) NWChem",
        "(.orc) Orca", "(.pqs) PQS", "(.qc) Q-Chem",
        "(.vfi) Valence Coordinate" };
    (jComboUse = new JComboBox<String>(useOps))
        .addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Object item = jComboUse.getSelectedItem();
            if (!item.equals("-Type-")) {
              String s = item.toString();
              showWorkpathDialogM(usePath,
                  s.substring(s.indexOf("(") + 2, s.indexOf(")")));
            }
          }
        });
    browse.setEnabled(false);
    JPanel p2 = new JPanel(new GridLayout(3, 2));
    p2.add(jrJmolIn);
    p2.add(jtJmolInput);
    p2.add(jrLineIn);
    p2.add(jtLineInput);
    p2.add(jrFileIn);
    p2.add(jComboUse);
    addListenersAndSize(jComboUse, jrFileIn);
    b.add(p2);
    p.add(b);
    p.add(folderBox());
    return p;
  }

  private void createInput(final JTextField field, JRadioButton radio) {

    field.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        getModel(field);
      }
    });
    addListenersAndSize(field, radio);
  }

  private void addListenersAndSize(JComponent field, final JRadioButton radio) {
    field.addMouseListener(new MouseListener() {

      @Override
      public void mouseClicked(MouseEvent e) {
      }

      @Override
      public void mousePressed(MouseEvent e) {
        radio.setSelected(true);
      }

      @Override
      public void mouseReleased(MouseEvent e) {
      }

      @Override
      public void mouseEntered(MouseEvent e) {
      }

      @Override
      public void mouseExited(MouseEvent e) {
      }

    });
    field.addKeyListener(new KeyListener() {

      @Override
      public void keyTyped(KeyEvent e) {
      }

      @Override
      public void keyPressed(KeyEvent e) {
        radio.setSelected(true);
      }

      @Override
      public void keyReleased(KeyEvent e) {
      }

    });
    field.setMinimumSize(new Dimension(226, 25));
    field.setMaximumSize(new Dimension(226, 25));
  }

  private JPanel folderSaveBox() {
    JPanel b = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.BOTH;
    (tfFolderS = new JTextField()).setPreferredSize(new Dimension(130, 20));
    tfFolderS.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        //browse.setSelected(true)
        showWorkpathDialogM(tfFolder.getText() + "/new", null);
      }
    });
    b.add(tfFolderS, c);
    c.gridx = 1;
    (tfNameS = new JTextField()).setPreferredSize(new Dimension(100, 20));
    b.add(tfNameS, c);
    c.gridx = 0;
    c.gridy = 1;
    b.add(new JLabel("         folder"), c);
    c.gridx = 1;
    b.add(new JLabel("          name"), c);
    c.gridx = 2;
    c.gridy = 0;
    (tfExtS = new JTextField()).setPreferredSize(new Dimension(40, 20));
    b.add(tfExtS, c);
    c.gridy = 1;
    b.add(new JLabel("  ext"), c);
    c.gridx = 3;
    c.gridy = 0;
    c.gridheight = 2;
    JButton btn = new JButton("Save");
    b.add(btn, c);
    b.setPreferredSize(new Dimension(350, 50));
    return b;
  }

  /**
   * edit action selected
   */
  protected void actionSelected() {
    nboService.runScriptQueued("select remove {*}; select on");
    Object item = action.getSelectedItem().toString().split(" ")[0];
    if (item.equals("-Action-")) {
      return;
    } else if (item.equals("clip") || item.equals("fuse")
        || item.equals("link") || item.equals("switch")) {
      editMode = CLIP;
      clip("Select two atoms:", null);
    } else if (item.equals("mutate")) {
      editMode = MUTATE;
      clip("Formula: ", jtSelectAtoms);
    } else if (item.equals("rebond")) {
      clip("Symtype: ", jtSelectAtoms);
    } else if (item.equals("alter") || item.equals("twist")) {
      editMode = ALTER;
      clip("New Value: ", jtSelectAtoms);
    }
    setComponents(editBox);
  }

  protected void clip(String st, Component c) {
    editBox.removeAll();
    editBox.setMaximumSize(new Dimension(200, 60));
    Box box = Box.createHorizontalBox();
    box.add(new JLabel(st));
    if (c != null) {
      jtSelectAtoms.setText("Select atoms...");
      box.add(c);
      jtSelectAtoms.setEnabled(false);
    } else
      jtSelectAtoms.setText("");
    editBox.add(box);
    box = Box.createHorizontalBox();
    JButton bu = new JButton("Clear Selected");
    bu.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        clearSelected();
      }
    });
    box.add(bu);
    box.add(jbEdit);
    editBox.add(box);
    editBox.repaint();
    editBox.revalidate();
  }

  protected void clearSelected() {
    if (!jCheckAtomNum.isSelected())
      nboService.runScriptQueued("label off");
    nboService.runScriptQueued("measure off;select remove {selected};refresh");
    selected = "";
    jtSelectAtoms.setText("Select atoms...");
    jtSelectAtoms.setEnabled(false);
    appendOutputWithCaret("Selection cleared");
  }

  /**
   * apply edit to model
   */
  protected void editModel() {
    SB sb = new SB();
    String cmd = action.getSelectedItem().toString().split(" ")[0] + " "
        + selected;
    selected = "";
    if (jtSelectAtoms != null)
      cmd += " " + jtSelectAtoms.getText();
    appendToFile("CMD " + cmd, sb);
    appendOutput(cmd);
    jbEdit.setEnabled(false);
    modelCmd(sb);
  }

  protected JPanel modelOut() {
    JPanel s = new JPanel();
    s.setLayout(new BorderLayout());
    JLabel lab = new JLabel("Session Dialog");
    lab.setFont(nboFont);
    s.add(lab, BorderLayout.PAGE_START);
    JScrollPane p1 = new JScrollPane();
    jpNboOutput = new JTextPane();
    jpNboOutput.setEditable(false);
    jpNboOutput.setFont(new Font("Arial", Font.PLAIN, 18));
    p1.getViewport().add(jpNboOutput);
    s.add(p1, BorderLayout.CENTER);
    JButton b = new JButton("Clear");
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        clearOutput();
      }
    });
    s.add(b, BorderLayout.SOUTH);
    return s;
  }

  private void saveHistoryM() {
    java.util.Properties props = new java.util.Properties();
    props.setProperty("savePath", savePath);
    props.setProperty("usePath", usePath);
    JmolPanel.historyFile.addProperties(props);
  }

  protected void getSymmetry() {
    jbSym.setSelected(false);
    SB sb = new SB();
    appendToFile("CMD symmetry", sb);
    appendOutputWithCaret("Symmetry: ");
    modelCmd(sb);
  }

  protected void getModel(JTextField textBox) {
    if (textBox.getText().equals(""))
      return;
    action.setSelectedIndex(0);
    String s = "";
    if (textBox.equals(jtJmolInput)) {
      jrJmolIn.setSelected(true);
      nboService.runScriptNow("zap");
      s = "load $" + textBox.getText();
      if (nboService.runScriptNow(s) == null) {
        if (nboService.runScriptNow("load :" + textBox.getText()) != null) {
          while (vwr.ms.ac == 0)
            try {
              Thread.sleep(10);
            } catch (Exception e) {
              System.out.println("HELLO");
              return;
            }
          loadModel();
          return;
        }
        appendOutputWithCaret("File not found");
        return;
      }
      while (vwr.ms.ac == 0)
        try {
          Thread.sleep(10);
        } catch (Exception e) {
          System.out.println("HELLO");
          return;
        }
      loadModel();
    } else if (textBox.equals(jtLineInput)) {
      jrLineIn.setSelected(true);
      SB sb = new SB();
      s = "show " + textBox.getText();
      appendToFile("CMD " + s, sb);
      modelCmd(sb);
    }
    appendOutputWithCaret(s);

  }

  private void loadModel(String path, String fname, String ext) {
    String ess = getEss(ext);
    SB sb = new SB();
    appendToFile("GLOBAL C_PATH " + path + sep, sb);
    appendToFile("GLOBAL C_ESS " + ess + sep, sb);
    appendToFile("GLOBAL C_FNAME " + fname + sep, sb);
    appendToFile("GLOBAL C_IN_EXT " + ext.toLowerCase() + sep, sb);
    appendToFile("CMD use", sb);
    appendOutputWithCaret("use." + ess + " " + fname + "." + ext);
    modelCmd(sb);
  }

  protected void loadModel() {
    File f = new File(new File(nboService.serverPath).getParent()
        + "/jmol_outfile.cfi");
    SB sb = new SB();
    vwr.script(LOAD_SCRIPT);
    try {
      String fileContents = nboService
          .evaluateJmolString("data({visible},'cfi')");
      nboService.writeToFile(fileContents, f);
      setInputFile(f, "cfi", null);
      appendToFile("GLOBAL C_PATH " + f.getParent() + sep, sb);
      appendToFile("GLOBAL C_ESS c" + sep, sb);
      appendToFile("GLOBAL C_FNAME jmol_outfile" + sep, sb);
      appendToFile("GLOBAL C_IN_EXT cfi" + sep, sb);
      appendToFile("CMD use", sb);
      modelCmd(sb);
      enableComps();
    } catch (IOException e) {
      System.out.println("could not write file contents to " + f);
    }
  }

  private void saveModel(String path, String fname, String ext) {
    String ess = getEss(ext);
    SB sb = new SB();
    appendToFile("GLOBAL C_PATH " + path + sep, sb);
    appendToFile("GLOBAL C_ESS " + ess + sep, sb);
    appendToFile("GLOBAL C_FNAME " + fname + sep, sb);
    appendToFile("GLOBAL C_OUT_EXT " + ext + sep, sb);
    appendToFile("CMD save", sb);
    modelCmd(sb);
    appendOutputWithCaret("save." + ess + " " + fname + "\n--Model Saved--");
  }

  private String getEss(String ext) {
    if (ext.equals("cfi") || ext.equals("vfi") || ext.equals("gau")
        || ext.equals("log"))
      return "" + ext.charAt(0);
    else if (ext.equals("47"))
      return "a";
    else if (ext.equals("mm2"))
      return "mm";
    else
      return ext;
  }

  protected synchronized void modelCmd(final SB sb) {
    nboService.queueJob("model", "creating model...", new Runnable() {
      @Override
      public void run() {
        nboService.rawCmdNew("m", sb, false, NBOService.MODE_MODEL);
      }
    });
  }

  /**
   * File opening and saving methods
   * 
   * @param type
   *        of file extension
   */
  @Override
  protected void showWorkpathDialogM(String path, String type) {
    if (path == null) {
      path = usePath;
      type = "47, adf, cfi, gau, gms, jag, log, mp, nw, orc, pqs, qc, vfi";
    }    
    JFileChooser myChooser = new JFileChooser();
    if (type != null)
      myChooser.setFileFilter(new FileNameExtensionFilter(type, type
          .split(", ")));
    myChooser.setFileHidingEnabled(true);
    myChooser.setSelectedFile(new File(path));
    int button = myChooser.showDialog(this, GT._("Select"));
    if (button == JFileChooser.APPROVE_OPTION) {
      jtJmolInput.setText("");
      File newFile = myChooser.getSelectedFile();
      loadModel(newFile.getParent(), getJobStem(newFile), getExt(newFile));
      setInputFile(newFile, getExt(newFile), null);
      nboResetV();
      enableComps();
      this.usePath = newFile.toString();
      saveHistoryM();
    } else if (button == JFileChooser.CANCEL_OPTION) {
      jComboUse.setSelectedIndex(0);
    }
  }

  protected void showSaveDialog(String type) {
    JFileChooser myChooser = new JFileChooser();
    myChooser
        .setFileFilter(new FileNameExtensionFilter(type, type.split(", ")));
    myChooser.setFileHidingEnabled(true);
    myChooser
        .setSelectedFile(new File(new File(savePath).getParent() + "/new"));
    int button = myChooser.showSaveDialog(this);
    if (button == JFileChooser.APPROVE_OPTION) {
      File newFile = myChooser.getSelectedFile();
      String ext = getExt(newFile);
      if (ext.equals(newFile.toString())) {
        String st = jComboSave.getSelectedItem().toString();
        ext = st.substring(st.indexOf("(") + 2, st.indexOf(")"));
      }
      if (PT.isOneOf(ext, INPUT_FILE_EXTENSIONS)) {
        //savePathLabel.setText(newFile.toString());
        setInputFile(newFile, ext, null);
        savePath = newFile.toString();
        saveModel(newFile.getParent(), jobStem, ext);
        saveHistoryM();
      } //else
        //savePathLabel.setText("Invalid extension");
    } else
      jComboSave.setSelectedIndex(0);
  }

  /**
   * enable components after model is loaded
   */
  protected void enableComps() {
    action.setEnabled(true);
    jComboSave.setEnabled(true);
    jbSym.setEnabled(true);
    jCheckAtomNum.setEnabled(true);
  }

  protected void notifyCallbackM(String atomno) {
    if (editMode != 0) {
      String st = nboService.runScriptNow("print {*}[" + atomno + "].selected");
      if (st.contains("1.0")) {
        appendOutputWithCaret("Atom # " + atomno + " deselected");
        selected = selected.replace(atomno + " ", "");
        nboService.runScriptNow("select remove {*}[" + atomno + "]");
        return;
      }
      appendOutputWithCaret("Atom # " + atomno + " selected");
      selected += atomno + " ";
      nboService.runScriptNow("select add {*}[" + atomno + "]");
      int cnt = selected.split(" ").length;
      switch (editMode) {
      case ALTER:
        if (cnt == 1) {
          jtSelectAtoms.setEnabled(true);
          jtSelectAtoms.setText("");
          nboService.runScriptQueued("label %a");
        } else if (cnt == 5) {
          nboService
              .runScriptNow("measure off;select remove {*};select add {*}["
                  + atomno + "]");
          selected = atomno + " ";
        } else {
          if (!jCheckAtomNum.isSelected())
            nboService.runScriptQueued("label off");
          if (cnt == 2)
            //TODO script needs to be run twice for some reason
            nboService.runScriptQueued("measure off;measure " + selected
                + "\"2:%0.4VALUE //A\"" + ";measure " + selected
                + "\"2:%0.4VALUE //A\"");
          else
            nboService.runScriptQueued("measure off;measure " + selected);
        }
        break;
      case CLIP:
        if (cnt == 2)
          jbEdit.setEnabled(true);
        else if (cnt == 3) {
          nboService.runScriptNow("select remove {*};select add {*}[" + atomno
              + "]");
          selected = atomno + " ";
        }
        break;
      case MUTATE:
        if (cnt == 1) {
          jtSelectAtoms.setEnabled(true);
          jtSelectAtoms.setText("");
        }
        if (cnt == 2) {
          nboService.runScriptNow("select remove {*};select add {*}[" + atomno
              + "]");
          selected = atomno + " ";
        }
      }
    }
  }
  
  
  protected boolean helpDialogM(JTextPane p, String key) {
    if (key == null)
      if (action.getSelectedIndex() == 0)
        key = "";
      else
        key = action.getSelectedItem().toString().split(" ")[0].toLowerCase();
    if (key.equals("")) {
      if ((jtJmolInput.hasFocus() || jComboUse.hasFocus())
          && jComboUse.getSelectedIndex() == 0)
        p.setText(showHelp);
      else if (/*savePathLabel.hasFocus() || */jComboSave.hasFocus())
        p.setText(saveHelp);
      else
        p.setText(helpModel + "\n\n" + showHelp + "\n" + useHelp + "\n"
            + symHelp);
    } else if (key.equals("alter")) {
      p.setText(alterHelp);
    } else if (key.equals("clip")) {
      p.setText(clipHelp);
    } else if (key.equals("fuse")) {
      p.setText(fuseHelp);
    } else if (key.equals("link")) {
      p.setText(linkHelp);
    } else if (key.equals("mutate")) {
      p.setText(mutateHelp);
    } else if (key.equals("rebond")) {
      p.setText(rebondHelp);
    } else if (key.equals("switch")) {
      p.setText(switchHelp);
    } else if (key.equals("twist")) {
      p.setText(twistHelp);
    } else if (key.equals("unify")) {
      p.setText(unifyHelp);
    } else if (key.equals("3chb")) {
      p.setText(chbHelp);
    } else if (key.equals("save")) {
      p.setText(saveHelp);
    } else if (key.equals("use")) {
      p.setText(useHelp);
    } else if (key.contains("sym")) {
      p.setText(symHelp);
    } else {
      appendOutputWithCaret("Unkown command type");
      return false;
    }
    return true;
  }

  protected void rawInputM(String cmd) {
    SB sb = new SB();
    appendToFile("CMD " + cmd, sb);
    appendOutputWithCaret(cmd);
    modelCmd(sb);
  }


}
