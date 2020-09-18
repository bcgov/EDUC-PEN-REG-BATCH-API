ALTER TABLE PEN_REQUEST_BATCH_STUDENT
    ADD (
        BEST_MATCH_PEN VARCHAR2(9)
        );

COMMENT ON COLUMN PEN_REQUEST_BATCH_STUDENT.BEST_MATCH_PEN IS 'The PEN assigned to an existing Student record which is the best match.';
