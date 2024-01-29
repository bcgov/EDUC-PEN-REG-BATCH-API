package ca.bc.gov.educ.penreg.api.service;

import ca.bc.gov.educ.penreg.api.rest.RestUtils;
import ca.bc.gov.educ.penreg.api.struct.SchoolContact;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class StudentRegistrationContactService {
  private final RestUtils restUtils;

  @Autowired
  public StudentRegistrationContactService(final RestUtils restUtils) {
    this.restUtils = restUtils;
  }

  public List<SchoolContact> getStudentRegistrationContactsByMincode(final String mincode) {
    if (StringUtils.length(mincode) != 8 || !StringUtils.isNumeric(mincode)) {
      return new ArrayList<>();
    }

    return this.restUtils.getStudentRegistrationContactList(mincode);
  }

  public List<String> getStudentRegistrationContactEmailsByMincode(final String mincode) {
    log.debug("getting pen coordinator email for mincode :: {}", mincode);
    if (StringUtils.length(mincode) != 8 || !StringUtils.isNumeric(mincode)) {
      return new ArrayList<>();
    }
    var contacts = this.restUtils.getStudentRegistrationContactList(mincode);
    return contacts.stream()
            .map(contact -> String.valueOf(contact.getEmail()))
            .toList();
  }
}
