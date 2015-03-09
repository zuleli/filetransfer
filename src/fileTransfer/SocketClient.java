/* Author: Zule Li
 * Email:zule.li@hotmail.com
 * */

package fileTransfer;

import java.io.*;
import java.net.*;
import java.util.Vector;
import javax.swing.*;
public class SocketClient
{
	private Socket server=null;
	private ObjectInputStream input;
	private ObjectOutputStream output;
  private boolean status=false;
	
	SocketClient(String targetAddress,int targetPort,int localport)
	{
  try{
      for(int i=0;i<15;i++)
      {
      try{
          InetAddress localIP=InetAddress.getLocalHost();
          localport+=1;
			    server=new Socket(targetAddress,targetPort,localIP,localport);
//			    output=new ObjectOutputStream(server.getOutputStream());
          status=true;
          break;
          }catch(BindException b)
          {}
      }

    }
    catch(IOException e)
		{
      status=false;
		}
	}
  public void close()
  {
      try{
      if(server!=null)
      {
				server.close();
				server =null;
      }
         }catch(IOException e)
         {System.out.println(e);}
  }
  public void finalize()
  {  try{
      server.close();
      server=null;
      }catch(IOException e)
      {System.out.println(e);}
  }
  public boolean getStatus()
  {
    return status;
  }
	public void sendData(String s)
	{
		try
		{   
			Vector v=new Vector();
			v.add(s);			
			output.writeObject(v);
		}catch(IOException e)
		{
			System.out.println("Data sending failed");
		}
	}
  public void sendData(String[] s)
	{
		try
		{   
			Vector v=new Vector();
			v.add(s);			
			output.writeObject(v);
		}catch(IOException e)
		{
			System.out.println("Data2 sending failed");
		}
	}
	public Vector getData()
	{
		try
		{
   
    	input=new ObjectInputStream(server.getInputStream());
 			System.out.println("Waiting for data.....");
			Vector message=(Vector)input.readObject();

			return message;
		}catch(IOException e)
		{
      e.printStackTrace();
			return null;
		}catch(ClassNotFoundException  cnfe)
		 {
			System.out.println(cnfe);
			return null;
		}
		
	}
	public boolean bytetransfer(File file,JProgressBar bar,long L,JTextField field,JLabel label,JTextField complete)
	{
    boolean status=true;

   bar.setMaximum((int)(L/1000));
   bar.setMinimum(0);
		try
		{
    /**
			File file=new File(path,name);
      File dir=new File(path);
      if(!(dir.exists()))
      {
          File temp=new File("temp");
          if(!(temp.exists()))
          temp.mkdir();
          file=new File(temp,"unwanted.txt");
          status=false;
      }
      //*/
			DataOutputStream writer=new DataOutputStream(
									new BufferedOutputStream(
									new FileOutputStream(file)));
			DataInputStream datain=new DataInputStream(server.getInputStream());
			byte c=0;
//			byte[] end={1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};//for test end of file
           byte[] end=new byte[126];
            for(int n=0;n<end.length;n++)
            {
              end[n]=(byte)(n+1);
            }

			byte[] temp=new byte[end.length];
      byte[] bs=null;
			int match=0;
			long length=0,cal=0;
      double speed=0;
      java.util.Date date=new java.util.Date(System.currentTimeMillis()+1000);
			boolean suspect=false;
			System.out.println("start writting "+file.getName());
			while(true)
			{
        java.util.Date date0=new java.util.Date(System.currentTimeMillis());
        if(date0.after(date))
        {
            speed=length-cal;
            cal=length;
            date=new java.util.Date(System.currentTimeMillis()+1000);
            complete.setText(""+length+"/"+L);
            if(speed<=1000)
            {
                field.setText(""+speed);
                label.setText("Bytes/second");
            }else if(speed>=1000000)
            {
                field.setText(""+(speed/1024/1024));
                label.setText("MBs/second");
            }else
            {
              field.setText(""+(speed/1024));
                label.setText("KBs/second");
            }
            
        }

        
        if(L-length>2100)
        {
          bs=new byte[2000];
          length=length+datain.read(bs);
          writer.write(bs);
          continue;
        }

        c=datain.readByte();
        bar.setValue((int)(length/1000));
				if((!(c==end[0]))&& (!suspect))
				{
					writer.writeByte(c);
					length++;
					continue;
				}
				else
				{	
					
					if(c==end[match])
					{
						temp[match]=c;
						match++;
						if(match>(end.length-1))
							break;
					}
					else
					{
						for(int i=0;i<match;i++)
						{
							writer.writeByte(temp[i]);
							length++;
						}
						writer.writeByte(c);
						length++;
						suspect=false;
						match=0;
					}
					suspect=true;//when data may come to end
				
				}
   
			}
			writer.close();
      if(length==0)
      status=false;
			return status;
		}catch(IOException e)
		{
			System.out.println("byte transfer failed");
		}
    return status;
		
	}
}

