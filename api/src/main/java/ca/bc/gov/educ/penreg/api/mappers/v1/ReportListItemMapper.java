package ca.bc.gov.educ.penreg.api.mappers.v1;

import ca.bc.gov.educ.penreg.api.struct.Student;
import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchStudent;
import ca.bc.gov.educ.penreg.api.struct.v1.reportstructs.ReportListItem;
import ca.bc.gov.educ.penreg.api.struct.v1.reportstructs.ReportUserMatchedListItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ReportListItemMapper {

    ReportListItemMapper mapper = Mappers.getMapper(ReportListItemMapper.class);

    @Mapping(target = "birthDate", expression = "java(penRequestBatchStudent.getDob() == null || penRequestBatchStudent.getDob().isEmpty() ? \"\" : java.time.LocalDate.parse(penRequestBatchStudent.getDob(), java.time.format.DateTimeFormatter.ofPattern(\"yyyyMMdd\")).format(java.time.format.DateTimeFormatter.ofPattern(\"yyyy/MM/dd\")))")
    @Mapping(source = "genderCode", target = "gender")
    @Mapping(source = "assignedPEN", target = "pen")
    @Mapping(target = "schoolID", expression = "java(penRequestBatchStudent.getMincode() == null || penRequestBatchStudent.getMincode().isEmpty() ? \"\" : penRequestBatchStudent.getMincode().substring(0, 3) + \" \" + penRequestBatchStudent.getMincode().substring(3))")
    @Mapping(source = "legalFirstName", target = "givenName")
    @Mapping(source = "legalLastName", target = "surname")
    @Mapping(target = "usualName", expression = "java(java.util.stream.Stream.of(penRequestBatchStudent.getUsualLastName(), penRequestBatchStudent.getUsualFirstName(), penRequestBatchStudent.getUsualMiddleNames()).filter(x -> x != null && !x.isEmpty()).collect(java.util.stream.Collectors.joining(\", \")))")
    @Mapping(source = "infoRequest", target = "reason")
    ReportListItem toReportListItem(PenRequestBatchStudent penRequestBatchStudent);

    @Mapping(target = "birthDate", expression = "java(student.getDob() == null || student.getDob().isEmpty() ? \"\" : java.time.LocalDate.parse(student.getDob()).format(java.time.format.DateTimeFormatter.ofPattern(\"yyyy/MM/dd\")))")
    @Mapping(source = "genderCode", target = "gender")
    @Mapping(target = "schoolID", expression = "java(student.getMincode() == null || student.getMincode().isEmpty() ? \"\" : student.getMincode().substring(0, 3) + \" \" + student.getMincode().substring(3))")
    @Mapping(source = "legalFirstName", target = "givenName")
    @Mapping(source = "legalLastName", target = "surname")
    @Mapping(target = "usualName", expression = "java(java.util.stream.Stream.of(student.getUsualLastName(), student.getUsualFirstName(), student.getUsualMiddleNames()).filter(x -> x != null && !x.isEmpty()).collect(java.util.stream.Collectors.joining(\", \")))")
    @Mapping(ignore = true, target = "reason")
    ReportListItem toReportListItem(Student student);

    @Mapping(target = "min", expression = "java(toReportListItem(student))")
    @Mapping(target = "school", expression = "java(toReportListItem(penRequestBatchStudent))")
    ReportUserMatchedListItem toReportUserMatchedListItem(PenRequestBatchStudent penRequestBatchStudent, Student student);
}
