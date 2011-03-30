package com.plugtree.solradvert;

/**
 *      Copyright 2011 Plugtree LLC
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

import java.io.File;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

/**
 * Using the request hanlders:
 * 
 * <requestHandler name="requestHandlerWithAdvert" class="solr.StandardRequestHandler">
 *		<lst name="defaults">
 *			<str name="defType">dismax</str>
 *			<str name="qf">id product brand description</str>
 *		</lst>
 *		<arr name="first-components">
 *			<str>adverts</str>
 *		</arr>
 *	</requestHandler>
 *
 *	<requestHandler name="requestHandlerWithoutAdvert" class="solr.StandardRequestHandler">
 *		<lst name="defaults">
 *			<str name="defType">dismax</str>
 *			<str name="qf">id product brand description</str>
 *		</lst>
 *	</requestHandler>
 *
 * @author salaboy
 */
public class SimpleSolrAdvertTest extends AbstractAdvertTestCase {
  
  @Override
  public String getSchemaFile() {
    return "solr/conf/schema.xml";
  }
  
  @Override
  public String getSolrConfigFile() {
    return "solr/conf/solrconfig.xml";
  }

  @Test
	public void testBoosting() throws Exception {
		assertAddDoc("1", "shoes", "nike", "running shoes", new Date(), 0.0);
		assertAddDoc("2", "shoes", "adidas", "football shoes", new Date(), 0.0);
		assertCommit();
		
		// we are using the requestHandlerWithAdvert, but the request parameter
		// "advert" isn't "true", so the documents should be returned in their
		// index order
		assertQuery(
		    newRequest(
		        "q","shoes", 
		        "qt", "requestHandlerWithAdvert"),
				"//*[@numFound='2']",
				"//result/doc[1]/int[@name='id'][.='1']",
				"//result/doc[2]/int[@name='id'][.='2']"
		);
		
		// now we add "&advert=true" to the request, so the Adidas shoes should
		// be boosted (see advert.drl)
		assertQuery(
		    newRequest(
		        "q","shoes", 
		        "qt", "requestHandlerWithAdvert", 
		        AdvertParams.ADVERT_COMPONENT_NAME, "true"),
				"//*[@numFound='2']",
				"//result/doc[1]/int[@name='id'][.='2']",
				"//result/doc[2]/int[@name='id'][.='1']"
		);

	}
	
  @Test
	public void testSorting() throws Exception {
    assertAddDoc("1", "tennis racquet", "babolat", "", new Date(), 150.0);
    assertAddDoc("2", "tennis racquet", "prince", "", new Date(), 100.0);
    assertAddDoc("3", "tennis racquet", "head", "", new Date(), 300.0);
    assertCommit();
    
    // we are using the requestHandlerWithAdvert, but the request parameter
    // "advert" isn't "true", so the documents should be returned in their
    // index order
    assertQuery(
        newRequest(
            "q","\"tennis racquet\"", 
            "qt", "requestHandlerWithAdvert"),
        "//*[@numFound='3']",
        "//result/doc[1]/int[@name='id'][.='1']",
        "//result/doc[2]/int[@name='id'][.='2']",
        "//result/doc[3]/int[@name='id'][.='3']"
    );
    
    // now we add "&advert=true" to the request, so the documents should be
    // returned sorted by price (see advert.drl)
    assertQuery(
        newRequest(
            "q","\"tennis racquet\"", 
            "qt", "requestHandlerWithAdvert",
            AdvertParams.ADVERT_COMPONENT_NAME, "true"),
        "//*[@numFound='3']",
        "//result/doc[1]/int[@name='id'][.='2']",
        "//result/doc[2]/int[@name='id'][.='1']",
        "//result/doc[3]/int[@name='id'][.='3']"
    );

  }
	
  @Test
	public void testTwoDifferentSessions() throws Exception {
	  assertAddDoc("1", "tennis racquet", "babolat", "", new Date(), 150.0);
    assertAddDoc("2", "tennis racquet", "prince", "", new Date(), 100.0);
    assertAddDoc("3", "tennis racquet", "head", "", new Date(), 300.0);
    assertCommit();
    
    // ksession1 will sort results by index order
    assertQuery(
        newRequest(
            "q","\"tennis racquet\"", 
            "qt", "requestHandlerWithAdvert"),
        "//*[@numFound='3']",
        "//result/doc[1]/int[@name='id'][.='1']",
        "//result/doc[2]/int[@name='id'][.='2']",
        "//result/doc[3]/int[@name='id'][.='3']"
    );
    
    // ksession1 will sort results by price in descending order
    assertQuery(
        newRequest(
            "q","\"tennis racquet\"", 
            "qt", "requestHandlerWithAdvert",
            AdvertParams.ADVERT_COMPONENT_NAME, "true",
            AdvertParams.ADVERT_RULES, "ksession1"),
        "//*[@numFound='3']",
        "//result/doc[1]/int[@name='id'][.='2']",
        "//result/doc[2]/int[@name='id'][.='1']",
        "//result/doc[3]/int[@name='id'][.='3']"
    );
    
    // ksession1 will sort results by price in ascending order
    assertQuery(
        newRequest(
            "q","\"tennis racquet\"", 
            "qt", "requestHandlerWithAdvert",
            AdvertParams.ADVERT_COMPONENT_NAME, "true", 
            AdvertParams.ADVERT_RULES, "ksession2"),
        "//*[@numFound='3']",
        "//result/doc[1]/int[@name='id'][.='3']",
        "//result/doc[2]/int[@name='id'][.='1']",
        "//result/doc[3]/int[@name='id'][.='2']"
    );
	}
	
  @Test
	public void testUndefinedSession() throws Exception {
	  assertQuery(
	      newRequest(
	          "q","foo", 
	          "qt", "requestHandlerWithAdvert",
	          AdvertParams.ADVERT_COMPONENT_NAME, "true",
	          AdvertParams.ADVERT_RULES, "bar123"),
        "//*[@numFound='0']"
    );
	}
	
  @Test
	public void testRulesChangeBetweenRequests() throws Exception {
	  assertAddDoc("1", "tennis racquet", "babolat", "", new Date(), 150.0);
    assertAddDoc("2", "tennis racquet", "prince", "", new Date(), 100.0);
    assertAddDoc("3", "tennis racquet", "head", "", new Date(), 300.0);
    assertCommit();
    
    File rulesFile = new File(getClass().getResource("/solr/conf/advert.tmp.drl").toURI());
    
    // advert1.drl will sort results by price in descending order
    FileUtils.copyURLToFile(getClass().getResource("/solr/conf/advert1.drl"), rulesFile);
    assertQuery(
        newRequest(
            "q", "\"tennis racquet\"", 
            "qt", "requestHandlerWithAdvert",
            AdvertParams.ADVERT_COMPONENT_NAME, "true", 
            AdvertParams.ADVERT_RULES, "ksession3",
            AdvertParams.ADVERT_RELOAD_RULES, "true"),
        "//*[@numFound='3']",
        "//result/doc[1]/int[@name='id'][.='2']",
        "//result/doc[2]/int[@name='id'][.='1']",
        "//result/doc[3]/int[@name='id'][.='3']"
    );
    
    // advert2.drl will sort results by price in ascending order
    FileUtils.copyURLToFile(getClass().getResource("/solr/conf/advert2.drl"), rulesFile);
    assertQuery(
        newRequest(
            "q", "\"tennis racquet\"", 
            "qt", "requestHandlerWithAdvert",
            AdvertParams.ADVERT_COMPONENT_NAME, "true",
            AdvertParams.ADVERT_RULES, "ksession3",
            AdvertParams.ADVERT_RELOAD_RULES, "true"),
        "//*[@numFound='3']",
        "//result/doc[1]/int[@name='id'][.='3']",
        "//result/doc[2]/int[@name='id'][.='1']",
        "//result/doc[3]/int[@name='id'][.='2']"
    );
	}

  @Test
	public void testQueryGeneralQuery() throws Exception {
		assertQuery(
		    newRequest("q", "some content"));
	}
	
}
