/*
 JavaRAP: a freely-available JAVA anaphora resolution implementation
 of the classic Lappin and Leass (1994) paper:

 An Algorithm for Pronominal Anaphora Resolution.
 Computational Linguistics, 20(4), pp. 535-561.

 Copyright (C) 2005,2011  Long Qiu

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
import com.datalanguage.javarap.utils.Util;

import java.io.File;
import java.util.Vector;

/**
 * <p>Title: Anaphora Resolution</p>
 * <p>Description: Class used during first development stage. Expired.</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 *
 * @author Qiu Long
 * @version 1.0
 *          Feb 12, 2006
 *          Make it work on windows. Long Qiu
 */

public class MainProcess {
    static String inputDir = null;
    static String outputDir = null;
    private static final boolean fullProcess = true;
    static Env env = new Env(); //initiallize path informations

    public MainProcess() {
    }

    public static void main(String[] args) {
        /*Things to know before the program could work properly:
        * whereToFindCharniakParser
        * whereToFindParsedFiles
        * whereToFindDelimitedFiles
        * whereToLoadDataForResolver
        * whereToFindInputFiles
        * whereToPutOutput
        */
        //Debugging code
//	  String parseText = Util.readFile(System.getProperty("user.home")+"/JavaRAPWebDemo.input.par.txt").toString();
//	  stageTwoTestV1Core(parseText);
        //System.out.println("Begin at "+new java.util.Date());
        //pipePhaseOne(inputDir);
        //stageOneTest();
        //stageTwoTest();
        //System.out.println("Finished at "+new java.util.Date());
        //System.exit(0);

    }

    public static Vector stageTwoTestV1(File plaintextFile) {
        String osType = System.getProperty("os.name");

        //Check the directory structure and make sure that the parser and its data can be located

        // default unix environment
        String cmd = System.getProperty("parserHomeDir") + File.separator +
                "PARSE" + File.separator + "parseIt";
        if (!new File(cmd).exists()) {
            //turns out to be an previous version Charniak parser, use the old environment instead
            cmd = System.getProperty("parserHomeDir") + File.separator +
                    "parseIt";
        }


        String dataPath = System.getProperty("parserHomeDir") + File.separator +
                "DATA" + File.separator + "EN" + File.separator; //the extra EN is for newer version of Charniak parser
        if (!new File(dataPath).exists()) {
            //turns out to be an previous version Charniak parser, use the old environment instead
            dataPath = System.getProperty("parserHomeDir") + File.separator +
                    "DATA" + File.separator;
        }

        //check whether the expected executable exist
        if (!new File(cmd).exists()) {
            System.err.println(cmd + " not found. Please check ...");
            System.err.println("JavaRAP exits ........");
            System.exit(-1);
        }

        if (!new File(dataPath).exists()) {
            System.err.println(dataPath + " not found. Please check ...");
            System.err.println("JavaRAP exits ........");
            System.exit(-1);
        }

        String parserOption = " -l399";
        cmd = cmd + parserOption;


        String delimitedSuffix = ".del"; //files where sentence delimation added
        String parsedSuffix = ".par"; //files where sentence delimation added

        java.util.Vector rlt = new Vector();

        try {
            Util.errLog(plaintextFile.getName() +
                    " under processing...seperating sentences..." +
                    new java.util.Date());
            StringBuffer Tcp = Util.read(plaintextFile.getCanonicalPath());
            if (Tcp.length() == 0) {
                Util.errLog("Empty file, or wrong file reading function invoked.");
                return rlt;
            }
            PlainText plainText1 = new PlainText(Tcp);

            plainText1.setSingleLine(true);
            plainText1.removeTag();

            String parse = null; //the parsing results
            String parseResultFileName = System.getProperty("outputDir") + File.separator +
                    plaintextFile.getName() + parsedSuffix;
            if (!new File(System.getProperty("outputDir")
                    + File.separator
                    + plaintextFile.getName()
                    + parsedSuffix).exists()) {
                //parse the file only when necessary
                //skip the following parsing block if the parsed version already exists.
                Util.errLog(plaintextFile.getName() + " under processing...reading..." +
                        new java.util.Date());
                Util.write(System.getProperty("tmpDir") + File.separator +
                        plaintextFile.getName() + delimitedSuffix,
                        plainText1.addQuote(false, "\n"));
                Util.errLog(plaintextFile.getName() + " under processing...parsing..." +
                        new java.util.Date());
                parse = Util.parse(cmd,
                        dataPath,
                        System.getProperty("tmpDir") + File.separator +
                                plaintextFile.getName() + delimitedSuffix,
                        parseResultFileName);
                //Keep the parse result in a file to speed up futher revisiting to the file
                //However, this could be PROBLEMATIC if the file under process has a name that
                //Has been used before. --Solution: clean the System.getProperty("outputDir") from time to time.
                if (parse.length() > 0) {
                    Util.write(parseResultFileName, parse);
                } else {

                }
            } else {//read the parse file if it's already there
                Util.errLog(plaintextFile.getName() +
                        " under processing...reading previous parser output WITHOUT parsing it ..." +
                        new java.util.Date());
                Util.errLog("Please check there is no name collision etc. so that the parse \""
                        + parseResultFileName + "\" is for the input file \"" + plaintextFile.getName() + "\"");
                //read parsed file
                parse = Util.read(parseResultFileName).toString();
            }

            Util.errLog(plaintextFile.getName() + " under processing...resolving..." +
                    new java.util.Date());
            AnnotatedText aText = new AnnotatedText(parse);
            //aText.test(aText.getPRPList());
            //aText.test(aText.getNPList());
            //aText.test(aText.getSNPList());
            //aText.test(aText.GlobalList);
            //Util.resolverBaseline(aText.getSNPList());
            rlt = Util.resolverV1(aText.getNPList(),
                    aText.getPRPList());

            //output substitution version if required
            if (System.getProperty("Substitution") != null &&
                    System.getProperty("Substitution").equals("true")) {
                String textWithSutstitution = Util.substitutionV1(aText.getTree(), rlt);
                Util.showMessage("********Text with substitution*****\n" +
                        textWithSutstitution + "\n",
                        System.getProperty("display substitution results").equals(
                                "true"));

                if (System.getProperty("write substitution results") != null &&
                        System.getProperty("write substitution results").equals("true")) {
                    Util.write(System.getProperty("outputDir")
                            + File.separator
                            + plaintextFile.getName()
                            + ".sub", textWithSutstitution);
                }
            }


            //Util.displayTree(aText.getTree());
        } catch (Exception e) {
            System.err.println("Wrong in MainProcess");
            e.printStackTrace();
        }
        return rlt;
    }

    /**
     * Take existing Penn Treebank-style syntactic analysis annotation as input and resolve third-person pronouns in it.
     *
     * @param parserFileName The file containing Penn Treebank-style annotation.
     * @return Vector of instances of <var>CorreferencialPair</var>.
     */
    public static Vector stageTwoTestV1(String parserFileName) {
        String parse = Util.readFile(parserFileName).toString();
        Vector rlt = stageTwoTestV1Core(parse);
        return rlt;
    }

    private static Vector stageTwoTestV1Core(String parse) {
        AnnotatedText aText = new AnnotatedText(parse);
        Vector rlt = Util.resolverV1(aText.getNPList(),
                aText.getPRPList());
        //output substitution version if required
        if (System.getProperty("Substitution") != null &&
                System.getProperty("Substitution").equals("true")) {
            String textWithSutstitution = Util.substitutionV1(aText.getTree(), rlt);
            Util.showMessage("********Text with substitution*****\n" +
                    textWithSutstitution + "\n",
                    System.getProperty("display substitution results").equals(
                            "true"));

//        if (System.getProperty("write substitution results") != null &&
//            System.getProperty("write substitution results").equals("true")) {
//          Util.write(System.getProperty("outputDir")
//                     + File.separator
//                     + plaintextFile.getName()
//                     + ".sub", textWithSutstitution);
//        }
        }
        return rlt;
    }

    public static void stageTwoTest() {
        String outputSuffix = ".out"; //parser output
        String tmpSuffix = ".tmp"; //files where sentence delimation added
        String cmd = System.getProperty("parserHomeDir") + File.separator +
                "parseIt";
        String dataPath = System.getProperty("parserHomeDir") + File.separator +
                "DATA/";
        String textDir = "Text";
        textDir = "TRECCorpus";
        String outputDir = "Text.out";

        try {
            String workPath = new File(".").getCanonicalPath();
            File[] inputFile = new File("." + File.separator + textDir).listFiles();
            for (int i = 0; i < inputFile.length; i++) {
                if (inputFile[i].isDirectory()) {
                    continue;
                }

                Util.errLog(inputFile[i].getName() +
                        " under processing...seperating sentences..." +
                        new java.util.Date());
                StringBuffer Tcp = Util.corpusRead(workPath + File.separator +
                        textDir + File.separator +
                        inputFile[i].getName());
                if (Tcp.length() == 0) {
                    Util.errLog("Empty file, or wrong file reading function invoked.");
                }
                PlainText plainText1 = new PlainText(Tcp);

                String osType = System.getProperty("os.name");
                if (fullProcess && (!osType.startsWith("Windows"))) {
                    //skip the following /**parsing***/ block for windows
                    Util.errLog(inputFile[i].getName() + " under processing...reading..." +
                            new java.util.Date());
                    Util.write(workPath + File.separator + outputDir +
                            File.separator +
                            inputFile[i].getName() + tmpSuffix,
                            plainText1.addQuote(false));
                    Util.errLog(inputFile[i].getName() + " under processing...parsing..." +
                            new java.util.Date());
                    Util.parse(cmd,
                            dataPath,
                            workPath + File.separator + outputDir + File.separator +
                                    inputFile[i].getName() + tmpSuffix,
                            workPath + File.separator + outputDir + File.separator +
                                    inputFile[i].getName() + outputSuffix);
                }

                Util.errLog(inputFile[i].getName() +
                        " under processing...reading output of parser..." +
                        new java.util.Date());
                //read parsed file
                StringBuffer taggedText = Util.read(
                        workPath + File.separator + outputDir + File.separator +
                                inputFile[i].getName() + outputSuffix);
                Util.errLog(inputFile[i].getName() + " under processing...resolving..." +
                        new java.util.Date());
                AnnotatedText aText = new AnnotatedText(taggedText.toString());
                System.out.println(new java.util.Date());
                //aText.test(aText.getPRPList());
                //aText.test(aText.getNPList());
                //aText.test(aText.getSNPList());
                //aText.test(aText.GlobalList);
                //Util.resolverBaseline(aText.getSNPList());
                java.util.Vector rlt = Util.resolverV1(aText.getNPList(),
                        aText.getPRPList());
                Util.write("output.sub", Util.substitutionV1(aText.getTree(), rlt));
            }
        } catch (Exception e) {
            System.err.println("Wrong in MainProcess");
            e.printStackTrace();
        }

    }

    public static void stageOneTest() {
        String outputSuffix = ".out"; //parser output
        String tmpSuffix = ".tmp"; //files where sentence delimation added
        String cmd = "$HOME/Charniak/parseIt";
        String dataPath = "$HOME/Charniak/DATA/";
        String textDir = "Text";
        String outputDir = "Text.out";

        try {
            String workPath = new File(".").getCanonicalPath();
            File[] inputFile = new File("." + File.separator + textDir).listFiles();
            for (int i = 0; i < inputFile.length; i++) {
                if (inputFile[i].isDirectory()) {
                    continue;
                }

                Util.errLog(inputFile[i].getName() +
                        " under processing...seperating sentences...");
                StringBuffer Tcp = Util.read(workPath + File.separator +
                        textDir + File.separator +
                        inputFile[i].getName());
                if (Tcp.length() == 0) {
                    Util.errLog("Empty file, or wrong file reading function invoked.");
                }
                PlainText plainText1 = new PlainText(Tcp);
                //System.out.println(inputFile[i].getName()+")*******************\n"+plainText1.addQuote(true));
                Util.write(workPath + File.separator + outputDir +
                        File.separator +
                        inputFile[i].getName() + tmpSuffix,
                        plainText1.addQuote(false));

                String osType = System.getProperty("os.name");
                if (!osType.startsWith("Windows")) {
                    //skip the following /**parsing***/ block for windows
                    Util.errLog(inputFile[i].getName() + " under processing...parsing...");
                    Util.parse(cmd,
                            dataPath,
                            workPath + File.separator + outputDir + File.separator +
                                    inputFile[i].getName() + tmpSuffix,
                            workPath + File.separator + outputDir + File.separator +
                                    inputFile[i].getName() + outputSuffix);

                    //read parsed file
                } else {
                    //simply read the parsed file, it should be there, hopefully. ;)
                }

            }
        } catch (Exception e) {
            System.err.println("Wrong in MainProcess");
        }

        //String[] files = {"01","02","03","04","05","06","07","08","09","10","11","12","13","14","15","16","17","18","19","20"};
        String[] files = {
                "t2"};
        String fileName;

        for (int i = 0; i < files.length; i++) {
            fileName = files[i];
            //**********Formatting***************//
            PlainText plainText1 = new PlainText("./Text/" + fileName + ".dat");
            System.out.println(fileName + ")*******************\n" +
                    plainText1.addQuote(true));

            StringBuffer taggedText = Util.read("./Text.out/" + fileName +
                    ".dat" + ".out");

            AnnotatedText aText = new AnnotatedText(taggedText.toString());
            //aText.test(aText.getPRPList());
            //aText.test(aText.getNPList());
            //aText.test(aText.getSNPList());
            //aText.test(aText.GlobalList);
            //displayTree(aText.getTree());
            //Util.resolverBaseline(aText.getSNPList());
            java.util.Vector rlt = Util.resolverV1(aText.getNPList(),
                    aText.getPRPList());
            Util.substitutionV1(aText.getTree(), rlt);
        }

        /*******test*******/
        //Util.treeNodeTest(aText.getTree());
        //displayTree(new AnnotatedText(PlainText.read("./Data/SampleSentence.out").toString()).getTree());
        /*******testends***/
        System.out.println("Done");
    }

    private static void visualizeParseTreeTxt(String fileName) {
        AnnotatedText aText = new AnnotatedText(Util.read(fileName).toString());
    }

    /**
     * .GZ file to <S> tagged text. Ready for Charniak parsing
     */
    private static void pipePhaseOne(String rootDir) {
        File[] inputFile = new File(rootDir).listFiles();
        if (inputFile == null) {
            System.out.println(
                    "Can not find files. Check the name of the path and try again. :P");
            System.exit(1);
        }
        try {
            for (int i = 0; i < inputFile.length; i++) {
                if (inputFile[i].isDirectory()) {
                    continue;
                }

                String fileName = inputFile[i].getCanonicalPath();

                if (fileName.endsWith(".GZ")) {
                    String outputFilePrefix = outputDir +
                            fileName.substring(fileName.lastIndexOf(File.separator),
                                    fileName.lastIndexOf(".GZ"));

                    if (!(new File(outputFilePrefix + ".txt").exists())) {
                        Util.errLog(new java.util.Date() + ": " + fileName +
                                " decompressing file....");
                        TRECFile trecF = new TRECFile(fileName, outputFilePrefix + ".tmp");
                        Util.errLog(new java.util.Date() + ": " + " removing tag....");
                        String s = trecF.removeTag();

                        //Todo : elimit the need of this tmp file
                        new File(outputFilePrefix + ".tmp").delete();

                        PlainText plainText1 = new PlainText(new StringBuffer(s));
                        Util.errLog(new java.util.Date() + ": " + " tagging sentences....");

                        Util.write(outputFilePrefix + ".txt", plainText1.addQuote(false));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Wrong in MainProcess");
        }
    }

}

