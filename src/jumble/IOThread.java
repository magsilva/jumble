package jumble;

import java.util.List;
import java.util.LinkedList;

import java.io.*;
/** Class which constantly polls an InputStream and allows the user
 * to see if any data is available.
 * @author Tin Pavlinic
 */
public class IOThread extends Thread {
       private BufferedReader mOut;
       private BufferedReader mErr;
       private List mBuffer;
       //Constructor
       public IOThread(InputStream out) {
           mOut = new BufferedReader(new InputStreamReader(out));
           mBuffer = new LinkedList();
       }
       /** Loops while the stream exists*/
       public void run() {
           String curLine;
           try {
               while((curLine = mOut.readLine())!= null) {
                   synchronized(this) {
                       mBuffer.add(0, curLine);
                   }
               }
               //Finished here
               //System.out.println("Finished with null");
           } catch(IOException e) {
               e.printStackTrace();
           }
       }
       /** Returns the next line of text if available. Otherwise
        * returns null.
        * @return the next line of text or null
        */
       public String getNext() {
           synchronized(this) {
               if(mBuffer.size() == 0)
                   return null;
               String ret = (String)mBuffer.get(mBuffer.size() - 1);
               mBuffer.remove(mBuffer.size() - 1);
               return ret;
           }
       }
   }