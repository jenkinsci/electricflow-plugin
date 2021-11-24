package org.jenkinsci.plugins.electricflow;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class MultipartUtilityTest {

    @Rule
    public final WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Test
    @Issue("NTVEPLUGIN-457")
    public void testMultipartUploadMultipleFile() throws NoSuchFieldException, IllegalAccessException, IOException {
        wireMockRule.resetAll();

        stubFor(post(urlEqualTo("/commander/cgi-bin/publishArtifactAPI.cgi"))
            .willReturn(aResponse()
                .withStatus(200)));

        File file1 = new File(Objects.requireNonNull(MultipartUtilityTest.class.getResource("file1.txt")).getFile());
        File file2 = new File(Objects.requireNonNull(MultipartUtilityTest.class.getResource("file2.dat")).getFile());
        File file3 = new File(Objects.requireNonNull(MultipartUtilityTest.class.getResource("file3.xml")).getFile());

        MultipartUtility utility = new MultipartUtility(
            wireMockRule.url("/commander/cgi-bin/publishArtifactAPI.cgi"),
            StandardCharsets.UTF_8.name(),
            true);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter osWriter = new OutputStreamWriter(baos, StandardCharsets.UTF_8.name())) {

            // Reflection to capture the stream being printed
            Field osField = MultipartUtility.class.getDeclaredField("outputStream");
            osField.setAccessible(true);
            osField.set(utility, baos);
            ((OutputStream) osField.get(utility)).close();

            Field writerField = MultipartUtility.class.getDeclaredField("writer");
            writerField.setAccessible(true);
            writerField.set(utility, new PrintWriter(osWriter));

            Field boundaryField = MultipartUtility.class.getDeclaredField("boundary");
            boundaryField.setAccessible(true);
            String boundary = (String) boundaryField.get(utility);

            Field lineFeedField = MultipartUtility.class.getDeclaredField("LINE_FEED");
            lineFeedField.setAccessible(true);
            String lineFeed = (String) lineFeedField.get(utility);

            // add file parts
            utility.addFilePart("files", file1, file1.getParentFile().getAbsolutePath());
            utility.addFilePart("files", file2, file2.getParentFile().getAbsolutePath());
            utility.addFilePart("files", file3, file3.getParentFile().getAbsolutePath());
            utility.finish();

            // Check the boundary are properly set, counting occurrences of opening/closing boundaries 
            // (split would return number of occurrences + 1)
            MatcherAssert.assertThat(baos.toString().split("--" + boundary + lineFeed), Matchers.arrayWithSize(4));
            MatcherAssert.assertThat(baos.toString().split(lineFeed + "--" + boundary), Matchers.arrayWithSize(4));
        }
    }

    @Test
    @Issue("NTVEPLUGIN-457")
    public void testMultipartUploadSingleFile() throws NoSuchFieldException, IllegalAccessException, IOException {
        wireMockRule.resetAll();

        stubFor(post(urlEqualTo("/commander/cgi-bin/publishArtifactAPI.cgi"))
            .willReturn(aResponse()
                .withStatus(200)));

        File file1 = new File(Objects.requireNonNull(MultipartUtilityTest.class.getResource("file1.txt")).getFile());

        MultipartUtility utility = new MultipartUtility(
            wireMockRule.url("/commander/cgi-bin/publishArtifactAPI.cgi"),
            StandardCharsets.UTF_8.name(),
            true);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter osWriter = new OutputStreamWriter(baos, StandardCharsets.UTF_8.name())) {

            // Reflection to capture the stream being printed
            Field osField = MultipartUtility.class.getDeclaredField("outputStream");
            osField.setAccessible(true);
            osField.set(utility, baos);
            ((OutputStream) osField.get(utility)).close();

            Field writerField = MultipartUtility.class.getDeclaredField("writer");
            writerField.setAccessible(true);
            writerField.set(utility, new PrintWriter(osWriter));

            Field boundaryField = MultipartUtility.class.getDeclaredField("boundary");
            boundaryField.setAccessible(true);
            String boundary = (String) boundaryField.get(utility);

            Field lineFeedField = MultipartUtility.class.getDeclaredField("LINE_FEED");
            lineFeedField.setAccessible(true);
            String lineFeed = (String) lineFeedField.get(utility);

            // add file parts
            utility.addFilePart("files", file1, file1.getParentFile().getAbsolutePath());
            utility.finish();

            // Check the boundary are properly set 
            MatcherAssert.assertThat(baos.toString().split("--" + boundary + lineFeed), Matchers.arrayWithSize(2));
            MatcherAssert.assertThat(baos.toString().split(lineFeed + "--" + boundary), Matchers.arrayWithSize(2));
        }
    }
}
