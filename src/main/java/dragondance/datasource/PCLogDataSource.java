package dragondance.datasource;

import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;

import dragondance.Log;

public class PCLogDataSource extends CoverageDataSource {

	public PCLogDataSource(String sourceFile, String mainModule) throws FileNotFoundException {
		super(sourceFile, mainModule, CoverageDataSource.SOURCE_TYPE_PCLOG);
	}

	private void parseInformation() {
		Log.setEnable(true);
		Log.enableGhidraConsoleLogging(true);
		Log.println("parseInformation");
		ArrayList<Long> entries = new ArrayList<Long>();
		int wordSize = 8;
		ByteBuffer buffer = ByteBuffer.allocate(wordSize);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		byte[] entryBuf = new byte[wordSize];
		byte[] header = { 114, 105, 112, 95, 117, 54, 52, 10 };

		while (true) {
			int readLen = this.readBytes(entryBuf, 0, wordSize);
			if (readLen == 0 || readLen == -1) {
				break;
			}
			if (readLen != wordSize) {
				break;
			}
			if (Arrays.equals(entryBuf, header)) {
				continue;
			}

			buffer.clear();
			buffer.put(entryBuf, 0, wordSize);
			buffer.flip();
			Long res = buffer.getLong();
			Log.println("got: 0x" + Long.toHexString(res));
			entries.add(res);
		}

		Long moduleStart = 0L;
		int moduleId = -1;

		for (var entry : entries) {
			if (entry - moduleStart > 0x1000000) {
				moduleStart = entry;
				Long moduleEnd = moduleStart + 0x1000000;
				moduleId += 1;
				this.pushModule(new ModuleInfo(moduleId, moduleStart, moduleEnd, ""));
			}
			int entryOffset = (int) (entry - moduleStart);
			int entrySize = 1;
			int instructionCount = 1;
			this.pushEntry(new BlockEntry(entryOffset, entrySize, moduleId, instructionCount));
		}
	}

	@Override
	public boolean process() {
		parseInformation();
		this.processed = true;
		return super.process();
	}

}
