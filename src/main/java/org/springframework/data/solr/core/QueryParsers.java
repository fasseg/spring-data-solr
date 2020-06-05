/*
 * Copyright 2012 - 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.core;

import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.solr.core.mapping.SolrPersistentEntity;
import org.springframework.data.solr.core.mapping.SolrPersistentProperty;
import org.springframework.data.solr.core.query.FacetQuery;
import org.springframework.data.solr.core.query.HighlightQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SolrDataQuery;
import org.springframework.data.solr.core.query.TermsQuery;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Christoph Strobl
 */
public class QueryParsers {

    private final Map<Class<?>, QueryParser> queryParsers = new LinkedHashMap<>();
    private final @Nullable
    MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> mappingContext;
    private QueryParser defaultQueryParser;
    private TermsQueryParser termsQueryParser;

    /**
     * @param mappingContext can be {@literal null}.
     * @since 4.0
     */
    public QueryParsers(@Nullable MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> mappingContext) {
        this.mappingContext = mappingContext;
        this.defaultQueryParser = new DefaultQueryParser(mappingContext);
        this.termsQueryParser = new TermsQueryParser(mappingContext);

        this.queryParsers.put(TermsQuery.class, this.termsQueryParser);
        this.queryParsers.put(FacetQuery.class, this.defaultQueryParser);
        this.queryParsers.put(HighlightQuery.class, this.defaultQueryParser);
        this.queryParsers.put(Query.class, this.defaultQueryParser);
    }

    /**
     * Get the {@link QueryParser} for given query type
     *
     * @param clazz
     * @return {@link DefaultQueryParser} if no matching parser found
     */
    public QueryParser getForClass(final Class<? extends SolrDataQuery> clazz) {
        for (final Map.Entry<Class<?>, QueryParser> entry : this.queryParsers.entrySet()) {
            if (entry.getKey().isAssignableFrom(clazz)) {
                return entry.getValue();
            }
        }
        return defaultQueryParser;
    }

    /**
     * Register additional {@link QueryParser} for {@link SolrQuery}
     *
     * @param clazz
     * @param parser
     */
    public void registerParser(final Class<? extends SolrDataQuery> clazz, final QueryParser parser) {
        Assert.notNull(parser, "Cannot register 'null' parser.");
        queryParsers.put(clazz, parser);
    }

    /**
     * Update the {@link MappingContext} for all default query parsers. But leave the previously registered custom {@link QueryParser} untouched.
     * Custom mapping context sensitive query parsers have to be re-registered, when the {@link MappingContext} changes.
     *
     * @param mappingContext
     */
    public void setMappingContext(final MappingContext<? extends SolrPersistentEntity<?>, SolrPersistentProperty> mappingContext) {
        final Map<Class<?>, QueryParser> updatedParsers = new HashMap<>();
        final DefaultQueryParser updatedDefaultQueryParser = new DefaultQueryParser(mappingContext);
        final TermsQueryParser updatedTermsQueryParser = new TermsQueryParser(mappingContext);

        for (final Map.Entry<Class<?>, QueryParser> entry : this.queryParsers.entrySet()) {
            if (entry.getValue() == this.defaultQueryParser) {
                updatedParsers.put(entry.getKey(), updatedDefaultQueryParser);
            } else if (entry.getValue() == this.termsQueryParser) {
                updatedParsers.put(entry.getKey(), updatedTermsQueryParser);
            }
        }
        this.queryParsers.putAll(updatedParsers);
        this.defaultQueryParser = updatedDefaultQueryParser;
        this.termsQueryParser = updatedTermsQueryParser;
    }
}
