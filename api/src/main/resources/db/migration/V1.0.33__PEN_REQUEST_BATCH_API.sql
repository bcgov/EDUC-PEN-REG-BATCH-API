CREATE INDEX PRB_STUDENT_STATUS_IDX ON API_PEN_REQUEST_BATCH.PEN_REQUEST_BATCH_STUDENT (PEN_REQUEST_BATCH_STUDENT_STATUS_CODE) TABLESPACE API_PEN_IDX;

CREATE INDEX PRB_STUDENT_BATCH_ID_IDX ON API_PEN_REQUEST_BATCH.PEN_REQUEST_BATCH_STUDENT (PEN_REQUEST_BATCH_ID) TABLESPACE API_PEN_IDX;

CREATE INDEX PRB_STUDENT_ASSIGNED_PEN_IDX ON API_PEN_REQUEST_BATCH.PEN_REQUEST_BATCH_STUDENT (ASSIGNED_PEN) TABLESPACE API_PEN_IDX;

CREATE INDEX PRB_BATCH_STATUS_CODE_IDX ON API_PEN_REQUEST_BATCH.PEN_REQUEST_BATCH (PEN_REQUEST_BATCH_STATUS_CODE) TABLESPACE API_PEN_IDX;

CREATE INDEX PRB_CREATE_DATE_IDX ON API_PEN_REQUEST_BATCH.PEN_REQUEST_BATCH (CREATE_DATE) TABLESPACE API_PEN_IDX;

CREATE INDEX PRB_STUDENT_CREATE_DATE_IDX ON API_PEN_REQUEST_BATCH.PEN_REQUEST_BATCH_STUDENT (CREATE_DATE) TABLESPACE API_PEN_IDX;
