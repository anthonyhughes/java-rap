package com.datalanguage.javarap.nlp;

import com.datalanguage.javarap.utils.Util;

import java.io.File;
import java.util.Hashtable;

/**
 * User: anthonyhughes
 * Date: 06/09/2014
 * Time: 15:59
 */
public class HumanList {
    final static String[] maleList = new String("he him himself his").split(" ");
    final static String[] femaleList = new String("she her herself").split(" ");
    final static String[] thirdPersonList = new String("he him himself his she her herself they them their themselves it its itself").split(" ");
    final static String[] secondPersonList = new String("you your yourself yourselves").split(" ");
    final static String[] firstPersonList = new String("i me my myself we us our ourselves").split(" ");
    final static String[] list = new String(
            "i me myself my we us ourselves our they them themselves their").split(" ");
    final static String[] pluralList = new String(
            "we us ourselves our they them themselves their").split(" ");
    final static String[] wholeList = new String("he him himself his she her herself"
            + " i me myself my we us ourselves our you your yourself").split(" ");
    final static String[] complementList = new String("it its itself").split(" ");
    final static String[] auxZList = new String("is does has was").split(" ");
    final static String[] titleList = new String("Mr. Mrs. Miss Ms.").split(" ");

    final static int numberOfNameToCheck = -1; //3000; //check only the first xx most common first names, respectively
    //  final static Hashtable maleNameTb = getNameTb(System.getProperty("dataPath") + File.separator +"male_first.txt",numberOfNameToCheck);
    final static Hashtable maleNameTb = getNameTb(System.getProperty("dataPath") + File.separator + "MostCommonMaleFirstNamesInUS.mongabay.txt", numberOfNameToCheck);
    final static Hashtable femaleNameTb = getNameTb(System.getProperty("dataPath") + File.separator + "female_first.txt", numberOfNameToCheck);
    final static Hashtable humanOccupationTb = getNameTb(System.getProperty("dataPath") + File.separator + "personTitle.txt");
    final static Hashtable lastNameTb = getNameTb(System.getProperty("dataPath") + File.separator + "name_last.txt");

    public HumanList() {
    }

    public static boolean isMale(String wd) {
        //People's name should start with a capital letter
        return contains(maleList, wd) || (wd.matches("[A-Z][a-z]*") && contains(maleNameTb, wd));
    }

    public static boolean isFemale(String wd) {
        //People's name should start with a capital letter
        return contains(femaleList, wd) || (wd.matches("[A-Z][a-z]*") && contains(femaleNameTb, wd));
    }

    public static boolean isHuman(String wd) {
        if (wd.indexOf(" ") > 0 && contains(titleList, wd.split(" ")[0], true)) {
            //contains more than a single word and starts with a title
            return true;
        }
        return contains(wholeList, wd)
                //|| contains((humanOccupationTb),wd)
                || isMale(wd) || isFemale(wd);
    }

    public static boolean isNotHuman(String wd) {
        return contains(complementList, wd);
    }

    public static boolean isPlural(String wd) {
        return contains(pluralList, wd);
    }

    public static boolean isThirdPerson(String wd) {
        return contains(thirdPersonList, wd);
    }

    public static boolean isSecondPerson(String wd) {
        return contains(secondPersonList, wd);
    }


    public static boolean isFirstPerson(String wd) {
        return contains(firstPersonList, wd);
    }

    public static boolean contains(String[] list, String str) {
        return contains(list, str, false);
    }

    public static boolean contains(String[] list, String str, boolean caseSensitive) {
        boolean contain = false;
        if (caseSensitive) { //make this a outer check for efficiency's sake
            for (int i = 0; i < list.length; i++) {
                if (list[i].equals(str)) {
                    contain = true;
                    break;
                }
            }
        } else {
            for (int i = 0; i < list.length; i++) {
                if (list[i].equalsIgnoreCase(str)) {
                    contain = true;
                    break;
                }
            }
        }
        return contain;
    }

    public static boolean contains(Hashtable tb, String wd) {
        return tb.containsKey(wd);
    }

    private static String[] retriveList(String listFile) {
        return Util.read(listFile).toString().split("\\s+");
    }

    private static Hashtable getNameTb(String listFile) {
        return getNameTb(listFile, -1);
    }

    private static Hashtable getNameTb(String listFile, int range) {
        String[] nameArray = retriveList(listFile);
        Hashtable tb = new Hashtable();

        if (nameArray.length <= 0) {
            System.err.println(listFile + " not found. Please download the latest data files. \n System quit.");
            System.exit(0);
        }

        if (nameArray != null) {
            int stopAt;
            if (range == -1) {
                stopAt = nameArray.length;
            } else {
                stopAt = Math.min(range, nameArray.length);
            }
            for (int i = 0; i < stopAt; i++) {
                String name = nameArray[i].substring(0, 1);
                if (nameArray[i].length() > 1) {
                    name += nameArray[i].substring(1).toLowerCase();
                }
                tb.put(name, name);
            }
        }
        return tb;
    }


}
