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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.validation.MetacardValidator;
import ddf.catalog.validation.ReportingMetacardValidator;
import ddf.catalog.validation.ValidationException;
import ddf.catalog.validation.impl.ValidationExceptionImpl;
import ddf.catalog.validation.impl.report.MetacardValidationReportImpl;
import ddf.catalog.validation.impl.violation.ValidationViolationImpl;
import ddf.catalog.validation.report.MetacardValidationReport;
import ddf.catalog.validation.violation.ValidationViolation;
import ddf.catalog.validation.violation.ValidationViolation.Severity;

public class RequiredAttributesMetacardValidator
        implements MetacardValidator, ReportingMetacardValidator {
    private final String metacardTypeName;

    private final Set<String> requiredAttributes;

    public RequiredAttributesMetacardValidator(final String metacardTypeName,
            final Set<String> requiredAttributes) {
        this.metacardTypeName = metacardTypeName;
        this.requiredAttributes = requiredAttributes;
    }

    @Override
    public void validate(final Metacard metacard) throws ValidationException {
        final MetacardValidationReport report = validateMetacard(metacard);

        final List<String> errors = report.getMetacardValidationViolations()
                .stream()
                .map(ValidationViolation::getMessage)
                .collect(Collectors.toList());

        if (!errors.isEmpty()) {
            final ValidationExceptionImpl exception = new ValidationExceptionImpl();
            exception.setErrors(errors);
            throw exception;
        }
    }

    @Override
    public MetacardValidationReport validateMetacard(final Metacard metacard) {
        final MetacardValidationReport report = new MetacardValidationReportImpl();

        final MetacardType metacardType = metacard.getMetacardType();

        if (metacardTypeName.equals(metacardType.getName())) {
            for (final String attributeName : requiredAttributes) {
                final Attribute attribute = metacard.getAttribute(attributeName);
                if (attribute != null) {
                    final AttributeDescriptor descriptor = metacardType.getAttributeDescriptor(
                            attributeName);
                    if (descriptor.isMultiValued()) {
                        if (attribute.getValues()
                                .size() == 0) {
                            reportMissingRequiredAttribute(attributeName, report);
                        }
                    } else if (attribute.getValue() == null) {
                        reportMissingRequiredAttribute(attributeName, report);
                    }
                } else {
                    reportMissingRequiredAttribute(attributeName, report);
                }
            }
        }

        return report;
    }

    private void reportMissingRequiredAttribute(final String attributeName,
            final MetacardValidationReport report) {
        report.getMetacardValidationViolations()
                .add(new ValidationViolationImpl(Collections.singleton(attributeName),
                        attributeName + " is required",
                        Severity.ERROR));
    }
}
