package client;
import java.io.File;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.status.StatusLogger;

public class loggerManager {
	private static HashMap<Class<?>, Logger> loggers=null;
	  
	public static Logger getInstance(Class<?> cls){
		if (loggers==null){
			StatusLogger statusLogger=StatusLogger.getLogger();
			Level statusLoggerLevel=statusLogger.getLevel();
			statusLogger.setLevel(Level.OFF);
			LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
	        File file = new File("conf/log4j2.xml");
	        context.setConfigLocation(file.toURI());
	        statusLogger.setLevel(statusLoggerLevel);
			loggers=new HashMap<Class<?>,Logger>();
		}
		
		Logger logger=loggers.get(cls);
		if (logger==null){
          logger=Logger.getLogger(cls);
          loggers.put(cls, logger);
		}  
		return logger;
	}
	
}
