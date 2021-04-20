package ca.bc.gov.educ.penreg.api.util;

import ca.bc.gov.educ.penreg.api.constants.BadLocalID;

import java.util.EnumSet;


public class LocalIDUtil {

    /**
     * Instantiates a new local id util.
     */
    private LocalIDUtil() {
    }

    // if localID is one of the BadLocalID enum value, change it to null
    public static String changeBadLocalID(String localID){
        for (BadLocalID info : EnumSet.allOf(BadLocalID.class)) {
            if(info.getLabel().equals(localID)) {
                return null;
            }
        }
        return localID;

    }
}
