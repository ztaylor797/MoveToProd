package com.some.company.mv2prd.util.logging;

import javax.xml.bind.JAXBException;
import java.io.PrintWriter;
import java.util.Date;

public class Logger {

	private String className = "unknown";

	private PrintWriter printWriter = null;
	private LogWriterSingleton logWriterSingleton;
	private DebugSettingSingleton debugSettingSingleton;

	private Logger(String className) {
		this.className = className;
		this.logWriterSingleton = LogWriterSingleton.getInstance();
		this.debugSettingSingleton = DebugSettingSingleton.getInstance();
	}

	private void log(String string) {
		System.out.println(string);
		if (printWriter == null)
			printWriter = this.logWriterSingleton.getPrintWriter();
		if (printWriter != null)
			printWriter.println(string);
	}

	public void info(String string) {
		log(now()+" INFO  "+string+" ["+className+"]");
	}

	public void plain(String string) {
		log(string);
	}

	public void debug(String string) {
		if (debugSettingSingleton.debugEnabled()) log(now()+" DEBUG  "+string+" ["+className+"]");
	}

	private String now() {
		Date d= new Date();
		return d.toString();
	}

	public static Logger getLogger(Class thisClass) {
		String className = thisClass.getName();
		Logger logger = new Logger(className);

		return logger;
	}

	public void error(String string) {
		log(now()+" ERROR "+string+" ["+className+"]");
	}

	public void error(String string, JAXBException e) {
		log(now()+" ERROR "+string+" ["+className+"]");
		e.printStackTrace();
	}

	public void error(String string, Exception e) {
		log(now()+" ERROR "+string+" ["+className+"]");
		e.printStackTrace();
	}
	public void warn(String string, Exception e) {
		log(now()+" WARN  "+string+" ["+className+"]");
		e.printStackTrace();
	}
	public void warn(String string) {
		log(now()+" WARN  "+string+" ["+className+"]");
	}

}
