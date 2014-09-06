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

import com.datalanguage.javarap.charniak.AnnotatedText;
import com.datalanguage.javarap.model.TagWord;
import com.datalanguage.javarap.utils.Util;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Enumeration;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Title: Anaphora Resolution</p>
 * <p>Description: Merger combines information from both MUC 6 file and corresponding parse.
 * A collaboration with Shiren. Has nothing to do with the rest of the anaphoraresolution parts.</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: </p>
 *
 * @author Qiu Long
 * @version 1.0 Sep. 2
 *          Have a list of NPs (from annotatedText) and a list of annotatables (from MUCFile), how to combine/merge them?
 */

public class Merger {
    final String fileName; //the name of muc file, possiblly with annotation (co-ref)

    //the content in the file, without modifications
    StringBuffer orgContent;
    //filtered and reformated output of parser
    String parseOut = new String();
    AnnotatedText parse = null; //the parse of the text
    MUCFile mucFile; //the structured muc file

    public Merger(String f) {
        fileName = f;
        parse = parseIt(fileName);
        mucFile = new MUCFile(fileName);
        mucFile.buildNPSet();
        //   merge();
    }

    public String merge() {
        /*Stragety: */
        /*Problematic cases
        its unions: its is nested and by convention "its unions" is neglected.
        */
        drawScratch();
        String rlt = replaceAnnotatables(mucFile.npContentSk);
        return rlt;
    }


    /**
     * This method transforms the parse into a list, ready for muc6 annotatables to find their place and continue with substitution
     */
    private void drawScratch() {
        DefaultMutableTreeNode root = parse.getTree();
        DefaultMutableTreeNode aNPNode = null; //a NP previously got
        //this part gets np at non-leave level and other component at leaf level
        for (Enumeration enu = root.preorderEnumeration(); enu.hasMoreElements(); ) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) enu.nextElement();
            //if(!node.isLeaf()){
            //  continue;
            //}
            Object obj = node.getUserObject();
            if (obj == null) {
                continue;
            }
            try {
                TagWord tw = (TagWord) obj;
                //gets 1)NP at non-leaf level, yet not too far from the leaves, and
                //     2)other leaves
                if (tw.getTag().equals("NP") && node.getDepth() <= 1) { //check if it's the correct np (not too gaint), constrained to one level higher than leaf
                    aNPNode = node;
                    //System.out.println(" <" + tw.getTag() + "> " + tw.getContent() + " </" + tw.getTag() + "> ");
                    parseOut += "\n <" + tw.getTag() + "> " + tw.getContent().replaceAll(" ", " ") + " </" +
                            tw.getTag() + "> ";
                } else if (node.isLeaf()) {
                    if (!node.isNodeAncestor(aNPNode)) { //a leaf not belong to a NP
                        //System.out.println(" <" + tw.getTag() + "> " + tw.getContent() +
                        //                  " </" + tw.getTag() + "> ");
                        parseOut += "\n <" + tw.getTag() + "> " +
                                tw.getContent().replaceAll(" ", "") + " </" +
                                tw.getTag() + "> ";
                    } else {
                        //the leaf being a child of a NP
                        parseOut += " <" + tw.getTag() + "> " + tw.getContent().replaceAll(" ", "") + " </" +
                                tw.getTag() + "> ";
                    }
                }

            } catch (Exception e) {
                System.exit(-1);
            }
        }

        /** @todo delete */
        //System.out.println(parseOut);
    }


    private String replaceAnnotatables(Vector vec) {
        //locate the annotatables in the modified and reformated output of the parser
        // and substitute them with the original string (without parse tags), embraced with co-reference annotations
        StringBuffer sbuf = new StringBuffer(); //m.appendReplacement(sbuf, m.group().toUpperCase());

        int flag = 0;
        for (int i = 0; i < vec.size(); i++) {
            MUCNP annotatable = (MUCNP) vec.get(i);
            if (!annotatable.hasChild()) { //consider only the finest items

                //divide one's into one---'s
                //try to match <> ABC <>, if fails (always fails, bug in java.util.reg?)
                //try to match <> A <> <> B <> <> C <>
                String pat = "\\n(.*)( <[A-Z\\$]+> " +
                        annotatable.getText(false).trim().
                                replaceAll("'", " \\'").//divide
                                replaceAll(" +", "( </[A-Z\\$]+> \\\\n? <[A-Z\\$]+> )??") +//inner tags
                        " </[A-Z\\$]+> )" + "(.*)\\n";
                //System.out.println("+" + annotatable.getText(false) + "+        [" + pat + "]");
                Pattern p = Pattern.compile(pat);
                Matcher m = p.matcher(parseOut);

                if (m.find(flag)) {
                    sbuf.append(parseOut.substring(flag, m.start()));

                    flag = m.end(); //advance one step to let the next search looking at something different

                    if (m.group(1).replaceAll(" ", "").indexOf(">" + annotatable.getText(false) + "<") > -1) {
                        /** @todo Add the tag of annotatable here, if necessary at all.*/
                        //System.out.println(annotatable.toStringTagForm());
                        sbuf.append("\n " + annotatable.toStringTagForm() + "\n");
                    } else {
                        //System.out.println(annotatable.toStringTagForm() +
                        //                   parseOut.substring(m.end(2), m.end()));
                        //important! Add an extra space at the beginning to indicate that the whole line should be retained in the final filtering
                        sbuf.append("\n " + annotatable.toStringTagForm() +
                                parseOut.substring(m.end(2), m.end()));
                    }
                    /*System.out.println(m.group(1).length() + "[] " + m.group(2).length() +
                                        "[] " +
                                        (m.group(0).length() - m.group(1).length() -
                                         m.group(2).length() - "\n\n".length()));
                              System.out.println("group 1: "+m.group(1));
                              System.out.println("group 2: "+m.group(2));
                              System.out.println("group 3: "+m.group(3));
                              System.out.print(flag);
                        System.out.println("<<"+parseOut.substring(m.start(), m.end()));
                        System.out.println(" +" + annotatable.getText(false) + "+");
                    */
                } else {
                    /** @todo 29% concatnated in muc but separated in parse */
                    System.err.println("Gush ............ I missed it in parser output. E:( " + annotatable.getText(false));
                    //System.exit( -1);
                }

            }
        }
        sbuf.append(parseOut.substring(flag));


        //final filter: keep only the first <tag> str </tag> in each line
        StringBuffer sbuf2 = new StringBuffer();
        Pattern p = Pattern.compile("(?m)^( *)(<[^<>]+>[^<>]*</[[^<>]]+>).*$");
        Matcher m = p.matcher(sbuf.toString());

        while (m.find()) {
            // System.out.println(m.group());
            // System.out.println(m.group(1));
            //System.out.println(m.group(1).length());
            if (m.group(1).length() == 1) { //a line in the original parseOut: a NP with, perhaps, its children. Retain only the NP.
                sbuf2.append(m.group(2));
            } else {//a line generated. Retain the whole line
                sbuf2.append(m.group());
            }
        }

        //checking
        /*
        System.err.println(sbuf2.toString().replaceAll("<[^<>]+>","").replaceAll("\\W",""));
        System.err.println(orgContent.toString().replaceAll("<[^<>]+>","").replaceAll("\\W",""));

        int a = sbuf2.toString().replaceAll("<[^<>]+>","").replaceAll("\\W","").length();
        int b = orgContent.toString().replaceAll("<[^<>]+>","").replaceAll("\\W","").length();
        */
        return sbuf2.toString();
    }


    private void test(Vector vec) {
        //locate the annotatables in the original text, they are in their natural orders, i.e. the orders in the orginal text
        /*String content = QLGadget.Util.removeTag(orgContent.toString());
        int flagA = 0;
        for (int i = 0; i < vec.size(); i++) {
          MUCNP annotatable = (MUCNP) vec.get(i);
          if (!annotatable.hasChild()) {
            //System.err.print("------------\t");
            //System.err.println(annotatable.toString().replaceAll("\n"," "));

            Pattern p = Pattern.compile(annotatable.getText(false));
            Matcher m = p.matcher(content);
            if (m.find(flagA)) {
              flagA = m.start() + 1; //advance one step to let the next search looking at something different
              System.err.print(flagA);
              System.err.println(" +" + annotatable.getText(false) + "+");
            }
            else {
              System.err.println("Gush ............ I missed it. E:(");
              System.exit( -1);
            }
          }
        }*/
        System.exit(-2);
    }

    /**
     * Can be used as a static method
     *
     * @param fileName
     * @return
     */
    private AnnotatedText parseIt(String fileName) {
        //reads the orginal file, removes the tags and parses it

        orgContent = Util.readFile(fileName);

        //hacking: adding a PERIOD before the tag <TXT> to separate the title from the the text
        int startingPoint = orgContent.indexOf("<TXT>");
        if (startingPoint > 0) {
            orgContent.insert(startingPoint,
                    "." + System.getProperty("line.separator"));
        } else {
            System.err.println("Could not find <TXT> tags in " + fileName + ", evaluation aborts. The text (other than the title or headline of the text) that the resolver/evaluator works on should be in <TXT></TXT> tags. ");
            System.exit(-1);
        }

        //removes tags
        String content = Util.removeTag(orgContent.toString());

        PlainText plainText = new PlainText(new StringBuffer(content));
        //delimites
        content = plainText.addQuote(false);

        //parses
        String parseFlat;
        if (!new java.io.File(fileName + ".par").exists()) {
            String tmpFileName = "/tmp" + fileName.substring(fileName.lastIndexOf("/")) +
                    ".del";
            Util.write(tmpFileName, content);
            parseFlat = Util.UnixSystemCall("parseIt -l399 ~/bin/DATA/ " +
                    tmpFileName);
            //keep the parse
            Util.write(fileName + ".par", parseFlat);
            new java.io.File(tmpFileName).delete();
        } else {
            parseFlat = Util.readFile(fileName + ".par").toString();
        }

        AnnotatedText parseStructured = new AnnotatedText(parseFlat);

        return parseStructured;
    }

    public static void main(String[] args) {
        //Given a MUC co-reference trainning/annotated file, find the POS tags of the context. Achieved by looking up the
        //annotatables in the parse generated by Charniak's Parser and do a substitutation. Much complicated than it sounds. Eh...
//    Arguments: "/home/qiulong/PhD/Shared/JBproject/AnaphoraResolution/Data/SampleResultFile.txt -a\n /home/qiulong/PhD/Research/summary/data/testdata/task1/docs.without.headlines/d100a/APW19990519.0113  /home/qiulong/PhD/Projects/MUC_Corref/muc6/train"


        String fileName = "$HOME/muc6/train/8701230009";
        Merger merger1 = new Merger(fileName);
        String rlt = merger1.merge();
        Util.write(fileName + ".shi", rlt);
    }
}
