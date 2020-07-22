CREATE TABLE STUDENT_GENDER_CODE
(
    GENDER_CODE    VARCHAR2(10)           NOT NULL,
    LABEL          VARCHAR2(30),
    DESCRIPTION    VARCHAR2(255),
    DISPLAY_ORDER  NUMBER DEFAULT 1       NOT NULL,
    EFFECTIVE_DATE DATE                   NOT NULL,
    EXPIRY_DATE    DATE                   NOT NULL,
    CREATE_USER    VARCHAR2(32)           NOT NULL,
    CREATE_DATE    DATE   DEFAULT SYSDATE NOT NULL,
    UPDATE_USER    VARCHAR2(32)           NOT NULL,
    UPDATE_DATE    DATE   DEFAULT SYSDATE NOT NULL,
    CONSTRAINT STUDENT_GENDER_CODE_PK PRIMARY KEY (GENDER_CODE)
);
COMMENT ON TABLE STUDENT_GENDER_CODE IS 'Gender Code lists the standard codes for Gender: Female, Male, Diverse.';

CREATE TABLE STUDENT_SEX_CODE
(
    SEX_CODE       VARCHAR2(10)           NOT NULL,
    LABEL          VARCHAR2(30),
    DESCRIPTION    VARCHAR2(255),
    DISPLAY_ORDER  NUMBER DEFAULT 1       NOT NULL,
    EFFECTIVE_DATE DATE                   NOT NULL,
    EXPIRY_DATE    DATE                   NOT NULL,
    CREATE_USER    VARCHAR2(32)           NOT NULL,
    CREATE_DATE    DATE   DEFAULT SYSDATE NOT NULL,
    UPDATE_USER    VARCHAR2(32)           NOT NULL,
    UPDATE_DATE    DATE   DEFAULT SYSDATE NOT NULL,
    CONSTRAINT STUDENT_SEX_CODE_PK PRIMARY KEY (SEX_CODE)
);
COMMENT ON TABLE STUDENT_SEX_CODE IS 'Sex Code lists the standard codes for Sex: Female, Male, Intersex.';

CREATE TABLE STUDENT
(
    STUDENT_ID         RAW(16)              NOT NULL,
    PEN                VARCHAR2(9)          NOT NULL
        CONSTRAINT PEN_UNIQUE UNIQUE,
    LEGAL_FIRST_NAME   VARCHAR2(40),
    LEGAL_MIDDLE_NAMES VARCHAR2(255),
    LEGAL_LAST_NAME    VARCHAR2(40)         NOT NULL,
    DOB                DATE                 NOT NULL,
    SEX_CODE           VARCHAR2(1),
    GENDER_CODE        VARCHAR2(1),
    USUAL_FIRST_NAME   VARCHAR2(40),
    USUAL_MIDDLE_NAMES VARCHAR2(255),
    USUAL_LAST_NAME    VARCHAR2(40),
    EMAIL              VARCHAR2(255),
    DECEASED_DATE      DATE,
    CREATE_USER        VARCHAR2(32)         NOT NULL,
    CREATE_DATE        DATE DEFAULT SYSDATE NOT NULL,
    UPDATE_USER        VARCHAR2(32)         NOT NULL,
    UPDATE_DATE        DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT STUDENT_PK PRIMARY KEY (STUDENT_ID)
);
COMMENT ON TABLE STUDENT IS 'Student contains core identifying data for students, include PEN, names, DOB, sex, etc.';
-- Column Comments
COMMENT ON COLUMN STUDENT.Student_ID IS 'Unique surrogate key for each Student. GUID value must be provided during insert.';
COMMENT ON COLUMN STUDENT.PEN IS 'Provincial Education Number assigned by system to this student, in SIN format; used to track a student all through their educational career. ';
COMMENT ON COLUMN STUDENT.LEGAL_FIRST_NAME IS 'The legal first name of the student';
COMMENT ON COLUMN STUDENT.LEGAL_MIDDLE_NAMES IS 'The legal middle names of the student';
COMMENT ON COLUMN STUDENT.LEGAL_LAST_NAME IS 'The legal last name of the student';
COMMENT ON COLUMN STUDENT.DOB IS 'The date of birth of the student';
COMMENT ON COLUMN STUDENT.SEX_CODE IS 'The sex of the student';
COMMENT ON COLUMN STUDENT.GENDER_CODE IS 'The gender of the student';
COMMENT ON COLUMN STUDENT.USUAL_FIRST_NAME IS 'The usual/preferred first name of the student';
COMMENT ON COLUMN STUDENT.USUAL_MIDDLE_NAMES IS 'The usual/preferred middle name of the student';
COMMENT ON COLUMN STUDENT.USUAL_LAST_NAME IS 'The usual/preferred last name of the student';
COMMENT ON COLUMN STUDENT.EMAIL IS 'The email address of the student';
COMMENT ON COLUMN STUDENT.DECEASED_DATE IS 'The date of death for the student. Will be known to EDUC only if student was an active student at the time.';

ALTER TABLE STUDENT
    ADD CONSTRAINT FK_STUDENT_STUDENT_GENDER_CODE FOREIGN KEY (GENDER_CODE) REFERENCES STUDENT_GENDER_CODE (GENDER_CODE);

ALTER TABLE STUDENT
    ADD CONSTRAINT FK_STUDENT_STUDENT_SEX_CODE FOREIGN KEY (SEX_CODE) REFERENCES STUDENT_SEX_CODE (SEX_CODE);

--Student Sex code
INSERT INTO STUDENT_SEX_CODE (SEX_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE, EXPIRY_DATE,
                              CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('F', 'Female', 'Persons who are of female sex as assigned at birth.', 1, to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'), 'IDIR/GRCHWELO', to_date('2019-11-07', 'YYYY-MM-DD'), 'IDIR/GRCHWELO',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO STUDENT_SEX_CODE (SEX_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE, EXPIRY_DATE,
                              CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('M', 'Male', 'Persons who were reported as being of male sex as assigned at birth.', 2,
        to_date('2020-01-01', 'YYYY-MM-DD'), to_date('2099-12-31', 'YYYY-MM-DD'), 'IDIR/GRCHWELO',
        to_date('2019-11-07', 'YYYY-MM-DD'), 'IDIR/GRCHWELO', to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO STUDENT_SEX_CODE (SEX_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE, EXPIRY_DATE,
                              CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('I', 'Intersex',
        'Persons who are intersex. Intersex people are born with any of several variations in sex characteristics, including chromosomes, gonads, sex hormones, or genitals that do not fit with typical conceptions of "male" or "female" bodies.',
        3, to_date('2020-01-01', 'YYYY-MM-DD'), to_date('2099-12-31', 'YYYY-MM-DD'), 'IDIR/GRCHWELO',
        to_date('2019-11-07', 'YYYY-MM-DD'), 'IDIR/GRCHWELO', to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO STUDENT_SEX_CODE (SEX_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE, EXPIRY_DATE,
                              CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('U', 'Unknown',
        'Persons whose sex is not known at the time of data collection. It may or may not get updated at a later point in time.',
        4, to_date('2020-01-01', 'YYYY-MM-DD'), to_date('2099-12-31', 'YYYY-MM-DD'), 'IDIR/GRCHWELO',
        to_date('2019-11-07', 'YYYY-MM-DD'), 'IDIR/GRCHWELO', to_date('2019-11-07', 'YYYY-MM-DD'));


-- Student Gender Code
INSERT INTO STUDENT_GENDER_CODE (GENDER_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE, EXPIRY_DATE,
                                 CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('F', 'Female',
        'Persons whose current gender is female. This includes cisgender and transgender persons who are female.', 1,
        to_date('2020-01-01', 'YYYY-MM-DD'), to_date('2099-12-31', 'YYYY-MM-DD'), 'IDIR/GRCHWELO',
        to_date('2019-11-07', 'YYYY-MM-DD'), 'IDIR/GRCHWELO', to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO STUDENT_GENDER_CODE (GENDER_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE, EXPIRY_DATE,
                                 CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('M', 'Male',
        'Persons whose current gender is male. This includes cisgender and transgender persons who are male.', 2,
        to_date('2020-01-01', 'YYYY-MM-DD'), to_date('2099-12-31', 'YYYY-MM-DD'), 'IDIR/GRCHWELO',
        to_date('2019-11-07', 'YYYY-MM-DD'), 'IDIR/GRCHWELO', to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO STUDENT_GENDER_CODE (GENDER_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE, EXPIRY_DATE,
                                 CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('X', 'Gender Diverse',
        'Persons whose current gender is not exclusively as male or female. It includes people who do not have one gender, have no gender, are non-binary, or are Two-Spirit.',
        3, to_date('2020-01-01', 'YYYY-MM-DD'), to_date('2099-12-31', 'YYYY-MM-DD'), 'IDIR/GRCHWELO',
        to_date('2019-11-07', 'YYYY-MM-DD'), 'IDIR/GRCHWELO', to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO STUDENT_GENDER_CODE (GENDER_CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE, EXPIRY_DATE,
                                 CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('U', 'Unknown',
        'Persons whose gender is not known at the time of data collection. It may or may not get updated at a later point in time. X is different than U.',
        4, to_date('2020-01-01', 'YYYY-MM-DD'), to_date('2099-12-31', 'YYYY-MM-DD'), 'IDIR/GRCHWELO',
        to_date('2019-11-07', 'YYYY-MM-DD'), 'IDIR/GRCHWELO', to_date('2019-11-07', 'YYYY-MM-DD'));

ALTER TABLE API_STUDENT.STUDENT RENAME CONSTRAINT FK_STUDENT_STUDENT_GENDER_CODE TO STUDENT_STUDENT_GENDER_CODE_FK;

ALTER TABLE API_STUDENT.STUDENT RENAME CONSTRAINT FK_STUDENT_STUDENT_SEX_CODE TO STUDENT_STUDENT_SEX_CODE_FK;

ALTER INDEX API_STUDENT.STUDENT_PK REBUILD LOGGING NOREVERSE TABLESPACE API_STUDENT_IDX NOCOMPRESS;

ALTER INDEX API_STUDENT.STUDENT_GENDER_CODE_PK REBUILD LOGGING NOREVERSE TABLESPACE API_STUDENT_IDX NOCOMPRESS;

ALTER INDEX API_STUDENT.STUDENT_SEX_CODE_PK REBUILD LOGGING NOREVERSE TABLESPACE API_STUDENT_IDX NOCOMPRESS;

ALTER TABLE API_STUDENT.STUDENT RENAME CONSTRAINT PEN_UNIQUE TO PEN_UK;

ALTER INDEX API_STUDENT.PEN_UNIQUE rename to PEN_UK;

ALTER INDEX API_STUDENT.PEN_UK REBUILD LOGGING NOREVERSE TABLESPACE API_STUDENT_IDX NOCOMPRESS;