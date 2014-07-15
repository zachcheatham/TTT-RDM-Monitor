package me.zachcheatham.rdmmonitor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DamageEvent 
{
	//private static final Pattern logItemPattern = Pattern.compile("\\t(.*?)\\t (.*?) \\[(.*?)\\] (.*?) (.*?) \\[(.*?)\\]");
	private static final Pattern damagePattern = Pattern.compile("(?:.*?)\\s-\\s(.*?):\\s?\\t\\s(.*?)\\s\\[(.*?)\\]\\s(?:damaged|killed|froze)\\s(.*?)\\s\\[(.*?)\\]");
	private static final Pattern typePattern = Pattern.compile("(?:.*?)\\s-\\s(.*?):(?:.*?)$");
	private static final Pattern dnaPattern = Pattern.compile("(?:.*?):\\s?\\t\\s(.*?)retrieved\\sDNA\\sof\\s(.*?)\\sfrom\\scorpse\\sof\\s(.*?)$");
	
	public enum Team
	{
		TRAITOR, INNOCENT, DETECTIVE, UNKNOWN;
	}
	
	public enum LogType
	{
		DAMAGE, KILL, FREEZE, DNA, UNKNOWN;
	}
	
	public final Team sourceTeam;
	public final String sourcePlayer;
	public final Team targetTeam;
	public final String targetPlayer;
	public final LogType type;
	public final String damage;
	
	public DamageEvent(String line)
	{			
		damage = line.trim();
		
		Matcher typeMatch = typePattern.matcher(damage);

		if (typeMatch.find())
		{
			if (typeMatch.group(1).equals("SAMPLE"))
			{
				type = LogType.DNA;
				
				Matcher dnaMatch = dnaPattern.matcher(damage);
				if (dnaMatch.find())
				{
					sourcePlayer = dnaMatch.group(1);
					targetPlayer = dnaMatch.group(2);
				}
				else
				{
					sourcePlayer = "unknown";
					targetPlayer = "unknown";
				}
					
				sourceTeam = Team.UNKNOWN;
				targetTeam = Team.UNKNOWN;
			}
			else
			{
				Matcher damageMatch = damagePattern.matcher(damage);
				if (!damageMatch.find())
				{
					type = LogType.UNKNOWN;
					targetTeam = Team.UNKNOWN;
					sourceTeam = Team.UNKNOWN;
					targetPlayer = "unknown";
					sourcePlayer = "unknown";
					return;
				}
				
				switch (typeMatch.group(1))
				{
				case "DMG":
					type = LogType.DAMAGE;
					break;
				case "KILL":
					type = LogType.KILL;
					break;
				case "FREEZE":
					type = LogType.FREEZE;
					break;
				default:
					type = LogType.UNKNOWN;
				}
				
				if (damage.contains("<something/world>"))
					sourcePlayer = "<something/world>";
				else
					sourcePlayer = damageMatch.group(2);
				
				switch (damageMatch.group(3).toLowerCase())
				{
				case "traitor":
					sourceTeam = Team.TRAITOR;
					break;
				case "innocent":
					sourceTeam = Team.INNOCENT;
					break;
				case "detective":
					sourceTeam = Team.DETECTIVE;
					break;
				default:
					sourceTeam = Team.UNKNOWN;
				}
				
				targetPlayer = damageMatch.group(4);
				
				switch (damageMatch.group(5).toLowerCase())
				{
				case "traitor":
					targetTeam = Team.TRAITOR;
					break;
				case "innocent":
					targetTeam  = Team.INNOCENT;
					break;
				case "detective":
					targetTeam  = Team.DETECTIVE;
					break;
				default:
					targetTeam = Team.UNKNOWN;
				}
			}
		}
		else
		{
			type = LogType.UNKNOWN;
			targetTeam = Team.UNKNOWN;
			sourceTeam = Team.UNKNOWN;
			targetPlayer = "unknown";
			sourcePlayer = "unknown";
		}
	}
	
	public boolean isRDM()
	{
		if (type != LogType.KILL && type != LogType.DAMAGE)
			return false;
		
		return (((targetTeam != Team.TRAITOR && sourceTeam != Team.TRAITOR) || (targetTeam == Team.TRAITOR && sourceTeam == Team.TRAITOR)) && !targetPlayer.equals(sourcePlayer));
	}
	
	public String getLogLine()
	{
		return damage;
	}
	
	public String compileLogLine()
	{
		return this.sourcePlayer + "[" + this.sourceTeam + "] " + this.type + " " + this.targetPlayer + "[" + this.targetTeam + "]";
	}
}
