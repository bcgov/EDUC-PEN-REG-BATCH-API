INSERT INTO PEN_REQUEST_BATCH_STUDENT_STATUS_CODE (PEN_REQUEST_BATCH_STUDENT_STATUS_CODE, LABEL, DESCRIPTION,
                                                   DISPLAY_ORDER, EFFECTIVE_DATE, EXPIRY_DATE, CREATE_USER, CREATE_DATE,
                                                   UPDATE_USER, UPDATE_DATE)
VALUES ('DUPLICATE',
        'Duplicate request',
        'Request is a duplicate of another request in the same batch file',
        10,
        to_date('2021-01-14', 'YYYY-MM-DD'),
        to_date('2099-12-31', 'YYYY-MM-DD'),
        'IDIR/JOCOX',
        to_date('2019-01-14', 'YYYY-MM-DD'),
        'IDIR/JOCOX',
        to_date('2019-01-14', 'YYYY-MM-DD'));
