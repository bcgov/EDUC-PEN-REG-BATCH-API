package ca.bc.gov.educ.penreg.api.mappers.v1;

import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import ca.bc.gov.educ.penreg.api.struct.v1.reportstructs.ReportListItem;
import ca.bc.gov.educ.penreg.api.struct.v1.reportstructs.ReportUserMatchedListItem;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
@DecoratedWith(ReportListItemDecorator.class)
public interface ReportListItemMapper {

    ReportListItemMapper mapper = Mappers.getMapper(ReportListItemMapper.class);

    @Mapping(target = "birthDate", ignore = true)
    @Mapping(source = "penRequestBatchStudent.genderCode", target = "gender", defaultValue = "")
    @Mapping(source = "penRequestBatchStudent.assignedPEN", target = "pen", defaultValue = "")
    @Mapping(source = "penRequestBatchStudent.localID", target = "schoolID", defaultValue = "")
    @Mapping(source = "penRequestBatchStudent.legalFirstName", target = "givenName", defaultValue = "")
    @Mapping(source = "penRequestBatchStudent.legalLastName", target = "surname", defaultValue = "")
    @Mapping(source = "penRequestBatchStudent.legalMiddleNames", target = "legalMiddleNames", defaultValue = "")
    @Mapping(target = "usualName", ignore = true)
    @Mapping(target = "reason", ignore = true)
    ReportListItem toReportListItem(PenRequestBatchStudent penRequestBatchStudent, String penRequestBatchStudentValidationIssue);

    @Mapping(target = "birthDate", ignore = true)
    @Mapping(source = "genderCode", target = "gender", defaultValue = "")
    @Mapping(source = "localID", target = "schoolID", defaultValue = "")
    @Mapping(source = "legalFirstName", target = "givenName", defaultValue = "")
    @Mapping(source = "legalLastName", target = "surname", defaultValue = "")
    @Mapping(source = "legalMiddleNames", target = "legalMiddleNames", defaultValue = "")
    @Mapping(target = "usualName", ignore = true)
    @Mapping(ignore = true, target = "reason")
    ReportListItem toReportListItem(Student student);

    @Mapping(target = "min", ignore = true)
    @Mapping(target = "school", ignore = true)
    ReportUserMatchedListItem toReportUserMatchedListItem(PenRequestBatchStudent penRequestBatchStudent, Student student);

    @Mapping(target = "min", ignore = true)
    @Mapping(target = "school", ignore = true)
    ReportUserMatchedListItem toReportUserMatchedDiffListItem(PenRequestBatchStudent penRequestBatchStudent, Student student);
}
