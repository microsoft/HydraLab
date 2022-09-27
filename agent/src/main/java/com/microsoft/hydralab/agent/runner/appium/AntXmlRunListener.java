/* Schmant, the build tool, http://www.schmant.org
 * Copyright (C) 2007, 2008 Karl Gustafsson, Holocene Software, 
 * http://www.holocene.se
 *
 * Schmant is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Schmant is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
/* This class contains code copied from Apache Ant's
 * org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter class
 * from Ant verson 1.7.1.
 */
// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.agent.runner.appium;

import com.microsoft.hydralab.common.util.DOMElementWriter;
import com.microsoft.hydralab.common.util.DateUtils;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * This {@link RunListener} produces an XML report formatted like Ant's JUnit
 * XML report.
 *
 * @author Karl Gustafsson
 * @task// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package org.junit.junit4
 * @since 0.9
 */
public class AntXmlRunListener extends RunListener {
    private static final double ONE_SECOND = 1000.0;

    // XML constants

    /**
     * the testsuite element
     */
    private static String TESTSUITE = "testsuite";

    /**
     * the testcase element
     */
    private static String TESTCASE = "testcase";

    /**
     * the failure element
     */
    private static String FAILURE = "failure";

    /** the system-err element */
    //	private static String SYSTEM_ERR = "system-err";
    /** the system-out element */
    //	private static String SYSTEM_OUT = "system-out";
    /**
     * name attribute for property, testcase and testsuite elements
     */
    private static String ATTR_NAME = "name";

    /**
     * time attribute for testcase and testsuite elements
     */
    private static String ATTR_TIME = "time";

    /**
     * errors attribute for testsuite elements
     */
    private static String ATTR_ERRORS = "errors";

    /**
     * failures attribute for testsuite elements
     */
    private static String ATTR_FAILURES = "failures";

    /**
     * tests attribute for testsuite elements
     */
    private static String ATTR_TESTS = "tests";

    /**
     * type attribute for failure and error elements
     */
    private static String ATTR_TYPE = "type";

    /**
     * message attribute for failure elements
     */
    private static String ATTR_MESSAGE = "message";

    /**
     * the properties element
     */
    private static String PROPERTIES = "properties";

    /**
     * classname attribute for testcase elements
     */
    private static String ATTR_CLASSNAME = "classname";

    /**
     * timestamp of test cases
     */
    private static String TIMESTAMP = "timestamp";

    /**
     * name of host running the tests
     */
    private static String HOSTNAME = "hostname";

    private OutputStream outputStream;
    /**
     * The XML document.
     */
    private Document m_doc;
    /**
     * The wrapper for the whole testsuite.
     */
    private Element m_rootElement;
    /**
     * Mapping between test Description:s -> Start timestamp (Long)
     */
    private final Map<Description, Long> m_testStarts = new HashMap<>();
    /**
     * Mapping between test Description:s -> Failure objects
     */
    private final Map<Description, Failure> m_failedTests = new HashMap<>();
    /**
     * Mapping between test Description:s -> XML Element:s
     */
    private final Map<Description, Element> m_testElements = new HashMap<>();

    String mReportPath = "";
    /**
     * Convenient method to retrieve the full stacktrace from a given exception.
     *
     * @param t the exception to get the stacktrace from.
     * @return the stacktrace from the given exception.
     */
    private static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        t.printStackTrace(pw);
        pw.flush();
        pw.close();
        return sw.toString();
    }

    /**
     * Returns a filtered stack trace.
     * This is ripped out of junit.runner.BaseTestRunner.
     *
     * @param t the exception to filter.
     * @return the filtered stack trace.
     */
    private static String getFilteredTrace(Throwable t) {
        return filterStack(getStackTrace(t));
    }

    /**
     * Filters stack frames from internal JUnit and Ant classes
     *
     * @param stack the stack trace to filter.
     * @return the filtered stack.
     */
    public static String filterStack(String stack) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        StringReader sr = new StringReader(stack);
        BufferedReader br = new BufferedReader(sr);

        String line;
        try {
            while ((line = br.readLine()) != null) {
                if (!filterLine(line)) {
                    pw.println(line);
                }
            }
        } catch (Exception e) {
            return stack; // return the stack unfiltered
        }
        return sw.toString();
    }

    private static final String[] DEFAULT_TRACE_FILTERS = new String[]{"junit.framework.TestCase", "junit.framework.TestResult", "junit.framework.TestSuite", "junit.framework.Assert.", // don't filter AssertionFailure
            "junit.swingui.TestRunner", "junit.awtui.TestRunner", "junit.textui.TestRunner", "java.lang.reflect.Method.invoke(", "sun.reflect.", "org.apache.tools.ant.",
            // JUnit 4 support:
            "org.junit.", "junit.framework.JUnit4TestAdapter",
            // See wrapListener for reason:
            "Caused by: java.lang.AssertionError", " more",};

    private static boolean filterLine(String line) {
        for (int i = 0; i < DEFAULT_TRACE_FILTERS.length; i++) {
            if (line.indexOf(DEFAULT_TRACE_FILTERS[i]) != -1) {
                return true;
            }
        }
        return false;
    }

    private static String getTestCaseName(String s) {
        if (s == null) {
            return "unknown";
        }

        if (s.endsWith(")")) {
            int paren = s.lastIndexOf('(');
            return s.substring(0, paren);
        } else {
            return s;
        }
    }

    private static String getTestCaseClassName(String s) {
        if (s == null) {
            return "unknown";
        }

        // JUnit 4 wraps solo tests this way. We can extract
        // the original test name with a little hack.
        int paren = s.lastIndexOf('(');
        if (paren != -1 && s.endsWith(")")) {
            return s.substring(paren + 1, s.length() - 1);
        } else {
            return s;
        }
    }

    private static DocumentBuilder getDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public AntXmlRunListener(File filePath) {
        if (filePath != null) {
            File target = new File(filePath, "junit_report.xml");
            mReportPath = target.getAbsolutePath();
            try {
                this.outputStream = new BufferedOutputStream(new FileOutputStream(target));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public String getAbsoluteReportPath() {
        return mReportPath ;
    }
    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    /**
     * get the local hostname
     *
     * @return the name of the local host, or "localhost" if we cannot work it out
     */
    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "localhost";
        }
    }

    /**
     * The whole test suite started.
     *
     * @param descr The test suite description.
     */
    public void testRunStarted(Description descr) {
        m_doc = getDocumentBuilder().newDocument();
        m_rootElement = m_doc.createElement(TESTSUITE);

        String n = descr.getDisplayName();
        m_rootElement.setAttribute(ATTR_NAME, n == null ? "unknown" : n);

        //add the timestamp
        final String timestamp = DateUtils.format(new Date(), DateUtils.ISO8601_DATETIME_PATTERN);
        m_rootElement.setAttribute(TIMESTAMP, timestamp);
        //and the hostname.
        m_rootElement.setAttribute(HOSTNAME, getHostname());

        // Output properties
        Element propsElement = m_doc.createElement(PROPERTIES);
        m_rootElement.appendChild(propsElement);
        // Where do these come from?
        //		Properties props = suite.getProperties();
        //		if (props != null)
        //		{
        //			Enumeration e = props.propertyNames();
        //			while (e.hasMoreElements())
        //			{
        //				String name = (String) e.nextElement();
        //				Element propElement = doc.createElement(PROPERTY);
        //				propElement.setAttribute(ATTR_NAME, name);
        //				propElement.setAttribute(ATTR_VALUE, props.getProperty(name));
        //				propsElement.appendChild(propElement);
        //			}
        //		}
    }

    /**
     * Interface TestListener.
     * <p/>
     * <p>A new Test is started.
     *
     * @param descr The test description.
     */
    public void testStarted(Description descr) {
        m_testStarts.put(descr, System.currentTimeMillis());
    }

    private void formatError(String type, Failure f) {
        testFinished(f.getDescription());
        m_failedTests.put(f.getDescription(), f);

        Element nested = m_doc.createElement(type);
        Element currentTest = (Element) m_testElements.get(f.getDescription());

        currentTest.appendChild(nested);

        String message = f.getMessage();
        if (message != null && message.length() > 0) {
            nested.setAttribute(ATTR_MESSAGE, message);
        }
        nested.setAttribute(ATTR_TYPE, f.getDescription().getDisplayName());

        String strace = getFilteredTrace(f.getException());
        Text trace = m_doc.createTextNode(strace);
        nested.appendChild(trace);
    }

    /**
     * Interface TestListener for JUnit &lt;= 3.4.
     * <p/>
     * <p>A Test failed.
     *
     * @param f The failure.
     */
    public void testFailure(Failure f) {
        formatError(FAILURE, f);
    }

    public void testAssumptionFailure(Failure f) {
        formatError(FAILURE, f);
    }

    /**
     * Interface TestListener.
     * <p/>
     * <p>A Test is finished.
     *
     * @param descr The test description.
     */
    public void testFinished(Description descr) {
        // Fix for bug #5637 - if a junit.extensions.TestSetup is
        // used and throws an exception during setUp then startTest
        // would never have been called
        if (!m_testStarts.containsKey(descr)) {
            testStarted(descr);
        }

        Element currentTest = null;
        if (!m_failedTests.containsKey(descr)) {
            //			Test test = (Test) descr.getAnnotation(Test.class);

            currentTest = m_doc.createElement(TESTCASE);
            String n = getTestCaseName(descr.getDisplayName());
            currentTest.setAttribute(ATTR_NAME, n == null ? "unknown" : n);
            // a TestSuite can contain Tests from multiple classes,
            // even tests with the same name - disambiguate them.
            currentTest.setAttribute(ATTR_CLASSNAME, getTestCaseClassName(descr.getDisplayName()));
            m_rootElement.appendChild(currentTest);
            m_testElements.put(descr, currentTest);
        } else {
            currentTest = (Element) m_testElements.get(descr);
        }

        Long l = (Long) m_testStarts.get(descr);
        currentTest.setAttribute(ATTR_TIME, "" + ((System.currentTimeMillis() - l.longValue()) / ONE_SECOND));
    }

    /**
     * The whole test suite ended.
     *
     * @param result The test suite result.
     * @throws BuildException on error.
     */
    public void testRunFinished(Result result) {
        try {
            try {
                m_rootElement.setAttribute(ATTR_TESTS, "" + result.getRunCount());
                m_rootElement.setAttribute(ATTR_FAILURES, "" + result.getFailureCount());
                // JUnit4 does not seem to discern between failures and errors.
                m_rootElement.setAttribute(ATTR_ERRORS, "" + 0);
                m_rootElement.setAttribute(ATTR_TIME, "" + (result.getRunTime() / ONE_SECOND));
                Writer wri = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF8"));
                wri.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
                (new DOMElementWriter()).write(m_rootElement, wri, 0, "  ");
                wri.flush();
            } finally {
                outputStream.close();
            }
        } catch (IOException exc) {
            throw new RuntimeException("Unable to write log file", exc);
        }
    }

    @SuppressWarnings("deprecation")
    protected void finalize() throws Throwable {
        if (outputStream != null) {
            outputStream.close();
        }
        super.finalize();
    }

    //
    //	/**
    //	 * Where to write the log to.
    //	 */
    //	private OutputStream out;
    //
    //	/** {@inheritDoc}. */
    //	public void setOutput(OutputStream out)
    //	{
    //		this.out = out;
    //	}
    //
    //	/** {@inheritDoc}. */
    //	public void setSystemOutput(String out)
    //	{
    //		formatOutput(SYSTEM_OUT, out);
    //	}
    //
    //	/** {@inheritDoc}. */
    //	public void setSystemError(String out)
    //	{
    //		formatOutput(SYSTEM_ERR, out);
    //	}
    //
    //	private void formatOutput(String type, String output)
    //	{
    //		Element nested = doc.createElement(type);
    //		rootElement.appendChild(nested);
    //		nested.appendChild(doc.createCDATASection(output));
    //	}

}
