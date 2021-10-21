// Copyright (c) Charney Kaye Inc. (https://charneykaye.com) All Rights Reserved.
package com.charneykaye;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mp4parser.Container;
import org.mp4parser.muxer.FileDataSourceImpl;
import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.builder.DefaultMp4Builder;
import org.mp4parser.muxer.tracks.AACTrackImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;


@RunWith(MockitoJUnitRunner.class)
public class DefaultMp4BuilderTest extends TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultMp4BuilderTest.class);
    private static final String K = "k";
    private static final String SHIP_KEY = "test5";
    private final String aacFilePath;
    private final String bitrateName;
    private final String m4sFileName;
    private final String m4sFilePath;
    private final String wavFilePath;
    private final int bitrate = 128000;

    private DefaultMp4Builder subject;

    public DefaultMp4BuilderTest(
    ) {
        bitrateName = String.format("%d%s", (int) Math.floor((double) bitrate / 1000), K);
// TODO        String mp4InitFileName = String.format("%s-%s-IS-default.mp4", SHIP_KEY, bitrateName);
        String tempFilePathPrefix = "/tmp/";
// TODO        String mp4InitFilePath = String.format("%s%s", tempFilePathPrefix, mp4InitFileName);

        int sequenceNumber = 151304042;
        String key = String.format("%s-%s-%d", SHIP_KEY, bitrateName, sequenceNumber);
        m4sFileName = String.format("%s-default.m4s", key);
        m4sFilePath = String.format("%s%s-default.m4s", tempFilePathPrefix, key);
        aacFilePath = String.format("%s%s-default.aac", tempFilePathPrefix, key);
        wavFilePath = getResourceFile("test5-151304042.wav").getAbsolutePath();
    }

    @Before
    public void setUp() {
        subject = new DefaultMp4Builder();
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

        var boxesActual = getMp4Boxes("/tmp/" + m4sFileName);
        var boxesExpected = getMp4Boxes(getResourceFile(m4sFileName).getAbsolutePath());

        LOG.info("EXPECTED");
        for (var box : boxesExpected) LOG.info("{}", box.toString());
        LOG.info("ACTUAL");
        for (var box : boxesActual) LOG.info("{}", box.toString());

// TODO        assertFileSizeToleranceFromResourceFile(mp4InitFileName, "/tmp/" + mp4InitFileName);
        assertFileSizeToleranceFromResourceFile("test5-128k-151304042-default.m4s", "/tmp/test5-128k-151304042-default.m4s");
    }

    /**
     Check the M4S output

     @throws IOException on failure
     */
    private void constructM4S() throws IOException {
        Files.deleteIfExists(Path.of(m4sFilePath));
        AACTrackImpl aacTrack = new AACTrackImpl(new FileDataSourceImpl(aacFilePath));
        Movie movie = new Movie();
        movie.addTrack(aacTrack);
        Container mp4file = subject.build(movie);
        FileChannel fc = new FileOutputStream(m4sFilePath).getChannel();
        mp4file.writeContainer(fc);
        fc.close();
    }

}
