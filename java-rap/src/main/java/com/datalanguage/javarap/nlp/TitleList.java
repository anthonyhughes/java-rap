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

package com.datalanguage.javarap.nlp;

/**
 * <p>Title: NLP Tools</p>
 * <p>Description: A copy of external class to make JavaRAP standalone. </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: NUS</p>
 *
 * @author Qiu Long qiul@comp.nus.edu.sg
 * @version 1.0
 */

public class TitleList {

    //Following list is form mmunoz@uiuc.edu 's sentence spliter, augmented of course
    private final static String list = new String("A|Adj|Adm|Adv|Asst|B|Bart|Bldg|Brig|Bros|C|Capt"
            + "|Cmdr|Col|Comdr|Con|Cpl|D|DR|Dr|E|Ens|F|G|Gen|Gov|H|Hon|Hosp|I|Insp|J"
            + "|K|L|Lt|M|MM|MR|MRS|MS|Maj|Messrs|Mlle|Mme|Mr|Mrs|Ms|Msgr|Mt|N|No|O|Op|Ord"
            + "|P|Pfc|Ph|Prof|Pvt|Q|R|Rep|Reps|Res|Rev|Rt|S|Sen|Sens|Sfc|Sgt|Sr|St"
            + "|Supt|Surg|T|U|V|W|X|Y|Z|v|vs").toLowerCase();

    public TitleList() {
    }

    public static boolean contains(String word) {
        return list.indexOf(word.toLowerCase()) > -1 ? true : false;
    }
}
