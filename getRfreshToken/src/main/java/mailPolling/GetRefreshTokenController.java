package mailPolling;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.graph.requests.GraphServiceClient;

import okhttp3.Request;

@RestController
@RequestMapping(path = "/adal4jsample/secure/aad")
public class GetRefreshTokenController {
	public GraphServiceClient<Request> graphClient = null;
	@RequestMapping(method = { RequestMethod.GET, RequestMethod.POST })
	public String sendMail(ModelMap model,HttpServletRequest httpRequest) {
		
		HttpSession session = httpRequest.getSession();
		AuthenticationResult result = (AuthenticationResult) session.getAttribute(AuthHelper.PRINCIPAL_SESSION_NAME);
		System.out.println("inside welcome method");
		String refreshToken = result.getRefreshToken();
		System.out.println("refreshToken: "+refreshToken);
		setVar(refreshToken);
		return refreshToken;
	}
	
	public  void setVar(String refreshToken) {
		try{
	        
	        File file = new File("E:\\code\\test.txt");

	        FileWriter fw = new FileWriter(file.getAbsoluteFile());
	        BufferedWriter bw = new BufferedWriter(fw);
	        bw.write(refreshToken);
	        bw.close();

	    }catch(IOException e){
	        e.printStackTrace();
	    }
	}
	
}
