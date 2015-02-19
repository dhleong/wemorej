package net.dhleong.wemore;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class SsdpTest {

    @Test
    public void infoParsing() {
        final String expectedHost = "192.168.1.56:49153";
        final String expectedLocation = "http://" + expectedHost + "/setup.xml";
        final String expectedService = "urn:Belkin:device:controllee:1";

        final Ssdp.Info info = Ssdp.Info.from(
          "CACHE-CONTROL: max-age=86400\r\n" +
          "DATE: Wed, 18 Feb 2015 20:42:42 GMT\r\n" +
          "EXT: \r\n" +
          "LOCATION: " + expectedLocation + "\r\n" +
          "OPT: \"http://schemas.upnp.org/upnp/1/0/\"; ns=01\r\n" +
          "01-NLS: 7f70757c-1dd2-11b2-aef5-ff8228370291\r\n" +
          "SERVER: Unspecified, UPnP/1.0, Unspecified\r\n" +
          "X-USER-AGENT: redsonic\r\n" +
          "ST: " + expectedService + "\r\n" +
          "USN: uuid:Socket-1_0-221332K0101C68::urn:Belkin:device:controllee:\r\n"
        );

        assertThat(info).isNotNull();
        assertThat(info.host).isEqualTo(expectedHost);
        assertThat(info.location).isEqualTo(expectedLocation);
        assertThat(info.serviceType).isEqualTo(expectedService);
    }
}
