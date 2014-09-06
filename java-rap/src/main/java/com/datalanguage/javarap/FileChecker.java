package com.datalanguage.javarap;

import com.datalanguage.javarap.utils.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * <p>Title: Anaphora Resolution</p>
 * <p>Description: This class contains the methods to check whether files from AQUAINT Corpus have been parsed successfully.</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 *
 * @author Qiu Long
 * @version 1.0
 */

public class FileChecker {
    public FileChecker() {
    }

    static private String getRealFilePath(String fileName) {
        String org = fileName.substring(9, 12);
        String year = fileName.substring(0, 4);
        if (org.equals("XIN")) {
            org = "XIE";
        }
        return org + File.separator + year + File.separator + fileName.subSequence(0, fileName.length() - 2) + "txt.out";
    }


    public static void main(String[] args) {
        new Env();
        if (args.length < 1) {
            System.out.println("Please specify the method: \n-m1:\tcheck file size\n-m2:\tcheck article ID");
            System.exit(0);
        }

        if (args[0].equals("-m1")) {
            checkSize(args);
        } else if (args[0].equals("-m2")) {
            checkID(args);
        } else {
            System.out.println("Please specify the method: \n-m1:\tcheck file size\n-m2:\tcheck article ID");
            System.exit(0);
        }
    }


    public static void checkSize(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: java FileChecker -m1 OrigionalFileSufix GZFileList ParsedFileRootPath [threshhold]");
            System.exit(0);
        }

        File parsedFileList = new File(args[2]);
        String s;

        int threshhold = 800;
        if (args.length > 4) {
            threshhold = Integer.decode(args[4]);
        }

        if (!parsedFileList.exists()) {
            System.out.println("File " + parsedFileList + " doesn't exist. System quits.");
            System.exit(1);
        }

        try {
            BufferedReader in =
                    new BufferedReader(new FileReader(args[2]));

            int counter = 0;
            long parsedSize = 0;
            long gzSize = 0;
            String gzSizeStr;
            String parsedFileName;
            File parsedFile;
            while ((s = in.readLine()) != null) {
                counter++;
                if (s.endsWith("." + args[1])) {
                    gzSizeStr = s.split(" +")[5];
                    gzSize = Integer.decode(gzSizeStr);
                    parsedFileName = args[3] + File.separator + getRealFilePath(s.substring(s.lastIndexOf(" ")).trim());
                    parsedFile = new File(parsedFileName);
                    parsedSize = parsedFile.length();
                    long ratio = (parsedSize * 100) / gzSize;
                    if (ratio < threshhold) {
                        System.out.println(s.substring(s.lastIndexOf(" ") + 1) + "\t" +
                                ratio);
                    }
                }
            }
            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void checkID(String[] args) {
        Pattern pDoc = Pattern.compile(TRECFile.articleIDReg);
        Matcher m = null;
        if (args.length < 5) {
            System.out.println("Usage: java FileChecker -m2 GZFileList GZFileRootPath DelimitedFileRootPath ParsedFileRootPath");
            System.exit(0);
        }

        File parsedFileList = new File(args[1]);
        String s;

        if (!parsedFileList.exists()) {
            System.out.println("File " + parsedFileList + " doesn't exist. System quits.");
            System.exit(1);
        }

        try {
            BufferedReader in =
                    new BufferedReader(new FileReader(args[1]));

            int counter = 0;

            String compressedFileName;
            String delimitedFileName;
            String parsedFileName;
            String tmpFileName = System.getProperty("tmpDir") + File.separator + "tmp";

            String articleID = null;
            boolean foundinDelim;
            boolean foundInParsed;
            while ((s = in.readLine()) != null) {
                counter++;
                if (s.endsWith(".GZ")) {
                    //reset
                    foundinDelim = false;
                    foundInParsed = false;
                    s = s.substring(s.lastIndexOf(" ") + 1);
                    System.out.println(s + "/" + counter);
                    //Parsed file
                    parsedFileName = getRealFilePath(s);
                    //GZ file
                    compressedFileName = parsedFileName.substring(0, parsedFileName.lastIndexOf("txt.out")) + "GZ";
                    //delimited file
                    delimitedFileName = parsedFileName.substring(0, parsedFileName.lastIndexOf(".out"));

                    //bind with root
                    compressedFileName = args[2] + File.separator + compressedFileName;
                    delimitedFileName = args[3] + File.separator + delimitedFileName;
                    parsedFileName = args[4] + File.separator + parsedFileName;

                    if (!new File(compressedFileName).exists()) {
                        System.out.println("Can't find compressed " + compressedFileName);
                        System.exit(0);
                    }

                    //unzip GZ file to a tmp file
                    TRECFile trecF = new TRECFile(compressedFileName, tmpFileName);
                    //find last article ID
                    String fileContent = Util.read(tmpFileName).toString();
                    int startAt = 0;
                    m = pDoc.matcher(fileContent);

                    while (m.find(startAt)) {
                        startAt = m.end();
                        articleID = fileContent.substring(m.start(), m.end());
                    }
                    //remove tmp file
                    new File(tmpFileName).delete();


                    //read parsed file //find article ID in the parsed file
                    if (!new File(parsedFileName).exists()) {
                        Util.errLog(parsedFileName);
                        continue;
                    }
                    foundInParsed = Util.read(parsedFileName).toString().indexOf(articleID) > 0;

                    if (foundInParsed) {
                        continue;
                    }

                    //read delimited file //find article ID in the delimited file
                    if (!new File(delimitedFileName).exists()) {
                        Util.errLog(delimitedFileName);
                    }
                    foundinDelim = Util.read(delimitedFileName).toString().indexOf(articleID) > 0;

                    if (foundinDelim) {
                        Util.errLog(parsedFileName);
                    } else {
                        Util.errLog(delimitedFileName);
                    }
                }
            }
            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}