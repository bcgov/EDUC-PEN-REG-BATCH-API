package ca.bc.gov.educ.penreg.api.messaging;

import ca.bc.gov.educ.penreg.api.constants.EventOutcome;
import ca.bc.gov.educ.penreg.api.constants.EventType;
import ca.bc.gov.educ.penreg.api.model.Saga;
import ca.bc.gov.educ.penreg.api.orchestrator.PenReqBatchNewPenOrchestrator;
import ca.bc.gov.educ.penreg.api.repository.SagaEventRepository;
import ca.bc.gov.educ.penreg.api.repository.SagaRepository;
import ca.bc.gov.educ.penreg.api.service.EventTaskSchedulerAsyncService;
import ca.bc.gov.educ.penreg.api.service.SagaService;
import ca.bc.gov.educ.penreg.api.struct.Event;
import ca.bc.gov.educ.penreg.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.nats.streaming.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import static ca.bc.gov.educ.penreg.api.constants.EventType.*;
import static ca.bc.gov.educ.penreg.api.constants.SagaEnum.PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * The type Message publisher test.
 */
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class MessageSubscriberTest {
  @Autowired
  SagaRepository repository;
  @Autowired
  SagaEventRepository sagaEventRepository;
  @Autowired
  private SagaService sagaService;

  @Mock
  private MessageSubscriber messageSubscriber;
  @Mock
  private MessagePublisher messagePublisher;
  @Mock
  private EventTaskSchedulerAsyncService taskSchedulerService;

  private final String penRequestBatchID = UUID.randomUUID().toString();

  private final String penRequestBatchStudentID = UUID.randomUUID().toString();

  private final String twinStudentID = UUID.randomUUID().toString();

  private final String mincode = "01292001";

  /**
   * The issue new pen orchestrator.
   */
  @Autowired
  private PenReqBatchNewPenOrchestrator orchestrator;

  private Saga saga;

  @Before
  public void setUp() {
    initMocks(this);
    var payload = dummyPenRequestBatchNewPenSagaDataJson();
    saga = sagaService.createSagaRecordInDB(PEN_REQUEST_BATCH_NEW_PEN_PROCESSING_SAGA.toString(), "Test", payload,
      UUID.fromString(penRequestBatchStudentID), UUID.fromString(penRequestBatchID));
  }

  @After
  public void after() {
    sagaEventRepository.deleteAll();
    repository.deleteAll();
  }

  /**
   * Test get next pen number.
   *
   * @throws Exception the exception
   */
  @Test
  public void testOnMessage_givenIssueNewPenSaga_shouldHandleEvent() throws JsonProcessingException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, InterruptedException {
    var event = Event.builder()
      .eventType(EventType.INITIATED)
      .eventOutcome(EventOutcome.INITIATE_SUCCESS)
      .sagaId(saga.getSagaId())
      .build();
    var constructor = Message.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    var message = constructor.newInstance();
    message.setData(JsonUtil.getJsonStringFromObject(event).getBytes());

    when(messageSubscriber.onMessage(orchestrator)).thenCallRealMethod();
    var messageHandler = messageSubscriber.onMessage(orchestrator);
    messageHandler.onMessage(message);

    assertThat(sagaService.findSagaById(saga.getSagaId()).get().getSagaState()).isEqualTo(GET_NEXT_PEN_NUMBER.toString());
    var sagaStates = sagaService.findAllSagaStates(saga);
    assertThat(sagaStates.size()).isEqualTo(1);
    assertThat(sagaStates.get(0).getSagaEventState()).isEqualTo(EventType.INITIATED.toString());
    assertThat(sagaStates.get(0).getSagaEventOutcome()).isEqualTo(EventOutcome.INITIATE_SUCCESS.toString());
  }

  protected String dummyPenRequestBatchNewPenSagaDataJson() {
    return " {\n" +
      "    \"createUser\": \"test\",\n" +
      "    \"updateUser\": \"test\",\n" +
      "    \"penRequestBatchID\": \"" + penRequestBatchID + "\",\n" +
      "    \"penRequestBatchStudentID\": \"" + penRequestBatchStudentID + "\",\n" +
      "    \"legalFirstName\": \"Jack\",\n" +
      "    \"mincode\": \""+ mincode + "\",\n" +
      "    \"genderCode\": \"X\",\n" +
      "    \"twinStudentIDs\": [\"" + twinStudentID + "\"]\n" +
      "  }";
  }
}
