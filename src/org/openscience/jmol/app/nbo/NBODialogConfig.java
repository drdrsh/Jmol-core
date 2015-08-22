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

import javajs.util.SB;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.TitledBorder;

import org.jmol.i18n.GT;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.jmolpanel.JmolPanel;

abstract class NBODialogConfig extends JDialog {

  protected static String sep = System.getProperty("line.separator");

  protected static final String DEFAULT_PARAMS = "PLOT CMO DIPOLE STERIC";

  protected static final int PANEL_RIGHT = -1;
  protected static final int PANEL_CENTER = -2;
  protected static final int PANEL_TOP = -3;
  protected static final int PANEL_MODEL_SELECT = 1;
  protected static final int PANEL_RUN_SELECT = 2;
  protected static final int PANEL_VIEW_SELECT = 3;
  protected static final int PANEL_SEARCH_SELECT = 4;
  protected static final int PANEL_RUN_FILE = 5;
  protected static final int PANEL_SEARCH_CENTER = 6;
  protected static final int PANEL_STATUS = 7;
  protected static final int PANEL_SEARCH_OUT = 0;
  
  abstract protected void goRunClicked(String defaultParams, File inputFile, Runnable load47Done);
  abstract protected void showWorkpathDialogR(String st);
  abstract protected void showWorkpathDialogM(String st, String type);
  abstract protected void showWorkpathDialogV(String st);
  abstract protected void showWorkpathDialogS(String st);
  
  protected NBOPanel topPanel,statusPanel;

  protected NBOService nboService;
  protected boolean haveService;
  boolean isJmolNBO;
  
  protected Viewer vwr;

  protected JButton nboPathButton, browse, helpBtn, modelButton,runButton,viewButton,searchButton;
  protected JTextField Field;
  protected JTextField dataPathLabel;
  protected JTextField serverPathLabel;
  
  protected JTextField savePathLabel;
  protected JScrollPane editPane2;
  protected JLabel statusLab = new JLabel();

  protected JComboBox<String> action, module;
  protected JTextPane fileText;
  protected JButton go;

  protected String reqInfo;

  abstract void buildView(Container p);
  JTextField rawInput;
  JTextField nboInput;
  protected JTextPane nboOutput;

  protected String jobStem;
  JLabel icon;
  protected Font nboFont = new Font("Monospaced", Font.BOLD, 16);

  /**
   * Creates a dialog for getting info related to output frames in nbo format.
   * 
   * @param f
   *        The frame assosiated with the dialog
   */
  protected NBODialogConfig(JFrame f) {
    super(f, GT._("NBO Server Interface"), false);
  }

  protected void buildMain(Container p) {
    p.removeAll();
    p.setLayout(new BorderLayout());
    topPanel=buildTopPanel();
    p.add(topPanel,BorderLayout.NORTH);
    p.add(statusPanel,BorderLayout.SOUTH);
    p.add(buildFilePanel(),BorderLayout.CENTER);
    centerDialog(this);
  }
  
  protected JPanel buildFilePanel() {

    JPanel filePanel = new JPanel(new BorderLayout());
    filePanel.setBorder(BorderFactory.createLoweredBevelBorder());

    //GUI for NBO path selection
    Box box = Box.createHorizontalBox();
    box.setBorder(BorderFactory.createTitledBorder(new TitledBorder("Location of NBOServe executable:")));
    serverPathLabel = new JTextField("");
    serverPathLabel.setEditable(false);
    serverPathLabel.setBorder(null);
    serverPathLabel.setText(nboService.serverPath);
    haveService = (serverPathLabel.getText().length() > 0);
    box.add(serverPathLabel);
    box.add(new JLabel("  "));
    nboPathButton = new JButton("Browse...");
    nboPathButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        showNBOPathDialog();
      }
    });
    box.add(nboPathButton);
    JButton b = new JButton("Connect");
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        connectPressed();
      }
    });
    box.add(b);
    filePanel.add(box, BorderLayout.NORTH);
    (nboOutput = new JTextPane()).setFont(new Font("Arial",Font.PLAIN,16));
    JScrollPane p = new JScrollPane();
    p.getViewport().add(nboOutput);
    p.setBorder(BorderFactory.createTitledBorder(new TitledBorder("NBO Output:")));
    filePanel.add(p, BorderLayout.CENTER);
    return filePanel;
  }
  
  protected void setComponents(Component comp){
    if(comp.equals(topPanel)||comp.equals(statusPanel)){
      setComponents2(comp);
      return;
    }
    if(comp instanceof JTextField)
      return;
    if(comp instanceof JComboBox)
      return;
    comp.setForeground(Color.BLACK);
    if(comp instanceof Container){
      comp.setBackground(Color.WHITE);
      for(Component c:((Container)comp).getComponents()){
        setComponents(c);
      }
    }
  }
  
  protected void setComponents2(Component comp){
    comp.setForeground(Color.WHITE);
    if(comp instanceof Container){
      comp.setBackground(Color.BLACK);
      for(Component c:((Container)comp).getComponents()){
        setComponents2(c);
      }
    }
  }
  
  protected NBOPanel buildStatusPanel(){
    NBOPanel p = new NBOPanel(this, PANEL_STATUS);
    p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
    Box bo = Box.createHorizontalBox();
    bo.add(new JLabel("NBO Input: "));
    bo.add(rawInput);
    bo.add(statusLab);
    bo.add(helpBtn);
    p.add(bo);
    return p;
  }
  
  /**
   * Top panel with logo/modules/file choosing options
   * @return top panel
   */
  protected NBOPanel buildTopPanel(){
    NBOPanel p = new NBOPanel(this,PANEL_TOP);
    Font f = new Font("Arial",Font.BOLD,20);
    p.add(modelButton).setFont(f);
    p.add(runButton).setFont(f);
    p.add(viewButton).setFont(f);
    p.add(searchButton).setFont(f);
    p.add(Box.createRigidArea(new Dimension(15,0)));
    p.add(icon=new JLabel());
    p.setBackground(Color.BLACK);
    return p;
  }

  protected JPanel folderBox() {
    JPanel b = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.gridx=0;
    c.gridy=0;
    c.fill=GridBagConstraints.BOTH;
    (tfFolder = new JTextField()).setPreferredSize(new Dimension(130,20));
    tfFolder.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        //browse.setSelected(true)
        showWorkpathDialogM(tfFolder.getText()+"/new",null);
      }
    });
    b.add(tfFolder,c);
    c.gridx=1;
    (tfName = new JTextField()).setPreferredSize(new Dimension(100,20));
    b.add(tfName,c);
    c.gridx=0;
    c.gridy=1;
    b.add(new JLabel("         folder"),c);
    c.gridx=1;
    b.add(new JLabel("          name"),c);
    c.gridx=2;
    c.gridy=0;
    (tfExt = new JTextField()).setPreferredSize(new Dimension(40,20));
    b.add(tfExt,c);
    c.gridy=1;
    b.add(new JLabel("  ext"),c);
    c.gridx=3;
    c.gridy=0;
    c.gridheight=2;
    b.add(browse,c);
    b.setPreferredSize(new Dimension(350, 50));
    return b;
  }
  
  /*protected NBOPanel buildRightPanel() {

    NBOPanel p = new NBOPanel(this, PANEL_SEARCH_OUT);
    p.setLayout(new BorderLayout());

    TitledBorder editTitle = BorderFactory.createTitledBorder("NBO Output");
    p.setBorder(editTitle);
    nboOutput = new JTextPane();
    nboOutput.setContentType("text/plain");
    nboOutput.setFont(nboFont);
    editPane2 = new JScrollPane();
    editPane2.getViewport().add(nboOutput);
    p.add(editPane2, BorderLayout.CENTER);
    return p;
  }*/


  protected void rawCmd(String name, final String cmd, final int mode) {
    nboService.queueJob(name, null, new Runnable() {
      @Override
      public void run() {
        nboService.rawCmdNew(cmd, null, false, mode);
      }
    });
  }
  
  protected void connectPressed() {
    boolean err = nboService.restartIfNecessary();
    //if (System.getProperty("sun.arch.data.model").equals("64"))
    String arch = System.getenv("PROCESSOR_ARCHITECTURE");
    String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");

    String realArch = arch.endsWith("64")
                      || wow64Arch != null && wow64Arch.endsWith("64")
                          ? "64" : "32";
    if(realArch.equals("64")){
      //TODO set gennbo
    }
//    appendModelOutPanel(null);
    if (err) {
      appendModelOutPanel("NBOServe successfully connected");
      modelButton.setEnabled(true);
      runButton.setEnabled(true);
      viewButton.setEnabled(true);
      searchButton.setEnabled(true);
      browse.setEnabled(true);
    } else {
      appendModelOutPanel("Could not connect");
    }
  }
//
//  protected void clearPressed() {
//    nboReport(null);
//  }

  /**
   * Responds to cancel being press- or equivalent eg window closed.
   */
  void closePressed() {
    nboService.closeProcess();
    saveHistory();
    setVisible(false);
    dispose();
  }

  /**
   * Just saves the path settings from this session.
   */
  protected void saveHistory() {
    java.util.Properties props = new java.util.Properties();
    props.setProperty("nboServerPath", nboService.serverPath);
    //props.setProperty("nboWorkingPath", workingPath);
    JmolPanel.historyFile.addProperties(props);
  }
  
  protected void saveWorkHistory(){
    java.util.Properties props = new java.util.Properties();
    props.setProperty("workingPath", workingPath);
    JmolPanel.historyFile.addProperties(props);
  }

  @Override
  public void setVisible(boolean b) {
    super.setVisible(b);
    }
  
  /**
   * Show a file selector when the savePath button is pressed.
   */
  void showNBOPathDialog() {
    JFileChooser myChooser = new JFileChooser();
    String fname = serverPathLabel.getText();
    myChooser.setSelectedFile(new File(fname));
    int button = myChooser.showDialog(this, GT._("Select"));
    if (button == JFileChooser.APPROVE_OPTION) {
      File newFile = myChooser.getSelectedFile();
      String path = newFile.toString();
      if (path.indexOf("NBO") < 0)
        return;
      serverPathLabel.setText(path);
      nboService.serverPath = path;
      saveHistory();
      pack();
    }
  }

  /**
   * Centers the dialog on the screen.
   * @param d 
   */
  protected void centerDialog(JDialog d) {
//    Dimension screenSize = d.getToolkit().getScreenSize();
//    Dimension size = d.getMinimumSize();
//    screenSize.height = screenSize.height / 2;
//    screenSize.width = screenSize.width / 2;
//    size.height = size.height / 2;
//    size.width = size.width / 2;
    int y = //screenSize.height - size.height;
    d.getParent().getY();
    int x = //screenSize.width - size.width;
    d.getParent().getX()+d.getParent().getWidth();
    d.setLocation(x, y);
  }
  
  protected void appendModelOutPanel(String line) {
    if (line.length() > 1)
      nboOutput.setText(nboOutput.getText() + line + "\n");
    nboOutput.setCaretPosition(nboOutput.getText().length());
  }
  
  protected void clearOutput(){
    nboOutput.setText("");
  }

  /**
   * builds the three panels
   * 
   * @param p
   * @param type
   * @return Dimension of this panel
   */

  protected Dimension setPreferredSPanelSize(NBOPanel p, int type) {
    switch (type) {
    case PANEL_TOP:
      return new Dimension(p.getParent().getWidth(), 85);
    case PANEL_MODEL_SELECT:
      return new Dimension((int) (p.getParent().getWidth() * .6667), (int) (p
          .getParent().getHeight()*.7));
    case PANEL_SEARCH_CENTER:
      return new Dimension((int) (p.getParent().getWidth() * .5), (int) (p
          .getParent().getHeight()*.7));
    case PANEL_SEARCH_SELECT:
      return new Dimension((int) (p.getParent().getWidth() * (.25)), (int) (p
          .getParent().getHeight()*.7));
    case PANEL_SEARCH_OUT:
      return new Dimension((int) (p.getParent().getWidth() * (.25)), (int) (p
          .getParent().getHeight()*.7));
    case PANEL_STATUS:
      return new Dimension(p.getParent().getWidth(), (int) (p.getParent().getHeight()*.05));
    default:
      return new Dimension(p.getParent().getWidth() / 3, (int) (p.getParent()
          .getHeight()*.7));
    }
  }

  protected void appendToFile(String s, SB sb) {
    sb.append(s);
  }

  protected JTextField tfFolder, tfName, tfExt;
  protected int jmolAtomCount;
  protected File inputFile;
  protected String workingPath;

  protected void nboReset() {
    // see subclasses
  }

  protected Runnable showWorkPathDone = new Runnable() {
    @Override
    public void run() {
      nboService.runScriptNow("load " + inputFile.toString());
      nboService.runScriptNow("refresh");
    }
  };
  
  protected void setInputFile(File inputFile, String useExt, final Runnable whenDone){
    this.inputFile = inputFile;
    if (inputFile.getName().indexOf(".") > 0)
      jobStem = getJobStem(inputFile);
    tfFolder.setText(inputFile.getParent());
    tfName.setText(jobStem);
    tfExt.setText(useExt);
    jmolAtomCount = nboService.evaluateJmol("{*}.count").asInt();
    if (getExt(inputFile).equals("47")){
      isJmolNBO = true;
      workingPath = inputFile.toString();
      saveWorkHistory();
      Runnable load47Done = new Runnable() {
        @Override
        public void run() {
          statusLab.setText("");
          if (whenDone != null)
            whenDone.run();
        }
      };
      if (!newNBOFile(inputFile, "46").exists()) {
        goRunClicked(DEFAULT_PARAMS, inputFile, load47Done);
        return;
      }
      load47Done.run();
    }
  }
  
  // useful file manipulation methods
  
  protected File newNBOFile(File f, String ext) {
    String fname = f.toString();
    return new File(fname.substring(0, fname.lastIndexOf(".")) + "." + ext);
  }

  protected String getJobStem(File inputFile) {
    String fname = inputFile.getName();
    return fname.substring(0, fname.lastIndexOf("."));
  }

  protected String getExt(File newFile) {
    String fname = 
    newFile.toString();
    return fname.substring(fname.lastIndexOf(".") + 1);
  }
  
  protected boolean isJmolNBO(){
    return (vwr.ms.getInfo(vwr.am.cmi, "nboType") != null || 
        getExt(new File(nboService.getJmolFilename())).equals("47"));
  }
  protected void clearInputFile(){
    tfFolder.setText("");
    tfName.setText("");
    tfExt.setText("");
    inputFile=null;
    nboService.runScriptQueued("zap");
  }


}
