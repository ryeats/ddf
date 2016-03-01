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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import com.google.common.collect.Sets;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.validation.impl.validator.EnumerationValidator;
import ddf.catalog.validation.report.AttributeValidationReport;

public class EnumerationValidatorTest {
    private static final String[] ENUMERATED_VALUES = {"hearts", "spades", "diamonds", "clubs"};

    @Test
    public void testValidValue() {
        validate(new AttributeImpl("test", "spades"), 0);
    }

    @Test
    public void testInvalidValue() {
        validate(new AttributeImpl("test", "other"), 1);
    }

    @Test
    public void testSuggestedValues() {
        final AttributeValidationReport report = getReport(new AttributeImpl("test", "something"));
        assertThat(report.getSuggestedValues(), containsInAnyOrder(ENUMERATED_VALUES));
    }

    private AttributeValidationReport getReport(final Attribute attribute) {
        final Set<String> enumeratedValuesSet = Arrays.stream(ENUMERATED_VALUES)
                .collect(Collectors.toSet());
        final EnumerationValidator validator = new EnumerationValidator(enumeratedValuesSet);
        return validator.validate(attribute);
    }

    private void validate(final Attribute attribute, final int expectedErrors) {
        final AttributeValidationReport report = getReport(attribute);
        assertThat(report.getAttributeValidationViolations()
                .size(), is(expectedErrors));
    }

    @Test
    public void testEquals() {
        final EnumerationValidator validator1 = new EnumerationValidator(Sets.newHashSet("first",
                "second"));
        final EnumerationValidator validator2 = new EnumerationValidator(Sets.newHashSet("first",
                "second"));
        assertThat(validator1.equals(validator1), is(true));
        assertThat(validator1.equals(validator2), is(true));
        assertThat(validator2.equals(validator1), is(true));
    }

    @Test
    public void testEqualsDifferentEnumerationValues() {
        final EnumerationValidator validator1 = new EnumerationValidator(Sets.newHashSet("first",
                "second"));
        final EnumerationValidator validator2 = new EnumerationValidator(Sets.newHashSet("first",
                "third"));
        assertThat(validator1.equals(validator2), is(false));
        assertThat(validator2.equals(validator1), is(false));
    }

    @Test
    public void testHashCode() {
        final EnumerationValidator validator1 = new EnumerationValidator(Sets.newHashSet("first",
                "second"));
        final EnumerationValidator validator2 = new EnumerationValidator(Sets.newHashSet("first",
                "second"));
        assertThat(validator1.hashCode(), is(validator2.hashCode()));
    }

    @Test
    public void testHashCodeDifferentEnumerationValues() {
        final EnumerationValidator validator1 = new EnumerationValidator(Sets.newHashSet("first",
                "second"));
        final EnumerationValidator validator2 = new EnumerationValidator(Sets.newHashSet("first",
                "third"));
        assertThat(validator1.hashCode(), not(validator2.hashCode()));
    }
}
