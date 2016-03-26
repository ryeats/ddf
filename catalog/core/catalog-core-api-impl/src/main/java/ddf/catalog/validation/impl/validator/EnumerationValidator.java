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
import ddf.catalog.validation.violation.ValidationViolation.Severity;

public class EnumerationValidator implements AttributeValidator {
    private final Set<String> values;

    public EnumerationValidator(final Set<String> values) {
        this.values = (values == null ? Collections.emptySet() : values);
    }

    @Override
    public AttributeValidationReport validate(Attribute attribute) {
        final AttributeValidationReport report = new AttributeValidationReportImpl();

        if (attribute != null) {
            final String name = attribute.getName();
            for (final Serializable value : attribute.getValues()) {
                final String stringValue = String.valueOf(value);
                if (!values.contains(stringValue)) {
                    report.getAttributeValidationViolations()
                            .add(new ValidationViolationImpl(Collections.singleton(name),
                                    name + " has an invalid value: [" + stringValue + "]",
                                    Severity.ERROR));
                    break;
                }
            }
        }

        report.getSuggestedValues()
                .addAll(values);

        return report;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EnumerationValidator that = (EnumerationValidator) o;

        return new EqualsBuilder().append(values, that.values)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(23, 37).append(values)
                .toHashCode();
    }
}
