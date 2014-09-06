/*
 JavaRAP: a freely-available JAVA anaphora resolution implementation
 of the classic Lappin and Leass (1994) paper:

 An Algorithm for Pronominal Anaphora Resolution.
 Computational Linguistics, 20(4), pp. 535-561.

 Copyright (C) 2005  Long Qiu

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

import java.io.File;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Title: Anaphora Resolution</p>
 * <p>Description: </p>
 * <br>
 * <br>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 *
 * @author Qiu Long
 * @version 1.0
 */

public class Evaluation {
    private static Env env = new Env();

    public Evaluation() {
    }

    public String check(File f, String resultFileName) {
        //Instead of using the input file F, we use a pure text version of F for resolving;
        //and as reference we use a updated version of F where the missing annotations, if there are any, are added.

        //if resultFileName is not "null", compare resolution results in "resultFileName" with file f (with annotation)
        boolean checkExistingResults = false;
        if (resultFileName != null) {
            checkExistingResults = true;
        }
        //input file
        String fileName = new String();
        //********First remove the tag in the file, if there is any ********
        //text in the file
        StringBuffer orgText = new StringBuffer();
        //text with the tags removed from the original text
        String pureTxt = "";
        //New file to store the text with tags removed; it is stored in the working directory of the resolver which is defined as "outputdir"
        String pureTextFileName = "";//to be defined shortly
        //remove the tags from the text and store it as a pureTextFile in the working dir
        try {
            fileName = f.getCanonicalPath();
            pureTextFileName = System.getProperty("outputDir") + File.separator +
                    fileName.substring(fileName.lastIndexOf("/") + 1) + ".pure";
            orgText = Util.read(fileName);

            //hacking: adding a PERIOD before the tag <TXT> to separate the title from the the text
            int startingPoint = orgText.indexOf("<TXT>");
            if (startingPoint > 0) {
                orgText.insert(startingPoint, "." + System.getProperty("line.separator"));
            } else {
                System.err.println("Could not find <TXT> tags in " + fileName + ", evaluation aborts. The text (other than the title or headline of the text) that the resolver/evaluator works on should be in <TXT></TXT> tags. ");
                System.exit(-1);
            }

            if (!checkExistingResults) {
                //not necessary for checking existing results
                pureTxt = Util.removeTag(orgText.toString());
                Util.write(pureTextFileName, pureTxt);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }


        Vector rlt = null;
        if (checkExistingResults) {
            rlt = this.readResultFile(resultFileName);
        } else {
            //********resolving part , dampen the output**********
            System.setProperty("display log", "false");
            System.setProperty("referenceChain", "false");
            Util.showMessage("\n\n***Resolving anaphora in \"" + fileName + "\"***", System.getProperty("EvaluationVerbose").equals("true"));
            rlt = MainProcess.stageTwoTestV1(new File(pureTextFileName));
        }

        Util.showMessage("\n***Errors*** \t[Complete text span > (anaphor) antecedent suggested by the resolver > minimal text span]*\n", System.getProperty("EvaluationVerbose").equals("true"));


        //update the annotated file, add a dummy annotation for third personal pronouns without annotation
        String str = orgText.toString();
        String strUpdated = new String(str);
        //********** Preprocessing ******
        //preprocess the file, annotate those ommitted in origional file, if there are any
        String[] thirdPersonList = new String(
                "he him himself his she her herself they them their themselves it its itself"
                        +
                        " He Him Himself His She Her Herself They Them Their Themselves It Its Itself").
                split(" ");
        for (int i = 0; i < thirdPersonList.length; i++) {
            String pattern = "[^>a-zA-Z]" + thirdPersonList[i] + "[^<a-zA-Z]";
            strUpdated = strUpdated.replaceAll(pattern,
                    " <COREF ID=\"DummyID\" >" +
                            thirdPersonList[i] + "</COREF> ");
        }

        //remove &nnn, which is considered illegial by java xml parser (sax?)
        strUpdated = strUpdated.replaceAll("\\&\\w{1,8}\\;", "");
        //remove &, which is considered illegial by java xml parser (sax?)
        strUpdated = strUpdated.replaceAll("\\&", "AND_AMP");

        //update the file if new annotation added
        String updatedFileName = fileName;
        if (str.length() != strUpdated.length()) {
            updatedFileName = System.getProperty("outputDir") + File.separator +
                    fileName.substring(fileName.lastIndexOf("/") + 1) + ".updated";
            Util.write(updatedFileName, strUpdated);
        }


        //extract the keys
        //note that updatedFileName will be the original file name if no new annotations are added
        MUCFile mucf = new MUCFile(updatedFileName);
        mucf.buildNPSet();


        String dataShow = mucf.evalutaion(rlt);
        String legend = "#pronoun #correct #pleoCorrect #incorrect #pleoWrong #missed";

        Util.showMessage("\n***Comparison results for \"" + fileName + "\": \n" + legend + "\n" + dataShow + "\n", System.getProperty("EvaluationVerbose").equals("true"));


        Vector key = mucf.getNPVec();
        Util.showMessage("***Details of the outcome of the resolver***", System.getProperty("EvaluationVerbose").equals("true"));
        Util.showMessage(rlt.toString() + "\n", System.getProperty("EvaluationVerbose").equals("true"));
        Util.showMessage("***Details of the annotated file (\"DummyID\" indicates a np not annotated in the file)***", System.getProperty("EvaluationVerbose").equals("true"));
        Util.showMessage(key.toString(), System.getProperty("EvaluationVerbose").equals("true"));

        return dataShow;
    }


    private Vector readResultFile(String filename) {
        Vector results = new Vector();
        String[] records = Util.readFile(filename).toString().split(System.
                getProperty("line.separator"));
        for (int i = 0; i < records.length; i++) {
            if (records[i].startsWith("#")
                    || records[i].trim().length() == 0) {
                //comments
                continue;
            }

            //(15,4) she <-- (15,9) her
            Pattern p = Pattern.compile("(.+)\\s*<--\\s*(.+)");
            Matcher m = p.matcher(records[i]);
            if (m.find()) {
                String refereeRecord = m.group(1);
                String refererRecord = m.group(2);
                results.add(new CorreferencialPair(refereeRecord, refererRecord));
            } else {
                System.err.println("Format error in: " + records[i]);
                System.exit(-1);
            }
        }
        return results;
    }


    public static void main(String[] args) {

        //starting time
        //System.err.println(new java.util.Date());

        //initialize the environment
        Env env = new Env();
        Evaluation eva = new Evaluation();
        System.setProperty("EvaluationVerbose", "true");


        if (args.length < 1) {
            showHelpMessage();
        }

        //Fix the file name
        String resultFileName = null;//a file containing the pairwise anaphora resolution results.
        String inputDir = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-r")) {
                if (i < args.length - 2) {
                    resultFileName = args[i + 1];
                    System.setProperty("PairInput", "true");
                } else {
                    showHelpMessage();
                    System.exit(1);
                }
            }
            if (args[i].endsWith("-help")) {
                showHelpMessage();
                System.exit(1);
            }
            if (args[i].endsWith("-q")) {
                System.setProperty("EvaluationVerbose", "false");
            }
            if (args[i].endsWith("-v")) {
                System.setProperty("EvaluationVerbose", "true");
            }
        }
        inputDir = args[args.length - 1];

        if (!new File(inputDir).exists()) {
            System.out.println(inputDir + " doesn't exist. \nExit.");
            System.exit(0);
        }

        int correct = 0;
        int pleoCorrect = 0;
        int incorrect = 0;
        int pleoWrong = 0;
        int missed = 0;
        int total = 0;

        try {
            //If the input is a FILE, process it; if it is a DIRECTORY, process all the files in it.
            if (new File(inputDir).isDirectory()) {
                //if it's a directory
                if (resultFileName != null) {
                    //a result file is not suitable for this mode, where a list of texts are to be processed.
                    System.err.println("Please specify a result file for each text individually.");
                    System.exit(-1);
                }

                File[] inputFile = new File(inputDir).listFiles();
                if (inputFile != null) {
                    for (int i = 0; i < inputFile.length; i++) {
                        if (inputFile[i].isFile()) {
                            String[] score = null;
                            try {

                                score = eva.check(inputFile[i], null).split("\t");

                                total += Integer.valueOf(score[0]).intValue();
                                correct += Integer.valueOf(score[1]).intValue();
                                pleoCorrect += Integer.valueOf(score[2]).intValue();
                                incorrect += Integer.valueOf(score[3]).intValue();
                                pleoWrong += Integer.valueOf(score[4]).intValue();
                                missed += Integer.valueOf(score[5]).intValue();

                            } catch (Exception ex) {
                                System.err.println(eva.check(inputFile[i], null));
                                ex.printStackTrace();
                                continue;
                            }

                        }
                    }
                }
            } else {
                // if it's a file
                File inputFile = new File(inputDir);

                try {
                    String[] score = null;
                    if (resultFileName == null) {
                        //no result file specified
                        score = eva.check(inputFile, null).split("\\s+");
                    } else {
                        //Todo:
                        score = eva.check(inputFile, resultFileName).split(" ");
                        //System.err.println("Coming in the next version (due to the problem of how to unambiguously identify the NPs");
                        System.exit(0);
                    }
                    total += Integer.valueOf(score[0]).intValue();
                    correct += Integer.valueOf(score[1]).intValue();
                    pleoCorrect += Integer.valueOf(score[2]).intValue();
                    incorrect += Integer.valueOf(score[3]).intValue();
                    pleoWrong += Integer.valueOf(score[4]).intValue();
                    missed += Integer.valueOf(score[5]).intValue();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            Util.showMessage("\n*************************\nEvaluation finished and the overall result is:", System.getProperty("EvaluationVerbose").equals("true"));
            System.out.println("Total resolvable anaphors:\t" + total +
                    "\nCorrectly identified:\t\t " + correct +
                    "\nPleonastic Identified:\t\t " + pleoCorrect +
                    "\nIncorrectly identified:\t\t " + incorrect +
                    "\nPleonastic Mistakes:\t\t " + pleoWrong +
                    "\nMissed in the annotated file:\t" + missed);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }
        //ending time
        //System.err.println(new java.util.Date());
    }


    private static void showHelpMessage() {
        //Todo: change this
        System.out.println("Usage: java Usage: java edu.nus.comp.NLP.tool.anaphoraresolution.Evaluation [options] gold_standard_file");
        System.out.println(
                "Compare the third person pronouns (nominative, objective, possessive,");
        System.out.println(
                "reflective) resolution results with a gold standard annotated text.");

        System.out.println("\nOptions:");
        System.out.println("\t -help\t display this message");
        System.out.println("\t -q\t quiet mode");
        System.out.println("\t -r filename\t Compare to the results stored in \"filename\" instead of resolving on-line.");
        System.out.println("\t -v\t verbose mode (default)");


        System.out.println(
                "Contact Mr. Long Qiu qiul@comp.nus.edu.sg for bug-reporting, please.");
    }
}
