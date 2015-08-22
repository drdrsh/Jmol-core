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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javajs.util.SB;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;

import org.jmol.c.CBK;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

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

  // local settings of the dialog type
  
  protected int dialogMode;
  private static final int DIALOG_CONFIG = 0;
  private static final int DIALOG_MODEL = 10;
  private static final int DIALOG_RUN = 20;
  private static final int DIALOG_VIEW = 30;
  private static final int DIALOG_SEARCH = 40;
  private final String helpConfig = "                       NBOPro v.6\n"
      +"The NBOPro6 program suite combines four distinct modules:\n"
      +" (1) NBOModel (molecular design editor)\n"
      +" (2) NBORun (GenNBO)\n"
      +" (3) NBOView (orbital viewer)\n"
      +" (4) NBOSearch (data miner)\n"
      +"The modules have distinct but interrelated NBO capabilities.\n"
      +"Each module has its own commands, syntax, and HELP screens.\n"
      +"\n"
      +"To get started, you must first locate your NBOServe executable"
      +"in the CONFIG menu and press connect.  Once successfully connected"
      +"the other modules will be available and you will not have to revisit" +
      "the CONFIG menu as long as the location of the NBOServe executable " +
      "is not changed.  All modules support the raw"
      +"keyboard input from NBOPro6 by typing commands into the NBO Input line";
  private final String helpModel ="NBOModel COMMAND SYNTAX\n"
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
  private final String runHelp = "                  NBORun: GENERAL PROGRAM USAGE\n"
      +"By default, the NBORun module performs NBO analysis of the"
      +"selected wavefunction archive (.47) file 'JOB.47' and"
      +"writes the output to a corresponding 'JOB.NBO' file (as"
      +"though the 'GenNBO < JOB.47 > JOB.NBO' command were given)."
      +"[Alternatively, NBORun can calculate the wavefunction with"
      +"a chosen ESS program, specified by a corresponding 'ESS.BAT'"
      +"batch file and 'JOB.ESS' input file (as though the command"
      +"'ESS JOB.ESS' were given). This allows you to perform the"
      +"wavefunction calculation and NBO analysis in a single step.]\n"
      +" \n"
      +"You will be prompted with the list of JOB.47 files as found"
      +"in the directory last used by Jmol-NBO. After a particular JOB is selected,"
      +"the program will display the current list of $NBO keyword"
      +"options and allow you to insert additional options, if desired."
      +"When notified that the job is finished processing the 'JOB.NBO' output becomes available for"
      +"NBOView orbital plotting (if the PLOT keyword was included)"
      +"or NBOSearch data-mining.\n";
  private final String viewHelp = "                  NBOView: GENERAL PROGRAM USAGE\n"
      +"NBOView program usage begins with selection of a JOB from "
      +"available PLOT (.31-.41, .46) files on the current directory. "
      +"Use the NBORun module to generate PLOT output files from "
      +"any available archive (.47) file in the directory.  \n\nAfter "
      +"selecting a basis set and orbital, press GO to view either "
      +"a 1D profile or a 2D contour, with the vector/plane defined by the atoms highlighted on the model.  "
      +"Selected atoms can be changed by clicking on the model.  "
      +"To view a 3D bitmap image, select one of the 9 items in storage "
      +"and press the '3D' button under display, raytracing may take some "
      +"time so you may need to be patient "
      +"The program presents many possible options to alter details "
      +"of the view/camera model. If uncertain, the default values shown "
      +"in the settings will be used.";
  private final String searchHelp = "             NBOSearch: COMMAND SYNTAX AND PROGRAM OVERVIEW\n"
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
  
  JDialog help;

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
    this.setSize(new Dimension(685, f.getHeight()));
    nboService.nboDialog = this;
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        //TODO
        close();
      }
    });
    topPanel = null;
    (modelButton = moduleButton("nbomodel_logo.gif",'m')).setEnabled(nboService.restartIfNecessary());
    (runButton = moduleButton("nborun_logo.gif",'r')).setEnabled(nboService.restartIfNecessary());
    (viewButton = moduleButton("nboview_logo.gif",'v')).setEnabled(nboService.restartIfNecessary());
    (searchButton = moduleButton("nbosearch_logo.gif",'s')).setEnabled(nboService.restartIfNecessary());
    (browse = new JButton("Browse")).setEnabled(nboService.restartIfNecessary());
    browse.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent arg0) {
        switch(dialogMode){
        case DIALOG_MODEL:showWorkpathDialogM(usePath,"47, adf, cfi, gau, gms, jag, " +
        		"log, mp, nw, orc, pqs, qc, vfi");
        break;
        case DIALOG_RUN:showWorkpathDialogR(workingPath);
        break;
        case DIALOG_VIEW:showWorkpathDialogV(workingPath);
        break;
        case DIALOG_SEARCH:showWorkpathDialogS(workingPath);
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
    rawInput = new JTextField();
    rawInput.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        rawInput();
      }
    });
    statusPanel = buildStatusPanel();
    if (haveService)
      connectPressed();
  }
  
  private JButton moduleButton(String path, final char mode){
    JButton button = new JButton();
    button.setBorderPainted(false);
    button.setBorder(null);
    button.setMargin(new Insets(0, 0, 0, 0));
    button.setContentAreaFilled(false);
    java.net.URL imgURL = getClass().getResource(path);
    ImageIcon icon2 = new ImageIcon(imgURL);
    button.setIcon(icon2);
    button.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        openPanel(mode);
      }
    });
    
    return button;
//    button.setRolloverIcon(myIcon2);
//    button.setPressedIcon(myIcon3);
//    button.setDisabledIcon(myIcon4);
  }
  protected void showHelp(String key){
    if(help!=null)
      if(help.isVisible())
        help.setVisible(false);
    help = new JDialog(this,"NBO Help");
    JTextPane p = new JTextPane();
    p.setEditable(false);
    p.setFont(new Font("Arial",Font.PLAIN,16));
    JScrollPane sp = new JScrollPane();
    sp.getViewport().add(p);
    help.add(sp);
    help.setSize(new Dimension(400,400));
    switch(dialogMode){
    case DIALOG_CONFIG:
      p.setText(helpConfig);
    break;
    case DIALOG_MODEL:
      if(key==null)
        if(action.getSelectedIndex()==0)key = "";
        else key = action.getSelectedItem().toString().split(" ")[0].toLowerCase();
      if (key.equals("")){
        if((workPathLabel.hasFocus()||use.hasFocus())&&use.getSelectedIndex()==0) 
          p.setText(showHelp);
        else if(savePathLabel.hasFocus()||saveBox.hasFocus())
          p.setText(saveHelp);
        else
          p.setText(helpModel+"\n\n"+showHelp+"\n"+useHelp+"\n"+symHelp);
        break;
      }else if(key.equals("alter")){
        p.setText(alterHelp);
        break;
      }else if(key.equals("clip")){
        p.setText(clipHelp);
        break;
      }else if(key.equals("fuse")){
        p.setText(fuseHelp);
        break;
      }else if(key.equals("link")){
        p.setText(linkHelp);
        break;
      }else if(key.equals("mutate")){
        p.setText(mutateHelp);
        break;
      }else if(key.equals("rebond")){
        p.setText(rebondHelp);
        break;
      }else if(key.equals("switch")){
        p.setText(switchHelp);
        break;
      }else if(key.equals("twist")){
        p.setText(twistHelp);
        break;
      }else if(key.equals("unify")){
        p.setText(unifyHelp);
        break;
      }else if(key.equals("3chb")){
        p.setText(chbHelp);
        break;
      }else if(key.equals("save")){
        p.setText(saveHelp);
        break;
      }else if(key.equals("use")){
        p.setText(useHelp);
        break;
      }else if(key.contains("sym")){
        p.setText(symHelp);
        break;
      }else{
        appendModelOutPanel("Unkown command type");
        return;
      }
    case DIALOG_RUN:
      p.setText(runHelp);
      break;
    case DIALOG_VIEW:
      p.setText(viewHelp);
      break;
    case DIALOG_SEARCH:
      p.setText(searchHelp);
      break;
    }
    p.setCaretPosition(0);
    centerDialog(help);
    help.setVisible(true);
  }
  
  protected void rawInput(){
    String cmd = rawInput.getText().trim();
    cmd = cmd.toUpperCase();
    switch(dialogMode){
    case DIALOG_MODEL:
      if(cmd.contains("HELP")){
        if(cmd.split(" ").length>1)
          showHelp(cmd.split(" ")[1].toLowerCase());
        else showHelp("");
        rawInput.setText("");
        return;
      }
      SB sb = new SB();
      appendToFile("CMD " + rawInput.getText(), sb);
      appendModelOutPanel(rawInput.getText());
      modelCmd(sb);
      break;
    case DIALOG_VIEW:
      if(!isJmolNBO())
        break;
      if(cmd.startsWith("BAS"))
        try{
          basis.setSelectedItem(cmd.split(" ")[1]);
          appendModelOutPanel("Basis changed:\n  "+cmd.split(" ")[1]);
        }catch(Exception e){
          appendModelOutPanel("NBO View can't do that");
        }
      else if(cmd.startsWith("CON")){
        try{
          int i = Integer.parseInt(cmd.split(" ")[1]);
          list.setSelectedIndex(i-1);
          oneD=false;
          goViewClicked();
        }catch(Exception e){
          appendModelOutPanel("NBO View can't do that");
        }
      }else if(cmd.startsWith("PR")){
        try{
          int i = Integer.parseInt(cmd.split(" ")[1]);
          list.setSelectedIndex(i-1);
          oneD=true;
          goViewClicked();
        }catch(Exception e){
          appendModelOutPanel("NBO View can't do that");
        }
      }else if(cmd.startsWith("VIEW")){
        try{
          int i = Integer.parseInt(cmd.split(" ")[1]);
          dList.setSelectedIndex(i-1);
          view3D();
        }catch(Exception e){
          appendModelOutPanel("NBO View can't do that");
        }
      }
      break;
    case DIALOG_SEARCH:
      if(cmd.startsWith("O ")){
        try{
          int i = Integer.parseInt(cmd.split(" ")[1]);
          orb.setSelectedIndex(i-1);
        }catch(Exception e){
          appendModelOutPanel("Invalid command");
        }
      }else if(cmd.startsWith("A ")){
        try{
          int i = Integer.parseInt(cmd.split(" ")[1]);
          at1.setSelectedIndex(i-1);
        }catch(Exception e){
          appendModelOutPanel("Invalid command");
        }
      }
      break;
    }
    rawInput.setText("");
  }

  protected void close() {
    this.dispose();
    nboService.closeProcess();
    nboResetV();
    nboService.runScriptQueued("mo delete; nbo delete; select off");
  }

  public void openPanel(char type) {
    if(dialogMode!=0){
      switch(dialogMode){
      case DIALOG_MODEL: 
        modelButton.setEnabled(true);
        break;
      case DIALOG_RUN: 
        runButton.setEnabled(true);
        break;
      case DIALOG_VIEW: 
        viewButton.setEnabled(true);
        nboService.runScriptQueued("mo delete; nbo delete; select off");
        break;
      case DIALOG_SEARCH: 
        searchButton.setEnabled(true);
        nboService.runScriptQueued("mo delete; nbo delete; select off");
        break;
      }
    }
    if(icon!=null)
      topPanel.remove(icon);
    isJmolNBO = isJmolNBO();
    ImageIcon icon2 = null;
    switch (type) {
    case 'c':
      // config
      java.net.URL imgURL = getClass().getResource("nbomodel_logo.gif");
      icon2 = new ImageIcon(imgURL);
      dialogMode = DIALOG_CONFIG;
      this.repaint();
      this.revalidate();
      buildMain(this.getContentPane());
      break;
    case 'm':
      dialogMode = DIALOG_MODEL;
      buildModel(this.getContentPane());
      modelButton.setEnabled(false);
      imgURL = getClass().getResource("nbomodel_logo.gif");
      icon2 = new ImageIcon(imgURL);
      if (vwr.ms.ac != 0 && !isJmolNBO) {
        loadModel();
        enableComps();
      }
      break;
    case 'r':
      runButton.setEnabled(false);
      dialogMode = DIALOG_RUN;
      buildRun(this.getContentPane());
      imgURL = getClass().getResource("nborun_logo.gif");
      icon2 = new ImageIcon(imgURL);
      break;
    case 'v':
      viewButton.setEnabled(false);
      dialogMode = DIALOG_VIEW;
      buildView(this.getContentPane());
      imgURL = getClass().getResource("nboview_logo.gif");
      icon2 = new ImageIcon(imgURL);
      break;
    case 's':
      searchButton.setEnabled(false);
      dialogMode = DIALOG_SEARCH;
      buildSearch(this.getContentPane());
      imgURL = getClass().getResource("nbosearch_logo.gif");
      icon2 = new ImageIcon(imgURL);
      break;
    }
    topPanel.add(icon=new JLabel(icon2));
    setComponents(this);
    this.repaint();
    this.revalidate();
    centerDialog(this);
    setVisible(true);

  }

  /**
   * Callback from Jmol Viewer indicating user actions
   * 
   * @param type
   * @param data
   */
  @SuppressWarnings("incomplete-switch")
  public void notifyCallback(CBK type, Object[] data) {
    Logger.debug("NBODialog.notifyCallback " + type);
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
        notifyCallbackModel(atomno);
        break;
      case DIALOG_VIEW:
        notifyCallbackView(atomno);
        break;
      case DIALOG_SEARCH:
        notifyCallbackSearch(atomIndex);
        break;
      }
    }
  }

}
