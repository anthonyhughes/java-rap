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

/**
 * <p>Title: Qiu Long's Personal Gadgets</p>
 *
 * <p>Description: A copy of external class to make JavaRAP standalone.</p>
 *
 * <p>Copyright: Copyright (c) 2004</p>
 *
 * <p>Company: </p>
 *
 * @author Qiu Long qiul@comp.nus.edu.sg
 * @version 1.1
 */

import com.datalanguage.javarap.utils.Util;

/**
 * <p>Title: Anaphora Resolution</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 *
 * @author Qiu Long
 * @version 1.0
 */

public class TRECFile {

    final static public String articleIDReg = "[A-Z]{3}[0-9]{8}\\.[0-9]{4}";
    StringBuffer content = null;
    String fileName = null;
    String outputFileName = null;

    public TRECFile(String fileName, String outputFile) {
        this.fileName = fileName;
        this.outputFileName = outputFile;
        //if(fileName.endsWith(".GZ")){
        decompress(fileName);
        //}
    }


    private void decompress(String fileName) {
        String command = " gzip ";
        String options = " -dc ";
        String[] cmd = {"/bin/sh", "-c", command + options + fileName + " > " + outputFileName};
        String str = null;

        try {
            Process proc = Runtime.getRuntime().exec(cmd);

            try {
                proc.waitFor();
            } catch (InterruptedException e) {
                System.out.println(e);
            }
        } catch (Exception e) {
            System.err.println("Wrong while decompressing " + fileName);
        }
    }

    /**
     * Only remain the parts between <DOCNO> </DOCNO> and <TEXT> </TEXT>
     */
    public String removeTag() {
        content = Util.readFile(outputFileName);
        String rlt = new String();
//
        if (content == null) {
            return rlt;
        }
        String s = content.toString();
        String[] lines = s.split("<DOCNO>|<DOCTYPE>|<TEXT>|</BODY>");

        for (int i = 0; i < lines.length; i++) {
            if (lines[i].indexOf("</DOCNO>") > 0) {
                rlt += lines[i].substring(0, lines[i].lastIndexOf("</DOCNO>"));
            } else if (lines[i].indexOf("</TEXT>") > 0) {
                rlt += lines[i].substring(0, lines[i].lastIndexOf("</TEXT>")) + "\n";
            }
        }

        String[] ps = rlt.split("<P>|</P>");
        rlt = "";
        for (int j = 0; j < ps.length; j++) {
            rlt += ps[j];
        }

        return rlt;
    }

}
