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
package fr.paris.lutece.plugins.identitystore.web.rs;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.core.header.ContentDisposition;
import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;

import fr.paris.lutece.plugins.identitystore.business.AttributeCertificate;
import fr.paris.lutece.plugins.identitystore.business.AttributeKey;
import fr.paris.lutece.plugins.identitystore.business.AttributeKeyHome;
import fr.paris.lutece.plugins.identitystore.business.Identity;
import fr.paris.lutece.plugins.identitystore.business.IdentityAttribute;
import fr.paris.lutece.plugins.identitystore.business.IdentityHome;
import fr.paris.lutece.plugins.identitystore.business.KeyType;
import fr.paris.lutece.plugins.identitystore.service.ChangeAuthor;
import fr.paris.lutece.plugins.identitystore.service.IdentityStoreService;
import fr.paris.lutece.plugins.identitystore.service.external.IdentityInfoExternalService;
import fr.paris.lutece.plugins.identitystore.web.exception.IdentityNotFoundException;
import fr.paris.lutece.plugins.identitystore.web.exception.IdentityStoreException;
import fr.paris.lutece.plugins.identitystore.web.rs.dto.AttributeDto;
import fr.paris.lutece.plugins.identitystore.web.rs.dto.AuthorDto;
import fr.paris.lutece.plugins.identitystore.web.rs.dto.IdentityChangeDto;
import fr.paris.lutece.plugins.identitystore.web.rs.dto.IdentityDto;
import fr.paris.lutece.plugins.identitystore.web.rs.dto.ResponseDto;
import fr.paris.lutece.plugins.identitystore.web.rs.service.Constants;
import fr.paris.lutece.plugins.rest.service.RestConstants;
import fr.paris.lutece.portal.business.file.File;
import fr.paris.lutece.portal.business.physicalfile.PhysicalFile;
import fr.paris.lutece.portal.business.physicalfile.PhysicalFileHome;
import fr.paris.lutece.portal.service.util.AppException;
import fr.paris.lutece.portal.service.util.AppLogService;

import net.sf.json.util.JSONUtils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;

import java.nio.charset.StandardCharsets;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;


/**
 * REST service for channel resource
 *
 */
@Path( RestConstants.BASE_PATH + Constants.PLUGIN_PATH + Constants.IDENTITY_PATH )
public final class IdentityStoreRestService
{
    private static final String ERROR_NO_IDENTITY_FOUND = "No identity found";
    private static final String ERROR_NO_IDENTITY_TO_UPDATE = "no identity to update";
    private static final String ERROR_NO_IDENTITY_PROVIDED = "Neither the guid, nor the cid, nor the identity attributes are provided !!!";
    private static final String ERROR_DURING_TREATMENT = "An error occured during the treatment.";
    private ObjectMapper _objectMapper;

    /**
     * private constructor
     */
    private IdentityStoreRestService(  )
    {
        _objectMapper = new ObjectMapper(  );
        _objectMapper.enable( SerializationFeature.INDENT_OUTPUT );
        _objectMapper.enable( SerializationFeature.WRAP_ROOT_VALUE );
        _objectMapper.enable( DeserializationFeature.UNWRAP_ROOT_VALUE );
    }

    /**
     * Gives Identity from a connectionId or customerID either connectionId or
     * customerId must be provided if connectionId AND customerId are provided,
     * they must be consistent otherwise an AppException is thrown
     *
     * @param strConnectionId
     *          connection ID
     * @param nCustomerId
     *          customerID
     * @param strClientAppCode
     *          client code
     * @param strAuthenticationKey
     *          client application hash
     * @return the identity
     */
    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Response getIdentity( @QueryParam( Constants.PARAM_ID_CONNECTION )
    String strConnectionId, @QueryParam( Constants.PARAM_ID_CUSTOMER )
    int nCustomerId, @QueryParam( Constants.PARAM_CLIENT_CODE )
    String strClientAppCode,
        @HeaderParam( value = Constants.PARAM_CLIENT_CONTROL_KEY )
    @DefaultValue( StringUtils.EMPTY )
    String strAuthenticationKey )
    {
        String strJsonResponse;

        try
        {
            IdentityRequestValidator.instance(  )
                                    .checkFetchParams( strConnectionId, nCustomerId, strClientAppCode,
                strAuthenticationKey );

            Identity identity = getIdentity( strConnectionId, nCustomerId, strClientAppCode );

            if ( identity == null )
            {
                ResponseDto response = new ResponseDto(  );
                response.setMessage( "No identity found for " + Constants.PARAM_ID_CONNECTION + "(" + strConnectionId +
                    ")" + " AND " + Constants.PARAM_ID_CUSTOMER + "(" + nCustomerId + ")" );
                response.setStatus( String.valueOf( Status.NOT_FOUND ) );

                String strResponse;
                strResponse = _objectMapper.writeValueAsString( response );

                return Response.status( Status.NOT_FOUND ).type( MediaType.APPLICATION_JSON ).entity( strResponse )
                               .build(  );
            }

            strJsonResponse = _objectMapper.writeValueAsString( DtoConverter.convertToDto( identity, strClientAppCode ) );

            return Response.ok( strJsonResponse ).build(  );
        }
        catch ( Exception exception )
        {
            return getErrorResponse( exception );
        }
    }

    /**
     * update identity method
     *
     * @param formParams
     *          form params, bodypars used for files upload
     * @param strAuthenticationKey
     *          client application hash
     * @return http 200 if update is ok with ResponseDto
     */
    @POST
    @Path( Constants.UPDATE_IDENTITY_PATH )
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    public Response updateIdentity( FormDataMultiPart formParams,
        @HeaderParam( Constants.PARAM_CLIENT_CONTROL_KEY )
    String strAuthenticationKey )
    {
        try
        {
            IdentityChangeDto identityChangeDto = fetchIdentityChange( formParams );
            IdentityRequestValidator.instance(  ).checkUpdateParams( identityChangeDto, strAuthenticationKey );

            Identity identity = getIdentity( identityChangeDto.getIdentity(  ).getConnectionId(  ),
                    identityChangeDto.getIdentity(  ).getCustomerId(  ),
                    identityChangeDto.getAuthor(  ).getApplicationCode(  ) );

            if ( identity == null )
            {
                ResponseDto response = new ResponseDto(  );
                response.setMessage( "no identity found for " + Constants.PARAM_ID_CONNECTION + "(" +
                    identityChangeDto.getIdentity(  ).getConnectionId(  ) + ")" + " AND " +
                    Constants.PARAM_ID_CUSTOMER + "(" + identityChangeDto.getIdentity(  ).getCustomerId(  ) + ")" );
                response.setStatus( String.valueOf( Status.NOT_FOUND ) );

                String strResponse;
                strResponse = _objectMapper.writeValueAsString( response );

                return Response.status( Status.NOT_FOUND ).type( MediaType.APPLICATION_JSON ).entity( strResponse )
                               .build(  );
            }

            Map<String, File> mapAttachedFiles = fetchAttachedFiles( formParams );

            IdentityRequestValidator.instance(  )
                                    .checkAttributes( identityChangeDto.getIdentity(  ),
                identityChangeDto.getAuthor(  ).getApplicationCode(  ), mapAttachedFiles );

            ChangeAuthor author = DtoConverter.getAuthor( identityChangeDto.getAuthor(  ) );
            updateAttributes( identity, identityChangeDto.getIdentity(  ), author, mapAttachedFiles );

            String strResponse = _objectMapper.writeValueAsString( DtoConverter.convertToDto( identity,
                        identityChangeDto.getAuthor(  ).getApplicationCode(  ) ) );

            return Response.ok( strResponse, MediaType.APPLICATION_JSON ).build(  );
        }
        catch ( Exception exception )
        {
            return getErrorResponse( exception );
        }
    }

    /**
     * Creates an identity <b>only if the identity does not already exist</b>.<br/>
     * The identity is created from the provided attributes.
     * <br/><br/>
     * The order to test if the identity exists:
     * <ul><li>by using the provided customer id if present</li>
     * <li>by using the provided connection id if present</li></ul>
     *
     * @param formParams
     *          form params, bodypars used for files upload
     * @param strAuthenticationKey
     *          client application hash
     * @return http 200 if update is ok with ResponseDto
     */
    @POST
    @Path( Constants.CREATE_IDENTITY_PATH )
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    public Response createIdentity( FormDataMultiPart formParams,
        @HeaderParam( Constants.PARAM_CLIENT_CONTROL_KEY )
    String strAuthenticationKey )
    {
        try
        {
            IdentityChangeDto identityChangeDto = fetchIdentityChange( formParams );
            int nCustomerId = identityChangeDto.getIdentity(  ).getCustomerId(  );
            String strConnectionId = identityChangeDto.getIdentity(  ).getConnectionId(  );
            String strClientAppCode = identityChangeDto.getAuthor(  ).getApplicationCode(  );
            Identity identity = null;

            if ( nCustomerId != Constants.NO_CUSTOMER_ID )
            {
                IdentityRequestValidator.instance(  )
                                        .checkFetchParams( null, nCustomerId, strClientAppCode, strAuthenticationKey );

                identity = getIdentity( null, nCustomerId, strClientAppCode );

                if ( identity == null )
                {
                    ResponseDto response = new ResponseDto(  );
                    response.setMessage( "No identity found for " + Constants.PARAM_ID_CUSTOMER + "(" + nCustomerId +
                        ")" );
                    response.setStatus( String.valueOf( Status.NOT_FOUND ) );

                    String strResponse;
                    strResponse = _objectMapper.writeValueAsString( response );

                    return Response.status( Status.NOT_FOUND ).type( MediaType.APPLICATION_JSON ).entity( strResponse )
                                   .build(  );
                }
            }
            else
            {
                if ( !StringUtils.isEmpty( strConnectionId ) )
                {
                    IdentityRequestValidator.instance(  )
                                            .checkFetchParams( strConnectionId, Constants.NO_CUSTOMER_ID,
                        strClientAppCode, strAuthenticationKey );

                    identity = getIdentity( strConnectionId, Constants.NO_CUSTOMER_ID, strClientAppCode );

                    if ( identity == null )
                    {
                        try
                        {
                            identity = initIdentity( strConnectionId );
                        }
                        catch ( IdentityNotFoundException e )
                        {
                            return getErrorResponse( e, Status.NOT_FOUND );
                        }
                    }
                }
                else
                {
                    Map<String, AttributeDto> mapAttributes = identityChangeDto.getIdentity(  ).getAttributes(  );

                    if ( ( mapAttributes != null ) && !mapAttributes.isEmpty(  ) )
                    {
                        Map<String, File> mapAttachedFiles = fetchAttachedFiles( formParams );

                        IdentityRequestValidator.instance(  ).checkCreateParams( identityChangeDto, strAuthenticationKey );
                        IdentityRequestValidator.instance(  )
                                                .checkAttributes( identityChangeDto.getIdentity(  ),
                            identityChangeDto.getAuthor(  ).getApplicationCode(  ), mapAttachedFiles );

                        ChangeAuthor author = DtoConverter.getAuthor( identityChangeDto.getAuthor(  ) );

                        identity = new Identity(  );
                        IdentityHome.create( identity );

                        updateAttributes( identity, identityChangeDto.getIdentity(  ), author, mapAttachedFiles );
                    }
                    else
                    {
                        throw new IdentityStoreException( ERROR_NO_IDENTITY_PROVIDED );
                    }
                }
            }

            String strResponse = _objectMapper.writeValueAsString( DtoConverter.convertToDto( identity, strClientAppCode ) );

            return Response.ok( strResponse, MediaType.APPLICATION_JSON ).build(  );
        }
        catch ( Exception exception )
        {
            return getErrorResponse( exception );
        }
    }

    /**
     * Initializes an identity from an external source by using the specified connection id
     * @param strConnectionId the connection id used to initialize the identity
     * @return the initialized identity
     * @throws IdentityNotFoundException if no identity can be retrieve from external source
     */
    private static Identity initIdentity( String strConnectionId )
        throws IdentityNotFoundException
    {
        IdentityChangeDto identityChangeDtoInitialized = new IdentityChangeDto(  );

        identityChangeDtoInitialized = IdentityInfoExternalService.instance(  ).getIdentityInfo( strConnectionId );

        IdentityDto identityDto = identityChangeDtoInitialized.getIdentity(  );
        AuthorDto authorDto = identityChangeDtoInitialized.getAuthor(  );
        Map<String, File> mapAttachedFiles = new HashMap<String, File>(  );

        IdentityRequestValidator.instance(  ).checkCreateParams( identityChangeDtoInitialized, StringUtils.EMPTY );
        IdentityRequestValidator.instance(  )
                                .checkAttributes( identityDto, authorDto.getApplicationCode(  ), mapAttachedFiles );

        ChangeAuthor author = DtoConverter.getAuthor( authorDto );

        Identity identity = new Identity(  );
        identity.setConnectionId( strConnectionId );
        IdentityHome.create( identity );

        updateAttributes( identity, identityDto, author, mapAttachedFiles );

        if ( AppLogService.isDebugEnabled(  ) )
        {
            AppLogService.debug( "New identity created with provided guid (" + strConnectionId +
                ". Associated customer id is : " + identity.getCustomerId(  ) + ". Associated attributes are : " +
                identity.getAttributes(  ) );
        }

        return identity;
    }

    /**
     * returns requested file matching attributeKey / connectionId if application
     * is authorized
     *
     * @param strConnectionId
     *          connectionId (must not be empty)
     * @param strClientAppCode
     *          client application code (must not be empty)
     * @param strAttributeKey
     *          attribute key containing file (must not be empty)
     * @param strAuthenticationKey
     *          client application hash
     * @return http 200 Response containing requested file, http 400 otherwise
     */
    @GET
    @Path( Constants.DOWNLOAD_FILE_PATH )
    public Response downloadFileAttribute( @QueryParam( Constants.PARAM_ID_CONNECTION )
    String strConnectionId, @QueryParam( Constants.PARAM_CLIENT_CODE )
    String strClientAppCode, @QueryParam( Constants.PARAM_ATTRIBUTE_KEY )
    String strAttributeKey, @HeaderParam( Constants.PARAM_CLIENT_CONTROL_KEY )
    String strAuthenticationKey )
    {
        File file = null;

        try
        {
            IdentityRequestValidator.instance(  )
                                    .checkDownloadFileAttributeParams( strConnectionId, strClientAppCode,
                strAttributeKey, strAuthenticationKey );
            file = getFileAttribute( strConnectionId, strClientAppCode, strAttributeKey );
        }
        catch ( Exception exception )
        {
            return getErrorResponse( exception );
        }

        ResponseBuilder response = Response.ok( (Object) PhysicalFileHome.findByPrimaryKey( 
                    file.getPhysicalFile(  ).getIdPhysicalFile(  ) ).getValue(  ) );
        response.header( "Content-Disposition", "attachment; filename=" + file.getTitle(  ) );
        response.header( "Content-Type", file.getMimeType(  ) );

        return response.build(  );
    }

    /**
     * get identity from connectionId or customerId
     * If no identity is found then a new one be created with attributes based on external provider data
     * @param strConnectionId connection id
     * @param nCustomerId customer id
     * @param strClientAppCode client application code
     * @return identity , null if no identity found
     * @throws AppException if provided connectionId and customerId are not consitent
     */
    private Identity getIdentity( String strConnectionId, int nCustomerId, String strClientAppCode )
    {
        Identity identity = null;

        if ( StringUtils.isNotBlank( strConnectionId ) )
        {
            identity = IdentityStoreService.getIdentityByConnectionId( strConnectionId, strClientAppCode );

            if ( ( identity != null ) && ( nCustomerId != Constants.NO_CUSTOMER_ID ) &&
                    ( nCustomerId != identity.getCustomerId(  ) ) )
            {
                throw new AppException( "inconsistent " + Constants.PARAM_ID_CONNECTION + "(" + strConnectionId + ")" +
                    " AND " + Constants.PARAM_ID_CUSTOMER + "(" + nCustomerId + ")" + " params provided " );
            }
        }
        else
        {
            identity = IdentityStoreService.getIdentityByCustomerId( nCustomerId, strClientAppCode );
        }

        if ( identity == null )
        {
            try
            {
                identity = initIdentity( strConnectionId );
            }
            catch ( IdentityNotFoundException e )
            {
                //Identity not found in External provider : creation is aborted
                AppLogService.info( "Could not create an identity from external source" );
            }
        }

        return identity;
    }

    /**
     * Fetches the object {@link IdentityChangeDto} from multi-part data
     * @param formParams the mutli-part data
     * @return the IdentityChangeDto
     * @throws IOException if an error occurs during the treatment
     */
    private IdentityChangeDto fetchIdentityChange( FormDataMultiPart formParams )
        throws IOException
    {
        IdentityChangeDto identityChangeDto = null;
        String strBody = StringUtils.EMPTY;

        for ( BodyPart part : formParams.getBodyParts(  ) )
        {
            InputStream inputStream = part.getEntityAs( InputStream.class );
            ContentDisposition contentDispo = part.getContentDisposition(  );

            if ( StringUtils.isBlank( contentDispo.getFileName(  ) ) &&
                    part.getMediaType(  ).isCompatible( MediaType.TEXT_PLAIN_TYPE ) &&
                    Constants.PARAM_IDENTITY_CHANGE.equals( contentDispo.getParameters(  ).get( Constants.PARAMETER_NAME ) ) )
            {
                // content-body of request
                strBody = IOUtils.toString( inputStream, StandardCharsets.UTF_8.toString(  ) );

                if ( JSONUtils.mayBeJSON( strBody ) )
                {
                    identityChangeDto = getIdentityChangeFromJson( strBody );
                }
                else
                {
                    throw new AppException( "Error parsing json request " + strBody );
                }
            }
        }

        return identityChangeDto;
    }

    /**
     * Fetches the attached files from the specified multi-part data
     * @param formParams the multi-part data
     * @return the attached files
     * @throws IOException  if an error occurs during the treatment
     */
    private static Map<String, File> fetchAttachedFiles( FormDataMultiPart formParams )
        throws IOException
    {
        Map<String, File> mapAttachedFiles = new HashMap<String, File>(  );

        for ( BodyPart part : formParams.getBodyParts(  ) )
        {
            InputStream inputStream = part.getEntityAs( InputStream.class );
            ContentDisposition contentDispo = part.getContentDisposition(  );

            if ( StringUtils.isNotBlank( contentDispo.getFileName(  ) ) )
            {
                // attachment file
                PhysicalFile physicalFile = new PhysicalFile(  );
                physicalFile.setValue( IOUtils.toByteArray( inputStream ) );

                fr.paris.lutece.portal.business.file.File file = new fr.paris.lutece.portal.business.file.File(  );
                file.setPhysicalFile( physicalFile );
                file.setMimeType( part.getMediaType(  ).getType(  ) + "/" + part.getMediaType(  ).getSubtype(  ) );
                file.setSize( physicalFile.getValue(  ).length );
                file.setTitle( contentDispo.getFileName(  ) );
                mapAttachedFiles.put( contentDispo.getFileName(  ), file );
            }
        }

        return mapAttachedFiles;
    }

    /**
     * build error response from exception
     *
     * @param e
     *          exception
     * @return ResponseDto from exception
     */
    private Response getErrorResponse( Exception e )
    {
        return getErrorResponse( e, Status.BAD_REQUEST );
    }

    /**
     * build error response from exception and status
     *
     * @param e
     *          exception
     * @param status the status to send
     * @return ResponseDto from exception
     */
    private Response getErrorResponse( Exception e, Status status )
    {
        AppLogService.error( e );

        String strMessage = null;

        // For security purpose, send a generic message
        switch ( status )
        {
            case NOT_FOUND:
                strMessage = ERROR_NO_IDENTITY_FOUND;

                break;

            default:
                strMessage = ERROR_DURING_TREATMENT;
        }

        ResponseDto response = new ResponseDto(  );
        response.setStatus( String.valueOf( status ) );
        response.setMessage( strMessage );

        String strResponse;

        try
        {
            strResponse = _objectMapper.writeValueAsString( response );

            return Response.status( status ).type( MediaType.APPLICATION_JSON ).entity( strResponse ).build(  );
        }
        catch ( JsonProcessingException jpe )
        {
            return Response.status( status ).type( MediaType.TEXT_PLAIN ).entity( strMessage ).build(  );
        }
    }

    /**
     * @param strConnectionId
     *          connectionId (must not be empty)
     * @param strClientCode
     *          client application code (must not be empty)
     * @param strAttributeKey
     *          attribute key containing file (must not be empty)
     * @return File matching connectionId/attribute key if readable for client
     *         application code
     * @throws AppException
     *           thrown if input parameters are invalid or no file is found
     */
    private File getFileAttribute( String strConnectionId, String strClientCode, String strAttributeKey )
        throws AppException
    {
        IdentityAttribute attribute = IdentityStoreService.getAttribute( strConnectionId, strAttributeKey, strClientCode );

        if ( ( attribute == null ) || ( attribute.getFile(  ) == null ) )
        {
            throw new AppException( Constants.PARAM_ATTRIBUTE_KEY + " " + strAttributeKey +
                " attribute not found for " + Constants.PARAM_ID_CONNECTION + "=" + strConnectionId + "  " +
                Constants.PARAM_CLIENT_CODE + "=" + strClientCode );
        }

        return attribute.getFile(  );
    }

    /**
     * returns IdentityChangeDto from jsonContent
     *
     * @param strJsonContent
     *          json content of request
     * @return IdentityChangeDto parsed identityDto
     * @throws AppException
     *           if request is not correct
     * @throws IOException
     *           if error occurs while parsing json
     * @throws JsonMappingException
     *           if error occurs while parsing json
     * @throws JsonParseException
     *           if error occurs while parsing json
     *
     */
    private IdentityChangeDto getIdentityChangeFromJson( String strJsonContent )
        throws AppException, JsonParseException, JsonMappingException, IOException
    {
        IdentityChangeDto identityChangeDto = _objectMapper.readValue( strJsonContent, IdentityChangeDto.class );

        if ( ( identityChangeDto == null ) || ( identityChangeDto.getIdentity(  ) == null ) )
        {
            throw new AppException( ERROR_NO_IDENTITY_TO_UPDATE );
        }

        return identityChangeDto;
    }

    /**
     * check if new identity attributes have errors and returns them
     *
     * @param identity the identity
     * @param identityDto
     *          new identity to update
     *          connectionId of identity which will be updated
     * @param author
     *          author responsible for modification
     * @param mapAttachedFiles
     *          map containing File matching key attribute name
     *
     */
    private static void updateAttributes( Identity identity, IdentityDto identityDto, ChangeAuthor author,
        Map<String, File> mapAttachedFiles )
    {
        StringBuilder sb = new StringBuilder( "Fields successfully updated : " );

        for ( AttributeDto attributeDto : identityDto.getAttributes(  ).values(  ) )
        {
            File file = null;
            AttributeKey attributeKey = AttributeKeyHome.findByKey( attributeDto.getKey(  ) );

            if ( attributeKey.getKeyType(  ).equals( KeyType.FILE ) &&
                    StringUtils.isNotBlank( attributeDto.getValue(  ) ) )
            {
                file = mapAttachedFiles.get( attributeDto.getValue(  ) );
            }

            AttributeCertificate certificate = DtoConverter.getCertificate( attributeDto.getCertificate(  ) );
            IdentityStoreService.setAttribute( identity, attributeDto.getKey(  ), attributeDto.getValue(  ), file,
                author, certificate );
            sb.append( attributeDto.getKey(  ) + "," );
        }

        AppLogService.debug( sb.toString(  ) );
    }
}
