// Copyright (c) Charney Kaye Inc. (https://charneykaye.com) All Rights Reserved.
package com.charneykaye;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mp4parser.boxes.iso14496.part12.MovieFragmentBox;
import org.mp4parser.boxes.iso14496.part12.MovieFragmentHeaderBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
    }

    @Before
    public void setUp() {
    }

    @Test
    public void run() throws IOException {
        LOG.debug("will encode AAC from WAV");
        try {
            encodeAAC(wavFilePath, bitrateName, aacFilePath);
            LOG.info("did encode AAC audio at {} to {}", bitrate, aacFilePath);
        } catch (Exception e) {
            LOG.error("Failed to encode AAC audio at {} to {}", bitrate, aacFilePath, e);
            return;
        }

        LOG.debug("will construct M4S from AAC");
        try {
            constructM4S();
            LOG.info("did construct M4S at {} to {}", bitrate, m4sFilePath);
        } catch (Exception e) {
            LOG.error("Failed to construct M4S at {} to {}", bitrate, m4sFilePath, e);
        }

        var boxesActual = getMp4Boxes(m4sFilePath);
        LOG.info("ACTUAL");
        for (var box : boxesActual) LOG.info("{}", box.toString());

        MovieFragmentBox mfb = (MovieFragmentBox) boxesActual.get(2);
        MovieFragmentHeaderBox mfhb = (MovieFragmentHeaderBox) mfb.getBoxes().get(0);
        assertEquals(sequenceNumber, mfhb.getSequenceNumber());
    }

    /**
     Check the M4S output

     @throws IOException on failure
     */
    private void constructM4S() throws IOException, InterruptedException {
        Files.deleteIfExists(Path.of(m4sFilePath));
        execute(String.join(" ", ImmutableList.of(
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
                "/tmp:period=%s", String.valueOf(sequenceNumber))));
    }

}
