/*
 * Copyright (c) 2002-2016, Mairie de Paris
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
package fr.paris.lutece.plugins.identitystore.business;

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.ReferenceList;

import java.util.Map;


/**
 * IIdentityAttributeDAO Interface
 */
public interface IIdentityAttributeDAO
{
    /**
     * Insert a new record in the table.
     * @param identityAttribute instance of the IdentityAttribute object to insert
     * @param plugin the Plugin
     */
    void insert( IdentityAttribute identityAttribute, Plugin plugin );

    /**
     * Update the record in the table
     * @param identityAttribute the reference of the IdentityAttribute
     * @param plugin the Plugin
     */
    void store( IdentityAttribute identityAttribute, Plugin plugin );

    /**
     * Delete a record from the table
     * @param nIdentityId The identity ID
     * @param nAttributeId The Attribute ID
     * @param plugin the Plugin
     */
    void delete( int nIdentityId, int nAttributeId, Plugin plugin );

    ///////////////////////////////////////////////////////////////////////////
    // Finders

    /**
     * Load the data from the table
     * @param nIdentityId The identity ID
     * @param nAttributeId The Attribute ID
     * @param plugin the Plugin
     * @return The instance of the identityAttribute
     */
    IdentityAttribute load( int nIdentityId, int nAttributeId, Plugin plugin );

    /**
     * Load the data of all the identityAttribute objects and returns them as a list
     * @param nIdentityId The Identity ID
     * @param plugin the Plugin
     * @return The map which contains the data of all the identityAttribute objects
     */
    Map<String, Attribute> selectAttributes( int nIdentityId, Plugin plugin );

    /**
     * Load the data of all attributes for a given identity ID which are alowed for
     * the client application provided
     * @param nIdentityId The identity ID
     * @param strApplicationCode code of client application
     * @param plugin the Plugin
     * @return the map which contains the data of all the identityAttribute objects
     */
    Map<String, Attribute> selectAttributes( int nIdentityId, String strApplicationCode, Plugin plugin );

    /**
     * Load the data of all the identityAttribute objects and returns them as a referenceList
     * @param plugin the Plugin
     * @return The referenceList which contains the data of all the identityAttribute objects
     */
    ReferenceList selectIdentityAttributesReferenceList( Plugin plugin );

    /**
     * Load the data of an attribute for a given identity ID and attribute key which is allowed for
     * the client application provided
     * @param nIdentityId The identity ID
     * @param strApplicationCode code of client application
     * @param strAttributeKey attribute key
     * @param plugin the Plugin
     * @return the list which contains the data of all the identityAttribute objects
     */
    Attribute selectAttribute( int nIdentityId, String strAttributeKey, String strApplicationCode, Plugin plugin );

    /**
     * Delete all attributes matching the provided identityId
     * @param nIdentityId The identity ID
     * @param plugin the Plugin
     */
    void deleteAllAttributes( int nIdentityId, Plugin plugin );
}
