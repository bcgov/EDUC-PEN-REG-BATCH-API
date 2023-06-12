package ca.bc.gov.educ.penreg.api.constants;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public enum StudentDemogCode {
    CONFIRMED("C");

    /**
     * The constant codeMap.
     */
    private static final Map<String, StudentDemogCode> codeMap = new HashMap<>();

    static {
        for (StudentDemogCode status: values()) {
            codeMap.put(status.getCode(), status);
        }
    }

    /**
     * The Code.
     */
    private final String code;

    /**
     * Instantiates a new student demog code codes.
     *
     * @param code the code
     */
    StudentDemogCode(String code) {
        this.code = code;
    }

    /**
     * To string string.
     *
     * @return the string
     */
    @Override
    public String toString(){
        return this.getCode();
    }

    /**
     * Value of code student demog code codes.
     *
     * @param code the code
     * @return the student demog code codes
     */
    public static StudentDemogCode valueOfCode(String code) {
        return codeMap.get(code);
    }
}
