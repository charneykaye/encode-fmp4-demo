package com.charneykaye;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableList;
import org.mp4parser.Box;
import org.mp4parser.IsoFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

public abstract class TestBase {

    /**
     Assert that the given test output file is within +/- 2% of the same size as the reference file@param referenceFilePath as source of truth

     @param actualFilePath to test
     */
    protected void assertFileSizeToleranceFromResourceFile(String expectResourcePath, String actualFilePath) {
        assertTrue("Demo output " + actualFilePath + " does not match file size +/-2% of reference audio for " + expectResourcePath + "!",
                isFileSizeWithin(new File(actualFilePath), getResourceFile(expectResourcePath)));
    }

    /**
     Assert size of two different files is within a tolerated threshold

     @param f1 to compare
     @param f2 to compare
     @return true if within tolerance
     */
    protected boolean isFileSizeWithin(File f1, File f2) {
        float deviance = (float) f1.getTotalSpace() / f2.getTotalSpace();
        return (1 - (float) 0.02) < deviance && (1 + (float) 0.02) > deviance;
    }

    /**
     get a file from java resources

     @param filePath to get
     @return File
     */
    protected File getResourceFile(String filePath) {
        ClassLoader classLoader = CustomFragmentMp4BuilderTest.class.getClassLoader();
        URL resource = classLoader.getResource(filePath);
        assert resource != null;
        return new File(resource.getFile());
    }

    /**
     Format multiline text in carriage-return-separated lines

     @param stack of strings to format as multiline
     @return formatted stack trace
     */
    protected String formatMultiline(Object[] stack) {
        String[] stackLines = Arrays.stream(stack).map(String::valueOf).toArray(String[]::new);
        return String.join(System.getProperty("line.separator"), stackLines);
    }

    /**
     Compute the command to run ffmpeg for this chunk printing@param wavFilePath
     */
    protected void encodeAAC(String wavFilePath, String bitrateName, String aacFilePath) throws IOException, InterruptedException {
        Files.deleteIfExists(Path.of(aacFilePath));
        execute(String.join(" ", ImmutableList.of(
                "ffmpeg",
                "-i", wavFilePath,
                "-ac", "2",
                "-c:a", "aac",
                "-b:a", bitrateName,
                "-minrate", bitrateName,
                "-maxrate", bitrateName,
                aacFilePath)));
    }


    /**
     Get the boxes of an MP4 file

     @param path of file to get
     @return mp4 boxes
     @throws IOException on failure
     */
    protected List<Box> getMp4Boxes(String path) throws IOException {
        var dataSource = Files.newByteChannel(Path.of(path));
        var isoFile = new IsoFile(dataSource);
        return isoFile.getBoxes();
    }

    /**
     Execute the given command

     @param cmd command to execute
     @throws IOException          on failure
     @throws InterruptedException on failure
     */
    protected void execute(String cmd) throws IOException, InterruptedException {
        var proc = Runtime.getRuntime().exec(cmd);
        String line;
        List<String> output = Lists.newArrayList();
        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        while ((line = stdError.readLine()) != null) output.add(line);
        if (0 != proc.waitFor()) {
            throw new IOException(String.format("Failed: %s\n\n%s", cmd, formatMultiline(output.toArray())));
        }
    }
}
