package it.fadeout.config;

import org.glassfish.jersey.servlet.ServletContainer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WasdiConfig {

//	@Bean
//    public ServletRegistrationBean<ServletContainer> servletRegistrationBean(){
//        // passing the JerseyConfig instance to ServletContainer
//        ServletContainer jerseyContainer = new ServletContainer(new it.fadeout.Wasdi());
//        return new ServletRegistrationBean<ServletContainer>(jerseyContainer, "/rest/*");
//    }

}
