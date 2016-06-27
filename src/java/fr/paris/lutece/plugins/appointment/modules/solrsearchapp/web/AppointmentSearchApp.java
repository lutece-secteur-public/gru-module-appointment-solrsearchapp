/*
 * Copyright (c) 2002-2015, Mairie de Paris
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.Group;
import org.apache.solr.client.solrj.response.GroupCommand;
import org.apache.solr.client.solrj.response.GroupResponse;
import org.apache.solr.client.solrj.response.PivotField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.GroupParams;

import fr.paris.lutece.plugins.search.solr.business.SolrServerService;
import fr.paris.lutece.portal.service.message.SiteMessageException;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.util.mvc.commons.annotations.Action;
import fr.paris.lutece.portal.util.mvc.commons.annotations.View;
import fr.paris.lutece.portal.util.mvc.xpage.MVCApplication;
import fr.paris.lutece.portal.util.mvc.xpage.annotations.Controller;
import fr.paris.lutece.portal.web.xpages.XPage;
import fr.paris.lutece.util.ReferenceItem;
import fr.paris.lutece.util.ReferenceList;

@Controller( xpageName = "appointmentsearch" , pageTitleI18nKey = "appointment.modules.solr.xpage.appointmentsearch.pageTitle" , pagePathI18nKey = "appointment.modules.solr.xpage.ideation.pagePathLabel" )
public class AppointmentSearchApp extends MVCApplication {

    private static final long serialVersionUID = 3579388931034541505L;

    private static final String VIEW_SEARCH = "search";
    private static final String ACTION_SEARCH = "search";
    private static final String ACTION_CLEAR = "clear";
    private static final String TEMPLATE_SEARCH = "skin/plugins/appointment/modules/solrsearchapp/search.html";

    private static final String PARAMETER_SITE = "site";
    private static final String PARAMETER_CATEGORY = "category";
    private static final String PARAMETER_FORM = "form";
    private static final String PARAMETER_FROM_DATE = "from_date";
    private static final String PARAMETER_FROM_TIME = "from_time";
    private static final String PARAMETER_TO_DATE = "to_date";
    private static final String PARAMETER_TO_TIME = "to_time";

    private static final String DATE_FORMAT_INPUT_DATE = "dd/MM/yyyy";
    private static final String DATE_FORMAT_INPUT_TIME = "HH:mm";
    private static final String DATE_FORMAT_OUTPUT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private static final String MARK_ITEM_SITES = "items_sites";
    private static final String MARK_ITEM_CATEGORIES = "items_categories";
    private static final String MARK_ITEM_FORMS = "items_forms";
    private static final String MARK_SITE = "site";
    private static final String MARK_CATEGORIE = "category";
    private static final String MARK_FORM = "form";
    private static final String MARK_FROM_DATE = "from_date";
    private static final String MARK_FROM_TIME = "from_time";
    private static final String MARK_TO_DATE = "to_date";
    private static final String MARK_TO_TIME = "to_time";
    private static final String MARK_RESULTS = "results";

    private static final List<SimpleImmutableEntry<String,String>> SEARCH_FIELDS = Arrays.asList(
        new SimpleImmutableEntry<>( PARAMETER_SITE     , MARK_SITE      ),
        new SimpleImmutableEntry<>( PARAMETER_CATEGORY , MARK_CATEGORIE ),
        new SimpleImmutableEntry<>( PARAMETER_FORM     , MARK_FORM      ),
        new SimpleImmutableEntry<>( PARAMETER_FROM_DATE, MARK_FROM_DATE ),
        new SimpleImmutableEntry<>( PARAMETER_FROM_TIME, MARK_FROM_TIME ),
        new SimpleImmutableEntry<>( PARAMETER_TO_DATE  , MARK_TO_DATE   ),
        new SimpleImmutableEntry<>( PARAMETER_TO_TIME  , MARK_TO_TIME   )
    );

    private static final int SOLR_GROUP_LIMIT = 3;

    private static final String SOLR_QUERY_ALL = "*:*";
    private static final String SOLR_FILTERQUERY_ALLOWED_NOW = "{!frange l=0}sub(sub(ms(date),mul(3600000,min_hours_before_appointment_long)),ms())";
    private static final String SOLR_FILTERQUERY_NOT_FULL = "NOT slot_nb_free_place_long:0";
    private static final String SOLR_FILTERQUERY_ACTIVE = "appointment_active_string:true";
    private static final String SOLR_FIELD_TYPE = "type";
    private static final String SOLR_FIELD_SITE = "site";
    private static final String SOLR_FIELD_CATEGORY = "categorie";
    private static final String SOLR_FIELD_UID = "uid";
    private static final String SOLR_FIELD_FORM_UID_TITLE = "form_id_title_string";
    private static final String SOLR_FIELD_FORM_UID = "uid_form_string";
    private static final String SOLR_FIELD_DATE = "date";
    private static final String SOLR_FIELD_NB_FREE_PLACES = "slot_nb_free_places_long";
    private static final String SOLR_FIELD_NB_PLACES = "slot_nb_places_long";
    private static final String SOLR_TYPE_APPOINTMENT = "appointment";
    private static final String SOLR_TYPE_APPOINTMENT_SLOT = "appointment-slot";

    private static final List<SimpleImmutableEntry<String,String>> FACET_FIELDS = Arrays.asList(
        new SimpleImmutableEntry<>( SOLR_FIELD_SITE, MARK_ITEM_SITES ),
        new SimpleImmutableEntry<>( SOLR_FIELD_CATEGORY, MARK_ITEM_CATEGORIES ),
        new SimpleImmutableEntry<>( SOLR_FIELD_FORM_UID_TITLE, MARK_ITEM_FORMS )
    );

    private static final List<SimpleImmutableEntry<String,String>> EXACT_FACET_QUERIES = Arrays.asList(
        new SimpleImmutableEntry<>( SOLR_FIELD_SITE, PARAMETER_SITE ),
        new SimpleImmutableEntry<>( SOLR_FIELD_CATEGORY, PARAMETER_CATEGORY ),
        new SimpleImmutableEntry<>( SOLR_FIELD_FORM_UID, PARAMETER_FORM )
    );

    private Map<String, String> _searchParameters;

    private String getSearchParameter( String parameter, HttpServletRequest request, Map<String, String> savedSearchParameters ) {
        String result = request.getParameter( parameter );
        if (result != null) {
            return result;
        }
        result = savedSearchParameters.get(parameter);
        return result;
    }

    private String getSearchParameterValue( String parameter, HttpServletRequest request, Map<String, String> savedSearchParameters ) {
        String requestValue = getSearchParameter( parameter, request, savedSearchParameters);
        if (requestValue != null) {
            String[] requestSplit = requestValue.split("\\|");
            return requestSplit[0];
        } else {
            return null;
        }
    }

    private String getSearchParameterLabel( String parameter, HttpServletRequest request, Map<String, String> savedSearchParameters ) {
        String requestValue = getSearchParameter( parameter, request, savedSearchParameters);
        if (requestValue != null) {
            String[] requestSplit = requestValue.split("\\|");
            return requestSplit[requestSplit.length-1];
        } else {
            return null;
        }
    }

    private void initSearchParameters() {
        if (_searchParameters == null) {
            _searchParameters = new HashMap<>();
        }
    }

    /**
     * Returns the content of the page AppointmentSearchApp.
     * @param request The HTTP request
     * @return The view
     * @throws SiteMessageException 
     */
    @View( value = VIEW_SEARCH , defaultView = true )
    public XPage viewSearch( HttpServletRequest request ) throws SiteMessageException {
        Map<String, Object> model = new HashMap<String,Object>();
        initSearchParameters();

        for (SimpleImmutableEntry<String,String> entry: SEARCH_FIELDS) {
            String strValue = getSearchParameter( entry.getKey(), request, _searchParameters );
            if (StringUtils.isNotBlank(strValue)) {
                model.put( entry.getValue(), strValue );
            }
        }

        SolrServer solrServer = SolrServerService.getInstance(  ).getSolrServer(  );
        if ( solrServer == null ) {
            AppLogService.error ( "AppointmentSolr error, getSolrServer returns null" ); 
        }

        SolrQuery query = new SolrQuery();
        query.setQuery( SOLR_QUERY_ALL );
        query.addFilterQuery( SOLR_FIELD_TYPE + ":" + SOLR_TYPE_APPOINTMENT_SLOT );
        query.addFilterQuery( SOLR_FILTERQUERY_ALLOWED_NOW );
        query.addFilterQuery( SOLR_FILTERQUERY_NOT_FULL    );
        query.addFilterQuery( SOLR_FILTERQUERY_ACTIVE      );
        query.addSort( SOLR_FIELD_DATE, SolrQuery.ORDER.asc );
        query.set(GroupParams.GROUP, true);
        query.set(GroupParams.GROUP_FIELD, SOLR_FIELD_FORM_UID );
        query.set(GroupParams.GROUP_LIMIT, SOLR_GROUP_LIMIT );
        query.setFacet( true );
        query.addFacetPivotField( SOLR_FIELD_FORM_UID + "," + SOLR_FIELD_NB_FREE_PLACES );
        query.addFacetPivotField( SOLR_FIELD_FORM_UID + "," + SOLR_FIELD_NB_PLACES );
        query.setFacetMinCount( 1 );

        for (SimpleImmutableEntry<String,String> entry: EXACT_FACET_QUERIES) {
            String strValue = getSearchParameterValue( entry.getValue(), request, _searchParameters );
            String strFacetField;
            if ( SOLR_FIELD_FORM_UID.equals(entry.getKey()) ) {
                strFacetField = SOLR_FIELD_FORM_UID_TITLE;
            } else {
                strFacetField = entry.getKey();
            }
            if (StringUtils.isNotBlank(strValue)) {
                query.addFilterQuery ( "{!tag=tag" + strFacetField + "}" + entry.getKey() + ":" + strValue );
                strFacetField = "{!ex=tag" + strFacetField + "}" + strFacetField;
            }
            query.addFacetField( strFacetField );
        }

        String strFromDate = getSearchParameterValue(PARAMETER_FROM_DATE, request, _searchParameters);
        String strFromTime = getSearchParameterValue(PARAMETER_FROM_TIME, request, _searchParameters);
        String strToDate = getSearchParameterValue(PARAMETER_TO_DATE, request, _searchParameters);
        String strToTime = getSearchParameterValue(PARAMETER_TO_TIME, request, _searchParameters);

        //SimpleDateFormat is not thread safe, create a new one each time..
        SimpleDateFormat formatter_input = new SimpleDateFormat( DATE_FORMAT_INPUT_DATE + " " + DATE_FORMAT_INPUT_TIME );
        Date dateFrom;
        try {
            dateFrom = formatter_input.parse( strFromDate + " " + strFromTime );
        } catch (ParseException e) {
            AppLogService.error("Failed parse" + e, e);
            dateFrom = null;
        }

        Date dateTo;
        try {
            dateTo = formatter_input.parse( strToDate + " " + strToTime );
        } catch (ParseException e) {
            dateTo = null;
        }

        if (dateFrom != null || dateTo != null) {
            SimpleDateFormat formatter_output = new SimpleDateFormat( DATE_FORMAT_OUTPUT );
            String strSolrDateFrom;
            String strSolrDateTo;
            if (dateFrom != null) {
                formatter_output.setTimeZone(TimeZone.getTimeZone("UTC"));
                strSolrDateFrom = formatter_output.format( dateFrom );
            } else {
                strSolrDateFrom = "*";
            }
            if (dateTo != null) {
                formatter_output.setTimeZone(TimeZone.getTimeZone("UTC"));
                strSolrDateTo = formatter_output.format( dateFrom );
            } else {
                strSolrDateTo = "*";
            }
            query.addFilterQuery ( SOLR_FIELD_DATE + ":[" + strSolrDateFrom + " TO " + strSolrDateTo + "]");
        }

        QueryResponse response = null;
        try {
            response = solrServer.query(query);
        } catch (SolrServerException e) {
            AppLogService.error ( "AppointmentSolr error, exception during query", e); 
        }
        if (response != null) {
            GroupResponse groupResponse = response.getGroupResponse();
            model.put( MARK_RESULTS, wrapGroupResponse(groupResponse) );

            HashMap<String, Object> mapPlacesCount = new HashMap<>();
            for (Map.Entry<String, List<PivotField>> entry: response.getFacetPivot()) {
                HashMap<String, Object> mapFormPlacesCount = new HashMap<>();
                String key;
                if (entry.getKey().contains( SOLR_FIELD_NB_FREE_PLACES )) {
                    key = SOLR_FIELD_NB_FREE_PLACES;
                } else if (entry.getKey().contains( SOLR_FIELD_NB_PLACES )) {
                    key = SOLR_FIELD_NB_PLACES;
                } else {
                    continue;
                }
                for (PivotField pivotFieldUid: entry.getValue()) {
                    int total = 0;
                    for (PivotField pivotFieldPlaces: pivotFieldUid.getPivot()) {
                        total += (Long) pivotFieldPlaces.getValue() * pivotFieldPlaces.getCount();
                    }
                    mapFormPlacesCount.put( (String) pivotFieldUid.getValue(), total );
                }
                mapPlacesCount.put(key, mapFormPlacesCount);
            }
            model.put( "placesCount", mapPlacesCount );

            for (SimpleImmutableEntry<String,String> entry: FACET_FIELDS) {
                ReferenceList referenceList = new ReferenceList();
                referenceList.addItem ( "", "Tous" ); // Count will be set later with the correct value

                FacetField facetField = response.getFacetField( entry.getKey() );
                String strSearchParameter;
                boolean bFacetAndLabel;
                if (SOLR_FIELD_FORM_UID_TITLE.equals(entry.getKey())) {
                    bFacetAndLabel = true;
                    strSearchParameter = PARAMETER_FORM;
                } else {
                    bFacetAndLabel = false;
                    strSearchParameter = PARAMETER_CATEGORY;
                    for (SimpleImmutableEntry<String,String> fq_entry: EXACT_FACET_QUERIES) {
                        if (fq_entry.getKey().equals(entry.getKey())) {
                            strSearchParameter = fq_entry.getValue();
                            break;
                        }
                    }
                }
                boolean bCurrentSearchParameterPresent = false;
                String strSearchParameterValue = getSearchParameterValue(strSearchParameter, request, _searchParameters);

                int total = 0;
                for (FacetField.Count facetFieldCount: facetField.getValues()) {
                    String strCode;
                    String strName;
                    String strCodeName;
                    if (bFacetAndLabel) {
                        String[] facetNameSplit = facetFieldCount.getName().split("\\|");
                        strCode = facetNameSplit[0];
                        strName = facetNameSplit[1];
                        strCodeName = facetNameSplit[0] + "|" + facetNameSplit[1];
                    } else {
                        strCode = facetFieldCount.getName();
                        strName = facetFieldCount.getName();
                        strCodeName = facetFieldCount.getName();
                    }
                    //If there is an empty value in solr,
                    //we don't display it as a filter
                    //but still count it in the total.
                    if ( StringUtils.isNotBlank( strCodeName ) ) {
                        referenceList.addItem( strCodeName, strName + " (" + facetFieldCount.getCount() + ")" );
                        bCurrentSearchParameterPresent |= strCode.equals(strSearchParameterValue);
                    }
                    total += facetFieldCount.getCount();
                }

                ReferenceItem itemAll = referenceList.get(0);
                itemAll.setName( itemAll.getName( ) + " (" + total + ")");

                if (!bCurrentSearchParameterPresent && StringUtils.isNotEmpty( strSearchParameterValue )) {
                    String strSearchParameterValueLabel = getSearchParameter(strSearchParameter, request, _searchParameters);
                    String strSearchParameterLabel = getSearchParameterLabel(strSearchParameter, request, _searchParameters);
                    referenceList.addItem ( strSearchParameterValueLabel, strSearchParameterLabel + " (0)" );
                }
                model.put( entry.getValue(), referenceList );
            }
        }
        return getXPage(TEMPLATE_SEARCH, request.getLocale(), model);
    }

    //SolrDocumentList extends ArrayList and has extra methods
    //But in Freemarker, we can't have access to those extra methods (like getNumFound())
    //So unwrap everything.
    //We could use ?api in freemarker version 2.3.22 but we are stuck with 2.3.21.
    private Map<String, Object> wrapGroupResponse(GroupResponse groupResponse) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> wrappedListGroupCommand = new ArrayList<>(groupResponse.getValues().size());
        for (GroupCommand groupCommand: groupResponse.getValues()) {
            wrappedListGroupCommand.add(wrapGroupCommand(groupCommand));
        }
        result.put( "values", wrappedListGroupCommand );
        return result;
    }
    private Map<String, Object> wrapGroupCommand(GroupCommand groupCommand) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> wrappedListGroup = new ArrayList<>(groupCommand.getValues().size());
        for (Group group: groupCommand.getValues()) {
            wrappedListGroup.add(wrapGroup(group));
        }
        result.put( "values", wrappedListGroup );
        result.put( "matches", groupCommand.getMatches() );
        result.put( "name", groupCommand.getName() );
        result.put( "nGroups", groupCommand.getNGroups() );
        return result;
    }
    private Map<String, Object> wrapGroup(Group group) {
        Map<String, Object> result = new HashMap<>();
        result.put( "result", wrapSolrDocumentList(group.getResult()));
        result.put( "groupValue", group.getGroupValue() );
        return result;
    }
    private Map<String, Object> wrapSolrDocumentList(SolrDocumentList solrDocumentList) {
        Map<String, Object> result = new HashMap<>();
        result.put( "maxScore", solrDocumentList.getMaxScore() );
        result.put( "numFound", solrDocumentList.getNumFound() );
        result.put( "start", solrDocumentList.getStart() );
        result.put( "list", solrDocumentList );
        return result;
    }


    /**
    * Process the search of the page ideation.
    * @param request The HTTP request
    * @return The view
    */
    @Action( value = ACTION_SEARCH )
    public XPage doSearch( HttpServletRequest request )
    {
        initSearchParameters();
        for (SimpleImmutableEntry<String,String> entry: SEARCH_FIELDS) {
            String strValue = request.getParameter(entry.getKey());
            if (strValue != null) {
                _searchParameters.put( entry.getValue(), strValue );
            }
        }

        return redirectView( request, VIEW_SEARCH );
    }

    /**
    * Process the search of the page ideation.
    * @param request The HTTP request
    * @return The view
    */
    @Action( value = ACTION_CLEAR )
    public XPage doClear( HttpServletRequest request )
    {
        initSearchParameters();
        _searchParameters.clear();

        return redirectView( request, VIEW_SEARCH );
    }
}
