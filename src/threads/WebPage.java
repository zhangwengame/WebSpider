package threads;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.htmlparser.Parser;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;

public class WebPage implements Runnable{
	String url;
	String html;
	String text;
	String charSet;
	String title=new String();
	int nowChild=0;
	int depth=0;
	Vector<WebPage> childWeb=new Vector<WebPage>();
	public void setTitle(String _title){
		title=_title;
	}
	public String getTitle(){
		return title;
	}
	private String getCharSet(String content) {  

		String regex = "<meta.+?charset=[^\\w]?([-\\w]+)";  
		Pattern pattern = Pattern.compile(regex);  
		Matcher matcher = pattern.matcher(content);  
		if (matcher.find())  
			return matcher.group(1);  
		else  
			return null;  
	} 
	private String getContent(String content){
		String regex = "</script></DIV> <BR>([\\s\\S]*)<!--/HTMLBUILERPART0--></p>";  
		Pattern pattern = Pattern.compile(regex);  
		Matcher matcher = pattern.matcher(content);  
		if (matcher.find()){
			String temp=matcher.group(1);
			return temp.replace("<BR>", "").replaceAll(" +","  ");
		}
		else  
			return null;  
	}
	public int getChildSize(){
		return childWeb.size();
	}
	public WebPage(String _url,int _depth){
		url=_url;
		depth=_depth;
	}
	public String getText(){
		return text;
	}
	private void writeText() {		
		String tmp=new String();
		for (int i=0;i<childWeb.size();i++)
		{
			tmp=tmp+"\r\n"+childWeb.elementAt(i).getTitle()+"\r\n"+"================================="+"\r\n"+
					childWeb.elementAt(i).getText();
		}
		BufferedWriter fileWriter;
		try {
			fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(url.replace('/', '_').replaceAll("[\\\\?:\\*<>]", "")+".txt"))));
			fileWriter.write(tmp);
			fileWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	private void getHtml() throws Exception{
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(url);
		CloseableHttpResponse response = httpclient.execute(httpGet);
		try {
			System.out.println(url);
		    System.out.println(response.getStatusLine());
		    HttpEntity entity = response.getEntity();
		    byte raw[]=EntityUtils.toByteArray(entity);
		    html=new String(raw);
		    charSet=getCharSet(html);
		    html=new String(raw,charSet);
		    EntityUtils.consume(entity);
		    Parser parser= new Parser(html);
		    TagNameFilter aFilter=new TagNameFilter("a");
		    NodeList nodes = parser.extractAllNodesThatMatch(aFilter); 
		    for (int i = 0; i < nodes.size(); i++) {       
                LinkTag link=(LinkTag)nodes.elementAt(i);
                String linkURL=link.getAttribute("href");
                if (linkURL.charAt(0)=='/'){
                	int i1=url.indexOf("//");
                	int i2=url.indexOf('/',i1+2);
                	String leftURL=url.substring(0, i2);
                	childWeb.add(new WebPage(leftURL+linkURL,depth-1));
                }else{
                	int i1=url.lastIndexOf('/');
                	String leftURL=url.substring(0, i1+1);
                	childWeb.add(new WebPage(leftURL+linkURL,depth-1));
                }
            	childWeb.elementAt(i).setTitle(link.getLinkText().replaceAll("[\\s]"," ").replaceAll(" +"," "));
          
            }
		} finally {
		    response.close();
		}
	}
	public void run(){
		Vector<Thread> threads= new Vector<Thread>();
		try {
			getHtml();
			text=getContent(html);

		} catch (Exception e) {
			e.printStackTrace();
		}
		if (depth>0){
			for (int i=0;i<childWeb.size();i++){
				Thread tThread=new Thread(childWeb.elementAt(i));
				threads.add(tThread);
				tThread.start();
			}
		}
		for (int i=0;i<childWeb.size();i++)
			try {
				threads.elementAt(i).join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		if (depth>=1)
			writeText();
	}
}
