package ca.bc.gov.educ.penreg.api.mappers.v1;

import ca.bc.gov.educ.penreg.api.struct.v1.BasePenRequestBatchReturnFilesSagaData;
import ca.bc.gov.educ.penreg.api.struct.v1.reportstructs.PenRequestBatchReportData;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
@DecoratedWith(PenRequestBatchReportDataDecorator.class)
public interface PenRequestBatchReportDataMapper {

  PenRequestBatchReportDataMapper mapper = Mappers.getMapper(PenRequestBatchReportDataMapper.class);

  @Mapping(target = "confirmedList", ignore = true)
  @Mapping(target = "diffList", ignore = true)
  @Mapping(target = "newPenList", ignore = true)
  @Mapping(target = "pendingList", ignore = true)
  @Mapping(target = "sysMatchedList", ignore = true)
  @Mapping(target = "fascimile", source = "facsimile")
  @Mapping(target = "penCordinatorEmail", source = "fromEmail")
  @Mapping(target = "mincode", expression = "java(data.getPenRequestBatch() == null || data.getPenRequestBatch().getMincode() == null || data.getPenRequestBatch().getMincode().isEmpty() || data.getPenRequestBatch().getMincode().length()<3 ? \"\" : data.getPenRequestBatch().getMincode().substring(0, 3) + \" \" + data.getPenRequestBatch().getMincode().substring(3))")
  @Mapping(target = "submissionNumber", source = "data.penRequestBatch.submissionNumber")
  @Mapping(target = "processDate", ignore = true)
  @Mapping(target = "processTime", ignore = true)
  @Mapping(target = "reportDate", ignore = true)
  PenRequestBatchReportData toReportData(BasePenRequestBatchReturnFilesSagaData data);
}
