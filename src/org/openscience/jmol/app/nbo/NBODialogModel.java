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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javajs.util.SB;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jmol.i18n.GT;
import org.openscience.jmol.app.jmolpanel.JmolPanel;


abstract class NBODialogModel extends NBODialogConfig {
  protected abstract void nboResetV();
  private String savePath;
  String usePath;
  protected JComboBox<String> use, saveBox;
  protected Box editBox;
  protected JButton edit,sym;
  protected JTextField nv, tfFolderS, tfNameS, tfExtS;
  protected JCheckBox atNum;
  protected JRadioButton jmolIn, formula, fileIn;
  private int editMode;
  protected JTextField workPathLabel;
  private final int ALTER = 1, 
                     CLIP = 2, 
                   MUTATE = 3;

  protected NBODialogModel(JFrame f) {
    super(f);
  }

  protected void buildModel(Container p) {
    java.util.Properties props = JmolPanel.historyFile.getProperties();
    savePath = (props.getProperty("savePath", System.getProperty("user.home")));
    usePath = (props.getProperty("usePath", System.getProperty("user.home")));
    p.removeAll();
    p.setLayout(new BorderLayout());
    if(topPanel==null) topPanel = buildTopPanel();
    p.add(topPanel,BorderLayout.PAGE_START);
    JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,modelS(),modelOut());
    sp.setDividerLocation(430);
    sp.setBorder(BorderFactory.createLoweredBevelBorder());
    p.add(sp, BorderLayout.CENTER);
    p.add(statusPanel, BorderLayout.PAGE_END);
  }
  
  /**
   * model selection panel
   * 
   * @return model select panel
   */
  protected JPanel modelS() {
    JPanel modelPanel = new JPanel();
    modelPanel.setLayout(new BoxLayout(modelPanel, BoxLayout.Y_AXIS));
    ////use////////
    modelPanel.add(useBox());
    Box box = Box.createVerticalBox();
    String[] useOps = {"-Type-","(.47) NBOarchive ", "(.adf) ADF ", "(.cfi) Cartesian Coordinate","(.gau) Gaussian",
        "(.gms) GAMESS", "(.jag) Jaguar", "(.log) Gaussian log", "(.mp) Molpro","(.nw) NWChem", "(.orc) Orca", 
        "(.pqs) PQS", "(.qc) Q-Chem","(.vfi) Valence Coordinate"};
    (use = new JComboBox<String>(useOps)).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Object item = use.getSelectedItem();
        if (!item.equals("-Type-")) {
          String s = item.toString();
          showWorkpathDialogM(usePath,s.substring(s.indexOf("(")+2, s.indexOf(")")));
        }
      }
    });
    use.setEnabled(false);
    browse.setEnabled(false);
    box.add(use);
    box.add(folderBox());
    box.setBorder(BorderFactory.createEmptyBorder(0, 75, 10, 5));
    modelPanel.add(box);
    modelPanel.add(new JSeparator());
    //save////////
    box = Box.createHorizontalBox();
    box.add(new JLabel("SAVE ")).setFont(new Font("Arial",Font.BOLD,25));
    String[] saveOps = {"-Type-","(.adf) ADF ", "(.cfi) Cartesian Coordinate","(.gau) Gaussian",
        "(.gms) GAMESS", "(.jag) Jaguar", "(.mm2) MM2-type","(.mnd) MINDO/AM1","(.mp) Molpro", "(.nw) NWChem",
        "(.orc) Orca", "(.pqs) PQS", "(.qc) Q-Chem","(.vfi) Valence Coordinate"};
    (saveBox = new JComboBox<String>(saveOps)).setEnabled(false);
    saveBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Object item = saveBox.getSelectedItem();
        if (!item.equals("-Type-")) {
          String s = item.toString();
          showSaveDialog(s.substring(s.indexOf("(")+2, s.indexOf(")")));
        }
      }
    });
    Box b2 = Box.createVerticalBox();
    b2.add(saveBox);
    b2.add(folderSaveBox());
    box.add(b2);
    box.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 5));
    modelPanel.add(box);
    modelPanel.add(new JSeparator());
    //edit//////
    box = Box.createHorizontalBox();
    box.add(new JLabel("EDIT  ")).setFont(new Font("Arial",Font.BOLD,25));
    String[] actions = { "-Action-", "alter - atomic charge, distance, angle, or dihedral angle", "clip - ", "fuse - ",
        "link - ", "mutate - ", "rebond - ", "switch - ", "twist - ",
        "unify - ", " 3chb - " };
    (action = new JComboBox<String>(actions)).setEnabled(false);
    box.add(action);
    action.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        actionSelected();
      }
    });
    box.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 5));
    modelPanel.add(box).setMaximumSize(new Dimension(500, 60));
    modelPanel.add(box);
    
    editBox = Box.createVerticalBox();
    editBox.setBorder(BorderFactory.createLoweredBevelBorder());
    editBox.add(Box.createRigidArea(new Dimension(200,60)));
    nv = new JTextField("Select atoms...");
    nv.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        editModel();
      }
    });
    nv.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void changedUpdate(DocumentEvent arg0) {}
      @Override
      public void insertUpdate(DocumentEvent arg0) {
        if (!nv.getText().equals(""))
          edit.setEnabled(true);
      }
      @Override
      public void removeUpdate(DocumentEvent arg0) {
        if (nv.getText().equals(""))
          edit.setEnabled(false);
      }
    });
    (edit = new JButton("Apply")).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        editModel();
      }
    });
    modelPanel.add(editBox);
    modelPanel.add(new JSeparator());
    sym = new JButton("Symmety");
    box = Box.createVerticalBox();
    box.setAlignmentX(0.5f);
    box.setBorder(BorderFactory.createEmptyBorder(10,0,20,5));
    sym.setAlignmentX(0.5f);
    box.add(sym).setEnabled(false);
    sym.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        getSymmetry();
      }
    });
    box.add(Box.createRigidArea(new Dimension(30,30)));
    atNum = new JCheckBox("View atom numbers");
    atNum.setEnabled(false);
    atNum.setAlignmentX(0.5f);
    atNum.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if(!atNum.isSelected())
          nboService.runScriptQueued("select {*};label off");
        else
          nboService.runScriptQueued("select {*};label %a");
        nboService.runScriptQueued("color labels white;select remove {*}");
      }
    });
    box.add(atNum);
    modelPanel.add(box);
    return modelPanel;
  }
  
  /**
   * adds use elements to main panel
   * @return use elements
   */
  protected Box useBox(){
    Box b = Box.createHorizontalBox();
    b.add(new JLabel("USE ")).setFont(new Font("Arial",Font.BOLD,25));
    (jmolIn = new JRadioButton("Jmol Input")).setFont(nboFont);
    jmolIn.setSelected(true);
    jmolIn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        formula.setSelected(false);
        fileIn.setSelected(false);
        jmolIn.setSelected(true);
        browse.setEnabled(false);
        use.setEnabled(false);
        workPathLabel.setEnabled(true);
      }
    });
    (formula = new JRadioButton("Line Formula")).setFont(nboFont);
    formula.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        jmolIn.setSelected(false);
        fileIn.setSelected(false);
        formula.setSelected(true);
        browse.setEnabled(false);
        use.setEnabled(false);
        workPathLabel.setEnabled(true);
      }
    });
    (fileIn = new JRadioButton("File Input")).setFont(nboFont);
    fileIn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        jmolIn.setSelected(false);
        formula.setSelected(false);
        fileIn.setSelected(true);
        use.setEnabled(true);
        browse.setEnabled(true);
        workPathLabel.setEnabled(false);
      }
    });
    Box box = Box.createVerticalBox();
    box.add(jmolIn);
    box.add(formula);
    box.add(fileIn);
    b.add(box);
    (workPathLabel = new JTextField()).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        generateClicked();
      }
    });
    workPathLabel.setMinimumSize(new Dimension(226,25));
    workPathLabel.setMaximumSize(new Dimension(226,25));
    b.add(workPathLabel);
    return b;
  }
  
  protected JPanel folderSaveBox() {
    JPanel b = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.gridx=0;
    c.gridy=0;
    c.fill=GridBagConstraints.BOTH;
    (tfFolderS = new JTextField()).setPreferredSize(new Dimension(130,20));
    tfFolderS.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        //browse.setSelected(true)
        showWorkpathDialogM(tfFolder.getText()+"/new",null);
      }
    });
    b.add(tfFolderS,c);
    c.gridx=1;
    (tfNameS = new JTextField()).setPreferredSize(new Dimension(100,20));
    b.add(tfNameS,c);
    c.gridx=0;
    c.gridy=1;
    b.add(new JLabel("         folder"),c);
    c.gridx=1;
    b.add(new JLabel("          name"),c);
    c.gridx=2;
    c.gridy=0;
    (tfExtS = new JTextField()).setPreferredSize(new Dimension(40,20));
    b.add(tfExtS,c);
    c.gridy=1;
    b.add(new JLabel("  ext"),c);
    c.gridx=3;
    c.gridy=0;
    c.gridheight=2;
    JButton btn = new JButton("Save");
    b.add(btn,c);
    b.setPreferredSize(new Dimension(350, 50));
    return b;
  }
  
  /**
   * edit action selected
   */
  protected void actionSelected(){
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
      clip("Formula: ",nv);
    } else if (item.equals("rebond")) {
      clip("Symtype: ",nv);
    } else if (item.equals("alter") || item.equals("twist")) {
      editMode = ALTER;
      clip("New Value: ",nv);
    }
    setComponents(editBox);
  }

  protected void clip(String st, Component c) {
    editBox.removeAll();
    editBox.setMaximumSize(new Dimension(200,60));
    Box box = Box.createHorizontalBox();
    box.add(new JLabel(st));
    if(c!=null){
      nv.setText("Select atoms...");    
      box.add(c);
      nv.setEnabled(false);
    }else nv.setText("");
    editBox.add(box);
    box = Box.createHorizontalBox();
    JButton bu = new JButton ("Clear Selected");
    bu.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        clearSelected();
      }
    });
    box.add(bu);
    box.add(edit);
    editBox.add(box);
    editBox.repaint();
    editBox.revalidate();
  }
  
  protected void clearSelected(){
    if(!atNum.isSelected())
      nboService.runScriptQueued("label off");
    nboService.runScriptQueued("measure off;select remove {selected};refresh");
    selected = "";
    nv.setText("Select atoms...");
    nv.setEnabled(false);
    appendModelOutPanel("Selection cleared");
  }

  /**
   * apply edit to model
   */
  protected void editModel() {
    SB sb = new SB();
    String cmd = action.getSelectedItem().toString().split(" ")[0] + " "
        + selected;
    selected = "";
    if(nv != null) cmd += " " + nv.getText();
    appendToFile("CMD " + cmd, sb);
    nboOutput.setText(nboOutput.getText() + cmd + "\n");
    edit.setEnabled(false);
    modelCmd(sb);
  }
  
  protected JPanel modelOut() {
    JPanel s = new JPanel();
    s.setLayout(new BorderLayout());
    JLabel lab = new JLabel("Session Dialog");
    lab.setFont(nboFont);
    s.add(lab,BorderLayout.PAGE_START);
    JScrollPane p1 = new JScrollPane();
    nboOutput = new JTextPane();
    nboOutput.setEditable(false);
    nboOutput.setFont(new Font("Arial",Font.PLAIN,18));
    p1.getViewport().add(nboOutput);
    s.add(p1, BorderLayout.CENTER);
    JButton b = new JButton("Clear");
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        clearOutput();
      }
    });
    s.add(b,BorderLayout.SOUTH);
    return s;
  }

  protected void saveHistoryM() {
    java.util.Properties props = new java.util.Properties();
    props.setProperty("savePath", savePath);
    props.setProperty("usePath", usePath);
    JmolPanel.historyFile.addProperties(props);
  }

  protected void getSymmetry() {
    sym.setSelected(false);
    SB sb = new SB();
    appendToFile("CMD symmetry", sb);
    appendModelOutPanel("Symmetry: ");
    modelCmd(sb);
  }

  protected void generateClicked() {
    if (workPathLabel.getText().equals("")) 
      return;
    action.setSelectedIndex(0);
    String s = "";
    if(jmolIn.isSelected()){
      nboService.runScriptNow("zap");
      s = "load $"+workPathLabel.getText();
      if(nboService.runScriptNow(s)==null){
        if(nboService.runScriptNow("load :"+workPathLabel.getText())!=null){
          while(vwr.ms.ac == 0)
            try {
              Thread.sleep(10);
            } catch (Exception e) {
              System.out.println("HELLO");
              return;
            }
          loadModel();
          return;
        }
        appendModelOutPanel("File not found");
        return;
      }
      while(vwr.ms.ac == 0)
        try {
          Thread.sleep(10);
        } catch (Exception e) {
          System.out.println("HELLO");
          return;
        }
      loadModel();
    }else if(formula.isSelected()){
      SB sb = new SB();
      s = "show "+workPathLabel.getText();
      appendToFile("CMD "+s,sb);
      modelCmd(sb);
    }
    appendModelOutPanel(s);
    
  }

  protected void loadModel(String path, String fname, String ext) {
    String ess = getEss(ext);
    SB sb = new SB();
    appendToFile("GLOBAL C_PATH " + path + sep, sb);
    appendToFile("GLOBAL C_ESS " + ess + sep, sb);
    appendToFile("GLOBAL C_FNAME " + fname + sep, sb);
    appendToFile("GLOBAL C_IN_EXT " + ext.toLowerCase() + sep, sb);
    appendToFile("CMD use", sb);
    appendModelOutPanel("use." + ess + " " + fname + "." + ext);
    modelCmd(sb);
  }

  protected void loadModel() {
    File f = new File(new File(nboService.serverPath).getParent() + "/jmol_outfile.cfi");
    SB sb = new SB();
    try {
      String fileContents = nboService.evaluateJmolString("data({visible},'cfi')");
      nboService.writeToFile(fileContents, f);
      setInputFile(f,"cfi",null);
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
  
  protected void saveModel(String path, String fname, String ext) {
    String ess = getEss(ext);
    SB sb = new SB();
    appendToFile("GLOBAL C_PATH " + path + sep, sb);
    appendToFile("GLOBAL C_ESS " + ess + sep, sb);
    appendToFile("GLOBAL C_FNAME " + fname + sep, sb);
    appendToFile("GLOBAL C_OUT_EXT " + ext + sep, sb);
    appendToFile("CMD save", sb);
    modelCmd(sb);
    appendModelOutPanel("save." + ess + " " + fname
        + "\n--Model Saved--");
  }
  
  private String getEss(String ext){
    if(ext.equals("cfi")||ext.equals("vfi")||ext.equals("gau")||ext.equals("log"))
      return "" + ext.charAt(0);
    else if(ext.equals("47"))
      return "a";
    else if(ext.equals("mm2"))
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
   * @param type of file extension
   */
  @Override
  protected void showWorkpathDialogM(String usePath,String type) {
    JFileChooser myChooser = new JFileChooser();
    if(type!=null) myChooser.setFileFilter(new FileNameExtensionFilter(type, type.split(", ")));
    myChooser.setFileHidingEnabled(true);
    myChooser.setSelectedFile(new File(usePath));
    int button = myChooser.showDialog(this, GT._("Select"));
    if (button == JFileChooser.APPROVE_OPTION) {
      workPathLabel.setText("");
      File newFile = myChooser.getSelectedFile();
      loadModel(newFile.getParent(), getJobStem(newFile), getExt(newFile));
      setInputFile(newFile,getExt(newFile),null);
      nboResetV();
      enableComps();
      this.usePath = newFile.toString();
      saveHistoryM();
    } else if (button == JFileChooser.CANCEL_OPTION) {
      use.setSelectedIndex(0);
    }
  }

  protected void showSaveDialog(String type) {
    JFileChooser myChooser = new JFileChooser();
    myChooser.setFileFilter(new FileNameExtensionFilter(type, type.split(", ")));
    myChooser.setFileHidingEnabled(true);
    myChooser
        .setSelectedFile(new File(new File(savePath).getParent() + "/new"));
    int button = myChooser.showSaveDialog(this);
    if (button == JFileChooser.APPROVE_OPTION) {
      File newFile = myChooser.getSelectedFile();
      String ext = getExt(newFile);
      if (ext.equals(newFile.toString())){
        String st = saveBox.getSelectedItem().toString();
        ext = st.substring(st.indexOf("(")+2,st.indexOf(")"));
      }
      if (ext.equalsIgnoreCase("cfi") || ext.equalsIgnoreCase("gau")
          || ext.equalsIgnoreCase("vfi") || ext.equalsIgnoreCase("gms")
          || ext.equalsIgnoreCase("jag") || ext.equalsIgnoreCase("mm2")
          || ext.equalsIgnoreCase("mnd") || ext.equalsIgnoreCase("mp")
          || ext.equalsIgnoreCase("nw")  || ext.equalsIgnoreCase("orc")
          || ext.equalsIgnoreCase("pqs") || ext.equalsIgnoreCase("qc")) {
        savePathLabel.setText(newFile.toString());
        setInputFile(newFile,ext,null);
        savePath = newFile.toString();
        saveModel(newFile.getParent(), jobStem, ext);
        saveHistoryM();
      } else
        savePathLabel.setText("Invalid extension");
    }else
      saveBox.setSelectedIndex(0);
  }

  /**
   * enable components after model is loaded
   */
  protected void enableComps(){
    action.setEnabled(true);
    saveBox.setEnabled(true);
    sym.setEnabled(true);
    atNum.setEnabled(true);
  }
  
  private String selected = "";

  protected void notifyCallbackModel(String atomno) {
    if(editMode!=0){
      String st = nboService.runScriptNow("print {*}["+atomno+"].selected");
      if(st.contains("1.0")){
        appendModelOutPanel("Atom # " + atomno + " deselected");
        selected = selected.replace(atomno+" ","");
        nboService.runScriptNow("select remove {*}["+atomno+"]");
        return;
      }
      appendModelOutPanel("Atom # " + atomno + " selected");
      selected += atomno + " ";
      nboService.runScriptNow("select add {*}["+atomno+"]");
      int cnt = selected.split(" ").length;
      switch(editMode){
      case ALTER:
        if(cnt==1){
          nv.setEnabled(true);
          nv.setText("");
          nboService.runScriptQueued("label %a");
        }else if(cnt == 5){
          nboService.runScriptNow("measure off;select remove {*};select add {*}["+atomno+"]");
          selected = atomno + " ";
        }else{
          if(!atNum.isSelected())
            nboService.runScriptQueued("label off");
          if(cnt==2)
            //TODO script needs to be run twice for some reason
            nboService.runScriptQueued("measure off;measure "+selected + "\"2:%0.4VALUE //A\"" +
            		";measure "+selected + "\"2:%0.4VALUE //A\"");
          else
            nboService.runScriptQueued("measure off;measure "+selected);
        }
        break;
      case CLIP:
        if(cnt == 2)
          edit.setEnabled(true);
        else if(cnt == 3){
          nboService.runScriptNow("select remove {*};select add {*}["+atomno+"]");
          selected = atomno + " ";
        }
        break;
      case MUTATE:
        if(cnt == 1){
          nv.setEnabled(true);
        nv.setText("");
        }if(cnt == 2){
          nboService.runScriptNow("select remove {*};select add {*}["+atomno+"]");
          selected = atomno + " ";
        }
      }
    }
  }
}
