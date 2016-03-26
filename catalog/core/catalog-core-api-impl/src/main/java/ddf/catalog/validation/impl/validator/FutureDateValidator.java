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
import java.time.Instant;
import java.util.Collections;
import java.util.Date;

import ddf.catalog.data.Attribute;
import ddf.catalog.validation.AttributeValidator;
import ddf.catalog.validation.impl.report.AttributeValidationReportImpl;
import ddf.catalog.validation.impl.violation.ValidationViolationImpl;
import ddf.catalog.validation.report.AttributeValidationReport;
import ddf.catalog.validation.violation.ValidationViolation.Severity;

public class FutureDateValidator implements AttributeValidator {
    private static final FutureDateValidator INSTANCE = new FutureDateValidator();

    private FutureDateValidator() {
    }

    public static FutureDateValidator getInstance() {
        return INSTANCE;
    }

    @Override
    public AttributeValidationReport validate(Attribute attribute) {
        final AttributeValidationReport report = new AttributeValidationReportImpl();

        if (attribute != null) {
            final String name = attribute.getName();
            final Date now = Date.from(Instant.now());
            for (final Serializable value : attribute.getValues()) {
                if (value instanceof Date && ((Date) value).before(now)) {
                    report.getAttributeValidationViolations()
                            .add(new ValidationViolationImpl(Collections.singleton(name),
                                    name + " must be in the future",
                                    Severity.ERROR));
                    break;
                }
            }
        }

        return report;
    }
}
