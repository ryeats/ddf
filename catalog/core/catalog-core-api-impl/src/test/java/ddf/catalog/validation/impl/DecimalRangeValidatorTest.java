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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.validation.impl.validator.DecimalRangeValidator;
import ddf.catalog.validation.report.AttributeValidationReport;

public class DecimalRangeValidatorTest {
    @Test
    public void testValidFloatValue() {
        validate(new AttributeImpl("test", 15.5f), 14.9, 15.6, 0.01, 0);
    }

    @Test
    public void testValidDoubleValue() {
        validate(new AttributeImpl("test", -0.15), -0.3, -0.1, 0);
        validate(new AttributeImpl("test", 1.191748213), 1.191748213, 1.191748219, 0);
        validate(new AttributeImpl("test", -9915123.1092384727),
                -9915123.1092384729,
                -9915123.1092384721,
                1e-11,
                0);
        validate(new AttributeImpl("test", 22.1234567), 22.1234568, 22.1234570, 1e-7, 0);
    }

    @Test
    public void testInvalidFloatValue() {
        validate(new AttributeImpl("test", 15.7f), 14.9, 15.6, 0.01, 1);
    }

    @Test
    public void testInvalidDoubleValue() {
        validate(new AttributeImpl("test", -0.09), -0.3, -0.1, 1);
        validate(new AttributeImpl("test", 1.191748220), 1.191748213, 1.191748219, 1e-10, 1);
        validate(new AttributeImpl("test", -991512.1092384728),
                -991512.1092384727,
                -991512.1092384721,
                1e-11,
                1);
        validate(new AttributeImpl("test", 22.1234566), 22.1234568, 22.1234570, 1e-7, 1);
    }

    private void validate(final Attribute attribute, final double min, final double max,
            final int expectedErrors) {
        final DecimalRangeValidator validator = new DecimalRangeValidator(min, max);
        validate(validator, attribute, expectedErrors);
    }

    private void validate(final Attribute attribute, final double min, final double max,
            final double epsilon, final int expectedErrors) {
        final DecimalRangeValidator validator = new DecimalRangeValidator(min, max, epsilon);
        validate(validator, attribute, expectedErrors);
    }

    private void validate(final DecimalRangeValidator validator, final Attribute attribute,
            final int expectedErrors) {
        final AttributeValidationReport report = validator.validate(attribute);
        assertThat(report.getAttributeValidationViolations(), hasSize(expectedErrors));
    }

    @Test
    public void testEquals() {
        final DecimalRangeValidator validator1 = new DecimalRangeValidator(-5.1, 17.9);
        final DecimalRangeValidator validator2 = new DecimalRangeValidator(-5.1, 17.9);
        assertThat(validator1.equals(validator1), is(true));
        assertThat(validator1.equals(validator2), is(true));
        assertThat(validator2.equals(validator1), is(true));
    }

    @Test
    public void testEqualsDifferentMin() {
        final DecimalRangeValidator validator1 = new DecimalRangeValidator(-5.1, 17.9);
        final DecimalRangeValidator validator2 = new DecimalRangeValidator(-5.2, 17.9);
        assertThat(validator1.equals(validator2), is(false));
        assertThat(validator2.equals(validator1), is(false));
    }

    @Test
    public void testEqualsDifferentMax() {
        final DecimalRangeValidator validator1 = new DecimalRangeValidator(-5.1, 17.9);
        final DecimalRangeValidator validator2 = new DecimalRangeValidator(-5.1, 18);
        assertThat(validator1.equals(validator2), is(false));
        assertThat(validator2.equals(validator1), is(false));
    }

    @Test
    public void testHashCode() {
        final DecimalRangeValidator validator1 = new DecimalRangeValidator(-5.1, 17.9);
        final DecimalRangeValidator validator2 = new DecimalRangeValidator(-5.1, 17.9);
        assertThat(validator1.hashCode(), is(validator2.hashCode()));
    }

    @Test
    public void testHashCodeDifferentMin() {
        final DecimalRangeValidator validator1 = new DecimalRangeValidator(-5.1, 17.9);
        final DecimalRangeValidator validator2 = new DecimalRangeValidator(-5.0, 17.9);
        assertThat(validator1.hashCode(), not(validator2.hashCode()));
    }

    @Test
    public void testHashCodeDifferentMax() {
        final DecimalRangeValidator validator1 = new DecimalRangeValidator(-5.1, 17.8);
        final DecimalRangeValidator validator2 = new DecimalRangeValidator(-5.1, 17.9);
        assertThat(validator1.hashCode(), not(validator2.hashCode()));
    }
}
