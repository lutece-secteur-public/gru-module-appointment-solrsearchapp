/*
 * Copyright (c) 2002-2018, Mairie de Paris
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
package fr.paris.lutece.plugins.appointment.modules.solrsearchapp.web;

import java.io.IOException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.Group;
import org.apache.solr.client.solrj.response.GroupCommand;
import org.apache.solr.client.solrj.response.GroupResponse;
import org.apache.solr.client.solrj.response.PivotField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.GroupParams;

import fr.paris.lutece.plugins.appointment.modules.solrsearchapp.service.SolrQueryService;
import fr.paris.lutece.plugins.appointment.modules.solrsearchapp.service.Utilities;
import fr.paris.lutece.plugins.search.solr.business.SolrServerService;
import fr.paris.lutece.portal.service.admin.AccessDeniedException;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.message.SiteMessageException;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.portal.util.mvc.xpage.MVCApplication;
import fr.paris.lutece.portal.util.mvc.xpage.annotations.Controller;
import fr.paris.lutece.portal.web.xpages.XPage;
import fr.paris.lutece.util.ReferenceItem;
import fr.paris.lutece.util.ReferenceList;

@Controller( xpageName = "appointmentsearch", pageTitleI18nKey = "module.appointment.solrsearchapp.pageTitle", pagePathI18nKey = "module.appointment.solrsearchapp.pagePathLabel" )
public class AppointmentSearchApp extends MVCApplication
{

    private static final long serialVersionUID = 3579388931034541505L;

    private static final int HUGE_INFINITY = 10000000;

    private static final String ACCESS_DENIED = "module.appointment.solrsearchapp.accessDenied";
    
    private static final String VIEW_SEARCH = "search";
    private static final String ACTION_SEARCH = "search";
    private static final String ACTION_CLEAR = "clear";
    private static final String TEMPLATE_SEARCH = "skin/plugins/appointment/modules/solrsearchapp/search.html";
    private static final String SOLR_FILTERQUERY_NOT_FULL = "NOT slot_nb_free_places_long:0";
    private static final String SOLR_FIELD_NB_FREE_PLACES = "slot_nb_free_places_long";
    private static final String SOLR_FIELD_NB_PLACES = "slot_nb_places_long";
    private static final String SOLR_FIELD_FORM_UID = "uid_form_string";
    private static final String SOLR_PIVOT_NB_FREE_PLACES = SOLR_FIELD_FORM_UID + "," + SOLR_FIELD_NB_FREE_PLACES;
    private static final String SOLR_PIVOT_NB_PLACES = SOLR_FIELD_FORM_UID + "," + SOLR_FIELD_NB_PLACES;
    private static final String MARK_ITEM_DAYS_OF_WEEK = "items_days_of_week";
    private static final String MARK_SITE = "site";
    private static final String MARK_CATEGORIE = "category";
    private static final String MARK_FORM = "form";
    private static final String MARK_FROM_DATE = "from_date";
    private static final String MARK_FROM_TIME = "from_time";
    private static final String MARK_TO_DATE = "to_date";
    private static final String MARK_TO_TIME = "to_time";
    private static final String MARK_FROM_DAY_MINUTE = "from_day_minute";
    private static final String MARK_TO_DAY_MINUTE = "to_day_minute";
    private static final String MARK_RESULTS = "results";

    private static final List<SimpleImmutableEntry<String, String>> SEARCH_FIELDS = Arrays.asList( new SimpleImmutableEntry<>( Utilities.PARAMETER_SITE,
            MARK_SITE ), new SimpleImmutableEntry<>( Utilities.PARAMETER_CATEGORY, MARK_CATEGORIE ), new SimpleImmutableEntry<>( Utilities.PARAMETER_FORM,
            MARK_FORM ), new SimpleImmutableEntry<>( Utilities.PARAMETER_FROM_DATE, MARK_FROM_DATE ), new SimpleImmutableEntry<>(
            Utilities.PARAMETER_FROM_TIME, MARK_FROM_TIME ), new SimpleImmutableEntry<>( Utilities.PARAMETER_TO_DATE, MARK_TO_DATE ),
            new SimpleImmutableEntry<>( Utilities.PARAMETER_TO_TIME, MARK_TO_TIME ), new SimpleImmutableEntry<>( Utilities.PARAMETER_FROM_DAY_MINUTE,
                    MARK_FROM_DAY_MINUTE ), new SimpleImmutableEntry<>( Utilities.PARAMETER_TO_DAY_MINUTE, MARK_TO_DAY_MINUTE ) );

    private static final int SOLR_GROUP_LIMIT = 3;
    private Map<String, String> _searchParameters;
    private Map<String, String [ ]> _searchMultiParameters;

    private void initSearchParameters( )
    {
        if ( _searchParameters == null )
        {
            _searchParameters = new HashMap<>( );
            _searchMultiParameters = new HashMap<>( );
            _searchMultiParameters.put( Utilities.PARAMETER_DAYS_OF_WEEK, Utilities.listDaysCodes );
            _searchParameters.put( Utilities.PARAMETER_FROM_DAY_MINUTE, "360" );
            _searchParameters.put( Utilities.PARAMETER_TO_DAY_MINUTE, "1260" );
            _searchParameters.put( Utilities.PARAMETER_FROM_TIME, "06:00" );
            _searchParameters.put( Utilities.PARAMETER_TO_TIME, "21:00" );
        }
    }

    /**
     * Returns the content of the page AppointmentSearchApp.
     * 
     * @param request
     *            The HTTP request
     * @return The view
     * @throws SiteMessageException
     * @throws AccessDeniedException 
     */
    @View( value = VIEW_SEARCH, defaultView = true )
    public XPage viewSearch( HttpServletRequest request ) throws SiteMessageException
    {
        Map<String, Object> model = new HashMap<String, Object>( );
    	String category = request.getParameter(Utilities.PARAMETER_CATEGORY);
    	if (StringUtils.isEmpty(category)){  
    		addError(ACCESS_DENIED, getLocale( request ));
    		model = getModel();
    		return getXPage( TEMPLATE_SEARCH, request.getLocale( ), model );
    	}
        initSearchParameters( );
        Locale locale = request.getLocale( );

        for ( SimpleImmutableEntry<String, String> entry : SEARCH_FIELDS )
        {
            String strValue = Utilities.getSearchParameter( entry.getKey( ), request, _searchParameters );
            if ( StringUtils.isNotBlank( strValue ) )
            {
                model.put( entry.getValue( ), strValue );
            }
        }

        SolrClient solrServer = SolrServerService.getInstance( ).getSolrServer( );
        if ( solrServer == null )
        {
            AppLogService.error( "AppointmentSolr error, getSolrServer returns null" );
        }

        SolrQuery basedQuery = SolrQueryService.getCommonFilteredQuery( request, _searchParameters, _searchMultiParameters );
        SolrQuery queryAllPlaces = basedQuery;
        queryAllPlaces.setRows( 0 );
        queryAllPlaces.addFacetPivotField( SOLR_PIVOT_NB_PLACES );
        QueryResponse responseAllPlaces = null;
        try
        {
            responseAllPlaces = solrServer.query( queryAllPlaces );
        }
        catch( SolrServerException | IOException e )
        {
            AppLogService.error( "AppointmentSolr error, exception during queryAllPlaces", e );
        }
        if ( responseAllPlaces != null )
        {
            HashMap<String, Integer> mapPlacesCount = getPlacesCount( responseAllPlaces, SOLR_PIVOT_NB_PLACES );
            model.put( "totalPlacesCount", mapPlacesCount );
        }

        SolrQuery query = basedQuery;
        query.setRows( HUGE_INFINITY );
        query.addFilterQuery( SOLR_FILTERQUERY_NOT_FULL );
        query.addSort( SolrQueryService.SOLR_FIELD_DATE, SolrQuery.ORDER.asc );
        query.set( GroupParams.GROUP, true );
        query.set( GroupParams.GROUP_FIELD, SOLR_FIELD_FORM_UID );
        query.set( GroupParams.GROUP_LIMIT, SOLR_GROUP_LIMIT );
        query.setFacet( true );
        query.addFacetPivotField( SOLR_PIVOT_NB_FREE_PLACES );
        query.setFacetMinCount( 1 );
        query.setFacetMissing( true );

        QueryResponse response = null;
        try
        {
            response = solrServer.query( query );
        }
        catch( SolrServerException | IOException e )
        {
            AppLogService.error( "AppointmentSolr error, exception during query", e );
        }
        if ( response != null )
        {
            GroupResponse groupResponse = response.getGroupResponse( );
            HashMap<String, Integer> mapFreePlacesCount = getPlacesCount( response, SOLR_PIVOT_NB_FREE_PLACES );

            Map<String, Object> wrapGroupResponse = wrapGroupResponse( groupResponse );
            sortResponses( wrapGroupResponse, mapFreePlacesCount );

            model.put( MARK_RESULTS, wrapGroupResponse );

            model.put( "freePlacesCount", mapFreePlacesCount );

            String strLabelAll = I18nService.getLocalizedString( "module.appointment.solrsearchapp.labelFilterAll", locale );
            String strLabelEmpty = I18nService.getLocalizedString( "module.appointment.solrsearchapp.labelFilterEmpty", locale );
            for ( SimpleImmutableEntry<String, String> entry : SolrQueryService.FACET_FIELDS )
            {
                ReferenceList referenceList = new ReferenceList( );
                referenceList.addItem( "", strLabelAll ); // Count will be set
                                                          // later with the
                                                          // correct value

                FacetField facetField = response.getFacetField( entry.getKey( ) );
                String strSearchParameter;
                boolean bFacetAndLabel;
                if ( SolrQueryService.SOLR_FIELD_FORM_UID_TITLE.equals( entry.getKey( ) ) )
                {
                    bFacetAndLabel = true;
                    strSearchParameter = Utilities.PARAMETER_FORM;
                }
                else
                {
                    bFacetAndLabel = false;
                    strSearchParameter = Utilities.PARAMETER_CATEGORY;
                    for ( SimpleImmutableEntry<String, String> fq_entry : SolrQueryService.EXACT_FACET_QUERIES )
                    {
                        if ( fq_entry.getKey( ).equals( entry.getKey( ) ) )
                        {
                            strSearchParameter = fq_entry.getValue( );
                            break;
                        }
                    }
                }
                boolean bCurrentSearchParameterPresent = false;
                String strSearchParameterValue = Utilities.getSearchParameterValue( strSearchParameter, request, _searchParameters );

                int total = 0;
                for ( FacetField.Count facetFieldCount : facetField.getValues( ) )
                {
                    String strCode;
                    String strName;
                    String strCodeName;

                    if ( facetFieldCount.getName( ) == null )
                    {
                        bFacetAndLabel = false;
                    }

                    if ( bFacetAndLabel )
                    {
                        String [ ] facetNameSplit = facetFieldCount.getName( ).split( "\\|" );
                        strCode = facetNameSplit [0];
                        strName = facetNameSplit [1];
                        strCodeName = facetNameSplit [0] + "|" + facetNameSplit [1];
                    }
                    else
                    {
                        strCode = facetFieldCount.getName( );
                        strName = facetFieldCount.getName( );
                        strCodeName = facetFieldCount.getName( );
                        // Here, we could add a difference between null and ""
                        // by using
                        // another special value (eg VALUE_FQ_MISSING =
                        // __MISSING__).
                        if ( StringUtils.isEmpty( strCode ) )
                        {
                            strCode = SolrQueryService.VALUE_FQ_EMPTY;
                            strCodeName = SolrQueryService.VALUE_FQ_EMPTY;
                            strName = strLabelEmpty;
                        }
                    }
                    // Even though we set FacetMinCount to 1, solr gives a
                    // result with a count of 0 for the docs missing the field
                    if ( facetFieldCount.getCount( ) > 0 )
                    {
                        referenceList.addItem( strCodeName, strName + " (" + facetFieldCount.getCount( ) + ")" );
                        bCurrentSearchParameterPresent |= strCode.equals( strSearchParameterValue );
                        total += facetFieldCount.getCount( );
                    }
                }

                ReferenceItem itemAll = referenceList.get( 0 );
                itemAll.setName( itemAll.getName( ) + " (" + total + ")" );

                if ( !bCurrentSearchParameterPresent && StringUtils.isNotEmpty( strSearchParameterValue ) )
                {
                    String strSearchParameterValueLabel = Utilities.getSearchParameter( strSearchParameter, request, _searchParameters );
                    String strSearchParameterLabel = Utilities.getSearchParameterLabel( strSearchParameter, request, _searchParameters );
                    referenceList.addItem( strSearchParameterValueLabel, strSearchParameterLabel + " (0)" );
                }
                model.put( entry.getValue( ), referenceList );
            }

            FacetField facetField = response.getFacetField( SolrQueryService.SOLR_FIELD_DAY_OF_WEEK );
            ReferenceList referenceListDaysOfWeek = new ReferenceList( );
            Set<String> searchDaysChecked = new HashSet<>( );
            String [ ] searchDays = Utilities.getSearchMultiParameter( Utilities.PARAMETER_DAYS_OF_WEEK, request, _searchMultiParameters );
            if ( searchDays != null )
            {
                searchDaysChecked.addAll( Arrays.asList( searchDays ) );
            }
            if ( searchDaysChecked.isEmpty( ) )
            {
                searchDaysChecked.addAll( Arrays.asList( Utilities.listDaysCodes ) );
            }
            Map<String, FacetField.Count> searchDaysCounts = new HashMap<>( );
            for ( FacetField.Count facetFieldCount : facetField.getValues( ) )
            {
                searchDaysCounts.put( facetFieldCount.getName( ), facetFieldCount );
            }

            for ( SimpleImmutableEntry<String, String> day : Utilities.listDays )
            {
                String strDayCode = day.getKey( );
                String strDayLabel = I18nService.getLocalizedString( day.getValue( ), locale );
                ReferenceItem item = new ReferenceItem( );
                item.setCode( strDayCode );
                long count;
                FacetField.Count facetFieldCount = searchDaysCounts.get( strDayCode );
                if ( facetFieldCount != null )
                {
                    count = facetFieldCount.getCount( );
                }
                else
                {
                    count = 0;
                }
                item.setName( strDayLabel + " (" + count + ")" );
                item.setChecked( searchDaysChecked.contains( strDayCode ) );
                referenceListDaysOfWeek.add( item );
            }
            model.put( MARK_ITEM_DAYS_OF_WEEK, referenceListDaysOfWeek );
        }
        return getXPage( TEMPLATE_SEARCH, request.getLocale( ), model );
    }

    // Getting the types right for all the maps and lists is just to tedious
    // Working directly on the solr types would make it easier, but I don't
    // want to mutate their objects directly in case it breaks their code.
    @SuppressWarnings( {
            "rawtypes", "unchecked"
    } )
    private void sortResponses( Map<String, Object> wrapGroupResponse, HashMap<String, Integer> mapFreePlacesCount )
    {
        List<Object> listGroupCommands = (List) wrapGroupResponse.get( "values" );
        List<Map> listGroups = ( (List) ( (Map) listGroupCommands.get( 0 ) ).get( "values" ) );

        listGroups.sort( Comparator.comparing( group -> {
            Map firstResult = getFirstResult( group );
            return (Date) firstResult.get( "date" );
        } ).thenComparing( group -> {
            String code = (String) ( (Map) group ).get( "groupValue" );
            return mapFreePlacesCount.get( code );
        }, Comparator.reverseOrder( ) ).thenComparing( group -> {
            Map firstResult = getFirstResult( group );
            return (String) firstResult.get( "title" );
        } ) );
    }

    // Getting the types right for all the maps and lists is just to tedious
    // Working directly on the solr types would make it easier, but I don't
    // want to mutate their objects directly in case it breaks their code.
    @SuppressWarnings( "rawtypes" )
    private Map getFirstResult( Object group )
    {
        Map mapGroup = (Map) group;
        Map mapResult = (Map) mapGroup.get( "result" );
        List listResult = (List) mapResult.get( "list" );
        Map firstResult = (Map) listResult.get( 0 );
        return firstResult;
    }

    /**
     * Process the search of the page solrsearchapp.
     * 
     * @param request
     *            The HTTP request
     * @return The view
     */
    @Action( value = ACTION_SEARCH )
    public XPage doSearch( HttpServletRequest request )
    {
        initSearchParameters( );
        for ( SimpleImmutableEntry<String, String> entry : SEARCH_FIELDS )
        {
            String strValue = request.getParameter( entry.getKey( ) );
            if ( strValue != null )
            {
                _searchParameters.put( entry.getKey( ), strValue );
            }
        }

        _searchMultiParameters.put( Utilities.PARAMETER_DAYS_OF_WEEK, request.getParameterValues( Utilities.PARAMETER_DAYS_OF_WEEK ) );
        // Need to put the category in the url
        LinkedHashMap<String, String> additionalParameters = new LinkedHashMap<String, String>( );
        additionalParameters.put( Utilities.PARAMETER_CATEGORY, request.getParameter( Utilities.PARAMETER_CATEGORY ) );
        return redirect( request, VIEW_SEARCH, additionalParameters );
    }

    /**
     * Process the search of the page solrsearchapp.
     * 
     * @param request
     *            The HTTP request
     * @return The view
     */
    @Action( value = ACTION_CLEAR )
    public XPage doClear( HttpServletRequest request )
    {
        initSearchParameters( );
        _searchParameters.clear( );
        _searchMultiParameters.clear( );
        _searchMultiParameters.put( Utilities.PARAMETER_DAYS_OF_WEEK, Utilities.listDaysCodes );
        _searchParameters.put( Utilities.PARAMETER_FROM_DAY_MINUTE, "360" );
        _searchParameters.put( Utilities.PARAMETER_TO_DAY_MINUTE, "1260" );
        _searchParameters.put( Utilities.PARAMETER_FROM_TIME, "06:00" );
        _searchParameters.put( Utilities.PARAMETER_TO_TIME, "21:00" );

        // Need to put the category in the url
        LinkedHashMap<String, String> additionalParameters = new LinkedHashMap<String, String>( );
        additionalParameters.put( Utilities.PARAMETER_CATEGORY, request.getParameter( Utilities.PARAMETER_CATEGORY ) );
        return redirect( request, VIEW_SEARCH, additionalParameters );
    }

    private HashMap<String, Integer> getPlacesCount( QueryResponse response, String strPivotName )
    {
        HashMap<String, Integer> mapPlacesCount = new HashMap<>( );
        List<PivotField> listPivotField = response.getFacetPivot( ).get( strPivotName );
        for ( PivotField pivotFieldUid : listPivotField )
        {
            int total = 0;
            for ( PivotField pivotFieldPlaces : pivotFieldUid.getPivot( ) )
            {
                total += (Long) pivotFieldPlaces.getValue( ) * pivotFieldPlaces.getCount( );
            }
            mapPlacesCount.put( (String) pivotFieldUid.getValue( ), total );
        }
        return mapPlacesCount;
    }

    // SolrDocumentList extends ArrayList and has extra methods
    // But in Freemarker, we can't have access to those extra methods (like
    // getNumFound())
    // So unwrap everything.
    // We could use ?api in freemarker version 2.3.22 but we are stuck with
    // 2.3.21.
    private Map<String, Object> wrapGroupResponse( GroupResponse groupResponse )
    {
        Map<String, Object> result = new HashMap<>( );
        List<Map<String, Object>> wrappedListGroupCommand = new ArrayList<>( groupResponse.getValues( ).size( ) );
        for ( GroupCommand groupCommand : groupResponse.getValues( ) )
        {
            wrappedListGroupCommand.add( wrapGroupCommand( groupCommand ) );
        }
        result.put( "values", wrappedListGroupCommand );
        return result;
    }

    private Map<String, Object> wrapGroupCommand( GroupCommand groupCommand )
    {
        Map<String, Object> result = new HashMap<>( );
        List<Map<String, Object>> wrappedListGroup = new ArrayList<>( groupCommand.getValues( ).size( ) );
        for ( Group group : groupCommand.getValues( ) )
        {
            wrappedListGroup.add( wrapGroup( group ) );
        }
        result.put( "values", wrappedListGroup );
        result.put( "matches", groupCommand.getMatches( ) );
        result.put( "name", groupCommand.getName( ) );
        result.put( "nGroups", groupCommand.getNGroups( ) );
        return result;
    }

    private Map<String, Object> wrapGroup( Group group )
    {
        Map<String, Object> result = new HashMap<>( );
        result.put( "result", wrapSolrDocumentList( group.getResult( ) ) );
        result.put( "groupValue", group.getGroupValue( ) );
        return result;
    }

    private Map<String, Object> wrapSolrDocumentList( SolrDocumentList solrDocumentList )
    {
        Map<String, Object> result = new HashMap<>( );
        result.put( "maxScore", solrDocumentList.getMaxScore( ) );
        result.put( "numFound", solrDocumentList.getNumFound( ) );
        result.put( "start", solrDocumentList.getStart( ) );
        result.put( "list", solrDocumentList );
        return result;
    }
}
