package org.camunda.community.bpmndt.api;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.util.LinkedList;
import java.util.List;

/**
 * Data, collected during test execution.
 */
public class TestExecutionData {

  /** Data version, encoded within the protocol record. */
  private static final int VERSION = 1;

  // Separators
  private static final char RECORD_SEPARATOR = '\036';
  private static final char VALUE_SEPARATOR = '\000';

  private final LinkedList<Record> records;

  public TestExecutionData() {
    records = new LinkedList<>();
  }

  protected void clear() {
    records.clear();
  }

  protected List<Record> getRecords() {
    return records;
  }

  public void recordActivityEnd(String activityId, String activityInstanceId) {
    records.add(new ActivityRecord(RecordType.ACTIVITY_END, activityId, activityInstanceId));
  }

  public void recordActivityStart(String activityId, String activityInstanceId) {
    records.add(new ActivityRecord(RecordType.ACTIVITY_START, activityId, activityInstanceId));
  }

  protected void recordTest(String testCaseId, String testName, String testMethodName) {
    TestRecord testRecord = new TestRecord();
    testRecord.testCaseId = testCaseId;
    testRecord.testMethodName = testMethodName;
    testRecord.testName = testName;

    records.add(new ProtocolRecord());
    records.add(testRecord);
  }

  protected void recordTestFailure(Throwable t) {
    if (records.isEmpty()) {
      throw new IllegalStateException("Test must be recorded first");
    }

    TestResultRecord testResultRecord = new TestResultRecord();
    testResultRecord.success = false;

    records.add(testResultRecord);
  }

  protected void recordTestSuccess() {
    if (records.isEmpty()) {
      throw new IllegalStateException("Test must be recorded first");
    }

    TestResultRecord testResultRecord = new TestResultRecord();
    testResultRecord.success = true;

    records.add(testResultRecord);
  }

  protected void write(OutputStreamWriter w) throws IOException {
    StringBuilder sb = new StringBuilder(512);

    for (Record record : records) {
      record.encode(sb);

      sb.append(RECORD_SEPARATOR);

      w.write(sb.toString());

      sb.setLength(0);
    }
  }

  public static enum RecordType {

    ACTIVITY_END,
    ACTIVITY_START,
    PROTOCOL,
    TEST,
    TEST_RESULT
  }

  /**
   * Abstract super class for records of any type.
   */
  public static abstract class Record {

    private final RecordType type;

    private Record(RecordType type) {
      this.type = type;
    }

    /**
     * Encodes the record's data, so that it can be sent to the test execution listener.
     * 
     * @param sb A string builder to append.
     */
    protected abstract void encode(StringBuilder sb);

    public RecordType getType() {
      return type;
    }
  }

  public static class ActivityRecord extends Record {

    private final String activityId;
    private final String activityInstanceId;

    private ActivityRecord(RecordType type, String activityId, String activityInstanceId) {
      super(type);

      this.activityId = activityId;
      this.activityInstanceId = activityInstanceId;
    }

    @Override
    protected void encode(StringBuilder sb) {
      sb.append(getType().name());
      sb.append(VALUE_SEPARATOR);
      sb.append(activityId);
      sb.append(VALUE_SEPARATOR);
      sb.append(activityInstanceId);
    }

    public String getActivityId() {
      return activityId;
    }
  }

  public static class ProtocolRecord extends Record {

    private ProtocolRecord() {
      super(RecordType.PROTOCOL);
    }

    @Override
    protected void encode(StringBuilder sb) {
      sb.append(getType());
      sb.append(VALUE_SEPARATOR);
      sb.append(VERSION);
      sb.append(VALUE_SEPARATOR);
      sb.append(ManagementFactory.getRuntimeMXBean().getStartTime());
    }
  }

  public static class TestRecord extends Record {

    private String testCaseId;
    private String testMethodName;
    private String testName;
    
    private TestRecord() {
      super(RecordType.TEST);
    }

    @Override
    protected void encode(StringBuilder sb) {
      sb.append(getType().name());
      sb.append(VALUE_SEPARATOR);
      sb.append(testCaseId);
      sb.append(VALUE_SEPARATOR);
      sb.append(testName);
      sb.append(VALUE_SEPARATOR);
      sb.append(testMethodName);
    }

    public String getTestName() {
      return testName;
    }
  }

  public static class TestResultRecord extends Record {

    private boolean success;

    private TestResultRecord() {
      super(RecordType.TEST_RESULT);
    }
    
    @Override
    protected void encode(StringBuilder sb) {
      sb.append(getType().name());
      sb.append(VALUE_SEPARATOR);
      sb.append(success);
    }

    public boolean isSuccess() {
      return success;
    }
  }
}
