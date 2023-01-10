//NAHIYAN AHMED
package com.eulerity.hackathon.imagefinder;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@WebServlet(
    name = "ImageFinder",
    urlPatterns = {"/main"}
)
public class ImageFinder extends HttpServlet{
	private static final long serialVersionUID = 1L;

	protected static final Gson GSON = new GsonBuilder().create();

	//This is just a test array
	public static final String[] testImages = {
			"https://images.pexels.com/photos/545063/pexels-photo-545063.jpeg?auto=compress&format=tiny",
			"https://images.pexels.com/photos/464664/pexels-photo-464664.jpeg?auto=compress&format=tiny",
			"https://images.pexels.com/photos/406014/pexels-photo-406014.jpeg?auto=compress&format=tiny",
			"https://images.pexels.com/photos/1108099/pexels-photo-1108099.jpeg?auto=compress&format=tiny"
	};
	
	public static int count = 0;
	
	public static String findDomain(String link) {
		//Returning the entire domain name so we can filter
		//out links that are outside this domain
		String name = "";
		
		String domainEnding[] = {
				".com",".net",".org",".edu",".us",".gov",".uk",".ru",".io"
				};
		
		for(int i = 0; i < domainEnding.length; i++) {
			if(link.contains(domainEnding[i])) {
				String[] serverName = link.split(domainEnding[i]);
				name = serverName[0] + domainEnding[i];
			}
		}
		return name;
	}
	
	public static String[] findImages(String link) throws IOException {
		//Connencting to link and returning all images within
		
		Document doc = Jsoup.connect(link).get(); 
		
		Elements docImgs = doc.select("img");
        
    	String[] images = new String[docImgs.size()];
    	
        for (int i = 0; i < docImgs.size(); i++) {
        	Element img = docImgs.get(i);
        	images[i] = img.attr("src");
        }
        
        return images;
}
	
	public static String[] findLinks(Document doc, String domainName) throws IOException {
		//Collecting all links with a source or href
        Elements docLinks = doc.select("a");
        
    	String[] endPoints = new String[docLinks.size()];
    	
    	int counter = 0;
        for (int i = 0; i < docLinks.size(); i++) {
        	
        	Element link = docLinks.get(i);
        	String temp = link.attr("href");
        	
        	//Checking if the source exists
        	//Checking if link starts with a front slash 
        	//Because we need to start within the domain
        	//Counter is used to limit the number of links 
        	//For performance issues
        	
        	if(temp.length() > 0 && temp.charAt(0) == '/' && counter < 25) {
        		endPoints[counter++] = temp;
        	}
        }
        
        //Returning a completed array with the domain name and endpoints
        String[] completeLinks = new String[counter];
        
        for(int i = 0; i < counter; i++) {
        	completeLinks[i] = domainName + endPoints[i];
        }
        
		return completeLinks;
	}
	
	public static int findCount(String link) throws IOException {
		//Returning image counts of a link
		Document doc = Jsoup.connect(link).get(); 
		
		Elements docImgs = doc.select("img");
		
		return docImgs.size();
	}
	
	@Override
	protected final synchronized void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		//Timer start
		Instant start = Instant.now();

		resp.setContentType("text/json");
		
		String path = req.getServletPath();
		String url = req.getParameter("url");
		String domainName = findDomain(url);
		
		System.out.println("Got request of:" + path + " with query param:" + url);
		
		Document doc = Jsoup.connect(url).get(); 
		
		//Find all links within the domain in the page
		String [] Links = findLinks(doc, domainName);
		
		//Using hashSet to get rid of all duplicate links
        LinkedHashSet<String> linksSet = new LinkedHashSet<String>(Arrays.asList(Links));
	    
        String[] newLinks = linksSet.toArray(new String[linksSet.size()]);
        
        //Counting images from all sub pages
		int imgCounter = 0;
		
		for (int i = 0; i < newLinks.length; i++) {
			imgCounter += findCount(newLinks[i]);
		}
		
		//Initializing images array
		String [] Images = new String[imgCounter];
		
		//Setting count to user if the user sends another request
		//This variable is used to keep count of all images
		count = 0;
		
		//Using parallel stream to make use of multiple cores 
		//Thus speeding up the process
	    linksSet.parallelStream().forEach(link -> {
	    	try {
	    		//Finding images from all links and storing
				String [] temp = findImages(link);
				for(int j = 0; j < temp.length; j++) {
					Images[count++] = temp[j];
				}
			} catch (IOException e) {
				e.printStackTrace();
			} 
	    });
	    
	    //Using hashSet to get rid of all duplicate images
	    LinkedHashSet<String> noDuplicateImgs  = new LinkedHashSet<String>(Arrays.asList(Images));
	    
	    //Converting back to array
	    String [] newImages = noDuplicateImgs.toArray(new String[noDuplicateImgs.size()]);
	    
	    //Sending response with images
		resp.getWriter().print(GSON.toJson(newImages));
		
		//Timer finished and printed out
		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		System.out.println("Time Elapsed: " + timeElapsed);
	}
}
