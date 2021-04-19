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
    @Mapping(source = "genderCode", target = "gender", defaultValue = "")
    @Mapping(source = "assignedPEN", target = "pen", defaultValue = "")
    @Mapping(source = "mincode", target = "schoolID", defaultValue = "")
    @Mapping(source = "legalFirstName", target = "givenName", defaultValue = "")
    @Mapping(source = "legalLastName", target = "surname", defaultValue = "")
    @Mapping(source = "legalMiddleNames", target = "legalMiddleNames", defaultValue = "")
    @Mapping(target = "usualName", ignore = true)
    @Mapping(source = "infoRequest", target = "reason")
    ReportListItem toReportListItem(PenRequestBatchStudent penRequestBatchStudent);

    @Mapping(target = "birthDate", ignore = true)
    @Mapping(source = "genderCode", target = "gender", defaultValue = "")
    @Mapping(source = "mincode", target = "schoolID", defaultValue = "")
    @Mapping(source = "legalFirstName", target = "givenName", defaultValue = "")
    @Mapping(source = "legalLastName", target = "surname", defaultValue = "")
    @Mapping(source = "legalMiddleNames", target = "legalMiddleNames", defaultValue = "")
    @Mapping(target = "usualName", ignore = true)
    @Mapping(ignore = true, target = "reason")
    ReportListItem toReportListItem(Student student);

    @Mapping(target = "min", ignore = true)
    @Mapping(target = "school", ignore = true)
    ReportUserMatchedListItem toReportUserMatchedListItem(PenRequestBatchStudent penRequestBatchStudent, Student student);
}
