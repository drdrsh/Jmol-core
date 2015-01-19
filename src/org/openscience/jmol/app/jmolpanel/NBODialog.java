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
package org.openscience.jmol.app.jmolpanel;

import org.jmol.viewer.Viewer;
import org.jmol.i18n.GT;
import org.jmol.util.Logger;
import org.openscience.jmol.app.jmolpanel.JmolPanel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.io.File;

/**
 * A dialog for interacting with NBO-Server (experimental)
 */
public class NBODialog extends JDialog implements ChangeListener {

  private transient Viewer vwr;
  
  protected JButton    nboPathButton;
  //protected JTextField commandLineField;
  protected JTextField Field;
  protected JTextField dataPathLabel;
  protected JTextField serverPathLabel;

  private JTextField saveField;

  private JTextField workingPathLabel;

  private JTextPane editArea1;

  private JScrollPane editPane1;

  private JTextPane editArea2;

  private JScrollPane editPane2;

  private JTabbedPane inputTabs;

  private Component modelPanel;

  private Component runPanel;

  private JTextField modelField;
  

  /**
   * Creates a dialog for getting info related to output frames in
   *  nbo format.
   * @param f The frame assosiated with the dialog
   * @param vwr The interacting display we are reproducing (source of view angle info etc)
   */
  public NBODialog(JFrame f, Viewer vwr) {

    super(f, GT._("NBO-Server Interface Setup"), false);
    this.vwr = vwr;

    
    
    JPanel container = new JPanel();
    container.setPreferredSize(new Dimension(700,500));  
    container.setLayout(new BoxLayout(container,BoxLayout.Y_AXIS));
    JPanel leftPanel = buildLeftPanel();    
    JPanel rightPanel = buildRightPanel();
    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
        leftPanel, rightPanel);
    splitPane.setOneTouchExpandable(true);  
    JPanel splitPanel = new JPanel(new BorderLayout());
    splitPanel.add(splitPane, BorderLayout.CENTER);
    container.add(splitPanel);

    JPanel filePanel = buildFilePanel();
    JPanel buttonPanel = buildButtonPanel();
    
    JPanel gluePanel = new JPanel(new BorderLayout());
    gluePanel.add(Box.createGlue(), BorderLayout.NORTH);
    gluePanel.add(filePanel, BorderLayout.SOUTH);
    container.add(gluePanel);
    container.add(buttonPanel);
    getContentPane().add(container);    
    
    getPathHistory();
    pack();
    centerDialog();
    setVisible(true);
    splitPane.setDividerLocation(0.5);
  }

  private JPanel buildButtonPanel() {
    
    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

    //GUI for panel with go, cancel and stop (etc) buttons
    Box buttonBox = Box.createHorizontalBox();
    buttonBox.add(Box.createGlue());
    JButton closeButton = new JButton("Close");
    closeButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        closePressed();
      }
    });
    buttonBox.add(closeButton);
    buttonPanel.add(buttonBox);
    return buttonPanel;
  }

  private JPanel buildFilePanel() {
    
    JPanel filePanel = new JPanel(new BorderLayout());

    
    //GUI for working path selection
    Box workingPathBox = Box.createHorizontalBox();
    workingPathBox.setBorder(new TitledBorder("Working Directory"));
    workingPathLabel = new JTextField("");
    workingPathLabel.setEditable(false);
    workingPathLabel.setBorder(null);
    workingPathBox.add(workingPathLabel);
    JButton pathButton = new JButton("Browse...");
    pathButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        showWorkingPathDialog();
      }
    });
    workingPathBox.add(pathButton);
    filePanel.add(workingPathBox, BorderLayout.NORTH);

    //GUI for NBO path selection
    Box nboPathBox = Box.createHorizontalBox();
    nboPathBox.setBorder(new TitledBorder("Location of the NBO-Server Executable"));
    serverPathLabel = new JTextField("");
    serverPathLabel.setEditable(false);
    serverPathLabel.setBorder(null);
    nboPathBox.add(serverPathLabel);
    nboPathButton = new JButton("Browse...");
    nboPathButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        showNBOPathDialog();
      }
    });
    nboPathBox.add(nboPathButton);
    filePanel.add(nboPathBox, BorderLayout.SOUTH);
    return filePanel;
  }

  private JPanel buildRightPanel() {
    
    JPanel editPanel = new JPanel();
    editPanel.setLayout(new BoxLayout(editPanel, BoxLayout.Y_AXIS));
    
    TitledBorder editTitle =
      BorderFactory.createTitledBorder("NBO Working File (temp)");
    editPanel.setBorder(editTitle);
  
    editArea1 = new JTextPane();
    editArea1.setContentType("text/plain");
    editArea1.setFont(new Font("Monospaced", Font.PLAIN, 10));
    editPane1 = new JScrollPane(editArea1);
    editPanel.add(editPane1);
    JButton goButton = new JButton("Execute");
    goButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        executeRaw();
      }
    });
    editPanel.add(goButton);
    editArea2 = new JTextPane();
    editArea2.setContentType("text/plain");
    editArea2.setFont(new Font("Monospaced", Font.PLAIN, 10));
    editPane2 = new JScrollPane(editArea2);
    //editPane2.setPreferredSize(new Dimension(500,100));  
    editPanel.add(editPane2);

    editPanel.setPreferredSize(new Dimension(500,100));
    return editPanel;
  }

  protected void executeRaw() {
    goPressed("raw");
    
  }

  private JPanel buildLeftPanel() {

    JPanel showPanel = new JPanel(new BorderLayout());
    inputTabs = new JTabbedPane();
    inputTabs.addTab("Model", null, modelPanel = getModelPanel());
    inputTabs.addTab("Run", null, runPanel = getRunPanel());
    inputTabs.setSelectedComponent(modelPanel);
    inputTabs.addChangeListener(this);
    showPanel.add(inputTabs,  BorderLayout.CENTER);
    return showPanel;
  }

  @Override
  public void stateChanged(ChangeEvent event) {
    if (event.getSource() == inputTabs) {
      tabSwitched();
    }
  }

  private void tabSwitched() {
    Component c = inputTabs.getSelectedComponent();
    if (c == modelPanel) {
      modelPanel.setVisible(true);
      runPanel.setVisible(false);
    } else if (c == runPanel) {
      modelPanel.setVisible(false);
      runPanel.setVisible(true);
    }
    pack();
  }

  private Component getRunPanel() {
    //Box: Window
    JPanel showPanel = new JPanel(new BorderLayout());
    //Box: Main
    Box rootBox = Box.createVerticalBox();
    rootBox.setBorder(new TitledBorder("NBO Data Files and Directory"));    
    Box saveBox = Box.createHorizontalBox();
    saveBox.setBorder(new TitledBorder("File Root"));
    saveField = new JTextField("Jmol");
    saveBox.add(saveField);
    rootBox.add(saveBox);
    rootBox.add(Box.createGlue());

    showPanel.add(rootBox, BorderLayout.CENTER);

    return showPanel;
  }

  private Component getModelPanel() {
    JPanel showPanel = new JPanel(new BorderLayout());
    Box a = Box.createVerticalBox();
    Box b = Box.createHorizontalBox();
    b.add(newLabel("Formula"));
    b.add(modelField = new JTextField("CH4"));
    a.add(b);
    a.add(Box.createVerticalGlue());
    b = Box.createHorizontalBox();
    b.add(Box.createHorizontalGlue());
    JButton goButton = new JButton("Load");
    goButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        loadModel();
      }
    });
    b.add(goButton);
    a.add(b);

    showPanel.add(a, BorderLayout.NORTH);
    //showPanel.add(Box.createGlue(), BorderLayout.CENTER);
    return showPanel;
  }

  protected void loadModel() {
    goPressed("model");
    vwr.loadInline(editArea2.getText());
  }

  private Component newLabel(String label) {
    JTextField t = new JTextField(label);
    t.setEditable(false);
    t.setBorder(null);
    return t;
  }

  private String lastText;
  
  void goPressed(String type) {

    String filename = "jmol_infile.txt";
    String workingPath = workingPathLabel.getText();
    File toNBOFile = new File(workingPath, filename);
    String s;
    if (type.equals("model")) {
      editArea1.setText("    1    1    1\nsh " + modelField.getText());
    } else if (type.equals("raw")) {

    }
    try {
      s = editArea1.getText();
      if (!s.equals(lastText)) {
        vwr.writeTextFile(toNBOFile.getAbsolutePath(), lastText = s);
      }

      String server = serverPathLabel.getText();
      File pathToExecutable = new File(server);
      ProcessBuilder builder = new ProcessBuilder(server);
      builder.directory(new File(pathToExecutable.getParent())); // this is where you set the root folder for the executable to run with
      builder.redirectErrorStream(true);
      Process proc = builder.start();
      //Process proc = Runtime.getRuntime().exec(commandLineArgs);
      proc.waitFor();
      processResult();
    } catch (Exception e) {
      Logger.errorEx("Caught IOException in NBO-Server exec", e);
    }
    //saveHistory();
    //dispose();
  }       

  /**
   * Responds to cancel being press- or equivalent eg window closed.
   */
  void closePressed() {
    saveHistory();
    setVisible(false);
    dispose();
  }

  @Override
  public void setVisible(boolean b) {
    super.setVisible(b);    
  }
  /**
   * Show a file selector when the savePath button is pressed.
   */
  void showWorkingPathDialog() {

    JFileChooser myChooser = new JFileChooser();
    myChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    String fname = workingPathLabel.getText();
    myChooser.setSelectedFile(new File(fname));

    int button = myChooser.showDialog(this, GT._("Select"));
    if (button == JFileChooser.APPROVE_OPTION) {
      File newFile = myChooser.getSelectedFile();
      String savePath;
      if (newFile.isDirectory()) {
        savePath = newFile.toString();
      } else {
        savePath = newFile.getParent();
      }
      workingPathLabel.setText(savePath);
      loadArea1();
      pack();
    }
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
      serverPathLabel.setText(newFile.toString());
      pack();
    }
  }

  /**
   * Save INI file
   * 
   * @return INI data
   */
  

  /**
   * Centers the dialog on the screen.
   */
  protected void centerDialog() {

    Dimension screenSize = this.getToolkit().getScreenSize();
    Dimension size = this.getSize();
    screenSize.height = screenSize.height / 2;
    screenSize.width = screenSize.width / 2;
    size.height = size.height / 2;
    size.width = size.width / 2;
    int y = screenSize.height - size.height;
    int x = screenSize.width - size.width;
    this.setLocation(x, y);
  }

  /**
   * Just recovers the path settings from last session.
   */
  private void getPathHistory() {

    java.util.Properties props = JmolPanel.historyFile.getProperties();
    if (serverPathLabel != null) {
      String nboPath = props.getProperty("nboServerPath",
        System.getProperty("user.home"));
      if (nboPath != null) {
        serverPathLabel.setText(nboPath);
      }
    }
    if (workingPathLabel != null) {
      String savePath = props.getProperty("nboWorkingPath",
        System.getProperty("user.home"));
      if (savePath != null) {
        workingPathLabel.setText(savePath);
        loadArea1();
      }
    }
  }

  /**
   * Just saves the path settings from this session.
   */
  private void saveHistory() {
    java.util.Properties props = new java.util.Properties();
    props.setProperty("nboServerPath", serverPathLabel.getText());
    props.setProperty("nboWorkingPath", workingPathLabel.getText());
    JmolPanel.historyFile.addProperties(props);
  }

  String doubleQuoteIfContainsSpace(String str) {
    for (int i = str.length(); --i >= 0; )
      if (str.charAt(i) == ' ')
        return "\"" + str + "\"";
    return str;
  }

  String simpleQuoteIfContainsSpace(String str) {
    for (int i = str.length(); --i >= 0; )
      if (str.charAt(i) == ' ')
        return "\'" + str + "\'";
    return str;
  }
  
  void loadArea1() {
    String s = vwr.getFileAsString3(workingPathLabel.getText().replace('\\', '/') + "/jmol_infile.txt", true, "nbodialog");
    editArea1.setText(s);
    lastText = s;
    editArea2.setText("");
  }
  
  void processResult() {
    loadArea1();
    loadArea2("jmol_molfile.txt");
  }

  private void loadArea2(String fname) {
    String s = vwr.getFileAsString3(workingPathLabel.getText().replace('\\', '/') + "/" + fname, true, "nbodialog");
    editArea2.setText(s);
  }


}
