package sendMail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.naming.ServiceUnavailableException;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import com.microsoft.graph.authentication.BaseAuthenticationProvider;
import com.microsoft.graph.httpcore.HttpClients;
import com.microsoft.graph.models.BodyType;
import com.microsoft.graph.models.EmailAddress;
import com.microsoft.graph.models.ItemBody;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.Recipient;
import com.microsoft.graph.models.User;
import com.microsoft.graph.models.UserSendMailParameterSet;
import com.microsoft.graph.requests.GraphServiceClient;

import okhttp3.OkHttpClient;
import okhttp3.Request;

@RestController
@RequestMapping(path = "/send")
@CrossOrigin("*")
public class MailController {
	private String authority;
	private String tenant;
	private String clientId;
	private String clientSecret;
	private String refreshToken;
	private String refreshTokenFilePath;
	private String subject = "";
	private String body= "";
	private final String INVALID_RECEPIENT_ERR = "Mail Send failed!!. Atleast one of the provided recepient for sending email is invalid.";
	private final String INVALID_CONTENT_ERR = "Invalid Subject or Mail Body!!";
	
	public GraphServiceClient<Request> graphClient = null;
	
	@GetMapping
	public String sendMail() {
		return "Please use post request to send Email";
	}
	
	@PostMapping
	public String sendMail(@RequestBody EmailModel emailModel) {
		setParams();
		AuthenticationResult result = null;
		try {
			fetchRefreshFromDB();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if((refreshToken==null)) {
			System.out.println("Refresh token not found!! Manual User Login required");
			
		}else {
			try {
				result = getAccessTokenFromRefreshToken(refreshToken);
			}catch(Throwable e) {
				System.out.println("Access token not found!! Manual User Login required: "+e.toString());
			}
		}
		String accessToken = null;
		accessToken = result.getAccessToken();
//		System.out.println("accessToken : "+accessToken);
		refreshToken = result.getRefreshToken();
		setNewRefreshToken();
		GetClient(true, accessToken);
		
		//send mail
		subject = emailModel.getSubject();
		body = emailModel.getBody();
		List<String> toRecepient = emailModel.getTo();
		List<String> ccRecepient = emailModel.getCc();
		if(subject == null || body == null) return INVALID_CONTENT_ERR;
		if(toRecepient == null && ccRecepient == null) return INVALID_RECEPIENT_ERR;
		boolean saveToSentItems = true;
		sendMail(subject, body, toRecepient, ccRecepient ,saveToSentItems);
		return "Email sent successfully: To>> "+emailModel.getTo()+" | CC>> "+emailModel.getCc();
	}
	
	public  void setNewRefreshToken() {
		try{
	        
	        File file = new File(refreshTokenFilePath);

	        FileWriter fw = new FileWriter(file.getAbsoluteFile());
	        BufferedWriter bw = new BufferedWriter(fw);
	        bw.write(refreshToken);
	        bw.close();

	    }catch(IOException e){
	        e.printStackTrace();
	    }
	}
	
	public void fetchRefreshFromDB() throws FileNotFoundException {
		Scanner fScn = new Scanner(new File(refreshTokenFilePath));
		
		while( fScn.hasNextLine() ){
			refreshToken = fScn.nextLine();
		}
	    fScn.close();
	}
	
	
	public void setParams()  {
        Properties properties = new Properties();
        try {
			properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties"));
	        clientId = properties.getProperty("CLIENT_ID");
	        clientSecret = properties.getProperty("SECRET");
	        authority	=	properties.getProperty("AUTHORITY");
	        tenant	=	properties.getProperty("TENANT_ID");
	        refreshTokenFilePath = properties.getProperty("REFRESH_TOKEN_PATH");
	        //refreshToken = properties.getProperty("REFRESH_TOKEN");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
       
    }
	
	private AuthenticationResult getAccessTokenFromRefreshToken(
            String refreshToken) throws Throwable {
        AuthenticationContext context;
        AuthenticationResult result = null;
        ExecutorService service = null;
        try {
            service = Executors.newFixedThreadPool(1);
            context = new AuthenticationContext(authority + tenant + "/", true,
                    service);
            Future<AuthenticationResult> future = context
                    .acquireTokenByRefreshToken(refreshToken, new ClientCredential(clientId, clientSecret), null, null);
            result = future.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        } finally {
            service.shutdown();
        }

        if (result == null) {
            throw new ServiceUnavailableException("authentication result was null");
        }
        return result;
    }
	
	
	private void GetClient(boolean authenticate, String accessToken){
        //if (graphClient == null) {
			graphClient = null;
        	//System.out.println("inside getClient method: "+accessToken);
            try {
                final OkHttpClient httpClient = HttpClients.createDefault(authenticate ? getAuthenticationProvider(accessToken) : getUnauthenticationProvider());
                graphClient = GraphServiceClient.builder()
                                                .httpClient(httpClient)
                                                .buildClient();
            }
            catch (Exception e)
            {
                throw new Error("Could not create a graph client: " + e.getLocalizedMessage());
            }
       // }
    }
	
	
	public BaseAuthenticationProvider getUnauthenticationProvider() {
        return new BaseAuthenticationProvider() {
            @Override
            public CompletableFuture<String> getAuthorizationTokenAsync(final URL requestUrl) {
                return CompletableFuture.completedFuture((String)null);
            }
        };
    }
	
	public BaseAuthenticationProvider getAuthenticationProvider(final String accessToken) {
        return new BaseAuthenticationProvider() {
            @Override
            public CompletableFuture<String> getAuthorizationTokenAsync(final URL requestUrl) {
                if(this.shouldAuthenticateRequestWithUrl(requestUrl)) {
                    return CompletableFuture.completedFuture(accessToken);
                } else {
                    return CompletableFuture.completedFuture(null);
                }
            }
        };
    }
	
	public void  sendMail(String subject,
			String bodyContent,
			java.util.List<String> to,
			java.util.List<String> cc,
			boolean saveToSentItems) {
		
		@SuppressWarnings("unused")
		User me = graphClient.me().buildRequest().get();
		Message message = new Message();
		message.subject = subject;
		ItemBody body = new ItemBody();
		body.contentType = BodyType.HTML;
		body.content = bodyContent;//"<h1>The new cafeteria is open.</h1>";
		message.body = body;
		LinkedList<Recipient> toRecipientsList = new LinkedList<Recipient>();
		for(String recipient: to) {
			Recipient toRecipients = new Recipient();
			EmailAddress emailAddress = new EmailAddress();
			emailAddress.address = recipient;
			toRecipients.emailAddress = emailAddress;
			toRecipientsList.add(toRecipients);
		}
		message.toRecipients = toRecipientsList;
		LinkedList<Recipient> ccRecipientsList = new LinkedList<Recipient>();
		for(String recipient: cc) {
			Recipient ccRecipients = new Recipient();
			EmailAddress emailAddress1 = new EmailAddress();
			emailAddress1.address = recipient;
			ccRecipients.emailAddress = emailAddress1;
			ccRecipientsList.add(ccRecipients);
		}
		message.ccRecipients = ccRecipientsList;
		

		graphClient.me()
		.sendMail(UserSendMailParameterSet
				.newBuilder()
				.withMessage(message)
				.withSaveToSentItems(saveToSentItems)
				.build())
		.buildRequest()
		.post();
		System.out.println("Mail sent Successfully || to:"+to.toString()+" cc: "+cc.toString());
	}
}
