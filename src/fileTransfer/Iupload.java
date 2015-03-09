package fileTransfer;

/* Author: Zule Li
 * Email:zule.li@hotmail.com
 * */

import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.util.*;
import java.sql.*;
import java.net.*;
import java.io.*;
import java.awt.*;

public class Iupload extends JFrame implements ActionListener, Runnable {
	private JTextField status;
	private JButton start, stop, exit;
	private JTable table;
	private ServerModel model;
	private File meta;
	private String drive, addr, iPath;
	private JMenuItem help, about, port;
	private String[] reply = null;
	private int portValue = 0;
	private byte[] add;// client IP Address
	private static ObjectInputStream input;
	private static ObjectOutputStream output;
	private static Socket client;
	private static ServerSocket server;
	private boolean startstatus = false;
	JLabel jLabel1 = new JLabel();

	// constructor:
	Iupload() {
		super();
		Dimension wndSize = getToolkit().getScreenSize();
		Dimension dim = new Dimension((int) (wndSize.width / 1.3),
				(int) (wndSize.height / 2));
		setTitle("iUpload File Transmission Server");
		start = new JButton("     Start     ");
		stop = new JButton("     Stop     ");
		exit = new JButton("     Exit     ");
		Border border = BorderFactory.createRaisedBevelBorder();
		start.setBorder(border);
		start.addActionListener(this);
		stop.setBorder(border);
		stop.addActionListener(this);
		stop.setEnabled(false);
		exit.setBorder(border);
		exit.addActionListener(this);
		JLabel statusL = new JLabel("Status");
		JLabel portnoL = new JLabel("Port Number");
		JLabel tableL = new JLabel("Transmission Record");
		status = new JTextField(20);
		status.setText("     Stand By    ");
		status.setEditable(false);
		model = new ServerModel();
		table = new JTable(model);

		JScrollPane tableScroll = new JScrollPane(table);
		tableScroll.setPreferredSize(dim);
		Container content = getContentPane();

		JMenuBar menubar = new JMenuBar();
		setJMenuBar(menubar);
		JMenu filemenu = new JMenu("File");
		JMenu helpmenu = new JMenu("Help");
		menubar.add(filemenu);
		menubar.add(helpmenu);
		help = helpmenu.add("iUpload Support");
		help.addActionListener(this);
		about = helpmenu.add("About");
		about.addActionListener(this);
		port = filemenu.add("Change Port Number");
		port.addActionListener(this);

		JPanel upper = new JPanel();
		upper.add(statusL);
		upper.add(status);
		upper.add(start);
		upper.add(stop);
		upper.add(exit);

		JPanel center = new JPanel(new BorderLayout());
		center.add(tableScroll);
		center.add(tableL, BorderLayout.NORTH);

		content.add(center);
		content.add(upper, BorderLayout.NORTH);
		pack();
		setVisible(true);
		try {
			jbInit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run() {
		begin: while (true) {
			status.setText("Stopped");
			while (!startstatus)// if start button not clicked
			{
				try {
					Thread.sleep(1000);

				} catch (InterruptedException ie) {
				}
			}
			status.setText("Detecting upload operation");
			while (true) {
				try {
					Thread.sleep(1000);

				} catch (InterruptedException ie) {
				}
				File web = new File("webfiles");
				String[] content = web.list();
				if (content.length == 0)// no meta file means no upload
										// operation
				{
					if (!startstatus)
						continue begin;
					continue;
				} else {
					meta = new File("webfiles/" + content[0]);
					break;
				}
			}
			// read meta file
			try {
				RandomAccessFile file = new RandomAccessFile(meta, "r");
				String s = file.readLine();
				file.close();
				meta.delete();
				meta = null;
				int index = 0;
				int attach = 0;
				int second = 0;
				int seperate = 0;
				index = s.indexOf(";");
				String fno = s.substring(0, index);
				int attachInd = fno.indexOf(":");
				int size = 0;
				if (attachInd < 0) {
					size = Integer.parseInt(fno);// total file number
				} else {
					size = Integer.parseInt(fno.substring(0, attachInd));// total
																			// file
																			// number
					attach = Integer.parseInt(fno.substring(attachInd + 1));// attached
																			// file
																			// number
				}
				// find out password

				second = s.indexOf(";", index + 1);
				String password = s.substring(index + 1, second);
				index = second;

				// check client password
				status.setText("Checking client password");
				long seconds = System.currentTimeMillis();
				Timestamp endcheck = new Timestamp(seconds + 600000);// 10
																		// minutes
																		// later
				Vector data = new Vector();
				if (server != null) {
					client.close();
					server.close();
					server = null;
				}
				server = new ServerSocket(portValue);
				while (true) {
					try {
						if (client != null)
							client.close();
						client = null;
						if (server == null) {
							server = new ServerSocket(portValue);
						}
						client = server.accept();
						add = (client.getInetAddress()).getAddress();
						addr = (add[0] & 0xff) + "." + (add[1] & 0xff) + "."
								+ (add[2] & 0xff) + "." + (add[3] & 0xff) + ":"
								+ client.getPort();
						status.setText(addr + " connected ");
						
						input = new ObjectInputStream(client.getInputStream());
						String passwordClient = "";
						data = (Vector) (input.readObject());
						passwordClient = (String) (data.get(0));
						
						if (password.equals(passwordClient)) {
							passwordClient = null;
							output = new ObjectOutputStream(
									client.getOutputStream());
							data = new Vector();
							data.add("APPROVED");
							output.writeObject(data);
							break;
						} else {
							client.close();
							client = null;
							server.close();
							server = null;
							seconds = System.currentTimeMillis();
							Timestamp now = new Timestamp(seconds);
							if (now.before(endcheck))
								continue;
							else {
								continue begin;
							}
						}

					} catch (IOException io) {
						continue begin;
					} catch (ClassNotFoundException e) {
						System.out.println(e);
						continue begin;
					}
				}

				password = null;
				// find oout iPath
				second = s.indexOf(";", index + 1);
				iPath = (s.substring(index + 1, second)).trim();
				index = second;

				seconds = System.currentTimeMillis();
				Timestamp stattime = new Timestamp(seconds);
				data = (Vector) input.readObject();
				drive = (String) (data.get(0));
				String[] path = new String[size];
				String[] name = new String[size];
				String[] content = new String[size - attach];
				String[] filesize = new String[size];

				for (int i = 0; i < size; i++) {
					second = s.indexOf(";", index + 1);
					String ss = s.substring(index + 1, second);
					seperate = ss.lastIndexOf("/");
					String dir = ss.substring(0, seperate);
					String filename = ss.substring(seperate + 1);
					path[i] = dir;
					name[i] = filename;
					index = second;
				}

				seconds = System.currentTimeMillis();
				Timestamp starttime = new Timestamp(seconds);
				File[] files = new File[size];
				RandomAccessFile[] random = new RandomAccessFile[size];
				for (int i = 0; i < size - attach; i++) {
					try {
						files[i] = new File(iPath + "/" + path[i] + "/"
								+ name[i]);
						filesize[i] = files[i].length() + "";
						random[i] = new RandomAccessFile(files[i], "r");
						char c = 'a';
						String str = "";
						boolean EOF = false;

						while (!EOF) {
							try {
								c = random[i].readChar();
								str += c;
							} catch (EOFException eof) {
								EOF = true;
							}
						}

						content[i] = str;// text file content
						random[i].close();
						random[i] = null;
					} catch (FileNotFoundException e) {
						System.out.println(e);
					}
				}
				status.setText("Preparing publishing Text files");
				data = new Vector();
				data.add(path);
				data.add(name);
				data.add(content);
				
				output.writeObject(data);
				status.setText("Text files sent");
				if (attach <= 0) {
					data = (Vector) input.readObject();
					reply = (String[]) (data.get(0));
					seconds = System.currentTimeMillis();
					Timestamp endtime = new Timestamp(seconds);

					// add record to log file
					File logdir = new File("logfile");
					if (!(logdir.exists()))
						logdir.mkdir();
					File log = new File("logfile/record.txt");
					if (!(log.exists()))
						log.createNewFile();
					FileWriter logWriter = new FileWriter("logfile/record.txt",
							true);
					String onerecord = "[IP:";
					onerecord += addr + ",";
					onerecord += "STARTTIME:" + starttime.toString() + ",";
					onerecord += "ENDTIME:" + endtime.toString() + ",";
					onerecord += "FILES:";
					String dest = "";
					for (int i = 0; i < size; i++) {
						if (i < size)
							dest += drive
									+ "/"
									+ path[i]
									+ "/"
									+ name[i]
									+ "("
									+ ((filesize[i] == null) ? "0"
											: filesize[i]) + ")(" + reply[i]
									+ "),";
					}
					onerecord += dest;
					onerecord += "STATUS:" + reply[size] + "]\n";
					logWriter.write(onerecord);
					logWriter.close();
					dest = "";
					for (int i = 0; i < size; i++) {
						if (i < size)
							dest += name[i]
									+ "("
									+ ((filesize[i] == null) ? "0"
											: filesize[i]) + ")(" + reply[i]
									+ "),";
					}
					model.setData(starttime.toString(), endtime.toString(),
							dest, reply[size]);
					client.close();
					server.close();
					server = null;
					continue begin;
				}
				status.setText("Preparing attached files");
				DataOutputStream dataout = new DataOutputStream(
						client.getOutputStream());
				for (int i = size - attach; i < size; i++) {
					
					try {
						files[i] = new File(iPath + "/" + path[i] + "/"
								+ name[i]);
						status.setText("start sending " + name[i]);
						DataInputStream fileinput = new DataInputStream(
								new BufferedInputStream(new FileInputStream(
										files[i])));
						byte c = 0;
						long length = 0;
						boolean EOF = false;
						status.setText("start sending " + name[i] + " file");
						while (!EOF) {
							try {
								c = fileinput.readByte();
								length++;
								dataout.writeByte(c);
							} catch (EOFException e) {
								EOF = true;
							}

						}
						filesize[i] = "" + length;
						status.setText("finish sending " + name[i]);
						
						byte[] end = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
								13, 14, 15 };// for test end of file
						for (int m = 0; m < 15; m++) {
							dataout.writeByte(end[m]);
						}
						fileinput.close();
					} catch (FileNotFoundException io) {
						System.out.println(io);
						byte[] end = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
								13, 14, 15 };// for test end of file
						for (int m = 0; m < 15; m++) {
							dataout.writeByte(end[m]);
						}
					}
					;
				}
				status.setText("All attached files sent");

				data = (Vector) input.readObject();
				reply = (String[]) (data.get(0));

				seconds = System.currentTimeMillis();
				Timestamp endtime = new Timestamp(seconds);
				// add record to log file
				File logdir = new File("logfile");
				if (!(logdir.exists()))
					logdir.mkdir();
				File log = new File("logfile/record.txt");
				if (!(log.exists()))
					log.createNewFile();
				FileWriter logWriter = new FileWriter("logfile/record.txt",
						true);
				String onerecord = "[IP:";
				onerecord += addr + ",";
				onerecord += "STARTTIME:" + starttime.toString() + ",";
				onerecord += "ENDTIME:" + endtime.toString() + ",";
				onerecord += "FILES:";
				String dest = "";

				for (int i = 0; i < size; i++) {
					if (i < size)
						dest += drive + "/" + path[i] + "/" + name[i] + "("
								+ ((filesize[i] == null) ? "0" : filesize[i])
								+ ")(" + reply[i] + "),";
				}

				onerecord += dest;
				onerecord += "STATUS:" + reply[size] + "]\n";
				logWriter.write(onerecord);
				logWriter.close();
				dest = "";
				for (int i = 0; i < size; i++) {
					if (i < size)
						dest += name[i] + "("
								+ ((filesize[i] == null) ? "0" : filesize[i])
								+ ")(" + reply[i] + "),";
				}
				model.setData(starttime.toString(), endtime.toString(), dest,
						reply[size]);
				input.close();
				client.close();
				server.close();
				server = null;
			} catch (IOException io) {
				System.out.println(io);

			} catch (ClassNotFoundException e) {
				System.out.println(e);
			}

		}

	}

	public static void main(String[] args) {
		Iupload home = new Iupload();
		Thread thread = new Thread(home);
		thread.start();
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == start) {
			if (portValue == 0) {
				while (true) {
					String dir = JOptionPane.showInputDialog(this,
							"Please enter port number:");
					if (dir == null || dir.length() == 0) {
						JOptionPane.showMessageDialog(this,
								"You have not specifiy a port number\n"
										+ "Please try again!");
						continue;

					}
					portValue = Integer.parseInt(dir);
					break;
				}

			}
			startstatus = true;
			start.setEnabled(false);
			start.setEnabled(false);
			stop.setEnabled(true);
		}
		if (e.getSource() == stop) {
			startstatus = false;
			try {
				if (server != null) {
					if (client != null)
						client.close();
					server.close();
					server = null;
				}
			} catch (IOException ie) {
			}

			start.setEnabled(true);
			stop.setEnabled(false);
		}
		if (e.getSource() == exit) {
			dispose();
			System.exit(0);
		}

		if (e.getSource() == help) {
			String message = "iUpload technical support:\n\n"
					+ "Email: support@surfmap.com\n" + "Phone: 905 681 5334\n"
					+ "SurfMap, Inc.\n" + "3385 Harvester Road\n"
					+ "Burlington, Ont\n" + "L7N 3N2\n" + "Floor 2, Suite 235";

			Display display = new Display(message);
			return;
		}
		if (e.getSource() == about) {
			String message = "iUpload Secure Access Client\n"
					+ "Version Number:2.01";

			Display display = new Display(message);
			return;
		}
		if (e.getSource() == port) {
			while (true) {
				String dir = JOptionPane.showInputDialog(this,
						"Please enter port number:");
				if (dir == null || dir.length() == 0) {
					if (dir == null)
						break;
					int result = JOptionPane.showConfirmDialog(this,
							"You have not specifiy a port number\n"
									+ "Do you want to try again?", "Confirm",
							JOptionPane.YES_NO_OPTION);
					if (result == JOptionPane.YES_OPTION)
						continue;
					else
						break;
				}
				portValue = Integer.parseInt(dir);
				if (stop.isEnabled())
					stop.doClick();
				start.doClick();
				break;
			}
		}

	}

	private void jbInit() throws Exception {
		jLabel1.setText("jLabel1");
		jLabel1.setAlignmentY((float) 4.0);
		jLabel1.setAlignmentX((float) 7.0);
		this.getContentPane().add(jLabel1, BorderLayout.WEST);
	}

}