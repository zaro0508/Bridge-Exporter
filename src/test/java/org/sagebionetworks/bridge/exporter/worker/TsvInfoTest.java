package org.sagebionetworks.bridge.exporter.worker;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.sagebionetworks.bridge.exporter.exceptions.BridgeExporterException;
import org.sagebionetworks.bridge.exporter.exceptions.BridgeExporterTsvException;

public class TsvInfoTest {
    private static final List<String> COLUMN_NAME_LIST = ImmutableList.of("foo", "bar");
    private static final String TEST_RECORD_ID = "test record id";

    private File mockFile;
    private PrintWriter mockWriter;
    private TsvInfo tsvInfo;

    @BeforeMethod
    public void before() {
        mockFile = mock(File.class);
        mockWriter = mock(PrintWriter.class);
        tsvInfo = new TsvInfo(COLUMN_NAME_LIST, mockFile, mockWriter);
    }

    @Test
    public void happyCase() throws Exception {
        // set up mocks
        when(mockWriter.checkError()).thenReturn(false);

        // write some lines
        tsvInfo.writeRow(new ImmutableMap.Builder<String, String>().put("foo", "foo value").put("bar", "bar value")
                .build());
        tsvInfo.writeRow(new ImmutableMap.Builder<String, String>().put("foo", "second foo value")
                .put("extraneous", "extraneous value").put("bar", "second bar value").build());
        tsvInfo.writeRow(new ImmutableMap.Builder<String, String>().put("bar", "has bar but not foo").build());
        tsvInfo.addRecordId(TEST_RECORD_ID);
        tsvInfo.flushAndCloseWriter();

        // validate TSV info fields
        assertSame(tsvInfo.getFile(), mockFile);
        assertEquals(tsvInfo.getLineCount(), 3);
        assertEquals(tsvInfo.getRecordIds().get(0), TEST_RECORD_ID);

        // validate writer
        verify(mockWriter).println("foo\tbar");
        verify(mockWriter).println("foo value\tbar value");
        verify(mockWriter).println("second foo value\tsecond bar value");
        verify(mockWriter).println("\thas bar but not foo");
        verify(mockWriter).flush();
        verify(mockWriter).checkError();
        verify(mockWriter).close();
    }

    @Test(expectedExceptions = BridgeExporterException.class, expectedExceptionsMessageRegExp =
            "TSV writer has unknown error")
    public void writerError() throws Exception {
        when(mockWriter.checkError()).thenReturn(true);
        tsvInfo.writeRow(new ImmutableMap.Builder<String, String>().put("foo", "realistic").put("bar", "lines")
                .build());
        tsvInfo.flushAndCloseWriter();
    }

    @Test
    public void initError() {
        Exception testEx = new Exception();
        TsvInfo errorTsvInfo = new TsvInfo(testEx);

        try {
            errorTsvInfo.checkInitAndThrow();
            fail("expected exception");
        } catch (BridgeExporterException ex) {
            assertTrue(ex instanceof BridgeExporterTsvException);
            assertSame(ex.getCause(), testEx);
        }

        try {
            errorTsvInfo.writeRow(ImmutableMap.of());
            fail("expected exception");
        } catch (BridgeExporterException ex) {
            assertTrue(ex instanceof BridgeExporterTsvException);
            assertSame(ex.getCause(), testEx);
        }

        try {
            errorTsvInfo.flushAndCloseWriter();
            fail("expected exception");
        } catch (BridgeExporterException ex) {
            assertTrue(ex instanceof BridgeExporterTsvException);
            assertSame(ex.getCause(), testEx);
        }

        assertNull(errorTsvInfo.getFile());
        assertEquals(errorTsvInfo.getLineCount(), 0);
        assertEquals(errorTsvInfo.getRecordIds().size(), 0);
    }
}
