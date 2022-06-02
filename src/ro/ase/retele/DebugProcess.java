package ro.ase.retele;

import java.io.*;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DebugProcess implements Runnable {
	private static final Logger LOG = Logger.getLogger("ro.ase.retele.DebugProcess");
	private PrintWriter out;
	private Process process;
	private String command;
	private String workDir;
	private Integer port;
	private boolean stopped;

	private Pattern patternJava = Pattern.compile("\\s\\|\\s((?!bin/java.exe).*)bin/java.exe");

	DebugProcess(PrintWriter out, String command, Integer port) {
		this.out = out;
		this.command = command;
		this.port = port;
	}

	private String prepareCommand(){
		String command = this.command;

//		command = command.replace("\\", "/");

		String rplc = "";
		Matcher matcherJava = patternJava.matcher(command);
		if (matcherJava.find()) {
			rplc = matcherJava.group(1);
		}

//		command = command.replace(rplc, "/opt/jdk/");

		for(char c = 'A'; c<='Z'; c++){
			command = command.replace(c+":/", "/"+Character.toLowerCase(c)+"/");
		}



		command = command.replace("java.exe", "java -agentlib:jdwp=transport=dt_socket,address="+this.port+",suspend=y,server=y && jdb -attach 127.0.0.1:55005");
		command = command.replace(";", ":");

		String workDir = command.substring(command.indexOf(" | ") + 3, command.length());

		command = command.substring(0, command.indexOf(" | "));

		this.workDir = workDir;

		//command = command.replace("classpath \"", "classpath \"" + workDir +":");

		return command;
	}

	private void start() throws IOException {

		String command = this.prepareCommand();

		String path = "C:\\Users\\gronk\\Desktop\\proiect_retele\\remote-debug-server\\src\\main\\java\\me\\vukas\\Test.java";

		String javaHome = System.getProperty("java.home");
		String javaBin = javaHome
				+ File.separator + "bin"
				+ File.separator + "java";
		String classpath = System.getProperty("java.class.path");
		Class toExecute = Test.class;
		String className = toExecute.getCanonicalName();

		String execCommand = "cd " + workDir + " && " + "exec " + command + " " + path;
//		ProcessBuilder pb = new ProcessBuilder("cmd.exe", "-c", execCommand);
		ProcessBuilder pb = new ProcessBuilder(javaBin,
				"-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=55009",
				"-cp",classpath, className);
		pb.redirectErrorStream(true);
		if(!stopped) {
			LOG.info("Starting process: " + command);
			this.process = pb.start();
			
			
			    ProcessBuilder builder = new ProcessBuilder("C:\\Program Files (x86)\\Java\\jdk1.8.0_111\\bin\\jdb.exe", classpath, className);
			    Process process = builder.start();
			    OutputStream stdin = process.getOutputStream();
			    InputStream stdout = process.getInputStream();
			    BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
			   BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
			        writer.write("stop at Test:11\n");
			        writer.flush();
			        writer.write("run Test\n");
			        writer.flush();
			    
			    String inputLine;
			    Scanner scanner = new Scanner(stdout);
			    while (scanner.hasNextLine()) {
			        System.out.println(scanner.nextLine());
			    }
			 
			this.writeOutput(this.process.getInputStream());
		}
	}

	private void writeOutput(InputStream is) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line;
		while ((line = br.readLine()) != null) {
			this.out.println(line);
		}
	}

	void stop() {
		this.stopped = true;
		if (this.process != null) {
			LOG.info("Stopping process on port " + this.port);
			this.process.destroy();
			try {
				Thread.sleep(5000);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(this.process.isAlive()){
				LOG.info("Forcing stop on port " + this.port);
				this.process.destroyForcibly();
			}
		}
		else{
			LOG.info("Process is null for port " + this.port);
		}
	}

	@Override
	public void run() {
		try {
			start();
		}
		catch (IOException e) {
			e.printStackTrace();
			stop();
		}
	}
}
