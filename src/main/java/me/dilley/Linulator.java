/*
 * Linulator - The Linux Simulator
 * Copyright (C) 2014 Lloyd Dilley
 * http://www.linulator.org/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package me.dilley;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Scanner;

class Linulator
{
  public static final String VERSION = "Linulator v0.1";
  static Scanner scanner = null;
  static String input = null;
  static int option = 0;

  public static void main(String[] args)
  {
    System.out.println(VERSION);
    System.out.println("Enforcing security policy...");
    System.setProperty("java.security.policy", "cfg" + System.getProperty("file.separator") + "linulator.policy");
    SecurityManager securityManager = new SecurityManager();
    System.setSecurityManager(securityManager);
    //securityManager.checkExec("/bin/ls"); // prove we cannot execute commands on the host OS
    Log.write(0, VERSION);
    Log.write(0, "Security policy enforced.");
    System.out.println("Parsing configuration file...");
    Log.write(0, "Parsing configuration file...");
    Config config = new Config();
    config.parseConfig();
    System.out.println("Setting hostname...");
    Log.write(0, "Setting hostname...");
    OperatingSystem.setHostName(config.getHostName());
    OperatingSystem.setShortName(config.getShortName());
    OperatingSystem.setDomainName(config.getDomainName());
    System.out.println("Populating commands...");
    Log.write(0, "Populating commands...");
    Shell.populateCommands();
    System.out.println("Connecting to database...");
    Log.write(0, "Connecting to database...");
    Database.connect();
    System.out.println("Loading filesystem (this can take some time)...");
    Log.write(0, "Loading filesystem (this can take some time)...");
    loadFilesystem();
    System.out.println("Starting network services...");
    Log.write(0, "Starting network services...");
    startServices(config);
    System.out.println("Linulator started successfully.");
    Log.write(0, "Linulator started successfully.");
    mainMenu(config);
  }

  public static void loadFilesystem()
  {
    try
    {
      int currentProgress = 0;
      int formerProgress = -1;
      int percentage = 0;
      long currentEntry = 0;
      long totalEntries = 0;
      double entryPerPercent = 0.00;
      File file = null;
      Filesystem filesystem = null;
      ResultSet resultSet = null;
      HashMap<String, File> fileMap = new HashMap<String, File>();

      // Determine number of directories and files in database
      String query = "SELECT COUNT(*) FROM filesystem";
      resultSet = Database.query(query);
      if(resultSet.next())
      {
        totalEntries = resultSet.getInt(1);
        System.out.println(totalEntries + " entries detected.");
      }

      // Set root of filesystem
      query = "SELECT path, name, inode, type, mode, linkcount, uid, gid, atime, ctime, mtime, link, bindata, txtdata FROM filesystem WHERE path = '/'";
      resultSet = Database.query(query);
      if(resultSet.next())
      {
        file = new File();
        file.setPath(resultSet.getString(1));
        file.setName(resultSet.getString(2));
        file.setInode(resultSet.getLong(3));
        file.setType(resultSet.getByte(4));
        file.setMode(resultSet.getInt(5));
        file.setLinkCount(resultSet.getInt(6));
        file.setUid(resultSet.getInt(7));
        file.setGid(resultSet.getInt(8));
        file.setAtime(resultSet.getLong(9));
        file.setCtime(resultSet.getLong(10));
        file.setMtime(resultSet.getLong(11));
        file.setLink(resultSet.getString(12));
        file.setBinaryContents(resultSet.getBlob(13));
        file.setTextContents(resultSet.getClob(14));
        file.setSize(file.calculateSize());
        //filesystem = new Filesystem(new Node(file));
      }
      else
      {
        System.err.println("Critical: Unable to set root of filesystem.");
        Log.write(2, "Unable to set root of filesystem.");
      }

      // Get everything else
      query = "SELECT path, name, inode, type, mode, linkcount, uid, gid, atime, ctime, mtime, link, bindata, txtdata FROM filesystem";
      resultSet = Database.query(query);
      entryPerPercent = 100.00 / totalEntries;
      System.out.print("Progress: ");
      while(resultSet.next())
      {
        currentEntry++;
        currentProgress = (int)Math.round(entryPerPercent * currentEntry);
        if((currentProgress % 5 == 0) && (currentProgress != formerProgress))
        {
          if(percentage == 0)
            System.out.print("0%");
          else
            System.out.print("..." + percentage + '%');
          percentage += 5;
          formerProgress = currentProgress;
        }
        file = new File();
        file.setPath(resultSet.getString(1));
        file.setName(resultSet.getString(2));
        file.setInode(resultSet.getLong(3));
        file.setType(resultSet.getByte(4));
        file.setMode(resultSet.getInt(5));
        file.setLinkCount(resultSet.getInt(6));
        file.setUid(resultSet.getInt(7));
        file.setGid(resultSet.getInt(8));
        file.setAtime(resultSet.getLong(9));
        file.setCtime(resultSet.getLong(10));
        file.setMtime(resultSet.getLong(11));
        file.setLink(resultSet.getString(12));
        file.setBinaryContents(resultSet.getBlob(13));
        file.setTextContents(resultSet.getClob(14));
        file.setSize(file.calculateSize());
        // Do not hold file contents in memory. That could exhaust memory. When needed, file contents will be accessed from disk.
        Blob binJunk = null;
        file.setBinaryContents(binJunk);
        Clob txtJunk = null;
        file.setTextContents(txtJunk);
        if(!file.getPath().equals("/")) // ignore adding root since we did that first
fileMap.put(file.getPath(), file);
          //filesystem.add(file);
      }
      Database.endQuery();
      System.out.println();
System.out.println("Filesytem size: " + fileMap.size());
System.out.println(fileMap.get("/tmp").getPath());
System.out.println(fileMap.get("/usr/local/bin").getPath());

      //filesystem.showTree();
    }
    catch(SQLException sqle)
    {
      System.err.println("Critical: Unable to load filesystem.");
      Log.write(2, "Unable to load filesystem.");
      System.err.println(sqle.getMessage());
      Log.write(2, sqle.getMessage());
      System.exit(1);
    }
  }

  public static void startServices(Config config)
  {
    if(config.getEchoPort() != 0)
    {
      System.out.println("Starting echo server...");
      Log.write(0, "Starting echo server...");
      TcpServer TcpEchoServer = new TcpServer(config.getListenAddress(), config.getEchoPort(), "echo");
      new Thread(TcpEchoServer).start();
      UdpServer UdpEchoServer = new UdpServer(config.getListenAddress(), config.getEchoPort(), "echo");
      new Thread(UdpEchoServer).start();
    }
    if(config.getDiscardPort() != 0)
    {
      System.out.println("Starting discard server...");
      Log.write(0, "Starting discard server...");
      TcpServer TcpDiscardServer = new TcpServer(config.getListenAddress(), config.getDiscardPort(), "discard");
      new Thread(TcpDiscardServer).start();
      UdpServer UdpDiscardServer = new UdpServer(config.getListenAddress(), config.getDiscardPort(), "discard");
      new Thread(UdpDiscardServer).start();
    }
    if(config.getDaytimePort() != 0)
    {
      System.out.println("Starting daytime server...");
      Log.write(0, "Starting daytime server...");
      TcpServer TcpDaytimeServer = new TcpServer(config.getListenAddress(), config.getDaytimePort(), "daytime");
      new Thread(TcpDaytimeServer).start();
      UdpServer UdpDaytimeServer = new UdpServer(config.getListenAddress(), config.getDaytimePort(), "daytime");
      new Thread(UdpDaytimeServer).start();
    }
    if(config.getChargenPort() != 0)
    {
      System.out.println("Starting chargen server...");
      Log.write(0, "Starting chargen server...");
      TcpServer TcpChargenServer = new TcpServer(config.getListenAddress(), config.getChargenPort(), "chargen");
      new Thread(TcpChargenServer).start();
      UdpServer UdpChargenServer = new UdpServer(config.getListenAddress(), config.getChargenPort(), "chargen");
      new Thread(UdpChargenServer).start();
    }
    if(config.getTimePort() != 0)
    {
      System.out.println("Starting time server...");
      Log.write(0, "Starting time server...");
      TcpServer TcpTimeServer = new TcpServer(config.getListenAddress(), config.getTimePort(), "time");
      new Thread(TcpTimeServer).start();
      UdpServer UdpTimeServer = new UdpServer(config.getListenAddress(), config.getTimePort(), "time");
      new Thread(UdpTimeServer).start();
    }
    if(config.getTelnetPort() != 0)
    {
      System.out.println("Starting telnet server...");
      Log.write(0, "Starting telnet server...");
      TcpServer telnetServer = new TcpServer(config.getListenAddress(), config.getTelnetPort(), "telnet");
      new Thread(telnetServer).start();
    }
    if(config.getHttpPort() != 0)
    {
      System.out.println("Starting HTTP server...");
      Log.write(0, "Starting HTTP server...");
      TcpServer httpServer = new TcpServer(config.getListenAddress(), config.getHttpPort(), "http");
      new Thread(httpServer).start();
    }
  }

  public static void mainMenu(Config config)
  {
    while(true)
    {
      try
      {
        System.out.println("\nAdministrative Console");
        System.out.println("======================\n");
        System.out.println("1.) Open console");
        System.out.println("2.) View logs");
        System.out.println("3.) Freeze environment");
        System.out.println("4.) Show configuration");
        System.out.println("5.) Shutdown Linulator\n");
        System.out.print("> ");

        scanner = new Scanner(System.in);
        input = scanner.nextLine();

        if(input == null || input.trim().length() == 0)
          continue;

        input = input.trim();
        option = Integer.parseInt(input);
      }
      catch(NumberFormatException nfe)
      {
        System.out.println("Invalid option");
        continue;
      }
      catch(InputMismatchException ime)
      {
        System.out.println("Invalid option");
        continue;
      }

      switch(option)
      {
        case 1:
          openConsole();
          break;
        case 2:
          Log.viewLog();
          break;
        case 3:
          System.out.println("Not implemented");
          break;
        case 4:
          config.showConfig();
          break;
        case 5:
          System.out.println("Shutting down...");
          Log.write(0, "Shutting down...");
          // ToDo: stop service threads and commit database changes
          Database.shutdown();
          System.exit(0);
          break;
        default:
          System.out.println("Invalid option");
          break;
      }
    }
  }

  public static void openConsole()
  {
    Scanner scanner = null;
    String command = null;
    String result = null;

    while(true)
    {
      scanner = new Scanner(System.in);
      System.out.print("root@" + OperatingSystem.getShortName() + "# ");
      command = scanner.nextLine();

      if(command == null || command.length() == 0)
        continue;
      if(command.trim().equals("exit") || command.trim().equals("logout"))
        break;

      command = command.trim();
      String[] args = command.split("\\s+");
      result = Shell.execute(args);

      if(result == null || result.length() == 0)
        System.out.println("-bash: " + args[0] + ": command not found");
      else
        System.out.println(result);
    }
  }
}
