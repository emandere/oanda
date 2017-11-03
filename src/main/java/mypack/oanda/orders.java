/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mypack.oanda;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;
import static java.lang.System.out;
import static java.lang.System.err;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.lang.String;
import javax.json.*;
import java.io.StringReader;
import java.util.*;
import java.lang.InterruptedException;
/**
 *
 * @author user
 */
public class orders {
      
    private Map <String, String> requests;
    private String phpURL;        
    private int consecFailures;
    
    public orders( String url )
    {
        phpURL = url;
        requests = new HashMap();
        requests.put( "ALL", "now");
        consecFailures = 0;
    }
  
    public int getFailures()
    {
      return consecFailures;
    }
    
    public Date getTime( String date )
    {
       Date ret = new Date();
       
       if( date.length() == 16 )
       {
              int year  = Integer.valueOf(date.substring(0,4));  
              int mon   = Integer.valueOf(date.substring(5,7)) - 1;  
              int day   = Integer.valueOf(date.substring(8,10));  
              int hour  = Integer.valueOf(date.substring(11,13));
              int min  =  Integer.valueOf(date.substring(14,16));
              
              Calendar temp = Calendar.getInstance();
              temp.set( year, mon, day, hour, min, 0);
              ret = temp.getTime();
           }
           
        //out.println( ret.toString());
        return(ret);
    }
    
    public void checkForRequests()
    {
        
        Iterator it = requests.entrySet().iterator(); 
        
        Date currTime = new Date();
        Map.Entry<String, String> e;
        
        while( it.hasNext() )
        {
            e = (Map.Entry)it.next();
            
            if( e.getValue().equalsIgnoreCase("now") || currTime.after(getTime(e.getValue())))
             {    
                //out.println( "Sending " + e.getKey()+" time..." + currTime.toString());
                sendMonRequest( e.getKey() );
                //out.println(requests.toString());
            }
        }
      
    }
    
    public void sendMonRequest( String pair )
    {
        try
        {
            URL api = new URL(phpURL);
            
            try
            {
                HttpURLConnection conn = (HttpURLConnection)api.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                OutputStream conn_s = conn.getOutputStream();
                String message = "pair=" + pair + "&auto=Yes";
                
                conn_s.write( message.getBytes(), 0, message.length());
                conn.connect();
                
                BufferedReader response = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                char[] buf = new char [conn.getContentLength()*2]; 
                response.read(buf,0,conn.getContentLength());
                response.close();

                StringReader sr = new StringReader( (new String(buf)).trim() );
                JsonReader jsonR = Json.createReader(sr);
                JsonArray jsonA = jsonR.readArray();
                jsonR.close();
                
                for( int i = 0; i < jsonA.size(); i++ )
                {
                    
                    if( jsonA.getJsonObject(i).getString("status").equalsIgnoreCase("false")  )
                    {
                        consecFailures++;
                        /*out.println( jsonA.getJsonObject(i).getString("pair") + " status = " + 
                                     jsonA.getJsonObject(i).getString("status") + " consec fail = " + 
                                     consecFailures );*/
                    }
                    else if(  jsonA.getJsonObject(i).getString("status").equalsIgnoreCase("true") && 
                             !jsonA.getJsonObject(i).getString("pair").equalsIgnoreCase("ALL") )
                    {
                        consecFailures = 0;
                    }
                    
                    requests.put( jsonA.getJsonObject(i).getString("pair"), 
                                  jsonA.getJsonObject(i).getString("future") );

                    //out.println(jsonA.getJsonObject(i).getString("pair"));
                    //out.println("," + jsonA.getJsonObject(i).getString("future"));
                    
                }
                
                conn.disconnect();
            }
            catch( IOException i)
            {
                //err.println(("IO Error: " + i.getMessage());
                out.println("IO Error: " + i.getMessage());
                consecFailures++;
            }
        }
        catch ( MalformedURLException m)
        {
            //err.println("Bad URL: " + m.getMessage());
            out.println("Bad URL: " + m.getMessage());
            consecFailures++;
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        if( args.length >= 1 )
        {
            boolean createOrders = false;
            boolean monOrders = false;
            orders Create = new orders("http://localhost:122/trade/orders.php");
            orders Monitor = new orders("http://localhost:122/trade/monitor.php");
        
            
            if( Arrays.asList(args).contains("create") )
               createOrders = true;
            
            //if( Arrays.asList(args).contains("monitor") )
            //   monOrders = true;
            
            while( (createOrders || monOrders ) && 
                    Create.getFailures() <= 10 && Monitor.getFailures() <= 10 )
            {
               try
               { 
                   if( createOrders )
                       Create.checkForRequests();
               
                   if( monOrders )
                       Monitor.checkForRequests();

                   Thread.sleep(60000);
               
               
               }catch( InterruptedException i )
               {
                    out.println(i.getMessage());
               }
            
            }
             
        }
        else
        {
            out.println("run with 'create' and/or 'monitor' args");
        }
    }
}
