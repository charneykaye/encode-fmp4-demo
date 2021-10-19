// Copyright (c) Charney Kaye Inc. (https://charneykaye.com) All Rights Reserved.
package com.charneykaye;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mp4parser.Box;
import org.mp4parser.Container;
import org.mp4parser.IsoFile;
import org.mp4parser.muxer.FileDataSourceImpl;
import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.tracks.AACTrackImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@RunWith(MockitoJUnitRunner.class)
public class ChunkMp4BuilderTest {
    private static final Logger LOG = LoggerFactory.getLogger(ChunkMp4BuilderTest.class);
    private static final int AUDIO_CHANNELS = 2;
    private static final String K = "k";
    private static final String SHIP_KEY = "test5";
    private final String aacFilePath;
    private final String bitrateName;
    private final String m4sFileName;
    private final String m4sFilePath;
    private final String mp4InitFileName;
    private final String mp4InitFilePath;
    private final String tempFilePathPrefix = "/tmp/";
    private final String wavFilePath;
    private final int bitrate = 128000;
    private final int lengthSeconds = 10;
    private final int printTimeoutSeconds = 30;
    private final int sampleRate = 48000;
    private final int sequenceNumber = 151304042;
    private final long dspBufferSize = 1024;

    private ChunkMp4Builder subject;

    public ChunkMp4BuilderTest(
    ) {
        bitrateName = String.format("%d%s", (int) Math.floor((double) bitrate / 1000), K);
        mp4InitFileName = String.format("%s-%s-IS.mp4", SHIP_KEY, bitrateName);
        mp4InitFilePath = String.format("%s%s", tempFilePathPrefix, mp4InitFileName);

        String key = String.format("%s-%s-%d", SHIP_KEY, bitrateName, this.sequenceNumber);
        m4sFileName = String.format("%s.m4s", key);
        m4sFilePath = String.format("%s%s.m4s", tempFilePathPrefix, key);
        aacFilePath = String.format("%s%s.aac", tempFilePathPrefix, key);
        wavFilePath = String.format("%s%s-%s.wav", tempFilePathPrefix, SHIP_KEY, this.sequenceNumber);
    }

    /**
     Format multiline text in carriage-return-separated lines

     @param stack of strings to format as multiline
     @return formatted stack trace
     */
    static String formatMultiline(Object[] stack) {
        String[] stackLines = Arrays.stream(stack).map(String::valueOf).toArray(String[]::new);
        return String.join(System.getProperty("line.separator"), stackLines);
    }

    /**
     Assert that the given test output file matches the reference file

     @param targetFilePath    to test
     @param referenceFilePath as source of truth
     @throws IOException on failure to read
     */
    @SuppressWarnings("UnstableApiUsage")
    public static void assertFileMatchesResourceFile(String targetFilePath, String referenceFilePath) throws IOException {
        assertTrue("Demo output " + targetFilePath + " does not match reference audio for " + referenceFilePath + "!",
                com.google.common.io.Files.equal(new File(targetFilePath), getResourceFile(referenceFilePath)));
    }

    /**
     Assert size of two different files is within a tolerated threshold

     @param f1 to compare
     @param f2 to compare
     @return true if within tolerance
     */
    static boolean isFileSizeWithin(File f1, File f2) {
        float deviance = (float) f1.getTotalSpace() / f2.getTotalSpace();
        return (1 - (float) 0.02) < deviance && (1 + (float) 0.02) > deviance;
    }

    /**
     get a file from java resources

     @param filePath to get
     @return File
     */
    static File getResourceFile(String filePath) {
        InternalResource internalResource = new InternalResource(filePath);
        return internalResource.getFile();
    }

    /**
     Compute the command to run ffmpeg for this chunk printing
     */
    private void encodeAAC() throws IOException, InterruptedException {
        var cmd = String.join(" ", ImmutableList.of(
                "ffmpeg",
                "-i", wavFilePath,
                "-ac", "2",
                "-c:a", "aac",
                "-b:a", bitrateName,
                "-minrate", bitrateName,
                "-maxrate", bitrateName,
                aacFilePath));
        Files.deleteIfExists(Path.of(aacFilePath));
        var proc = Runtime.getRuntime().exec(cmd);
        String line;
        List<String> output = Lists.newArrayList();
        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        while ((line = stdError.readLine()) != null) output.add(line);
        if (0 != proc.waitFor()) {
            LOG.error("Failed: {}\n\n{}", cmd, formatMultiline(output.toArray()));
            throw new IOException(String.format("Failed: %s\n\n%s", cmd, formatMultiline(output.toArray())));
        }
    }

    /**
     Check the M4S output

     @throws IOException on failure
     */
    private void constructM4S() throws IOException {
        AACTrackImpl aacTrack = new AACTrackImpl(new FileDataSourceImpl(aacFilePath));
        Movie movie = new Movie();
        movie.addTrack(aacTrack);
        Container mp4file = new ChunkMp4Builder(sampleRate, lengthSeconds, sequenceNumber, dspBufferSize).build(movie);
        FileChannel fc = new FileOutputStream(m4sFilePath).getChannel();
        mp4file.writeContainer(fc);
        fc.close();
    }

    @Before
    public void setUp() {
        subject = new ChunkMp4Builder(48000, 10, 1, 1024);
    }

    @Test
    public void run() throws IOException {

        LOG.debug("will encode AAC from WAV");
        try {
            encodeAAC();
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

        var loader = ChunkMp4BuilderTest.class.getClassLoader();
        var input = loader.getResourceAsStream("ogg_decoding/test5-1633586832900943.ogg");
        assertFileMatchesResourceFile("/tmp/test5-151304042.wav", "chunk_reference_outputs/test5-151304042.wav");

/*
  TODO assertions
    assertFileSizeToleranceFromResourceFile("/tmp/test5-128kbps-151304042.m4s", "chunk_reference_outputs/test5-128kbps-151304042.m4s");
    assertFileSizeToleranceFromResourceFile("/tmp/test5-128kbps-IS.mp4", "chunk_reference_outputs/test5-128kbps-IS.mp4");
*/


        var boxesActual = getMp4Boxes("/tmp/test5-128k-151304042.m4s");
        var boxesExpected = getMp4Boxes(new InternalResource("chunk_reference_outputs/test5-128kbps-151304042.m4s").getFile().getAbsolutePath());

        LOG.info("EXPECTED");
        for (var box : boxesExpected) LOG.info("{}", box.toString());
        LOG.info("ACTUAL");
        for (var box : boxesActual) LOG.info("{}", box.toString());

        assertEquals(boxesExpected, boxesActual);
    }

    /**
     Get the boxes of an MP4 file

     @param path of file to get
     @return mp4 boxes
     @throws IOException on failure
     */
    private List<Box> getMp4Boxes(String path) throws IOException {
        var dataSource = Files.newByteChannel(Path.of(path));
        var isoFile = new IsoFile(dataSource);
        return isoFile.getBoxes();
    }

    /**
     Get a File() for something in the resources folder
     */
    public static class InternalResource {

        private File file;

        public InternalResource(String fileName) {
            ClassLoader classLoader = getClass().getClassLoader();
            URL resource = classLoader.getResource(fileName);
            if (resource != null) {
                file = new File(resource.getFile());
            }
        }

        public File getFile() {
            return file;
        }
    }

}
