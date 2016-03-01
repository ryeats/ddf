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
package ddf.catalog.validation.impl.validator;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import ddf.catalog.data.Attribute;
import ddf.catalog.validation.AttributeValidator;
import ddf.catalog.validation.impl.report.AttributeValidationReportImpl;
import ddf.catalog.validation.impl.violation.ValidationViolationImpl;
import ddf.catalog.validation.report.AttributeValidationReport;
import ddf.catalog.validation.violation.ValidationViolation;
import ddf.catalog.validation.violation.ValidationViolation.Severity;

public class IntegerRangeValidator implements AttributeValidator {
    private final long min;

    private final long max;

    public IntegerRangeValidator(final long min, final long max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public AttributeValidationReport validate(Attribute attribute) {
        final AttributeValidationReport report = new AttributeValidationReportImpl();

        if (attribute != null) {
            final String name = attribute.getName();
            for (final Serializable value : attribute.getValues()) {
                final boolean valid;
                if (value instanceof Short) {
                    valid = checkRange((short) value);
                } else if (value instanceof Integer) {
                    valid = checkRange((int) value);
                } else if (value instanceof Long) {
                    valid = checkRange((long) value);
                } else if (value instanceof Float) {
                    valid = checkRange((float) value);
                } else if (value instanceof Double) {
                    valid = checkRange((double) value);
                } else {
                    continue;
                }

                if (!valid) {
                    final Set<ValidationViolation> violations =
                            report.getAttributeValidationViolations();
                    final String violationMessage = String.format("%s must be between %d and %d",
                            name,
                            min,
                            max);
                    violations.add(new ValidationViolationImpl(Collections.singleton(name),
                            violationMessage,
                            Severity.ERROR));
                    break;
                }
            }
        }

        return report;
    }

    private boolean checkRange(final long value) {
        return min <= value && value <= max;
    }

    private boolean checkRange(final double value) {
        return min <= value && value <= max;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IntegerRangeValidator validator = (IntegerRangeValidator) o;

        return new EqualsBuilder().append(min, validator.min)
                .append(max, validator.max)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(23, 41).append(min)
                .append(max)
                .toHashCode();
    }
}
