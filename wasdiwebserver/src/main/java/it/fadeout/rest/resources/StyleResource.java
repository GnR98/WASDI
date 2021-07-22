package it.fadeout.rest.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.fadeout.Wasdi;
import it.fadeout.rest.resources.largeFileDownload.FileStreamingOutput;
import org.glassfish.jersey.media.multipart.FormDataParam;
import wasdi.shared.business.User;
import wasdi.shared.utils.Utils;

import javax.servlet.ServletConfig;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Path("style")
public class StyleResource {

    @Context
    ServletConfig m_oServletConfig;

    /**
     * Retrieves the available styles from the *.sld files
     * available on the current instance of WASDI main node.
     * Use the configuration on the server to get the location of the directory
     *
     * @param sSessionId The session of the current user
     * @return a List of the available style on the current server
     */
    @GET
    @Path("getlist")
    public List<String> getStyleList(@HeaderParam("x-session-token") String sSessionId) {
        // Check session
        User oUser = Wasdi.getUserFromSession(sSessionId);
        try {
            // Domain Check
            if (oUser == null) {
                Utils.debugLog("StyleResource.getStyleList: invalid session");
                return null;
            }
            if (Utils.isNullOrEmpty(oUser.getUserId())) {
                String sMessage = "user not found";
                Utils.debugLog("StyleResource.getStyleList: " + sMessage);
            }
            String sNodeCode = m_oServletConfig.getInitParameter("NODECODE");
            String sDownloadRootPath = m_oServletConfig.getInitParameter("DownloadRootPath");
            String sResponse;
            // I am the main node:
            if (sNodeCode.equals("wasdi")) {
                Utils.debugLog("StyleResource.getStyleList: Locally search for styles");
                File oStyleDirectory = new File(sDownloadRootPath + "/styles");
                return Arrays.stream(oStyleDirectory.listFiles())
                        .map(
                                file -> file.getName().replace(".sld", "")
                        ).collect(Collectors.toList());
            }
            // I'm not the main node
            else {
                Utils.debugLog("StyleResource.getStyleList: Forwarding request to the main node");
                // make this call on
                String sUrl = "http://www.wasdi.net/wasdiwebserver/rest/product/stylesfromfile";
                HashMap<String, String> asHeaders = new HashMap<>();
                asHeaders.put("x-session-token", sSessionId);
                sResponse = Wasdi.httpGet(sUrl, asHeaders);
                return Arrays.asList(new ObjectMapper().readValue(sResponse, String[].class));

            }

        } catch (Exception e) {
        }

        return null;
    }

    /**
     * Download a SLD style
     *
     * @param sSessionId      Session Token
     * @param sTokenSessionId Alternative session token to allow client download
     * @param sStyle          Name of the style to download
     * @return SLD file
     */
    @GET
    @Path("download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadStyleByName(@HeaderParam("x-session-token") String sSessionId,
                                        @QueryParam("token") String sTokenSessionId,
                                        @QueryParam("style") String sStyle) {

        Utils.debugLog("StyleResource.downloadStyleByName( WorkflowId: " + sStyle + " )");

        try {

            if (Utils.isNullOrEmpty(sSessionId) == false) {
                sTokenSessionId = sSessionId;
            }

            User oUser = Wasdi.getUserFromSession(sTokenSessionId);

            if (oUser == null) {
                Utils.debugLog("StyleResource.downloadStyleByName( Session: " + sSessionId + ", Style: " + sStyle + " ): invalid session");
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            // Take path
            String sDownloadRootPath = Wasdi.getDownloadPath(m_oServletConfig);
            String sStyleSldPath = sDownloadRootPath + "styles/" + sStyle + ".sld";

            File oFile = new File(sStyleSldPath);

            Response.ResponseBuilder oResponseBuilder = null;

            if (oFile.exists() == false) {
                Utils.debugLog("StyleResource.downloadStyleByName: file does not exists " + oFile.getPath());
                oResponseBuilder = Response.serverError();
            } else {

                Utils.debugLog("StyleResource.downloadStyleByName: returning file " + oFile.getPath());

                FileStreamingOutput oStream;
                oStream = new FileStreamingOutput(oFile);

                oResponseBuilder = Response.ok(oStream);
                oResponseBuilder.header("Content-Disposition", "attachment; filename=" + oFile.getName());
                oResponseBuilder.header("Content-Length", Long.toString(oFile.length()));
            }

            return oResponseBuilder.build();

        } catch (Exception oEx) {
            Utils.debugLog("StyleResource.downloadStyleByName: " + oEx);
        }

        return null;
    }


    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadStyle(@HeaderParam("x-session-token") String sSessionId,
                                @FormDataParam("file") InputStream fileInputStream,
                                @QueryParam("name") String sName) {

        Utils.debugLog("StyleResource.uploadStyle( Name: " + sName);

        try {
            // Check authorization
            if (Utils.isNullOrEmpty(sSessionId)) {
                Utils.debugLog("StyleResource.uploadStyle: invalid session");
                return Response.status(401).build();
            }
            User oUser = Wasdi.getUserFromSession(sSessionId);

            if (oUser == null) return Response.status(401).build();
            if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(401).build();

            // Get Download Path
            String sDownloadRootPath = Wasdi.getDownloadPath(m_oServletConfig);

            File oStylesPath = new File(sDownloadRootPath + "styles/");

            if (!oStylesPath.exists()) {
                oStylesPath.mkdirs();
            }

            // Generate Workflow Id and file
            File oStyleXmlFile = new File(sDownloadRootPath + "workflows/" + sName + ".sld");

            Utils.debugLog("StyleResource.uploadStyle: style file Path: " + oStyleXmlFile.getPath());

            // save uploaded file
            int iRead = 0;
            byte[] ayBytes = new byte[1024];

            try (OutputStream oOutStream = new FileOutputStream(oStyleXmlFile)) {
                while ((iRead = fileInputStream.read(ayBytes)) != -1) {
                    oOutStream.write(ayBytes, 0, iRead);
                }
                oOutStream.flush();
            }

        } catch (Exception oEx) {
            Utils.debugLog("StyleResource.uploadStyle: " + oEx);
            return Response.serverError().build();
        }

        return Response.ok().build();
    }

}
