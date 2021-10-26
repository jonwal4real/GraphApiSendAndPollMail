package sendMail;

import java.util.ArrayList;

public class EmailModel {
	private String subject;
	private String body;
	private ArrayList<String> to;
	private ArrayList<String> cc;
	
	public EmailModel(String subject, String body, ArrayList<String> to, ArrayList<String> cc) {
		this.subject = subject;
		this.body = body;
		this.to = to;
		this.cc = cc;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public ArrayList<String> getTo() {
		return to;
	}

	public void setTo(ArrayList<String> to) {
		this.to = to;
	}

	public ArrayList<String> getCc() {
		return cc;
	}

	public void setCc(ArrayList<String> cc) {
		this.cc = cc;
	}

	@Override
	public String toString() {
		return "EmailModel [subject=" + subject + ", body=" + body + ", to=" + to + ", cc=" + cc + "]";
	}
	
	
}
