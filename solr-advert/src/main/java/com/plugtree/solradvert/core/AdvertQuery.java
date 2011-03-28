package com.plugtree.solradvert.core;

/**
 *  Copyright 2011 Plugtree LLC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.search.FunctionQParserPlugin;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SortSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The query that will be inserted in the KnowledgeSession. It's a
 * wrapper of {@link Query} that introduces a set of useful methods
 * for rules writing.
 * 
 * @author jgrande
 *
 */
public class AdvertQuery {
	
	private static Logger logger = LoggerFactory.getLogger(AdvertQuery.class);
	
	private ResponseBuilder rb;
	
	private Query q;
	
	public AdvertQuery(ResponseBuilder rb) {
		this.rb = rb;
		this.q = rb.getQuery();
	}
	
	/**
	 * @return <code>true</code> if this query contains a TermQuery
	 * for Term(field, text)
	 */
	public boolean hasTerm(String field, String text) {
		Term term = new Term(field, text);
		HasTermQueryVisitor queryVisitor = new HasTermQueryVisitor(term);
		return queryVisitor.visit(this.q);
	}
	
	/**
	 * Add the score returned by the query <code>qstr</code>
	 * to the original score of each document. By default,
	 * <code>qstr</code> is parsed with {@link FunctionQParserPlugin}, but
	 * this can be overridden using local params, eg:
	 * <code>{!lucene}field:text</code>.
	 * 
	 * @param qstr the query string to use 
	 */
	public void boost(String qstr) {
		logger.debug("Boosting with function query '" + qstr + "'");
		try {
			QParser qparser = QParser.getParser(qstr, FunctionQParserPlugin.NAME, rb.req);
			Query qq = qparser.parse();
			
			BooleanQuery newq = new BooleanQuery();
			newq.add(new BooleanClause(q, Occur.MUST));
			newq.add(new BooleanClause(qq, Occur.SHOULD));
			
			rb.setQuery(newq);
		} catch(ParseException ex) {
			
		}
	}
	
	/**
	 * Replace the sort specification given in the <code>sort</code> request
	 * parameter by the specification given in the <code>sortSpec</code> parameter
	 * of this function.
	 * 
	 * @param sortSpec the sort specification to use
	 */
	public void setSort(String sortSpec) {
	  Sort newSort = QueryParsing.parseSort(sortSpec, rb.req.getSchema());
	  int offset = rb.getSortSpec().getOffset();
	  int count = rb.getSortSpec().getCount();
	  rb.setSortSpec(new SortSpec(newSort, offset, count));
	}

}
