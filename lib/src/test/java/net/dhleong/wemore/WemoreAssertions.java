package net.dhleong.wemore;

import org.assertj.core.api.Assertions;

public class WemoreAssertions extends Assertions {

    public static BinaryStateAssert assertThat(Wemore.BinaryState state) {
        return new BinaryStateAssert(state);
    }

    public static DeviceAssert assertThat(Wemore.Device device) {
        return new DeviceAssert(device);
    }
}
