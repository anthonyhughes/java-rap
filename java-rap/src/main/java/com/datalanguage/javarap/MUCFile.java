/*
 JavaRAP: a freely-available JAVA anaphora resolution implementation
 of the classic Lappin and Leass (1994) paper:

 An Algorithm for Pronominal Anaphora Resolution.
 Computational Linguistics, 20(4), pp. 535-561.

 Copyright (C) 2005,2006  Long Qiu

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.datalanguage.javarap;

import com.datalanguage.javarap.model.CorreferencialPair;
import com.datalanguage.javarap.utils.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Stack;
import java.util.Vector;

/**
 * <p>Title: Anaphora Resolution</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author Qiu Long
 * @version 1.0
 * @history
 * Dec 15, 2006
 * Problem: MUCNP.minPact is "NULL" when an antecedent has no 'MIN' defined in the file annotated with MUC
 * co-reference annotations. This "NULL" was mistakenly required to be part of a proposed antecedent
 * for it to be correct.
 * Correction: MUCNP.minPact is checked only when it's not "NULL".
 * Thanks to Thomas @utexas
 *
 */

/**
 * Sep 2
 * If a MUCNP has no children, use it's element's annotation to annotate its string directly, otherwise,
 * annotate the "min" value.
 * Check
 * 1)The value of attribute TYPE is always IDENT, if there is one? (yes)
 * 2)Would "min" appear in the text field of other annotatables (e.g. <COREF min="a">b <COREF>c a</COREF></COREF>, which
 * causes nested annotations in the final output? (Manually checked, seems not.)
 */
public class MUCFile {
    Document document;
    Element rootElem;
    Vector npVec = new Vector();
    HashMap npIdTb = new HashMap();//where nps are stored and indexed by their id
    HashMap npContentTb = new HashMap();//where nps are stord and indexed by their content (text span with spaces removed)
    Stack npContentSk = new Stack();
    //CoreferenceChian: where coreference chains are stored,
    // indexed by the id of the first np in the chain and the record is a vector of all the ids in the chain
    HashMap coreferenceChain = new HashMap();

    public MUCFile(String fileName) {
        loadFromFile(fileName);
    }


    public void displayNP() {
        rootElem = document.getDocumentElement();
        printDescendents(rootElem, "");
    }


    public void loadFromFile(String fileName) {
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
        //factory.setValidating(true);
        //factory.setNamespaceAware(true);
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(fileName);
        } catch (SAXException sxe) {
            // Error generated during parsing)
            Exception x = sxe;
            if (sxe.getException() != null)
                x = sxe.getException();
            x.printStackTrace();
            System.exit(-1);
        } catch (ParserConfigurationException pce) {
            // Parser with specified options can't be built
            pce.printStackTrace();

        } catch (IOException ioe) {
            // I/O error
            ioe.printStackTrace();
        }

    }


    public static void printDescendents(Element parent, String indent) {
        indent = indent + "   ";
        NodeList nodeList = parent.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); ++i) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                System.out.println(indent + "<" + element.getTagName() + "> " + Util.removeTag(element.toString()));
                printDescendents(element, indent);
            }
        }
    }

    public void buildNPSet() {
        try {
            rootElem = document.getDocumentElement();

            /*if (rootElem.getTagName().equals("COREF")) {
              MUCNP mucnp = new MUCNP(rootElem);
              npVec.add(mucnp);
              npIdTb.put(mucnp.corefID, mucnp);
              npContentSk.push(mucnp);
            }*/
        } catch (Exception ex) {
            ex.printStackTrace();
            //System.exit(0);
        }

        buildNPSet(rootElem);
    }

    private void buildNPSet(Element parent) {

        NodeList nodeList = parent.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); ++i) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (element.getTagName().equals("COREF")) {
                    MUCNP mucnp = new MUCNP(element);
                    npVec.add(mucnp);
                    npIdTb.put(mucnp.corefID, mucnp);

                    /** @todo delete or address Sep 2, 2004 */
                    if (npContentTb.put(mucnp.completePact.toLowerCase(), mucnp) != null) {
                        //System.err.println("\t\t"+mucnp.completePact.toLowerCase());
                        //System.exit(-1);
                    }
                    ;

                    npContentSk.push(mucnp);

                    if (mucnp.ref.length() == 0) {
                        //a np not refering to any existing np
                        Vector chains = new Vector();
                        mucnp.setChainID(mucnp.corefID);
                        chains.add(mucnp);
                        this.coreferenceChain.put(mucnp.getChainID(), chains);
                    } else {
                        MUCNP referee = (MUCNP) npIdTb.get(mucnp.ref);
                        mucnp.setChainID(referee.getChainID());
                        Vector chains = (Vector) (coreferenceChain.get(mucnp.getChainID()));
                        chains.add(mucnp);
                    }


                }
                buildNPSet(element);
            }
        }
    }

    public Vector getCoreferenceChain(String chainID) {
        return (Vector) (coreferenceChain.get(chainID));
    }

    public Vector getNPVec() {
        return npVec;
    }

    private HashMap getNPTable() {
        return npIdTb;
    }

    /**
     * @param input Vector of CorreferencialPair from resolver
     */
    public String evalutaion(Vector input) {
        int correct = 0;
        int pleoCorrect = 0;
        int incorrect = 0;
        int plenoWrong = 0;
        int missed = 0;


        for (int i = 0; i < input.size(); i++) {
            CorreferencialPair pair = (CorreferencialPair) input.get(i);

            //antecedent
            String resolverAns = "null";
            String resolverAnsOrg = "null";
            if (pair.getReferee() != null) {
                resolverAnsOrg = pair.getReferee().getText();
                resolverAns = Util.removeSpace(resolverAnsOrg).toLowerCase();

                //remove the ending ",", which affects the comparison with the gold standard annotation
                if (resolverAns.endsWith(",")) {
                    resolverAns = resolverAns.substring(0, resolverAns.length() - 1);
                }
            }
            //anaphor
            String resolverAna = Util.removeSpace(pair.getReferer().getText()).toLowerCase();

            MUCNP key = null;

            try {

                if (!npContentTb.containsKey(resolverAna.toLowerCase())) {
                    missed++;
                    Util.showMessage("\t\t******Missed:    " + resolverAna, System.getProperty("EvaluationVerbose").equals("true"));
                    continue;
                }

                MUCNP org = (MUCNP) npContentSk.remove(0);
                while (!org.completePact.equalsIgnoreCase(resolverAna)) {
                    //skip non-third person pronouns
                    org = (MUCNP) npContentSk.remove(0);
                }

                key = (MUCNP) npIdTb.get(org.ref);
                if (key == null) {
                    if (!resolverAna.equals("it")) {
                        missed++;
                        Util.showMessage("\t\t******Missed:    " + resolverAna, System.getProperty("EvaluationVerbose").equals("true"));
                    } else if (resolverAns.equals("null")) {
                        pleoCorrect++;
                    } else {
                        plenoWrong++;
                        Util.showMessage("\t\t******PleoWrong    " + " " + pair.getReferer().getSentenceIdx() + "," + pair.getReferer().getOffset() + " " + resolverAna + " --> " + resolverAnsOrg, System.getProperty("EvaluationVerbose").equals("true"));
                    }
                    continue;
                }
            } catch (Exception ex) {

                System.out.println(resolverAna);
                System.out.println(npContentSk);
                ex.printStackTrace();
                return input.size() + "\t"
                        + correct
                        + "\t" + pleoCorrect
                        + "\t" + incorrect
                        + "\t" + plenoWrong
                        + "\t" + missed;
            }

            //System.out.println(key.complete +" >"+resolverAns+" > "+key.min);
//      if (key.completePact.toLowerCase().indexOf(resolverAns) >= 0 && resolverAns.indexOf(key.minPact.toLowerCase()) >= 0) {Changed to below on Dec 15, 2006
            if (key.completePact.toLowerCase().indexOf(resolverAns) >= 0 && (key.minPact.equals("NULL") || resolverAns.indexOf(key.minPact.toLowerCase()) >= 0)) {
                correct++; //System.out.println("Right");
            } else {
                //Try the np that the key is referring to:
                //A<--B<--C:
                //C might refer to A, according to resolver but it refers to B according to the annotation.

                MUCNP previousKey = (MUCNP) npIdTb.get(key.ref);
//        if (previousKey !=null && previousKey.completePact.toLowerCase().indexOf(resolverAns) >= 0 && resolverAns.indexOf(previousKey.minPact.toLowerCase()) >= 0) {Changed to below on Dec 15, 2006
                if (previousKey != null && previousKey.completePact.toLowerCase().indexOf(resolverAns) >= 0 && (previousKey.minPact.equals("NULL") || resolverAns.indexOf(previousKey.minPact.toLowerCase()) >= 0)) {
                    correct++; //System.out.println("Right");
                } else {

                    incorrect++; //System.out.println("Wrong");
                    Util.showMessage("\t\t" + key.completeOrg + " >" + " (" +
                            pair.getReferer().getSentenceIdx() + "," +
                            pair.getReferer().getOffset() + " " + resolverAna + ")" +
                            resolverAnsOrg + " > " + key.minOrg, System.getProperty("EvaluationVerbose").equals("true"));
                }
            }
        }
        return input.size() + "\t"
                + correct
                + "\t" + pleoCorrect
                + "\t" + incorrect
                + "\t" + plenoWrong
                + "\t" + missed;
    }

    private void test() {
        for (int i = 0; i < npVec.size(); i++) {
            MUCNP mnp = (MUCNP) npVec.get(i);
            System.out.println(mnp.toString());
        }
    }


    public static void main(String[] args) {
        MUCFile mucF = new MUCFile("$HOME/MUC_Corref/muc6/train/8701230009");
        mucF.buildNPSet();
        mucF.test();
    }

}

class MUCNP {
    Element element = null;
    String coreferenceChainID;
    String corefID;//the id of this np
    String ref;//the id of the np that this np is refering to
    String type;
    protected String completeOrg = "NULL";//the entire text span of this np, with space removed for comparison's sake, orginally
    protected String minOrg = "NULL";//the minimal text span semantically eqivalent to this np, orginally
    //remove the spaces to be insensitive to the differences in spaces during comparing
    String completePact = "NULL";//the entire text span of this np, with space removed for comparison's sake, space removed
    String minPact = "NULL";//the minimal text span semantically eqivalent to this np, space removed

    public MUCNP(String corefID, String ref, String compStr, String minStr) {
        this.corefID = corefID;
        this.ref = ref;
        this.completeOrg = compStr;
        this.completePact = Util.removeSpace(completeOrg);
        this.minOrg = minStr.split("\\|")[0];
        this.minPact = Util.removeSpace(minOrg);
    }

    public MUCNP(Element element) {
        this.element = element;
        this.corefID = element.getAttribute("ID");
        this.ref = element.getAttribute("REF");
        this.completeOrg = Util.removeTag(element.toString());
        this.completePact = Util.removeSpace(completeOrg);
        this.minOrg = element.getAttribute("MIN").length() == 0 ? "NULL" : Util.removeTag(element.getAttribute("MIN")).split("\\|")[0];
        this.minPact = Util.removeSpace(minOrg);
        this.type = element.getAttribute("TYPE");

        /** @todo remove/delete in release version */
        if (type.length() > 0 && !type.equals("IDENT")) {
            System.err.println("TYPE has other values.");
            System.err.println("*" + type + "*" + this.toString());
            System.exit(-1);
        }
    }

    public void setChainID(String id) {
        coreferenceChainID = id;
    }

    public String getChainID() {
        return coreferenceChainID;
    }

    public String getText(boolean packed) {
        if (packed) {
            return completePact;
        } else {
            return completeOrg;
        }
    }

    /**
     * Checks whether there is nested co-referencible within.
     *
     * @return true if there is one.
     */
    public boolean hasChild() {
        NodeList nList = element.getChildNodes();
        for (int j = 0; j < nList.getLength(); j++) {
            Node e = nList.item(j);
            if (e.getNodeName().equals("COREF")) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        minOrg = minOrg.length() == 0 ? "NULL" : minOrg;
        completeOrg = completeOrg.length() == 0 ? "NULL" : completeOrg;
        return corefID + "\t\t" + ref + "\t\t" + completeOrg + "\t\t<" + minOrg + ">";
    }

    public String toStringTagForm() {
        String Did = " ID=\"" + this.corefID + "\" ";
        String Dtype = " TYPE=\"" + this.type + "\" ";
        String Dref = " REF=\"" + this.ref + "\"";
        String Dmin = " MIN=\"" + this.minOrg + "\" ";
        return " <COREF " + Did + Dtype + Dref + Dmin + ">" + getText(false) + "</COREF> ";
    }
}
