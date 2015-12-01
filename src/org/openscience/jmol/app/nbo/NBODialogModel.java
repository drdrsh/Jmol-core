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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
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
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jmol.i18n.GT;
import org.openscience.jmol.app.jmolpanel.GuiMap;
import org.openscience.jmol.app.jmolpanel.JmolPanel;

abstract class NBODialogModel extends NBODialogConfig {

  private static final String INPUT_FILE_EXTENSIONS = 
      ";adf;cfi;gau;gms;jag;mm2;mnd;mp;nw;orc;pqs;qc;vfi;g09;com";

  private static final String[] SAVE_OPTIONS = {
    "",
    "JMol Cartesian   [.cfi]",
    "JMol Valence     [.vfi]",
    "Gaussian Input   [.gau]",
    "GAMESS Input     [.gms]",
    "ADF Input        [.adf]", 
    "Jaguar Input     [.jag]", 
    "MM2-Input        [.mm2]",
    "Dewar Type Input [.mnd]",
    "Molpro Input     [.mp]", 
    "NWChem Input     [.nw]",
    "Orca Input       [.orc]", 
    "PQS Input        [.pqs]", 
    "Q-Chem Input     [.qc]"
  };

  private final static int ALTER = 4, CLIP = 2, MUTATE = 1;
  protected final static int VALUE = 5;

  private static final String LOAD_SCRIPT = ";set zoomlarge false;zoomTo 0.5 {*} 0;";
  protected static final int CONFIRM_SAVE = 0, CONFIRM_VIEWOUTPUT = 1;

  protected abstract void nboResetV();

  protected int editMode;
  protected String savePath;
  protected String selected = "";
  
  protected String usePath;
  protected Box editBox;
  protected JTextField jtSelectAtoms, tfFolderS, tfNameS, tfExtS;
  protected JTextField jtJmolInput, jtLineInput;
  protected JButton jbEdit;
  protected static final Color titleColor = new Color(0,128,255);
  
  final static protected Font titleFont = new Font("Arial",Font.BOLD,18);


  protected NBODialogModel(JFrame f) {
    super(f);
  }

  protected void buildModel(Container p) {
    p.removeAll();
    p.setLayout(new BorderLayout());
    if (topPanel == null)
      topPanel = buildTopPanel();
    p.add(topPanel, BorderLayout.PAGE_START);
    JPanel leftPanel = new JPanel();
    leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
    leftPanel.add(useBox());
    leftPanel.add(editBox()).setVisible(false);
    leftPanel.add(saveBox()).setVisible(false);
    JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel,
        modelOut());
    sp.setDividerLocation(350);
    sp.setBorder(BorderFactory.createLoweredBevelBorder());
    p.add(sp, BorderLayout.CENTER);
    p.add(statusPanel, BorderLayout.PAGE_END);
    if (vwr.ms.ac != 0 && !isJmolNBO) {
      loadModel();
      enableComponentsR(this,true);
    }else if(isJmolNBO){
      loadModel(inputFile.getParent(),jobStem,"47");
      tfExt.setText("47");
      enableComponentsR(this,true);
    }
  }
  
  protected String editAction;

  private Component editBox() {
    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));
    JLabel title = new JLabel(" Edit Model ");
    Box box = Box.createVerticalBox();
    title.setBackground(titleColor);
    title.setAlignmentX(0.0f);
    title.setFont(titleFont);
    title.setOpaque(true);
    box.add(title);
    title.setForeground(Color.WHITE);
    box.add(p);
    box.setAlignmentX(0.0f);
    p.setAlignmentX(0.0f);
    p.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    Box b = Box.createVerticalBox();
    final String[] actions = {"Alter","Clip","Fuse","Link", "Mutate", "Switch","Twist"};
    JRadioButton[] btns = new JRadioButton[actions.length];
    ButtonGroup rg = new ButtonGroup();
    for(int i=0;i<actions.length;i++){
      btns[i]=new JRadioButton(actions[i]);
      b.add(btns[i]);
      rg.add(btns[i]);
      final int op = i;
      btns[i].addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        editAction = actions[op].toLowerCase();
        actionSelected(editAction);
        clearSelected();
      }
      });
    }

    p.add(b);
    b = Box.createVerticalBox();
    editBox = Box.createVerticalBox();
    editBox.setBorder(BorderFactory.createLoweredBevelBorder());
    editBox.setMaximumSize(new Dimension(275,200));
    editBox.add(Box.createRigidArea(new Dimension(250, 75)));
    editBox.setAlignmentX(0.5f);
    JButton sym = new JButton("Symmetry?");
    JButton getVal = new JButton("Current Value?");
    getVal.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if(selected.trim().equals(""))
          vwr.alert("Select atoms");
        else {
        String[] ats = selected.split(" ");
        if(ats.length == 1){
          vwr.alert("Atom number: " + nboService.runScriptNow("print {*}["+selected+"].elemno"));
        }else{
          String script = "print measure(";
          for(String x:ats)
            script += "{*}[" + x +"]";
          script += ")";
            vwr.alert(nboService.runScriptNow(script));
        }}
      }
    });
    sym.setAlignmentX(0.5f);
    sym.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        getSymmetry();
      }
    });
    Box box2 = Box.createHorizontalBox();
    box2.add(getVal);
    box2.add(sym);
    b.add(box2);
    b.add(editBox);
    p.add(b);
    jtSelectAtoms = new JTextField("Select atoms...");
    jtSelectAtoms.setMaximumSize(new Dimension(200,30));
    jtSelectAtoms.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        editModel(editAction);
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
    return box;
  }

  private Component saveBox() {
    JPanel p = new JPanel();
    p.setMaximumSize(new Dimension(350,150));
    p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
    Box b = Box.createVerticalBox();
    b.setAlignmentX(0.0f);
    JLabel title = new JLabel(" Save Model ");
    title.setOpaque(true);
    title.setBackground(titleColor);
    title.setAlignmentX(0.0f);
    title.setForeground(Color.white);
    title.setFont(titleFont);
    b.add(title);
    b.add(p);
    p.setAlignmentX(0.0f);
    p.setBorder(BorderFactory.createLineBorder(Color.black));
//    box.add(new JLabel("SAVE ")).setFont(new Font("Arial", Font.BOLD, 25));
    final JComboBox<String> jComboSave = new JComboBox<String>(SAVE_OPTIONS);
    jComboSave.setEnabled(false);
    jComboSave.setFont(nboFont);
    jComboSave.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Object item = jComboSave.getSelectedItem();
        if (jComboSave.getSelectedIndex()!=0) {
          String s = item.toString();
          String ext = s.substring(s.indexOf("[") + 2, s.indexOf("]"));
          tfExtS.setText(ext);
          //showSaveDialog(ext);
        }
      }
    });
    p.add(jComboSave);
    title = new JLabel("File type");
    title.setAlignmentX(0.5f);
    p.add(title);
    p.add(folderSaveBox(jComboSave));
    //p.setPreferredSize(new Dimension(430,50));
    return b;
  }

  /**
   * adds use elements to main panel
   * 
   * @return use elements
   */
  private Component useBox() {
    JPanel p = new JPanel();
    p.setMaximumSize(new Dimension(355,155));
    p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
    JLabel title = new JLabel(" Input Model ");
    title.setAlignmentX(0.0f);
    title.setBackground(titleColor);
    title.setForeground(Color.white);
    title.setFont(titleFont);
    title.setOpaque(true);
    Box box = Box.createVerticalBox();
    box.add(title);
    box.add(p);
    p.setAlignmentX(0.0f);
    box.setAlignmentX(0.0f);
    p.setBorder(BorderFactory.createLineBorder(Color.black));
    Box b = Box.createHorizontalBox();
    final JRadioButton jrJmolIn = new JRadioButton("Jmol Input");
    jrJmolIn.setFont(nboFont);
    jrJmolIn.setSelected(true);
    final JRadioButton jrLineIn = new JRadioButton("Line Formula");
    jrLineIn.setFont(nboFont);
    final JRadioButton jrFileIn = new JRadioButton("File Input");
    jrFileIn.setFont(nboFont);
    ButtonGroup rg = new ButtonGroup();
    rg.add(jrJmolIn);
    rg.add(jrLineIn);
    rg.add(jrFileIn);
    createInput(jtJmolInput = new JTextField(), jrJmolIn);
    createInput(jtLineInput = new JTextField(), jrLineIn);
    jtLineInput.add(new JLabel("line formula"));
    String[] useOps = {"-All File Types-",
        "[.cfi]  JMol Cartesian",
        "[.vfi]  JMol Valence",
        "[.47]   NBO Archive",
        "[.gau]  Gaussian Input",
        "[.log]  Gaussian Output",
        "[.gms]  GAMESS Input",
        "[.adf]  ADF Input", 
        "[.jag]  Jaguar Input", 
        "[.mm2]  MM2-Input",
        "[.mnd]  Dewar Type Input",
        "[.mp]   Molpro Input", 
        "[.nw]   NWChem Input",
        "[.orc]  Orca Input", 
        "[.pqs]  PQS Input", 
        "[.qc]   Q-Chem Input"};
    final JComboBox<String> jComboUse = new JComboBox<String>(useOps);
    jComboUse.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            Object item = jComboUse.getSelectedItem();
            String tmp = tfExt.getText();
            if (jComboUse.getSelectedIndex()!=0) {
              String s = item.toString();
              s = s.substring(s.indexOf("[") + 2, s.indexOf("]"));
              tfExt.setText(s);
              if(!showWorkpathDialogM(usePath,s))
                tfExt.setText(tmp);
            }else{
              tfExt.setText("");
              if(!showWorkpathDialogM(null,null))
                tfExt.setText(tmp);
            }
          }
        });
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
    p.add(new JLabel("              file type"));
    p.add(folderBox());
    return box;
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
  }

  private JPanel folderSaveBox(final JComboBox<String> cBox) {
    JPanel b = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.BOTH;
    (tfFolderS = new JTextField()).setPreferredSize(new Dimension(130, 20));
    tfFolderS.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        showSaveDialog(tfExt.getText().equals("")?"cfi,vfi,47,gau,log,gms,adf,jag,mm2,mnd,mp,nw,orc,pqs,qc":tfExt.getText());
      }
    });
    b.add(tfFolderS, c);
    tfFolderS.setText(savePath.substring(0,savePath.lastIndexOf("\\")));
    c.gridx = 1;
    (tfNameS = new JTextField()).setPreferredSize(new Dimension(100, 20));
    tfNameS.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        showSaveDialog(tfExt.getText().equals("")?"cfi,vfi,47,gau,log,gms,adf,jag,mm2,mnd,mp,nw,orc,pqs,qc":tfExt.getText());
      }
    });
    b.add(tfNameS, c);
    c.gridx = 0;
    c.gridy = 1;
    b.add(new JLabel("         folder"), c);
    c.gridx = 1;
    b.add(new JLabel("          name"), c);
    c.gridx = 2;
    c.gridy = 0;
    (tfExtS = new JTextField()).setPreferredSize(new Dimension(40, 20));
    tfExtS.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        showSaveDialog(!tfExtS.getText().equals("")?tfExtS.getText():"cfi,vfi,47,gau,log,gms,adf,jag,mm2,mnd,mp,nw,orc,pqs,qc");
      }
    });
    b.add(tfExtS, c);
    c.gridy = 1;
    b.add(new JLabel("  ext"), c);
    c.gridx = 3;
    c.gridy = 0;
    c.gridheight = 2;
    JButton btn = new JButton("Save");
    btn.setEnabled(jmolAtomCount>0);
    b.add(btn, c);
    btn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if(tfNameS.getText().trim().equals("")||tfExtS.getText().equals("")||cBox.getSelectedIndex()==0){
          vwr.alert("Enter name and valid extension");
          return;
        }
        showSaveDialog(!tfExtS.getText().equals("")?tfExtS.getText():"cfi,vfi,47,gau,log,gms,adf,jag,mm2,mnd,mp,nw,orc,pqs,qc");
      }
    });
    //b.setPreferredSize(new Dimension(350, 50));
    return b;
  }

  /**
   * edit action selected
   * @param selected 
   */
  protected void actionSelected(String selected) {
    //nboService.runScriptQueued("select remove {*}; select on");
    String item = selected.split(" ")[0];
    int cnt = selected.split(" ").length;
    if (item.equals("-Action-")) {
      return;
    } else if (item.equals("clip") || item.equals("fuse")
        || item.equals("link") || item.equals("switch")) {
      editMode = CLIP;
      if(cnt > 2) selected = "";
      clip(item,"Select two atoms:", null);
    } else if (item.equals("mutate")) {
      if(cnt>1){
        selected = "";
        showSelected(selected.split(" "));
      }
      editMode = MUTATE;
      clip(item,"Formula: ", jtSelectAtoms);
    } else if (item.equals("rebond")) {
      if(cnt > 2) selected = "";
      clip(item,"Symtype: ", jtSelectAtoms);
    } else if (item.equals("alter") || item.equals("twist") || item.equals("value")) {
      editMode = ALTER;
      clip(item,"New Value: ", jtSelectAtoms);
    }
  }
  
  protected JTextField currVal;
  protected JTextField[] atomNumBox;
  protected JLabel valLab;

  protected void clip(final String action, String st, Component c) {
    editBox.removeAll();
    Box box = Box.createHorizontalBox();
    jbEdit = new JButton("Apply");
    jbEdit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        editModel(action);
      }
    });
    box.add(new JLabel(st));
      Box b2 = Box.createHorizontalBox();
      b2.add(new JLabel("Atoms: "));
      atomNumBox = new JTextField[editMode];
      //editMode encodes number of atoms that can be selected in each mode
      for(int i=0;i<editMode;i++){
        atomNumBox[i]=new JTextField();
        final int num = i;
        atomNumBox[i].addFocusListener(new FocusListener(){
          @Override
          public void focusGained(FocusEvent arg0) {
            if(num==editMode-1){
              jbEdit.setEnabled(true);
            }
          }
          @Override
          public void focusLost(FocusEvent arg0) {
            int atnum = 0;
            try{
              atnum = Integer.parseInt(atomNumBox[num].getText());
            }catch(Exception e){
              return;
            }
            if(atnum>jmolAtomCount){
              atomNumBox[num].setText("");
            }
            String[] tmp = new String[editMode];
            selected = "";
            for(int j = 0; j<atomNumBox.length;j++){
              tmp[j] = atomNumBox[j].getText();
              selected += (tmp[j].length()>0?tmp[j]+" ":"");
            }
            //getValue();
            showSelected(selected.split(" "));
            jtSelectAtoms.setText("");
            jtSelectAtoms.setEnabled(true);
          }
        });
        atomNumBox[i].addActionListener(new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent e){
          String[] tmp = new String[editMode];
          selected = "";
          for(int j = 0; j<atomNumBox.length;j++){
            tmp[j] = atomNumBox[j].getText();
            selected += (tmp[j].length()>0?tmp[j]+" ":"");
          }
          editModel(action);
        }
        });
        b2.add(atomNumBox[i]).setMaximumSize(new Dimension(40,40));
      }
      editBox.add(b2);
      if(selected.equals("")){
        jtSelectAtoms.setText("Select atoms...");
        jtSelectAtoms.setEnabled(false);
      }else{
        jtSelectAtoms.setText("");
        jtSelectAtoms.setEnabled(true);
      }
    if (editMode == ALTER){
      b2 = Box.createHorizontalBox();
      b2.add(new JLabel("Current Value"));
      b2.add(currVal = new JTextField()).setMaximumSize(new Dimension(200,40));
      editBox.add(b2);
      editBox.add(valLab = new JLabel(" "));
    }
    if (c != null) {
      box.add(c);
    } 
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
    for(int i=0;i<editMode;i++)
      atomNumBox[i].setText("");
    if(currVal != null)
      currVal.setText("");
    if(valLab != null)
      valLab.setText(" ");
    if (!jCheckAtomNum.isSelected())
      nboService.runScriptQueued("label off");
    nboService.runScriptQueued("measure off;select remove {selected};refresh");
    selected = "";
    jtSelectAtoms.setText("Select atoms...");
    jtSelectAtoms.setEnabled(false);
    jbEdit.setEnabled(false);
  }

  /**
   * apply edit to model
   * @param item 
   */
  protected void editModel(String item) {
    SB sb = new SB();
    String cmd = item + " "
        + selected;
    if (jtSelectAtoms != null)
      if(editMode == ALTER || editMode == MUTATE)
        cmd += " " + jtSelectAtoms.getText();
    appendToFile("CMD " + cmd, sb);
    appendOutputWithCaret(cmd,'i');
    //jbEdit.setEnabled(false);
    modelCmd(sb);
    clearSelected();
  }

  protected JPanel modelOut() {
    JPanel s = new JPanel();
    s.setLayout(new BorderLayout());
    JLabel lab = new JLabel("Session Dialog");
    lab.setFont(nboFont);
    s.add(lab, BorderLayout.PAGE_START);
    JScrollPane p1 = new JScrollPane();
    if(jpNboOutput == null){
    jpNboOutput = new JTextPane();
    jpNboOutput.setEditable(false);
    jpNboOutput.setFont(new Font("Arial", Font.PLAIN, 16));
    bodyText = "";
    }
    jpNboOutput.setContentType("text/html");
    setComponents(s,Color.WHITE,Color.BLACK);
    p1.getViewport().add(jpNboOutput);
    s.add(p1, BorderLayout.CENTER);
    JPanel box = new JPanel(new GridLayout(4,1));
    JButton b = new JButton("Clear");
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        clearOutput();
      }
    });
    box.add(b);
    box.add(jCheckAtomNum);
    box.add(jCheckNboView);
    s.add(box, BorderLayout.SOUTH);
    return s;
  }
  
  protected void saveHistoryM() {
    java.util.Properties props = new java.util.Properties();
    props.setProperty("savePath", savePath);
    props.setProperty("usePath", usePath);
    JmolPanel.historyFile.addProperties(props);
  }

  protected void getSymmetry() {
    //jbSym.setSelected(false);
    editMode = 0;
    SB sb = new SB();
    appendToFile("CMD symmetry", sb);
    appendOutputWithCaret("Symmetry: ",'p');
    modelCmd(sb);
  }

  protected void getModel(JTextField textBox) {
    clearModel();
    if (textBox.getText().equals(""))
      return;
    //action.setSelectedIndex(0);
    String s = "";
    //clearInputFile();
    if (textBox.equals(jtJmolInput)) {
      //nboService.runScriptNow("zap");
      s = "load $" + (jobStem = textBox.getText());
      appendOutputWithCaret(s,'i');
      if (nboService.runScriptNow(s) == null) {
        if (nboService.runScriptNow("load :" + textBox.getText()) != null) {
//          while (vwr.ms.ac == 0)
//            try {
//              Thread.sleep(10);
//            } catch (Exception e) {
//              return;
//            }
         // loadModel();
          return;
        }
        appendOutputWithCaret("File not found",'i');
        return;
      }
    } else if (textBox.equals(jtLineInput)) {
      SB sb = new SB();
      s = "show " + (jobStem=textBox.getText());
      appendToFile("CMD " + s, sb);
      modelCmd(sb);
    }
    enableComponentsR(this, true);
    appendOutputWithCaret(s,'i');

  }

  private void loadModel(String path, String fname, String ext) {
    clearModel();
    String ess = getEss(ext);
    SB sb = new SB();
    appendToFile("GLOBAL C_PATH " + path + sep, sb);
    appendToFile("GLOBAL C_ESS " + ess + sep, sb);
    appendToFile("GLOBAL C_FNAME " + fname + sep, sb);
    appendToFile("GLOBAL C_IN_EXT " + ext.toLowerCase() + sep, sb);
    appendToFile("CMD use", sb);
    appendOutputWithCaret("use." + ess + " " + fname + "." + ext,'i');
    modelCmd(sb);
  }
  
  protected void clearModel(){
    this.tfName.setText("");
    this.tfExt.setText("");
    this.tfExtS.setText("");
    this.tfNameS.setText("");
  }

  protected void loadModel() {
    clearModel();
    File f = new File(new File(nboService.serverPath).getParent()
        + "/jmol_outfile.gau");
    SB sb = new SB();
      vwr.script(LOAD_SCRIPT);
    try {
      //TODO this no longer works with new build
      String fileContents = nboService
          .evaluateJmolString("data({visible},'gau')");
      nboService.writeToFile(fileContents, f);
      //setInputFile(f, "cfi", null);
      appendToFile("GLOBAL C_PATH " + f.getParent() + sep, sb);
      appendToFile("GLOBAL C_ESS g" + sep, sb);
      appendToFile("GLOBAL C_FNAME jmol_outfile" + sep, sb);
      appendToFile("GLOBAL C_IN_EXT gau" + sep, sb);
      appendToFile("CMD use", sb);
      modelCmd(sb);
      enableComponentsR(this,true);
      
    } catch (IOException e) {
      System.out.println("could not write file contents to " + f);
    }
  }

  protected void saveModel(String path, String fname, String ext) {
    String ess = getEss(ext);
    SB sb = new SB();
    appendToFile("GLOBAL C_PATH " + path + sep, sb);
    appendToFile("GLOBAL C_ESS " + ess + sep, sb);
    appendToFile("GLOBAL C_FNAME " + fname + sep, sb);
    appendToFile("GLOBAL C_OUT_EXT " + ext + sep, sb);
    appendToFile("CMD save", sb);
    modelCmd(sb);
    appendOutputWithCaret("save." + ess + " " + fname,'i');
    appendOutputWithCaret("--Model Saved--\n"+path+"\\"+fname+"."+ext,'b');
    tfFolderS.setText(path);
    tfNameS.setText(fname);
    tfExtS.setText(ext);
  }

  private String getEss(String ext) {
    ext = ext.toLowerCase();
    if (ext.equals("cfi") || ext.equals("vfi") || ext.equals("gau")
        || ext.equals("log") || ext.equals("g09")) 
      return "" + ext.charAt(0);
    else if (ext.equals("47"))
      return "a";
    else if (ext.equals("mm2"))
      return "mm";
    else if (ext.equals("com"))
      return "g";
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
  protected boolean showWorkpathDialogM(String path, String type) {
    if (path == null) {
      path = usePath;
      type = "47,adf,cfi,com,gau,g09,gms,jag,log,mp,nw,orc,pqs,qc,vfi";
    } 
    String folder = tfFolder.getText().trim();
    if(!folder.equals("")){
      if(!folder.contains(":")) folder = "C:/"+folder;
      path = folder + "/"+(tfName.getText().equals("") ? "new":tfName.getText())+"."+tfExt.getText();
    }
    if(!tfExt.getText().equals("")) 
      type = tfExt.getText();
    JFileChooser myChooser = new JFileChooser();  
    if (type != null)
      myChooser.setFileFilter(new FileNameExtensionFilter(type, type
          .split(",")));
    myChooser.setFileHidingEnabled(true);
    myChooser.setSelectedFile(new File(path));
    int button = myChooser.showDialog(this, GT._("Select"));
    if (button == JFileChooser.APPROVE_OPTION) {
      jtJmolInput.setText("");
      File newFile = myChooser.getSelectedFile();
      if(newFile.toString().indexOf(".")<0){
        appendOutputWithCaret("File not found",'i');
        return false;
      }
      loadModel(newFile.getParent(), getJobStem(newFile), getExt(newFile));
      setInputFile(newFile, getExt(newFile), null);
      nboResetV();
      this.usePath = newFile.toString();
      saveHistoryM();
      enableComponentsR(this,true);
      return true;
    }
    return false;
  }

  protected void showSaveDialog(String type) {
    JFileChooser myChooser = new JFileChooser();
    myChooser.setFileFilter(new FileNameExtensionFilter(type, type.split(",")));
    myChooser.setFileHidingEnabled(true);
    String savePath = this.savePath;
    String folder = tfFolderS.getText().trim();
    if(!folder.equals("")){
      if(!folder.contains(":")) 
        folder = "C:/"+folder;
    }else
      folder = new File(this.savePath).getParent();
    System.out.println("-----"+folder+this.savePath);
    if(tfNameS.getText().equals(""))
      savePath = folder + "/"+(jobStem.equals("") ? "new.cfi":jobStem+"."+tfExtS.getText());
    else
      savePath = folder + "/"+tfNameS.getText()+"."+tfExtS.getText();
    myChooser.setSelectedFile(new File(savePath));
    int button = myChooser.showSaveDialog(this);
    if (button == JFileChooser.APPROVE_OPTION) {
      File newFile = myChooser.getSelectedFile();
      String ext = getExt(newFile);
      if(newFile.exists()) showConfirmationDialog("File already exists, overwrite file?",newFile,ext,CONFIRM_SAVE);
      else if (PT.isOneOf(ext, INPUT_FILE_EXTENSIONS)) {
        this.savePath = newFile.toString();
        saveModel(newFile.getParent(), getJobStem(newFile), ext);
        saveHistoryM();
      }else appendOutputWithCaret("No valid extension defined",'b');
    }
  }

  @Override
  protected void showConfirmationDialog(String st, final File newFile, final String ext, final int mode){
    final JDialog d = new JDialog();
    int i = JOptionPane.showConfirmDialog(this, st, "Message", JOptionPane.YES_NO_OPTION);
    if(i==JOptionPane.YES_OPTION){
        if(mode == CONFIRM_SAVE){
          savePath = newFile.toString();
          saveModel(newFile.getParent(), getJobStem(newFile), ext);
          saveHistoryM();
          d.setVisible(false);
        }else if(mode == CONFIRM_VIEWOUTPUT){
          d.setVisible(false);
          showNboOutput(newFile.toString());
        }else if(mode == 2){
          d.setVisible(false);
          goRunClicked("PLOT","gennbo",newFile,showRunDone);
        }
      }
  }

  protected void notifyCallbackM(String atomno) {
    if (editMode != 0) {
      jtSelectAtoms.requestFocus();
      jtSelectAtoms.setText("");
      jtSelectAtoms.setEnabled(true);
      String st = nboService.runScriptNow("print {*}[" + atomno + "].selected");
      if (st.contains("1.0")) {
        appendOutputWithCaret("Atom # " + atomno + " deselected",'i');
        selected = selected.replace(atomno + " ", "");
        nboService.runScriptNow("select remove {*}[" + atomno + "];measure off;measure "+selected);
        for(int i=0;i<atomNumBox.length;i++)
          if(i>=selected.split(" ").length)
            atomNumBox[i].setText("");
          else
            atomNumBox[i].setText(selected.split(" ")[i]);
        return;
      }
      int cnt = (selected.equals("")?1:selected.split(" ").length + 1);
      switch (editMode) {
      case ALTER:
        String [] ats = selected.split(" ");
        String script;
        if(cnt == 1){
          valLab.setText("(Atomic number)");
          jtSelectAtoms.setText("");
          jtSelectAtoms.setEnabled(true);
          script = "print {*}["+atomno+"].elemno";
          currVal.setText(nboService.runScriptNow(script));
        }else if (cnt == 5) {
          clearSelected();
          valLab.setText("(Atomic number)");
          script = "print {*}["+atomno+"].elemno";
          currVal.setText(nboService.runScriptNow(script));
          cnt=1;
        } else{
          if (!jCheckAtomNum.isSelected())
            nboService.runScriptQueued("label off");
          if (cnt == 2){
            valLab.setText("(Bond length)");
            //TODO script needs to be run twice for some reason
            nboService.runScriptQueued("measure off;measure " + selected + " " + atomno
                + "\"2:%0.4VALUE //A\"" + ";measure " + selected + " " + atomno
                + "\"2:%0.4VALUE //A\"");
          }
          else{
            nboService.runScriptQueued("measure off;measure " + selected + " " + atomno);
            if(cnt == 3) valLab.setText("(Valence angle)");
            else if(cnt == 4) valLab.setText("(Dihedral angle)");
          }
          script = "print measure({*}[";
          for(String x:ats)
            script += x +"],{*}[";
          script += atomno+"])";
          currVal.setText(nboService.runScriptNow(script).split("\\s+")[1]);
        }
        break;
      case CLIP:
        if (cnt == 2)
          jbEdit.setEnabled(true);
        else if (cnt == 3) {
          clearSelected();
          cnt = 1;
        }
        break;
      case MUTATE:
        if (cnt == 1) {
          jtSelectAtoms.setEnabled(true);
          jtSelectAtoms.setText("");
        }
        if (cnt == 2) {
          clearSelected();
          cnt = 1;
        }
      }
      nboService.runScriptQueued("select add {*}[" + atomno + "]");
      selected += atomno + " ";
      atomNumBox[cnt-1].setText("  "+atomno);
    }
  }
  
  protected void getValue(){
    String script = "";
    String [] ats = selected.split(" ");
    int cnt = ats.length;
    if(cnt == 1){
      valLab.setText("(Atomic number)");
      jtSelectAtoms.setText("");
      jtSelectAtoms.setEnabled(true);
      script = "print {*}["+selected+"].elemno";
      currVal.setText(nboService.runScriptNow(script));
    } else{
      if (cnt == 2){
        valLab.setText("(Bond length)");
        //TODO script needs to be run twice for some reason
        nboService.runScriptQueued("measure off;measure " + selected
            + "\"2:%0.4VALUE //A\"" + ";measure " + selected
            + "\"2:%0.4VALUE //A\"");
      }
      else{
        nboService.runScriptQueued("measure off;measure " + selected + " ");
        if(cnt == 3) valLab.setText("(Valence angle)");
        else if(cnt == 4) valLab.setText("(Dihedral angle)");
      }
      script = "print measure({*}[";
      for(int i = 0; i<ats.length-1; i++)
        script += ats[i] +"],{*}[";
      script += ats[ats.length-1]+"]);";
      String s = nboService.runScriptNow(script);
      
      currVal.setText(s.split("\\s+")[1]);
    }
  }
  
  protected void notifyLoadModel(){
    if(nboView){
      String s2 = nboService.runScriptNow("print {*}.bonds");
      nboService.runScriptQueued("select "+s2+";color bonds lightgrey; wireframe 0.1");
    }
    if(jCheckAtomNum.isSelected()) showAtomNums();
    jmolAtomCount = vwr.ms.ac;
    nboService.runScriptQueued("select on");
    if(selected.equals("")) return;
    showSelected(selected.split(" "));
    if(editMode == ALTER){
      int cnt = selected.split(" ").length;
      if(cnt == 1) return;
      if(cnt == 2)
        //TODO script needs to be run twice for some reason
        nboService.runScriptQueued("measure off;measure " + selected
            + "\"2:%0.4VALUE //A\"" + ";measure " + selected
            + "\"2:%0.4VALUE //A\"");
      else
        nboService.runScriptQueued("measure off;measure " + selected);
    }
  }
  
  protected boolean helpDialogM(JTextPane p, String key) {
//    if (key == null)
//      if (action.getSelectedIndex() == 0)
//        key = "";
//      else
//        key = action.getSelectedItem().toString().split(" ")[0].toLowerCase();
    if (key.equals("")) {
      if (jtJmolInput.hasFocus())
        p.setText(showHelp);
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
      appendOutputWithCaret("Unkown command type",'p');
      return false;
    }
    return true;
  }

  protected void rawInputM(String cmd) {
    SB sb = new SB();
    String [] tokens = cmd.split(" ");
    if(tokens.length==0) return;
    cmd = tokens[0].toLowerCase();
    if(cmd.startsWith("al")){
      cmd = "alter";
    }else if(cmd.startsWith("sh")){
      cmd = "show";
      enableComponentsR(this,true);
    }else if(cmd.startsWith("cl")){
      cmd = "clip";
    }else if(cmd.startsWith("mu")){
      cmd = "mutate";
    }else if(cmd.startsWith("fu")){
      cmd = "fuse";
    }else if(cmd.startsWith("sy")){
      cmd = "symmetry";
      editMode = 0;
    }else if(cmd.startsWith("sw")){
      cmd = "switch";
    }else if(cmd.startsWith("li")){
      cmd = "link";
    }else if(cmd.startsWith("re")){
      cmd = "rebond";
    }else if(cmd.startsWith("save")){
      //TODO
    }else if(cmd.startsWith("va")){
      cmd = "value";
      editMode = VALUE;
    }else if(cmd.startsWith("tw")){
      cmd = "twist";
    }else if(cmd.startsWith("ro")){
      cmd = "rotate";
    }
    for(int i = 1; i<tokens.length; i++){
      cmd += " " + tokens[i];
    }
    appendToFile("CMD " + cmd, sb);
    appendOutputWithCaret(cmd,'i');
    modelCmd(sb);
  }


}
