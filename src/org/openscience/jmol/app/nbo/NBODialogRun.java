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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
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
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jmol.i18n.GT;
import org.jmol.util.Logger;
import org.openscience.jmol.app.jmolpanel.GaussianDialog;
import org.openscience.jmol.app.jmolpanel.JmolPanel;

abstract class NBODialogRun extends NBODialogModel {

  private String useExt;
  protected GaussianDialog gau;
  protected JFrame runFrame;
  protected DefaultListModel<String> sList;
  protected JList<String> keywords;
  private JComboBox<String> editOps;
  private JButton run,save;
  private DefaultComboBoxModel<String> editModel;
  String fileData, fileData2, params;

  protected NBODialogRun(JFrame f) {
    super(f);
    runFrame = f;
  }

  protected void buildRun(Container p) {
    useExt = "47";
    java.util.Properties props = JmolPanel.historyFile.getProperties();
    workingPath = (props.getProperty("workingPath",
        System.getProperty("user.home")));
    p.removeAll();
    p.setLayout(new BorderLayout());
    if(topPanel == null) topPanel=buildTopPanel();
    p.add(topPanel, BorderLayout.PAGE_START);
    JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,runS(),modelOut());
    sp.setDividerLocation(430);
    sp.setBorder(BorderFactory.createLoweredBevelBorder());
    p.add(sp, BorderLayout.CENTER);
    NBOPanel status = new NBOPanel(this, PANEL_STATUS);
    status.setLayout(new FlowLayout(FlowLayout.LEFT));
    statusLab.setText("");
    p.add(statusPanel, BorderLayout.PAGE_END);
    if(isJmolNBO) setInputFile(inputFile,"47",null);
  }
  
  protected JPanel runS() {
    params = "";
    JPanel runPanel = new JPanel();
    runPanel.setLayout(new BoxLayout(runPanel, BoxLayout.Y_AXIS));
    Box box = Box.createHorizontalBox();
    box.add(new JLabel("ESS  ")).setFont(new Font("Arial",Font.BOLD,25));
    String[] essList = {"GenNBO", "GO9", "GAMESS" };
    action = new JComboBox<String>(essList);
    action.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        essChanged();
      }
    });
    box.add(action);
    box.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 5));
    runPanel.add(box).setMaximumSize(new Dimension(500, 60));
    runPanel.add(new JSeparator());
    box = Box.createHorizontalBox();
    box.add(new JLabel("INPUT")).setFont(new Font("Arial",Font.BOLD,25));
    box.add(folderBox());
    browse.setEnabled(true);
    runPanel.add(box);
    runPanel.add(new JSeparator());
    box = Box.createHorizontalBox();
    box.add(new JLabel("EDIT ")).setFont(new Font("Arial",Font.BOLD,25));
    editModel = new DefaultComboBoxModel<String>();
    editModel.addElement("-type-");
    editModel.addElement("$NBO Keylist");
    box.add(editOps=new JComboBox<String>(editModel));
    editOps.setEnabled(isJmolNBO);
    editOps.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        editOpChanged();
      }
    });
    box.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 5));
    runPanel.add(box).setMaximumSize(new Dimension(500, 60));
    (editBox = Box.createVerticalBox()).setBorder(BorderFactory.createLoweredBevelBorder());
    editBox.add(Box.createRigidArea(new Dimension(430,230)));
    runPanel.add(editBox);
    box = Box.createHorizontalBox();
    box.add(run = new JButton("Run")).setFont(new Font("Arial",Font.PLAIN,20));
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
    box.add(Box.createRigidArea(new Dimension(100,0)));
    save = new JButton("Save File");
    save.setEnabled(isJmolNBO);
    save.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          nboService.writeToFile(fileData+" "+params+sep+fileData2, inputFile);
          //System.out.println(fileData);
        } catch (IOException e1) {
          appendModelOutPanel("-Error saving file-");
          return;
        }
        appendModelOutPanel("-File saved-");
      }
    });
    box.add(save);
    run.setEnabled(isJmolNBO);
    run.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        goRunClicked(nv.getText(),inputFile,null);
      }
    });
    runPanel.add(box);
    if(isJmolNBO){
      getParams(inputFile);
      editOps.setSelectedIndex(1);
    }
    return runPanel;
  }
  
  protected void editOpChanged(){
    Object item = editOps.getSelectedItem();
    if(item.equals("-type-")){
      editBox.removeAll();
      editBox.add(Box.createRigidArea(new Dimension(430,230)));
    }else if(item.equals("$NBO Keylist")){
      addList();
    }else if(item.equals("Gaussian Input File")){
      editBox.removeAll();
      JScrollPane p = new JScrollPane();
      editBox.setSize(new Dimension(430,230));
      gau = new GaussianDialog(runFrame, vwr);
      p.setViewportView(gau.getContentPane());
      editBox.add(p);
    }
    setComponents(editBox);
    this.repaint();
    this.revalidate();
  }

  @Override
  protected void showWorkpathDialogR(String workingPath) {
    JFileChooser myChooser = new JFileChooser();
    myChooser.setFileFilter(new FileNameExtensionFilter(useExt, useExt));
    myChooser.setFileHidingEnabled(true);
    myChooser.setSelectedFile(new File(workingPath));
    int button = myChooser.showDialog(this, GT._("Select"));
    if (button == JFileChooser.APPROVE_OPTION) {
      inputFile = myChooser.getSelectedFile();
      setInputFile(inputFile,useExt, showWorkPathDone);
      editOps.setEnabled(true);
      getParams(inputFile);
      appendModelOutPanel("File loaded:\n  "+jobStem+"."+useExt);
      editOps.setSelectedIndex(1);
      if(!useExt.equals("47")) nboService.runScriptQueued("load \"input::"+inputFile.toString()+"\"");
      run.setEnabled(true);
      atNum.setEnabled(true);
      save.setEnabled(true);
      nboResetV();
      saveHistory();
      isJmolNBO = isJmolNBO();
    }
  }
  
  protected void getParams(File inputFile) {
    params = "";
    String data = nboService.getFileData(inputFile.toString());
    String[] tokens = PT.split(data, "$END");
    boolean atParams = false;
    SB fout = new SB(), fout2 = new SB();
    if(tokens.length<=0) return;
    for (int i = 0;;) {
      String s = tokens[i];
      //System.out.println("----"+s);
      s = PT.trim(s, "\t\r\n ");
      if(!atParams){
        if (s.indexOf("$NBO") >= 0) {
          atParams=true;
          if(PT.split(s, "$NBO").length>1)
          params = cleanParams(PT.split(s, "$NBO")[1]);
          s = s.substring(0,s.indexOf("$NBO"))+"$NBO";// + params;
          Logger.info("Params read: " + params);
        }
        fout.append(s).append(sep);
        if (++i == tokens.length)
          break;
        if(!atParams)
          fout.append("$END").append(sep);
        else 
          fout2.append("$END").append(sep);
      }else{
        fout2.append(s).append(sep);
        if (++i == tokens.length)
          break;
        fout2.append("$END").append(sep);
      }
    }
    fileData = fout.toString();
    fileData2 = fout2.append(sep).toString();
  }

  private String cleanParams(String params) {
    String[] tokens = PT.getTokens(PT.rep(PT.clean(params), "file=", "FILE="));
    String strOut = " ";
    for (int i = 0; i < tokens.length; i++) {
      String key = " " + tokens[i] + " ";
      if (key.indexOf("=") < 0)
        key = key.toUpperCase();
      if (strOut.indexOf(key) < 0) {
        if (strOut.length() + key.length() - strOut.lastIndexOf(sep) >= 80)
          strOut += sep + " ";
        strOut += key.substring(1);
      }
    }
    return strOut.trim()+" ";
  }

  protected void addList() {
    if(inputFile!=null){
      editBox.removeAll();
      editBox.setSize(new Dimension(430,230));
      sList = new DefaultListModel<String>();
      for (String s : params.split(" |\\n"))
        if (s.length() > 1)
          sList.addElement(s);
      keywords = new JList<String>(sList);
      keywords.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          if (e.getClickCount() == 2)
            removeParams(keywords.getSelectedValuesList());
        }
      });
      Box box = Box.createHorizontalBox();
      JScrollPane p1 = new JScrollPane();
      p1.getViewport().add(keywords);
      p1.setBorder(new TitledBorder("Current Keywords:"));
      box.add(p1);
      Box b = Box.createVerticalBox();
      JButton but = new JButton("Add");
      but.setAlignmentX(0.5f);
      b.add(but).addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
        }
      });
      (but = new JButton("Remove")).setAlignmentX(0.5f);
      b.add(but).addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          if(!keywords.isSelectionEmpty())
            removeParams(keywords.getSelectedValuesList());
        }
      });
      box.add(b);
      editBox.add(box);
      nv = new JTextField();
      nv.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          addParams(nv.getText().split(" "));
        }
      });
      nv.setBorder(new TitledBorder("Additional Keywords"));
      editBox.add(nv);
    }
  }

  protected void addParams(String[] s) {
    appendModelOutPanel("Keyword(s) added:");
    for (String x : s)
      if (!x.equals("")){
        if(x.toUpperCase().indexOf("FILE=")>=0){
          removeParams("FILE=");
          String st = "FILE="+x.substring(5);
          params += st + " ";
          sList.addElement(st);
          appendModelOutPanel("  " + st);
        }else if (!params.contains(x.toUpperCase())) {
          appendModelOutPanel("  " + x.toUpperCase());
            params += x.toUpperCase() + " ";
            sList.addElement(x.toUpperCase());
          }else{ 
            params += x + " ";
            sList.addElement(x);
          }
      }
      
    nv.setText("");
  }

  protected void removeParams(List<String> list) {
    appendModelOutPanel("Keyword(s) removed:");
    for(String x : list){
      sList.removeElement(x);
      if (params.toUpperCase().contains(x.toUpperCase())){
        params = params.substring(0, params.indexOf(x.toUpperCase()))+ params.substring(params.indexOf(x.toUpperCase())
            + x.length() );
        appendModelOutPanel("  " + x);
      }
    }
    String item = keywords.getSelectedValue();
    sList.removeElement(item);
  }
  
  private void removeParams(String str){
    String tmp = "";
    for(String x:params.split(" "))
      if(!x.contains(str))
        tmp = tmp.concat(x) + " ";
      else sList.removeElement(x);
    params = tmp;
  }

  protected void essChanged() {
    clearInputFile();
    run.setEnabled(false);
    save.setEnabled(false);
    editOps.setEnabled(false);
    editBox.removeAll();
    editBox.add(Box.createRigidArea(new Dimension(430,230)));
    appendModelOutPanel("ESS changed:\n  " + action.getSelectedItem().toString());
    Object item = action.getSelectedItem();
    if(item.equals("GenNBO")){
      useExt = "47";
      editModel.removeElement("Gaussian Input File");
      return;
    }else if(item.equals("GO9")){
      useExt = "gau";
      editModel.addElement("Gaussian Input File");
      return;
    }
  }

  @Override
  synchronized protected void goRunClicked(String keywords, File inputFile, Runnable whenDone) {
    String ess;
    if (inputFile == null) {
      keywords = PT.clean(nv.getText());
      ess = "gennbo";
      inputFile = this.inputFile;
    } else {
      if(action != null) ess = (String) action.getSelectedItem();
      else ess = "gennbo";
    }
    runJob(keywords, inputFile, ess, whenDone);
  }
  
 private void runJob(String keywords, final File inputFile, String ess, final Runnable whenDone) {
   String label = "";
   for (String x : keywords.split(" ")) {
     x = x.toUpperCase();
     if (!params.contains(x))
       label += x + " ";
   }
   if(fileData == null) getParams(inputFile);
   if(!params.toUpperCase().contains("FILE=")) params += "FILE=" + jobStem;
   params += label;
   label = "";
   try {
     nboService.writeToFile(fileData + " " + params + sep + fileData2,
         inputFile);
   } catch (IOException e) {
     Logger.info("Could not create " + inputFile);
     return;
   }
   final SB sb = new SB();
   appendToFile("GLOBAL C_PATH " + inputFile.getParent() + sep, sb);
   appendToFile("GLOBAL C_JOBSTEM " + jobStem + sep, sb);
   appendToFile("GLOBAL C_ESS " + ess + sep, sb);
   appendToFile("GLOBAL C_LABEL_1 FILE="+jobStem, sb);
   final String jobStem = this.jobStem;
   nboService.queueJob("run", "running " + ess + "...", new Runnable() {
      @Override
      public void run() {
        nboService.rawCmdNew("r", sb, true, NBOService.MODE_RUN);
        try {
          nboService.writeToFile(fileData+" "+params+sep+fileData2, inputFile);
          getParams(inputFile);
          addList();
          //nboService.runScriptQueued("load " + inputFile.toString());
          statusLab.setText("Complete: Output to " + inputFile.getParent()
              + "\\" + jobStem + ".nbo");
          if (whenDone != null)
            nboService.queueJob("load file", "", whenDone);//whenDone.run();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
   });
 }
}

