package net.dhleong.wemore;

import net.dhleong.wemore.Wemore.BinaryState;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public class BinaryStateAssert extends
        AbstractAssert<BinaryStateAssert, BinaryState> {

    protected BinaryStateAssert(BinaryState actual) {
        super(actual, BinaryStateAssert.class);
    }

    public BinaryStateAssert isOn() {
        Assertions.assertThat(actual)
            .isNotNull()
            .isEqualTo(BinaryState.ON);
        return this;
    }

    public BinaryStateAssert isOff() {
        Assertions.assertThat(actual)
            .isNotNull()
            .isEqualTo(BinaryState.OFF);
        return this;
    }

    public BinaryStateAssert isUnknown() {
        Assertions.assertThat(actual)
            .isNotNull()
            .isEqualTo(BinaryState.UNKNOWN);
        return this;
    }

    public BinaryStateAssert isKnown() {
        Assertions.assertThat(actual)
            .isNotNull()
            .isNotEqualTo(BinaryState.UNKNOWN);
        return this;
    }

}
