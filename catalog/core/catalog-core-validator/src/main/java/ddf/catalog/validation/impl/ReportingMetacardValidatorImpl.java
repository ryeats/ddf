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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.validation.AttributeValidator;
import ddf.catalog.validation.AttributeValidatorRegistry;
import ddf.catalog.validation.MetacardValidator;
import ddf.catalog.validation.ReportingMetacardValidator;
import ddf.catalog.validation.ValidationException;
import ddf.catalog.validation.impl.report.MetacardValidationReportImpl;
import ddf.catalog.validation.report.MetacardValidationReport;
import ddf.catalog.validation.violation.ValidationViolation;
import ddf.catalog.validation.violation.ValidationViolation.Severity;

/**
 * Default {@link Metacard} validator that validates all of a {@link Metacard}'s attributes using
 * the {@link AttributeValidator}s registered in the attribute validator registry.
 */
public class ReportingMetacardValidatorImpl
        implements MetacardValidator, ReportingMetacardValidator {
    private final AttributeValidatorRegistry validatorRegistry;

    public ReportingMetacardValidatorImpl(final AttributeValidatorRegistry validatorRegistry) {
        this.validatorRegistry = validatorRegistry;
    }

    private void getMessages(final Set<ValidationViolation> violations, final List<String> warnings,
            final List<String> errors) {
        for (final ValidationViolation violation : violations) {
            if (violation.getSeverity() == Severity.WARNING) {
                warnings.add(violation.getMessage());
            } else {
                errors.add(violation.getMessage());
            }
        }
    }

    @Override
    public void validate(Metacard metacard) throws ValidationException {
        final MetacardValidationReport report = validateMetacard(metacard);

        if (!report.getAttributeValidationViolations()
                .isEmpty() || !report.getMetacardValidationViolations()
                .isEmpty()) {
            final ValidationExceptionImpl exception = new ValidationExceptionImpl();

            final List<String> warnings = new ArrayList<>();
            final List<String> errors = new ArrayList<>();

            getMessages(report.getAttributeValidationViolations(), warnings, errors);
            getMessages(report.getMetacardValidationViolations(), warnings, errors);

            exception.setWarnings(warnings);
            exception.setErrors(errors);

            throw exception;
        }
    }

    @Override
    public MetacardValidationReport validateMetacard(Metacard metacard) {
        final MetacardValidationReport report = new MetacardValidationReportImpl();

        final Set<ValidationViolation> attributeViolations =
                report.getAttributeValidationViolations();
        for (final AttributeDescriptor descriptor : metacard.getMetacardType()
                .getAttributeDescriptors()) {
            final String attributeName = descriptor.getName();
            for (final AttributeValidator validator : validatorRegistry.getValidators(attributeName)) {
                attributeViolations.addAll(validator.validate(metacard.getAttribute(attributeName))
                        .getAttributeValidationViolations());
            }
        }

        return report;
    }
}
