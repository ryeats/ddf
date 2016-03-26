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

public class DecimalRangeValidator implements AttributeValidator {
    private final double min;

    private final double max;

    private final double epsilon;

    public DecimalRangeValidator(final double min, final double max) {
        this(min, max, 1e-6);
    }

    public DecimalRangeValidator(final double min, final double max, final double epsilon) {
        this.min = min;
        this.max = max;
        this.epsilon = epsilon;
    }

    @Override
    public AttributeValidationReport validate(Attribute attribute) {
        final AttributeValidationReport report = new AttributeValidationReportImpl();

        if (attribute != null) {
            final String name = attribute.getName();
            for (final Serializable value : attribute.getValues()) {
                final double doubleValue;
                if (value instanceof Short) {
                    doubleValue = ((Short) value).doubleValue();
                } else if (value instanceof Integer) {
                    doubleValue = ((Integer) value).doubleValue();
                } else if (value instanceof Long) {
                    doubleValue = ((Long) value).doubleValue();
                } else if (value instanceof Float) {
                    doubleValue = ((Float) value).doubleValue();
                } else if (value instanceof Double) {
                    doubleValue = (double) value;
                } else {
                    continue;
                }

                if (!checkRange(doubleValue)) {
                    final String violationMessage = String.format("%s must be between %f and %f",
                            name,
                            min,
                            max);
                    final Set<ValidationViolation> violations =
                            report.getAttributeValidationViolations();
                    violations.add(new ValidationViolationImpl(Collections.singleton(name),
                            violationMessage,
                            Severity.ERROR));
                    break;
                }
            }
        }

        return report;
    }

    private boolean checkRange(final double value) {
        return (min - epsilon) <= value && value <= (max + epsilon);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DecimalRangeValidator that = (DecimalRangeValidator) o;

        return new EqualsBuilder().append(min, that.min)
                .append(max, that.max)
                .append(epsilon, that.epsilon)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 41).append(min)
                .append(max)
                .append(epsilon)
                .toHashCode();
    }
}
