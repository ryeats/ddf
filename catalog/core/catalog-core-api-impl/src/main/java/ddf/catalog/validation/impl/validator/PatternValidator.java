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
import java.util.regex.Pattern;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import ddf.catalog.data.Attribute;
import ddf.catalog.validation.AttributeValidator;
import ddf.catalog.validation.impl.report.AttributeValidationReportImpl;
import ddf.catalog.validation.impl.violation.ValidationViolationImpl;
import ddf.catalog.validation.report.AttributeValidationReport;
import ddf.catalog.validation.violation.ValidationViolation.Severity;

public class PatternValidator implements AttributeValidator {
    private final Pattern pattern;

    public PatternValidator(final String regex) {
        pattern = Pattern.compile(regex);
    }

    @Override
    public AttributeValidationReport validate(Attribute attribute) {
        final AttributeValidationReport report = new AttributeValidationReportImpl();

        if (attribute != null) {
            final String name = attribute.getName();
            for (final Serializable value : attribute.getValues()) {
                if (value instanceof String && !(pattern.matcher((String) value)).matches()) {
                    report.getAttributeValidationViolations()
                            .add(new ValidationViolationImpl(Collections.singleton(name),
                                    name + " does not follow the pattern " + pattern.pattern(),
                                    Severity.ERROR));
                    break;
                }
            }
        }

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

        PatternValidator validator = (PatternValidator) o;

        return new EqualsBuilder().append(pattern.pattern(), validator.pattern.pattern())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 29).append(pattern.pattern())
                .toHashCode();
    }
}
