package me.zachcheatham.rdmmonitor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import me.zachcheatham.rdmmonitor.DamageEvent.LogType;
import edu.cmu.relativelayout.BindingFactory;
import edu.cmu.relativelayout.RelativeConstraints;
import edu.cmu.relativelayout.RelativeLayout;

public class RDMMonitor implements LogListenerCallback, ItemListener
{
	public static RDMMonitor instance;
	
	
	public static void main(String[] args)
	{
		instance = new RDMMonitor();
		instance.start();
	}
	
	private final LogListener logListener;
	private final String requiredFile;
	List<DamageEvent> events = new ArrayList<DamageEvent>();
	
	private final JFrame mainFrame = new JFrame("RDM Monitor");
	private final JLabel lastUpdateLabel;
	private final JTextPane textPane;
	private final JComboBox<String> playerFilter;
	
	public RDMMonitor()
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		if (new File("C:\\Program Files (x86)").exists())
		{
			if (new File("C:\\Program Files (x86)\\Steam\\SteamApps\\common\\GarrysMod\\garrysmod\\console.log").exists())
			{
				logListener = new LogListener(this, "C:\\Program Files (x86)\\Steam\\SteamApps\\common\\GarrysMod\\garrysmod\\console.log");
				requiredFile = null;
			}
			else if (new File("D:\\Games\\SteamApps\\").exists())
			{
				logListener = new LogListener(this, "D:\\Games\\SteamApps\\common\\GarrysMod\\garrysmod\\console.log");
				requiredFile = null;
			}
			else
			{
				requiredFile = "C:\\Program Files (x86)\\Steam\\SteamApps\\common\\GarrysMod\\garrysmod\\console.log";
				logListener = null;
			}
		}
		else
		{
			if (new File("C:\\Program Files\\Steam\\SteamApps\\common\\GarrysMod\\garrysmod\\console.log").exists())
			{
				logListener = new LogListener(this, "C:\\Program Files\\Steam\\SteamApps\\common\\GarrysMod\\garrysmod\\console.log");
				requiredFile = null;
			}
			else
			{
				requiredFile = "C:\\Program Files\\Steam\\SteamApps\\common\\GarrysMod\\garrysmod\\console.log";
				logListener = null;
			}
		}
				
		lastUpdateLabel = new JLabel("Waiting for first damage log...");
		playerFilter = new JComboBox<String>();
		textPane = new JTextPane();
	}
	
	public void start()
	{
		BindingFactory bindingFactory = new BindingFactory();
		
		mainFrame.setLayout(new RelativeLayout());
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setLocationRelativeTo(null);
		
		RelativeConstraints lastUpdateLabelConstraints = new RelativeConstraints();
		lastUpdateLabelConstraints.addBinding(bindingFactory.leftEdge());
		lastUpdateLabelConstraints.addBinding(bindingFactory.verticallyCenterAlignedWith(playerFilter));
		
		mainFrame.add(lastUpdateLabel, lastUpdateLabelConstraints);
		
		playerFilter.addItem("Filter by Player...");
		playerFilter.addItemListener(this);
		
		RelativeConstraints playerFilterConstraints = new RelativeConstraints();
		playerFilterConstraints.addBinding(bindingFactory.rightEdge());
		playerFilterConstraints.addBinding(bindingFactory.topEdge());
		
		mainFrame.add(playerFilter, playerFilterConstraints);
		
		JScrollPane scrollPane = new JScrollPane(textPane);
		scrollPane.setAutoscrolls(false);
		
		textPane.setEditable(false);
		
		RelativeConstraints scrollPaneConstraints = new RelativeConstraints();
		scrollPaneConstraints.addBinding(bindingFactory.below(playerFilter));
		scrollPaneConstraints.addBinding(bindingFactory.bottomEdge());
		scrollPaneConstraints.addBinding(bindingFactory.leftEdge());
		scrollPaneConstraints.addBinding(bindingFactory.rightEdge());
		
		mainFrame.add(scrollPane, scrollPaneConstraints);
		
		mainFrame.pack();
		mainFrame.setSize(600, 600);
		Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
	    int x = (int) ((dimension.getWidth() - mainFrame.getWidth()) / 2);
	    int y = (int) ((dimension.getHeight() - mainFrame.getHeight()) / 2);
	    mainFrame.setLocation(x, y);
		mainFrame.setVisible(true);
		
		if (requiredFile == null)
		{
			try
			{
				logListener.start();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			JOptionPane.showMessageDialog(mainFrame, "The log file at " + requiredFile + " could not be found.\nMake sure you've added -condebug to your launch options.", "Missing Log File", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void displayEvents(List<DamageEvent> events)
	{
		textPane.setText("");
		
		for (DamageEvent e : events)
		{
			SimpleAttributeSet color = null;
			
			if (e.isRDM())
			{
				color = new SimpleAttributeSet();
				
				if (e.type == LogType.KILL)
					StyleConstants.setForeground(color, Color.red);
				else if (e.type == LogType.DAMAGE)
					StyleConstants.setForeground(color, new Color(255,140,0));
			}
			else if (e.type == LogType.FREEZE)
			{
				color = new SimpleAttributeSet();
				StyleConstants.setForeground(color, Color.blue);
			}
			else if (e.type == LogType.DNA)
			{
				color = new SimpleAttributeSet();
				StyleConstants.setForeground(color, new Color(0,150,0));
			}
			
			try
			{
				textPane.getDocument().insertString(0, e.getLogLine() + "\n", color);
			}
			catch (BadLocationException e1)
			{
				e1.printStackTrace();
			}
		}
	}
	
	@Override
	public void roundOver(final String[] damageLog)
	{
		final List<String> players = new ArrayList<String>();
		events.clear();
		
		for (int i = damageLog.length - 1; i >= 0; i--)
		{
			DamageEvent e = new DamageEvent(damageLog[i]);
			
			if (!players.contains(e.sourcePlayer))
				players.add(e.sourcePlayer);
			
			if (!players.contains(e.targetPlayer))
				players.add(e.targetPlayer);
			
			events.add(e);
		}
		
		SwingUtilities.invokeLater(new Runnable()
		{
		    public void run()
		    {
				lastUpdateLabel.setText("Last update: " + new SimpleDateFormat("hh:mm").format(new Date()));

				playerFilter.setEnabled(false);
				playerFilter.removeAllItems();
				playerFilter.addItem("Filter by Player...");
				for (String player : players)
					playerFilter.addItem(player);					
				
				displayEvents(events);
				
				playerFilter.setEnabled(true);
		    }
		});
	}
	
	@Override
	public void itemStateChanged(ItemEvent event)
	{
		if (!playerFilter.isEnabled())
			return;
		
		String player = (String) event.getItem();
		
		if (player.equals("Filter by Player..."))
		{
			displayEvents(events);
		}
		else
		{
			List<DamageEvent> filtered = new ArrayList<DamageEvent>();
			
			for (DamageEvent e : events)
				if (e.sourcePlayer.equals(player) || e.targetPlayer.equals(player))
					filtered.add(e);
			
			displayEvents(filtered);
		}
	}
}
