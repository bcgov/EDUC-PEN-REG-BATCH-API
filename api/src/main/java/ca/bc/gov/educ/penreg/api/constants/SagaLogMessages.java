package ca.bc.gov.educ.penreg.api.constants;

import lombok.Getter;

public enum SagaLogMessages {
    NO_RECORD_SAGA_ID_EVENT_TYPE("no record found for the saga id and event type combination, processing."),
    RECORD_FOUND_FOR_SAGA_ID_EVENT_TYPE("record found for the saga id and event type combination, might be a duplicate or replay," +
            " just updating the db status so that it will be polled and sent back again."),
    EVENT_PAYLOAD("event is :: {}");

    /**
     * The Message.
     */
    @Getter
    private final String message;

    /**
     * Instantiates a new sage log message.
     *
     * @param message the message
     */
    SagaLogMessages(String message) {
        this.message = message;
    }
}
