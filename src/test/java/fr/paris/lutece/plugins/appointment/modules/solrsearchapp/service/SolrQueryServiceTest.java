/*
 * Copyright (c) 2002-2023, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.appointment.modules.solrsearchapp.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.junit.jupiter.api.Test;

import fr.paris.lutece.test.LuteceTestCase;
import fr.paris.lutece.test.mocks.MockHttpServletRequest;

/**
 * Covers the filters the public search sends to Solr from the request.
 */
public class SolrQueryServiceTest extends LuteceTestCase
{
    /**
     * Build the query of a request.
     *
     * @param request
     *            the request
     * @return the filter queries
     */
    private static List<String> filters( MockHttpServletRequest request )
    {
        SolrQuery query = SolrQueryService.getCommonFilteredQuery( request, new HashMap<>( ), new HashMap<>( ) );
        return Arrays.asList( query.getFilterQueries( ) );
    }

    /**
     * Only the day codes 1 to 7 reach the day filter.
     */
    @Test
    public void testDaysOfWeekKeepOnlyDayCodes( )
    {
        MockHttpServletRequest request = new MockHttpServletRequest( );
        request.addParameter( "days_of_week", "1" );
        request.addParameter( "days_of_week", "3) OR (*:*" );
        request.addParameter( "days_of_week", "5" );
        assertTrue( filters( request ).contains( "{!tag=tagday_of_week_long}day_of_week_long:(1 OR 5)" ) );
    }

    /**
     * Non numeric minutes and a non positive number of slots fall back to open bounds and one slot.
     */
    @Test
    public void testMalformedBoundsFallBack( )
    {
        MockHttpServletRequest request = new MockHttpServletRequest( );
        request.addParameter( "from_day_minute", "0 TO *] OR x:[*" );
        request.addParameter( "to_day_minute", "1260" );
        request.addParameter( "nb_consecutive_slots", "-4" );
        List<String> listFilters = filters( request );
        assertTrue( listFilters.stream( ).anyMatch( fq -> fq.endsWith( ":[* TO 1260]" ) ) );
        assertTrue( listFilters.contains( "nb_consecutives_slots_long:[1 TO *]" ) );
    }

    /**
     * A role carrying Solr syntax is escaped.
     */
    @Test
    public void testRoleEscaped( )
    {
        MockHttpServletRequest request = new MockHttpServletRequest( );
        request.addParameter( "role", "a OR b" );
        assertTrue( filters( request ).stream( ).anyMatch( fq -> fq.endsWith( ":a\\ OR\\ b" ) ) );
    }
}
