package it.fadeout.config;

import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Primary
public class WasdiServletConfig implements ServletConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(WasdiServletConfig.class);

	@Autowired
	private Environment env;

	@Override
	public String getServletName() {
		LOGGER.info("getServletName()");

		return null;
	}

	@Override
	public ServletContext getServletContext() {
		LOGGER.info("getServletContext()");

		return null;
	}

	@Override
	public String getInitParameter(String name) {
		LOGGER.info("getInitParameter()");

		return env.getProperty(name);
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		LOGGER.info("getInitParameterNames()");

		return null;
	}

}
