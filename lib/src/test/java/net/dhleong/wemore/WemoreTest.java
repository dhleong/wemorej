package net.dhleong.wemore;

import static org.assertj.core.api.Assertions.assertThat;

import net.dhleong.wemore.Wemore.BinaryState;

import org.junit.Test;

public class WemoreTest {

    @Test
    public void test() throws InterruptedException {
        // TODO Mock Ssdp and use mockwebserver
        //  for a real unit test

        final Wemore.Device device = new Wemore().search()
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
            .first()
            .toggleBinaryState()
            .toBlocking()
            .first();
    }

}
