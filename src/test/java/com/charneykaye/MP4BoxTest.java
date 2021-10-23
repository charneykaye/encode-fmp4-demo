// Copyright (c) Charney Kaye Inc. (https://charneykaye.com) All Rights Reserved.
package com.charneykaye;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mp4parser.boxes.iso14496.part12.MovieFragmentBox;
import org.mp4parser.boxes.iso14496.part12.MovieFragmentHeaderBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.joda.time.DateTimeConstants.MILLIS_PER_SECOND;
import static org.junit.Assert.assertEquals;


@SuppressWarnings("FieldCanBeLocal")
@RunWith(MockitoJUnitRunner.class)
public class MP4BoxTest extends TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(MP4BoxTest.class);
    private static final String K = "k";
    private static final String shipKey = "test5";
    private static final int bitrate = 128000;
    private static final int lengthSeconds = 10;
    private static final int sampleRate = 48000;
    private static final int sequenceNumber = 151304042;
    private static final long dspBufferSize = 1024;
    private final String aacFilePath;
    private final String bitrateName;
    private final String m4sFileName;
    private final String m4sFilePath;
    private final String wavFilePath;
    private final String tempPlaylistPath;
    private final String segmentName;
    private final String initSegPath;
    private final String testMp4Path;

    public MP4BoxTest(
    ) {
        bitrateName = String.format("%d%s", (int) Math.floor((double) bitrate / 1000), K);
        String tempFilePathPrefix = "/tmp/";

        String key = String.format("%s-%s-%d", shipKey, bitrateName, sequenceNumber);
        m4sFileName = String.format("%s.m4s", key);
        m4sFilePath = String.format("%s%s-1.m4s", tempFilePathPrefix, shipKey);
        tempPlaylistPath = String.format("%s%s", tempFilePathPrefix, shipKey);
        segmentName = String.format("%s-", shipKey);
        aacFilePath = String.format("%s%s.aac", tempFilePathPrefix, key);
        wavFilePath = getResourceFile("test5-151304042.wav").getAbsolutePath();
        initSegPath = getResourceFile("test5-128k-IS.mp4").getAbsolutePath();
        testMp4Path = String.format("%s%s.mp4", tempFilePathPrefix, "test-mp4box");
    }

    @Before
    public void setUp() {
    }

    @Test
    public void run() throws IOException, InterruptedException {
        LOG.debug("will encode AAC from WAV");
        encodeAAC(wavFilePath, bitrateName, aacFilePath);
        LOG.info("did encode AAC audio at {} to {}", bitrate, aacFilePath);

        LOG.debug("will construct M4S from AAC");
        constructM4S();
        LOG.info("did construct M4S at {} to {}", bitrate, m4sFilePath);
        var m4sFile = assertValidMp4("ACTUAL m4S FRAGMENT", m4sFilePath);
        MovieFragmentBox mfb = (MovieFragmentBox) org.mp4parser.tools.Path.getPath(m4sFile, "moof");
        MovieFragmentHeaderBox mfhb = (MovieFragmentHeaderBox) mfb.getBoxes().get(0);
        assertEquals(sequenceNumber, mfhb.getSequenceNumber());

        LOG.debug("will concatenate a test .mp4 from the initial .mp4 and first .m4s fragment");
        Files.deleteIfExists(Path.of(testMp4Path));
        try (
                FileChannel fromInit = new FileInputStream(initSegPath).getChannel();
                FileChannel fromFrag = new FileInputStream(m4sFilePath).getChannel();
                FileChannel toTest = new FileOutputStream(testMp4Path, true).getChannel();
        ) {
            toTest.transferFrom(fromInit, toTest.size(), fromInit.size());
            toTest.transferFrom(fromFrag, toTest.size(), fromFrag.size());
            var testMp4File = assertValidMp4("ACTUAL CONCATENATED MP4", testMp4Path);
        }
        LOG.info("did concatenate test .mp4 at {} to {}", bitrate, testMp4Path);
    }

    /**
     Check the M4S output

     @throws IOException on failure
     */
    private void constructM4S() throws IOException, InterruptedException {
        Files.deleteIfExists(Path.of(m4sFilePath));
        execute(List.of(
                "MP4Box",
                "-add", aacFilePath,
                "-dash", String.valueOf(lengthSeconds * MILLIS_PER_SECOND),
                "-frag", String.valueOf(lengthSeconds * MILLIS_PER_SECOND),
                "-idx", String.valueOf(sequenceNumber - 1),
                "-moof-sn", String.valueOf(sequenceNumber - 1),
                "-out", tempPlaylistPath,
                "-profile", "live",
                "-segment-name", segmentName,
                "-v",
                "/tmp:period=%s", String.valueOf(sequenceNumber)));
    }

}
