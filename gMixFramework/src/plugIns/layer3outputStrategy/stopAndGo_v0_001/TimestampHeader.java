package plugIns.layer3outputStrategy.stopAndGo_v0_001;

import java.util.Arrays;

import framework.core.util.Util;


public class TimestampHeader {

	public long tsMin;
	public long tsMax;
	public int delay;
	public byte[] headerAsArray;
	
	
	public TimestampHeader(byte[] timestamps) {
		this.headerAsArray  = timestamps;
		if (timestamps.length == 4) {
			this.delay = Util.byteArrayToInt(timestamps);
		} else {
			byte[] tsMinArray = Arrays.copyOfRange(timestamps, 0, 8);
			byte[] tsMaxArray = Arrays.copyOfRange(timestamps, 8, 16);
			byte[] delay = Arrays.copyOfRange(timestamps, 16, 22);
			this.tsMin = Util.byteArrayToLong(tsMinArray);
			this.tsMax = Util.byteArrayToLong(tsMaxArray);
			this.delay = Util.byteArrayToInt(delay);
			
		}
	}
	
	
	public TimestampHeader(long tsMin, long tsMax, int delay) {
		this.tsMin = tsMin;
		this.tsMax = tsMax;
		this.delay = delay;
		this.headerAsArray = Util.concatArrays(
				new byte[][] {
						Util.longToByteArray(tsMin),
						Util.longToByteArray(tsMax),
						Util.intToByteArray(delay)
				});
	}
	
	
	public TimestampHeader(int delay) {
		this.delay = delay;
		this.headerAsArray = Util.intToByteArray(delay);
	}

}
