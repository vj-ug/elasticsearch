/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.elasticsearch.Version;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.indices.analysis.IndicesAnalysisService;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.IndexSettingsModule;
import org.elasticsearch.test.VersionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.instanceOf;

public class AnalysisServiceTests extends ESTestCase {

    private static AnalyzerProviderFactory analyzerProvider(final String name) {
        return (name1, settings) -> new PreBuiltAnalyzerProvider(name1, AnalyzerScope.INDEX, new EnglishAnalyzer());
    }

    public void testDefaultAnalyzers() {
        Version version = VersionUtils.randomVersion(getRandom());
        Settings settings = Settings.builder().put(IndexMetaData.SETTING_VERSION_CREATED, version).build();
        IndicesAnalysisService indicesAnalysisService = new IndicesAnalysisService(settings);
        AnalysisService analysisService = new AnalysisService(IndexSettingsModule.newIndexSettings(new Index("index"), settings, Collections.EMPTY_LIST), indicesAnalysisService,
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        assertThat(analysisService.defaultIndexAnalyzer().analyzer(), instanceOf(StandardAnalyzer.class));
        assertThat(analysisService.defaultSearchAnalyzer().analyzer(), instanceOf(StandardAnalyzer.class));
        assertThat(analysisService.defaultSearchQuoteAnalyzer().analyzer(), instanceOf(StandardAnalyzer.class));
    }

    public void testOverrideDefaultAnalyzer() {
        Version version = VersionUtils.randomVersion(getRandom());
        Settings settings = Settings.builder().put(IndexMetaData.SETTING_VERSION_CREATED, version).build();
        IndicesAnalysisService indicesAnalysisService = new IndicesAnalysisService(settings);
        AnalysisService analysisService = new AnalysisService(IndexSettingsModule.newIndexSettings(new Index("index"), settings, Collections.EMPTY_LIST), indicesAnalysisService,
                Collections.singletonMap("default", analyzerProvider("default")),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        assertThat(analysisService.defaultIndexAnalyzer().analyzer(), instanceOf(EnglishAnalyzer.class));
        assertThat(analysisService.defaultSearchAnalyzer().analyzer(), instanceOf(EnglishAnalyzer.class));
        assertThat(analysisService.defaultSearchQuoteAnalyzer().analyzer(), instanceOf(EnglishAnalyzer.class));
    }

    public void testOverrideDefaultIndexAnalyzer() {
        Version version = VersionUtils.randomVersionBetween(getRandom(), Version.V_3_0_0, Version.CURRENT);
        Settings settings = Settings.builder().put(IndexMetaData.SETTING_VERSION_CREATED, version).build();
        IndicesAnalysisService indicesAnalysisService = new IndicesAnalysisService(settings);
        try {
            AnalysisService analysisService = new AnalysisService(IndexSettingsModule.newIndexSettings(new Index("index"), settings, Collections.EMPTY_LIST), indicesAnalysisService,
                    Collections.singletonMap("default_index", new PreBuiltAnalyzerProviderFactory("default_index", AnalyzerScope.INDEX, new EnglishAnalyzer())),
                    Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
            fail("Expected ISE");
        } catch (IllegalArgumentException e) {
            // expected
            assertTrue(e.getMessage().contains("[index.analysis.analyzer.default_index] is not supported"));
        }
    }

    public void testBackCompatOverrideDefaultIndexAnalyzer() {
        Version version = VersionUtils.randomVersionBetween(getRandom(), VersionUtils.getFirstVersion(), VersionUtils.getPreviousVersion(Version.V_3_0_0));
        Settings settings = Settings.builder().put(IndexMetaData.SETTING_VERSION_CREATED, version).build();
        IndicesAnalysisService indicesAnalysisService = new IndicesAnalysisService(settings);
        AnalysisService analysisService = new AnalysisService(IndexSettingsModule.newIndexSettings(new Index("index"), settings, Collections.EMPTY_LIST), indicesAnalysisService,
                Collections.singletonMap("default_index", analyzerProvider("default_index")),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        assertThat(analysisService.defaultIndexAnalyzer().analyzer(), instanceOf(EnglishAnalyzer.class));
        assertThat(analysisService.defaultSearchAnalyzer().analyzer(), instanceOf(StandardAnalyzer.class));
        assertThat(analysisService.defaultSearchQuoteAnalyzer().analyzer(), instanceOf(StandardAnalyzer.class));
    }

    public void testOverrideDefaultSearchAnalyzer() {
        Version version = VersionUtils.randomVersion(getRandom());
        Settings settings = Settings.builder().put(IndexMetaData.SETTING_VERSION_CREATED, version).build();
        IndicesAnalysisService indicesAnalysisService = new IndicesAnalysisService(settings);
        AnalysisService analysisService = new AnalysisService(IndexSettingsModule.newIndexSettings(new Index("index"), settings, Collections.EMPTY_LIST), indicesAnalysisService,
                Collections.singletonMap("default_search", analyzerProvider("default_search")),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        assertThat(analysisService.defaultIndexAnalyzer().analyzer(), instanceOf(StandardAnalyzer.class));
        assertThat(analysisService.defaultSearchAnalyzer().analyzer(), instanceOf(EnglishAnalyzer.class));
        assertThat(analysisService.defaultSearchQuoteAnalyzer().analyzer(), instanceOf(EnglishAnalyzer.class));
    }

    public void testBackCompatOverrideDefaultIndexAndSearchAnalyzer() {
        Version version = VersionUtils.randomVersionBetween(getRandom(), VersionUtils.getFirstVersion(), VersionUtils.getPreviousVersion(Version.V_3_0_0));
        Settings settings = Settings.builder().put(IndexMetaData.SETTING_VERSION_CREATED, version).build();
        IndicesAnalysisService indicesAnalysisService = new IndicesAnalysisService(settings);
        Map<String, AnalyzerProviderFactory> analyzers = new HashMap<>();
        analyzers.put("default_index", analyzerProvider("default_index"));
        analyzers.put("default_search", analyzerProvider("default_search"));
        AnalysisService analysisService = new AnalysisService(IndexSettingsModule.newIndexSettings(new Index("index"), settings, Collections.EMPTY_LIST), indicesAnalysisService,
                analyzers, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        assertThat(analysisService.defaultIndexAnalyzer().analyzer(), instanceOf(EnglishAnalyzer.class));
        assertThat(analysisService.defaultSearchAnalyzer().analyzer(), instanceOf(EnglishAnalyzer.class));
        assertThat(analysisService.defaultSearchQuoteAnalyzer().analyzer(), instanceOf(EnglishAnalyzer.class));
    }
}
