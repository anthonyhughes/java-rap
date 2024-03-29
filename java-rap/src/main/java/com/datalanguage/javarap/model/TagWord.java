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

package com.datalanguage.javarap.model;

import com.datalanguage.javarap.nlp.HumanList;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Title: Anaphora Resolution</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2011</p>
 *
 * @author Qiu Long
 * @version 1.0
 * @contributor Anthony Hughes
 * @history Feb 08, 2011
 * @update September 08, 2014
 * <p/>
 * To getGender() given a person name, first check against the 300 most common US male
 * names (by the 1990 census). If no match is found, then move on to a list of known female names.
 * Previously, a more exhaustive male name list is first checked and regardless of the result the
 * above mentioned female name list is checked. In this case, names such as John are potentially taken
 * as female as they do appear in the female name list. This is not absolutely a mistake but in most of
 * the cases it tend to be.
 */

public class TagWord {
    private String tag;
    private int sIdx; //indicates sentence
    private String word;

    int depth = 0; //depth in the parse tree;
    int offset; //offset of the word as a whole in the sentence
    int number = 1; //0:single, 1:unclear, 2:plural
    int gender = 1; //0 for Male, 2 for Female and 1 for unclear
    int human = 1; //0 for human, 2 for none-human 1 for unclear
    int people = 0;//o for unclear, 1 for first, 2 for second and 3 for third
    boolean pleonastic = false; //represents a pleonastic pronoun

    boolean isHeadNP = false;
    boolean hasNPAncestor = false;
    DefaultMutableTreeNode head = null; //reference to the head for this NP
    DefaultMutableTreeNode argumentHead = null; //the head as this NP is augument for
    DefaultMutableTreeNode argumentHost = null; //the other np as augument for the same head
    DefaultMutableTreeNode adjunctHost = null; //the unit as adjunct for
    DefaultMutableTreeNode NPDomainHost = null;
    DefaultMutableTreeNode determiner = null;
    DefaultMutableTreeNode determinee = null;
    Vector containHost = new Vector();
    NP npRepresentation = null;
    TagWord antecedent = null;

    //the dynamically updated salience value
    int tmpSalience = 0;

    public TagWord(String tag, String word, int s, int offset) {
        this.tag = tag;
        this.setWord(word);
        this.sIdx = s;
        this.offset = offset;
    }

    public TagWord(String record) {
        //record is in the format of
        //(sentenceIdx,offset) word
        Pattern p = Pattern.compile("\\((\\d+),(\\d+)\\)(.+)");
        Matcher m = null;
        m = p.matcher(record);
        if (m.find()) {
            String sentIdxStr = m.group(1);
            String offsetStr = m.group(2);
            String content = m.group(3);
            this.tag = "UNKNOWN";
            this.setWord(content.trim());
            this.sIdx = Integer.parseInt(sentIdxStr);
            this.offset = Integer.parseInt(offsetStr);
        } else {
            System.err.println("Format error in: \"" + record + "\"");
        }

    }

    /**
     * combinedStr : (Tag content), where content could be also a combinedStr
     */
    public TagWord(String combinedStr, int s, int offset) {
        this.sIdx = s;
        this.offset = offset;
        try {
            this.tag = combinedStr.substring(combinedStr.indexOf("(") + 1,
                    combinedStr.indexOf(" "));
            this.setWord(combinedStr.substring(combinedStr.indexOf(" ") + 1,
                    combinedStr.lastIndexOf(")")));
        } catch (Exception ex) {
            combinedStr = "(-LRB- -LRB-)";//dummy element
            this.tag = combinedStr.substring(combinedStr.indexOf("(") + 1,
                    combinedStr.indexOf(" "));
            this.setWord(combinedStr.substring(combinedStr.indexOf(" ") + 1,
                    combinedStr.lastIndexOf(")")));
        }
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getOffset() {
        return this.offset;
    }

    public int getSentenceIdx() {
        return this.sIdx;
    }

    public void setNP(NP n) {
        this.npRepresentation = n;
    }

    public void setNumber(int n) {
        //0:single, 1:unclear, 2:plural
        //refer to getNumber
        this.number = n;
    }


    public int getNumber() {
        if (this.number != 1) {
            return this.number;
        }
        String tag = "";
        if (this.npRepresentation.tagWord.size() == 1) {
            tag = ((TagWord) this.npRepresentation.tagWord.firstElement()).getTag();
            if (tag.endsWith("S")) { //NNS, NPS
                this.number = 2;
            } else if (HumanList.isPlural(getText())) {
                this.number = 2;
            } else {
                this.number = 0;
            }
        } else {
            if (getNPRepresentation().hasAnd()) {
                this.number = 2;
            } else if (this.head != null) {
                this.number = ((TagWord) head.getUserObject()).getNumber();
            }
        }
        return this.number;
    }

    public void setDepth(int d) {
        this.depth = d;
    }

    public int getDepth() {
        return this.depth;
    }

    public NP getNPRepresentation() {
        return this.npRepresentation;
    }

    /**
     * @return 0 for Male, 2 for Female and 1 for unclear
     */
    public int getGender() {
        if (this.gender != 1) {
            return this.gender;
        }
        String h;
        if (head == null) {
            h = getWord(); //for prp
        } else {
            TagWord tw = (TagWord) (head.getUserObject());
            h = tw.getContent();
        }

        if (HumanList.isMale(h)) {
            this.gender = 0;
        } else //Added on Feb 08, 2011
            if (HumanList.isFemale(h)) {
                this.gender = 2;
            }
        return gender;
    }


    public int getPronounIdx() {

        if (people != 0) {
            return people;
        }
        String h;
        if (head == null) {
            h = getWord(); //for prp,
        } else {
            TagWord tw = (TagWord) (head.getUserObject());
            h = tw.getContent();
        }

        if (HumanList.isThirdPerson(h)) {
            return people = 3;
        } else if (HumanList.isSecondPerson(h)) {
            return people = 2;
        } else if (HumanList.isFirstPerson(h)) {
            return people = 1;
        } else {
            return 0;
        }
    }

    /**
     * @return 0 for human, 2 for none-human and 1 for unclear
     */
    public int getHumanIdx() {
        if (human != 1) {
            return human;
        }

        if (gender != 1) {
            human = 0;
            return human;
        }

        String h;
        //check the content of this NP as the first attempt
        h = getContent();
        if (HumanList.isHuman(h)) {
            human = 0;
            return human;
        }

        //If above fails, check the head of this NP
        if (head != null) {
            TagWord tw = (TagWord) (head.getUserObject());
            h = tw.getContent();
        }
        if (HumanList.isHuman(h)) {
            this.human = 0;
        } else if (HumanList.isNotHuman(h)) {/****para****/
            this.human = 2;
        }

        return human;
    }

    public void setPeople(int i) {
        people = i;
    }

    public int getPeople() {
        if (people != 0) {
            return people;
        }

        if (this.getContent().toLowerCase().matches("we|us")) {
            return people = 1;
        }

        if (this.getContent().toLowerCase().matches("you")) {
            return people = 2;
        }

        return people = 3; //default

    }


    public String getTag() {
        return this.tag;
    }

    public boolean isPRP() {
        return this.getNPRepresentation().isPRP();
    }

    public String getText() {
        return getContent();
    }

    public void setHead(DefaultMutableTreeNode n) {
        if (this.number != 1) {
            //number should be set afterword
            System.err.println("Number shouldn't be set before setHead.");
        }
        this.head = n;
    }

    public DefaultMutableTreeNode getHead() {
        return this.head;
    }


    public void setDeterminer(DefaultMutableTreeNode n) {
        determiner = n;
    }

    public DefaultMutableTreeNode getDeterminer() {
        return determiner;
    }

    public void setDeterminee(DefaultMutableTreeNode n) {
        determinee = n;
    }

    public DefaultMutableTreeNode getDeterminee() {
        return determinee;
    }

    /**
     * @param defaultMutableTreeNode: the NP in the same argument domain
     */
    public void setArgumentHost(DefaultMutableTreeNode defaultMutableTreeNode) {
        this.argumentHost = defaultMutableTreeNode;
    }

    public DefaultMutableTreeNode getArgumentHost() {
        return this.argumentHost;
    }

    public void setArgumentHead(DefaultMutableTreeNode n) {
        this.argumentHead = n;
    }

    public DefaultMutableTreeNode getArgumentHead() {
        return this.argumentHead;
    }

    public void setAdjunctHost(DefaultMutableTreeNode n) {
        this.adjunctHost = n;
    }

    public DefaultMutableTreeNode getAdjunctHost() {
        return this.adjunctHost;
    }

    public void setNPDomainHost(DefaultMutableTreeNode n) {
        this.NPDomainHost = n;
    }

    public DefaultMutableTreeNode getNPDomainHost() {
        return this.NPDomainHost;
    }

    public void setContainHost(DefaultMutableTreeNode n) {
        this.containHost.add(n);
    }

    public void setContainHost(Vector n) {
        this.containHost.addAll(n);
    }

    public java.util.Vector getContainHost() {
        return this.containHost;
    }

    public void setPleonastic(boolean b) {
        this.pleonastic = b;
    }

    public boolean isPleonastic() {
        return this.pleonastic;
    }


    /**
     * @param npAlien The np that salience weight of this TagWord is considered for.
     */
    public int getSalience(NP npAlien) {

        int sal = 0;
        NP np = this.getNPRepresentation();
        if ((np != null) && (npAlien != null)) {
            sal = np.getSalience(npAlien);
        }
        //dampen the salience as distance increases
        sal = sal / (Math.abs(this.getSentenceIdx() - npAlien.getSentenceIdx()) + 1);

        //penalize cataphora (if this appears after npAlien)
        if ((this.getSentenceIdx() == npAlien.getSentenceIdx()
                && this.getNPRepresentation().getOffset() > npAlien.getOffset())
                || this.getSentenceIdx() > npAlien.getSentenceIdx()) {
            sal = sal / 4; //reduce the weight substantially
        }
        return sal;
    }

    /**
     * @return true if thsi NP is not contained in another NP
     */
    public boolean isHeadNP() {
        if (argumentHead != null) {
            return false;
        }

        if (adjunctHost != null) {
            return false;
        }

        if (hasNPAncestor) {
            return false;
        }
        isHeadNP = true;
        return isHeadNP;
    }

    public boolean hasNPAncestor() {
        return hasNPAncestor;
    }

    public void setHasNPAncestor(boolean b) {
        hasNPAncestor = b;
    }

    /**
     * merge all salience factors true for tw, salience factors for tw remain unchanged
     *
     * @param tw
     */
    public void mergeSalience(TagWord tw) {
        //In theory: merge salience factors for members in a equvalent class (coreferencial chain)
        //In fact: accumulate salience factors in the chain, a member in the chain has all the factors processed by the leading members

        NP np = this.getNPRepresentation();
        NP npGuest = tw.getNPRepresentation();

        if (np != null && npGuest != null) {
            np.mergeSalience(npGuest);
        }
    }


    public int getSalience(TagWord tw) {
        return getSalience(tw.getNPRepresentation());
    }

    public void setTmpSalience(int s) {
        this.tmpSalience = s;
    }

    public int getTmpSalience() {
        return this.tmpSalience;
    }

    /**
     * amplify sentence index difference by multiply 100
     */
    public int distanceInText(TagWord tw) {
        return Math.abs(this.getSentenceIdx() - tw.getSentenceIdx()) * 100
                + Math.abs(this.getOffset() - tw.getOffset());
    }

    public String getContent() {

        String w = getWord();
        if (getWord().indexOf(")") == -1) {
            //word = " something"
            return getWord();
        }
        int pointerR = getWord().indexOf(")");
        int pointerL = getWord().substring(0, pointerR).lastIndexOf(" ");
        int spaceAfter;
        String text = getWord().substring(pointerL + 1, pointerR);
        //check for the leading "("
        if (text.endsWith("-LRB-")) {
            text = "(";
        } else if (text.endsWith("-RRB-")) {
            text = ")";
        }


        while (((spaceAfter = getWord().indexOf(" ", pointerR)) != -1)
                && ((pointerR = getWord().indexOf(")", spaceAfter)) != -1)) {
            pointerL = getWord().substring(0, pointerR).lastIndexOf(" ");
            String tmp = " " + getWord().substring(pointerL + 1, pointerR);
            if (tmp.endsWith("-LRB-")) {
                tmp = " (";
            } else if (tmp.endsWith("-RRB-")) {
                tmp = " )";
            }

            text += tmp;
        }
        return text;
    }

    public String getSubstitutedContent() {
        return this.getAntecedent().getContent();
    }

    public void setAntecedent(TagWord ant) {
        if (ant.getAntecedent() == this) {
            return;
        }
        antecedent = ant;
    }

    /**
     * @return the anaphoric antecedent of the TagWord, if there is one.
     *         Itself is returned otherwise.
     */
    public TagWord getAntecedent() {
        if (antecedent == null) {
            return this;
        } else {
            return this.antecedent.getAntecedent();
        }
    }

    public String toStringBrief() {
        return "(" + sIdx + "," + offset + ") " + getContent();
    }

    public String toString() {
        String localhead = " NULL";
        if (head != null) {
            localhead = ((TagWord) (head.getUserObject())).getContent();
        }

        String argHStr = " NULL";
        if (this.argumentHost != null) {
            argHStr = " (ARG " +
                    ((TagWord) (this.argumentHost.getUserObject())).getContent() + ")";
        }

        String adjHStr = " NULL";
        if (this.adjunctHost != null) {
            adjHStr = " (ADJ " +
                    ((TagWord) (this.adjunctHost.getUserObject())).getContent() + ")";
        }

        String NPDHStr = " NULL";
        if (this.NPDomainHost != null) {
            NPDHStr = " (NPDomain " +
                    ((TagWord) (this.NPDomainHost.getUserObject())).getContent() + ")";
        }

        String argHeadStr = " NULL";
        if (this.argumentHead != null) {
            NPDHStr = " (ARGHead " +
                    ((TagWord) (this.argumentHead.getUserObject())).getContent() + ")";
        }

        String containHostStr = " NULL";
        if (this.containHost.size() > 0) {
            containHostStr = " (containHost ";
            for (int i = 0; i < containHost.size(); i++) {
                containHostStr +=
                        ((TagWord) ((DefaultMutableTreeNode) containHost.get(i)).
                                getUserObject()).getContent() + "/";
            }
            containHostStr += ") ";
        }

        if (this.tag.startsWith("NP")
                || this.tag.startsWith("PP")
                || this.tag.startsWith("VP")) {
            localhead = " (HEAD " + localhead + ")";
        } else {
            localhead = "";
        }

        String npShow;
        NP np = this.getNPRepresentation();
        if (np != null) {
            npShow = np.toString();
        } else {
            npShow = "no NP";
        }

        return offset + " in " + sIdx + "         " + tag + " " + getContent()
                + " <NUMBER> " + this.number
                + localhead
                + argHStr
                + argHeadStr
                + adjHStr
                + NPDHStr
                + containHostStr
                + "\t " + npShow;

    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }
}

