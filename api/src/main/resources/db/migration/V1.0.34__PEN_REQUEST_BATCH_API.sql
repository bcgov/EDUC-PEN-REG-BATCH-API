ALTER TABLE PEN_REQUEST_BATCH_SAGA_EVENT_STATES
    ADD CONSTRAINT SI_SES_SEO_SSN_UK UNIQUE (SAGA_ID, SAGA_EVENT_STATE, SAGA_EVENT_OUTCOME, SAGA_STEP_NUMBER);

CREATE INDEX PRB_SAGA_CREATE_DATE_IDX ON API_PEN_REQUEST_BATCH.PEN_REQUEST_BATCH_SAGA (CREATE_DATE) TABLESPACE API_PEN_IDX;