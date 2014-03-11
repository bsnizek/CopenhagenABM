package copenhagenabm.loggers;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import copenhagenabm.loggers.calibrationlogger.CalibrationLogger;
import copenhagenabm.loggers.decisionmatrixlogger.DecisionMatrixLogger;
import copenhagenabm.main.ContextManager;

public class CopenhagenABMLogging {

	private ArrayList<CopenhagenABMLogger> copenhagenABMLoggers = new ArrayList<CopenhagenABMLogger>();

	private DecisionMatrixLogger decisionMatrixLogger;

	private String outputFolder;

	private CalibrationLogger calibrationLogger;

	/**
	 * 
	 * CopenhagenABMLogging
	 * 
	 * @param loggingFolder
	 * 
	 * Sets up the logging by building a folder within the loggingFolder with a unique name 
	 * 
	 */
	public CopenhagenABMLogging(String loggingFolder) {

		outputFolder = loggingFolder;

		createLoggingSettings();

		long modelID = ContextManager.getUniqueModelID();

		String dfF = new SimpleDateFormat("yyyy.MM.dd@hh:mm:ss").format(System.currentTimeMillis());

		String nameBase = modelID + "-" + dfF;

		String folderName = outputFolder + File.separatorChar + nameBase;

		boolean success = (new File(folderName).mkdir());

		if (!success) {
			// Directory creation failed
		}

		// TODO: check whether the decisionmatrixlogger er set up in the config file		
		createDecisionMatrixLogger(folderName + File.separatorChar + "decisionlogger-" + nameBase + ".txt");

		String calibrationFolderName = folderName + File.separatorChar + "calibrationlog";

		boolean success2 = (new File(calibrationFolderName).mkdir());

		if (!success2) {
			// Directory creation failed
		}

		createCalibrationLogger(calibrationFolderName + File.separatorChar + "calibrationlog-" + 
				ContextManager.getCalibrationModeData().getAngleToDestWeight() + "-"  + 
				ContextManager.getOmitDecisionMatrixMultifields() + "-" + ContextManager.getCalibrationModeData().getNumberOfRepetitions() + "-reps.txt");
	}

	private void createLoggingSettings() {
		// TODO
	}

	public void createDecisionMatrixLogger(String fileName) {
		decisionMatrixLogger = new DecisionMatrixLogger(fileName);
		copenhagenABMLoggers.add(decisionMatrixLogger);
		System.out.println("DECISIONMATRIXLOGGER created.");
	}

	public void createCalibrationLogger(String fileName) {
		calibrationLogger = new CalibrationLogger(fileName);
		copenhagenABMLoggers.add(calibrationLogger);
		System.out.println("CalibrationLogger created.");
	}


	public void close() {

		for (CopenhagenABMLogger cAL : copenhagenABMLoggers) {
			cAL.close();
		}

		System.out.println("All LOGGERS closed");

	}

	/**
	 * getDecisionMatrixLogger() : simple accessor
	 * 
	 * @return
	 */
	public DecisionMatrixLogger getDecisionMatrixLogger() {
		return decisionMatrixLogger;
	}

	public boolean isDecisionLoggerOn() {
		// TODO Auto-generated method stub
		return true;
	}

	public CalibrationLogger getCalibrationLogger() {
		return calibrationLogger;
	}

	public void setCalibrationLogger(CalibrationLogger calibrationLogger) {
		this.calibrationLogger = calibrationLogger;
	}

}
