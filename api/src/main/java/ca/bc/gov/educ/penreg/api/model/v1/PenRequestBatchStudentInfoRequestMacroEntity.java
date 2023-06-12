package ca.bc.gov.educ.penreg.api.model.v1;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

@Data
@Entity
@Table(name = "PEN_REQUEST_BATCH_STUDENT_INFO_REQUEST_MACRO")
public class PenRequestBatchStudentInfoRequestMacroEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
            @org.hibernate.annotations.Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
    @Column(name = "PRB_STUDENT_INFO_REQUEST_MACRO_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
    UUID macroId;

    @Column(name = "MACRO_CODE")
    private String macroCode;

    @Column(name = "MACRO_TEXT")
    private String macroText;

    @Column(name = "create_user", updatable = false)
    String createUser;

    @PastOrPresent
    @Column(name = "create_date", updatable = false)
    LocalDateTime createDate;

    @Column(name = "update_user")
    String updateUser;

    @PastOrPresent
    @Column(name = "update_date")
    LocalDateTime updateDate;
}

