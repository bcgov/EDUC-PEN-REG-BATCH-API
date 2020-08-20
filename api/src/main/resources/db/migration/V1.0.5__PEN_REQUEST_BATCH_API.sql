--PEN_REQUEST_BATCH_EVENT_CODE
CREATE TABLE PEN_REQUEST_BATCH_EVENT_CODE
(
    PEN_REQUEST_BATCH_EVENT_CODE VARCHAR2(10)           NOT NULL,
    LABEL                        VARCHAR2(30),
    DESCRIPTION                  VARCHAR2(255),
    DISPLAY_ORDER                NUMBER DEFAULT 1       NOT NULL,
    EFFECTIVE_DATE               DATE                   NOT NULL,
    EXPIRY_DATE                  DATE                   NOT NULL,
    CREATE_USER                  VARCHAR2(32)           NOT NULL,
    CREATE_DATE                  DATE   DEFAULT SYSDATE NOT NULL,
    UPDATE_USER                  VARCHAR2(32)           NOT NULL,
    UPDATE_DATE                  DATE   DEFAULT SYSDATE NOT NULL,
    CONSTRAINT PEN_REQUEST_BATCH_EVENT_CODE_PK PRIMARY KEY (PEN_REQUEST_BATCH_EVENT_CODE)
);
COMMENT ON TABLE PEN_REQUEST_BATCH_EVENT_CODE IS 'Lists the possible events for PEN request batches.';
--PEN_REQUEST_BATCH_EVENT_CODE
INSERT INTO PEN_REQUEST_BATCH_EVENT_CODE (PEN_REQUEST_BATCH_EVENT_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER,
                                          EFFECTIVE_DATE,
                                          EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('STATUSCHG',
        'Status Change',
        'Status of the PEN Request Batch was updated.',
        1,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));

INSERT INTO PEN_REQUEST_BATCH_EVENT_CODE (PEN_REQUEST_BATCH_EVENT_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER,
                                          EFFECTIVE_DATE,
                                          EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('RETURNED',
        'Responses Returned',
        'Responses to the PEN Request Batch were returned to the school.',
        2,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));

--PEN Request Batch History
CREATE TABLE PEN_REQUEST_BATCH_HISTORY
(
    PEN_REQUEST_BATCH_HISTORY_ID  RAW(16)              NOT NULL,
    PEN_REQUEST_BATCH_ID          RAW(16)              NOT NULL,
    EVENT_DATE                    DATE                 NOT NULL,
    PEN_REQUEST_BATCH_EVENT_CODE  VARCHAR2(10)         NOT NULL,
    PEN_REQUEST_BATCH_STATUS_CODE VARCHAR2(10)         NOT NULL,
    EVENT_REASON                  VARCHAR2(255),
    CREATE_USER                   VARCHAR2(32)         NOT NULL,
    CREATE_DATE                   DATE DEFAULT SYSDATE NOT NULL,
    UPDATE_USER                   VARCHAR2(32)         NOT NULL,
    UPDATE_DATE                   DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT PEN_REQUEST_BATCH_HISTORY_PK PRIMARY KEY (PEN_REQUEST_BATCH_HISTORY_ID)
);
COMMENT ON TABLE PEN_REQUEST_BATCH_HISTORY IS 'Contains Each change the PEN REQUEST BATCH Table goes through.';

COMMENT ON COLUMN PEN_REQUEST_BATCH_HISTORY.PEN_REQUEST_BATCH_HISTORY_ID IS 'Unique surrogate key for each PEN Request Batch History record. GUID value must be provided during insert.';
COMMENT ON COLUMN PEN_REQUEST_BATCH_HISTORY.PEN_REQUEST_BATCH_ID IS 'Foreign key to the parent PEN Request Batch record.';
COMMENT ON COLUMN PEN_REQUEST_BATCH_HISTORY.EVENT_DATE IS 'Date and time at which the PEN Request Batch event occurred.';
COMMENT ON COLUMN PEN_REQUEST_BATCH_HISTORY.PEN_REQUEST_BATCH_EVENT_CODE IS 'Code identifying the type of event that occurred with the batch. Examples: Status Change, Responses returned to school.';
COMMENT ON COLUMN PEN_REQUEST_BATCH_HISTORY.PEN_REQUEST_BATCH_STATUS_CODE IS 'Code identifying the status of the batch after the event. This is provided for all event types, not just status changes. For status changes, it is the new value of the status, as a result of the change.';
COMMENT ON COLUMN PEN_REQUEST_BATCH_HISTORY.EVENT_REASON IS 'Additional information about the event, that is recorded only when needed.';

ALTER TABLE PEN_REQUEST_BATCH_HISTORY
    ADD CONSTRAINT PEN_REQUEST_BATCH_HISTORY_PEN_REQUEST_BATCH_ID_FK FOREIGN KEY (PEN_REQUEST_BATCH_ID) REFERENCES PEN_REQUEST_BATCH (PEN_REQUEST_BATCH_ID);
ALTER TABLE PEN_REQUEST_BATCH_HISTORY
    ADD CONSTRAINT PEN_REQUEST_BATCH_HISTORY_PEN_REQUEST_BATCH_EVENT_CODE_FK FOREIGN KEY (PEN_REQUEST_BATCH_EVENT_CODE) REFERENCES PEN_REQUEST_BATCH_EVENT_CODE (PEN_REQUEST_BATCH_EVENT_CODE);
ALTER TABLE PEN_REQUEST_BATCH_HISTORY
    ADD CONSTRAINT PEN_REQUEST_BATCH_HISTORY_PEN_REQUEST_BATCH_STATUS_CODE_FK FOREIGN KEY (PEN_REQUEST_BATCH_STATUS_CODE) REFERENCES PEN_REQUEST_BATCH_STATUS_CODE (PEN_REQUEST_BATCH_STATUS_CODE);