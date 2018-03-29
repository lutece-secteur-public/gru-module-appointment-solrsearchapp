package fr.paris.lutece.plugins.appointment.modules.solrsearchapp.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.util.ClientUtils;

public class SolrQueryService
{

    private static final String SOLR_FIELD_SITE = "site";
    private static final String SOLR_FIELD_CATEGORY = "categorie";
    public static final String SOLR_FIELD_FORM_UID_TITLE = "form_id_title_string";
    private static final String SOLR_FIELD_FORM_UID = "uid_form_string";
    private static final String SOLR_QUERY_ALL = "*:*";
    private static final String SOLR_FILTERQUERY_ALLOWED_NOW = "{!frange l=0}sub(sub(ms(date),mul(3600000,min_hours_before_appointment_long)),ms())";
    private static final String SOLR_FILTERQUERY_DAY_OPEN = "day_open_string:true";
    private static final String SOLR_FILTERQUERY_ENABLED = "enabled_string:true";
    private static final String SOLR_FILTERQUERY_ACTIVE = "appointment_active_string:true";
    private static final String SOLR_FIELD_TYPE = "type";
    public static final String SOLR_FIELD_DATE = "date";
    private static final String SOLR_FIELD_MINUTE_OF_DAY = "minute_of_day_long";
    public static final String SOLR_FIELD_DAY_OF_WEEK = "day_of_week_long";
    private static final String SOLR_TYPE_APPOINTMENT_SLOT = "appointment-slot";
    public static final String VALUE_FQ_EMPTY = "__EMPTY__";

    public static SolrQuery getCommonFilteredQuery( HttpServletRequest request, Map<String, String> _searchParameters,
            Map<String, String [ ]> _searchMultiParameters )
    {
        SolrQuery query = new SolrQuery( );
        query.setQuery( SOLR_QUERY_ALL );
        query.addFilterQuery( SOLR_FIELD_TYPE + ":" + SOLR_TYPE_APPOINTMENT_SLOT );
        query.addFilterQuery( SOLR_FILTERQUERY_ALLOWED_NOW );
        query.addFilterQuery( SOLR_FILTERQUERY_DAY_OPEN );
        query.addFilterQuery( SOLR_FILTERQUERY_ENABLED );
        query.addFilterQuery( SOLR_FILTERQUERY_ACTIVE );

        for ( SimpleImmutableEntry<String, String> entry : EXACT_FACET_QUERIES )
        {
            String strValue = Utilities.getSearchParameterValue( entry.getValue( ), request, _searchParameters );
            String strFacetField;
            if ( SOLR_FIELD_FORM_UID.equals( entry.getKey( ) ) )
            {
                strFacetField = SOLR_FIELD_FORM_UID_TITLE;
            }
            else
            {
                strFacetField = entry.getKey( );
            }
            if ( StringUtils.isNotBlank( strValue ) )
            {
                String strFilterQuery;
                if ( VALUE_FQ_EMPTY.equals( strValue ) )
                {
                    strFilterQuery = entry.getKey( ) + ":" + "\"\" OR (*:* NOT " + entry.getKey( ) + ":*)";
                }
                else
                {
                    strFilterQuery = entry.getKey( ) + ":" + ClientUtils.escapeQueryChars( strValue );
                }
                query.addFilterQuery( "{!tag=tag" + strFacetField + "}" + strFilterQuery );
                strFacetField = "{!ex=tag" + strFacetField + "}" + strFacetField;
            }
            query.addFacetField( strFacetField );
        }

        StringBuffer sbFqDaysOfWeek = new StringBuffer( );
        String [ ] searchDays = Utilities.getSearchMultiParameter( Utilities.PARAMETER_DAYS_OF_WEEK, request, _searchMultiParameters );
        if ( searchDays != null && searchDays.length > 0 )
        {
            sbFqDaysOfWeek.append( "{!tag=tag" + SOLR_FIELD_DAY_OF_WEEK + "}" + SOLR_FIELD_DAY_OF_WEEK + ":(" );
            for ( int nDay = 0; nDay < searchDays.length; nDay++ )
            {
                if ( nDay > 0 )
                {
                    sbFqDaysOfWeek.append( " OR " );
                }
                sbFqDaysOfWeek.append( searchDays [nDay] );
            }
            sbFqDaysOfWeek.append( ")" );
        }
        query.addFilterQuery( sbFqDaysOfWeek.toString( ) );
        query.addFacetField( "{!ex=tag" + SOLR_FIELD_DAY_OF_WEEK + "}" + SOLR_FIELD_DAY_OF_WEEK );

        String strFromDate = Utilities.getSearchParameterValue( Utilities.PARAMETER_FROM_DATE, request, _searchParameters );
        String strFromTime = Utilities.getSearchParameterValue( Utilities.PARAMETER_FROM_TIME, request, _searchParameters );
        String strToDate = Utilities.getSearchParameterValue( Utilities.PARAMETER_TO_DATE, request, _searchParameters );
        String strToTime = Utilities.getSearchParameterValue( Utilities.PARAMETER_TO_TIME, request, _searchParameters );
        LocalDateTime localDateTimeFrom = null;
        try
        {
            localDateTimeFrom = LocalDateTime.parse( strFromDate + " " + strFromTime, Utilities.inputFormatter );
        }
        catch( DateTimeParseException e )
        {
            localDateTimeFrom = null;
        }
        String strSolrDateTimeFrom = "*";
        if ( localDateTimeFrom != null )
        {
            strSolrDateTimeFrom = localDateTimeFrom.format( Utilities.outputFormatter );
        }
        LocalDateTime localDateTimeTo = null;
        try
        {
            localDateTimeTo = LocalDateTime.parse( strToDate + " " + strToTime, Utilities.inputFormatter );
        }
        catch( DateTimeParseException e )
        {
            localDateTimeTo = null;
        }
        String strSolrDateTimeTo = "*";
        if ( localDateTimeFrom != null )
        {
            strSolrDateTimeTo = localDateTimeTo.format( Utilities.outputFormatter );
        }
        query.addFilterQuery( SOLR_FIELD_DATE + ":[" + strSolrDateTimeFrom + " TO " + strSolrDateTimeTo + "]" );
        String strFromDayMinute = Utilities.getSearchParameterValue( Utilities.PARAMETER_FROM_DAY_MINUTE, request, _searchParameters );
        String strToDayMinute = Utilities.getSearchParameterValue( Utilities.PARAMETER_TO_DAY_MINUTE, request, _searchParameters );
        String strSolrDayMinuteFrom = "*";
        if ( strFromDayMinute != null )
        {
            strSolrDayMinuteFrom = strFromDayMinute;
        }
        String strSolrDayMinuteTo = "*";
        if ( strToDayMinute != null )
        {
            strSolrDayMinuteTo = strToDayMinute;
        }
        query.addFilterQuery( SOLR_FIELD_MINUTE_OF_DAY + ":[" + strSolrDayMinuteFrom + " TO " + strSolrDayMinuteTo + "]" );
        return query;
    }

    public static final List<SimpleImmutableEntry<String, String>> EXACT_FACET_QUERIES = Arrays.asList( new SimpleImmutableEntry<>( SOLR_FIELD_SITE,
            Utilities.PARAMETER_SITE ), new SimpleImmutableEntry<>( SOLR_FIELD_CATEGORY, Utilities.PARAMETER_CATEGORY ), new SimpleImmutableEntry<>(
            SOLR_FIELD_FORM_UID, Utilities.PARAMETER_FORM ) );

    public static final List<SimpleImmutableEntry<String, String>> FACET_FIELDS = Arrays.asList( new SimpleImmutableEntry<>( SOLR_FIELD_SITE,
            Utilities.MARK_ITEM_SITES ), new SimpleImmutableEntry<>( SOLR_FIELD_CATEGORY, Utilities.MARK_ITEM_CATEGORIES ), new SimpleImmutableEntry<>(
            SOLR_FIELD_FORM_UID_TITLE, Utilities.MARK_ITEM_FORMS ) );

}
