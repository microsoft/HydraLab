/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.microsoft.hydralab.agent.runner.espresso;

import com.android.ddmlib.Log;
import com.android.ddmlib.Log.LogLevel;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.ddmlib.testrunner.TestResult;
import com.android.ddmlib.testrunner.TestResult.TestStatus;
import com.android.ddmlib.testrunner.TestRunResult;
import com.google.common.collect.ImmutableMap;
import com.microsoft.hydralab.common.util.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.kxml2.io.KXmlSerializer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Writes JUnit results to an XML files in a format consistent with
 * Ant's XMLJUnitResultFormatter.
 * <p>
 * Creates a separate XML file per test run.
 *
 * @see <a href="https://svn.jenkins-ci.org/trunk/hudson/dtkit/dtkit-format/dtkit-junit-model/src/main/resources/com/thalesgroup/dtkit/junit/model/xsd/junit-4.xsd">junit-4.xsd</a>
 * Copy from com.android.ddmlib.testrunner.XmlTestRunListener to use getAbsoluteReportPath() as a public method.
 */
public class XmlTestRunListener implements ITestRunListener {

    private static final String LOG_TAG = "XmlResultReporter";

    private static final String TEST_RESULT_FILE_SUFFIX = ".xml";
    private static final String TEST_RESULT_FILE_PREFIX = "test_result_";

    private static final String TESTSUITE = "testsuite";
    private static final String TESTCASE = "testcase";
    private static final String ERROR = "error";
    private static final String FAILURE = "failure";
    private static final String SKIPPED_TAG = "skipped";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_TIME = "time";
    private static final String ATTR_ERRORS = "errors";
    private static final String ATTR_FAILURES = "failures";
    private static final String ATTR_SKIPPED = "skipped";
    private static final String ATTR_ASSERTIOMS = "assertions";
    private static final String ATTR_TESTS = "tests";
    //private static final String ATTR_TYPE = "type";
    //private static final String ATTR_MESSAGE = "message";
    private static final String PROPERTIES = "properties";
    private static final String PROPERTY = "property";
    private static final String ATTR_CLASSNAME = "classname";
    private static final String TIMESTAMP = "timestamp";
    private static final String HOSTNAME = "hostname";
    private static final String SYSTEM_ERR = "system-err";

    /**
     * the XML namespace
     */
    @SuppressWarnings("constantname")
    private static final String ns = null;

    private String mHostName = "localhost";

    private File mReportDir = null;

    private String mReportPath = "";

    private TestRunResult mRunResult = new TestRunResult();

    private StringBuilder mSystemError = new StringBuilder();

    /**
     * Sets the report file to use.
     */
    public void setReportDir(File file) {
        mReportDir = file;
    }

    public void setHostName(String hostName) {
        mHostName = hostName;
    }

    /**
     * Returns the {@link TestRunResult}
     *
     * @return the test run results.
     */
    public TestRunResult getRunResult() {
        return mRunResult;
    }

    @Override
    public void testRunStarted(String runName, int numTests) {
        mRunResult = new TestRunResult();
        mRunResult.testRunStarted(runName, numTests);
    }

    @Override
    public void testStarted(TestIdentifier test) {
        mRunResult.testStarted(test);
    }

    @Override
    public void testFailed(TestIdentifier test, String trace) {
        mRunResult.testFailed(test, trace);
    }

    @Override
    public void testAssumptionFailure(TestIdentifier test, String trace) {
        mRunResult.testAssumptionFailure(test, trace);
    }

    @Override
    public void testIgnored(TestIdentifier test) {
        mRunResult.testIgnored(test);
    }

    @Override
    public void testEnded(TestIdentifier test, Map<String, String> testMetrics) {
        mRunResult.testEnded(test, testMetrics);
    }

    @Override
    public void testRunFailed(String errorMessage) {
        mRunResult.testRunFailed(errorMessage);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void testRunStopped(long elapsedTime) {
        mRunResult.testRunStopped(elapsedTime);
    }

    @Override
    public void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {
        mRunResult.testRunEnded(elapsedTime, runMetrics);
        generateDocument(mReportDir, elapsedTime);
    }

    /**
     * Creates a report file and populates it with the report data from the completed tests.
     */
    private void generateDocument(File reportDir, long elapsedTime) {
        String timestamp = getTimestamp();

        OutputStream stream = null;
        try {
            stream = createOutputResultStream(reportDir);
            KXmlSerializer serializer = new KXmlSerializer();
            serializer.setOutput(stream, FileUtil.UTF_8);
            serializer.startDocument(FileUtil.UTF_8, null);
            serializer.setFeature(
                    "http://xmlpull.org/v1/doc/features.html#indent-output", true);
            // TODO: insert build info
            printTestResults(serializer, timestamp, elapsedTime);
            serializer.endDocument();
            String msg = String.format("XML test result file generated at %s. %s",
                    getAbsoluteReportPath(), mRunResult.getTextSummary());
            Log.logAndDisplay(LogLevel.INFO, LOG_TAG, msg);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to generate report data");
            // TODO: consider throwing exception
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public String getAbsoluteReportPath() {
        return mReportPath;
    }

    /**
     * Return the current timestamp as a {@link String}.
     */
    String getTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                Locale.getDefault());
        TimeZone gmt = TimeZone.getTimeZone("UTC");
        dateFormat.setTimeZone(gmt);
        dateFormat.setLenient(true);
        String timestamp = dateFormat.format(new Date());
        return timestamp;
    }

    /**
     * Returns the system error message to be written into XML file in "system-err" element.
     * The element is not created if this returns an empty string.
     */
    public String getSystemError() {
        return mSystemError.toString();
    }

    /**
     * Add system error message.
     */
    public void addSystemError(String systemError) {
        mSystemError.append(systemError);
    }

    /**
     * Creates a {@link File} where the report will be created.
     *
     * @param reportDir the root directory of the report.
     * @return a file
     * @throws IOException
     */
    protected File getResultFile(File reportDir) throws IOException {
        File reportFile = File.createTempFile(TEST_RESULT_FILE_PREFIX, TEST_RESULT_FILE_SUFFIX,
                reportDir);
        Log.i(LOG_TAG, String.format("Created xml report file at %s",
                reportFile.getAbsolutePath()));

        return reportFile;
    }

    /**
     * Creates the output stream to use for test results. Exposed for mocking.
     */
    OutputStream createOutputResultStream(File reportDir) throws IOException {
        File reportFile = getResultFile(reportDir);
        mReportPath = reportFile.getAbsolutePath();
        return new BufferedOutputStream(new FileOutputStream(reportFile));
    }

    protected String getTestSuiteName() {
        return mRunResult.getName();
    }

    void printTestResults(KXmlSerializer serializer, String timestamp, long elapsedTime)
            throws IOException {
        serializer.startTag(ns, TESTSUITE);
        String name = getTestSuiteName();
        if (name != null) {
            serializer.attribute(ns, ATTR_NAME, name);
        }
        serializer.attribute(ns, ATTR_TESTS, Integer.toString(mRunResult.getNumTests()));
        serializer.attribute(ns, ATTR_FAILURES, Integer.toString(
                mRunResult.getNumAllFailedTests()));
        // legacy - there are no errors in JUnit4
        serializer.attribute(ns, ATTR_ERRORS, "0");
        serializer.attribute(ns, ATTR_SKIPPED, Integer.toString(mRunResult.getNumTestsInState(
                TestStatus.IGNORED)));

        serializer.attribute(ns, ATTR_TIME, Double.toString((double) elapsedTime / 1000.f));
        serializer.attribute(ns, TIMESTAMP, timestamp);
        serializer.attribute(ns, HOSTNAME, mHostName);

        serializer.startTag(ns, PROPERTIES);
        for (Map.Entry<String, String> entry : getPropertiesAttributes().entrySet()) {
            serializer.startTag(ns, PROPERTY);
            serializer.attribute(ns, "name", entry.getKey());
            serializer.attribute(ns, "value", entry.getValue());
            serializer.endTag(ns, PROPERTY);
        }
        serializer.endTag(ns, PROPERTIES);

        Map<TestIdentifier, TestResult> testResults = mRunResult.getTestResults();
        for (Map.Entry<TestIdentifier, TestResult> testEntry : testResults.entrySet()) {
            print(serializer, testEntry.getKey(), testEntry.getValue());
        }

        String systemError = getSystemError();
        if (!systemError.isEmpty()) {
            serializer.startTag(ns, SYSTEM_ERR);
            serializer.text(systemError);
            serializer.endTag(ns, SYSTEM_ERR);
        }

        serializer.endTag(ns, TESTSUITE);
    }

    /**
     * Get the properties attributes as key value pairs to be included in the test report.
     */
    @NotNull
    protected Map<String, String> getPropertiesAttributes() {
        return ImmutableMap.of();
    }

    protected String getTestName(TestIdentifier testId) {
        return testId.getTestName();
    }

    void print(KXmlSerializer serializer, TestIdentifier testId, TestResult testResult)
            throws IOException {

        serializer.startTag(ns, TESTCASE);
        serializer.attribute(ns, ATTR_NAME, getTestName(testId));
        serializer.attribute(ns, ATTR_CLASSNAME, testId.getClassName());
        long elapsedTimeMs = testResult.getEndTime() - testResult.getStartTime();
        serializer.attribute(ns, ATTR_TIME, Double.toString((double) elapsedTimeMs / 1000.f));

        switch (testResult.getStatus()) {
            case FAILURE:
                printFailedTest(serializer, FAILURE, testResult.getStackTrace());
                break;
            case ASSUMPTION_FAILURE:
                printFailedTest(serializer, SKIPPED_TAG, testResult.getStackTrace());
                break;
            case IGNORED:
                serializer.startTag(ns, SKIPPED_TAG);
                serializer.endTag(ns, SKIPPED_TAG);
                break;
            default:
                break;
        }

        serializer.endTag(ns, TESTCASE);
    }

    private void printFailedTest(KXmlSerializer serializer, String tag, String stack)
            throws IOException {
        serializer.startTag(ns, tag);
        // TODO: get message of stack trace ?
        // String msg = testResult.getStackTrace();
        // if (msg != null && msg.length() > 0) {
        //     serializer.attribute(ns, ATTR_MESSAGE, msg);
        // }
        // TODO: get class name of stackTrace exception
        // serializer.attribute(ns, ATTR_TYPE, testId.getClassName());
        serializer.text(sanitize(stack));
        serializer.endTag(ns, tag);
    }

    /**
     * Returns the text in a format that is safe for use in an XML document.
     */
    private String sanitize(String text) {
        return text.replace("\0", "<\\0>");
    }
}
