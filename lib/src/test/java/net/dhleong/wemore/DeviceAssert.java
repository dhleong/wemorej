package net.dhleong.wemore;

import net.dhleong.wemore.Wemore.BinaryState;
import net.dhleong.wemore.Wemore.Device;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public class DeviceAssert extends AbstractAssert<DeviceAssert, Wemore.Device> {

    protected DeviceAssert(Device actual) {
        super(actual, DeviceAssert.class);
    }

    public DeviceAssert hasFriendlyNameLike(final String name) {
        isNotNull();
        if (!actual.hasFriendlyNameLike(name)) {
            // close enough, I guess
            Assertions.assertThat(actual.getFriendlyName()).contains(name);
        }
        return this;
    }

    public DeviceAssert hasAnyBinaryState() {
        WemoreAssertions.assertThat(actual.binaryState)
            .isKnown();
        return this;
    }

    public DeviceAssert hasBinaryState(BinaryState state) {
        Assertions.assertThat(actual.binaryState)
            .isEqualTo(state);
        return this;
    }
}
