package net.dhleong.wemore;

import static net.dhleong.wemore.WemoreAssertions.assertThat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;

import net.dhleong.wemore.Wemore.BinaryState;
import net.dhleong.wemore.Wemore.Device;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rx.Observable;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

public class WemoreTest {

    // extra long in case travis is slow;
    //  we should be safe by a long shot
    static final int TIMEOUT = 5000;

    static final String LIGHTS_BODY =
        "<root><device>" +
        "<friendlyName>Lights</friendlyName>" +
        "</device></root>";

    Ssdp mockSsdp;
    MockWebServer server;

    @Before
    public void setUp() throws IOException {
        mockSsdp = mock(Ssdp.class);
        server = new MockWebServer();
        server.play();
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test(timeout=TIMEOUT)
    public void resolve() throws InterruptedException {

        // initial resolve()
        final URL setupUrl = server.getUrl("/setup.xml");
        server.enqueue(new MockResponse().setBody(LIGHTS_BODY));

        when(mockSsdp.discover(anyString(), anyInt()))
            .thenReturn(Observable.just(new Ssdp.Info("", setupUrl.toString())));

        final Wemore.Device device = new Wemore(mockSsdp).search()
            .toBlocking()
            .first();
        assertThat(device)
            .isNotNull()
            .hasFriendlyNameLike("Lights")
            .hasBinaryState(BinaryState.UNKNOWN);
    }

    @Test(timeout=TIMEOUT)
    public void getBinaryState() throws InterruptedException {

        // initial resolve()
        final URL setupUrl = server.getUrl("/setup.xml");
        server.enqueue(new MockResponse().setBody(LIGHTS_BODY));

        // getBinaryState()
        final URL actionUrl = server.getUrl(Device.BASIC_EVENT_PATH);
        server.enqueue(new MockResponse().setBody(
                    Device.soapPayload("GetBinaryStateResponse", "1")));

        when(mockSsdp.discover(anyString(), anyInt()))
            .thenReturn(Observable.just(new Ssdp.Info("", setupUrl.toString())));

        final Wemore.Device device = new Wemore(mockSsdp).search()
            .toBlocking()
            .first();
        assertThat(device)
            .isNotNull()
            .hasFriendlyNameLike("Lights")
            .hasBinaryState(BinaryState.UNKNOWN);
        server.takeRequest();

        final BinaryState state = device.getBinaryState()
            .toBlocking()
            .first();
        assertThat(state).isOn();
        assertThat(device).hasAnyBinaryState();
        assertThat(server.takeRequest().getPath())
            .isEqualTo(actionUrl.getPath());
    }

    @Test(timeout=TIMEOUT)
    public void toggleBinaryState() throws InterruptedException {
        // initial resolve()
        final URL setupUrl = server.getUrl("/setup.xml");
        server.enqueue(new MockResponse().setBody(LIGHTS_BODY));

        // getBinaryState() (called from toggle)
        final URL actionUrl = server.getUrl(Device.BASIC_EVENT_PATH);
        server.enqueue(new MockResponse().setBody(
                    Device.soapPayload("GetBinaryStateResponse", "0")));

        // setBinaryState() (called from toggle)
        server.enqueue(new MockResponse().setBody(
                    Device.soapPayload("SetBinaryStateResponse", "1")));

        when(mockSsdp.discover(anyString(), anyInt()))
            .thenReturn(Observable.just(new Ssdp.Info("", setupUrl.toString())));

        final Wemore.Device device = new Wemore(mockSsdp).search()
            .toBlocking()
            .first();
        assertThat(device)
            .isNotNull()
            .hasFriendlyNameLike("Lights")
            .hasBinaryState(BinaryState.UNKNOWN);
        server.takeRequest();

        final Wemore.Device updated = device.toggleBinaryState()
            .toBlocking()
            .first();
        assertThat(updated)
            .isSameAs(device)
            .hasBinaryState(BinaryState.ON);

        // first, Get was called
        RecordedRequest getReq = server.takeRequest();
        assertThat(getReq.getPath())
            .isEqualTo(actionUrl.getPath());
        assertThat(getReq.getUtf8Body())
            .contains("u:GetBinaryState");

        // then, Set was called
        RecordedRequest setReq = server.takeRequest();
        assertThat(setReq.getPath())
            .isEqualTo(actionUrl.getPath());
        assertThat(setReq.getUtf8Body())
            .contains("u:SetBinaryState")
            .contains("<BinaryState>1</BinaryState>");
    }

    // annotate and run to test in live
    public void testbed() {
        final Wemore wemore = new Wemore();
        final Wemore.Device device = wemore.search()
            .toBlocking()
            .first();
        System.out.println("Received device:" + device);
        // assertThat(device.hasFriendlyNameLike("Lights")).isTrue();
        assertThat(device).isNotNull()
            // .isNull(); // temp for testing
            ;

        BinaryState state = device.getBinaryState()
                .toBlocking()
                .first();
        assertThat(state).isEqualTo(BinaryState.ON);
        
        device.toggleBinaryState()
            .toBlocking()
            .first();

        Device again = wemore.search()
            .toBlocking()
            .first();

        System.out.println("Again!" + again);

        device
            .toggleBinaryState()
            .toBlocking()
            .first();
    }

}
