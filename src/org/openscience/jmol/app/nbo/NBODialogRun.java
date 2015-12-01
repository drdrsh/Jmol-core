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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jmol.util.Logger;
import org.openscience.jmol.app.jmolpanel.GaussianDialog;

abstract class NBODialogRun extends NBODialogModel {
  protected static final String[] keywordList = {"CMO: bonding character of MO's","DIPOLE:","NBBP:","NBCP:","NCE:","NCU:","NRT:","PLOT:","STERIC:"};
  private static final String CHOOSE_DESCRIPTION = "$CHOOSE Keylist - Edit lone pairs and bond orders";
  private JFrame runFrame;
  protected String validBonds = "",validLP = "",valid3C = "", delOrbs = "", delOrbsL = "";
  protected ArrayList<String> deleteElements, deleteBlocks, deleteAtomBlocks, delDeloc;
  protected boolean deleteLewisOrbs, deleteVicinal, deleteGeminal;
  String fileData, fileData2, nboKeywords;
  protected JTextField plotFileName;
  Hashtable<String, String> chooseBonds, choose3C;
  int lastEss;
  protected JComboBox<String> editOps;
  protected JRadioButton[] keywordButtons;

  protected NBODialogRun(JFrame f) {
    super(f);
    runFrame = f;
  }

  protected void buildRun(Container p) {
    if(tfExt!=null)
      if(tfExt.getText().equals("47")){
        isJmolNBO = true;
        nboService.runScriptQueued("load "+inputFile.toString()+";refresh");
        setBonds(null,null);
        String s = nboService.runScriptNow("print {*}.bonds");
        nboService.runScriptQueued("select "+s+";color bonds lightgrey; wireframe 0.1");
        if(jCheckAtomNum.isSelected()) showAtomNums();
      }
    p.removeAll();
    p.setLayout(new BorderLayout());
    if(topPanel == null) topPanel=buildTopPanel();
    p.add(topPanel, BorderLayout.PAGE_START);
    JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,runS(),modelOut());
    sp.setDividerLocation(350);
    sp.setBorder(BorderFactory.createLoweredBevelBorder());
    p.add(sp, BorderLayout.CENTER);
    statusLab.setText("");
    p.add(statusPanel, BorderLayout.PAGE_END);
    if(isJmolNBO){
      addNBOKeylist();
    }
    showRunDone = new Runnable() {
      @Override
      public void run() {
        try {
          nboService.writeToFile(getFileContents(), inputFile);
        } catch (IOException e) {
          // TODO
        }
        nboService.runScriptNow("load " + inputFile.toString() + ";refresh");
        if(!nboKeywords.contains("PRINT=0"))
          if(!getChooseList()){
          File f = newNBOFile(inputFile,"nbo");
          if(!f.exists()||f.length()==0){
            f.delete();
            if(validBonds.equals("")){
              f = newNBOFile(inputFile,"nbo");
              showConfirmationDialog("Error occurred during run. View .nbo file?",f,"nbo",CONFIRM_VIEWOUTPUT);
              return;
            }
            resetBadChooseList();
            vwr.alert("Invalid $CHOOSE list");
            //appendModelOutPanel("Invalid $CHOOSE list");
            goRunClicked(DEFAULT_PARAMS, "gennbo", inputFile, showRunDone);
            return;
          }
        }
        appendOutputWithCaret("Complete: Output to " + inputFile.getParent()
            + "\\" + jobStem + ".nbo",'b');
        editOps.setSelectedIndex(editOps.getSelectedIndex());
        if(nboView){
          //setBonds();
          String s = nboService.runScriptNow("print {*}.bonds");
          nboService.runScriptQueued("select "+s+";color bonds lightgrey; wireframe 0.1");
        }
        //setBonds();
        //jmolAtomCount = vwr.ms.ac;
        if(jCheckAtomNum.isSelected())
          showAtomNums();
      }
    };
  }
  
  protected JPanel runS() {
    JPanel runPanel = new JPanel();
    runPanel.setLayout(new BoxLayout(runPanel, BoxLayout.Y_AXIS));
    final JComboBox<String> essBox = new JComboBox<String>(
        new String[]{"GenNBO", "GO9", "GAMESS" });
    final DefaultComboBoxModel<String> editModel = new DefaultComboBoxModel<String>();
    editOps=new JComboBox<String>(editModel);
    //ESS////////////////////////////////////////
    Box box = Box.createVerticalBox();
    essBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if(lastEss != essBox.getSelectedIndex()){
          essChanged(essBox.getSelectedItem().toString(),editModel);
          lastEss = essBox.getSelectedIndex();
        }
      }
    });
    box.add(essBox);
    JLabel lab = new JLabel("ESS type");
    lab.setAlignmentX(0.5f);
    box.add(lab);
    essBox.setAlignmentX(0.5f);
    essBox.setMaximumSize(new Dimension(100,40));
    //INPUT/////////////////////////
    box.add(folderBox());
    Box box2 = Box.createVerticalBox();
    tfExt.setText("47");
    tfName.setText("");
    browse.setEnabled(true);
    JLabel title = new JLabel(" Select Input ");
    title.setAlignmentX(0.0f);
    title.setBackground(titleColor);
    title.setForeground(Color.white);
    title.setFont(titleFont);
    title.setOpaque(true);
    box2.add(title);
    box.setBorder(BorderFactory.createLineBorder(Color.black));
    box.setMaximumSize(new Dimension(355,80));
    box.setAlignmentX(0.0f);
    box2.setAlignmentX(0.0f);
    box2.add(box);
    runPanel.add(box2);
    //EDIT////////////////
    box = Box.createVerticalBox();
    title = new JLabel(" Choose $NBO Keywords ");
    title.setAlignmentX(0.0f);
    title.setBackground(titleColor);
    title.setForeground(Color.white);
    title.setFont(titleFont);
    title.setOpaque(true);
    box.add(title);
    box.setMinimumSize(new Dimension(350,400));
    box.setAlignmentX(0.0f);
    editBox = Box.createVerticalBox();
    editBox.setAlignmentX(0.0f);
    editBox.setBorder(BorderFactory.createLineBorder(Color.black));
    box.add(editBox);
    box.setVisible(isJmolNBO);
    runPanel.add(box);
    //BOTTOM OPTIONS///////////////
    JButton btn = new JButton("Run");
    btn.setAlignmentX(-0.5f);
    btn.setVisible(isJmolNBO);
    runPanel.add(btn).setFont(new Font("Arial",Font.PLAIN,20));
    btn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        goRunClicked("",essBox.getSelectedItem().toString(),inputFile,showRunDone);
      }
    });
    if(isJmolNBO){
      //readInputFile(inputFile);
      editOps.setSelectedIndex(editOps.getSelectedIndex()==0?1:editOps.getSelectedIndex());
    }
    return runPanel;
  }
  
  protected int showConfDialog(){
    return JOptionPane.showConfirmDialog(this, "File already exists, overwrite contents?","Overwrite file?",JOptionPane.YES_NO_OPTION);
  }
  
  protected String showInputDialog(){
    return (String)JOptionPane.showInputDialog(
        this,
        "Save file as:",
        "Save file...",
        JOptionPane.PLAIN_MESSAGE,
        null,
        null,
        plotFileName.getText()+".47");
  }
  
  protected void addManualEditor(){
    final JDialog d = new JDialog(this, "Keylist Editor");
    d.setLayout(new BorderLayout());
    final JTextPane p = new JTextPane();
    d.add(p,BorderLayout.CENTER);
      p.setText("$NBO\nFile=" + jobStem + " " + nboKeywords + "\n$END");
      p.setCaretPosition(7);
    JButton btn = new JButton("Save Changes");
    btn.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent e){
        String s = p.getText();
          String tmp = s.replace("$NBO", "").replace("$END", "");
          nboKeywords = "";
          for(String x:tmp.split("\\s+"))
            if(!x.contains("="))
              nboKeywords += x + " ";
            else
              plotFileName.setText(x.substring(x.indexOf("="+1)));
          //addNBOKeylist();
          d.setVisible(false);
        
        editBox.repaint();
        editBox.revalidate();
      }
    });
    d.add(btn,BorderLayout.SOUTH);
    centerDialog(d);
    d.setVisible(true);
    d.setSize(new Dimension(300,300));
  }
  
  protected String getFileContents(){
    String fname = plotFileName.getText();
    String fileContents = fileData +"$NBO\n "+"FILE="+(fname.equals("")?jobStem:fname)+" "+nboKeywords +"  $END" + sep;
    return fileContents + getCurrentChooseList() + fileData2;
  }
  
  protected String getCurrentChooseList(){
    String tmp = "$CHOOSE";
    String tmp2 = "";
    if(lonePairs==null)
      return "";
    for(String s:lonePairs.keySet())
      if(!lonePairs.get(s).equals("0"))
        tmp2 += s + " " + lonePairs.get(s) + " ";
    if(!tmp2.equals(""))
      tmp += sep + "  LONE " + tmp2 + "END";
    tmp2 = "";
    for(String s:chooseBonds.keySet())
      tmp2 += chooseBonds.get(s) + " " + s + " ";
    if(!tmp2.equals(""))
      tmp += sep + "  BOND " + tmp2 + "END";
    tmp2 = "";
    for(String s:choose3C.keySet())
      tmp2 += choose3C.get(s) + " " + s + " ";
    if(!tmp2.equals(""))
      tmp += sep + "  3C "+ tmp2 + " END";
    
    return (tmp.equals("$CHOOSE")?"":tmp+sep + "$END" + sep);
  }
  
  protected void essChanged(String item, DefaultComboBoxModel<String> editModel) {
    clearInputFile();
    editBox.removeAll();
    editBox.add(Box.createRigidArea(new Dimension(430,230)));
    //appendOutputWithCaret("ESS changed:\n  " + action.getSelectedItem().toString());
    if(item.equals("GenNBO")){
      tfExt.setText("47");
      editModel.removeElement("Gaussian Input File");
      editModel.addElement(CHOOSE_DESCRIPTION);
      return;
    }else if(item.equals("GO9")){
      tfExt.setText("gau");
      editModel.addElement("Gaussian Input File");
      editModel.removeElement(CHOOSE_DESCRIPTION);
      return;
    }
  }

  protected void editOpChanged(String item){
    editBox.removeAll();
    if(item.equals("-type-")){
      editBox.add(Box.createRigidArea(new Dimension(320,295)));
    }else if(item.startsWith("$NBO")){
      addNBOKeylist();
    }else if(item.startsWith("Gaussian")){
      JScrollPane p = new JScrollPane();
      GaussianDialog gau = new GaussianDialog(runFrame, vwr);
      p.setViewportView(gau.getContentPane());
      editBox.add(p);
    }else if(item.startsWith("$CHOOSE")){
      addChooseKeylist();
    }else if(item.startsWith("$DEL")){
      addDelKeylist();
    }
    this.repaint();
    this.revalidate();
  }

  @Override
  protected void readInputFile(File inputFile) {
    statusLab.setText("Reading input file");
    nboKeywords = "";
    plotFileName = new JTextField();
    BufferedReader b = null;
    try {
      b = new BufferedReader(new FileReader(inputFile));
    } catch (FileNotFoundException e1) {
      // TODO
    }
    SB data = new SB();
    String line;
    try {
      while((line = b.readLine()) != null){
        data.append(line+sep);
      }
    } catch (IOException e) {
      // TODO
    }
    PT.rep(data.toString(), "$end", "$END");
    String[] tokens = PT.split(data.toString(), "$END");
    if(tokens[0].trim().startsWith("$GENNBO")){
      int pt = tokens[0].indexOf("NATOMS=");
      jmolAtomCount = Integer.parseInt(tokens[0].substring(pt+7, tokens[0].indexOf(" ", pt)));
    }
    boolean atParams = false;
    SB fout = new SB(), fout2 = new SB();
    if(tokens.length<=0) return;
    for (int i = 0;;) {
      String s = tokens[i];
      s = PT.trim(s, "\t\r\n ");
      if(!atParams){
        if (s.indexOf("$NBO") >= 0) {
          atParams=true;
          if(PT.split(s, "$NBO").length>1)
            nboKeywords = cleanNBOKeylist(PT.split(s, "$NBO")[1]);
          else
            cleanNBOKeylist("");
          s = "";
          Logger.info("Params read: " + nboKeywords);
        }
        if(!s.equals(""))
          fout.append(s).append(sep);
        if (++i == tokens.length)
          break;
        if(!atParams)
          fout.append("$END").append(sep);
      }else{
        if(s.indexOf("$CHOOSE") >= 0){
          setChooseList(s.substring(8),false);
          i++;
        }else{
          fout2.append(s).append(sep);
          if (++i == tokens.length)
            break;
          fout2.append("$END").append(sep);
        }
      }
    }
    fileData = fout.toString();
    fileData2 = fout2.toString();
    if(!nboKeywords.contains("PRINT=0"))
      getChooseList();
    statusLab.setText("");
  }
  /**
   * gets a valid $CHOOSE list from nbo file if it exists and corrects the bonds in the jmol model
   * @return false if output contains error
   */
  protected boolean getChooseList(){
    validLP = validBonds = valid3C = "";
    File f = newNBOFile(inputFile,"nbo");
    if(!f.exists()||f.length()==0)
      return false;
    String[] tokens = PT.split(nboService.getFileData(f.toString()), "$CHOOSE");
    int i = 1;
    if(tokens.length<2){
      showConfirmationDialog("Error occurred during run. View .nbo file?",f,"nbo",CONFIRM_VIEWOUTPUT);
      return false;
    }
    if(tokens[1].trim().startsWith("keylist")){
      if(tokens[1].contains("NOBOND")) {
        nboService.runScriptQueued("select {*};connect delete; select remove {*}");
        return true;
      }
      if(!tokens[1].contains("Structure accepted:")){
        if(tokens[1].contains("missing END?")){
          showConfirmationDialog("Error occurred during run. View .nbo file?",f,"nbo",CONFIRM_VIEWOUTPUT);
          return false;
        }else if(tokens[2].contains("ignoring")){
          vwr.alert("Ignoring $CHOOSE list"); 
        }else{
          return false;
        }
      }
      i = 3;
    }
    String data = tokens[i].substring(0,tokens[i].indexOf("$END"));
    setChooseList(data,true);
    return true;
  }
  
  private void setChooseList(String data, boolean isValid){
    String [] tokens = PT.split(data, "END");
    lonePairs = new Hashtable<String,String>();
    chooseBonds = new Hashtable<String, String>();
    choose3C = new Hashtable<String, String>();
    for(String x:tokens){
      String [] list = x.trim().split("\\s+");
      if(list[0].trim().equals("BOND"))
        for(int j=1;j<list.length;j+=3){
          if(isValid) validBonds += list[j] + "  " + list[j+1]+" "+list[j+2]+"\n";
          chooseBonds.put(list[j+1]+" "+list[j+2],list[j]);
          //setBonds(new String[] {list[j+1],list[j+2]},list[j]);
        }
      else if (list[0].trim().equals("LONE"))
        for(int j = 1; j<list.length;j+=2){
          if(isValid) validLP += list[j]+"  "+list[j+1]+"\n";
          lonePairs.put(list[j],list[j+1]);
        }
      else if (list[0].equals("3C"))
        for(int j = 1; j<list.length;j+=4){
          if(isValid) valid3C += list[j] + "  " + list[j+1]+" "+list[j+2]+" "+list[j+3]+"\n";
          choose3C.put(list[j+1]+" "+list[j+2]+" "+list[j+3],list[j]);
          //setBonds(new String[] {list[j+1],list[j+2],list[j+3]},list[j]);
        }
    }
  }
  
  protected void resetBadChooseList(){
    chooseBonds = new Hashtable<String,String>();
    String[] data = validBonds.split("\n");
    if(!validBonds.equals(""))
      for(int i = 0;i<data.length;i++){
        String[] st = data[i].split("  ");
        chooseBonds.put(st[1],st[0]);
      }
    lonePairs = new Hashtable<String,String>();
    data = validLP.split("\n");
    if(!validLP.equals(""))
      for(int i = 0;i<data.length;i++){
        String[] st = data[i].split("  ");
        lonePairs.put(st[0],st[1]);
      }
    choose3C = new Hashtable<String,String>();
    data = valid3C.split("\n");
    if(!valid3C.equals(""))
      for(int i = 0;i<data.length;i++){
        String[] st = data[i].split("  ");
        choose3C.put(st[1],st[0]);
      }
  }

  @Override
  protected void setBonds(String [] atoms, String key){
    if(!validBonds.equals("")){
      nboService.runScriptNow("select {*};connect delete; select remove {*}");
      for(String s:validBonds.split("\n")){
        nboService.runScriptNow("select remove {*}");
        String[] tokens = s.split("  ");
        key = tokens[0];
        atoms = tokens[1].split(" ");
      String script = "";
      for(String x:atoms)
        script += "select add (atomno=" + x + ");";
      String partial = "";
      script += "connect ";
      if(atoms.length>2) partial = "partial ";
      if(key.startsWith("S")) script += partial + (!partial.equals("")?"":"single");
      else if(key.startsWith("D")) script += partial+"double";
      else if(key.startsWith("T")) script += partial+"triple";
      else if(key.startsWith("Q")) script += partial+"quadruple";
      else if(key.equals("")) script += "delete";
      nboService.runScriptNow(script);
      }
    }
  }
  
  protected void addBond(String[] atoms, String key){
    nboService.runScriptNow("select remove {*}");
    String script = "";
    for(String x:atoms)
      script += "select add (atomno=" + x + ");";
    String partial = "";
    script += "connect ";
    if(atoms.length>2) partial = "partial ";
    if(key.startsWith("S")) script += partial + (!partial.equals("")?"":"single");
    else if(key.startsWith("D")) script += partial+"double";
    else if(key.startsWith("T")) script += partial+"triple";
    else if(key.startsWith("Q")) script += partial+"quadruple";
    else if(key.equals("")) script += "delete";
    nboService.runScriptNow(script);
  }
  
  private String cleanNBOKeylist(String params) {
    String[] tokens = PT.getTokens(PT.rep(PT.clean(params), "file=", "FILE="));
    String tmp = "";
    for (String s : tokens)
      if (s.length() > 0)
        if(s.toLowerCase().contains("file=")){
          plotFileName.setText(s.substring(s.indexOf("=")+1));
        }else{
          if (tmp.length() + s.length() - tmp.lastIndexOf(sep) >= 80)
            tmp += sep + " ";
          //sList.addElement(s);
          tmp+=s.toUpperCase()+" ";
        }
    if(plotFileName.getText().equals(""))
      plotFileName.setText(jobStem);
    return tmp;
  }

  protected void addNBOKeylist() {
    
    if(inputFile!=null){
      editBox.removeAll();
      editBox.add(Box.createRigidArea(new Dimension(350,0)));
      final DefaultListModel<String> sList = new DefaultListModel<String>();
      String tmp = "";
      for (String s : nboKeywords.split("\\s+"))
        if (s.length() > 0 && !s.toUpperCase().contains("FILE=")){
          sList.addElement(s);
          tmp+=s+" ";
        }
      nboKeywords=tmp;
      final JList<String> keywords = new JList<String>(sList);
      keywords.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          if (e.getClickCount() == 2)
            removeListParams(keywords.getSelectedValuesList(),sList);
        }
      });
      //FILE=///////////
      Box box = Box.createHorizontalBox();
      box.add(new JLabel("Jobname ")).setFont(new Font("Arial",Font.BOLD,16));
      box.add(plotFileName).setMaximumSize(new Dimension(100,30));
      box.setAlignmentX(0.5f);
      Box box2 = Box.createVerticalBox();
      box2.add(box);
      JLabel lab =  new JLabel("(Plot files will be created with this name)");
      lab.setAlignmentX(0.5f);
      box2.add(lab);
      editBox.add(box2);
      //NBOKEYLIST/////////
      box2 = Box.createHorizontalBox();
      box2.add(new JLabel("Keywords:  ")).setFont(new Font("Arial",Font.BOLD,16));
      final Box menu =Box.createVerticalBox();
      final JLabel lab2 = new JLabel("(Select one or more)");
      lab2.setVisible(false);
      menu.setVisible(false);
      JButton btn = new JButton("Menu Select");
      box2.add(btn);
      btn.addActionListener(new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent e){
          menu.setVisible(true);
          lab2.setVisible(true);
        }
      });
      btn = new JButton("Text Editor");
      box2.add(btn);
      btn.addActionListener(new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent e){
          addManualEditor();
        }
      });
      editBox.add(box2);
      menu.setBorder(BorderFactory.createLoweredBevelBorder());
      keywordButtons = new JRadioButton[keywordList.length];
      for(int i=0;i<keywordButtons.length;i++){
        keywordButtons[i] = new JRadioButton(keywordList[i]);
        if(nboKeywords.contains(keywordList[i].split(":")[0]))
          keywordButtons[i].setSelected(true);
        final int op = i;
        keywordButtons[i].addActionListener(new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent e){
          String key = keywordList[op].split(":")[0];
          if(keywordButtons[op].isSelected()){
            nboKeywords += key + " ";
            appendOutputWithCaret("Keyword added: " + key,'i');
          }else{
            nboKeywords = nboKeywords.replaceAll(key + " ", "");
            appendOutputWithCaret("Keyword removed: " + key,'i');
          }
        }
      });
        menu.add(keywordButtons[i]);
      }
      menu.setAlignmentX(0.5f);
      editBox.add(menu);//.setMinimumSize(new Dimension(300,200));
      lab2.setAlignmentX(0.5f);
      editBox.add(lab2);
      //editBox.setMinimumSize(new Dimension(340,200));
    }
  }

  /**
   * Builds the choose key list interface
   */
  void addChooseKeylist(){ 
    //Lone Pairs////////////////
    if(chooseBonds == null){
      JTextField tf = new JTextField("$CHOOSE keylist not found");
      tf.setEditable(false);
      tf.setBorder(null);
      editBox.add(tf);
      editBox.add(Box.createRigidArea(new Dimension(290,230)));
      return;
    }
    String[] s = new String[jmolAtomCount];
    for(int i = 0; i<jmolAtomCount; i++){
      s[i] = "" + (i+1);
    }
    Box b = chooseBox(s, "Number of Lone Pairs",new SpinnerNumberModel(0,0,8,1),lonePairs,0);
    b.setBorder(BorderFactory.createTitledBorder("Lone Pairs"));
    editBox.add(b);
    //Bonds/////////////////
    b = chooseBox(chooseBonds.keySet().toArray(),"Bond Order", new SpinnerListModel(new String[] {"S","D","T","Q"}),chooseBonds,2);
    b.setBorder(BorderFactory.createTitledBorder("Bonds"));
    editBox.add(b);
    //3c////////////////////
    b = chooseBox(choose3C.keySet().toArray(),"Bond Order", new SpinnerListModel(new String[] {"S","D","T","Q"}),choose3C,3);
    b.setBorder(BorderFactory.createTitledBorder("3 Center Bonds"));
    editBox.add(b);
    JButton btn = new JButton("Reset to Default Choose List");
    btn.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent arg0) {
//        lonePairs = null;
//        chooseBonds = null;
//        choose3C = null;
//        validLP = validBonds = valid3C = "";
//        
      }
    });
    btn.setAlignmentX(0.5f);
    editBox.add(btn);
  }
  
  /**
   * Box for editing the $Choose list in input file
   * @param list
   * @param title
   * @param model
   * @param table
   * @param num
   * @return choose box
   */
  private Box chooseBox(Object [] list, String title, SpinnerModel model, final Hashtable<String,String> table, final int num){
    Box b2 = Box.createHorizontalBox();
    String [] s = new String[list.length];
    for(int i = 0;i<s.length;i++)
      s[i] = list[i].toString();
    final DefaultComboBoxModel<String> boxModel = new DefaultComboBoxModel<String>(s);
    final JComboBox<String> atoms = new JComboBox<String>(boxModel);
    atoms.setMaximumSize(new Dimension(70,30));
    final JSpinner numLP = new JSpinner(model);
    numLP.setMaximumSize(new Dimension(40,30));
    atoms.addActionListener(new ActionListener(){
      @SuppressWarnings("boxing")
      @Override
      public void actionPerformed(ActionEvent arg0) {
        String item = atoms.getSelectedItem().toString();
        showSelected(item.split(" "));
        if(table.get(item)!=null){
          if(num==0)
            numLP.setValue(Integer.parseInt(table.get(item)));
          else
            numLP.setValue(table.get(item));
        }else numLP.setValue(0);
      }
    });
    numLP.addChangeListener(new ChangeListener(){
      @Override
      public void stateChanged(ChangeEvent arg0) {
          table.put(atoms.getSelectedItem().toString(),numLP.getValue().toString());
        if(num==0)
          return;
        addBond(atoms.getSelectedItem().toString().split(" "),numLP.getValue().toString());
        nboService.runScriptQueued("refresh");
        atoms.requestFocus();
      }
    });
    b2.add(new JLabel("Atom "));
    b2.add(atoms);
    b2.add(new JLabel(title));
    b2.add(numLP);
    
    if(boxModel.getSize()>0)
      atoms.setSelectedIndex(0);
    if(num==0){
      b2.setAlignmentX(0.5f);
      return b2;
    }
    JButton remove = new JButton("Remove");
    remove.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent e){
        String s = atoms.getSelectedItem().toString();
        table.remove(s);
        boxModel.removeElement(atoms.getSelectedItem());
        String script = "select remove {*};";
        for(String x:s.split(" "))
        		script = script.concat("select add (atomno="+x+");");
        nboService.runScriptQueued(script + "connect delete");
      }
    });
    b2.add(remove);
    Box box = Box.createVerticalBox();
    box.add(b2);
    Box box2 = Box.createHorizontalBox();
    box2.add(new JLabel("Add (Enter atom numbers a1 a2)"));
    final JTextField tf = new JTextField();
    tf.setMaximumSize(new Dimension(200,30));
    tf.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent e){
        String item = tf.getText().trim();
        String [] st = item.split(" ");
        tf.setText("");
        if(st.length!=num) return;
        boxModel.addElement(item);
        if(num==2)
          chooseBonds.put(item, "S");
        else
          choose3C.put(item, "S");
        addBond(st,"S");
        nboService.runScriptQueued("refresh");
        atoms.setSelectedItem(item);
      }
    });
    box2.add(tf);
    box.add(box2);
    box.setAlignmentX(0.5f);
    return box;
  }
  
  /**
   * Builds the deletion key list interface
   */
  private void addDelKeylist(){
    Box b = Box.createVerticalBox();
    
    //ORBITALS////////////
    Box b2 = Box.createHorizontalBox();
    b2.add(new JLabel("Orbital #s:"));
    final JTextField tf = new JTextField();
    tf.getDocument().addDocumentListener(new DocumentListener(){
      @Override
      public void changedUpdate(DocumentEvent arg0) {
        delOrbs = tf.getText();
      }
      @Override
      public void insertUpdate(DocumentEvent arg0) {
        delOrbs = tf.getText();
      }
      @Override
      public void removeUpdate(DocumentEvent arg0) {
        delOrbs = tf.getText();
      }
    });
    b2.add(tf);
    b2.setBorder(BorderFactory.createTitledBorder("Delete orbitals (ex: 1 22 13 ...)"));
    b2.setAlignmentX(0);
    b.add(b2);
    
    //ELEMENTS//////////
    final JButton btn2 = new JButton("Add Element");
    if(deleteElements == null) deleteElements = new ArrayList<String>(10);
    final JPanel elems = new JPanel(new FlowLayout());
    //elems.add(new JLabel("Element: "));
    final JScrollPane p2 = new JScrollPane();
    btn2.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent e){
        addDelFieldEl(elems, btn2 ,p2,"","",deleteElements,0);
      }
    });
    //addDelFieldEl(elems,p2,"","",deleteElements,0);
    elems.add(btn2);
    p2.setBorder(BorderFactory.createTitledBorder("Delete single Fock matrix elements"));
    p2.getViewport().add(elems);
    p2.setPreferredSize(new Dimension(300,80));
    p2.setAlignmentX(0);
    b.add(p2);
    
    //DELETE BLOCKS//////////////////
    if(deleteBlocks == null){ 
      deleteBlocks = new ArrayList<String>(5);
      deleteBlocks.add("");
    }
    final Box box = Box.createVerticalBox();
    box.setAlignmentX(0);
    final JButton btn = new JButton("Add Deletion Block");
    btn.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent e){
        deleteBlocks(btn,box, deleteBlocks, 0);
      }
    });
    box.add(btn);
    box.setBorder(BorderFactory.createTitledBorder("Delete blocks of Fock matrix"));
    box.setMaximumSize(new Dimension(320,1000));
    b.add(box);
    
    //LEWIS UNIT////////////
    b2 = Box.createHorizontalBox();
    b2.add(new JLabel("Orbital #s:"));
    final JTextField tf2 = new JTextField();
    tf2.getDocument().addDocumentListener(new DocumentListener(){
      @Override
      public void changedUpdate(DocumentEvent arg0) {
        delOrbsL = tf2.getText();
      }
      @Override
      public void insertUpdate(DocumentEvent arg0) {
        delOrbsL = tf2.getText();
      }
      @Override
      public void removeUpdate(DocumentEvent arg0) {
        delOrbsL = tf2.getText();
      }
    });
    b2.add(tf2);
    b2.setBorder(BorderFactory.createTitledBorder("Delete non-Lewis orbitals (ex: 1 22 13 ...)"));
    b2.setAlignmentX(0);
    b.add(b2);
    
    //DELOC/////////////
    final JButton btn4 = new JButton("Add Delocalization");
    
    if(delDeloc == null) delDeloc = new ArrayList<String>(5);
    final JPanel elems2 = new JPanel(new FlowLayout());
    final JScrollPane p3 = new JScrollPane();
    btn4.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent e){
        addDelFieldEl(elems2, btn4 ,p3,"From","To",delDeloc,0);
      }
    });
    elems2.add(btn4);
    //addDelFieldEl(elems2,p3,"FROM","TO",delDeloc,0);
    p3.setBorder(BorderFactory.createTitledBorder("Zero delocilation between molecular units"));
    p3.getViewport().add(elems2);
    p3.setPreferredSize(new Dimension(300,80));
    p3.setAlignmentX(0);
    b.add(p3);
    
    //DELETE ATOM BLOCKS///////
    if(deleteAtomBlocks == null){ 
      deleteAtomBlocks = new ArrayList<String>(5);
      deleteAtomBlocks.add("");
    }
    final Box box2 = Box.createVerticalBox();
    box2.setAlignmentX(0);
    final JButton btn3 = new JButton("Add Deletion Block");
    btn3.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent e){
        deleteBlocks(btn3,box2, deleteAtomBlocks,0);
      }
    });
    box2.add(btn3);
    box2.setBorder(BorderFactory.createTitledBorder("Zero delocalizations between atom blocks"));
    box2.setMaximumSize(new Dimension(320,1000));
    b.add(box2);
    
    //OTHER OPTIONS///////////
    b2 = Box.createVerticalBox();
    final JCheckBox cb = new JCheckBox("Delete all non-Lewis orbitals");
    cb.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent e){
        if(cb.isSelected()) deleteLewisOrbs = true;
        else deleteLewisOrbs = false;
      }
    });
    cb.setAlignmentX(0);
    b2.add(cb);
    final JCheckBox cb2 = new JCheckBox("Delete all vicinal delocalizations");
    cb2.setAlignmentX(0);
    b2.add(cb2);
    cb2.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent e){
        if(cb2.isSelected()) deleteVicinal = true;
        else deleteVicinal = false;
      }
    });
    final JCheckBox cb3 = new JCheckBox("Delete all geminal delocalizations");
    cb3.setAlignmentX(0);
    b2.add(cb3);
    cb3.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent e){
        if(cb3.isSelected()) deleteGeminal = true;
        else deleteGeminal = false;
      }
    });
    b2.setBorder(BorderFactory.createTitledBorder("Other options"));
    //b2.add(Box.createRigidArea(new Dimension(300,0)));
    b.add(b2);
    JScrollPane sp = new JScrollPane();
    sp.getViewport().add(b);
    sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    editBox.add(sp);
    b.setMinimumSize(new Dimension(300,400));
  }
  
  protected void addDelFieldEl(final JPanel b, final JButton btn, final JScrollPane p, final String s1, final String s2, 
                               final List<String> deleteElements, final int val){
    final JTextField tf = new JTextField();
    final JTextField tf2 = new JTextField();
    p.remove(btn);
    deleteElements.add("");
    DocumentListener listener = new DocumentListener(){
      @Override
      public void changedUpdate(DocumentEvent arg0) {
        deleteElements.set(val, tf.getText() + " " + tf2.getText());
      }
      @Override
      public void insertUpdate(DocumentEvent arg0) {
        deleteElements.set(val, tf.getText() + " " + tf2.getText());
      }
      @Override
      public void removeUpdate(DocumentEvent arg0) {
        deleteElements.set(val, tf.getText() + " " + tf2.getText());
        //deleteElements.set(val, s1 + " " + tf.getText() + " " + s2 + " "+ tf2.getText() + "  ");
      }
    };
    tf.getDocument().addDocumentListener(listener);
    tf2.getDocument().addDocumentListener(listener);
    tf.setPreferredSize(new Dimension(30,30));
    tf2.setPreferredSize(new Dimension(30,30));
    if(val!=0)b.add(new JLabel(","));
    b.add(new JLabel(s1));
    b.add(tf);
    b.add(new JLabel(s2));
    b.add(tf2);
    b.repaint();
    b.revalidate();
    for(ActionListener a:btn.getActionListeners())
      btn.removeActionListener(a);
    btn.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent e){
        addDelFieldEl(b,btn,p,s1,s2,deleteElements,val+1);
      }
    });
    b.add(btn);
    SwingUtilities.invokeLater(new Runnable(){
      @Override
      public void run(){
        tf.requestFocus();
        p.getHorizontalScrollBar().setValue(p.getHorizontalScrollBar().getMaximum()+30);
      }});
  }
  
  protected void deleteBlocks(final JButton btn, final Box container, final ArrayList<String> deleteBlocks, final int val){
    for(ActionListener a:btn.getActionListeners())
      btn.removeActionListener(a);
    container.remove(btn);
    deleteBlocks.add("");
    final Box blockBox = Box.createHorizontalBox();
    Integer[] intOps = new Integer[] {new Integer(1), 2, 3, 4, 5, 6, 7, 8, 9, 10};
    final JComboBox<Integer> cBox = new JComboBox<Integer>(intOps);
    cBox.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent e){
        
      }
    });
    cBox.setMaximumSize(new Dimension(40,30));
    Box b3 = Box.createHorizontalBox();
    b3.add(new JLabel("BLOCK "));
    b3.add(cBox);
    b3.add(new JLabel(" by "));
    b3.setAlignmentX(0.0f);
    final JComboBox<Integer> cBox2 = new JComboBox<Integer>(intOps);
    cBox2.setMaximumSize(new Dimension(40,30));
    b3.add(cBox2);
    Box b2 = Box.createVerticalBox();
    b2.add(b3);
    JButton btn2 = new JButton("Remove Block");
    btn2.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent e){
        container.remove(blockBox);
        container.repaint();
        container.revalidate();
      }
    });
    btn2.setAlignmentX(0.0f);
    b2.add(btn2);
    blockBox.add(b2);
    cBox2.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent e){
        
      }
    });
    b2 = Box.createVerticalBox();
    final JTextField tf = new JTextField();
    final JTextField tf2 = new JTextField();
    DocumentListener listener = new DocumentListener(){
      @Override
      public void changedUpdate(DocumentEvent arg0) {
        String s = tf.getText().trim();
        String s2 = tf2.getText().trim();
        int v1 = s.split(" ").length;
        int v2 = s2.split(" ").length;
        cBox.setSelectedIndex(v1-1);
        cBox2.setSelectedIndex(v2 - 1);
        deleteBlocks.set(val, tf.getText() + "\n  " + tf2.getText());
        deleteBlocks.set(0, v1 + " " + v2);
      }
      @Override
      public void insertUpdate(DocumentEvent arg0) {
        String s = tf.getText().trim();
        String s2 = tf2.getText().trim();
        int v1 = s.split(" ").length;
        int v2 = s2.split(" ").length;
        cBox.setSelectedIndex(v1-1);
        cBox2.setSelectedIndex(v2 - 1);
        deleteBlocks.set(val, tf.getText() + "\n  " + tf2.getText());
        deleteBlocks.set(0, v1 + " " + v2);
      }
      @Override
      public void removeUpdate(DocumentEvent arg0) {
        String s = tf.getText().trim();
        String s2 = tf2.getText().trim();
        int v1 = s.split(" ").length;
        int v2 = s2.split(" ").length;
        cBox.setSelectedIndex(v1-1);
        cBox2.setSelectedIndex(v2 - 1);
        deleteBlocks.set(val, tf.getText() + "\n  " + tf2.getText());
        deleteBlocks.set(0, v1 + " " + v2);
      }
    };
    tf.getDocument().addDocumentListener(listener);
    tf2.getDocument().addDocumentListener(listener);
    b2.add(tf);
    b2.add(tf2);
    b2.setPreferredSize(new Dimension(100,80));
    blockBox.add(b2);
    btn.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent e){
        deleteBlocks(btn,container, deleteBlocks, val+1);
      }
    });
    container.add(blockBox);
    container.add(btn);
    container.repaint();
    container.revalidate();
  }
  
  protected String getDelList(){
    String tmp = "$DEL";
    if(!delOrbs.trim().equals(""))
      tmp += "\n  DELETE " + delOrbs.split("\\s+").length + " ORBITALS " + delOrbs;
    if(deleteElements.size()!=0){
      tmp += "\n  DELETE " + deleteElements.size() + " ELEMENTS ";
      for(String x:deleteElements)
        tmp += x + "  ";
    }
    if(deleteBlocks.size()!=0){
      tmp += "\n  DELETE " + (deleteBlocks.size()-1) + " BLOCKS ";
      for(int i = 1; i< deleteBlocks.size();){
        
      }
    }
    
    if(delDeloc.size()!=0){
      tmp += "\n  ZERO " + delDeloc.size() + " UNITS ";
      for(String x:delDeloc)
        tmp += x + "  ";
    }
    
    if(!delOrbsL.trim().equals(""))
      tmp += "\n  ULEWIS " + delOrbsL.split("\\s+").length + " UNITS " + delOrbsL;
    if(deleteLewisOrbs)
      tmp += "\n  LEWIS";
    if(deleteGeminal)
      tmp += "\n  NOGEM";
    if(deleteVicinal)
      tmp += "\n NOVIC";
    return (tmp.equals("$DEL") ? null : tmp + "\n$END");
  }
  
  protected void resetRunFile(){
    chooseBonds = new Hashtable<String,String>();
    lonePairs = new Hashtable<String,String>();
    choose3C = new Hashtable<String,String>();
    delOrbs = delOrbsL =  "";
    deleteElements = new ArrayList<String>();
    deleteBlocks = new ArrayList<String>();
    delDeloc = new ArrayList<String>();
    deleteAtomBlocks = new ArrayList<String>();
  }
  
  protected void removeListParams(List<String> list, DefaultListModel<String> listModel) {
    appendOutputWithCaret("Keyword(s) removed:",'p');
    for(String x : list){
      listModel.removeElement(x);
      if (nboKeywords.toUpperCase().contains(x.toUpperCase())){
        nboKeywords = nboKeywords.substring(0, nboKeywords.indexOf(x.toUpperCase()))+ nboKeywords.substring(nboKeywords.indexOf(x.toUpperCase())
            + x.length() );
        appendOutputWithCaret("  " + x,'i');
      }
    }
  }
  
  @Override
  synchronized protected void goRunClicked(String keywords, String ess, File inputFile,Runnable whenDone){
//    if (inputFile == null) {
//      keywords = PT.clean(jtSelectAtoms.getText());
//      ess = "gennbo";
//      inputFile = this.inputFile;
//    } else {
//        ess = "gennbo";
//    }
    runJob(keywords, inputFile, ess, whenDone);
  }
  
  private void runJob(String keywords, final File inputFile, String ess,
                      final Runnable whenDone) {
    String label = "";
    for (String x : keywords.split(" ")) {
      x = x.toUpperCase();
      if (!nboKeywords.contains(x))
        label += x + " ";
    }
    if(fileData == null) readInputFile(inputFile);
    //nboKeywords = "FILE=" + plotFileName.getText().trim() +" "+nboKeywords;
    nboKeywords += label;
    label = "";
    try {
      nboService.writeToFile(getFileContents(),inputFile);
    } catch (IOException e) {
      Logger.info("Could not create " + inputFile);
      return;
    }
    final SB sb = new SB();
    appendToFile("GLOBAL C_PATH " + inputFile.getParent() + sep, sb);
    appendToFile("GLOBAL C_JOBSTEM " + jobStem + sep, sb);
    appendToFile("GLOBAL C_ESS " + ess.toLowerCase() + sep, sb);
    String st = plotFileName.getText().trim();
    appendToFile("GLOBAL C_LABEL_1 FILE="+(st.equals("")?jobStem:st), sb);
    nboService.queueJob("run", "running " + ess + "...", new Runnable() {
       @Override
       public void run() {
         nboService.rawCmdNew("r", sb, true, NBOService.MODE_RUN);
         try {
           if (whenDone != null)
             nboService.queueJob("load file", "", whenDone);//whenDone.run();
         } catch (Exception e) {
           e.printStackTrace();
         }
       }
    });
  }
}

