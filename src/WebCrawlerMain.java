import threads.WebPage;
public class WebCrawlerMain {

	public static void main(String[] args){
		WebPage web=new WebPage("http://www.readers365.com/jinyong/08/index.htm",1);
		new Thread(web).start();		
	}
}
//http://www.readers365.com/jinyong/11/index.htm