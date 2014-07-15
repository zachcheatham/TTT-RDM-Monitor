package me.zachcheatham.rdmmonitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.commons.io.input.TailerListenerAdapter;


public class LogListener
{
	private final LogListenerCallback callback;
	public final String logPath;
	private final File logFile;
	private Thread listenThread = null;
	
	private boolean isDamageLog = false;
	private List<String> damageLog = new ArrayList<String>();
	
	public LogListener(LogListenerCallback callback, String file)
	{
		this.callback = callback;
		this.logPath = file;
		this.logFile = new File(file);
	}
	
	public void start() throws IOException
	{
		if (listenThread != null)
			throw new IllegalStateException("Log listener is already started!");
		
		if (!logFile.exists())
			throw new FileNotFoundException();

		truncateFile();
		
		TailerListener listener = new LogTailer(this);
		Tailer tailer = new Tailer(logFile, listener, 1000);
		listenThread = new Thread(tailer);
		listenThread.start();
		
		new Tailer(logFile, listener, 1000L, false, false, 4096);
	}
	
	protected void handleLog(String line)
	{
		line = rebuildUTF8String(line);
		
		if (line.equals("	*** Damage log:"))
		{
			isDamageLog = true;
			return;
		}
		else if (line.equals("	*** Damage log end."))
		{
			isDamageLog = false;
			final String[] stringLog = damageLog.toArray(new String[damageLog.size()]);
			Thread t = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					callback.roundOver(stringLog);
				}
			});
			
			t.start();
			
			damageLog.clear();
			return;
		}
		else if (isDamageLog)
		{
			damageLog.add(line);
			return;
		}
	}
	
	private String rebuildUTF8String(String line)
	{
		int len = line.length();
		byte[] bytes = new byte[len];
		
		for (int i=0; i<len; i++)
			bytes[i] = (byte)line.charAt(i);

		try
		{
			return new String(bytes, "UTF8");
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
			return line;
		}
	}

	private void truncateFile() throws IOException
	{
		Files.write(logFile.toPath(), "".getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
	}
	
	private class LogTailer extends TailerListenerAdapter
	{
		private final LogListener ll;
		
		public LogTailer(LogListener ll)
		{
			this.ll = ll;
		}
		
		public void handle(String line)
		{
			ll.handleLog(line);
		}
	}
}
