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

import com.datalanguage.javarap.nlp.TitleList;
import com.datalanguage.javarap.utils.Util;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Title: NLP Tools</p>
 * <p>Description: A copy of external class to make JavaRAP standalone.
 * </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: NUS</p>
 *
 * @author Qiu Long qiul@comp.nus.edu.sg
 * @version 2.01
 *          <p/>
 *          History:
 *          2004.09.27 V2.01 Bug fixing: Single upper case letter could be the head of sentences. (As well as X.X.X.)
 *          So "... was found. U.S. ... " are separated now.
 *          2004.04.28 V2.00 Bug fixing: Sentences contain more than one <i>Title CapitalLetters</i> combinations no longer wrongly separated.
 *          2003.12.09 Original puncuations retained. Switch from split to StringTokenizer to keep the delimiters.
 *          2003.11.04 New lines within sentences removed.
 */


public class PlainText {
    final static public String sentDelim =
            "``|''|\\?\"|\\!\"|\\.\"|\\. |\\.\n|\n\n|\\!|\\?";
    static String extraStr = ""; //string goes between <s> and the sentence


    static boolean singleLine = false;
    StringBuffer text = null;
    String[] sents = null;


    public PlainText() {
    }

    public PlainText(String fileName) {
        text = Util.readFile(fileName);
        mergeQuots();
    }

    public PlainText(String fileName, String commentFlag) {
        text = Util.readFile(fileName, commentFlag);
        mergeQuots();
    }

    public PlainText(StringBuffer str) {
        text = str;
        mergeQuots();
    }

    public int getSentCount() {
        if (sents == null) {
            sents = PlainText.splitSentences(text.toString(),
                    PlainText.sentDelim);
        }
        return sents.length;
    }

    public String addQuote(boolean debugMode, String exStr) {
        this.extraStr = exStr;
        return addQuote(debugMode);
    }

    /**
     * @param debugMode: If true, a index for each sentence is displayed.
     * @return
     */
    public String addQuote(boolean debugMode) {
        if (sents == null) {
            sents = PlainText.splitSentences(text.toString(),
                    PlainText.sentDelim);
        }
        String outStr = new String();

        String openFlag = "<s> ";
        String closingFlag = "</s>";

        for (int i = 0; i < sents.length; i++) {
            if (debugMode) {
                outStr += String.valueOf(i + 1) + "\t" + openFlag + extraStr + sents[i] +
                        " " + extraStr + closingFlag + "\n";
            } else {
                outStr += openFlag + extraStr + sents[i] + " " + extraStr + closingFlag +
                        "\n";
            }
        }
        return outStr;
    }

    public void mergeQuots() {
        String pureTextA = Util.mergeDoubleSingleQuots(text.toString());
        text = new StringBuffer(pureTextA);
    }

    public String[] splitSentences() {
        return splitSentences(text.toString(), PlainText.sentDelim);
    }

    static public String[] splitSentences(String text, String delim) {
        ArrayList sents = new ArrayList();
        String delimForTokenizer = "!|\"|\\.|\\?"; //| is not necessary here, added for check "tmp.matches(delimForTokenizer"
        if (text != null) {
            //List sentenceList = Arrays.asList(text.toString().split(delim));
            //ListIterator iterator = sentenceList.listIterator();
            StringTokenizer tokenizer = new StringTokenizer(text, delimForTokenizer, true);

            String sentence = null;
            String title_acronym = "";

            Pattern pSentenceHead = Pattern.compile("(<[.[^<>]]+>\\W?)*\\W*[A-Z\"].*");//sep27 2004, make it match single upper case letter as well.
            boolean acronym = false; //
            boolean openQuotation = true;

            while (tokenizer.hasMoreTokens()) {


                String tmp = new String(tokenizer.nextToken()).trim();
                if (false) {
                    System.err.println(tmp);
                    System.err.println("=" + title_acronym + "=");
                    System.err.println(acronym);
                }
                Matcher mSentenceHead = pSentenceHead.matcher(tmp);

                if (tmp.length() == 0) {
                    //skip spaces
                    continue;
                } else if (tmp.matches(delimForTokenizer)) {

                    if (acronym || (title_acronym.length() > 0)) {
                        title_acronym += tmp + " ";
                        if (!tokenizer.hasMoreTokens()) {
                            //hacked on Jan.30 2004, to couple with sentence like A is a B.
                            sents.add(title_acronym);
                        }
                    } else if (tmp.equals("\"") && openQuotation) {
                        openQuotation = !openQuotation;

                        //concatenate with previous part which doesn't end with a valid sentence deliminator
                        String previousSent = " ";
                        if (sents.size() > 0) {
                            previousSent = ((String) sents.get(sents.size() - 1)).trim();
                        }

                        String ending = previousSent.substring(previousSent.length() - 1);
                        if (!ending.matches(delimForTokenizer)) {
                            //The previous segment does not end with a legal sentence deliminator, concatenate
                            if (sents.size() > 0) {
                                sents.add(((String) sents.remove(sents.size() - 1)) +
                                        tmp + " ");
                            } else {
                                //Happens when the article starts with an openning quot
                                sents.add(tmp + " ");
                            }
                        } else {
                            title_acronym += tmp + " ";
                        }
                    } else {

                        if (tmp.equals("\"")) {
                            openQuotation = !openQuotation;
                        }
                        //it is possibly a closing punctuation
                        sents.add(((String) sents.remove(sents.size() - 1)) +
                                tmp + " ");
                    }
                    continue;
                }


                if ((!mSentenceHead.lookingAt()) //not a sentence head
                        && (title_acronym.length() == 0)
                        && (sents.size() > 0)) {
                    //tmp not start with capital letter, concatnate it with previous sentence added
                    if (TitleList.contains(tmp.substring(tmp.lastIndexOf(" ") +
                            1))) {
                        sentence = (String) sents.remove(sents.size() - 1) + " " + tmp;
                    } else {
                        sents.add(((String) sents.remove(sents.size() - 1)) + " " + tmp);
                        sentence = "";
                    }
                    //continue;
                } else if (acronym && mSentenceHead.lookingAt()) { // (acronym && tmp.matches("[A-Z\"].+"))
                    ////Purpose:
                    ////Previous sentence has an acronym in the end, put into sentences list
                    ////This ends the sentnece "... I.B.M. Another expert pointed out ...."
                    ////But there is a noticed bug case  "U.S. Congress"

                    ////Situation: conditions never satisfied. acronym never set true and literally never affects the program.
                    /** @todo Check whether a X.X.X Xxxx should be divided, according to statistics. Sep 27, 2004.*/

                    sents.add(title_acronym);
                    sentence = tmp;
                    acronym = false;
                } else {
                    //identified a title (Mr. ...) concatenate
                    sentence = (title_acronym + tmp).trim();
                }


                int lastWdIdx = sentence.lastIndexOf(" ") + 1;
                //Pattern p = Pattern.compile("([A-Z]\\.)+"); //To match acronym F.O.O.
                //Matcher m = p.matcher(sentence.substring(lastWdIdx));

                if (sentence.length() == 0) {
                    continue;
                }

                if (TitleList.contains(sentence.substring(lastWdIdx))) {
                    //find a title at the end of current candidate sentence, then the next segment should be concatenated to form a complete sentence.
                    title_acronym = sentence + "";
                    continue;
                } else if (sentence.substring(lastWdIdx).matches("([A-Z]\\.)+")) { //before sep 27, 2004, was/*(m.find() && (m.end() == (sentence.substring(lastWdIdx).length() - 1)))*/
                    //find an acronym here.
                    title_acronym = sentence + "";
                    acronym = true;
                    continue;
                } else {
                    title_acronym = "";
                }

                if (sentence.length() > 0) {
                    //todo: check if there is a opening quotation mark --> concatnate if necessary "A. B. C!" three sentences or one??
                    sents.add(sentence);
                }
            }

        }

        String[] out = new String[sents.size()];

        if (singleLine) {
            //remove newlines
            for (int i = 0; i < sents.size(); i++) {
                out[i] = ((String) (sents.get(i))).replaceAll("\n", " ");
            }
        } else {
            for (int i = 0; i < sents.size(); i++) {
                out[i] = (String) (sents.get(i));
            }
        }

        return out;
    }


    public void setSingleLine(boolean b) {
        singleLine = b;
    }


    public void removeTag() {
        String pureTextA = Util.removeTag(text.toString());
        String pureTextB = Util.removeBlankLine(pureTextA);
        String pureTextC = pureTextB.replaceAll("\\&\\w{1,8}\\;", "");
        text = new StringBuffer(pureTextC);
    }

    public void run() {
        System.out.println(addQuote(true));
    }

    public void run(boolean debug) {
        System.out.println(addQuote(debug));
    }


    public String toString() {
        return text.toString();
    }


}
