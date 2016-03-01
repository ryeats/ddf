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
import static org.junit.Assert.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.junit.Test;

import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.validation.impl.validator.FutureDateValidator;
import ddf.catalog.validation.report.AttributeValidationReport;

public class FutureDateValidatorTest {
    @Test
    public void testValidValue() {
        final Instant futureInstant = Instant.now()
                .plus(5, ChronoUnit.MINUTES);
        validate(futureInstant, 0);
    }

    @Test
    public void testInvalidValue() {
        final Instant pastInstant = Instant.now()
                .minus(5, ChronoUnit.MINUTES);
        validate(pastInstant, 1);
    }

    private void validate(final Instant instant, final int expectedErrors) {
        final FutureDateValidator validator = FutureDateValidator.getInstance();
        final AttributeValidationReport report = validator.validate(new AttributeImpl("test",
                Date.from(instant)));
        assertThat(report.getAttributeValidationViolations()
                .size(), is(expectedErrors));
    }
}
