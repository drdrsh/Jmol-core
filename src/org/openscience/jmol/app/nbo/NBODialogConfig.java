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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javajs.util.SB;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.TitledBorder;

import org.jmol.i18n.GT;
import org.jmol.viewer.Viewer;
import org.openscience.jmol.app.jmolpanel.GuiMap;
import org.openscience.jmol.app.jmolpanel.JmolPanel;

abstract class NBODialogConfig extends JDialog {

  protected static final String sep = System.getProperty("line.separator");

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

  abstract protected boolean connect();
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
  protected JButton[] mainButtons;
  protected JTextField Field;
  protected JTextField dataPathLabel;
  protected JTextField serverPathLabel;
  
  protected JScrollPane editPane2;
  protected JLabel statusLab = new JLabel();

  protected JComboBox<String> action, module;
  protected JTextPane fileText;
  protected JButton go;

  protected String reqInfo;

  protected JTextPane jpNboOutput;

  protected String jobStem;
  JLabel icon;
  protected Font nboFont = new Font("Monospaced", Font.BOLD, 16);

  /**
   * Creates a dialog for getting info related to output frames in nbo format.
   * 
   * @param f
   *        The frame associated with the dialog
   */
  protected NBODialogConfig(JFrame f) {
    super(f, GT._("NBO Server Interface"), false);
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
  
  protected void buildConfig(Container p) {
    p.removeAll();
    p.setLayout(new BorderLayout());
    topPanel=buildTopPanel();
    p.add(topPanel,BorderLayout.NORTH);
    p.add(statusPanel,BorderLayout.SOUTH);
    p.add(buildFilePanel(),BorderLayout.CENTER);
    centerDialog(this);
  }
  
  private JPanel buildFilePanel() {
    JPanel filePanel = new JPanel(new BorderLayout());
    filePanel.setBorder(BorderFactory.createLoweredBevelBorder());

    //GUI for NBO path selection
    Box box = Box.createHorizontalBox();
    box.setBorder(BorderFactory.createTitledBorder(new TitledBorder("Location of NBOServe executable:")));
    serverPathLabel = new JTextField("");
    serverPathLabel.setEditable(false);
    serverPathLabel.setBorder(null);
    serverPathLabel.setText(nboService.serverPath);
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
        connect();
      }
    });
    box.add(b);
    filePanel.add(box, BorderLayout.NORTH);
    (jpNboOutput = new JTextPane()).setFont(new Font("Arial",Font.PLAIN,16));
    JScrollPane p = new JScrollPane();
    p.getViewport().add(jpNboOutput);
    p.setBorder(BorderFactory.createTitledBorder(new TitledBorder("NBO Output:")));
    filePanel.add(p, BorderLayout.CENTER);
    return filePanel;
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
    JPanel p = new JPanel(new GridBagLayout());
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
    p.add(tfFolder,c);
    c.gridx=1;
    (tfName = new JTextField()).setPreferredSize(new Dimension(100,20));
    p.add(tfName,c);
    c.gridx=0;
    c.gridy=1;
    p.add(new JLabel("         folder"),c);
    c.gridx=1;
    p.add(new JLabel("          name"),c);
    c.gridx=2;
    c.gridy=0;
    (tfExt = new JTextField()).setPreferredSize(new Dimension(40,20));
    p.add(tfExt,c);
    c.gridy=1;
    p.add(new JLabel("  ext"),c);
    c.gridx=3;
    c.gridy=0;
    c.gridheight=2;
    p.add(browse,c);
    //p.setPreferredSize(new Dimension(350, 70));
    return p;
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
  protected void showNBOPathDialog() {
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
    Dimension screenSize = d.getToolkit().getScreenSize();
    Dimension size = d.getSize();
//    screenSize.height = screenSize.height / 2;
//    screenSize.width = screenSize.width / 2;
//    size.height = size.height / 2;
//    size.width = size.width / 2;
    int y = //screenSize.height - size.height;
    d.getParent().getY();
    int x = Math.min(screenSize.width - size.width,
    d.getParent().getX()+d.getParent().getWidth());
    d.setLocation(x, y);
  }
  
  protected void appendOutputWithCaret(String line) {
    if (jpNboOutput == null)
      return;
    if (line.length() > 1)
      jpNboOutput.setText(jpNboOutput.getText() + line + "\n");
    try {
    jpNboOutput.setCaretPosition(jpNboOutput.getText().length());
    } catch (Exception e) {
      System.out.println(e + " in appendOutputWithCaret");
    }
  }
  
  protected void appendOutput(String cmd) {
    jpNboOutput.setText(jpNboOutput.getText() + cmd + "\n");
  }

  protected void clearOutput(){
    jpNboOutput.setText("");
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
  
  protected boolean checkJmolNBO(){
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

  protected static String helpConfig;
  
  protected static final String helpModel ="NBOModel COMMAND SYNTAX\n"
      +" \n"
      +"Command verbs are case-insensitive and can"
      +"be abbreviated by the leading unique characters."
      +"Arguments are separated by commas or spaces."
      +"Parameters are attached to the command verb"
      +"after a dot (viz., DRAW.ap MODEL).  Arguments"
      +"and parameters are case-insensitive, except"
      +"for chemical formulas and group acronyms."
      +"Use 'HELP <command>' (e.g., 'HELP SHOW') for"
      +"further specifics of each COMMAND type.\n"
      +" \n"
      +"COMMAND(.t)   arguments\n"
      +"------------------------------------\n"
      +"ALTER         IA [IB IC ID] newvalue\n"
      +"CLIP          IA IB\n"
      +"DRAW          filename\n"
      +"FUSE(.R)      IA IB\n"
      +"HELP          command\n"
      +"LINK          IA IB\n"
      +"MUTATE        IA formula\n"
      +"REBOND        IA symtype\n"
      +"ROTATE        AXIS angle\n"
      +"SAVE.t        filename\n"
      +"SHOW          formula\n"
      +"SWITCH        IA IB\n"
      +"SYMMETRY\n"
      +"TRANSLATE     AXIS shift\n"
      +"TWIST         IA IB IC ID newvalue\n"
      +"UNIFY         CFI1 CFI2 IA1 IB1 IA2 IB2 dist\n"
      +"USE.t         filename\n"
      +"VALUE         IA [IB IC ID]\n"
      +"3CHB          IA IB :Ligand\n",
      alterHelp = "ALTER IA newval     (nuclear charge of atom IA)\n"
      +"      IA IB newval          (bond length IA-IB)\n"
      +"      IA IB IC newval  (valence angle IA-IB-IC)\n"
      +"      IA IB IC ID newval (dihedral IA-IB-IC-IC)\n"
      +" \n"
      +"Examples:\n"
      +" ALTER 10 14.   [change atom 10 to Si (Z = 14)]\n"
      +" ALTER  2 5 1.69  [change R(5-8) bond to 1.69A]\n"
      +" ALTER  1 2 3 4 180.   [change 1-2-3-4 dihedral\n"
      +"                          angle to 180 degrees]\n"
      +" \n"
      +"Note that 'ALTER 1 2 3 4 180.' changes ONLY"
      +"the 1-2-3-4 dihedral (often giving unphysical"
      +"distorted geometry).  Use 'TWIST 1 2 3 4 180.'"
      +"to form a proper torsional rotamer.\n"
      +" \n"
      +"Use VFILE to determine which angles can be"
      +"safely ALTERed.  Otherwise, the coordinates"
      +"may be re-defined, with unexpected effects"
      +"on other variables.",
      clipHelp = "CLIP IA IB          (erase bond between IA, IB)\n"
      +" \n"
      +"Example:\n"
      +" CLIP 1 2        [erase bond between atoms 1,2]\n"
      +" \n"
      +"Note that CLIP takes no account of electronic"
      +"requirements for a Lewis-compliant model.",
      fuseHelp = "FUSE IA,IB       (remove IA,IB and join the two\n"
      +"                'dangling' sites by a new bond)\n"
      +" \n"
      +"Allowed parameter:\n"
      +" .r = ring-forming (conformational search)\n"
      +" \n"
      +"Examples:\n"
      +" FUSE 4 12    [remove atoms 4, 12 and draw a new\n"
      +"          bond between resulting radical centers\n"
      +"          (e.g., 3-11), with no geometry change]\n"
      +" FUSE.r 4 12      [similar, but a conformational\n"
      +"            search is performed to find the most\n"
      +"                 suitable ring-closing geometry]\n"
      +" \n"
      +"Note that IA, IB must have similar valency, so\n"
      +"the resulting structure remains Lewis-compliant.\n",
      linkHelp = "LINK IA IB  (draw a 'bond' between atoms IA, IB)\n"
      +"Examples:\n"
      +" LINK 3 17    [draws a 'bond: between atoms 3-17\n"
      +"Note that this command (unlike FUSE) takes no\n"
      +"account of chemical reasonability.\n",
      mutateHelp = "MUTATE IA formula (replace atom IA by the group\n"
      +"               of specified chemical 'formula',\n"
      +"             if both are of consistent valency)\n"
      +" \n"
      +"Example:\n"
      +" MUTATE 4 CH3     [remove monovalent atom 4 and\n"
      +"           attach a methyl (CH3) radical in its\n"
      +"         place, preserving valence consistency]\n",
      rebondHelp = "REBOND IA symtype   (select a new Lewis valence\n"
      +"                   isomer of 'symtype' symmetry\n"
      +"                   at transition metal atom IA)\n"
      +" \n"
      +"Allowed 'symtype' parameters (TM species only):\n"
      +" \n"
      +" ML6 bonding: c3vo      ('Outer' C3v [default])\n"
      +"              c3vi       ('Inner' C3v symmetry)\n"
      +"              c5vo       ('Outer' C5v symmetry)\n"
      +"              c5vi       ('Inner' C5v symmetry)\n"
      +" \n"
      +" ML5 bonding: c4vo      ('Outer' C4v [default])\n"
      +"              c4vi       ('Inner' C4v symmetry)\n"
      +" \n"
      +" ML4 bonding: td        (Td symmetry [default])\n"
      +"              c3vi       ('Inner' C3v symmetry)\n"
      +"              c4v        (C4v symmetry)\n"
      +"Example:\n"
      +" SHOW WH6       [Tungsten hexahydride, in ideal\n"
      +"                        'c3vo' isomer geometry]\n"
      +" REBOND 2 c5vi     [reform preceding WH6 isomer\n"
      +"                     to alternative 'inner C5v'\n"
      +"                         geometry at TM atom 2]\n",
      saveHelp = "SAVE.t filename     (save current model as file\n"
      +"              'filename' of type 't' extension)\n"
      +" \n"
      +"Parameters: \n"
      +" .v   = valence coordinate VFILE ([.vfi])\n"
      +" .c   = cartesian coordinate CFILE (.cfi)\n"
      +" .adf = ADF input file (.adf)\n"
      +" .g   = Gaussian input file (.gau)\n"
      +" .gms = GAMESS input file (.gms)\n"
      +" .jag = Jaguar input file (.jag)\n"
      +" .mm  = MM2 molecular mechanics file (.mm2)\n"
      +" .mnd = AM1/MINDO-type input file (.mnd)\n"
      +" .mp  = Molpro input file (.mp)\n"
      +" .nw  = NWChem input file (.nw)\n"
      +" .orc = Orca input file (.orc)\n"
      +" .pqs = PQS input file (.pqs)\n"
      +" .qc  = Q-Chem input file (.qc)\n"
      +"Example:\n"
      +" SAVE.G job   [save Gaussian-type 'job.gau' file]\n",
      showHelp = "SHOW <formula> (create a molecule model from\n"
      +"                its 'formula')\n"
      +"SHOW <acceptor> <donor-1> <donor-2>...\n"
      +"               (create supramolecular model from\n"
      +"                radical 'acceptor' and ligand\n"
      +"                'donor-i' formulas)\n"
      //+"SHOW.O         (Ortep plot of current species)\n"
      +"The chemical 'formula' is a valid Lewis-type"
      +"line formula, similar to textbook examples."
      +"Use colons to denote multiple bonds (C::O double"
      +"bond, C:::N triple bond, etc.) and parentheses"
      +"to identify repeat units or side groups."
      +"Atomic symbols in the range H-Cf (Z = 1-98)"
      +"and repetition numbers 1-9 are allowed."
      +"Chemical formula symbols are case-sensitive.\n"
      +" \n"
      +"Ligated free radicals (with free-valent acceptor"
      +"sites) can also be formed in specified hapticity"
      +"motifs with chosen molecular ligands. Radical"
      +"<acceptor> and ligand <donor-i> monomers are"
      +"specified by valid line formulas, with each"
      +"ligand <donor> formula preceded by a number of"
      +"colons (:) representing the number of 2e sites"
      +"in the desired ligand denticity (such as ':NH3'"
      +"for monodentate ammine ligand, '::NH2CH::CH2'"
      +"for bidentate vinylamine ligand, or ':::Bz' for"
      +"tridentate benzene ligand). Each such ligation"
      +"symbol may be prefixed with a stoichiometric"
      +"coefficient 2-9 for the number of ligands.\n"
      +" \n"
      +"In both molecular and supramolecular formulas,"
      +"valid transition metal duodectet structures"
      +"are also accepted. For d-block molecular species,"
      +"the default idealized metal hybridization isomer"
      +"can be altered with the REBOND command."
      +"For d-block species one can also include"
      +"coordinative ligands (:Lig), enclosed in"
      +"parentheses and preceded by a colon symbol."
      +"Formal 'ylidic' charges are allowed only for"
      +"adjacent atom pairs (e.g., dative pi-bonds).\n"
      +" \n"
      +"Models may also be specified by using acronyms"
      +"from a library of pre-formed species (many"
      +"at B3LYP/6-31+G* optimized level). Each such"
      +"acronym can also be used as a monovalent ligand"
      +"in MUTATE commands, as illustrated below.\n"
      +" \n"
      +"Common cyclic aromatic species\n"
      +" Bz        C6H6   benzene\n"
      +" A10R2L    C10H8  naphthalene\n"
      +" A14R3L    C14H12 anthracene\n"
      +" A18R4L    C18H16 tetracene\n"
      +" A22R5L    C22H20 pentacene\n"
      +" A14R3     C14H10 phenanthrene\n"
      +" A14R4     C14H12 chrysene\n"
      +" A16R4     C16H10 pyrene\n"
      +" A18R4     C18H12 triphenylene\n"
      +" A20R5     C20H12 benzopyrene\n"
      +" A20R6     C20H10 corannulene\n"
      +" A24R7     C24H12 coronene\n"
      +" A32R10    C32H14 ovalene\n"
      +"Common cyclic saturated species\n"
      +" R6C       C6H12 cyclohexane (chair)\n"
      +" R6B         '        '      (boat t.s.) \n"
      +" R6T         '        '      (twist-boat)\n"
      +" R5        C5H10 cyclopentane\n"
      +" R4        C4H8  cyclobutane\n"
      +" R3        C3H6  cyclopropane\n"
      +" RB222     [2,2,2]bicyclooctane\n"
      +" RB221     [2,2,1]bicycloheptane (norbornane)\n"
      +" RB211     [2,1,1]bicyclohexane\n"
      +" RB111     [1,1,1]bicyclopentane (propellane)\n"
      +" R5S       spiropentane\n"
      +" RAD       adamantane\n"
      +" \n"
      +"Common inorganic ligands\n"
      +" acac   acetylacetonate anion   (bidentate)\n"
      +" bipy   2,2\"\"-bipyridine         (bidentate)\n"
      +" cp     cyclopentadienyl anion  (:, ::, :::)\n"
      +" dien   diethylenetriamine      (tridentate)\n"
      +" dppe   1,2-bis(diphenylphosphino)ethane\n"
      +"                                (bidentate)\n"
      +" edta   ethylenediaminetetraacetate anion\n"
      +"                                (hexadentate)\n"
      +" en     ethylenediamine         (bidentate)\n"
      +" phen   1,10-phenanthroline     (bidentate)\n"
      +" tren   tris(2-aminoethyl)amine (tetradentate)\n"
      +" trien  triethylenetetramine    (tetradentate)\n"
      +" \n"
      +"Peptide fragments (HC::ONHCH2R)\n"
      +" GLY       glycine\n"
      +" ALA       alanine\n"
      +" VAL       valine\n"
      +" LEU       leucine\n"
      +" ILE       isoleucine\n"
      +" PRO       proline\n"
      +" PHE       phenylalanine\n"
      +" TYR       tyrosine\n"
      +" TRP       tryptophan\n"
      +" SER       serine\n"
      +" THR       threonine\n"
      +" CYS       cysteine\n"
      +" MET       methionine\n"
      +" ASN       asparagine\n"
      +" GLN       glutamine\n"
      +" ASP       aspartate\n"
      +" GLU       glutamate\n"
      +" LYS       lysine\n"
      +" ARG       argenine\n"
      +" HIS       histidine\n"
      +" \n"
      +"Nucleic acid fragments\n"
      +" NA_G      guanine\n"
      +" NA_C      cytosine\n"
      +" NA_A      adenine\n"
      +" NA_T      thymine\n"
      +" NA_U      uracil\n"
      +" NA_R      ribose backbone fragment\n"
      +" \n"
      +"In addition, the SHOW command recognizes\n"
      +"'D3H' (trigonal bipyramid) or 'D4H' (octahedral)\n"
      +"species, created as SF5, SF6, respectively.\n"
      +" \n"
      +"('SHOW' and 'FORM' are synonymous commands.) \n"
      +"Molecular examples:\n"
      +" SHOW CH3C::OOH      acetic acid\n"
      +" SHOW CH3(CH2)4CH3   n-hexane\n"
      +" SHOW WH2(:NH3)2     diammine of WH2\n"
      +" SHOW NA_C           cytosine\n"
      +" SHOW CH4            methane\n"
      +"  MUTATE 3 RAD       methyladamantane\n"
      +" SHOW ALA            alanine\n"
      +"  MUTATE 7 ALA       ala-ala\n"
      +"  MUTATE 17 ALA      ala-ala-ala, etc.\n"
      +"Supramolecular examples:\n"
      +" SHOW CH3 :H2O       hydrated methyl radical\n"
      +" SHOW Cr 2:::Bz      dibenzene chromium\n"
      +" SHOW CrCl3 2:H2O :NH3\n"
      +" SHOW Cr 3::acac\n"
      +" SHOW Cr ::::::edta\n",
      switchHelp = "SWITCH IA IB      [switch atoms IA, IB (and\n"
      +"                  attached groups) to invert\n"
      +"                  configuration at an attached\n"
      +"                  stereocenter.]\n"
      +"Example:\n"
      +" SHOW ALA         (L-alanine)\n"
      +" SWITCH 6 7       (switch to D-alanine)\n",
      symHelp = "SYMMETRY           (determine point group)\n"
      +" \n"
      +"Note that exact point-group symmetry is a"
      +"mathematical idealization. NBOModel recognizes"
      +"'effective' symmetry, adequate for chemical"
      +"purposes even if actual atom positions deviate"
      +"slightly (say, ~0.02A) from idealized symmetry.",
      twistHelp = "TWIST IA IB IC IC newval\n"
      +"              IA-IB-IC-ID angle to 'newval')\n"
      +" \n"
      +"Example:\n"
      +" SHOW C2H6          ethane (staggered)\n"
      +" TWIST 1 2 3 4 0.   ethane (eclipsed)\n",
      unifyHelp = "UNIFY CFI-1 CFI-2 IA1 IB1 IA2 IB2 dist\n"
      +"          (form a complex from molecules in\n"
      +"           cfiles CFI-1, CFI-2, chosen to have\n"
      +"           linear IA1-IB1-IB2-IA2 alignment\n"
      +"           and IA1-IA2 separation 'dist')\n"
      +" \n"
      +"CFI-1 and CFI-2 are two CFILES (previously\n"
      +"created with SAVE.C); IA1, IB1 are two atoms\n"
      +"of CFI-1 and IA2, IB2 are two atoms of CFI-2\n"
      +"that will be 'unified' in linear IA1-IB1-IB2-IA2\n"
      +"arrangement, with specified IA1-IA2 'dist'.\n"
      +" \n"
      +"Example:\n"
      +" SHOW H2C::O       (create formaldehyde)\n"
      +" SAVE.C H2CO       (save H2CO.cfi)\n"
      +" SHOW NH3          (create ammonia)\n"
      +" SAVE.C NH3        (save NH3.cfi)\n"
      +" UNIFY H2CO.cfi NH3.cfi 2 3 1 2 4.3\n"
      +"                   (creates H-bonded complex)\n",
      useHelp = "USE.t filename  (use file 'filename' of type 't'\n"
      +"                 to initiate a modeling session)\n"
      +" \n"
      +"'t' parameters: \n"
      +" .v   = valence coordinate VFILE ([.vfi])\n"
      +" .c   = cartesian coordinate CFILE (.cfi)\n"
      +" .a   = NBO archive file (.47)\n"
      +" .adf = ADF input file (.adf)\n"
      +" .g   = Gaussian input file (.gau)\n"
      +" .gms = GAMESS input file (.gms)\n"
      +" .jag = Jaguar input file (.jag)\n"
      +" .l   = Gaussian log file (.log)\n"
      +" .mp  = Molpro input file (.mp)\n"
      +" .nw  = NWChem input file (.nw)\n"
      +" .orc = Orca input file (.orc)\n"
      +" .pqs = PQS input file (.pqs)\n"
      +" .qc  = Q-Chem input file (.qc)\n"
      +"Example:\n"
      +" USE.G ACETIC   (use Gaussian-type ACETIC.GAU\n"
      +"                input file to start session)\n",
      chbHelp = "3CHB IA IB :Lig     (form 3-center hyperbond\n"
      +"                    IA-IB-Lig to ligand :Lig)\n"
      +"Examples:\n"
      +" SHOW W(:NH3)3      (normal-valent W triammine)\n"
      +" 3CHB  1 2 :NH3     (hyperbonded N-W-N triad)\n"
      +" SHOW H2O           (water monomer)\n"
      +" 3CHB  2 3 :OH2     (H-bonded water dimer)\n";
  protected final static String searchHelp = "             NBOSearch: COMMAND SYNTAX AND PROGRAM OVERVIEW\n"
      +"PROGRAM OVERVIEW:\n"
      +"Follow menu prompts through the decision tree to the "
      +"keyword module and datum "
      +"of interest. Each menu appears with "
      +"'Current [V-list] settings' and a scrolling "
      +"list of output values. All output lines are "
      +"also echoed to an external "
      +"NBOLOG$$.DAT file and error messages go to NBOERR$$.DAT for "
      +"later reference.\n\n"
      +"GENERAL 'M V n' COMMAND SYNTAX:\n"
      +"NBOSearch user responses generally consist of 'commands' \n"
      +"(replies to prompts)\n"
      +"of the form 'M (V (n))', where\n"
      +"   M (integer)   = [M]enu selection from displayed items\n"
      +"   V (character) = [V]ariable data type to be selected\n"
      +"                   [J](obname)\n"
      +"                   [B](asis)\n"
      +"                   [O](rbital number)\n"
      +"                   [A](tom number, in context)\n"
      +"                   [U](nit number)\n"
      +"                   [d](onor NBO number)\n"
      +"                   [a](cceptor NBO number, in context)\n"
      +"   n (integer)   = [n]umber of the desired O/A/U/d/a selection\n"
      +"Responses may also be of simple 'M', 'V', or 'Vn' form , where\n"
      +"  'M' : selects a numbered menu choice (for current [V] choices)\n"
      +"  'V' : requests a menu of [V] choices\n"
      +"  'Vn': selects [V] number 'n' (and current [S])\n"
      +"Note that [V]-input is case-insensitive, so 'A' (or 'a') is "
      +"interpreted as "
      +"'atom' or 'acceptor' according to context.  Note also that "
      +"'Vn' commands can be\n"
      +"given in separated 'V n' form. Although not explicitly "
      +"included in each active "
      +"[V]-select list, the 'H'(elp) key is recognized at each prompt.  "
      +"For NRT search (only), variable [V] may also be 'R' (for "
      +"'resonance structure' "
      +"and A' (for 'interacting atom'). Current A (atom) "
      +" and A' (interacting "
      +"atom) values determine the current A-A\' 'bond' selection "
      +"small fractional bond order.)\n\n"
      +"EXAMPLES:\n"
      +"  '2 a7'  : requests menu item 2 for atom 7 (if A-select active)\n"
      +"  '3 o2'  : requests menu item 3 for orbital 2 \n";
  
  protected int dialogMode;
  static final int DIALOG_CONFIG = 0;
  static final int DIALOG_MODEL = 10;
  static final int DIALOG_RUN = 20;
  static final int DIALOG_VIEW = 30;
  static final int DIALOG_SEARCH = 40;
  static final int DIALOG_LIST = -1; // used only for addLine
  

  private final static Map<String, String> htHelp = new HashMap<String, String>();
  
  /**
   * Retrieve and cache a help string.
   *  
   * @param key
   * @return resource string or a message that it cannot be found
   * 
   */
  synchronized protected String getHelp(String key) {
    String help = htHelp.get(key);
    if (help == null) {
      try {
        String fname = "org/openscience/jmol/app/nbo/help/" + key + ".txt";
        help = GuiMap.getResourceString(this, fname);
      } catch (IOException e) {
        help = "<resource not found>";
      }
      htHelp.put(key, help);
    }
    return help;
  }

}
