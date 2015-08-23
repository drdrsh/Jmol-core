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
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Hashtable;

import javajs.util.SB;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jmol.i18n.GT;
import org.jmol.util.Logger;
import org.openscience.jmol.app.jmolpanel.JmolPanel;

abstract class NBODialogSearch extends NBODialogView {

  NBODialogSearch(JFrame f) {
    super(f);
    lists = new Hashtable<String, String[]>();
  }

  private int operator = 1;
  protected boolean twoOps = false;
  protected JPanel comboBox;
  protected JTextField opTxt;
  protected JComboBox<String> bas2, orb, nbo1, nbo2, unit, at1, at2, row, col, keyWord;
  protected String keyProp;
  protected JCheckBox viewAll;
  protected JList<String> opList;
  private DefaultListModel<String> listM;
  
  private final static int KEYWD_NPA = 1;
  private final static int KEYWD_NBO = 2;
  private final static int KEYWD_BEND = 3;
  private final static int KEYWD_E2PERT = 4;
  private final static int KEYWD_NLMO = 5;
  private final static int KEYWD_NRT = 6;
  private final static int KEYWD_STERIC = 7;
  private final static int KEYWD_CMO = 8;
  private final static int KEYWD_DIPOLE = 9;
  private final static int KEYWD_OPBAS = 10;
  private final static int KEYWD_BAS1BAS2 = 11;
  private final static int NPA_VIS         = 11;
  private final static int NBO_VIS         = 7;
  private final static int BEND_VIS        = 8;
  private final static int E2_VIS          = 7;
  private final static int NLMO_VIS        = 7;
  private final static int STERIC_VIS      = 8;
  private final static int MO_VIS          = 8;
  final static int CMO_VIS         = 9;

  private String[] keyW = { "-Select Keyword Property-",
      "NPA        Atomic and NAO properties",
      "NBO        Natural Lewis Structure and NBO properties",
      "BEND       NHO directionality and bond-bending",
      "E2PERT     2nd-order energtics of NBO donor-acceptor interactions",
      "NLMO       NLMO properties",
      "NRT        Natural Resonance Theory weightings and bond orders",
      "STERIC     Total and pairwise contributions to steric exchange energy",
      "CMO        NBO-based character of canonical molecular orbitals",
      "DIPOLE     L/NL contributions to electric dipole moment",
      "<OPBAS>    Matrix elements of chosen operator in chosen basis set",
      "<BAS1BAS2> Transformation matrix between chosen basis sets" };
  
  String[] npa = {"NPA Atomic Properties:",
      "  (1) NPA atomic charge",
      "  (2) NPA atomic spin density",
      "  (3) NEC atomic electron configuration",
      "NPA Molecular Uit Properties:",
      "  (4) NPA molecular unit charge",
      "  (5) NPA molecular unit spin density", 
      "NAO Orbital Properties:",
      "  (6) NAO label",
      "  (7) NAO orbital population", 
      "  (8) NAO orbital energy",
      "  (9) NAO orbital spin density", 
      "  (10) NMB minimal basis %-accuracy",
      "Display Options:",
      "  (11) Display (P)NAO visualization", 
      "  (12) Display atomic charges"},
      nbo = {"NBO Orbital Properties:",
          "  (1) NBO orbital label",
          "  (2) NBO orbital population", 
          "  (3) NBO orbital energy",
          "  (4) NBO ionicity", 
          "Natural Lew Structure Properties:",
          "  (5) NLS rho(NL)", 
          "  (6) NLS %-rho(L)",
          "Display Options:",
          "  (7) Display (P)NAO visualization", 
          "  (8) Display NLS diagram"},
      bend = {"NHO Orbital Prperties:", 
          "  (1) NHO orbital label",
          "  (2) NHO orbital population", 
          "  (3) NHO orbital energy",
          "  (4) NHO hybrid composition", 
          "  (5) NHO direction angles",
          "  (6) NHO bending deviation from line of centers",
          "  (7) Strongest bending deviation for any NHO",
          "Display Option",
          "  (8) Display (P)NHO visualiztion"},
      e2 = {"E2 Values for Selected Donor-Acceptor NBOs:",
          "  (1) E(2) interaction for current d/a NBOs",
          "  (2) Strongest E(2) interaction for current d-NBO",
          "  (3) Strongest E(2) interaction for current a-NBO",
          "  (4) Strongest E(2) interaction for any d/a NBOs",
          "Intermolecular E2 Options:",
          "  (5) Strongest intermolecular E(2) for current unit",
          "  (6) Strongest intermolecular E(2) for any units",
          "Display Option:",
          "  (7) Display (P)NBO visualization for current d/a NBOs", "back" },
      nlmo = {"NLMO Orbital Properties:", 
          "  (1) NLMO orbital label",
          "  (2) NLMO population", 
          "  (3) NLMO orbital energy",
          "  (4) NLMO %-NBO parentage",
          "NLMO Delocalization Tail Properties:",
          "  (5) NLMO delocalization tail population",
          "  (6) NLMO delocalization tail NBO components",
          "Display Option:",
          "  (7) Display (P)NLMO visualization"},
      nrt = {"Atom (A) Properties:", 
          "  (1) atomic valency (total)",
          "  (2) atomic covalency", 
          "  (3) atomic electrovalency",
          "Bond [A-A'] Properties:",
          "  (4) bond order (total)", 
          "  (5) covalent bond order",
          "  (6) electrovalent bond order",
          "Resonance Structure Properties:",
          "  (7) RS weighting",
          "  (8) RS rho(NL) (reference structures only)",
          "Display Options:",
          "  (9) Display NRT atomic valencies",
          "  (10) Display NRT bond orders", 
          "  (11) Display RS diagram"},
      steric = {"Total Steric Exchange Energy (SXE) Estimates:",
          "  (1) Total SXE", 
          "  (2) Sum of pairwise (PW-SXE) contributions",
          "Selected PW-SXE contributions:",
          "  (3) PW-SXE for current d-d' NLMOs",
          "  (4) Strongest PW-SXE for current d NLMO",
          "Intra- and intermolecular options:",
          "  (5) Strongest PW-SXE within current unit",
          "  (6) Strongest PW-SXE within any unit",
          "  (7) Strongest PW-SXE between any units",
          "Display Option:",
          "  (8) Display (P)NLMO diagram for current PW-SXE"}, 
      mo = {"Character of current MO (c):",
          "  (1) Current MO energy and type",
          "  (2) Bonding character of current MO",
          "  (3) Nonbonding character of current MO",
          "  (4) Antibonding character of current MO",
          "NBO (n) %-contribution to selected MO (c):",
          "  (5) %-contribution of current NBO to current MO",
          "  (6) Largest %-contribution to current MO from any NBO",
          "  (7) Largest %-contribution of current NBO to any MO",
          "Display Options:",
          "  (8) Display current MO", "  (9) Display current NBO"},
      dip = {"Total Dipole Properties:", 
          "  (1) Total dipole moment",
          "  (2) Total L-type dipole",
          "  (3) Total NL-type (resonance) dipole",
          "Bond [NBO/NLMO] Dipole Properties:",
          "  (4) Dipole moment of current NLMO",
          "  (5) L-type (NBO bond dipole) contribution",
          "  (6) NL-type (resonance dipole) contribution",
          "Molecular Unit Dipole Properties:",
          "  (7) Dipole moment of current molecular unit",
          "  (8) L-type contribution to unit dipole",
          "  (9) NL-type contribution to unit dipole"}, 
      op = {
          "***** Select OP *****", "  (1) S   overlap (unit) operator",
          "  (2) F   1e Hamiltonian (Fock/Kohn-Sham) operator",
          "  (3) K   kinetic energy operator",
          "  (4) V   1e potential (nuclear-electron attraction) operator",
          "  (5) DM  1e density matrix operator",
          "  (6) DIx dipole moment operator (x component)",
          "  (7) DIy dipole moment operator (y component)",
          "  (8) DIz dipole moment operator (z component)"};

  protected void buildSearch(Container p) {
    opList = null;
    reqInfo = keyProp = "";
    java.util.Properties props = JmolPanel.historyFile.getProperties();
    workingPath = (props.getProperty("workingPath",
        System.getProperty("user.home")));
    p.removeAll();
    p.setLayout(new BorderLayout());
    if(topPanel == null) topPanel = buildTopPanel();
    p.add(topPanel,BorderLayout.PAGE_START);
    JPanel p2 = new JPanel(new BorderLayout());
    JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,searchP(true),searchS());
    sp.setDividerLocation(430);
    p2.add(sp,BorderLayout.CENTER);
    Box box = Box.createHorizontalBox();
    box.add(new JLabel("JobFile")).setFont(new Font("Arial",Font.BOLD,25));
    box.add(folderBox());
    browse.setEnabled(true);
    p2.add(box,BorderLayout.NORTH);
    p.add(p2, BorderLayout.CENTER);
    statusLab.setText("");
    p.add(statusPanel, BorderLayout.PAGE_END);
    if(isJmolNBO){
      setInputFile(inputFile,"47",null);
      keyWord.setSelectedIndex(1);
    }
  }

  private JPanel searchP(boolean view) {
    JPanel selectPanel = new JPanel(new BorderLayout());
    listM = new DefaultListModel<String>();
    opList = new JList<String>(listM);
    opList.setFont(new Font("Arial",Font.PLAIN,18));
    opList.setSelectedIndex(1);
    opList.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if(opList.getSelectedIndex()!=-1)
          goSearchClicked();
      }
    });
    JScrollPane p = new JScrollPane();
    p.getViewport().add(opList);
    selectPanel.add(p, BorderLayout.CENTER);
    keyWord = new JComboBox<String>(keyW);
    keyWord.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) { 
        keywordNumber = keyWord.getSelectedIndex();
        listClicked(keyWord.getSelectedIndex());
      }
    });
    keyWord.setEnabled(isJmolNBO);
    selectPanel.add(keyWord,BorderLayout.NORTH);
    jCheckAtomNum = new JCheckBox("Show Atom #'s");
    jCheckAtomNum.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) { 
        if(!jCheckAtomNum.isSelected())
          nboService.runScriptQueued("select {*};label off");
        else
          nboService.runScriptQueued("select {*};label %a");
      nboService.runScriptQueued("color labels white;select remove {*}");
      }
    });
    Box bo = Box.createHorizontalBox();
    bo.add(jCheckAtomNum);
    if(view){
      viewAll = new JCheckBox("View all");
      bo.add(viewAll);
    }else
      viewAll=null;
    selectPanel.add(bo,BorderLayout.SOUTH);
    return selectPanel;
  }

  private JPanel searchS() {
    keyProp = "";
    JPanel searchPanel = new JPanel();
    searchPanel.setBorder(BorderFactory.createLoweredBevelBorder());
    searchPanel.setLayout(new BorderLayout());
    basis = new JComboBox<String>(basSet);
    comboBox = new JPanel(new BorderLayout());
    searchPanel.add(comboBox, BorderLayout.PAGE_START);
    searchPanel.add(modelOut(), BorderLayout.CENTER);
    return searchPanel;
  }

  protected void getList1(final String get) {
    final SB sb = new SB();
    appendToFile("GLOBAL C_PATH " + inputFile.getParent() + sep, sb);
    appendToFile("GLOBAL I_KEYWORD " + keywordNumber + sep, sb);
    appendToFile("GLOBAL C_JOBSTEM " + jobStem + sep, sb);
    appendToFile("GLOBAL I_BAS_1 " + keywordNumber + sep, sb);
    final String key = get.split(" ")[0];
    appendToFile("CMD " + key, sb);
    nboService.queueJob("getSearchList", "Getting search list " + get.substring(get.indexOf("-") + 1),
        new Runnable() {
          @Override
          public void run() {
            reqInfo = "";
            nboService.rawCmdNew("s", sb, true, NBOService.MODE_VIEW_LIST);
//            if (reqInfo.length() == 0)
//            System.out.println(reqInfo);
            String[] st = new String[reqInfo.length() / 20];
            for (int i = 0; (i + 1) * 20 <= reqInfo.length(); i++)
              st[i] = reqInfo.substring(i * 20, (i + 1) * 20);
            lists.put(get, st);
            System.out.println("created list " + get + " len=" + st.length + " " + reqInfo);
            reqInfo = null;
          }
        });
  }
  
  protected void setLists(final String[] get, final String[] labs) {
    comboBox.removeAll();
    nboService.queueJob("setLists", null, new Runnable() {
      @Override
      public void run() {
        JPanel l = new JPanel(new GridLayout(labs.length, 1));
        JPanel l2 = new JPanel(new GridLayout(labs.length, 1));
        for (int i = 0; i < labs.length; i++) {
          final String key = get[i].split(" ")[0];
          l.add(new JLabel(labs[i]));
          if (key.equals("b"))
            l2.add(new JLabel(basis.getSelectedItem().toString()));
          else if (key.equals("o")) {
            orb = new JComboBox<String>(lists.get(get[i]));
            orb.addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                showOrbJmol(basis.getSelectedItem().toString(),
                    orb.getSelectedIndex() + 1);
              }
            });
            orb.setSelectedIndex(0);
            l2.add(orb);
          } else if (key.equals("d") || get[i].equals("c cmo")) {
            nbo1 = new JComboBox<String>(lists.get(get[i]));
            nbo1.addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                showOrbJmol(basis.getSelectedItem().toString(),
                    nbo1.getSelectedIndex() + 1);
              }
            });
            nbo1.setSelectedIndex(0);
            l2.add(nbo1);
          } else if (key.equals("d'") || key.equals("n")
              || get[i].equals("a nbo")) {
            nbo2 = new JComboBox<String>(lists.get(get[i]));
            nbo2.addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                if (key.equals("n"))
                  showOrbJmol("NBO", nbo2.getSelectedIndex() + 1);
                else if (key.equals("a")) {
                  showOrbJmol("NBO",
                      lists.get("d nbo").length + 1 + nbo2.getSelectedIndex());
                } else
                  showOrbJmol(basis.getSelectedItem().toString(),
                      nbo2.getSelectedIndex() + 1);
              }
            });
            nbo2.setSelectedIndex(0);
            l2.add(nbo2);
          } else if (key.equals("u") || key.equals("rs")) {
            unit = new JComboBox<String>(lists.get(get[i]));
            l2.add(unit);
          } else if (key.equals("a")) {
            at1 = new JComboBox<String>(lists.get(get[i]));
            at1.addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                if (at2 == null)
                  nboService.runScriptQueued("select on; select {*}["
                      + (at1.getSelectedIndex() + 1) + "]");
                else
                  nboService.runScriptQueued("select on; select remove{*}; "
                      + "select add {*}[" + (at1.getSelectedIndex() + 1)
                      + "]; select add {*}[" + (at2.getSelectedIndex() + 1)
                      + "]");
              }
            });
            at1.setSelectedIndex(0);
            l2.add(at1);
          } else if (key.equals("a'")) {
            at2 = new JComboBox<String>(lists.get("a"));
            at2.addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                nboService.runScriptQueued("select on; select remove{*}; "
                    + "select add {*}[" + (at1.getSelectedIndex() + 1)
                    + "]; select add {*}[" + (at2.getSelectedIndex() + 1) + "]");
              }
            });
            at2.setSelectedIndex(1);
            l2.add(at2);
          } else if (key.equals("b1")) {
            l2.add(basis);
            basis.addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                basChange();
              }
            });
          } else if (key.equals("b2")) {
            bas2 = new JComboBox<String>(basSet);
            bas2.addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                basChange();
              }
            });
            bas2.setSelectedIndex(1);
            l2.add(bas2);
          } else if (key.equals("b12")) {
            basis = new JComboBox<String>(basSet);
            l2.add(basis);
          } else if (key.equals("r")) {
            row = new JComboBox<String>(lists.get("r"));
            l2.add(row);
          } else if (key.equals("c")) {
            col = new JComboBox<String>(lists.get("c"));
            l2.add(col);
          }
        }
        appendOutputWithCaret((jpNboOutput.getText().equals("") ? "" : "\n")
            + keyProp + " Search Results:");
        comboBox.add(l, BorderLayout.WEST);
        comboBox.add(l2, BorderLayout.CENTER);
      }
    });

  }

  protected void basChange() {
    String[] b1b2 = {
        "Current (r,c) matrix element:",
        "  (1) current <" + basis.getSelectedItem().toString() + "(r)|"
            + bas2.getSelectedItem().toString() + "(c)> value",
        "Extremal off-diagonal values for current r(ow) orbital:",
        "  (2) max <" + basis.getSelectedItem().toString() + "(r)|"
            + bas2.getSelectedItem().toString() + "(*c)> value for current r",
        "  (3) min <" + basis.getSelectedItem().toString() + "(r)|"
            + bas2.getSelectedItem().toString() + "(*c)> value for current r",
        "Extremal off-diagonal values for current c(ol) orbital:",
        "  (4) max <" + basis.getSelectedItem().toString() + "(*r)|"
            + bas2.getSelectedItem().toString() + "(c)> value for current c",
        "  (5) min <" + basis.getSelectedItem().toString() + "(*r)|"
            + bas2.getSelectedItem().toString() + "(c)> value for current c",
        "Extremal off-diagonal values for any (*r,*c) orbitals:",
        "  (6) max <" + basis.getSelectedItem().toString() + "(*r)|"
            + bas2.getSelectedItem().toString() + "(*c)> value for any *r,*c",
        "  (7) min <" + basis.getSelectedItem().toString() + "(*r)|"
            + bas2.getSelectedItem().toString() + "(*c)> value for any *r,*c",
        "back" };
    changeKey(b1b2);
  }
  
  private void changeKey(String[] s){
    listM.removeAllElements();
    for(String x:s)
      listM.addElement(x);
  }

  protected void listLogic() {
    if (twoOps) {
      if (opList.getSelectedIndex() != 0)
      if (keyProp.equals("")) {
        operator = opList.getSelectedIndex();
        //String val = opList.getSelectedValue().trim();
        if(lists.get("r")==null) getList1("r");
        if(lists.get("c")==null) getList1("c");
        setLists("b12 r c".split(" "),new String[] {"Basis:","Row: ", "Collumn: "});
        //searchP(opBas(val.split(" ")[1]),false);
        keyProp = "OPBAS";
      }else goSearchClicked();
      return;
    }
    if (keyProp.equals("")) {
      keywordNumber = opList.getSelectedIndex();
      if(keywordNumber!=10)
      basis.setEnabled(keywordNumber == 10 || keywordNumber == 11);
      listClicked(keywordNumber);
      return;
    }
    if (opList.getSelectedIndex()!=0){
      goSearchClicked();
    }
  }
  
  private String[] opBas(String operator){
    return new String[] {
        "Current [r,c] matrix element",
        "  (1) current <r\\" + operator + "\\c> value",
        "Extremal off-diagonal values for current r[ow] orbital:",
        "  (2) max <r\\" + operator + "\\*c> value for current r",
        "  (3) min <r\\" + operator + "\\*c> value for current r",
        "Extremal off-diagonal values for current c[ol] orbital:",
        "  (4) max <*r\\" + operator + "\\c> value for current c",
        "  (5) min <*r\\" + operator + "\\c> value for current c",
        "Extremal off-diagonal values for any [*r,*c] orbitals:",
        "  (6) max <*r\\" + operator +"\\*c> value for any *r,*c",
        "  (7) min <*r\\" + operator + "\\*c> value for any *r,*c", "back" };
  }

  protected void listClicked(int index) {
    switch (index) {
    case KEYWD_NPA:
      at2=null;
      basis.setSelectedIndex(1);
      keyProp = "NPA";
      if(lists.get("a")==null) getList1("a");
      if(lists.get("o PNAO")==null) getList1("o PNAO");
      if(lists.get("u")==null) getList1("u");
      setLists(new String[] {"b","a","o PNAO","u"}, new String[] {"Basis: ", "Atom: ", "Orbital: ", "Unit: "});
      changeKey(npa);
      break;
    case KEYWD_NBO:
      basis.setSelectedIndex(5);
      keyProp = "NBO";
      if(lists.get("o PNBO")==null) getList1("o PNBO");
      setLists(new String[] {"b","o PNBO"}, new String[] {"Basis: ", "Orbital: "});
      changeKey(nbo);
      break;
    case KEYWD_NLMO:
      basis.setSelectedIndex(7);
      keyProp = "NLMO";
      if(lists.get("o PNLMO")==null) getList1("o PNLMO");
      setLists(new String[] {"b","o PNLMO"}, new String[] {"Basis: ", "Orbital: "});
      changeKey(nlmo);
      break;
    case KEYWD_BEND:
      basis.setSelectedIndex(3);
      keyProp = "BEND";
      if(lists.get("o PNHO")==null) getList1("o PNHO");
      setLists(new String[] {"b","o PNHO"}, new String[] {"Basis: ", "Orbital: "});
      changeKey(bend);
      break;
    case KEYWD_NRT:
      keyProp = "NRT";
      if(lists.get("a")==null) getList1("a");
      if(lists.get("rs")==null) getRs();
      setLists("a a' rs".split(" "), new String[] {"Atom 1: ","Atom 2: ", "Res Struct: "});
      changeKey(nrt);
      break;
    case KEYWD_E2PERT:
      basis.setSelectedIndex(6);
      keyProp = "E2";
      if(lists.get("d nbo")==null) getList1("d nbo");
      if(lists.get("a nbo")==null) getList1("a nbo");
      if(lists.get("u")==null) getList1("u");
      setLists(new String[] {"b", "d nbo", "a nbo", "u"}, new String[] {"Basis: ","d-NBO: ", "a-NBO:", "Unit: "});
      changeKey(e2);
      break;
    case KEYWD_STERIC:
      basis.setSelectedIndex(8);
      keyProp = "STERIC";
      if(lists.get("d nlmo")==null) getList1("d nlmo");
      if(lists.get("d' nlmo")==null) getList1("d' nlmo");
      if(lists.get("u")==null) getList1("u");
      setLists(new String[] {"b", "d nlmo", "d' nlmo", "u"}, new String[] {"Basis: ","d-NLMO: ", "d'-NLMO:", "Unit: "});
      changeKey(steric);
      break;
    case KEYWD_CMO:
      basis.setSelectedIndex(9);
      keyProp = "CMO";
      if(lists.get("c cmo")==null) getList1("c cmo");
      if(lists.get("n")==null) getList1("n");
      setLists(new String[] {"b", "c cmo","n"}, new String[] {"Basis: ","c-CMO: ", "n-NBO:"});
      changeKey(mo);
      break;
    case KEYWD_DIPOLE:
      basis.setSelectedIndex(8);
      keyProp = "DIPOLE";
      if(lists.get("u")==null) getList1("u");
      setLists("b u".split(" "), new String[] {"Basis: ","Unit:"});
      changeKey(dip);
      break;
    case KEYWD_OPBAS:
      comboBox.removeAll();
      basis = new JComboBox<String>(basSet);
      basis.setEditable(false);
      changeKey(opBas("s"));
      break;
    case KEYWD_BAS1BAS2:
      keyProp = "B1B2";
      if(lists.get("r")==null) getList1("r");
      if(lists.get("c")==null) getList1("c");
      setLists("b1 b2 r c".split(" "), new String[] {"Basis 1:","Basis 2:","Row:","Collumn:"});
      //searchP(b1b2);
      break;
    }
    
  }

  protected void labelAt() {
    Logger.info(reqInfo);
    String[] st = reqInfo.trim().split(" |\\n");
    String[] st2 = new String[jmolAtomCount];
    for (int i = 0; i < jmolAtomCount - 1; i++) {
      st2[i] = st[i + 1];
      System.out.println("___" + st2[i] + "    =    " + st[i + 1]);
    }
    st2[jmolAtomCount - 1] = st[0];
    for (int i = 0; i < jmolAtomCount; i++) {
      nboService.runScriptQueued("select{*}[" + (i + 1) + "];label " + st2[i]);
    }
    //nboService.restart();
  }

  @Override
  protected void showWorkpathDialogS(String workingpath) {
    JFileChooser myChooser = new JFileChooser();
    myChooser.setFileFilter(new FileNameExtensionFilter("NBO", "nbo"));
    myChooser.setFileHidingEnabled(true);
    myChooser.setSelectedFile(new File(workingPath));
    int button = myChooser.showDialog(this, GT._("Select"));
    if (button != JFileChooser.APPROVE_OPTION)
      return;
    nboResetV();
    setInputFile(newNBOFile(myChooser.getSelectedFile(),"47"),"nbo",showWorkPathDone);
    keyWord.setEnabled(true);
    keyWord.setSelectedIndex(keywordNumber);
  }

  private void getRs() {
    final SB sb = new SB();
    appendToFile("GLOBAL C_PATH " + inputFile.getParent() + sep, sb);
    appendToFile("GLOBAL I_KEYWORD " + keywordNumber + sep, sb);
    appendToFile("GLOBAL C_JOBSTEM " + jobStem + sep, sb);
    appendToFile("GLOBAL I_BAS_1 " + keywordNumber + sep, sb);
    appendToFile("CMD  r", sb);
    nboService.queueJob("search", "getting resonance structures", new Runnable() {
      @Override
      public void run() {
        reqInfo = "";
        nboService.rawCmdNew("s", sb, true, NBOService.MODE_SEARCH_SELECT);
        int r = Integer.parseInt(reqInfo.substring(reqInfo.indexOf("-") + 1,
            reqInfo.indexOf(")")));
        System.out.println("...." + r);
        String[] st = new String[r];
        for (int i = 0; i < r; i++)
          st[i] = (i+1)+". R.S. " + (i + 1);
        lists.put("rs", st);
      }      
    });
  }

  protected void goSearchClicked() {
    final SB sb = new SB();
    appendToFile("GLOBAL C_PATH " + inputFile.getParent() + sep, sb);
    appendToFile("GLOBAL I_KEYWORD " + keywordNumber + sep, sb);
    appendToFile("GLOBAL C_JOBSTEM " + jobStem + sep, sb);
    String str = opList.getSelectedValue();
    if(str.indexOf("(")<0||(keyProp.equals("E2")&&opList.getSelectedIndex()==2))
      return;
    int op = Integer.parseInt(str.substring(str.indexOf("(")+1,str.indexOf(")")));
    boolean isImage = false;
    boolean isLabel = false;
    boolean isDrawing = false;
    switch (keywordNumber) {
    case KEYWD_NPA:
      appendToFile("GLOBAL I_ATOM_1 " + (at1.getSelectedIndex() + 1) + sep, sb);
      appendToFile("GLOBAL I_UNIT " + (unit.getSelectedIndex() + 1) + sep, sb);
      appendToFile("GLOBAL I_ORB_1 " + (orb.getSelectedIndex() + 1) + sep, sb);
      isImage = (op == NPA_VIS);
      isLabel = (op == 12);
      break;
    case KEYWD_NBO:
      isDrawing = (op == 8);
      isImage = (op == NBO_VIS);
      appendToFile("GLOBAL I_ORB_1 " + (orb.getSelectedIndex() + 1) + sep, sb);
      break;
    case KEYWD_BEND:
      isImage = (op == BEND_VIS);
      appendToFile("GLOBAL I_ORB_1 " + (orb.getSelectedIndex() + 1) + sep, sb);
      break;
    case KEYWD_E2PERT:
      isImage = (op == E2_VIS);
      appendToFile("GLOBAL I_d_NBO_1 " + (nbo1.getSelectedIndex() + 1) + sep,
          sb);
      appendToFile("GLOBAL I_a_NBO " + (nbo2.getSelectedIndex() + 1) + sep, sb);
      appendToFile("GLOBAL I_UNIT " + (unit.getSelectedIndex() + 1) + sep, sb);
      break;
    case KEYWD_NLMO:
      isImage = (op == NLMO_VIS);
      appendToFile("GLOBAL I_ORB_1 " + (orb.getSelectedIndex() + 1) + sep, sb);
      break;
    case KEYWD_NRT:
      appendToFile("GLOBAL I_ATOM_1 " + (at1.getSelectedIndex() + 1) + sep, sb);
      appendToFile("GLOBAL I_ATOM_2 " + (at2.getSelectedIndex() + 1) + sep,
          sb);
      appendToFile("GLOBAL I_RES_STR " + (unit.getSelectedIndex() + 1) + sep, sb);
      isLabel = (op == 9)||(op == 10);
      break;
    case KEYWD_STERIC:
      isImage = (op == STERIC_VIS);
      appendToFile("GLOBAL I_d_NBO_1 " + (nbo1.getSelectedIndex() + 1) + sep,
          sb);
      appendToFile("GLOBAL I_d_NBO_2 " + (nbo2.getSelectedIndex() + 1) + sep,
          sb);
      appendToFile("GLOBAL I_UNIT " + (unit.getSelectedIndex() + 1) + sep, sb);
      break;
    case KEYWD_CMO:
      isImage = (op == CMO_VIS)||(op == MO_VIS);
      appendToFile("GLOBAL I_CMO " + (nbo1.getSelectedIndex() + 1) + sep, sb);
      appendToFile("GLOBAL I_NBO " + (nbo2.getSelectedIndex() + 1) + sep, sb);
      break;
    case KEYWD_DIPOLE:
      appendToFile("GLOBAL I_UNIT " + (unit.getSelectedIndex() + 1) + sep, sb);
      break;
    case KEYWD_OPBAS:
      appendToFile("GLOBAL I_BAS_1 " + (basis.getSelectedIndex() + 1) + sep, sb);
      appendToFile("GLOBAL I_OPERATOR " + operator + sep, sb);
      appendToFile("GLOBAL I_ROW " + (row.getSelectedIndex() + 1) + sep, sb);
      appendToFile("GLOBAL I_COLUMN " + (col.getSelectedIndex() + 1) + sep, sb);
      break;
    case KEYWD_BAS1BAS2:
      appendToFile("GLOBAL I_BAS_1 " + (basis.getSelectedIndex() + 1) + sep, sb);
      appendToFile("GLOBAL I_BAS_2 " + (bas2.getSelectedIndex() + 1) + sep, sb);
      appendToFile("GLOBAL I_ROW " + (row.getSelectedIndex() + 1) + sep, sb);
      appendToFile("GLOBAL I_COLUMN " + (col.getSelectedIndex() + 1) + sep, sb);
      break;
    }
    if(viewAll!=null){
      if(viewAll.isSelected()){
        if((keyProp.equals("NPA") && op<=9)|| op <= 4)
          appendToFile("GLOBAL I_OPT_" + keyProp + " " + op + "*", sb);
        else
          appendToFile("GLOBAL I_OPT_" + keyProp + " " + op, sb);
      }else 
        appendToFile("GLOBAL I_OPT_" + keyProp + " " + op, sb);
    }else
      appendToFile("GLOBAL I_OPT_" + keyProp + " " + op, sb);
    if (isImage) {
      nboService.queueJob("search", "Raytracing, please be patient...",
          new Runnable() {
            @Override
            public void run() {
              File f = new File(new File(nboService.serverPath).getParent()
                  + "\\pic.bmp");
              if (f.exists())
                f.delete();
              System.out.println("-----" +f.toString());
              nboService.rawCmdNew("s", sb, false, NBOService.MODE_IMAGE);
              while (!f.exists()) {
                try {
                  Thread.sleep(10);
                } catch (InterruptedException e) {
                  break;
                }
              }
              try {
                // TODO need to get this id business fixed here as well
                nboService.runScriptQueued("image id pic close; image id pic \""
                    + f.toString().replaceAll("\\\\",  "\\\\\\\\") + "\"");
                statusLab.setText("");
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
          });
    } else if (isLabel) {
      nboService.queueJob("search", "getting list...", new Runnable() {
        @Override
        public void run() {
          reqInfo = "";
          nboService.rawCmdNew("s", sb, false, NBOService.MODE_SEARCH_LIST);
          while (reqInfo.trim().split(" |\\n").length < jmolAtomCount) {
            try {
              Thread.sleep(10);
            } catch (InterruptedException e) {
              break;
            }
          }
          try {
            labelAt();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }});
    } else if (isDrawing) {
      nboService.runScriptQueued("select {*}; show drawing");
    } else {
      nboService.queueJob("search", "getting value...", new Runnable() {
        @Override
        public void run() {
          reqInfo = "";
          nboService.rawCmdNew("s", sb, false, NBOService.MODE_SEARCH_VALUE);
          appendOutputWithCaret("  "+reqInfo);
        }
      });
    }

  }
  
  private boolean secondPick = true;

  protected void notifyCallbackS(int atomIndex) {
    if (at1 != null && at2 == null)
      at1.setSelectedIndex(atomIndex);
    else if (at1 != null && at2 != null)
      if (secondPick) {
        at1.setSelectedIndex(atomIndex);
        secondPick = false;
      } else {
        at2.setSelectedIndex(atomIndex);
        secondPick = true;
      }
  }
  
  protected void rawInputS(String cmd) {
    if (cmd.startsWith("O ")) {
      try {
        int i = Integer.parseInt(cmd.split(" ")[1]);
        orb.setSelectedIndex(i - 1);
      } catch (Exception e) {
        appendOutputWithCaret("Invalid command");
      }
    } else if (cmd.startsWith("A ")) {
      try {
        int i = Integer.parseInt(cmd.split(" ")[1]);
        at1.setSelectedIndex(i - 1);
      } catch (Exception e) {
        appendOutputWithCaret("Invalid command");
      }
    }
  }


}