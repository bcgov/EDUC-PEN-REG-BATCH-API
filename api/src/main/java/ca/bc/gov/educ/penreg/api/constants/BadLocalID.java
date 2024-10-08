package ca.bc.gov.educ.penreg.api.constants;

import lombok.Getter;

/**
 * The enum Bad Local ID.
 */
@Getter
public enum BadLocalID {
    /**
     * BAD Local ID.
     */
    NA("NA"),
    N_HASHTAG_A("N#A"),
    N_FORWARD_SLASH_A("N/A");

    /**
     * The label.
     */
    private final String label;

    /**
     * Instantiates a new bad local id label.
     *
     * @param label the label
     */
    BadLocalID(String label) {
        this.label = label;
    }
}
