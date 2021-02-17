package it.fadeout.rest.resources;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FilenameUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.google.common.io.Files;

import it.fadeout.Wasdi;
import it.fadeout.business.ImageResourceUtils;
import wasdi.shared.business.AppCategory;
import wasdi.shared.business.Processor;
import wasdi.shared.business.ProcessorSharing;
import wasdi.shared.business.Review;
import wasdi.shared.business.User;
import wasdi.shared.data.AppsCategoriesRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.ProcessorSharingRepository;
import wasdi.shared.data.ReviewRepository;
import wasdi.shared.utils.ImageFile;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.AppCategoryViewModel;
import wasdi.shared.viewmodels.ListReviewsViewModel;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.PublisherFilterViewModel;
import wasdi.shared.viewmodels.ReviewViewModel;

@Path("processormedia")
public class ProcessorsMediaResource {
	
	@Context
	ServletConfig m_oServletConfig;
	
	public static int MAX_IMAGE_MB_SIZE = 2;
	public static String[] IMAGE_PROCESSORS_EXTENSIONS = {"jpg", "png", "svg"};
	public static String DEFAULT_LOGO_PROCESSOR_NAME = "logo";
	public static Integer LOGO_SIZE = 540;
	public static String[] IMAGE_NAMES = { "1", "2", "3", "4", "5", "6" };
	
	@POST
	@Path("/logo/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadProcessorLogo(@FormDataParam("image") InputStream oInputFileStream, @FormDataParam("image") FormDataContentDisposition oFileMetaData,
										@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId ) {
		
		
		Utils.debugLog("ProcessorsMediaResource.uploadProcessorLogo( ProcId: " + sProcessorId + ")");
		
		if (Utils.isNullOrEmpty(sSessionId)) return Response.status(Status.UNAUTHORIZED).build();
		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser==null) return Response.status(Status.UNAUTHORIZED).build();
		if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();
		
		Utils.debugLog("ProcessorsResource.uploadProcessorLogo: get Processor " + sProcessorId);	
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		
		if (oProcessor == null) {
			Utils.debugLog("ProcessorsResource.uploadProcessorLogo: unable to find processor " + sProcessorId);
			return Response.serverError().build();
		}
		
		if (!oProcessor.getUserId().equals(oUser.getUserId())) {
			
			ProcessorSharingRepository oProcessorSharingRepository = new ProcessorSharingRepository();
			
			ProcessorSharing oSharing = oProcessorSharingRepository.getProcessorSharingByUserIdProcessorId(oUser.getUserId(), sProcessorId);
			
			if (oSharing == null) {
				Utils.debugLog("ProcessorsResource.uploadProcessorLogo: processor not of user " + oUser.getUserId());
				return Response.status(Status.UNAUTHORIZED).build();					
			}
			else {
				Utils.debugLog("ProcessorsResource.uploadProcessorLogo: processor of user " + oProcessor.getUserId() + " is shared with " + oUser.getUserId());
			}
			
		}

		String sExt;
		String sFileName;
		
		//get filename and extension 
		if(oFileMetaData != null && Utils.isNullOrEmpty(oFileMetaData.getFileName()) == false){
			
			sFileName = oFileMetaData.getFileName();
			sExt = FilenameUtils.getExtension(sFileName);
			
			Utils.debugLog("ProcessorsResource.uploadProcessorLogo: FileName " + sFileName + " Extension: " + sExt);
		} 
		else {
			Utils.debugLog("ProcessorsResource.uploadProcessorLogo: File metadata not available");
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		// Check if this is an accepted file extension
		if(ImageResourceUtils.isValidExtension(sExt,IMAGE_PROCESSORS_EXTENSIONS) == false ){
			Utils.debugLog("ProcessorsResource.uploadProcessorLogo: extension invalid");
			return Response.status(Status.BAD_REQUEST).build();
		}

		// Take path
		String sPath = ImageResourceUtils.getProcessorImagesBasePath(oProcessor.getName(), false);
		
		Utils.debugLog("ProcessorsResource.uploadProcessorLogo: sPath: " + sPath);
		
		String sExtensionOfSavedLogo = ImageResourceUtils.getExtensionOfImageInFolder(sPath+DEFAULT_LOGO_PROCESSOR_NAME, IMAGE_PROCESSORS_EXTENSIONS);
		
		//if there is a saved logo with a different extension remove it 
		if(sExtensionOfSavedLogo.isEmpty() == false){
			Utils.debugLog("ProcessorsResource.uploadProcessorLogo: delete old logo");
		    File oOldLogo = new File(sPath + DEFAULT_LOGO_PROCESSOR_NAME + "." + sExtensionOfSavedLogo);
		    if (!oOldLogo.delete()) {
		    	Utils.debugLog("ProcessorsResource.uploadProcessorLogo: can't delete old logo");
		   	}
		}
			
		ImageResourceUtils.createDirectory(sPath);
	    
	    String sOutputFilePath = sPath + DEFAULT_LOGO_PROCESSOR_NAME + "." + sExt.toLowerCase();
	    
	    Utils.debugLog("ProcessorsResource.uploadProcessorLogo: sOutputFilePath: " + sOutputFilePath);
	    
	    File oTouchFile = new File(sOutputFilePath);
	    
	    try {
			if (!oTouchFile.createNewFile()) {
				Utils.debugLog("ProcessorsResource.uploadProcessorLogo: can't create new file");
			}
		} catch (IOException e) {
			Utils.debugLog("ProcessorsResource.uploadProcessorLogo: " + e.toString());
			e.printStackTrace();
		}	    
	    
	    ImageFile oOutputLogo = new ImageFile(sOutputFilePath);
	    boolean bIsSaved =  oOutputLogo.saveImage(oInputFileStream);
	    
	    if(bIsSaved == false){
	    	Utils.debugLog("ProcessorsResource.uploadProcessorLogo:  not saved!");
	    	return Response.status(Status.BAD_REQUEST).build();
	    }
	    
	    boolean bIsResized = oOutputLogo.resizeImage(LOGO_SIZE, LOGO_SIZE);
	    
	    if(bIsResized == false){
	    	Utils.debugLog("ProcessorsResource.uploadProcessorLogo: error in resize");
	    }	    
	    	    
	    try {
	    	
		    // Create the thumb:
		    String sThumbPath = sOutputFilePath;	    	
	    	
	    	Utils.debugLog("ProcessorsResource.uploadProcessorLogo: creating thumb");
	    	
	    	String [] asSplit = sOutputFilePath.split("\\.");
	    	
	    	sThumbPath = asSplit[0] + "_thumb." + asSplit[1];
	    	
	    	Utils.debugLog("ProcessorsResource.uploadProcessorLogo: thumb file path: " + sThumbPath);
	    	
	    	File oThumb = new File(sThumbPath);
	    	
	    	Files.copy(oOutputLogo, oThumb);
	    	
	    	Utils.debugLog("ProcessorsResource.uploadProcessorLogo: thumb file created");
	    	
	    	ImageFile oImageThumb = new ImageFile(sThumbPath);
	    	if (!oImageThumb.resizeImage(50, 44)) {
	    		Utils.debugLog("ProcessorsResource.uploadProcessorLogo: error resizing the thumb");
	    	}
	    	
	    }
	    catch (Exception oEx) {
	    	Utils.debugLog("ProcessorsResource.uploadProcessorLogo:  error creating the thumb " + oEx.toString());
		}
	    
	    
	    oProcessorRepository.updateProcessorDate(oProcessor);
	    
		return Response.status(Status.OK).build();
	}	
	
	@GET
	@Path("/logo/get")
	public Response getProcessorLogo(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId ) {
		
		Utils.debugLog("ProcessorsMediaResource.getProcessorLogo ( ProcId: " + sProcessorId + " )");
		
		if (Utils.isNullOrEmpty(sSessionId)) return Response.status(Status.UNAUTHORIZED).build();
		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser==null) return Response.status(Status.UNAUTHORIZED).build();
		if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();
		
		Utils.debugLog("ProcessorsResource.getProcessorLogo: get Processor " + sProcessorId);	
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		
		if (oProcessor == null) {
			Utils.debugLog("ProcessorsResource.getProcessorLogo: unable to find processor " + sProcessorId);
			return Response.serverError().build();
		}
		
		if (!oProcessor.getUserId().equals(oUser.getUserId())) {
			
			ProcessorSharingRepository oProcessorSharingRepository = new ProcessorSharingRepository();
			
			ProcessorSharing oSharing = oProcessorSharingRepository.getProcessorSharingByUserIdProcessorId(oUser.getUserId(), sProcessorId);
			
			if (oSharing == null) {
				Utils.debugLog("ProcessorsResource.getProcessorLogo: processor not of user " + oUser.getUserId());
				return Response.status(Status.UNAUTHORIZED).build();					
			}
			else {
				Utils.debugLog("ProcessorsResource.getProcessorLogo: processor of user " + oProcessor.getUserId() + " is shared with " + oUser.getUserId());
			}
			
		}
		
		String sPathLogoFolder = ImageResourceUtils.getProcessorImagesBasePath(oProcessor.getName(), false);
		
		ImageFile oLogo = ImageResourceUtils.getImageInFolder(sPathLogoFolder,IMAGE_PROCESSORS_EXTENSIONS );
		String sLogoExtension = ImageResourceUtils.getExtensionOfImageInFolder(sPathLogoFolder,IMAGE_PROCESSORS_EXTENSIONS );
		
		//Check the logo and extension
		if(oLogo == null || sLogoExtension.isEmpty() ){
			return Response.status(Status.NO_CONTENT).build();
		}
		//prepare buffer and send the logo to the client 
		ByteArrayInputStream abImageLogo = oLogo.getByteArrayImage();
		
	    return Response.ok(abImageLogo).build();

	}
	
	
	@GET
	@Path("/images/get")
	public Response getAppImage(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId,
								@QueryParam("imageName") String sImageName) {
		
		Utils.debugLog("ProcessorsMediaResource.getAppImage + " + sImageName + " Proc " + sProcessorId);
		
		if (Utils.isNullOrEmpty(sSessionId)) return Response.status(Status.UNAUTHORIZED).build();
		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser==null) return Response.status(Status.UNAUTHORIZED).build();
		if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();
		
		Utils.debugLog("ProcessorsResource.getAppImage: get Processor " + sProcessorId);	
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		
		if (oProcessor == null) {
			Utils.debugLog("ProcessorsResource.getAppImage: unable to find processor " + sProcessorId);
			return Response.serverError().build();
		}
		
		if (!oProcessor.getUserId().equals(oUser.getUserId())) {
			
			ProcessorSharingRepository oProcessorSharingRepository = new ProcessorSharingRepository();
			
			ProcessorSharing oSharing = oProcessorSharingRepository.getProcessorSharingByUserIdProcessorId(oUser.getUserId(), sProcessorId);
			
			if (oSharing == null) {
				Utils.debugLog("ProcessorsResource.getAppImage: processor not of user " + oUser.getUserId());
				return Response.status(Status.UNAUTHORIZED).build();					
			}
			else {
				Utils.debugLog("ProcessorsResource.getAppImage: processor of user " + oProcessor.getUserId() + " is shared with " + oUser.getUserId());
			}
		}
		
		
		String sPathLogoFolder = Wasdi.getDownloadPath(m_oServletConfig) + "/processors/" + oProcessor.getName();
		ImageFile oImage = ImageResourceUtils.getImageInFolder(sPathLogoFolder + sImageName,IMAGE_PROCESSORS_EXTENSIONS );
		String sLogoExtension = ImageResourceUtils.getExtensionOfImageInFolder(sPathLogoFolder + sImageName,IMAGE_PROCESSORS_EXTENSIONS );;
		
		//Check the logo and extension
		if(oImage == null || sLogoExtension.isEmpty() ){
			return Response.status(Status.NO_CONTENT).build();
		}
		//prepare buffer and send the logo to the client 
		ByteArrayInputStream abImage = oImage.getByteArrayImage();
		
	    return Response.ok(abImage).build();

	}
	
	@DELETE
	@Path("/images/delete")
	public Response deleteProcessorImage(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId, @QueryParam("imageName") String sImageName ) {
		
		Utils.debugLog("ProcessorsMediaResource.deleteProcessorImage( ProcId: " + sProcessorId + ", Image Name: " + sImageName + " )");
		
		if (Utils.isNullOrEmpty(sSessionId)) return Response.status(Status.UNAUTHORIZED).build();
		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser==null) return Response.status(Status.UNAUTHORIZED).build();
		if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();
		
		Utils.debugLog("ProcessorsResource.deleteProcessorImage: get Processor " + sProcessorId);	
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		
		if (oProcessor == null) {
			Utils.debugLog("ProcessorsResource.deleteProcessorImage: unable to find processor " + sProcessorId);
			return Response.serverError().build();
		}
		
		if (!oProcessor.getUserId().equals(oUser.getUserId())) {
			ProcessorSharingRepository oProcessorSharingRepository = new ProcessorSharingRepository();
			ProcessorSharing oSharing = oProcessorSharingRepository.getProcessorSharingByUserIdProcessorId(oUser.getUserId(), sProcessorId);
			
			if (oSharing == null) {
				Utils.debugLog("ProcessorsResource.deleteProcessorImage: processor not of user " + oUser.getUserId());
				return Response.status(Status.UNAUTHORIZED).build();					
			}
			else {
				Utils.debugLog("ProcessorsResource.deleteProcessorImage: processor of user " + oProcessor.getUserId() + " is shared with " + oUser.getUserId());
			}
		}
				
		if(Utils.isNullOrEmpty(sImageName)) {
			Utils.debugLog("ProcessorsResource.deleteProcessorImage: Image name is null" );
			return Response.status(Status.BAD_REQUEST).build();
		}

		//sanity check: is sImageName safe? It must be a file name, not a path
		if(sImageName.contains("/") || sImageName.contains("\\")) {
			Utils.debugLog("ProcessorsResource.deleteProcessorImage: Image name looks like a path" );
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		String sPathFolder = ImageResourceUtils.getProcessorImagesBasePath(oProcessor.getName(), false);

		java.nio.file.Path oNioDir = null;
		try {
			oNioDir = Paths.get(sPathFolder);
		} catch (InvalidPathException  oE) {
			Utils.debugLog("ProcessorsResource.deleteProcessorImage: requested directory " + sPathFolder + " could not be obtained from OS due to " + oE );
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		if(!java.nio.file.Files.isDirectory(oNioDir)) {
			Utils.debugLog("ProcessorsResource.deleteProcessorImage: " + sPathFolder + " is not a directory" );
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		java.nio.file.Path oNioFile = oNioDir.resolve(sImageName);
		if(!java.nio.file.Files.exists(oNioFile)) {
			Utils.debugLog("ProcessorsResource.deleteProcessorImage: file " + sImageName +" not found in " + sPathFolder);
			return Response.status(Status.NOT_FOUND).build();
		}
		
		//delete file
		try {
			java.nio.file.Files.delete(oNioFile);
		} catch (NoSuchFileException oE) {
			Utils.debugLog("ProcessorsResource.deleteProcessorImage: file " + sImageName +" not found in " + sPathFolder + ": " + oE);
			return Response.status(Status.NOT_FOUND).build();
		} catch (DirectoryNotEmptyException oE) {
			Utils.debugLog("ProcessorsResource.deleteProcessorImage: " + sImageName + " in " + sPathFolder + " is a non-empty directory and cannot be deleted: " + oE);
			return Response.status(Status.BAD_REQUEST).build();
		} catch (Exception oE) {
			Utils.debugLog("ProcessorsResource.deleteProcessorImage: requested directory " + sPathFolder + " could not be deleted due to " + oE );
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		return Response.status(Status.OK).build();
	}
	
	
	@POST
	@Path("/images/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadProcessorImage(@FormDataParam("image") InputStream fileInputStream, @FormDataParam("image") FormDataContentDisposition fileMetaData,
										@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId ) {
		
		Utils.debugLog("ProcessorsMediaResource.uploadProcessorImage( ProcId: " + sProcessorId + " )");
		
		if (Utils.isNullOrEmpty(sSessionId)) return Response.status(Status.UNAUTHORIZED).build();
		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser==null) return Response.status(Status.UNAUTHORIZED).build();
		if (Utils.isNullOrEmpty(oUser.getUserId())) return Response.status(Status.UNAUTHORIZED).build();
		
		Utils.debugLog("ProcessorsResource.uploadProcessorImage: get Processor " + sProcessorId);	
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		
		if (oProcessor == null) {
			Utils.debugLog("ProcessorsResource.uploadProcessorImage: unable to find processor " + sProcessorId);
			return Response.serverError().build();
		}
		
		if (!oProcessor.getUserId().equals(oUser.getUserId())) {
			
			ProcessorSharingRepository oProcessorSharingRepository = new ProcessorSharingRepository();
			
			ProcessorSharing oSharing = oProcessorSharingRepository.getProcessorSharingByUserIdProcessorId(oUser.getUserId(), sProcessorId);
			
			if (oSharing == null) {
				Utils.debugLog("ProcessorsResource.uploadProcessorImage: processor not of user " + oUser.getUserId());
				return Response.status(Status.UNAUTHORIZED).build();					
			}
			else {
				Utils.debugLog("ProcessorsResource.uploadProcessorImage: processor of user " + oProcessor.getUserId() + " is shared with " + oUser.getUserId());
			}
		}		
	
		String sExt;
		String sFileName;
				
		//get filename and extension 
		if(fileMetaData != null && Utils.isNullOrEmpty(fileMetaData.getFileName()) == false){
			sFileName = fileMetaData.getFileName();
			sExt = FilenameUtils.getExtension(sFileName);
		} else {
			Utils.debugLog("ProcessorsResource.uploadProcessorImage: Invalid uploaded file");
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		if( ImageResourceUtils.isValidExtension(sExt,IMAGE_PROCESSORS_EXTENSIONS) == false ){
			Utils.debugLog("ProcessorsResource.uploadProcessorImage: Invalid extension");
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		// Take path
		String sAbsolutePathFolder = ImageResourceUtils.getProcessorImagesBasePath(oProcessor.getName(), false);
		String sRelativePathFolder = ImageResourceUtils.getProcessorImagesBasePath(oProcessor.getName());
		
		ImageResourceUtils.createDirectory(sAbsolutePathFolder);
		String sAvaibleFileName = ImageResourceUtils.getAvaibleProcessorImageFileName(sAbsolutePathFolder);
		
		Utils.debugLog("ProcessorsResource.uploadProcessorImage: available file name: " + sAvaibleFileName);
		
		if(sAvaibleFileName.isEmpty()){
			Utils.debugLog("ProcessorsResource.uploadProcessorImage: max images count reached");
			//the user have reach the max number of images 
	    	return Response.status(Status.BAD_REQUEST).build();
		}
		
		String sAbsoluteImageFilePath = sAbsolutePathFolder + sAvaibleFileName + "." + sExt.toLowerCase();
		String sRelativeImageFilePath = sRelativePathFolder + sAvaibleFileName + "." + sExt.toLowerCase();
		
	    File oTouchFile = new File(sAbsoluteImageFilePath);
	    
	    try {
			if (!oTouchFile.createNewFile()) {
				Utils.debugLog("ProcessorsResource.uploadProcessorImage: can't create new file");
			}
		} catch (IOException e) {
			Utils.debugLog("ProcessorsResource.uploadProcessorImage: " + e.toString());
			e.printStackTrace();
		}
		
		ImageFile oNewImage = new ImageFile(sAbsoluteImageFilePath);

		//TODO SCALE IMAGE ?
		boolean bIsSaved = oNewImage.saveImage(fileInputStream);
	    if(bIsSaved == false){
	    	Utils.debugLog("ProcessorsResource.uploadProcessorImage: error saving the image");
	    	return Response.status(Status.BAD_REQUEST).build();
	    }
	    
	    Utils.debugLog("ProcessorsResource.uploadProcessorImage: image saved, check size");
	    
		double dBytes = (double) oNewImage.length();
		double dKilobytes = (dBytes / 1024);
		double dMegabytes = (dKilobytes / 1024);
		
		if( dMegabytes > (double) ProcessorsMediaResource.MAX_IMAGE_MB_SIZE){
			Utils.debugLog("ProcessorsResource.uploadProcessorImage: image too big, delete it");
			oNewImage.delete();
	    	return Response.status(Status.BAD_REQUEST).build();
		}
		
	    try {
	    	
		    // Create the thumb:
		    String sThumbPath = sAbsoluteImageFilePath;	    	
	    	
	    	Utils.debugLog("ProcessorsResource.uploadProcessorImage: creating thumb");
	    	
	    	String [] asSplit = sAbsoluteImageFilePath.split("\\.");
	    	
	    	sThumbPath = asSplit[0] + "_thumb." + asSplit[1];
	    	
	    	Utils.debugLog("ProcessorsResource.uploadProcessorImage: thumb file path: " + sThumbPath);
	    	
	    	File oThumb = new File(sThumbPath);
	    	
	    	Files.copy(oNewImage, oThumb);
	    	
	    	Utils.debugLog("ProcessorsResource.uploadProcessorImage: thumb file created");
	    	
	    	ImageFile oImageThumb = new ImageFile(sThumbPath);
	    	if (!oImageThumb.resizeImage(50, 44)) {
	    		Utils.debugLog("ProcessorsResource.uploadProcessorImage: error resizing the thumb");
	    	}
	    	
	    }
	    catch (Exception oEx) {
	    	Utils.debugLog("ProcessorsResource.uploadProcessorLogo:  error creating the thumb " + oEx.toString());
		}
		
		
		Utils.debugLog("ProcessorsResource.uploadProcessorImage: image uploaded");
		oProcessorRepository.updateProcessorDate(oProcessor);
		
		PrimitiveResult oPrimitiveResult = new PrimitiveResult();
		oPrimitiveResult.setStringValue(sRelativeImageFilePath);
		
		return Response.ok(oPrimitiveResult).build();
	}
	
	@GET
	@Path("categories/get")
	public Response getCategories(@HeaderParam("x-session-token") String sSessionId) {
		
		Utils.debugLog("ProcessorsMediaResource.getCategories");
		
		User oUser = getUser(sSessionId);
		
		AppsCategoriesRepository oAppCategoriesRepository = new AppsCategoriesRepository();
		
		// Check the user session
		if(oUser == null){
			return Response.status(Status.UNAUTHORIZED).build();
		}

		
		List<AppCategory> aoAppCategories = oAppCategoriesRepository.getCategories();
		ArrayList<AppCategoryViewModel> aoAppCategoriesViewModel = getCategoriesViewModel(aoAppCategories);
		
	    return Response.ok(aoAppCategoriesViewModel).build();

	}
	
	@DELETE
	@Path("/reviews/delete")
	public Response deleteReview(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId, @QueryParam("reviewId") String sReviewId ) {
		
		try {
		    sProcessorId = java.net.URLDecoder.decode(sProcessorId, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			Utils.debugLog("ProcessorsMediaResource.deleteReview excepion decoding processor Id");
		}
		
		try {
			sReviewId = java.net.URLDecoder.decode(sReviewId, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			Utils.debugLog("ProcessorsMediaResource.deleteReview excepion decoding review Id");
		}		
		
		Utils.debugLog("ProcessorsMediaResource.deleteReview( sProcessorId: "+ sProcessorId +" reviewId: "+sReviewId+")");
		
		//************************ TODO CHECK IF THE USER IS THE OWNER OF THE REVIEW ************************//
		
		User oUser = getUser(sSessionId);
		// Check the user session
		if(oUser == null){
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		String sUserId = oUser.getUserId();

		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);


		if( oProcessor != null && Utils.isNullOrEmpty(oProcessor.getName()) ) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		ReviewRepository oReviewRepository = new ReviewRepository();
		
		//CHEK USER ID TOKEN AND USER ID IN VIEW MODEL ARE ==
		if( oReviewRepository.isTheOwnerOfTheReview(sProcessorId,sReviewId,sUserId) == false ){
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		int iDeletedCount = oReviewRepository.deleteReview(sProcessorId, sReviewId);

		if( iDeletedCount == 0 ){
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		return Response.status(Status.OK).build();
	}
	
	@POST
	@Path("/reviews/update")
	public Response updateReview(@HeaderParam("x-session-token") String sSessionId, ReviewViewModel oReviewViewModel) {
		
		Utils.debugLog("ProcessorsMediaResource.updateReview");
	
		User oUser = getUser(sSessionId);
		// Check the user session
		if(oUser == null){
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		String sUserId = oUser.getUserId();
		
		if(oReviewViewModel == null ){
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		ReviewRepository oReviewRepository =  new ReviewRepository();
		
		//CHECK THE VALUE OF THE VOTE === 1 - 5
		if( isValidVote(oReviewViewModel.getVote()) == false ){
			return Response.status(Status.BAD_REQUEST).build();
		}
				
		Review oReview = getReviewFromViewModel(oReviewViewModel, sUserId, oReviewViewModel.getId());
		
		boolean isUpdated = oReviewRepository.updateReview(oReview);
		if(isUpdated == false){
			return Response.status(Status.BAD_REQUEST).build();
		}
		else {
			return Response.status(Status.OK).build();
		}
	}
	
	@POST
	@Path("/reviews/add")
//	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response addReview(@HeaderParam("x-session-token") String sSessionId, ReviewViewModel oReviewViewModel) {//
		
		Utils.debugLog("ProcessorsMediaResource.addReview");
	
		User oUser = getUser(sSessionId);
		// Check the user session
		if(oUser == null){
			Utils.debugLog("ProcessorsMediaResource.addReview: invalid user");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		if(oReviewViewModel == null ){
			Utils.debugLog("ProcessorsMediaResource.addReview: invalid view model");
			return Response.status(Status.BAD_REQUEST).build();
		}		
		
		String sUserId = oUser.getUserId();
				
		//CHECK THE VALUE OF THE VOTE === 1 - 5
		if( isValidVote(oReviewViewModel.getVote()) == false ){
			Utils.debugLog("ProcessorsMediaResource.addReview: invalid vote");
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		String sProcessorId = oReviewViewModel.getProcessorId();
		
		if (Utils.isNullOrEmpty(sProcessorId)) {
			Utils.debugLog("ProcessorsMediaResource.addReview: invalid proc id");
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		
		if (oProcessor == null) {
			Utils.debugLog("ProcessorsMediaResource.addReview: processor null " + sProcessorId);
			return Response.status(Status.BAD_REQUEST).build();			
		}
		
		ReviewRepository oReviewRepository =  new ReviewRepository();
		
		Review oReview = getReviewFromViewModel(oReviewViewModel,sUserId, Utils.GetRandomName());
		
		//LIMIT THE NUMBER OF COMMENTS
		if(oReviewRepository.alreadyVoted(oReviewViewModel.getProcessorId(), sUserId) == true){
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		// ADD ID 
		oReview.setId(Utils.GetRandomName()); 
		
		oReviewRepository.addReview(oReview);
		
		return Response.status(Status.OK).build();
	}
	
	@GET
	@Path("/reviews/getlist")
	public Response getReviewListByProcessor(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorName") String sProcessorName, @QueryParam("page") Integer iPage, @QueryParam("itemsperpage") Integer iItemsPerPage) {
		
		Utils.debugLog("ProcessorsMediaResource.getReview");


		User oUser = getUser(sSessionId);
		// Check the user session
		if(oUser == null){
			return Response.status(Status.UNAUTHORIZED).build();
		}

		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessorByName(sProcessorName);
		
		if(oProcessor == null || Utils.isNullOrEmpty(oProcessor.getName()) ) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		if (iPage==null) iPage = 0;
		if (iItemsPerPage==null) iItemsPerPage = 4;
		
		
		// Get all the reviews
		ReviewRepository oReviewRepository =  new ReviewRepository();
		List<Review> aoApplicationReviews = oReviewRepository.getReviews(oProcessor.getProcessorId());
		
		if(aoApplicationReviews == null || aoApplicationReviews.size() == 0){
			  return Response.ok(new ListReviewsViewModel()).build();
		}
		
		// Cast in a list, computing all the statistics
		ListReviewsViewModel oListReviewsViewModel = getListReviewsViewModel(aoApplicationReviews);
		oListReviewsViewModel.setAlreadyVoted(oReviewRepository.alreadyVoted(oProcessor.getProcessorId(), oUser.getUserId()));
		
		ArrayList<ReviewViewModel> aoCleanedList = new ArrayList<ReviewViewModel>();
		
		// Clean the list: return only elements in the pagination
		for(int iReviews=0; iReviews<oListReviewsViewModel.getReviews().size(); iReviews ++) {
			
			if (iReviews<iPage*iItemsPerPage) continue;
			
			aoCleanedList.add(oListReviewsViewModel.getReviews().get(iReviews));
			
			if (iReviews>=((iPage+1)*iItemsPerPage)) {
				break;
			}
		}
		
		oListReviewsViewModel.setReviews(aoCleanedList);
		
	    return Response.ok(oListReviewsViewModel).build();

	}
	
	
	@GET
	@Path("/publisher/getlist")
	public Response getPublishers(@HeaderParam("x-session-token") String sSessionId) {
		
		Utils.debugLog("ProcessorsMediaResource.getPublishers");


		User oUser = getUser(sSessionId);
		// Check the user session
		if(oUser == null){
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		// Get all the processors
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		ProcessorSharingRepository oProcessorSharingRepository = new ProcessorSharingRepository();
		
		List<Processor> aoProcessors = oProcessorRepository.getDeployedProcessors();
		
		if(aoProcessors == null) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		// Create a return list of publishers
		ArrayList<PublisherFilterViewModel> aoPublishers = new ArrayList<PublisherFilterViewModel>();
		
		// For each processor
		for (Processor oProcessor : aoProcessors) {
			
			if (!oProcessor.getShowInStore()) continue;
			
			ProcessorSharing oSharing = oProcessorSharingRepository.getProcessorSharingByUserIdProcessorId(oUser.getUserId(), oProcessor.getProcessorId());
			
			if (oProcessor.getIsPublic() != 1) {
				if (oProcessor.getUserId().equals(oUser.getUserId()) == false) {
					if (oSharing == null) continue;
				}
			}			
			
			boolean bFound = false;
			
			// Check if there is already a view model of the publisher
			for (PublisherFilterViewModel oPublisher : aoPublishers) {
				if (oPublisher.getPublisher().equals(oProcessor.getUserId())) {
					// Yes, increment the count and break;
					bFound = true;
					oPublisher.setAppCount(oPublisher.getAppCount()+1);
					break;
				}
			}
			
			if (!bFound) {
				// New Publisher, create the View Model
				PublisherFilterViewModel oPublisherFilter = new PublisherFilterViewModel();
				oPublisherFilter.setPublisher(oProcessor.getUserId());
				oPublisherFilter.setAppCount(1);
				aoPublishers.add(oPublisherFilter);
			}
		}
		
	    return Response.ok(aoPublishers).build();

	}
	
	
	/**
	 * Checks if a vote is valid or not
	 * @param fVote
	 * @return
	 */
	private boolean isValidVote(Float fVote){
		if (fVote>=0.0 && fVote<=5.0) return true;
		else return false;
	}
	
	/**
	 * Converts a Review View Model in a Review Entity
	 * @param oReviewViewModel
	 * @param sUserId
	 * @param sId
	 * @return
	 */
	private Review getReviewFromViewModel(ReviewViewModel oReviewViewModel, String sUserId, String sId){
		if(oReviewViewModel != null){
			Review oReview = new Review();
			oReview.setTitle(oReviewViewModel.getTitle());
			oReview.setComment(oReviewViewModel.getComment());
			oReview.setDate((double)(new Date()).getTime());
			oReview.setId(sId); 
			oReview.setProcessorId(oReviewViewModel.getProcessorId());
			oReview.setUserId(sUserId);
			oReview.setVote(oReviewViewModel.getVote());
			return oReview;
		}
		return null;
	}
	
	/**
	 * Fill the Review Wrappwer View Model result from a list of reviews
	 * @param aoReviewRepository
	 * @return
	 */
	private ListReviewsViewModel getListReviewsViewModel(List<Review> aoReviewRepository ){
		ListReviewsViewModel oListReviews = new ListReviewsViewModel();
		List<ReviewViewModel> aoReviews = new ArrayList<ReviewViewModel>();
		if(aoReviewRepository == null){
			return null; 
		}
		
		//CHECK VALUE VOTE policy 1 - 5
		float fSumVotes = 0;

		for(Review oReview: aoReviewRepository){
			ReviewViewModel oReviewViewModel = new ReviewViewModel();
			oReviewViewModel.setComment(oReview.getComment());

			oReviewViewModel.setDate( Utils.getDate(oReview.getDate()) );
			
			oReviewViewModel.setId(oReview.getId());
			oReviewViewModel.setUserId(oReview.getUserId());
			oReviewViewModel.setProcessorId(oReview.getProcessorId());
			oReviewViewModel.setVote(oReview.getVote());
			oReviewViewModel.setTitle(oReview.getTitle());
			fSumVotes = fSumVotes + oReview.getVote();
			
			aoReviews.add(oReviewViewModel);
		}
		
		float avgVote = (float)fSumVotes / aoReviews.size();
		
		oListReviews.setReviews(aoReviews);
		oListReviews.setAvgVote(avgVote);
		oListReviews.setNumberOfOneStarVotes(getNumberOfVotes(aoReviews , 1));
		oListReviews.setNumberOfTwoStarVotes(getNumberOfVotes(aoReviews , 2));
		oListReviews.setNumberOfThreeStarVotes(getNumberOfVotes(aoReviews , 3));
		oListReviews.setNumberOfFourStarVotes(getNumberOfVotes(aoReviews , 4));
		oListReviews.setNumberOfFiveStarVotes(getNumberOfVotes(aoReviews , 5));

		return oListReviews;
	}
	
	/**
	 * Count the number of votes of a specified type in a list of references
	 * @param aoReviews
	 * @param iReferenceVote
	 * @return
	 */
	private int getNumberOfVotes(List<ReviewViewModel> aoReviews, int iReferenceVote ){
		int iNumberOfVotes = 0;
		for(ReviewViewModel oReview : aoReviews){
			if( oReview.getVote() == ((float)iReferenceVote)){
				iNumberOfVotes++;
			}
			
		}
		return iNumberOfVotes;
	}
	
	/**
	 * Converts a List of App Categories in the equivalent list of view models
	 * @param aoAppCategories
	 * @return
	 */
	private ArrayList<AppCategoryViewModel> getCategoriesViewModel(List<AppCategory> aoAppCategories ){
		
		ArrayList<AppCategoryViewModel> aoAppCategoriesViewModel = new ArrayList<AppCategoryViewModel>();
		
		for(AppCategory oCategory:aoAppCategories){
			AppCategoryViewModel oAppCategoryViewModel = new AppCategoryViewModel();
			oAppCategoryViewModel.setId(oCategory.getId());
			oAppCategoryViewModel.setCategory(oCategory.getCategory());
			aoAppCategoriesViewModel.add(oAppCategoryViewModel);
		}
		
		return aoAppCategoriesViewModel;
	} 
	
	protected User getUser(String sSessionId){
		
		if (Utils.isNullOrEmpty(sSessionId)) {
			return null;
		}
		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		if (oUser == null) {
			return null;
		}
		if (Utils.isNullOrEmpty(oUser.getUserId())) {
			return null;
		}
		return oUser;	
	}

}

