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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jmol.c.CBK;
import org.jmol.i18n.GT;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.jmolpanel.GuiMap;
import org.openscience.jmol.app.jmolpanel.JmolPanel;

/**
 * A dialog for interacting with NBOServer
 * 
 * The NBODialog class includes all public entry points. In addition, there are
 * several superclasses:
 * 
 * JDialog NBODialogConfig
 * 
 * -- NBODialogModel
 * 
 * ---- NBODialogRun
 * 
 * ------ NBODialogView
 * 
 * -------- NBODialogSearch
 * 
 * ---------- NBODialog
 * 
 * All of these are one object, just separated this way to allow some 
 * compartmentalization of tasks along the lines of NBOPro6.
 * 
 * 
 */
public class NBODialog extends NBODialogSearch {

  private JTextField jtRawInput;


  // local settings of the dialog type

  /**
   * Creates a dialog for getting info related to output frames in nbo format.
   * 
   * @param f
   *        The frame assosiated with the dialog
   * @param vwr
   *        The interacting display we are reproducing (source of view angle
   *        info etc)
   * @param nboService
   */
  public NBODialog(JFrame f, Viewer vwr, NBOService nboService) {
    super(f);
    this.vwr = vwr;
    this.nboService = nboService;
    //get saved properties
    java.util.Properties props = JmolPanel.historyFile.getProperties();
    savePath = (props.getProperty("savePath", System.getProperty("user.home")));
    usePath = (props.getProperty("usePath", System.getProperty("user.home")));
    workingPath = (props.getProperty("workingPath",
        System.getProperty("user.home")));
    nboService.nboDialog = this;
    createDialog(570, 615);
    if(props.getProperty("useNBOView","").equals("true"))
      jCheckNboView.doClick();
//    nboService.runScriptQueued("background [0,0,204]");
//    for(String s: colors)
//      nboService.runScriptQueued("color " + s);
    
  }

  protected boolean isSet;
  
  private void createDialog(int width, int height) {
    haveService = (nboService.serverPath.length() > 0);
    setSize(new Dimension(width, height));
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        close();
      }
    });
    final NBODialog dialog = this;
    addComponentListener(new ComponentListener() {

      @Override
      public void componentResized(ComponentEvent e) {
        if (!isSet)
          placeNBODialog(dialog);
        isSet = true;
      }

      @Override
      public void componentMoved(ComponentEvent e) {
      }

      @Override
      public void componentShown(ComponentEvent e) {
      }

      @Override
      public void componentHidden(ComponentEvent e) {        
      }
      
    });

    //static page components
    mainButtons = new JButton[]{
        modelButton = getMainButton("Model",'m'),
        runButton = getMainButton("Run",'r'),
        viewButton = getMainButton("View",'v'),
        searchButton = getMainButton("Search",'s')
    };
    topPanel = buildTopPanel();
    browse = new JButton("Browse");
    browse.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent arg0) {
        switch(dialogMode){
        case DIALOG_MODEL:showWorkpathDialogM(usePath,"cfi,vfi,47,gau,log,gms,adf,jag,mm2,mnd,mp,nw,orc,pqs,qc");
        break;
        default: showWorkpathDialog(workingPath);
        break;
        }
      }
    });
    helpBtn = new JButton("Help");
    helpBtn.setFocusable(false);
    helpBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        showHelp(null);
      }
    });
    jtRawInput = new JTextField();
    jtRawInput.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        rawInput();
      }
    });
    statusPanel = buildStatusPanel();
    statusPanel.setSize(this.getWidth(),60);
    jpNboOutput = new JTextPane();
    jpNboOutput.setEditable(false);
    jpNboOutput.setEnabled(false);
    jpNboOutput.setFont(new Font("Arial",Font.PLAIN,16));
    jCheckAtomNum = new JCheckBox("Show Atom Numbers");//.setAlignmentX(0.5f);
    jCheckAtomNum.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        showAtomNums();
      }
    });
    jCheckAtomNum.setSelected(true);
    jCheckNboView = new JCheckBox("Use NBO View");
    jCheckNboView.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if(jCheckNboView.isSelected())
          setNBOColorScheme();
        else
          resetColorScheme();
        java.util.Properties props = new java.util.Properties();
        props.setProperty("useNBOView", nboView ? "true":"false");
        JmolPanel.historyFile.addProperties(props);
      }
    });
    if (haveService)
      connect();
  }
  
  private JButton getMainButton(String path, final char mode){
    JButton b = new JButton(path);
    b.setBorder(null);
    b.setMargin(new Insets(4, 4, 4, 4));
    b.setContentAreaFilled(false);
    b.setForeground(Color.white);
    b.setFont(new Font("Arial",Font.BOLD,20));
    //b.setIcon(new ImageIcon(getClass().getResource(path)));
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        openPanel(mode);
      }
    });
    return b;
//    button.setRolloverIcon(myIcon2);
//    button.setPressedIcon(myIcon3);
//    button.setDisabledIcon(myIcon4);
  }

  @Override
  protected void showWorkpathDialog(String workingPath) {
    JFileChooser myChooser = new JFileChooser();
    String useExt = tfExt.getText();
    if(!useExt.equals("")){
      myChooser.setFileFilter(new FileNameExtensionFilter(useExt, useExt));
      myChooser.setFileHidingEnabled(true);
    }
    String folder = tfFolder.getText().trim();
    if(!folder.equals("")){
      if(!folder.contains(":")) folder = "C:/"+folder;
      workingPath = folder + "/"+(tfName.getText().equals("") ? "new":tfName.getText());
    }
    myChooser.setSelectedFile(new File(workingPath));
    int button = myChooser.showDialog(this, GT._("Select"));
    if (button == JFileChooser.APPROVE_OPTION) {
      nboResetV();
      inputFile = myChooser.getSelectedFile();
      if(!useExt.equals("47")&&!useExt.equals("31")&&!useExt.equals("nbo")) 
        return;
      setInputFile(newNBOFile(myChooser.getSelectedFile(), "47"), useExt, showWorkPathDone);
      switch(dialogMode){
      case(DIALOG_VIEW):
        dispBox();
        basis.setSelectedIndex(5);
        orbBox.setVisible(true);
        break;
      case(DIALOG_RUN):
        enableComponentsR(this,true);
        addNBOKeylist();
        editBox.setVisible(true);
        break;
      case(DIALOG_SEARCH):
        enableComponentsR(this,true);
        if(keywordNumber!=0){
          listClicked(keywordNumber);
        }
        break;
      default:
        enableComponentsR(this,true);
      }
    }
  }
  
  protected void showErrorFile(String err){
    showNboOutput(err);
  }
  
  protected JPanel buildStatusPanel(){
    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
    Box bo = Box.createHorizontalBox();
    bo.add(new JLabel("Command Input: "));
    bo.add(jtRawInput);
    bo.add(statusLab);
    bo.add(helpBtn);
    p.add(bo);
    setComponents(p,Color.white,Color.black);
    return p;
 }

  protected void close() {
    saveHistory();
    nboService.closeProcess();
    nboResetV();
    nboService.runScriptQueued("mo delete; nbo delete; select off");
    dispose();
  }
  
  private boolean checkEnabled() {
    haveService = (nboService.serverPath.length() > 0);
    boolean enabled = (haveService && nboService.restartIfNecessary());    
    for (int i = mainButtons.length; --i >= 0;) {
      mainButtons[i].setEnabled(enabled);
    }
    if (!enabled)
      dialogMode = DIALOG_CONFIG;
    return enabled;
  }

  @Override
  protected boolean connect() {
    //if (System.getProperty("sun.arch.data.model").equals("64"))
    String arch = System.getenv("PROCESSOR_ARCHITECTURE");
    File f = new File(nboService.serverDir+"gennbo.bat");
    if(!f.exists()){
      appendOutputWithCaret("gennbo.bat not found, make sure gennbo.bat is in same directory as nboserve.exe",'b');
      return false;
    }
    String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");
    String realArch = arch.endsWith("64")
                      || wow64Arch != null && wow64Arch.endsWith("64")
                          ? "64" : "32";
    BufferedReader b = null;
    try {
      b = new BufferedReader(new FileReader(f));
      String line;
      String contents = "";
      while((line = b.readLine())!=null){
        if(line.startsWith("set INT=")){
          line = (realArch.equals("64")?"set INT=i8":"set INT=i4");
        }
        contents += line + sep;
      }
      nboService.writeToFile(contents, f);
      b.close();
    } catch (FileNotFoundException e) {
      appendOutputWithCaret("Error opening gennbo.bat",'b');
      return false;
    } catch (IOException e) {
      appendOutputWithCaret("Error opening gennbo.bat",'b');
      return false;
    }
    boolean isOK = checkEnabled(); 
    appendOutputWithCaret(isOK ? "NBOServe successfully connected" : "Could not connect",'p');
    return isOK;
  }

  public void openPanel(char type) {
    switch (dialogMode) {
    case DIALOG_CONFIG:
    case DIALOG_MODEL:
    case DIALOG_RUN:
      break;
    case DIALOG_VIEW:
    case DIALOG_SEARCH:
      nboService.runScriptQueued("mo delete; nbo delete; select off");
      break;
    }
    topPanel.remove(icon);
    isJmolNBO = checkJmolNBO();
    if (!checkEnabled())
      type = 'c';
    for (int i = mainButtons.length; --i >= 0;)
      mainButtons[i].setBorder(null);
    switch (type) {
    case 'c':
      dialogMode = DIALOG_CONFIG;
      buildConfig(this.getContentPane());
      break;
    case 'm':
      dialogMode = DIALOG_MODEL;
      setThis(modelButton);
      buildModel(this.getContentPane());
      icon = new JLabel(new ImageIcon(this.getClass().getResource("nbomodel_logo.gif")));
      break;
    case 'r':
      dialogMode = DIALOG_RUN;
      setThis(runButton);
      buildRun(this.getContentPane());
      icon = new JLabel(new ImageIcon(this.getClass().getResource("nborun_logo.gif")));
      break;
    case 'v':
      dialogMode = DIALOG_VIEW;
      setThis(viewButton);
      buildView(this.getContentPane());
      icon = new JLabel(new ImageIcon(this.getClass().getResource("nboview_logo.gif")));
      break;
    case 's':
      dialogMode = DIALOG_SEARCH;
      setThis(searchButton);
      buildSearch(this.getContentPane());
      icon = new JLabel(new ImageIcon(this.getClass().getResource("nbosearch_logo.gif")));
      break;
    }
    //setComponents(this, null, null);
    topPanel.add(icon);
    invalidate();
    setVisible(true);
    connect();
  }

  private void setThis(JButton btn) {
    btn.setEnabled(false);
    btn.setBorder(new LineBorder(Color.WHITE, 2));
    invalidate();
  }

  protected void rawInput() {
    String cmd0 = jtRawInput.getText();
    String cmd = cmd0.trim().toUpperCase();
    switch (dialogMode) {
    case DIALOG_MODEL:
      if (cmd.startsWith("HELP"))
        showHelp(cmd.indexOf(" ") >= 0 ? cmd.split(" ")[1].toLowerCase() : "");
      else
        rawInputM(cmd0); // no upper case here
      break;
    case DIALOG_RUN:
      break;
    case DIALOG_VIEW:
      rawInputV(cmd);
      break;
    case DIALOG_SEARCH:
      rawInputS(cmd);
      break;
    }
    jtRawInput.setText("");
  }

  /**
   * Callback from Jmol Viewer indicating user actions
   * 
   * @param type
   * @param data
   */
  @SuppressWarnings("incomplete-switch")
  public void notifyCallback(CBK type, Object[] data) {
    switch (type) {
    case STRUCTUREMODIFIED:
      if(dialogMode == DIALOG_MODEL){
        loadModel();
        //nboService.runScriptQueued("select on");
        //nboService.runScriptNow();
      }
      break;
    case PICK:
      int atomIndex = ((Integer) data[2]).intValue();
      if (atomIndex < 0)
        break;
      String atomno = "" + (atomIndex + 1);
      switch (dialogMode) {
      case DIALOG_MODEL:
        notifyCallbackM(atomno);
        break;
      case DIALOG_VIEW:
        notifyCallbackV(atomno);
        break;
      case DIALOG_SEARCH:
        notifyCallbackS(atomIndex);
        break;
      }
      break;
    case LOADSTRUCT:
      if(dialogMode==DIALOG_MODEL)
        notifyLoadModel();
      break;
    }
  }

  void alert(String msg) {
    try {
      switch (dialogMode) {
      case DIALOG_MODEL:
        appendOutputWithCaret(msg,'b');
        return;
      case DIALOG_RUN:
      case DIALOG_VIEW:
      case DIALOG_SEARCH:
      }
    } catch (Exception e) {
      vwr.alert(msg);
    }
  }

  protected void showHelp(String key) {
    JDialog help = new JDialog(this, "NBO Help");
    JTextPane p = new JTextPane();
    p.setEditable(false);
    p.setFont(new Font("Arial", Font.PLAIN, 16));
    JScrollPane sp = new JScrollPane();
    sp.getViewport().add(p);
    help.add(sp);
    help.setSize(new Dimension(400, 400));
    switch (dialogMode) {
    case DIALOG_CONFIG:
      p.setText(getHelp("config"));
      break;
    case DIALOG_MODEL:
      if (!helpDialogM(p, key))
        return;
      break;
    case DIALOG_RUN:
      p.setText(getHelp("run"));
      break;
    case DIALOG_VIEW:
      p.setText(getHelp("view"));
      break;
    case DIALOG_SEARCH:
      p.setText(searchHelp);
      break;
    }
    p.setCaretPosition(0);
    centerDialog(help);
    help.setVisible(true);
  }
    
  public void addLine(int type, String line) {
    if(line==null)
      return;
    switch (type) {
    case DIALOG_CONFIG:
      reqInfo = line;
      break;
    case DIALOG_MODEL:
      if(line.contains("NBOModel")){
        //loadModel();
        appendOutputWithCaret(line,'b');
      }else if(this.editMode==VALUE)
        appendOutputWithCaret("   " + line,'b');
      else 
        appendOutputWithCaret("   " + line.charAt(1)+"<sub>"+line.substring(2)+"</sub>",'b');
      break;
    case DIALOG_VIEW:
      line = line.trim();
      while (line.length() % 20 != 0)
        line += " ";
      reqInfo += line;
      break;
      //$FALL-THROUGH$
    case DIALOG_SEARCH:
      reqInfo += "-" + line;
      break;
    case DIALOG_LIST:
      if (reqInfo.trim().split(" ").length <= jmolAtomCount)
        reqInfo += line + " ";
      break;
    }
  }

  public void setStatus(String statusInfo) {
    statusLab.setText(statusInfo);
  }
  
  

}
