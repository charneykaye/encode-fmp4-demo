// Copyright (c) Charney Kaye Inc. (https://charneykaye.com) All Rights Reserved.
package com.charneykaye;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mp4parser.Container;
import org.mp4parser.muxer.FileDataSourceImpl;
import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.tracks.AACTrackImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;


@RunWith(MockitoJUnitRunner.class)
public class CustomFragmentMp4BuilderTest extends TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(CustomFragmentMp4BuilderTest.class);
    private static final String K = "k";
    private static final String SHIP_KEY = "test5";
    private final String aacFilePath;
    private final String bitrateName;
    private final String m4sFileName;
    private final String m4sFilePath;
    private final String wavFilePath;
    private final int bitrate = 128000;
    private final int sequenceNumber = 151304042;

    private CustomFragmentMp4Builder subject;

    public CustomFragmentMp4BuilderTest(
    ) {
        bitrateName = String.format("%d%s", (int) Math.floor((double) bitrate / 1000), K);
// TODO        String mp4InitFileName = String.format("%s-%s-IS.mp4", SHIP_KEY, bitrateName);
        String tempFilePathPrefix = "/tmp/";
// TODO        String mp4InitFilePath = String.format("%s%s", tempFilePathPrefix, mp4InitFileName);

        String key = String.format("%s-%s-%d", SHIP_KEY, bitrateName, this.sequenceNumber);
        m4sFileName = String.format("%s.m4s", key);
        m4sFilePath = String.format("%s%s.m4s", tempFilePathPrefix, key);
        aacFilePath = String.format("%s%s.aac", tempFilePathPrefix, key);
        wavFilePath = getResourceFile("test5-151304042.wav").getAbsolutePath();
    }

    @Before
    public void setUp() {
        long dspBufferSize = 1024;
        int lengthSeconds = 10;
        subject = new CustomFragmentMp4Builder(bitrate, lengthSeconds, sequenceNumber, dspBufferSize);
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
            Files.deleteIfExists(Path.of(m4sFilePath));
            constructM4S();
            LOG.info("did construct M4S at {} to {}", bitrate, m4sFilePath);
        } catch (Exception e) {
            LOG.error("Failed to construct M4S at {} to {}", bitrate, m4sFilePath, e);
        }

        var boxesActual = getMp4Boxes("/tmp/" + m4sFileName);
        LOG.info("ACTUAL");
        for (var box : boxesActual) LOG.info("{}", box.toString());

        // TODO         var boxesExpected = getMp4Boxes(getResourceFile(m4sFileName).getAbsolutePath());
// TODO        LOG.info("EXPECTED");
// TODO        for (var box : boxesExpected) LOG.info("{}", box.toString());

        assertFileSizeToleranceFromResourceFile("test5-128k-151304042.m4s", "/tmp/test5-128k-151304042.m4s");
    }

    /**
     Check the M4S output

     @throws IOException on failure
     */
    private void constructM4S() throws IOException {
        AACTrackImpl aacTrack = new AACTrackImpl(new FileDataSourceImpl(aacFilePath));
        Movie movie = new Movie();
        movie.addTrack(aacTrack);
        Container mp4file = subject.build(movie);
        FileChannel fc = new FileOutputStream(m4sFilePath).getChannel();
        mp4file.writeContainer(fc);
        fc.close();
    }

}
