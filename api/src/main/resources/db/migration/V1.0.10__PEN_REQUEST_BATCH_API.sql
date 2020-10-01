ALTER TABLE PEN_REQUEST_BATCH_STUDENT
    ADD (
        REPEAT_REQUEST_SEQUENCE_NUMBER NUMBER,
        REPEAT_REQUEST_ORIGINAL_ID RAW(16)
        );
COMMENT ON COLUMN PEN_REQUEST_BATCH_STUDENT.REPEAT_REQUEST_SEQUENCE_NUMBER IS 'For requests that are identical repeats of an earlier request, this indicates the position of this request in the sequence of repeating requests. The original request is not a repeat. The first repeat is seq number 1, the second is number 2...';
COMMENT ON COLUMN PEN_REQUEST_BATCH_STUDENT.REPEAT_REQUEST_ORIGINAL_ID IS 'Foreign key to another PEN Request Student record, in the event that this request is an identical repeat in a chain of repeated requests. This value will reference the original request that has since been repeated.';
