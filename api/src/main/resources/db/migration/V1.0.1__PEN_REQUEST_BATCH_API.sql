CREATE TABLE SCHOOL_GROUP_CODE
(
    SCHOOL_GROUP_CODE VARCHAR2(10)           NOT NULL,
    LABEL             VARCHAR2(30),
    DESCRIPTION       VARCHAR2(255),
    DISPLAY_ORDER     NUMBER DEFAULT 1       NOT NULL,
    EFFECTIVE_DATE    DATE                   NOT NULL,
    EXPIRY_DATE       DATE                   NOT NULL,
    CREATE_USER       VARCHAR2(32)           NOT NULL,
    CREATE_DATE       DATE   DEFAULT SYSDATE NOT NULL,
    UPDATE_USER       VARCHAR2(32)           NOT NULL,
    UPDATE_DATE       DATE   DEFAULT SYSDATE NOT NULL,
    CONSTRAINT SCHOOL_GROUP_CODE_PK PRIMARY KEY (SCHOOL_GROUP_CODE)
);

CREATE TABLE UNARCHIVED_BATCH_STATUS_CODE
(
    UNARCHIVED_BATCH_STATUS_CODE VARCHAR2(10)           NOT NULL,
    LABEL                        VARCHAR2(30),
    DESCRIPTION                  VARCHAR2(255),
    DISPLAY_ORDER                NUMBER DEFAULT 1       NOT NULL,
    EFFECTIVE_DATE               DATE                   NOT NULL,
    EXPIRY_DATE                  DATE                   NOT NULL,
    CREATE_USER                  VARCHAR2(32)           NOT NULL,
    CREATE_DATE                  DATE   DEFAULT SYSDATE NOT NULL,
    UPDATE_USER                  VARCHAR2(32)           NOT NULL,
    UPDATE_DATE                  DATE   DEFAULT SYSDATE NOT NULL,
    CONSTRAINT UNARCHIVED_BATCH_STATUS_CODE_PK PRIMARY KEY (UNARCHIVED_BATCH_STATUS_CODE)
);
--Modify Columns in STUDENT to lengthen names
ALTER TABLE PEN_REQUEST_BATCH RENAME COLUMN UNARCHIVED_FLAG TO UNARCHIVED_BATCH_STATUS_CODE;
ALTER TABLE PEN_REQUEST_BATCH
    ADD (
        SCHOOL_GROUP_CODE VARCHAR2(10),
        UNARCHIVED_USER varchar2(255)
        );
--Modify Columns in PEN_REQUEST_BATCH to make UNARCHIVED_BATCH_STATUS_CODE field 10
ALTER TABLE PEN_REQUEST_BATCH
    MODIFY (
        UNARCHIVED_BATCH_STATUS_CODE VARCHAR2(10)
        );
--Constraints
ALTER TABLE PEN_REQUEST_BATCH
    ADD CONSTRAINT PEN_REQUEST_BATCH_SCHOOL_GROUP_CODE_FK FOREIGN KEY (SCHOOL_GROUP_CODE) REFERENCES SCHOOL_GROUP_CODE (SCHOOL_GROUP_CODE);

ALTER TABLE PEN_REQUEST_BATCH
    ADD CONSTRAINT PEN_REQUEST_BATCH_UNARCHIVED_BATCH_STATUS_CODE_FK FOREIGN KEY (UNARCHIVED_BATCH_STATUS_CODE) REFERENCES UNARCHIVED_BATCH_STATUS_CODE (UNARCHIVED_BATCH_STATUS_CODE);

--SCHOOL_GROUP_CODE
INSERT INTO SCHOOL_GROUP_CODE (SCHOOL_GROUP_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                               EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('K12',
        'K-12',
        'Schools categorized as K-12. May include early learning schools.',
        1,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));

INSERT INTO SCHOOL_GROUP_CODE (SCHOOL_GROUP_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                               EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('PSI',
        'PSI',
        'Schools categorized as PSI, meaning Post Secondary Institution.',
        2,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));

--UNARCHIVED_BATCH_STATUS_CODE
INSERT INTO UNARCHIVED_BATCH_STATUS_CODE (UNARCHIVED_BATCH_STATUS_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER,
                                          EFFECTIVE_DATE,
                                          EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('NA',
        'Not Applicable',
        'Batch has not yet been archived or has been archived, but not yet unarchived.',
        1,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));

INSERT INTO UNARCHIVED_BATCH_STATUS_CODE (UNARCHIVED_BATCH_STATUS_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER,
                                          EFFECTIVE_DATE,
                                          EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('UNCHANGED',
        'Unchanged while unarchived',
        'Batch was unarchived but has not yet been edited/modified while unarchived.',
        2,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO UNARCHIVED_BATCH_STATUS_CODE (UNARCHIVED_BATCH_STATUS_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER,
                                          EFFECTIVE_DATE,
                                          EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('CHANGED',
        'Batch has been modified',
        'Batch was unarchived and then edited/modified while unarchived.',
        3,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));

INSERT INTO UNARCHIVED_BATCH_STATUS_CODE (UNARCHIVED_BATCH_STATUS_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER,
                                          EFFECTIVE_DATE,
                                          EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('REARCHIVED',
        'Was Unarchived then Rearchived',
        'Batch was Unarchived then Rearchived.',
        4,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));