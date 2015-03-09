/* Author: Zule Li
 * Email:zule.li@hotmail.com
 * */

package fileTransfer;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.util.*;
import java.sql.*;
import java.net.*;
import java.io.*;
import java.awt.*;

public class Clientsite extends JFrame implements ActionListener, Runnable {
	private JButton start, stop, exit;
	private JTextField status, drive;
	private SocketClient client;
	private ClientModel model;
	private String driveValue = "", password;
	private int clientport;
	private JTable table;
	private JMenuItem help, about, pass, path;
	private boolean startstatus;

	Clientsite() {
		super();
		Dimension wndSize = getToolkit().getScreenSize();
		Dimension dim = new Dimension((int) (wndSize.width / 1.3),
				(int) (wndSize.height / 4));
		setTitle("iUpload Client");
		Border border = BorderFactory.createRaisedBevelBorder();
		start = new JButton("   Start   ");
		stop = new JButton("    Stop    ");
		exit = new JButton("     Exit    ");
		start.setBorder(border);
		start.addActionListener(this);
		stop.setBorder(border);
		stop.addActionListener(this);
		stop.setEnabled(false);
		exit.setBorder(border);
		exit.addActionListener(this);
		File config = new File("config.txt");
		try {
			RandomAccessFile file = new RandomAccessFile(config, "r");
			String s = file.readLine();
			int con = s.indexOf("=");
			int coma = s.indexOf(",");
			driveValue = (s.substring(con + 1, coma)).trim();
			drive = new JTextField(10);
			drive.setText(driveValue);
			drive.setEditable(false);

			con = s.indexOf("=", coma);
			coma = s.indexOf("#");
			String portStr = (s.substring(con + 1, coma)).trim();
			clientport = Integer.parseInt(portStr);

			status = new JTextField(20);
			status.setText("Stopped");
			status.setEditable(false);
			file.close();
		} catch (IOException e) {
			System.out.println(e);
		}
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
		path = filemenu.add("Change Directory");
		path.addActionListener(this);
		pass = filemenu.add("Change Password");
		pass.addActionListener(this);

		JPanel upper = new JPanel(new GridLayout(2, 1));
		JPanel u1 = new JPanel();
		u1.add(new JLabel("Status:"));
		u1.add(status);
		u1.add(new JLabel("Directory:"));
		u1.add(drive);
		upper.add(u1);

		JPanel u3 = new JPanel();
		u3.add(start);
		u3.add(stop);
		u3.add(exit);
		upper.add(u3);

		JPanel lower = new JPanel(new BorderLayout());
		lower.add(new JLabel("Operation record"), BorderLayout.NORTH);
		model = new ClientModel();
		table = new JTable(model);
		JScrollPane scroll = new JScrollPane(table);
		scroll.setPreferredSize(dim);
		lower.add(scroll);
		Container content = getContentPane();
		content.add(upper, BorderLayout.NORTH);
		content.add(lower);
		pack();
		setVisible(true);

	}

	public void setPassword(String p) {
		password = p;
	}

	public void run() {
		begin: while (true) {
			status.setText("Stopped");

			while (!startstatus) {
				try {
					Thread.sleep(1000);

				} catch (InterruptedException ie) {
				}
			}
			status.setText("Ready");

			while (true) {

				try {
					Thread.sleep(5000);

				} catch (InterruptedException ie) {
				}

				client = new SocketClient(clientport);
				if (!client.getStatus()) {
					continue begin;
				}
				
				client.sendData(password);
				Vector reply = client.getData();
				if (reply == null) {
					client.close();
					continue begin;
				}
				String tem = (String) (reply.get(0));
				if (!(tem.equals("APPROVED")))
					continue begin;
				long seconds = System.currentTimeMillis();
				Timestamp starttime = new Timestamp(seconds);
				status.setText("Approved");
				
				client.sendData(driveValue);
				reply = client.getData();
				String[] dir = (String[]) reply.get(0);
				String[] name = (String[]) reply.get(1);
				String[] content = (String[]) reply.get(2);
				int size = dir.length;
				String[] result = new String[size + 1];
				int attach = size - content.length;
				try {
					File[] files = new File[size];
					for (int i = 0; i < size - attach; i++) {
						files[i] = new File(driveValue + "/" + dir[i]);
						
						if (!(files[i].exists()) || content[i] == null
								|| content[i].length() == 0) {
							result[i] = "FAILURE";
							continue;
						}
						File newfile = new File(files[i], name[i]);
						RandomAccessFile writer = new RandomAccessFile(newfile,
								"rw");
						writer.writeChars(content[i]);
						writer.close();
						result[i] = "SUCCESS";
					}
					status.setText("Text files published");
					
					if (attach <= 0) {
						seconds = System.currentTimeMillis();
						Timestamp endtime = new Timestamp(seconds);
						int failed = 0;
						int success = 0;
						for (int i = 0; i < size; i++) {
							if (result[i].equalsIgnoreCase("FAILURE")) {
								failed++;
								continue;
							}
							success++;
						}
						if (failed == 0)
							result[size] = "SUCCESS";
						else if (success == 0)
							result[size] = "FAILURE";
						else
							result[size] = "PARTIAL";
						String dest = "";
						for (int i = 0; i < size; i++) {
							if (i < size)
								dest += dir[i] + "/" + name[i] + "("
										+ result[i] + "),";
						}
						client.sendData(result);
						model.setData(starttime.toString(), endtime.toString(),
								dest, result[size]);
						continue begin;

					}

					status.setText("Text files published");
					boolean err = false;
					for (int j = size - attach; j < size; j++) {

						if (!client.bytetransfer(driveValue + "/" + dir[j],
								name[j])) {
							err = true;
							result[j] = "FAILURE";
						} else
							result[j] = "SUCCESS";
					}
					seconds = System.currentTimeMillis();
					Timestamp endtime = new Timestamp(seconds);

					if (err) {
						File tobedeleted = new File("temp");
						if (tobedeleted.exists()) {
							File[] finished = tobedeleted.listFiles();
							for (int i = 0; i < finished.length; i++) {
								finished[i].delete();
							}
							tobedeleted.delete();
						}
					}
					int failed = 0;
					int success = 0;
					for (int i = 0; i < size; i++) {
						if (result[i].equalsIgnoreCase("FAILURE")) {
							failed++;
							continue;
						}
						success++;
					}
					if (failed == 0)
						result[size] = "SUCCESS";
					else if (success == 0)
						result[size] = "FAILURE";
					else
						result[size] = "PARTIAL";
					String dest = "";
					for (int i = 0; i < size; i++) {
						if (i < size)
							dest += dir[i] + "/" + name[i] + "(" + result[i]
									+ "),";
					}
					client.sendData(result);
					client.close();
					model.setData(starttime.toString(), endtime.toString(),
							dest, result[size]);
					continue begin;

				} catch (IOException io) {
					client.close();
					System.out.println(io);
				}
			}

		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == start) {
			if (password == null) {
				Password pas = new Password(this, true);
			}
			startstatus = true;
			start.setEnabled(false);
			drive.setEditable(false);
			stop.setEnabled(true);
		}
		if (e.getSource() == stop) {
			startstatus = false;
			client = null;
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
		if (e.getSource() == path) {
			while (true) {
				String dir = JOptionPane.showInputDialog(this,
						"Please enter file directory:");
				if (dir == null || dir.length() == 0) {
					if (dir == null)
						break;
					int result = JOptionPane.showConfirmDialog(this,
							"You have not specifiy a directory\n"
									+ "Do you want to try again?", "Confirm",
							JOptionPane.YES_NO_OPTION);
					if (result == JOptionPane.YES_OPTION)
						continue;
					else
						break;
				}
				drive.setText(dir);
				driveValue = dir;
				break;
			}
		}
		if (e.getSource() == pass) {
			Password passw = new Password(this, false);
		}
	}

	public static void main(String[] args) {
		Clientsite site = new Clientsite();
		Thread thread = new Thread(site);
		thread.start();
	}
}
