package com.datalanguage.javarap.model;

import com.datalanguage.javarap.utils.Util;

public class CorreferencialPair {
    TagWord referee;
    TagWord referer;

    public CorreferencialPair(String refereeRecord, String refererRecord) {
        //(sentenceIdx,offset) word
        if (refereeRecord.trim().equals("NULL")) {
            referee = null;
        } else {
            referee = new TagWord(refereeRecord.trim());
        }
        referer = new TagWord(refererRecord.trim());
    }

    public CorreferencialPair(TagWord referee, TagWord referer) {
        referee = referee;
        referer = referer;
    }

    public TagWord getReferer() {
        return referer;
    }

    public TagWord getReferee() {
        return referee;
    }

    public String toString() {
        return Util.processResult(referee, referer);
    }
}
