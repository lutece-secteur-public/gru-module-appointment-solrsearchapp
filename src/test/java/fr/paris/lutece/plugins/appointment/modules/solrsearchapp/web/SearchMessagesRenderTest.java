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
package fr.paris.lutece.plugins.appointment.modules.solrsearchapp.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.portal.util.mvc.utils.MVCMessage;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;

/**
 * The search page lists the errors and infos of the model. Since lutece-core LUT-30580 the front office
 * MVCApplication.addError / addInfo store plain strings (Set of String), as the access-denied error of
 * AppointmentSearchApp.viewSearch; MVCMessage objects must still be rendered.
 */
public class SearchMessagesRenderTest
{
    private static final String TEMPLATE = "skin/plugins/appointment/modules/solrsearchapp/search.html";
    private static final String MACROS = "solrsearchapp_search_test_macros.ftl";
    private static final String MACROS_SOURCE = "<#macro cTpl rest...><#nested></#macro><#macro cForm rest...><#nested></#macro>"
            + "<#macro cFieldset rest...><#nested></#macro><#macro cSelect rest...><#nested></#macro><#macro cOption rest...></#macro>"
            + "<#macro cBtn rest...></#macro><#macro cCheckbox rest...></#macro><#macro cIcon rest...></#macro><#macro cInput rest...></#macro>"
            + "<#macro cInputDate rest...></#macro><#macro cLabel rest...></#macro><#macro cTitle rest...><#nested></#macro>"
            + "<#macro cAlert title='' rest...><div class=\"alert\">${title}</div></#macro>";

    private static String render( Object errors, Object infos ) throws Exception
    {
        StringTemplateLoader stubs = new StringTemplateLoader( );
        stubs.putTemplate( MACROS, MACROS_SOURCE );
        Configuration cfg = new Configuration( Configuration.VERSION_2_3_32 );
        cfg.setOutputFormat( HTMLOutputFormat.INSTANCE );
        cfg.setAutoEscapingPolicy( Configuration.ENABLE_IF_DEFAULT_AUTO_ESCAPING_POLICY );
        cfg.setTemplateLoader( new MultiTemplateLoader( new TemplateLoader [ ] {
                stubs, new FileTemplateLoader( new File( "webapp/WEB-INF/templates" ) )
        } ) );
        cfg.addAutoInclude( MACROS );
        Map<String, Object> model = new HashMap<>( );
        model.put( "errors", errors );
        model.put( "infos", infos );
        StringWriter writer = new StringWriter( );
        cfg.getTemplate( TEMPLATE ).process( model, writer );
        return writer.toString( );
    }

    /**
     * The access-denied error added by viewSearch through the core (plain string) is shown.
     *
     * @throws Exception
     *             if the rendering fails
     */
    @Test
    public void testErrorAddedByTheCoreAsText( ) throws Exception
    {
        Set<String> errors = new LinkedHashSet<>( );
        errors.add( "Accès refusé" );
        Set<String> infos = new LinkedHashSet<>( );
        infos.add( "Aucun créneau n'est disponible" );
        String strHtml = render( errors, infos );
        assertTrue( strHtml.contains( "<div class=\"alert\">Accès refusé</div>" ), strHtml );
        assertTrue( strHtml.contains( "<div class=\"alert\">Aucun créneau n&#39;est disponible</div>" ), strHtml );
    }

    /**
     * Messages given as MVCMessage objects are still shown.
     *
     * @throws Exception
     *             if the rendering fails
     */
    @Test
    public void testMessagesGivenAsMessageObjects( ) throws Exception
    {
        String strHtml = render( List.of( new MVCMessage( "Denied" ) ), List.of( new MVCMessage( "Hello" ) ) );
        assertTrue( strHtml.contains( "<div class=\"alert\">Denied</div>" ), strHtml );
        assertTrue( strHtml.contains( "<div class=\"alert\">Hello</div>" ), strHtml );
    }
}
