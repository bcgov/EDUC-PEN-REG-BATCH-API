CREATE TABLE PEN_REQUEST_BATCH_VALIDATION_FIELD_CODE
(
    CODE           VARCHAR2(20)         NOT NULL,
    LABEL          VARCHAR2(30)         NOT NULL,
    DESCRIPTION    VARCHAR2(255),
    DISPLAY_ORDER  NUMBER,
    EFFECTIVE_DATE DATE                 NOT NULL,
    EXPIRY_DATE    DATE                 NOT NULL,
    CREATE_USER    VARCHAR2(32)         NOT NULL,
    CREATE_DATE    DATE DEFAULT SYSDATE NOT NULL,
    UPDATE_USER    VARCHAR2(32)         NOT NULL,
    UPDATE_DATE    DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT PEN_REQUEST_BATCH_VALIDATION_FIELD_CODE_PK PRIMARY KEY (CODE)
);
--PEN_REQUEST_BATCH_VALIDATION_FIELD_CODE
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_FIELD_CODE (CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                                     EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('LOCALID', 'School Student ID', 'Local identifier used by the school for the student', 1,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_FIELD_CODE (CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                                     EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('SUBMITPEN', 'Submitted PEN', 'Value provided by the school as a suggestion for the matching PEN', 2,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_FIELD_CODE (CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                                     EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('LEGALFIRST', 'Legal First Name', 'Legal First Name or Names', 3, to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_FIELD_CODE (CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                                     EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('LEGALMID', 'Legal Middle Names', 'Legal Middle Name or Names', 4, to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_FIELD_CODE (CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                                     EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('LEGALLAST', 'Legal Last Name', 'Legal Last Name or Names', 5, to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_FIELD_CODE (CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                                     EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('USUALFIRST', 'Usual First Name', 'Usual First Name or Names', 6, to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_FIELD_CODE (CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                                     EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('USUALMID', 'Usual Middle Name', 'Usual Middle Name or Names', 7, to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_FIELD_CODE (CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                                     EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('USUALLAST', 'Usual Last Name', 'Usual Last Name or Names', 8, to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_FIELD_CODE (CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                                     EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('POSTALCODE', 'Postal Code',
        'Canadian Postal Code of student, or for offshore schools, postal code of the BC contact managing the school',
        9, to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_FIELD_CODE (CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                                     EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('GRADECODE', 'Student Grade Code',
        'Code used to indicate the grade-level of the student at the time of the request. ', 10,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_FIELD_CODE (CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                                     EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('BIRTHDATE', 'Birthdate', 'Date of birth of the Student', 11, to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_FIELD_CODE (CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                                     EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER, UPDATE_DATE)
VALUES ('GENDER', ' Gender', ' Gender of the Student', 12, to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));


CREATE TABLE PEN_REQUEST_BATCH_VALIDATION_ISSUE_SEVERITY_CODE
(
    CODE           VARCHAR2(20)         NOT NULL,
    LABEL          VARCHAR2(30)         NOT NULL,
    DESCRIPTION    VARCHAR2(255),
    DISPLAY_ORDER  NUMBER,
    EFFECTIVE_DATE DATE                 NOT NULL,
    EXPIRY_DATE    DATE                 NOT NULL,
    CREATE_USER    VARCHAR2(32)         NOT NULL,
    CREATE_DATE    DATE DEFAULT SYSDATE NOT NULL,
    UPDATE_USER    VARCHAR2(32)         NOT NULL,
    UPDATE_DATE    DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT PEN_REQUEST_BATCH_VALIDATION_ISSUE_SEVERITY_CODE_PK PRIMARY KEY (CODE)
);
--PEN_REQUEST_BATCH_VALIDATION_ISSUE_SEVERITY_CODE

INSERT INTO PEN_REQUEST_BATCH_VALIDATION_ISSUE_SEVERITY_CODE (CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                                              EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                              UPDATE_DATE)
VALUES ('ERROR', 'Error', 'The condition is a hard or fatal error', 1, to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_ISSUE_SEVERITY_CODE (CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                                              EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                              UPDATE_DATE)
VALUES ('WARNING', 'Warning', 'The condition is a warning, or soft error', 2, to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));



CREATE TABLE PEN_REQUEST_BATCH_VALIDATION_ISSUE_TYPE_CODE
(
    CODE           VARCHAR2(20)         NOT NULL,
    LABEL          VARCHAR2(50)         NOT NULL,
    LEGACY_LABEL   VARCHAR2(50)         NOT NULL,
    DESCRIPTION    VARCHAR2(255),
    DISPLAY_ORDER  NUMBER,
    EFFECTIVE_DATE DATE                 NOT NULL,
    EXPIRY_DATE    DATE                 NOT NULL,
    CREATE_USER    VARCHAR2(32)         NOT NULL,
    CREATE_DATE    DATE DEFAULT SYSDATE NOT NULL,
    UPDATE_USER    VARCHAR2(32)         NOT NULL,
    UPDATE_DATE    DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT PEN_REQUEST_BATCH_VALIDATION_ISSUE_TYPE_CODE_PK PRIMARY KEY (CODE)
);
--PEN_REQUEST_BATCH_VALIDATION_ISSUE_TYPE_CODE
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_ISSUE_TYPE_CODE (CODE, LABEL, LEGACY_LABEL, DESCRIPTION, DISPLAY_ORDER,
                                                          EFFECTIVE_DATE,
                                                          EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                          UPDATE_DATE)
VALUES ('1CHARNAME', 'Field is just one character', 'C6, C8, C11, C13, C77, C78',
        'Field consists of just one character', 10, to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_ISSUE_TYPE_CODE (CODE, LABEL, LEGACY_LABEL, DESCRIPTION, DISPLAY_ORDER,
                                                          EFFECTIVE_DATE,
                                                          EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                          UPDATE_DATE)
VALUES ('APOSTROPHE', 'Filed value is an apostrophe', 'C9', 'Field contains only an apostrophe character', 20,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_ISSUE_TYPE_CODE (CODE, LABEL, LEGACY_LABEL, DESCRIPTION, DISPLAY_ORDER,
                                                          EFFECTIVE_DATE,
                                                          EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                          UPDATE_DATE)
VALUES ('BEGININVALID', 'Field begins with special char', 'C6,C8, C11, C13, C77, C78', 'Field begins with one several special characters', 30,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_ISSUE_TYPE_CODE (CODE, LABEL, LEGACY_LABEL, DESCRIPTION, DISPLAY_ORDER,
                                                          EFFECTIVE_DATE,
                                                          EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                          UPDATE_DATE)
VALUES ('BLANKFIELD', 'Field is blank', 'C7, C9', 'Required field is blank', 40, to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_ISSUE_TYPE_CODE (CODE, LABEL, LEGACY_LABEL, DESCRIPTION, DISPLAY_ORDER,
                                                          EFFECTIVE_DATE,
                                                          EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                          UPDATE_DATE)
VALUES ('BLANKINNAME', 'Field has embedded blanks', 'C6, C8, C11, C13, C77, C78', 'Field has embedded blanks', 50,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_ISSUE_TYPE_CODE (CODE, LABEL, LEGACY_LABEL, DESCRIPTION, DISPLAY_ORDER,
                                                          EFFECTIVE_DATE,
                                                          EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                          UPDATE_DATE)
VALUES ('CHKDIG', 'Invalid Check Digit', 'C5', 'PEN not valid because check digit is invalid', 60,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_ISSUE_TYPE_CODE (CODE, LABEL, LEGACY_LABEL, DESCRIPTION, DISPLAY_ORDER,
                                                          EFFECTIVE_DATE,
                                                          EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                          UPDATE_DATE)
VALUES ('DOB_INVALID', 'Birthdate is not a valid date', 'C17', 'Birthdate is not a valid date', 70,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_ISSUE_TYPE_CODE (CODE, LABEL, LEGACY_LABEL, DESCRIPTION, DISPLAY_ORDER,
                                                          EFFECTIVE_DATE,
                                                          EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                          UPDATE_DATE)
VALUES ('DOB_PAST', 'Birthdate before 1900', 'C16', 'Birthdate before 1900', 80, to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_ISSUE_TYPE_CODE (CODE, LABEL, LEGACY_LABEL, DESCRIPTION, DISPLAY_ORDER,
                                                          EFFECTIVE_DATE,
                                                          EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                          UPDATE_DATE)
VALUES ('DOB_FUTURE', 'Birthdate is in the future', 'C16', 'Birthdate is in the future', 90,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_ISSUE_TYPE_CODE (CODE, LABEL, LEGACY_LABEL, DESCRIPTION, DISPLAY_ORDER,
                                                          EFFECTIVE_DATE,
                                                          EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                          UPDATE_DATE)
VALUES ('EMBEDDEDMID', 'Middle name is in First name', 'C38', 'The Middle name is part of the First name', 100,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_ISSUE_TYPE_CODE (CODE, LABEL, LEGACY_LABEL, DESCRIPTION, DISPLAY_ORDER,
                                                          EFFECTIVE_DATE,
                                                          EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                          UPDATE_DATE)
VALUES ('GENDER_ERR', 'Invalid Gender code', 'C22', 'Invalid Gender code', 110, to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_ISSUE_TYPE_CODE (CODE, LABEL, LEGACY_LABEL, DESCRIPTION, DISPLAY_ORDER,
                                                          EFFECTIVE_DATE,
                                                          EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                          UPDATE_DATE)
VALUES ('GRADECD_ERR', 'Invalid Grade Code', 'C46', 'Invalid Grade Code', 120, to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_ISSUE_TYPE_CODE (CODE, LABEL, LEGACY_LABEL, DESCRIPTION, DISPLAY_ORDER,
                                                          EFFECTIVE_DATE,
                                                          EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                          UPDATE_DATE)
VALUES ('INVCHARS', 'Field contains invalid character', 'C6, C8, C11, C13, C77, C78',
        'Field contains invalid character', 130, to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_ISSUE_TYPE_CODE (CODE, LABEL, LEGACY_LABEL, DESCRIPTION, DISPLAY_ORDER,
                                                          EFFECTIVE_DATE,
                                                          EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                          UPDATE_DATE)
VALUES ('INVPREFIX', 'Field starts with XX or ZZ', 'C6, C8, C11, C13, C77, C78', 'Field starts with XX or ZZ', 140,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_ISSUE_TYPE_CODE (CODE, LABEL, LEGACY_LABEL, DESCRIPTION, DISPLAY_ORDER,
                                                          EFFECTIVE_DATE,
                                                          EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                          UPDATE_DATE)
VALUES ('OLD4GRADE', 'Age is too old for Grade', 'C44', 'Age is too old for Grade', 150,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_ISSUE_TYPE_CODE (CODE, LABEL, LEGACY_LABEL, DESCRIPTION, DISPLAY_ORDER,
                                                          EFFECTIVE_DATE,
                                                          EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                          UPDATE_DATE)
VALUES ('ONBLOCKLIST', 'Name is on the blocked list', 'C6, C8, C11, C13, C77, C78',
        'Name matches one of the values on the block list: PEN_NAME_TEXT', 160, to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_ISSUE_TYPE_CODE (CODE, LABEL, LEGACY_LABEL, DESCRIPTION, DISPLAY_ORDER,
                                                          EFFECTIVE_DATE,
                                                          EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                          UPDATE_DATE)
VALUES ('PC_ERR', 'Postal Code is invalid', 'C24',
        'Postal Code is not in the format of a Canadian postal code', 165, to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_ISSUE_TYPE_CODE (CODE, LABEL, LEGACY_LABEL, DESCRIPTION, DISPLAY_ORDER,
                                                          EFFECTIVE_DATE,
                                                          EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                          UPDATE_DATE)
VALUES ('REPEATMID', 'Middlename is a repeat', 'C38', 'Middlename is a repeat of the First or Last name', 170,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_ISSUE_TYPE_CODE (CODE, LABEL, LEGACY_LABEL, DESCRIPTION, DISPLAY_ORDER,
                                                          EFFECTIVE_DATE,
                                                          EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                          UPDATE_DATE)
VALUES ('SCHARPREFIX', 'First char is invalid', 'C6, C8, C11, C13, C77, C78',
        'Field starts with an invalid special character', 180, to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALIDATION_ISSUE_TYPE_CODE (CODE, LABEL, LEGACY_LABEL, DESCRIPTION, DISPLAY_ORDER,
                                                          EFFECTIVE_DATE,
                                                          EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                          UPDATE_DATE)
VALUES ('YOUNG4GRADE', 'Age is too young for Grade', 'C44', 'Age is too young for Grade', 190,
        to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));


CREATE TABLE PEN_REQUEST_BATCH_VALID_ONE_LETTER_GIVEN_NAME_CODE
(
    CODE           VARCHAR2(20)         NOT NULL,
    LABEL          VARCHAR2(30)         NOT NULL,
    DESCRIPTION    VARCHAR2(255),
    DISPLAY_ORDER  NUMBER,
    EFFECTIVE_DATE DATE                 NOT NULL,
    EXPIRY_DATE    DATE                 NOT NULL,
    CREATE_USER    VARCHAR2(32)         NOT NULL,
    CREATE_DATE    DATE DEFAULT SYSDATE NOT NULL,
    UPDATE_USER    VARCHAR2(32)         NOT NULL,
    UPDATE_DATE    DATE DEFAULT SYSDATE NOT NULL,
    CONSTRAINT PEN_REQUEST_BATCH_VALID_ONE_LETTER_GIVEN_NAME_CODE_PK PRIMARY KEY (CODE)
);
-- PEN_REQUEST_BATCH_VALID_ONE_LETTER_GIVEN_NAME_CODE

INSERT INTO PEN_REQUEST_BATCH_VALID_ONE_LETTER_GIVEN_NAME_CODE (CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                                                EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                                UPDATE_DATE)
VALUES ('A', 'Lettter A', 'Letter A used as a Given Name', 1, to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALID_ONE_LETTER_GIVEN_NAME_CODE (CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                                                EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                                UPDATE_DATE)
VALUES ('I', 'Lettter I', 'Letter I used as a Given Name', 2, to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALID_ONE_LETTER_GIVEN_NAME_CODE (CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                                                EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                                UPDATE_DATE)
VALUES ('O', 'Lettter O', 'Letter O used as a Given Name', 3, to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));
INSERT INTO PEN_REQUEST_BATCH_VALID_ONE_LETTER_GIVEN_NAME_CODE (CODE, LABEL, DESCRIPTION, DISPLAY_ORDER, EFFECTIVE_DATE,
                                                                EXPIRY_DATE, CREATE_USER, CREATE_DATE, UPDATE_USER,
                                                                UPDATE_DATE)
VALUES ('Y', 'Lettter Y', 'Letter Y used as a Given Name', 4, to_date('2020-01-01', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'),
        'IDIR/MVILLENE',
        to_date('2019-11-07', 'YYYY-MM-DD'));

CREATE TABLE PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE
(
    PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE_ID            RAW(16)      NOT NULL,
    PEN_REQUEST_BATCH_STUDENT_ID                             RAW(16)      NOT NULL,
    PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE_SEVERITY_CODE VARCHAR2(20) NOT NULL,
    PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE_TYPE_CODE     VARCHAR2(20) NOT NULL,
    PEN_REQUEST_BATCH_STUDENT_VALIDATION_FIELD_CODE          VARCHAR2(20) NOT NULL,
    ADDITIONAL_INFO                                          VARCHAR2(255),
    CONSTRAINT PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE_PK PRIMARY KEY (PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE_ID)
);
COMMENT ON TABLE PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE IS 'Contains all validation results(errors or warnings), corresponding to a particular student request.';
-- Column Comments
COMMENT ON COLUMN PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE.PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE_ID IS 'Unique surrogate key for each validation record. GUID value must be provided during insert.';
COMMENT ON COLUMN PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE.PEN_REQUEST_BATCH_STUDENT_ID IS 'Foreign key to the parent PEN Request Student record.';
COMMENT ON COLUMN PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE.PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE_SEVERITY_CODE IS 'Code identifying the severity of issue found: Warning or Error.';
COMMENT ON COLUMN PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE.PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE_TYPE_CODE IS 'Code identifying the type of error that occurred. Examples: Invalid characters, invalid birthdate, etc.';
COMMENT ON COLUMN PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE.PEN_REQUEST_BATCH_STUDENT_VALIDATION_FIELD_CODE IS 'Code identifying the the data field in which the issue was found.';
COMMENT ON COLUMN PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE.ADDITIONAL_INFO IS 'Free text additional information about a validation issue that is optional and only used when required to supplement the generic description of the issue provided by the description for the Issue Type Code.';

ALTER TABLE PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE
    ADD CONSTRAINT PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE_PEN_REQUEST_BATCH_STUDENT_ID_FK FOREIGN KEY (PEN_REQUEST_BATCH_STUDENT_ID) REFERENCES PEN_REQUEST_BATCH_STUDENT (PEN_REQUEST_BATCH_STUDENT_ID);

ALTER TABLE PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE
    ADD CONSTRAINT PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE_ISSUE_SEVERITY_CODE_FK FOREIGN KEY (PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE_SEVERITY_CODE) REFERENCES PEN_REQUEST_BATCH_VALIDATION_ISSUE_SEVERITY_CODE (CODE);

ALTER TABLE PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE
    ADD CONSTRAINT PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE_ISSUE_TYPE_CODE_FK FOREIGN KEY (PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE_TYPE_CODE) REFERENCES PEN_REQUEST_BATCH_VALIDATION_ISSUE_TYPE_CODE (CODE);

ALTER TABLE PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE
    ADD CONSTRAINT PEN_REQUEST_BATCH_STUDENT_VALIDATION_ISSUE_FIELD_CODE_FK FOREIGN KEY (PEN_REQUEST_BATCH_STUDENT_VALIDATION_FIELD_CODE) REFERENCES PEN_REQUEST_BATCH_VALIDATION_FIELD_CODE (CODE);