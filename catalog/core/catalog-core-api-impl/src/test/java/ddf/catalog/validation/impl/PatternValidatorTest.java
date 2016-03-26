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

import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.validation.impl.validator.PatternValidator;
import ddf.catalog.validation.report.AttributeValidationReport;

public class PatternValidatorTest {
    private static final String REGEX = "(?i)^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$";

    @Test
    public void testValidValue() {
        validate("12test34@test-789.org", 0);
    }

    @Test
    public void testInvalidValue() {
        validate("12test34@test-789", 1);
    }

    private void validate(final String value, final int expectedErrors) {
        final PatternValidator validator = new PatternValidator(REGEX);
        final AttributeValidationReport report = validator.validate(new AttributeImpl("test",
                value));
        assertThat(report.getAttributeValidationViolations()
                .size(), is(expectedErrors));
    }

    @Test
    public void testEquals() {
        final PatternValidator validator1 = new PatternValidator("test");
        final PatternValidator validator2 = new PatternValidator("test");
        assertThat(validator1.equals(validator1), is(true));
        assertThat(validator1.equals(validator2), is(true));
        assertThat(validator2.equals(validator1), is(true));
    }

    @Test
    public void testEqualsDifferentPattern() {
        final PatternValidator validator1 = new PatternValidator("test1");
        final PatternValidator validator2 = new PatternValidator("test2");
        assertThat(validator1.equals(validator2), is(false));
        assertThat(validator2.equals(validator1), is(false));
    }

    @Test
    public void testHashCode() {
        final PatternValidator validator1 = new PatternValidator("test");
        final PatternValidator validator2 = new PatternValidator("test");
        assertThat(validator1.hashCode(), is(validator2.hashCode()));
    }

    @Test
    public void testHashCodeDifferentPattern() {
        final PatternValidator validator1 = new PatternValidator("test1");
        final PatternValidator validator2 = new PatternValidator("test2");
        assertThat(validator1.hashCode(), not(validator2.hashCode()));
    }
}
