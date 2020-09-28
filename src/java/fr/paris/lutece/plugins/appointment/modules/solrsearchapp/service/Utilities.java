/*
 * Copyright (c) 2002-2020, City of Paris
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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import fr.paris.lutece.portal.service.util.AppPropertiesService;

public final class Utilities
{
    public static final String PROPERTY_DATE_FORMAT_INPUT_DATE = "appointment-solrsearchapp.app.date.format";

    public static final String PARAMETER_DAYS_OF_WEEK = "days_of_week";
    public static final String PARAMETER_FROM_DATE = "from_date";
    public static final String PARAMETER_FROM_TIME = "from_time";
    public static final String PARAMETER_TO_DATE = "to_date";
    public static final String PARAMETER_TO_TIME = "to_time";
    public static final String PARAMETER_FROM_DAY_MINUTE = "from_day_minute";
    public static final String PARAMETER_TO_DAY_MINUTE = "to_day_minute";
    public static final String DEFAULT_DATE_FORMAT_INPUT_DATE = "dd/MM/yyyy";
    // Bad because it will not reload on property reload, but then caching DateTimeFormatter becomes a pain..
    public static final String DATE_FORMAT_INPUT_DATE = AppPropertiesService.getProperty( PROPERTY_DATE_FORMAT_INPUT_DATE, DEFAULT_DATE_FORMAT_INPUT_DATE );
    public static final String DATE_FORMAT_INPUT_TIME = "HH:mm";
    public static final String FORMAT_INPUT_DATE = DATE_FORMAT_INPUT_DATE + " " + DATE_FORMAT_INPUT_TIME;
    public static final DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern( FORMAT_INPUT_DATE );
    public static final String FORMAT_OUTPUT_DATE = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern( FORMAT_OUTPUT_DATE );

    public static final String PARAMETER_SITE = "site";
    public static final String PARAMETER_CATEGORY = "category";
    public static final String PARAMETER_FORM = "form";

    public static final String MARK_ITEM_SITES = "items_sites";
    public static final String MARK_ITEM_CATEGORIES = "items_categories";
    public static final String MARK_ITEM_FORMS = "items_forms";

    public static final String [ ] LIST_DAYS_CODE = {
            "1", "2", "3", "4", "5", "6", "7"
    };
    public static final List<SimpleImmutableEntry<String, String>> LIST_DAYS = Collections.unmodifiableList( Arrays.asList(
            new SimpleImmutableEntry<>( "1", "module.appointment.solrsearchapp.days.1" ),
            new SimpleImmutableEntry<>( "2", "module.appointment.solrsearchapp.days.2" ),
            new SimpleImmutableEntry<>( "3", "module.appointment.solrsearchapp.days.3" ),
            new SimpleImmutableEntry<>( "4", "module.appointment.solrsearchapp.days.4" ),
            new SimpleImmutableEntry<>( "5", "module.appointment.solrsearchapp.days.5" ),
            new SimpleImmutableEntry<>( "6", "module.appointment.solrsearchapp.days.6" ),
            new SimpleImmutableEntry<>( "7", "module.appointment.solrsearchapp.days.7" ) ) );

    private Utilities( )
    {
        // private constructor
    }
    
    public static String getSearchParameterValue( String parameter, HttpServletRequest request, Map<String, String> savedSearchParameters )
    {
        String requestValue = getSearchParameter( parameter, request, savedSearchParameters );
        if ( requestValue != null )
        {
            String [ ] requestSplit = requestValue.split( "\\|" );
            return requestSplit [0];
        }
        else
        {
            return null;
        }
    }

    public static String getSearchParameter( String parameter, HttpServletRequest request, Map<String, String> savedSearchParameters )
    {
        String result = request.getParameter( parameter );
        if ( result != null )
        {
            return result;
        }
        result = savedSearchParameters.get( parameter );
        return result;
    }

    public static String [ ] getSearchMultiParameter( String parameter, HttpServletRequest request, Map<String, String [ ]> savedMultiSearchParameters )
    {
        String [ ] result = request.getParameterValues( parameter );
        if ( result != null )
        {
            return result;
        }
        result = savedMultiSearchParameters.get( parameter );
        return result;
    }

    public static String getSearchParameterLabel( String parameter, HttpServletRequest request, Map<String, String> savedSearchParameters )
    {
        String requestValue = getSearchParameter( parameter, request, savedSearchParameters );
        if ( requestValue != null )
        {
            String [ ] requestSplit = requestValue.split( "\\|" );
            return requestSplit [requestSplit.length - 1];
        }
        else
        {
            return null;
        }
    }

}
