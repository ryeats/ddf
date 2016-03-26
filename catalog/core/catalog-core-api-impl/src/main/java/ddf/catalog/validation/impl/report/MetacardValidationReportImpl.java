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
package ddf.catalog.validation.impl.report;

import java.util.HashSet;
import java.util.Set;

import ddf.catalog.validation.report.MetacardValidationReport;
import ddf.catalog.validation.violation.ValidationViolation;

public class MetacardValidationReportImpl implements MetacardValidationReport {
    private final Set<ValidationViolation> attributeValidationViolations;

    private final Set<ValidationViolation> metacardValidationViolations;

    public MetacardValidationReportImpl() {
        attributeValidationViolations = new HashSet<>();
        metacardValidationViolations = new HashSet<>();
    }

    @Override
    public Set<ValidationViolation> getAttributeValidationViolations() {
        return attributeValidationViolations;
    }

    @Override
    public Set<ValidationViolation> getMetacardValidationViolations() {
        return metacardValidationViolations;
    }
}
