package it.fadeout.config;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import it.fadeout.Wasdi;

//This @Order is required!!!
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WasdiWebInitializer implements WebApplicationInitializer {

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
		// spring WebMvcConfigurer
		ctx.register(WebConfig.class);
		ctx.setServletContext(servletContext);

		// Spring servlet
		ServletRegistration.Dynamic servlet = servletContext.addServlet("dispatcher", new DispatcherServlet(ctx));
		servlet.setLoadOnStartup(1);
		servlet.addMapping("/");

		// Add cuatom filters to servletContext
		FilterRegistration.Dynamic filterRegistration = servletContext.addFilter("WasdiFilter", it.fadeout.rest.resources.CORSFilter.class);
//		filterRegistration.setInitParameter("encoding", "UTF-8");
//		filterRegistration.setInitParameter("forceEncoding", "true");
		filterRegistration.addMappingForUrlPatterns(null, true, "/*");

		// Register Jersey 2.0 servlet
		ServletRegistration.Dynamic jerseyServlet = servletContext.addServlet("wasdiwebserver",
				"org.glassfish.jersey.servlet.ServletContainer");
		// note "javax.ws.rs.Application" doesn't have "core"
		jerseyServlet.setInitParameter("javax.ws.rs.Application", Wasdi.class.getName());
		jerseyServlet.addMapping("/rest/*");
		jerseyServlet.setLoadOnStartup(1);
	}

}