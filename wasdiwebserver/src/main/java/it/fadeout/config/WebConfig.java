package it.fadeout.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
@ComponentScan(basePackageClasses = { it.fadeout.Wasdi.class })
public class WebConfig implements WebMvcConfigurer {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebConfig.class);

	@Autowired
	private ApplicationContext applicationContext;

	public WebConfig() {
	}

	@Bean
	public FilterRegistrationBean<it.fadeout.rest.resources.CORSFilter> loggingFilter() {
		FilterRegistrationBean<it.fadeout.rest.resources.CORSFilter> registrationBean = new FilterRegistrationBean<>();

		registrationBean.setFilter(new it.fadeout.rest.resources.CORSFilter());
		registrationBean.addUrlPatterns("/*");

		return registrationBean;
	}

}