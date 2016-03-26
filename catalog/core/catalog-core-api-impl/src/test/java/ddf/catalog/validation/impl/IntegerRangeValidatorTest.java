/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.validation.impl;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.validation.impl.validator.IntegerRangeValidator;
import ddf.catalog.validation.report.AttributeValidationReport;

public class IntegerRangeValidatorTest {
    @Test
    public void testValidShortValue() {
        validate(new AttributeImpl("test", (short) 5), -1, 10, 0);
    }

    @Test
    public void testValidIntValue() {
        validate(new AttributeImpl("test", -55000), -100000, -55000, 0);
    }

    @Test
    public void testValidLongValue() {
        validate(new AttributeImpl("test", 951250L), 900000, 1000000, 0);
    }

    @Test
    public void testValidFloatValue() {
        validate(new AttributeImpl("test", 45.01f), 45, 50, 0);
        validate(new AttributeImpl("test", 49.99f), 45, 50, 0);
    }

    @Test
    public void testValidDoubleValue() {
        validate(new AttributeImpl("test", -9.99), -10, 0, 0);
        validate(new AttributeImpl("test", -0.01), -10, 0, 0);
    }

    @Test
    public void testInvalidShortValue() {
        validate(new AttributeImpl("test", (short) -2), -1, 10, 1);
    }

    @Test
    public void testInvalidIntValue() {
        validate(new AttributeImpl("test", -54999), -100000, -55000, 1);
    }

    @Test
    public void testInvalidLongValue() {
        validate(new AttributeImpl("test", 20000000L), 9000000, 1000000, 1);
    }

    @Test
    public void testInvalidFloatValue() {
        validate(new AttributeImpl("test", 44.99f), 45, 50, 1);
        validate(new AttributeImpl("test", 50.01f), 45, 50, 1);
    }

    @Test
    public void testInvalidDoubleValue() {
        validate(new AttributeImpl("test", -10.01), -10, 0, 1);
        validate(new AttributeImpl("test", 0.01), -10, 0, 1);
    }

    private void validate(final Attribute attribute, final long min, final long max,
            final int expectedErrors) {
        final IntegerRangeValidator validator = new IntegerRangeValidator(min, max);
        final AttributeValidationReport report = validator.validate(attribute);
        assertThat(report.getAttributeValidationViolations()
                .size(), is(expectedErrors));
    }

    @Test
    public void testEquals() {
        final IntegerRangeValidator validator1 = new IntegerRangeValidator(-55, 179);
        final IntegerRangeValidator validator2 = new IntegerRangeValidator(-55, 179);
        assertThat(validator1.equals(validator1), is(true));
        assertThat(validator1.equals(validator2), is(true));
        assertThat(validator2.equals(validator1), is(true));
    }

    @Test
    public void testEqualsDifferentMin() {
        final IntegerRangeValidator validator1 = new IntegerRangeValidator(-55, 179);
        final IntegerRangeValidator validator2 = new IntegerRangeValidator(0, 179);
        assertThat(validator1.equals(validator2), is(false));
        assertThat(validator2.equals(validator1), is(false));
    }

    @Test
    public void testEqualsDifferentMax() {
        final IntegerRangeValidator validator1 = new IntegerRangeValidator(-55, 179);
        final IntegerRangeValidator validator2 = new IntegerRangeValidator(-55, 0);
        assertThat(validator1.equals(validator2), is(false));
        assertThat(validator2.equals(validator1), is(false));
    }

    @Test
    public void testHashCode() {
        final IntegerRangeValidator validator1 = new IntegerRangeValidator(-55, 179);
        final IntegerRangeValidator validator2 = new IntegerRangeValidator(-55, 179);
        assertThat(validator1.hashCode(), is(validator2.hashCode()));
    }

    @Test
    public void testHashCodeDifferentMin() {
        final IntegerRangeValidator validator1 = new IntegerRangeValidator(-55, 179);
        final IntegerRangeValidator validator2 = new IntegerRangeValidator(0, 179);
        assertThat(validator1.hashCode(), not(validator2.hashCode()));
    }

    @Test
    public void testHashCodeDifferentMax() {
        final IntegerRangeValidator validator1 = new IntegerRangeValidator(-55, 179);
        final IntegerRangeValidator validator2 = new IntegerRangeValidator(-55, 0);
        assertThat(validator1.hashCode(), not(validator2.hashCode()));
    }
}
