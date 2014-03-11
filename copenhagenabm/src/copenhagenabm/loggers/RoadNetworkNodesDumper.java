package copenhagenabm.loggers;

import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import copenhagenabm.loggers.KillLogger.SimpleTextFormatter;

public class RoadNetworkNodesDumper {
	
	class SimpleTextFormatter extends Formatter {

		@Override
		public String format(LogRecord rec) {
			StringBuffer buf= new StringBuffer(1000);
			buf.append(formatMessage(rec));
			return buf.toString() + "\n";
		}
		
		public String getHead(Handler h) {
			return "NodeID,BESTEMMI_1,XCoord,YCoord,ELEVATION,FID_1,AdMapKey,AREA,NIEUW_LO_1,VAKGROEP_1,VLOERPAS_1,Distance,AttentionZ,CenterGang";
		}
		
		
	}
	
	
	private final static Logger LOGGER = Logger.getLogger(RoadNetworkNodesDumper.class.getName());
	private SimpleTextFormatter simpleTextFormatter;
	private FileHandler fileTxt;

	public void dump() {
		// TODO Auto-generated method stub
		
	}

}
