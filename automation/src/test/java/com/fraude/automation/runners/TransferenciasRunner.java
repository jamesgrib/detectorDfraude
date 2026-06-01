package com.fraude.automation.runners;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Isolated runner for the transfers feature.
 * Runs only @smoke scenarios from transferencias.feature.
 * Use: mvnw -f automation/pom.xml test -Dtest=TransferenciasRunner
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/transferencias.feature")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty,net.serenitybdd.cucumber.core.plugin.SerenityReporterParallel"
)
@ConfigurationParameter(
    key = GLUE_PROPERTY_NAME,
    value = "com.fraude.automation.stepdefinitions"
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "@smoke"
)
public class TransferenciasRunner {}
