package ca.bc.gov.educ.penreg.api.mappers.v1;

import ca.bc.gov.educ.penreg.api.struct.v1.PenRequestBatchArchiveAndReturnSagaData;
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
    @Mapping(target="fascimile", source="facsimile")
    @Mapping(target = "penCordinatorEmail", source = "fromEmail")
    @Mapping(target = "mincode", source = "data.penRequestBatch.mincode")
    @Mapping(target = "submissionNumber", source = "data.penRequestBatch.submissionNumber")
    @Mapping(target = "reviewer", source = "data.penRequestBatch.updateUser")
    @Mapping(target = "processDate", ignore = true)
    @Mapping(target = "processTime", ignore = true)
    @Mapping(target = "reportDate", ignore = true)
    PenRequestBatchReportData toReportData(PenRequestBatchArchiveAndReturnSagaData data);
}
