/*
 * Copyright (c) 2002-2026, City of Paris
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.portal.service.editor.RichTextContentMethod;
import fr.paris.lutece.portal.service.template.HtmlAttributesMethod;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.TemplateMethodModelEx;

/**
 * Renders the search page with the configuration of the core (LUT-33153): HTML output format, auto-escaping, the front
 * office commons of the exploded webapp and htmlAttributes strict. The values of the Solr results and of the request are
 * written as text, the links of the slots go through safeUrl, and the points of the map are written in data attributes
 * instead of a script.
 */
public class AppointmentSolrSearchAppTemplatesRenderTest
{
    private static final String TEMPLATES_ROOT = "webapp/WEB-INF/templates";
    private static final String CORE_TEMPLATES_ROOT = "target/lutece/WEB-INF/templates";
    private static final String COMMONS_SITE = "commons_site.html";
    private static final String COMMONS_THEME = "skin/themes/global_theme_commons.ftl";
    /** The number format of AbstractFreeMarkerTemplateService, which the random( ) function of the theme relies on. */
    private static final String NUMBER_FORMAT = "0.######";
    private static final String TEMPLATE_SEARCH = "skin/plugins/appointment/modules/solrsearchapp/search.html";

    private static final String TEXT = "O'Hara \"Jo\" </script><b>x</b>";
    private static final String UID = "appointment_1";
    private static final String GEOJSON = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[2.35,48.85]}}";

    /**
     * Renders a template of the module.
     *
     * @param strTemplate
     *            the template path
     * @param model
     *            the model
     * @return the HTML
     * @throws Exception
     *             if the rendering fails
     */
    private static String render( String strTemplate, Map<String, Object> model ) throws Exception
    {
        File coreTemplates = new File( CORE_TEMPLATES_ROOT );
        assertTrue( new File( coreTemplates, COMMONS_SITE ).isFile( ), "core templates not found under " + CORE_TEMPLATES_ROOT + ": run lutece:exploded first" );
        Configuration cfg = new Configuration( Configuration.VERSION_2_3_0 );
        cfg.setOutputFormat( HTMLOutputFormat.INSTANCE );
        cfg.setAutoEscapingPolicy( Configuration.ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY );
        cfg.setURLEscapingCharset( "UTF-8" );
        cfg.setNumberFormat( NUMBER_FORMAT );
        cfg.setTemplateLoader( new MultiTemplateLoader( new TemplateLoader [ ] {
                new FileTemplateLoader( new File( TEMPLATES_ROOT ) ), new FileTemplateLoader( coreTemplates )
        } ) );
        cfg.addAutoInclude( COMMONS_SITE );
        cfg.addAutoInclude( COMMONS_THEME );
        cfg.setSharedVariable( "htmlAttributes", new HtmlAttributesMethod( true ) );
        cfg.setSharedVariable( "i18n", (TemplateMethodModelEx) arguments -> String.valueOf( arguments.get( 0 ) ) );
        cfg.setSharedVariable( "dskey", (TemplateMethodModelEx) arguments -> "DS Value Missing" );
        cfg.setSharedVariable( "richText", new RichTextContentMethod( ) );
        StringWriter writer = new StringWriter( );
        cfg.getTemplate( strTemplate ).process( model, writer );

        return writer.toString( );
    }

    /**
     * The model AppointmentSearchApp builds for one form with one free slot.
     *
     * @param strSlotUrl
     *            the URL of the slot, read from Solr
     * @return the model
     */
    private static Map<String, Object> model( String strSlotUrl )
    {
        Map<String, Object> slot = new HashMap<>( );
        slot.put( "uid_form_string", UID );
        slot.put( "title", TEXT );
        slot.put( "appointmentslot_address_text", TEXT );
        slot.put( "date", new Date( 1_800_000_000_000L ) );
        slot.put( "url", strSlotUrl );
        slot.put( "url_form_string", strSlotUrl );
        slot.put( "url_base_string", "http://site/\"base" );
        slot.put( "appointmentslot_geojson", GEOJSON );
        Map<String, Object> documents = new HashMap<>( );
        documents.put( "list", List.of( slot ) );
        documents.put( "numFound", 3L );
        Map<String, Object> group = new HashMap<>( );
        group.put( "groupValue", UID );
        group.put( "result", documents );
        Map<String, Object> groupCommand = new HashMap<>( );
        groupCommand.put( "values", List.of( group ) );
        Map<String, Object> results = new HashMap<>( );
        results.put( "values", List.of( groupCommand ) );
        Map<String, Object> day = new HashMap<>( );
        day.put( "code", "1" );
        day.put( "name", "Monday" );
        day.put( "checked", true );

        Map<String, Object> model = new HashMap<>( );
        model.put( "results", results );
        model.put( "freePlacesCount", Map.of( UID, 1234 ) );
        model.put( "totalPlacesCount", Map.of( UID, 5678 ) );
        model.put( "items_days_of_week", List.of( day ) );
        model.put( "category", "a&b\"c" );
        model.put( "from_date", TEXT );
        model.put( "nb_consecutive_slots", "2" );
        model.put( "role", "none" );

        return model;
    }

    /**
     * The values of the Solr results and of the request are escaped once, as text.
     *
     * @throws Exception
     *             if the rendering fails
     */
    @Test
    public void testValuesAreWrittenAsText( ) throws Exception
    {
        String strHtml = render( TEMPLATE_SEARCH, model( "jsp/site/Portal.jsp?page=appointment&view=getViewAppointmentCalendar&id_form=1" ) );

        assertFalse( strHtml.contains( "<b>x</b>" ), strHtml );
        assertFalse( strHtml.contains( "&amp;lt;" ), strHtml );
        assertTrue( strHtml.contains( "O&#39;Hara &quot;Jo&quot; &lt;/script&gt;&lt;b&gt;x&lt;/b&gt;" ), strHtml );
        assertTrue( strHtml.contains( "value=\"a&amp;b&quot;c\"" ), strHtml );
        assertTrue( strHtml.contains( "&amp;view=search&amp;category=a%26b%22c&amp;nb_consecutive_slots=2" ), strHtml );
    }

    /**
     * The links of the slots keep their parameters escaped once.
     *
     * @throws Exception
     *             if the rendering fails
     */
    @Test
    public void testSlotLinks( ) throws Exception
    {
        String strHtml = render( TEMPLATE_SEARCH, model( "jsp/site/Portal.jsp?page=appointment&view=getViewAppointmentCalendar&id_form=1" ) );

        assertTrue( strHtml.contains( "href=\"jsp/site/Portal.jsp?page=appointment&amp;view=getViewAppointmentCalendar&amp;id_form=1&amp;nbPlacesToTake=2\"" ),
                strHtml );
        assertTrue( strHtml.contains( "id=\"link_" + UID + "_first_slot\"" ), strHtml );
        assertTrue( strHtml.contains( "id=\"link_" + UID + "_full_calendar\"" ), strHtml );
        assertFalse( strHtml.contains( "&amp;amp;" ), strHtml );
    }

    /**
     * A slot URL whose scheme is not a web one is not written in a link.
     *
     * @throws Exception
     *             if the rendering fails
     */
    @Test
    public void testSlotLinksKeepWebSchemesOnly( ) throws Exception
    {
        String strHtml = render( TEMPLATE_SEARCH, model( "javascript:void(0)" ) );

        assertFalse( strHtml.contains( "href=\"javascript:" ), strHtml );
        assertTrue( strHtml.contains( "href=\"#\"" ), strHtml );
    }

    /**
     * The points of the map are written in data attributes, as text: no value of the results is written in a script,
     * and the numbers are written without grouping.
     *
     * @throws Exception
     *             if the rendering fails
     */
    @Test
    public void testPointsAreWrittenInDataAttributes( ) throws Exception
    {
        String strHtml = render( TEMPLATE_SEARCH, model( "jsp/site/Portal.jsp" ) );

        assertTrue( strHtml.contains( "<div id=\"appointment-search-points\" hidden>" ), strHtml );
        assertTrue( strHtml.contains( "data-id=\"" + UID + "\"" ), strHtml );
        assertTrue( strHtml.contains( "data-url-base=\"http://site/&quot;base\"" ), strHtml );
        assertTrue( strHtml.contains( "data-geojson=\"{&quot;type&quot;:&quot;Feature&quot;," ), strHtml );
        assertTrue( strHtml.contains( "data-free=\"1234\" data-total=\"5678\"" ), strHtml );
        assertFalse( strHtml.contains( "lutece_appointment_solrsearchapp_points" ), strHtml );
    }
}
