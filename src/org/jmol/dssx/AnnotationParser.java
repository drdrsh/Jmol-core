/* $RCSfile$
 * $Author: nicove $
 * $Date: 2007-03-25 06:44:28 -0500 (Sun, 25 Mar 2007) $
 * $Revision: 7224 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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
package org.jmol.dssx;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javajs.api.GenericLineReader;
import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.PT;
import javajs.util.Rdr;
import javajs.util.SB;

import org.jmol.api.JmolAnnotationParser;
import org.jmol.java.BS;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.HBond;
import org.jmol.modelset.ModelSet;
import org.jmol.modelsetbio.BasePair;
import org.jmol.modelsetbio.BioModel;
import org.jmol.modelsetbio.BioPolymer;
import org.jmol.modelsetbio.NucleicMonomer;
import org.jmol.modelsetbio.NucleicPolymer;
import org.jmol.script.SV;
import org.jmol.script.T;
import org.jmol.util.BSUtil;
import org.jmol.util.C;
import org.jmol.util.Edge;
import org.jmol.util.Logger;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;

/** 
 * 
 * A parser for output from 3DNA web service.
 * 
 *   load =1d66/dssr
 * 
 * also other annotations now, 
 * 
 *   load *1cbs/dom
 *   
 * calls EBI for the mmCIF file and also 
 * retrieves the domains mapping JSON report.
 * 
 * 
 *   load *1cbs/val
 *   
 * calls EBI for the mmCIF file and also
 * retrieves the validation outliers JSON report. 
 * 
 * Bob Hanson
 * July 2014
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */
public class AnnotationParser implements JmolAnnotationParser {

  private GenericLineReader reader;
  private String line;
  private Map<String, Object> dssr;
  private Map<String, Object> htTemp;
  private SB message;
  private Map<String, String[]> htPar;
  private Lst<Map<String, Object>> basePairs;

  public AnnotationParser() {
    // for reflection
  }

  @Override
  public String processDSSR(Map<String, Object> info, GenericLineReader reader,
                        String line0, Map<String, String> htGroup1) throws Exception {
    info.put("dssr", dssr = new Hashtable<String, Object>());
    htTemp = new Hashtable<String, Object>();
    htPar = new Hashtable<String, String[]>();
    htPar.put("bp", new String[] { "bpShear", "bpStretch", "bpStagger",
        "bpPropeller", "bpBuckle", "bpOpening" });
    htPar.put("bpChiLambda", new String[] { "bpChi1", "bpLambda1", "bpChi2",
        "bpLambda2" });
    htPar.put("bpDistTor", new String[] { "bpDistC1C1", "bpDistNN",
        "bpDistC6C8", "bpTorCNNC" });
    htPar.put("step", new String[] { "stShift", "stSlide", "stRise", "stTilt",
        "stRoll", "stTwist" });
    htPar.put("hel", new String[] { "heXDisp", "heYDisp", "heRise", "heIncl",
        "heTip", "heTwist" });

    this.reader = reader;
    message = new SB();
    line = (line0 == null ? "" : line0.trim());
    // output the header section with credits
    skipTo("DSSR:", false);
    addToMessages(null);
    boolean haveHeader = false;
    while (rd() != null) {
      if (line.startsWith("List of")) {
        int n = PT.parseInt(line.substring(8));
        if (n < 0 || line.endsWith("files"))
          continue;
        addMessage(line);
        line = PT.rep(PT.trim(line, "s"), " interaction", "");
        int pt = "pair elix lice plet stem tack loop ulge tion ment otif pper turn bond file"
            .indexOf(line.trim().substring(line.length() - 4));
        //0    5    10        20        30        40        50        60
        switch (pt) {
        case 0:
          readPairs(n);
          break;
        case 5:
        case 10:
          getHelixOrStem(n, "helices", "helix", true);
          break;
        case 15:
          readNTList(null, "multiplets", n);
          break;
        case 20:
          getHelixOrStem(n, "stems", "stem", false);
          break;
        case 25:
          readStacks(n);
          break;
        case 30:
          readLoops(n); // hairpin, internal, or kissing
          break;
        case 35:
          readBulges(n);
          break;
        case 40:
          readJunctions(n);
          break;
        case 45:
          readNTList(null, "singleStranded", n);
          break;
        case 50:
          readMotifs(n);
          break;
        case 55:
          readNTList(null, "riboseZippers", n);
          break;
        case 60:
          readTurns(n);
          break;
        case 65:
          readHBonds(n);
          break;
        case 70: // files
          break;
        default:
          addMessage("DSSRParser ignored: " + line);
          break;
        }
      } else if (!haveHeader && line.startsWith("Date and time")) {
        haveHeader = true;
        addToMessages("");
      } else if (line.startsWith("Secondary structures in dot-bracket")) {
        readStructure();
      } else if (line.startsWith("Mapping of")) {
        mapGroups(htGroup1);
      }
    }
    dssr.put("summary", message.toString());
    return message.toString();
  }


//Mapping of 76 nucleotide identifiers
//  no.    3-letter   1-letter    idstr
//  58     1MA         a        [1MA]58:A
//  76       A         A        [A]76:A
//0         1         2
//0123456789012345678901

  private void mapGroups(Map<String, String> map) throws Exception {
    rd();
    int n = 0;
    String s = "";
    if (NucleicPolymer.htGroup1 == null)
      NucleicPolymer.htGroup1 = new Hashtable<String, String>();
    while (rd() != null && line.length() > 21) {
      String g3 = line.substring(9, 12).trim();
      String g1 = line.substring(21, 22);
      if ("ACGTU".indexOf(g1) < 0) {
        NucleicPolymer.htGroup1.put(g3, g1);
        String key = " " + g3 + "(" + g1 + ")";
        if (s.indexOf(key) < 0) {
          n++;
          s += key;
        }
        if (map != null)
          map.put(g3, g1);
      }
    }
    if (n > 0)
      addMessage(n + " nonstandard base" + (n > 1 ? "s" :"") + ":" + s);
  }
  
//  >1ehz nts=18 [whole]
//  TAACGGTTA&TAACCGTTA
//  .((((((((&)))))))).

  private void readStructure() throws Exception {
    addMessage("");
    addMessage(line);
    addMessage(rd());
    dssr.put("seq", rd());
    addMessage(line);
    dssr.put("dbn", rd());
    addToMessages(line);
    addMessage("");
  }

//  List of 39 H-bonds
//   15   578  #1     p    2.768 O/N O4@A.U2647 N1@A.G2673
//   35   555  #2     p    2.776 O/N O6@A.G2648 N3@A.U2672
//   36   554  #3     p    2.826 N/O N1@A.G2648 O2@A.U2672
//   55   537  #4     p    2.965 O/N O2@A.C2649 N2@A.G2671

  private void readHBonds(int n) throws Exception {
    Lst<Map<String, Object>> list = newList("hBonds");
    for (int i = 0; i < n; i++) {
      Map<String, Object> data = new Hashtable<String, Object>();
      String[] tokens = PT.getTokens(rd());
      data.put("atno1", Integer.valueOf(PT.parseInt(tokens[0])));
      data.put("atno2", Integer.valueOf(PT.parseInt(tokens[1])));
      data.put("id", tokens[2]);
      data.put("hbType", tokens[3]);
      data.put("distAng", Float.valueOf(tokens[4]));
      int pt = (tokens.length > 8 ? 6 : 5);
      data.put("energy", Float.valueOf(pt == 6 ? tokens[5] : "0"));
      data.put("label", tokens[pt++]);
      data.put("atom1", fix(tokens[pt++], true));
      data.put("atom2", fix(tokens[pt++], true));
      // --more option:
      //  1678  1703  #187   p    2.722    1.438 O/N O@B.LEU61 N@B.LEU64 primary
      if (pt < tokens.length)
        data.put("primary", Boolean.valueOf(tokens[pt++].equals("primary")));
      list.addLast(data);
    }
  }

  private void addToMessages(String s) throws Exception {
    if (s != null)
      addMessage(s);
    while (line != null && line.length() > 0 && line.indexOf("****") < 0) {
      addMessage(s == null ? line.trim() : line);
      rd();
    }
  }

  private void addMessage(String s) {
    message.append(s).append("\n");

  }

  private Lst<Map<String, Object>> newList(String name) {
    Lst<Map<String, Object>> list = new Lst<Map<String, Object>>();
    if (name != null)
      dssr.put(name, list);
    return list;
  }

  /**
   * List of 40 coaxial stacks
   * 
   * 1 Helix#1 contains 4 stems: [#1,#2,#3,#4]
   * 
   * 2 Helix#4 contains 4 stems: [#6,#7,#9,#13]
   * 
   * 3 Helix#10 contains 3 stems: [#16,#17,#18]
   * 
   * 4 Helix#13 contains 2 stems: [#21,#22]
   * 
   * @param n
   * @throws Exception
   */
  private void readStacks(int n) throws Exception {
    Lst<Map<String, Object>> list = newList("coaxialStacks");
    for (int i = 0; i < n; i++) {
      Map<String, Object> data = new Hashtable<String, Object>();
      String[] tokens = PT.getTokens(rd());
      data.put("helix", tokens[1]);
      data.put("stemCount", Integer.valueOf(tokens[3]));
      data.put("stems", tokens[5]);
      data.put("basePairs", getLinkNTList(tokens[5], "stem", null));
      list.addLast(data);
    }
  }

  /**
   * Default for no real processing -- just the lines
   * 
   * @param key
   * @param n
   * @return list of information
   * @throws Exception
   */
  private Lst<String> readInfo(String key, int n) throws Exception {
    Lst<String> list = new Lst<String>();
    if (key != null)
      dssr.put(key, list);
    for (int i = 0; i < n; i++)
      list.addLast(rd());
    return list;
  }

//  ****************************************************************************
//  Note: for the various types of loops listed below, numbers within the first
//      set of brackets are the number of loop nts, and numbers in the second
//      set of brackets are the identities of the stems (positive number) or
//      lone WC/wobble pairs (negative numbers) to which they are linked.
//
//  ****************************************************************************
//  List of 68 hairpin loops
//   1 hairpin loop: nts=10; [8]; linked by [#7]
//     nts=10 UGCCAAGCUG 0.U55,0.G56,0.C57,0.C58,0.A59,0.A60,0.G61,0.C62,0.U63,0.G64
//       nts=8 GCCAAGCU 0.G56,0.C57,0.C58,0.A59,0.A60,0.G61,0.C62,0.U63
//
//
//  ****************************************************************************
//  List of 67 internal loops
//   1 symmetric internal loop: nts=14; [5,5]; linked by [#1,#-1]
//     nts=14 GUGGAUUAUGAAAU 0.G21,0.U22,0.G23,0.G24,0.A25,0.U26,0.U27,0.A516,0.U517,0.G518,0.A519,0.A520,0.A521,0.U522
//       nts=5 UGGAU 0.U22,0.G23,0.G24,0.A25,0.U26
//       nts=5 UGAAA 0.U517,0.G518,0.A519,0.A520,0.A521
//   2 asymmetric internal loop: nts=7; [1,2]; linked by [#4,#-2]
//     nts=7 GCGCAAC 0.G39,0.C40,0.G41,0.C440,0.A441,0.A442,0.C443
//       nts=1 C 0.C40
//       nts=2 AA 0.A441,0.A442
//   3 symmetric internal loop: nts=8; [2,2]; linked by [#8,#9]
//     nts=8 CAAGCACG 0.C58,0.A59,0.A60,0.G61,0.C85,0.A86,0.C87,0.G88
//       nts=2 AA 0.A59,0.A60
//       nts=2 AC 0.A86,0.C87
//
//
//  List of 1 kissing loop interaction
//   1 stem #29 between hairpin loops #12 and #50
//
//

  private void readLoops(int n) throws Exception {
    if (line.indexOf("internal") >= 0) {
      readSets("internalLoops", n, 2, 4);
    } else if (line.indexOf("hairpin") >= 0) {
      readSets("hairpinLoops", n, 1, 3);
    } else if (line.indexOf("kissing") >= 0) {
      readSets("kissingLoops", n, -1, -1);
    }
  }

//  List of 35 junctions
//   1 3-way junction: nts=12; [0,6,0]; linked by [#-1,#2,#-14]
//     nts=12 UGCUGCAAAGCA 0.U27,0.G28,0.C480,0.U481,0.G482,0.C483,0.A484,0.A485,0.A486,0.G487,0.C515,0.A516
//       nts=0
//       nts=6 UGCAAA 0.U481,0.G482,0.C483,0.A484,0.A485,0.A486
//       nts=0
//   2 3-way junction: nts=27; [2,15,4]; linked by [#2,#3,#30]
//     nts=27 CUCGCGAUAGUGAACAAGUAGCGAACG 0.C29,0.U30,0.C31,0.G32,0.C451,0.G452,0.A453,0.U454,0.A455,0.G456,0.U457,0.G458,0.A459,0.A460,0.C461,0.A462,0.A463,0.G464,0.U465,0.A466,0.G467,0.C474,0.G475,0.A476,0.A477,0.C478,0.G479
//       nts=2 UC 0.U30,0.C31
//       nts=15 GAUAGUGAACAAGUA 0.G452,0.A453,0.U454,0.A455,0.G456,0.U457,0.G458,0.A459,0.A460,0.C461,0.A462,0.A463,0.G464,0.U465,0.A466
//       nts=4 GAAC 0.G475,0.A476,0.A477,0.C478
//   3 6-way junction: nts=36; [0,3,9,3,3,6]; linked by [#-2,#5,#-4,#15,#16,#18]
//     nts=36 GCGGAACGGAACAGAAAAUGAUGUACAGCUAAACAC 0.G41,0.C42,0.G149,0.G150,0.A151,0.A152,0.C153,0.G184,0.G185,0.A186,0.A187,0.C188,0.A189,0.G190,0.A191,0.A192,0.A193,0.A194,0.U202,0.G203,0.A204,0.U205,0.G206,0.U233,0.A234,0.C235,0.A236,0.G237,0.C433,0.U434,0.A435,0.A436,0.A437,0.C438,0.A439,0.C440
//       nts=0
//       nts=3 GAA 0.G150,0.A151,0.A152
//       nts=9 GAACAGAAA 0.G185,0.A186,0.A187,0.C188,0.A189,0.G190,0.A191,0.A192,0.A193
//       nts=3 GAU 0.G203,0.A204,0.U205
//       nts=3 ACA 0.A234,0.C235,0.A236
//       nts=6 UAAACA 0.U434,0.A435,0.A436,0.A437,0.C438,0.A439

  private void readJunctions(int n) throws Exception {
    readSets("junctions", n, 0, 3);
  }

//  ****************************************************************************
//  List of 38 bulges
//   1 bulge: nts=5; [0,1]; linked by [#3,#4]
//     nts=5 GCGAC 0.G33,0.C34,0.G448,0.A449,0.C450
//       nts=0
//       nts=1 A 0.A449
//   2 bulge: nts=5; [0,1]; linked by [#-4,#14]
//     nts=5 CCGAG 0.C153,0.C154,0.G182,0.A183,0.G184
//       nts=0
//       nts=1 A 0.A183

  private void readBulges(int n) throws Exception {
    readSets("bulges", n, 2, 2);
  }

  private void readSets(String key, int n, int nway, int ptnts)
      throws Exception {
    Lst<Map<String, Object>> sets = newList(key);
    boolean isJunction = (nway == 0);
    boolean isKissingLoop = (ptnts == -1);
    for (int i = 0; i < n; i++) {
      Map<String, Object> set = new Hashtable<String, Object>();
      String[] tokens = PT.getTokens(rd());
      set.put("id", tokens[0]);
      htTemp.put(key + tokens[0], set);
      Lst<Object> lst = new Lst<Object>();
      set.put("desc", line);
      if (isKissingLoop) {
        //    1 stem #8 between hairpin loops #1 and #3
        //    0   1  2    3       4       5    6   7  8
        getNTs(getLinkNTList(tokens[2], "stem", null), lst, true, false);
        getNTs(getLinkNTList(tokens[6], "hairpinLoops", null), lst, false,
            false);
        getNTs(getLinkNTList(tokens[8], "hairpinLoops", null), lst, false,
            false);
        set.put("nts", lst);
        lst = new Lst<Object>();
        getNTs(getLinkNTList(tokens[2], "stem", null), lst, true, true);
        getNTs(getLinkNTList(tokens[6], "hairpinLoops", null), lst, false, true);
        getNTs(getLinkNTList(tokens[8], "hairpinLoops", null), lst, false, true);
        set.put("resnos", lst);
      } else {
        set.put("dssrType", tokens[1]);
        if (isJunction)
          nway = PT.parseInt(tokens[1].substring(0, tokens[1].indexOf("-")));
        set.put("nway", Integer.valueOf(nway));
        set.put("n", Integer.valueOf(PT.trim(tokens[ptnts], ";").substring(4)));
        set.put("linkedBy", getLinkNTList(tokens[ptnts + 4], "stem", lst));
        set.put("basePairs", readNTList(key + "#" + (i + 1), null, nway + 1));
      }
      sets.addLast(set);
    }
  }

  @SuppressWarnings("unchecked")
  private void getNTs(Lst<Object> linkNTList, Lst<Object> lst, boolean isStem,
                      boolean isResno) {
    Lst<Object> o = (Lst<Object>) linkNTList.get(0);
    int n = o.size();
    String key = (!isResno ? "nt" : isStem ? "res" : "resno");
    Lst<Object> nts = (isStem ? new Lst<Object>() : null);
    for (int i = 0; i < n; i++) {
      Map<String, Object> m = (Map<String, Object>) o.get(i);
      if (isStem) {
        nts.addLast(m.get(key + "1"));
        nts.addLast(m.get(key + "2"));
      } else {
        lst.addLast(m.get(key + "s"));
      }
    }
    if (isStem) {
      lst.addLast(nts);
    }
  }

  private Lst<Object> getLinkNTList(String linkStr, String type,
                                    Lst<Object> list) {
    //  [#3,#4]
    if (list == null)
      list = new Lst<Object>();
    String[] tokens = PT
        .getTokens(PT.replaceAllCharacters(linkStr, "[,]", " "));
    for (int i = 0; i < tokens.length; i++)
      list.addLast(htTemp.get((tokens[i].startsWith("-") ? "" : type)
          + tokens[i]));
    return list;
  }

//  List of 106 A-minor motifs
//   1  type=I A/U-A  0.A48/0.U43,0.A148 WC
//        -0.U43  H-bonds[1]: "O2'(hydroxyl)-O2'(hydroxyl)[2.90]"
//        +0.A148 H-bonds[1]: "N1-O2'(hydroxyl)[2.74]"
//   2  type=I A/G-C  0.A69/0.G54,0.C65 WC
//        +0.G54  H-bonds[2]: "N1-O2'(hydroxyl)[2.69],N3-N2(amino)[2.84]"
//        -0.C65  H-bonds[2]: "O2'(hydroxyl)-O2'(hydroxyl)[2.62],O2'(hydroxyl)-O2(carbonyl)[2.61]"
//   3  type=I A/G-C  0.A98/0.G81,0.C93 WC
//        +0.G81  H-bonds[2]: "N1-O2'(hydroxyl)[2.67],N3-N2(amino)[3.12]"
//        -0.C93  H-bonds[0]: ""
//   4  type=II A/G-C 0.A152/0.G41,0.C440 WC
//        +0.G41  H-bonds[0]: ""
//        -0.C440 H-bonds[3]: "O2'(hydroxyl)-O3'[3.17],O2'(hydroxyl)-O2'(hydroxyl)[2.73],N3-O2'(hydroxyl)[2.70]"

  private void readMotifs(int n) throws Exception {
    Lst<Map<String, Object>> motifs = newList("aMinorMotifs");
    for (int i = 0; i < n; i++) {
      Map<String, Object> motif = new Hashtable<String, Object>();
      String[] tokens = PT.getTokens(rd());
      motif.put("motiftype", after(tokens[1], "=") + " " + tokens[2]);
      motif.put("info", line);
      motif.put("data", readInfo(null, 2));
      motifs.addLast(motif);
    }
  }

 
//  List of 9 (possible) kink turns
//   1 Normal k-turn with GA on NC-helix#5; iloop#4
//      C#11[0.C93,0.G81 CG] [0.G97,0.A80 GA] NC#10[0.G77,0.C100 GC]
//      strand1 nts=15; GGCGAAGAACCAUGG 0.G91,0.G92,0.C93,0.G94,0.A95,0.A96,0.G97,0.A98,0.A99,0.C100,0.C101,0.A102,0.U103,0.G104,0.G105
//      strand2 nts=12; CCAUGGGGAGCC 0.C72,0.C73,0.A74,0.U75,0.G76,0.G77,0.G78,0.G79,0.A80,0.G81,0.C82,0.C83
//   2 Undecided case with GA on NC-helix#14; iloop#7
//      C#22[0.C280,0.G369 CG] [0.A285,0.G367 AG] NC#-9[0.G365,0.C287 GC]
//      strand1 nts=13; GCUACCUCUCAUC 0.G275,0.C276,0.U277,0.A278,0.C279,0.C280,0.U281,0.C282,0.U283,0.C284,0.A285,0.U286,0.C287
//      strand2 nts=10; GUGCGGUAGU 0.G365,0.U366,0.G367,0.C368,0.G369,0.G370,0.U371,0.A372,0.G373,0.U374
   
  private void readTurns(int n) throws Exception {
    Lst<Map<String, Object>> turns = newList("kinkTurns");
    for (int i = 0; i < n; i++) {
      Map<String, Object> turn = new Hashtable<String, Object>();
      String[] tokens = PT.getTokens(rd());
      turn.put("turnType", tokens[1]);
      turn.put("info", line);
      turn.put("details", rd());
      turn.put("basePairs", readNTList(null, null, 2));
      turns.addLast(turn);
    }
  }

//  List of 12 base pairs
//      nt1              nt2             bp  name         Saenger    LW DSSR
//   1 A.C1             B.G12            C-G WC           19-XIX    cWW cW-W
//   2 A.G2             B.C11            G-C WC           19-XIX    cWW cW-W
//   3 A.C3             B.G10            C-G WC           19-XIX    cWW cW-W
//   4 A.G4             B.C9             G-C WC           19-XIX    cWW cW-W
//   5 A.A5             B.T8             A-T WC           20-XX     cWW cW-W
//   6 A.A6             B.T7             A-T WC           20-XX     cWW cW-W
//   7 A.T7             B.A6             T-A WC           20-XX     cWW cW-W
//   8 A.T8             B.A5             T-A WC           20-XX     cWW cW-W
//   9 A.C9             B.G4             C-G WC           19-XIX    cWW cW-W
//  10 A.G10            B.C3             G-C WC           19-XIX    cWW cW-W
//  11 A.C11            B.G2             C-G WC           19-XIX    cWW cW-W
//  12 A.G12            B.C1             G-C WC           19-XIX    cWW cW-W
//  
//     9 [U]8:A           [A]21:A          U+A --           n/a       ... ....
//       [-161.5(anti) C3'-endo lambda=111.1] [-160.2(anti) C3'-endo lambda=50.1]
//       d(C1'-C1')=8.76 d(N1-N9)=8.70 d(C6-C8)=10.78 tor(C1'-N1-N9-C1')=158.5
//       H-bonds[1]: "O2'(hydroxyl)-N1[2.68]"
//       bp-pars: [-1.22   -8.06   -0.19   -9.35   19.04   -117.98]
//       
//       
//   9 [U]8:A           [A]21:A          U+A --           00-n/a    ... ....
//       [-161.5(anti) C3'-endo lambda=111.1] [-160.2(anti) C3'-endo lambda=50.1]
//       d(C1'-C1')=8.76 d(N1-N9)=8.70 d(C6-C8)=10.78 tor(C1'-N1-N9-C1')=158.5
//       H-bonds[1]: "O2'(hydroxyl)-N1[2.68]"
//       bp-pars: [-1.22   -8.06   -0.19   -9.35   19.04   -117.98]
//
//
//  List of 50 lone WC/wobble pairs
//  Note: lone WC/wobble pairs are assigned negative indices to differentiate
//        them from the stem numbers, which are positive.
//        ------------------------------------------------------------------
//  -1 0.U27            0.A516           U-A WC           20-XX     cWW cW-W

  
  private void readPairs(int n) throws Exception {
    Lst<Map<String, Object>> pairs;
    if (line.indexOf("lone ") >= 0) {
      // just store negative indices in temporary map.
      // pointing to original base pair
      rd();
      skipHeader();
      pairs = newList("lonePairs");
      for (int i = 0; i < n; i++) {
        String[] tokens = PT.getTokens(line);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) htTemp.get(tokens[1]
            + tokens[2]);
        htTemp.put("#" + line.substring(0, 5).trim(), data);
        data.put("lonePair", Boolean.TRUE);
        pairs.addLast(data);
        rd();
      }
      return;
    }
    basePairs = newList("basePairs");
    skipHeader();
    for (int i = 0; i < n; i++)
      getBPData(0, null, true);
  }

//  List of 1 helix
//  Note: a helix is defined by base-stacking interactions, regardless of bp
//        type and backbone connectivity, and may contain more than one stem.
//      helix#number[stems-contained] bps=number-of-base-pairs in the helix
//      bp-type: '|' for a canonical WC/wobble pair, '.' otherwise
//      helix-form: classification of a dinucleotide step comprising the bp
//        above the given designation and the bp that follows it. Types
//        include 'A', 'B' or 'Z' for the common A-, B- and Z-form helices,
//        '.' for an unclassified step, and 'x' for a step without a
//        continuous backbone.
//      --------------------------------------------------------------------
//  helix#1[1] bps=12
//      strand-1 5'-CGCGAATTCGCG-3'
//       bp-type    ||||||||||||
//      strand-2 3'-GCGCTTAAGCGC-5'
//      helix-form  BBBBBBBBBBB
//   1 A.C1             B.G12            C-G WC           19-XIX    cWW cW-W
//   2 A.G2             B.C11            G-C WC           19-XIX    cWW cW-W
//   3 A.C3             B.G10            C-G WC           19-XIX    cWW cW-W
//   4 A.G4             B.C9             G-C WC           19-XIX    cWW cW-W
//   5 A.A5             B.T8             A-T WC           20-XX     cWW cW-W
//   6 A.A6             B.T7             A-T WC           20-XX     cWW cW-W
//   7 A.T7             B.A6             T-A WC           20-XX     cWW cW-W
//   8 A.T8             B.A5             T-A WC           20-XX     cWW cW-W
//   9 A.C9             B.G4             C-G WC           19-XIX    cWW cW-W
//  10 A.G10            B.C3             G-C WC           19-XIX    cWW cW-W
//  11 A.C11            B.G2             C-G WC           19-XIX    cWW cW-W
//  12 A.G12            B.C1             G-C WC           19-XIX    cWW cW-W
//  
//  List of 1 stem
//  Note: a stem is defined as a helix consisting of only canonical WC/wobble
//        pairs, with a continuous backbone.
//      stem#number[#helix-number containing this stem]
//      Other terms are defined as in the above Helix section.
//      --------------------------------------------------------------------
//  stem#1[#1] bps=12
//      strand-1 5'-CGCGAATTCGCG-3'
//       bp-type    ||||||||||||
//      strand-2 3'-GCGCTTAAGCGC-5'
//      helix-form  BBBBBBBBBBB
//   1 A.C1             B.G12            C-G WC           19-XIX    cWW cW-W
//   2 A.G2             B.C11            G-C WC           19-XIX    cWW cW-W
//   3 A.C3             B.G10            C-G WC           19-XIX    cWW cW-W
//   4 A.G4             B.C9             G-C WC           19-XIX    cWW cW-W
//   5 A.A5             B.T8             A-T WC           20-XX     cWW cW-W
//   6 A.A6             B.T7             A-T WC           20-XX     cWW cW-W
//   7 A.T7             B.A6             T-A WC           20-XX     cWW cW-W
//   8 A.T8             B.A5             T-A WC           20-XX     cWW cW-W
//   9 A.C9             B.G4             C-G WC           19-XIX    cWW cW-W
//  10 A.G10            B.C3             G-C WC           19-XIX    cWW cW-W
//  11 A.C11            B.G2             C-G WC           19-XIX    cWW cW-W
//  12 A.G12            B.C1             G-C WC           19-XIX    cWW cW-W

  /**
   * 
   * @param i0
   * @param type
   * @param readParams
   *        helix or base pair, not stem
   * @return data
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  private Map<String, Object> getBPData(int i0, String type, boolean readParams)
      throws Exception {
    String[] tokens = PT.getTokens(line);
    String nt12 = tokens[1] + tokens[2];
    Map<String, Object> data;
    //boolean isReversed = false;
    data = (Map<String, Object>) htTemp.get(nt12);
    int i = i0;
    if (data == null) {
      data = new Hashtable<String, Object>();
      i = PT.parseInt(tokens[0]);
      if (type != null)
        i = -((Integer) ((Map<String, Object>) htTemp
            .get(tokens[2] + tokens[1])).get("id")).intValue();
      data.put("id", Integer.valueOf(i));
      String nt1 = fix(tokens[1], true);
      String nt2 = fix(tokens[2], true);
      data.put("key", nt1 + " " + nt2);
      data.put("nt1", nt1);
      data.put("nt2", nt2);
      data.put("nt2", fix(tokens[2], true));
      data.put("res1", fix(tokens[1], false));
      data.put("res2", fix(tokens[2], false));
      String bp = tokens[3];
      data.put("bp", bp);
      data.put("g1", bp.substring(0, 1));
      data.put("g2", bp.substring(2, 3));
      // helix can be missing name
      //    1 0.C2769          0.A2805          C-A              n/a    t.S c.-m

      int pt = (tokens.length == 8 ? 5 : 4);
      data.put("name", pt == 5 ? tokens[4] : "?");
      int pt1 = tokens[pt].indexOf("-");
      data.put("Saenger",
          Integer.valueOf(pt1 > 0 ? tokens[pt].substring(0, pt1) : "0"));
      data.put("LW", tokens[++pt]);
      data.put("DSSR", tokens[++pt]);
      htTemp.put(nt12, data);
      basePairs.addLast(data);
    }
    if (type != null)
      data.put(type + "Id", Integer.valueOf(i0));
    if (readParams)
      readMore(data, type == null, i < 0);
    else
      skipHeader();
    return data;
  }

//  "--more" option for base pairs:
//   [-167.8(anti) C3'-endo lambda=42.1] [-152.8(anti) C3'-endo lambda=68.6] 
//   d(C1'-C1')=10.44 d(N1-N9)=8.84 d(C6-C8)=9.70 tor(C1'-N1-N9-C1')=-8.1 
//   H-bonds[2]: "O6(carbonyl)-N3(imino)[2.78],N1(imino)-O2(carbonyl)[2.83]" 
//   bp-pars: [-2.37 -0.60 0.11 4.67 -7.75 -2.95]
//  
//   stems/helices:
//   
//       bp1-pars:  [-0.09    -0.11    0.08     -4.80    -11.89   0.28]
//       step-pars:  [0.38     -1.60    2.63     -5.03    7.04     22.51]
//       heli-pars:  [-5.46    -2.08    1.92     17.23    12.32    24.10]
//        bp2-pars:  [-2.56    -0.51    0.46     11.05    -9.69    -1.55]
//       C1'-based:                rise=3.62                 twist=36.20
//       C1'-based:              h-rise=2.57               h-twist=37.91

  private void readMore(Map<String, Object> data, boolean isBP, boolean isRev)
      throws Exception {
    String info = "";
    while (isHeader(rd())) {
      int pt = line.indexOf("[");
      line = PT.rep(line, "_pars", "-pars");
      if (isBP) {
        if (line.indexOf("bp-pars:") >= 0) {
          addArray(data, "bp", PT.parseFloatArray(line.substring(pt + 1)));
        } else if (line.indexOf("lambda") >= 0) {
          //[-156.7(anti) C3'-endo lambda=84.4] [-165.6(anti) C3'-endo lambda=38.0]
          extractFloats(data, htPar.get("bpChiLambda"));
        } else if (line.indexOf("tor(") >= 0) {
          // d(C1'-C1')=10.44 d(N1-N9)=8.84 d(C6-C8)=9.70 tor(C1'-N1-N9-C1')=-8.1 
          extractFloats(data, htPar.get("bpDistTor"));
        }
        info += line + "\n";
      } else {
        if (isRev && line.indexOf("bp1-pars:") >= 0) {
          addArray(data, "bp", PT.parseFloatArray(line.substring(pt + 1)));
        } else if (line.indexOf("heli-pars:") >= 0) {
          addArray(data, "hel", PT.parseFloatArray(line.substring(pt + 1)));
        } else if (line.indexOf("step-pars:") >= 0) {
          addArray(data, "step", PT.parseFloatArray(line.substring(pt + 1)));
        } else if ((pt = line.indexOf("h-rise=")) >= 0) {
          addFloat(data, "heRiseC1", pt + 7);
          addFloat(data, "heTwistC1", line.indexOf("h-twist=") + 8);
        } else if ((pt = line.indexOf("rise=")) >= 0) {
          addFloat(data, "stRiseC1", pt + 5);
          addFloat(data, "stTwistC1", line.indexOf("twist=") + 6);
        }
      }
    }
    if (isBP)
      data.put("info", info);
  }

  private int[] next = new int[1];

  private void extractFloats(Map<String, Object> data, String[] names) {
    line = line.replace('[', '=').replace('(', ' ').replace(']', ' ');
    next[0] = -1;
    int n = names.length;
    for (int i = 0, pt = 0; i < n; i++) {
      if ((next[0] = pt = line.indexOf("=", pt) + 1) == 0)
        break;
      data.put(names[i], Float.valueOf(PT.parseFloatNext(line, next)));
    }
  }

  private void addArray(Map<String, Object> data, String key, float[] f) {
    String[] keys = htPar.get(key);
    int n = Math.min(f.length, keys == null ? f.length : keys.length);
    for (int i = 0; i < n; i++)
      data.put(keys == null ? key + (i + 1) : keys[i], Float.valueOf(f[i]));
  }

  private void addFloat(Map<String, Object> data, String key, int pt) {
    data.put(
        key,
        Float.valueOf(PT.parseFloat(line.substring(pt,
            Math.min(line.length(), pt + 10)))));
  }

//  List of 31 non-loop single-stranded segments
//   1 nts=3 UAU 0.U10,0.A11,0.U12
//   2 nts=1 A 0.A128
//
//  List of 46 ribose zippers
//   1 nts=4 UUAG 0.U26,0.U27,0.A1318,0.G1319
//   2 nts=4 ACAC 0.A152,0.C153,0.A439,0.C440
//   
//  List of 233 multiplets
//  10 nts=3 AAA 0.A59,0.A60,0.A86
//  11 nts=3* AGG 0.A80,0.G94,0.G97

  private Lst<Map<String, Object>> readNTList(String ntsKey, String type, int n)
      throws Exception {
    boolean isHairpin = (n == 2);
    Lst<Map<String, Object>> list = newList(type);
    if (ntsKey != null)
      htTemp.put(ntsKey, list);
    if (isHairpin)
      rd();
    for (int i = (isHairpin ? 1 : 0); i < n; i++)
      list.addLast(getNTList());
    return list;
  }

  private void getHelixOrStem(int n, String key, String type, boolean isHelix)
      throws Exception {
    Lst<Map<String, Object>> list = newList(key);
    for (int i = 0; i < n; i++) {
      skipTo("  " + type + "#", true);
      int bps = PT.parseInt(after(line, "="));
      Map<String, Object> data = new Hashtable<String, Object>();
      String header = getHeader();
      data.put("info", header);
      data.put("bpCount", Integer.valueOf(bps));
      if (isHelix) {
        String[] lines = PT.split(header, "\n");
        if (lines.length == 8) {
          data.put("helicalAxisData", after(lines[5], "s"));
          data.put("p1", getPoint(lines[6]));
          data.put("p2", getPoint(lines[7]));
          //    helical-axis[2.87(0.31)]:   0.534   0.823   0.193 
          //    point-one:  49.135  20.676  97.513
          //    point-two:  56.822  32.522 100.293
        }
      }

      list.addLast(data);
      Lst<Map<String, Object>> pairs = newList(null);
      data.put("basePairs", pairs);
      htTemp.put(type + "#" + (i + 1), pairs);
      for (int j = 0; j < bps; j++)
        pairs.addLast(getBPData(i + 1, type, isHelix));
    }
  }

  private P3 getPoint(String data) {
    float[] a = PT.parseFloatArray(after(data, ":"));
    return P3.new3(a[0], a[1], a[2]);

  }

  private Map<String, Object> getNTList() throws Exception {
    Map<String, Object> data = new Hashtable<String, Object>();
    //1 nts=0
    //3 nts=4 CGAA 0.C303,0.G304,0.A305,0.A306
    //nts=8 GCCAAGCU 0.G56,0.C57,0.C58,0.A59,0.A60,0.G61,0.C62,0.U63
    String[] tokens = PT.getTokens(rd());
    int pt = (tokens[0].startsWith("nts") ? 0 : 1);
    if (tokens.length > pt + 2) {
      data.put("nres", Integer.valueOf(PT.replaceAllCharacters(
          after(tokens[pt], "="), "*;", "")));
      data.put("seq", tokens[++pt]);
      data.put("nts", getNT(tokens[++pt], false));
      data.put("resnos", getNT(tokens[pt], true));
    }
    return data;
  }

  private Object getNT(String s, boolean isResno) {
    String[] tokens = PT.split(s, ",");
    Lst<Object> list = new Lst<Object>();
    for (int i = 0; i < tokens.length; i++)
      list.addLast(fix(tokens[i], !isResno));
    return list;
  }

  private String getHeader() throws Exception {
    SB header = new SB();
    header.append(line).append("\n");
    while (isHeader(rd()))
      header.append(line).append("\n");
    return header.toString();
  }

  /**
   * Numbers are right justified in columns 0-3 followed by a space and a
   * character;
   * 
   * base-pair data start in column 5, but notes start with "Note: " or "     ",
   * both of which have a space in column 5.
   * 
   * 
   * 
   * @throws Exception
   */
  private void skipHeader() throws Exception {
    while (isHeader(rd())) {
    }
  }

  private boolean isHeader(String line) {
    return line.length() < 6 || line.charAt(3) == ' ' || line.charAt(5) == ' ';
  }

  private void skipTo(String key, boolean startsWith) throws Exception {
    while (!(startsWith ? line.startsWith(key) : line.contains(key))) {
      rd();
    }
  }

  /**
   * A.T8 --> [T]8:A N1@A.G2673 --> [G]2673:A.N1
   * 
   * A.US3/4 --> [US3]4:A
   * 
   * @param nt
   * @param withName
   * @return Jmol atom residue as [name]resno:chain or just resno:chain
   */
  private String fix(String nt, boolean withName) {
    int pt1;
    if (nt.startsWith("[")) {
      // Jmol [res]resno^ins.atm%alt
      // disregard any model indicator
      if ((pt1 = nt.indexOf("/")) >= 0)
        nt = nt.substring(0, pt1);
      if (withName)
        return nt;
      if ((pt1 = nt.indexOf(".")) >= 0)
        nt = nt.substring(0, pt1);
      return (nt.substring(nt.indexOf("]") + 1));
    }
    pt1 = nt.indexOf(".");
    String chain = nt.substring(0, pt1);
    int pt = nt.length();
    char ch;
    while (PT.isDigit(ch = nt.charAt(--pt))) {
    }
    int ptn = chain.indexOf("@");
    if (ptn >= 0)
      chain = chain.substring(ptn + 1)
          + (withName ? "." + chain.substring(0, ptn) : "");
    int pt2 = (ch == '/' ? pt : pt + 1);
    return (withName ? "[" + nt.substring(pt1 + 1, pt2) + "]" : "")
        + nt.substring(pt + 1) + ":" + chain;
  }

  private String after(String s, String key) {
    return s.substring(s.indexOf(key) + 1);
  }

  private String rd() throws Exception {
    line = reader.readNextLine();
    if (Logger.debugging)
      Logger.info(line);
    return line;
  }

  //////////////// Annotation post load /////////////////
  
  ////////////////////  DSSR ///////////////
  
  @SuppressWarnings("unchecked")
  @Override
  public void setAllDSSRParametersForModel(Viewer vwr, int modelIndex) {
    Map<String, Object> dssr = (Map<String, Object>) vwr.ms.getInfo(modelIndex,
        "dssr");
    Lst<Map<String, Object>> lst = (dssr == null ? null
        : (Lst<Map<String, Object>>) dssr.get("basePairs"));
    Lst<Map<String, Object>> lst1 = (dssr == null ? null
        : (Lst<Map<String, Object>>) dssr.get("singleStranded"));

    if (lst == null && lst1 == null) {
      BioModel m = (BioModel) vwr.ms.am[modelIndex];
      int n = m.getBioPolymerCount();
      for (int i = n; --i >= 0;) {
        BioPolymer bp = m.getBioPolymer(i);
        if (bp.isNucleic())
          ((NucleicPolymer) bp).isDssrSet = true;
      }
      return;
    }
    Map<String, BS> htChains = new Hashtable<String, BS>();
    BS bs = new BS();
    if (lst != null) {
      for (int i = lst.size(); --i >= 0;) {
        Map<String, Object> bpInfo = lst.get(i);
        BasePair.add(bpInfo, setDSSRPhos(vwr, 1, bpInfo, bs, htChains), setDSSRPhos(
            vwr, 2, bpInfo, bs, htChains));
      }
    }
    if (lst1 != null)
      for (int i = lst1.size(); --i >= 0;) {
        Map<String, Object> bp = lst1.get(i);
        Lst<Object> resnos = (Lst<Object>) bp.get("resnos");
        for (int j = resnos.size(); --j >= 0;)
          setDSSRRes(vwr, (String) resnos.get(j), bs, htChains);
      }
  }

  private NucleicMonomer setDSSRPhos(Viewer vwr, int n, Map<String, Object> bp,
                                 BS bs, Map<String, BS> htChains) {
    return setDSSRRes(vwr, (String) bp.get("res" + n), bs, htChains);
  }

  private NucleicMonomer setDSSRRes(Viewer vwr, String res, BS bs,
                                Map<String, BS> htChains) {
    bs.clearAll();
    getDSSRAtoms(vwr, res, null, bs, htChains);
    NucleicMonomer group = (NucleicMonomer) vwr.ms.at[bs.nextSetBit(0)]
        .getGroup();
    ((NucleicPolymer) group.bioPolymer).isDssrSet = true;
    return group;
  }

  @SuppressWarnings("unchecked")
  @Override
  public String getHBonds(ModelSet ms, int modelIndex, Lst<Bond> vHBonds,
                          boolean doReport) {
    Object info = ms.getInfo(modelIndex, "dssr");
    if (info != null)
      info = ((Map<String, Object>) info).get("hBonds");
    if (info == null)
      return "no DSSR hydrogen-bond data";
    Lst<Map<String, Object>> list = (Lst<Map<String, Object>>) info;
    int a0 = ms.am[modelIndex].firstAtomIndex - 1;
    try {
      for (int i = list.size(); --i >= 0;) {
        Map<String, Object> hbond = list.get(i);
        int a1 = ((Integer) hbond.get("atno1")).intValue() + a0;
        int a2 = ((Integer) hbond.get("atno2")).intValue() + a0;
        float energy = (hbond.containsKey("energy") ? ((Float) hbond
            .get("energy")).floatValue() : 0);
        vHBonds.addLast(new HBond(ms.at[a1], ms.at[a2], Edge.BOND_H_REGULAR,
            (short) 1, C.INHERIT_ALL, energy));
      }
    } catch (Exception e) {
      Logger.error("Exception " + e + " in DSSRParser.getHBonds");
    }
    return "DSSR reports " + list.size() + " hydrogen bonds";
  }

  @Override
  public String calculateDSSRStructure(Viewer vwr, BS bsAtoms) {
    BS bs = vwr.ms.getModelBS(bsAtoms == null ? vwr.bsA() : bsAtoms, true);
    String s = "";
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
      s += getDSSRForModel(vwr, i);
    return s;
  }

  @SuppressWarnings("unchecked")
  private String getDSSRForModel(Viewer vwr, int modelIndex) {
    Map<String, Object> info = null;
    String out = null;
    while (true) {
      if (!vwr.ms.am[modelIndex].isBioModel)
        break;
      info = vwr.ms.getModelAuxiliaryInfo(modelIndex);
      if (info.containsKey("dssr"))
        break;
      BS bs = vwr.getModelUndeletedAtomsBitSet(modelIndex);
      bs.and(vwr.ms.getAtoms(T.nucleic, null));
      if (bs.nextClearBit(0) < 0) {
        info = null;
        break;
      }
      try {
        String name = (String) vwr.setLoadFormat("=dssrModel/", '=', false);
        name = PT.rep(name, "%20", " ");
        Logger.info("fetching " + name + "[pdb data]");
        String data = vwr.getPdbAtomData(bs, null);
        data = vwr.getFileAsString3(name + data, false, null);
        processDSSR(info, new Rdr(Rdr.getBR(data)), null, null);
      } catch (Exception e) {
        info = null;
        out = "" + e;
      }
      break;
    }
    return (info != null ? (String) ((Map<String, Object>) info.get("dssr"))
        .get("summary") : out == null ? "model has no nucleotides" : out);
  }

  
  ///////////////////// EBI annotations ////////////////


//format developed at EBI July 31, 2014
  
// domains:
//  http://wwwdev.ebi.ac.uk/pdbe/api/mappings/sequence_domains/1cbs?metadata=true&pretty=true
//  {
//    "1cbs": {
//        "Pfam": {
//            "PF00061": {
//                "identifier": "Lipocalin / cytosolic fatty-acid binding protein family",
//                "description": "Lipocalin / cytosolic fatty-acid binding protein family",
//                "mappings": [
//                    {
//                        "start": {
//                            "author_residue_number": 4,
//                            "author_insertion_code": "",
//                            "residue_number": 4
//                        },
//                        "entity_id": 1,
//                        "end": {
//                            "author_residue_number": 137,
//                            "author_insertion_code": "",
//                            "residue_number": 137
//                        },
//                        "chain_id": "A",
//                        "struct_asym_id": "A"
//                    }
//                ]
//            }
//        },
//        "InterPro": {
//            "IPR000566": {
//                "identifier": "Lipocalin/cytosolic fatty-acid binding domain",
//                "name": "Lipocalin/cytosolic fatty-acid binding domain",
//                "mappings": [
//                    {
//                        "entity_id": 1,
//                        "end": {
//                            "author_residue_number": 137,
//                            "author_insertion_code": "",
//                            "residue_number": 137
//                        },
//                        "start": {
//                            "author_residue_number": 4,
//                            "author_insertion_code": "",
//                            "residue_number": 4
//                        },
//                        "chain_id": "A",
//                        "struct_asym_id": "A"
//                    }
//                ]
//            },
// ...

// validation:
//  {
//    "1cbs": {
//        "bond_angles": {},
//        "rsrz": {},
//        "symm_clashes": {},
//        "rama": {},
//        "clashes": {
//            "outliers": [
//                {
//                    "units": [
//                        "|1|A|LEU|100|CD2|||",
//                        "|1|A|LYS|82|HG2|||"
//                    ],
//                    "value": 0.58999999999999997
//                },
//     ...
//            ]
//        },
//        "bond_lengths": {},
//        "RNA_pucker": {},
//        "planes": {},
//        "RNA_suite": {},
//        "sidechains": {
//            "outliers": [
//                {
//                    "units": [
//                        "|1|A|ASN|14||||"
//                    ]
//                },  
//...
  
 /**
  * Construct a nice listing for this annotation, including validation 
  * 
  * @param a
  * @param match
  * @param dotPath
  * @param sb
  * @param pre
  * @param showDetail
  * @param isMappingOnly
  * @param type
  */
 private void getAnnotationKVPairs(SV a, String match, String dotPath, SB sb,
                                   String pre, boolean showDetail,
                                   boolean isMappingOnly, int type) {
   Map<String, SV> map = a.getMap();
   if (map == null || map.isEmpty())
     return;
   if (map.containsKey("_map"))
     map = map.get("_map").getMap();
   //    map = map.values().iterator().next().getMap();
   String detailKey = getDataKey(type);
   if (showDetail && map.containsKey(detailKey)) {
     if (match == null || dotPath.indexOf(match) >= 0)
       sb.append(map.get(detailKey).asString()).append("\n");
     return;
   }
   for (Entry<String, SV> e : map.entrySet()) {
     String key = e.getKey();
     if (key.equals(detailKey))
       continue;
     if (key.equals("metadata"))
       sb.append("\n");
     SV val = e.getValue();
     if (val.tok == T.hash) {
       if (type == T.validation && !showDetail) {
         sb.append(key).append("\n");
       } else {
         getAnnotationKVPairs(val, match, (dotPath.length() == 0 ? ""
             : dotPath + ".") + key, sb, (pre.length() == 0 ? "" : pre + "\t")
             + key, showDetail, isMappingOnly, type);
       }
     } else {
       String s = val.asString();
       if (match == null || s.indexOf(match) >= 0 || pre.indexOf(match) >= 0
           || key.indexOf(match) >= 0 || dotPath.indexOf(match) >= 0) {
         if (showDetail && isMappingOnly)
           continue;
         if (pre.length() > 0)
           sb.append(pre).append("\t");
         sb.append(key).append("=");
         sb.append(s).append("\n");
       }
     }
   }
 }

  private String getDataKey(int type) {
    switch (type) {
    case T.domains:
      return "mappings";
    case T.validation: 
      return "outliers";
    }
  return null;
}

  /**
   * Returns a Lst<Object> of property data in the form name(String),
   * data(float[]), modelIndex (Integer), isGroup (Boolean);
   * 
   */
  @Override
  public Lst<Object> catalogValidations(Viewer viewer, SV map0,
                                        int[] modelAtomIndices,
                                        Map<String, int[]> resMap,
                                        Map<String, Integer> atomMap,
                                        Map<String, Integer> modelMap) {
    Map<String, SV> data = map0.getMap();
    if (data == null)
      return null;
    Lst<Object> retProperties = new Lst<Object>();
    int nModels = modelAtomIndices.length - 1;
    try {
      // get second level, skipping "xxxx":
      data = getMainItem(data).getMap();
      // add _map as a new top-level key pointing to the same thing
      map0.getMap().put("_map", SV.newV(T.hash, data));
      Lst<SV> list = new Lst<SV>();
      map0.getMap().put("_list", SV.newV(T.varray, list));

      //    {
      //      "1blu": {
      //  -->     "bond_angles": [
      //              {
      //                  "units": [
      //                      "|1|A|TYR|44|CB|||",
      //                      "|1|A|TYR|44|CG|||",
      //                      "|1|A|TYR|44|CD1|||"
      //                  ],
      //                  "value": 5.57
      //              },
      //
      /// each residue ID points to the specific properties that involve them. 
      Set<Entry<String, SV>> set = data.entrySet();
      SV sv;
      Map<String, SV> map;
      for (Entry<String, SV> e : set) {
        float[][] floats = AU.newFloat2(nModels);
        for (int m = nModels; --m >= 0;)
          floats[m] = new float[modelAtomIndices[m + 1] - modelAtomIndices[m]];
        sv = e.getValue();
        Lst<SV> outliers = sv.getList();
        if (outliers == null) {
          map = sv.getMap();
          if (map != null && (sv = map.get("outliers")) != null)
            outliers = sv.getList();
        }
        if (outliers != null) {
          boolean hasUnit = false;
          String key = e.getKey();
          SV svPath = SV.newS(key);
          boolean isRes = false;
          for (int j = outliers.size(); --j >= 0;) {
            SV out = outliers.get(j);
            map = out.getMap();
            sv = map.get("units");
            SV svv = map.get("value");
            float val = (svv == null ? 1 : SV.fValue(svv));
            Lst<SV> units = (val == 0 || sv == null || sv.tok == T.varray ? sv
                .getList() : sv.tok == T.string ? new Lst<SV>() : null);
            if (units != null) {
              if (sv.tok == T.string) {
                // optional string of unit ids
                String[] svl = PT.split(sv.asString(),",");
                for (int i = svl.length; --i >= 0;)
                  units.addLast(SV.newS(svl[i].trim()));
              }
              if (units.size() > 0) {
                BS bsAtoms = new BS();
                map.put("_atoms", SV.getVariable(bsAtoms));
                map.put("_path", svPath);
                hasUnit = true;
                list.addLast(out);
                for (int k = units.size(); --k >= 0;) {
                  boolean ret = catalogUnit(viewer, floats, units.get(k).asString(),
                      val, bsAtoms, modelAtomIndices, resMap, atomMap, modelMap);
                  if (ret)
                    map.put("_isres", SV.vT);
                  isRes |= ret;
                }
              }
            }
          }
          if (hasUnit) {
            for (int m = nModels; --m >= 0;)
              if (floats[m] != null) {
                retProperties.addLast(key);
                retProperties.addLast(floats[m]);
                retProperties.addLast(Integer.valueOf(m));
                retProperties.addLast(Boolean.valueOf(isRes));
              }
          }
        }
      }
      return retProperties;
    } catch (Exception e) {
      Logger.info(e + " while cataloging validations");
      return null;
    }
  }

  private SV getMainItem(Map<String, SV> data) {
    for (Entry<String, SV> e: data.entrySet()) {
      String key = e.getKey();
      if (!key.contains("metadata"))
        return e.getValue();
    }
    return null;
  }

  /**
   * We create a main list of mappings, where each mapping
   * has _atoms and _path
   * 
   * @param objAnn
   * @return  Lst of mappings
   */
  @Override
  public Lst<SV> initializeAnnotation(SV objAnn, int type, int modelIndex) {
    Map<String, SV> map = objAnn.getMap();
    SV _list = map.get("_list");
    if (_list != null)
      return _list.getList();
    String dataKey = getDataKey(type);
    // assume ONE top-level key
    SV main = getMainItem(map);
    map.put("_map",  main);
    boolean noSingles = true; // different for validation
    Map<String, SV> _cat = new Hashtable<String, SV>();
    map.put("_cat", SV.newV(T.hash, _cat));
    Lst<SV> list = new Lst<SV>();
    map.put("_list", _list = SV.newV(T.varray,  list));
    for (Entry<String, SV> e : main.getMap().entrySet()) {
      // first level: SCOP, InterPro, etc.
      String _dbName = e.getKey();
      SV _dbMap = e.getValue();
      _cat.putAll(_dbMap.getMap());
      for (Entry<String, SV> e2 : _dbMap.getMap().entrySet()) {
        // second level: ID
        String _domainName = e2.getKey();
        SV _domainMap = e2.getValue();
        SV _domainList = _domainMap.mapGet(dataKey);
        Lst<SV> _mapList = _domainList.getList();
        for (int i = _mapList.size(); --i >= 0;) {
          SV mapping = _mapList.get(i);
          list.addLast(mapping);
          Map<String, SV> mmap = mapping.getMap();
          SV _chain = mmap.get("chain_id");
          SV start = mmap.get("start");
          SV end = mmap.get("end");
          int res1 = 0;
          int res2 = 0;
          String rescode = "modelIndex=" + modelIndex + "&chain='" + _chain.value + "'";
          if (start != null && end != null) {
            res1 = start.getMap().get("residue_number").intValue;
            res2 = end.getMap().get("residue_number").intValue;
            rescode += "&seqid>=" + res1 + "&seqid<=" + res2;
          } else {
            res2 = 1;
            rescode += "&seqid>0";
          }
          SV _atoms = (noSingles && res1 >= res2 ? SV.getVariable(new BS()) : _cat.get(rescode));
          if (_atoms == null)
            _cat.put(rescode, _atoms = SV.newS(rescode));
          // note that using SV, we can MUTATE _atoms from String to Bitset
          // and _cat and all references will update, because it is just 
          // a pointer, not a copy.
          
          mmap.put("_atoms", _atoms);
          mmap.put("_path", SV.newS(_dbName + "." + _domainName));
          mmap.put("domain", _domainMap);
        }        
      }
    }
    return list;
  }

  /**
   * find annotations; allows for wild cards InterPro.* where .....
   * 
   * @param vwr
   * @param name
   * @param _list
   * @param key
   * @param bs
   */
  @SuppressWarnings("unchecked")
  private void findAnnotationAtoms(Viewer vwr, String name,
                                   Lst<SV> _list, String key, BS bs) {
    if (_list == null)
      return;
    System.out.println("Checking " + name + " for " + key);
    Object data = vwr.extractProperty(_list, "[" + key + "]", -1);
    Lst<SV> list = null;
    if (data instanceof Lst) {
      list = (Lst<SV>) data;
    } else  if (data instanceof SV) {
      list = ((SV) data).getList();
    }
    if (list == null)
      return;

    // go through all mappings
    for (int i = 0, n = list.size(); i < n; i++) {
      Object o = list.get(i);
      Map<String, SV> mapping = (o instanceof SV ? ((SV)o).getMap() : (Map<String, SV>) o);
      if (mapping == null)
        return;
      bs.or(setAnnotationAtoms(vwr, mapping, i));
    }
  }

  private BS setAnnotationAtoms(Viewer vwr, Map<String, SV> mapping, int i) {
    SV _atoms = mapping.get("_atoms");
    if (_atoms.tok != T.bitset) {
      BS bs2 = vwr.getAtomBitSet(_atoms.value);
      if (i >= 0)
        Logger.info("#" + (i + 1) + " found " + bs2.cardinality()
            + " atoms for " + _atoms.value);
      // mutate _atoms from String to BitSet!
      // all references are updated instantly!
      _atoms.tok = T.bitset;
      _atoms.value = bs2;
    }
    return (BS) _atoms.value;
  }

  //UnitIDs are based on http://rna.bgsu.edu/main/rna-3d-hub-help/unit-ids/
  //  
  //  Unit Identifier Specification
  //
  //  We describe the type and case sensitivity of each field in the list below. In addition, we list which item in the mmCIF the data for each field comes from. We also show several examples of the IDs and their interpretation at the end.
  //
  //  Unit ids can also be used to identify atoms. When identifying entire residues, the atom field is left blank.
  //
  //      PDB ID Code
  //          From PDBx/mmCIF item: _entry.id
  //          4 characters, case-insensitive
  //      Model Number
  //          From PDBx/mmCIF item: _atom_site.pdbx_PDB_model_num
  //          integer, range 1-99
  //      Chain ID
  //          From PDBx/mmCIF item: _atom_site.auth_asym_id
  //          1 character, case-sensitive
  //      Residue/Nucleotide/Component Identifier
  //          From PDBx/mmCIF item: _atom_site.label_comp_id
  //          1-3 characters, case-insensitive
  //      Residue/Nucleotide/Component Number
  //          From PDBx/mmCIF item: _atom_site.auth_seq_id
  //          integer, range: -999..9999 (there are negative residue numbers)
  //      Atom Name (Optional, default: blank)
  //          From PDBx/mmCIF item: _atom_site.label_atom_id
  //          0-4 characters, case-insensitive
  //          blank means all atoms
  //      Alternate ID (Optional, default: blank)
  //          From PDBx/mmCIF item: _atom_site.label_alt_id
  //          Default value: blank
  //          One of ['A', 'B', '0'], case-insensitive
  //      Insertion Code (Optional, default: blank)
  //          From PDBx/mmCIF item: _atom_site.pdbx_PDB_ins_code
  //          1 character, case-insensitive
  //      Symmetry Operation (Optional, default: 1_555)
  //          As defined in PDBx/mmCIF item: _pdbx_struct_oper_list.name
  //          5-6 characters, case-insensitive
  //          For viral icosahedral structures, use “P_” + model number instead of symmetry operators. For example, 1A34|1|A|VAL|88|||P_1
  //
  //  Examples
  //
  //      Chain A in 1ABC = “1ABC|1|A”
  //      Nucleotide U(10) chain B of 1ABC = “1ABC|1|B|U|10”
  //      Nucleotide U(15A) chain B, default symmetry operator = “1ABC|1|B|U|15|||A”
  //      Nucleotide C(25) chain D subject to symmetry operation 2_655 = “1ABC|1|D|C|25||||2_655”
  //
  //  Unit ids for entire residues can contain 4, 7, or 8 string separators (|).

  /**
   * Carried out for each unit
   * 
   * @param viewer
   * @param vals
   *        model-based array of float values for a given validation type
   * @param unitID
   * @param val
   * @param bsAtoms
   * @param modelAtomIndices
   * @param resMap
   * @param atomMap
   * @param modelMap
   *        TODO
   * 
   * @return true if this is residue-based validation (to be added to H atoms
   *         when pdbAddHydrogens is set
   */
  private boolean catalogUnit(Viewer viewer, float[][] vals, String unitID,
                              float val, BS bsAtoms, int[] modelAtomIndices,
                              Map<String, int[]> resMap,
                              Map<String, Integer> atomMap,
                              Map<String, Integer> modelMap

  ) {

    // (pdbid)|model|chain|RESNAME|resno|ATOMNAME|altcode|inscode|(symmetry)
    //   0       1     2      3      4      5        6       7       8

    // becomes

    // model_chainCode_resno_inscode
    // model_chainCode_resno_inscode_ATOMNAME_altcode
    //   

    String[] s = PT.split(unitID + "|||", "|");
    // must have at least model, chain, resname, and resno
    if (s.length < 8 || s[1].length() == 0 || s[2].length() == 0
        || s[3].length() == 0 || s[4].length() == 0)
      return false;
    String sm = (s[1].length() == 0 ? "1" : s[1]);
    int m = (modelMap == null ? PT.parseInt(sm) - 1 : -1);
    Integer im = (m >= 0 ? null : modelMap.get(sm));
    if (im != null)
      m = im.intValue();
    if (m >= modelAtomIndices.length)
      return false;
    String res = s[1] + "_" + viewer.getChainID(s[2]) + "_" + s[4] + "_"
        + s[7].toLowerCase();
    int i0 = modelAtomIndices[m];
    boolean isRes = (atomMap == null || s[5].length() == 0);
    if (isRes) {
      int[] a2 = resMap.get(res);
      if (a2 != null)
        for (int j = a2[1], j0 = a2[0]; --j >= j0;) {
          bsAtoms.set(i0 + j);
          vals[m][j] += Math.abs(val);
        }
    } else {
      if (s[5].charAt(0) == 'H')
        s[5] = getAttachedAtomForPDBH(s[3], s[5]);
      String atom = res + "_" + s[5] + "_" + s[6].toLowerCase();
      Integer ia = atomMap.get(atom);
      if (ia != null) {
        int j = ia.intValue();
        bsAtoms.set(i0 + j);
        vals[m][j] += Math.abs(val);
      }
    }
    return isRes;
  }
  
  private String fixKeyDSSR(String key) {
    String s = key.toLowerCase();
    // Check to see if we have already asked for pairs or the data type
    // does not have the "basePairs" key
    if (s.indexOf("pairs") < 0 && s.indexOf("kissingloops") < 0
        && s.indexOf("linkedby") < 0 && s.indexOf("multiplets") < 0
        && s.indexOf("singlestrand") < 0)
      key += ".basePairs";
    if (s.indexOf(".nt") < 0 && s.indexOf(".res") < 0
        && s.indexOf("[select res") < 0 && s.indexOf("[select nt") < 0)
      key += ".res*";
    return key;
  }

  /**
   * parsing a residue designation for tokens in DSSR.
   * 
   * @param vwr
   * @param res
   * @param lst
   * @param bs
   * @param htChains
   */
  private void getDSSRAtoms(Viewer vwr, String res, Lst<?> lst, BS bs,
                          Map<String, BS> htChains) {
    String[] tokens;
    if (lst == null) {
      tokens = PT
          .getTokens(PT.replaceAllCharacters(res.toString(), "=[,]", " "));
    } else if (lst.size() == 0) {
      return;
    } else {
      tokens = new String[lst.size()];
      for (int i = lst.size(); --i >= 0;) {
        Object o = lst.get(i);
        if (o instanceof SV)
          o = ((SV) o).value;
        if (o instanceof Lst<?>) {
          getDSSRAtoms(vwr, null, (Lst<?>) o, bs, htChains);
        } else {
          String s = (o instanceof SV ? ((SV) o).asString() : o.toString());
          tokens[i] = (s.startsWith("[") ? s.substring(s.indexOf("]") + 1) : s);
        }
      }
    }
    for (int j = tokens.length; --j >= 0;) {
      String t = tokens[j];
      if (t == null)
        continue;
      int pt = t.indexOf(":");
      if (pt < 0 || pt + 1 == t.length())
        continue;
      String chain = t.substring(pt + 1);
      BS bsChain = htChains.get(chain);
      try {
        if (bsChain == null)
          htChains.put(
              chain,
              bsChain = vwr.ms.getAtoms(T.spec_chain,
                  Integer.valueOf(vwr.getChainID(chain))));
        BS bsRes = vwr.ms
            .getAtoms(T.resno, Integer.valueOf(t.substring(0, pt)));
        bsRes.and(bsChain);
        bs.or(bsRes);
      } catch (Exception e) {
        // ignore
      }
    }
  }

///////////////////// general post-load processing ////////////////

  @SuppressWarnings("unchecked")
  @Override
  /**
   * 
   * Retrieve a set of atoms using vwr.extractProperty with 
   * and for other annotations
   * 
   */
  public BS getAtomBits(Viewer vwr, String key, Object dbObj,
                        Map<String, Object> annotationCache, int type, int modelIndex, BS bsModel) {
    if (dbObj == null)
      return new BS();
    boolean isDomains = (type == T.domains);
    boolean isValidation = (type == T.validation);
    boolean isDSSR = (type == T.dssr);
    boolean doCache = !key.contains("NOCACHE");
    if (!doCache) {
      key = PT.rep(key, "NOCACHE","").trim();
    }
    if (!isDomains && !isValidation)
      key = fixKeyDSSR(key);
    BS bs = (doCache ? (BS) annotationCache.get(key) : null);
    if (bs != null)
      return bs;
    bs = new BS();
    if (doCache)
      annotationCache.put(key, bs);
    try {
      if (isDSSR) {
        Object data = vwr.extractProperty(dbObj, key, -1);
        if (data instanceof Lst<?>) {
          Map<String, BS> htChains = new Hashtable<String, BS>();
          getDSSRAtoms(vwr, null, (Lst<SV>) data, bs, htChains);
        }
        return bs;
      }      
      Lst<SV> list = initializeAnnotation((SV) dbObj, type, modelIndex);
      // select within(domains,"InterPro where identifier like '*-like'")
      int pt = key.toLowerCase().indexOf(" where ");
      String path = PT.rep((pt < 0 ? key : key.substring(0, pt)), " ","");
      String newKey = (pt < 0 ? "" : key.substring(pt + 7).trim());
      if (path.indexOf(".") < 0) {
        path = " _path like '" + path + "*'";
      } else {
        path = " _path='"+path+"'";        
      }
      newKey = "select * where " + (pt < 0 ? path : "(" + newKey + ") and (" + path + ")"); 
      Logger.info("looking for " + newKey);
      // this is either the right map or we have a wildcard.
        findAnnotationAtoms(vwr, path, list, newKey, bs);
      bs.and(bsModel);
    } catch (Exception e) {
      System.out.println(e.toString() + " in AnnotationParser");
      bs.clearAll();
    }
    return bs;
  }

  /////////// EBI validation /////////////
  
  /**
   * Get all validation values corresponding to a specific validation type. 
   * Used by label %[validation.clashes]
   * 
   * @param vwr
   * @param type  e.g. "clashes" 
   * @param atom  
   * @return a list of Float values associated with this atom and this type of validation
   */
  @Override
  public Lst<Float> getAtomValidation(Viewer vwr, String type, Atom atom) {
    int i = 0;
    int n = 0;
    
    Lst<Float> l = null;
    Map<String, SV>map = null;
    Lst<SV> list = null;
    try {
      int ia = atom.i;
      l = new Lst<Float>();
      list = ((SV) vwr.ms.getModelAuxiliaryInfo(atom.mi).get("validation")).getMap().get("_list").getList();
      
      for (i = 0, n = list.size(); i < n; i++) {
        map = list.get(i).getMap();
        if (map.get("_path").value.equals(type) && ((BS) map.get("_atoms").value).get(ia)) {
          SV v = map.get("value");
          l.addLast(v.tok == T.decimal ? (Float) v.value : Float.valueOf(v.asFloat())); 
        }
      }
      return l;
    } catch (Exception e) {
      return null;
    }
  }


  /**
   * 
   * Get a string report of annotation data
   * @param a an annotation structure wrapped as a script variable
   * @param match can contain "mappings" to get those specifically
   * 
   * @return tab-separated line-based listing
   */
  @Override
  public String getAnnotationInfo(Viewer vwr, SV a, String match, int type, int modelIndex) {
    SB sb = new SB();
    if ("".equals(match))
      match = null;
    boolean isDetail = (match != null && (match.equals("all") || match.endsWith(" all")));
    if (isDetail) {
      Lst<SV> _list = initializeAnnotation(a, type, modelIndex);
      for (int i = _list.size(); --i >= 0;)
        setAnnotationAtoms(vwr,  _list.get(i).getMap(), -1);
      match = match.substring(0, Math.max(0, match.length() - 4)).trim();
    }
    if ("".equals(match))
      match = null;
    if (type == T.validation && !isDetail && match == null)
      return a.getMap().get("_note").asString();
    boolean isMappingOnly = (match != null && match.indexOf(".") >= 0 && match.indexOf(".*") < 0);
    match = PT.rep(match, "*", "");
    try{
    getAnnotationKVPairs(a, match, "", sb, "", isDetail, isMappingOnly, type);
    } catch (Exception e) {
      /**
       * @j2sNative
       * 
       * System.out.println(e);
       */
      {
      System.out.println(e.getStackTrace());
      }
    }
    return sb.toString();
  }

  private static Map<String, String>pdbAtomForH;
  
  /**
   * Finds the standard attached heavy atom for a PDB H  atom;
   * used in EBI clash validation.
   * 
   * @param group3
   * @param name
   * @return name of attached atom or hName
   */
  public String getAttachedAtomForPDBH(String group3, String name) {
    if (name.charAt(0) == 'H') {
      if (pdbAtomForH == null) {
        pdbAtomForH = new Hashtable<String, String>();
        assignPDBH(
            "",
            "N H H1 H2 H3 CB HB2 HB3 CD HD2 HD3 CG HG2 HG3 C2' H2'' H2' C5' H5'' H5' OXT HXT");
        for (int i = JC.pdbBondInfo.length; --i >= 1;) {
          assignPDBH(JC.group3Names[i], JC.pdbBondInfo[i]);
        }
      }
      String a = pdbAtomForH.get(name);
      if (a == null)
        a = pdbAtomForH.get(group3 + name);
      if (a != null)
        return a;
    }
    return name;
  }
  
  private void assignPDBH(String group3, String sNames) {
    String[] names = PT.getTokens(PT.rep(sNames, "@", " "));
    String a = null;
    for (int i = 0, n = names.length; i < n; i++) {
      String s = names[i];
      if (s.charAt(0) != 'H') {
        // just assigning attached atom
        a = s;
        continue;
      }
      // this is an H
      s = group3 + s;
      if (s.indexOf("?") >= 0) {
        // CH3 groups
        s = s.substring(0, s.length() - 1);
        pdbAtomForH.put(s + "1", a);
        pdbAtomForH.put(s + "2", a);
        pdbAtomForH.put(s + "3", a);
      } else {
        pdbAtomForH.put(s, a);
      }
    }
  }

  /**
   * Adjusts _atoms bitset to account for added hydrogen atoms.
   * A margin of 20 allows for 20 added H atoms per group
   * 
   */
  @Override
  public void fixAtoms(int modelIndex, SV dbObj, BS bsAddedMask, int type, int margin) {
    Lst<SV> _list = initializeAnnotation(dbObj, type, modelIndex);
    for (int i = _list.size(); --i >= 0;) {
      Map<String, SV> m = _list.get(i).getMap();
      SV _atoms = m.get("_atoms");
      if (_atoms != null && _atoms.tok == T.bitset)
        BSUtil.shiftBits((BS) _atoms.value, bsAddedMask, _list.get(i).getMap().containsKey("_isres"), ((BS) _atoms.value).length() + margin);
    }
  }


}
