CREATE TABLE PEN_REQUEST_BATCH_SAGA
(
    SAGA_ID                      RAW(16)              NOT NULL,
    PEN_REQUEST_BATCH_STUDENT_ID RAW(16),
    PEN_REQUEST_BATCH_ID         RAW(16),
    SAGA_NAME                    VARCHAR2(50)         NOT NULL,
    SAGA_STATE                   VARCHAR2(100)        NOT NULL,
    PAYLOAD                      VARCHAR2(4000)       NOT NULL,
    STATUS                       VARCHAR2(20)         NOT NULL,
    CREATE_USER                  VARCHAR2(32)         NOT NULL,
    CREATE_DATE                  DATE DEFAULT SYSDATE NOT NULL,
    UPDATE_USER                  VARCHAR2(32)         NOT NULL,
    UPDATE_DATE                  DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT PEN_REQUEST_BATCH_SAGA_PK PRIMARY KEY (SAGA_ID)
);
CREATE INDEX PEN_REQUEST_BATCH_SAGA_STATUS_IDX ON PEN_REQUEST_BATCH_SAGA (STATUS);
CREATE INDEX PEN_REQUEST_BATCH_SAGA_PEN_REQUEST_BATCH_STUDENT_ID_IDX ON PEN_REQUEST_BATCH_SAGA (PEN_REQUEST_BATCH_STUDENT_ID);
CREATE INDEX PEN_REQUEST_BATCH_SAGA_PEN_REQUEST_BATCH_ID_IDX ON PEN_REQUEST_BATCH_SAGA (PEN_REQUEST_BATCH_ID);

CREATE TABLE PEN_REQUEST_BATCH_SAGA_EVENT_STATES
(
    SAGA_EVENT_ID       RAW(16)              NOT NULL,
    SAGA_ID             RAW(16)              NOT NULL,
    SAGA_EVENT_STATE    VARCHAR2(100)        NOT NULL,
    SAGA_EVENT_OUTCOME  VARCHAR2(100)        NOT NULL,
    SAGA_STEP_NUMBER    NUMBER(4)            NOT NULL,
    SAGA_EVENT_RESPONSE VARCHAR2(4000)       NOT NULL,
    CREATE_USER         VARCHAR2(32)         NOT NULL,
    CREATE_DATE         DATE DEFAULT SYSDATE NOT NULL,
    UPDATE_USER         VARCHAR2(32)         NOT NULL,
    UPDATE_DATE         DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT STUDENT_PROFILE_SAGA_EVENT_STATES_PK PRIMARY KEY (SAGA_EVENT_ID)
);
ALTER TABLE PEN_REQUEST_BATCH_SAGA_EVENT_STATES
    ADD CONSTRAINT PEN_REQUEST_BATCH_SAGA_EVENT_STATES_SAGA_ID_FK FOREIGN KEY (SAGA_ID) REFERENCES PEN_REQUEST_BATCH_SAGA (SAGA_ID);

CREATE TABLE MATCH_ALGORITHM_STATUS_CODE
(
    MATCH_ALGORITHM_STATUS_CODE VARCHAR2(10)           NOT NULL,
    LABEL                       VARCHAR2(30),
    DESCRIPTION                 VARCHAR2(255),
    DISPLAY_ORDER               NUMBER DEFAULT 1       NOT NULL,
    EFFECTIVE_DATE              DATE                   NOT NULL,
    EXPIRY_DATE                 DATE                   NOT NULL,
    CREATE_USER                 VARCHAR2(32)           NOT NULL,
    CREATE_DATE                 DATE   DEFAULT SYSDATE NOT NULL,
    UPDATE_USER                 VARCHAR2(32)           NOT NULL,
    UPDATE_DATE                 DATE   DEFAULT SYSDATE NOT NULL,
    CONSTRAINT MATCH_ALGORITHM_STATUS_CODE_PK PRIMARY KEY (MATCH_ALGORITHM_STATUS_CODE)
);
INSERT INTO MATCH_ALGORITHM_STATUS_CODE (MATCH_ALGORITHM_STATUS_CODE, LABEL, DESCRIPTION,
                                                   DISPLAY_ORDER, EFFECTIVE_DATE, EXPIRY_DATE, CREATE_USER, CREATE_DATE,
                                                   UPDATE_USER, UPDATE_DATE)
VALUES ('AA',
        'PEN is confirmed',
        'The Submitted PEN was valid and confirmed',
        1,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));

INSERT INTO MATCH_ALGORITHM_STATUS_CODE (MATCH_ALGORITHM_STATUS_CODE, LABEL, DESCRIPTION,
                                         DISPLAY_ORDER, EFFECTIVE_DATE, EXPIRY_DATE, CREATE_USER, CREATE_DATE,
                                         UPDATE_USER, UPDATE_DATE)
VALUES ('B0',
        'Valid CD but bad match',
        'Check digit is valid on the Submitted PEN, but PEN appears to be for a different student. No matches are found using points formula. "B0" is changed to "F1" (the one match being the supplied PEN)',
        2,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));

INSERT INTO MATCH_ALGORITHM_STATUS_CODE (MATCH_ALGORITHM_STATUS_CODE, LABEL, DESCRIPTION,
                                         DISPLAY_ORDER, EFFECTIVE_DATE, EXPIRY_DATE, CREATE_USER, CREATE_DATE,
                                         UPDATE_USER, UPDATE_DATE)
VALUES ('B1',
        'Valid CD but merged PEN',
        'Check digit is valid on the Submitted PEN, but PEN is merged. True (correct) PEN is returned as possible match.',
        3,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));

INSERT INTO MATCH_ALGORITHM_STATUS_CODE (MATCH_ALGORITHM_STATUS_CODE, LABEL, DESCRIPTION,
                                         DISPLAY_ORDER, EFFECTIVE_DATE, EXPIRY_DATE, CREATE_USER, CREATE_DATE,
                                         UPDATE_USER, UPDATE_DATE)
VALUES ('BM',
        'Valid CD but multiple matches',
        'Check digit is valid on the Submitted PEN, but PEN appears to be for another student. Multiple matches found using points formula.',
        4,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));

INSERT INTO MATCH_ALGORITHM_STATUS_CODE (MATCH_ALGORITHM_STATUS_CODE, LABEL, DESCRIPTION,
                                         DISPLAY_ORDER, EFFECTIVE_DATE, EXPIRY_DATE, CREATE_USER, CREATE_DATE,
                                         UPDATE_USER, UPDATE_DATE)
VALUES ('C0',
        'New PEN created',
        'Check digit is invalid on the Submitted PEN, or Submitted PEN is not on file. No matches are found using points formula. A brand new PEN is assigned.',
        5,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));

INSERT INTO MATCH_ALGORITHM_STATUS_CODE (MATCH_ALGORITHM_STATUS_CODE, LABEL, DESCRIPTION,
                                         DISPLAY_ORDER, EFFECTIVE_DATE, EXPIRY_DATE, CREATE_USER, CREATE_DATE,
                                         UPDATE_USER, UPDATE_DATE)
VALUES ('C1',
        'PEN bad but 1 match by points',
        'Check digit is invalid on the Submitted PEN, or PEN is not on file. One match is found using points formula.',
        6,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));

INSERT INTO MATCH_ALGORITHM_STATUS_CODE (MATCH_ALGORITHM_STATUS_CODE, LABEL, DESCRIPTION,
                                         DISPLAY_ORDER, EFFECTIVE_DATE, EXPIRY_DATE, CREATE_USER, CREATE_DATE,
                                         UPDATE_USER, UPDATE_DATE)
VALUES ('CM',
        'PEN bad; M matches by points',
        'Check digit is invalid on the Submitted PEN, or PEN is not on file.  Multiple matches were found using points formula.',
        7,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));

INSERT INTO MATCH_ALGORITHM_STATUS_CODE (MATCH_ALGORITHM_STATUS_CODE, LABEL, DESCRIPTION,
                                         DISPLAY_ORDER, EFFECTIVE_DATE, EXPIRY_DATE, CREATE_USER, CREATE_DATE,
                                         UPDATE_USER, UPDATE_DATE)
VALUES ('D0',
        'No match found',
        'No PEN was submitted. No matches found, brand new PEN assigned.',
        8,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));

INSERT INTO MATCH_ALGORITHM_STATUS_CODE (MATCH_ALGORITHM_STATUS_CODE, LABEL, DESCRIPTION,
                                         DISPLAY_ORDER, EFFECTIVE_DATE, EXPIRY_DATE, CREATE_USER, CREATE_DATE,
                                         UPDATE_USER, UPDATE_DATE)
VALUES ('D1',
        'Exactly one match found',
        'No PEN was submitted. One and only one match found.',
        9,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));

INSERT INTO MATCH_ALGORITHM_STATUS_CODE (MATCH_ALGORITHM_STATUS_CODE, LABEL, DESCRIPTION,
                                         DISPLAY_ORDER, EFFECTIVE_DATE, EXPIRY_DATE, CREATE_USER, CREATE_DATE,
                                         UPDATE_USER, UPDATE_DATE)
VALUES ('DM',
        'Multiple matches found',
        'No PEN was submitted. Multiple matches found using points formula.',
        10,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));

INSERT INTO MATCH_ALGORITHM_STATUS_CODE (MATCH_ALGORITHM_STATUS_CODE, LABEL, DESCRIPTION,
                                         DISPLAY_ORDER, EFFECTIVE_DATE, EXPIRY_DATE, CREATE_USER, CREATE_DATE,
                                         UPDATE_USER, UPDATE_DATE)
VALUES ('F1',
        'One questionable match',
        'One questionable match found. A PEN may or may not have been submitted.',
        11,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));

INSERT INTO MATCH_ALGORITHM_STATUS_CODE (MATCH_ALGORITHM_STATUS_CODE, LABEL, DESCRIPTION,
                                         DISPLAY_ORDER, EFFECTIVE_DATE, EXPIRY_DATE, CREATE_USER, CREATE_DATE,
                                         UPDATE_USER, UPDATE_DATE)
VALUES ('G0',
        'Insufficient demog data',
        'No PEN was submitted. Insufficient demographic data to perform pen processing when in update mode and the pen process has returned a "D0" code.',
        12,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));

ALTER TABLE PEN_REQUEST_BATCH_STUDENT
    ADD (
        MATCH_ALGORITHM_STATUS_CODE VARCHAR2(10),
        QUESTIONABLE_MATCH_STUDENT_ID RAW(16),
        INFO_REQUEST VARCHAR2(4000),
        RECORD_NUMBER NUMBER
        );

ALTER TABLE PEN_REQUEST_BATCH_STUDENT
    ADD CONSTRAINT PEN_REQUEST_BATCH_STUDENT_MATCH_ALGORITHM_STATUS_CODE_FK FOREIGN KEY (MATCH_ALGORITHM_STATUS_CODE) REFERENCES MATCH_ALGORITHM_STATUS_CODE (MATCH_ALGORITHM_STATUS_CODE);
